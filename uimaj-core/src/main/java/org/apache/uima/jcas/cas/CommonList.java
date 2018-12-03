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

package org.apache.uima.jcas.cas;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.impl.CasSerializerSupport;
import org.apache.uima.cas.impl.XmiSerializationSharedData;
import org.apache.uima.cas.impl.XmiSerializationSharedData.OotsElementData;
import org.apache.uima.internal.util.XmlAttribute;
import org.apache.uima.internal.util.function.Consumer_withSaxException;
import org.xml.sax.SAXException;

/**
 * This class is the super class of list nodes (both empty and non empty)
 * 
 */
public interface CommonList extends FeatureStructure {

  public final static String _FeatName_head = "head";
  public final static String _FeatName_tail = "tail";

  static final List<String> EMPTY_LIST_STRING = Collections.emptyList();

  /**
   * Get the nth node.
   * @param i -
   * @return the nth node, which may be an "empty" node
   */
	default CommonList getNthNode(int i) {
	  if (this instanceof EmptyList) {
			throw new CASRuntimeException(CASRuntimeException.JCAS_GET_NTH_ON_EMPTY_LIST, "EmptyList");
		}
		if (i < 0) {
			throw new CASRuntimeException(CASRuntimeException.JCAS_GET_NTH_NEGATIVE_INDEX, i);
		}
		int originali = i;
		CommonList node = this;

		for (;; i--) {
			if (i == 0) {
				return node;
			}
			node = node.getCommonTail();
      if (node instanceof EmptyList) {
        throw new CASRuntimeException(CASRuntimeException.JCAS_GET_NTH_PAST_END, originali);
      }
		}
	}
	
	/**
	 * Like GetNthNode, but throws exception if empty
	 * @param i - 
	 * @return -
	 */
	default CommonList getNonEmptyNthNode(int i) {
	  CommonList node = getNthNode(i);
    if (node instanceof EmptyList) {
	    throw new CASRuntimeException(CASRuntimeException.JCAS_GET_NTH_PAST_END, i);
    }
    return node;
	}
		
	/**
	 * length of a list, handling list loops.
	 * returns the number of unique nodes in the list
	 * @return the number of items in the list
	 */
	default int getLength() {
	  final int[] length = {0};
    walkList(n -> length[0]++, () -> {});
	  return length[0];
	}
	
	/**
	 * Walks a list, executing the consumer on each element.
	 * If a loop is found, the foundloop method is run.
	 * @param consumer  a Consumer with Sax Exception 
	 * @param foundLoop run if a loop happens
	 * @throws SAXException -
	 */
  default void walkList_saxException(Consumer_withSaxException<NonEmptyList> consumer, Runnable foundLoop)
      throws SAXException {
    final Set<CommonList> visited = Collections.newSetFromMap(new IdentityHashMap<>());
    CommonList node = this;
    while (node instanceof NonEmptyList) {
      consumer.accept((NonEmptyList) node);
      node = node.getCommonTail();
      if (node == null) {
        break;
      }
      if (visited.contains(node) && foundLoop != null) {
        foundLoop.run();
      }
    }
  }

  /**
   * Walks a list, executing the consumer on each element.
   * If a loop is found, the foundloop method is run.
   * @param consumer  a Consumer (with no declared exceptions) 
   * @param foundLoop run if a loop happens
   */
  default void walkList(Consumer<NonEmptyList> consumer, Runnable foundLoop) {
    final Set<CommonList> visited = Collections.newSetFromMap(new IdentityHashMap<>());
    CommonList node = this;
    while (node instanceof NonEmptyList) {
      consumer.accept((NonEmptyList) node);
      node = node.getCommonTail();
      if (node == null) {
        break;
      }
      if (visited.contains(node) && foundLoop != null) {
        foundLoop.run();
      }
    }
  }

  
	/**
	 * Creates a non empty node
	 * @return a new non empty node
	 */
	CommonList createNonEmptyNode();
	
	/**
	 * @return a shared instance of the empty node.
	 */
	CommonList emptyList();
	
	/**
	 * Internal use
   * overridden in nonempty nodes
	 * Return the head value of a list as a string suitable for serialization.
	 * 
	 * For FeatureStructure values, return the _id.
	 * @return value suitable for serialization
	 */
	default String get_headAsString() {
	  throw new UnsupportedOperationException();
	};
	
