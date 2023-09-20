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
package org.apache.uima.cas.serdes;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableSet;
import static java.util.Comparator.comparing;
import static java.util.Comparator.comparingInt;
import static java.util.Comparator.reverseOrder;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.csv.CSVFormat.DEFAULT;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.cas.ArrayFS;
import org.apache.uima.cas.BooleanArrayFS;
import org.apache.uima.cas.ByteArrayFS;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CommonArrayFS;
import org.apache.uima.cas.DoubleArrayFS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.FloatArrayFS;
import org.apache.uima.cas.IntArrayFS;
import org.apache.uima.cas.LongArrayFS;
import org.apache.uima.cas.ShortArrayFS;
import org.apache.uima.cas.StringArrayFS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.AnnotationBase;

/**
 * @author entwicklerteam
 */
public class CasToComparableText {

  // Parameters
  private boolean markIndexed = true;
  private boolean markView = true;
  private boolean coveredTextColumnEnabled = true;
  private boolean indexedColumnEnabled = false;
  private boolean treatEmptyStringsAsNull = false;
  private int maxLengthCoveredText = 30;
  private boolean sortAnnotationsInMultiValuedFeatures = true;
  private boolean uniqueAnchors = true;
  private Set<String> excludeFeaturePatterns = new HashSet<>();
  private Set<String> excludeTypePatterns = new HashSet<>();
  private String nullValue = "<NULL>";

  // State
  private final CAS cas;
  private Set<FeatureStructure> _indexedFses;
  private Map<String, Pattern> regexCache = new HashMap<>();
  private Map<String, Boolean> exclusionCache;

  public CasToComparableText(CAS aCas) {

    cas = aCas;
  }

  public CasToComparableText(JCas jCas) {

    this(jCas.getCas());
  }

  public void addExcludeTypePatterns(String... aPatterns) {

    resetExclusionCache();
    excludeTypePatterns.addAll(asList(aPatterns));
  }

  public Set<String> getExcludeTypePatterns() {

    return unmodifiableSet(excludeTypePatterns);
  }

  public void setExcludeTypePatterns(Collection<String> aExcludeFeaturePatterns) {

    resetExclusionCache();
    excludeTypePatterns.clear();

    if (aExcludeFeaturePatterns != null) {
      excludeTypePatterns.addAll(aExcludeFeaturePatterns);
    }
  }

  public void addExcludeFeaturePatterns(String... aPatterns) {

    excludeFeaturePatterns.addAll(asList(aPatterns));
  }

  public Set<String> getExcludeFeaturePatterns() {

    return unmodifiableSet(excludeFeaturePatterns);
  }

  public void setExcludeFeaturePatterns(Collection<String> aExcludeFeaturePatterns) {

    resetExclusionCache();
    excludeFeaturePatterns.clear();

    if (aExcludeFeaturePatterns != null) {
      excludeFeaturePatterns.addAll(aExcludeFeaturePatterns);
    }
  }

  public void setUniqueAnchors(boolean aUniqueAnchors) {

    uniqueAnchors = aUniqueAnchors;
  }

  public boolean isUniqueAnchors() {

    return uniqueAnchors;
  }

  public void setSortAnnotationsInMultiValuedFeatures(
          boolean aSortAnnotationsInMultiValuedFeatures) {

    sortAnnotationsInMultiValuedFeatures = aSortAnnotationsInMultiValuedFeatures;
  }

  public boolean isSortAnnotationsInMultiValuedFeatures() {

    return sortAnnotationsInMultiValuedFeatures;
  }

  public boolean isMarkIndexed() {

    return markIndexed;
  }

  /**
   * @param aMarkIndexed
   *          whether to mark indexed feature structures with an asterisk in the anchor and to add a
   *          column indicating the indexing status.
   */
  public void setMarkIndexed(boolean aMarkIndexed) {

    markIndexed = aMarkIndexed;
  }

  public boolean isCoveredTextColumnEnabled() {

    return coveredTextColumnEnabled;
  }

