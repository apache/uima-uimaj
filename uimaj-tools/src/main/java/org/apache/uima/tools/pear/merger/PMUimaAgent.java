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

package org.apache.uima.tools.pear.merger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import org.apache.uima.ResourceSpecifierFactory;
import org.apache.uima.UIMAFramework;
import org.apache.uima.UIMA_IllegalStateException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.TypeOrFeature;
import org.apache.uima.analysis_engine.metadata.AnalysisEngineMetaData;
import org.apache.uima.analysis_engine.metadata.FixedFlow;
import org.apache.uima.collection.CasConsumerDescription;
import org.apache.uima.pear.tools.InstallationDescriptor;
import org.apache.uima.pear.util.StringUtil;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.resource.metadata.Capability;
import org.apache.uima.resource.metadata.Import;
import org.apache.uima.resource.metadata.OperationalProperties;
import org.apache.uima.resource.metadata.ProcessingResourceMetaData;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.apache.uima.util.XMLParser;
import org.apache.uima.util.XMLizable;
import org.xml.sax.SAXException;

/**
 * The <code>PMUimaAgent</code> class implements UIMA-based utility methods utilized by the
 * <code>{@link PMController}</code> class. The class allows generating analysis engine descriptor
 * for output aggregate AE based on the specified input descriptors.
 */

public class PMUimaAgent {
  /**
   * Creates UIMA aggregate analysis engine description object, based on given aggregate component name
   * (ID), aggregate root directory and array of delegate installation descriptors. Returns the UIMA
   * aggregate analysis engine description object. 
   * 
   * @param aggCompName
   *          The given aggregate component name (ID).
   * @param aggRootDir
   *          The given aggregate root directory.
   * @param dlgInstDescs
   *          The given array of delegate installation descriptors.
   * @return The UIMA aggregate analysis engine description object.
   */
  static AnalysisEngineDescription createAggregateDescription(String aggCompName, File aggRootDir,
          InstallationDescriptor[] dlgInstDescs) {
    AnalysisEngineDescription aggDescription = null;
    int lastInputNo = 0;
    try {
      // get UIMA resource specifier factory
      ResourceSpecifierFactory rsFactory = UIMAFramework.getResourceSpecifierFactory();
      // create aggregate AE description
      aggDescription = rsFactory.createAnalysisEngineDescription();
      aggDescription.setPrimitive(false);
      // get Map of delegate specifiers with imports
      Map delegatesMap = aggDescription.getDelegateAnalysisEngineSpecifiersWithImports();
      // add delegate imports to the Map
      for (int i = 0; i < dlgInstDescs.length; i++) {
        // get delegate component attributes
        InstallationDescriptor dlgInsD = dlgInstDescs[i];
        String dlgName = dlgInsD.getMainComponentId();
        String dlgDescPath = dlgInsD.getMainComponentDesc();
        // create Import for delegate component
        Import dlgImport = rsFactory.createImport();
        // set relative delegate descriptor location
        String dlgDescRelPath = dlgDescPath.replaceAll(PMControllerHelper.MAIN_ROOT_REGEX,
                StringUtil.toRegExpReplacement(".."));
        dlgImport.setLocation(dlgDescRelPath);
        // add delegate Import to the Map
        delegatesMap.put(dlgName, dlgImport);
      }
      // get AE metadata
      AnalysisEngineMetaData aggMetadata = aggDescription.getAnalysisEngineMetaData();
      // set AE name and textual description
      aggMetadata.setName(aggCompName);
      aggMetadata.setDescription("Merged aggregate component" + "(" + PMController.PEAR_MERGER
              + ")");
      // set fixed flow constraints
      FixedFlow aggFixedFlow = rsFactory.createFixedFlow();
      String[] aggFlowSpecs = new String[dlgInstDescs.length];
      for (int i = 0; i < dlgInstDescs.length; i++)
        aggFlowSpecs[i] = dlgInstDescs[i].getMainComponentId();
      aggFixedFlow.setFixedFlow(aggFlowSpecs);
      aggMetadata.setFlowConstraints(aggFixedFlow);
      // collect capabilities & check operational props of delegates
      ArrayList allCapabilities = new ArrayList();
      boolean isMultipleDeploymentAllowed = true;
      boolean modifiesCas = false;
      for (int i = 0; i < dlgInstDescs.length; i++) {
        lastInputNo = i + 1;
        ResourceSpecifier dlgSpecifier = retrieveDelegateSpecifier(aggRootDir, dlgInstDescs[i]);
        if (dlgSpecifier instanceof AnalysisEngineDescription) {
          // get AE metadata
          AnalysisEngineMetaData dlgAeMetadata = ((AnalysisEngineDescription) dlgSpecifier)
                  .getAnalysisEngineMetaData();
          // collect AE capabilities
          Capability[] dlgCapabilities = dlgAeMetadata.getCapabilities();
          if (dlgCapabilities != null)
            for (int n = 0; n < dlgCapabilities.length; n++)
              allCapabilities.add(dlgCapabilities[n]);
          // check operational properties
          OperationalProperties dlgOperProps = dlgAeMetadata.getOperationalProperties();
          if (dlgOperProps != null) {
            if (!dlgOperProps.isMultipleDeploymentAllowed())
              isMultipleDeploymentAllowed = false;
            if (dlgOperProps.getModifiesCas())
              modifiesCas = true;
          } else
            // by default, AE modifies CAS
            modifiesCas = true;
        } else if (dlgSpecifier instanceof CasConsumerDescription) {
          // get CC metadata
          ProcessingResourceMetaData dlgCcMetadata = ((CasConsumerDescription) dlgSpecifier)
                  .getCasConsumerMetaData();
          // collect CC capabilities
          Capability[] dlgCapabilities = dlgCcMetadata.getCapabilities();
          if (dlgCapabilities != null)
            for (int n = 0; n < dlgCapabilities.length; n++)
              allCapabilities.add(dlgCapabilities[n]);
          // check operational properties
          OperationalProperties dlgOperProps = dlgCcMetadata.getOperationalProperties();
          if (dlgOperProps != null) {
            if (!dlgOperProps.isMultipleDeploymentAllowed())
              isMultipleDeploymentAllowed = false;
          }
        } else
          // other categories (CR, CI) are not allowed
          throw new IllegalArgumentException("unsupported input component");
      }
      // merge capabilities, excluding duplicates
      Capability[] mergedCapabilities = mergeCapabilities(allCapabilities, rsFactory);
      // set aggregate capabilities
      aggMetadata.setCapabilities(mergedCapabilities);
      // set aggregate operational properties
      OperationalProperties aggOperProps = aggMetadata.getOperationalProperties();
      if (aggOperProps != null) {
        aggOperProps.setMultipleDeploymentAllowed(isMultipleDeploymentAllowed);
        aggOperProps.setModifiesCas(modifiesCas);
      }
    } catch (IllegalArgumentException exc) {
      PMController.logErrorMessage("Invalid input component # " + lastInputNo);
      PMController.logErrorMessage("IllegalArgumentException: " + exc.getMessage());
      aggDescription = null;
    } catch (Throwable err) {
      if (lastInputNo > 0)
        PMController.logErrorMessage("Error in input component # " + lastInputNo);
      PMController.logErrorMessage(err.toString());
      aggDescription = null;
    } finally {
    }
    return aggDescription;
  }

