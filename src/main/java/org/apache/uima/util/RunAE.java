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

package org.apache.uima.util;

import java.io.File;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.collection.CasConsumerDescription;
import org.apache.uima.collection.CasInitializer;
import org.apache.uima.collection.CasInitializerDescription;
import org.apache.uima.collection.CollectionProcessingManager;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.collection.EntityProcessStatus;
import org.apache.uima.collection.StatusCallbackListener;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.resource.metadata.ConfigurationParameterSettings;
import org.apache.uima.util.AnalysisEnginePerformanceReports;
import org.apache.uima.util.FileSystemCollectionReader;
import org.apache.uima.util.InlineXmlCasConsumer;
import org.apache.uima.util.SimpleXmlCasInitializer;
import org.apache.uima.util.XMLInputSource;

/**
 * An example application that reads documents from the file system, sends them
 * though an Analysis Engine(AE), and produces XML files with inline 
 * annotations.  This application uses the {@link CollectionProcessingManager}
 * to drive the processing.  For a simpler introduction to using AEs in an
 * application, see {@link ExampleApplication}.
 * <p>
 * <code>Usage: java org.apache.uima.examples.RunAE [OPTIONS] 
 * &lt;AE descriptor or JAR file name&gt; &lt;input dir&gt; 
 * [&lt;output dir&gt;]</code> 
 * <p>
 * If <code>output dir</code> is not specified, the analysis results will not 
 * be output.  This can be useful when only interested in performance statistics.
 * <p>
 * <u>OPTIONS</u>
 * <p>
 * -t &lt;TagName&gt; (XML Text Tag) - specifies the name of an XML tag, found 
 * within the input documents, that contains the text to be analyzed.  Documents 
 * not containing this tag will not be processed.  If this option is not 
 * specified, the entire document text will be processed.
 * <br>
 * -l &lt;ISO code&gt; (Language) - specifies the ISO code for the language of
 * the input documents.  Some AEs require this.
 * <br>
 * -e &lt;Encoding&gt; - specifies character encoding of the input documents.
 * The default is UTF-8.
 * <br> 
 * -q (Quiet) - supresses progress messages that are normally printed as each 
 * document is processed.
 * <br>
 * -s&lt;x&gt; (Stats level) - determines the verboseness of performance
 *  statistics.  s0=none, s1=brief, s2=full.  The default is brief. 
 * <br>
 * -x - process input files as XCAS files.
 * 
 * 
 */
public class RunAE implements StatusCallbackListener
{
	int docsProcessed;
	
