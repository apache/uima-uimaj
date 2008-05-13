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

package org.apache.uima.tools.viewer;

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import javax.swing.JApplet;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.admin.CASFactory;
import org.apache.uima.cas.admin.CASMgr;
import org.apache.uima.cas.impl.CASCompleteSerializer;
import org.apache.uima.cas.impl.Serialization;
import org.apache.uima.internal.util.SerializationUtils;

/**
 * The CasAnnotationViewer as an Applet.
 * 
 */
public class CasAnnotationViewerApplet extends JApplet {
  private static final long serialVersionUID = 1273090141115008715L;

  /** The CAS Annotation Viewer panel */
  private CasAnnotationViewer mViewer;

  /**
   * Called when the applet is initialized.
   */
  public void init() {
    try {
      // get applet parameter - URL from which to get the CAS
      String casURL = getParameter("CasUrl");

      // open URL connection to get the serialized CAS
      URLConnection con = new URL(casURL).openConnection();

      con.setDoInput(true);
      con.setDoOutput(true);
      con.setUseCaches(false);
      con.setDefaultUseCaches(false);
      con.setRequestProperty("Content-Type", "application/octet-stream");
      // con.connect();

      InputStream in = con.getInputStream();
      ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
      byte[] buf = new byte[2048];
      int bytesRead = in.read(buf);
      while (bytesRead > 0) {
        byteStream.write(buf, 0, bytesRead);
        bytesRead = in.read(buf);
      }
      byte[] bytes = byteStream.toByteArray();
      in.close();
      byteStream.close();
      System.out.println("Got " + bytes.length + " bytes.");

      // deserialize CAS
      CASMgr casMgr = CASFactory.createCAS();
      CASCompleteSerializer serializer = (CASCompleteSerializer) SerializationUtils
              .deserialize(bytes);
      Serialization.deserializeCASComplete(serializer, casMgr);

      // get 2nd applet parameter - right-to-left text orientation
      boolean rightToLeft = false;
      String rightToLeftParam = getParameter("RightToLeftTextOrientation");
      if (rightToLeftParam != null && rightToLeftParam.equalsIgnoreCase("true")) {
        rightToLeft = true;
      }

      // create viewer component and add to this applet
      mViewer = new CasAnnotationViewer();
      // NOTE: it seems to be important to add the viewer to the frame
      // before calling setCAS. If we do it the other way around
      // we seem to frequently cause the browser to hang.
      getContentPane().add(mViewer);

      mViewer.setCAS(casMgr.getCAS().getView(CAS.NAME_DEFAULT_SOFA));
      mViewer.setRightToLeftTextOrientation(rightToLeft);

      // add a listener that detects resize events
      addComponentListener(new MyComponentListener());

      // set initial size of tree viewer panel
      resizeTreeViewer();

    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  /**
   * Sets the size of the tree viewer so that it fits within the applet.
   */
  private void resizeTreeViewer() {
    System.out.println("Resizing viewer");
    Dimension appletSize = getSize();
    Insets insets = getInsets();
    Dimension panelSize = new Dimension(appletSize.width - insets.left - insets.right - 10,
            appletSize.height - insets.top - insets.bottom - 10);

    mViewer.setPreferredSize(panelSize);
    mViewer.setSize(panelSize);
    validate();
    System.out.println("Resize complete");
  }

  /**
   * A listener for detecting resize events. Sets the size of the TreeViewer panel whenever the
   * applet's size changes.
   */
  class MyComponentListener extends ComponentAdapter {
    public void componentResized(ComponentEvent e) {
      resizeTreeViewer();
    }
  }
}
