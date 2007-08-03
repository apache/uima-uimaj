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

package org.apache.vinci.transport.vns.client;

import org.apache.vinci.transport.context.VinciContext;

/**
 * Deprecated class whose functions are now entirely provided by VinciContext.
 */
public final class VNSConfig {

  public static VNSConfig INSTANCE = new VNSConfig();

  /**
   * Return the VNS hostname. When this class is first loaded, it will set the hostname from the
   * java property VNS_HOST. To set the VNS_HOST using this java property, you must therefore
   * specify the property before the class is ever referenced, e.g. through the command-line
   * property option -DVNS_HOST=[hostname], or by calling System.setProperty("VNS_HOST", [hostname])
   * before ever invoking any Vinci client code. Otherwise, you can set the hostname using the
   * setHost() method provided by this class.
   * 
   * @deprecated Use VinciContext.getVNSHost() instead.
   * @throws IllegalStateException
   *           if no VNS host has been specified.
   */
  public String getHost() {
    return VinciContext.getGlobalContext().getVNSHost();
  }

  /**
   * Return the VNS listener port. When this class is first loaded, it will attempt to set the port
   * number from the java property VNS_PORT. To set the port using this java property, you must
   * therefore specify the VNS_PORT property before the class is ever referenced, e.g. through the
   * command-line property option -DVNS_PORT=[hostname], or by calling
   * System.setProperty("VNS_PORT", [hostname]) before ever invoking any Vinci client code.
   * Otherwise, the port will default to 9000. You can override this default (or any
   * property-specified value) by calling the setPort() method provided by this class.
   * 
   * @deprecated Use VinciContext.getVNSPort() instead.
   */
  public int getPort() {
    return VinciContext.getGlobalContext().getVNSPort();
  }

  /**
   * Set the VNS hostname. Explicitly setting the VNS hostname using this method will override any
   * hostname set via the VNS_HOST java property.
   * 
   * @deprecated Use VinciContext.setVNSHost() instead.
   */
  public void setHost(String h) {
    VinciContext.getGlobalContext().setVNSHost(h);
  }

  /**
   * Set the VNS port. Explicitly setting the VNS hostname using this method will override any port
   * number set via the VNS_PORT java property.
   * 
   * @deprecated Use VinciContext.setVNSPort() instead.
   */
  public void setPort(int p) {
    VinciContext.getGlobalContext().setVNSPort(p);
  }

}
