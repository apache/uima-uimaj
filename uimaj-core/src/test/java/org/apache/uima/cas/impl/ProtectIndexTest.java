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
package org.apache.uima.cas.impl;

import static org.apache.uima.util.CasCreationUtils.createCas;
import static org.junit.Assert.assertEquals;

import org.apache.uima.cas.CASException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.junit.jupiter.api.Test;

public class ProtectIndexTest {

  @Test
  public void testProtectIndex() throws CASException, ResourceInitializationException {
    JCas jcas = createCas().getJCas();

    Annotation a = new Annotation(jcas, 0, 2);

    jcas.protectIndexes(() -> a.setBegin(a.getBegin() + 1));

    assertEquals(a.getBegin(), 1);
  }
}
