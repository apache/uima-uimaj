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

import org.apache.uima.cas.CommonArrayFS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.cas.impl.SlotKinds.SlotKind;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.internal.util.Misc;
import org.apache.uima.jcas.cas.BooleanArray;
import org.apache.uima.jcas.cas.ByteArray;
import org.apache.uima.jcas.cas.DoubleArray;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.FloatArray;
import org.apache.uima.jcas.cas.IntegerArray;
import org.apache.uima.jcas.cas.LongArray;
import org.apache.uima.jcas.cas.ShortArray;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.tcas.Annotation;


/**
 * A node in the FS Tree Model
 * 
 * A node is
 *   - an Feature Structure array
 *     -- elements may be other nodes
 *   - a primitive value, including Strings
 *   - a Feature Structure.
 */

public class FSNode extends FSTreeNode {

  /** The Constant maxStringLength. */
  private static final int maxStringLength = 100;
  
  /** The s tree model. */
  private final FSTreeModel fSTreeModel;

  /** The Constant INT_FS. */
  static final int INT_FS = 0;

  /** The Constant FLOAT_FS. */
  static final int FLOAT_FS = 1;

  /** The Constant STRING_FS. */
  static final int STRING_FS = 2;

  /** The Constant ARRAY_FS. */
  static final int ARRAY_FS = 3;

  /** The Constant STD_FS. */
  static final int STD_FS = 4;

  /** The Constant DISPLAY_NODE. */
  static final int DISPLAY_NODE = 5;

  /** The Constant BYTE_FS. */
  static final int BYTE_FS = 6;

  /** The Constant BOOL_FS. */
  static final int BOOL_FS = 7;

  /** The Constant SHORT_FS. */
  static final int SHORT_FS = 8;

  /** The Constant LONG_FS. */
  static final int LONG_FS = 9;

  /** The Constant DOUBLE_FS. */
  static final int DOUBLE_FS = 10;

  /** The node class. */
  private final int nodeClass;

  /** The int or long like value. */
  private final long intOrLongLikeValue;
  
  /** The fs. */
  private final TOP fs;
  
  /** The string. */
  private final String string;

  /** The feat. */
  private final Feature feat;

  /** The array elem idx. */
  private final int arrayElemIdx;

  /** The is array elem. */
  private final boolean isArrayElem;
  
  /** The is shortened string. */
  // Remember if we're displaying a shortened string.
  private boolean isShortenedString = false;
  
  /**
   * K 2 nc.
   *
   * @param kind the kind
   * @return the int
   */
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

  /**
   * Instantiates a new FS node.
   *
   * @param fSTreeModel the f S tree model
   * @param nodeClass the node class
   * @param fsOrString the fs or string
   * @param intOrLongLikeValue the int or long like value
   * @param feat the feat
   */
  FSNode(FSTreeModel fSTreeModel, int nodeClass, Object fsOrString, long intOrLongLikeValue, Feature feat) {
    this.fSTreeModel = fSTreeModel;
    this.nodeClass = nodeClass;
    this.intOrLongLikeValue = intOrLongLikeValue;
    this.fs = (fsOrString instanceof TOP) ? (TOP) fsOrString : null;
    this.string = (fsOrString instanceof String) ? (String) fsOrString : null;
    this.feat = feat;
    this.arrayElemIdx = 0;
    this.isArrayElem = false;
  }

  /**
   * Instantiates a new FS node.
   *
   * @param fSTreeModel the f S tree model
   * @param nodeClass the node class
   * @param fsOrString the fs or string
   * @param intOrLongLikeValue the int or long like value
   * @param elementIndex the element index
   */
  FSNode(FSTreeModel fSTreeModel, int nodeClass, Object fsOrString, long intOrLongLikeValue, int elementIndex) {
    this.fSTreeModel = fSTreeModel;
    this.nodeClass = nodeClass;
    this.intOrLongLikeValue = intOrLongLikeValue;
    this.fs = (fsOrString instanceof TOP) ? (TOP) fsOrString : null;
    this.string = (fsOrString instanceof String) ? (String) fsOrString : null;
    this.feat = null;
    this.arrayElemIdx = elementIndex;
    this.isArrayElem = true;
  }

