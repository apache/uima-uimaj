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
 * Reference implementation of {@link Capability}
 * 
 * 
 */
public class Capability_impl extends MetaDataObject_impl implements Capability {

  private static final long serialVersionUID = -2821073595288674925L;

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

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.metadata.Capability#getDescription()
   */
  @Override
  public String getDescription() {
    return mDescription;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.metadata.Capability#setDescription(java.lang.String)
   */
  @Override
  public void setDescription(String aDescription) {
    mDescription = aDescription;
  }

  /**
   * @see org.apache.uima.resource.metadata.Capability#getInputs()
   */
  @Override
  public TypeOrFeature[] getInputs() {
    return mInputs;
  }

  /**
   * @see org.apache.uima.resource.metadata.Capability#getOutputs()
   */
  @Override
  public TypeOrFeature[] getOutputs() {
    return mOutputs;
  }

  /**
   * @see org.apache.uima.resource.metadata.Capability#getPreconditions()
   */
  @Override
  public Precondition[] getPreconditions() {
    return mPreconditions;
  }

  /**
   * @see org.apache.uima.resource.metadata.Capability#getLanguagesSupported()
   */
  @Override
  public String[] getLanguagesSupported() {
    // search for LanguagePreconditions
    Precondition[] preconditions = getPreconditions();
    for (int i = 0; i < preconditions.length; i++) {
      if (preconditions[i] instanceof LanguagePrecondition) {
        return ((LanguagePrecondition) preconditions[i]).getLanguages();
      }
    }

    // No language precondition found. Return an empty array.
    return Constants.EMPTY_STRING_ARRAY;
  }

  /**
   * @see org.apache.uima.resource.metadata.Capability#getMimeTypesSupported()
   */
  @Override
  public String[] getMimeTypesSupported() {
    // search for MimeTypePreconditions
    Precondition[] preconditions = getPreconditions();
    for (int i = 0; i < preconditions.length; i++) {
      if (preconditions[i] instanceof MimeTypePrecondition) {
        return ((MimeTypePrecondition) preconditions[i]).getMimeTypes();
      }
    }

    // No language precondition found. Return an empty array.
    return Constants.EMPTY_STRING_ARRAY;
  }

  /**
   * @see org.apache.uima.resource.metadata.Capability#setInputs(TypeOrFeature[])
   */
  @Override
  public void setInputs(TypeOrFeature[] aInputs) {
    mInputs = aInputs;
  }

  /**
   * @see org.apache.uima.resource.metadata.Capability#setOutputs(TypeOrFeature[])
   */
  @Override
  public void setOutputs(TypeOrFeature[] aOutputs) {
    mOutputs = aOutputs;
  }

  /**
   * @see org.apache.uima.resource.metadata.Capability#setPreconditions(Precondition[])
   */
  @Override
  public void setPreconditions(Precondition[] aPreconditions) {
    mPreconditions = aPreconditions;
  }

  /**
   * @see org.apache.uima.resource.metadata.Capability#setLanguagesSupported(String[])
   */
  @Override
  public void setLanguagesSupported(String[] aLanguageIDs) {
    // create a list of existing preconditions
    List<Precondition> preconditions = new ArrayList<>();
    Precondition[] precondArray = getPreconditions();
    if (precondArray != null) {
      preconditions.addAll(Arrays.asList(precondArray));
    }

    // remove any existing LanguagePrecondtiions
    preconditions.removeIf(p -> p instanceof LanguagePrecondition);

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

  /**
   * @see org.apache.uima.resource.metadata.Capability#setMimeTypesSupported(java.lang.String[])
   */
  @Override
  public void setMimeTypesSupported(String[] aMimeTypes) {
    // create a list of existing preconditions
    List<Precondition> preconditions = new ArrayList<>();
    Precondition[] precondArray = getPreconditions();
    if (precondArray != null) {
      preconditions.addAll(Arrays.asList(precondArray));
    }

    // remove any existing MimeTypePrecondtiions
    preconditions.removeIf(p -> p instanceof MimeTypePrecondition);

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

  /**
   * @see org.apache.uima.resource.metadata.Capability#addInputType(java.lang.String, boolean)
   */
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

  /**
   * @see org.apache.uima.resource.metadata.Capability#addInputFeature(java.lang.String)
   */
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

  /**
   * @see org.apache.uima.resource.metadata.Capability#addOutputType(java.lang.String, boolean)
   */
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

  /**
   * @see org.apache.uima.resource.metadata.Capability#addOutputFeature(java.lang.String)
   */
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

  /**
   * @see org.apache.uima.resource.metadata.Capability#addSupportedLanguage(java.lang.String)
   */
  @Override
  public void addSupportedLanguage(String aLanguage) {
    String[] oldArr = getLanguagesSupported();
    String[] newArr = new String[oldArr.length + 1];
    System.arraycopy(oldArr, 0, newArr, 0, oldArr.length);
    newArr[newArr.length - 1] = aLanguage;
    setLanguagesSupported(newArr);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.metadata.Capability#removeSupportedLanguage(java.lang.String)
   */
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

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.metadata.Capability#getInputSofaNames()
   */
  @Override
  public String[] getInputSofas() {
    return mInputSofas;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.metadata.Capability#getOutputSofaNames()
   */
  @Override
  public String[] getOutputSofas() {
    return mOutputSofas;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.metadata.Capability#setInputSofaNames(java.lang.String[])
   */
  @Override
  public void setInputSofas(String[] aInputSofaNames) {
    mInputSofas = aInputSofaNames;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.metadata.Capability#setOutputSofaNames(java.lang.String[])
   */
  @Override
  public void setOutputSofas(String[] aOutputSofaNames) {
    mOutputSofas = aOutputSofaNames;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.metadata.Capability#addInputSofaName(java.lang.String)
   */
  @Override
  public void addInputSofa(String aSofaName) {
    String[] oldArr = getInputSofas();
    String[] newArr = new String[oldArr.length + 1];
    System.arraycopy(oldArr, 0, newArr, 0, oldArr.length);
    newArr[newArr.length - 1] = aSofaName;
    setInputSofas(newArr);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.metadata.Capability#addOutputSofaName(java.lang.String)
   */
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

  static final private XmlizationInfo XMLIZATION_INFO = new XmlizationInfo("capability",
          new PropertyXmlInfo[] { new PropertyXmlInfo("description"),
              new PropertyXmlInfo("inputs", false), new PropertyXmlInfo("outputs", false),
              new PropertyXmlInfo("inputSofas", "inputSofas", true, "sofaName"),
              new PropertyXmlInfo("outputSofas", "outputSofas", true, "sofaName"),
              new PropertyXmlInfo("languagesSupported", "languagesSupported", false, "language"),
              new PropertyXmlInfo("mimeTypesSupported", "mimeTypesSupported", true, "mimeType")

          });
}
