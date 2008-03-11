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

package org.apache.uima.examples;

import java.io.File;
import java.util.Iterator;

import org.apache.uima.UIMAException;
import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.collection.CollectionProcessingEngine;
import org.apache.uima.collection.EntityProcessStatus;
import org.apache.uima.collection.StatusCallbackListener;
import org.apache.uima.collection.impl.metadata.cpe.CpeDescriptorFactory;
import org.apache.uima.collection.metadata.CasProcessorConfigurationParameterSettings;
import org.apache.uima.collection.metadata.CpeCasProcessor;
import org.apache.uima.collection.metadata.CpeCollectionReader;
import org.apache.uima.collection.metadata.CpeDescription;
import org.apache.uima.collection.metadata.CpeSofaMapping;
import org.apache.uima.collection.metadata.CpeSofaMappings;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.tools.components.FileSystemCollectionReader;
import org.apache.uima.tools.components.InlineXmlCasConsumer;
import org.apache.uima.tools.components.XmlDetagger;
import org.apache.uima.util.AnalysisEnginePerformanceReports;

/**
 * An example application that reads documents from the file system, sends them though an Analysis
 * Engine(AE), and produces XML files with inline annotations. This application uses a
 * {@link CollectionProcessingEngine} to drive the processing. For a simpler introduction to using
 * AEs in an application, see {@link ExampleApplication}.
 * <p>
 * <code>Usage: java org.apache.uima.examples.RunAE [OPTIONS] 
 * &lt;AE descriptor or JAR file name&gt; &lt;input dir&gt; 
 * [&lt;output dir&gt;]</code>
 * <p>
 * If <code>output dir</code> is not specified, the analysis results will not be output. This can
 * be useful when only interested in performance statistics.
 * <p>
 * <u>OPTIONS</u>
 * <p>
 * -t &lt;TagName&gt; (XML Text Tag) - specifies the name of an XML tag, found within the input
 * documents, that contains the text to be analyzed. The text will also be detagged. If this option
 * is not specified, the entire document will be processed. <br>
 * -l &lt;ISO code&gt; (Language) - specifies the ISO code for the language of the input documents.
 * Some AEs require this. <br>
 * -e &lt;Encoding&gt; - specifies character encoding of the input documents. The default is UTF-8.
 * <br>
 * -q (Quiet) - supresses progress messages that are normally printed as each document is processed.
 * <br>
 * -s&lt;x&gt; (Stats level) - determines the verboseness of performance statistics. s0=none,
 * s1=brief, s2=full. The default is brief. <br>
 * -x - process input files as XCAS files.
 */
public class RunAE implements StatusCallbackListener {

  // Values read from cmd line args
  private File aeSpecifierFile = null;

  private File inputDir = null;

  private File outputDir = null;

  private String xmlTagName = null;

  private String language;

  private String encoding;

  private boolean genProgressMessages = true;

  private int statsLevel = 1;

  private boolean xcasInput = false;

  int docsProcessed;

  private CollectionProcessingEngine mCPE;

