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
import java.io.FileWriter;
import java.util.Calendar;
import java.util.GregorianCalendar;

import org.apache.uima.internal.util.TimeSpan;

/**
 * PerformanceTestResultImpl implements the PerformanceTestResult interface and provides the results of a performance
 * test run.
 * 
 */
public class PerformanceTestResultImpl implements PerformanceTestResult {

  private static String NEWLINE = System.getProperty("line.separator");

  private boolean repeatSingleMode = false;

  private boolean doWarmup = false;

  private int numsToRun = 1;

  private File aeDescFile = null;

  private File testFileDir = null;

  private int numberOfFiles = 0;

  private long collectionFileSize = 0;

  private int numberOfCharacters = 0;

  private String datapath = null;

  private int numberOfCreatedAnnotations = 0;

  private TimeSpan initTime = null;

  private TimeSpan warmupTime = null;

  private TimeSpan overallTime = null;

  private TimeSpan ioTime = null;

  private TimeSpan processingTime = null;

  private TimeSpan cleanupTime = null;

  private TimeSpan documentPreparationTime = null;

  /* (non-Javadoc)
   * @see org.apache.uima.test.junit_extension.PerformanceTestResult#getNumberOfProcessedCharacters()
   */
  public int getNumberOfProcessedCharacters() {
    return this.numberOfCharacters * this.numsToRun;
  }