  /**
   * Constructor.  Sets up and runs a Text Analysis Engine.  
   */
  public RunAE(String[] args)
  {
    try
    {
      //Read and validate command line arguments
      if (!processCmdLineArgs(args))
      {
        printUsageMessage();
        return;
      }  
      
      //Enable schema validation (omit this to speed up initialization)
      //UIMAFramework.getXMLParser().enableSchemaValidation(true);
      
      //create CPM instance that will drive processing
      mCPM = UIMAFramework.newCollectionProcessingManager();

      //read AE descriptor from file
      long startTime = System.currentTimeMillis();
      XMLInputSource in = new XMLInputSource(aeSpecifierFile);
      ResourceSpecifier aeSpecifier =
         UIMAFramework.getXMLParser().parseResourceSpecifier(in);
         
      //taeSpecifier.toXML(new FileWriter("spec.xml"));
            
      //instantiate AE
      AnalysisEngine ae = UIMAFramework.produceAnalysisEngine(aeSpecifier);
      long initTime = System.currentTimeMillis() - startTime;
      System.out.println("AE initialized in " + initTime + "ms.");
      mCPM.setAnalysisEngine(ae);

      //create and configure collection reader that will read input docs
      CollectionReaderDescription collectionReaderDesc =
          FileSystemCollectionReader.getDescription();
      ConfigurationParameterSettings paramSettings =
          collectionReaderDesc.getMetaData().getConfigurationParameterSettings();
      paramSettings.setParameterValue(
          FileSystemCollectionReader.PARAM_INPUTDIR, inputDir.getAbsolutePath());
      paramSettings.setParameterValue(
          FileSystemCollectionReader.PARAM_ENCODING, encoding);
      paramSettings.setParameterValue(
          FileSystemCollectionReader.PARAM_LANGUAGE, language);
      paramSettings.setParameterValue(
              FileSystemCollectionReader.PARAM_XCAS, Boolean.toString(xcasInput));
      CollectionReader collectionReader =
          UIMAFramework.produceCollectionReader(collectionReaderDesc);

			//if XML tag was specified, also create SimpleXmlCasInitializer to handle this
			if (xmlTagName != null && xmlTagName.length() > 0)
			{
				CasInitializerDescription casIniDesc =
						SimpleXmlCasInitializer.getDescription();
				ConfigurationParameterSettings casIniParamSettings =
						casIniDesc.getMetaData().getConfigurationParameterSettings();
				casIniParamSettings.setParameterValue(
						SimpleXmlCasInitializer.PARAM_XMLTAG, xmlTagName);
				CasInitializer casInitializer = UIMAFramework.produceCasInitializer(casIniDesc);
				collectionReader.setCasInitializer(casInitializer);
			}
                
      //create and configure CAS consumer that will write the output
      if (outputDir != null)
      {   
        CasConsumerDescription casConsumerDesc = 
            InlineXmlCasConsumer.getDescription();
        ConfigurationParameterSettings consumerParamSettings =
            casConsumerDesc.getMetaData().getConfigurationParameterSettings();
        consumerParamSettings.setParameterValue(
          InlineXmlCasConsumer.PARAM_OUTPUTDIR, outputDir.getAbsolutePath());
        consumerParamSettings.setParameterValue(
          InlineXmlCasConsumer.PARAM_XCAS, Boolean.toString(xcasInput));
        mCPM.addCasConsumer(UIMAFramework.produceCasConsumer(casConsumerDesc));  
      }

      //register callback listener
      mCPM.addStatusCallbackListener(this);      
     
      //execute
      docsProcessed = 0;
      mCPM.process(collectionReader);
    }
    catch(Exception e)
    {
      e.printStackTrace();
    } 
  }
  


  /**
   * @see org.apache.uima.collection.base_cpm.BaseStatusCallbackListener#initializationComplete()
   */
  public void initializationComplete()
  {
  }

  /**
   * @see org.apache.uima.collection.StatusCallbackListener#entityProcessComplete(org.apache.uima.cas.CAS, org.apache.uima.collection.EntityProcessStatus)
   */
  public void entityProcessComplete(CAS aCas, EntityProcessStatus aStatus)
  {
    if (aStatus.isException())
    {
      mCPM.stop();
      ((Exception)aStatus.getExceptions().get(0)).printStackTrace();
    }
    else if (genProgressMessages)
    {
      // retreive the filename of the input file from the CAS 
      // (it was put there by the FileSystemCollectionReader)
      if (!xcasInput) {
          Type fileLocType = aCas.getTypeSystem().getType("org.apache.uima.examples.SourceDocumentInformation");
          Feature fileNameFeat = fileLocType.getFeatureByBaseName("uri");
          FSIterator it = aCas.getAnnotationIndex(fileLocType).iterator();
          FeatureStructure fileLoc = it.get();
          File inFile = new File(fileLoc.getStringValue(fileNameFeat));
          System.out.println("Processed Document " + inFile.getName());
      }
      else {
	      System.out.println("doc" + docsProcessed++ + " processed successfully");
      }
    }
  }

  /**
   * @see org.apache.uima.collection.base_cpm.BaseStatusCallbackListener#aborted()
   */
  public void aborted()
  {
    System.out.println("Processing Aborted");

  }

  /**
   * @see org.apache.uima.collection.base_cpm.BaseStatusCallbackListener#batchProcessComplete()
   */
  public void batchProcessComplete()
  {
  }

  /**
   * @see org.apache.uima.collection.base_cpm.BaseStatusCallbackListener#collectionProcessComplete()
   */
  public void collectionProcessComplete()
  {
    //output performance stats
    if (statsLevel > 0)
    {
      AnalysisEnginePerformanceReports performanceReports = 
        new AnalysisEnginePerformanceReports(mCPM.getPerformanceReport());
      System.out.println("\n\nPERFORMANCE STATS\n-----------------\n\n");
      if (statsLevel > 1)
      {
        System.out.println(performanceReports.getFullReport());
        System.out.println();
      }
      System.out.println(performanceReports);     
    }  
  }


