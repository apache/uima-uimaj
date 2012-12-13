/*
 Copyright 2011
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

import org.apache.uima.fit.factory.FsIndexFactory;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.resource.metadata.FsIndexDescription;
import org.apache.uima.resource.metadata.FsIndexKeyDescription;

/**
 * @author Richard Eckart de Castilho
 * @see FsIndexDescription
 * @see FsIndexFactory
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface FsIndex {
	/**
	 * @see FsIndexDescription#KIND_SORTED
	 */
	public static final String KIND_SORTED = FsIndexDescription.KIND_SORTED;

	/**
	 * @see FsIndexDescription#KIND_SET
	 */
	public static final String KIND_SET = FsIndexDescription.KIND_SET;

	/**
	 * @see FsIndexDescription#KIND_BAG
	 */
	public static final String KIND_BAG = FsIndexDescription.KIND_BAG;

	/**
	 * @see FsIndexDescription#getLabel()
	 */
	String label();

	/**
	 * @see FsIndexDescription#getTypeName()
	 */
	String typeName() default NO_NAME_TYPE_SET;

	Class<? extends TOP> type() default NoClassSet.class;

	/**
	 * @see FsIndexDescription#getKind()
	 */
	String kind() default KIND_BAG;

	/**
	 * @see FsIndexDescription#getKeys()
	 */
	FsIndexKey[] keys() default {};

	/**
	 * @see FsIndexKeyDescription#isTypePriority()
	 */
	boolean typePriorities() default true;

	public static final class NoClassSet extends TOP { /* Nothing */ }

	public static final String NO_NAME_TYPE_SET = "org.uimafit.descriptor.FsIndex.NO_NAME_TYPE_SET";

}
