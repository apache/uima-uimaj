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
import org.apache.uima.it.pear_with_typesystem.type.SimpleAnnotation;
import org.apache.uima.jcas.JCas;

/**
 * Annotator for CAS test cases. Does nothing. Its only purpose is to load a type system and provide
 * a CAS for testing.
 */
public class Scenario2TestAnnotator
    extends TestAnnotator_ImplBase
{
    private static final String TYPE_NAME_COMPLEX_ANNOTATION = "org.apache.uima.it.pear_with_typesystem.type.ComplexAnnotation";
    private static final String FEATURE_BASE_NAME_DELEGATE = "delegate";

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException
    {
        assertClassIsGlobal(TYPE_NAME_COMPLEX_ANNOTATION);
        assertClassIsLocal(SimpleAnnotation.class);

        var simpleAnnotation = new SimpleAnnotation(aJCas);
        simpleAnnotation.addToIndexes();

        var cas = aJCas.getCas();
        var ts = cas.getTypeSystem();
        var complexAnnotation = cas.createFS(ts.getType(TYPE_NAME_COMPLEX_ANNOTATION));
        getLogger().info("{} created as {} ({})", TYPE_NAME_COMPLEX_ANNOTATION,
                complexAnnotation.getClass(), complexAnnotation.getClass().getClassLoader());
        complexAnnotation.setFeatureValue(
                complexAnnotation.getType().getFeatureByBaseName(FEATURE_BASE_NAME_DELEGATE),
                simpleAnnotation);
        cas.addFsToIndexes(complexAnnotation);
    }
}
