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

package org.apache.uima.analysis_engine.impl.compatibility;

import java.util.Map;

import org.apache.uima.analysis_component.AnalysisComponent;
import org.apache.uima.analysis_engine.annotator.BaseAnnotator;
import org.apache.uima.analysis_engine.metadata.AnalysisEngineMetaData;
import org.apache.uima.collection.CasConsumer;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.resource.ResourceInitializationException;

/**
 * Factory that builds {@link AnalysisComponent} instances from AnalysisEngineDescription,
 * CasConsumerDescription, or CollectionReaderDescription objects.
 */
public class AnalysisComponentAdapterFactory {
  /**
   * resource bundle for log messages
   */
  protected static final String LOG_RESOURCE_BUNDLE = "org.apache.uima.impl.log_messages";

  /**
   * Creates an adapter that allows the given object to implement the {@link AnalysisComponent}
   * interface. This is needed because UIMA has distinct interfaces that are implemented by
   * different types of components, e.g. Annotator, CAS Consumer, CollectionReader, but the UIMA
   * framework itself simplifies things by interacting with all of them through a single, common
   * AnalysisComponent interface.
   * 
   * @param aAdaptee
   *          Object to adapet to the AnalysisComponent interface
   * @param aMetaData
   *          metadata for the AnalysisEngine containing this component
   * @param aAdditionalParams
   *          parameters passed to AE's initialize method
   * 
   * @return an object that wraps <code>aAdaptee</code> and implements the
   *         <code>AnalysisComponent</code> interface.
   * @throws ResourceInitializationException if passed an adaptee which is not an analysis component
   */
  public static AnalysisComponent createAdapter(Object aAdaptee, AnalysisEngineMetaData aMetaData,
          Map<String, Object> aAdditionalParams) throws ResourceInitializationException {
    if (aAdaptee instanceof BaseAnnotator) {
      return new AnnotatorAdapter((BaseAnnotator) aAdaptee, aMetaData, aAdditionalParams);
    } else if (aAdaptee instanceof CasConsumer) {
      return new CasConsumerAdapter((CasConsumer) aAdaptee, aMetaData);
    } else if (aAdaptee instanceof CollectionReader) {
      return new CollectionReaderAdapter((CollectionReader) aAdaptee, aMetaData);
    } else {
      throw new ResourceInitializationException(
              ResourceInitializationException.NOT_AN_ANALYSIS_COMPONENT, new Object[] {
                  aAdaptee.getClass().getName(), aMetaData.getSourceUrlString() });
    }
  }

  /**
   * Determines whether this factory is capable of producing an adapter that adapts the given class
   * to the AnalysisComponent interface.
   * 
   * @param cls
   *          the adaptee class
   * @return true if this factory can adapt <code>cls</code> to <code>AnalysisComponent</code>.
   */
  public static boolean isAdaptable(Class<?> cls) {
    return BaseAnnotator.class.isAssignableFrom(cls) || CasConsumer.class.isAssignableFrom(cls)
            || CollectionReader.class.isAssignableFrom(cls);
  }

}
