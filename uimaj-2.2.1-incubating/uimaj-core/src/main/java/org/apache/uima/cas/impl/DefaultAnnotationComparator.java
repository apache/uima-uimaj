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

/**
 * Default implementation to compare two annotations.
 * 
 * 
 * @version $Revision: 1.2 $
 */
public class DefaultAnnotationComparator implements FSImplComparator {

  private final int startCode;

  private final int endCode;

  private final CASImpl cas;

  public DefaultAnnotationComparator(CASImpl cas) {
    super();
    this.cas = cas;
    this.startCode = ((FeatureImpl) cas.getTypeSystem().getFeatureByFullName(
            CAS.FEATURE_BASE_NAME_BEGIN)).getCode();
    this.endCode = ((FeatureImpl) cas.getTypeSystem().getFeatureByFullName(
            CAS.FEATURE_BASE_NAME_END)).getCode();
  }

  /**
   * Compare two annotations. First compare by start position, where smaller start position means
   * smaller annotation. If start positions are equal, compare by end position, where larger end
   * position is smaller. Finally, compare by type code, arbitrarily. Does not compare feature
   * values.
   * 
   * @param addr1
   *          Address of FS1.
   * @param addr2
   *          Address of FS2.
   * @return <code>-1</code>, if FS1 is "smaller" than FS2; <code>1</code>, if FS2 is smaller
   *         than FS1; and <code>0</code>, if FS1 equals FS2.
   */
  public int compare(int addr1, int addr2) {
    if (addr1 == addr2) {
      return 0;
    }
    int i1 = this.cas.getFeatureValue(addr1, this.startCode);
    int i2 = this.cas.getFeatureValue(addr2, this.startCode);
    if (i1 < i2) {
      return -1;
    } else if (i1 > i2) {
      return 1;
    } else { // Start positions are equal.
      i1 = this.cas.getFeatureValue(addr1, this.endCode);
      i2 = this.cas.getFeatureValue(addr2, this.endCode);
      if (i1 > i2) {
        return -1;
      } else if (i2 > i1) {
        return 1;
      } else { // End positions are equal.
        i1 = this.cas.getHeapValue(addr1);
        i2 = this.cas.getHeapValue(addr2);
        if (i1 < i2) {
          return -1;
        } else if (i2 < i1) {
          return 1;
        } else { // Types are equal.
          return 0;
        }
      }
    }
  }

}
