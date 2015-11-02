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

import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.jcas.JCas;

public abstract class FSList extends TOP implements CommonList {

	// Never called.
	protected FSList() {// Disable default constructor
	}

	public FSList(JCas jcas) {
		super(jcas);
	}

  /**
  * used by generator
  * Make a new AnnotationBase
  * @param c -
  * @param t -
  */

   public FSList(TypeImpl t, CASImpl c) {
     super(t, c);
   }
	
  public NonEmptyFSList createNonEmptyNode(CommonList tail) {
    NonEmptyFSList node = new NonEmptyFSList(this._typeImpl, this._casView);
    node.setTail((FSList) tail);
    return node;
  }
  
  public NonEmptyFSList createNonEmptyNode() { return createNonEmptyNode(null); }

  /* (non-Javadoc)
   * @see org.apache.uima.jcas.cas.CommonList#getEmptyNode()
   */
  @Override
  public EmptyFSList getEmptyNode() {
    return EmptyFSList.singleton;
  }
  
  /* (non-Javadoc)
   * @see org.apache.uima.jcas.cas.CommonList#get_headAsString()
   */
  @Override
  public String get_headAsString() {
    throw new CASRuntimeException(); // not yet impl
//    return ((NonEmptyFSList)this).getHead().toString();
  }

  
}
