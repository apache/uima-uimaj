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

package org.apache.uima.resource.metadata;

import org.apache.uima.UIMA_UnsupportedOperationException;

/**
 * A precondition on the MIME Type of the entity. This interface is a kind of
 * <code>SimplePrecondition</code> to be used as a convenience.
 * 
 * 
 */
public interface MimeTypePrecondition extends SimplePrecondition {
  /**
   * Gets the MIME types that satisfy this precondition.
   * <p>
   * Note that if the document's MIME Type is unknown, the value of the
   * {@link #setDefault(boolean) default} property determines whether this precondition is
   * satisfied.
   * 
   * @return the MIME Types that satisfy this precondition
   */
  public String[] getMimeTypes();

  /**
   * Sets the MIME types that satisfy this precondition.
   * <p>
   * Note that if the document's MIME Type is unknown, the value of the
   * {@link #setDefault(boolean) default} property determines whether this precondition is
   * satisfied.
   * 
   * @param aMimeTypes
   *          the MIME Types that satisfy this precondition
   * 
   * @throws UIMA_UnsupportedOperationException
   *           if this <code>MetaDataObject</code> is not modifiable.
   */
  public void setMimeTypes(String[] aMimeTypes);
}
