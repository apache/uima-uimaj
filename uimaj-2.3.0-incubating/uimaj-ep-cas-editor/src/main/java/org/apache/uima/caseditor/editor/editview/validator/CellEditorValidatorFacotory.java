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

package org.apache.uima.caseditor.editor.editview.validator;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.viewers.ICellEditorValidator;

/**
 * TODO: add javadoc here
 */
public class CellEditorValidatorFacotory {
  private static Map<Class<?>, ICellEditorValidator> sValidatorMap = 
	  	new HashMap<Class<?>, ICellEditorValidator>();

  static {
    sValidatorMap.put(Byte.class, new ByteCellEditorValidator());
    sValidatorMap.put(Short.class, new ShortCellEditorValidator());
    sValidatorMap.put(Integer.class, new IntegerCellEditorValidator());
    sValidatorMap.put(Long.class, new LongCellEditorValidator());
    sValidatorMap.put(Float.class, new FloatCellEditorValidator());
  }

  private CellEditorValidatorFacotory() {
    // must not be instantiated
  }

  /**
   * Retrieves the appropriate {@link ICellEditorValidator} for the given class or none if not
   * available.
   *
   * @param type
   *
   * @return {@link ICellEditorValidator} or null
   */
  public static ICellEditorValidator createValidator(Class<?> type) {
    if (type == null) {
      throw new IllegalArgumentException("type must not be null!");
    }

    return sValidatorMap.get(type);
  }
}