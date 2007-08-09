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

package org.apache.uima.test.junit_extension;

import java.io.File;
import java.io.FileFilter;
import java.util.HashMap;
import java.util.logging.LogManager;

import junit.framework.Assert;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.cas.CAS;
import org.apache.uima.internal.util.Timer;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.util.FileUtils;
import org.apache.uima.util.Level;
import org.apache.uima.util.Logger;
import org.apache.uima.util.XMLInputSource;

/**
 * AnnotatorPerfTester is a helper class to execute annotator performance tests. The performance
 * test results are returned as {@link PerformanceTestResultImpl} object.
 * 
 */
public class AnnotatorPerformanceTester {

  private static class FileFileFilter implements FileFilter {

    private FileFileFilter() {
      super();
    }

    public boolean accept(File arg0) {
      return arg0.isFile();
    }

  }

  private static HashMap logLevels = new HashMap(9);
  static {
    logLevels.put("OFF", Level.OFF);
    logLevels.put("SEVERE", Level.SEVERE);
    logLevels.put("WARNING", Level.WARNING);
    logLevels.put("INFO", Level.INFO);
    logLevels.put("CONFIG", Level.CONFIG);
    logLevels.put("FINE", Level.FINE);
    logLevels.put("FINER", Level.FINER);
    logLevels.put("FINEST", Level.FINEST);
    logLevels.put("ALL", Level.ALL);
  }

