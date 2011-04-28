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

package org.apache.uima.caseditor;

import org.eclipse.jface.resource.ImageDescriptor;

/**
 * All images in the Cas Editor are referenced here.
 *
 * Call {@link CasEditorPlugin#getTaeImageDescriptor(Images)} to retrieve
 * an actual {@link ImageDescriptor}.
 */
public enum Images {


  /**
   * The source folder image.
   */
  MODEL_PROCESSOR_FOLDER("svgicons/processor.png"),

  /**
   * The wide left side image.
   */
  WIDE_LEFT_SIDE("WideLeftSide.png"),

  /**
   * The lower left side image.
   */
  LOWER_LEFT_SIDE("LowerLeftSide.png"),

  /**
   * The wide right side image.
   */
  WIDE_RIGHT_SIDE("WideRightSide.png"),

  /**
   * The lower right side image.
   */
  LOWER_RIGHT_SIDE("LowerRightSide.png"),

  /**
   * The merge image.
   */
  MERGE("merge.png"),

  /**
   * The add image.
   */
  ADD("svgicons/add.png"),

  PIN("svgicons/pin.png");

  private final String mPath;

  private Images(String path) {
    mPath = path;
  }

  /**
   * Retrieves the Path. The path is a handle for the shared image.
   *
   * @return the id
   */
  String getPath() {
    return mPath;
  }
}