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

package org.apache.uima.impl.collection.cpm;

import java.util.HashMap;

import org.apache.uima.analysis_engine.ResultSpecification;
import org.apache.uima.analysis_engine.annotator.AnnotatorConfigurationException;
import org.apache.uima.analysis_engine.annotator.AnnotatorContext;
import org.apache.uima.analysis_engine.annotator.AnnotatorContextException;
import org.apache.uima.analysis_engine.annotator.AnnotatorInitializationException;
import org.apache.uima.analysis_engine.annotator.AnnotatorProcessException;
import org.apache.uima.analysis_engine.annotator.JTextAnnotator_ImplBase;

import org.apache.uima.jcas.impl.JCas;
import org.apache.uima.util.Level;
import org.apache.uima.util.Logger;

/**
 *
 */
public class ErrorTestAnnotator extends JTextAnnotator_ImplBase {

	
	// Parameter fields in the xml
	private final String TEST_THIS_ANNOTATOR = "TestAnnotator";
	private final String EXCEPTION = "Exception";
	private final String ERROR_COUNT = "ErrorCount";
	
	private HashMap errorConfig;
	private String[] keyList;
	private final static String FUNC_PROCESS_KEY = "process";
	private final static String FUNC_INITIALIZE_KEY = "initialize";
	private final static String FUNC_RECONFIGURE_KEY = "reconfigure";
	
	private Logger logger;
	private final static Level LOG_LEVEL = Level.SEVERE;
	private boolean aTestAnnotator = false;
	
   /* (non-Javadoc)
    * @see org.apache.uima.analysis_engine.annotator.JTextAnnotator#process(org.apache.uima.jcas.impl.JCas, org.apache.uima.analysis_engine.ResultSpecification)
    */
   public void process(JCas aJCas, ResultSpecification aResultSpec) throws AnnotatorProcessException {
      // count the calls...
		logger.log(LOG_LEVEL, "process was called");
      if(errorConfig.containsKey(FUNC_PROCESS_KEY)){
			((FunctionErrorStore)errorConfig.get(FUNC_PROCESS_KEY)).methodeCalled1();
      }
   }
   

   /* (non-Javadoc)
    * @see org.apache.uima.analysis_engine.annotator.BaseAnnotator#initialize(org.apache.uima.analysis_engine.annotator.AnnotatorContext)
    */
   public void initialize(AnnotatorContext aContext)
      throws AnnotatorInitializationException, AnnotatorConfigurationException {
      super.initialize(aContext);		
		try {
			// set logger
			logger = getContext().getLogger();
			logger.log(LOG_LEVEL, "initialize was called");
			
			// initialize some attributs
			errorConfig = new HashMap();
			keyList = new String[3];
			
			// all crashable functions
			keyList[0] = FUNC_PROCESS_KEY;
			keyList[1] = FUNC_INITIALIZE_KEY;
			keyList[2] = FUNC_RECONFIGURE_KEY;
			
			// check the config what to do
         this.aTestAnnotator = safeGetConfigParameterValue(aContext, TEST_THIS_ANNOTATOR, true);
			if(this.aTestAnnotator == true){
				String[] aGroups = aContext.getConfigurationGroupNames();
				// walk through all configured (error)groups - ignore all appearing errors
				for (int i = 0; i < aGroups.length; i++){
					String functionName = aGroups[i];
					try{
						String exceptionName = (String) aContext.getConfigParameterValue(aGroups[i], EXCEPTION);
						int errorCount = ((Integer)aContext.getConfigParameterValue(aGroups[i], ERROR_COUNT)).intValue();
						FunctionErrorStore fes = new FunctionErrorStore(exceptionName, errorCount, functionName);
						// add the error object to the corresponding HashMap Entry
						addError(functionName, fes);
					}catch(NullPointerException e){
						//e.printStackTrace();
					}
				}
			}
      } catch (AnnotatorContextException e) {
         e.printStackTrace();
      } 
		if(errorConfig.containsKey(FUNC_INITIALIZE_KEY)){
			((FunctionErrorStore)errorConfig.get(FUNC_INITIALIZE_KEY)).methodeCalled2();
		}
   }

   /* (non-Javadoc)
    * @see org.apache.uima.analysis_engine.annotator.BaseAnnotator#reconfigure()
    */
   public void reconfigure()
      throws AnnotatorConfigurationException, AnnotatorInitializationException {
      super.reconfigure();
		logger.log(LOG_LEVEL, "reconfigure was called");
		if(errorConfig.containsKey(FUNC_RECONFIGURE_KEY)){
			((FunctionErrorStore)errorConfig.get(FUNC_RECONFIGURE_KEY)).methodeCalled2();
		}
   }

   
   /*
    * helper functions
    */
	private static boolean safeGetConfigParameterValue(AnnotatorContext context, String param, boolean defaultValue) throws AnnotatorContextException
	{
		Boolean v = (Boolean) context.getConfigParameterValue(param);
		if (v != null)
		{
			return v.booleanValue();
		}
		return defaultValue;
	}
	
	private void addError(String function, Object obj){
		for(int i=0; i<keyList.length; i++){
			if(keyList[i].equals(function)){
				errorConfig.put(keyList[i], obj);
				return;
			}
		}
	}
	
	class FunctionErrorStore {

		private String functionError = "Exception";
		private String functionName = "";
		private int functionCounter = 0;
		private int functionCounted = 0;
	
		public FunctionErrorStore(String exception, int functionCount, String functionName){
			functionCounter = functionCount;
			functionError = exception;
			this.functionName = functionName;
		}
		//	exceptions from JTextAnnotator_ImplBase.process
		public void methodeCalled1()throws AnnotatorProcessException {
			functionCounted++;

			if(functionCounted >= functionCounter){
				logger.log(LOG_LEVEL, "Die Function " + functionName + " versucht folgende Ausnahme zu werfen: " + functionError);
				if(functionError.equals("AnnotatorProcessException")){
					throw new AnnotatorProcessException();
				}else {
					throwAnException(functionError);
				}
			}
		}
		// exceptions from JTextAnnotator_ImplBase.initialize and JTextAnnotator_ImplBase.reconfigure
		public void methodeCalled2()throws AnnotatorConfigurationException, AnnotatorInitializationException {
			functionCounted++;
		
			if(functionCounted >= functionCounter){
				logger.log(LOG_LEVEL, "Die Function " + functionName + " versucht folgende Ausnahme zu werfen: " + functionError);
				if(functionError.equals("AnnotatorConfigurationException")){
					throw new AnnotatorConfigurationException();
				}else if(functionError.equals("AnnotatorInitializationException")){
					throw new AnnotatorInitializationException();
				}else {
					throwAnException(functionError);
				}      	
			}
		}
		// runtime exceptions
		private void throwAnException(String exception){
				if(exception.equals("IndexOutOfBoundsException")){
					throw new IndexOutOfBoundsException();
				}else if(exception.equals("OutOfMemoryError")){
					throw new OutOfMemoryError();
				}else if(exception.equals("NullPointerException")){
					throw new NullPointerException();
				}else if(exception.equals("RuntimeException")){
					throw new RuntimeException();
				}
		}
	}
}