  /**
   * runs an annotator performance test
   * 
   * @param repeatSingle
   *          if true, every document is process "numsToRun" times before the next document is
   *          processed. If false, all documents are processed and this is repeated "numsToRun"
   *          times.
   * 
   * @param numsToRun
   *          repeat count for the input documents
   * @param taeDescFilePath
   *          ae descriptor - absolute file path
   * @param testFileDir
   *          test file directory
   * @param dataPath
   *          ae datapath
   * @param doWarmup
   *          do warum for analysis engine - runs an short english sample document
   * @return PerformanceTestResult - returns the performance test results
   * 
   * @throws Exception
   */
  public static PerformanceTestResult runPerformanceTest(boolean repeatSingle, int numsToRun,
          File taeDescFilePath, File testFileDir, String dataPath, boolean doWarmup)
          throws Exception {

    // create performance result object
    PerformanceTestResultImpl result = new PerformanceTestResultImpl();

    // check mandetory settings
    Assert.assertNotNull(taeDescFilePath);
    Assert.assertNotNull(testFileDir);

    // save settings
    result.setRepeatSingleMode(repeatSingle);
    result.setDoWarmup(doWarmup);
    result.setNumsToRun(numsToRun);
    result.setAeDescFilePath(taeDescFilePath);
    result.setTestFileDir(testFileDir);
    result.setDatapath(dataPath);

    // set and check test file directory
    if (testFileDir == null || !testFileDir.isDirectory() || !testFileDir.canRead()) {
      throw new Exception("test file directory not valid");
    }

    // get current log level setting
    Level defaultLogLevel = (Level) logLevels.get(LogManager.getLogManager()
            .getProperty(".level"));

    if (defaultLogLevel == null) {
      // no log level was specified, use default log level settings "INFO" that is also
      // used by the Java logging framework.
      defaultLogLevel = Level.INFO;
    }
    // turn of logging for the performance test
    Logger logger = UIMAFramework.getLogger();
    logger.setLevel(Level.OFF);

    //create timer 
    Timer globalTimer = new Timer();
    Timer initTimer = new Timer();
    Timer warmupTimer = new Timer();
    Timer ioTimer = new Timer();
    Timer processResetTimer = new Timer();
    Timer cleanupTimer = new Timer();
    Timer documentPreparationTimer = new Timer();
    
    //start timer for global time
    globalTimer.start();

    // init analysis engine
    try {

      // start initialization timer   
      initTimer.start();
      
      // set datapath
      ResourceManager resMgr = UIMAFramework.newDefaultResourceManager();
      if (dataPath != null) {
        resMgr.setDataPath(dataPath);
      }

      AnalysisEngine ae = null;
      CAS cas = null;
      // get resource specifier from XML file
      XMLInputSource in = new XMLInputSource(taeDescFilePath);
      ResourceSpecifier specifier = UIMAFramework.getXMLParser().parseResourceSpecifier(in);

      // create analysis engine with resource manager
      ae = UIMAFramework.produceAnalysisEngine(specifier, resMgr, null);
      // check ae
      Assert.assertNotNull(ae);

      // create new cas
      cas = ae.newCAS();
      // check cas
      Assert.assertNotNull(cas);

      // access cas type system
      cas.getTypeSystem().getFeatureByFullName(CAS.FEATURE_FULL_NAME_LANGUAGE);

      // stop initalization timer
      initTimer.stop();
      result.setInitTime(initTimer.getTimeSpan());

      if (doWarmup) {
        // start warmup timer     
        warmupTimer.start();

        // process dummy document
        cas.setDocumentLanguage("en");
        cas.setDocumentText("This is a test sentence.");
        ae.process(cas);
        cas.reset();

        // stop warmup timer
        warmupTimer.stop();
        result.setWarmupTime(warmupTimer.getTimeSpan());
      }

      // start io timer
      ioTimer.start();

      // read all files in the test file directory
      File[] inputFiles = testFileDir.listFiles(new FileFileFilter());
      // create string array for the file content and language
      String[] fileTexts = new String[inputFiles.length];
      String[] languages = new String[inputFiles.length];
      int numChars = 0;
      long fileSize = 0;
      // iterate of all input files and extract content and language
      for (int i = 0; i < inputFiles.length; i++) {
        // get file language
        languages[i] = inputFiles[i].getName().substring(0, 2);
        // get file content
        fileTexts[i] = FileUtils.file2String(inputFiles[i], "UTF-8");
        fileSize += inputFiles[i].length();
        // count characters
        numChars += fileTexts[i].length();
      }

      // stop io timer
      ioTimer.stop();

      // save results
      result.setNumberOfFiles(inputFiles.length);
      result.setNumberOfCharacters(numChars);
      result.setTotalFileSize(fileSize);
      result.setIoTime(ioTimer.getTimeSpan());

      // start real processing
      int numAnnot = 0;

      // check repeat single mode setting
      // repeatSingle=true: iterates of all files and repeat each file "numsToRun" times
      // repeatSingle=false: iterates of all files and repeat the collection "numsToRun" times
      if (repeatSingle) {
        // iterate over all text files (over the cached content)
        for (int i = 0; i < fileTexts.length; i++) {
          // file repeat mode
          // iterate over the current document "numsToRun" times
          for (int j = 0; j < numsToRun; j++) {
            documentPreparationTimer.start();
            // set cas data
            cas.setDocumentLanguage(languages[i]);
            cas.setDocumentText(fileTexts[i]);
            documentPreparationTimer.stop();
            processResetTimer.start();
            ae.process(cas);
            processResetTimer.stop();
            documentPreparationTimer.start();
            numAnnot += cas.getAnnotationIndex().size();
            cas.reset();
            documentPreparationTimer.stop();
          }
        }
      }
      // use collection repeat mode
      else {
        // process the file collection "numsToRun" times
        for (int j = 0; j < numsToRun; j++) {
          // iterate over all text files (over the cached content)
          for (int i = 0; i < fileTexts.length; i++) {
            documentPreparationTimer.start();
            // set cas data
            cas.setDocumentLanguage(languages[i]);
            cas.setDocumentText(fileTexts[i]);
            documentPreparationTimer.stop();
            processResetTimer.start();
            ae.process(cas);
            processResetTimer.stop();
            documentPreparationTimer.start();
            numAnnot += cas.getAnnotationIndex().size();
            cas.reset();
            documentPreparationTimer.stop();
          }
        }
      }

      // cleanup ae and stop global timer
      cleanupTimer.start();
      ae.destroy();
      ae = null;
      cleanupTimer.stop();
      globalTimer.stop();

      // save results
      result.setNumberOfCreatedAnnotations(numAnnot);
      result.setOverallTime(globalTimer.getTimeSpan());
      result.setProcessingTime(processResetTimer.getTimeSpan());
      result.setCleanupTime(cleanupTimer.getTimeSpan());
      result.setDocumentPreparationTime(documentPreparationTimer.getTimeSpan());

      // turn on logging as it was before
      logger.setLevel(defaultLogLevel);

      // return result object
      return result;

    } catch (Exception e) { // Bail out.
      throw e;
    }

  }

}
