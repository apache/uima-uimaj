/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.uima.ep_launcher;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.impl.XCASDeserializer;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.ep_launcher.LauncherConstants.InputFormat;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.util.FileUtils;
import org.apache.uima.util.XMLInputSource;
import org.apache.uima.util.XMLSerializer;
import org.apache.uima.util.impl.ProcessTrace_impl;
import org.xml.sax.SAXException;

/**
 * The RemoteLauncher runs the actual Analysis Engine in the launched VM.
 */
public class RemoteLauncher {

  public static final String DESCRIPTOR_PARAM = "-descriptor";
  public static final String INPUT_RESOURCE_PARAM = "-inputResource";
  public static final String INPUT_RECURSIVE_PARAM = "-recursive";
  public static final String INPUT_FORMAT_PARAM = "-format";
  public static final String INPUT_ENCODING_PARAM = "-encoding";
  public static final String INPUT_LANGUAGE_PARAM = "-language";
  public static final String OUTPUT_FOLDER_PARAM = "-output";
  public static final String OUTPUT_CLEAR_PARAM = "-clear";
  
  private static File descriptor;
  private static File inputResource;
  private static boolean inputRecursive;
  private static InputFormat inputFormat = InputFormat.CAS;
  private static String inputEncoding = java.nio.charset.Charset.defaultCharset().name();
  private static String inputLanguage;
  private static File outputFolder;
  private static boolean outputFolderClear;
  
  private static boolean parseCmdLineArgs(String[] args) {
    
    int necessaryArgCount = 0;
    
    int index = 0;
    while (index < args.length) {
      
      String arg = args[index++];
      
      if (DESCRIPTOR_PARAM.equals(arg)) {
        if (index >= args.length) {
          return false;
        }
        
        descriptor = new File(args[index++]);
        necessaryArgCount++;
      }
      else if (INPUT_RESOURCE_PARAM.equals(arg)) {
        if (index >= args.length) {
          return false;
        }
        
        inputResource = new File(args[index++]);
        necessaryArgCount++;
      }
      else if (INPUT_RECURSIVE_PARAM.equals(arg)) {
        if (index >= args.length) {
          return false;
        }
        
        inputRecursive = Boolean.parseBoolean(args[index++]);
      }
      else if (INPUT_FORMAT_PARAM.equals(arg)) {
        if (index >= args.length) {
          return false;
        }
        
        String inputFormatName = args[index++];
        
        if (InputFormat.CAS.toString().equals(inputFormatName)) {
          inputFormat = InputFormat.CAS;
        }
        else if (InputFormat.PLAIN_TEXT.toString().equals(inputFormatName)) {
          inputFormat = InputFormat.PLAIN_TEXT;
        }
        else {
          System.err.println("Unkown input format: " + inputFormatName);
          return false;
        }
        
      }
      else if (INPUT_ENCODING_PARAM.equals(arg)) {
        if (index >= args.length) {
          return false;
        }
        
        inputEncoding = args[index++];
      }
      else if (INPUT_LANGUAGE_PARAM.equals(arg)) {
        if (index >= args.length) {
          return false;
        }
        
        inputLanguage = args[index++];
      }
      else if (OUTPUT_FOLDER_PARAM.equals(arg)) {
        if (index >= args.length) {
          return false;
        }
        
        outputFolder = new File(args[index++]);
      }
      else if (OUTPUT_CLEAR_PARAM.equals(arg)) {
        if (index >= args.length) {
          return false;
        }
        
        outputFolderClear = Boolean.parseBoolean(args[index++]);
      }
    }
    
    return necessaryArgCount == 2;
  }
  
