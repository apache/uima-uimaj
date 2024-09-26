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
package org.apache.uima.fit.util;

import java.io.IOException;

import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.collection.base_cpm.BaseCollectionReader;
import org.apache.uima.resource.Resource;
import org.apache.uima.resource.ResourceManager;

/**
 * Helper methods to handle the life cycle of UIMA components.
 */
public final class LifeCycleUtil {

  private LifeCycleUtil() {
    // No instances
  }

  /**
   * Notify a set of {@link AnalysisEngine analysis engines} that the collection process is
   * complete.
   * 
   * @param engines
   *          the engines to notify of completion
   * @throws AnalysisEngineProcessException
   *           if there was a problem in the engines reacting to completion
   */
  public static void collectionProcessComplete(final AnalysisEngine... engines)
          throws AnalysisEngineProcessException {
    for (AnalysisEngine e : engines) {
      e.collectionProcessComplete();
    }
  }

  /**
   * Destroy a set of {@link ResourceManager resource manager}.
   * 
   * @param aResMgr
   *          the resource manager to destroy
   */
  public static void destroy(final ResourceManager aResMgr) {
    if (aResMgr != null) {
      aResMgr.destroy();
    }
  }

  /**
   * Destroy a set of {@link Resource resources}.
   * 
   * @param resources
   *          the resources to destroy
   */
  public static void destroy(final Resource... resources) {
    for (Resource r : resources) {
      if (r != null) {
        r.destroy();
      }
    }
  }

  /**
   * Close a reader.
   * 
   * @param aReader
   *          the reader to close
   */
  public static void close(final BaseCollectionReader aReader) {
    if (aReader == null) {
      return;
    }

    try {
      aReader.close();
    } catch (IOException e) {
      // Ignore.
    }
  }
}
