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

package org.apache.uima.tools.images;

import java.awt.Image;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

/**
 * Names of images and utility methods to get images.
 */
public class Images {
  public static final String BANNER = "/org/apache/uima/tools/images/UIMA_banner2tlp.png";

  public static final String MICROSCOPE = "/org/apache/uima/tools/images/Micro_16.gif";

  public static final String UIMA_LOGO_SMALL = "/org/apache/uima/tools/images/UIMA_logo_50.png";

  public static final String UIMA_LOGO_BIG = "/org/apache/uima/tools/images/UIMA_logo_big.png";

  public static final String ROW_DELETE = "/org/apache/uima/tools/images/RowDelete_24.gif";

  public static final String DOWN = "/org/apache/uima/tools/images/down_24.gif";

  public static final String FORWARD = "/org/apache/uima/tools/images/forward_24.gif";

  public static final String PAUSE = "/org/apache/uima/tools/images/pause_24.gif";

  public static final String PLAY = "/org/apache/uima/tools/images/play_24.gif";

  public static final String SMALL_ARROW = "/org/apache/uima/tools/images/smallarrow_16.gif";

  public static final String STOP = "/org/apache/uima/tools/images/stop_24.gif";

  public static final String TEXT_DOC = "/org/apache/uima/tools/images/text_16.gif";

  public static final String UP = "/org/apache/uima/tools/images/up_24.gif";

  public static final String XML_DOC = "/org/apache/uima/tools/images/xml_16.gif";

  public static Image getImage(String fileName) throws IOException {
    return ImageIO.read(Images.class.getResource(fileName));
  }

  public static ImageIcon getImageIcon(String fileName) {
    return new ImageIcon(Images.class.getResource(fileName));
  }

}