  public void setCoveredTextColumnEnabled(boolean aCoveredTextColumnEnabled) {

    coveredTextColumnEnabled = aCoveredTextColumnEnabled;
  }

  public boolean isIndexedColumnEnabled() {

    return indexedColumnEnabled;
  }

  public void setIndexedColumnEnabled(boolean aIndexedColumnEnabled) {

    indexedColumnEnabled = aIndexedColumnEnabled;
  }

  /**
   * @return to add the view name to the anchor. Should be disabled when this class is used to
   *         compare feature structures across views.
   */
  public boolean isMarkView() {

    return markView;
  }

  public void setMarkView(boolean aMarkView) {

    markView = aMarkView;
  }

  public int getMaxLengthCoveredText() {

    return maxLengthCoveredText;
  }

  public void setMaxLengthCoveredText(int aMaxLengthCoveredText) {

    maxLengthCoveredText = aMaxLengthCoveredText;
  }

  public String getNullValue() {

    return nullValue;
  }

  public void setNullValue(String aNullValue) {

    nullValue = aNullValue;
  }

  public void setTreatEmptyStringsAsNull(boolean aTreatEmptyStringsAsNull) {
    treatEmptyStringsAsNull = aTreatEmptyStringsAsNull;
  }

  public boolean isTreatEmptyStringsAsNull() {
    return treatEmptyStringsAsNull;
  }

  private Pattern pattern(String aRegex) {

    return regexCache.computeIfAbsent(aRegex, ex -> Pattern.compile(ex));
  }

  public String toString(FeatureStructure aFS) {

    if (aFS.getCAS() != cas) {
      throw new IllegalArgumentException("FeatureStructure does not belong to CAS");
    }

    return toString(asList(aFS));
  }

  public String toString(Collection<? extends FeatureStructure> aSeeds) {

    try (StringWriter out = new StringWriter()) {
      write(out, aSeeds);
      return out.toString();
    } catch (IOException e) {
      // The StringWriter shouldn't be throwing any IOExceptions, so if something goes wrong,
      // it must be the fault of the rendering code / feature structure *caugh*
      throw new IllegalArgumentException(e);
    }
  }

  public void write(Writer out) throws IOException {

    write(out, cas.getIndexedFSs());
  }

  private Set<FeatureStructure> getIndexedFses() {

    if (!markIndexed && !indexedColumnEnabled) {
      return null;
    }

    if (_indexedFses == null) {
      _indexedFses = new HashSet<>(cas.getIndexedFSs());
    }

    return _indexedFses;
  }

  public void write(Writer out, Collection<? extends FeatureStructure> aSeeds) throws IOException {

    if (aSeeds.isEmpty()) {
      return;
    }

    for (FeatureStructure fs : aSeeds) {
      if (fs.getCAS() != cas && fs.getCAS() != ((CASImpl) cas).getBaseCAS()) {
        throw new IllegalArgumentException("FeatureStructure does not belong to CAS");
      }
    }

    Set<FeatureStructure> reachableFses = findReachableFeatureStructures(aSeeds);

    // First group by type so we have per-type sections in the output
    Map<Type, List<FeatureStructure>> indexByType = reachableFses.stream()
            .collect(Collectors.groupingBy(fs -> fs.getType()));

    // Ensure that the type sections have a stable order
    List<Type> typesSorted = indexByType.keySet().stream()
            .filter(type -> excludeTypePatterns.stream()
                    .noneMatch(p -> pattern(p).matcher(type.getName()).matches()))
            .sorted(comparing(Type::getName)).collect(Collectors.toList());

    // Build an anchor for every feature structure
    Map<FeatureStructure, Anchor> fsToAnchor = generateAnchors(typesSorted, indexByType);

    // Process the feature structures in each type section
    PrintWriter pout = new PrintWriter(out);
    for (Type type : typesSorted) {
      try (CSVPrinter csv = new CSVPrinter(new CloseShieldAppendable(pout), DEFAULT)) {
        renderHeader(csv, type);

        // Generate all the rows for this type and then we sort them - this is necessary
        // because there can be multiple annotations of the same type at the same location
        // and we need to have a semantically stable ordering - so we take the actual
        // row content into consideration
        // @formatter:off
				List<Pair<FeatureStructure, List<String>>> rows = indexByType.get(type).stream()
						.map(fs -> renderFS(fsToAnchor, fs))
						.sorted(comparing(
								// Compare by type name and offsets
								Pair<FeatureStructure, List<String>>::getKey, new FSComparator())
								// ... then (if necessary) compare by the actual data
								.thenComparing(p -> p.getValue().stream().collect(joining("\0"))))
						.collect(toList());
				// @formatter:on

        for (Pair<FeatureStructure, List<String>> row : rows) {
          csv.printRecord(row.getValue());
        }
      }

      pout.print("\n");
    }
  }