  /**
   * @param numberOfCharacters
   *          The number of characters.
   */
  public void setNumberOfCharacters(int numberOfCharacters) {
    this.numberOfCharacters = numberOfCharacters;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.test.junit_extension.PerformanceTestResult#getNumberOfProcessedFiles()
   */
  public int getNumberOfProcessedFiles() {
    return this.numberOfFiles * this.numsToRun;
  }

  /**
   * @param numberOfFiles
   *          The number of files.
   */
  public void setNumberOfFiles(int numberOfFiles) {
    this.numberOfFiles = numberOfFiles;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.test.junit_extension.PerformanceTestResult#getUIMADatapath()
   */
  public String getUIMADatapath() {
    return this.datapath;
  }

  /**
   * @param datapath
   *          The UIMA datapath.
   */
  public void setDatapath(String datapath) {
    this.datapath = datapath;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.test.junit_extension.PerformanceTestResult#getAeInitTime()
   */
  public TimeSpan getAeInitTime() {
    return this.initTime;
  }

  /**
   * @param initTime
   *          The analysis engine init time.
   */
  public void setInitTime(TimeSpan initTime) {
    this.initTime = initTime;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.test.junit_extension.PerformanceTestResult#getFileIoTime()
   */
  public TimeSpan getFileIoTime() {
    return this.ioTime;
  }

  /**
   * @param ioTime
   *          The file io time to set.
   */
  public void setIoTime(TimeSpan ioTime) {
    this.ioTime = ioTime;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.test.junit_extension.PerformanceTestResult#getNumberOfCreatedAnnotations()
   */
  public int getNumberOfCreatedAnnotations() {
    return this.numberOfCreatedAnnotations;
  }

  /**
   * @param numberOfCreatedAnnotations
   *          The number of created annotations to set.
   */
  public void setNumberOfCreatedAnnotations(int numberOfCreatedAnnotations) {
    this.numberOfCreatedAnnotations = numberOfCreatedAnnotations;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.test.junit_extension.PerformanceTestResult#getNumberOfRepeatedRuns()
   */
  public int getNumberOfRepeatedRuns() {
    return this.numsToRun;
  }

  /**
   * @param numsToRun
   *          The number of repeated runs.
   */
  public void setNumsToRun(int numsToRun) {
    this.numsToRun = numsToRun;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.test.junit_extension.PerformanceTestResult#getOverallTime()
   */
  public TimeSpan getOverallTime() {
    return this.overallTime;
  }

  /**
   * @param overallTime
   *          The overall processing time.
   */
  public void setOverallTime(TimeSpan overallTime) {
    this.overallTime = overallTime;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.test.junit_extension.PerformanceTestResult#getAeProcessingTime()
   */
  public TimeSpan getAeProcessingTime() {
    return this.processingTime;
  }

  /**
   * @param processingTime
   *          The analysis engine processing time.
   */
  public void setProcessingTime(TimeSpan processingTime) {
    this.processingTime = processingTime;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.test.junit_extension.PerformanceTestResult#isRepeatSingleMode()
   */
  public boolean isRepeatSingleMode() {
    return this.repeatSingleMode;
  }

  /**
   * @param repeatSingleMode
   *          The repeat single mode setting
   */
  public void setRepeatSingleMode(boolean repeatSingleMode) {
    this.repeatSingleMode = repeatSingleMode;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.test.junit_extension.PerformanceTestResult#getAeDescFilePath()
   */
  public String getAeDescFilePath() {
    return this.aeDescFile.getAbsolutePath();
  }

  /**
   * @param aeDescFile
   *          The analysis engine descriptor file.
   */
  public void setAeDescFilePath(File aeDescFile) {
    this.aeDescFile = aeDescFile;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.test.junit_extension.PerformanceTestResult#getTestFileDirectoryPath()
   */
  public String getTestFileDirectoryPath() {
    return this.testFileDir.getAbsolutePath();
  }

  /**
   * @param testFileDir
   *          The test file directory.
   */
  public void setTestFileDir(File testFileDir) {
    this.testFileDir = testFileDir;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.test.junit_extension.PerformanceTestResult#getAeWarmupTime()
   */
  public TimeSpan getAeWarmupTime() {
    return this.warmupTime;
  }

  /**
   * @param warmupTime
   *          The analysis engine warmup time.
   */
  public void setWarmupTime(TimeSpan warmupTime) {
    this.warmupTime = warmupTime;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.test.junit_extension.PerformanceTestResult#isDoAeWarmup()
   */
  public boolean isDoAeWarmup() {
    return this.doWarmup;
  }

  /**
   * @param doWarmup
   *          the doWarmup setting
   */
  public void setDoWarmup(boolean doWarmup) {
    this.doWarmup = doWarmup;
  }

  /**
   * returns a performance report with the current performance results.
   * 
   * @see java.lang.Object#toString()
   */
  public String toString() {
    StringBuffer resultString = new StringBuffer();
    resultString.append(NEWLINE + "Performance Test run results " + NEWLINE);
    resultString.append("-----------------------------" + NEWLINE + NEWLINE);
    resultString.append("ConfigParameters: " + NEWLINE);
    resultString.append("repeat-single mode is: " + this.repeatSingleMode + NEWLINE);
    resultString.append("doWarump is: " + this.doWarmup + NEWLINE);
    resultString.append("Run test " + this.numsToRun + " times" + NEWLINE);
    resultString.append("Use " + this.aeDescFile.getAbsolutePath() + " descriptor" + NEWLINE);
    resultString.append("Fileset " + this.testFileDir + " is used" + NEWLINE);
    resultString.append("Datapath setting is: " + this.datapath + NEWLINE);
    resultString.append("Number of documents (overall): " + this.getNumberOfProcessedFiles() + NEWLINE);
    resultString
            .append("Number of characters (overall): " + this.getNumberOfProcessedCharacters() + NEWLINE);
    resultString.append("Total document size (overall): "
            + (float) (this.getProcessedFileSize() / 1024) + " KB" + NEWLINE);
    resultString.append(NEWLINE + "Initialize: " + NEWLINE);
    resultString.append("AE initialize time: " + this.initTime + NEWLINE);
    if (this.doWarmup) {
      resultString.append("AE warmup time: " + this.warmupTime + NEWLINE);
    }
    resultString.append("IO time for reading files: " + this.ioTime + NEWLINE);
    resultString.append(NEWLINE + "Processing: " + NEWLINE);
    resultString.append("Created annotations: " + this.numberOfCreatedAnnotations + NEWLINE);
    resultString.append("Document preparation time: " + this.documentPreparationTime + NEWLINE);
    resultString.append("Analysis Engine processing time: " + this.processingTime + NEWLINE);
    if (this.processingTime.getFullMilliseconds() > 0 && this.getProcessedFileSize() > 0) {
      resultString.append("Processing throughput: "
              + (long) ((this.getProcessedFileSize() / 1024) / ((float) this.processingTime
                      .getFullMilliseconds() / 1000)) + " KB/s" + NEWLINE);
      long mbPerSecond = (long) ((this.getProcessedFileSize() / 1024 / 1024) / ((float) this.processingTime
              .getFullMilliseconds() / 1000 / 60 / 60));
      resultString.append("Processing throughput: " + mbPerSecond + " MB/h" + NEWLINE);
    }

    resultString.append(NEWLINE + "Cleanup: " + NEWLINE);
    resultString.append("Cleanup time: " + this.cleanupTime + NEWLINE);

    resultString.append(NEWLINE + "Overall: " + NEWLINE);
    resultString.append("Overall time: " + this.overallTime + NEWLINE);

    return resultString.toString();
  }

  /**
   * write performance results as colum.
   * 
   * @param level
   *          Test level name of the performance test
   * @param file
   *          Output file where the results are written to
   * 
   * @throws Exception
   */
  public void writePerfResultsAsColumn(String level, File file) throws Exception {

    // set level name to "none" if it is not available
    if (level == null) {
      level = "none";
    }

    // create current time stamp
    GregorianCalendar cal = new GregorianCalendar();
    StringBuffer timestamp = new StringBuffer();
    timestamp.append(cal.get(Calendar.DATE));
    timestamp.append(".");
    timestamp.append(cal.get(Calendar.MONTH) + 1);
    timestamp.append(".");
    timestamp.append(cal.get(Calendar.YEAR));
    timestamp.append(" ");
    timestamp.append(cal.get(Calendar.HOUR_OF_DAY));
    timestamp.append(":");
    timestamp.append(cal.get(Calendar.MINUTE));
    timestamp.append(":");
    timestamp.append(cal.get(Calendar.SECOND));

    // create result line
    StringBuffer resultLine = new StringBuffer();
    resultLine.append(timestamp.toString()); // add timestamp
    resultLine.append(" ; ");
    resultLine.append(level); // add level name
    resultLine.append(" ; ");
    resultLine.append(this.repeatSingleMode); // repeate single mode
    resultLine.append(" ; ");
    resultLine.append(this.doWarmup); // doWarmup
    resultLine.append(" ; ");
    resultLine.append(this.numberOfFiles * this.numsToRun); // number of docs overall
    resultLine.append(" ; ");
    resultLine.append(this.numberOfCreatedAnnotations); // number of created annots
    resultLine.append(" ; ");
    resultLine.append(this.processingTime); // processing time
    resultLine.append(" ; ");
    resultLine.append(this.initTime); // init time
    resultLine.append(" ; ");
    resultLine.append(this.ioTime); // io time
    resultLine.append(System.getProperty("line.separator"));

    boolean writeHeader = false;
    // check if file does not exist, write file header
    if (!file.exists()) {
      writeHeader = true;
    }

    // create outputStream in appending mode
    FileWriter fileWriter = new FileWriter(file, true);

    if (writeHeader) {
      fileWriter
              .write("Timestamp ; Levelname ; singleMode ; warmup; Docs ; Annotations ; Processing time ; init time ; io time");
      fileWriter.write(System.getProperty("line.separator"));
    }

    // write result
    fileWriter.write(resultLine.toString());

    // flush and close writer
    fileWriter.flush();
    fileWriter.close();
  }

  /* (non-Javadoc)
   * @see org.apache.uima.test.junit_extension.PerformanceTestResult#getAeCleanupTime()
   */
  public TimeSpan getAeCleanupTime() {
    return this.cleanupTime;
  }

  /**
   * @param cleanupTime
   *          the analysis engine cleanup time
   */
  public void setCleanupTime(TimeSpan cleanupTime) {
    this.cleanupTime = cleanupTime;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.test.junit_extension.PerformanceTestResult#getDocumentPreparationTime()
   */
  public TimeSpan getDocumentPreparationTime() {
    return this.documentPreparationTime;
  }

  /**
   * @param documentPreparationTime
   *          the document preparation time
   */
  public void setDocumentPreparationTime(TimeSpan documentPreparationTime) {
    this.documentPreparationTime = documentPreparationTime;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.test.junit_extension.PerformanceTestResult#getProcessedFileSize()
   */
  public long getProcessedFileSize() {
    return this.collectionFileSize * this.numsToRun;
  }

  /**
   * @param collectionFileSize
   *          the collection file size
   */
  public void setTotalFileSize(long collectionFileSize) {
    this.collectionFileSize = collectionFileSize;
  }

}
