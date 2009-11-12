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

import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.impl.XmiSerializationSharedData.OotsElementData;
import org.apache.uima.internal.util.IntVector;
import org.apache.uima.internal.util.XmlAttribute;
import org.apache.uima.internal.util.rb_trees.IntRedBlackTree;
import org.apache.uima.util.Level;
import org.apache.uima.util.Logger;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Utilities for dealing with CAS List types.
 * 
 */
public class ListUtils {
  CASImpl cas;

  // list type and feature codes
  private int intListType;

  private int floatListType;

  private int stringListType;

  private int fsListType;

  private int neIntListType;

  private int neFloatListType;

  private int neStringListType;

  private int neFsListType;

  private int eIntListType;

  private int eFloatListType;

  private int eStringListType;

  private int eFsListType;

  private int intHeadFeat;

  private int intTailFeat;

  private int floatHeadFeat;

  private int floatTailFeat;

  private int stringHeadFeat;

  private int stringTailFeat;

  private int fsHeadFeat;

  private int fsTailFeat;

  private Logger logger;

  private ErrorHandler eh;

  /**
   * Creates a new ListUtils object.
   * 
   * @param aCASImpl
   *          the CAS that this ListUtils will operate on
   * @param aLogger
   *          optional logger, to receive warning messages
   * @param aErrorHandler
   *          optional SAX ErrorHandler, to receive warning messages
   */
  public ListUtils(CASImpl aCASImpl, Logger aLogger, ErrorHandler aErrorHandler) {
    this.cas = aCASImpl;
    this.logger = aLogger;
    this.eh = aErrorHandler;
    TypeSystemImpl ts = aCASImpl.getTypeSystemImpl();
    this.intListType = ts.ll_getCodeForTypeName(CAS.TYPE_NAME_INTEGER_LIST);
    this.floatListType = ts.ll_getCodeForTypeName(CAS.TYPE_NAME_FLOAT_LIST);
    this.stringListType = ts.ll_getCodeForTypeName(CAS.TYPE_NAME_STRING_LIST);
    this.fsListType = ts.ll_getCodeForTypeName(CAS.TYPE_NAME_FS_LIST);
    this.neIntListType = ts.ll_getCodeForTypeName(CAS.TYPE_NAME_NON_EMPTY_INTEGER_LIST);
    this.neFloatListType = ts.ll_getCodeForTypeName(CAS.TYPE_NAME_NON_EMPTY_FLOAT_LIST);
    this.neStringListType = ts.ll_getCodeForTypeName(CAS.TYPE_NAME_NON_EMPTY_STRING_LIST);
    this.neFsListType = ts.ll_getCodeForTypeName(CAS.TYPE_NAME_NON_EMPTY_FS_LIST);
    this.eIntListType = ts.ll_getCodeForTypeName(CAS.TYPE_NAME_EMPTY_INTEGER_LIST);
    this.eFloatListType = ts.ll_getCodeForTypeName(CAS.TYPE_NAME_EMPTY_FLOAT_LIST);
    this.eStringListType = ts.ll_getCodeForTypeName(CAS.TYPE_NAME_EMPTY_STRING_LIST);
    this.eFsListType = ts.ll_getCodeForTypeName(CAS.TYPE_NAME_EMPTY_FS_LIST);
    this.intHeadFeat = ts.ll_getCodeForFeatureName(CAS.FEATURE_FULL_NAME_INTEGER_LIST_HEAD);
    this.floatHeadFeat = ts.ll_getCodeForFeatureName(CAS.FEATURE_FULL_NAME_FLOAT_LIST_HEAD);
    this.stringHeadFeat = ts.ll_getCodeForFeatureName(CAS.FEATURE_FULL_NAME_STRING_LIST_HEAD);
    this.fsHeadFeat = ts.ll_getCodeForFeatureName(CAS.FEATURE_FULL_NAME_FS_LIST_HEAD);
    this.intTailFeat = ts.ll_getCodeForFeatureName(CAS.FEATURE_FULL_NAME_INTEGER_LIST_TAIL);
    this.floatTailFeat = ts.ll_getCodeForFeatureName(CAS.FEATURE_FULL_NAME_FLOAT_LIST_TAIL);
    this.stringTailFeat = ts.ll_getCodeForFeatureName(CAS.FEATURE_FULL_NAME_STRING_LIST_TAIL);
    this.fsTailFeat = ts.ll_getCodeForFeatureName(CAS.FEATURE_FULL_NAME_FS_LIST_TAIL);
  }

