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

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.jcas.cas.CommonList;
import org.apache.uima.jcas.cas.EmptyList;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.cas.FloatList;
import org.apache.uima.jcas.cas.IntegerList;
import org.apache.uima.jcas.cas.NonEmptyList;
import org.apache.uima.jcas.cas.StringList;
import org.apache.uima.util.Logger;
import org.apache.uima.util.MessageReport;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Utilities for dealing with CAS List types.
 * 
 * MERGE THIS WITH CommonList
 * 
 * There are many places operations-to-set-values are done to update feature values, 
 * which skip the corruption of indexes checking because
 * list elements cannot be part of an index key.
 *
 * Creation methods don't journal, these are guaranteed to be above the line.
 * 
 */
public class ListUtils {
  private static final List<String> EMPTY_LIST_STRING = Collections.emptyList();
  
  private static final AtomicInteger errorCount = new AtomicInteger(0);

    
  final CASImpl cas;

  final private Logger logger;

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
  }  


  public boolean isIntListType(FeatureStructure fs) {
    return fs instanceof IntegerList;
   }

  public boolean isFloatListType(FeatureStructure fs) {
    return fs instanceof FloatList;
  }

  public boolean isStringListType(FeatureStructure fs) {
    return fs instanceof StringList;
  }

  public boolean isFsListType(FeatureStructure fs) {
    return fs instanceof FSList;
  }

  public boolean isListType(FeatureStructure fs) {
    return isIntListType(fs) || isFloatListType(fs) || isStringListType(fs) || isFsListType(fs);
  }

  public int getLength(CommonList node) {
    return node.getLength();
  }

  static abstract class ListOutput {
    abstract void append(String item);
  }
  
  /**
   * Go thru a list, calling the ListOutput append method to append strings (to arrays, or string buffers)
   * Stop at the end node, or a null, or a loop (no error reported here)
   * @param curNode -
   * @param sharedData -
   * @param cds -
   * @param out -
   */
  public void anyListToOutput(
      CommonList curNode, 
      XmiSerializationSharedData sharedData, 
      CasSerializerSupport.CasDocSerializer cds,
      ListOutput out) {
    if (curNode == null) {
      return;
    }

//    final int type = cas.getHeapValue(curNode);
//    final int headFeat = getHeadFeatCode(type);
//    final int tailFeat = getTailFeatCode(type);
//    final int neListType = getNeListType(type);
    final Set<CommonList> visited = new IdentityHashMap<CommonList, Boolean>().keySet();

    while (curNode != null) {      
      if (curNode instanceof EmptyList) { 
        break;  // would be the end element.  
      }
      
      if (!visited.add(curNode)) {
        break;  // hit loop
      }
      
      out.append(curNode.get_headAsString());      
//      if (curNodeType == neStringListType) {
//        out.append(cas.getStringForCode(val));
//      }
//      if (curNodeType == neIntListType) {
//        out.append(Integer.toString(val));
//
//      } else if (curNodeType == neFloatListType) {
//        out.append(Float.toString(CASImpl.int2float(val)));
//
//      } else if (curNodeType == neFsListType) {
//        if (val == 0) {
//          if (sharedData != null) {
//            OotsElementData oed = sharedData.getOutOfTypeSystemFeatures(curNode);
//            if (oed != null) {
//              assert oed.attributes.size() == 1; //only the head feature can possibly be here
//              XmlAttribute attr = oed.attributes.get(0);
//              assert CAS.FEATURE_BASE_NAME_HEAD.equals(attr.name);
//              out.append(attr.value);
//            } else {
//              out.append("0");
//            }
//          } else {
//            out.append("0");
//          }
//        } else if (sharedData != null) {
//          out.append(sharedData.getXmiId(val));
//        } else {
//          out.append(Integer.toString(val));
//        }
//      } // end of Fs List type
      
      curNode = curNode.getTail();
    } // end of while loop
  } 
  
  public String[] anyListToStringArray(
      CommonList curNode, 
      XmiSerializationSharedData sharedData) throws SAXException {
    List<String> r = anyListToStringList(curNode, sharedData, null);
    return r.toArray(new String[r.size()]);
  }
  
  public List<String> anyListToStringList(
      CommonList curNode, 
      XmiSerializationSharedData sharedData, 
      CasSerializerSupport.CasDocSerializer cds) {
    if (curNode == null) {
      return EMPTY_LIST_STRING;
    }
    final List<String> list = new ArrayList<String>();
    anyListToOutput(curNode, sharedData, cds, new ListOutput() {
      @Override
      void append(String item) {
        list.add(item);
      }
    });
    return list;
  }

  //called for enqueueing 
