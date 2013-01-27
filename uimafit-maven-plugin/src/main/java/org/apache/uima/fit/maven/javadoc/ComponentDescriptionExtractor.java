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
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.TypeDeclaration;

/**
 * Find JavaDoc for a given uimaFIT parameter.
 */
public class ComponentDescriptionExtractor extends ASTVisitor {
  private String className;

  private Javadoc javadoc;

  public ComponentDescriptionExtractor(String aClassName) {
    className = aClassName;
  }

  public Javadoc getJavadoc() {
    return javadoc;
  }

  @Override
  public boolean visit(TypeDeclaration aNode) {
    StringBuilder name = new StringBuilder();
    
    // rec 2013-01-27: This should work, but for some reason resolveBinding() returns null even
    // though binding resolving and binding recovery are both enabled in the parser.
    // name = aNode.resolveBinding().getQualifiedName();
    
    // Different approach to try and get the qualified name at least for the top-level class
    CompilationUnit root = (CompilationUnit) aNode.getRoot();
    if (root.getPackage() != null) {
      name.append(root.getPackage().getName());
      name.append('.');
    }
    name.append(aNode.getName().getIdentifier());
   
    if (name.toString().equals(className)) {
      javadoc = aNode.getJavadoc();
    }
    
    return true;
  }
}