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
package org.apache.uima.tools.jcas.internal;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Internal class, part of utility to migrate to version 3 of JCas
 * 
 * Analyzes one decompiled JCas type to determine if it was customized.
 * 
 * If customized, outputs a list of these to standard output.
 * 
 */

public class AnalyzeContent {

  private static final boolean trace = false;

  /*********************************************
   * String fragments reused in patterns below
   *********************************************/
  private static final String ppLlAccess = "this\\.jcasType\\.ll_cas\\.ll_";
  
  private static final String ppJcasTypeCasted = "\\(\\(\\w*_Type\\)this\\.jcasType\\)";
  
  private static final String ppFeatOkTst = 
        "\\s*if \\(\\w*_Type\\.featOkTst \\&\\& " + ppJcasTypeCasted + "\\.casFeat_\\w* \\=\\= null\\) \\{"
      + "\\s*this\\.jcasType\\.jcas\\.throwFeatMissing\\(\"\\w*\", \"[\\w\\.]*\"\\);"
      + "\\s*\\}";
  
  private static final String ppFeatCode = ppJcasTypeCasted + "\\.casFeatCode_\\w*"; 
  
  private static final String methodStart = "(?:\\s*\\@Override)?\\s*public \\w*";
  
  private static final String ppMaybeValueRef = "(?:, (?:" + ppLlAccess + "getFSRef\\(v\\)|v))?";
  
  /********************************************
   * instance variables
   ********************************************/

  final String s;
  int pos = 0;
  int savedPos = 0;
  Matcher m;
  public String msg;  
  
  String packageName;
  String shortClassName;
  String shortSuperName;
  boolean foundRegister = false; // because register might be in two places it seems
  
  public boolean isCustomized = true;

  private final Brna bPackage = newBrna("package ([^;]*);", "package");
  private final Brna bImports = newBrna("(\\s*import [^;]*;)+", "imports");
  private final Brna bClass = newBrna("\\s*public class (\\w*) extends (\\w*)\\s*\\{", "class");
  private final Brna bBp1 = newBrna(
        "\\s*public static final int typeIndexID;"
      + "\\s*public static final int type;", "type and index declares");
  private final Brna bRegister = newBrna(
        "\\s*static \\{"
      + "\\s*typeIndexID = JCasRegistry.register\\((\\w*)\\.class\\);"
      + "\\s*type = (\\w*).typeIndexID;"
      + "\\s*\\}", "register");
  private final Brna bBp2 = newBrna(
        "\\s*\\@Override"
      + "\\s*public int getTypeIndexID\\(\\) \\{"
      + "\\s*return (\\w*)\\.typeIndexID;"
      + "\\s*\\}"
      + "\\s*protected (\\w*)\\(\\) \\{"
      + "\\s*\\}", "getTypeIndex, empty constructor");
  
  private final Brna bConstr = newBrna(
        "\\s*public \\w*\\(final int addr, final TOP_Type type\\) \\{"
      + "\\s*super\\(addr, type\\);"
      + "(\\s*this\\.readObject\\(\\);)?"
      + "\\s*\\}"
      + "\\s*public \\w*\\(final JCas jcas\\) \\{"
      + "\\s*super\\(jcas\\);"
      + "(\\s*this\\.readObject\\(\\);)?" 
      + "\\s*\\}", "2 constructors");
      
  private final Brna bConstrAnnot = newBrna(
        "\\s*public \\w*\\(final JCas jcas, final int begin, final int end\\) \\{"
      + "\\s*super\\(jcas\\);"
      + "\\s*this\\.setBegin\\(begin\\);"
      + "\\s*this\\.setEnd\\(end\\);"
      + "(\\s*this\\.readObject\\(\\);)?" 
      + "\\s*\\}", "annot constructor");
  
  private final Brna bReadObj = newBrna(
        "\\s*private void readObject\\(\\) \\{"
      + "\\s*\\}", "readObject");
 
  private final Brna bSemicolon = newBrna("\\;", "bSemicolon");
  private final Brna bCloseParen = newBrna("\\)", "bCloseParen");
  private final Brna bCloseBrace = newBrna("\\s*}", "bCloseBrace");
  private final Brna bWhiteSpace = newBrna("\\s*", "bWhiteSpace");
  private final Brna bReturn = newBrna("\\s*return ", "bReturn");
  private final Brna bEndOfClass = newBrna("\\s*\\}\\s*$", "end of class");
  private final Brna bCopyright = newBrna("\\s*static String copyright\\(\\) \\{"
      + "\\s*return \"[^\"]*\";"
      + "\\s*}\\s*?(?m:$)", "Copyright function");  // (?m:  ) turns on multiline, which makes $ match end of line 

