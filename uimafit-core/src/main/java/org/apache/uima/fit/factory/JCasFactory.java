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

import static org.apache.uima.fit.factory.TypeSystemDescriptionFactory.createTypeSystemDescription;
import static org.apache.uima.fit.factory.TypeSystemDescriptionFactory.createTypeSystemDescriptionFromPath;

import java.io.File;
import java.io.IOException;

import org.apache.uima.UIMAException;
import org.apache.uima.fit.util.CasIOUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;

/**
 * Convenience methods to create {@link JCas} objects.
 */
public final class JCasFactory {
  private JCasFactory() {
    // This class is not meant to be instantiated
  }

  /**
   * Creates a new JCas for the automatically derived type system. See
   * {@link TypeSystemDescriptionFactory#createTypeSystemDescription()}
   * 
   * @return a new JCas
   * @throws UIMAException
   *           if the JCas could not be initialized
   */
  public static JCas createJCas() throws UIMAException {
    return CasCreationUtils.createCas(createTypeSystemDescription(), null, null).getJCas();

  }

  /**
   * Creates a new JCas from type system descriptor files found by name
   * 
   * @param typeSystemDescriptorNames
   *          names of the type system descriptors on the classpath used to initialize the JCas (in
   *          Java notation, e.g. "my.package.TypeSystem" without the ".xml" extension)
   * @return a new JCas
   * @throws UIMAException
   *           if the JCas could not be initialized
   */
  public static JCas createJCas(String... typeSystemDescriptorNames) throws UIMAException {
    return CasCreationUtils.createCas(createTypeSystemDescription(typeSystemDescriptorNames), null,
            null).getJCas();
  }

  /**
   * Creates a new JCas from type system descriptor files.
   * 
   * @param typeSystemDescriptorPaths
   *          paths to type system descriptor files
   * @return a new JCas
   * @throws UIMAException
   *           if the JCas could not be initialized
   */
  public static JCas createJCasFromPath(String... typeSystemDescriptorPaths) throws UIMAException {
    return createJCas(createTypeSystemDescriptionFromPath(typeSystemDescriptorPaths));
  }

  /**
   * Create a new JCas for the given type system description
   * 
   * @param typeSystemDescription
   *          a type system description to initialize the JCas
   * @return a new JCas
   * @throws UIMAException
   *           if the JCas could not be initialized
   */
  public static JCas createJCas(TypeSystemDescription typeSystemDescription) throws UIMAException {
    return CasCreationUtils.createCas(typeSystemDescription, null, null).getJCas();
  }

  /**
   * This method creates a new JCas and loads the contents of an XMI or XCAS file into it.
   * 
   * @param fileName
   *          a file name for the serialized CAS data
   * @param typeSystemDescription
   *          a type system description to initialize the JCas
   * @return a new JCas
   * @throws UIMAException
   *           if the JCas could not be initialized
   * @throws IOException
   *           if there is a problem reading the file
   */
  public static JCas createJCas(String fileName, TypeSystemDescription typeSystemDescription)
          throws UIMAException, IOException {
    JCas jCas = createJCas(typeSystemDescription);
    CasIOUtil.readJCas(jCas, new File(fileName));
    return jCas;
  }
}
