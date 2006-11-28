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

package org.apache.uima.adapter.vinci;

import java.io.IOException;

import org.xml.sax.SAXException;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.impl.OutOfTypeSystemData;
import org.apache.vinci.transport.document.AFrame;

/**
 * Interface for a component that can serialize a CAS to a Vinci Frame.
 * 
 * 
 */
public interface VinciCASSerializer {

  /**
   * Serializes a CAS to a Vinci frame and adds that frame to the given parent frame.
   * 
   * @param aCAS
   *          the CAS to be serialized
   * @param aParentFrame
   *          the Vinci Frame to which to add the serialized CAS
   * @param aOutOfTypeSystemData
   *          data that does not conform to CAS's type system but should be included in XCAS anyway.
   *          May be null.
   */
  public void serialize(CAS aCAS, AFrame aParentFrame, OutOfTypeSystemData aOutOfTypeSystemData)
          throws IOException, SAXException;

}
