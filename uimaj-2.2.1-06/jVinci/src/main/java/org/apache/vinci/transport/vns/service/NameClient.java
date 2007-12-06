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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Random;
import java.util.StringTokenizer;

import org.apache.vinci.transport.BaseClient;
import org.apache.vinci.transport.FrameLeaf;
import org.apache.vinci.transport.KeyValuePair;
import org.apache.vinci.transport.QueryableFrame;
import org.apache.vinci.transport.Transportable;
import org.apache.vinci.transport.VinciFrame;
import org.apache.vinci.transport.context.VinciContext;
import org.apache.vinci.transport.vns.VNSConstants;

/**
 * Provides a command-line interface for querying VNS.
 */
public class NameClient {

  String vnsHost;

  int vnsPort = 9000;

  static class HitsList {
    public HitsList() {
    }

    String[] types;

    int[] hits;

    int totalhits;

    String starttime;
  };

  static Random R = new Random();

  public NameClient() {
    configure(VinciContext.getGlobalContext().getVNSHost(), VinciContext.getGlobalContext()
            .getVNSPort());
  }

  public NameClient(String host, int port) {
    configure(host, port);
  }

  // Configure the VNS host / port
  public void configure(String host, int port) {
    if (host != null) {
      vnsHost = host;
    }
    if (port > -1) {
      vnsPort = port;
    }
  }

  // Method to parse a fully qualified name into sub-parts
  public static ServiceInfo parseQName(String qname) {
    String[] result = new String[5];

    int i = qname.indexOf("/");
    if (i > -1)
      result[0] = qname.substring(0, i);

    i++;
    qname = qname.substring(i);

    i = qname.indexOf("[");
    String lhi;
    if (i > -1) {
      lhi = qname.substring(i + 1, qname.length() - 1);
      StringTokenizer str = new StringTokenizer(lhi, ",");
      int j = 2;
      while (str.hasMoreTokens())
        result[j++] = str.nextToken().trim();
    } else
      i = qname.length();

    result[1] = qname.substring(0, i);

    return new ServiceInfo(result);
  }

  // Methods to perform service lookup
  public ServiceInfo[] lookup(String name, int level, String host, String instance, String ws) {
    VinciFrame req = new VinciFrame();

    req.fadd("vinci:COMMAND", VNSConstants.RESOLVE_COMMAND).fadd("SERVICE", name).fadd("LEVEL",
            level).fadd("HOST", host).fadd("INSTANCE", instance).fadd("WORKSPACE", ws);

    System.out.println(req.toXML());
    VinciFrame resp = (VinciFrame) transmit(req);

    checkError(resp);

    return constructServiceInfo(resp.fget("SERVER"), resp.fgetString("LEVEL"), name);
  }

  public ServiceInfo[] lookup(String name) {
    return lookup(name, -1, null, null, null);
  }

  public ServiceInfo[] lookup(String name, int level) {
    return lookup(name, level, null, null, null);
  }

  public ServiceInfo[] lookup(String name, int level, String host) {
    return lookup(name, level, host, null, null);
  }

  public ServiceInfo[] lookup(String name, int level, String host, String instance) {
    return lookup(name, level, host, instance, null);
  }

  public ServiceInfo[] lookup(String name, String host) {
    return lookup(name, -1, host, null, null);
  }

  public ServiceInfo[] lookup(String name, String host, String instance) {
    return lookup(name, -1, host, instance, null);
  }

  public ServiceInfo[] lookup(String name, String host, String instance, String ws) {
    return lookup(name, -1, host, instance, ws);
  }

  // Method to perform service resolve
  public ServiceInfo resolve(String name, String host, String ip, String ws, int level, int inst) {
    VinciFrame req = new VinciFrame();
    req.fadd("vinci:COMMAND", VNSConstants.RESOLVE_COMMAND).fadd("SERVICE", name);
    smFrameAdd(req, "HOST", host);
    smFrameAdd(req, "IP", ip);
    smFrameAdd(req, "WORKSPACE", ws);
    req.fadd("LEVEL", level);
    if (inst > 0) {
      req.fadd("INSTANCE", inst);
    }

    VinciFrame resp = (VinciFrame) transmit(req);

    checkError(resp);

    ServiceInfo[] S = constructServiceInfo(resp.fget("SERVER"), resp.fgetString("LEVEL"), name);

    return ((S.length > 0) ? S[R.nextInt(S.length)] : null);
  }

