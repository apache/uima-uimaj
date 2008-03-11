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

package org.apache.uima.flow.impl;

import java.util.HashMap;
import java.util.HashSet;

import org.apache.uima.analysis_engine.TypeOrFeature;
import org.apache.uima.resource.metadata.Capability;

/**
 * The CapabilityContainer maps the capabilites of an AnalysisEngine to hash maps for a quick search
 * of supported ToFs or languages.
 * 
 */
public class CapabilityContainer {
  // capability source
  private Capability[] capabilities;

  // capability hash maps
  private HashMap outputLanguageCapability;

  private HashMap outputToFCapability;

  private HashMap inputLanguageCapability;

  private HashMap inputToFCapability;

  // main language separator e.g 'en' and 'en-US'
  private static final char LANGUAGE_SEPARATOR = '-';

  private static final String UNSPECIFIED_LANGUAGE = "x-unspecified";

  /**
   * constructor create the HashMaps for all capabilities
   * 
   * @param capabilities
   *          Annotator capabilities
   */
  public CapabilityContainer(Capability[] capabilities) {
    // set capability array
    this.capabilities = capabilities;

    // create hashmap for output capabilities - language key
    outputLanguageCapability = createLanguageCapabilities(true);

    // create hashmap for output capabilities - ToF key
    outputToFCapability = createToFCapabilities(true);

    // create hashmap for input capabilities - language key
    inputLanguageCapability = createLanguageCapabilities(false);

    // create hashmap for input capabilities - ToF key
    inputToFCapability = createToFCapabilities(false);
  }

  /**
   * constructor create for the given state the capabilities
   * 
   * @param capabilities
   *          Annotator capabilities
   * @param input
   *          create input capability hash map
   * @param output
   *          create output capability hash map
   */
  public CapabilityContainer(Capability[] capabilities, boolean input, boolean output) {
    // set capability array
    this.capabilities = capabilities;

    // init class members
    outputLanguageCapability = null;
    outputToFCapability = null;
    inputLanguageCapability = null;
    inputToFCapability = null;

    // compile output capabilities
    if (output == true) {
      // create hashmap for output capabilities - language key
      outputLanguageCapability = createLanguageCapabilities(true);

      // create hashmap for output capabilities - ToF key
      outputToFCapability = createToFCapabilities(true);
    }

    // compile input capabilities
    if (input == true) {
      // create hashmap for input capabilities - language key
      inputLanguageCapability = createLanguageCapabilities(false);

      // create hashmap for input capabilities - ToF key
      inputToFCapability = createToFCapabilities(false);
    }
  }

  /**
   * methode compileOutputCapability() creates the output capability hash maps
   */
  public void compileOutputCapabilitiy() {
    // create hashmap for output capabilities - language key
    outputLanguageCapability = createLanguageCapabilities(true);

    // create hashmap for output capabilities - ToF key
    outputToFCapability = createToFCapabilities(true);
  }

  /**
   * methode compileInputCapability() creates the input capability hash maps
   */
  public void compileInputCapabilitiy() {
    // create hashmap for output capabilities - language key
    inputLanguageCapability = createLanguageCapabilities(true);

    // create hashmap for output capabilities - ToF key
    inputToFCapability = createToFCapabilities(true);
  }