  private String escape(String aString) {

    return StringUtils.replaceEach(aString, new String[] { "\t", "\n", "\r", "[", "]", ",", "\\" },
            new String[] { "\\t", "\\n", "\\r", "\\[", "\\]", "\\,", "\\\\" });
  }

  private void renderHeader(CSVPrinter aCSV, Type aType) throws IOException {

    TypeSystem ts = cas.getTypeSystem();
    Type annotationType = ts.getType(CAS.TYPE_NAME_ANNOTATION);

    // Type as comment
    aCSV.printRecord(aType.getName());

    List<String> sectionHeader = new ArrayList<>();
    sectionHeader.add("<ANCHOR>");

    if (indexedColumnEnabled) {
      sectionHeader.add("<INDEXED>");
    }

    if (coveredTextColumnEnabled && ts.subsumes(annotationType, aType)) {
      sectionHeader.add("<COVERED_TEXT>");
    }

    listFeatures(aType).stream() //
            .filter(f -> !isExcluded(f)) //
            .map(f -> f.getShortName()) //
            .forEachOrdered(sectionHeader::add);
    aCSV.printRecord(sectionHeader);
  }

  /**
   * Build an anchor for every feature structure.
   */
  private Map<FeatureStructure, Anchor> generateAnchors(List<Type> aTypesSorted,
          Map<Type, List<FeatureStructure>> aIndexByType) {

    Set<FeatureStructure> indexedFses = getIndexedFses();
    Map<FeatureStructure, Anchor> fsToAnchor = new HashMap<>();
    Map<String, Integer> disambiguationByPrefix = new HashMap<>();
    for (Type type : aTypesSorted) {
      List<FeatureStructure> fses = new ArrayList<>(aIndexByType.get(type));
      fses.sort(new FSComparator());

      for (FeatureStructure fs : fses) {
        Anchor anchor = new Anchor(fs, markIndexed && indexedFses.contains(fs),
                disambiguationByPrefix);
        fsToAnchor.put(fs, anchor);
      }
    }

    return fsToAnchor;
  }

  private List<Feature> listFeatures(Type aType) {

    // Determine which feature to show in which column
    List<Feature> features = new ArrayList<>(aType.getFeatures());
    features.sort(comparing(Feature::getShortName));

    // Features going into the anchor column are suppressed
    features.removeIf(f -> CAS.FEATURE_BASE_NAME_SOFA.equals(f.getShortName()));
    features.removeIf(f -> CAS.FEATURE_BASE_NAME_BEGIN.equals(f.getShortName()));
    features.removeIf(f -> CAS.FEATURE_BASE_NAME_END.equals(f.getShortName()));

    return features;
  }

