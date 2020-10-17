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

import org.apache.uima.internal.util.TimeSpan;


/**
 * PerformanceTestResult interfance contains all the methods to access the performance test results.
 * 
 */
public interface PerformanceTestResult {

   /**
    * Gets the number of processed characters.
    *
    * @return Returns the number of processed characters.
    */
  public int getNumberOfProcessedCharacters();
  
  /**
   * Gets the number of processed files.
   *
   * @return Returns the number of processed files.
   */
  public int getNumberOfProcessedFiles();

  /**
   * Gets the UIMA datapath.
   *
   * @return Returns the UIMA datapath setting used for the performance test.
   */
  public String getUIMADatapath();
   
  /**
   * Gets the ae init time.
   *
   * @return Returns the analysis engine initialization time.
   */
  public TimeSpan getAeInitTime();

  /**
   * Gets the file io time.
   *
   * @return Returns the file I/O time.
   */
  public TimeSpan getFileIoTime();

  /**
   * Gets the number of created annotations.
   *
   * @return Returns the number of created annotations.
   */
  public int getNumberOfCreatedAnnotations();

  /**
   * Gets the number of repeated runs.
   *
   * @return Returns the setting for the number of repeated runs.
   */
  public int getNumberOfRepeatedRuns();

  /**
   * Gets the overall time.
   *
   * @return Returns the overall time of the performance run
   */
  public TimeSpan getOverallTime();

  /**
   * Gets the ae processing time.
   *
   * @return Returns the analysis engine processing time.
   */
  public TimeSpan getAeProcessingTime();

  /**
   * Checks if is repeat single mode.
   *
   * @return Returns the setting of the repeat mode mode.
   */
  public boolean isRepeatSingleMode();

  /**
   * Gets the ae desc file path.
   *
   * @return Returns the analysis engine descriptor file path.
   */
  public String getAeDescFilePath();

  /**
   * Gets the test file directory path.
   *
   * @return Returns the test file directory used for the performance run.
   */
  public String getTestFileDirectoryPath();

  /**
   * Gets the ae warmup time.
   *
   * @return Returns the analysis engine warmup time.
   */
  public TimeSpan getAeWarmupTime();

  /**
   * Checks if is do ae warmup.
   *
   * @return Returns the setting the the analysis engine warmup.
   */
  public boolean isDoAeWarmup();

  /**
   * write performance results as colum.
   * 
   * @param level
   *          Test level name of the performance test
   * @param file
   *          Output file where the results are written to
   * 
   * @throws Exception passthru
   */
  public void writePerfResultsAsColumn(String level, File file) throws Exception;
  
  /**
   * Gets the ae cleanup time.
   *
   * @return Returns the analysis engine cleanup time.
   */
  public TimeSpan getAeCleanupTime();

  /**
   * Gets the document preparation time.
   *
   * @return Returns the document preparation time.
   */
  public TimeSpan getDocumentPreparationTime();

  /**
   * Gets the processed file size.
   *
   * @return Returns the processed file collection size.
   */
  public long getProcessedFileSize();
}