  /**
   * Constructor. Sets up and runs an Analysis Engine.
   */
  public RunAE(String[] args) {
    try {
      // Read and validate command line arguments
      if (!processCmdLineArgs(args)) {
        printUsageMessage();
        return;
      }

      // Enable schema validation (omit this to speed up initialization)
      // UIMAFramework.getXMLParser().enableSchemaValidation(true);

      // build a Collection Processing Engine descriptor that will drive processing
      CpeDescription cpeDesc = CpeDescriptorFactory.produceDescriptor();

      // add collection reader that will read input docs
      cpeDesc.addCollectionReader(FileSystemCollectionReader.getDescriptorURL().toString());
      // specify configuration parameters for collection reader
      CasProcessorConfigurationParameterSettings crSettings = CpeDescriptorFactory
              .produceCasProcessorConfigurationParameterSettings();
      CpeCollectionReader cpeCollRdr = cpeDesc.getAllCollectionCollectionReaders()[0];
      cpeCollRdr.setConfigurationParameterSettings(crSettings);
      crSettings.setParameterValue(FileSystemCollectionReader.PARAM_INPUTDIR, inputDir
              .getAbsolutePath());
      crSettings.setParameterValue(FileSystemCollectionReader.PARAM_ENCODING, encoding);
      crSettings.setParameterValue(FileSystemCollectionReader.PARAM_LANGUAGE, language);
      crSettings.setParameterValue(FileSystemCollectionReader.PARAM_XCAS, Boolean
              .toString(xcasInput));

      // if XML tag was specified, configure XmlDetagger annotator and add to CPE
      CpeCasProcessor xmlDetaggerCasProc = null;
      if (xmlTagName != null && xmlTagName.length() > 0) {
        xmlDetaggerCasProc = CpeDescriptorFactory.produceCasProcessor("XmlDetagger");
        xmlDetaggerCasProc.setDescriptor(XmlDetagger.getDescriptorURL().toString());
        CasProcessorConfigurationParameterSettings detaggerSettings = CpeDescriptorFactory
                .produceCasProcessorConfigurationParameterSettings();
        xmlDetaggerCasProc.setConfigurationParameterSettings(detaggerSettings);
        detaggerSettings.setParameterValue(XmlDetagger.PARAM_TEXT_TAG, xmlTagName);
        xmlDetaggerCasProc.setMaxErrorCount(0);
        cpeDesc.addCasProcessor(xmlDetaggerCasProc);
      }

      // add user's AE to CPE
      CpeCasProcessor casProc = CpeDescriptorFactory.produceCasProcessor("UserAE");
      casProc.setDescriptor(aeSpecifierFile.getAbsolutePath());
      casProc.setMaxErrorCount(0);
      cpeDesc.addCasProcessor(casProc);

      // add CAS Consumer that will write the output
      // create and configure CAS consumer that will write the output
      CpeCasProcessor casCon = null;
      if (outputDir != null) {
        casCon = CpeDescriptorFactory.produceCasProcessor("CasConsumer");
        casCon.setDescriptor(InlineXmlCasConsumer.getDescriptorURL().toString());
        CasProcessorConfigurationParameterSettings consumerSettings = CpeDescriptorFactory
                .produceCasProcessorConfigurationParameterSettings();
        casCon.setConfigurationParameterSettings(consumerSettings);
        consumerSettings.setParameterValue(InlineXmlCasConsumer.PARAM_OUTPUTDIR, outputDir
                .getAbsolutePath());
        consumerSettings.setParameterValue(InlineXmlCasConsumer.PARAM_XCAS, Boolean
                .toString(xcasInput));
        casCon.setMaxErrorCount(0);
        cpeDesc.addCasProcessor(casCon);
      }

      // if XML detagger is used, we need to configure sofa mappings for the CPE
      if (xmlDetaggerCasProc != null) {
        // For XML detagger map default sofa to "xmlDocument"
        CpeSofaMapping sofaMapping = CpeDescriptorFactory.produceSofaMapping();
        sofaMapping.setComponentSofaName("xmlDocument");
        sofaMapping.setCpeSofaName(CAS.NAME_DEFAULT_SOFA);
        CpeSofaMappings xmlDetaggerSofaMappings = CpeDescriptorFactory.produceSofaMappings();
        xmlDetaggerSofaMappings.setSofaNameMappings(new CpeSofaMapping[] { sofaMapping });
        xmlDetaggerCasProc.setSofaNameMappings(xmlDetaggerSofaMappings);

        // User AE and InlineXmlCasConsumer (if present) operate on the "plainTextDocument"
        // sofa produced by the XmlDetagger
        CpeSofaMapping aeSofaMapping = CpeDescriptorFactory.produceSofaMapping();
        aeSofaMapping.setCpeSofaName("plainTextDocument");
        CpeSofaMappings userAeSofaMappings = CpeDescriptorFactory.produceSofaMappings();
        userAeSofaMappings.setSofaNameMappings(new CpeSofaMapping[] { aeSofaMapping });
        casProc.setSofaNameMappings(userAeSofaMappings);

        if (casCon != null) {
          CpeSofaMapping casConSofaMapping = CpeDescriptorFactory.produceSofaMapping();
          casConSofaMapping.setCpeSofaName("plainTextDocument");
          CpeSofaMappings consumerSofaMappings = CpeDescriptorFactory.produceSofaMappings();
          consumerSofaMappings.setSofaNameMappings(new CpeSofaMapping[] { casConSofaMapping });
          casCon.setSofaNameMappings(consumerSofaMappings);
        }
      }

      // instantiate CPE
      mCPE = UIMAFramework.produceCollectionProcessingEngine(cpeDesc);
      // register callback listener
      mCPE.addStatusCallbackListener(this);

      // execute
      docsProcessed = 0;
      mCPE.process();
    } catch (Exception e) {
      //special check for using XML detagger with remotes, which will generate an error
      //since sofa mappings aren't supported for remotes
      if (xmlTagName != null && xmlTagName.length() > 0 && e instanceof UIMAException &&
              ((UIMAException)e).hasMessageKey(ResourceInitializationException.SOFA_MAPPING_NOT_SUPPORTED_FOR_REMOTE)) {
        System.err.println("The XML detagging feature (-t) is not supported for remote Analysis Engines or for Aggregates containing remotes.");
      }
      else {
        e.printStackTrace();
      }
    }
  }
  

  /**
   * @see org.apache.uima.collection.base_cpm.BaseStatusCallbackListener#initializationComplete()
   */
  public void initializationComplete() {
  }

