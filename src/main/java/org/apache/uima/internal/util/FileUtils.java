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

package org.apache.uima.internal.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Random;

/**
 * Some utilities for handling files.
 * 
 * 
 */
public class FileUtils {

  /**
   * Get a list of all files in a directory. Optionally, get files in subdirectories as well.
   * 
   * @param directory
   *          The directory for which to get the files.
   * @param getRecursive
   *          Should we get files from subdirectories?
   * @return ArrayList A list of <code>File</code> objects. <code>null</code> if
   *         <code>directory</code> does not exist, or is not a directory.
   */
  public static final ArrayList getFiles(File directory, boolean getRecursive) {
    if (!directory.exists() || !directory.isDirectory()) {
      return null;
    }
    ArrayList fileList = new ArrayList();
    File[] fileArray = directory.listFiles();
    File file;
    for (int i = 0; i < fileArray.length; i++) {
      file = fileArray[i];
      if (file.isDirectory()) {
        if (getRecursive) {
          fileList.addAll(getFiles(file, getRecursive));
        }
      } else {
        fileList.add(file);
      }
    }
    return fileList;
  }

  /**
   * Get a list of all files in a directory, ignoring subdirectories.
   * 
   * @param directory
   *          The directory for which to get the files.
   * @return ArrayList A list of <code>File</code> objects. <code>null</code> if
   *         <code>directory</code> does not exist, or is not a directory.
   */
  public static final ArrayList getFiles(File directory) {
    return getFiles(directory, false);
  }

  /**
   * Get a list of all subdirectories in a directory, ignoring regular files.
   * 
   * @param directory
   *          The directory for which to get the subdirectories.
   * @return ArrayList A list of <code>File</code> objects. <code>null</code> if
   *         <code>directory</code> does not exist, or is not a directory.
   */
  public static final ArrayList getSubDirs(File directory) {
    if (!directory.exists() || !directory.isDirectory()) {
      return null;
    }
    ArrayList dirList = new ArrayList();
    File[] fileList = directory.listFiles();
    File file;
    for (int i = 0; i < fileList.length; i++) {
      file = fileList[i];
      if (file.isDirectory()) {
        dirList.add(file);
      }
    }
    return dirList;
  }

  /**
   * Read the contents of a file into a string, using the default platform encoding.
   * 
   * @param file
   *          The file to be read in.
   * @return String The contents of the file.
   * @throws IOException
   *           Various I/O errors.
   */
  public static String file2String(File file) throws IOException {
    // Read the file into a string using a char buffer.
    char[] buf = new char[10000];
    int charsRead;
    BufferedReader reader = new BufferedReader(new FileReader(file));
    StringBuffer strbuf = new StringBuffer();
    while ((charsRead = reader.read(buf)) >= 0) {
      strbuf.append(buf, 0, charsRead);
    }
    reader.close();
    final String text = strbuf.toString();
    return text;
  }

