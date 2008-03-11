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

package org.apache.uima.search;

import org.apache.uima.UIMAException;

/**
 * Thrown by the Index to indicate that a failure has occurred during indexing.
 * 
 * 
 */
public class IndexingException extends UIMAException {

  private static final long serialVersionUID = 5002247376240314251L;

  /**
   * Creates a new exception with a null message.
   */
  public IndexingException() {
    super();
  }

  /**
   * Creates a new exception with the specified cause and a null message.
   * 
   * @param aCause
   *          the original exception that caused this exception to be thrown, if any
   */
  public IndexingException(Throwable aCause) {
    super(aCause);
  }

  /**
   * Creates a new exception with a the specified message.
   * 
   * @param aResourceBundleName
   *          the base name of the resource bundle in which the message for this exception is
   *          located.
   * @param aMessageKey
   *          an identifier that maps to the message for this exception. The message may contain
   *          placeholders for arguments as defined by the
   *          {@link java.text.MessageFormat MessageFormat} class.
   * @param aArguments
   *          The arguments to the message. <code>null</code> may be used if the message has no
   *          arguments.
   */
  public IndexingException(String aResourceBundleName, String aMessageKey, Object[] aArguments) {
    super(aResourceBundleName, aMessageKey, aArguments);
  }

  /**
   * Creates a new exception with the specified message and cause.
   * 
   * @param aResourceBundleName
   *          the base name of the resource bundle in which the message for this exception is
   *          located.
   * @param aMessageKey
   *          an identifier that maps to the message for this exception. The message may contain
   *          placeholders for arguments as defined by the
   *          {@link java.text.MessageFormat MessageFormat} class.
   * @param aArguments
   *          The arguments to the message. <code>null</code> may be used if the message has no
   *          arguments.
   * @param aCause
   *          the original exception that caused this exception to be thrown, if any
   */
  public IndexingException(String aResourceBundleName, String aMessageKey, Object[] aArguments,
          Throwable aCause) {
    super(aResourceBundleName, aMessageKey, aArguments, aCause);
  }

  /**
   * Creates a new exception with a message from the {@link #STANDARD_MESSAGE_CATALOG}.
   * 
   * @param aMessageKey
   *          an identifier that maps to the message for this exception. The message may contain
   *          placeholders for arguments as defined by the
   *          {@link java.text.MessageFormat MessageFormat} class.
   * @param aArguments
   *          The arguments to the message. <code>null</code> may be used if the message has no
   *          arguments.
   */
  public IndexingException(String aMessageKey, Object[] aArguments) {
    super(aMessageKey, aArguments);
  }

  /**
   * Creates a new exception with the specified cause and a message from the
   * {@link #STANDARD_MESSAGE_CATALOG}.
   * 
   * @param aMessageKey
   *          an identifier that maps to the message for this exception. The message may contain
   *          placeholders for arguments as defined by the
   *          {@link java.text.MessageFormat MessageFormat} class.
   * @param aArguments
   *          The arguments to the message. <code>null</code> may be used if the message has no
   *          arguments.
   * @param aCause
   *          the original exception that caused this exception to be thrown, if any
   */
  public IndexingException(String aMessageKey, Object[] aArguments, Throwable aCause) {
    super(aMessageKey, aArguments, aCause);
  }

  /**
   * Message key for a standard UIMA exception message: The filter syntax {0} is not supported by
   * this implementation.
   */
  public static final String UNSUPPORTED_FILTER_SYNTAX = "unsupported_filter_syntax";

  /**
   * Message key for a standard UIMA exception message: Invalid filter expression: '{0}' cannot
   * start a feature name.
   */
  public static final String INVALID_FILTER_FEATURE_NAME = "invalid_filter_feature_name";

  /**
   * Message key for a standard UIMA exception message: Invalid filter expression: Expected a string
   * or number but found '{0}'.
   */
  public static final String INVALID_FILTER_EXPECTED_LITERAL = "invalid_filter_expected_literal";

  /**
   * Message key for a standard UIMA exception message: Invalid filter expression: Invalid escape
   * sequence {0}.
   */
  public static final String INVALID_FILTER_ESCAPE = "invalid_filter_escape";

  /**
   * Message key for a standard UIMA exception message: Invalid filter expression: Expected a digit
   * or '.' but found {0}.
   */
  public static final String INVALID_FILTER_EXPECTED_DIGIT_OR_POINT = "invalid_filter_expected_digit_or_point";

  /**
   * Message key for a standard UIMA exception message: Invalid filter expression: Expected a digit
   * but found {0}.
   */
  public static final String INVALID_FILTER_EXPECTED_DIGIT = "invalid_filter_expected_digit";

  /**
   * Message key for a standard UIMA exception message: Invalid filter expression: Expected the end
   * of the expression but found {0}.
   */
  public static final String INVALID_FILTER_EXPECTED_END = "invalid_filter_expected_end";

  /**
   * Message key for a standard UIMA exception message: Invalid filter expression: Unterminated
   * String: {0}.
   */
  public static final String INVALID_FILTER_UNTERMINATED_STRING = "invalid_filter_unterminated_string";

  /**
   * Message key for a standard UIMA exception message: Invalid filter expression: Expected operator
   * but found {0}.
   */
  public static final String INVALID_FILTER_EXPECTED_OPERATOR = "invalid_filter_expected_operator";

  /**
   * Message key for a standard UIMA exception message: Invalid filter expression: The operator {0}
   * cannot be applied to strings.
   */
  public static final String INVALID_FILTER_STRING_OPERATOR = "invalid_filter_string_operator";

  /**
   * Message key for a standard UIMA exception message: Feature {0} referenced in build item {1} is
   * not known.
   */
  public static final String UNKNOWN_FEATURE_IN_BUILD_ITEM = "unknown_feature_in_build_item";

  /**
   * Message key for a standard UIMA exception message: Invalid attributes in build item {0}: The
   * combination [{1}] is not allowed.
   */
  public static final String INVALID_ATTRIBUTE_COMBINATION_IN_BUILD_ITEM = "invalid_attribute_combination_in_build_item";

  /**
   * Message key for a standard UIMA exception message: The semantic search index at "{0}" was built
   * with a UIMA version prior to v2.0. This index format is no longer supported. You will need to
   * delete your index and reindex your content.
   */
  public static final String INCOMPATIBLE_INDEX_VERSION = "incompatible_index_version";
}
