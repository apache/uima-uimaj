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
package org.apache.uima.json;

import static com.fasterxml.jackson.core.JsonEncoding.UTF8;
import static java.util.Arrays.asList;
import static java.util.Arrays.sort;
import static java.util.Collections.sort;
import static java.util.Collections.unmodifiableSet;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static org.apache.uima.json.Json2CasSerializer.FeatureStructuresMode.AS_ARRAY;
import static org.apache.uima.json.Json2CasSerializer.ViewsMode.INLINE;
import static org.apache.uima.json.Json2CasSerializer.ViewsMode.SEPARATE;
import static org.apache.uima.json.Json2Names.COMPONENT_TYPE_FIELD;
import static org.apache.uima.json.Json2Names.FEATURE_STRUCTURES_FIELD;
import static org.apache.uima.json.Json2Names.FLAG_DOCUMENT_ANNOTATION;
import static org.apache.uima.json.Json2Names.ID_FIELD;
import static org.apache.uima.json.Json2Names.REF;
import static org.apache.uima.json.Json2Names.SUPER_TYPE_FIELD;
import static org.apache.uima.json.Json2Names.TYPES_FIELD;
import static org.apache.uima.json.Json2Names.TYPE_FIELD;
import static org.apache.uima.json.Json2Names.VIEWS_FIELD;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.StreamSupport;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.jcas.cas.TOP;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Json2CasSerializer {
  public enum FeatureStructuresMode {
    AS_OBJECT, AS_ARRAY
  }

  public enum ViewsMode {
    SEPARATE, INLINE
  }

  private final Set<String> BUILT_IN_TYPES = unmodifiableSet(new HashSet<>(asList(
          CAS.TYPE_NAME_ANNOTATION, CAS.TYPE_NAME_ANNOTATION_BASE, CAS.TYPE_NAME_ARRAY_BASE,
          CAS.TYPE_NAME_BOOLEAN, CAS.TYPE_NAME_BOOLEAN_ARRAY, CAS.TYPE_NAME_BYTE,
          CAS.TYPE_NAME_BYTE_ARRAY, CAS.TYPE_NAME_DOCUMENT_ANNOTATION, CAS.TYPE_NAME_DOUBLE,
          CAS.TYPE_NAME_DOUBLE_ARRAY, CAS.TYPE_NAME_EMPTY_FLOAT_LIST, CAS.TYPE_NAME_EMPTY_FS_LIST,
          CAS.TYPE_NAME_EMPTY_INTEGER_LIST, CAS.TYPE_NAME_EMPTY_STRING_LIST, CAS.TYPE_NAME_FLOAT,
          CAS.TYPE_NAME_FLOAT_ARRAY, CAS.TYPE_NAME_FLOAT_LIST, CAS.TYPE_NAME_FS_ARRAY,
          CAS.TYPE_NAME_FS_LIST, CAS.TYPE_NAME_INTEGER, CAS.TYPE_NAME_INTEGER_ARRAY,
          CAS.TYPE_NAME_INTEGER_LIST, CAS.TYPE_NAME_LIST_BASE, CAS.TYPE_NAME_LONG,
          CAS.TYPE_NAME_LONG_ARRAY, CAS.TYPE_NAME_NON_EMPTY_FLOAT_LIST,
          CAS.TYPE_NAME_NON_EMPTY_FS_LIST, CAS.TYPE_NAME_NON_EMPTY_INTEGER_LIST,
          CAS.TYPE_NAME_NON_EMPTY_STRING_LIST, CAS.TYPE_NAME_SHORT, CAS.TYPE_NAME_SHORT_ARRAY,
          CAS.TYPE_NAME_SOFA, CAS.TYPE_NAME_STRING, CAS.TYPE_NAME_STRING_ARRAY,
          CAS.TYPE_NAME_STRING_LIST, CAS.TYPE_NAME_TOP)));

  private JsonGenerator jg;

  private FeatureStructuresMode featureStructuresMode = AS_ARRAY;

  private ViewsMode viewsMode = INLINE;

  private Supplier<Function<FeatureStructure, String>> idRefGeneratorSupplier;
  private Function<FeatureStructure, String> idRefGenerator;
  private Map<FeatureStructure, String> idRefCache;

  private Supplier<Function<Type, String>> typeRefGeneratorSupplier;
  private Function<Type, String> typeRefGenerator;
  private Map<Type, String> typeRefCache;

  private Map<FeatureStructure, Set<String>> fsToViewsCache;

  public Json2CasSerializer(JsonGenerator aJg) {
    jg = aJg;
    setIdRefGeneratorSupplier(SequentialIdRefGenerator::new);
    setTypeRefGeneratorSupplier(FullyQualifiedTypeRefGenerator::new);
  }

  public void setViewsMode(ViewsMode aViewsMode) {
    viewsMode = aViewsMode;
  }

  public ViewsMode getViewsMode() {
    return viewsMode;
  }

  public FeatureStructuresMode getFeatureStructuresMode() {
    return featureStructuresMode;
  }

  public void setFeatureStructuresMode(FeatureStructuresMode aFeatureStructuresMode) {
    featureStructuresMode = aFeatureStructuresMode;
  }

  public void setIdRefGeneratorSupplier(
          Supplier<Function<FeatureStructure, String>> aIdRefGeneratorSupplier) {
    idRefGeneratorSupplier = aIdRefGeneratorSupplier;
  }

  public void setTypeRefGeneratorSupplier(
          Supplier<Function<Type, String>> aTypeRefGeneratorSupplier) {
    typeRefGeneratorSupplier = aTypeRefGeneratorSupplier;
  }

  public void write(CAS aCas) throws IOException {
    idRefGenerator = idRefGeneratorSupplier.get();
    typeRefGenerator = typeRefGeneratorSupplier.get();

    idRefCache = new HashMap<>();
    typeRefCache = new HashMap<>();
    fsToViewsCache = new IdentityHashMap<>();

    jg.writeStartObject(aCas);

    writeTypeSystem(aCas.getTypeSystem());

    List<CAS> views = new ArrayList<>();
    aCas.getViewIterator().forEachRemaining(views::add);
    if (!views.isEmpty()) {
      sort(views, comparing(CAS::getViewName));

      if (viewsMode == SEPARATE) {
        jg.writeObjectFieldStart(VIEWS_FIELD);
        for (CAS view : views) {
          jg.writeFieldName(view.getViewName());
          writeView(view);
        }
        jg.writeEndObject();
      }

      if (viewsMode == INLINE) {
        for (CAS view : views) {
          for (FeatureStructure fs : view.select()) {
            fsToViewsCache.computeIfAbsent(fs, _fs -> new HashSet<>()).add(view.getViewName());
          }
        }
      }
    }

    Set<FeatureStructure> allFSes = findAllFeatureStructures(aCas);

    if (!allFSes.isEmpty()) {
      switch (featureStructuresMode) {
        case AS_ARRAY:
          jg.writeArrayFieldStart(FEATURE_STRUCTURES_FIELD);
          for (FeatureStructure fs : allFSes) {
            jg.writeStartObject(fs);
            jg.writeStringField(ID_FIELD, fsRef(fs));
            writeFeatureStructure(fs);
            jg.writeEndObject();
          }
          jg.writeEndArray();
          break;
        case AS_OBJECT:
          jg.writeObjectFieldStart(FEATURE_STRUCTURES_FIELD);
          for (FeatureStructure fs : allFSes) {
            jg.writeFieldName(fsRef(fs));
            jg.writeStartObject(fs);
            writeFeatureStructure(fs);
            jg.writeEndObject();
          }
          jg.writeEndObject();
          break;
        default:
          throw new IOException("Unsupported feature structures serialization mode: ["
                  + featureStructuresMode + "]");
      }
    }

    jg.writeEndObject();
  }

  private Set<FeatureStructure> findAllFeatureStructures(CAS aCas) {
    Set<FeatureStructure> allFSes = new LinkedHashSet<>();
    ((CASImpl) aCas).walkReachablePlusFSsSorted(allFSes::add, null, null, null);
    return allFSes;
  }

  private String fsRef(FeatureStructure aFs) {
    return idRefCache.computeIfAbsent(aFs, idRefGenerator);
  }

  private String typeRef(Type aType) {
    return typeRefCache.computeIfAbsent(aType, typeRefGenerator);
  }

  private void writeFeatureStructure(FeatureStructure aFs) throws IOException {
    Type type = aFs.getType();
    jg.writeStringField(TYPE_FIELD, typeRef(type));

    if (viewsMode == INLINE) {
      Set<String> views = fsToViewsCache.get(aFs);

      if (views != null && !views.isEmpty()) {
        String[] viewsArray = views.toArray(new String[views.size()]);
        sort(viewsArray);
        jg.writeArrayFieldStart(VIEWS_FIELD);
        for (String view : viewsArray) {
          jg.writeString(view);
        }
        jg.writeEndArray();
      }
    }

    List<String> flags = new ArrayList<>();
    if (((CASImpl) aFs.getCAS()).getDocumentAnnotationNoCreate() == aFs) {
      flags.add(FLAG_DOCUMENT_ANNOTATION);
    }

    if (!flags.isEmpty()) {
      jg.writeArrayFieldStart(Json2Names.FLAGS_FIELD);
      for (String flag : flags) {
        jg.writeString(flag);
      }
      jg.writeEndArray();
    }

    for (Feature feature : type.getFeatures()) {
      writeFeature(aFs, feature);
    }
  }

  private void writeFeature(FeatureStructure aFs, Feature aFeature) throws IOException {
    if (!aFeature.getRange().isPrimitive()) {
      FeatureStructure target = aFs.getFeatureValue(aFeature);
      if (target != null) {
        jg.writeFieldName(REF + aFeature.getShortName());
        jg.writeString(fsRef(aFs.getFeatureValue(aFeature)));
      }
      return;
    }

    if (aFeature.getRange().isStringOrStringSubtype()) {
      String value = aFs.getStringValue(aFeature);
      if (value != null) {
        jg.writeFieldName(aFeature.getShortName());
        jg.writeString(value);
      }

      return;
    }

    jg.writeFieldName(aFeature.getShortName());
    String rangeTypeName = aFeature.getRange().getName();
    switch (rangeTypeName) {
      case CAS.TYPE_NAME_INTEGER:
        jg.writeNumber(aFs.getIntValue(aFeature));
        break;
      default:
        throw new IOException("Unsupported primitive type [" + rangeTypeName + "]");
    }
  }

  private void writeView(CAS aView) throws IOException {
    jg.writeStartArray();

    jg.writeString(fsRef(aView.getSofa()));
    for (TOP fs : aView.getIndexedFSs()) {
      jg.writeString(fsRef(fs));
    }

    jg.writeEndArray();
  }

  private void writeTypeSystem(TypeSystem aTypeSystem) throws IOException {
    List<Type> types = StreamSupport.stream(aTypeSystem.spliterator(), false)
            .sorted(comparing(Type::getName))
            .filter(type -> !BUILT_IN_TYPES.contains(type.getName())).collect(toList());

    if (types.isEmpty()) {
      return;
    }

    jg.writeFieldName(TYPES_FIELD);

    jg.writeStartObject(aTypeSystem);

    for (Type type : types) {
      writeType(aTypeSystem, type);
    }

    jg.writeEndObject();
  }

  private void writeType(TypeSystem aTypeSystem, Type aType) throws IOException {
    jg.writeFieldName(typeRef(aType));

    jg.writeStartObject(aType);

    if (!typeRef(aType).equals(aType.getName())) {
      jg.writeStringField(Json2Names.NAME_FIELD, aType.getName());
    }

    Type parent = aTypeSystem.getParent(aType);
    if (parent != null) {
      jg.writeStringField(SUPER_TYPE_FIELD, parent.getName());
    }

    if (aType.getComponentType() != null) {
      jg.writeStringField(COMPONENT_TYPE_FIELD, aType.getComponentType().getName());
    }

    List<Feature> newFeatures = aType.getFeatures().stream().filter(f -> f.getDomain() == aType)
            .collect(toList());
    if (!newFeatures.isEmpty()) {
      for (Feature feature : newFeatures) {
        jg.writeStringField(feature.getShortName(), feature.getRange().getName());
      }
    }

    jg.writeEndObject();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private Supplier<Function<FeatureStructure, String>> idRefGeneratorSupplier;
    private Supplier<Function<Type, String>> typeRefGeneratorSupplier;
    private FeatureStructuresMode featureStructuresMode = AS_ARRAY;
    private ViewsMode viewsMode = INLINE;

    public Builder() {
      setIdRefGeneratorSupplier(SequentialIdRefGenerator::new);
      setTypeRefGeneratorSupplier(FullyQualifiedTypeRefGenerator::new);
    }

    public Builder setFeatureStructuresMode(FeatureStructuresMode aFeatureStructuresMode) {
      featureStructuresMode = aFeatureStructuresMode;
      return this;
    }

    public Builder setViewsMode(ViewsMode aViewsMode) {
      viewsMode = aViewsMode;
      return this;
    }

    public Builder setIdRefGeneratorSupplier(
            Supplier<Function<FeatureStructure, String>> aIdRefGeneratorSupplier) {
      idRefGeneratorSupplier = aIdRefGeneratorSupplier;
      return this;
    }

    public Builder setTypeRefGeneratorSupplier(
            Supplier<Function<Type, String>> aTypeRefGeneratorSupplier) {
      typeRefGeneratorSupplier = aTypeRefGeneratorSupplier;
      return this;
    }

    public Json2CasSerializer build(JsonGenerator jg) {
      Json2CasSerializer ser = new Json2CasSerializer(jg);
      ser.setFeatureStructuresMode(featureStructuresMode);
      ser.setViewsMode(viewsMode);
      ser.setIdRefGeneratorSupplier(idRefGeneratorSupplier);
      ser.setTypeRefGeneratorSupplier(typeRefGeneratorSupplier);
      return ser;
    }

    public void write(CAS aCas, File aTargetFile) throws IOException {
      JsonFactory jsonFactory = new JsonFactory();
      jsonFactory.setCodec(new ObjectMapper());
      try (JsonGenerator jg = jsonFactory.createGenerator(aTargetFile, UTF8)
              .useDefaultPrettyPrinter()) {
        Json2CasSerializer ser = build(jg);
        ser.write(aCas);
      }
    }
  }

  public static void write(CAS aCas, File aTargetFile) throws IOException {
    JsonFactory jsonFactory = new JsonFactory();
    jsonFactory.setCodec(new ObjectMapper());
    try (JsonGenerator jg = jsonFactory.createGenerator(aTargetFile, UTF8)
            .useDefaultPrettyPrinter()) {
      Json2CasSerializer ser = new Json2CasSerializer(jg);
      ser.write(aCas);
    }
  }

  public static class SequentialIdRefGenerator implements Function<FeatureStructure, String> {
    private int nextId = 0;

    @Override
    public String apply(FeatureStructure aT) {
      return String.valueOf(nextId++);
    }
  }

  public static class FullyQualifiedTypeRefGenerator implements Function<Type, String> {
    @Override
    public String apply(Type aType) {
      return aType.getName();
    }
  }

  public static class ShortTypeRefGenerator implements Function<Type, String> {
    private Set<String> usedNames = new HashSet<>();

    @Override
    public String apply(Type aType) {
      if (!usedNames.contains(aType.getShortName())) {
        usedNames.add(aType.getShortName());
        return aType.getShortName();
      }

      int n = 1;
      String newName;
      while (usedNames.contains(newName = aType.getShortName() + "-" + n)) {
        n++;
      }

      usedNames.add(newName);
      return newName;
    }
  }
}