  /**
   * Read the contents of a file into a string using a specific character encoding.
   * 
   * @param file
   *          The input file.
   * @param fileEncoding
   *          The character encoding of the file (see your Java documentation for supported
   *          encodings).
   * @return String The contents of the file.
   * @throws IOException
   *           Various I/O errors.
   */
  public static String file2String(File file, String fileEncoding) throws IOException {
    // Read the file into a string using a char buffer.
    char[] buf = new char[10000];
    int charsRead;
    BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file),
                    fileEncoding));
    StringBuffer strbuf = new StringBuffer();
    while ((charsRead = reader.read(buf)) >= 0) {
      strbuf.append(buf, 0, charsRead);
    }
    reader.close();
    final String text = strbuf.toString();
    return text;
  }

  /**
   * Write a string to a file. If the file exists, it is overwritten.
   * 
   * @param fileContents
   *          The file contents.
   * @param file
   *          The file to save to.
   * @throws IOException
   *           If for any reason the file can't be written.
   */
  public static void saveString2File(String fileContents, File file) throws IOException {
    BufferedWriter writer = new BufferedWriter(new FileWriter(file));
    writer.write(fileContents);
    writer.close();
  }

  /**
   * Write a string to a file. If the file exists, it is overwritten.
   * 
   * @param fileContents
   *          The file contents.
   * @param file
   *          The file to save to.
   * @param encoding
   *          The character encoding to use.
   * @throws IOException
   *           If for any reason the file can't be written.
   */
  public static void saveString2File(String s, File file, String encoding) throws IOException {
    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file),
                    encoding));
    writer.write(s);
    writer.close();
  }

  /**
   * Delete all files in a directory (not recursive).
   * 
   * @param directory
   *          The directory that contains the files to be deleted.
   */
  public static final void deleteAllFiles(File directory) {
    File[] fileList = directory.listFiles();
    // If file does not exist, or is not a directory, do nothing.
    if (fileList == null) {
      return;
    }
    File file;
    for (int i = 0; i < fileList.length; i++) {
      file = fileList[i];
      if (file.isFile()) {
        file.delete();
      }
    }
  }

  /**
   * Recursively delete possibly non-empty directory or file.
   * 
   * @param file
   *          The file or directory to be deleted.
   * @return <code>true</code> iff the file/directory could be deleted.
   */
  public static final boolean deleteRecursive(File file) {
    if (!file.exists()) {
      return false;
    }
    boolean rc = true;
    if (file.isDirectory()) {
      File[] fileList = file.listFiles();
      for (int i = 0; i < fileList.length; i++) {
        rc &= deleteRecursive(fileList[i]);
      }
    }
    rc &= file.delete();
    return rc;
  }

  /**
   * Create a new directory. The parent directory must exist.
   * 
   * @param directory
   *          The directory to be created.
   * @return boolean Will fail if directory already exists, or File.mkdir() returns
   *         <code>false</code>.
   */
  public static final boolean mkdir(File directory) {
    // Check if directory already exists. The documentation is silent on
    // what
    // could actually cause File.mkdir() to fail.
    if (directory.exists()) {
      return false;
    }
    return directory.mkdir();
  }

  /**
   * Create a temporary directory using random numbers as name suffixes.
   * 
   * @param parent
   *          Where the directory is created.
   * @param prefix
   *          Prefix of the directory names to be created.
   * @return A file object corresponding to the newly created dir, or <code>null</code> if none
   *         could be created for some reason (e.g., if the parent is not writable).
   */
  public static final File createTempDir(File parent, String prefix) {
    Random rand = new Random();
    File tempDir;
    while (true) {
      tempDir = new File(parent, prefix + rand.nextInt());
      if (!tempDir.exists()) {
        if (tempDir.mkdirs()) {
          tempDir.deleteOnExit();
          return tempDir;
        }
        return null;
      }
    }
  }

  public static final File createTempFile(String prefix, String suffix, File tempDir)
                  throws IOException {
    File file = File.createTempFile(prefix, suffix, tempDir);
    file.deleteOnExit();
    return file;
  }

  /**
   * Copy file <code>file</code> to location <code>dir</code>. This method will not fail
   * silently. Anything that prevents the copy from happening will be treated as an error condition.
   * If the method does not throw an exception, the copy was successful.
   * 
   * <p>
   * <b>Note: </b> this is a completely brain-dead implementation of file copy. The complete file is
   * read into memory before it is copied. If you need something more sophisticated for copying
   * large files, feel free to add your improvements.
   * 
   * @param file
   *          The file to copy.
   * @param dir
   *          The destination directory.
   * @throws IOException
   *           For various reason: if <code>file</code> does not exist or is not readable, if the
   *           destination directory does not exist or isn't a directory, or if the file can't be
   *           copied for any reason.
   */
  public static final void copyFile(File file, File dir) throws IOException {
    if (!file.exists() || !file.canRead()) {
      throw new IOException("File does not exist or is not readable: " + file.getAbsolutePath());
    }
    if (!dir.exists() || !dir.isDirectory()) {
      throw new IOException("Destination does not exist or is not a directory: "
                      + dir.getAbsolutePath());
    }
    File outFile = new File(dir, file.getName());
    if (outFile.exists() && !outFile.canWrite()) {
      throw new IOException("Can't write output file: " + outFile);
    }
    byte[] bytes = new byte[(int) file.length()];
    BufferedInputStream is = new BufferedInputStream(new FileInputStream(file));
    is.read(bytes);
    is.close();
    BufferedOutputStream os = null;
    try {
      os = new BufferedOutputStream(new FileOutputStream(outFile));
      os.write(bytes);
      os.close();
    } finally {
      if (os != null) {
        os.close();
      }
    }
  }

  public static void main(String[] args) {
    if (args.length != 1) {
      return;
    }
    String fileName = args[0];
    System.out.println("Deleting: " + fileName);
    if (deleteRecursive(new File(fileName))) {
      System.out.println("File successfully deleted.");
    } else {
      System.out.println("Could not delete (all) file(s).");
    }
  }

}
