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
package org.apache.uima.jcas.impl;

import org.apache.uima.cas.impl.TypeSystemImpl;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.Sofa;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.resource.metadata.impl.TypeSystemDescription_impl;
import org.apache.uima.util.CasCreationUtils;
import org.junit.jupiter.api.Test;

public class JCasCoverClassLoadingTest {

  /**
   * This test failed when we were using {@link TypeSystemImpl#createCallSite(Class, String)}
   * instead of {@link TypeSystemImpl#createCallSiteForBuiltIn(Class, String)} to initialize
   * built-in JCas cover classes.
   * 
   * @see <a href="https://issues.apache.org/jira/browse/UIMA-6274">UIMA-6274</a>
   */
  @Test
  public void thatLoadingABuiltInCoverClassBeforeTypeSystemImplDoesNotBreakTheClass()
          throws Exception {
    Class.forName(Sofa.class.getName());

    TypeSystemDescription tsd = new TypeSystemDescription_impl();
    JCas jcas = CasCreationUtils.createCas(tsd, null, null).getJCas();
    // Originally an exception was thrown in the following line
    jcas.setDocumentText("test text");
  }
}
