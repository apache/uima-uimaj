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

package org.apache.uima.cas.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.StringTokenizer;

import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;

/**
 * Class comment for TypeSystemUtils.java goes here.
 * 
 * 
 */
public abstract class TypeSystemUtils {

  // Return value constants for feature path checking on type system
  public static enum PathValid {
    NEVER, POSSIBLE, ALWAYS
  }

  static abstract class TypeSystemParse {

    private ParsingError error = null;

    protected TypeSystemParse() {
      super();
    }

    boolean hasError() {
      return (this.error != null);
    }

    /**
     * Returns the error.
     * 
     * @return ParsingError
     */
    ParsingError getError() {
      return this.error;
    }

    /**
     * Sets the error.
     * 
     * @param error
     *                The error to set
     */
    void setError(ParsingError error) {
      this.error = error;
    }

  }

  static class NameSpaceParse extends TypeSystemParse {

    private String name;

    NameSpaceParse() {
      super();
    }

    /**
     * Returns the name.
     * 
     * @return String
     */
    String getName() {
      return this.name;
    }

    /**
     * Sets the name.
     * 
     * @param name
     *                The name to set
     */
    void setName(String name) {
      this.name = name;
    }

  }

  static class TypeParse extends TypeSystemParse {

    private String name;

    private NameSpaceParse nameSpace;

    TypeParse() {
      super();
    }

    TypeParse(String name) {
      this();
      this.name = name;
    }

    boolean isQualified() {
      return (this.nameSpace != null);
    }

    String getName() {
      return this.name;
    }

    NameSpaceParse getNameSpace() {
      return this.nameSpace;
    }

    /**
     * Sets the name.
     * 
     * @param name
     *                The name to set
     */
    public void setName(String name) {
      this.name = name;
    }

    /**
     * Sets the nameSpace.
     * 
     * @param nameSpace
     *                The nameSpace to set
     */
    public void setNameSpace(NameSpaceParse nameSpace) {
      this.nameSpace = nameSpace;
    }

  }

  static class FeatureParse extends TypeSystemParse {

    private TypeParse type;

    private String name;

    /**
     * Returns the name.
     * 
     * @return String
     */
    public String getName() {
      return this.name;
    }

    /**
     * Returns the type.
     * 
     * @return Type
     */
    public TypeParse getType() {
      return this.type;
    }

    /**
     * Sets the name.
     * 
     * @param name
     *                The name to set
     */
    public void setName(String name) {
      this.name = name;
    }

    /**
     * Sets the type.
     * 
     * @param type
     *                The type to set
     */
    public void setType(TypeParse type) {
      this.type = type;
    }

  }

  static class ParsingError {

    private int errorCode;

    private int errorPosition;

    /**
     * Returns the errorCode.
     * 
     * @return int
     */
    public int getErrorCode() {
      return this.errorCode;
    }

    /**
     * Returns the errorPosition.
     * 
     * @return int
     */
    public int getErrorPosition() {
      return this.errorPosition;
    }

    /**
     * Sets the errorCode.
     * 
     * @param errorCode
     *                The errorCode to set
     */
    public void setErrorCode(int errorCode) {
      this.errorCode = errorCode;
    }

    /**
     * Sets the errorPosition.
     * 
     * @param errorPosition
     *                The errorPosition to set
     */
    public void setErrorPosition(int errorPosition) {
      this.errorPosition = errorPosition;
    }

  }

  public static boolean isIdentifier(String s) {
    if (s == null) {
      return false;
    }
    final int len = s.length();
    if (s == null || len == 0) {
      return false;
    }
    int pos = 0;
    // Check that the first character is a letter.
    if (!isIdentifierStart(s.charAt(pos))) {
      return false;
    }
    ++pos;
    while (pos < len) {
      if (!isIdentifierChar(s.charAt(pos))) {
        return false;
      }
      ++pos;
    }
    return true;
  }

  static boolean isNonQualifiedName(String s) {
    return isIdentifier(s);
  }

  static boolean isIdentifierStart(char c) {
    return Character.isLetter(c);
  }

  static boolean isIdentifierChar(char c) {
    return (Character.isLetter(c) || Character.isDigit(c) || (c == '_'));
  }

  static private final String NAMESPACE_SEPARATOR_AS_STRING = "" + TypeSystem.NAMESPACE_SEPARATOR;

