/*
  Copyright 2009-2010	Regents of the University of Colorado.
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

package org.uimafit.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Philip Ogren
 * @author Richard Eckart de Castilho
 */
public final class ReflectionUtil {
	
	private ReflectionUtil() {
		// Library class
	}

	/**
	 * Get all the fields for the class (and superclasses) of the passed in object
	 * 
	 * @param aObject
	 *            any object will do
	 * @return the fields for the class of the object
	 */
	public static List<Field> getFields(final Object aObject) {
		return getFields(aObject.getClass());
	}

	/**
	 * Get all the fields for this class and all of its superclasses
	 * 
	 * @param aClass
	 *            any class will do
	 * @return the fields for the class and all of its superclasses
	 */
	public static List<Field> getFields(final Class<?> aClass) {
		Class<?> cls = aClass;
		final List<Field> fields = new ArrayList<Field>();
		while (!cls.equals(Object.class)) {
			final Field[] flds = cls.getDeclaredFields();
			fields.addAll(Arrays.asList(flds));
			cls = cls.getSuperclass();
		}
		return fields;
	}
	
	/**
	 * Get the given field of the passed in object from its class or the first superclass that
	 * declares it.
	 * 
	 * @param aObject
	 *            any object will do
	 * @return the fields for the class of the object
	 */
	public static Field getField(final Object aObject, final String aName)
			throws NoSuchFieldException {
		return getField(aObject.getClass(), aName);
	}

	/**
	 * Get the given field from the class or the first superclass that declares it.
	 * 
	 * @param aClass
	 *            any class will do
	 * @return the fields for the class of the object
	 */
	public static Field getField(final Class<?> aClass, final String aName)
			throws NoSuchFieldException {
		try {
			return aClass.getDeclaredField(aName);
		}
		catch (NoSuchFieldException e) {
			if (aClass.getSuperclass() == null) {
				throw e;
			}
			
			return getField(aClass.getSuperclass(), aName);
		}
	}

	/**
	 * Search for an annotation of the specified type starting on the given class and tracking back
	 * the inheritance hierarchy. Only parent classes are tracked back, no implemented interfaces.
	 * 
	 * @param <T>
	 *            the annotation type
	 * @param aAnnotation
	 *            the annotation class
	 * @param aClass
	 *            the class to start searching on
	 * @return the annotation or {@code null} if it could not be found
	 */
	public static <T extends Annotation> T getInheritableAnnotation(final Class<T> aAnnotation,
			final Class<?> aClass) {
		if (aClass.isAnnotationPresent(aAnnotation)) {
			return aClass.getAnnotation(aAnnotation);
		}

		if (aClass.getSuperclass() != null) {
			return getInheritableAnnotation(aAnnotation, aClass.getSuperclass());
		}

		return null;
	}
}
