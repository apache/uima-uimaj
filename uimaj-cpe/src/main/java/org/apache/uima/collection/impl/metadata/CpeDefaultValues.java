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

package org.apache.uima.collection.impl.metadata;

public class CpeDefaultValues {
  public static final String TIMER_IMPL = "org.apache.uima.internal.util.JavaTimer";

  public static final String START_AT = "1";

  public static final long NUM_TO_PROCESS = -1;

  public static final String DEPLOY_AS = "immediate";

  public static final String FILE = "C:/resporator/CpmConverged/checkpoint.dat";

  public static final String TIME = "100000s";

  public static final short BATCH = 1000;

  public static final String INPUT_Q_SIZE = "5";

  public static final String OUTPUT_Q_SIZE = "5";

  public static final String PROCESSING_UNIT_THREAD_COUNT = "1";

  public static final String DEPLOYMENT = "integrated";

  public static final short MAX_CONSEQUTIVE_RESTART_VAL = 1;

  public static final String MAX_CONSEQUTIVE_RESTART_ACTION = "terminate";

  public static final String ERROR_THRESHOLD_VAL = "3/1000";

  public static final String ERROR_THRESHOLD_ACTION = "disable";

  public static final String TIMEOUT_DEFAULT = "3000";

  public static final String TIMEOUT_MAX = "5000";

  public static final String PROCESSOR_CHECKPOINT_FILE = "";

  public static final String PROCESSOR_CHECKPOINT_TIME = "3000ms";

  public static final short PROCESSOR_CHECKPOINT_BATCH = 2;

}
