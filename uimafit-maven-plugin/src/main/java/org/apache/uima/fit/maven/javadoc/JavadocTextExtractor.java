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
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.TextElement;

/**
 * Extract text from a JavaDoc comment.
 */
public class JavadocTextExtractor extends ASTVisitor {
  private StringBuilder text = new StringBuilder();

  @Override
  public boolean visit(TextElement aNode) {
    // In multi-line JavaDoc, each line is parsed as a separate node. We have to insert something
    // (a line break or space) to prevent gluing words together.
    if (text.length() > 0 && text.charAt(text.length()-1) != ' ') {
      text.append(' ');
    }
    text.append(aNode.getText());
    return true;
  }

  @Override
  public boolean visit(Javadoc aNode) {
    return true;
  }
  
  public String getText() {
    return text.toString();
  }
}