  /**
   * Gets the node class.
   *
   * @return the node class
   */
  int getNodeClass() {
    return this.nodeClass;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.tools.cvd.FSTreeNode#initChildren()
   */
  @Override
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
      int arrayLength = ((CommonArrayFS)fs).size();
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
      List<FSNode> arrayNodes = new ArrayList<>(arrayLength);
      SlotKind kind = type.getComponentSlotKind();
      int nc = k2nc(kind);
      switch (kind) {
      case Slot_Int: {
        int[] a = ((IntegerArray)fs)._getTheArray();
        makeNodes(arrayNodes, arrayLength, i -> new FSNode(this.fSTreeModel, nc, null, a[i], i));
        break;
        }
      case Slot_Float: {
        float[] a = ((FloatArray)fs)._getTheArray();
        makeNodes(arrayNodes, arrayLength, i -> new FSNode(this.fSTreeModel, nc, null, CASImpl.float2int(a[i]), i));
        break;
        }
      case Slot_StrRef: {
        String[] a = ((StringArray)fs)._getTheArray();
        makeNodes(arrayNodes, arrayLength, i -> new FSNode(this.fSTreeModel, nc, a[i], 0, i));
        break;
        }
      case Slot_HeapRef: {
        TOP[] a = ((FSArray)fs)._getTheArray();
        makeNodes(arrayNodes, arrayLength, i -> new FSNode(this.fSTreeModel, nc, a[i], 0, i));
        break;
        }
      case Slot_BooleanRef: {
        boolean[] a = ((BooleanArray)fs)._getTheArray();
        makeNodes(arrayNodes, arrayLength, i -> new FSNode(this.fSTreeModel, nc, null, a[i]? 1 : 0, i));
        break;
        }
      case Slot_ByteRef: {
        byte[] a = ((ByteArray)fs)._getTheArray();
        makeNodes(arrayNodes, arrayLength, i -> new FSNode(this.fSTreeModel, nc, null, a[i], i));
        break;
        }
      case Slot_ShortRef: {
        short[] a = ((ShortArray)fs)._getTheArray();
        makeNodes(arrayNodes, arrayLength, i -> new FSNode(this.fSTreeModel, nc, null, a[i], i));
        break;
        }
      case Slot_LongRef: {
        long[] a = ((LongArray)fs)._getTheArray();
        makeNodes(arrayNodes, arrayLength, i -> new FSNode(this.fSTreeModel, nc, null, a[i], i));
        break;
        }
      case Slot_DoubleRef: {
        double[] a = ((DoubleArray)fs)._getTheArray();
        makeNodes(arrayNodes, arrayLength, i -> new FSNode(this.fSTreeModel, nc, null, CASImpl.double2long(a[i]), i));
        break;
        }
      default: Misc.internalError();
      }  // end of switch
      
      this.children = FSTreeModel.createArrayChildren(0, arrayLength, arrayNodes, this.fSTreeModel);
    } else {
      this.children = new ArrayList<>(type.getNumberOfFeatures());
      FeatureImpl[] feats = type.getFeatureImpls();
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

  /**
   * Make nodes.
   *
   * @param arrayNodes the array nodes
   * @param newFSNode the new FS node
   */
  private void makeNodes(List<FSNode> arrayNodes, int size, IntFunction<FSNode> newFSNode) {
    for (int idx = 0; idx < size; idx++) {
      arrayNodes.add(newFSNode.apply(idx));
    }
  }  
  
  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
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

  /**
   * Gets the feature string.
   *
   * @return the feature string
   */
  private String getFeatureString() {
    return "<i>" + this.feat.getShortName() + "</i>";
  }
  
  /**
   * Checks if is shortened string.
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

  /**
   * Gets the value string.
   *
   * @return the value string
   */
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
                + ((CommonArrayFS)fs).size() + "]";
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

  /**
   * Shorten string.
   *
   * @param s the s
   * @return the string
   */
  private static final String shortenString(String s) {
    if (s.length() <= maxStringLength) {
      return s;
    }
    StringBuffer buf = new StringBuffer();
    buf.append(s.substring(0, maxStringLength));
    buf.append("...");
    return buf.toString();
  }
  
  /**
   * Escape lt.
   *
   * @param s the s
   * @return the string
   */
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
  
  /**
   * Gets the null string.
   *
   * @return the null string
   */
  private String getNullString() {
    return "&lt;null&gt;";
  }

  /**
   * Gets the array pos.
   *
   * @return the array pos
   */
  int getArrayPos() {
    return this.arrayElemIdx;
  }

  /**
   * Gets the type.
   *
   * @return the type
   */
  TypeImpl getType() {
    return fs._getTypeImpl();
  }

  /**
   * Checks if is annotation.
   *
   * @return true, if is annotation
   */
  public boolean isAnnotation() {
    return fs != null && fs instanceof Annotation;
  }

  /**
   * Gets the start.
   *
   * @return the start
   */
  public int getStart() {
    return isAnnotation() ? ((Annotation)fs).getBegin() : -1;
  }

  /**
   * Gets the end.
   *
   * @return the end
   */
  public int getEnd() {
    return isAnnotation() ? ((Annotation)fs).getEnd() : -1;
  }

}
