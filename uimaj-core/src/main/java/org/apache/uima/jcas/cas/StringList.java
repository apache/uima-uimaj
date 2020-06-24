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

import java.util.Collections;
import java.util.Iterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.internal.util.Misc;
import org.apache.uima.jcas.JCas;

public abstract class StringList extends TOP implements CommonList, Iterable<String> {

	/* (non-Javadoc)
   * @see java.lang.Iterable#iterator()
   */
  @Override
  public Iterator<String> iterator() {
//IC see: https://issues.apache.org/jira/browse/UIMA-5207
    return Collections.emptyIterator(); // overridden by NonEmptyStringList
  }

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
   
  public NonEmptyStringList createNonEmptyNode() {
//IC see: https://issues.apache.org/jira/browse/UIMA-4697
    NonEmptyStringList node = new NonEmptyStringList(this._casView.getTypeSystemImpl().stringNeListType, this._casView);
    return node;
  }  
  
  public NonEmptyStringList push(String item) {
//IC see: https://issues.apache.org/jira/browse/UIMA-2147
    return new NonEmptyStringList(this._casView.getJCasImpl(), item, this);
  } 
  
  @Override
  public EmptyStringList emptyList() {
//IC see: https://issues.apache.org/jira/browse/UIMA-5584
    return this._casView.emptyStringList();
  }

  /**
   * Create an StringList from an existing array of Strings
   * @param jcas the JCas to use
   * @param a the array of Strings to populate the list with
   * @return an StringList, with the elements from the array
   */
  public static StringList create(JCas jcas, String[] a) {
//IC see: https://issues.apache.org/jira/browse/UIMA-5584
    StringList stringList = jcas.getCasImpl().emptyStringList();   
    for (int i = a.length - 1; i >= 0; i--) {
      stringList = stringList.push(a[i]);
    }   
    return stringList;
  }
  
  /**
   * @return a stream over this FSList
   */
  public Stream<String> stream() {
//IC see: https://issues.apache.org/jira/browse/UIMA-5620
    return StreamSupport.stream(spliterator(), false);
  }
 
  public boolean contains(String v) {
    StringList node = this;
    while (node instanceof NonEmptyStringList) {
      NonEmptyStringList n = (NonEmptyStringList) node;
      if (Misc.equalStrings(v, n.getHead())) {
        return true;
      }
      node = n.getTail();
    }
    return false;
  }
}