  public boolean isIntListType(int type) {
    return (type == this.intListType || type == this.neIntListType || type == this.eIntListType);
  }

  public boolean isFloatListType(int type) {
    return (type == this.floatListType || type == this.neFloatListType || type == this.eFloatListType);
  }

  public boolean isStringListType(int type) {
    return (type == this.stringListType || type == this.neStringListType || type == this.eStringListType);
  }

  public boolean isFsListType(int type) {
    return (type == this.fsListType || type == this.neFsListType || type == this.eFsListType);
  }

  public boolean isListType(int type) {
    return isIntListType(type) || isFloatListType(type) || isStringListType(type)
            || isFsListType(type);
  }

  public int getLength(int type, int addr) {
	int neListType = -1;
	int tailFeat = -1;
	if (isIntListType(type)) {
	  neListType = neIntListType;
	  tailFeat = intTailFeat;
	} else if ( isFloatListType(type)) {
	  neListType = neFloatListType;
	  tailFeat = floatTailFeat;
	} else if (isStringListType(type)) {
      neListType = neStringListType;
	  tailFeat = stringTailFeat;
	} else if (isFsListType(type)) {
	  neListType = neFsListType;
	  tailFeat = fsTailFeat;
	}
	IntRedBlackTree visited = new IntRedBlackTree();
	boolean foundCycle = false;
	// first count length of list so we can allocate array
	int length = 0;
	int curNode = addr;
	while (cas.getHeapValue(curNode) == neListType) {
	  if (!visited.put(curNode, curNode)) {
	    foundCycle = true;
	    break;
	  }
	  length++;
	  curNode = cas.getHeapValue(curNode + cas.getFeatureOffset(tailFeat));
	}
	return length;
  }
  
  public String[] intListToStringArray(int addr) throws SAXException {
    IntRedBlackTree visited = new IntRedBlackTree();
    boolean foundCycle = false;
    // first count length of list so we can allocate array
    int length = 0;
    int curNode = addr;
    while (cas.getHeapValue(curNode) == neIntListType) {
      if (!visited.put(curNode, curNode)) {
        foundCycle = true;
        break;
      }
      length++;
      curNode = cas.getHeapValue(curNode + cas.getFeatureOffset(intTailFeat));
    }

    String[] array = new String[length];

    // now populate list
    curNode = addr;
    for (int i = 0; i < length; i++) {
      array[i] = Integer.toString(cas.getHeapValue(curNode + cas.getFeatureOffset(intHeadFeat)));
      curNode = cas.getHeapValue(curNode + cas.getFeatureOffset(intTailFeat));
    }
    if (foundCycle) {
      reportWarning("Found a cycle in an IntegerList.  List truncated to "
              + Arrays.asList(array).toString());
    }
    return array;
  }

  public String[] floatListToStringArray(int addr) throws SAXException {
    boolean foundCycle = false;
    IntRedBlackTree visited = new IntRedBlackTree();
    // first count length of list so we can allocate array
    int length = 0;
    int curNode = addr;
    while (cas.getHeapValue(curNode) == neFloatListType) {
      if (!visited.put(curNode, curNode)) {
        foundCycle = true;
        break;
      }
      length++;
      curNode = cas.getHeapValue(curNode + cas.getFeatureOffset(floatTailFeat));
    }

    String[] array = new String[length];

    // now populate list
    curNode = addr;
    for (int i = 0; i < length; i++) {
      array[i] = Float.toString(CASImpl.int2float(cas.getHeapValue(curNode
              + cas.getFeatureOffset(floatHeadFeat))));
      curNode = cas.getHeapValue(curNode + cas.getFeatureOffset(floatTailFeat));
    }
    if (foundCycle) {
      reportWarning("Found a cycle in a FloatList.  List truncated to "
              + Arrays.asList(array).toString() + ".");
    }
    return array;
  }

