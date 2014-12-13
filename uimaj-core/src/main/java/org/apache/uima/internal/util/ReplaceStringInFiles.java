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

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

import org.apache.uima.util.FileUtils;

/**
 * String replacement utility.
 */
public class ReplaceStringInFiles {

  private static DirFileFilter dirFileFilter = new DirFileFilter();

  private static class ExtFileFilter implements FileFilter {

    private String ext;

    private ExtFileFilter(String extension) {
      this.ext = extension;
    }

    public boolean accept(File file) {
      return (file.isFile() && file.getName().endsWith(this.ext));
    }

  }

  private static class DirFileFilter implements FileFilter {

    public boolean accept(File file) {
      return file.isDirectory();
    }

  }

  /**
   * Result class for string replacement.
   */
  public static class ReplacementResult {
    public String outString;

    public int numReplaced;
  }

  /**
   * @param mainStr
   *          The given string object.
   * @param argStr
   *          The given string argument.
   * @param start -
   * @param caseSensitive -
   * @return If the given string argument occurs as a substring, ignoring case, within the given
   *         string object, then the index of the first character of the first such substring is
   *         returned; if it does not occur as a substring, <code>-1</code> is returned.
   */
  public static int indexOfCaseSensitive(String mainStr, String argStr, int start,
          boolean caseSensitive) {
    if (caseSensitive) {
      return mainStr.indexOf(argStr, start);
    }
    String source = mainStr.toLowerCase();
    String pattern = argStr.toLowerCase();
    int index = source.indexOf(pattern, start);
    return index;
  }

  /**
   * Replace occurrences of <code>toReplace</code> in <code>text</code> by
   * <code>replacement.</code>
   * 
   * @param text
   *          The text where the replacement should happen.
   * @param toReplace
   *          The string that should be replaced.
   * @param replacement
   *          The string it should be replaced with.
   * @param res
   *          The result object, containing the result string and the number of times replacement
   *          happened.
   * @param caseSensitive -         
   */
  public static final void replaceStringInString(String text, String toReplace, String replacement,
          ReplacementResult res, boolean caseSensitive) {
    StringBuffer buf = new StringBuffer();
    int current = 0;
    int next;
    int count = 0;
    while ((next = indexOfCaseSensitive(text, toReplace, current, caseSensitive)) >= 0) {
      buf.append(text.substring(current, next));
      buf.append(replacement);
      current = next + toReplace.length();
      ++count;
    }
    buf.append(text.substring(current, text.length()));
    res.outString = buf.toString();
    res.numReplaced = count;
  }

  /**
   * Replace a string in all files of a directory, recursively.
   * 
   * @param dir
   *          The directory where replacement should happen.
   * @param fileFilter
   *          A file filter for which files replacement should happen.
   * @param toReplace
   *          String which should be replaced.
   * @param replacement
   *          String it should be replaced with.
   * @param caseSensitive -         
   * @throws IOException
   *           Whenever anything goes wrong reading or writing a file.
   */
  public static final void replaceStringInFiles(File dir, FileFilter fileFilter, String toReplace,
          String replacement, boolean caseSensitive) throws IOException {
    // Get all files with correct extension.
    File[] fileList = null;
    if (fileFilter == null) {
      fileList = dir.listFiles();
    } else {
      fileList = dir.listFiles(fileFilter);
    }
    File file;
    ReplacementResult result = new ReplacementResult();
    for (int i = 0; i < fileList.length; i++) {
      file = fileList[i];
      // Skip files that are not proper files.
      if (!file.isFile()) {
        continue;
      }
      // Skip files we can't read.
      if (!file.canRead()) {
        System.err.println("Warning, can't read file: " + file.getAbsolutePath());
        continue;
      }
      // Skip files we can't write.
      if (!file.canWrite()) {
        System.err.println("Warning, can't write file: " + file.getAbsolutePath());
        continue;
      }
      System.out.println("Working on file: " + file.getAbsolutePath());
      // Read file contents.
      String fileContents = FileUtils.file2String(file);
      // Replace.
      // fileContents = fileContents.replaceAll(toReplace, replacement);
      replaceStringInString(fileContents, toReplace, replacement, result, caseSensitive);
      fileContents = result.outString;
      if (result.numReplaced > 0) {
        // Write file contents back out.
        FileUtils.saveString2File(fileContents, file);
        System.out.println("File modified, number of instances replaced: " + result.numReplaced);
      }
    }
    // Recursively call for subdirectories.
    fileList = dir.listFiles(dirFileFilter);
    for (int i = 0; i < fileList.length; i++) {
      replaceStringInFiles(fileList[i], fileFilter, toReplace, replacement, caseSensitive);
    }
  }

  /**
   * Replace a certain string with other strings in files. Example usage:<br>
   * <code>java org.apache.uima.util.ReplaceStringInFile /home/tom/stuff .prop $ROOT$ /home/tom/root</code>
   * 
   * @param args dir, extension toreplace replacement [-ignorecase]
   */
  public static void main(String[] args) {
    if (args.length != 4 && args.length != 5) {
      System.out
              .println("Usage: java org.apache.uima.util.ReplaceStringInFile <Dir> <Extension> <ToReplace> <Replacement> [-ignorecase]");
      System.exit(1);
    }
    try {
      String dirName = args[0];
      String extension = args[1];
      String toReplace = args[2];
      String replacement = args[3];
      boolean caseSensitive = false;
      if (args.length >= 5) {
        caseSensitive = (args[4].compareToIgnoreCase("-ignorecase") != 0);
      }
      if (caseSensitive) {
        System.out.println("Case sensitive");
      } else {
        System.out.println("Ignoring case");
      }

      File dir = new File(dirName);
      if (dir.isDirectory()) {
        replaceStringInFiles(dir, new ExtFileFilter(extension), toReplace, replacement,
                caseSensitive);
      }

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}
