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

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Hashtable;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import org.apache.vinci.transport.document.XMLToVinci;

/**
 * Primary interface into the services database.
 * 
 */
public class ServiceRegistry {

  /* Constants */
  static int minAutoPort = 10000;

  static int maxAutoPort = 11000;

  static int sizeAutoPort = 10;

  static int MAX_ALIAS_LINKS = 25; // Make number of continuous alias links

  /* Instance vars */
  ServiceTree services = null;

  Hashtable bindings = null;

  /* constructor */
  public ServiceRegistry() {
    services = new ServiceTree();
    bindings = new Hashtable();
  }

  /* Methods */

  /* Method to add a binding */
  public void addBinding(Service S) {
    String ip = S.realhost;

    ArrayList v = null;
    if (ip != null)
      v = (ArrayList) bindings.get(ip);
    if (v == null)
      v = new ArrayList();

    v.add(new PortRange(S.minport, S.maxport));
    bindings.put(ip, v);
  }

  /* Method to remove a binding */
  public void delBinding(Service S) {
    ArrayList v = (ArrayList) bindings.get(S.realhost);
    if (v == null)
      return;

    v.remove(new PortRange(S.minport, S.maxport));

    if (v.size() < 1)
      bindings.remove(S.realhost);
  }

  /* Method to check for port range conflict */
  public boolean checkConflict(String ip, int min, int max) {
    ArrayList v = (ArrayList) bindings.get(ip);
    if (v == null) {
      return false;
    }
    PortRange p;
    for (int i = 0; i < v.size(); i++) {
      p = (PortRange) v.get(i);
      if ((p.minPort <= min && min <= p.maxPort) || (p.minPort <= max && max <= p.maxPort)) {
        return true;
      }
    }

    return false;
  }

  /* Method to check for binding */
  public boolean checkBinding(Service S) {
    if (S.name == null || S.name.trim().equals("")) {
      pr("Service not added: Null name");
      return false;
    }

    int mylevel = getLevel(S.name, S.level);
    S.level = "" + mylevel;

    if (services.find(S.name) != null) {
      Object O = services.find(S.name);
      if (!(O instanceof ServiceAlias)) {
        Object[] objs = ((ServiceStack) O).get("None");
        Service srv;
        for (int i = 0; i < objs.length; i++) {
          srv = (Service) objs[i];
          if (srv == null)
            continue;
          if (srv.realhost.equals(S.realhost) && srv.level.equals(S.level)
                  && srv.instance == S.instance) {
            pr("Service not added: Found one with same everything");
            return false;
          }
        }
      }
    }

    String ip = S.realhost;

    if (ip == null) {
      pr("No IP provided");
      return false; // added by RiK
    }

    pr("Checking binding for : " + ip);

    if (!(S.minport > 0 && S.maxport > 0)) {
      if (!(bindings.containsKey(ip))) {
        S.minport = minAutoPort;
        S.maxport = minAutoPort + sizeAutoPort - 1;
        pr("Binding not found");
        return true;
      }

      for (int p = minAutoPort; p < maxAutoPort; p += sizeAutoPort) {
        if (!checkConflict(ip, p, p + sizeAutoPort - 1)) {
          S.minport = p;
          S.maxport = p + sizeAutoPort - 1;
          return true;
        }
      }

      return false;
    } else {
      if (ip == null || !(bindings.containsKey(ip))) {
        return true;
      }

      // Okay to add if there isn't a conflict
      return !checkConflict(ip, S.minport, S.maxport);
    }

  }

  /* Method to add a service to the tree */
  public boolean addService(Service S) {
    pr("Adding Service : " + S.name);
    if (!checkBinding(S))
      return false;
    ServiceStack stack = getStack(S.name, true);
    if (stack == null)
      return false;
    stack.add(S);
    addBinding(S);

    return true;
  }

  /* Method to add an alias to the tree */
  public boolean addAlias(ServiceAlias S) {
    if (services.find(S.name) != null)
      return false;
    if (services.find(S.target) == null)
      return false;
    services.setitem(S.name, S);
    return true;
  }

  /* Method to del an alias from the tree */
  public boolean delAlias(String name) {
    Object o = services.find(name);
    if (o == null || !(o instanceof ServiceAlias)) {
      return false;
    }

    services.setitem(name, null);
    return true;
  }

