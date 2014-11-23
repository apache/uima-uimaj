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

package org.apache.uima.internal.util;

import java.util.HashMap;

/**
 * Simple command line parsing utility.
 * 
 * <p>
 * The parser can only handle parameters that take 0 or 1 arguments. That is, you can parse command
 * lines like
 * 
 * <pre>
 *    doit -f -i file1 -o file2 --dir file3 /h file4 file5 file6
 * </pre>
 * 
 * <p>
 * The syntax of parameters is left to the user, no common prefix is assumed or enforced. Parameter
 * names can be arbitrarily long. You can define aliases for parameters: <code>-h</code>,
 * <code>/H</code> and <code>--help</code> can all mean the same parameter if so configured.
 * 
 */
public class CommandLineParser {

  private static class CmdLineParam {

    private final boolean hasArg;

    private CmdLineParam(boolean hasArg) {
      super();
      this.hasArg = hasArg;
    }

  }
  
  private final static String[] EMPTY_STRINGS = new String[0];

  private HashMap<String, CmdLineParam> paramMap = null;

  private HashMap<CmdLineParam, String> cmdLineMap = null;

  private String[] restArgs;

  /**
   * Create a new command line parser.
   */
  public CommandLineParser() {
    super();
    this.paramMap = new HashMap<String, CmdLineParam>();
  }

  /**
   * Add a new parameter name.
   * 
   * @param paramName
   *          The name of the parameter.
   * @param hasArg
   *          If the command line argument following this parameter should be interpreted as an
   *          argument to the parameter.
   * @return <code>false</code> iff <code>paramName</code> already exists.
   */
  public boolean addParameter(String paramName, boolean hasArg) {
    if (this.paramMap.containsKey(paramName)) {
      return false;
    }
    this.paramMap.put(paramName, new CmdLineParam(hasArg));
    return true;
  }

  /**
   * Add a new switch. This is the same as calling <code>addParameter(name, false)</code>.
   * 
   * @param paramName
   *          The name of the parameter.
   * @return <code>false</code> iff <code>paramName</code> already exists.
   */
  public boolean addParameter(String paramName) {
    return addParameter(paramName, false);
  }

  /**
   * Add an alias for an already defined parameter.
   * 
   * @param param
   *          A known parameter.
   * @param alias
   *          The alias.
   * @return <code>false</code> iff the parameter does not exist or the alias is already known.
   */
  public boolean addAlias(String param, String alias) {
    if (this.paramMap.containsKey(alias) || !this.paramMap.containsKey(param)) {
      return false;
    }
    this.paramMap.put(alias, this.paramMap.get(param));
    return true;
  }

  /**
   * Parse the command line.
   * 
   * @param args
   *          The command line args as passed to <code>main()</code>.
   * @throws Exception
   *           If a parameter that requires an argument does not have one (i.e., is the last
   *           parameter in the list).
   */
  public void parseCmdLine(String[] args) throws Exception {
    this.cmdLineMap = new HashMap<CmdLineParam, String>();
    int i = 0;
    while (i < args.length) {
      String cmdLineArg = args[i];
      if (this.paramMap.containsKey(cmdLineArg)) {
        CmdLineParam metaParam = this.paramMap.get(cmdLineArg);
        if (metaParam.hasArg) {
          ++i;
          if (i >= args.length) {
            // TODO: throw proper exception.
            throw new Exception("Required argument to parameter missing: " + cmdLineArg);
          }
          this.cmdLineMap.put(metaParam, args[i]);
        } else {
          this.cmdLineMap.put(metaParam, null);
        }
      } else {
        this.restArgs = new String[args.length - i];
        System.arraycopy(args, i, this.restArgs, 0, this.restArgs.length);
        return;
      }
      ++i;
    }
    this.restArgs = EMPTY_STRINGS;
  }

  /**
   * Get the rest of the args, i.e., args that follow the last know parameter.
   * 
   * @return The tail end of the args list, usually file name arguments.
   */
  public String[] getRestArgs() {
    return this.restArgs;
  }

  /**
   * Check if the given parameter name is known to this parser.
   * 
   * @param paramName
   *          The name of the parameter.
   * @return <code>true</code> iff the name was added with
   *         {@link #addParameter(String, boolean) addParameter()} or
   *         {@link #addAlias(String, String) addAlias()}.
   */
  public boolean isKnownParameter(String paramName) {
    return this.paramMap.containsKey(paramName);
  }

  /**
   * Check if the parameter was used on the command line.
   * 
   * @param paramName
   *          The name of the parameter.
   * @return <code>true</code> iff the name is known and was used as a command line argument.
   */
  public boolean isInArgsList(String paramName) {
    if (!this.paramMap.containsKey(paramName)) {
      return false;
    }
    return this.cmdLineMap.containsKey(this.paramMap.get(paramName));
  }

  /**
   * Get the argument to a parameter, if it exists.
   * 
   * @param paramName
   *          The name of the parameter.
   * @return The argument to the parameter if the parameter was used and takes an argument;
   *         <code>null</code>, else.
   */
  public String getParamArgument(String paramName) {
    if (isKnownParameter(paramName)) {
      return this.cmdLineMap.get(this.paramMap.get(paramName));
    }
    return null;
  }

}