  /**
   * Merges source <code>Capability</code> objects specified in a given 
   * <code>ArrayList</code>, creating one <code>Capability</code> object 
   * that contains all non-duplicated inputs and outputs of the source 
   * <code>Capability</code> objects. Returns an array of 
   * <code>Capability</code> objects, containing the merged object.
   * 
   * @param allCapabilities The given <code>ArrayList</code> of the source 
   * <code>Capability</code> objects.
   * @param rsFactory The <code>ResourceSpecifierFactory</code> object 
   * used to create new <code>Capability</code> object.
   * @return Array of <code>Capability</code> objects, containing the merged 
   * object.
   */
  private static Capability[] mergeCapabilities(ArrayList allCapabilities,
          ResourceSpecifierFactory rsFactory) {
    // collect all the inputs and all the outputs in 2 Hashtables
    Hashtable mergedInputs = new Hashtable();
    Hashtable mergedOutputs = new Hashtable();
    Iterator allList = allCapabilities.iterator();
    while (allList.hasNext()) {
      Capability entry = (Capability) allList.next();
      // get inputs/outputs for this entry
      TypeOrFeature[] entryInps = entry.getInputs();
      TypeOrFeature[] entryOuts = entry.getOutputs();
      // add/merge inputs in Hashtable
      for (int i = 0; i < entryInps.length; i++) {
        TypeOrFeature nextTof = entryInps[i];
        String name = nextTof.getName();
        TypeOrFeature prevTof = (TypeOrFeature) mergedInputs.get(name);
        if (prevTof != null) {
          // choose next or prev, if it's 'type'
          if (prevTof.isType()) {
            // leave more general one
            if (!prevTof.isAllAnnotatorFeatures() && nextTof.isAllAnnotatorFeatures())
              mergedInputs.put(name, nextTof);
          }
        } else
          // add next ToF
          mergedInputs.put(name, nextTof);
      }
      // add/merge outputs in Hashtable
      for (int i = 0; i < entryOuts.length; i++) {
        TypeOrFeature nextTof = entryOuts[i];
        String name = nextTof.getName();
        TypeOrFeature prevTof = (TypeOrFeature) mergedOutputs.get(name);
        if (prevTof != null) {
          // choose next or prev, if it's 'type'
          if (prevTof.isType()) {
            // leave more general one
            if (!prevTof.isAllAnnotatorFeatures() && nextTof.isAllAnnotatorFeatures())
              mergedOutputs.put(name, nextTof);
          }
        } else
          // add next ToF
          mergedOutputs.put(name, nextTof);
      }
    }
    // create merged Capability object and add merged inputs/outputs
    Capability mergedCapability = rsFactory.createCapability();
    // add merged inputs
    Enumeration inpsList = mergedInputs.keys();
    while (inpsList.hasMoreElements()) {
      String name = (String) inpsList.nextElement();
      TypeOrFeature tof = (TypeOrFeature) mergedInputs.get(name);
      if (tof.isType())
        mergedCapability.addInputType(name, tof.isAllAnnotatorFeatures());
      else
        mergedCapability.addInputFeature(name);
    }
    // add merged outputs
    Enumeration outsList = mergedOutputs.keys();
    while (outsList.hasMoreElements()) {
      String name = (String) outsList.nextElement();
      TypeOrFeature tof = (TypeOrFeature) mergedOutputs.get(name);
      if (tof.isType())
        mergedCapability.addOutputType(name, tof.isAllAnnotatorFeatures());
      else
        mergedCapability.addOutputFeature(name);
    }
    // put merged Capability in the array
    Capability[] mergedArray = new Capability[1];
    mergedArray[0] = mergedCapability;
    return mergedArray;
  }

