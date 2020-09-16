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

import static org.apache.uima.fit.util.CasUtil.selectAll;
import static org.apache.uima.fit.validation.ValidationResult.error;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.validation.CasValidationCheck;
import org.apache.uima.fit.validation.ValidationResult;

/**
 * Simple CAS validation check ensuring that annotations do not end before they start.
 * <p>
 * <b>Note:</b> This is used for testing in uimaFIT. It is not meant for general use!
 */
public class EndAfterBeginCheckForTesting implements CasValidationCheck {
  @Override
  public List<ValidationResult> validate(CAS cas) {
    List<ValidationResult> results = new ArrayList<>();

    for (AnnotationFS anno : selectAll(cas)) {
      if (anno.getEnd() < anno.getBegin()) {
        results.add(error(this, "%s ends (%d) before it begins (%d)", anno.getType().getShortName(),
                anno.getEnd(), anno.getBegin()));
      }
    }

    return results;
  }
}
