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

package org.apache.uima.util;

import static java.lang.Character.isDigit;
import static java.lang.Character.isLetter;
import static org.apache.uima.UIMAFramework.getLogger;
import static org.apache.uima.cas.CAS.TYPE_NAME_ANNOTATION;
import static org.apache.uima.cas.CAS.TYPE_NAME_FS_ARRAY;
import static org.apache.uima.cas.CAS.TYPE_NAME_STRING;
import static org.apache.uima.cas.CAS.TYPE_NAME_TOP;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.resource.impl.ResourceManager_impl;
import org.apache.uima.resource.metadata.AllowedValue;
import org.apache.uima.resource.metadata.FeatureDescription;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.spi.TypeSystemDescriptionProvider;

public class TypeSystemUtil {

  private static final String NAMESPACE_SEPARATOR_AS_STRING = "" + TypeSystem.NAMESPACE_SEPARATOR;

  /**
   * Loads type system descriptions and resolves their imports. For example when you place a
   * {@link TypeSystemDescriptionProvider} implementation and place the type system descriptions it
   * should provide in the same package, you can use this method to conveniently load them simply by
   * name in the provider implementation.
   * 
   * <pre>
   * public class MyTypeSystemDescriptionProvider implements TypeSystemDescriptionProvider {
   *   {@code @Override}
   *   {@code public List<TypeSystemDescription> listTypeSystemDescriptions()} {
   *     return TypeSystemUtil.loadTypeSystemDescriptionsFromClasspath(getClass(), "TypeSystem1.xml",
   *             "TypeSystem2.xml");
   *   }
   * }
   * </pre>
   * 
   * @param aContext
   *          a context class. If the locations are not absolute, then they are looked up relative
   *          to this context class as per {@link Class#getResource(String)}.
   * @param typeSystemDescriptionLocations
   *          type system description locations to load.
   * @return list of the loaded and resolved descriptions.
   */
  public static List<TypeSystemDescription> loadTypeSystemDescriptionsFromClasspath(
          Class<?> aContext, String... typeSystemDescriptionLocations) {

    var resMgr = new ResourceManager_impl(aContext.getClassLoader());
    try {
      var typeSystemDescriptions = new ArrayList<TypeSystemDescription>();

      for (var typeSystem : typeSystemDescriptionLocations) {
        var resource = aContext.getResource(typeSystem);
        if (resource == null) {
          getLogger().error("Unable to locate type system description as a resource [{}]",
                  typeSystem);
          continue;
        }

        try {
          var tsd = UIMAFramework.getXMLParser()
                  .parseTypeSystemDescription(new XMLInputSource(resource));
          tsd.resolveImports(resMgr);
          typeSystemDescriptions.add(tsd);
        } catch (InvalidXMLException | IOException e) {
          getLogger().error("Error loading type system description [{}] from [{}]", typeSystem,
                  resource, e);
        }
      }

      return typeSystemDescriptions;
    } finally {
      resMgr.destroy();
    }
  }

  /**
   * Convert a {@link TypeSystem} to an equivalent {@link TypeSystemDescription}.
   * 
   * @param aTypeSystem
   *          type system object to convert
   * @return a TypeSystemDescription that is equivalent to <code>aTypeSystem</code>
   */
  public static TypeSystemDescription typeSystem2TypeSystemDescription(TypeSystem aTypeSystem) {
    var fact = UIMAFramework.getResourceSpecifierFactory();
    var tsDesc = fact.createTypeSystemDescription();
    var typeIter = aTypeSystem.getTypeIterator();
    var typeDescs = new ArrayList<TypeDescription>();
    while (typeIter.hasNext()) {
      var type = typeIter.next();
      if (!type.getName().startsWith("uima.cas") && !type.getName().equals(TYPE_NAME_ANNOTATION)
              && !type.isArray()) {
        typeDescs.add(type2TypeDescription(type, aTypeSystem));
      }
    }
    var typeDescArr = new TypeDescription[typeDescs.size()];
    typeDescs.toArray(typeDescArr);
    tsDesc.setTypes(typeDescArr);

    return tsDesc;
  }

