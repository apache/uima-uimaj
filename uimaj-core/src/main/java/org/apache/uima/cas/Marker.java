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
* An object obtained from the <code>CAS</code> that is used to query when FSs were
* created or modified with respect to the marker. 
*/
public interface Marker {

 /**
  * @param fs 
  * @return true if a <code>FeatureStructure</code> was created after the mark represented by this <code>Marker</code> object. 
  */
 boolean isNew(FeatureStructure fs);
 
 /**
  * represented by this <code>Markere</code> is modified.
  * @param fs
  * @return true if a  <code>FeatureStructure</code> that existed prior to the mark
  */
 boolean isModified(FeatureStructure fs);
 
 /**
  * A Marker becomes invalid when the <code>CAS</code> from which it was obtained
  * is reset.
  * @return true if the Marker is still valid.
  */
 boolean isValid();

}