  public String[] stringListToStringArray(int addr) throws SAXException {
    boolean foundCycle = false;
    IntRedBlackTree visited = new IntRedBlackTree();
    // first count length of list so we can allocate array
    int length = 0;
    int curNode = addr;
    while (cas.getHeapValue(curNode) == neStringListType) {
      if (!visited.put(curNode, curNode)) {
        foundCycle = true;
        break;
      }
      length++;
      curNode = cas.getHeapValue(curNode + cas.getFeatureOffset(stringTailFeat));
    }

    String[] array = new String[length];

    // now populate list
    curNode = addr;
    for (int i = 0; i < length; i++) {
      array[i] = cas.getStringForCode(cas.getHeapValue(curNode
              + cas.getFeatureOffset(stringHeadFeat)));
      curNode = cas.getHeapValue(curNode + cas.getFeatureOffset(stringTailFeat));
    }
    if (foundCycle) {
      reportWarning("Found a cycle in a StringList.  List truncated to "
              + Arrays.asList(array).toString() + ".");
    }
    return array;
  }

  public String[] fsListToXmiIdStringArray(int addr, XmiSerializationSharedData sharedData)
          throws SAXException {
    boolean foundCycle = false;
    IntRedBlackTree visited = new IntRedBlackTree();
    // first count length of list so we can allocate array
    int length = 0;
    int curNode = addr;
    while (cas.getHeapValue(curNode) == neFsListType) {
      if (!visited.put(curNode, curNode)) {
        foundCycle = true;
        break;
      }
      length++;
      curNode = cas.getHeapValue(curNode + cas.getFeatureOffset(fsTailFeat));
    }

    String[] strArray = new String[length];

    // now populate list
    curNode = addr;
    for (int i = 0; i < length; i++) {
      int heapVal = cas.getHeapValue(curNode + cas.getFeatureOffset(fsHeadFeat));
      if (heapVal == 0) {
        //null value in list.  Represent with "0".
        strArray[i] = "0";
        // However, this may be null because the element was originally a reference to an 
        // out-of-typesystem FS, so chck the XmiSerializationSharedData
        if (sharedData != null) {
          OotsElementData oed = sharedData.getOutOfTypeSystemFeatures(curNode);
          if (oed != null) {
            assert oed.attributes.size() == 1; //only the head feature can possibly be here
            XmlAttribute attr = (XmlAttribute)oed.attributes.get(0);
            assert CAS.FEATURE_BASE_NAME_HEAD.equals(attr.name);
            strArray[i] = attr.value;
          }
        }        
      }
      else {
        if (sharedData != null) {
          strArray[i] = heapVal == 0 ? null : sharedData.getXmiId(heapVal);
        } else {
          strArray[i] = Integer.toString(heapVal);
        }
      }
      curNode = cas.getHeapValue(curNode + cas.getFeatureOffset(fsTailFeat));
    }
    if (foundCycle) {
      reportWarning("Found a cycle in an FSList.  List truncated to "
              + Arrays.asList(strArray).toString() + ".");
    }
    return strArray;
  }

  public int[] fsListToAddressArray(int addr) throws SAXException {
    boolean foundCycle = false;
    IntRedBlackTree visited = new IntRedBlackTree();
    // first count length of list so we can allocate array
    int length = 0;
    int curNode = addr;
    while (cas.getHeapValue(curNode) == neFsListType) {
      if (!visited.put(curNode, curNode)) {
        foundCycle = true;
        break;
      }
      length++;
      curNode = cas.getHeapValue(curNode + cas.getFeatureOffset(fsTailFeat));
    }

    int[] array = new int[length];

    // now populate list
    curNode = addr;
    for (int i = 0; i < length; i++) {
      array[i] = cas.getHeapValue(curNode + cas.getFeatureOffset(fsHeadFeat));
      curNode = cas.getHeapValue(curNode + cas.getFeatureOffset(fsTailFeat));
    }
    if (foundCycle) {
      reportWarning("Found a cycle in an FSList.  List truncated where cycle occurs.");
    }
    return array;
  }

