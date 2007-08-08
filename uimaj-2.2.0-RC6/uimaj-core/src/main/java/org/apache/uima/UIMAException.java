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
 * This is the superclass for all exceptions in UIMA.
 * <p>
 * <code>UIMAException</code> extends {@link InternationalizedException} for internationalization
 * support. Since UIMA Exceptions are internationalized, the thrower does not supply a hardcoded
 * message. Instead, the thrower specifies a key that identifies the message. That key is then
 * looked up in a locale-specific {@link java.util.ResourceBundle ResourceBundle} to find the actual
 * message associated with this exception.
 * <p>
 * The thrower may specify the name of the <code>ResourceBundle</code> in which to find the
 * exception message. Any name may be used. If the name is omitted, the resource bundle identified
 * by {@link #STANDARD_MESSAGE_CATALOG} will be used. This contains the standard UIMA exception
 * messages.
 * 
 * 
 */
public class UIMAException extends InternationalizedException {

  private static final long serialVersionUID = 7521732353239537026L;

  /**
   * The name of the {@link java.util.ResourceBundle ResourceBundle} containing the standard UIMA
   * Exception messages.
   */
  public static final String STANDARD_MESSAGE_CATALOG = "org.apache.uima.UIMAException_Messages";

  /**
   * Creates a new exception with a null message.
   */
  public UIMAException() {
    super();
  }

  /**
   * Creates a new exception with the specified cause and a null message.
   * 
   * @param aCause
   *          the original exception that caused this exception to be thrown, if any
   */
  public UIMAException(Throwable aCause) {
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
  public UIMAException(String aResourceBundleName, String aMessageKey, Object[] aArguments) {
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
  public UIMAException(String aResourceBundleName, String aMessageKey, Object[] aArguments,
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
  public UIMAException(String aMessageKey, Object[] aArguments) {
    super(STANDARD_MESSAGE_CATALOG, aMessageKey, aArguments);
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
  public UIMAException(String aMessageKey, Object[] aArguments, Throwable aCause) {
    super(STANDARD_MESSAGE_CATALOG, aMessageKey, aArguments, aCause);
  }
}