  private Pair<FeatureStructure, List<String>> renderFS(Map<FeatureStructure, Anchor> aFsToAnchor,
          FeatureStructure aFS) {

    List<String> data = new ArrayList<>();

    // First column is always the anchor
    data.add(aFsToAnchor.get(aFS).toString());

    // Then add if the FS was in the index
    if (indexedColumnEnabled) {
      data.add(String.valueOf(getIndexedFses().contains(aFS)));
    }

    if (coveredTextColumnEnabled && aFS instanceof AnnotationFS) {
      String coveredText = ((AnnotationFS) aFS).getCoveredText();
      if (maxLengthCoveredText > 0) {
        coveredText = StringUtils.abbreviateMiddle(coveredText, "...", maxLengthCoveredText);
      }
      data.add(escape(coveredText));
    }

    // Process the rest of the features
    nextFeature: for (Feature feature : listFeatures(aFS.getType())) {

      // Check if the feature is excluded
      if (isExcluded(feature)) {
        continue;
      }

      // Primitive features can be rendered as strings
      if (feature.getRange().isStringOrStringSubtype()) {
        data.add(escape(renderStringValue(aFS.getFeatureValueAsString(feature))));
        continue;
      }

      if (feature.getRange().isPrimitive()) {
        data.add(escape(aFS.getFeatureValueAsString(feature)));
        continue;
      }

      // For multi-valued features, we need to dive into the list/array
      if (isMultiValuedFeature(aFS, feature)) {
        data.add(renderMultiValuedFeatureStructure(aFS.getFeatureValue(feature), aFsToAnchor));
        continue nextFeature;
      }

      // So once we get here, it must be a feature structure or null
      FeatureStructure value = aFS.getFeatureValue(feature);
      if (value == null) {
        data.add(nullValue);
        continue nextFeature;
      }

      // Ok, so it's a feature structure
      Anchor anchor = aFsToAnchor.get(value);
      if (anchor == null) {
        throw new IllegalStateException("No anchor - bug - should not happen");
      }
      data.add(anchor.toString());
    }

    return Pair.of(aFS, data);
  }

  private void resetExclusionCache() {

    exclusionCache = null;
  }

  private boolean isExcluded(Feature aFeature) {

    if (exclusionCache == null) {
      exclusionCache = new HashMap<>();
    }

    return exclusionCache.computeIfAbsent(aFeature.getName(),
            f -> excludeFeaturePatterns.stream().anyMatch(p -> pattern(p).matcher(f).matches()));
  }

  private static boolean isMultiValuedFeature(FeatureStructure aFS, Feature aFeature) {
    if (aFeature == null) {
      return false;
    }

    TypeSystem aTS = aFS.getCAS().getTypeSystem();

    return aFeature.getRange().isArray()
            || aTS.subsumes(aTS.getType(CAS.TYPE_NAME_LIST_BASE), aFeature.getRange());
  }

  private boolean isMultiValued(FeatureStructure fs) {
    TypeSystem ts = fs.getCAS().getTypeSystem();
    return fs.getType().isArray() || ts.subsumes(ts.getType(CAS.TYPE_NAME_LIST_BASE), fs.getType());
  }

  private String renderMultiValuedFeatureStructure(FeatureStructure aFS,
          Map<FeatureStructure, Anchor> aFsToAnchor) {
    List<Object> values = multiValuedFeatureStructureToList(aFS);

    if (values == null) {
      return nullValue;
    }

    // Optionally sort multi-valued feature that consist only of annotations. This essentially
    // means that annotation-typed multi-valued features are treated as sets.
    boolean allValuesAreAnnotations = values.stream().allMatch(v -> v instanceof AnnotationFS);
    if (sortAnnotationsInMultiValuedFeatures && allValuesAreAnnotations) {
      values = values.stream().map(v -> (AnnotationFS) v)
              .sorted(comparingInt(AnnotationFS::getBegin)
                      .thenComparing(AnnotationFS::getEnd, reverseOrder())
                      .thenComparing(r -> r.getType().getName()))
              .collect(Collectors.toList());
    }

    List<String> items = new ArrayList<>();
    nextItem: for (Object item : values) {
      if (item == null) {
        items.add(nullValue);
        continue nextItem;
      }

      if (item instanceof String) {
        items.add(escape(renderStringValue((String) item)));
        continue nextItem;
      }

      if (item instanceof FeatureStructure) {
        FeatureStructure fsItem = (FeatureStructure) item;
        if (isMultiValued(fsItem)) {
          items.add(renderMultiValuedFeatureStructure(fsItem, aFsToAnchor));
        } else {
          Anchor anchor = aFsToAnchor.get(fsItem);
          if (anchor == null) {
            throw new IllegalStateException("No anchor - bug - should not happen");
          }
          items.add(anchor.toString());
        }
        continue nextItem;
      }

      items.add(escape(String.valueOf(item)));
    }

    return items.stream().collect(joining(",", "[", "]"));
  }