  public int createIntList(List<String> stringValues) {
    int first = cas.ll_createFS(eIntListType);
    ListIterator<String> iter = stringValues.listIterator(stringValues.size());
    while (iter.hasPrevious()) {
      int value = Integer.parseInt((String) iter.previous());
      int node = cas.ll_createFS(neIntListType);
      cas.setFeatureValue(node, intHeadFeat, value);
      cas.setFeatureValue(node, intTailFeat, first);
      first = node;
    }
    return first;
  }
  
  public int createFloatList(List<String> stringValues) {
    int first = cas.ll_createFS(eFloatListType);
    ListIterator<String> iter = stringValues.listIterator(stringValues.size());
    while (iter.hasPrevious()) {
      float value = Float.parseFloat((String) iter.previous());
      int node = cas.ll_createFS(neFloatListType);
      cas.setFeatureValue(node, floatHeadFeat, CASImpl.float2int(value));
      cas.setFeatureValue(node, floatTailFeat, first);
      first = node;
    }
    return first;
  }

  public int createStringList(List<String> stringValues) {
    int first = cas.ll_createFS(eStringListType);
    ListIterator<String> iter = stringValues.listIterator(stringValues.size());
    while (iter.hasPrevious()) {
      String value = iter.previous();
      int node = cas.ll_createFS(neStringListType);
      cas.setFeatureValue(node, stringHeadFeat, cas.addString(value));
      cas.setFeatureValue(node, stringTailFeat, first);
      first = node;
    }
    return first;
  }

  public int createFsList(List<String> stringValues, IntVector fsAddresses) {
    int first = cas.ll_createFS(eFsListType);
    ListIterator<String> iter = stringValues.listIterator(stringValues.size());
    while (iter.hasPrevious()) {
      int value = Integer.parseInt((String) iter.previous());
      int node = cas.ll_createFS(neFsListType);
      fsAddresses.add(node);
      cas.setFeatureValue(node, fsHeadFeat, value);
      cas.setFeatureValue(node, fsTailFeat, first);
      first = node;
    }
    return first;
  }

