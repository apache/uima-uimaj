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
package org.apache.uima.internal.util;

import java.util.Collections;
import java.util.List;

/**
 * Data structure used to encapsulate an XML element name (including Namespace URI, 
 * local name, and the qname) as well as its attributes and character content.
 */
public class XmlElementNameAndContents {
  public XmlElementNameAndContents(XmlElementName name, String contents) {
    this(name, contents, Collections.<XmlAttribute>emptyList());
  }

  public XmlElementNameAndContents(XmlElementName name, String contents, List<XmlAttribute> attributes) {
    this.name = name;
    this.contents = contents;
    this.attributes = attributes;
  }

  public XmlElementName name;
  
  /**
   * List of XmlAttribute objects each holding name and value of an attribute.
   */
  public List<XmlAttribute> attributes;

  public String contents;
}