  /**
   * @see org.apache.uima.collection.base_cpm.BaseStatusCallbackListener#paused()
   */
  public void paused()
  {
  }

  /**
   * @see org.apache.uima.collection.base_cpm.BaseStatusCallbackListener#resumed()
   */
  public void resumed()
  {
  }



 /**
  * Prints usage message.
  */ 
  private void printUsageMessage()
  {
    System.err.println("\nUsage: java " + this.getClass().getName() + 
      " [OPTIONS] <AE descriptor or JAR file name> <input dir> [<output dir>] ");
    System.err.println("\nIf <output dir> is not specified, the analysis " +
      "results will not be output.  This can be useful when only interested " +
      "in performance statistics.");
    System.err.println("\nOPTIONS\n-------");
    System.err.println("-t <TagName> (XML Text Tag) - specifies the name of " +
      "an XML tag, found within the input documents, that contains the text " +
      "to be analyzed.  Documents not containing this tag will not be " +
      "processed.  If this option is not specified, the entire document text " +
      "will be processed.");
    System.err.println("-q (Quiet) - supresses progress messages that are " +
      "normally printed as each document is processed.");  
    System.err.println("-s<x> (Stats level) - determines the verboseness of " +
      "performance statistics.  s0=none, s1=brief, s2=full.  The default is brief."); 
    System.err.println("-x - process input files as XCAS files."); 

  }
  
 /**
  * Reads command line arguments and sets static class variables appropriately.
  * 
  * @return true if command line args were valid, false if not
  */
  private boolean processCmdLineArgs(String[] args)
  {
    encoding = "UTF-8"; //default
    int index = 0;
    while (index < args.length)
    {
      String arg = args[index++];
      if (arg.equals("-q")) //quiet mode
      {
        genProgressMessages = false;
      }
      else if (arg.equals("-s0")) //no stats
      {
        statsLevel = 0;
      }
      else if (arg.equals("-s2")) //full stats
      {
        statsLevel = 2;
      }
      else if (arg.equals("-t")) //XML tag text
      {
        //tag name is next argument
        if (index >= args.length)
        {
          return false;
        }  
        xmlTagName = args[index++];
      }
      else if (arg.equals("-l")) //Language
      {
        //language ISO code is next argument
        if (index >= args.length)
        {
          return false;
        }
        language = args[index++];
      }
      else if (arg.equals("-e")) //Encoding
      {
        //encoding is next argument
        if (index >= args.length)
        {
          return false;
        }
        encoding = args[index++];
      }
      else if (arg.equals("-x")) //XCAS file input
      {
        xcasInput = true;
      }
      else //one of the standard params - whichever we haven't read yet
      {
        if (aeSpecifierFile == null)
        {   
          aeSpecifierFile = new File(arg);
          if (!aeSpecifierFile.exists() || aeSpecifierFile.isDirectory())
          {
            System.err.println(arg + " does not exist");
            System.exit(1);
          }
        }
        else if (inputDir == null)
        {
          inputDir = new File(arg);
          if (!inputDir.exists() || !inputDir.isDirectory())
          {
            System.err.println(arg + " does not exist or is not a directory");
            System.exit(1);
          }
        }  
        else if (outputDir == null)
        {
          outputDir = new File(arg);
          if (!outputDir.exists() && !outputDir.mkdirs())
          {
            System.err.println(arg + " does not exist and could not be created");
            System.exit(1);
          }
        }  
      }
    }
    //make sure required values were specified
    return (aeSpecifierFile != null) && (inputDir != null);    
  }

  public static void main(String[] args)
  {
    new RunAE(args);
  }    



  //Values read from cmd line args
  private File aeSpecifierFile = null;
  private File inputDir = null;
  private File outputDir = null;
  private String xmlTagName = null;
  private String language;
  private String encoding;
  private boolean genProgressMessages = true;
  private int statsLevel = 1;  
  private CollectionProcessingManager mCPM;
  private boolean xcasInput = false;

}
