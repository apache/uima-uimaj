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

package org.apache.vinci.transport;

import org.apache.vinci.debug.Debug;

/**
 * Base class for components that can be included in Frame documents. Descendents currently
 * include only FrameLeaf and Frame.
 */
public class FrameComponent {
  /**
   * Set the attributes (replacing any previous ones) assocated with this
   * Frame component.  Default implementation does nothing since
   * attributes are not supported by default.
   */
  public void setAttributes(Attributes s) {
    Debug.p("WARNING: Attempt to set attributes of FrameComponent "
        + "which does not support them. Attribute info will be lost.");
  }

  /**
   * Get the attributes associated with this FrameComponent.
   * 
   * @return The set of attributes associated with this FrameComponent. This method may return
   * "null" to indicate there are no attributes.  It could however return an empty Attribute set
   * depending on the implementation.  
   */
  public Attributes getAttributes() {
    return null;
  }

}
