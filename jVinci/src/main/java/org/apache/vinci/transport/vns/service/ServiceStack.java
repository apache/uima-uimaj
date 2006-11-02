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

import java.util.ArrayList;
import java.util.Stack;

/**
 * Services of the same name get put in the same stack and stored within the ServiceTree.
 */
public class ServiceStack {

  String name;
  Stack  stack;
  int    toplevel;

  public ServiceStack(String name) {
    this.name = name;
    reinit();
  }

  public void reinit() {
    stack = new Stack();
    toplevel = 0;
  }

  /* Return the Vinci view of the level
   * No specification (null) or "none" -> -1 (highest)
   * "all" -> -2 (invalid / indication to concatenate all)
   * Positive int specification -> actual level from bottom of stack
   * Negative int specification -> level from top of stack
   * "new" or "next" -> one above the toplevel
   */

  public static int getAbsLevel(String level) {

    // Default as specified in SPEC.txt
    if (level == null || level.trim().toLowerCase().equals("none"))
      level = "-1";

    level = level.toLowerCase().trim();

    if (level.equals("all"))
      return -2;

    if (level.equals("new") || level.equals("next"))
      return 0; // Start with lowest priority

    int mylevel = Integer.parseInt(level);

    if (mylevel == -1)
      return -1; // Special case for highest priority servers

    if (mylevel < 0)
      return 0; // Can't count from top as top doesn't exist => return 0

    return mylevel;
  }

  public int absLevel(String level) {

    // Default as specified in SPEC.txt
    if (level == null || level.trim().toLowerCase().equals("none"))
      level = "-1";

    level = level.toLowerCase().trim();

    if (level.equals("all")) {
      return -2;
    }

    if (level.equals("new") || level.equals("next")) {
      // Will be adjusted in actualLevel
      return toplevel;
    }

    int mylevel = Integer.parseInt(level);

    if (mylevel == -1)
      return -1; // Special case for highest priority servers

    if (mylevel < 0) {
      return Math.max(0, toplevel + mylevel); // If invalid level specified, return 0
    } else
      return mylevel;
  }

  public int absLevel(int i) {
    return absLevel("" + i);
  }

  /* Return the mapping into the ArrayList
   * Returns -1 if the number cannot be mapped to a positive val
   */
  private int actualLevel(int level) {
    if (level < -1)
      return -1;

    return level + 1;
  }

  /* Expand the stack to the specified index level 
   * level represents the actual vector index
   */
  public void expand(int level) {

    while (stack.size() <= level)
      stack.push(null);

    if (toplevel < level)
      toplevel = level;

  }

  /* Get the set of values above the specified Vinci level 
   * Returns an Object [] of those values (Service [])
   */
  public Service[] get(String mylevel) {
    int level = absLevel(mylevel);
    ArrayList result = new ArrayList();

    // Concatenate all
    if (level == -2) {

      for (int i = 0; i < stack.size(); i++)
        if (stack.get(i) != null)
          result.addAll((ArrayList) stack.get(i));
    } else {
      level = actualLevel(level); // convert to actual level index

      expand(level);

      level = Math.min(level, toplevel);

      // Special case check for topmost level
      if (level == 0) {
        if (stack.get(level) != null)
          result.addAll((ArrayList) stack.get(level));
        if (result.size() < 1)
          level = Math.max(level, toplevel); // Go to the next lower level if no results
      }

      while (level >= 1) { // count go downto level 0 if service not found yet
        if (stack.get(level) != null) {
          result.addAll((ArrayList) stack.get(level));
          break; // Break as soon as we find a level with services running
        }
        level--;
      }

    }

    Service[] res = new Service[result.size()];
    for (int i = 0; i < result.size(); i++)
      res[i] = (Service) result.get(i);

    return res;
  }

  public Object[] get(int mylevel) {
    return get("" + mylevel);
  }

  /* Updates the level fields in the Service and makes them
   * consistent i.e. transforms the vinciLevel string and the
   * actualLevel int if need be.
   */
  public void makeConsistent(Service S) {
    if (S.actualLevel < -1) {
      S.level = "" + absLevel(S.level);
      S.actualLevel = actualLevel(Integer.parseInt(S.level));
      if (S.level.equals("-2"))
        S.level = "all"; // Switch back to all to prevent the VNS from screwing up
      S.update();
    }
  }

  /* Add the specified service to the stack
   * The level in the Service must be a non-index level
   */
  public void add(Service S) {
    makeConsistent(S);

    // Get the actual vector index
    int mylevel = S.actualLevel;

    // Grow the stack if need be
    expand(mylevel);

    ArrayList v;

    if (stack.get(mylevel) == null) {
      v = new ArrayList();
      v.add(S);
      stack.setElementAt(v, mylevel);
    } else {
      v = (ArrayList) stack.get(mylevel);
      v.add(S);
    }

    pr("Added " + S.toString());
  }

  public boolean update(Service S) {

    // Can't update something without a level specification
    if (S.level == null) {
      pr("No level specified");
      return false;
    }

    makeConsistent(S);

    // Get the actual vector index
    int mylevel = S.actualLevel;

    if (stack.get(mylevel) == null) {
      pr("No services found at specified level");
      return false;
    }

    ArrayList v = (ArrayList) stack.get(mylevel);
    Service temp;

    for (int i = 0; i < v.size(); i++) {
      temp = (Service) v.get(i);

      if (temp.equals(S)) {
        S.minport = temp.minport;
        S.maxport = temp.maxport;
        S.port = temp.port;
        S.level = temp.level; // Weird bug [level being reset]
        v.set(i, S);
        pr("Update of " + S.toString() + " successful");

        return true;
      }
    }

    pr("No matching service found");
    return false;
  }

  public boolean isEmpty(String level) {
    // Convert to a valid Vinci level first
    return isEmpty(absLevel(level));
  }

  public boolean isEmpty(int level) {
    if (stack.size() == 0)
      return true;

    // Got request for all
    if (level == -2)
      return false;

    // toplevel should never be greater than max index of the stack
    int mylevel = Math.min(actualLevel(level), toplevel);

    // Check if the level is the topmost level, if so, start from the topmost
    if (level == -1)
      mylevel = stack.size() - 1;

    while (mylevel >= 0) {
      if (stack.get(mylevel) != null)
        return false;
      mylevel--;
    }

    return true;
  }

  public Object[] delete(Service S) {
    ArrayList dellist = new ArrayList(), newlist = new ArrayList();

    if (S.level == null) {
      Object[] result = get("None");
      reinit();
      return result;
    }

    makeConsistent(S);

    // Get the actual vector index
    int mylevel = S.actualLevel;

    if (stack.get(mylevel) == null)
      return dellist.toArray();

    ArrayList srv = (ArrayList) stack.get(mylevel);
    Service temp;
    pr("Deleting : " + S);
    for (int i = 0; i < srv.size(); i++) {
      temp = (Service) srv.get(i);
      pr("Checking : " + temp);
      if (!(temp.toString().equals(S.toString()))) {
        newlist.add(temp);
      } else {
        pr("Delete match : " + temp.toString());
        dellist.add(temp);
      }
    }

    if (newlist.size() == 0) {
      stack.set(mylevel, null);
      // Decrease the stack size
      while (toplevel > 0 && stack.get(toplevel) == null)
        toplevel--;
    } else {
      stack.setElementAt(newlist, mylevel);
    }

    return dellist.toArray();
  }

  public static String pr(String s) {
    System.err.println(s);
    return s;
  }

} // end class
