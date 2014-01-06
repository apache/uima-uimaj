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
package org.apache.uima.impl;

import java.util.Collections;

import junit.framework.TestCase;

import org.apache.uima.collection.CasConsumer;
import org.apache.uima.collection.CasConsumerDescription;
import org.apache.uima.collection.impl.CasConsumerDescription_impl;
import org.apache.uima.resource.ResourceInitializationException;


public class CasConsumerFactory_implTest extends TestCase {
 
  private CasConsumerFactory_impl ccFactory;

  protected void setUp() throws Exception {
    ccFactory = new CasConsumerFactory_impl();
  }

  public void testInvalidFrameworkImplementation() {
    CasConsumerDescription desc = new CasConsumerDescription_impl();
    desc.setFrameworkImplementation("foo");    
    try {
      ccFactory.produceResource(CasConsumer.class, desc, Collections.EMPTY_MAP);
      fail();
    } catch (ResourceInitializationException e) {
      assertNotNull(e.getMessage());
      assertFalse(e.getMessage().startsWith("EXCEPTION MESSAGE LOCALIZATION FAILED"));
      assertEquals(e.getMessageKey(), ResourceInitializationException.UNSUPPORTED_FRAMEWORK_IMPLEMENTATION);
    }
  }

}
