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
package org.apache.uima.json.jsoncas2.ser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASRuntimeException;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public abstract class CasDeserializer_ImplBase<T> extends StdDeserializer<T> {

  private static final long serialVersionUID = -1269025453230384321L;

  public static final String CONTEXT_CAS = "UIMA.CAS";
  public static final String CONTEXT_POST_PROCESSORS = "UIMA.PostProcessors";
  public static final String CONTEXT_DOCUMENT_ANNOTATION_READ_FLAG = "UIMA.DocumentAnnotatonRead";

  protected CasDeserializer_ImplBase(Class<T> aVc) {
    super(aVc);
  }

  protected CAS getCas(DeserializationContext aCtxt) {
    return (CAS) aCtxt.getAttribute(CONTEXT_CAS);
  }

  protected void schedulePostprocessing(DeserializationContext aCtxt, Runnable aAction) {
    List<Runnable> postProcessors = (List<Runnable>) aCtxt.getAttribute(CONTEXT_POST_PROCESSORS);
    if (postProcessors == null) {
      postProcessors = new ArrayList<>();
      aCtxt.setAttribute(CONTEXT_POST_PROCESSORS, postProcessors);
    }

    postProcessors.add(aAction);
  }

  protected void runPostprocessors(DeserializationContext aCtxt) {
    List<Runnable> postProcessors = (List<Runnable>) aCtxt.getAttribute(CONTEXT_POST_PROCESSORS);
    if (postProcessors != null) {
      postProcessors.forEach(Runnable::run);
    }
  }

  protected void markDocumentAnnotationCreated(DeserializationContext aCtxt, String aView) {
    documentAnnotationCreatedFlags(aCtxt).add(aView);
  }

  protected boolean isDocumentAnnotationCreated(DeserializationContext aCtxt, String aView) {
    return documentAnnotationCreatedFlags(aCtxt).contains(aView);
  }

  private Set<String> documentAnnotationCreatedFlags(DeserializationContext aCtxt) {
    Set<String> flags = (Set<String>) aCtxt.getAttribute(CONTEXT_DOCUMENT_ANNOTATION_READ_FLAG);
    if (flags == null) {
      flags = new HashSet<>();
      aCtxt.setAttribute(CONTEXT_DOCUMENT_ANNOTATION_READ_FLAG, flags);
    }
    return flags;
  }

  protected CAS createOrGetView(CAS aCas, String aViewName) {
    try {
      return aCas.getView(aViewName);
    } catch (CASRuntimeException e) {
      return aCas.createView(aViewName);
    }
  }
}
