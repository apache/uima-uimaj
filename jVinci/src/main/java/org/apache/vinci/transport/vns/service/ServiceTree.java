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
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.StringTokenizer;

/**
 * Data structure for storing/indexing/querying the set of service entries.
 */
public class ServiceTree {

  Object    value = null;
  Hashtable children;

  public ServiceTree() {
    value = null;
    children = new Hashtable();
  }

  public ServiceTree(Object o) {
    value = o;
    children = new Hashtable();
  }

  // Add the value under the specified key
  public void setitem(String key, Object value) {
    StringTokenizer str = new StringTokenizer(key, ".");
    ServiceTree at = this;
    ServiceTree temp;
    String s;

    while (str.hasMoreTokens()) {
      if ((temp = (ServiceTree) at.children.get(s = str.nextToken())) == null) {
        at.children.put(s, (at = new ServiceTree()));
      } else {
        at = temp;
      }
    }

    at.value = value;
  }

  // Find the value for the key specified or null if not found
  public Object find(String s) {
    StringTokenizer str = new StringTokenizer(s, ".");
    ServiceTree at = this;
    while (str.countTokens() != 0) {
      at = (ServiceTree) at.children.get(str.nextToken());
      if (at == null) {
        return null;
      }
    }

    return at.value;
  }

  // Returns the all the values whose key has the specified prefix
  public Object[] findprefix(String s) {
    StringTokenizer str = new StringTokenizer(s, ".");
    ServiceTree at = this;

    while (str.countTokens() != 0) {
      at = (ServiceTree) at.children.get(str.nextToken());
      if (at == null)
        return null;
    }

    ArrayList V = new ArrayList();
    if (at.value != null)
      V.add(at.value);

    Object[] C = at.children.values().toArray();
    Object[] O;
    for (int i = 0; i < C.length; i++) {
      O = ((ServiceTree) C[i]).findprefix("");
      if (O == null)
        continue;
      for (int j = 0; j < O.length; j++) {
        if (O[j] != null) {
          V.add(O[j]);
        }
      }
    }

    return (V.size() > 0) ? V.toArray() : null;
  }

  public static void main(String[] args) throws Exception {
    ServiceTree root = new ServiceTree();

    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    String s, k, v;
    Object[] O;

    while (true) {
      System.out.print("Enter a command (I, F, FP) : ");
      s = br.readLine();
      if (s == null || s.equals(""))
        break;
      s = s.trim().toUpperCase();

      if (!(s.equals("I") || s.equals("F") || s.equals("FP")))
        continue;

      System.out.print("Enter the key : ");
      k = br.readLine();

      if (k == null)
        continue;

      if (s.equals("F")) {
        System.out.println("RESULT: " + root.find(k));
        continue;
      }

      if (s.equals("FP")) {
        O = root.findprefix(k);
        if (O == null) {
          System.out.println("No entries found");
          continue;
        }
        for (int i = 0; i < O.length; i++)
          System.out.println("[" + i + "] : " + O[i].toString());
        continue;
      }

      if (s.equals("I")) {
        System.out.print("Enter the value for " + k + " : ");
        v = br.readLine();
        root.setitem(k, v);
        System.out.println("Done");
        continue;
      }
    }
  }
}
