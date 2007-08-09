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
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.StringTokenizer;

import org.apache.vinci.debug.Debug;

/**
 * Represents a VNS Workspace.
 */
public class WorkspaceConfig {
  protected String workspace = null;

  protected ArrayList search = null;

  VNS parent = null;

  public WorkspaceConfig() {
    workspace = null;
    search = new ArrayList();
    this.parent = null;
  }

  public WorkspaceConfig(VNS parent) {
    workspace = null;
    search = new ArrayList();
    this.parent = parent;
  }

  public void save(Writer f) throws IOException {
    if (workspace == null)
      throw new RuntimeException("No workspace -- cannot save");

    f.write("workspace " + workspace + "\n");

    if (search.size() > 0) {
      StringBuffer searchString = new StringBuffer();
      for (int i = 0; i < search.size(); i++)
        searchString.append(search.get(i).toString() + " ");
      f.write("search " + searchString + "\n");
    }
  }

  public void load(FileReader f) throws IOException {
    if (f == null)
      throw new RuntimeException("Invalid file");

    BufferedReader br = new BufferedReader(f);

    String s = null;
    StringTokenizer str = null;
    String directive = null;
    ArrayList args = null;
    int i;

    while ((s = br.readLine()) != null) {
      // Skip over comments
      if (s.charAt(0) == '#')
        continue;

      str = new StringTokenizer(s);

      if (str.countTokens() < 2)
        throw new RuntimeException("Syntax error in workspace config file");

      directive = str.nextToken();
      args = new ArrayList(str.countTokens());
      i = 0;
      while (str.hasMoreTokens()) {
        args.add(str.nextToken());
      }

      if (directive.equals("workspace")) {
        if (workspace != null)
          throw new RuntimeException("Multiple workspace directives");

        if (args.size() != 1)
          throw new RuntimeException("Too many args for workspace directive");

        workspace = (String) args.get(0);
      } else if (directive.equals("search")) {
        search.addAll(args);
      } else {
        throw new RuntimeException("Unknown workspace directive " + directive);
      }
    }

    br.close();

    StringBuffer searchString = new StringBuffer();
    for (i = 0; i < search.size(); i++)
      searchString.append(search.get(i).toString() + " ");

    if (parent != null) {
      Debug.p("workspace = " + workspace);
      Debug.p("search = " + searchString);
    }
  }

  public static void main(String[] args) throws Exception {
    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

    System.out.print("Enter filename to read workspace from : ");
    String wFile = br.readLine();

    FileReader F = new FileReader(wFile);
    WorkspaceConfig WS = new WorkspaceConfig();
    WS.load(F);
    System.out.println("workspace = " + WS.workspace);

    StringBuffer searchString = new StringBuffer();
    ;
    if (WS.search.size() > 0) {
      for (int i = 0; i < WS.search.size(); i++)
        searchString.append(WS.search.get(i).toString() + " ");
      System.out.print("search " + searchString + "\n");
    }

    F.close();

    FileWriter F2 = new FileWriter(wFile + ".txt");
    WS.save(F2);
    F2.close();
  }
}
