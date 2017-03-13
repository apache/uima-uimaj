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

import java.util.HashMap;

import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CasConsumer_ImplBase;

import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.util.Level;
import org.apache.uima.util.Logger;


public class ErrorTestCasConsumer extends CasConsumer_ImplBase {

  // Parameter fields in the xml
  private static final String ERROR_EXCEPTION = "ErrorException";

  private static final String ERROR_COUNT = "ErrorCount";

  private static final String ERROR_FUNCTION = "ErrorFunction";

  private HashMap errorConfig;

  private String[] keyList;

  private final static String FUNC_PROCESSCAS_KEY = "processCas";

  private final static String FUNC_INITIALIZE_KEY = "initialize";

  private final static String FUNC_RECONFIGURE_KEY = "reconfigure";

  private Logger logger;

  private final static Level LOG_LEVEL = Level.FINE;

  public ErrorTestCasConsumer() {
    FunctionErrorStore.increaseCasConsumerCount();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.analysis_engine.annotator.BaseAnnotator#initialize(org.apache.uima.analysis_engine.annotator.AnnotatorContext)
   */
  public void initialize() throws ResourceInitializationException {
    keyList = new String[] { FUNC_INITIALIZE_KEY, FUNC_PROCESSCAS_KEY, FUNC_RECONFIGURE_KEY };
    errorConfig = new HashMap();
    logger = getLogger();
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
      // add the error object to the corresponding HashMap Entry
      addError(errorFunction, new FunctionErrorStore(errorExceptionName, errorCountName,
              errorFunction));
    }
    logger.log(LOG_LEVEL, "initialize() was called");
    if (errorConfig.containsKey(FUNC_INITIALIZE_KEY)) {
      ((FunctionErrorStore) errorConfig.get(FUNC_INITIALIZE_KEY)).methodeCalled4();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.base_cpm.CasObjectProcessor#processCas(org.apache.uima.cas.CAS)
   */
  public void processCas(CAS aCAS) throws ResourceProcessException {
    logger.log(LOG_LEVEL, "processCas() was called");
    FunctionErrorStore.increaseCasConsumerProcessCount();
    if (errorConfig.containsKey(FUNC_PROCESSCAS_KEY)) {
      ((FunctionErrorStore) errorConfig.get(FUNC_PROCESSCAS_KEY)).methodeCalled3();
    }
  }

  /*
   * helper functions
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
