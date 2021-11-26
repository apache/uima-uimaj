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
package org.apache.uima.fit.factory;

import static org.apache.uima.fit.factory.FsIndexFactory.createFsIndexCollection;
import static org.apache.uima.fit.factory.TypePrioritiesFactory.createTypePriorities;
import static org.apache.uima.fit.factory.TypeSystemDescriptionFactory.createTypeSystemDescription;
import static org.apache.uima.fit.factory.TypeSystemDescriptionFactory.createTypeSystemDescriptionFromPath;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.uima.cas.CAS;
import org.apache.uima.fit.internal.ResourceManagerFactory;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.metadata.FsIndexCollection;
import org.apache.uima.resource.metadata.TypePriorities;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.CasIOUtils;

/**
 * Convenience methods to create {@link CAS} objects.
 */
public final class CasFactory {
  private CasFactory() {
    // This class is not meant to be instantiated
  }

  /**
   * Creates a new CAS with the given text. The type system is detected automatically using
   * {@link TypeSystemDescriptionFactory#createTypeSystemDescription()}. Type priorities are
   * detected automatically using {@link TypePrioritiesFactory#createTypePriorities()}. Indexes are
   * detected automatically using {@link FsIndexFactory#createFsIndexCollection()}.
   * 
   * @param aText
   *          the document text to be set in the new CAS.
   * @return a new CAS
   * @throws ResourceInitializationException
   *           if the CAS could not be initialized
   */
  public static CAS createText(String aText) throws ResourceInitializationException {
    return createText(aText, null);
  }

  /**
   * Creates a new CAS with the given text and language. The type system is detected automatically
   * using {@link TypeSystemDescriptionFactory#createTypeSystemDescription()}. Type priorities are
   * detected automatically using {@link TypePrioritiesFactory#createTypePriorities()}. Indexes are
   * detected automatically using {@link FsIndexFactory#createFsIndexCollection()}.
   * 
   * @param aText
   *          the document text to be set in the new CAS.
   * @param aLanguage 
   *          the document language to be set in the new CAS.
   * @return a new CAS
   * @throws ResourceInitializationException
   *           if the CAS could not be initialized
   */
  public static CAS createText(String aText, String aLanguage)
          throws ResourceInitializationException {
    CAS cas = createCas();
    if (aText != null) {
      cas.setDocumentText(aText);
    }
    if (aLanguage != null) {
      cas.setDocumentLanguage(aLanguage);
    }
    return cas;
  }
  
  /**
   * Creates a new {@link CAS}. The type system is detected automatically using
   * {@link TypeSystemDescriptionFactory#createTypeSystemDescription()}. Type priorities are
   * detected automatically using {@link TypePrioritiesFactory#createTypePriorities()}. Indexes are
   * detected automatically using {@link FsIndexFactory#createFsIndexCollection()}.
   * 
   * @return a new CAS
   * @throws ResourceInitializationException
   *           if the CAS could not be initialized
   */
  public static CAS createCas() throws ResourceInitializationException {
    TypeSystemDescription tsd = createTypeSystemDescription();
    TypePriorities tp = createTypePriorities();
    FsIndexCollection indexes = createFsIndexCollection();
    ResourceManager resMgr = ResourceManagerFactory.newResourceManager();
    return CasCreationUtils.createCas(tsd, tp, indexes.getFsIndexes(), null, resMgr);
  }

  /**
   * Creates a new {@link CAS} from type system descriptor files found by name. No auto-detection 
   * for type priorities, or indexes is performed.
   * 
   * @param typeSystemDescriptorNames
   *          names of the type system descriptors on the classpath used to initialize the CAS (in
   *          Java notation, e.g. "my.package.TypeSystem" without the ".xml" extension)
   * @return a new CAS
   * @throws ResourceInitializationException
   *           if the CAS could not be initialized
   */
  public static CAS createCas(String... typeSystemDescriptorNames)
          throws ResourceInitializationException {
    return CasCreationUtils.createCas(createTypeSystemDescription(typeSystemDescriptorNames), null,
            null);
  }

  /**
   * Creates a new CAS from type system descriptor files. No auto-detection for type priorities, or
   * indexes is performed.
   * 
   * @param typeSystemDescriptorPaths
   *          paths to type system descriptor files
   * @return a new CAS
   * @throws ResourceInitializationException
   *           if the CAS could not be initialized
   */
  public static CAS createCasFromPath(String... typeSystemDescriptorPaths)
          throws ResourceInitializationException {
    return createCas(createTypeSystemDescriptionFromPath(typeSystemDescriptorPaths));
  }

  /**
   * Create a new CAS for the given type system description. No auto-detection type priorities, or
   * indexes is performed.
   * 
   * @param typeSystemDescription
   *          a type system description to initialize the CAS
   * @return a new CAS
   * @throws ResourceInitializationException
   *           if the CAS could not be initialized
   */
  public static CAS createCas(TypeSystemDescription typeSystemDescription)
          throws ResourceInitializationException {
    return CasCreationUtils.createCas(typeSystemDescription, null, null);
  }

  /**
   * This method creates a new CAS and loads the contents of an XMI or XCAS file into it.
   * 
   * @param fileName
   *          a file name for the serialized CAS data
   * @param typeSystemDescription
   *          a type system description to initialize the CAS
   * @return a new CAS
   * @throws ResourceInitializationException
   *           if the CAS could not be initialized
   * @throws IOException
   *           if there is a problem reading the file
   */
  public static CAS createCas(String fileName, TypeSystemDescription typeSystemDescription)
          throws ResourceInitializationException, IOException {
    CAS cas = createCas(typeSystemDescription);
    try (InputStream is = new FileInputStream(fileName)) {
      CasIOUtils.load(is, cas);
    }
    return cas;
  }
}
