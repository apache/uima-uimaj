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
package org.apache.uima.fit.internal.propertyeditors;

import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.Locale;

import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.beans.propertyeditors.CharsetEditor;
import org.springframework.beans.propertyeditors.CustomCollectionEditor;

/**
 * INTERNAL API
 */
public final class PropertyEditorUtil {

  private PropertyEditorUtil() {
    // Utility class
  }

  /**
   * Register the property editors provided by uimaFIT in the given property editor registry.
   * 
   * @param aRegistry
   *          a property editor registry
   */
  public static void registerUimaFITEditors(PropertyEditorRegistry aRegistry) {
    aRegistry.registerCustomEditor(Charset.class, new CharsetEditor());
    aRegistry.registerCustomEditor(Locale.class, new LocaleEditor());
    aRegistry.registerCustomEditor(String.class, new GetAsTextStringEditor(aRegistry));
    aRegistry.registerCustomEditor(LinkedList.class, new CustomCollectionEditor(LinkedList.class));
  }
}