  public int updateIntList(int addr, List<String> stringValues) throws SAXException  {
    int first = addr;
    int currLength = this.getLength(this.neIntListType, addr);
    int curNode = addr;
    int prevNode = 0;
    IntRedBlackTree visited = new IntRedBlackTree();
    boolean foundCycle = false;
    int i =0;
    
    //if (currLength != stringValues.size() ) {  
	//   first = createIntList(stringValues);
	   
    if (currLength < stringValues.size()) {
  	  while (cas.getHeapValue(curNode) == neIntListType) {
  	  	if (!visited.put(curNode, curNode)) {
  	  	           foundCycle = true;
  	  	           break;
  	  	}
  	  	int value = Integer.parseInt(stringValues.get(i++));
  	    cas.setFeatureValue(curNode,floatHeadFeat, value);
  	    prevNode = curNode;
  	  	curNode = cas.getHeapValue(curNode + cas.getFeatureOffset(intTailFeat));
  	  }
  	  //add nodes for remaining values
  	  if (i < stringValues.size()) {
  	  	int emptyListFs = curNode; 
  	  	//check that first is eintlisttype
  	  	while (i < stringValues.size()) {
  	  	  int newNode = cas.ll_createFS(neIntListType);
  	  	  int value = Integer.parseInt((String) stringValues.get(i++));
  	      cas.setFeatureValue(newNode,intHeadFeat, value);
  	      cas.setFeatureValue(newNode, intTailFeat, emptyListFs);
  	      cas.setFeatureValue(prevNode, intTailFeat, newNode);
  	  	  prevNode = newNode;
  	    }
  	  }
  	} else if (currLength > stringValues.size()) {
  		while (cas.getHeapValue(curNode) == neIntListType && i < stringValues.size()) {
  		  if (!visited.put(curNode, curNode)) {
  		  	           foundCycle = true;
  		  	           break;
  		  }
  		  float value = Integer.parseInt((String) stringValues.get(i++));
  		  cas.setFeatureValue(curNode,intHeadFeat, value);
  		  curNode = cas.getHeapValue(curNode + cas.getFeatureOffset(intTailFeat));
  		}	
  		int finalNode = curNode;
        //loop till we get a FS that is of type eStringListType
        while (cas.getHeapValue(curNode) == neIntListType) {
    	  if (!visited.put(curNode, curNode)) {
    	    foundCycle = true;
    	    break;
    	    //TODO throw exc
    	  }
    	  curNode = cas.getHeapValue(curNode + cas.getFeatureOffset(intTailFeat));
        }
        //set the tail feature to eStringListType fs
        cas.setFeatureValue(finalNode, intTailFeat, curNode);   
    } else {
      while (cas.getHeapValue(curNode) == neIntListType) {
        if (!visited.put(curNode, curNode)) {
           foundCycle = true;
           break;
        }
        int value = Integer.parseInt((String) stringValues.get(i++));
        cas.setFeatureValue(curNode,intHeadFeat, value );
        curNode = cas.getHeapValue(curNode + cas.getFeatureOffset(intTailFeat));
      }
    }
    if (foundCycle) {
        reportWarning("Found a cycle in an IntegerList.  List truncated to "
                + i);
    }
    return first;
  }
	  
  public int updateFloatList(int addr, List<String> stringValues) throws SAXException  {
    int first = addr;
    int currLength = this.getLength(this.neFloatListType, addr);
    int curNode = addr;
    int prevNode = 0;
    IntRedBlackTree visited = new IntRedBlackTree();
    boolean foundCycle = false;
    int i =0;
    
    //if (currLength != stringValues.size() ) {  
	//   first = createFloatList(stringValues);
	if (currLength < stringValues.size()) {
	  while (cas.getHeapValue(curNode) == neFloatListType) {
	  	if (!visited.put(curNode, curNode)) {
	  	           foundCycle = true;
	  	           break;
	  	}
	  	float value = Float.parseFloat(stringValues.get(i++));
	    cas.setFeatureValue(curNode,floatHeadFeat, value);
	    prevNode = curNode;
	  	curNode = cas.getHeapValue(curNode + cas.getFeatureOffset(floatTailFeat));
	  }
	  //add nodes for remaining values
	  if (i < stringValues.size()) {
	  	int emptyListFs = curNode; 
	  	//check that first is efloatlisttype
	  	while (i < stringValues.size()) {
	  	  int newNode = cas.ll_createFS(neFloatListType);
	  	  float value = Float.parseFloat((String) stringValues.get(i++));
	      cas.setFeatureValue(newNode,floatHeadFeat, value);
	      cas.setFeatureValue(newNode, floatTailFeat, emptyListFs);
	      cas.setFeatureValue(prevNode, floatTailFeat, newNode);
	  	  prevNode = newNode;
	    }
	  }
	} else if (currLength > stringValues.size()) {
		while (cas.getHeapValue(curNode) == neFloatListType && i < stringValues.size()) {
		  if (!visited.put(curNode, curNode)) {
		  	           foundCycle = true;
		  	           break;
		  }
		  float value = Float.parseFloat((String) stringValues.get(i++));
		  cas.setFeatureValue(curNode,floatHeadFeat, value);
		  curNode = cas.getHeapValue(curNode + cas.getFeatureOffset(floatTailFeat));
		}	
		int finalNode = curNode;
        //loop till we get a FS that is of type eStringListType
        while (cas.getHeapValue(curNode) == neFloatListType) {
  	      if (!visited.put(curNode, curNode)) {
  	        foundCycle = true;
  	        break;
  	      //TODO throw exc
  	      }
  	      curNode = cas.getHeapValue(curNode + cas.getFeatureOffset(floatTailFeat));
        }
        //set the tail feature to eStringListType fs
      	cas.setFeatureValue(finalNode, floatTailFeat, curNode);
    } else {
      while (cas.getHeapValue(curNode) == neFloatListType) {
        if (!visited.put(curNode, curNode)) {
           foundCycle = true;
           break;
        }
        float value = Float.parseFloat((String) stringValues.get(i++));
        cas.setFeatureValue(curNode,floatHeadFeat,  CASImpl.float2int(value));
        curNode = cas.getHeapValue(curNode + cas.getFeatureOffset(floatTailFeat));
      }
    }
	
    if (foundCycle) {
        reportWarning("Found a cycle in an IntegerList.  List truncated to "
                  + i);
    }
    return first;
  }  
  
