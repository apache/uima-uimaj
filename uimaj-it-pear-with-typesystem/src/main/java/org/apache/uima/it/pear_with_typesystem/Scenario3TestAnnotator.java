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

package org.apache.uima.it.pear_with_typesystem;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.it.pear_with_typesystem.type.ComplexAnnotation;
import org.apache.uima.it.pear_with_typesystem.type.SimpleAnnotation;
import org.apache.uima.jcas.JCas;

/**
 * Annotator for CAS test cases. Does nothing. Its only purpose is to load a type system and provide
 * a CAS for testing.
 */
public class Scenario3TestAnnotator
    extends TestAnnotator_ImplBase
{
    private static final String TYPE_NAME_COMPLEX_ANNOTATION_SUBTYPE = "org.apache.uima.it.pear_with_typesystem.type.ComplexAnnotationSubtype";

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException
    {
        assertClassIsLocal(SimpleAnnotation.class);
        assertClassIsLocal(ComplexAnnotation.class);

        // The unit test should have prepared the CAS with one of these
        assertFalse(aJCas.select(TYPE_NAME_COMPLEX_ANNOTATION_SUBTYPE).isEmpty());

        // Iterating over ComplexAnnotation instances also returns the ComplexAnnotationSubtype.
        // The PEAR does not have a JCas wrapper for ComplexAnnotationSubtype, so the framework
        // wraps it with the nearest PEAR-loaded ancestor wrapper (ComplexAnnotation). This means
        // the iteration must succeed without a ClassCastException and the returned FS must be an
        // instance of the PEAR-local ComplexAnnotation class even though its UIMA type is the
        // sub-type. See https://github.com/apache/uima-uimaj/issues/384.
        var complexAnnotation = aJCas.select(ComplexAnnotation.class).get();
        assertTrue(ComplexAnnotation.class.isInstance(complexAnnotation));
        assertTrue(TYPE_NAME_COMPLEX_ANNOTATION_SUBTYPE.equals(complexAnnotation.getType().getName()));
    }
}
