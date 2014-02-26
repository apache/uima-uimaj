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

package org.apache.uima.resource;

import java.util.Map;

import org.apache.uima.UIMAException;

/**
 * Thrown by {@link Resource#initialize(ResourceSpecifier,Map)} to indicate that a failure has
 * occurred during initialization.
 * 
 * 
 */
public class ResourceInitializationException extends UIMAException {

  private static final long serialVersionUID = -2521675776941322837L;

  /**
   * Message key for a standard UIMA exception message: "Error initializing "{0}" from descriptor
   * {1}."
   */
  public static final String ERROR_INITIALIZING_FROM_DESCRIPTOR = "error_initializing_from_descriptor";

  /**
   * Message key for a standard UIMA exception message: "Annotator class name is required for a
   * primitive Text Analysis Engine."
   */
  public static final String MISSING_ANNOTATOR_CLASS_NAME = "missing_annotator_class_name";

  /**
   * Message key for a standard UIMA exception message: "Annotator class "{0}" was not found."
   */
  public static final String ANNOTATOR_CLASS_NOT_FOUND = "annotator_class_not_found";

  /**
   * Message key for a standard UIMA exception message: "Class "{0}" is not an Annotator."
   */
  public static final String NOT_AN_ANNOTATOR = "not_an_annotator";

  /**
   * Message key for a standard UIMA exception message: "Annotator class {0} does not implement an
   * Annotator interface that is supported by Analysis Engine implementation {1}."
   */
  public static final String ANNOTATOR_INTERFACE_NOT_SUPPORTED = "annotator_interface_not_supported";

  /**
   * Message key for a standard UIMA exception message: "Could not instantiate Annotator class
   * "{0}". Check that your annotator class is not abstract and has a zero-argument constructor."
   */
  public static final String COULD_NOT_INSTANTIATE_ANNOTATOR = "could_not_instantiate_annotator";

  /**
   * Message key for a standard UIMA exception message: "Initialization of annotator class "{0}"
   * failed."
   */
  public static final String ANNOTATOR_INITIALIZATION_FAILED = "annotator_initialization_failed";

  /**
   * Message key for a standard UIMA exception message: "The feature {0} is declared twice, with 
   * incompatible range types {1} and {2}. (Descriptor: {3})"
   */
  public static final String INCOMPATIBLE_RANGE_TYPES = "incompatible_range_types";

  /**
   * Message key for a standard UIMA exception message: "The feature {0} is declared twice, with 
   * incompatible element range types {1} and {2}. (Descriptor: {3})."
   */
  public static final String INCOMPATIBLE_ELEMENT_RANGE_TYPES = "incompatible_element_range_types";

  /**
   * Message key for a standard UIMA exception message: "The Type {0} is declared twice, with
   * incompatible supertypes {1} and {2}."
   */
  public static final String INCOMPATIBLE_SUPERTYPES = "incompatible_supertypes";

  /**
   * Message key for a standard UIMA exception message: "The feature {0} is declared twice, with 
   * incompatible multipleReferencesAllowed specifications. (Descriptor: {1})"
   */
  public static final String INCOMPATIBLE_MULTI_REFS = "incompatible_multi_refs";

  /**
   * Message key for a standard UIMA exception message: The String subtype {0} is declared twice, 
   * with different sets of allowed values: {1} and {2}.  (Descriptor: {3})
   */
  public static final String ALLOWED_VALUES_NOT_IDENTICAL = "string_allowed_values_not_the_same";
  
  /**
   * Message key for a standard UIMA exception message: "Undefined type "{0}", referenced in feature
   * "{1}" declared on type "{2}"."
   */
  public static final String UNDEFINED_RANGE_TYPE = "undefined_range_type";

  /**
   * Message key for a standard UIMA exception message: "Undefined type "{0}", referenced as
   * supertype of type "{1}"."
   */
  public static final String UNDEFINED_SUPERTYPE = "undefined_supertype";

  /**
   * Message key for a standard UIMA exception message: "Undefined type "{0}", referenced in
   * definition of index "{1}"."
   */
  public static final String UNDEFINED_TYPE_FOR_INDEX = "undefined_type_for_index";

  /**
   * Message key for a standard UIMA exception message: "Undefined type "{0}" in type priority
   * list."
   */
  public static final String UNDEFINED_TYPE_FOR_PRIORITY_LIST = "undefined_type_for_priority_list";

