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

package org.apache.uima.tools.stylemap;


/**
 * The Class StyleConstants.
 */
public class StyleConstants {
  
  /** The Constant NR_TABLE_COLUMNS. */
  public static final int NR_TABLE_COLUMNS = 7;

  /** The Constant LABEL_COLUMN. */
  // Column zero is for visual indication of selection
  public static final int LABEL_COLUMN = 1;

  /** The Constant TYPE_NAME_COLUMN. */
  public static final int TYPE_NAME_COLUMN = 2;

  /** The Constant BG_COLUMN. */
  // public static final int FEATURE_VALUE_COLUMN = 3;
  public static final int BG_COLUMN = 3;

  /** The Constant FG_COLUMN. */
  public static final int FG_COLUMN = 4;

  /** The Constant CHECK_COLUMN. */
  public static final int CHECK_COLUMN = 5; // check box column

  /** The Constant HIDDEN_COLUMN. */
  public static final int HIDDEN_COLUMN = 6; // hide these

  /** The Constant columnNames. */
  static final String[] columnNames = { "|", "Annotation Label", "Annotation Type / Feature",
      "Background", "Foreground", "Checked", "Hidden" };

}
