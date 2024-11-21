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

package org.apache.uima.taeconfigurator.editors.xml;

import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.MultiLineRule;
import org.eclipse.jface.text.rules.RuleBasedPartitionScanner;
import org.eclipse.jface.text.rules.Token;

/**
 * The Class XMLPartitionScanner.
 */
public class XMLPartitionScanner extends RuleBasedPartitionScanner {

  /** The Constant XML_DEFAULT. */
  public static final String XML_DEFAULT = "__xml_default";

  /** The Constant XML_COMMENT. */
  public static final String XML_COMMENT = "__xml_comment";

  /** The Constant XML_TAG. */
  public static final String XML_TAG = "__xml_tag";

  /**
   * Instantiates a new XML partition scanner.
   */
  public XMLPartitionScanner() {

    IToken xmlComment = new Token(XML_COMMENT);
    IToken tag = new Token(XML_TAG);

    IPredicateRule[] rules = new IPredicateRule[2];

    rules[0] = new MultiLineRule("<!--", "-->", xmlComment);
    rules[1] = new TagRule(tag);

    setPredicateRules(rules);
  }
}