  /**
   * Convert a {@link Type} to an equivalent {@link TypeDescription}.
   * 
   * @param aType
   *          type object to convert
   * @param aTypeSystem
   *          the TypeSystem that contains <code>aType</code>
   * @return a TypeDescription that is equivalent to <code>aType</code>
   */
  public static TypeDescription type2TypeDescription(Type aType, TypeSystem aTypeSystem) {
    var typeDesc = UIMAFramework.getResourceSpecifierFactory().createTypeDescription();
    typeDesc.setName(aType.getName());
    var superType = aTypeSystem.getParent(aType);
    typeDesc.setSupertypeName(superType.getName());
    // special handling for string subtypes (which have "allowed values", rather than features)
    var stringType = aTypeSystem.getType(TYPE_NAME_STRING);
    if (aTypeSystem.subsumes(stringType, aType)) {
      var allowedValues = getAllowedValuesForType(aType, aTypeSystem);
      var avObjs = new AllowedValue[allowedValues.length];
      for (int i = 0; i < allowedValues.length; i++) {
        var av = UIMAFramework.getResourceSpecifierFactory().createAllowedValue();
        av.setString(allowedValues[i]);
        avObjs[i] = av;
      }
      typeDesc.setAllowedValues(avObjs);
    } else {
      var featDescs = new ArrayList<FeatureDescription>();
      for (var feat : aType.getFeatures()) {
        if (!superType.getFeatures().contains(feat)) {
          featDescs.add(feature2FeatureDescription(feat));
        }
      }
      var featDescArr = new FeatureDescription[featDescs.size()];
      featDescs.toArray(featDescArr);
      typeDesc.setFeatures(featDescArr);
    }
    return typeDesc;
  }

  /**
   * Convert a {@link Feature} to an equivalent {@link FeatureDescription}.
   * 
   * @param aFeature
   *          feature object to convert
   * @return a FeatureDescription that is equivalent to <code>aFeature</code>
   */
  public static FeatureDescription feature2FeatureDescription(Feature aFeature) {
    var featDesc = UIMAFramework.getResourceSpecifierFactory().createFeatureDescription();
    featDesc.setName(aFeature.getShortName());
    if (aFeature.isMultipleReferencesAllowed()) {
      featDesc.setMultipleReferencesAllowed(true);
    }
    var rangeType = aFeature.getRange();
    // special check for array range types, which are represented in the CAS as
    // elementType[] but in the descriptor as an FSArray with an <elementType>
    if (rangeType.isArray() && !rangeType.getComponentType().isPrimitive()) {
      featDesc.setRangeTypeName(TYPE_NAME_FS_ARRAY);
      var elementTypeName = rangeType.getComponentType().getName();
      if (!TYPE_NAME_TOP.equals(elementTypeName)) {
        featDesc.setElementType(elementTypeName);
      }
    } else {
      featDesc.setRangeTypeName(rangeType.getName());
    }
    return featDesc;
  }

  /**
   * Gets the allowed values for a string subtype.
   * 
   * @param aType
   *          the type, which must be a subtype of uima.cas.String
   * @param aTypeSystem
   *          the type system to use
   * @return array of allowed values for <code>aType</code> TODO - this should be a method on Type.
   */
  public static String[] getAllowedValuesForType(Type aType, TypeSystem aTypeSystem) {
    var lts = aTypeSystem.getLowLevelTypeSystem();
    return lts.ll_getStringSet(lts.ll_getCodeForType(aType));
  }

  /**
   * @return if the given {@code name} is a valid feature name. Does not check if the feature
   *         actually exists!
   * @param name
   *          The name to check.
   */
  public static boolean isFeatureName(String name) {

    return isIdentifier(name);
  }

  /**
   * Check if {@code name} is a possible type name. Does not check if this type actually exists!
   * 
   * @param name
   *          The name to check.
   * @return <code>true</code> iff <code>name</code> is a possible type name.
   */
  public static boolean isTypeName(String name) {
    // Create a string tokenizer that will split the string at the name space
    // boundaries. We need to see the delimiters to make sure there are no
    // gratuitous delimiters at the beginning or the end.
    var tok = new StringTokenizer(name, NAMESPACE_SEPARATOR_AS_STRING, true);
    // Loop over the tokens and check that every item is an identifier.
    while (tok.hasMoreTokens()) {
      // Any subsequence must start with an identifier.
      if (!isIdentifier(tok.nextToken())) {
        return false;
      }
      // If there is a next token, it must be a separator.
      if (tok.hasMoreTokens()) {
        // A sequence can not end in a separator.
        if (!tok.nextToken().equals(NAMESPACE_SEPARATOR_AS_STRING) || !tok.hasMoreTokens()) {
          return false;
        }
      }
    }
    return true;
  }

  private static boolean isIdentifier(String s) {
    if (s == null) {
      return false;
    }
    final int len = s.length();
    if (len == 0) {
      return false;
    }
    int pos = 0;
    // Check that the first character is a letter.
    if (!isIdentifierStart(s.charAt(pos))) {
      return false;
    }
    ++pos;
    while (pos < len) {
      if (!isIdentifierChar(s.charAt(pos))) {
        return false;
      }
      ++pos;
    }
    return true;
  }

  private static boolean isIdentifierStart(char c) {
    return isLetter(c);
  }

  private static boolean isIdentifierChar(char c) {
    return (isLetter(c) || isDigit(c) || (c == '_'));
  }
}
