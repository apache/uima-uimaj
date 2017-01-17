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

package org.apache.uima.examples.cpm.sofa;

import org.junit.Assert;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.collection.CasConsumer_ImplBase;
import org.apache.uima.resource.ResourceProcessException;

/**
 * Sofa-unaware CAS Consumer for Sofa test cases. Expects German CAS view to be passed to process
 * method
 */
public class SofaUnawareCasConsumer extends CasConsumer_ImplBase {

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.collection.base_cpm.CasObjectProcessor#processCas(org.apache.uima.cas.CAS)
   */
  public void processCas(CAS cas) throws ResourceProcessException {
    Assert.assertNotNull(cas);
    Assert.assertEquals("das bier ist gut", cas.getDocumentText());
    Type cross = cas.getTypeSystem().getType("sofa.test.CrossAnnotation");
    Feature other = cross.getFeatureByBaseName("otherAnnotation");
    FSIndex anIndex = cas.getAnnotationIndex(cross);
    FSIterator anIter = anIndex.iterator();
    AnnotationFS annot = (AnnotationFS) anIter.next();
    Assert.assertEquals("das", annot.getCoveredText());
    AnnotationFS crossAnnot = (AnnotationFS) annot.getFeatureValue(other);
    Assert.assertEquals("this", crossAnnot.getCoveredText());
    annot = (AnnotationFS) anIter.next();
    Assert.assertEquals("bier", annot.getCoveredText());
    crossAnnot = (AnnotationFS) annot.getFeatureValue(other);
    Assert.assertEquals("beer", crossAnnot.getCoveredText());
    annot = (AnnotationFS) anIter.next();
    Assert.assertEquals("ist", annot.getCoveredText());
    crossAnnot = (AnnotationFS) annot.getFeatureValue(other);
    Assert.assertEquals("is", crossAnnot.getCoveredText());
    annot = (AnnotationFS) anIter.next();
    Assert.assertEquals("gut", annot.getCoveredText());
    crossAnnot = (AnnotationFS) annot.getFeatureValue(other);
    Assert.assertEquals("good", crossAnnot.getCoveredText());
    Assert.assertFalse(anIter.hasNext());
  }

}
