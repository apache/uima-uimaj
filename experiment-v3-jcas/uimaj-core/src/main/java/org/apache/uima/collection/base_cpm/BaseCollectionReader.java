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

package org.apache.uima.collection.base_cpm;

import java.io.IOException;

import org.apache.uima.collection.CollectionException;
import org.apache.uima.resource.Resource;
import org.apache.uima.resource.metadata.ProcessingResourceMetaData;
import org.apache.uima.util.Progress;

/**
 * The Base <code>CollectionReader</code> interface. Collection Readers should not implement this
 * interface directly. Instead they should implement
 * {@link org.apache.uima.collection.CollectionReader} or {@link CasDataCollectionReader}. Most
 * UIMA developers will prefer to use the former.
 * <p>
 * A <i>consuming</i> <code>CollectionReader</code> is one that removes each element from the
 * collection as it is read. To find out whether a <code>CollectionReader</code> will consume
 * elements in this way, call the {@link #isConsuming()} method.
 * <p>
 * Users of a <code>CollectionReader</code> should always {@link #close() close} it when they are
 * finished using it.
 * 
 * 
 */
public interface BaseCollectionReader extends Resource {
  /**
   * Gets whether there are any elements remaining to be read from this
   * <code>CollectionReader</code>.
   * 
   * @return true if and only if there are more elements available from this
   *         <code>CollectionReader</code>.
   * 
   * @throws IOException
   *           if an I/O failure occurs
   * @throws CollectionException
   *           if there is some other problem with reading from the Collection
   */
  public boolean hasNext() throws IOException, CollectionException;

  /**
   * Gets whether this is a <i>consuming</i> <code>CollectionReader</code>. Consuming
   * <code>CollectionReader</code>s remove each element from the <code>Collection</code> as it
   * is read.
   * 
   * @return true if and only if this is a consuming <code>CollectionReader</code>
   */
  public boolean isConsuming();

  /**
   * Gets information about the number of entities and/or amount of data that has been read from
   * this <code>CollectionReader</code>, and the total amount that remains (if that information
   * is available).
   * <p>
   * This method returns an array of <code>Progress</code> objects so that results can be reported
   * using different units. For example, the CollectionReader could report progress in terms of the
   * number of documents that have been read and also in terms of the number of bytes that have been
   * read. In many cases, it will be sufficient to return just one <code>Progress</code> object.
   * 
   * @return an array of <code>Progress</code> objects. Each object may have different units (for
   *         example number of entities or bytes).
   */
  public Progress[] getProgress();

  /**
   * Closes this <code>CollectionReader</code>, after which it may no longer be used.
   * 
   * @throws IOException
   *           if an I/O failure occurs
   */
  public void close() throws IOException;

  /**
   * Gets the metadata that describes this <code>CasProcesor</code>.
   * 
   * @return an object containing all metadata for this CasProcessor
   */
  public ProcessingResourceMetaData getProcessingResourceMetaData();
}
