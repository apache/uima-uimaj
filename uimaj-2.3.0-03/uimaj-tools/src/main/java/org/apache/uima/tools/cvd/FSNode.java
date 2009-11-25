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

package org.apache.uima.tools.cvd;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.cas.impl.LowLevelCAS;
import org.apache.uima.cas.text.AnnotationFS;

public class FSNode extends FSTreeNode {

  private static final int maxStringLength = 100;
  
  private final FSTreeModel fSTreeModel;

  static final int INT_FS = 0;

  static final int FLOAT_FS = 1;

  static final int STRING_FS = 2;

  static final int ARRAY_FS = 3;

  static final int STD_FS = 4;

  static final int DISPLAY_NODE = 5;

  static final int BYTE_FS = 6;

  static final int BOOL_FS = 7;

  static final int SHORT_FS = 8;

  static final int LONG_FS = 9;

  static final int DOUBLE_FS = 10;

  private final int nodeClass;

  private final long addr;

  private final Feature feat;

  private final int arrayElem;

  private final boolean isArrayElem;
  
  // Remember if we're displaying a shortened string.
  private boolean isShortenedString = false;

  FSNode(FSTreeModel fSTreeModel, int nodeClass, long addr, Feature feat) {
    super();
    this.fSTreeModel = fSTreeModel;
    this.nodeClass = nodeClass;
    this.addr = addr;
    this.feat = feat;
    this.arrayElem = 0;
    this.isArrayElem = false;
  }

  FSNode(FSTreeModel fSTreeModel, int nodeClass, long addr, int elem) {
    super();
    this.fSTreeModel = fSTreeModel;
    this.nodeClass = nodeClass;
    this.addr = addr;
    this.feat = null;
    this.arrayElem = elem;
    this.isArrayElem = true;
  }

  int getNodeClass() {
    return this.nodeClass;
  }

  protected void initChildren() {
    if (this.children != null) {
      return;
    }
    if ((this.nodeClass != STD_FS) && (this.nodeClass != ARRAY_FS)) {
      return;
    }
    if (this.addr == 0) {
      return;
    }
    Type type = getType();
    CASImpl cas = this.fSTreeModel.getCas();
    if (this.fSTreeModel.getCas().isArrayType(type)) {
      int arrayLength = cas.ll_getArraySize((int) this.addr);
//      if (arrayLength > 20) {
//        arrayLength = 20;
//      }
      FSNode node = null;
      int arrayPos = cas.getArrayStartAddress((int) this.addr);
      int nodeClass1;
      if (cas.isIntArrayType(type)) {
        nodeClass1 = FSNode.INT_FS;
      } else if (cas.isFloatArrayType(type)) {
        nodeClass1 = FSNode.FLOAT_FS;
      } else if (cas.isStringArrayType(type)) {
        nodeClass1 = FSNode.STRING_FS;
      } else if (cas.isByteArrayType(type)) {
        nodeClass1 = FSNode.BYTE_FS;
      } else if (cas.isBooleanArrayType(type)) {
        nodeClass1 = FSNode.BOOL_FS;
      } else if (cas.isShortArrayType(type)) {
        nodeClass1 = FSNode.SHORT_FS;
      } else if (cas.isLongArrayType(type)) {
        nodeClass1 = FSNode.LONG_FS;
      } else if (cas.isDoubleArrayType(type)) {
        nodeClass1 = FSNode.DOUBLE_FS;
      } else {
        nodeClass1 = FSNode.STD_FS;
      }
      List<FSNode> arrayNodes = new ArrayList<FSNode>(arrayLength);
      if (nodeClass1 == INT_FS || nodeClass1 == FLOAT_FS || nodeClass1 == STRING_FS
              || nodeClass1 == STD_FS) {
        for (int i = 0; i < arrayLength; i++) {
          node = new FSNode(this.fSTreeModel, nodeClass1, cas.getHeapValue(arrayPos), i);
          arrayNodes.add(node);
          ++arrayPos;
        }
      } else if (nodeClass1 == BYTE_FS) {
        for (int i = 0; i < arrayLength; i++) {
          node = new FSNode(this.fSTreeModel, nodeClass1, cas.ll_getByteArrayValue((int) this.addr,
                  i), i);
          arrayNodes.add(node);
          ++arrayPos;
        }
      } else if (nodeClass1 == SHORT_FS) {
        for (int i = 0; i < arrayLength; i++) {
          node = new FSNode(this.fSTreeModel, nodeClass1, cas.ll_getShortArrayValue(
                  (int) this.addr, i), i);
          arrayNodes.add(node);
          ++arrayPos;
        }
      } else if (nodeClass1 == BOOL_FS) {
        for (int i = 0; i < arrayLength; i++) {
          int temp = cas.ll_getBooleanArrayValue((int) this.addr, i) ? 1 : 0;
          node = new FSNode(this.fSTreeModel, nodeClass1, temp, i);
          arrayNodes.add(node);
          ++arrayPos;
        }
      } else if (nodeClass1 == LONG_FS) {
        for (int i = 0; i < arrayLength; i++) {
          long temp = cas.ll_getLongArrayValue((int) this.addr, i);
          node = new FSNode(this.fSTreeModel, nodeClass1, temp, i);
          arrayNodes.add(node);
          ++arrayPos;
        }
      } else if (nodeClass1 == DOUBLE_FS) {
        for (int i = 0; i < arrayLength; i++) {
          double temp = cas.ll_getDoubleArrayValue((int) this.addr, i);
          node = new FSNode(this.fSTreeModel, nodeClass1, CASImpl.double2long(temp), i);
          arrayNodes.add(node);
          ++arrayPos;
        }
      }
      this.children = FSTreeModel.createArrayChildren(0, arrayLength, arrayNodes, this.fSTreeModel);
    } else {
      this.children = new ArrayList<FSTreeNode>();
      List<?> feats = type.getFeatures();
      for (int i = 0; i < feats.size(); i++) {
        FeatureImpl feat1 = (FeatureImpl) feats.get(i);
        long featAddr = cas.getHeapValue((int) this.addr + cas.getFeatureOffset(feat1.getCode()));
        if (cas.isDoubleType(feat1.getRange()) || cas.isLongType(feat1.getRange())) {
          featAddr = cas.ll_getLongValue((int) featAddr);
        }
        FSNode childNode = new FSNode(this.fSTreeModel, this.fSTreeModel.getNodeType(
                (int) this.addr, feat1), featAddr, feat1);
        this.children.add(childNode);
      }
    }
  }