  public static void smFrameAdd(VinciFrame v, String tag, String val) {
    if (val != null && tag != null)
      v.fadd(tag, val);
  }

  public ServiceInfo resolve(String name, int level) {
    VinciFrame req = (VinciFrame) new VinciFrame().fadd("vinci:COMMAND",
            VNSConstants.RESOLVE_COMMAND).fadd("SERVICE", name).fadd("LEVEL", level);

    VinciFrame resp = (VinciFrame) transmit(req);

    checkError(resp);

    ServiceInfo[] S = constructServiceInfo(resp.fget("SERVER"), resp.fgetString("LEVEL"), name);

    return ((S.length > 0) ? S[R.nextInt(S.length)] : null);
  }

  public ServiceInfo resolve(String name) {
    return resolve(name, -1);
  }

  // Method to get the list of services that are registered
  public ServiceInterface[] getList(String prefix, String level) {
    VinciFrame req = new VinciFrame();
    req.fadd("vinci:COMMAND", VNS.dirCmdGetList);
    req.fadd("LEVEL", level);
    smartAdd(req, "PREFIX", prefix);

    VinciFrame resp = (VinciFrame) transmit(req);

    checkError(resp);

    ArrayList A = resp.fget("SERVICE");
    Hashtable H;
    QueryableFrame Q;
    ServiceInterface[] S = new ServiceInterface[A.size()];
    for (int i = 0; i < A.size(); i++) {
      Q = (QueryableFrame) A.get(i);

      // Check if it is a Service or a ServiceAlias and parse accordingly
      if (Q.fgetString("TARGET") == null) {
        H = new Hashtable();
        int total = Q.getKeyValuePairCount();
        KeyValuePair P = null;
        for (int j = 0; j < total; j++) {
          P = Q.getKeyValuePair(j);
          if (P.isValueALeaf()) {
            H.put(P.getKey(), P.getValueAsString());
          } else {
            H.put(P.getKey(), P.getValue());
          }
        }

        S[i] = new Service(H);
      } else {
        S[i] = new ServiceAlias(Q.fgetString("NAME"), Q.fgetString("TARGET"));
      }
    }

    return S;
  }

  public ServiceInterface[] getList(String prefix, int level) {
    return getList(prefix, "" + level);
  }

  public ServiceInterface[] getList() {
    return getList(null, -1);
  }

  public ServiceInterface[] getList(String prefix) {
    return getList(prefix, -1);
  }

  public ServiceInterface[] getList(int level) {
    return getList(null, level);
  }

  // Method to get the registered service names
  public String[] getNames(String prefix, String level) {
    VinciFrame req = new VinciFrame();
    req.fadd("vinci:COMMAND", VNS.dirCmdGetNames);
    req.fadd("LEVEL", level);
    smartAdd(req, "PREFIX", prefix);

    VinciFrame resp = (VinciFrame) transmit(req);

    checkError(resp);

    ArrayList A = resp.fget("SERVICE");
    String[] S = new String[A.size()];
    for (int i = 0; i < A.size(); i++) {
      S[i] = ((FrameLeaf) A.get(i)).toString().trim();
    }

    return S;
  }

  public String[] getNames(String prefix, int level) {
    return getNames(prefix, "" + level);
  }

  public String[] getNames() {
    return getNames(null, -1);
  }

  public String[] getNames(String prefix) {
    return getNames(prefix, -1);
  }

  public String[] getNames(int level) {
    return getNames(null, level);
  }

  // Method to get the hits for a particular service
  public int getHits(String type) {
    VinciFrame out = new VinciFrame();
    out.fadd("vinci:COMMAND", VNS.dirCmdGetHits);
    smartAdd(out, "TYPE", type);
    VinciFrame resp = (VinciFrame) transmit(out);

    checkError(resp);

    return (resp.fgetInt("HITS"));
  }

