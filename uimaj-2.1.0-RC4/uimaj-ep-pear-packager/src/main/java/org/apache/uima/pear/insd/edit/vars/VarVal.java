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

package org.apache.uima.pear.insd.edit.vars;

/**
 * 
 * Represents a table row in a two column table (of environment varibles).
 * 
 * 
 * 
 */
public class VarVal {

  // Set the table column property names
  public static final String VAR_NAME = "Variable_Name";

  public static final String VAR_VALUE = "Variable_Value";

  // Set column names
  public static final String[] fieldNames = new String[] { VAR_NAME, VAR_VALUE };

  private String varName = "";

  private String varValue = "";

  /**
   * 
   * Create a VarVal instance
   * 
   * @param varName
   * @param varValue
   */
  public VarVal(String varName, String varValue) {
    super();
    this.varName = varName;
    this.varValue = varValue;
  }

  /**
   * Return field names
   * 
   * @return String[] An arry of column field names
   */
  public static String[] getFieldNames() {
    return fieldNames;
  }

  /**
   * Returns the variable name
   * 
   * @return the variable name
   */
  public String getVarName() {
    return varName;
  }

  /**
   * Returns the variable value
   * 
   * @return the variable value
   */
  public String getVarValue() {
    return varValue;
  }

  /**
   * Sets the variable name
   * 
   * @param string
   *          the variable name
   */
  public void setVarName(String string) {
    varName = string;
  }

  /**
   * Sets the variable value
   * 
   * @param string
   *          the variable value
   */
  public void setVarValue(String string) {
    varValue = string;
  }

}
