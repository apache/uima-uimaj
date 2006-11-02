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

package org.apache.uima.cas;



/**
 * The base interface extended by all CAS (Common Analysis System) interfaces in 
 * the UIMA SDK.  A CAS is an object that provides access to an artifact and
 * metadata about that artifact.  Analysis Components, such as Annotators, read from a 
 * CAS interface in order to do their analysis and may write new metadata back to the 
 * CAS interface.
 * <p>
 * The UIMA SDK provides the CAS interfaces {@link org.apache.uima.jcas.impl.JCas} and 
 * {@link org.apache.uima.cas.CAS}, but in future versions, other CAS interfaces may be 
 * available.   
 */
public interface AbstractCas
{
//  /** 
//   * Gets the ID for this CAS, which is unique within the CasManager that owns it.
//   * 
//   * @return the ID for this CAS.  This is always a positive integer.
//   */
//  int getID();
// 
//  /** 
//   * Sets the ID for this CAS, which must be unique within the CasManager that owns it.
//   * TODO: this is a dangerous method to allow user code to call.  Come to think of it,
//   * most or all methods here are for framework management of the CAS.  Can we hide this?
//   * @param aID the ID for this CAS.  This should always be a positive integer.
//   */
//  void setID(int aID);
//  
//  /**
//   * Gets the parent ID for this CAS.  If this CAS is a segment of another CAS, this
//   * will be set to the ID of that CAS.  If this CAS is not a segment of any CAS, this
//   * will be 0.
//   * 
//   * @return the parent CAS ID, or 0 if none
//   */
//  int getParentID();
//
//  /**
//   * Sets the parent ID for this CAS.  If this CAS is a segment of another CAS, this
//   * should be set to the ID of that CAS.  If this CAS is not a segment of any CAS, this
//   * should be set to -1.
//   * 
//   * @param aParentID the parent CAS ID, or -1 if none
//   */
//  void setParentID(int aParentID);
//  
//  /** 
//   * Serializes the data from this CAS's internal representation to the standard
//   * XMI representation, by calling events on SAX ContentHandler.  All CAS implementations
//   * are required to support serialization to XMI in order to be interoperable with
//   * other CAS implementations.
//   * 
//   * @param aContentHandler a ContentHandler implementation, provided by the caller,
//   *   on which this CAS will call events that represent the XMI serialization of the
//   *   data stored in this CAS.
//   */
//  void toXmiSax(ContentHandler aContentHandler);
//    
//  
//  /** 
//   * Returns a SAX ContentHandler that can be used to deserialize XMI into
//   * this CAS's internal representation.  All CAS implementations
//   * are required to support deserialization from XMI in order to be interoperable with
//   * other CAS implementations.
//   * 
//   * @return aContentHandler a ContentHandler implementation on which the caller should
//   *   call events that represent an XMI document.  This ContentHandler will process
//   *   those events by storing data in this CAS's internal representation.
//   */  
//  ContentHandler getXmiSaxContentHandler();
//
//  
//  /** 
//   * Initializes this CAS from another CAS.  This method may copy data from 
//   * <code>aAnotherCas</code> into this CAS's internal representation, or may 
//   * simply store a handle to <code>aAnotherCas</code> so that this CAS may function
//   * as an adapter that presents a different interface to the same underlying data
//   * in <code>aAnotherCas</code>.
//   * <p> 
//   * Note that it is always possible to transfer data from any CAS to any other CAS by
//   * using the {@link #toXmiSax(ContentHandler)} and {@link #getXmiSaxContentHandler()} methods.
//   * 
//   * @param aAnotherCas a CAS instance, the data from which should be used to initialie
//   *   this CAS.
//   */
//  void initializeFrom(AbstractCas aAnotherCas);
//
//  
//  /** 
//   * Gets the View associated with the specified Subject of Analysis (Sofa).
//   * Not all CAS implementations may support this operation.
//   * 
//   * @param aSofaID ID of the Subject of Analysis (Sofa)
//   * 
//   * @return a View associated with the specified Sofa.  The View implements the same
//   *   interfaces that this CAS does.
//   *   
//   * @throws org.apache.uima.UIMA_UnsupportedOperationException if this CAS implementation 
//   *    does not support views.
//   */
//  AbstractCas getView(String aSofaID);
//
  /**
   * Indicates that the caller is done using this CAS.  Some CAS instances
   * may be pooled, in which case this method returns this CAS to the pool
   * that owns it. 
   */
  void release();  
}
