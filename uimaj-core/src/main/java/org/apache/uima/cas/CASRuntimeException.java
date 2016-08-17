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

package org.apache.uima.cas;

import org.apache.uima.UIMARuntimeException;

/**
 * Runtime exception class for package org.apache.uima.cas.
 * Messages in org.apache.uima.UIMAException_Messages
 */
public class CASRuntimeException extends UIMARuntimeException {

	private static final long serialVersionUID = 2597111625069248081L;

	/** Can''t create FS of type "{0}" with this method. */
	public static final String NON_CREATABLE_TYPE = "NON_CREATABLE_TYPE";

	/** Array size must be &gt;= 0. */
	public static final String ILLEGAL_ARRAY_SIZE = "ILLEGAL_ARRAY_SIZE";

	/** Expected value of type "{0}", but found "{1}". */
	public static final String INAPPROP_TYPE = "INAPPROP_TYPE";

	/** Feature "{0}" is not defined for type "{1}". */
	public static final String INAPPROP_FEAT = "INAPPROP_FEAT";

	/**
   * Trying to access value of feature "{0}" as "{1}", but range of feature is "{2}".
   */
	public static final String INAPPROP_RANGE = "INAPPROP_RANGE";

	/**
   * Trying to access value of feature "{0}" as feature structure, but is primitive type.
   */
	public static final String PRIMITIVE_VAL_FEAT = "PRIMITIVE_VAL_FEAT";

	/** Error accessing type system: the type system has not been committed. */
	public static final String TYPESYSTEM_NOT_LOCKED = "TYPESYSTEM_NOT_LOCKED";

	/**
   * Error setting string value: string "{0}" is not valid for a value of type "{1}".
   */
	public static final String ILLEGAL_STRING_VALUE = "ILLEGAL_STRING_VALUE";

	/** Error applying FS constraString: no type "{0}" in current type system. */
	public static final String UNKNOWN_CONSTRAINT_TYPE = "UNKNOWN_CONSTRAINT_TYPE";

	/**
   * Error applying FS constraString: no feature "{0}" in current type system.
   */
	public static final String UNKNOWN_CONSTRAINT_FEAT = "UNKNOWN_CONSTRAINT_FEAT";

	/** Error accessing child node in tree, index out of range. */
	public static final String CHILD_INDEX_OOB = "CHILD_INDEX_OOB";

	/** CAS type system doesn''t match JCas Type definition for type "{0}". */
	public static final String JCAS_CAS_MISMATCH = "JCAS_CAS_MISMATCH";

	/**
   * JCas type "{0}" used in Java code, but was not declared in the XML type descriptor.
   */
	public static final String JCAS_TYPE_NOT_IN_CAS = "JCAS_TYPE_NOT_IN_CAS";

	/**
   * Unknown JCas type used in Java code but was not declared or imported in the XML descriptor for
   * this component.
   */
	public static final String JCAS_UNKNOWN_TYPE_NOT_IN_CAS = "JCAS_UNKNOWN_TYPE_NOT_IN_CAS";

	/**
   * JCas getNthElement method called via invalid object - an empty list: {0}.
   */
	public static final String JCAS_GET_NTH_ON_EMPTY_LIST = "JCAS_GET_NTH_ON_EMPTY_LIST";

	/** JCas getNthElement method called with index "{0}" which is negative. */
	public static final String JCAS_GET_NTH_NEGATIVE_INDEX = "JCAS_GET_NTH_NEGATIVE_INDEX";

	/**
   * JCas getNthElement method called with index "{0}" larger than the length of the list.
   */
	public static final String JCAS_GET_NTH_PAST_END = "JCAS_GET_NTH_PAST_END";

	/**
   * JCas is referencing via a JFSIterator or get method, a type, "{0}", which has no JCAS class
   * model. You must use FSIterator instead of JFSIterator.
   */
	public static final String JCAS_OLDSTYLE_REF_TO_NONJCAS_TYPE = "JCAS_OLDSTYLE_REF_TO_NONJCAS_TYPE";

	/**
   * A CAS iterator or createFS call is trying to make an instance of type "{0}", but that type has
   * been declared "abstract" in JCas, and no instances are allowed to be made.
   */
	public static final String JCAS_MAKING_ABSTRACT_INSTANCE = "JCAS_MAKING_ABSTRACT_INSTANCE";

	/**
   * The method "{0}" is not supported by this JCAS because it is not associated with a CAS view of
   * a CAS, but rather just with a base CAS.
   */
	public static final String JCAS_UNSUPPORTED_OP_NOT_CAS = "JCAS_UNSUPPORTED_OP_NOT_CAS";

	/** A sofaFS with name {0} has already been created. */
	public static final String SOFANAME_ALREADY_EXISTS = "SOFANAME_ALREADY_EXISTS";

	/** Data for Sofa feature {0} has already been set. */
	public static final String SOFADATA_ALREADY_SET = "SOFADATA_ALREADY_SET";

	/** No sofaFS with name {0} found. */
	public static final String SOFANAME_NOT_FOUND = "SOFANAME_NOT_FOUND";

	/** No sofaFS for specified sofaRef found. */
	public static final String SOFAREF_NOT_FOUND = "SOFAREF_NOT_FOUND";

  /** Sofa reference for FS {0} is required, but it is not set.  This can happen during deserialization when the type system changes where this FeatureStructure''s type definition is now a subtype of uima.cas.AnnotationBase but was not when the serialized form was created.*/
  public static final String SOFAREF_NOT_SET= "SOFAREF_NOT_SET";

