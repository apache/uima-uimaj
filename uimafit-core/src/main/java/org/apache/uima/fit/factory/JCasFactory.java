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

import java.io.IOException;

import org.apache.uima.cas.CASException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;

/**
 * Convenience methods to create {@link JCas} objects.
 */
public final class JCasFactory {
  private JCasFactory() {
    // This class is not meant to be instantiated
  }

  /**
   * Creates a new JCas with the given text. The type system is detected automatically using
   * {@link TypeSystemDescriptionFactory#createTypeSystemDescription()}. Type priorities are
   * detected automatically using {@link TypePrioritiesFactory#createTypePriorities()}. Indexes are
   * detected automatically using {@link FsIndexFactory#createFsIndexCollection()}.
   * 
   * @param aText
   *          the document text to be set in the new JCas.
   * @return a new JCas
   * @throws ResourceInitializationException
   *           if the CAS could not be initialized
   * @throws CASException 
   *           if the JCas could not be initialized
   */
  public static JCas createText(String aText) throws ResourceInitializationException, CASException {
    return CasFactory.createText(aText, null).getJCas();
  }

  /**
   * Creates a new JCas with the given text and language. The type system is detected automatically
   * using {@link TypeSystemDescriptionFactory#createTypeSystemDescription()}. Type priorities are
   * detected automatically using {@link TypePrioritiesFactory#createTypePriorities()}. Indexes are
   * detected automatically using {@link FsIndexFactory#createFsIndexCollection()}.
   * 
   * @param aText
   *          the document text to be set in the new JCas.
   * @param aLanguage 
   *          the document language to be set in the new JCas.
   * @return a new JCas
   * @throws ResourceInitializationException
   *           if the CAS could not be initialized
   * @throws CASException 
   *           if the JCas could not be initialized
   */
  public static JCas createText(String aText, String aLanguage)
          throws ResourceInitializationException, CASException {
    return CasFactory.createText(aText, aLanguage).getJCas();
  }
  
  /**
   * Creates a new {@link JCas}. The type system is detected automatically using
   * {@link TypeSystemDescriptionFactory#createTypeSystemDescription()}. Type priorities are
   * detected automatically using {@link TypePrioritiesFactory#createTypePriorities()}. Indexes are
   * detected automatically using {@link FsIndexFactory#createFsIndexCollection()}.
   * 
   * @return a new JCas
   * @throws ResourceInitializationException
   *           if the CAS could not be initialized
   * @throws CASException 
   *           if the JCas could not be initialized
   */
  public static JCas createJCas() throws ResourceInitializationException, CASException {
    return CasFactory.createCas().getJCas();
  }

  /**
   * Creates a new JCas from type system descriptor files found by name. No auto-detection for type
   * priorities, or indexes is performed.
   * 
   * @param typeSystemDescriptorNames
   *          names of the type system descriptors on the classpath used to initialize the JCas (in
   *          Java notation, e.g. "my.package.TypeSystem" without the ".xml" extension)
   * @return a new JCas
   * @throws ResourceInitializationException
   *           if the CAS could not be initialized
   * @throws CASException 
   *           if the JCas could not be initialized
   */
  public static JCas createJCas(String... typeSystemDescriptorNames)
          throws ResourceInitializationException, CASException {
    return CasFactory.createCas(typeSystemDescriptorNames).getJCas();
  }

  /**
   * Creates a new JCas from type system descriptor files. No auto-detection for type priorities, or
   * indexes is performed.
   * 
   * @param typeSystemDescriptorPaths
   *          paths to type system descriptor files
   * @return a new JCas
   * @throws ResourceInitializationException
   *           if the CAS could not be initialized
   * @throws CASException 
   *           if the JCas could not be initialized
   */
  public static JCas createJCasFromPath(String... typeSystemDescriptorPaths)
          throws ResourceInitializationException, CASException {
    return CasFactory.createCasFromPath(typeSystemDescriptorPaths).getJCas();
  }

  /**
   * Create a new JCas for the given type system description. No auto-detection type priorities, or
   * indexes is performed.
   * 
   * @param typeSystemDescription
   *          a type system description to initialize the JCas
   * @return a new JCas
   * @throws ResourceInitializationException
   *           if the CAS could not be initialized
   * @throws CASException 
   *           if the JCas could not be initialized
   */
  public static JCas createJCas(TypeSystemDescription typeSystemDescription)
          throws ResourceInitializationException, CASException {
    return CasFactory.createCas(typeSystemDescription).getJCas();
  }

  /**
   * This method creates a new JCas and loads the contents of an XMI or XCAS file into it.
   * 
   * @param fileName
   *          a file name for the serialized CAS data
   * @param typeSystemDescription
   *          a type system description to initialize the JCas
   * @return a new JCas
   * @throws ResourceInitializationException
   *           if the CAS could not be initialized
   * @throws CASException 
   *           if the JCas could not be initialized
   * @throws IOException
   *           if there is a problem reading the file
   */
  public static JCas createJCas(String fileName, TypeSystemDescription typeSystemDescription)
          throws ResourceInitializationException, CASException, IOException {
    return CasFactory.createCas(fileName, typeSystemDescription).getJCas();
  }
}