  private String renderStringValue(String aString) {
    if ((aString == null) || (treatEmptyStringsAsNull && aString.isEmpty())) {
      return nullValue;
    }

    return aString;
  }

  // This method was derived from uimaFIT FSUtil.getFeature()
  private List<Object> multiValuedFeatureStructureToList(FeatureStructure aValue) {

    if (aValue == null) {
      return null;
    }

    // Handle case where feature is an array
    TypeSystem ts = aValue.getCAS().getTypeSystem();
    Object target = null;
    int length = -1;
    if (aValue instanceof CommonArrayFS) {
      CommonArrayFS<?> source = (CommonArrayFS<?>) aValue;
      length = source.size();
      if (aValue instanceof BooleanArrayFS) {
        target = new boolean[length];
        ((BooleanArrayFS) source).copyToArray(0, (boolean[]) target, 0, length);
      } else if (aValue instanceof ByteArrayFS) {
        target = new byte[length];
        ((ByteArrayFS) source).copyToArray(0, (byte[]) target, 0, length);
      } else if (aValue instanceof DoubleArrayFS) {
        target = new double[length];
        ((DoubleArrayFS) source).copyToArray(0, (double[]) target, 0, length);
      } else if (aValue instanceof FloatArrayFS) {
        target = new float[length];
        ((FloatArrayFS) source).copyToArray(0, (float[]) target, 0, length);
      } else if (aValue instanceof IntArrayFS) {
        target = new int[length];
        ((IntArrayFS) source).copyToArray(0, (int[]) target, 0, length);
      } else if (aValue instanceof LongArrayFS) {
        target = new long[length];
        ((LongArrayFS) source).copyToArray(0, (long[]) target, 0, length);
      } else if (aValue instanceof ShortArrayFS) {
        target = new short[length];
        ((ShortArrayFS) source).copyToArray(0, (short[]) target, 0, length);
      } else if (aValue instanceof StringArrayFS) {
        target = new String[length];
        ((StringArrayFS) source).copyToArray(0, (String[]) target, 0, length);
      } else {
        target = new FeatureStructure[length];
        ((ArrayFS<?>) source).copyToArray(0, (FeatureStructure[]) target, 0, length);
      }
    }
    // Handle case where feature is a list
    else if (ts.subsumes(ts.getType(CAS.TYPE_NAME_LIST_BASE), aValue.getType())) {
      // Get length of list
      length = 0;
      {
        FeatureStructure cur = aValue;
        // We assume to by facing a non-empty element if it has a "head" feature
        while (cur.getType().getFeatureByBaseName(CAS.FEATURE_BASE_NAME_HEAD) != null) {
          length++;
          cur = cur.getFeatureValue(cur.getType().getFeatureByBaseName(CAS.FEATURE_BASE_NAME_TAIL));
        }
      }

      if (ts.subsumes(ts.getType(CAS.TYPE_NAME_FLOAT_LIST), aValue.getType())) {
        float[] floatTarget = new float[length];
        FeatureStructure cur = aValue;
        // We assume to by facing a non-empty element if it has a "head" feature
        int i = 0;
        while (cur.getType().getFeatureByBaseName(CAS.FEATURE_BASE_NAME_HEAD) != null) {
          floatTarget[i] = cur
                  .getFloatValue(cur.getType().getFeatureByBaseName(CAS.FEATURE_BASE_NAME_HEAD));
          cur = cur.getFeatureValue(cur.getType().getFeatureByBaseName(CAS.FEATURE_BASE_NAME_TAIL));
          i++;
        }
        target = floatTarget;
      } else if (ts.subsumes(ts.getType(CAS.TYPE_NAME_INTEGER_LIST), aValue.getType())) {
        int[] intTarget = new int[length];
        FeatureStructure cur = aValue;
        // We assume to by facing a non-empty element if it has a "head" feature
        int i = 0;
        while (cur.getType().getFeatureByBaseName(CAS.FEATURE_BASE_NAME_HEAD) != null) {
          intTarget[i] = cur
                  .getIntValue(cur.getType().getFeatureByBaseName(CAS.FEATURE_BASE_NAME_HEAD));
          cur = cur.getFeatureValue(cur.getType().getFeatureByBaseName(CAS.FEATURE_BASE_NAME_TAIL));
          i++;
        }
        target = intTarget;
      } else if (ts.subsumes(ts.getType(CAS.TYPE_NAME_STRING_LIST), aValue.getType())) {
        String[] stringTarget = new String[length];
        FeatureStructure cur = aValue;
        // We assume to by facing a non-empty element if it has a "head" feature
        int i = 0;
        while (cur.getType().getFeatureByBaseName(CAS.FEATURE_BASE_NAME_HEAD) != null) {
          stringTarget[i] = cur
                  .getStringValue(cur.getType().getFeatureByBaseName(CAS.FEATURE_BASE_NAME_HEAD));
          cur = cur.getFeatureValue(cur.getType().getFeatureByBaseName(CAS.FEATURE_BASE_NAME_TAIL));
          i++;
        }
        target = stringTarget;
      } else if (ts.subsumes(ts.getType(CAS.TYPE_NAME_FS_LIST), aValue.getType())) {
        target = new FeatureStructure[length];
        FeatureStructure cur = aValue;
        // We assume to by facing a non-empty element if it has a "head" feature
        int i = 0;
        while (cur.getType().getFeatureByBaseName(CAS.FEATURE_BASE_NAME_HEAD) != null) {
          Array.set(target, i, cur
                  .getFeatureValue(cur.getType().getFeatureByBaseName(CAS.FEATURE_BASE_NAME_HEAD)));
          cur = cur.getFeatureValue(cur.getType().getFeatureByBaseName(CAS.FEATURE_BASE_NAME_TAIL));
          i++;
        }
      } else {
        throw new IllegalStateException(
                "Unsupported list type [" + aValue.getType().getName() + "]");
      }
    }

    if (length == -1) {
      throw new IllegalStateException("Unable to extract values");
    }

    List<Object> targetCollection = new ArrayList<>();
    for (int i = 0; i < length; i++) {
      targetCollection.add(Array.get(target, i));
    }

    return targetCollection;
  }