  /**
   * method hasOutputTypeOrFeature() returns true if the given tof is supported by this annotator
   * for the given language
   * 
   * @param tof
   *          ToF to search for
   * @param language
   *          current document language
   * @param fuzzySearch
   *          do loose search also (e.g. language: 'en-US' -> search for 'en-US' and 'en')
   * @return - true if the annotator support this tof for the current language
   */
  public boolean hasOutputTypeOrFeature(TypeOrFeature tof, String language, boolean fuzzySearch) {
    boolean found = false;

    if (outputLanguageCapability == null) {
      return false;
    }

    // extract ToFs for the current language
    HashSet languageToFs = (HashSet) outputLanguageCapability.get(language);
    HashSet unspecifiedLanguageToFs = (HashSet) outputLanguageCapability.get(UNSPECIFIED_LANGUAGE);
    HashSet tofs = null;
    if (languageToFs != null || unspecifiedLanguageToFs != null) {
      tofs = new HashSet();

      if (languageToFs != null)
        tofs.addAll(languageToFs);

      if (unspecifiedLanguageToFs != null)
        tofs.addAll(unspecifiedLanguageToFs);
    }
    if (tofs != null) {
      // check if tof is supported for the current language
      if (tofs.contains(tof.getName())) {
        // mark item found
        found = true;
      }
    }

    // do fuzzy search if nothing was found
    if (found == false && fuzzySearch == true) {
      // search first part of the language
      int index = language.indexOf(LANGUAGE_SEPARATOR);

      // check if search was successful
      if (index != -1) {
        // search with new main language
        tofs = (HashSet) outputLanguageCapability.get(language.substring(0, index));
        if (tofs != null) {
          // check if tof is supported for the current language
          if (tofs.contains(tof.getName())) {
            // mark item found
            found = true;
          }
        }
      }
    }

    // in all other cases return false
    return found;
  }

  /**
   * method hasOutputTypeOrFeature() returns true if the given tof is supported for this annotator
   * 
   * @param tof
   *          ToF to search for
   * @return - true if the annotator supports this tof
   */
  public boolean hasOutputTypeOrFeature(TypeOrFeature tof) {
    if (outputToFCapability != null) {
      // check if tof is supported
      if (outputToFCapability.containsKey(tof.getName())) {
        return true;
      }
    }

    // in all other cases return false
    return false;
  }

  /**
   * method hasInputTypeOrFeature() returns true if the given tof is preconditioned for this
   * annotator for the given language
   * 
   * @param tof
   *          ToF to search for
   * @param language
   *          current document language
   * @param fuzzySearch
   *          do loose search also (e.g. language: 'en-US' -> search for 'en-US' and 'en')
   * @return - true if the annotator preconditiones this tof for the current language
   */
  public boolean hasInputTypeOrFeature(TypeOrFeature tof, String language, boolean fuzzySearch) {
    boolean found = false;

    if (inputLanguageCapability == null) {
      return false;
    }

    // extract ToFs for the current language
    HashSet languageToFs = (HashSet) inputLanguageCapability.get(language);
    HashSet unspecifiedLanguageToFs = (HashSet) inputLanguageCapability.get(UNSPECIFIED_LANGUAGE);
    HashSet tofs = null;
    if (languageToFs != null || unspecifiedLanguageToFs != null) {
      tofs = new HashSet();

      if (languageToFs != null)
        tofs.addAll(languageToFs);

      if (unspecifiedLanguageToFs != null)
        tofs.addAll(unspecifiedLanguageToFs);
    }
    if (tofs != null) {
      // check if tof is supported for the current language
      if (tofs.contains(tof.getName())) {
        // mark item found
        found = true;
      }
    }

    // do fuzzy search if nothing was found
    if (found == false && fuzzySearch == true) {
      // search first part of the language
      int index = language.indexOf(LANGUAGE_SEPARATOR);

      // check if search was successful
      if (index != -1) {
        // search with new main language
        tofs = (HashSet) outputLanguageCapability.get(language.substring(0, index));

        if (tofs != null) {
          // check if tof is supported for the current language
          if (tofs.contains(tof.getName())) {
            // mark item found
            found = true;
          }
        }
      }
    }

    // in all other cases return false
    return found;
  }

  /**
   * method hasInputTypeOrFeature() returns true if the given tof is preconditioned for this
   * annotator
   * 
   * @param tof
   *          ToF to search for
   * @return - true if the annotator preconditiones this tof
   */
  public boolean hasInputTypeOrFeature(TypeOrFeature tof) {
    if (inputToFCapability != null) {
      // check if tof is supported
      if (inputToFCapability.containsKey(tof.getName())) {
        return true;
      }
    }

    // in all other cases return false
    return false;
  }

