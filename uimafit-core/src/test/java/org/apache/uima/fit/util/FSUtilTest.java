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
package org.apache.uima.fit.util;

import static java.util.Arrays.asList;
import static org.apache.uima.fit.util.FSUtil.getFeature;
import static org.apache.uima.fit.util.FSUtil.setFeature;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.List;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;
import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.AnnotationImpl;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;
import org.junit.Test;

public class FSUtilTest {
  @Test
  public void setGetFeatureTestCAS() throws Exception {
    setGetFeatureTest(false);
  }

  @Test
  public void setGetFeatureTestJCas() throws Exception {
    setGetFeatureTest(true);
  }

  public void setGetFeatureTest(boolean aActivateJCas) throws Exception {
    TypeSystemDescription tsd = UIMAFramework.getResourceSpecifierFactory()
            .createTypeSystemDescription();

    TypeDescription type = tsd.addType("MyType", "", CAS.TYPE_NAME_TOP);

    String[] PRIMITIVE_TYPES = { CAS.TYPE_NAME_BOOLEAN, CAS.TYPE_NAME_BYTE, CAS.TYPE_NAME_DOUBLE,
        CAS.TYPE_NAME_FLOAT, CAS.TYPE_NAME_INTEGER, CAS.TYPE_NAME_LONG, CAS.TYPE_NAME_SHORT,
        CAS.TYPE_NAME_STRING };
    
    String[] LIST_TYPES = { CAS.TYPE_NAME_FLOAT_LIST, CAS.TYPE_NAME_FS_LIST,
        CAS.TYPE_NAME_INTEGER_LIST, CAS.TYPE_NAME_STRING_LIST };

    // Add primitive fields and primitive array fields
    for (String t : PRIMITIVE_TYPES) {
      String baseName = StringUtils.substringAfterLast(t, ".");
      type.addFeature(baseName + "Value", "", t);
      type.addFeature(baseName + "ArrayValue", "", t + "Array");
    }

    // Add list fields
    for (String t : LIST_TYPES) {
      String baseName = StringUtils.substringAfterLast(t, ".");
      type.addFeature(baseName + "Value", "", t);
    }
    
    // Add feature types
    type.addFeature("TopValue", "", CAS.TYPE_NAME_TOP);
    type.addFeature("AnnotationValue", "", CAS.TYPE_NAME_ANNOTATION);
    type.addFeature("TopArrayValue", "", CAS.TYPE_NAME_FS_ARRAY, CAS.TYPE_NAME_TOP, false);
    type.addFeature("AnnotationArrayValue", "", CAS.TYPE_NAME_FS_ARRAY, CAS.TYPE_NAME_ANNOTATION, false);
    type.addFeature("TopListValue", "", CAS.TYPE_NAME_FS_LIST, CAS.TYPE_NAME_TOP, false);
    type.addFeature("AnnotationListValue", "", CAS.TYPE_NAME_FS_LIST, CAS.TYPE_NAME_ANNOTATION, false);
    
    // Avoid using JCasFactory here because we want to initialize only CAS *not* JCas.
    CAS cas = CasCreationUtils.createCas(tsd, null, null);
    
    if (aActivateJCas) {
      cas.getJCas();
    }
    
    Type annotationType = cas.getTypeSystem().getType(CAS.TYPE_NAME_ANNOTATION);

    FeatureStructure fs = cas.createFS(cas.getTypeSystem().getType("MyType"));
    setFeature(fs, "BooleanValue", true);
    assertEquals(true, getFeature(fs, "BooleanValue", Boolean.class));

    setFeature(fs, "ByteValue", (byte) 1);
    assertTrue(((byte) 1) == getFeature(fs, "ByteValue", Byte.class));

    setFeature(fs, "DoubleValue", 1d);
    assertTrue(1d == getFeature(fs, "DoubleValue", Double.class));
    
    setFeature(fs, "FloatValue", 1f);
    assertTrue(1f == getFeature(fs, "FloatValue", Float.class));

    setFeature(fs, "IntegerValue", 1);
    assertTrue(1 == getFeature(fs, "IntegerValue", Integer.class));
    
    setFeature(fs, "LongValue", 1l);
    assertTrue(1l == getFeature(fs, "LongValue", Long.class));
    
    setFeature(fs, "ShortValue", (short) 1);
    assertTrue(((short) 1) == getFeature(fs, "ShortValue", Short.class));
    
    setFeature(fs, "StringValue", "set");
    assertEquals("set", getFeature(fs, "StringValue", String.class));
    
    setFeature(fs, "TopValue", cas.createArrayFS(1));

    setFeature(fs, "AnnotationValue", cas.createAnnotation(annotationType, 0, 1));
    if (aActivateJCas) {
      assertEquals(Annotation.class.getName(),
              getFeature(fs, "AnnotationValue", FeatureStructure.class).getClass().getName());
    }
    else {
      assertEquals(Annotation.class.getName(),
              getFeature(fs, "AnnotationValue", FeatureStructure.class).getClass().getName());
    }
    assertEquals(0, getFeature(fs, "AnnotationValue", AnnotationFS.class).getBegin());
    assertEquals(1, getFeature(fs, "AnnotationValue", AnnotationFS.class).getEnd());

    setFeature(fs, "BooleanArrayValue", (boolean[]) null);
    assertEquals(null, getFeature(fs, "BooleanArrayValue", boolean[].class));

    setFeature(fs, "BooleanArrayValue", (Collection) null);
    assertEquals(null, getFeature(fs, "BooleanArrayValue", boolean[].class));

    setFeature(fs, "BooleanArrayValue", new boolean[0]);
    assertEquals(0, getFeature(fs, "BooleanArrayValue", boolean[].class).length);

    setFeature(fs, "BooleanArrayValue", true);
    assertEquals(true, getFeature(fs, "BooleanArrayValue", boolean[].class)[0]);

    setFeature(fs, "BooleanArrayValue", true, false);
    assertEquals(true, getFeature(fs, "BooleanArrayValue", boolean[].class)[0]);
    assertEquals(false, getFeature(fs, "BooleanArrayValue", boolean[].class)[1]);
    
    setFeature(fs, "BooleanArrayValue", new boolean[] { true, false });
    assertEquals(true, getFeature(fs, "BooleanArrayValue", boolean[].class)[0]);
    assertEquals(false, getFeature(fs, "BooleanArrayValue", boolean[].class)[1]);
    
    setFeature(fs, "ByteArrayValue", new byte[] { 0, 1 });
    setFeature(fs, "DoubleArrayValue", new double[] { 0d, 1d });
    setFeature(fs, "FloatArrayValue", new float[] { 0f, 1f });
    setFeature(fs, "IntegerArrayValue", new int[] { 0, 1 });
    setFeature(fs, "LongArrayValue", new long[] { 0l, 1l });
    setFeature(fs, "ShortArrayValue", new short[] { 0, 1 });
    setFeature(fs, "StringArrayValue", new String[] { "one", "two" });
    setFeature(fs, "TopArrayValue", cas.createArrayFS(1), cas.createDoubleArrayFS(1));

    setFeature(fs, "AnnotationArrayValue", cas.createAnnotation(annotationType, 0, 1), 
            cas.createAnnotation(annotationType, 1, 2));
    assertEquals(0, getFeature(fs, "AnnotationArrayValue", AnnotationFS[].class)[0].getBegin());
    assertEquals(1, getFeature(fs, "AnnotationArrayValue", AnnotationFS[].class)[1].getBegin());

    setFeature(fs, "BooleanArrayValue", asList(true, false));
    assertEquals(true, getFeature(fs, "BooleanArrayValue", List.class).get(0));
    assertEquals(false, getFeature(fs, "BooleanArrayValue", List.class).get(1));
    
    setFeature(fs, "ByteArrayValue", asList((byte) 0, (byte) 1));
    setFeature(fs, "DoubleArrayValue", asList(0d, 1d));
    setFeature(fs, "FloatArrayValue", asList(0f, 1f));
    setFeature(fs, "IntegerArrayValue", asList(0, 1));
    setFeature(fs, "LongArrayValue", asList(0l, 1l));
    setFeature(fs, "ShortArrayValue", asList((short) 0, (short) 1));
    setFeature(fs, "StringArrayValue", asList("one", "two"));
    setFeature(fs, "TopArrayValue", asList(cas.createArrayFS(1), cas.createDoubleArrayFS(1)));
    setFeature(fs, "AnnotationArrayValue", asList(cas.createAnnotation(annotationType, 0, 1), 
            cas.createAnnotation(annotationType, 1, 2)));
    assertEquals(0, ((List<AnnotationFS>) getFeature(fs, "AnnotationArrayValue", List.class))
            .get(0).getBegin());
    assertEquals(1, ((List<AnnotationFS>) getFeature(fs, "AnnotationArrayValue", List.class))
            .get(1).getBegin());
    assertEquals(0, ((List<AnnotationFS>) getFeature(fs, "AnnotationArrayValue", Vector.class))
            .get(0).getBegin());
    assertEquals(1, ((List<AnnotationFS>) getFeature(fs, "AnnotationArrayValue", Vector.class))
            .get(1).getBegin());

    setFeature(fs, "FloatListValue", new float[] { 0f, 1f });
    setFeature(fs, "IntegerListValue", new int[] { 0, 1 });
    setFeature(fs, "StringListValue", new String[] { "one", "two" });
    setFeature(fs, "TopListValue",
            new FeatureStructure[] { cas.createArrayFS(1), cas.createDoubleArrayFS(1) });
    setFeature(fs, "AnnotationListValue", new AnnotationFS[] { 
            cas.createAnnotation(annotationType, 0, 1), 
            cas.createAnnotation(annotationType, 1, 2) } );
    assertEquals(0, getFeature(fs, "AnnotationListValue", AnnotationFS[].class)[0].getBegin());
    assertEquals(1, getFeature(fs, "AnnotationListValue", AnnotationFS[].class)[1].getBegin());
    if (aActivateJCas) {
      assertEquals(Annotation.class.getName(),
              getFeature(fs, "AnnotationListValue", AnnotationFS[].class)[0].getClass().getName());
    } else {
      assertEquals(Annotation.class.getName(),
              getFeature(fs, "AnnotationListValue", AnnotationFS[].class)[0].getClass().getName());
    }
      
    setFeature(fs, "FloatListValue", asList(0f, 1f));
    setFeature(fs, "IntegerListValue", asList(0, 1));
    setFeature(fs, "StringListValue", asList("one", "two"));
    setFeature(fs, "TopListValue", asList(cas.createArrayFS(1), cas.createDoubleArrayFS(1)));
    setFeature(fs, "AnnotationListValue", asList(cas.createAnnotation(annotationType, 0, 1), 
            cas.createAnnotation(annotationType, 1, 2)));
    assertEquals(0, ((List<AnnotationFS>) getFeature(fs, "AnnotationListValue", List.class))
            .get(0).getBegin());
    assertEquals(1, ((List<AnnotationFS>) getFeature(fs, "AnnotationListValue", List.class))
            .get(1).getBegin());
  }  
}
