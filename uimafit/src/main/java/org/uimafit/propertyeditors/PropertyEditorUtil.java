/*
 Copyright 2009-2012
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
package org.uimafit.propertyeditors;

import java.util.LinkedList;
import java.util.Locale;

import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.beans.propertyeditors.CustomCollectionEditor;

/**
 * @author Richard Eckart de Castilho
 */
public final class PropertyEditorUtil {

	private PropertyEditorUtil() {
		// Utility class
	}
	
	public static void registerUimaFITEditors(PropertyEditorRegistry aRegistry)
	{
		aRegistry.registerCustomEditor(Locale.class, new LocaleEditor());
		aRegistry.registerCustomEditor(String.class, new GetAsTextStringEditor(aRegistry));
		aRegistry.registerCustomEditor(LinkedList.class, new CustomCollectionEditor(LinkedList.class));
	}
}
