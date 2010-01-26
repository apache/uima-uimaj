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

package org.apache.uima.tools.cvd;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * List of recently used files.
 * 
 * <p>
 * Provides functionality to keep a list of a certain max size. New files are added to the top. When
 * a file is added that is already in the list, it is shuffled to the top.  When the maximum list
 * size has been reached, files start dropping off the end.
 */
public class RecentFilesList {

  // The internal list
  private final List<File> list;

  // The maximum size to which this list can grow
  private final int max;
  
  /**
   * Constructor.
   * @param maxSize The maximum size to which the list can grow.
   */
  public RecentFilesList(int maxSize) {
    super();
    this.list = new ArrayList<File>();
    this.max = maxSize;
  }

  /**
   * Get the internal list of files, in the proper order (most recent first).
   * @return A recency-sorted list of files.
   */
  public List<File> getFileList() {
    return this.list;
  }

  /**
   * Append a file at the end of the list (useful for initialization).
   * @param file The file to be added.
   */
  public void appendFile(File file) {
    if (this.list.size() < this.max) {
      this.list.add(file);
    }
  }

  /**
   * Add a file at the beginning.  If maximum capacity is exceeded, drop last file.  If
   * <code>file</code> is already in the list, move it to the front.
   * @param file The file to be added.
   */
  public void addFile(File file) {
    // Check to see if list contains file already.
    if (this.list.contains(file)) {
      // Switch file to top and shuffle rest down.
      // No need to check for overflow here because we're not adding anything.
      final int filePos = this.list.indexOf(file);
      for (int i = filePos - 1; i >= 0; i--) {
        this.list.set(i + 1, this.list.get(i));
      }
      // Set file to the beginning
      this.list.set(0, file);
    } else {
      // File is not contained in list.  First, check if list is empty as shift code assumes
      // non-empty list.
      if (this.list.isEmpty()) {
        // To an empty list, we can just add the file.
        this.list.add(file);
      } else {
        // List is not empty.  Shift list right by one, possibly dropping last file.
        shiftRight();
        // Set file to beginning
        this.list.set(0, file);
      }
    }
  }

  private final void shiftRight() {
    if (this.list.size() < this.max) {
      // If max has not been reached, duplicate last file
      this.list.add(this.list.get(this.list.size() - 1));
    }
    // Last file has been taken care of (either duplicated or it will be dropped).  Shift all
    // others to the right by one.
    for (int i = this.list.size() - 2; i >= 0; i--) {
      this.list.set(i + 1, this.list.get(i));
    }
  }

  /**
   * Return the file list as a list of strings for persistence.
   * @return The file list as a list of absolute file names.
   */
  public List<String> toStringList() {
    List<String> out = new ArrayList<String>();
    for (File file : this.list) {
      out.add(file.getAbsolutePath());
    }
    return out;
  }

}
