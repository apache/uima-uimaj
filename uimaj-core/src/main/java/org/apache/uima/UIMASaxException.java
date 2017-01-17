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

import java.util.Locale;

import org.xml.sax.SAXException;

/**
 * This is the superclass for all SaxExceptions in UIMA.
 * It provides the link to common implementation in i18nExceptionI (via default methods), and
 * extends SAXException.
 * 
 * <p>It supplies a messages properties file org.apache.uima.UIMASaxException_Messages</p>
 */
public class UIMASaxException extends SAXException implements I18nExceptionI {

  private static final long serialVersionUID = 1L;

  /**
   * The name of the {@link java.util.ResourceBundle ResourceBundle} containing the standard UIMA
   * Exception messages.
   */
  public static final String STANDARD_MESSAGE_CATALOG = "org.apache.uima.UIMASaxException_Messages";

  /**
   * The base name of the resource bundle in which the message for this
   * exception is located.
   */
  private String mResourceBundleName;

  /**
   * An identifier that maps to the message for this exception.
   */
  private String mMessageKey;

  /**
   * The arguments to this exception's message, if any. This allows an
   * <code>InternationalizedException</code> to have a compound message, made
   * up of multiple parts that are concatenated in a language-neutral way.
   */
  private Object[] mArguments;

  /**
   * The exception that caused this exception to occur.
   */
  private Throwable mCause;

  /**
   * Creates a new exception with a null message.
   */
  public UIMASaxException() {
    super();
  }

  /**
   * Creates a new exception with the specified cause and a null message.
   * 
   * @param aCause
   *          the original exception that caused this exception to be thrown, if any
   */
  public UIMASaxException(Exception aCause) {
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
  public UIMASaxException(String aResourceBundleName, String aMessageKey, Object[] aArguments) {
    this(aResourceBundleName, aMessageKey, aArguments, null);
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
  public UIMASaxException(String aResourceBundleName, String aMessageKey, Object[] aArguments,
          Throwable aCause) {
    super();
    this.mResourceBundleName = aResourceBundleName;
    this.mMessageKey = aMessageKey;
    this.mArguments = aArguments;
    this.mCause = aCause;
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
  public UIMASaxException(String aMessageKey, Object[] aArguments) {
    this(STANDARD_MESSAGE_CATALOG, aMessageKey, aArguments, null);
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
  public UIMASaxException(String aMessageKey, Object[] aArguments, Throwable aCause) {
    this(STANDARD_MESSAGE_CATALOG, aMessageKey, aArguments, aCause);
  }
  
  /**
   * Gets the cause of this Exception.
   * 
   * @return the Throwable that caused this Exception to occur, if any. Returns
   *         <code>null</code> if there is no such cause.
   */
  public Throwable getCause() {
     return mCause;
  }

  public synchronized Throwable initCause(Throwable cause) {
     mCause = cause;
     return this;
  }
  
  /**
   * Gets the base name of the resource bundle in which the message for this
   * exception is located.
   * 
   * @return the resource bundle base name. May return <code>null</code> if
   *         this exception has no message.
   */
  public String getResourceBundleName() {
     return mResourceBundleName;
  }

  /**
   * Gets the identifier for this exception's message. This identifier can be
   * looked up in this exception's
   * {@link java.util.ResourceBundle ResourceBundle} to get the locale-specific
   * message for this exception.
   * 
   * @return the resource identifier for this exception's message. May return
   *         <code>null</code> if this exception has no message.
   */
  public String getMessageKey() {
     return mMessageKey;
  }

  /**
   * Gets the arguments to this exception's message. Arguments allow a
   * <code>InternationalizedException</code> to have a compound message, made
   * up of multiple parts that are concatenated in a language-neutral way.
   * 
   * @return the arguments to this exception's message.
   */
  public Object[] getArguments() {
     if (mArguments == null)
        return new Object[0];
     return mArguments.clone();
  }
  /**
   * @return The message of the exception. Useful for including the text in
   *         another exception.
   */
  public String getMessage() {
    return getLocalizedMessage(Locale.ENGLISH);
  }

  /**
   * Gets the localized detail message for this exception. This uses the default
   * Locale for this JVM. A Locale may be specified using
   * {@link #getLocalizedMessage(Locale)}.
   * 
   * @return this exception's detail message, localized for the default Locale.
   */
  public String getLocalizedMessage() {
    return getLocalizedMessage(Locale.getDefault());
  }
}
