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

import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.jcas.JCas;

public abstract class StringList extends CommonList {

	// Never called.
	protected StringList() { // Disable default constructor
	}

	public StringList(JCas jcas) {
		super(jcas);
	}

  /**
   * used by generator
   * Make a new AnnotationBase
   * @param c -
   * @param t -
   */

  public StringList(TypeImpl t, CASImpl c) {
    super(t, c);
  }

  public String getNthElement(int i) {
    return ((NonEmptyStringList) getNonEmptyNthNode(i)).getHead();
  }
   
  public NonEmptyStringList createNonEmptyNode(CommonList tail) {
    NonEmptyStringList node = new NonEmptyStringList(this._typeImpl, this._casView);
    node.setTail((StringList) tail);
    return node;
  }
  
  public NonEmptyStringList createNonEmptyNode() { return createNonEmptyNode(null); }

  /* (non-Javadoc)
   * @see org.apache.uima.jcas.cas.CommonList#getEmptyNode()
   */
  @Override
  public EmptyStringList getEmptyNode() {
    return EmptyStringList.singleton;
  }
  
 
  /* (non-Javadoc)
   * @see org.apache.uima.jcas.cas.CommonList#get_headAsString()
   */
  @Override
  public String get_headAsString() {
    return ((NonEmptyStringList)this).getHead();
  }

  /* (non-Javadoc)
   * @see org.apache.uima.jcas.cas.CommonList#getTail()
   */
  @Override
  public CommonList getTail() {
    return ((NonEmptyStringList)this).getTail();
  }

  /* (non-Javadoc)
   * @see org.apache.uima.jcas.cas.CommonList#setTail(org.apache.uima.jcas.cas.CommonList)
   */
  @Override
  public void setTail(CommonList v) {
    ((NonEmptyStringList)this).setTail(v);
  }
  

}
