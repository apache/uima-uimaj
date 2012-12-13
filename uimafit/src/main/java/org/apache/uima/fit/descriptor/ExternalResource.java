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
 */

package org.apache.uima.fit.descriptor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apache.uima.resource.Resource;

/**
 * Mark a field as external resource.
 * 
 * @author Richard Eckart de Castilho
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ExternalResource {
	/**
	 * The key to which external resources bind to. If no key is set, the class name of the
	 * annotated field will be used.
	 * 
	 * @return the key;
	 */
	String key() default "";

	/**
	 * The interface that external resources need to implement. Normally this has to be the type of
	 * the field, but if {@link ExternalResourceLocator}s are used, this should be set to
	 * {@link ExternalResourceLocator} or to a derived interface.
	 * 
	 * @return the required interface.
	 */
	Class<? extends Resource> api() default Resource.class;

	/**
	 * Determines if this external resource is mandatory.
	 */
	boolean mandatory() default true;
}
