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

import org.apache.uima.internal.util.I18nUtil;
import org.apache.uima.internal.util.I18nx_impl;

/**
 * The <code>InternationalizedRuntimeException</code> class adds internationalization support to
 * the standard functionality provided by <code>java.lang.RuntimeException</code>. Because this
 * is a <code>RuntimeException</code>, it does not need to be declared in the throws clause of
 * methods.
 * <p>
 * To support internationalization, the thrower of an exception must not specify a hardcoded message
 * string. Instead, the thrower specifies a key that identifies the message. That key is then looked
 * up in a locale-specific {@link java.util.ResourceBundle ResourceBundle} to find the actual
 * message associated with this exception.
 * <p>
 * This class also supports arguments to messages. The full message will be constructed using the
 * {@link java.text.MessageFormat MessageFormat} class. For more information on
 * internationalization, see the <a href="http://java.sun.com/j2se/1.4/docs/guide/intl/index.html">
 * Java Internationalization Guide</a>.
 * <p>
 * This version of this class works with JDK versions prior to 1.4, since it does not assume support
 * for exception chaining. The file <code>InternationalizedException.java_1_4</code> is a version
 * that uses the exception chaining support built-in to JDK1.4.
 * 
 * 
 */
public class InternationalizedRuntimeException extends RuntimeException {

  private static final long serialVersionUID = 6387360855459370559L;

  private final I18nx_impl c;  // common code 

  /**
   * Creates a new <code>InternationalizedRuntimeException</code> with a null message.
   */
  public InternationalizedRuntimeException() {
    this(null, null, null, null);
  }

  /**
   * Creates a new <code>InternationalizedRuntimeException</code> with the specified cause and a
   * null message.
   * 
   * @param aCause
   *          the original exception that caused this exception to be thrown, if any
   */
  public InternationalizedRuntimeException(Throwable aCause) {
    this(null, null, null, aCause);
  }

  /**
   * Creates a new <code>InternationalizedRuntimeException</code> with the specified message.
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
  public InternationalizedRuntimeException(String aResourceBundleName, String aMessageKey,
          Object[] aArguments) {
    this(aResourceBundleName, aMessageKey, aArguments, null);
  }

  /**
   * Creates a new <code>InternationalizedRuntimeException</code> with the specified message and
   * cause.
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
  public InternationalizedRuntimeException(String aResourceBundleName, String aMessageKey,
          Object[] aArguments, Throwable aCause) {
    super();
    c = new I18nx_impl(aResourceBundleName, aMessageKey, aArguments, aCause);
  }

  /**
   * Gets the base name of the resource bundle in which the message for this exception is located.
   * 
   * @return the resource bundle base name. May return <code>null</code> if this exception has no
   *         message.
   */
  public String getResourceBundleName() {
    return c.getResourceBundleName();
  }

  /**
   * Gets the identifier for this exception's message. This identifier can be looked up in this
   * exception's {@link java.util.ResourceBundle ResourceBundle} to get the locale-specific message
   * for this exception.
   * 
   * @return the resource identifier for this exception's message. May return <code>null</code> if
   *         this exception has no message.
   */
  public String getMessageKey() {
    return c.getMessageKey();
  }

  /**
   * Gets the arguments to this exception's message. Arguments allow a
   * <code>InternationalizedRuntimeException</code> to have a compound message, made up of
   * multiple parts that are concatenated in a language-neutral way.
   * 
   * @return the arguments to this exception's message.
   */
  public Object[] getArguments() {
    return c.getArguments();
  }

  /**
   * Gets the <i>English</i> detail message for this exception. For the localized message use
   * {@link #getLocalizedMessage()}.
   * 
   * @return the English detail message for this exception.
   */
  public String getMessage() {
    return c.getMessage();
  }

  /**
   * Gets the localized detail message for this exception. This uses the default Locale for this
   * JVM. A Locale may be specified using {@link #getLocalizedMessage(Locale)}.
   * 
   * @return this exception's detail message, localized for the default Locale.
   */
  public String getLocalizedMessage() {
    return c.getLocalizedMessage();
  }

  /**
   * Gets the localized detail message for this exception using the specified <code>Locale</code>.
   * 
   * @param aLocale
   *          the locale to use for localizing the message
   * 
   * @return this exception's detail message, localized for the specified <code>Locale</code>.
   */
  public String getLocalizedMessage(Locale aLocale) {
    return c.getLocalizedMessage(aLocale);
  }

  /**
   * Gets the cause of this Exception.
   * 
   * @return the Throwable that caused this Exception to occur, if any. Returns <code>null</code>
   *         if there is no such cause.
   */
  public Throwable getCause() {
    return c.getCause();
  }

  public synchronized Throwable initCause(Throwable cause) {
    c.setCause(cause);
    return this;
  }

  /**
   * For the case where the default locale is not being used for getting messages,
   * and the lookup path in the classpath for the resource bundle needs to be set 
   * at a specific point, call this method to set the resource bundle at that point in the call stack.
   * 
   * Example: If in a Pear, and you are throwing an exception, which is defined in a bundle
   * in the Pear context, but the catcher of the throw is up the stack above where the pear context
   * exists (and therefore, is no longer present at "catch" time), and
   * you don't want to use the default-locale for getting the message out of the message bundle,
   * 
   * then do something like this
   *   Exception e = new AnalysisEngineProcessException(MESSAGE_BUNDLE, "TEST_KEY", objects);
   *   e.setResourceBundle(my_locale);  // call this method, pass in the needed locale object
   *   throw e;  // or whatever should be done with it
   * @param aLocale the locale to use when getting the message from the message bundle at a later time
   */
  public void setResourceBundle(Locale aLocale) {
    c.setResourceBundle(aLocale);
  }

}
