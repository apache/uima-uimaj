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

package org.apache.uima.tools.util.gui;

import java.text.NumberFormat;

import javax.swing.JFormattedTextField;
import javax.swing.text.NumberFormatter;

public class IntegerField extends JFormattedTextField {
  private static final long serialVersionUID = -9172169254226111684L;

  private NumberFormat numberFormat = NumberFormat.getIntegerInstance();

  public IntegerField(int min, int max, int initialValue) {
    super();

    NumberFormatter formatter = new NumberFormatter(numberFormat);
    formatter.setMinimum(new Integer(min));
    formatter.setMaximum(new Integer(max));
    formatter.setCommitsOnValidEdit(true);
    setFormatter(formatter);

    setValue(new Integer(initialValue));
  }
}
