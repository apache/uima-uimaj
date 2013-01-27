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
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

/**
 * Find JavaDoc for a given uimaFIT parameter.
 */
public class ComponentDescriptionExtractor extends ASTVisitor {
  private String parameterName;

  private Javadoc javadoc;

  public ComponentDescriptionExtractor(String aParameterName) {
    parameterName = aParameterName;
  }

  public Javadoc getJavadoc() {
    return javadoc;
  }

  @Override
  public boolean visit(FieldDeclaration aNode) {
    if ((!aNode.fragments().isEmpty())
            && (aNode.fragments().get(0) instanceof VariableDeclarationFragment)) {
      VariableDeclarationFragment f = (VariableDeclarationFragment) aNode.fragments().get(0);
      if (f.getName().getIdentifier().startsWith("PARAM_")
              && (f.getInitializer() instanceof StringLiteral)) {
        String name = f.getName().getIdentifier();
        String value = ((StringLiteral) f.getInitializer()).getLiteralValue();
        if (parameterName.equals(value)) {
          javadoc = aNode.getJavadoc();
        }
      }
    }
    return false;
  }
}