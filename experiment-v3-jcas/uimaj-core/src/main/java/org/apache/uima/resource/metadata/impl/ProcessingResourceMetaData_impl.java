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

package org.apache.uima.resource.metadata.impl;

import org.apache.uima.analysis_engine.metadata.impl.AnalysisEngineMetaData_impl;
import org.apache.uima.resource.metadata.ProcessingResourceMetaData;

/**
 * Reference implementation of {@link ProcessingResourceMetaData}. This class only exists for
 * historical reasons. It inherits almost everything from AnalysisEngineMetaData_impl. The only
 * reason we need this class is because its XML tag name needs to be
 * &lt;processingResourceMetaData&gt;, not
 * 
 * &lt;analysisEngienMetaData&gt;, for compatibility with existing descriptors.
 */
public class ProcessingResourceMetaData_impl extends AnalysisEngineMetaData_impl implements
        ProcessingResourceMetaData {

  static final long serialVersionUID = -4839907155580879702L;

  protected XmlizationInfo getXmlizationInfo() {
    return XMLIZATION_INFO;
  }

  static final private XmlizationInfo XMLIZATION_INFO = new XmlizationInfo(
          "processingResourceMetaData", null); // properties assigned below

  static {
    // this class's Xmlization info is derived from that of its superclass
    XmlizationInfo superclassInfo = AnalysisEngineMetaData_impl.getXmlizationInfoForClass();

    XMLIZATION_INFO.propertyInfo = new PropertyXmlInfo[superclassInfo.propertyInfo.length];
    System.arraycopy(superclassInfo.propertyInfo, 0, XMLIZATION_INFO.propertyInfo, 0,
            superclassInfo.propertyInfo.length);
  }
}
