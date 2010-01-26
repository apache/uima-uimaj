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

package org.apache.uima.tools.cvd.tsview;

import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;

/**
 * Insert comment for enclosing_type here.
 * 
 * 
 */
public class FeatureTableModel extends AbstractTableModel {

  private static final long serialVersionUID = -6010925680514336742L;

  static final String[] columnHeaders = { "Feature", "Value Type", "Defined On" };

  private Type type = null;

  public FeatureTableModel() {
    super();
  }

  /**
   * Constructor for FeatureTableModel.
   */
  public FeatureTableModel(Type type) {
    super();
    this.type = type;
  }

  public void setType(Type type) {
    this.type = type;
    fireTableDataChanged();
  }

  public String getColumnName(int i) {
    if (i < 0 || i >= columnHeaders.length) {
      return "";
    }
    return columnHeaders[i];
  }

  /**
   * @see javax.swing.table.TableModel#getRowCount()
   */
  public int getRowCount() {
    if (this.type == null) {
      return 0;
    }
    return this.type.getNumberOfFeatures();
  }

  /**
   * @see javax.swing.table.TableModel#getColumnCount()
   */
  public int getColumnCount() {
    return 3;
  }

  /**
   * @see javax.swing.table.TableModel#getValueAt(int, int)
   */
  public Object getValueAt(int rowIndex, int columnIndex) {
    if (this.type == null) {
      return null;
    }
    List<?> feats = this.type.getFeatures();
    if (rowIndex < 0 || rowIndex >= feats.size()) {
      return null;
    }
    Feature feat = (Feature) feats.get(rowIndex);
    switch (columnIndex) {
      case 0: {
        return feat.getShortName();
      }
      case 1: {
        return feat.getRange().getName();
      }
      case 2: {
        return feat.getDomain().getName();
      }
      default: {
        return null;
      }
    }
  }

}
