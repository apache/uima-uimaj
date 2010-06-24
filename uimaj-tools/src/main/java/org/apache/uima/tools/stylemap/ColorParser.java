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

import java.awt.Color;
import java.util.HashMap;
import java.util.StringTokenizer;

public class ColorParser {
  private HashMap colorNameMap;

  public ColorParser() {
    initializeColorNameMap();
  }

  private void initializeColorNameMap() {
    colorNameMap = new HashMap();
    colorNameMap.put("#000000", "black");
    colorNameMap.put("#c0c0c0", "silver");
    colorNameMap.put("#808080", "gray");
    colorNameMap.put("#ffffff", "white");
    colorNameMap.put("#800000", "maroon");
    colorNameMap.put("#ff0000", "red");
    colorNameMap.put("#800080", "purple");
    colorNameMap.put("#ff00ff", "fuchsia");
    colorNameMap.put("#008000", "green");
    colorNameMap.put("#00ff00", "lime");
    colorNameMap.put("#808000", "olive");
    colorNameMap.put("#ffff00", "yellow");
    colorNameMap.put("#000080", "navy");
    colorNameMap.put("#0000ff", "blue");
    colorNameMap.put("#00ffff", "aqua");
    colorNameMap.put("#000000", "black");
    colorNameMap.put("#add8e6", "lightblue");
    colorNameMap.put("#90ee90", "lightgreen");
    colorNameMap.put("#ffa500", "orange");
    colorNameMap.put("#ffc0cb", "pink");
    colorNameMap.put("#fa8072", "salmon");
    colorNameMap.put("#00ffff", "cyan");
    colorNameMap.put("#ee82ee", "violet");
    colorNameMap.put("#d2b48c", "tan");
    colorNameMap.put("#a52a2a", "brown");
    colorNameMap.put("#ffffff", "white");
    colorNameMap.put("#9370db", "mediumpurple");
    // in other order for lookup
    colorNameMap.put("black", "#000000");
    colorNameMap.put("silver", "#c0c0c0");
    colorNameMap.put("gray", "#808080");
    colorNameMap.put("white", "#ffffff");
    colorNameMap.put("maroon", "#800000");
    colorNameMap.put("red", "#ff0000");
    colorNameMap.put("purple", "#800080");
    colorNameMap.put("fuchsia", "#ff00ff");
    colorNameMap.put("green", "#008000");
    colorNameMap.put("lime", "#00ff00");
    colorNameMap.put("olive", "#808000");
    colorNameMap.put("yellow", "#ffff00");
    colorNameMap.put("navy", "#000080");
    colorNameMap.put("blue", "#0000ff");
    colorNameMap.put("aqua", "#00ffff");
    colorNameMap.put("black", "#000000");
    colorNameMap.put("lightblue", "#add8e6");
    colorNameMap.put("lightgreen", "#90ee90");
    colorNameMap.put("orange", "#ffa500");
    colorNameMap.put("pink", "#ffc0cb");
    colorNameMap.put("salmon", "#fa8072");
    colorNameMap.put("cyan", "#00ffff");
    colorNameMap.put("violet", "#ee82ee");
    colorNameMap.put("tan", "#d2b48c");
    colorNameMap.put("brown", "#a52a2a");
    colorNameMap.put("white", "#ffffff");
    colorNameMap.put("mediumpurple", "#9370db");
  }

  // --------------------
  public HashMap getColorNameMap() {
    return colorNameMap;
  }

  // --------------------
  public StyleMapEntry parseAndAssignColors(String typeName, String featureValue,
          String labelString, String styleColor) {
    StyleMapEntry sme = new StyleMapEntry();
    sme.setAnnotationTypeName(typeName);
    sme.setFeatureValue(featureValue);
    sme.setLabel(labelString);
    StringTokenizer token = new StringTokenizer(styleColor, ":;");
    if (token.hasMoreTokens()) {
      token.nextToken();
      String fgString = token.nextToken().toLowerCase();
      if (fgString.startsWith("#")) {
        sme.setForeground(Color.decode(fgString));
      } else {
        String newFgString = (String) colorNameMap.get(fgString);
        if (newFgString != null)
          sme.setForeground(Color.decode(newFgString));
        else
          sme.setForeground(Color.black);

      }
      token.nextToken();
      String bgString = token.nextToken().toLowerCase();
      if (bgString.startsWith("#")) {
        sme.setBackground(Color.decode(bgString));
      } else {
        String newBgString = (String) colorNameMap.get(bgString);
        if (newBgString != null)
          sme.setBackground(Color.decode(newBgString));
        else
          sme.setBackground(Color.white);
      }
      // parses the string
      // checked:false
      // and
      // hidden:true
      // this is added for check boxes
      parseChecked(token, sme);
      parseHidden(token, sme);
    }
    return sme;
  }

  // test for "checked:true" or false
  private void parseChecked(StringTokenizer token, StyleMapEntry sme) {
    if (token.hasMoreTokens()) {
      String ck = token.nextToken(); // checked
      String tf = token.nextToken(); // true or false
      if (ck.equals("checked")) {
        boolean checked = false;
        if (tf.equals("true")) {
          checked = true;
        }
        sme.setChecked(Boolean.valueOf(checked));
      }
    } else {
      sme.setChecked(Boolean.TRUE);// default to Checked
    }

  }

  // test for "hidden:true" or false
  private void parseHidden(StringTokenizer token, StyleMapEntry sme) {
    if (token.hasMoreTokens()) {
      String ck = token.nextToken(); // checked
      String tf = token.nextToken(); // true or false
      if (ck.equals("hidden")) {
        boolean checked = false;
        if (tf.equals("true")) {
          checked = true;
        }
        sme.setHidden(Boolean.valueOf(checked));
      }
    } else {
      sme.setHidden(Boolean.FALSE); // Default to not hidden
    }
  }

}