  private final Brna bSimpleCore = newBrna(
      ppLlAccess + "(?:get|set)\\w*Value\\(this\\.addr, " + ppFeatCode + ppMaybeValueRef + "\\)",
      "SimpleCore");

  private final Brna bArrayCoreStart = newBrna(ppLlAccess + "(?:get|set)\\w*ArrayValue\\(", "ArrayCore start");
  private final Brna bArrayCoreEnd   = newBrna(", i" + ppMaybeValueRef + "\\)", "ArrayCore end");
  private final Brna bArrayCore; // initialized in constructor, otherwise may get null value due to order of initialization of fields

  private final Brna bArrayBoundsCheckStart = newBrna("\\s*this\\.jcasType\\.jcas\\.checkArrayBounds\\(", "arrayBoundsCheck start");
  private final Brna bArrayBoundsCheckEnd = newBrna(", i\\);", "arrayBoundsCheck end");
  private final Brna bArrayBoundsCheck; // initialized in constructor, otherwise may get null value due to order of initialization of fields

  private final Brna bGetFeatureValue; // initialized in constructor, otherwise may get null value due to order of initialization of fields
  private final Brna bSetFeatureValue; // initialized in constructor, otherwise may get null value due to order of initialization of fields

  private final Brna pGet1 = newBrna(methodStart + " get\\w*\\(\\) \\{" + ppFeatOkTst, "get start");  
  private final Brna bGet;  // initialized in constructor, otherwise may get null value due to order of initialization of fields

  private final Brna bSet1 = newBrna(methodStart + " set\\w*\\(final \\w* v\\) \\{" + ppFeatOkTst, "set start");  
  private final Brna bSet;  // initialized in constructor, otherwise may get null value due to order of initialization of fields

  private final Brna bArrayGet1 = newBrna(methodStart + " get\\w*\\(final int i\\) \\{" + ppFeatOkTst, "arrayGet start");
  private final Brna bArrayGet;  // initialized in constructor, otherwise may get null value due to order of initialization of fields
  
  private final Brna bArraySet1 = newBrna(methodStart + " set\\w*\\(final int i, final \\w* v\\) \\{" + ppFeatOkTst, "arraySet start");
  private final Brna bArraySet;  // initialized in constructor, otherwise may get null value due to order of initialization of fields

  private final Brna bCastStart = newBrna("\\s*\\(\\w\\)\\(", "bCastStart");
  private final Brna bWrapToGetFS_Start = newBrna("this\\.jcasType\\.ll_cas\\.ll_getFSForRef\\(", "Wrap_to_get_FS Start");

  /********************************************
   * functional support for combining matchers
   ********************************************/
  @FunctionalInterface
  private interface Brna { // Boolean Result No Arguments
    abstract boolean match();
  }

  private Brna newBrna(String s, String name) {
    return newBrna(Pattern.compile(s), name);
  }

  /**
   * A matcher method object
   * @param p the pattern to match
   * @param name for trace output
   * @return a Brna matcher function
   */
  private Brna newBrna(Pattern p, String name) {
    return () -> {  
      m.usePattern(p);
      m.region(pos, s.length());
      boolean r = m.lookingAt();
      if (trace) {
        System.out.format(" *T%s* BrnaName: %s, pattern: %s string: %s%n", r ? "+" : "-", name,  p, stringPart());
      }
      if (r) {
        pos = m.end();
      } 
      return r;
    };
  }
  
  private String stringPart() {
    int endPos = Math.min(s.length(), pos + 360);
    return s.substring(pos, endPos);
  }

  /**
   * A Brna matcher function that is true if all inner matchers return true
   * @param inners matchers to run, these are run sequentially
   * @return true if all match. 
   *              If false the position of the global matcher is reset to its original spot 
   */
  private Brna all(Brna ... inners) {
    return () -> { 
      int savedPos = pos;
      
      
      //debug
      if (null == inners) {
        System.out.println("debug");
      }
      for (Brna inr : inners) {
        if (null == inr) {
          System.out.println("debug");
        }
      }
      
      
      if (Arrays.stream(inners).sequential().allMatch(Brna::match)) {
        return true;
      }
      pos = savedPos;
      return false;    
    };
  }
  