  private Set<FeatureStructure> findReachableFeatureStructures(
          Collection<? extends FeatureStructure> aSeeds) {

    // Collect the seed points for the reachability tracking. We use a set here instead of a
    // Deque because we use the contains() method on this queue and it is slow for typical
    // Deques (e.g. LinkedList) but fast for sets.
    Set<FeatureStructure> toProcess = new LinkedHashSet<>(aSeeds);

    // Collect all feature structures that are reachable via the seed points
    Set<FeatureStructure> seen = new HashSet<>();
    while (!toProcess.isEmpty()) {
      // Poll the next element from the processing queue
      FeatureStructure fs = toProcess.iterator().next();
      toProcess.remove(fs);

      if (seen.contains(fs)) {
        continue;
      }

      seen.add(fs);

      if (isMultiValued(fs)) {
        List<Object> values = multiValuedFeatureStructureToList(fs);
        if (values != null) {
          values.stream().filter(v -> v instanceof FeatureStructure).filter(v -> !seen.contains(v))
                  .forEach(v -> toProcess.add((FeatureStructure) v));
        }
      } else {
        for (Feature feature : fs.getType().getFeatures()) {
          

          // Check if the feature is excluded
          if (feature.getRange().isPrimitive() || isExcluded(feature) || CAS.FEATURE_BASE_NAME_SOFA.equals(feature.getShortName())) {
            continue;
          }

          if (isMultiValuedFeature(fs, feature)) {
            List<Object> featureValues = multiValuedFeatureStructureToList(
                    fs.getFeatureValue(feature));

            if (featureValues != null) {
              for (Object value : featureValues) {
                if (value instanceof FeatureStructure && !seen.contains(value)) {
                  toProcess.add((FeatureStructure) value);
                }
              }
            }
          } else {
            FeatureStructure value = fs.getFeatureValue(feature);
            if (value != null && !seen.contains(value)) {
              toProcess.add(value);
            }
          }
        }
      }
    }

    return seen;
  }

