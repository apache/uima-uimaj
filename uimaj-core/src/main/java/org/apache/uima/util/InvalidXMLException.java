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

package org.apache.uima.util;

import org.apache.uima.UIMAException;

/**
 * Thrown by the {@link XMLParser} to indicate that an XML document is invalid or does not specify a
 * valid object of the desired class.
 * 
 * 
 */
public class InvalidXMLException extends UIMAException {

  /**
   * Message key for a standard UIMA exception message: "Invalid descriptor file {0}."
   */
  public static final String INVALID_DESCRIPTOR_FILE = "invalid_descriptor_file";

  /**
   * Message key for a standard UIMA exception message: "The XML parser encountered an unknown
   * element type: {0}."
   */
  public static final String UNKNOWN_ELEMENT = "unknown_element";

  /**
   * Message key for a standard UIMA exception message: "An object of class {0} was requested, but
   * the XML input contained an object of class {1}."
   */
  public static final String INVALID_CLASS = "invalid_class";

  /**
   * Message key for a standard UIMA exception message: "Expected an element of type {0}, but found
   * an element of type {1}."
   */
  public static final String INVALID_ELEMENT_TYPE = "invalid_element_type";

  /**
   * Message key for a standard UIMA exception message: "Required element type {0} not found within
   * element type {1}."
   */
  public static final String ELEMENT_NOT_FOUND = "element_not_found";

  /**
   * Message key for a standard UIMA exception message: "Required attribute {0} not found within
   * element type {1}."
   */
  public static final String REQUIRED_ATTRIBUTE_MISSING = "required_attribute_missing";

  /**
   * Message key for a standard UIMA exception message: "The XML document attempted to include an
   * external file "{0}", which was not found."
   */
  public static final String INCLUDE_FILE_NOT_FOUND = "include_file_not_found";

  /**
   * Message key for a standard UIMA exception message: "The text "{0}" is not valid content for the
   * element "{1}"."
   */
  public static final String INVALID_ELEMENT_TEXT = "invalid_element_text";

  /**
   * Message key for a standard UIMA exception message: Malformed URL {0} in import declaration.
   */
  public static final String MALFORMED_IMPORT_URL = "malformed_import_url";

  /**
   * Message key for a standard UIMA exception message: An import could not be resolved. No .xml
   * file with name "{0}" was found in the class path or data path.
   */
  public static final String IMPORT_BY_NAME_TARGET_NOT_FOUND = "import_by_name_target_not_found";

  /**
   * Message key for a standard UIMA exception message: Import failed. Could not read from URL {0}.
   */
  public static final String IMPORT_FAILED_COULD_NOT_READ_FROM_URL = "import_failed_could_not_read_from_url";

  /**
   * Message key for a standard UIMA exception message: Invalid import declaration. Import
   * declarations must have a "name" or a "location" attribute, but not both.
   */
  public static final String IMPORT_MUST_HAVE_NAME_XOR_LOCATION = "import_must_have_name_xor_location";

  /**
   * Message key for a standard UIMA exception message: This is not a valid CPE descriptor.
   */
  public static final String INVALID_CPE_DESCRIPTOR = "invalid_cpe_descriptor";

  /**
   * Message key for a standard UIMA exception message: Cycle found in imports. The descriptor for
   * Aggregate Analysis Engine "{0}" has imported itself as one of its delegate descriptors (perhaps
   * indirectly through another intermediate Analysis Engine descriptor).
   */
  public static final String CIRCULAR_AE_IMPORT = "circular_ae_import";

  /**
   * Message key for a standard UIMA exception message: The element "fsIndexes" cannot occur outside
   * the containing element "fsIndexCollection"
   */
  public static final String FS_INDEXES_OUTSIDE_FS_INDEX_COLLECTION = "fs_indexes_outside_fs_index_collection";

    /**
   * Message key for a standard UIMA exception message: "Element type {0} cannot be duplicated within
   * element type {1}."
   */
  public static final String DUPLICATE_ELEMENT_FOUND = "duplicate_element_found";
  
  
  
  private static final long serialVersionUID = 4470343379909952803L;

  /**
   * Creates a new exception with a null message.
   */
  public InvalidXMLException() {
    super();
  }

  /**
   * Creates a new exception with the specified cause and a null message.
   * 
   * @param aCause
   *          the original exception that caused this exception to be thrown, if any
   */
  public InvalidXMLException(Throwable aCause) {
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
  public InvalidXMLException(String aResourceBundleName, String aMessageKey, Object[] aArguments) {
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
  public InvalidXMLException(String aResourceBundleName, String aMessageKey, Object[] aArguments,
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
  public InvalidXMLException(String aMessageKey, Object[] aArguments) {
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
  public InvalidXMLException(String aMessageKey, Object[] aArguments, Throwable aCause) {
    super(aMessageKey, aArguments, aCause);
  }
}