  public int getHits() {
    return getHits(null);
  }

  // Method to get all the hits
  public HitsList getAllHits() {
    VinciFrame out = new VinciFrame();
    out.fadd("vinci:COMMAND", VNS.dirCmdGetHits);
    out.fadd("TYPE", "all");

    VinciFrame resp = (VinciFrame) transmit(out);

    checkError(resp);

    HitsList H = new HitsList();
    H.totalhits = resp.fgetInt("TOTAL");
    H.starttime = resp.fgetString("STARTED");
    ArrayList A = resp.fget("HITS");
    H.hits = new int[A.size()];
    H.types = new String[A.size()];

    QueryableFrame Q;
    for (int i = 0; i < H.hits.length; i++) {
      Q = (QueryableFrame) A.get(i);
      H.hits[i] = Q.fgetInt("COUNT");
      H.types[i] = Q.fgetString("TYPE");
    }

    return H;
  }

  // Method to delete a service
  public boolean delService(Service S) {
    return modifyService(S, VNS.dirCmdDelService);
  }

  // Method to add a service
  public boolean addService(Service S) {
    return modifyService(S, VNS.dirCmdAddService);
  }

  // Method to update a service
  public boolean updateService(Service S) {
    return modifyService(S, VNS.dirCmdUpdateService);
  }

  // Generic service interaction method
  public boolean modifyService(Service S, String type) {
    VinciFrame out = new VinciFrame();
    out.fadd("vinci:COMMAND", type);
    out.fadd("SERVICE", S.toFrame());

    VinciFrame resp = (VinciFrame) transmit(out);

    checkError(resp);

    return (resp.fgetString("STATUS").toLowerCase().trim().equals("ok"));
  }

  // Method to add an alias
  public boolean addAlias(String name, String target) {
    return modifyAlias(VNS.dirCmdAddAlias, name, target);
  }

  // Method to del an alias
  public boolean delAlias(String name) {
    return modifyAlias(VNS.dirCmdAddAlias, name, null);
  }

  // Generic alias interaction method
  public boolean modifyAlias(String type, String name, String target) {
    VinciFrame out = new VinciFrame();
    out.fadd("vinci:COMMAND", type);
    VinciFrame srv = new VinciFrame();
    smartAdd(srv, "NAME", name);
    smartAdd(srv, "TARGET", target);
    out.fadd("SERVICE", srv);

    VinciFrame resp = (VinciFrame) transmit(out);

    checkError(resp);

    return (resp.fgetString("STATUS").toLowerCase().trim().equals("ok"));
  }

  // Method to find out the port to serve on
  public int[] serveon(String name, String host, int level, int instance) {
    if (strip(host) == null || host.trim().toLowerCase().equals("localhost")) {
      try {
        host = InetAddress.getLocalHost().getHostName();
      } catch (Exception e) {
        throw new RuntimeException("Could not resolve local host");
      }
    }

    VinciFrame out = (VinciFrame) new VinciFrame().fadd("vinci:COMMAND",
            VNSConstants.SERVEON_COMMAND).fadd("SERVICE", name).fadd("HOST", host).fadd("LEVEL",
            level).fadd("INSTANCE", instance);

    VinciFrame resp = (VinciFrame) transmit(out);

    checkError(resp);

    int[] result = new int[3];
    result[0] = resp.fgetInt("PORT");
    result[1] = resp.fgetInt("LEVEL");
    result[2] = resp.fgetInt("INSTANCE");

    return result;
  }

  public int[] serveon(String name) {
    return serveon(name, null, -1, 0);
  }

  // Helper methods
  private void smartAdd(VinciFrame req, String tag, String val) {
    if (val != null)
      req.fadd(tag, val);
  }

  private ServiceInfo[] constructServiceInfo(ArrayList A, String level, String name) {
    if (level == null)
      level = "-1";

    ServiceInfo[] S = new ServiceInfo[A.size()];
    QueryableFrame L;
    for (int i = 0; i < S.length; i++) {
      L = (QueryableFrame) A.get(i);
      S[i] = new ServiceInfo(name, L.fgetString("HOST"), L.fgetString("PORT"), level, L
              .fgetString("INSTANCE"));
    }

    return S;
  }