	/** Can''t use standard set methods with SofaFS features. */
	public static final String PROTECTED_SOFA_FEATURE = "PROTECTED_SOFA_FEATURE";

	/** The JCAS cover class "{0}" could not be loaded. */
	public static final String JCAS_MISSING_COVERCLASS = "JCAS_MISSING_COVERCLASS";

	/** The feature path "{0}" is not valid. */
	public static final String INVALID_FEATURE_PATH = "INVALID_FEATURE_PATH";

	/** The feature path does not end in a primitive valued feature. */
	public static final String NO_PRIMITIVE_TAIL = "NO_PRIMITIVE_TAIL";

	/**
   * Error trying to do binary serialization of CAS data and write the BLOB to an output stream.
   */
	public static final String BLOB_SERIALIZATION = "BLOB_SERIALIZATION";

	/** Unrecognized serialized CAS format. */
  public static final String UNRECOGNIZED_SERIALIZED_CAS_FORMAT = "UNRECOGNIZED_SERIALIZED_CAS_FORMAT";
	
	/**
   * Error while deserializing binary CAS. {0}.
   */
	public static final String BLOB_DESERIALIZATION = "BLOB_DESERIALIZATION";

	/** Deserializing Compressed Form 6 with CasLoadMode LENIENT, but no Type System provided. */
	public static final String LENIENT_FORM_6_NO_TS = "LENIENT_FORM_6_NO_TS";
	
	/** Error trying to open a stream to Sofa data. */
	public static final String SOFADATASTREAM_ERROR = "SOFADATASTREAM_ERROR";

	/** Can''t call method "{0}" on the base CAS. */
	public static final String INVALID_BASE_CAS_METHOD = "INVALID_BASE_CAS_METHOD";
  

	/**
   * Error - the Annotation "{0}" is over view "{1}" and cannot be added to indexes associated with
   * the different view "{2}".
   */
	public static final String ANNOTATION_IN_WRONG_INDEX = "ANNOTATION_IN_WRONG_INDEX";

	/**
   * Error accessing index "{0}" for type "{1}". Index "{0}" is over type "{2}", which is not a
   * supertype of "{1}".
   */
	public static final String TYPE_NOT_IN_INDEX = "TYPE_NOT_IN_INDEX";
  
  /**
   * The type "{0}", a subtype of AnnotationBase, can''t be created in the Base CAS.
   *
   */
  public static final String DISALLOW_CREATE_ANNOTATION_IN_BASE_CAS = "DISALLOW_CREATE_ANNOTATION_IN_BASE_CAS";
  
  /**
   * SofaFS may not be cloned.
   *
   */
  public static final String CANNOT_CLONE_SOFA = "CANNOT_CLONE_SOFA";
  
  /** Mismatched CAS "{0}". */
  public static final String CAS_MISMATCH = "CAS_MISMATCH";
  
  /** Received pre-existing FS "{0}". */
  public static final String DELTA_CAS_PREEXISTING_FS_DISALLOWED = "DELTA_CAS_PREEXISTING_FS_DISALLOWED";

  /** Invalid Marker. */
  public static final String INVALID_MARKER = "INVALID_MARKER";

  /** Multiple Create Marker call for a CAS */
  public static final String MULTIPLE_CREATE_MARKER = "MULTIPLE_CREATE_MARKER";

  /** Deserializing Binary Header invalid */
  public static final String DESERIALIZING_BINARY_INVALID_HEADER = "DESERIALIZING_BINARY_INVALID_HEADER";
  
  /** Deserializing compressed binary other than form 4 not supported by this method */
  public static final String DESERIALIZING_COMPRESSED_BINARY_UNSUPPORTED = "DESERIALIZING_COMPRESSED_BINARY_UNSUPPORTED";

  /** Dereferencing a FeatureStructure of a CAS in a different CAS's context.
   *  This can happen if you try to set a feature structure reference to a value of a feature structure belonging to 
   *  an entirely different CAS.
   */
  public static final String DEREF_FS_OTHER_CAS = "DEREF_FS_OTHER_CAS";
  
  /** While FS was in the index, illegal attempt to modify Feature "{0}" which is used as a key in one or more indexes; FS = "{1}" */
  public static final String ILLEGAL_FEAT_SET = "ILLEGAL_FEAT_SET";

  /** Lenient deserialization not support for input of type {0}. */
  public static final String LENIENT_NOT_SUPPORTED = "LENIENT_NOT_SUPPORTED";
  
  /** Multiply nested classloaders not supported.  Original base loader: {0}, current nested loader: {1}, trying to switch to loader: {2}.*/
  public static final String SWITCH_CLASS_LOADER_NESTED = "SWITCH_CLASS_LOADER_NESTED";
  
	public CASRuntimeException() {
		super();
	}
	
	public CASRuntimeException(String messageKey) {
		super(messageKey, null);
	}

	public CASRuntimeException(String aMessageKey, Object[] aArguments, Throwable aCause) {
		super(aMessageKey, aArguments, aCause);
	}

	public CASRuntimeException(String aMessageKey, Object[] aArguments) {
		super(aMessageKey, aArguments);
	}

	public CASRuntimeException(String aResourceBundleName, String aMessageKey, Object[] aArguments,
			Throwable aCause) {
		super(aResourceBundleName, aMessageKey, aArguments, aCause);
	}

	public CASRuntimeException(String aResourceBundleName, String aMessageKey, Object[] aArguments) {
		super(aResourceBundleName, aMessageKey, aArguments);
	}

	public CASRuntimeException(Throwable aCause) {
		super(aCause);
	}

}
