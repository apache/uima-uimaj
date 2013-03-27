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
 * This is the superclass for all runtime exceptions in UIMA. Runtime exceptions do not need to be
 * declared in the throws clause of methods.
 * <p>
 * <code>UIMARuntimeException</code> extends {@link InternationalizedRuntimeException} for
 * internationalization support. Since UIMA Runtime Exceptions are internationalized, the thrower
 * does not supply a hardcoded message. Instead, the thrower specifies a key that identifies the
 * message. That key is then looked up in a locale-specific
 * {@link java.util.ResourceBundle ResourceBundle} to find the actual message associated with this
 * exception.
 * <p>
 * The thrower may specify the name of the <code>ResourceBundle</code> in which to find the
 * exception message. Any name may be used. If the name is omitted, the resource bundle identified
 * by {@link #STANDARD_MESSAGE_CATALOG} will be used. This contains the standard UIMA exception
 * messages.
 * 
 * 
 */
public class UIMARuntimeException extends InternationalizedRuntimeException {

  private static final long serialVersionUID = 6738051692628592989L;

  /**
   * The name of the {@link java.util.ResourceBundle ResourceBundle} containing the standard UIMA
   * Exception messages.
   */
  public static final String STANDARD_MESSAGE_CATALOG = "org.apache.uima.UIMAException_Messages";

  /**
   * Message key for a standard UIMA exception message: CasManager.releaseCas(CAS) was called with a
   * CAS that does not belong to this CasManager.
   */
  public static final String CAS_RELEASED_TO_WRONG_CAS_MANAGER = "cas_released_to_wrong_cas_manager";

  /**
   * Message key for a standard UIMA exception message: "The Ecore model for the UIMA built-in types
   * (uima.ecore) was not found in the classpath."
   */
  public static final String UIMA_ECORE_NOT_FOUND = "uima_ecore_not_found";

  /**
   * Message key for a standard UIMA exception message: "The Ecore model contained an unresolved
   * proxy {0}."
   */
  public static final String ECORE_UNRESOLVED_PROXY = "ecore_unresolved_proxy";

  /**
   * Message key for a standard UIMA exception message: "AnalysisComponent "{0}" requested more
   * CASes ({1}) than defined in its getCasInstancesRequired() method ({2}). It is possible that the
   * AnalysisComponent is not properly releasing CASes when it encounters an error."
   */
  public static final String REQUESTED_TOO_MANY_CAS_INSTANCES = "requested_too_many_cas_instances";

  /**
   * Message key for a standard UIMA exception message: "The method CasManager.defineCasPool() was
   * called twice by the same Analysis Engine ({0)}."
   */
  public static final String DEFINE_CAS_POOL_CALLED_TWICE = "define_cas_pool_called_twice";

  /**
   * Message key for a standard UIMA exception message: "Unsupported CAS interface {0}."
   */
  public static final String UNSUPPORTED_CAS_INTERFACE = "unsupported_cas_interface";

  /**
   * Message key for a standard UIMA exception message: "Incompatible taf jni library {0}."
   */
  public static final String INCOMPATIBLE_TAF_JNI_LIBRARY = "incompatible_taf_jni_library";

  /**
   * Message key for a standard UIMA exception message: "Attempted to copy a FeatureStructure of
   * type "{0}", which is not defined in the type system of the destination CAS."
   */
  public static final String TYPE_NOT_FOUND_DURING_CAS_COPY = "type_not_found_during_cas_copy";

  /**
   * Message key for a standard UIMA exception message: "Attempted to copy a Feature "{0}", which is
   * not defined in the type system of the destination CAS."
   */
  public static final String FEATURE_NOT_FOUND_DURING_CAS_COPY = "feature_not_found_during_cas_copy";

  /**
   * Message key for a standard UIMA exception message: 
   * Saved UIMA context is null; probable cause: Annotator initialize(context) method failed to call super.initialize(context). 
   */
  public static final String UIMA_CONTEXT_NULL = "uima_context_null";

  /**
   * Message key for a standard UIMA exception message: 
   * Saved result specification is null; probable cause: Annotator overrode setResultSpecification(spec) but failed to call super.setResultSpecification(spec). 
   */
  public static final String RESULT_SPEC_NULL = "result_spec_null";

  /**
   * Creates a new exception with a null message.
   */
  public UIMARuntimeException() {
    super();
  }

  /**
   * Creates a new exception with the specified cause and a null message.
   * 
   * @param aCause
   *          the original exception that caused this exception to be thrown, if any
   */
  public UIMARuntimeException(Throwable aCause) {
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
  public UIMARuntimeException(String aResourceBundleName, String aMessageKey, Object[] aArguments) {
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
  public UIMARuntimeException(String aResourceBundleName, String aMessageKey, Object[] aArguments,
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
  public UIMARuntimeException(String aMessageKey, Object[] aArguments) {
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
  public UIMARuntimeException(String aMessageKey, Object[] aArguments, Throwable aCause) {
    super(STANDARD_MESSAGE_CATALOG, aMessageKey, aArguments, aCause);
  }
}