	/**
	 * Internal use
   * overridden in nonempty nodes
   * used when deserializing
   * @param v value to set, as a string
   */
  default void set_headFromString(String v) {
    throw new UnsupportedOperationException();
  }
  
	/**
	 * insert a new nonempty node following this node
	 * @return the new node
	 */
	default CommonList insertNode() {
	  assert(this instanceof NonEmptyList);
	  CommonList newNode = createNonEmptyNode();
	  CommonList tail = getCommonTail();
	  setTail(newNode);
	  newNode.setTail(tail);
	  return newNode;	  
	}
	
	/**
	 * Creates a new node and pushes it onto the front of the existing node
	 * @return the new node
	 */
	default CommonList pushNode() {
	  CommonList newNode = createNonEmptyNode();
	  newNode.setTail(this);
	  return newNode;
	}

	/**
	 * default impl for empty and nonempty lists
	 * @return - instance of CommonList
	 * 
	 * This has to be named differently from getTail, otherwise the 
	 * "default" method in the interface appears as declared method in reflection named getTail which conflicts
	 * with the one returning a specific typed value
	 */
  default CommonList getCommonTail() {
    if (this instanceof NonEmptyFloatList) {
      return ((NonEmptyFloatList)this).getTail();
    }
    if (this instanceof NonEmptyIntegerList) {
      return ((NonEmptyIntegerList)this).getTail();
    }
    if (this instanceof NonEmptyStringList) {
      return ((NonEmptyStringList)this).getTail();
    }
    if (this instanceof NonEmptyFSList) {
      return ((NonEmptyFSList)this).getTail();
    }
    throw new UnsupportedOperationException();
  }

  // there is no common getTail, because each one has a different return type
  // the impl of setTail(CommonList v) is split:
  //   the default impl throws UnsupportedOperationException;
  //   each kind of non-empty list class has its own impl
  /**
   * sets the tail of this node 
   * @param v the tail
   */
  default void setTail(CommonList v) {
    throw new UnsupportedOperationException();
  }

  /**
   * Internal Use.
   * List to String for XMI and JSON serialization, for the special format where
   *   all the list elements are in one serialized item
   * 
   * Go thru a list, calling the ListOutput append method to append strings (to arrays, or string buffers)
   * Stop at the end node, or a null, or a loop (no error reported here)
   * @param sharedData - 
   * @param cds - 
   * @param out - a Consumer of strings
   */
  default void anyListToOutput(XmiSerializationSharedData sharedData, CasSerializerSupport.CasDocSerializer cds, Consumer<String> out) {

//    final int type = cas.getHeapValue(curNode);
//    final int headFeat = getHeadFeatCode(type);
//    final int tailFeat = getTailFeatCode(type);
//    final int neListType = getNeListType(type);
    final Set<CommonList> visited = Collections.newSetFromMap(new IdentityHashMap<>());

    CommonList curNode = this;
    while (curNode != null) {      
      if (curNode instanceof EmptyList) { 
        break;  // would be the end element.  
      }
      
      if (!visited.add(curNode)) {
        break;  // hit loop
      }
      
//      out.accept(curNode.get_headAsString());
      
      if (curNode instanceof NonEmptyFSList) {
        TOP val = ((NonEmptyFSList)curNode).getHead();
        if (val == null) {
          if (sharedData != null) {
            OotsElementData oed = sharedData.getOutOfTypeSystemFeatures((TOP)curNode);
            if (oed != null) {
              assert oed.attributes.size() == 1; //only the head feature can possibly be here
              XmlAttribute attr = oed.attributes.get(0);
              assert CAS.FEATURE_BASE_NAME_HEAD.equals(attr.name);
              out.accept(attr.value);
            } else {
              out.accept("0");
            }
          } else {
            out.accept("0");
          }
        } else {
          out.accept(cds.getXmiId(val));
        }
        
      } else {
        // is instance of float, integer, or string list
        out.accept(curNode.get_headAsString());
      }
      

      
      curNode = curNode.getCommonTail();
    } // end of while loop
  } 
  
  /**
   * Internal use
   * @param sharedData -
   * @param cds -
   * @return -
   */
  default List<String> anyListToStringList(XmiSerializationSharedData sharedData, CasSerializerSupport.CasDocSerializer cds) {
    final List<String> list = new ArrayList<>();
    anyListToOutput(sharedData, cds, s -> list.add(s)); 
    return list;
  }
  
  /**
   * @return true if this object represents an empty list
   */
  default boolean isEmpty() {
    return this instanceof EmptyList;
  }
    
}
