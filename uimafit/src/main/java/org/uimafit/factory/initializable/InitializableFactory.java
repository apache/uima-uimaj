/*
  Copyright 2010 Regents of the University of Colorado.
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

package org.uimafit.factory.initializable;

import org.apache.uima.UimaContext;
import org.apache.uima.resource.ResourceInitializationException;

/**
 * Please see {@link Initializable} for a description of how this class is intended to be used.
 *
 *
 * @author Philip Ogren
 * @author Philipp Wetzler
 * @author Steven Bethard
 * @see Initializable
 */
public final class InitializableFactory {
	private InitializableFactory() {
		// This class is not meant to be instantiated
	}

	/**
	 * Provides a way to create an instance of T. If the class specified by className implements
	 * Initializable, then the UimaContext provided here will be passed to its initialize method.
	 */
	public static <T> T create(UimaContext context, String className, Class<T> superClass)
			throws ResourceInitializationException {
		Class<? extends T> cls = getClass(className, superClass);
		return create(context, cls);
	}

	public static <T> Class<? extends T> getClass(String className, Class<T> superClass)
			throws ResourceInitializationException {
		try {
			Class<? extends T> cls = Class.forName(className).asSubclass(superClass);
			return cls;
		}
		catch (Exception e) {
			throw new ResourceInitializationException(new IllegalStateException("classname = "
					+ className + " superClass = " + superClass.getName(), e));
		}
	}

	public static <T> T create(UimaContext context, Class<? extends T> cls)
			throws ResourceInitializationException {
		T instance;
		try {
			instance = cls.newInstance();
		}
		catch (Exception e) {
			throw new ResourceInitializationException(e);
		}
		initialize(instance, context);
		return instance;
	}

	public static void initialize(Object object, UimaContext context)
			throws ResourceInitializationException {
		if (object instanceof Initializable) {
			((Initializable) object).initialize(context);
		}
	}

}