  /**
   * Message key for a standard UIMA exception message: "The key Feature "{0}" declared for Index
   * "{1}" was not found."
   */
  public static final String INDEX_KEY_FEATURE_NOT_FOUND = "index_key_feature_not_found";

  /**
   * Message key for a standard UIMA exception message: "The Analysis Engine Descriptor is invalid -
   * a Type System may not be explicitly set for an aggregate Analysis Engine."
   */
  public static final String AGGREGATE_AE_TYPE_SYSTEM = "aggregate_ae_type_system";

  /**
   * Message key for a standard UIMA exception message: "Type {0} extends String and must specify a
   * list of allowed values."
   */
  public static final String MISSING_ALLOWED_VALUES = "missing_allowed_values";

  /**
   * Message key for a standard UIMA exception message: "Type {0} specifies a list of allowed values
   * but is not a subtype of uima.cas.String. You may only specify a list of allowed values for
   * string subtypes."
   */
  public static final String ALLOWED_VALUES_ON_NON_STRING_TYPE = "allowed_values_on_non_string_type";

  /**
   * Message key for a standard UIMA exception message: "Duplicate configuration parameter name
   * "{0}" in component "{1}"."
   */
  public static final String DUPLICATE_CONFIGURATION_PARAMETER_NAME = "duplicate_configuration_parameter_name";

  /**
   * Message key for a standard UIMA exception message: "This resource requires {0} parameter(s)."
   */
  public static final String INCORRECT_NUMBER_OF_PARAMETERS = "incorrect_number_of_parameters";

  /**
   * Message key for a standard UIMA exception message: "No resource could be found for the
   * parameters {0}."
   */
  public static final String NO_RESOURCE_FOR_PARAMETERS = "no_resource_for_parameters";

  /**
   * Message key for a standard UIMA exception message: "Could not access the resource data at {0}."
   */
  public static final String COULD_NOT_ACCESS_DATA = "could_not_access_data";

  /**
   * Message key for a standard UIMA exception message: "The Resource class {0} could not be found."
   */
  public static final String CLASS_NOT_FOUND = "class_not_found";

  /**
   * Message key for a standard UIMA exception message: "Could not instantiate class {0}."
   */
  public static final String COULD_NOT_INSTANTIATE = "could_not_instantiate";

  /**
   * Message key for a standard UIMA exception message: "The Resource Factory does not know how to
   * create a resource of class {0} from the given ResourceSpecifier."
   */
  public static final String DO_NOT_KNOW_HOW = "do_not_know_how";

  /**
   * Message key for a standard UIMA exception message: "The class {0} does not implement
   * org.apache.uima.resource.SharedResourceObject."
   */
  public static final String NOT_A_SHARED_RESOURCE_OBJECT = "not_a_shared_resource_object";

  /**
   * Message key for a standard UIMA exception message: "For resource "{0}", could not load data
   * from class {1} into class {2}, because class {1} is not a Data Resource."
   */
  public static final String NOT_A_DATA_RESOURCE = "not_a_data_resource";

  /**
   * Message key for a standard UIMA exception message: "No resource with name "{0}" has been
   * declared in this Analysis Engine."
   */
  public static final String UNKNOWN_RESOURCE_NAME = "unknown_resource_name";

  /**
   * Message key for a standard UIMA exception message: "The resource with name "{0}" does not
   * implement the required interface {1}."
   */
  public static final String RESOURCE_DOES_NOT_IMPLEMENT_INTERFACE = "resource_does_not_implement_interface";

  /**
   * Message key for a standard UIMA exception message: "There is no resource satisfying the
   * required resource dependency with key "{0}"."
   */
  public static final String RESOURCE_DEPENDENCY_NOT_SATISFIED = "resource_dependency_not_satisfied";

  /**
   * Message key for a standard UIMA exception message: "Unknown Protocol: "{0}"."
   */
  public static final String UNKNOWN_PROTOCOL = "unknown_protocol";

  /**
   * Message key for a standard UIMA exception message: "Malformed URL "{0}"."
   */
  public static final String MALFORMED_URL = "malformed_url";

  /**
   * Message key for a standard UIMA exception message: "The configuration data {0} for Configuraion
   * parameter {1} in the resource is absent or not valid"
   */
  public static final String RESOURCE_DATA_NOT_VALID = "resource_data_not_valid";

