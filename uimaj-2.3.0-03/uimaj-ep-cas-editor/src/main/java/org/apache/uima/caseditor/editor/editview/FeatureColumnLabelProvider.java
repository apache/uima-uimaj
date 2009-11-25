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

import org.apache.uima.caseditor.editor.ArrayValue;
import org.apache.uima.caseditor.editor.CasEditorError;
import org.apache.uima.caseditor.editor.FeatureValue;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ViewerCell;

final class FeatureColumnLabelProvider extends CellLabelProvider {
  @Override
  public void update(ViewerCell cell) {

    Object element = cell.getElement();

    if (element instanceof FeatureValue) {
      FeatureValue featureValue = (FeatureValue) cell.getElement();

      cell.setText(featureValue.getFeature().getShortName());

    }
    else if (element instanceof ArrayValue) {
      ArrayValue arrayValue = (ArrayValue) cell.getElement();

      cell.setText(Integer.toString(arrayValue.slot()));
    }
    else {
      throw new CasEditorError("Unkown element!");
    }
  }
}