  public int updateFsList(int addr, List<String> stringValues, IntVector fsAddresses) throws SAXException  {
    int first = addr;
    int currLength = this.getLength(this.neFsListType, addr);
    boolean foundCycle = false;
    IntRedBlackTree visited = new IntRedBlackTree();
    int curNode = addr;
    int prevNode = 0;
    
    //if (currLength != stringValues.size() ) {  
	//   first = createFsList(stringValues, fsAddresses);
    int i=0;
    if (currLength < stringValues.size() ) {    
  	  while (cas.getHeapValue(curNode) == neFsListType) {
  	    if (!visited.put(curNode, curNode)) {
  	           foundCycle = true;
  	           break;
  	    }
  	    int value = Integer.parseInt(stringValues.get(i++));
        cas.setFeatureValue(curNode,fsHeadFeat, value);
        fsAddresses.add(curNode);
        prevNode = curNode;
  	    curNode = cas.getHeapValue(curNode + cas.getFeatureOffset(fsTailFeat));
  	  }
  	  //add nodes for remaining values
  	  if (i < stringValues.size()) {
  	    int emptyListFs = curNode; 
  	    //check that first is estringlisttype
  	    while (i < stringValues.size()) {
  	      int newNode = cas.ll_createFS(neFsListType);
  	      int value = Integer.parseInt((String) stringValues.get(i++));
          cas.setFeatureValue(newNode,fsHeadFeat, value);
          fsAddresses.add(newNode);
          cas.setFeatureValue(newNode, fsTailFeat, emptyListFs);
          cas.setFeatureValue(prevNode, fsTailFeat, newNode);
  	      prevNode = newNode;
  	    }
  	  }
    } else if (currLength > stringValues.size()) {	
        while (cas.getHeapValue(curNode) == neFsListType && i < stringValues.size()) {
   	      if (!visited.put(curNode, curNode)) {
   		    foundCycle = true;
   		    break;
   		  }
   	      int value = Integer.parseInt((String) stringValues.get(i++));
	      fsAddresses.add(curNode);
          cas.setFeatureValue(curNode,fsHeadFeat, value);
   		  curNode = cas.getHeapValue(curNode + cas.getFeatureOffset(fsTailFeat));
        } 
        int finalNode = curNode;
        //loop till we get a FS that is of type eStringListType
        while (cas.getHeapValue(curNode) == neFsListType) {
  	      if (!visited.put(curNode, curNode)) {
  	        foundCycle = true;
  	        break;
  	      //TODO throw exc
  	      }
  	      curNode = cas.getHeapValue(curNode + cas.getFeatureOffset(fsTailFeat));
        }
        //set the tail feature to eStringListType fs
      	cas.setFeatureValue(finalNode, fsTailFeat, curNode);
    } else {
      while (cas.getHeapValue(curNode) == neFsListType) {
        if (!visited.put(curNode, curNode)) {
           foundCycle = true;
           break;
        }
        int value = Integer.parseInt((String) stringValues.get(i++));
        cas.setFeatureValue(curNode,fsHeadFeat, value);
        fsAddresses.add(curNode);
        curNode = cas.getHeapValue(curNode + cas.getFeatureOffset(fsTailFeat));
      }
    }
    if (foundCycle) {
      reportWarning("Found a cycle in an IntegerList.  List truncated to "
                  + i);
    }
    return first;
  }  
  