  /**
   * Message key for a standard UIMA exception message: Configuration setting for {0} is absent
   */
  public static final String CONFIG_SETTING_ABSENT = "config_setting_absent";

  /**
   * Message key for a standard UIMA exception message: Two different CAS FeatureStructure indexes
   * with name "{0}" have been defined.
   */
  public static final String DUPLICATE_INDEX_NAME = "duplicate_index_name";

  /**
   * Message key for a standard UIMA exception message: Configuration parameter "{0}" in primitive
   * Analysis Engine "{1}" declares an override. Parameter overrides are allowed only in aggregate
   * Analysis Engines.
   */
  public static final String PARAM_OVERRIDE_IN_PRIMITIVE = "param_override_in_primitive";

  /**
   * Message key for a standard UIMA exception message: Configuration parameter "{0}" in aggregate
   * Analysis Engine "{1}" does not declare any overrides.  Implicit overrides are no longer supported. 
   * (Descriptor: {2})
   */
  public static final String INVALID_PARAM_OVERRIDE_NO_OVERRIDES = "invalid_param_override_no_overrides";

  /**
   * Message key for a standard UIMA exception message: Invalid configuration parameter override
   * syntax "{0}" in parameter "{1}" of Analysis Engine "{2}". Overrides must be of the form
   * "&lt;delegateKey&gt;/&lt;paramName&gt;"
   */
  public static final String INVALID_PARAM_OVERRIDE_SYNTAX = "invalid_param_override_syntax";

  /**
   * Message key for a standard UIMA exception message: Invalid configuration parameter override
   * "{0}" in parameter "{1}" of Analysis Engine "{2}" -- delegate Analysis Engine "{3}" does not
   * exist.
   */
  public static final String INVALID_PARAM_OVERRIDE_NONEXISTENT_DELEGATE = "invalid_param_override_nonexistent_delegate";

  /**
   * Message key for a standard UIMA exception message: Invalid configuration parameter override
   * "{0}" in parameter "{1}" of Analysis Engine "{2}" -- delegate Analysis Engine "{3}" does not
   * declare parameter {4}.
   */
  public static final String INVALID_PARAM_OVERRIDE_NONEXISTENT_PARAMETER = "invalid_param_override_nonexistent_parameter";

  /**
   * Message key for a standard UIMA exception message: Invalid configuration parameter override
   * "{0}" in parameter "{1}" of Analysis Engine "{2}" -- delegate Analysis Engine "{3}" does not
   * declare parameter {4} in group {5}.
   */
  public static final String INVALID_PARAM_OVERRIDE_NONEXISTENT_PARAMETER_IN_GROUP = "invalid_param_override_nonexistent_parameter_in_group";

  /**
   * Message key for a standard UIMA exception message: The output Sofa "{0}" in component "{1}" is
   * not mapped to any output Sofa in its containing aggregate, "{2}".
   */
  public static final String OUTPUT_SOFA_NOT_DECLARED_IN_AGGREGATE = "output_sofa_not_declared_in_aggregate";

  /**
   * Message key for a standard UIMA exception message: The input Sofa "{0}" in component "{1}" is
   * not an input of the containing aggregate, "{2}", nor is it an output of another component in
   * the same aggregate.
   */
  public static final String INPUT_SOFA_HAS_NO_SOURCE = "input_sofa_has_no_source";

  /**
   * Message key for a standard UIMA exception message: The Sofa "{0}" in aggregate "{1}" is not
   * mapped to any sofa of any component in that aggregate.
   */
  public static final String AGGREGATE_SOFA_NOT_MAPPED = "aggregate_sofa_not_mapped";

  /**
   * Message key for a standard UIMA exception message: The Sofa "{0}" in component "{1}" of
   * aggregate "{2}" has conflicting mappings to aggregate sofas "{3}" and "{4}".
   */
  public static final String SOFA_MAPPING_CONFLICT = "sofa_mapping_conflict";

  /**
   * Message key for a standard UIMA exception message: An implementation class name is missing from
   * the descriptor.
   */
  public static final String MISSING_IMPLEMENTATION_CLASS_NAME = "missing_implementation_class_name";

  /**
   * Message key for a standard UIMA exception message: "Error creating CAS Processor with name
   * "{0}". The descriptor type "{1}" is not allowed - you must specify an AnalysisEngine or CAS
   * Consumer descriptor."
   */
  public static final String NOT_A_CAS_PROCESSOR = "not_a_cas_processor";

