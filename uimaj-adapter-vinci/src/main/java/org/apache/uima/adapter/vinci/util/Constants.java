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

package org.apache.uima.adapter.vinci.util;

// TODO: Auto-generated Javadoc
/**
 * Constants used by the Vinci service.
 */
public class Constants {

  /** The Constant DEFAULT_VNS_HOST. */
  // default VNS host
  public static final String DEFAULT_VNS_HOST = "localhost";

  /** The Constant VINCI_COMMAND. */
  public static final String VINCI_COMMAND = "vinci:COMMAND";

  /** The Constant VINCI_DETAG. */
  public static final String VINCI_DETAG = "Detag:DetagContent";

  /** The Constant KEYS. */
  public static final String KEYS = "KEYS";

  /** The Constant DATA. */
  public static final String DATA = "DATA";

  /** The Constant FRAME_TO_CAS_TIME. */
  public static final String FRAME_TO_CAS_TIME = "TAE:FrameToCasTime";

  /** The Constant ANNOTATION_TIME. */
  public static final String ANNOTATION_TIME = "TAE:AnnotationTime";

  /** The Constant CAS_TO_FRAME_TIME. */
  public static final String CAS_TO_FRAME_TIME = "TAE:CasToFrameTime";

  /** The Constant GETMETA. */
  public static final String GETMETA = "GetMeta";

  /** The Constant ANNOTATE. */
  public static final String ANNOTATE = "Annotate";

  /** The Constant SHUTDOWN. */
  public static final String SHUTDOWN = "Shutdown";

  /** The Constant BATCH_PROCESS_COMPLETE. */
  public static final String BATCH_PROCESS_COMPLETE = "BatchProcessComplete";

  /** The Constant COLLECTION_PROCESS_COMPLETE. */
  public static final String COLLECTION_PROCESS_COMPLETE = "CollectionProcessComplete";

  /** The Constant IS_STATELESS. */
  public static final String IS_STATELESS = "IsStateless";

  /** The Constant IS_READONLY. */
  public static final String IS_READONLY = "IsReadOnly";

  /** The Constant PROCESS_CAS. */
  public static final String PROCESS_CAS = "ProcessCas";

  /** The Constant SHUTDOWN_MSG. */
  public static final String SHUTDOWN_MSG = "Shutting Down the Vinci Analysis Engine Service";

  /** The Constant GET_SUPPORTED_XCAS_VERSIONS. */
  public static final String GET_SUPPORTED_XCAS_VERSIONS = "GetSupportedXCasVersions";
  
  /** The Constant SUPPORTED_XCAS_VERSIONS_RESPONSE. */
  public static final String SUPPORTED_XCAS_VERSIONS_RESPONSE = "2";
}
