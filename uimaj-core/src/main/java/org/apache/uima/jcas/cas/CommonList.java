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

import java.util.IdentityHashMap;
import java.util.Set;

import org.apache.uima.cas.CASRuntimeException;

/**
 * This class is the super class of list nodes (both empty and non empty)
 */
public interface CommonList {

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
			node = node.getTail();
      if (node instanceof EmptyList) {
        throw new CASRuntimeException(CASRuntimeException.JCAS_GET_NTH_PAST_END, originali);
      }
		}
	}
	
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
	 * @param fs - a list element
	 * @return the number of items in the list
	 */
	default int getLength() {
	  // detect loops
	  final Set<CommonList> visited = new IdentityHashMap<CommonList, Boolean>().keySet();
	  
	  int length = 0;
	  CommonList node = this;
	  while (node instanceof NonEmptyList) {
	    length ++;
	    visited.add(node);
	    node = node.getTail();
	    if (visited.contains(node)) {
	      break;
	    }
	  }
	  return length;
	}
	 
	static void setNewValueInExistingNode(CommonList node, String v) {
//	  node.setHead(null);
    throw new CASRuntimeException(); // not yet impl
	}

	CommonList createNonEmptyNode();
	CommonList createNonEmptyNode(CommonList tail);
	CommonList getEmptyNode();   // returns a shared constant empty node
	String get_headAsString();
	/**
	 * insert a new nonempty node following this node
	 * @return the new node
	 */
	default CommonList insertNode() {
	  assert(this instanceof NonEmptyList);
	  CommonList newNode = createNonEmptyNode();
	  CommonList tail = getTail();
	  setTail(newNode);
	  newNode.setTail(tail);
	  return newNode;	  
	}

  default CommonList getTail() {
    throw new UnsupportedOperationException();
  }

  default void setTail(CommonList v) {
    throw new UnsupportedOperationException();
  }

}