  /**
   * Message key for a standard UIMA exception message: "A CollectionReader descriptor specified
   * implementation class "{0}", but this class does not implement the CollectionReader interface."
   */
  public static final String NOT_A_COLLECTION_READER = "not_a_collection_reader";

  /**
   * Message key for a standard UIMA exception message: "A CasConsumer descriptor specified
   * implementation class "{0}", but this class does not implement the CasConsumer interface."
   */
  public static final String NOT_A_CAS_CONSUMER = "not_a_cas_consumer";

  /**
   * Message key for a standard UIMA exception message: "A CasInitializer descriptor specified
   * implementation class "{0}", but this class does not implement the CasInitializer interface."
   */
  public static final String NOT_A_CAS_INITIALIZER = "not_a_cas_initializer";

  /**
   * Message key for a standard UIMA exception message: "Initialization of CAS Processor with name
   * "{0}" failed.
   */
  public static final String CAS_PROCESSOR_INITIALIZE_FAILED = "cas_processor_initialize_failed";

  /**
   * Message key for a standard UIMA exception message: "The descriptor for aggregate AnalysisEngine
   * "{0}" declared multipleDeploymentAllowed == true, but its component "{1}" declared
   * multipleDeploymentAllowed == false, which is inconsistent."
   */
  public static final String INVALID_MULTIPLE_DEPLOYMENT_ALLOWED = "invalid_multiple_deployment_allowed";

  /**
   * Message key for a standard UIMA exception message: "The descriptor for aggregate AnalysisEngine
   * "{0}" declared modifiesCas == false, but its component "{1}" declared modifiesCas == true,
   * which is inconsistent."
   */
  public static final String INVALID_MODIFIES_CAS = "invalid_modifies_cas";

  /**
   * Message key for a standard UIMA exception message: "Invalid type priorities."
   */
  public static final String INVALID_TYPE_PRIORITIES = "invalid_type_priorities";

  /**
   * Message key for a standard UIMA exception message: "Type "{0}" does not define a supertype."
   */
  public static final String NO_SUPERTYPE = "no_supertype";

  /**
   * Message key for a standard UIMA exception message: Undefined component key "{1}", referenced in
   * Sofa mapping for Sofa "{2}" of aggregate "{3}".
   */
  public static final String SOFA_MAPPING_HAS_UNDEFINED_COMPONENT_KEY = "sofa_mapping_has_undefined_component_key";

  /**
   * Message key for a standard UIMA exception message: Component "{0}" does not contain Sofa "{1}",
   * referenced in Sofa mapping for Sofa "{2}" of aggregate "{3}".
   */
  public static final String SOFA_MAPPING_HAS_UNDEFINED_COMPONENT_SOFA = "sofa_mapping_has_undefined_component_sofa";

  /**
   * Message key for a standard UIMA exception message: "The class {0} is not a valid Analysis
   * Component. You must specify an Annotator, CAS Consumer, Collection Reader, or CAS Multiplier. "
   */
  public static final String NOT_AN_ANALYSIS_COMPONENT = "not_an_analysis_component";

  /**
   * Message key for a standard UIMA exception message: "An Aggregate Analysis Engine specified a
   * Flow Controller descriptor {0} of an invalid type ({1})). A FlowControllerDescription is
   * required."
   */
  public static final String NOT_A_FLOW_CONTROLLER_DESCRIPTOR = "not_a_flow_controller_descriptor";

  /**
   * Message key for a standard UIMA exception message: "{0} is not a supported framework
   * implementation"
   */
  public static final String UNSUPPORTED_FRAMEWORK_IMPLEMENTATION = "unsupported_framework_implementation";

  /**
   * Message key for a standard UIMA exception message: "The descriptor for aggregate AnalysisEngine
   * "{0}" declared outputsNewCASes == true, but all of its components declared outputsNewCASes ==
   * false, which is inconsistent."
   */
  public static final String INVALID_OUTPUTS_NEW_CASES = "invalid_outputs_new_CASes";

  /**
   * Message key for a standard UIMA exception message: "The aggregate AnalysisEngine "{0}" declared
   * an empty &lt;flowController/&gt; element. You must specify an import or a flowControllerDescription."
   */
  public static final String EMPTY_FLOW_CONTROLLER_DECLARATION = "empty_flow_controller_declaration";

