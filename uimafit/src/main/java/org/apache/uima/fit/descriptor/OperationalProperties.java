/*
 Copyright 2009
 Ubiquitous Knowledge Processing (UKP) Lab
 Technische Universitaet Darmstadt
 All rights reserved.

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.

 JavaDoc for multipleDeploymentAllowed(), modifiesCas() and outputsNewCases() is duplicated from
 org.apache.uima.resource.metadata.OperationalProperties for sake of the users' convenience. The
 copied JavaDoc is licensed as under the Apache 2.0 license as well. No particular author
 attribution was given in the original source file.
 */

package org.apache.uima.fit.descriptor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apache.uima.analysis_engine.AnalysisEngine;

/**
 * Control aspects of the UIMA analysis component.<br/>
 * 
 * @author Richard Eckart de Castilho
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface OperationalProperties {
	/**
	 * the default value for multipleDeploymentAllowed if none is given.
	 */
	public static final boolean MULTIPLE_DEPLOYMENT_ALLOWED_DEFAULT = true;
	/**
	 * the default value for modifiesCas if none is given
	 */
	public static final boolean MODIFIES_CAS_DEFAULT = true;
	/**
	 * the default value for outputsNewCases if none is given
	 */
	public static final boolean OUTPUTS_NEW_CASES_DEFAULT = false;

	/**
	 * Gets whether multiple instances of this component can be run in parallel, each receiving a
	 * subset of the documents from a collection.
	 * 
	 * @return true if multiple instances can be run in parallel, false if not
	 * @see org.apache.uima.resource.metadata.OperationalProperties#isMultipleDeploymentAllowed()
	 */
	boolean multipleDeploymentAllowed() default MULTIPLE_DEPLOYMENT_ALLOWED_DEFAULT;

	/**
	 * Gets whether this component will modify the CAS.
	 * 
	 * @return true if this component modifies the CAS, false if it does not.
	 * @see org.apache.uima.resource.metadata.OperationalProperties#getModifiesCas()
	 */
	boolean modifiesCas() default MODIFIES_CAS_DEFAULT;

	/**
	 * Gets whether this AnalysisEngine may output new CASes. If this property is set to true, an
	 * application can use the
	 * {@link AnalysisEngine#processAndOutputNewCASes(org.apache.uima.cas.CAS)} to pass a CAS to
	 * this this AnalysisEngine and then step through all of the output CASes that it produces. For
	 * example, such an AnalysisEngine could segment a CAS into smaller pieces, emitting each as a
	 * separate CAS.
	 * 
	 * @return true if this component may output new CASes, false if it does not
	 * @see org.apache.uima.resource.metadata.OperationalProperties#getOutputsNewCASes()
	 */
	boolean outputsNewCases() default OUTPUTS_NEW_CASES_DEFAULT;
}