  /**
   * Creates <code>ResourceSpecifier</code> object for a delegate component descriptor, specified
   * by a given <code>InstallationDescriptor</code> object. Returns the delegate component
   * <code>ResourceSpecifier</code> object.
   * 
   * @param aggRootDir
   *          The given aggregate root directory.
   * @param dlgInstDesc
   *          The given delegate <code>InstallationDescriptor</code> object.
   * @return The given delegate component <code>ResourceSpecifier</code> object.
   * @throws IOException
   *           If an I/O exception occurred while creating XML input source.
   * @throws InvalidXMLException
   *           If <code>ResourceSpecifier</code> object cannot be created from the specified
   *           descriptor.
   */
  private static ResourceSpecifier retrieveDelegateSpecifier(File aggRootDir,
          InstallationDescriptor dlgInstDesc) throws IOException, InvalidXMLException {
    // get delegate desciptor path
    String aggRootDirPath = aggRootDir.getAbsolutePath().replace('\\', '/');
    String dlgDescPath = dlgInstDesc.getMainComponentDesc().replaceAll(
            PMControllerHelper.MAIN_ROOT_REGEX, StringUtil.toRegExpReplacement(aggRootDirPath));
    // parse component descriptor
    XMLInputSource xmlSource = null;
    ResourceSpecifier dlgSpecifier = null;
    try {
      xmlSource = new XMLInputSource(dlgDescPath);
      XMLParser xmlParser = UIMAFramework.getXMLParser();
      dlgSpecifier = xmlParser.parseResourceSpecifier(xmlSource);
    } catch (InvalidXMLException xmlExc) {
      String msgKey = xmlExc.getMessageKey();
      // if msg key is INVALID_CLASS, this is TS or RR desc
      if (InvalidXMLException.INVALID_CLASS.equals(msgKey))
        throw new IllegalArgumentException(xmlExc.toString());
      else
        // otherwise, XML descriptor is not valid
        throw xmlExc;
    } catch (UIMA_IllegalStateException urtExc) {
      String msgKey = urtExc.getMessageKey();
      // if msg key is COULD_NOT_INSTANTIATE_XMLIZABLE, this is CPE desc
      if (UIMA_IllegalStateException.COULD_NOT_INSTANTIATE_XMLIZABLE.equals(msgKey))
        throw new IllegalArgumentException(urtExc.toString());
      else
        // otherwise, something is wrong
        throw urtExc;
    } finally {
      if (xmlSource != null) {
        try {
          xmlSource.getInputStream().close();
        } catch (Exception e) {
        }
      }
    }
    return dlgSpecifier;
  }

  /**
   * Saves a given UIMA aggregate component desciption in a specified XML descriptor file.
   * 
   * @param aggDescription
   *          The given UIMA aggregate component desciption.
   * @param aggDescFile
   *          The given XML descriptor file.
   * @throws IOException
   *           If an I/O exception occurrs.
   */
  static void saveAggregateDescription(AnalysisEngineDescription aggDescription, File aggDescFile)
          throws IOException {
    FileWriter fWriter = null;
    try {
      fWriter = new FileWriter(aggDescFile);
      aggDescription.toXML(fWriter);
    } catch (SAXException exc) {
      throw new IOException(exc.toString());
    } finally {
      if (fWriter != null) {
        try {
          fWriter.close();
        } catch (Exception e) {
        }
      }
    }
  }

  /**
   * Converts a given <code>XMLizable</code> object to String. This method is useful for
   * debugging.
   * 
   * @param content
   *          The given <code>XMLizable</code> object
   * @return A String that represents the given <code>XMLizable</code> object.
   */
  static String toXmlString(XMLizable content) {
    StringWriter sWriter = new StringWriter();
    PrintWriter oWriter = null;
    try {
      oWriter = new PrintWriter(sWriter);
      content.toXML(oWriter);
      oWriter.flush();
    } catch (Exception exc) {
    } finally {
      if (oWriter != null) {
        try {
          oWriter.close();
        } catch (Exception e) {
        }
      }
    }
    return sWriter.toString();
  }
}
