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

package org.apache.uima.resource.metadata.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.TypeOrFeature;
import org.apache.uima.resource.metadata.Capability;
import org.apache.uima.resource.metadata.LanguagePrecondition;
import org.apache.uima.resource.metadata.MimeTypePrecondition;
import org.apache.uima.resource.metadata.Precondition;
import org.apache.uima.util.impl.Constants;

/**
 * Reference implementation of {@link Capability}.
 */
public class Capability_impl extends MetaDataObject_impl implements Capability {

  static final long serialVersionUID = -2821073595288674925L;

  private final static TypeOrFeature[] EMPTY_TYPE_OR_FEATURE_ARRAY = new TypeOrFeature[0];

  private final static Precondition[] EMPTY_PRECONDITION_ARRAY = new Precondition[0];

  /** a description of this capability */
  private String mDescription;

  /** Input Types and/or Features. */
  private TypeOrFeature[] mInputs = EMPTY_TYPE_OR_FEATURE_ARRAY;

  /** Output Types and/or Features. */
  private TypeOrFeature[] mOutputs = EMPTY_TYPE_OR_FEATURE_ARRAY;

  /** Preconditions (includes languages supported). */
  private Precondition[] mPreconditions = EMPTY_PRECONDITION_ARRAY;

  /** input SofAs */
  private String[] mInputSofas = Constants.EMPTY_STRING_ARRAY;

  /** output SofAs */
  private String[] mOutputSofas = Constants.EMPTY_STRING_ARRAY;

  @Override
  public String getDescription() {
    return mDescription;
  }

  @Override
  public void setDescription(String aDescription) {
    mDescription = aDescription;
  }

  @Override
  public TypeOrFeature[] getInputs() {
    return mInputs;
  }

  @Override
  public TypeOrFeature[] getOutputs() {
    return mOutputs;
  }

  @Override
  public Precondition[] getPreconditions() {
    return mPreconditions;
  }

