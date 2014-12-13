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

package org.apache.uima.analysis_engine.metadata.impl;

import java.util.List;

import org.apache.uima.UIMAFramework;
import org.apache.uima.UIMA_IllegalArgumentException;
import org.apache.uima.analysis_engine.metadata.AnalysisEngineMetaData;
import org.apache.uima.analysis_engine.metadata.FlowConstraints;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.metadata.Capability;
import org.apache.uima.resource.metadata.FsIndexCollection;
import org.apache.uima.resource.metadata.FsIndexDescription;
import org.apache.uima.resource.metadata.OperationalProperties;
import org.apache.uima.resource.metadata.TypePriorities;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.resource.metadata.impl.FsIndexCollection_impl;
import org.apache.uima.resource.metadata.impl.PropertyXmlInfo;
import org.apache.uima.resource.metadata.impl.ResourceMetaData_impl;
import org.apache.uima.resource.metadata.impl.XmlizationInfo;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLParser;
import org.apache.uima.util.XMLParser.ParsingOptions;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * Reference implementation of {@link AnalysisEngineMetaData}.
 * 
 * 
 */
public class AnalysisEngineMetaData_impl extends ResourceMetaData_impl implements
        AnalysisEngineMetaData {
  static final long serialVersionUID = -3030574527767871396L;

  private boolean mAsynchronousModeSupported;

  private FlowConstraints mFlowConstraints;

  private AnalysisEngineMetaData[] mDelegateAnalysisEngineMetaData;

  private Capability[] mCapabilities = Capability.EMPTY_CAPABILITIES;

  private TypeSystemDescription mTypeSystem;

  private TypePriorities mTypePriorities;

  private FsIndexCollection mFsIndexCollection;

  private OperationalProperties mOperationalProperties;

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.metadata.ProcessingResourceMetaData#resolveImports()
   */
  public void resolveImports() throws InvalidXMLException {
    resolveImports(UIMAFramework.newDefaultResourceManager());
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.metadata.ProcessingResourceMetaData#resolveImports(org.apache.uima.resource.ResourceManager)
   */
  public void resolveImports(ResourceManager aResourceManager) throws InvalidXMLException {
    if (getTypeSystem() != null) {
      getTypeSystem().resolveImports(aResourceManager);
    }
    if (getTypePriorities() != null) {
      getTypePriorities().resolveImports(aResourceManager);
    }
    if (getFsIndexCollection() != null) {
      getFsIndexCollection().resolveImports(aResourceManager);
    }
  }

  /**
   * @see org.apache.uima.analysis_engine.metadata.AnalysisEngineMetaData#getCapabilities()
   */
  public Capability[] getCapabilities() {
    return mCapabilities;
  }

  /**
   * @see org.apache.uima.analysis_engine.metadata.AnalysisEngineMetaData#setCapabilities(Capability[])
   */
  public void setCapabilities(Capability[] aCapabilities) {
    if (aCapabilities == null) {
      throw new UIMA_IllegalArgumentException(UIMA_IllegalArgumentException.ILLEGAL_ARGUMENT,
              new Object[] { "null", "aCapabilities", "setCapabilities" });
    }
    mCapabilities = aCapabilities;
  }

  /**
   * @see org.apache.uima.analysis_engine.metadata.AnalysisEngineMetaData#getTypeSystem()
   */
  public TypeSystemDescription getTypeSystem() {
    return mTypeSystem;
  }

  /**
   * @see org.apache.uima.analysis_engine.metadata.AnalysisEngineMetaData#setTypeSystem(TypeSystemDescription)
   */
  public void setTypeSystem(TypeSystemDescription aTypeSystem) {
    mTypeSystem = aTypeSystem;
  }

  /**
   * @see org.apache.uima.resource.metadata.ProcessingResourceMetaData#getTypePriorities()
   */
  public TypePriorities getTypePriorities() {
    return mTypePriorities;
  }

  /**
   * @see org.apache.uima.resource.metadata.ProcessingResourceMetaData#setTypePriorities(TypePriorities)
   */
  public void setTypePriorities(TypePriorities aTypePriorities) {
    mTypePriorities = aTypePriorities;
  }

  /** 
   * @see org.apache.uima.resource.metadata.ProcessingResourceMetaData#getFsIndexCollection()
   */
  public FsIndexCollection getFsIndexCollection() {
    return mFsIndexCollection;
  }

  /**
   * @see org.apache.uima.resource.metadata.ProcessingResourceMetaData#setFsIndexCollection(FsIndexCollection)
   */
  public void setFsIndexCollection(FsIndexCollection aFsIndexCollection) {
    mFsIndexCollection = aFsIndexCollection;
  }

  /**
   * @see org.apache.uima.analysis_engine.metadata.AnalysisEngineMetaData#getFsIndexes()
   */
  public FsIndexDescription[] getFsIndexes() {
    return mFsIndexCollection == null ? null : mFsIndexCollection.getFsIndexes();
  }

  /**
   * @see org.apache.uima.analysis_engine.metadata.AnalysisEngineMetaData#setFsIndexes(FsIndexDescription[])
   */
  public void setFsIndexes(FsIndexDescription[] aFsIndexes) {
    if (mFsIndexCollection == null) {
      mFsIndexCollection = new FsIndexCollection_impl();
    }
    mFsIndexCollection.setFsIndexes(aFsIndexes);
  }

  public OperationalProperties getOperationalProperties() {
    return mOperationalProperties;
  }

  public void setOperationalProperties(OperationalProperties aOperationalProperties) {
    mOperationalProperties = aOperationalProperties;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.metadata.impl.MetaDataObject_impl#writePropertyAsElement(org.apache.uima.resource.metadata.impl.PropertyXmlInfo,
   *      java.lang.String, org.xml.sax.ContentHandler)
   */
  @Override
  protected void writePropertyAsElement(PropertyXmlInfo aPropInfo, String aNamespace) throws SAXException {
    // Prevent the fsIndexes property from being written to XML - it exists only so old-style XML
    // can be read.
    if (!"fsIndexes".equals(aPropInfo.propertyName)) {
      super.writePropertyAsElement(aPropInfo, aNamespace);
    }
  }

  protected void readPropertyValueFromXMLElement(PropertyXmlInfo aPropXmlInfo, Element aElement,
          XMLParser aParser, ParsingOptions aOptions) throws InvalidXMLException {
    // Catch the case where both fsIndexes and fsIndexCollection are specified
    if ("fsIndexes".equals(aPropXmlInfo.xmlElementName)
            || "fsIndexCollection".equals(aPropXmlInfo.xmlElementName)) {
      if (mFsIndexCollection != null) {
        throw new InvalidXMLException(InvalidXMLException.FS_INDEXES_OUTSIDE_FS_INDEX_COLLECTION,
                null);
      }
    }
    super.readPropertyValueFromXMLElement(aPropXmlInfo, aElement, aParser, aOptions);
  }

  protected void readUnknownPropertyValueFromXMLElement(Element aElement, XMLParser aParser,
          ParsingOptions aOptions, List<String> aKnownPropertyNames) throws InvalidXMLException {
    // Catch the case where both fsIndexes and fsIndexCollection are specified
    if ("fsIndexes".equals(aElement.getTagName())
            || "fsIndexCollection".equals(aElement.getTagName())) {
      if (mFsIndexCollection != null) {
        throw new InvalidXMLException(InvalidXMLException.FS_INDEXES_OUTSIDE_FS_INDEX_COLLECTION,
                null);
      }
    }
    super.readUnknownPropertyValueFromXMLElement(aElement, aParser, aOptions, aKnownPropertyNames);
  }

  /**
   * @see org.apache.uima.analysis_engine.metadata.AnalysisEngineMetaData#isAsynchronousModeSupported()
   */
  public boolean isAsynchronousModeSupported() {
    return mAsynchronousModeSupported;
  }

  /**
   * @see org.apache.uima.analysis_engine.metadata.AnalysisEngineMetaData#setAsynchronousModeSupported(boolean)
   */
  public void setAsynchronousModeSupported(boolean aSupported) {
    mAsynchronousModeSupported = aSupported;
  }

  /**
   * @see org.apache.uima.analysis_engine.metadata.AnalysisEngineMetaData#getFlowConstraints()
   */
  public FlowConstraints getFlowConstraints() {
    return mFlowConstraints;
  }

  /**
   * @see org.apache.uima.analysis_engine.metadata.AnalysisEngineMetaData#setFlowConstraints(FlowConstraints)
   */
  public void setFlowConstraints(FlowConstraints aFlowConstraints) {
    mFlowConstraints = aFlowConstraints;
  }

  /**
   * @see org.apache.uima.analysis_engine.metadata.AnalysisEngineMetaData#getDelegateAnalysisEngineMetaData()
   */
  public AnalysisEngineMetaData[] getDelegateAnalysisEngineMetaData() {
    return mDelegateAnalysisEngineMetaData;
  }

  /**
   * Used internally to set the AnalysisEngine metadata. Not published through the interface or
   * available to the JavaBeans introspector.
   * 
   * @param aMetaData
   *          metadata for the delegate AnalysisEngines
   */
  public void _setDelegateAnalysisEngineMetaData(AnalysisEngineMetaData[] aMetaData) {
    mDelegateAnalysisEngineMetaData = aMetaData;
  }

  /**
   * Gets whether this AE is sofa-aware. This is a derived property that cannot be set directly. An
   * AE is sofa-aware if and only if it declares at least one input sofa or output sofa.
   * 
   * @return true if this component is sofa-aware, false if it is sofa-unaware.
   */
  public boolean isSofaAware() {
    Capability[] capabilities = getCapabilities();
    if (capabilities != null) {
      for (int i = 0; i < capabilities.length; i++) {
        if (capabilities[i].getInputSofas().length > 0
                || capabilities[i].getOutputSofas().length > 0) {
          return true;
        }
      }
    }
    return false;
  }

  protected XmlizationInfo getXmlizationInfo() {
    return XMLIZATION_INFO;
  }

  /**
   * Static method to get XmlizationInfo, used by subclasses to set up their own XmlizationInfo.
   * @return XmlizationInfo, used by subclasses to set up their own XmlizationInfo.
   */
  protected static XmlizationInfo getXmlizationInfoForClass() {
    return XMLIZATION_INFO;
  }

  static final private XmlizationInfo XMLIZATION_INFO = new XmlizationInfo(
          "analysisEngineMetaData", null); // properties assigned below

  static {
    // this class's Xmlization info is derived from that of its superclass
    // (would derive from ProcessingResourceMetaData_impl but preexisting XML
    // schema requires that flowConstraints come before type system.)
    XmlizationInfo superclassInfo = ResourceMetaData_impl.getXmlizationInfoForClass();

    PropertyXmlInfo[] newProperties = new PropertyXmlInfo[] {
        new PropertyXmlInfo("flowConstraints"), new PropertyXmlInfo("typeSystem", null),
        new PropertyXmlInfo("typePriorities", null),
        new PropertyXmlInfo("fsIndexCollection", null), 
        new PropertyXmlInfo("fsIndexes"),
        new PropertyXmlInfo("capabilities", false),
        new PropertyXmlInfo("operationalProperties", null), 
        new PropertyXmlInfo("casInterface") };

    XMLIZATION_INFO.propertyInfo = new PropertyXmlInfo[superclassInfo.propertyInfo.length
            + newProperties.length];
    System.arraycopy(superclassInfo.propertyInfo, 0, XMLIZATION_INFO.propertyInfo, 0,
            superclassInfo.propertyInfo.length);
    System.arraycopy(newProperties, 0, XMLIZATION_INFO.propertyInfo,
            superclassInfo.propertyInfo.length, newProperties.length);
  }
}