  /**
   * Check if <code>name</code> is a possible type name. Does not check if this type actually
   * exists!
   * 
   * @param name
   *                The name to check.
   * @return <code>true</code> iff <code>name</code> is a possible type name.
   */
  static boolean isTypeName(String name) {
    // Create a string tokenizer that will split the string at the name
    // space
    // boundaries. We need to see the delimiters to make sure there are no
    // gratuitous delimiters at the beginning or the end.
    StringTokenizer tok = new StringTokenizer(name, NAMESPACE_SEPARATOR_AS_STRING, true);
    // Loop over the tokens and check that every item is an identifier.
    while (tok.hasMoreTokens()) {
      // Any subsequence must start with an identifier.
      if (!isIdentifier(tok.nextToken())) {
        return false;
      }
      // If there is a next token, it must be a separator.
      if (tok.hasMoreTokens()) {
        if (!tok.nextToken().equals(NAMESPACE_SEPARATOR_AS_STRING)) {
          return false;
        }
        // A sequence can not end in a separator.
        if (!tok.hasMoreTokens()) {
          return false;
        }
      }
    }
    return true;
  }

  static boolean isTypeNameSpaceName(String name) {
    // Syntactically, there is no difference between a type name and a name
    // space name.
    return isTypeName(name);
  }

  /**
   * Checks if a feature path is valid for a given type.
   * 
   * <p>
   * We distinguish three cases:
   * <ol>
   * <li><code>PathValid.NEVER</code>: there is no object of <code>type</code> on which
   * <code>path</code> can ever be defined.</li>
   * <li><code>PathValid.ALWAYS</code>: if all intermediate objects are non-null, this
   * <code>path</code> will always be defined on any object of <code>type</code>. </li>
   * <li><code>PathValid.POSSIBLE</code>: some objects of <code>type</code> will have<code>path</code> 
   * defined, while others may not.</li>
   * </ol>
   * <b>Note:</b> we always assume that all references are not null.  A return value of ALWAYS
   * can of course not guarantee that all intermediate objects will always exist; only that if they
   * exist, the path will be defined.
   * 
   * @param type The type.
   * @param path The path to check.
   * @return One of {@link PathValid#ALWAYS ALWAYS}, {@link PathValid#POSSIBLE POSSIBLE}, or
   * {@link PathValid#NEVER NEVER}.
   */
  public static final PathValid isPathValid(Type type, List<String> path) {
    Stack<String> fStack = new Stack<String>();
    // Note: addAll() adds elements to the stack in the wrong order.
    for (int i = (path.size() - 1); i >= 0; i--) {
      fStack.push(path.get(i));
    }
    return isPathValid(type, fStack, PathValid.ALWAYS);
  }

  private static final PathValid isPathValid(Type type, Stack<String> path, 
      PathValid status) {
    // If the path is empty, return the input status.
    if (path.isEmpty()) {
      return status;
    }
    // Pop the next feature name from the stack and check if it's defined for the current type.
    String fName = path.pop();
    Feature feat = type.getFeatureByBaseName(fName);
    if (feat != null) {
      // If feature is defined, we can continue directly.
      return isPathValid(feat.getRange(), path, status);
    }
    // If feature is not defined for type, check to see if there are any subtypes for which the
    // path is defined (possible).
    List<Type> subtypes = new ArrayList<Type>();
    getFeatureDefiningSubtypes(type, fName, subtypes);
    for (int i = 0; i < subtypes.size(); i++) {
      // Retrieve the feature value type
      Type nextType = subtypes.get(i).getFeatureByBaseName(fName).getRange();
      // Call isPathValid() on next type in chain.
      PathValid newStatus = isPathValid(nextType, path, PathValid.POSSIBLE);
      if (newStatus == PathValid.POSSIBLE) {
        // If we found one, we can stop here and return.
        return PathValid.POSSIBLE;
      }
    }
    // No subtype was found for which the path was defined.
    return PathValid.NEVER;
  }
  
  // Find subtypes that define the feature.  Add subtypes to list.
  private static final void getFeatureDefiningSubtypes(Type type, String fName, List<Type> types) {
    TypeSystem ts = ((TypeImpl) type).getTypeSystem();
    List<?> subtypes = ts.getDirectSubtypes(type);
    for (int i = 0; i < subtypes.size(); i++) {
      Type subtype = (Type) subtypes.get(i);
      if (subtype.getFeatureByBaseName(fName) != null) {
        types.add((Type) subtypes.get(i));
      } else {
        getFeatureDefiningSubtypes(subtype, fName, types);
      }
    }
  }

  /**
   * Classify types into FS type, array type etc. For the full list of return types, see the
   * <code>LowLevelCAS.TYPE_CLASS*</code> constants, as well as the documentation for
   * {@link LowLevelCAS#ll_getTypeClass(int) LowLevelCAS.ll_getTypeClass(int)}.
   * 
   * @param type
   *                The type to classify.
   * @return An integer encoding the the type class. See above.
   */
  public static final int classifyType(Type type) {
    LowLevelTypeSystem llts = ((TypeImpl) type).getTypeSystem().getLowLevelTypeSystem();
    return llts.ll_getTypeClass(llts.ll_getCodeForType(type));
  }

}
