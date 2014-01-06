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

package org.apache.uima.collection.impl.cpm.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.naming.ConfigurationException;

import org.apache.uima.UIMAFramework;
import org.apache.uima.adapter.vinci.util.Descriptor;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.collection.impl.cpm.CPMException;
import org.apache.uima.collection.impl.cpm.Constants;
import org.apache.uima.internal.util.JavaTimer;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.util.Level;
import org.apache.uima.util.ProcessTrace;
import org.apache.uima.util.ProcessTraceEvent;
import org.apache.uima.util.UimaTimer;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class CPMUtils {
  public static final String CPM_LOG_RESOURCE_BUNDLE = "org.apache.uima.collection.impl.cpm.cpm_messages";

  private static UimaTimer timer = null;

  /**
   * Currently, this returns initialized array of Strings.
   * 
   * @param aKeyDropMapFile -
   *          a file containing a list of features that should be removed from CAS being sent to Cas
   *          Processor. Currently not used.
   * 
   * @return - Array of empty Strings
   * @throws ResourceConfigurationException -
   */
  public static String[] getKeys2Drop(String aKeyDropMapFile) throws ResourceConfigurationException {
    return new String[] { "", "" };

  }

  /**
   * 
   * @param aTimer
   */
  public static void setTimer(UimaTimer aTimer) {
    timer = aTimer;
  }

  /**
   * 
   * @return the timer
   */
  public static UimaTimer getTimer() {
    return timer;
  }

  /**
   * 
   * @param aSystemVar
   * @param aExpr
   * @param aPathToConvert
   * @return absolute path
   */
  public static String convertToAbsolutePath(String aSystemVar, String aExpr, String aPathToConvert) {
    if (aPathToConvert == null || aSystemVar == null || !aPathToConvert.startsWith(aExpr)) {
      return aPathToConvert;
    }
    return aSystemVar + aPathToConvert.substring(aExpr.length());
  }

  /**
   * Return timer to measure performace of the cpm. The timer can optionally be configured in the
   * CPE descriptor. If none defined, the method returns default timer.
   * 
   * @return - customer timer or JavaTimer (default)
   * 
   * @throws Exception -
   */
  public static UimaTimer getTimer(String aTimerClass) throws Exception {
    if (aTimerClass != null) {
      new TimerFactory(aTimerClass);
      return TimerFactory.getTimer();
    }
    // If not timer defined return default timer based on System.currentTimeMillis()
    return new JavaTimer();
  }

  /**
   * Returns the total duration of a given event
   * 
   * @param aPT -
   *          Event container
   * @param eventName -
   *          name of the event for which the time is needed
   * @return - total duration of an event
   */
  public synchronized static long extractTime(ProcessTrace aPT, String eventName) {
    List aList = aPT.getEvents();
    int counter = 0;
    while (aList != null && aList.size() > 0 && counter < aList.size()) {
      ProcessTraceEvent pte = (ProcessTraceEvent) aList.get(counter++);
      if (pte == null) {
        return 0;
      } else if (eventName == null || eventName.equals(pte.getDescription())) {
        return pte.getDurationExcludingSubEvents();
      } else {
        List subEvents = pte.getSubEvents();
        for (int i = 0; subEvents != null && i < subEvents.size(); i++) {

          if (eventName.equals(((ProcessTraceEvent) subEvents.get(i)).getType())) {
            return ((ProcessTraceEvent) subEvents.get(i)).getDurationExcludingSubEvents();
          }
        }
      }
    }
    return 0;
  }

  /**
   * Dumps all events in the process trace object
   * 
   * @param aPTr -
   *          event container
   */
  public static void dumpEvents(ProcessTrace aPTr) {
    List aList = aPTr.getEvents();
    for (int i = 0; i < aList.size(); i++) {
      ProcessTraceEvent prEvent = (ProcessTraceEvent) aList.get(i);
      String aEvType = prEvent.getType();
      if (System.getProperty("DEBUG_EVENTS") != null) {
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(CPMUtils.class).log(
                  Level.FINEST,
                  "Returning Report With Event::" + aEvType + " For Component:::"
                          + prEvent.getComponentName() + " Duration:::"
                          + prEvent.getDurationExcludingSubEvents());
        }
      }
    }

  }

  /**
   * Finds an occurance of the ##CPM_HOME in a value parameter and returns it with an expanded form
   * (ie.c:/cpm/...) based on the env variable CPM_HOME.
   * 
   */
  public static String scrubThePath(String value) {
    if (value != null && value.indexOf(Constants.CPMPATH) > -1) {
      String rootPath = System.getProperty("CPM_HOME");
      if (rootPath != null) {
        return rootPath + value.substring(Constants.CPMPATH.length());
      }
    }
    return value;

  }

  /**
   * Finds a node with a given path and returns its textual value
   * 
   * @param path
   *          String - XPath path to a node
   * @return textual value of a node indicated in the XPath path
   * 
   * @exception Exception
   */
  private static String extractText(Node aNode) throws Exception {
    String text = null;

    NodeList children = aNode.getChildNodes();

    for (int i = 0; i < children.getLength(); i++) {
      Node achild = children.item(i);
      if (achild.getNodeType() == Node.TEXT_NODE) {
        text = achild.getNodeValue().trim();
      }
    }
    return text;

  }

  /**
   * 
   * @param entityNode
   * @return a configurable feature
   * @throws ConfigurationException -
   */
  private static ConfigurableFeature getConfigurableFeature(Node entityNode)
          throws ConfigurationException // SITHException
  {
    ConfigurableFeature featureStructure = null;
    try {
      String from = null;
      String to = null;
      ArrayList featureList = null;
      NodeList children = entityNode.getChildNodes();
      for (int i = 0; i < children.getLength(); i++) {
        Node achild = children.item(i);
        if (achild.getNodeType() != Node.ELEMENT_NODE) {
          continue;
        }

        if (achild.getNodeName().equals("from")) {
          from = getTextValue(achild.getChildNodes());
        } else if (achild.getNodeName().equals("to")) {
          to = getTextValue(achild.getChildNodes());
        } else if (achild.getNodeName().equals("features")) {
          featureList = getFeatures(achild);
        }
      }
      featureStructure = new ConfigurableFeature(from, to);
      featureStructure.addAttributes(featureList);
    } catch (Exception ex) {
      throw new ConfigurationException(ex.getMessage());
    }
    return featureStructure;
  }

  /**
   * Returns text associated with TEXT_NODE element
   * 
   * @param aList -
   *          list of elements
   * 
   * @return - Text
   */
  private static String getTextValue(NodeList aList) {
    for (int i = 0; i < aList.getLength(); i++) {
      Node achild = aList.item(i);
      if (achild.getNodeType() == Node.TEXT_NODE) {
        return achild.getNodeValue();
      }
    }
    return null;
  }

  /**
   * 
   * @param attributesNode
   * @return a list of features
   * @throws ConfigurationException -
   */
  private static ArrayList getFeatures(Node attributesNode) throws ConfigurationException {
    ArrayList attributeList = new ArrayList();
    try {
      String from = null;
      String to = null;
      NodeList children = attributesNode.getChildNodes();
      for (int i = 0; i < children.getLength(); i++) {
        Node achild = children.item(i);
        if (achild.getNodeType() == Node.ELEMENT_NODE && achild.getNodeName().equals("name")) {
          NodeList atts = achild.getChildNodes();

          for (int j = 0; j < atts.getLength(); j++) {

            Node attribute = atts.item(j);

            if (attribute.getNodeType() != Node.ELEMENT_NODE) {
              continue;
            }
            if (attribute.getNodeName().equals("from")) {
              from = getTextValue(attribute.getChildNodes());
            } else if (attribute.getNodeName().equals("to")) {
              to = getTextValue(attribute.getChildNodes());
            }

          }
          ValuePair value = new ValuePair(from, to);
          attributeList.add(value);
        }

      }
    } catch (Exception ex) {
      throw new ConfigurationException(ex.getMessage());
    }
    return attributeList;

  }

  /**
   * 
   * @param aServiceName
   * @return the deploy directory
   * @throws Exception -
   */
  public static File findDeployDirectory(String aServiceName) throws Exception {
    if (aServiceName == null) {
      throw new Exception(CpmLocalizedMessage.getLocalizedMessage(CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
              "UIMA_CPM_EXP_service_not_defined__WARNING", new Object[] {
                  Thread.currentThread().getName(), "NULL" }));
    }

    File[] dirList = getDirectories();

    for (int i = 0; dirList != null && i < dirList.length; i++) {

      String taeDescriptor = dirList[i].getAbsolutePath() + System.getProperty("file.separator")
              + "bin" + System.getProperty("file.separator") + "desc.xml";
      Descriptor descriptor = null;
      try {
        descriptor = new Descriptor(taeDescriptor);
        if (aServiceName.equals(descriptor.getServiceName().trim())) {
          return dirList[i];
        }

      } catch (Exception e) {
        e.printStackTrace();
        continue;
      }
    }
    return null;
  }

  /**
   * 
   * @return an array of directories
   * @throws Exception -
   */
  private static File[] getDirectories() throws Exception {
    String rootPath = System.getProperty("CPM_HOME");

    String appRoot = "annotators";

    File rootDir = new File(rootPath, appRoot);
    if (rootDir.isDirectory() == false) {
      throw new Exception(CpmLocalizedMessage.getLocalizedMessage(CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
              "UIMA_CPM_EXP_not_directory__WARNING", new Object[] {
                  Thread.currentThread().getName(), appRoot }));
    }
    String[] list = rootDir.list();
    String currentFile;
    Vector dirList = new Vector();
    File aFile = null;
    for (int i = 0; i < list.length; i++) {
      currentFile = list[i];
      aFile = new File(rootDir.getAbsolutePath() + System.getProperty("file.separator")
              + currentFile);
      if (aFile.isDirectory()) {
        dirList.add(aFile);
      }
    }
    File[] dirs = new File[dirList.size()];
    dirList.copyInto(dirs);
    return dirs;

  }

  public static int getFeatureAsInt(CAS aCas, Feature aFeature, String aName) throws Exception {
    Feature seqNo2 = aFeature.getRange().getFeatureByBaseName(aName);
    FeatureStructure documentMetaData = aCas.getView(CAS.NAME_DEFAULT_SOFA).getDocumentAnnotation()
            .getFeatureValue(aFeature);
    return documentMetaData.getIntValue(seqNo2);
  }

  /**
   * Returns a value associated with a given feature
   * 
   * @param aCas -
   *          Cas containing data to extract
   * @param aFeature -
   *          feature to locate in the CAS
   * @param aName -
   *          name of the feature
   * @return - value as String
   */
  public static String getFeatureAsString(CAS aCas, Feature aFeature, String aName)
          throws Exception {
    Feature seqNo2 = aFeature.getRange().getFeatureByBaseName(aName);
    FeatureStructure documentMetaData = aCas.getView(CAS.NAME_DEFAULT_SOFA).getDocumentAnnotation()
            .getFeatureValue(aFeature);
    return documentMetaData.getStringValue(seqNo2);

  }

  /**
   * Extract metadata associated with chunk from a given CAS.
   * 
   * @param aCas -
   *          Cas to extract chunk metadata from
   * @return - chunk metadata
   */
  public static synchronized ChunkMetadata getChunkMetadata(CAS aCas) {
    Feature feat = aCas.getTypeSystem().getFeatureByFullName(
            "uima.tcas.DocumentAnnotation:esDocumentMetaData");
    if (feat != null) {
      try {
        int sequenceNo = getFeatureAsInt(aCas, feat, ChunkMetadata.SEQUENCE); // "sequenceNumber");
        int docId = getFeatureAsInt(aCas, feat, ChunkMetadata.DOCUMENTID); // "documentId");
        int isCompleted = getFeatureAsInt(aCas, feat, ChunkMetadata.ISCOMPLETED); // "isCompleted");
        String throttleID = getFeatureAsString(aCas, feat, ChunkMetadata.THROTTLEID); // "isCompleted");
        String url = getFeatureAsString(aCas, feat, ChunkMetadata.DOCUMENTURL); // "isCompleted");
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(CPMUtils.class).log(
                  Level.FINEST,
                  Thread.currentThread().getName() + "===========================>SeqNo::"
                          + sequenceNo + " docId::" + docId + " isComplete::" + isCompleted
                          + " ThrottleID:" + throttleID + " Document URL:" + url);
        }
        ChunkMetadata cm = new ChunkMetadata(String.valueOf(docId), sequenceNo,
                isCompleted == 1 ? true : false);
        if (throttleID != null && throttleID.trim().length() > 0) {
          cm.setThrottleID(throttleID);
        }
        if (url != null && url.trim().length() > 0) {
          cm.setURL(url);
        }
        return cm;
      } catch (NullPointerException e) {
        if (UIMAFramework.getLogger().isLoggable(Level.WARNING)) {
          Exception newE = new CPMException(
                  "Possible misconfiguration. CPM configured to use chunking but chunk metadata is not present in the CAS. Check if the CAS has been properly initialized by the CollectionReader.");
          UIMAFramework.getLogger(CPMUtils.class).log(Level.WARNING,
                  Thread.currentThread().getName(), newE);
        }
      }

      catch (Exception e) {
        if (UIMAFramework.getLogger().isLoggable(Level.WARNING)) {
          UIMAFramework.getLogger(CPMUtils.class).log(Level.WARNING,
                  Thread.currentThread().getName(), e);
        }
      }
    }
    return null;
  }

}