  private int featureHash(FeatureStructure aFS) {
    int hash = 0;
    for (Feature f : aFS.getType().getFeatures()) {
      if (f.getRange().isStringOrStringSubtype() || f.getRange().isPrimitive()) {
        String value = renderStringValue(aFS.getFeatureValueAsString(f));
        hash += value != null ? value.hashCode() : 0;
        continue;
      }

      if (f.getRange().isArray()) {
        if (f.getRange().getComponentType().isStringOrStringSubtype()) {
          StringArrayFS array = ((StringArrayFS) aFS.getFeatureValue(f));
          if (array != null) {
            for (int i = 0; i < array.size(); i++) {
              String v = renderStringValue(array.get(i));
              hash += v != null ? v.hashCode() : 0;
            }
          }
          continue;
        }

        switch (f.getRange().getComponentType().getName()) {
          case CAS.TYPE_NAME_BOOLEAN: {
            BooleanArrayFS array = ((BooleanArrayFS) aFS.getFeatureValue(f));
            if (array != null) {
              for (int i = 0; i < array.size(); i++) {
                hash += array.get(i) ? -(i + 1) : (i + 1);
              }
            }
            break;
          }
          case CAS.TYPE_NAME_BYTE: {
            ByteArrayFS array = ((ByteArrayFS) aFS.getFeatureValue(f));
            if (array != null) {
              for (int i = 0; i < array.size(); i++) {
                hash += array.get(i);
              }
            }
            break;
          }
          case CAS.TYPE_NAME_DOUBLE: {
            DoubleArrayFS array = ((DoubleArrayFS) aFS.getFeatureValue(f));
            if (array != null) {
              for (int i = 0; i < array.size(); i++) {
                hash += Double.hashCode(array.get(i));
              }
            }
            break;
          }
          case CAS.TYPE_NAME_FLOAT: {
            FloatArrayFS array = ((FloatArrayFS) aFS.getFeatureValue(f));
            if (array != null) {
              for (int i = 0; i < array.size(); i++) {
                hash += Float.hashCode(array.get(i));
              }
            }
            break;
          }
          case CAS.TYPE_NAME_INTEGER: {
            IntArrayFS array = ((IntArrayFS) aFS.getFeatureValue(f));
            if (array != null) {
              for (int i = 0; i < array.size(); i++) {
                hash += array.get(i);
              }
            }
            break;
          }
          case CAS.TYPE_NAME_LONG: {
            LongArrayFS array = ((LongArrayFS) aFS.getFeatureValue(f));
            if (array != null) {
              for (int i = 0; i < array.size(); i++) {
                hash += Long.hashCode(array.get(i));
              }
            }
            break;
          }
          case CAS.TYPE_NAME_SHORT: {
            ShortArrayFS array = ((ShortArrayFS) aFS.getFeatureValue(f));
            if (array != null) {
              for (int i = 0; i < array.size(); i++) {
                hash += array.get(i);
              }
            }
            break;
          }
          case CAS.TYPE_NAME_FS_ARRAY:
            // We cannot really recursively calculate the hash... let's just use the array length
            if (aFS.getFeatureValue(f) != null) {
              hash *= ((CommonArrayFS) aFS.getFeatureValue(f)).size() + 1;
            }
            break;
        }
      }

      // If we get here, it is a feature structure reference... we cannot really recursively
      // go into it to calculate a recursive hash... so we just check if the value is non-null
      hash *= aFS.getFeatureValue(f) != null ? 1 : -1;
    }

    return hash;
  }

  private static class CloseShieldAppendable implements Appendable, Closeable {

    private final Appendable delegate;

