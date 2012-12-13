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
package org.apache.uima.fit.propertyeditors;

import java.beans.PropertyEditorSupport;
import java.util.Locale;

import org.apache.uima.fit.util.LocaleUtil;

/**
 * Custom property editor for {@link Locale} that supports "-" as separator and sets the default
 * locale when {@code null} or {@code ""} is passed. This is used to be backwards-compatible with 
 * previous uimaFIT behavior.
 * 
 * @author Richard Eckart de Castilho
 */
public class LocaleEditor extends PropertyEditorSupport {

	@Override
	public void setAsText(String text) {
		if (text == null) {
			setValue(Locale.getDefault());
		}
		else if (text.length() == 0) {
			setValue(Locale.getDefault());
		}
		else {
			setValue(LocaleUtil.getLocale(text));
		}
	}

	@Override
	public String getAsText() {
		Object value = getValue();
		return (value != null ? value.toString() : "");
	}
}
