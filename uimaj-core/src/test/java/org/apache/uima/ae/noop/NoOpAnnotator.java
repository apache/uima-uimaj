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

package org.apache.uima.ae.noop;

import java.io.FileNotFoundException;

import org.apache.uima.UIMAFramework;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.CasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;
import org.apache.uima.util.Logger;

public class NoOpAnnotator extends CasAnnotator_ImplBase {
  private long counter = 0;

  private long countDown = 0;

  int errorFrequency = 0;

  int processDelay = 0;

  int finalCount = 0;

  int cpcDelay = 0;
  
  public void initialize(UimaContext aContext) throws ResourceInitializationException {
    super.initialize(aContext);
    
    if (getContext().getConfigParameterValue("FailDuringInitialization") != null) {
      throw new ResourceInitializationException(new FileNotFoundException("Simulated Exception"));
    }

    if (getContext().getConfigParameterValue("ErrorFrequency") != null) {
      errorFrequency = ((Integer) getContext().getConfigParameterValue("ErrorFrequency"))
              .intValue();
      countDown = errorFrequency;
    }

    if (getContext().getConfigParameterValue("CpCDelay") != null) {
      cpcDelay = ((Integer) getContext().getConfigParameterValue("CpCDelay")).intValue();
      System.out.println("NoOpAnnotator.initialize() Initializing With CpC Delay of " + cpcDelay
              + " millis");
    }
    if (getContext().getConfigParameterValue("ProcessDelay") != null) {
      processDelay = ((Integer) getContext().getConfigParameterValue("ProcessDelay")).intValue();
      System.out.println("NoOpAnnotator.initialize() Initializing With Process Delay of "
              + processDelay + " millis");

    }
    if (getContext().getConfigParameterValue("FinalCount") != null) {
      finalCount = ((Integer) getContext().getConfigParameterValue("FinalCount")).intValue();
    }

    // write log messages
    Logger logger = getContext().getLogger();
    logger.log(Level.CONFIG, "NAnnotator initialized");
  }

  public void typeSystemInit(TypeSystem aTypeSystem) throws AnalysisEngineProcessException {
  }

  public void collectionProcessComplete() throws AnalysisEngineProcessException {
    System.out
            .println("NoOpAnnotator.collectionProcessComplete() Called -------------------------------------");

    if (cpcDelay > 0) {
      try {
        System.out.println("NoOpAnnotator.collectionProcessComplete() Delaying CpC Reply For "
                + cpcDelay + " millis");
        synchronized (this) {
          this.wait(cpcDelay);
        }
      } catch (InterruptedException e) {

      }
    }
    if (finalCount > 0 && finalCount != counter) {
      String msg = "NoOpAnnotator expected " + finalCount + " CASes but was given " + counter;
      System.out.println(msg);
      throw new AnalysisEngineProcessException(new Exception(msg));
    }
    counter = 0;
  }

  public void process(CAS aCAS) throws AnalysisEngineProcessException {
    ++counter;
    try {
      if (processDelay == 0) {
        if (UIMAFramework.getLogger().isLoggable(Level.FINE))
          System.out.println("NoOpAnnotator.process() called for the " + counter
                  + "th time. Hashcode:" + hashCode());
      } else {
        // if ( UIMAFramework.getLogger().isLoggable(Level.FINE))
        System.out.println("NoOpAnnotator.process() called for the " + counter
                + "th time, delaying Response For:" + processDelay + " millis");
        synchronized (this) {
          try {
            wait(processDelay);
          } catch (InterruptedException e) {
          }
        }
      }

      // If creating exceptions, reduce interval between them each time.
      if (errorFrequency > 0) {
        if (--countDown == 0) {
          System.out.println("Generating OutOfBoundsException on " + counter + "th call.");
          if (errorFrequency > 1) {
            --errorFrequency;
          }
          countDown = errorFrequency;
          throw new IndexOutOfBoundsException();
        }
      }
    } catch (Exception e) {
      throw new AnalysisEngineProcessException(e);
    }
  }

}
