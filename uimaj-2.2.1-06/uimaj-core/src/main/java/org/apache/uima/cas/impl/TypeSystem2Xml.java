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

package org.apache.uima.cas.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.uima.ResourceSpecifierFactory;
import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.resource.metadata.AllowedValue;
import org.apache.uima.resource.metadata.FeatureDescription;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.XMLSerializer;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * Dumps a Type System object to XML.
 */
public class TypeSystem2Xml {
  /**
   * Converts a TypeSystem object to XML
   * 
   * @param aTypeSystem
   *          the TypeSystem to convert
   * @param aOutputStream
   *          the streamt o which XML output will be written
   * 
   * @throws IOException
   *           if there is a problem writing to the provided OutputStream
   * @throws SAXException
   *           if an error occurs during the translation of the type system to XML
   */
  public static void typeSystem2Xml(TypeSystem aTypeSystem, OutputStream aOutputStream)
          throws SAXException, IOException {
    XMLSerializer sax2xml = new XMLSerializer(aOutputStream);
    typeSystem2Xml(aTypeSystem, sax2xml.getContentHandler());
  }

  /**
   * Traverses a TypeSystem and calls SAX events on the specified ContentHandler.
   * 
   * @param aTypeSystem
   *          the TypeSystem to traverse
   * @param aContentHandler
   *          the ContentHandler on which events will be called
   * 
   * @throws SAXException
   *           if an exception is thrown by the ContentHandler
   */
  public static void typeSystem2Xml(TypeSystem aTypeSystem, ContentHandler aContentHandler)
          throws SAXException {
    ResourceSpecifierFactory factory = UIMAFramework.getResourceSpecifierFactory();
    TypeSystemDescription tsDesc = factory.createTypeSystemDescription();

    List typeDescs = new ArrayList();
    Iterator typeIterator = aTypeSystem.getTypeIterator();
    while (typeIterator.hasNext()) {
      Type type = (Type) typeIterator.next();

      Type superType = aTypeSystem.getParent(type);
      if (type.getName().startsWith("uima.cas") && type.isFeatureFinal()) {
        continue; // this indicates a primitive type
      }

      TypeDescription typeDesc = factory.createTypeDescription();
      typeDesc.setName(type.getName());
      typeDesc.setSupertypeName(superType.getName());

      List featDescs = new ArrayList();
      Iterator featIterator = type.getFeatures().iterator();
      while (featIterator.hasNext()) {
        Feature feat = (Feature) featIterator.next();
        FeatureDescription featDesc = factory.createFeatureDescription();
        featDesc.setName(feat.getShortName());
        featDesc.setRangeTypeName(feat.getRange().getName());
        featDescs.add(featDesc);
      }
      FeatureDescription[] featDescArr = new FeatureDescription[featDescs.size()];
      featDescs.toArray(featDescArr);
      typeDesc.setFeatures(featDescArr);

      // check for string subtypes
      if (type instanceof StringTypeImpl) {
	LowLevelTypeSystem lts = aTypeSystem.getLowLevelTypeSystem();
	final int typeCode = lts.ll_getCodeForType(type);
        String[] strings = lts.ll_getStringSet(typeCode);
        AllowedValue[] allowedVals = new AllowedValue[strings.length];
        for (int i = 0; i < strings.length; i++) {
          allowedVals[i] = factory.createAllowedValue();
          allowedVals[i].setString(strings[i]);
        }
        typeDesc.setAllowedValues(allowedVals);
      }

      typeDescs.add(typeDesc);
    }

    TypeDescription[] typeDescArr = new TypeDescription[typeDescs.size()];
    typeDescs.toArray(typeDescArr);
    tsDesc.setTypes(typeDescArr);

    aContentHandler.startDocument();
    tsDesc.toXML(aContentHandler);
    aContentHandler.endDocument();
  }
}
