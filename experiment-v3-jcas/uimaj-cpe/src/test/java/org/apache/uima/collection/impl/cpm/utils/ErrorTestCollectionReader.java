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

package org.apache.uima.collection.impl.cpm.utils;

import java.io.IOException;
import java.util.HashMap;

import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader_ImplBase;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;
import org.apache.uima.util.Logger;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;

/**
 * A simple collection reader emulates to read a given number of documents
 * 
 */
public class ErrorTestCollectionReader extends CollectionReader_ImplBase {

  /**
   * Name of configuration parameter that must be set to the path of a directory containing input
   * files.
   */
  public static final String PARAM_INPUTDIR = "InputDirectory";

  /**
   * Name of configuration parameter that contains the character encoding used by the input files.
   * If not specified, the default system encoding will be used.
   */
  public static final String PARAM_ENCODING = "Encoding";

  /**
   * Name of optional configuration parameter that contains the language of the documents in the
   * input directory. If specified this information will be added to the CAS.
   */
  public static final String PARAM_LANGUAGE = "Language";

  // Parameter fields in the xml
  private static final String DOCUMENT_COUNT = "DocumentCount";

  private static final String ERROR_EXCEPTION = "ErrorException";

  private static final String ERROR_COUNT = "ErrorCount";

  private static final String ERROR_FUNCTION = "ErrorFunction";

  private HashMap errorConfig;

  private String[] keyList;

  private final static String FUNC_GETPROCESS_KEY = "getProgress";

  private final static String FUNC_INITIALIZE_KEY = "initialize";

  private final static String FUNC_CLOSE_KEY = "close";

  private final static String FUNC_GETNEXT_KEY = "getNext";

  private final static String FUNC_HASNEXT_KEY = "hasNext";

  private int documentCount = 0;

  private int documentsCounted = 0;

  private Logger logger;

  private final static Level LOG_LEVEL = Level.FINE;

  public ErrorTestCollectionReader() {
    FunctionErrorStore.increaseCollectionReaderCount();
  }

  /**
   * @see org.apache.uima.collection.CollectionReader_ImplBase#initialize()
   */
  public void initialize() throws ResourceInitializationException {
    keyList = new String[] { FUNC_INITIALIZE_KEY, FUNC_GETPROCESS_KEY, FUNC_CLOSE_KEY,
        FUNC_GETNEXT_KEY, FUNC_HASNEXT_KEY };
    errorConfig = new HashMap();
    logger = getLogger();
    documentCount = (((Integer) getConfigParameterValue(DOCUMENT_COUNT)).intValue());
    documentsCounted = 0;
    String errorFunction = (String) getConfigParameterValue(ERROR_FUNCTION);
    Integer errorCount = (Integer) getConfigParameterValue(ERROR_COUNT);
    String errorException = (String) getConfigParameterValue(ERROR_EXCEPTION);
    if (errorFunction != null) {
      int errorCountName = 0;
      String errorExceptionName = "RuntimeException";
      if (errorCount != null) {
        errorCountName = errorCount.intValue();
      }
      if (errorException != null) {
        errorExceptionName = errorException;
      }
      System.out.println("adding error!");
      // add the error object to the corresponding HashMap Entry
      addError(errorFunction, new FunctionErrorStore(errorExceptionName, errorCountName,
              errorFunction));
    }

    logger.log(LOG_LEVEL, "initialize() was called");
    if (errorConfig.containsKey(FUNC_INITIALIZE_KEY)) {
      ((FunctionErrorStore) errorConfig.get(FUNC_INITIALIZE_KEY)).methodeCalled5();
    }
  }

  /**
   * @see org.apache.uima.application_library.DocumentReader#hasNext()
   */
  public boolean hasNext() {
    logger.log(LOG_LEVEL, "getNext(CAS) was called");
    if (errorConfig.containsKey(FUNC_HASNEXT_KEY)) {
      ((FunctionErrorStore) errorConfig.get(FUNC_HASNEXT_KEY)).methodeCalled8();
    }
    return documentCount > documentsCounted;
  }

  /**
   * @see org.apache.uima.collection.CollectionReader#getNext(org.apache.uima.cas.CAS)
   */
  public void getNext(CAS aCAS) throws IOException, CollectionException {
    documentsCounted++;
    FunctionErrorStore.increaseCollectionReaderGetNextCount();
    logger.log(LOG_LEVEL, "getNext(CAS) was called");
    if (errorConfig.containsKey(FUNC_GETNEXT_KEY)) {
      ((FunctionErrorStore) errorConfig.get(FUNC_GETNEXT_KEY)).methodeCalled6();
    }
  }

  /**
   * @see org.apache.uima.collection.base_cpm.BaseCollectionReader#close()
   */
  public void close() throws IOException {
    logger.log(LOG_LEVEL, "close() was called");
    if (errorConfig.containsKey(FUNC_CLOSE_KEY)) {
      ((FunctionErrorStore) errorConfig.get(FUNC_CLOSE_KEY)).methodeCalled7();
    }
  }

  /**
   * @see org.apache.uima.collection.base_cpm.BaseCollectionReader#getProgress()
   */
  public Progress[] getProgress() {
    logger.log(LOG_LEVEL, "getProgress() was called");
    if (errorConfig.containsKey(FUNC_GETPROCESS_KEY)) {
      ((FunctionErrorStore) errorConfig.get(FUNC_GETPROCESS_KEY)).methodeCalled8();
    }
    return new Progress[] { new ProgressImpl(documentsCounted, documentCount, Progress.ENTITIES) };
  }

  /**
   * Gets the total number of documents that will be returned by this collection reader. This is not
   * part of the general collection reader interface.
   * 
   * @return the number of documents in the collection
   */
  public int getNumberOfDocuments() {
    return documentCount;
  }

  /**
   * @param function
   *          name of the methode where the error should be added
   * @param obj
   *          the error object -
   * @see FunctionErrorStore
   */
  private void addError(String function, Object obj) {
    for (int i = 0; i < keyList.length; i++) {
      if (keyList[i].equals(function)) {
        errorConfig.put(keyList[i], obj);
        return;
      }
    }
  }
}
