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
import java.util.function.IntFunction;

import org.apache.uima.cas.Feature;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.cas.impl.SlotKinds.SlotKind;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.jcas.cas.BooleanArray;
import org.apache.uima.jcas.cas.ByteArray;
import org.apache.uima.jcas.cas.CommonArray;
import org.apache.uima.jcas.cas.DoubleArray;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.FloatArray;
import org.apache.uima.jcas.cas.IntegerArray;
import org.apache.uima.jcas.cas.LongArray;
import org.apache.uima.jcas.cas.ShortArray;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.util.Misc;

/**
 * A node in the FS Tree Model
 * 
 * A node is
 *   - an Feature Structure array
 *     -- elements may be other nodes
 *   - a primitive value, including Strings
 *   - a Feature Structure
 */

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

  private final long intOrLongLikeValue;
  
  private final TOP fs;
  
  private final String string;

  private final Feature feat;

  private final int arrayElemIdx;

  private final boolean isArrayElem;
  
  // Remember if we're displaying a shortened string.
  private boolean isShortenedString = false;
  
  private int k2nc(SlotKind kind) {
    switch(kind) {
    case Slot_Boolean: 
    case Slot_BooleanRef: return BOOL_FS;
    case Slot_Byte:
    case Slot_ByteRef: return BYTE_FS;
    case Slot_Short:
    case Slot_ShortRef: return SHORT_FS;
    case Slot_Int: return INT_FS;
    case Slot_Float: return FLOAT_FS;
    case Slot_LongRef: return LONG_FS;
    case Slot_DoubleRef: return DOUBLE_FS;
    case Slot_StrRef: return STRING_FS;
    case Slot_HeapRef: return STD_FS;
    default: Misc.internalError(); return -1;
    }
  }

  FSNode(FSTreeModel fSTreeModel, int nodeClass, Object fsOrString, long intOrLongLikeValue, Feature feat) {
    super();
    this.fSTreeModel = fSTreeModel;
    this.nodeClass = nodeClass;
    this.intOrLongLikeValue = intOrLongLikeValue;
    this.fs = (fsOrString instanceof TOP) ? (TOP) fsOrString : null;
    this.string = (fsOrString instanceof String) ? (String) fsOrString : null;
    this.feat = feat;
    this.arrayElemIdx = 0;
    this.isArrayElem = false;
  }

  FSNode(FSTreeModel fSTreeModel, int nodeClass, Object fsOrString, long intOrLongLikeValue, int elementIndex) {
    super();
    this.fSTreeModel = fSTreeModel;
    this.nodeClass = nodeClass;
    this.intOrLongLikeValue = intOrLongLikeValue;
    this.fs = (fsOrString instanceof TOP) ? (TOP) fsOrString : null;
    this.string = (fsOrString instanceof String) ? (String) fsOrString : null;
    this.feat = null;
    this.arrayElemIdx = elementIndex;
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
    // only FSs or Arrays have children
    if (this.fs == null) {
      return;
    }
    
    TypeImpl type = getType();
//    CASImpl cas = this.fSTreeModel.getCas();
    if (type.isArray()) {
      int arrayLength = ((CommonArray)fs).size();
//      if (arrayLength > 20) {
//        arrayLength = 20;
//      }

//      FSNode node = null;
//      int arrayPos = cas.getArrayStartAddress((int) this.addr);
//      int nodeClass1;
//      if (cas.isIntArrayType(type)) {
//        nodeClass1 = FSNode.INT_FS;
//      } else if (cas.isFloatArrayType(type)) {
//        nodeClass1 = FSNode.FLOAT_FS;
//      } else if (cas.isStringArrayType(type)) {
//        nodeClass1 = FSNode.STRING_FS;
//      } else if (cas.isByteArrayType(type)) {
//        nodeClass1 = FSNode.BYTE_FS;
//      } else if (cas.isBooleanArrayType(type)) {
//        nodeClass1 = FSNode.BOOL_FS;
//      } else if (cas.isShortArrayType(type)) {
//        nodeClass1 = FSNode.SHORT_FS;
//      } else if (cas.isLongArrayType(type)) {
//        nodeClass1 = FSNode.LONG_FS;
//      } else if (cas.isDoubleArrayType(type)) {
//        nodeClass1 = FSNode.DOUBLE_FS;
//      } else {
//        nodeClass1 = FSNode.STD_FS;
//      }
      List<FSNode> arrayNodes = new ArrayList<FSNode>(arrayLength);
      SlotKind kind = type.getComponentSlotKind();
      int nc = k2nc(kind);
      switch (kind) {
      case Slot_Int: {
        int[] a = ((IntegerArray)fs)._getTheArray();
        makeNodes(arrayNodes, i -> new FSNode(this.fSTreeModel, nc, null, a[i], i));
        break;
        }
      case Slot_Float: {
        float[] a = ((FloatArray)fs)._getTheArray();
        makeNodes(arrayNodes, i -> new FSNode(this.fSTreeModel, nc, null, CASImpl.float2int(a[i]), i));
        break;
        }
      case Slot_StrRef: {
        String[] a = ((StringArray)fs)._getTheArray();
        makeNodes(arrayNodes, i -> new FSNode(this.fSTreeModel, nc, a[i], 0, i));
        break;
        }
      case Slot_HeapRef: {
        TOP[] a = ((FSArray)fs)._getTheArray();
        makeNodes(arrayNodes, i -> new FSNode(this.fSTreeModel, nc, a[i], 0, i));
        break;
        }
      case Slot_BooleanRef: {
        boolean[] a = ((BooleanArray)fs)._getTheArray();
        makeNodes(arrayNodes, i -> new FSNode(this.fSTreeModel, nc, null, a[i]? 1 : 0, i));
        break;
        }
      case Slot_ByteRef: {
        byte[] a = ((ByteArray)fs)._getTheArray();
        makeNodes(arrayNodes, i -> new FSNode(this.fSTreeModel, nc, null, a[i], i));
        break;
        }
      case Slot_ShortRef: {
        short[] a = ((ShortArray)fs)._getTheArray();
        makeNodes(arrayNodes, i -> new FSNode(this.fSTreeModel, nc, null, a[i], i));
        break;
        }
      case Slot_LongRef: {
        long[] a = ((LongArray)fs)._getTheArray();
        makeNodes(arrayNodes, i -> new FSNode(this.fSTreeModel, nc, null, a[i], i));
        break;
        }
      case Slot_DoubleRef: {
        double[] a = ((DoubleArray)fs)._getTheArray();
        makeNodes(arrayNodes, i -> new FSNode(this.fSTreeModel, nc, null, CASImpl.double2long(a[i]), i));
        break;
        }
      default: Misc.internalError();
      }  // end of switch
      
      this.children = FSTreeModel.createArrayChildren(0, arrayLength, arrayNodes, this.fSTreeModel);
    } else {
      this.children = new ArrayList<FSTreeNode>(type.getNumberOfFeatures());
      List<FeatureImpl> feats = type.getFeatureImpls();
      for (FeatureImpl f : feats) {
        SlotKind kind = f.getSlotKind();
        int nc = k2nc(kind);
        switch(kind) {
        case Slot_Boolean: children.add(new FSNode(this.fSTreeModel, nc, null, fs.getBooleanValue(f) ? 1 : 0, f)); break;
        case Slot_Byte:    children.add(new FSNode(this.fSTreeModel, nc, null, fs.getByteValue(f), f)); break;
        case Slot_Short:   children.add(new FSNode(this.fSTreeModel, nc, null, fs.getShortValue(f), f)); break;
        case Slot_Int:     children.add(new FSNode(this.fSTreeModel, nc, null, fs.getIntValue(f), f)); break;
        case Slot_Float:   children.add(new FSNode(this.fSTreeModel, nc, null, CASImpl.float2int(fs.getFloatValue(f)), f)); break;
        case Slot_LongRef: children.add(new FSNode(this.fSTreeModel, nc, null, fs.getLongValue(f), f)); break;
        case Slot_DoubleRef:children.add(new FSNode(this.fSTreeModel, nc, null, CASImpl.double2long(fs.getDoubleValue(f)), f)); break;
        case Slot_StrRef:  children.add(new FSNode(this.fSTreeModel, nc, fs.getStringValue(f), 0, f)); break;
        case Slot_HeapRef: children.add(new FSNode(this.fSTreeModel, nc, fs.getFeatureValue(f), 0, f)); break;
        default: Misc.internalError();
        } // end of switch
      }
    }
  }

  private void makeNodes(List<FSNode> arrayNodes, IntFunction<FSNode> newFSNode) {
    final int size = arrayNodes.size();
    for (int idx = 0; idx < size; idx++) {
      arrayNodes.add(newFSNode.apply(idx));
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
      buf.append(this.arrayElemIdx);
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
   * @return for string nodes, return the string value (so it can be displayed seperately
   */
  String getFullString() {
    if (getNodeClass() != STRING_FS) {
      return null;
    }
    return this.string;
  }

  private String getValueString() {
    
    switch (this.nodeClass) {
      case INT_FS:
      case BYTE_FS:
      case SHORT_FS: {
        return Long.toString(this.intOrLongLikeValue);
      }
      case FLOAT_FS: {
        return Float.toString(CASImpl.int2float((int) this.intOrLongLikeValue));
      }
      case BOOL_FS: {
        return (0 == this.intOrLongLikeValue) ? "false" : "true";
      }
      case LONG_FS: {
        return Long.toString(this.intOrLongLikeValue);
      }
      case DOUBLE_FS: {
        return Double.toString(CASImpl.long2double(this.intOrLongLikeValue));
      }
      case STRING_FS: {
        if (this.string == null) {
          return getNullString();
        }
        String s = this.string;
        // Check if we need to shorten the string for display purposes
        String s1 = shortenString(s);
        // Remember if string is shortened
        this.isShortenedString = (s != s1);
        return "\"" + escapeLt(s1) + "\"";
      }
      case ARRAY_FS: {
        if (this.fs == null) {
          return getNullString();
        }
        return "<font color=blue>" + getType().getName() + "</font>["
                + ((CommonArray)fs).size() + "]";
      }
      case STD_FS: {
        if (fs == null) {
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
    return this.arrayElemIdx;
  }

  TypeImpl getType() {
    return fs._typeImpl;
  }

  public boolean isAnnotation() {
    return fs != null && fs instanceof Annotation;
  }

  public int getStart() {
    return isAnnotation() ? ((Annotation)fs).getBegin() : -1;
  }

  public int getEnd() {
    return isAnnotation() ? ((Annotation)fs).getEnd() : -1;
  }

}