  /* Method to add a general entry */
  public boolean addEntry(Object o) {
    if (o == null)
      return false;
    if (o instanceof ServiceAlias)
      return addAlias((ServiceAlias) o);
    else
      return addService((Service) o);
  }

  /* Method to update the service */
  public boolean updateService(Service S) {
    ServiceStack stack = getStack(S.name);
    if (stack == null)
      return false;
    return stack.update(S);
  }

  /* Method to del the service */
  public boolean delService(Service S) {
    ServiceStack stack = getStack(S.name);
    if (stack == null)
      return false;
    Object[] dellist = stack.delete(S);
    if (dellist != null) {
      for (int i = 0; i < dellist.length; i++)
        delBinding((Service) dellist[i]);
      return (dellist.length > 0);
    }

    return false;
  }

  /* Method to get a list of a particular service */
  public Service[] getServices(String name) {
    return getServices(name, null, false);
  }

  public Service[] getServices(String name, String level) {
    return getServices(name, level, false);
  }

  public Service[] getServices(String name, int level) {
    return getServices(name, "" + level, false);
  }

  public Service[] getServices(String name, int level, boolean resolveAlias) {
    return getServices(name, "" + level, resolveAlias);
  }

  // Accepts a Vinci level
  public Service[] getServices(String name, String level, boolean resolveAlias) {
    // Find the ServiceStack / ServiceAlias that matches the names specified
    Object O = services.find(name);

    if (O != null) {
      if (resolveAlias) {
        int i = 0; // Prevent an alias infinite loop
        while (ServiceAlias.isAlias(O) && i < MAX_ALIAS_LINKS) {
          O = services.find(((ServiceAlias) O).target);
          i++;
        }
      }

      if (ServiceAlias.isAlias(O))
        return null;
      else {
        // Get all the services from the ServiceStack
        // that are at or below the level specified
        Object[] temp = ((ServiceStack) O).get(level);
        Service[] result = new Service[temp.length];
        for (int i = 0; i < temp.length; i++)
          result[i] = (Service) temp[i];
        return result;
      }
    } else
      return null;
  }

  /* Helper methods */

  // Returns the absLevel as determined by the ServiceStack
  int getLevel(String name, String level) {
    ServiceStack S = getStack(name);
    if (S == null)
      return ServiceStack.getAbsLevel(level);
    return S.absLevel(level);
  }

  // Returns the ServiceStack that corresponds to the given name
  ServiceStack getStack(String name) {
    return getStack(name, false);
  }

  // Returns the ServiceStack that corresponds to the given name
  // Creates a stack if create is true, and no ServiceStack is found
  ServiceStack getStack(String name, boolean create) {
    Object o = services.find(name);

    if (o == null) {
      if (create) {
        o = new ServiceStack(name);
        services.setitem(name, o);
      }
    } else {
      if (o instanceof ServiceAlias)
        o = null;
    }

    return (ServiceStack) o;
  }

  // Returns all entries corresponding to a given name
  Object[] getEntries(String name) {
    return getEntries(name, -1);
  }

  Object[] getEntries(String name, int level) {
    return getEntries(name, "" + level);
  }

  Object[] getEntries(String name, String level) {
    Object o = services.find(name);

    if (o == null)
      return null;

    if (o instanceof ServiceAlias) {
      Object[] result = new Object[1];
      result[0] = o;
      return result;
    }

    return ((ServiceStack) o).get(level);
  }

  // Returns a list of all the services in the registry
  public Object[] listServices(String prefix, int level) {
    return listServices(prefix, "" + level);
  }

  public Object[] listServices(String prefix, String level) {
    if (prefix == null)
      prefix = "";

    ArrayList res = new ArrayList();
    Object[] items = services.findprefix(prefix);
    if (items == null)
      return res.toArray();

    for (int i = 0; i < items.length; i++) {
      if (ServiceAlias.isAlias(items[i])) {
        res.add(items[i]);
      } else {
        Object[] subitems = ((ServiceStack) items[i]).get(level);
        if (subitems != null) {
          for (int j = 0; j < subitems.length; j++)
            res.add(subitems[j]);
        }
      }
    }

    return res.toArray();
  }

  // Returns a list of all the services' names in the registry
  public String[] listNames(String prefix, int level) {
    return listNames(prefix, "" + level);
  }