//  public int[] fsListToAddressArray(int curNode) throws SAXException {
//    final int type = cas.getHeapValue(curNode);
//    
//    int length = getLength(type, curNode, neFsListType, fsTailFeat);
//    
//    int[] array = new int[length];
//
//    // now populate list
//    for (int i = 0; i < length; i++) {
//      array[i] = cas.getHeapValue(curNode + cas.getFeatureOffset(fsHeadFeat));
//      curNode = cas.getHeapValue(curNode + cas.getFeatureOffset(fsTailFeat));
//    }
//    if (foundCycle) {
//      reportWarning("Found a cycle in an FSList.  List truncated where cycle occurs.");
//    }
//    return array;
//  }

  public CommonList createListFromStringValues(List<String> stringValues, CommonList emptyNode) {
    CommonList lastCreatedNode = emptyNode;
    for (int i = stringValues.size() - 1; i >= 0; i--) {
      CommonList node = emptyNode.createNonEmptyNode(lastCreatedNode);
      node.setTail(lastCreatedNode);
      lastCreatedNode = node;
      node.setNewValueInExistingNode(stringValues.get(i));
    }
    return lastCreatedNode;
  }
    


//  public CommonList updateIntList(CommonList addr, List<String> stringValues) throws SAXException  {
//    return updateCommonList(addr, stringValues, updateIntActions);
//  }
//	  
//  public CommonList updateFloatList(CommonList addr, List<String> stringValues) throws SAXException  {
//    return updateCommonList(addr, stringValues, updateFloatActions);
//  }  
//  
//  public CommonList updateFsList(CommonList addr, List<String> stringValues, IntVector fsAddresses) throws SAXException  {
//    updateFsActions.fsAddresses = fsAddresses;
//    return updateCommonList(addr, stringValues, updateFsActions);
//  }  
//  
//  public CommonList updateStringList(CommonList addr, List<String> stringValues) throws SAXException   {
//    return updateCommonList(addr, stringValues, updateStringActions);
//  }  
		  
	private CommonList updateCommonList(CommonList node, List<String> stringValues) throws SAXException {
    final CommonList first = node;
    final int numberOfValues = stringValues.size();
    boolean foundCycle = false;
    
    final Set<CommonList> visited = new IdentityHashMap<CommonList, Boolean>().keySet();
    CommonList curNode = node;
    CommonList prevNode = null;
    int i = 0;
//    final int neListType = actions.neType;
//    final int tailFeat = actions.tailFeat; 
    final int currLength = node.getLength();
    
    // replace list value with values from string
    // reuse existing list cells
    // reuse existing end-of-list cell
    
    while (curNode instanceof NonEmptyList && i < numberOfValues) {
      if (!visited.add(curNode)) {
        foundCycle = true;
        break;
      }
      curNode.setNewValueInExistingNode(stringValues.get(i++));
      prevNode = curNode;
      curNode = curNode.getTail();
    }

    // if there are more values add them to the list
    if ((!foundCycle) && (currLength < numberOfValues) ) {    
      
      while (i < numberOfValues) {
        CommonList newNode = prevNode.insertNode();
        newNode.setNewValueInExistingNode(stringValues.get(i));
        prevNode = newNode;
      }
    } else if ((!foundCycle) && (currLength > numberOfValues)) {
      
      // if there are fewer values than in the list, truncate the list
      prevNode.setTail(prevNode.getEmptyNode());      
    } 

    if (foundCycle) {
      reportWarning("While updating a " + first.getClass().getSimpleName() + ", a cycle was found; the list is truncated at that point.");
    }
    return first;
	}
  
  private void reportWarning(String message) throws SAXException {
    MessageReport.decreasingWithTrace(errorCount, message, logger);
    if (this.eh != null) {
      this.eh.warning(new SAXParseException(message, null));
    }
  }
}
