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
package org.apache.uima.fit.validation.checks;

import static org.apache.uima.fit.util.JCasUtil.select;
import static org.apache.uima.fit.validation.ValidationResult.error;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.fit.validation.JCasValidationCheck;
import org.apache.uima.fit.validation.ValidationResult;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

/**
 * Simple JCas validation check ensuring that annotations do not have the same start/end position.
 * <p>
 * <b>Note:</b> This is used for testing in uimaFIT. It is not meant for general use!
 */
public class EndSameAsBeginCheckForTesting implements JCasValidationCheck {
  @Override
  public List<ValidationResult> validate(JCas aJCas) {
    List<ValidationResult> results = new ArrayList<>();

    for (Annotation anno : select(aJCas, Annotation.class)) {
      if (anno.getEnd() == anno.getBegin()) {
        results.add(error(this, "%s starts and ends at the same position (%d)",
                anno.getType().getShortName(), anno.getBegin()));
      }
    }

    return results;
  }
}