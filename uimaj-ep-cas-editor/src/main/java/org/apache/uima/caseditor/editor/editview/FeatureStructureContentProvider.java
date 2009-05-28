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

import java.util.Collection;
import java.util.LinkedList;

import org.apache.uima.cas.ArrayFS;
import org.apache.uima.cas.CommonArrayFS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.StringArrayFS;
import org.apache.uima.cas.Type;
import org.apache.uima.caseditor.editor.AbstractDocumentListener;
import org.apache.uima.caseditor.editor.AnnotationDocument;
import org.apache.uima.caseditor.editor.ArrayValue;
import org.apache.uima.caseditor.editor.CasEditorError;
import org.apache.uima.caseditor.editor.FeatureValue;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Display;

final class FeatureStructureContentProvider extends AbstractDocumentListener
        implements ITreeContentProvider {

  private AnnotationDocument mDocument;

  private Viewer viewer;

  FeatureStructureContentProvider(AnnotationDocument document) {

    if (document == null) {
      throw new IllegalArgumentException("document parameter must not be null!");
    }

    mDocument = document;
  }

  private int arraySize(FeatureStructure value) {

    if (!value.getType().isArray()) {
      throw new IllegalArgumentException();
    }

    int size;

    if (value instanceof ArrayFS) {
      ArrayFS array = (ArrayFS) value;

      size = array.size();
    } else if (value instanceof CommonArrayFS) {
      CommonArrayFS array = (CommonArrayFS) value;

      size = array.size();
    } else if (value instanceof StringArrayFS) {
      StringArrayFS array = (StringArrayFS) value;

      size = array.size();
    } else {
      throw new CasEditorError("Unkown array type!");
    }

    return size;
  }

  public Object[] getElements(Object inputElement) {

    if (inputElement != null) {

      FeatureStructure featureStructure = (FeatureStructure) inputElement;

      Type type = featureStructure.getType();

      if (!type.isArray()) {

        Collection<FeatureValue> featureValues = new LinkedList<FeatureValue>();

        for (Feature feature : type.getFeatures()) {
          featureValues.add(new FeatureValue(mDocument, featureStructure, feature));
        }

        return featureValues.toArray();
      }
      else {
        int size = arraySize(featureStructure);

        ArrayValue arrayValues[] = new ArrayValue[size];

        for (int i = 0; i < size; i++) {
          arrayValues[i] = new ArrayValue(featureStructure, i);
        }

        return arrayValues;
      }

    } else {
      return new Object[0];
    }
  }

  public void dispose() {
    mDocument.removeChangeListener(this);
  }

  public void inputChanged(final Viewer viewer, Object oldInput, Object newInput) {
    this.viewer = viewer;

    if (newInput != null) {
      Display.getDefault().syncExec(new Runnable() {
        public void run() {
          viewer.refresh();
        }
      });
    }
  }

  public void added(Collection<FeatureStructure> newFeatureStructure) {
  }

  public void changed() {

    Display.getDefault().syncExec(new Runnable() {
      public void run() {
        viewer.setInput(null);
      }
    });
  }

  public void removed(Collection<FeatureStructure> deletedFeatureStructure) {
    for(FeatureStructure fs : deletedFeatureStructure) {
      if (viewer.getInput() == fs) {
        viewer.setInput(null);
        break;
      }
    }

  }

  public void updated(Collection<FeatureStructure> featureStructure) {
    Display.getDefault().syncExec(new Runnable() {
      public void run() {
        viewer.setSelection(viewer.getSelection());
        viewer.refresh();
      }
    });
  }

  public Object[] getChildren(Object parentElement) {

    if (parentElement instanceof FeatureValue) {
      FeatureValue value = (FeatureValue) parentElement;

      if (!value.getFeature().getRange().isArray()) {
        FeatureStructure childStructure = (FeatureStructure) value.getValue();
        return getElements(childStructure);
      } else {
        FeatureStructure arrayFS = value.getFeatureStructure().getFeatureValue(value.getFeature());

        return getElements(arrayFS);
      }
    } else if (parentElement instanceof ArrayValue) {
      ArrayValue value = (ArrayValue) parentElement;

      ArrayFS array = (ArrayFS) value.getFeatureStructure();

      return getElements(array.get(value.slot()));
    }
    else {
      throw new CasEditorError("Unexpected element type!");
    }
  }

  public Object getParent(Object element) {
    return null;
  }

  private boolean hasChildren(FeatureStructure value) {

    boolean result;


    if (value != null) {

      if (value.getType().isArray()) {
        // if array it has children if size != 0
        result = arraySize(value) > 0;
      } else {
        result = value.getType().getFeatures().size() > 0;
      }
    } else {
      result = false;
    }

    return result;
  }

  public boolean hasChildren(Object element) {

    if (element instanceof FeatureValue) {
      FeatureValue value = (FeatureValue) element;

      if (value.getFeature().getRange().isPrimitive()) {
        return false;
      }
      else {
        return hasChildren((FeatureStructure) value.getValue());
      }
    } else if (element instanceof ArrayValue) {

      ArrayValue value = (ArrayValue) element;

      if (value.getFeatureStructure() instanceof ArrayFS) {

        ArrayFS array = (ArrayFS) value.getFeatureStructure();

        return hasChildren(array.get(value.slot()));
      }
      else {
        // false for primitive arrays
        return false;
      }
    }
    else {
      throw new CasEditorError("Unkown element type");
    }
  }
}