  public int updateStringList(int addr, List<String> stringValues) throws SAXException   {
    int first = addr;
    boolean foundCycle = false;
    IntRedBlackTree visited = new IntRedBlackTree();
    int curNode = addr;
    int prevNode = 0;
    int currLength = this.getLength(this.neStringListType, addr);
    
    if (currLength < stringValues.size() ) {    
      int i =0;
	  while (cas.getHeapValue(curNode) == neStringListType) {
	    if (!visited.put(curNode, curNode)) {
	           foundCycle = true;
	           break;
	    }
	    String curValue = cas.getStringForCode(cas.getHeapValue(curNode
	              + cas.getFeatureOffset(stringHeadFeat)));
	    String newValue = stringValues.get(i++);
        if (!curValue.equals(newValue)) {		  
          cas.setFeatureValue(curNode, stringHeadFeat, cas.addString(newValue));
        }
        prevNode = curNode;
	    curNode = cas.getHeapValue(curNode + cas.getFeatureOffset(stringTailFeat));
	 }
	 //extend the list
	 if (i < stringValues.size()) {
	   int emptyListFs = curNode; 
	   while (i < stringValues.size()) {
	     int newNode = cas.ll_createFS(neStringListType);
	     String value = (String) stringValues.get(i++);
	     cas.setFeatureValue(newNode, stringHeadFeat, cas.addString(value));
	     cas.setFeatureValue(newNode, stringTailFeat, emptyListFs);
	     cas.setFeatureValue(prevNode,stringTailFeat, newNode);
	     prevNode = newNode;
	   }
	 }
    } else if (currLength > stringValues.size()) {
      int i=0;	
      while (cas.getHeapValue(curNode) == neStringListType && i < stringValues.size()) {
 	    if (!visited.put(curNode, curNode)) {
 		           foundCycle = true;
 		           break;
 		}
 		String curValue = cas.getStringForCode(cas.getHeapValue(curNode
 		              + cas.getFeatureOffset(stringHeadFeat)));
 		String newValue = (String)stringValues.get(i++);
        if (!curValue.equals(newValue)) {		  
           cas.setFeatureValue(curNode, stringHeadFeat, cas.addString(newValue));
        }
 		curNode = cas.getHeapValue(curNode + cas.getFeatureOffset(stringTailFeat));
      } 
      int finalNode = curNode;
      while (cas.getHeapValue(curNode) == neStringListType) {
	    if (!visited.put(curNode, curNode)) {
	      foundCycle = true;
	      break;
	      //TODO throw exc
	    }
	    curNode = cas.getHeapValue(curNode + cas.getFeatureOffset(stringTailFeat));
      }
      cas.setFeatureValue(finalNode, stringTailFeat, curNode);
    } else {
      int i =0;
      while (cas.getHeapValue(curNode) == neStringListType) {
        if (!visited.put(curNode, curNode)) {
           foundCycle = true;
           break;
        }
        String curValue = cas.getStringForCode(cas.getHeapValue(curNode
	              + cas.getFeatureOffset(stringHeadFeat)));
        String newValue = (String)stringValues.get(i++);
        if (!curValue.equals(newValue)) {		  
          cas.setFeatureValue(curNode, stringHeadFeat, cas.addString(newValue));
        }
        curNode = cas.getHeapValue(curNode + cas.getFeatureOffset(stringTailFeat));
      }
    }
    
    if (foundCycle) {
        reportWarning("Found a cycle in an IntegerList.  List truncated. ");
    }
    return first;
  }  
		  
	  
  
  private void reportWarning(String message) throws SAXException {
    if (this.logger != null) {
      logger.log(Level.WARNING, message);
    }
    if (this.eh != null) {
      this.eh.warning(new SAXParseException(message, null));
    }
  }
}