  private static void processFile(File inputFile, InputFormat format, 
          AnalysisEngine aAE, CAS aCAS) throws IOException,
          AnalysisEngineProcessException {
    
    if (InputFormat.PLAIN_TEXT.equals(format)) {
      String document = FileUtils.file2String(inputFile, inputEncoding);
      document = document.trim();
  
      // put document text in CAS
      aCAS.setDocumentText(document);
      
      if (inputLanguage != null)
        aCAS.setDocumentLanguage(inputLanguage);
    }
    else if (InputFormat.CAS.equals(format)) {
      if (inputFile.getName().endsWith(".xmi")) {
        FileInputStream inputStream = new FileInputStream(inputFile);
        try {
          XmiCasDeserializer.deserialize(inputStream, aCAS, true);
        } catch (SAXException e) {
          throw new IOException(e.getMessage());
        } finally {
          inputStream.close();
        }
      }
      else if (inputFile.getName().endsWith(".xcas")) {
        FileInputStream inputStream = new FileInputStream(inputFile);
        try {
          XCASDeserializer.deserialize(inputStream, aCAS, true);
        } catch (SAXException e) {
          throw new IOException(e.getMessage());
        } finally {
          inputStream.close();
        }
      }
    }
    else {
      throw new IllegalStateException("Unexpected format!");
    }
    
    // process
    aAE.process(aCAS);

    if (outputFolder != null) {
      
      File inputDirectory;
      if (inputResource.isFile()) {
        inputDirectory = inputResource.getParentFile();
      }
      else {
        inputDirectory = inputResource;
      }
      
      String inputFilePath = inputFile.getPath();
      String relativeInputFilePath;
      if (inputFilePath.startsWith(inputDirectory.getPath())) {
        relativeInputFilePath = inputFilePath.substring(inputDirectory.getPath().length());
      }
      else {
        System.err.println("Error: Unable to construct output file path, output file will not be written!");
        return;
      }
      
      String outputFilePath = new File(outputFolder.getPath(), relativeInputFilePath).getPath();
      
      // cutoff file ending
      int fileTypeIndex = outputFilePath.lastIndexOf(".");
      if (fileTypeIndex != -1) {
        outputFilePath = outputFilePath.substring(0, fileTypeIndex);
      }
      
      File outputFile = new File(outputFilePath + ".xmi");
      
      // Create sub-directories
      if (!outputFile.getParentFile().exists()) {
        outputFile.getParentFile().mkdirs();
      }
      
      FileOutputStream out = new FileOutputStream(outputFile);
      
      try {
        // write XMI
        XmiCasSerializer ser = new XmiCasSerializer(aCAS.getTypeSystem());
        XMLSerializer xmlSer = new XMLSerializer(out, false);
        try {
          ser.serialize(aCAS, xmlSer.getContentHandler());
        } catch (SAXException e) {
          throw new IOException(e.getMessage());
        }
      } finally {
        if (out != null) {
          out.close();
        }
      }
    }
    
    // reset the CAS to prepare it for processing the next document
    aCAS.reset();
  }
  
  private static void findAndProcessFiles(File inputResource, FileFilter fileFilter, 
          AnalysisEngine aAE, CAS aCAS) throws IOException,
          AnalysisEngineProcessException {
    
    // Figure out if input resource is file or directory
    if (inputResource.isDirectory()) {
      // get all files in the input directory
      File[] files = inputResource.listFiles(fileFilter);
      if (files != null) {
        for (int i = 0; i < files.length; i++) {
          if (!files[i].isDirectory()) {
            processFile(files[i], inputFormat, aAE, aCAS);
          }
          else {
            findAndProcessFiles(files[i], fileFilter, aAE, aCAS);
          }
        }
      }
    }
    else if (inputResource.isFile()) {
      // Just process the single file
      processFile(inputResource, inputFormat, aAE, aCAS);
    }
  }
  
  private static boolean deleteFile(File file) {
    
    if (file.isDirectory()) {
      File subFiles[] = file.listFiles();
      
      boolean success = true;
      for (File subFile : subFiles) {
        success = success && deleteFile(subFile);
      }
      
      return success;
    }
    else {
      return file.delete();
    }
  }
  
  public static void main(String[] args) throws Exception {
    
    // debug / testing : see if jvm arg passed in...
    
//    MemoryMXBean memoryMxBean = ManagementFactory.getMemoryMXBean();
//    long maxHeap = memoryMxBean.getHeapMemoryUsage().getMax();
//    System.out.println("JVM MaxHeap: " + maxHeap);
    
    // show what command line args (not jvm args) got passed
//    System.out.println("Cmdline args: ");
//    for (int i = 0; i < args.length; i++) {
//      System.out.println("  arg " + i + " = " + args[i]);      
//    }
    
    if (!parseCmdLineArgs(args)) {
      throw new IllegalArgumentException("Passed arguments are invalid!");
    }
    
    if (outputFolder != null && outputFolderClear) {
      File filesToDelete[] = outputFolder.listFiles();
      
      for (File file : filesToDelete) {
        deleteFile(file);
      }
    }
    
    // get Resource Specifier from XML file
    XMLInputSource in = new XMLInputSource(descriptor);
    ResourceSpecifier specifier = UIMAFramework.getXMLParser().parseResourceSpecifier(in);
    
    // create Analysis Engine
    AnalysisEngine ae = UIMAFramework.produceAnalysisEngine(specifier);
    
    // create a CAS
    CAS cas = ae.newCAS();
    
    // Create a file filter depending on the format
    // to filter out all file which do not have the
    // expected file ending
    FileFilter fileFilter;
    if (InputFormat.CAS.equals(inputFormat)) {
      fileFilter = new FileFilter() {
        
        public boolean accept(File file) {
          return file.getName().endsWith(".xmi") || file.getName().endsWith(".xcas") || 
                  (inputRecursive && file.isDirectory());
        }
      };
    }
    else if (InputFormat.PLAIN_TEXT.equals(inputFormat)) {
      fileFilter = new FileFilter() {
        
        public boolean accept(File file) {
          return file.getName().endsWith(".txt") || (inputRecursive && file.isDirectory());
        }
      };
    }
    else {
      throw new IllegalStateException("Unexpected input format!");
    }
    
    findAndProcessFiles(inputResource, fileFilter, ae, cas);
    
    ae.collectionProcessComplete(new ProcessTrace_impl());
    ae.destroy();
  }
}
