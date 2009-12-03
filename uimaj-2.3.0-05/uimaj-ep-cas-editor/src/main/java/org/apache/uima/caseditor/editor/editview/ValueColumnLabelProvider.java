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

package org.apache.uima.caseditor.editor.editview;

import org.apache.uima.cas.ArrayFS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.caseditor.editor.ArrayValue;
import org.apache.uima.caseditor.editor.CasEditorError;
import org.apache.uima.caseditor.editor.FeatureValue;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ViewerCell;

/**
 * Provides the labels for the edit view.
 */
final class ValueColumnLabelProvider extends CellLabelProvider {
  
  @Override
  public void update(ViewerCell cell) {

    Object element = cell.getElement();

    if (element instanceof FeatureValue) {
      FeatureValue featureValue = (FeatureValue) element;

      if (featureValue.getFeature().getRange().isPrimitive()) {
        cell.setText(featureValue.getFeatureStructure().getFeatureValueAsString(
                featureValue.getFeature()));
      }
      else {
        FeatureStructure value = (FeatureStructure) featureValue.getValue();

        if (value == null) {
          cell.setText("null");
        } else {
          cell.setText("[" + value.getType().getShortName() + "]");
        }
      }
    }
    else if (element instanceof ArrayValue) {

      ArrayValue value = (ArrayValue) element;

      if (value.getFeatureStructure() instanceof ArrayFS) {
        ArrayFS array = (ArrayFS) value.getFeatureStructure();

        FeatureStructure fs = array.get(value.slot());

        if (fs == null) {
          cell.setText("null");
        }
        else {
          cell.setText("[" + fs.getType().getShortName() + "]");
        }
      }
      else {
        cell.setText(value.get().toString());
      }
    }
    else {
      throw new CasEditorError("Unkown element!");
    }
  }
}