  public String[] listNames(String prefix, String level) {
    if (prefix == null)
      prefix = "";

    ArrayList res = new ArrayList();
    Object[] items = services.findprefix(prefix);

    if (items == null)
      return new String[0];

    for (int i = 0; i < items.length; i++) {
      if (ServiceAlias.isAlias(items[i]))
        res.add(((ServiceAlias) items[i]).name);
      if (level == null || !((ServiceStack) items[i]).isEmpty(level)) {
        res.add(((ServiceStack) items[i]).name);
      }
    }

    String[] result = new String[res.size()];
    for (int i = 0; i < res.size(); i++)
      result[i] = res.get(i).toString();

    return result;
  }

  /* Methods to load and save registry info */
  public void load(String fname) throws Exception {

    DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

    FileReader readme = new FileReader(fname);
    Document doc;
    try {
      doc = docBuilder.parse(new InputSource(readme));
    } finally {
      readme.close();
    }

    Element root = doc.getDocumentElement();

    if (!root.getNodeName().equals("CONFIGURATION"))
      throw new RuntimeException("Illegal file specified");

    NodeList services = doc.getElementsByTagName("SERVICE");
    Node N, C;
    Hashtable H;
    Object S;

    for (int i = 0; i < services.getLength(); i++) {
      N = services.item(i);
      H = new Hashtable();

      NodeList children = N.getChildNodes();

      for (int j = 0; j < children.getLength(); j++) {
        C = children.item(j);

        if (C.getNodeType() == Node.TEXT_NODE)
          continue;

        NodeList subchildren;

        subchildren = C.getChildNodes();

        if (subchildren.getLength() == 1 && C.getFirstChild().getNodeType() == Node.TEXT_NODE) {
          // Simple elements
          H.put(C.getNodeName(), C.getFirstChild().getNodeValue());
        } else {
          // Complex elements
          H.put(C.getNodeName(), XMLToVinci.xmlToVinciFrame(new StringReader(constructXMLString(C,
                  true))));
        }
      } // End for j

      // Hack to distinguish Service from ServiceAlias
      if (!(H.get("TARGET") != null && H.get("NAME") != null))
        try {
          S = new Service(H);
        } catch (RuntimeException e) {
          System.err.println("Got exception while adding Service : " + H.get("NAME").toString());
          System.err.println("Exception generated : " + e);
          continue;
        }
      else
        S = new ServiceAlias((String) H.get("NAME"), (String) H.get("TARGET"));

      addEntry(S);
    }

  }

  static String constructXMLString(Node C, boolean include) {
    if (C.getNodeType() == Node.TEXT_NODE || C.getNodeType() == Node.CDATA_SECTION_NODE) {
      if (C.getNodeType() == Node.CDATA_SECTION_NODE)
        return "<![CDATA[" + C.getNodeValue() + "]]>";
      return C.getNodeValue();
    } else {
      String result = "";
      if (include)
        result += "<" + C.getNodeName() + ">";
      NodeList N = C.getChildNodes();
      for (int i = 0; i < N.getLength(); i++) {
        result += constructXMLString(N.item(i), true);
      }
      if (include)
        result += "</" + C.getNodeName() + ">";
      return result;
    }
  }

  public void save(Writer F) throws IOException {

    F.write("<CONFIGURATION>\n");
    Object[] objs = listServices("", -1); // Get all the services
    if (objs != null)
      for (int i = 0; i < objs.length; i++) {
        if (objs[i] == null)
          continue;
        if (ServiceAlias.isAlias(objs[i]))
          F.write(((ServiceAlias) objs[i]).toXML());
        else
          F.write(((Service) objs[i]).toXML());
        F.write("\n");
      }
    F.write("</CONFIGURATION>\n");

  }

  /* Inner class to represent port range as a data structure */
  class PortRange {
    int minPort, maxPort;

    PortRange(int min, int max) {
      minPort = min;
      maxPort = max;
    }

    public boolean equals(Object o) {
      if (!(o instanceof PortRange))
        return false;

      PortRange p = (PortRange) o;

      return (p.minPort == minPort) && (p.maxPort == maxPort);
    }

    @Override
    public int hashCode() {
      return minPort * 31 + maxPort;
    }
  }

  /* For testing */
  public static void main(String[] args) throws Exception {
    ServiceRegistry SR = new ServiceRegistry();
    SR.load(args[0]);

    FileWriter F;
    SR.save(F = new FileWriter(args[0] + ".bak"));
    F.close();
  }

  public static void pr(String s) {
    System.out.println(s);
  }

}
