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
package org.apache.uima.fit.maven.javadoc;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

/**
 * Find JavaDoc for a given uimaFIT parameter. Javadoc on a parameter name constant is preferred
 * over JavaDoc on the parameter field itself.
 */
public class ParameterDescriptionExtractor extends ASTVisitor {
  private String parameter;
  
  private String parameterNameConstant;

  private Javadoc nameConstantJavadoc;

  private Javadoc parameterJavadoc;

  /**
   * New extractor.
   * 
   * @param aParameter name of the parameter field.
   * @param aParameterNameConstant name of the parameter name constant field.
   */
  public ParameterDescriptionExtractor(String aParameter, String aParameterNameConstant) {
    parameter = aParameter;
    parameterNameConstant = aParameterNameConstant;
  }

  public Javadoc getJavadoc() {
    if (parameterNameConstant != null && nameConstantJavadoc != null) {
      return nameConstantJavadoc;
    }
    else {
      return parameterJavadoc;
    }
  }
  
  public Javadoc getParameterJavadoc() {
    return parameterJavadoc;
  }
  
  public Javadoc getNameConstantJavadoc() {
    return nameConstantJavadoc;
  }

  @Override
  public boolean visit(FieldDeclaration aNode) {
    if ((!aNode.fragments().isEmpty())
            && (aNode.fragments().get(0) instanceof VariableDeclarationFragment)) {
      VariableDeclarationFragment f = (VariableDeclarationFragment) aNode.fragments().get(0);

      String fieldName = f.getName().getIdentifier();
      
      // CASE 1: JavaDoc is located on parameter name constant
      if (parameterNameConstant != null && parameterNameConstant.equals(fieldName)) {
        nameConstantJavadoc = aNode.getJavadoc();
      }
      
      // CASE 2: JavaDoc is located on the parameter field itself
      if (parameter.equals(fieldName)) {
        parameterJavadoc = aNode.getJavadoc();
      }
    }
    return false;
  }
}