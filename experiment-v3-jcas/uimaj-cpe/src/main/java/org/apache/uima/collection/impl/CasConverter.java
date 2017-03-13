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

package org.apache.uima.collection.impl;

import java.io.IOException;

import org.xml.sax.SAXException;

import org.apache.uima.UIMARuntimeException;
import org.apache.uima.cas.impl.OutOfTypeSystemData;
import org.apache.uima.cas.impl.XCASDeserializer;
import org.apache.uima.cas.impl.XCASSerializer;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas_data.CasData;
import org.apache.uima.cas_data.impl.CasDataImpl;
import org.apache.uima.cas_data.impl.CasDataToXCas;
import org.apache.uima.cas_data.impl.XCasToCasDataSaxHandler;
import org.apache.uima.collection.CollectionException;

/**
 * Converts {@link CasData} to and from Cas Object ({@link CAS}).
 * 
 * 
 */
public class CasConverter {

  private String mDocumentTextTypeName = "uima.cpm.DocumentText";

  private String mDocumentTextFeatureName = "value";

  /**
   * Gets the name of the CASData FeatureStructure Type that stores the document text.
   * 
   * @return the document text type name
   */
  public String getDocumentTextTypeName() {
    return mDocumentTextTypeName;
  }

  /**
   * Sets the name of the CASData FeatureStructure Type that stores the document text.
   * 
   * @param aDocumentTextTypeName the document text type name
   */
  public void setDocumentTextTypeName(String aDocumentTextTypeName) {
    mDocumentTextTypeName = aDocumentTextTypeName;
  }

  /**
   * Gets the name of the CASData Feature that stores the document text.
   * 
   * @return the document text feature name
   */
  public String getDocumentTextFeatureName() {
    return mDocumentTextFeatureName;
  }

  /**
   * Sets the name of the CASData Feature that stores the document text.
   * 
   * @param aDocumentTextFeatureName
   *          the document text feature name
   */
  public void setDocumentTextFeatureName(String aDocumentTextFeatureName) {
    mDocumentTextFeatureName = aDocumentTextFeatureName;
  }

  /**
   * Convert CAS Data to CAS Container (aka CAS Object)
   * 
   * @param aData
   *          CAS Data to convert
   * @param aContainer
   *          CAS to convert into
   * @param aLenient
   *          if true, data that does not fit into CAS type system will be ignored. If false, an
   *          exception will be thrown in that case.
   * 
   * @throws CollectionException
   *           if <code>aLenient</code> is false and a type system incompatibility is found
   */
  public void casDataToCasContainer(CasData aData, CAS aContainer, boolean aLenient)
          throws CollectionException {
    // clear existing contents of container
    aContainer.reset();

    // Generate XCAS events and pipe them to XCASDeserializer
    CasDataToXCas generator = new CasDataToXCas();
    generator.setDocumentTextTypeName(this.getDocumentTextTypeName());
    generator.setDocumentTextFeatureName(this.getDocumentTextFeatureName());
    XCASDeserializer xcasDeser = new XCASDeserializer(aContainer.getTypeSystem());
    xcasDeser.setDocumentTypeName(this.getDocumentTextTypeName());
    // xcasDeser.setDocumentTextFeautre(this.getDocumentTextFeatureName()); NOT NEEDED

    // to be lenient, install OutOfTypeSystemData object to collect data that doesn't
    // fit into target CAS's type system.
    OutOfTypeSystemData ootsd = null;
    if (aLenient) {
      ootsd = new OutOfTypeSystemData();
    }
    generator.setContentHandler(xcasDeser.getXCASHandler(aContainer, ootsd));
    try {
      generator.generateXCas(aData);
    } catch (Exception e) {
      throw new CollectionException(e);
    }
  }

  /**
   * Convert CAS Container (aka CAS Object) to CAS Data
   * 
   * @param aContainer
   *          CAS to convert
   * 
   * @return CasData object containing all information from the CAS
   */
  public CasData casContainerToCasData(CAS aContainer) {
    // generate XCAS events and pipe them to XCasToCasDataSaxHandler
    CasData result = new CasDataImpl();
    XCasToCasDataSaxHandler handler = new XCasToCasDataSaxHandler(result);
    XCASSerializer xcasSer = new XCASSerializer(aContainer.getTypeSystem());
    xcasSer.setDocumentTypeName(this.getDocumentTextTypeName());
    xcasSer.setDocumentTextFeature(this.getDocumentTextFeatureName());
    try {
      xcasSer.serialize(aContainer, handler);
    } catch (IOException e) {
      throw new UIMARuntimeException(e);
    } catch (SAXException e) {
      throw new UIMARuntimeException(e);
    }
    return result;
  }
}
