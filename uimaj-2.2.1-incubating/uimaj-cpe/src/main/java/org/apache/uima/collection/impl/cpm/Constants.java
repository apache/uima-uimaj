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

package org.apache.uima.collection.impl.cpm;

/**
 * CPE Constants
 * 
 * 
 */

public class Constants {

  public static final String LONG_COLON_TERM = "_colon_";

  public static final String SHORT_COLON_TERM = ":";

  public static final String LONG_DASH_TERM = "_dash_";

  public static final String SHORT_DASH_TERM = "-";

  public static final String DOCUMENT_READ = "Document.Read";

  public static final String DOCUMENT_STORE = "Document.Store";

  public static final String DOCUMENT_ANALYSIS = "Document.Analysis";

  public static final String WFEnumReader = "WFEnumerationReader";

  public static final String FEATURE_COUNT = "FeatureCount";

  public static final String STAT_FEATURE = "Analysis:PerformanceStats";

  public static final String CPMPATH = "##CPM_HOME";

  public static final String LAST_DOC_ID = "LAST_DOC_ID";

  public static final String LAST_REPOSITORY = "LAST_REPOSITORY";

  public static final String COMPLETED = "COMPLETED";

  public static final String HALTED = "HALTED";

  public static final String KILLED = "KILLED";

  public static final String UNKNOWN = "UNKNOWN";

  public static final String RUNNING = "RUNNING";

  public static final String DISABLED = "DISABLED";

  public static final String READY = "READY";

  public static final String RETRY_COUNT = "retryCount";

  public static final String SAMPLE_SIZE = "sampleSize";

  public static final String ERROR_RATE = "errorRate";

  public static final String ACTION_ON_MAX_ERRORS = "actionOnMaxErrors";

  public static final String ACTION_ON_MAX_RESTARTS = "actionOnMaxRestarts";

  public static final String MAX_RESTARTS = "maxRestarts";

  public static final String TIMEOUT = "timeout";

  public static final String SERVICE_ADAPTER = "serviceAdapter";

  public static final String TAE_DESCRIPTOR = "taeDescriptor";

  public static final String ENV_SETTINGS = "envSettings";

  public static final String PROGRAM = "program";

  public static final String JAVA = "java";

  public static final String PROGRAM_FLAGS = "programFlags";

  public static final String FILTER = "filter";

  public static final String SERVICE_NAME = "serviceName";

  public static final String VNS_HOST = "vnsHost";

  public static final String VNS_PORT = "vnsPort";

  public static final String VNS_START_PORT = "vnsServiceStartPort";

  public static final String VNS_MAX_PORT = "vnsServiceMaxPort";

  public static final String DISABLE_CASPROCESSOR = "disable";

  public static final String TERMINATE_CPE = "terminate";

  public static final String CONTINUE_DESPITE_ERROR = "continue";

  public static final String KILL_PROCESSING_PIPELINE = "kill-pipeline";

  public static final int CONNECT_RETRY_COUNT = 300;

  public static final String DOC_NAME = "Title";

  public static final String DOC_ID = "ID";

  public static final String DOC_SIZE = "Doc.Size";

  public static final String DOC_AUTHOR = "Author";

  public static final String DOC_DATE = "Date";

  public static final String DOC_SUBJECT = "Subject";

  public static final String FILENAME = "Filename";

  public static final String DELIMITER = "||";

  public static final String DEPLOYMENT_MODEL = "deployModel";

  public static final String DEPLOYMENT_LOCAL = "local";

  public static final String DEPLOYMENT_REMOTE = "remote";

  public static final String DEPLOYMENT_INTEGRATED = "integrated";

  public static final String INTERACTIVE_MODE = "interactive";

  public static final String VINCI_SERVICE_MODE = "vinciService";

  public static final String NONINTERACTIVE_MODE = "immediate";

  public static final String CAS_PROCESSOR_CONFIG = "processorConfig";

  public static final int CAS_PROCESSOR_READY = 1;

  public static final int CAS_PROCESSOR_RUNNING = 2;

  public static final int CAS_PROCESSOR_DISABLED = 3;

  public static final int CAS_PROCESSOR_COMPLETED = 4;

  public static final int CAS_PROCESSOR_KILLED = 5;

  public static final String CONTENT_TAG = "uima.cpm.DocumentText";

  public static final String CONTENT_CAS_TAG = "uima.tcas.DocumentAnnotation";

  public static final String CONTENT_TAG_VALUE = "value";

  public static final String BATCH_SIZE = "batchSize";

  public static final String METADATA_KEY = "uima.cpm.DocumentMetadata";

  // ProcessTrace Stuff
  public static final String COLLECTION_READER_DOCS_PROCESSED = "DOCS_PROCESSED";

  public static final String COLLECTION_READER_BYTES_PROCESSED = "BYTES_PROCESSED";

  // Progress access points
  public static final long PROGRESS_ENTITIES_COUNT = 1;

  public static final long PROGRESS_ENTITIES_SIZE = 2;

  public static final long PROGRESS_ABORT_COUNT = 3;

  public static final String PROGRESS_BYTES_IN = "Processor BYTESIN";

  public static final String PROGRESS_BYTES_OUT = "Processor BYTESOUT";

  public static final String PROGRESS_RETRIES = "Processor Retries";

  public static final String PROGRESS_RESTARTS = "Processor Restarts";

  public static final String PROGRESS_FILTERED = "Filtered Entities";

  public static final String PROGRESS_CPM_TIME = "CPM PROCESSING TIME";

  public static final String PROCESSOR_STATUS = "Processor Status";

  public static final String SOCKET_PROTOCOL = "socket";

  public static final String VINCI_PROTOCOL = "vinci";
}
