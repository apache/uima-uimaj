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

package org.apache.uima.impl.analysis_engine.service.vinci.util;

/**
 * 
 * Constants used by the Vinci service
 * 
 * 
 */
public class Constants
{
   


  //default VNS host
  public static final String DEFAULT_VNS_HOST = "localhost";
	
  public static final String VINCI_COMMAND = "vinci:COMMAND";

	public static final String VINCI_DETAG = "Detag:DetagContent";

	public static final String KEYS = "KEYS";

	public static final String DATA = "DATA";
  
    public static final String FRAME_TO_CAS_TIME = "TAE:FrameToCasTime";

    public static final String ANNOTATION_TIME = "TAE:AnnotationTime";

    public static final String CAS_TO_FRAME_TIME = "TAE:CasToFrameTime";

	public static final String GETMETA = "GetMeta";

	public static final String ANNOTATE = "Annotate";

    public static final String SHUTDOWN = "Shutdown";

  public static final String BATCH_PROCESS_COMPLETE = "BatchProcessComplete";

  public static final String COLLECTION_PROCESS_COMPLETE = "CollectionProcessComplete";

  public static final String IS_STATELESS = "IsStateless";

  public static final String IS_READONLY = "IsReadOnly";
    
  public static final String PROCESS_CAS = "ProcessCas";

	public static final String SHUTDOWN_MSG = "Shutting Down the Vinci Analysis Engine Service";

}