  private Transportable transmit(Transportable T) {
    try {
      return BaseClient.rpc(T, vnsHost, vnsPort);
    } catch (Exception e) {
      VinciFrame F = new VinciFrame();
      F.fadd("vinci:ERROR", e.toString());
      return F;
    }
  }

  private void checkError(VinciFrame in) {
    String s = in.fgetString("vinci:ERROR");
    if (s != null)
      throw new RuntimeException(s);
  }

  // Main method for testing
  public static void main(String[] args) {
    NameClient nc = new NameClient();
    if (args.length > 1) {
      nc.configure(args[0], Integer.parseInt(args[1]));
    }

    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    String s;
    while (true) {
      prMainMenu();
      s = getLine(br).trim();
      if (s.equals("q"))
        break;
      try {
        switch (parseMainOption(s)) {
          case 0:
            handleParseQName(br, nc);
            break;
          case 1:
            handleLookup(br, nc);
            break;
          case 2:
            handleResolve(br, nc);
            break;
          case 3:
            handleGetList(br, nc);
            break;
          case 4:
            handleGetNames(br, nc);
            break;
          case 5:
            handleGetHits(br, nc);
            break;
          case 6:
            handleGetAllHits(br, nc);
            break;
          case 7:
            handleServeon(br, nc);
            break;
          case 8:
            handleAddService(br, nc);
            break;
          default:
            pr("Unknown option");
        }
      } catch (RuntimeException e) {
        pr("" + e);
      }
    }
  }

  private static void handleParseQName(BufferedReader br, NameClient nc) {
    pr("Enter the qname to parse : ", false);
    ServiceInfo S = NameClient.parseQName(getLine(br));
    pr(S.toString());
  }

  private static void handleLookup(BufferedReader br, NameClient nc) {
    pr("Enter the service name : ", false);
    String name = getLine(br);
    pr("Enter the level : ", false);
    String level = strip(getLine(br));
    int l = -1;
    try {
      l = Integer.parseInt(level);
    } catch (Exception e) {
      l = -1;
    }
    pr("Enter the host : ", false);
    String host = strip(getLine(br));
    pr("Enter the instance : ", false);
    String instance = strip(getLine(br));
    pr("Enter the workspace : ", false);
    String ws = strip(getLine(br));

    ServiceInfo[] S = nc.lookup(name, l, host, instance, ws);
    for (int i = 0; i < S.length; i++)
      pr("Service " + i + ":\n" + S[i]);
  }

  private static void handleResolve(BufferedReader br, NameClient nc) {
    pr("Enter the service name : ", false);
    String name = getLine(br);
    pr("Enter the service host : ", false);
    String host = getLine(br);
    pr("Enter the service IP : ", false);
    String ip = getLine(br);
    pr("Enter the service workspace : ", false);
    String ws = getLine(br);
    pr("Enter the level : ", false);
    String level = strip(getLine(br));
    int l = -1;
    try {
      l = Integer.parseInt(level);
    } catch (Exception e) {
      l = -1;
    }

    ServiceInfo S = nc.resolve(strip(name), strip(host), strip(ip), strip(ws), l, -1);
    pr("Service : \n" + S);
  }

  private static void handleServeon(BufferedReader br, NameClient nc) {
    pr("Enter the service name : ", false);
    String name = getLine(br);
    pr("Enter the host : ", false);
    String host = strip(getLine(br));
    pr("Enter the level : ", false);
    String level = strip(getLine(br));
    int l = -1;
    try {
      l = Integer.parseInt(level);
    } catch (Exception e) {
      l = -1;
    }
    pr("Enter the instance : ", false);
    String instance = strip(getLine(br));
    int inst = 0;
    try {
      inst = Integer.parseInt(instance);
    } catch (Exception e) {
      inst = 0;
    }

    int[] temp = nc.serveon(name, host, l, inst);
    pr("PORT: " + temp[0]);
  }

