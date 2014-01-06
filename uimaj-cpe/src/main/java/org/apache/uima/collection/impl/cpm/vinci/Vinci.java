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

package org.apache.uima.collection.impl.cpm.vinci;

import java.util.Iterator;

import org.apache.uima.adapter.vinci.util.Constants;
import org.apache.uima.cas_data.CasData;
import org.apache.uima.cas_data.FeatureStructure;
import org.apache.uima.cas_data.FeatureValue;
import org.apache.uima.cas_data.PrimitiveValue;
import org.apache.vinci.transport.BaseClient;
import org.apache.vinci.transport.Transportable;
import org.apache.vinci.transport.TransportableFactory;
import org.apache.vinci.transport.VinciClient;
import org.apache.vinci.transport.VinciFrame;
import org.apache.vinci.transport.document.AFrame;

public class Vinci {
  public static final String VNS_HOST = "VNS_HOST";

  public static final String VNS_PORT = "VNS_PORT";

  public static class AFFactory implements TransportableFactory {
    public Transportable makeTransportable() {
      return new AFrame();
    }
  }

  /**
   * Returns a new VinciFrame
   * 
   * @return {@link org.apache.vinci.transport.VinciFrame} instance
   */
  private AFrame getAFrame() {
    return new AFrame();
  }

  /**
   * Creates and populates an error frame.
   * 
   * @param errorMsg -
   *          error message to place in the error frame
   * 
   * @return {@link org.apache.vinci.transport.VinciFrame} instance containing error
   */
  public VinciFrame replyWithError(String errorMsg) {
    AFrame aFrame = getAFrame();
    aFrame.fadd("Error", errorMsg);
    return aFrame;
  }

  /**
   * Package the {@link org.apache.vinci.transport.VinciFrame} containing result of the requested
   * operation into a Vinci Data frame.
   * 
   * @param conn the connection
   * @param requestFrame {@link org.apache.vinci.transport.VinciFrame}
   *          containing result of thsi service operation 
   * @return {@link org.apache.vinci.transport.VinciFrame} VinciData frame.
   * @throws Exception -
   */
  public static AFrame replyWithAnalysis(BaseClient conn, VinciFrame requestFrame) throws Exception {
    AFFactory af = new AFFactory();
    return (AFrame) conn.sendAndReceive(requestFrame, af);
  }

  /**
   * Package the {@link org.apache.vinci.transport.VinciFrame} containing result of the requested
   * operation into a Vinci Data frame.
   * 
   * @param conn the connection
   * @param requestFrame {@link org.apache.vinci.transport.VinciFrame}
   *          containing result of thsi service operation
   * 
   * @return {@link org.apache.vinci.transport.VinciFrame} VinciData frame.
   */
  public static AFrame replyWithAnalysis(VinciClient conn, VinciFrame requestFrame)
          throws Exception {
    AFFactory af = new AFFactory();
    return (AFrame) conn.sendAndReceive(requestFrame, af);
  }

  public static AFrame produceAFrame(String cmd, String content) {
    AFrame query = new AFrame();
    query.fadd(Constants.VINCI_COMMAND, Constants.ANNOTATE);

    AFrame keys = new AFrame();
    keys.fadd(Constants.VINCI_DETAG, content);

    AFrame data = new AFrame();
    data.fadd(Constants.KEYS, keys);
    query.fadd(Constants.DATA, data);

    return query;
  }

  public static String extractKEYSAsString(AFrame frame) {
    String keys = "";
    if (frame == null)
      return keys;

    String frameAsString = frame.toXML();
    if (frameAsString.indexOf("KEYS") > -1 && frameAsString.indexOf("</KEYS>") > -1) {
      keys = frameAsString.substring(frameAsString.indexOf("KEYS") + 5, frameAsString
              .indexOf("</KEYS>"));
    }
    return keys;
  }

  public static String getFeatureValueByType(CasData aCAS, String featureName) {
    if (aCAS == null) {
      return "";
    }
    Iterator it = aCAS.getFeatureStructures();
    String featureValue = null;
    while (it.hasNext()) {
      FeatureStructure fs = (FeatureStructure) it.next();
      FeatureValue fValue = fs.getFeatureValue(featureName);
      if (fValue != null) {
        featureValue = fValue.toString();
        break;
      }
    }
    return featureValue;
  }

  public static String getContentFromDATACas(CasData aCas) {
    Iterator it = aCas.getFeatureStructures();
    while (it.hasNext()) {
      FeatureStructure fs = (FeatureStructure) it.next();
      if (org.apache.uima.collection.impl.cpm.Constants.CONTENT_TAG.equals(fs.getType())) {
        return ((PrimitiveValue) fs
                .getFeatureValue(org.apache.uima.collection.impl.cpm.Constants.CONTENT_TAG_VALUE))
                .toString();
      }
    }
    return "";
  }

  /**
   * Returns a content from a given VinciFrame.
   * 
   */
  public static String stripVinciFrame(VinciFrame aFrame) {
    String contentFrame = aFrame.toXML();
    int pos = contentFrame.indexOf(">");
    int end = contentFrame.lastIndexOf("</vinci:FRAME>");
    if (pos == -1 || end == -1) {
      return null;
    }
    return contentFrame.substring(pos + 1, end - 1);
  }
}