  @Override
  public String[] getLanguagesSupported() {
    // search for LanguagePreconditions
    Precondition[] preconditions = getPreconditions();
    for (int i = 0; i < preconditions.length; i++) {
      if (preconditions[i] instanceof LanguagePrecondition languagePrecondition) {
        return (languagePrecondition.getLanguages();
      }
    }

    // No language precondition found. Return an empty array.
    return Constants.EMPTY_STRING_ARRAY;
  }

  @Override
  public String[] getMimeTypesSupported() {
    // search for MimeTypePreconditions
    Precondition[] preconditions = getPreconditions();
    for (Precondition precondition : preconditions) {
      if (precondition instanceof MimeTypePrecondition mimeTypePrecondition) {
        return mimeTypePrecondition.getMimeTypes();
      }
    }

    // No language precondition found. Return an empty array.
    return Constants.EMPTY_STRING_ARRAY;
  }

  @Override
  public void setInputs(TypeOrFeature... aInputs) {
    mInputs = aInputs;
  }

  @Override
  public void setOutputs(TypeOrFeature... aOutputs) {
    mOutputs = aOutputs;
  }

  @Override
  public void setPreconditions(Precondition... aPreconditions) {
    mPreconditions = aPreconditions;
  }

  @Override
  public void setLanguagesSupported(String... aLanguageIDs) {
    // create a list of existing preconditions
    List<Precondition> preconditions = new ArrayList<>();
    Precondition[] precondArray = getPreconditions();
    if (precondArray != null) {
      preconditions.addAll(Arrays.asList(precondArray));
    }

    // remove any existing LanguagePrecondtiions
    preconditions.removeIf(LanguagePrecondition.class::isInstance);

    // add new precondition
    if (aLanguageIDs != null && aLanguageIDs.length > 0) {
      LanguagePrecondition languagePrecond = new LanguagePrecondition_impl();
      languagePrecond.setLanguages(aLanguageIDs);
      preconditions.add(languagePrecond);
    }

    // set attribute value
    Precondition[] newPrecondArray = new Precondition[preconditions.size()];
    preconditions.toArray(newPrecondArray);
    setPreconditions(newPrecondArray);
  }

  @Override
  public void setMimeTypesSupported(String... aMimeTypes) {
    // create a list of existing preconditions
    List<Precondition> preconditions = new ArrayList<>();
    Precondition[] precondArray = getPreconditions();
    if (precondArray != null) {
      preconditions.addAll(Arrays.asList(precondArray));
    }

    // remove any existing MimeTypePrecondtiions
    preconditions.removeIf(MimeTypePrecondition.class::isInstance);

    // add new precondition
    if (aMimeTypes != null && aMimeTypes.length > 0) {
      MimeTypePrecondition mimeTypePrecond = new MimeTypePrecondition_impl();
      mimeTypePrecond.setMimeTypes(aMimeTypes);
      preconditions.add(mimeTypePrecond);
    }

    // set attribute value
    Precondition[] newPrecondArray = new Precondition[preconditions.size()];
    preconditions.toArray(newPrecondArray);
    setPreconditions(newPrecondArray);
  }

  @Override
  public void addInputType(String aTypeName, boolean aAllAnnotatorFeatures) {
    TypeOrFeature type = UIMAFramework.getResourceSpecifierFactory().createTypeOrFeature();
    type.setType(true);
    type.setName(aTypeName);
    type.setAllAnnotatorFeatures(aAllAnnotatorFeatures);

    TypeOrFeature[] oldArr = getInputs();
    TypeOrFeature[] newArr = new TypeOrFeature[oldArr.length + 1];
    System.arraycopy(oldArr, 0, newArr, 0, oldArr.length);
    newArr[newArr.length - 1] = type;
    setInputs(newArr);
  }

  @Override
  public void addInputFeature(String aFeatureName) {
    TypeOrFeature feat = UIMAFramework.getResourceSpecifierFactory().createTypeOrFeature();
    feat.setType(false);
    feat.setName(aFeatureName);

    TypeOrFeature[] oldArr = getInputs();
    TypeOrFeature[] newArr = new TypeOrFeature[oldArr.length + 1];
    System.arraycopy(oldArr, 0, newArr, 0, oldArr.length);
    newArr[newArr.length - 1] = feat;
    setInputs(newArr);
  }

  @Override
  public void addOutputType(String aTypeName, boolean aAllAnnotatorFeatures) {
    TypeOrFeature type = UIMAFramework.getResourceSpecifierFactory().createTypeOrFeature();
    type.setType(true);
    type.setName(aTypeName);
    type.setAllAnnotatorFeatures(aAllAnnotatorFeatures);

    TypeOrFeature[] oldArr = getOutputs();
    TypeOrFeature[] newArr = new TypeOrFeature[oldArr.length + 1];
    System.arraycopy(oldArr, 0, newArr, 0, oldArr.length);
    newArr[newArr.length - 1] = type;
    setOutputs(newArr);
  }

  @Override
  public void addOutputFeature(String aFeatureName) {
    TypeOrFeature feat = UIMAFramework.getResourceSpecifierFactory().createTypeOrFeature();
    feat.setType(false);
    feat.setName(aFeatureName);

    TypeOrFeature[] oldArr = getOutputs();
    TypeOrFeature[] newArr = new TypeOrFeature[oldArr.length + 1];
    System.arraycopy(oldArr, 0, newArr, 0, oldArr.length);
    newArr[newArr.length - 1] = feat;
    setOutputs(newArr);
  }

  @Override
  public void addSupportedLanguage(String aLanguage) {
    String[] oldArr = getLanguagesSupported();
    String[] newArr = new String[oldArr.length + 1];
    System.arraycopy(oldArr, 0, newArr, 0, oldArr.length);
    newArr[newArr.length - 1] = aLanguage;
    setLanguagesSupported(newArr);
  }

  @Override
  public void removeSupportedLanguage(String aLanguage) {
    String[] current = getLanguagesSupported();
    for (int i = 0; i < current.length; i++) {
      if (current[i].equals(aLanguage)) {
        String[] newArr = new String[current.length - 1];
        System.arraycopy(current, 0, newArr, 0, i);
        System.arraycopy(current, i + 1, newArr, i, current.length - i - 1);
        setLanguagesSupported(newArr);
        break;
      }
    }
  }

  @Override
  public String[] getInputSofas() {
    return mInputSofas;
  }

  @Override
  public String[] getOutputSofas() {
    return mOutputSofas;
  }

  @Override
  public void setInputSofas(String... aInputSofaNames) {
    mInputSofas = aInputSofaNames;
  }

  @Override
  public void setOutputSofas(String... aOutputSofaNames) {
    mOutputSofas = aOutputSofaNames;
  }

  @Override
  public void addInputSofa(String aSofaName) {
    String[] oldArr = getInputSofas();
    String[] newArr = new String[oldArr.length + 1];
    System.arraycopy(oldArr, 0, newArr, 0, oldArr.length);
    newArr[newArr.length - 1] = aSofaName;
    setInputSofas(newArr);
  }

  @Override
  public void addOutputSofa(String aSofaName) {
    String[] oldArr = getOutputSofas();
    String[] newArr = new String[oldArr.length + 1];
    System.arraycopy(oldArr, 0, newArr, 0, oldArr.length);
    newArr[newArr.length - 1] = aSofaName;
    setOutputSofas(newArr);
  }

  @Override
  protected XmlizationInfo getXmlizationInfo() {
    return XMLIZATION_INFO;
  }

  private static final XmlizationInfo XMLIZATION_INFO = new XmlizationInfo("capability",
          new PropertyXmlInfo[] { new PropertyXmlInfo("description"),
              new PropertyXmlInfo("inputs", false), new PropertyXmlInfo("outputs", false),
              new PropertyXmlInfo("inputSofas", "inputSofas", true, "sofaName"),
              new PropertyXmlInfo("outputSofas", "outputSofas", true, "sofaName"),
              new PropertyXmlInfo("languagesSupported", "languagesSupported", false, "language"),
              new PropertyXmlInfo("mimeTypesSupported", "mimeTypesSupported", true, "mimeType")

          });
}