  private static void handleGetList(BufferedReader br, NameClient nc) {
    pr("Enter the prefix : ", false);
    String name = getLine(br);
    pr("Enter the level : ", false);
    String level = strip(getLine(br));
    // int l = -1;
    // try {
    // l = Integer.parseInt(level);
    // } catch (Exception e) {
    // l = -1;
    // }

    Object[] S = nc.getList(name, level);
    for (int i = 0; i < S.length; i++)
      if (ServiceAlias.isAlias(S[i]))
        pr("Service alias " + i + ":\n" + ((ServiceAlias) S[i]).toXML());
      else
        pr("Service " + i + ":\n" + ((Service) S[i]).toXML());
  }

  private static void handleGetNames(BufferedReader br, NameClient nc) {
    pr("Enter the prefix : ", false);
    String name = getLine(br);
    pr("Enter the level : ", false);
    String level = strip(getLine(br));
    // int l = -1;
    // try {
    // l = Integer.parseInt(level);
    // } catch (Exception e) {
    // l = -1;
    // }

    String[] S = nc.getNames(name, level);
    for (int i = 0; i < S.length; i++)
      pr("Service " + i + ": " + S[i]);

  }

  private static void handleGetHits(BufferedReader br, NameClient nc) {
    pr("Enter the type : ", false);
    String type = getLine(br);
    pr("Result : " + nc.getHits(type));
  }

  private static void handleGetAllHits(BufferedReader br, NameClient nc) {
    HitsList H = nc.getAllHits();
    for (int i = 0; i < H.hits.length; i++) {
      pr("[" + i + "] " + H.types[i].trim() + " : " + H.hits[i]);
    }
    pr("Total : " + H.totalhits);
    pr("Starttime : " + H.starttime);
  }

  private static void handleAddService(BufferedReader br, NameClient nc) {
    pr("Enter the service name : ", false);
    String name = getLine(br);
    pr("Enter the service host : ", false);
    String host = getLine(br);
    pr("Enter the service level : ", false);
    String level = getLine(br);
    pr("Enter the service minport : ", false);
    String minport = getLine(br);
    pr("Enter the service maxport : ", false);
    String maxport = getLine(br);
    pr("Enter the service port : ", false);
    String port = getLine(br);

    Hashtable H = new Hashtable();
    smAddHT(H, "NAME", name);
    smAddHT(H, "HOST", host);
    smAddHT(H, "LEVEL", level);
    smAddHT(H, "MINPORT", minport);
    smAddHT(H, "MAXPORT", maxport);
    smAddHT(H, "PORT", port);

    Service S = new Service(H);

    if (nc.addService(S))
      System.out.println("Successfully added service.\n" + S);
    else
      System.out.println("Could not add the service");
  }

  private static String[] options = { "parseqname", "lookup", "resolve", "getlist", "getnames",
      "gethits", "getallhits", "serveon", "addservice" };

  private static void prMainMenu() {
    pr("\nMenu \n");
    for (int i = 0; i < options.length; i++) {
      pr("" + i + " : " + options[i]);
    }
    pr("\nq : quit\n");
    pr("Enter your selection : ", false);
  }

  private static void smAddHT(Hashtable H, String key, String val) {
    if (key != null && val != null)
      H.put(key, val);
  }

  private static int parseMainOption(String s) {
    s = s.toLowerCase().trim();

    for (int i = 0; i < options.length; i++)
      if (s.equals(options[i]))
        return i;

    try {
      return Integer.parseInt(s);
    } catch (Exception e) {
    }

    return -1;
  }

  public static String pr(String s) {
    System.out.println(s);
    return s;
  }

  public static String pr(String s, boolean newline) {
    System.out.print(s + ((newline) ? "\n" : ""));
    return s;
  }

  private static String strip(String s) {
    if (s == null || s.trim().equals(""))
      return null;
    return s.trim();
  }

  private static String getLine(BufferedReader br) {
    try {
      return br.readLine();
    } catch (IOException e) {
      return null;
    }
  }
}
