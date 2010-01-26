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

import java.net.InetAddress;
import java.util.Enumeration;
import java.util.Hashtable;

import org.apache.vinci.transport.Frame;
import org.apache.vinci.transport.VinciFrame;

public class Service implements ServiceInterface {

  // Attributes defined
  protected int port = -1, minport, maxport, instance;

  protected String realhost, host, qhost, name, level;

  public int actualLevel = -2;

  protected boolean meta;

  protected Hashtable dict;

  public Service(Hashtable H) {
    dict = H;

    constructAttr();
  }

  // Extract the relevant stuff from the Hashtable
  private void constructAttr() {
    name = (String) dict.get("NAME");

    instance = atoi((String) dict.get("INSTANCE"));

    level = lvl((String) dict.get("LEVEL"));

    actualLevel = -2; // Unassigned as yet

    minport = atoi((String) dict.get("MINPORT"));
    maxport = atoi((String) dict.get("MAXPORT"));
    port = atoi((String) dict.get("PORT"));

    host = (String) dict.get("HOST");
    qhost = (String) dict.get("FDQN");

    meta = dict.get("META") != null;

    realhost = (String) dict.get("IP");

    if (realhost == null && host != null)
      realhost = getHostByAddr(host);

    if (realhost == null && qhost != null)
      realhost = getHostByAddr(qhost);

    if (realhost == null)
      throw new RuntimeException("No IP specified for the service");
  }

  // Equivalent to the scalar conversion of String to int in Python
  private int atoi(String s) {
    if (s == null)
      return 0;

    try {
      return Integer.parseInt(s);
    } catch (Exception e) {
      return 0;
    }
  }

  // Equivalent to the __getattr__ method
  public Object getAttr(String name) {

    if (name.equals("name"))
      return this.name;

    if (name.equals("host"))
      return this.host;

    if (name.equals("realhost"))
      if (realhost == null)
        return (realhost = getHostByAddr(host));
      else
        return realhost;

    if (name.equals("qhost"))
      return (qhost = getHostByAddr(host));

    if (name.equals("instance"))
      return new Integer(instance);

    if (name.equals("level")) {
      if (level == null || level.equals(""))
        level = "0";
      return new String(level);
    }

    if (name.equals("meta")) {
      Object o = dict.get(name.toUpperCase());
      if (o instanceof Frame) {
        return ((Frame) o).toXML();
      }
      return o;
    }

    // For exact equivalent
    // throw new Exception("Attribute Error");

    name = name.toUpperCase();

    return dict.get(name); // Allows extraction of all the parameters
  }

  // Equivalent to the __cmp__ method
  public boolean equals(Object o) {
    if (!(o instanceof Service))
      return false;

    return (o.toString().equals(toString()));
  }

  public int hashCode() {
    return toString().hashCode();
  }

  // Equivalent to the updatePort method
  public void updatePort() {
    if (port == -1)
      port = minport;

    port++;

    if (port > maxport || port < minport)
      port = minport;

    update();
  }

  // Update the settings in the hashtable
  public void update() {
    if (level != null)
      dict.put("LEVEL", level);
    if (realhost != null)
      dict.put("IP", realhost);
    dict.put("INSTANCE", new Integer(instance));
    dict.put("MINPORT", new Integer(minport));
    dict.put("MAXPORT", new Integer(maxport));
    dict.put("PORT", new Integer(port));
    dict.put("IP", getHostByName(host));
  }

  // Trivial implementation - should return the DNS of the host
  private String getHostByName(String host) {
    if (realhost != null)
      return realhost;
    return getHostByAddr(host);
  }

  // Trivial implementation - should return the IP of the host
  private String getHostByAddr(String host) {
    try {
      return InetAddress.getByName(host).getHostAddress();
    } catch (Exception e) {
      return null;
    }
  }

  // Almost equivalent to _lvl
  // Upto the other classes to parse this properly
  private String lvl(String s) {
    if (s == null)
      return "0"; // Returns "0" as the default level if none specified
    return s;
  }

  // Equivalent to toFrame()
  // Need to check out the sources for this method

  public Frame toFrame() {
    Frame F = new VinciFrame(); // hack around the fact that Frame is an abstract class

    Object[] keys = dict.keySet().toArray();
    String S;
    Object O;

    for (int i = 0; i < keys.length; i++) {
      S = (String) keys[i];

      O = dict.get(S);
      // if (Frame.isFrame(O))
      if (1 == 0) // hack to get it to compile - can't find the isFrame() method
        F.fadd(S, (Frame) O);
      else
        F.fadd(S, O.toString());
    }

    return F;
  }

  // Equivalent to the toXML() method
  public String toXML() {
    update();

    StringBuffer str = new StringBuffer();
    str.append("<SERVICE>\n");
    Enumeration keys = dict.keys();

    Object key, value;

    while (keys.hasMoreElements()) {
      key = keys.nextElement();

      value = dict.get(key);
      if (value instanceof Frame) {
        str.append("   " + ((Frame) value).toXML());
      } else {
        str.append("   <" + xmlquote(key.toString()) + ">" + xmlquote(value.toString()) + "</"
                + xmlquote(key.toString()) + ">\n");
      }
    }
    str.append("</SERVICE>\n");

    return str.toString();
  }

  // Equivalent to Frame.xmlquote
  // Replaces all the necessary characters in the string with appropriate entity refs
  // or encloses the whole thing in a CDATA section (if there are more than 4 refs)
  public static String xmlquote(String s) {
    int i = 0;
    StringBuffer result = new StringBuffer();
    for (int j = 0; j < s.length() && i < 4; j++) {
      if (isEntity(s.charAt(j))) {
        i++;
        result.append(entityRef(s.charAt(j)));
      } else
        result.append(s.charAt(j));
    }
    if (i > 3) {
      return "<![CDATA[" + s + "]]>";
    }

    return result.toString();
  }

  static boolean isEntity(char c) {
    return (c == '<' || c == '>' || c == '&' || c == '"' || c == '\'');
  }

  static String entityRef(char c) {
    switch (c) {
      case '<':
        return "&lt;";
      case '>':
        return "&gt;";
      case '&':
        return "&amp;";
      case '"':
        return "&quot;";
      case '\'':
        return "&apos;";
      default:
        return "c";
    }
  }

  public String toString() {
    return "NAME/REALHOST/LEVEL/INSTANCE=" + name + "/" + getHostByName(host) + "/" + level + "/"
            + instance;
  }
}
