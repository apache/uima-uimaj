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
 * Signals that a method has been invoked at an illegal or inappropriate time. In other words, the
 * object on which the method was called is not in an appropriate state for the requested operation.
 * This extends <code>RuntimeException</code> and so does not need to be declared in the throws
 * clause of methods.
 * 
 * 
 */
public class UIMA_IllegalStateException extends UIMARuntimeException {

  private static final long serialVersionUID = -8081807814100358556L;

  /**
   * Message key for a standard UIMA exception message: "The UIMA framework implementation (class
   * {0}) could not be created."
   */
  public static final String COULD_NOT_CREATE_FRAMEWORK = "could_not_create_framework";

  /**
   * Message key for a standard UIMA exception message: "The initialize(ResourceSpecifier) method on
   * Resource {0} was called more than once. A Resource may only be initialized once."
   */
  public static final String RESOURCE_ALREADY_INITIALIZED = "resource_already_initialized";

  /**
   * Message key for a standard UIMA exception message: "The XML parser was configured to
   * instantiate class {0}, but that class is not able to be instantiated."
   */
  public static final String COULD_NOT_INSTANTIATE_XMLIZABLE = "could_not_instantiate_xmlizable";

  /**
   * Message key for a standard UIMA exception message: "This ASB has not been provided with any
   * Analysis Engines with which to communicate"
   */
  public static final String NO_DELEGATE_ANALYSIS_ENGINES = "no_delegate_analysis_engines";

  /**
   * Message key for a standard UIMA exception message: "The method {0} must be called before the
   * method {1}."
   */
  public static final String REQUIRED_METHOD_CALL = "required_method_call";

  /**
   * Message key for a standard UIMA exception message: There are no more elements in the
   * collection.
   */
  public static final String READ_PAST_END_OF_COLLECTION = "read_past_end_of_collection";

  /**
   * Message key for a standard UIMA exception message: CasIterator.next was called when there were
   * no more CASes remaining to be read.
   */
  public static final String NO_NEXT_CAS = "no_next_cas";

  /**
   * Message key for a standard UIMA exception message: ResourceManager.setCasManager was called
   * after the CAS Manager had already been set.  You can only call setCasManager once, and you
   * cannot have previously called ResourceManager.getCasManager or initialized any
   * AnalysisEngines that use this ResourceManager.
   */
  public static final String CANNOT_SET_CAS_MANAGER = "cannot_set_cas_manager";
  
  /**
   * Creates a new exception with a null message.
   */
  public UIMA_IllegalStateException() {
    super();
  }

  /**
   * Creates a new exception with the specified cause and a null message.
   * 
   * @param aCause
   *          the original exception that caused this exception to be thrown, if any
   */
  public UIMA_IllegalStateException(Throwable aCause) {
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
  public UIMA_IllegalStateException(String aResourceBundleName, String aMessageKey,
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
  public UIMA_IllegalStateException(String aResourceBundleName, String aMessageKey,
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
  public UIMA_IllegalStateException(String aMessageKey, Object[] aArguments) {
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
  public UIMA_IllegalStateException(String aMessageKey, Object[] aArguments, Throwable aCause) {
    super(aMessageKey, aArguments, aCause);
  }
}