  /**
   * Message key for a standard UIMA exception message: "The primitive AnalysisEngine "{0}" has an
   * annotator of type {1} but its descriptor declares input or output Sofas. Text annotators are
   * not allowed to declare input or output Sofas. Consider extending CasAnnotator_ImplBase or
   * JCasAnnotator_ImplBase instead."
   */
  public static final String TEXT_ANNOTATOR_CANNOT_BE_SOFA_AWARE = "text_annotator_cannot_be_sofa_aware";

  /**
   * Message key for a standard UIMA exception message: "Component descriptor did not specify the
   * required &lt;frameworkImplementation&gt; element."
   */
  public static final String MISSING_FRAMEWORK_IMPLEMENTATION = "missing_framework_implementation";

  /**
   * Message key for a standard UIMA exception message: "The CasCreationUtils.createCas method was
   * passed a collection containing an object of class {0}, which is not supported.  Refer to the
   * Javadoc for a list of types accepted by this method."
   */
  public static final String UNSUPPORTED_OBJECT_TYPE_IN_CREATE_CAS = "unsupported_object_type_in_create_cas";

  /**
   * Message key for a standard UIMA exception message: "Sofa mappings were specified for the remote Analysis
   * Engine {0}.  Sofa mappings are not currently supported for remote Analysis Engines.  A workaround is
   * to wrap the remotely deployed AE in an Aggregate (on the remote side), and specify Sofa mappings in that
   * aggregate."
   */
  public static final String SOFA_MAPPING_NOT_SUPPORTED_FOR_REMOTE = "sofa_mapping_not_supported_for_remote";

  /**
   * Message key for a standard UIMA exception message: The descriptor for Aggregate Analysis Engine "{0}" 
   * specified an invalid flow.  The key "{1}" was used in the flow but is not defined as a key in the
   * &lt;delegateAnalysisEngineSpecifiers&gt; element of the descriptor.
   */
  public static final String UNDEFINED_KEY_IN_FLOW = "undefined_key_in_flow";

  /**
   * Message key for a standard UIMA exception message: The value "{0}" is an invalid value for
   * the FixedFlowController's "ActionAfterCasMultiplier" configuration parameter.  Valid values
   * are "continue", "stop", "drop", and "dropIfNewCasProduced".
   */
  public static final String INVALID_ACTION_AFTER_CAS_MULTIPLIER = "invalid_action_after_cas_multiplier";

  /**
   * Message key for a standard UIMA exception message: The Flow Controller "{0}" requires a flow constraints
   * element of type "{1}" in the aggregate descriptor
   */
  public static final String FLOW_CONTROLLER_REQUIRES_FLOW_CONSTRAINTS = "flow_controller_requires_flow_constraints";

  /**
   * Message key for a standard UIMA exception message: The aggregate "{0}" references a non-existent delegate
   * "{1}" in it's Flow Controller's flow constraints
   */
  public static final String FLOW_CONTROLLER_MISSING_DELEGATE = "flow_controller_missing_delegate";

  /**
   * Message key for a standard UIMA exception message: 
   * Unexpected Exception thrown when initializing Custom Resource "{0}" from descriptor "{1}". 
   */
  public static final String EXCEPTION_WHEN_INITIALIZING_CUSTOM_RESOURCE = "exception_when_initializing_custom_resource";

  /**
   * Message key for a standard UIMA exception message: 
   * Unexpected Throwable or Error thrown when initializing Custom Resource "{0}" from descriptor "{1}". 
   */
  public static final String THROWABLE_WHEN_INITIALIZING_CUSTOM_RESOURCE = "throwable_when_initializing_custom_resource";
  
  public static final String REDEFINING_BUILTIN_TYPE = "redefining_builtin_type";

  /**
   * Creates a new exception with a null message.
   */
  public ResourceInitializationException() {
    super();
  }

  /**
   * Creates a new exception with the specified cause and a null message.
   * 
   * @param aCause
   *          the original exception that caused this exception to be thrown, if any
   */
  public ResourceInitializationException(Throwable aCause) {
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
  public ResourceInitializationException(String aResourceBundleName, String aMessageKey,
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
  public ResourceInitializationException(String aResourceBundleName, String aMessageKey,
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
  public ResourceInitializationException(String aMessageKey, Object[] aArguments) {
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
  public ResourceInitializationException(String aMessageKey, Object[] aArguments, Throwable aCause) {
    super(aMessageKey, aArguments, aCause);
  }
}