  public String toString() {
    if (this.nodeClass == DISPLAY_NODE) {
      return this.fSTreeModel.getRootString();
    }
    StringBuffer buf = new StringBuffer();
    buf.append("<html>");
    if (this.isArrayElem) {
      buf.append('[');
      buf.append(this.arrayElem);
      buf.append("] = ");
    } else if (this.feat != null) {
      buf.append(getFeatureString());
      buf.append(" = ");
    }
    buf.append(getValueString());
    buf.append("</html>");
    return buf.toString();
  }

  private String getFeatureString() {
    return "<i>" + this.feat.getShortName() + "</i>";
  }
  
  /**
   * 
   * @return if this is a string node displaying a shortened string
   */
  boolean isShortenedString() {
    return this.isShortenedString;
  }
  
  /**
   * 
   * @return for string nodes, return the string value (so it can be displayed seperately
   */
  String getFullString() {
    if (getNodeClass() != STRING_FS) {
      return null;
    }
    return this.fSTreeModel.getCas().getStringForCode((int) this.addr);
  }

  private String getValueString() {
    CASImpl cas = this.fSTreeModel.getCas();
    switch (this.nodeClass) {
      case INT_FS:
      case BYTE_FS:
      case SHORT_FS: {
        return Long.toString(this.addr);
      }
      case FLOAT_FS: {
        return Float.toString(CASImpl.int2float((int) this.addr));
      }
      case BOOL_FS: {
        return (0 == this.addr) ? "false" : "true";
      }
      case LONG_FS: {
        return Long.toString(this.addr);
      }
      case DOUBLE_FS: {
        return Double.toString(CASImpl.long2double(this.addr));
      }
      case STRING_FS: {
        if (this.addr == LowLevelCAS.NULL_FS_REF) {
          return getNullString();
        }
        String s = cas.getStringForCode((int) this.addr);
        // Check if we need to shorten the string for display purposes
        String s1 = shortenString(s);
        // Remember if string is shortened
        this.isShortenedString = (s != s1);
        return "\"" + escapeLt(s1) + "\"";
      }
      case ARRAY_FS: {
        if (cas.getHeapValue((int) this.addr) == LowLevelCAS.NULL_FS_REF) {
          return getNullString();
        }
        return "<font color=blue>" + getType().getName() + "</font>["
                + cas.ll_getArraySize((int) this.addr) + "]";
      }
      case STD_FS: {
        if (cas.getHeapValue((int) this.addr) == LowLevelCAS.NULL_FS_REF) {
          return getNullString();
        }
        return "<font color=blue>" + getType().getName() + "</font>";
      }
    }
    return null;
  }

  private static final String shortenString(String s) {
    if (s.length() <= maxStringLength) {
      return s;
    }
    StringBuffer buf = new StringBuffer();
    buf.append(s.substring(0, maxStringLength));
    buf.append("...");
    return buf.toString();
  }
  
  private static final String escapeLt(String s) {
    final int max = s.length();
    int i = 0;
    while (i < max) {
      if (s.charAt(i) == '<') {
	break;
      }
      ++i;
    }
    if (i == max) {
      return s;
    }
    StringBuffer buf = new StringBuffer(s.substring(0, i));
    while (i < max) {
      if (s.charAt(i) == '<') {
	buf.append("&lt;");
      } else {
	buf.append(s.charAt(i));
      }
      ++i;
    }
    return buf.toString();
  }
  
  private String getNullString() {
    return "&lt;null&gt;";
  }

  int getArrayPos() {
    return this.arrayElem;
  }

  Type getType() {
    CASImpl cas = this.fSTreeModel.getCas();
    return cas.getTypeSystemImpl().ll_getTypeForCode(cas.getHeapValue((int) this.addr));
  }

  public boolean isAnnotation() {
    CASImpl cas = this.fSTreeModel.getCas();
    if (this.nodeClass != STD_FS || this.addr == 0) {
      return false;
    }
    if (cas.getTypeSystem().subsumes(cas.getAnnotationType(), getType())) {
      if (cas == ((AnnotationFS) cas.createFS((int) this.addr)).getView()) {
        return true;
      }
    }
    return false;
  }

  public int getStart() {
    CASImpl cas = this.fSTreeModel.getCas();
    if (isAnnotation()) {
      final FeatureImpl feat1 = (FeatureImpl) cas.getBeginFeature();
      return cas.getHeapValue((int) this.addr + cas.getFeatureOffset(feat1.getCode()));
    }
    return -1;
  }

  public int getEnd() {
    CASImpl cas = this.fSTreeModel.getCas();
    if (isAnnotation()) {
      final FeatureImpl feat1 = (FeatureImpl) cas.getEndFeature();
      return cas.getHeapValue((int) this.addr + cas.getFeatureOffset(feat1.getCode()));
    }
    return -1;
  }

}