  /**
   * A Brna matcher function that first tries to match with the wrapper, but if that fails,
   * matches the inners without the wrapper
   * @param head matcher to match first
   * @param tail matcher to match after the inners
   * @param inners matchers to match the inner part, with or without the wrapper
   * @return true if matches, with or without the wrapper.  
   *              If false the position of the global matcher is reset to its original spot 
   */
  private Brna maybeWrap(String caller, Brna head, Brna tail, Brna ... inners) {
    if (trace) {
      System.out.println("MaybeWrap: called by " + caller + ", head: " + head);
    }
    return () -> {
      int savedPos = pos;
      if (head == null) {
        System.out.println("debug");
      }
      if (head.match()) {
        if (all(inners).match() && tail.match()) {
          return true;
        }
      } else {
        if (all(inners).match()) {
          return true;
        }
      }
      pos = savedPos;
      return false;
    };
  }

  /*******************************
   * Composible matcher functions
   *******************************/
  private Brna bMaybeCastResult(Brna innerMatcher) {
    return maybeWrap("bMaybeCastResult", bCastStart, bCloseParen, innerMatcher);
  }
  
  private Brna bMaybeWrapToGetFSResult(Brna core) {
    return maybeWrap("bMaybeWrapToGetFSResult", bWrapToGetFS_Start, bCloseParen,  core);
  }
      
  public AnalyzeContent(String content) {
    s = content;
    bArrayCore = all(bArrayCoreStart, bSimpleCore, bArrayCoreEnd);

    bArrayBoundsCheck = all(bArrayBoundsCheckStart, bSimpleCore, bArrayBoundsCheckEnd);

    bGetFeatureValue = all(bReturn, bMaybeCastResult(bMaybeWrapToGetFSResult(bSimpleCore)), bSemicolon);
    bSetFeatureValue = all(bWhiteSpace, bSimpleCore, bSemicolon);

    bGet = all(pGet1, bGetFeatureValue, bCloseBrace);

    bSet = all(bSet1, bSetFeatureValue, bCloseBrace);

    bArrayGet = all(bArrayGet1, bArrayBoundsCheck, bReturn, bMaybeCastResult(bMaybeWrapToGetFSResult(bArrayCore)), bSemicolon, bCloseBrace);
    
    bArraySet = all(bArraySet1, bArrayBoundsCheck, bWhiteSpace, bArrayCore, bSemicolon, bCloseBrace);
    
    analyzeJCas();
    
  }
  
  private String analyzeJCas() {    
   
    m = Pattern.compile("").matcher(s);
    if (!bPackage.match()) return msg = "No package statement, string:" + stringPart();
    packageName = m.group(1);
    
    if (!bImports.match()) return msg = "Missing imports, string:" + stringPart();

    if (!bClass.match()) return msg = "Missing class, string:" + stringPart();
    shortClassName = m.group(1);
    shortSuperName = m.group(2);

    if (!bBp1.match()) return msg = "Missing type/index declares, string:" + stringPart();

    checkRegister();
    
    bCopyright.match();  // ok if not match
    
    if (!bBp2.match()) return msg = "missing getTypeIndex and/or empty constructor, string:" + stringPart();
    
    if (!bConstr.match()) return msg = "missing some constructors, string:" + stringPart();
    
    bConstrAnnot.match(); // ok if not match
    
//    if (!bReadObj.match()) return msg = "readObject missing or customized, string:" + stringPart();
    bReadObj.match();  // ok if not there 
    /**
     * match getters and setters
     */
    
    
    
    while (!bEndOfClass.match()) {
      if (!checkRegister()) return msg;
      if (bEndOfClass.match()) break;
      if (!bGet.match()) return msg = "getter customized or moved or missing, string:" + stringPart();
      if (!bSet.match()) return msg = "setter customized or moved or missing, string:" + stringPart();
      
      if (bArrayGet.match()) {
        if (!bArraySet.match()) return msg ="array setter customized or moved or missing, string:" + stringPart();
      }
    }
    
    isCustomized = false;
    return msg = "Not Customized";
  }
  
  private boolean checkRegister() {
    if (!foundRegister && bRegister.match()) {
      foundRegister = true;
      if (!m.group(1).equals(shortClassName) || !m.group(2).equals(shortClassName)) {
        msg =  String.format("register boilerplate has wrong name parts: expected short class name %s but saw %s and/or %s", 
                              shortClassName, m.group(1), m.group(2));
        return false;
      }      
    }
    return true;
  }
    
}