  /**
   * @see org.apache.uima.collection.StatusCallbackListener#entityProcessComplete(org.apache.uima.cas.CAS,
   *      org.apache.uima.collection.EntityProcessStatus)
   */
  public void entityProcessComplete(CAS aCas, EntityProcessStatus aStatus) {
    if (aStatus.isException()) {
      Iterator iter = aStatus.getExceptions().iterator();
      while (iter.hasNext()) {
        ((Throwable) iter.next()).printStackTrace();
      }
    } else if (genProgressMessages) {
      // retreive the filename of the input file from the CAS
      // (it was put there by the FileSystemCollectionReader)
      if (!xcasInput) {
        Type fileLocType = aCas.getTypeSystem().getType(
                "org.apache.uima.examples.SourceDocumentInformation");
        Feature fileNameFeat = fileLocType.getFeatureByBaseName("uri");
        FSIterator it = aCas.getAnnotationIndex(fileLocType).iterator();
        FeatureStructure fileLoc = it.get();
        File inFile = new File(fileLoc.getStringValue(fileNameFeat));
        System.out.println("Processed Document " + inFile.getName());
      } else {
        System.out.println("doc" + docsProcessed++ + " processed successfully");
      }
    }
  }

  /**
   * @see org.apache.uima.collection.base_cpm.BaseStatusCallbackListener#aborted()
   */
  public void aborted() {
    System.out.println("Processing Aborted");

  }

  /**
   * @see org.apache.uima.collection.base_cpm.BaseStatusCallbackListener#batchProcessComplete()
   */
  public void batchProcessComplete() {
  }

  /**
   * @see org.apache.uima.collection.base_cpm.BaseStatusCallbackListener#collectionProcessComplete()
   */
  public void collectionProcessComplete() {
    // output performance stats
    if (statsLevel > 0) {
      AnalysisEnginePerformanceReports performanceReports = new AnalysisEnginePerformanceReports(
              mCPE.getPerformanceReport());
      System.out.println("\n\nPERFORMANCE STATS\n-----------------\n\n");
      if (statsLevel > 1) {
        System.out.println(performanceReports.getFullReport());
        System.out.println();
      }
      System.out.println(performanceReports);
    }
  }

  /**
   * @see org.apache.uima.collection.base_cpm.BaseStatusCallbackListener#paused()
   */
  public void paused() {
  }

  /**
   * @see org.apache.uima.collection.base_cpm.BaseStatusCallbackListener#resumed()
   */
  public void resumed() {
  }

  /**
   * Prints usage message.
   */
  private void printUsageMessage() {
    System.err.println("\nUsage: java " + this.getClass().getName()
            + " [OPTIONS] <AE descriptor filename> <input dir> [<output dir>] ");
    System.err.println("\nIf <output dir> is not specified, the analysis "
            + "results will not be output.  This can be useful when only interested "
            + "in performance statistics.");
    System.err.println("\nOPTIONS\n-------");
    System.err.println("-t <TagName> (XML Text Tag) - specifies the name of "
            + "an XML tag, found within the input documents, that contains the text "
            + "to be analyzed.  The text will also be detagged. If this option is not "
            + "specified, the entire document will be processed.");
    System.err.println("-q (Quiet) - supresses progress messages that are "
            + "normally printed as each document is processed.");
    System.err.println("-s<x> (Stats level) - determines the verboseness of "
            + "performance statistics.  s0=none, s1=brief, s2=full.  The default is brief.");
    System.err.println("-x - process input files as XCAS files.");

  }

  /**
   * Reads command line arguments and sets static class variables appropriately.
   * 
   * @return true if command line args were valid, false if not
   */
  private boolean processCmdLineArgs(String[] args) {
    encoding = "UTF-8"; // default
    int index = 0;
    while (index < args.length) {
      String arg = args[index++];
      if (arg.equals("-q")) // quiet mode
      {
        genProgressMessages = false;
      } else if (arg.equals("-s0")) // no stats
      {
        statsLevel = 0;
      } else if (arg.equals("-s2")) // full stats
      {
        statsLevel = 2;
      } else if (arg.equals("-t")) // XML tag text
      {
        // tag name is next argument
        if (index >= args.length) {
          return false;
        }
        xmlTagName = args[index++];
      } else if (arg.equals("-l")) // Language
      {
        // language ISO code is next argument
        if (index >= args.length) {
          return false;
        }
        language = args[index++];
      } else if (arg.equals("-e")) // Encoding
      {
        // encoding is next argument
        if (index >= args.length) {
          return false;
        }
        encoding = args[index++];
      } else if (arg.equals("-x")) // XCAS file input
      {
        xcasInput = true;
      } else // one of the standard params - whichever we haven't read yet
      {
        if (aeSpecifierFile == null) {
          aeSpecifierFile = new File(arg);
          if (!aeSpecifierFile.exists() || aeSpecifierFile.isDirectory()) {
            System.err.println(arg + " does not exist");
            System.exit(1);
          }
        } else if (inputDir == null) {
          inputDir = new File(arg);
          if (!inputDir.exists() || !inputDir.isDirectory()) {
            System.err.println(arg + " does not exist or is not a directory");
            System.exit(1);
          }
        } else if (outputDir == null) {
          outputDir = new File(arg);
          if (!outputDir.exists() && !outputDir.mkdirs()) {
            System.err.println(arg + " does not exist and could not be created");
            System.exit(1);
          }
        }
      }
    }
    // make sure required values were specified
    return (aeSpecifierFile != null) && (inputDir != null);
  }

  public static void main(String[] args) {
    new RunAE(args);
  }
}