  /**
   * method createToFCapabilities() create a HashMap which includes all ToFs with their assigned
   * languages
   * 
   * @param outputCapabilities -
   *          true to create ouput capabilities, false to create input capabilities
   * @return HashMap - HashMap includes all in- or output capabilities with their assigned languages
   */
  private HashMap createToFCapabilities(boolean outputCapabilities) {
    // check if capabilities are set
    if (capabilities == null) {
      return null;
    }

    // create new HashMap
    HashMap tofCapability = new HashMap();

    for (int i = 0; i < this.capabilities.length; i++) {
      // check if capability is set and is not null
      if (capabilities[i] == null) {
        continue; // go on with the next if capability is null
      }

      // get supported languages
      String[] supportedLanguagesArr = this.capabilities[i].getLanguagesSupported();

      HashSet supportedLanguages = null;
      if (supportedLanguagesArr != null) {
        // create new HashSet with the initial capacity of the supportedLanguageArr
        supportedLanguages = new HashSet(supportedLanguagesArr.length);

        // add all supported languages to at HashSet
        for (int x = 0; x < supportedLanguagesArr.length; x++) {
          // add current language to the HashSet
          supportedLanguages.add(supportedLanguagesArr[x]);
        }

        // get ToFs
        TypeOrFeature[] tofArr = null;
        if (outputCapabilities == true) {
          // get ouput ToFs
          tofArr = this.capabilities[i].getOutputs();
        } else {
          // get input ToFs
          tofArr = this.capabilities[i].getInputs();
        }
        if (tofArr != null) {
          // add all ToFs to at HashMap
          for (int y = 0; y < tofArr.length; y++) {
            if (tofArr[y] != null) {
              // add current ToF with their assigned languages to the HashMap
              tofCapability.put(tofArr[y].getName(), supportedLanguages);
            }
          }
        }
      }
    }
    // return HashMap
    return tofCapability;
  }

  /**
   * method createLanguageCapabilities() create a HashMap which includes all languages supported by
   * this annotator with their assigned ToFs
   * 
   * @param outputCapabilities -
   *          true to create ouput capabilities, false to create input capabilities
   * @return HashMap - HashMap includes languages supported by this annotator with their assigned
   *         ToFs
   */
  private HashMap createLanguageCapabilities(boolean outputCapabilities) {
    // check if capability are set
    if (this.capabilities == null) {
      return null;
    }

    // create new HashMap
    HashMap languageCapability = new HashMap();

    for (int i = 0; i < this.capabilities.length; i++) {
      // check if capability is set
      if (capabilities[i] == null) {
        continue; // go on with the next if capability is null
      }

      // get supported languages
      String[] supportedLanguagesArr = this.capabilities[i].getLanguagesSupported();

      // get ToFs
      TypeOrFeature[] tofArr;
      if (outputCapabilities == true) {
        // get ouput ToFs
        tofArr = this.capabilities[i].getOutputs();
      } else {
        // get input ToFs
        tofArr = this.capabilities[i].getInputs();
      }

      if (supportedLanguagesArr != null && tofArr != null) {
        // loop over all languages and add language with their assigned ToFs to the HashMap
        for (int x = 0; x < supportedLanguagesArr.length; x++) {
          HashSet tofs = null;

          // check if current language is already in the HashMap
          if (languageCapability.containsKey(supportedLanguagesArr[x])) {
            // get the value of the current language
            tofs = (HashSet) languageCapability.get(supportedLanguagesArr[x]);
          } else // add language to HashMap
          {
            // create new HashSet with the initial capacity of the tofArr
            tofs = new HashSet(tofArr.length);
          }

          if (tofs != null) {
            // add to the given HashSet all current ToFs
            for (int y = 0; y < tofArr.length; y++) {
              if (tofArr[y] != null) {
                // add to the current HashSet all ToFs
                tofs.add(tofArr[y].getName());
              }
            }
          }

          // set new HashSet as value for the current language key in the HashMap
          languageCapability.put(supportedLanguagesArr[x], tofs);
        } // end for-loop
      }
    }
    // return HashMap
    return languageCapability;
  }
}
