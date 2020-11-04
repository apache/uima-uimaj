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

import static java.util.Arrays.asList;
import static org.apache.uima.UIMAFramework.getResourceSpecifierFactory;
import static org.apache.uima.util.CasCreationUtils.createCas;
import static org.assertj.core.api.Assertions.assertThat;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.metadata.TypePriorities;
import org.apache.uima.resource.metadata.TypePriorityList;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Includes several additional tests for {@code coverdBy} selection which require the CAS to be
 * prepared differently than in the {@link SelectFsTest}, e.g. with type priorities.
 */
public class SelectFsCoveredByWithTypePrioritiesTest {

    private static final String TYPE_NAME_SUBTYPE = "uima.test.selectfs.SubType";
    
    private static CAS cas;
    private static Type typeSubType;
    
    @BeforeClass
    public static void setupClass() throws Exception
    {
        TypeSystemDescription tsd = getResourceSpecifierFactory().createTypeSystemDescription();
        tsd.addType(TYPE_NAME_SUBTYPE, "", CAS.TYPE_NAME_ANNOTATION);
        
        TypePriorities prios = getResourceSpecifierFactory().createTypePriorities();
        TypePriorityList typePrioList = prios.addPriorityList();
        typePrioList.addType(TYPE_NAME_SUBTYPE);
        typePrioList.addType(CAS.TYPE_NAME_ANNOTATION);
        
        cas = createCas(tsd, prios, null, null, null);
        typeSubType = cas.getTypeSystem().getType(TYPE_NAME_SUBTYPE);
    }
    
    @Before
    public void setup()
    {
        cas.reset();
    }
    
    @Test
    public void thatCoveredByFindsTypeUsingSubtype() throws Exception {
       
        Annotation a1 = (Annotation) cas.createAnnotation(cas.getAnnotationType(), 5, 10);
        Annotation subType = (Annotation) cas.createAnnotation(typeSubType, 5, 10);
        
        asList(a1, subType)
            .forEach(a -> cas.addFsToIndexes(a));

        assertThat(cas.select(Annotation.class).coveredBy(subType).asList())
                .containsExactly(a1);
    }
    
    @Test
    public void thatCoveredByFindsTypeUsingUnindexedSubtype() throws Exception {
        Annotation a1 = (Annotation) cas.createAnnotation(cas.getAnnotationType(), 5, 10);
        Annotation subType = (Annotation) cas.createAnnotation(typeSubType, 5, 10);
        
        asList(a1)
            .forEach(a -> cas.addFsToIndexes(a));

        assertThat(cas.select(Annotation.class).coveredBy(subType).asList())
                .containsExactly(a1);
    }

    @Test
    public void thatCoveredByFindsSubtypeUsingType() throws Exception {
        Annotation a1 = (Annotation) cas.createAnnotation(cas.getAnnotationType(), 5, 10);
        Annotation subType = (Annotation) cas.createAnnotation(typeSubType, 5, 10);
        
        asList(a1, subType)
                .forEach(a -> cas.addFsToIndexes(a));

        assertThat(cas.select(Annotation.class).coveredBy(a1).asList())
                .containsExactly(subType);
    }

    @Test
    public void thatCoveredByWorksWithOffsets() throws Exception {
        Annotation a1 = (Annotation) cas.createAnnotation(cas.getAnnotationType(), 5, 10);
        
        asList(a1)
            .forEach(a -> cas.addFsToIndexes(a));

        assertThat(cas.select(Annotation.class).coveredBy(5, 10).asList())
                .containsExactly(a1);
    }
    
    @Test
    public void thatCoveredBySkipsIndexedAnchorAnnotation() throws Exception {
      JCas jCas = cas.getJCas();

      Annotation a1 = new Annotation(jCas, 5, 10);
      Annotation a2 = new Annotation(jCas, 5, 15);
      Annotation a3 = new Annotation(jCas, 0, 10);
      Annotation a4 = new Annotation(jCas, 0, 15);
      Annotation a5 = new Annotation(jCas, 5, 7);
      Annotation a6 = new Annotation(jCas, 8, 10);
      Annotation a7 = new Annotation(jCas, 6, 9);
      Annotation a8 = new Annotation(jCas, 5, 10);
      asList(a1, a2, a3, a4, a5, a6, a7, a8).forEach(Annotation::addToIndexes);

      assertThat(jCas.select(Annotation.class).coveredBy(a1).asList())
          .containsExactly(a8, a5, a7, a6);

      Annotation subType = (Annotation) cas.createAnnotation(typeSubType, 5, 10);
      subType.addToIndexes();

      assertThat(jCas.select(Annotation.class).coveredBy(subType).asList())
          .containsExactly(a1, a8, a5, a7, a6);
    }
}

