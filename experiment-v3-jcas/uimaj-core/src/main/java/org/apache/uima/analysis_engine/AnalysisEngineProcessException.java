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

package org.apache.uima.analysis_engine;

import org.apache.uima.resource.ResourceProcessException;

/**
 * An <code>AnalysisEngineProcessException</code> may be thrown by an Analysis Engine's process
 * method, indicating that a failure occurred during processing.
 * 
 * 
 */
public class AnalysisEngineProcessException extends ResourceProcessException {

  private static final long serialVersionUID = 2815910374191768858L;

  /**
   * Message key for a standard UIMA exception message: "Annotator processing failed."
   */
  public static final String ANNOTATOR_EXCEPTION = "annotator_exception";

  /**
   * Message key for a standard UIMA exception message: "AnalysisEngine subclass {0} does not
   * support CAS class {1}."
   */
  public static final String UNSUPPORTED_CAS_TYPE = "unsupported_cas_type";

  /**
   * Message key for a standard UIMA exception message: "This AnalysisEngine is serving too many
   * simultaneous requests. The timeout period of {0}ms has elapsed."
   */
  public static final String TIMEOUT_ELAPSED = "timeout_elapsed";

  /**
   * Message key for a standard UIMA exception message: "The ASB encountered an unknown Analysis
   * Engine ID "{0}" in the execution sequence."
   */
  public static final String UNKNOWN_ID_IN_SEQUENCE = "unknown_id_in_sequence";

  /**
   * Message key for a standard UIMA exception message: "The FlowController returned a Step object
   * of class {0}, which is not supported by this framework implementation."
   */
  public static final String UNSUPPORTED_STEP_TYPE = "unsupported_step_type";

  /**
   * Message key for a standard UIMA exception message: "The FlowController attempted to drop a CAS
   * that was passed as input to the Aggregate AnalysisEngine containing that FlowController. The
   * only CASes that may be dropped are those that are created within the same Aggregate
   * AnalysisEngine as the FlowController."
   */
  public static final String ILLEGAL_DROP_CAS = "illegal_drop_cas";

  /**
   * Message key for a standard UIMA exception message: "Expected CAS interface {0}, but received
   * interface {1}."
   */
  public static final String INCORRECT_CAS_INTERFACE = "incorrect_cas_interface";

  /**
   * Message key for a standard UIMA exception message: "The FlowController class {0} does not 
   * support the removeAnalysisEngines method.  Analysis Engines cannot be dynamically removed
   * from the flow."
   */
  public static final String REMOVE_AE_FROM_FLOW_NOT_SUPPORTED = "remove_ae_from_flow_not_supported";

  /**
   * Message key for a standard UIMA exception message: "The Analysis Engine(s) {0} have
   * been removed from the flow, and the FlowController has determined the Aggregate 
   * Analysis Engine's processing can no longer continue."
   */
  public static final String FLOW_CANNOT_CONTINUE_AFTER_REMOVE = "flow_cannot_continue_after_remove";

  /**
   * Creates a new exception with a null message.
   */
  public AnalysisEngineProcessException() {
    super();
  }

  /**
   * Creates a new exception with the specified cause and a null message.
   * 
   * @param aCause
   *          the original exception that caused this exception to be thrown, if any
   */
  public AnalysisEngineProcessException(Throwable aCause) {
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
  public AnalysisEngineProcessException(String aResourceBundleName, String aMessageKey,
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
  public AnalysisEngineProcessException(String aResourceBundleName, String aMessageKey,
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
  public AnalysisEngineProcessException(String aMessageKey, Object[] aArguments) {
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
  public AnalysisEngineProcessException(String aMessageKey, Object[] aArguments, Throwable aCause) {
    super(aMessageKey, aArguments, aCause);
  }
}
