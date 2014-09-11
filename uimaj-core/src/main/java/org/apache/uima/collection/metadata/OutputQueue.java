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

package org.apache.uima.collection.metadata;

import org.apache.uima.resource.metadata.MetaDataObject;

/**
 * An object that holds configuration that is part of the CPE descriptor. It provides the means of
 * configuring CPE output queue that is shared between processing pipelines and cas consumers
 * 
 */
public interface OutputQueue extends MetaDataObject {
  /**
   * Milliseconds to wait for new message
   * 
   * @return - ms to wait for message
   */
  public int getDequeueTimeout();

  /**
   * Class name of the queue object to be used as Output Queue
   * 
   * @return - name of the queue class
   */
  public String getQueueClass();

  /**
   * Define the time (in ms) to wait for a new message
   * 
   * @param i the time (in ms) to wait for a new message
   */
  public void setDequeueTimeout(int i);

  /**
   * Define a class for a queue object
   * 
   * @param string -
   *          name of the queue class
   */
  public void setQueueClass(String string);
}
