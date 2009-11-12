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

package org.apache.uima.cas.impl;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;

/**
 * Default implementation to compare two annotations.
 * 
 * 
 * @version $Revision: 1.1 $
 */
public class DefaultFSAnnotationComparator implements FSComparator {

  private final DefaultAnnotationComparator internalComparator;

  public DefaultFSAnnotationComparator(CAS cas) {
    super();
    this.internalComparator = new DefaultAnnotationComparator((CASImpl) cas);
  }

  /**
   * Compare two annotations. First compare by start position, where smaller start position means
   * smaller annotation. If start positions are equal, compare by end position, where larger end
   * position is smaller. Finally, compare by type code, arbitrarily. Does not compare feature
   * values.
   * 
   * @param fs1
   *          FS1.
   * @param fs2
   *          FS2.
   * @return <code>-1</code>, if FS1 is "smaller" than FS2; <code>1</code>, if FS2 is smaller
   *         than FS1; and <code>0</code>, if FS1 equals FS2.
   */
  public int compare(FeatureStructure fs1, FeatureStructure fs2) {
    final int addr1 = ((FeatureStructureImpl) fs1).getAddress();
    final int addr2 = ((FeatureStructureImpl) fs2).getAddress();
    return this.internalComparator.compare(addr1, addr2);
  }

}
