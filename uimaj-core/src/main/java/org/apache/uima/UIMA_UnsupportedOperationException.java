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

package org.apache.uima;

/**
 * Thrown to indicate that the requested operation is not supported. This extends
 * <code>RuntimeException</code> and so does not need to be declared in the throws clause of
 * methods.
 * 
 * 
 */
public class UIMA_UnsupportedOperationException extends UIMARuntimeException {

  private static final long serialVersionUID = 9056907160021698405L;

  /**
   * Message key for a standard UIMA exception message: "Class {0} does not support method {1}."
   */
  public static final String UNSUPPORTED_METHOD = "unsupported_method";

  /**
   * Message key for a standard UIMA exception message: "Attribute {0} of class {1} is not
   * modifiable."
   */
  public static final String NOT_MODIFIABLE = "not_modifiable";

  /**
   * Message key for a standard UIMA exception message: "This is a shared resource and cannot be
   * reconfigured."
   */
  public static final String SHARED_RESOURCE_NOT_RECONFIGURABLE = "shared_resource_not_reconfigurable";

  /**
   * Message key for a standard UIMA exception message: "The Flow class {0} does not support the
   * production of new CASes in the middle of the flow and so cannot be deployed in an Aggregate
   * AnalysisEngine that includes a CAS Multiplier component."
   */
  public static final String CAS_MULTIPLIER_NOT_SUPPORTED = "cas_multiplier_not_supported";

  /**
   * Creates a new exception with a null message.
   */
  public UIMA_UnsupportedOperationException() {
    super();
  }

  /**
   * Creates a new exception with the specified cause and a null message.
   * 
   * @param aCause
   *          the original exception that caused this exception to be thrown, if any
   */
  public UIMA_UnsupportedOperationException(Throwable aCause) {
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
  public UIMA_UnsupportedOperationException(String aResourceBundleName, String aMessageKey,
          Object[] aArguments) {
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
  public UIMA_UnsupportedOperationException(String aResourceBundleName, String aMessageKey,
          Object[] aArguments, Throwable aCause) {
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
  public UIMA_UnsupportedOperationException(String aMessageKey, Object[] aArguments) {
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
  public UIMA_UnsupportedOperationException(String aMessageKey, Object[] aArguments,
          Throwable aCause) {
    super(aMessageKey, aArguments, aCause);
  }
}
