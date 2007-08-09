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

package org.apache.vinci.transport.vns.service;

import java.util.Hashtable;

import org.apache.vinci.transport.Frame;
import org.apache.vinci.transport.VinciFrame;

public class ServiceAlias implements ServiceInterface {

  protected String name, target;

  public ServiceAlias(String name, String target) {
    this.name = name;
    this.target = target;
  }

  public Frame toFrame() {
    Frame F = new VinciFrame(); // hack around the fact that Frame is an abstract class

    F.fadd("NAME", name);
    F.fadd("TARGET", target);

    return F;
  }

  public String toXML() {
    return toXML(0);
  }

  public String toXML(int offset) {
    String indent = "";
    while (offset > 0)
      indent += " ";

    String result = "";
    result += indent + "<SERVICE>\n";
    result += indent + "   <NAME>" + name + "</NAME>\n";
    result += indent + "   <TARGET>" + target + "</TARGET>\n";
    result += indent + "</SERVICE>\n";

    return result;
  }

  public static boolean isAlias(Object o) {
    return (o instanceof ServiceAlias);
  }

  public Object toService(Hashtable H) {
    if (H.get("TARGET") != null)
      return new ServiceAlias((String) H.get("NAME"), (String) H.get("TARGET"));
    else
      return new Service(H);
  }

  public Object getAttr(String name) {
    name = name.toLowerCase().trim();
    if (name.equals("name"))
      return name;
    if (name.equals("target"))
      return target;
    return null;
  }
}
