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

package org.apache.uima.caseditor.editor;

import org.apache.uima.cas.ArrayFS;
import org.apache.uima.cas.BooleanArrayFS;
import org.apache.uima.cas.ByteArrayFS;
import org.apache.uima.cas.DoubleArrayFS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.FloatArrayFS;
import org.apache.uima.cas.IntArrayFS;
import org.apache.uima.cas.LongArrayFS;
import org.apache.uima.cas.ShortArrayFS;
import org.apache.uima.cas.StringArrayFS;
import org.apache.uima.cas.text.AnnotationFS;
import org.eclipse.core.runtime.IAdaptable;

public class ArrayValue implements IAdaptable {

  private final FeatureStructure arrayFS;

  private final int slot;

  public ArrayValue(FeatureStructure arrayFS, int slot) {

    if (!arrayFS.getType().isArray()) {
      throw new IllegalArgumentException("The arrayFS parameter must contain an array type FS!");
    }

    this.arrayFS = arrayFS;
    this.slot = slot;
  }

  public FeatureStructure getFeatureStructure() {
    return arrayFS;
  }

  public int slot() {
    return slot;
  }

  public void set(String value) {

    if (arrayFS instanceof BooleanArrayFS) {
      BooleanArrayFS array = (BooleanArrayFS) arrayFS;
      array.set(slot, Boolean.parseBoolean(value));
    } else if (arrayFS instanceof ByteArrayFS) {
      ByteArrayFS array = (ByteArrayFS) arrayFS;
      array.set(slot, Byte.parseByte(value));
    } else if (arrayFS instanceof ShortArrayFS) {
      ShortArrayFS array = (ShortArrayFS) arrayFS;
      array.set(slot, Short.parseShort(value));
    } else if (arrayFS instanceof IntArrayFS) {
      IntArrayFS array = (IntArrayFS) arrayFS;
      array.set(slot, Integer.parseInt(value));
    } else if (arrayFS instanceof LongArrayFS) {
      LongArrayFS array = (LongArrayFS) arrayFS;
      array.set(slot, Long.parseLong(value));
    } else if (arrayFS instanceof FloatArrayFS) {
      FloatArrayFS array = (FloatArrayFS) arrayFS;
      array.set(slot, Float.parseFloat(value));
    } else if (arrayFS instanceof DoubleArrayFS) {
      DoubleArrayFS array = (DoubleArrayFS) arrayFS;
      array.set(slot, Double.parseDouble(value));
    } else if (arrayFS instanceof StringArrayFS) {
      StringArrayFS array = (StringArrayFS) arrayFS;
      array.set(slot, value);
    } else {
      throw new CasEditorError("Unkown array type!");
    }
  }

  public Object get() {

    if (arrayFS instanceof BooleanArrayFS) {
      BooleanArrayFS array = (BooleanArrayFS) arrayFS;
      return array.get(slot);
    } else if (arrayFS instanceof ByteArrayFS) {
      ByteArrayFS array = (ByteArrayFS) arrayFS;
      return array.get(slot);
    } else if (arrayFS instanceof ShortArrayFS) {
      ShortArrayFS array = (ShortArrayFS) arrayFS;
      return array.get(slot);
    } else if (arrayFS instanceof IntArrayFS) {
      IntArrayFS array = (IntArrayFS) arrayFS;
      return array.get(slot);
    } else if (arrayFS instanceof LongArrayFS) {
      LongArrayFS array = (LongArrayFS) arrayFS;
      return array.get(slot);
    } else if (arrayFS instanceof FloatArrayFS) {
      FloatArrayFS array = (FloatArrayFS) arrayFS;
      return array.get(slot);
    } else if (arrayFS instanceof DoubleArrayFS) {
      DoubleArrayFS array = (DoubleArrayFS) arrayFS;
      return array.get(slot);
    } else if (arrayFS instanceof StringArrayFS) {
      StringArrayFS array = (StringArrayFS) arrayFS;

      String value = array.get(slot);

      if (value == null) {
        value = "";
      }

      return value;
    } else if (arrayFS instanceof ArrayFS) {
      ArrayFS array = (ArrayFS) arrayFS;
      return array.get(slot);
    } else {
      throw new CasEditorError("Unkown array type!");
    }
  }

  public Object getAdapter(Class adapter) {

    if (FeatureStructure.class.equals(adapter)) {
      if (arrayFS instanceof ArrayFS) {
        ArrayFS array = (ArrayFS) arrayFS;

        return array.get(slot());
      }
    }

    if (AnnotationFS.class.equals(adapter)) {
      FeatureStructure fs = (FeatureStructure) getAdapter(FeatureStructure.class);

      if (fs instanceof AnnotationFS) {
        return fs;
      }
    }

    return null;
  }

  @Override
  public String toString() {
    return Integer.toString(slot()) ;
  }
}