    public CloseShieldAppendable(Appendable aDelegate) {

      delegate = aDelegate;
    }

    @Override
    public Appendable append(CharSequence aSequence) throws IOException {

      return delegate.append(aSequence);
    }

    @Override
    public Appendable append(CharSequence aSequence, int aStart, int aEnd) throws IOException {

      return delegate.append(aSequence, aStart, aEnd);
    }

    @Override
    public Appendable append(char aCharacter) throws IOException {

      return delegate.append(aCharacter);
    }

    @Override
    public void close() throws IOException {

      // Do not forward close
    }
  }

  private class FSComparator implements Comparator<FeatureStructure> {

    @Override
    public int compare(FeatureStructure aFS1, FeatureStructure aFS2) {

      if (aFS1 == aFS2) {
        return 0;
      }

      // Same name?
      int nameCmp = aFS2.getType().getName().compareTo(aFS2.getType().getName());
      if (nameCmp != 0) {
        return nameCmp;
      }

      // Annotation? Then sort by offsets
      boolean fs1IsAnnotation = aFS1 instanceof AnnotationFS;
      boolean fs2IsAnnotation = aFS2 instanceof AnnotationFS;
      if (fs1IsAnnotation != fs2IsAnnotation) {
        return -1;
      }
      if (fs1IsAnnotation && fs2IsAnnotation) {
        AnnotationFS ann1 = (AnnotationFS) aFS1;
        AnnotationFS ann2 = (AnnotationFS) aFS2;

        // Ascending by begin
        int beginCmp = ann1.getBegin() - ann2.getBegin();
        if (beginCmp != 0) {
          return beginCmp;
        }

        // Descending by end
        int endCmp = ann2.getEnd() - ann1.getEnd();
        if (endCmp != 0) {
          return endCmp;
        }
      }

      // Ok, so let's calculate a hash over the features then...
      int fh1 = featureHash(aFS1);
      int fh2 = featureHash(aFS2);
      if (fh1 < fh2) {
        return -1;
      }
      if (fh1 > fh2) {
        return 1;
      }
      return 0;
    }
  }

  private class Anchor {

    private final String stringValue;
    private final int disambiguationId;

    public Anchor(FeatureStructure aFS, boolean aIndexed,
            Map<String, Integer> aDisambiguationByPrefix) {

      StringBuilder anchor = new StringBuilder();

      anchor.append(aFS.getType().getShortName());

      // Special handling for AnnotationFS
      if (aFS instanceof AnnotationFS) {
        AnnotationFS ann = (AnnotationFS) aFS;
        anchor.append("[");
        anchor.append(ann.getBegin());
        anchor.append("-");
        anchor.append(ann.getEnd());
        anchor.append("]");
      }

      if (markIndexed && aIndexed) {
        anchor.append("*");
      }

      // Special handling for AnnotationBase
      if (markView && aFS instanceof AnnotationBase) {
        AnnotationBase annBase = (AnnotationBase) aFS;
        anchor.append('@');
        anchor.append(String.valueOf(annBase.getSofa().getSofaID()));
      }

      // If we have the same anchor multiple times, then we need to disambiguate
      String prefix = anchor.toString();
      disambiguationId = aDisambiguationByPrefix.computeIfAbsent(prefix, key -> 0);

      if (uniqueAnchors && disambiguationId > 0) {
        anchor.append("(");
        anchor.append(disambiguationId);
        anchor.append(")");
      }

      aDisambiguationByPrefix.put(prefix, disambiguationId + 1);

      stringValue = anchor.toString();
    }

    @Override
    public String toString() {

      return stringValue;
    }
  }

  public static String toComparableString(CAS aCas) {
    try (StringWriter sourceCasRepresentationBuffer = new StringWriter()) {
      new CasToComparableText(aCas).write(sourceCasRepresentationBuffer);
      return sourceCasRepresentationBuffer.toString();
    } catch (IOException e) {
      // This should normally never happen, so it should be ok to not throw a checked exception here
      throw new IllegalStateException("Unable to serialize CAS", e);
    }
  }
}
