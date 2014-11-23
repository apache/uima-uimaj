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

package org.apache.uima.pear.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * The <code>FileUtil</code> class provides utility methods for working with general files.
 */

public class FileUtil {

  /**
   * The <code>FileTimeComparator</code> class allows comparing 'last modified' time in 2 given
   * <code>File</code> objects.
   */
  public static class FileTimeComparator implements Comparator<File> {
    /**
     * @return A negative integer, zero, or a positive integer as the first argument is less than,
     *         equal to, or greater than the second.
     * @throws java.lang.ClassCastException
     *           if the arguments' types prevent them from being compared by this
     *           <code>Comparator</code>.
     */
    public int compare(File o1, File o2) throws ClassCastException {
      long t1 = o1.lastModified();
      long t2 = o2.lastModified();
      return (t1 >= t2) ? -1 : 1;
    }

    /**
     * @param obj
     *          The reference object with which to compare.
     * @return <code>true</code> only if the specified object is also a
     *         <code>FileTimeComparator</code>, and it imposes the same ordering as this
     *         comparator.
     */
    public boolean equals(Object obj) {
      return (obj instanceof FileTimeComparator);
    }
  } // FileTimeComparator end

  /**
   * The <code>DirFileFilter</code> class allows to filter files based on specified directory path
   * and filename extension.
   */
  public static class DirFileFilter implements FileFilter {
    // attributes
    private String _dirPath;

    private String _fileExt;

    /**
     * Constructs <code>DirFileFilter</code> object for specified directory and file extension.
     * 
     * @param dirPath
     *          The given directory.
     * @param fileExt
     *          The given file extension.
     */
    public DirFileFilter(String dirPath, String fileExt) {
      _dirPath = (dirPath != null) ? dirPath.replace('\\', '/') : null;
      if (fileExt != null)
        _fileExt = fileExt.startsWith(".") ? fileExt.toLowerCase() : "." + fileExt.toLowerCase();
      else
        _fileExt = null;
    }

    /**
     * @param file
     *          The given file to be tested.
     * @return <code>true</code> if the given file should be accepted, <code>false</code>
     *         otherwise.
     */
    public boolean accept(File file) {
      boolean dirAccepted = true;
      boolean extAccepted = true;
      if (_dirPath != null) {
        String parentDir = file.getParent();
        dirAccepted = parentDir != null && parentDir.replace('\\', '/').startsWith(_dirPath);
      }
      if (_fileExt != null) {
        extAccepted = file.getPath().toLowerCase().endsWith(_fileExt);
      }
      return dirAccepted && extAccepted;
    }
  } // DirFileFilter end

  /**
   * The <code>NameFileFilter</code> class allows to filter files based on specified file name.
   * 
   */
  public static class NameFileFilter implements FileFilter {
    // attributes
    private String _fileName;

    /**
     * Constructs <code>NameFileFilter</code> object for a given file name.
     * 
     * @param fileName
     *          The given file name for filtering.
     */
    public NameFileFilter(String fileName) {
      _fileName = fileName.replace('\\', '/');
    }

    /**
     * @param file
     *          The given file to be tested.
     * @return <code>true</code> if the given file should be accepted, <code>false</code>
     *         otherwise.
     */
    public boolean accept(File file) {
      String filePath = file.getAbsolutePath().replace('\\', '/');
      if (filePath.endsWith(_fileName)) {
        if (filePath.length() > _fileName.length()) {
          char prevChar = filePath.charAt(filePath.length() - _fileName.length() - 1);
          if (prevChar == ':' || prevChar == '/')
            return true;
        } else
          return true;
      }
      return false;
    }
  } // NameFileFilter end

  /**
   * The <code>ExtFileFilter</code> allows to filter file names based on the specified filename
   * extension.
   * 
   */
  public static class ExtFilenameFilter implements FilenameFilter {
    // attributes
    private String _fileExt;

    private boolean _ignoreCase;

    /**
     * Create instance of the <code>ExtFileFilter</code> class for a given filename extension. By
     * default, this filename filter is case insensitive. If the given filename extension does not
     * start from the '.' character, adds this character at the beginning.
     * 
     * @param fileExt
     *          The given filename extension.
     */
    public ExtFilenameFilter(String fileExt) {
      this(fileExt, true);
    }

    /**
     * Create instance of the <code>ExtFileFilter</code> class for a given filename extension. If
     * a given <code>boolean</code> flag is <code>true</code>, this filename filter is case
     * insensitive, otherwise it's case sensitive. If the given filename extension does not start
     * from the '.' character, adds this character at the beginning.
     * 
     * @param fileExt
     *          The given filename extension.
     * @param ignoreCase
     *          The given 'case sensitivity' flag.
     */
    public ExtFilenameFilter(String fileExt, boolean ignoreCase) {
      _fileExt = fileExt.startsWith(".") ? fileExt : "." + fileExt;
      _ignoreCase = ignoreCase;
      if (ignoreCase)
        _fileExt = _fileExt.toLowerCase();
    }

    /**
     * Tests if a specified file should be included in a file list.
     * 
     * @param dir
     *          The directory in which the file was found.
     * @param name
     *          The given name of the file.
     * @return <code>true</code>, if the given file should be included in the list,
     *         <code>false</code> otherwise.
     */
    public boolean accept(File dir, String name) {
      String fileName = _ignoreCase ? name.toLowerCase() : name;
      return fileName.endsWith(_fileExt);
    }
  } // ExtFilenameFilter end

  /**
   * Deletes all files and subdirectories in a given directory. In case of unsuccessful deletion,
   * calls the <code>deleteOnExit()</code> method to request that files and subdirs are deleted
   * when the JVM terminates.
   * 
   * @param directory
   *          The given directory to be cleaned-up.
   * @return The number of successfully deleted entries in the given directory.
   * @throws IOException
   *           If an I/O exception occurred.
   */
  public static int cleanUpDirectoryContent(File directory) throws IOException {
    int counter = 0;
    File[] allDirFiles = directory.listFiles();
    if (allDirFiles != null) {
      for (int i = 0; i < allDirFiles.length; i++) {
        File aFile = allDirFiles[i];
        if (aFile.isDirectory()) {
          counter += cleanUpDirectoryContent(aFile);
          if (aFile.delete())
            counter++;
          else
            aFile.deleteOnExit();
        } else if (aFile.isFile()) {
          if (aFile.delete())
            counter++;
          else
            aFile.deleteOnExit();
        }
      }
    }
    return counter;
  }

  /**
   * Deletes all files in a given directory. In case of unsuccessful deletion, calls the
   * <code>deleteOnExit()</code> method to request that files are deleted when the JVM terminates.
   * 
   * @param directory
   *          The given directory to be cleaned-up.
   * @return The number of successfully deleted entries in the given directory.
   * @throws IOException
   *           If an I/O exception occurred.
   */
  public static int cleanUpDirectoryFiles(File directory) throws IOException {
    int counter = 0;
    File[] allDirFiles = directory.listFiles();
    if (allDirFiles != null) {
      for (int i = 0; i < allDirFiles.length; i++) {
        File aFile = allDirFiles[i];
        if (aFile.isFile()) {
          if (aFile.delete())
            counter++;
          else
            aFile.deleteOnExit();
        }
      }
    }
    return counter;
  }

  /**
   * Cleans-up a given directory by keeping the number of files within a given limit. Deletes the
   * oldest files first. In case of unsuccessful deletion, calls the <code>deleteOnExit()</code>
   * method to request that files are deleted when the JVM terminates.
   * 
   * @param directory
   *          The given directory.
   * @param maxLimit
   *          The given maximum limit of the number of files in the given directory.
   * @return The number of actually deleted files.
   * @throws IOException
   *           If an I/O exception occurred.
   */
  public static int cleanUpDirectoryFiles(File directory, int maxLimit) throws IOException {
    int counter = 0;
    Collection<File> fileList = createFileList(directory, false);
    SortedSet<File> sortedFileSet = sortFileListByTime(fileList);
    if (sortedFileSet.size() > maxLimit) {
      Iterator<File> list = sortedFileSet.iterator();
      int no = 0;
      while (list.hasNext()) {
        File file = list.next();
        no++;
        if (no > maxLimit) {
          if (file.delete())
            counter++;
          else
            file.deleteOnExit();
        }
      }
    }
    return counter;
  }

  /**
   * Computes relative path to a given file from a given reference directory, if both the reference
   * directory and the file are in the same logical file system (partition).
   * 
   * @param referenceDir
   *          The given reference directory.
   * @param file
   *          The given file.
   * @return The relative path to the given file from the given reference directory, or
   *         <code>null</code>, if the relative path does not exist.
   * @throws IOException
   *           If an I/O error occurs, which is possible because the construction of the canonical
   *           pathname may require filesystem queries.
   */
  public static String computeRelativePath(File referenceDir, File file) throws IOException {
    // get canonical path expressions
    String refPath = referenceDir.getCanonicalPath().replace('\\', '/');
    String filePath = file.getCanonicalPath().replace('\\', '/');
    // compute relative path from reference dir to file dir-tree
    StringBuffer relBuffer = new StringBuffer();
    while (refPath != null && !filePath.startsWith(refPath)) {
      relBuffer.append("../");
      refPath = (new File(refPath)).getParent();
      if (refPath != null)
        refPath = refPath.replace('\\', '/');
    }
    if (refPath != null) {
      // construct relative path
      String subPath = filePath.substring(refPath.length());
      if (relBuffer.length() == 0)
        relBuffer.append("./");
      if (subPath.startsWith("/"))
        relBuffer.append(subPath.substring(1));
      else
        relBuffer.append(subPath);
      return relBuffer.toString();
    }
    // relative path does not exist
    return null;
  }

  /**
   * Copies the content of a given source file to a given destination file.
   * 
   * @param source
   *          The given source file.
   * @param destination
   *          The given destination file.
   * @return <code>true</code> if the copy operation completed successfully, <code>false</code>
   *         otherwise.
   * @throws IOException
   *           If any I/O exception occurred.
   */
  public static boolean copyFile(File source, File destination) throws IOException {
    boolean completed = false;
    BufferedInputStream iStream = null;
    BufferedOutputStream oStream = null;
    try {
      iStream = new BufferedInputStream(new FileInputStream(source));
      oStream = new BufferedOutputStream(new FileOutputStream(destination));
      byte[] block = new byte[4096];
      int bCount = 0;
      while ((bCount = iStream.read(block)) > 0) {
        oStream.write(block, 0, bCount);
      }
      iStream.close();
      oStream.close();
      completed = true;
    } finally {
      if (iStream != null) {
        try {
          iStream.close();
        } catch (Exception e) {
        }
      }
      if (oStream != null) {
        try {
          oStream.close();
        } catch (Exception e) {
        }
      }
    }
    return completed;
  }

  /**
   * Copies the content of a given remote source file to a given destination file.
   * 
   * @param sourceUrl
   *          The given source file URL.
   * @param destination
   *          The given destination file.
   * @return <code>true</code> if the copy operation completed successfully, <code>false</code>
   *         otherwise.
   * @throws IOException
   *           If any I/O exception occurred.
   */
  public static boolean copyFile(URL sourceUrl, File destination) throws IOException {
    boolean completed = false;
    BufferedInputStream iStream = null;
    BufferedOutputStream oStream = null;
    try {
      iStream = new BufferedInputStream(sourceUrl.openStream());
      oStream = new BufferedOutputStream(new FileOutputStream(destination));
      byte[] block = new byte[4096];
      int bCount = 0;
      while ((bCount = iStream.read(block)) > 0) {
        oStream.write(block, 0, bCount);
      }
      iStream.close();
      oStream.close();
      completed = true;
    } finally {
      if (iStream != null) {
        try {
          iStream.close();
        } catch (Exception e) {
        }
      }
      if (oStream != null) {
        try {
          oStream.close();
        } catch (Exception e) {
        }
      }
    }
    return completed;
  }

  /**
   * Creates list of subdirectories in a given root directory, including all its subdirectories.
   * 
   * @param rootDir
   *          The given root directory.
   * @return <code>Collection</code> of <code>File</code> objects, representing subdirectories
   *         in the given root directory and all its subdirectories.
   * 
   * @throws java.io.IOException
   *           If any I/O exception occurs.
   */
  public static Collection<File> createDirList(File rootDir) throws IOException {
    return createDirList(rootDir, true);
  }

  /**
   * Creates list of subdirectories in a given root directory. If a given <code>boolean</code>
   * flag is <code>true</code>, all the subdirectories of the given root directory are also
   * scanned, otherwise only subdirectories in the given root directory are included.
   * 
   * @return <code>Collection</code> of <code>File</code> objects, representing subdirectories
   *         in the given root directory.
   * @param rootDir
   *          The given root directory.
   * @param includeSubdirs
   *          If <code>true</code>, the returned list includes sub-directories from all
   *          sub-directories of the given root directory, otherwise it includes only
   *          sub-directories from the given root directory itself.
   * 
   * @exception java.io.IOException
   *              If any I/O exception occurs.
   */
  public static Collection<File> createDirList(File rootDir, boolean includeSubdirs) throws IOException {
    ArrayList<File> listOfDirs = new ArrayList<File>();
    File[] allDirFiles = rootDir.listFiles();
    if (allDirFiles == null)
      throw new FileNotFoundException("invalid directory specified");
    for (int i = 0; i < allDirFiles.length; i++) {
      File aFile = allDirFiles[i];
      if (aFile.isDirectory()) {
        listOfDirs.add(aFile);
        if (includeSubdirs)
          listOfDirs.addAll(createDirList(aFile, includeSubdirs));
      }
    }
    return listOfDirs;
  }

  /**
   * Creates a list of directories in a given archive (JAR) file. The root directory path, used to
   * represent the directories, is set to the input archive file path without the file name
   * extension.
   * 
   * @param archive
   *          The input archive (JAR) file.
   * @return <code>Collection</code> of <code>File</code> objects, representing directories in
   *         the given archive file.
   * @throws IOException
   *           If any I/O exception occurs.
   */
  public static Collection<File> createDirList(JarFile archive) throws IOException {
    ArrayList<File> listOfDirs = new ArrayList<File>();
    // set root_dir_path = archive_file_path (w/o file name extension)
    int nameEndIndex = archive.getName().lastIndexOf('.');
    String rootDirPath = (nameEndIndex > 0) ? archive.getName().substring(0, nameEndIndex)
            : archive.getName();
    File rootDir = new File(rootDirPath);
    // add directories to the list
    Enumeration<JarEntry> entries = archive.entries();
    while (entries.hasMoreElements()) {
      JarEntry entry = entries.nextElement();
      File file = new File(rootDir, entry.getName());
      if (entry.isDirectory())
        listOfDirs.add(file);
      else {
        // make sure the parent dir is added
        File parentDir = file.getParentFile();
        while (!parentDir.equals(rootDir)) {
          if (!listOfDirs.contains(parentDir))
            listOfDirs.add(parentDir);
          parentDir = parentDir.getParentFile();
        }
      }
    }
    return listOfDirs;
  }

  /**
   * Creates list of files in a given directory, including all its subdirectories.
   * 
   * @return <code>Collection</code> of <code>File</code> objects in the given directory,
   *         including all its subdirectories.
   * @param filesDir
   *          The given directory.
   * 
   * @exception java.io.IOException
   *              If any I/O exception occurs.
   */
  public static Collection<File> createFileList(File filesDir) throws IOException {
    return createFileList(filesDir, true);
  }

  /**
   * Creates list of files in a given directory. If a given <code>boolean</code> flag is
   * <code>true</code>, all the sub-directories of the given directory are also scanned,
   * otherwise only files in the given directory are included.
   * 
   * @return <code>Collection</code> of <code>File</code> objects in the given directory.
   * @param filesDir
   *          The given directory.
   * @param includeSubdirs
   *          If <code>true</code>, the returned file list includes files from all the
   *          sub-directories of the given directory, otherwise it includes only files from the
   *          given directory itself.
   * 
   * @exception java.io.IOException
   *              If any I/O exception occurs.
   */
  public static Collection<File> createFileList(File filesDir, boolean includeSubdirs) throws IOException {
    ArrayList<File> listOfFiles = new ArrayList<File>();
    File[] allDirFiles = filesDir.listFiles();
    if (allDirFiles == null)
      throw new FileNotFoundException("invalid directory specified");
    for (int i = 0; i < allDirFiles.length; i++) {
      File aFile = allDirFiles[i];
      if (aFile.isDirectory() && includeSubdirs)
        listOfFiles.addAll(createFileList(aFile, includeSubdirs));
      else if (!aFile.isDirectory())
        listOfFiles.add(aFile);
    }
    return listOfFiles;
  }

  /**
   * Creates a list of files in a given archive (JAR) file. The root directory path, used to
   * represent the files, is set to the input archive file path without the file name extension.
   * 
   * @param archive
   *          The input archive (JAR) file.
   * @return <code>Collection</code> of <code>File</code> objects, representing files in the
   *         given archive file.
   * @throws IOException
   *           If any I/O exception occurs.
   */
  public static Collection<File> createFileList(JarFile archive) throws IOException {
    ArrayList<File> listOfFiles = new ArrayList<File>();
    // set root_dir_path = archive_file_path (w/o file name extension)
    int nameEndIndex = archive.getName().lastIndexOf('.');
    String rootDirPath = (nameEndIndex > 0) ? archive.getName().substring(0, nameEndIndex)
            : archive.getName();
    File rootDir = new File(rootDirPath);
    // add directories to the list
    Enumeration<JarEntry> entries = archive.entries();
    while (entries.hasMoreElements()) {
      JarEntry entry = entries.nextElement();
      File file = new File(rootDir, entry.getName());
      if (!entry.isDirectory())
        listOfFiles.add(file);
    }
    return listOfFiles;
  }

  /**
   * Creates a new empty file in a directory specified by the 'java.io.tmpdir' or the 'user.home'
   * system property, using given prefix and suffix strings to generate its name. For more
   * information see the documentation on the <code>java.io.File.createTempFile()</code> method.
   * 
   * @param prefix
   *          The given prefix string to be used in generating the file's name; must be at least
   *          three characters long.
   * @param suffix
   *          The given suffix string to be used in generating the file's name; may be
   *          <code>null</code>, in which case the suffix ".tmp" will be used.
   * @return The <code>File</code> object denoting the newly created file.
   * @throws IOException
   *           If a temporary directory not found or other I/O exception occurred.
   */
  public static File createTempFile(String prefix, String suffix) throws IOException {
    String tempDirPath = System.getProperty("java.io.tmpdir");
    if (tempDirPath == null)
      tempDirPath = System.getProperty("user.home");
    if (tempDirPath == null)
      throw new IOException("could not find temporary directory");
    File tempDir = new File(tempDirPath);
    if (!tempDir.isDirectory())
      throw new IOException("temporary directory not available");
    return File.createTempFile(prefix, suffix, tempDir);
  }

  /**
   * Deletes a given directory, including all its subdirectories and files. Returns
   * <code>true</code> if the deletion was successful, otherwise returns <code>false</code>. In
   * case of unsuccessful deletion, calls <code>deleteOnExit()</code> method to request that files
   * and subdirs be deleted when the virtual machine terminates.
   * 
   * @param dir
   *          The given directory to be deleted.
   * @return <code>true</code> if the deletion was successful, otherwise <code>false</code>.
   * @throws java.io.IOException
   *           If any I/O exception occurs.
   */
  public static boolean deleteDirectory(File dir) throws IOException {
    boolean done = true;
    // list immediate files/subdirs in this dir
    File[] fileList = dir.listFiles();
    // first, delete plain files and sub-directories (recursive)
    for (int i = 0; i < fileList.length; i++) {
      File entry = fileList[i];
      if (entry.isDirectory())
        done = deleteDirectory(entry);
      else if (!entry.delete()) {
        entry.deleteOnExit();
        done = false;
      }
    }
    // second, delete the root dir itself
    if (!dir.delete()) {
      dir.deleteOnExit();
      done = false;
    }
    return done;
  }

  /**
   * Extracts all files in a given JAR directory (including all its subdirectories) from a given JAR
   * file to a given target directory.
   * 
   * @param jarFile
   *          The given JAR file.
   * @param dirPath
   *          The given JAR directory.
   * @param targetDir
   *          The given target directory.
   * @return Total number of bytes extracted.
   * @throws IOException
   *           If any I/O exception occurred.
   */
  public static long extractDirectoryFromJar(JarFile jarFile, String dirPath, File targetDir)
          throws IOException {
    return extractFilesFromJar(jarFile, targetDir, new DirFileFilter(dirPath, null));
  }

  /**
   * Extracts all files that have a given extension from a given JAR file to a given target
   * directory. To extract files without extension, use <code>null</code> as the
   * <code>fileExt</code> parameter.
   * 
   * @param jarFile
   *          The given JAR file.
   * @param fileExt
   *          The given file extension.
   * @param targetDir
   *          The given target directory.
   * @return Total number of bytes extracted.
   * @throws IOException
   *           If any I/O exception occurred.
   */
  public static long extractFilesWithExtFromJar(JarFile jarFile, String fileExt, File targetDir)
          throws IOException {
    return extractFilesFromJar(jarFile, targetDir, new DirFileFilter(null, fileExt));
  }

  /**
   * Extracts all files from a given JAR file to a given target directory.
   * 
   * @param jarFile
   *          The given JAR file.
   * @param targetDir
   *          The given target directory.
   * @return Total number of bytes extracted.
   * @throws IOException
   *           If any I/O exception occurred.
   */
  public static long extractFilesFromJar(JarFile jarFile, File targetDir) throws IOException {
    return extractFilesFromJar(jarFile, targetDir, null);
  }

  /**
   * Extracts files from a given JAR file to a given target directory, based on a given
   * <code>FileFilter</code> object.
   * 
   * @param jarFile
   *          The given JAR file.
   * @param targetDir
   *          The given target directory.
   * @param filter
   *          The given <code>FileFilter</code> object.
   * @return Total number of bytes extracted.
   * @throws IOException
   *           If any I/O exception occurred.
   */
  public static long extractFilesFromJar(JarFile jarFile, File targetDir, FileFilter filter)
          throws IOException {
    long totalBytes = 0;
    byte[] block = new byte[4096];
    Enumeration<JarEntry> jarList = jarFile.entries();
    while (jarList.hasMoreElements()) {
      JarEntry jarEntry = jarList.nextElement();
      if (!jarEntry.isDirectory()) {
        // check that file is accepted
        if (filter != null && !filter.accept(new File(jarEntry.getName())))
          continue;
        // extract file
        File file = new File(targetDir, jarEntry.getName());
        // make sure the file directory exists
        File dir = file.getParentFile();
        if (!dir.exists() && !dir.mkdirs())
          throw new IOException("could not create directory " + dir.getAbsolutePath());
        BufferedInputStream iStream = null;
        BufferedOutputStream oStream = null;
        try {
          iStream = new BufferedInputStream(jarFile.getInputStream(jarEntry));
          oStream = new BufferedOutputStream(new FileOutputStream(file));
          int bCount = 0;
          while ((bCount = iStream.read(block)) > 0) {
            totalBytes += bCount;
            oStream.write(block, 0, bCount);
          }
          iStream.close();
          oStream.close();
        } finally {
          // close streams
          if (iStream != null) {
            try {
              iStream.close();
            } catch (Exception e) {
            }
          }
          if (oStream != null) {
            try {
              oStream.close();
            } catch (Exception e) {
            }
          }
        }
      }
    }
    return totalBytes;
  }

  /**
   * Constructs an absolute path of a given object, located in a given root directory, based on its
   * relative path in this directory.
   * 
   * @param rootDir
   *          The given root directory.
   * @param relativePath
   *          The given relative path of the object.
   * @return The absolute path for the given object, located in the given root directory.
   */
  public static String getAbsolutePath(File rootDir, String relativePath) {
    File object = new File(rootDir, relativePath);
    return object.getAbsolutePath();
  }

  /**
   * Identifies a given file name extension.
   * 
   * @param fileName
   *          The given file name.
   * @return The file name extension
   */
  public static String getFileNameExtension(String fileName) {
    StringBuffer buffer = new StringBuffer();
    int begIndex = fileName.lastIndexOf('.');
    if (begIndex > 0) {
      buffer.append('.');
      for (int i = begIndex + 1; i < fileName.length(); i++) {
        char ch = fileName.charAt(i);
        if (Character.isLetterOrDigit(ch))
          buffer.append(ch);
        else
          break;
      }
    }
    return buffer.toString();
  }

  /**
   * Returns file size for a given file.
   * 
   * @param fileLocation
   *          The given file location - local file path or URL.
   * @return The given file size, if the specified file can be accessed, -1 otherwise.
   */
  public static long getFileSize(String fileLocation) {
    long fileSize = 0;
    // choose file size method: local FS or HTTP
    File file = new File(fileLocation);
    if (file.isFile())
      fileSize = file.length();
    else {
      try {
        URL fileUrl = new URL(fileLocation);
        URLConnection urlConn = fileUrl.openConnection();
        // See https://issues.apache.org/jira/browse/UIMA-1746
        urlConn.setUseCaches(false);
        fileSize = urlConn.getContentLength();
      } catch (IOException e) {
        fileSize = -1;
      }
    }
    return fileSize;
  }

  /**
   * Constructs a relative path of a given object, located in a given root directory, based on its
   * absolute path.
   * 
   * @param rootDir
   *          The given root directory.
   * @param absolutePath
   *          The given absolute path of the object.
   * @return The relative path of the given object, located in the given root directory.
   */
  public static String getRelativePath(File rootDir, String absolutePath) {
    String rootDirPath = rootDir.getAbsolutePath().replace('\\', '/');
    String objectPath = absolutePath.replace('\\', '/');
    if (objectPath.startsWith(rootDirPath))
      objectPath = objectPath.substring(rootDirPath.length());
    if (objectPath.startsWith("/"))
      objectPath = objectPath.substring(1);
    return objectPath;
  }

  /**
   * Makes and attempt to identify possible UTF signature (BOM) in a given sequence of bytes.
   * Returns the identified UTF signature name or <code>null</code>, if the signature could not
   * be identified. For more on UTF and its signatures see <a
   * href="http://www.unicode.org/faq/utf_bom.html" target="_blank"> FAQ - UTF and BOM</a>.
   * 
   * @param prefix
   *          The given sequence of bytes to analyze.
   * @param length
   *          The length of the given sequence of bytes.
   * @return The UTF signature name or <code>null</code>, if the signature could not be
   *         identified.
   */
  public static String identifyUtfSignature(int[] prefix, int length) {
    String utfSignature = null;
    if (length == 3) {
      // check for UTF-8 signature
      if (prefix[0] == 0xEF && prefix[1] == 0xBB && prefix[2] == 0xBF)
        utfSignature = "UTF-8";
    } else if (length == 2) {
      // check for UTF-16 signature
      if (prefix[0] == 0xFE && prefix[1] == 0xFF)
        utfSignature = "UTF-16BE";
      else if (prefix[0] == 0xFF && prefix[1] == 0xFE)
        utfSignature = "UTF-16LE";
    } else if (length == 4) {
      // check for UTF-32 signature
      if (prefix[0] == 0x00 && prefix[1] == 0x00 && prefix[2] == 0xFE && prefix[3] == 0xFF)
        utfSignature = "UTF-32BE";
      else if (prefix[0] == 0xFF && prefix[1] == 0xFE && prefix[2] == 0x00 && prefix[3] == 0x00)
        utfSignature = "UTF-32LE";
    }
    return utfSignature;
  }

  /**
   * Returns <code>true</code>, if a given text file contains only ASCII characters, otherwise
   * returns <code>false</code>.
   * 
   * @param textFile
   *          The given text file.
   * @return <code>true</code>, if the given text file contains only ASCII characters,
   *         <code>false</code> otherwise.
   * @throws IOException
   *           If an I/O exception occurred.
   */
  public static boolean isAsciiFile(File textFile) throws IOException {
    boolean isAscii = true;
    FileInputStream iStream = null;
    try {
      iStream = new FileInputStream(textFile);
      isAscii = isAsciiStream(iStream);
      iStream.close();
    } catch (IOException exc) {
      isAscii = false;
      throw exc;
    } finally {
      if (iStream != null)
        try {
          iStream.close();
        } catch (Exception e) {
        }
    }
    return isAscii;
  }

  /**
   * Returns <code>true</code>, if a given input stream contains only ASCII characters, otherwise
   * returns <code>false</code>.
   * 
   * @param iStream
   *          The given input stream.
   * @return <code>true</code>, if the given input stream contains only ASCII characters,
   *         <code>false</code> otherwise.
   * @throws IOException
   *           If an I/O exception occurred.
   */
  public static boolean isAsciiStream(InputStream iStream) throws IOException {
    boolean isAscii = true;
    try {
      int nextByte = 0;
      while ((nextByte = iStream.read()) >= 0) {
        if (nextByte > 127) {
          isAscii = false;
          break;
        }
      }
    } catch (IOException exc) {
      isAscii = false;
      throw exc;
    }
    return isAscii;
  }

  /**
   * Loads a list of non-empty EOL-delimited strings from a given text stream.
   * 
   * @param iStream
   *          The given input text stream.
   * @return The array of non-empty strings loaded from the given text stream.
   * @throws IOException
   *           If any I/O exception occurred.
   */
  public static String[] loadListOfStrings(BufferedReader iStream) throws IOException {
    String[] outputArray = null;
    List<String> outputList = new ArrayList<String>();
    String line = null;
    while ((line = iStream.readLine()) != null) {
      String string = line.trim();
      if (string.length() > 0)
        outputList.add(string);
    }
    if (outputList.size() > 0) {
      outputArray = new String[outputList.size()];
      outputList.toArray(outputArray);
    }
    return (outputArray != null) ? outputArray : new String[0];
  }

  /**
   * Loads a list of non-empty EOL-delimited strings from a given text file using the default file
   * encoding.
   * 
   * @param textFile
   *          The given text file.
   * @return The array of non-empty strings loaded from the given text file.
   * @throws IOException
   *           If any I/O exception occurred.
   */
  public static String[] loadListOfStrings(File textFile) throws IOException {
    BufferedReader iStream = null;
    String[] outputArray = null;
    try {
      iStream = new BufferedReader(new InputStreamReader(new FileInputStream(textFile)));
      outputArray = loadListOfStrings(iStream);
    } catch (IOException exc) {
      throw exc;
    } finally {
      if (iStream != null) {
        try {
          iStream.close();
        } catch (Exception e) {
        }
      }
    }
    return (outputArray != null) ? outputArray : new String[0];
  }

  /**
   * Loads a list of non-empty EOL-delimited strings from a given remote text file.
   * 
   * @param textFileURL
   *          The URL of the given input text file.
   * @return The array of non-empty strings loaded from the given text file.
   * @throws IOException
   *           If any I/O exception occurred.
   */
  public static String[] loadListOfStrings(URL textFileURL) throws IOException {
    URLConnection urlConnection = textFileURL.openConnection();
    // See https://issues.apache.org/jira/browse/UIMA-1746
    urlConnection.setUseCaches(false);
    BufferedReader iStream = null;
    String[] outputArray = null;
    try {
      iStream = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
      outputArray = loadListOfStrings(iStream);
    } catch (IOException exc) {
      throw exc;
    } finally {
      if (iStream != null) {
        try {
          iStream.close();
        } catch (Exception e) {
        }
      }
    }
    return (outputArray != null) ? outputArray : new String[0];
  }

  /**
   * Loads a specified properties file from a given JAR file.
   * 
   * @param propFilePath
   *          The given properties file path in the JAR file.
   * @param jarFile
   *          The given JAR file.
   * @return <code>Properties</code> object containing loaded properties, or <code>null</code>,
   *         if the properties file was not found in the given JAR file.
   * @throws IOException
   *           If any I/O exception occurred.
   */
  public static Properties loadPropertiesFromJar(String propFilePath, JarFile jarFile)
          throws IOException {
    Properties properties = null;
    String name = propFilePath.replace('\\', '/');
    JarEntry jarEntry = jarFile.getJarEntry(name);
    if (jarEntry != null) {
      InputStream iStream = null;
      try {
        iStream = jarFile.getInputStream(jarEntry);
        properties = new Properties();
        properties.load(iStream);
      } finally {
        if (iStream != null) {
          try {
            iStream.close();
          } catch (Exception e) {
          }
        }
      }
    }
    return properties;
  }

  /**
   * Loads a text file associated with a given input stream.
   * 
   * @param iStream
   *          The given text input stream.
   * @return The content of the text file.
   * @throws IOException
   *           If any I/O exception occurs.
   */
  public static String loadTextFile(BufferedReader iStream) throws IOException {
    StringWriter buffer = null;
    PrintWriter writer = null;
    try {
      buffer = new StringWriter();
      writer = new PrintWriter(buffer);
      String line = null;
      while ((line = iStream.readLine()) != null)
        writer.println(line);
      writer.flush();
    } catch (IOException exc) {
      throw exc;
    } finally {
      if (writer != null) {
        try {
          writer.close();
        } catch (Exception e) {
        }
      }
    }
    return buffer.toString();
  }

  /**
   * Loads a given local text file using the default file encoding.
   * 
   * @return The content of the text file.
   * @param textFile
   *          The given text file.
   * @throws IOException
   *           If any I/O exception occurs.
   */
  public static String loadTextFile(File textFile) throws IOException {
    BufferedReader iStream = null;
    String content = null;
    try {
      iStream = new BufferedReader(new FileReader(textFile));
      content = loadTextFile(iStream);
    } catch (IOException exc) {
      throw exc;
    } finally {
      if (iStream != null) {
        try {
          iStream.close();
        } catch (Exception e) {
        }
      }
    }
    return content;
  }

  /**
   * Loads a given local text file using a specified file encoding.
   * 
   * @return The content of the text file.
   * @param textFile
   *          The given text file.
   * @param encoding
   *          The given text file encoding name.
   * @throws IOException
   *           If any I/O exception occurs.
   */
  public static String loadTextFile(File textFile, String encoding) throws IOException {
    BufferedReader iStream = null;
    String content = null;
    try {
      iStream = new BufferedReader(new InputStreamReader(new FileInputStream(textFile), encoding));
      content = loadTextFile(iStream);
    } catch (IOException exc) {
      throw exc;
    } finally {
      if (iStream != null) {
        try {
          iStream.close();
        } catch (Exception e) {
        }
      }
    }
    return content;
  }

  /**
   * Loads a given remote text file.
   * 
   * @param textFileURL
   *          The given text file URL.
   * @return The content of the text file.
   * @throws IOException
   *           If any I/O exception occurs.
   */
  public static String loadTextFile(URL textFileURL) throws IOException {
    URLConnection urlConnection = textFileURL.openConnection();
    // See https://issues.apache.org/jira/browse/UIMA-1746
    urlConnection.setUseCaches(false);    
    return loadTextFile(urlConnection);
  }

  /**
   * Loads a given remote text file.
   * 
   * @param urlConnection
   *          The given URL connection.
   * @return The content of the text file.
   * @throws IOException
   *           If any I/O exception occurs.
   */
  public static String loadTextFile(URLConnection urlConnection) throws IOException {
    BufferedReader iStream = null;
    String content = null;
    // See https://issues.apache.org/jira/browse/UIMA-1746
    urlConnection.setUseCaches(false);    
    try {
      iStream = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
      content = loadTextFile(iStream);
    } catch (IOException exc) {
      throw exc;
    } finally {
      if (iStream != null) {
        try {
          iStream.close();
        } catch (Exception e) {
        }
      }
    }
    return content;
  }

  /**
   * Loads a specified text file from a given JAR file.
   * 
   * @param filePath
   *          The specified text file path inside the JAR file.
   * @param jarFile
   *          The given JAR file.
   * @return The content of the text specified file, or <code>null</code>, if the text file was
   *         not found in the given JAR file.
   * @throws IOException
   *           If any I/O exception occurs.
   */
  public static String loadTextFileFromJar(String filePath, JarFile jarFile) throws IOException {
    String content = null;
    String name = filePath.replace('\\', '/');
    JarEntry jarEntry = jarFile.getJarEntry(name);
    if (jarEntry != null) {
      BufferedReader iStream = null;
      try {
        iStream = new BufferedReader(new InputStreamReader(jarFile.getInputStream(jarEntry)));
        content = loadTextFile(iStream);
      } finally {
        if (iStream != null) {
          try {
            iStream.close();
          } catch (Exception e) {
          }
        }
      }
    }
    return content;
  }

  /**
   * Converts a given input file path into a valid file URL string.
   * 
   * @param path
   *          The given file path to be converted.
   * @return The file URL string for the specified file.
   */
  public static String localPathToFileUrl(String path) {
    // get absolute path
    File file = new File(path);
    String absPath = file.getAbsolutePath().replace('\\', '/');
    // construct file URL
    StringBuffer urlBuffer = new StringBuffer("file:///");
    urlBuffer.append(absPath.replace(':', '|'));
    String fileUrlString = urlBuffer.toString().replaceAll(" ", "%20");
    URL fileUrl = null;
    try {
      fileUrl = new URL(fileUrlString);
    } catch (MalformedURLException e) {
      fileUrl = null;
    }
    return (fileUrl != null) ? fileUrl.toExternalForm() : fileUrlString;
  }

  /**
   * Moves a given source file to a given destination directory.
   * 
   * @param source
   *          The given source file.
   * @param destinationDir
   *          The given destination directory.
   * @return <code>true</code> if the move operation completed successfully, <code>false</code>
   *         otherwise.
   * @throws IOException
   *           If any I/O exception occurred.
   */
  public static boolean moveFile(File source, File destinationDir) throws IOException {
    boolean completed = false;
    File destination = new File(destinationDir, source.getName());
    if (destination.exists())
      destination.delete();
    if (copyFile(source, destination)) {
      completed = source.delete();
    }
    return completed;
  }

  /**
   * Replaces all occurrences of a given regular expression with a given string in a given text file.
   * Supports only 1 file encoding - ASCII - for all general text files. Supports 2 encodings -
   * UTF-8 (ASCII) and UTF-16 for XML files.
   * 
   * @param textFile
   *          The given text file.
   * @param subStringRegex
   *          The given regular expression string to be replaced.
   * @param replacement
   *          The given replacement string.
   * @return The number of actual string replacements performed.
   * @throws IOException
   *           If any I/O exception occurs.
   */
  public static int replaceStringInFile(File textFile, String subStringRegex, String replacement)
          throws IOException {
    int counter = 0;
    // for general text file - supporting ASCII encoding only
    String encoding = "ASCII";
    // check file extension
    int extIndex = textFile.getName().lastIndexOf('.');
    String fileExt = (extIndex > 0) ? textFile.getName().substring(extIndex) : null;
    if (".xml".equalsIgnoreCase(fileExt)) {
      // for XML file - supporting UTF-8 (ASCII) and UTF-16 encodings
      String xmlEncoding = XMLUtil.detectXmlFileEncoding(textFile);
      if (xmlEncoding != null) {
        encoding = xmlEncoding;
      } else {
        encoding = "UTF-8";
      }
    }
    // load text file, using supported encoding
    String fileContent = loadTextFile(textFile, encoding);
    BufferedReader sReader = null;
    PrintStream fStream = null;
    boolean done = false;
    File backupFile = null;
    // get pattern for given regex
    Pattern pattern = Pattern.compile(subStringRegex);
    // format replacement string
    String replaceWith = StringUtil.toRegExpReplacement(replacement);
    try {
      // save backup copy of input file
      backupFile = new File(textFile.getAbsolutePath() + ".bak");
      if (backupFile.exists())
        backupFile.delete();
      if (!textFile.renameTo(backupFile))
        throw new IOException("can't save backup copy of " + textFile.getAbsolutePath());
      sReader = new BufferedReader(new StringReader(fileContent));
      fStream = new PrintStream(new FileOutputStream(textFile), true, encoding);
      String srcLine = null;
      while ((srcLine = sReader.readLine()) != null) {
        // count pattern matches in the source string
        Matcher matcher = pattern.matcher(srcLine);
        while (matcher.find())
          counter++;
        // replace all pattern matches in the source string
        String resLine = srcLine.replaceAll(subStringRegex, replaceWith);
        fStream.println(resLine);
      }
      fStream.close();
      done = true;
    } catch (IOException exc) {
      throw exc;
    } catch (Throwable err) {
      if (err instanceof IOException)
        throw new IOException(err.toString() + " in " + textFile.getAbsolutePath());
      throw new RuntimeException(err.toString() + " in " + textFile.getAbsolutePath());
    } finally {
      if (sReader != null) {
        try {
          sReader.close();
        } catch (Exception e) {
        }
      }
      if (fStream != null) {
        try {
          fStream.close();
        } catch (Exception e) {
        }
      }
      if (done) {
        // remove backup file
        backupFile.delete();
      } else {
        // restore input file
        textFile.delete();
        backupFile.renameTo(textFile);
      }
    }
    return counter;
  }

  /**
   * Sorts a given list of files by the 'last modified' time in the descending order.
   * 
   * @param fileList
   *          The given list of files.
   * @return The list of files sorted by the 'last modified' time in the descending order.
   */
  public static SortedSet<File> sortFileListByTime(Collection<File> fileList) {
    TreeSet<File> set = new TreeSet<File>(new FileTimeComparator());
    set.addAll(fileList);
    return set;
  }

  /**
   * Zips the contents of a given directory. The output ZIP file, by default, is created in the
   * given directory, and its name is the given directory name with 'zip' extension.
   * 
   * @param dir2zip
   *          The given directory to be zipped.
   * @return The output ZIP file.
   * @throws IOException
   *           If any I/O exception occurred.
   */
  public static File zipDirectory(File dir2zip) throws IOException {
    // construct zipped file path
    String zipFileName = dir2zip.getName() + ".zip";
    File zipFile = new File(dir2zip, zipFileName);
    return zipDirectory(dir2zip, zipFile);
  }

  /**
   * Zips the contents of a given directory to a given output ZIP file.
   * 
   * @param dir2zip
   *          The given directory to be zipped.
   * @param zippedFile
   *          The given output ZIP file.
   * @return The output ZIP file.
   * @throws IOException
   *           If any I/O exception occurred.
   */
  public static File zipDirectory(File dir2zip, File zippedFile) throws IOException {
    ZipOutputStream zoStream = null;
    try {
      // open compressed output stream
      zoStream = new ZipOutputStream(new FileOutputStream(zippedFile));
      // add output zip file to exclusions
      File[] excludeFiles = new File[1];
      excludeFiles[0] = zippedFile;
      zipDirectory(dir2zip, zoStream, dir2zip, excludeFiles);
    } finally {
      if (zoStream != null) {
        try {
          zoStream.close();
        } catch (Exception e) {
        }
      }
    }
    return zippedFile;
  }

  /**
   * Zips the contents of a given directory to a given ZIP output stream. Paths of file entries in
   * the ZIP stream are taken relatively to a given reference directory. If the reference directory
   * is <code>null</code>, the file paths are taken relatively to the given directory to be
   * zipped. The method allows to specify the list of files (or dirs) that should not be zipped.
   * 
   * @param dir2zip
   *          The given directory to be zipped.
   * @param zoStream
   *          The given ZIP output stream.
   * @param referenceDir
   *          The given reference directory or <code>null</code>.
   * @param excludeFiles
   *          The given list of files (or dirs) that should not be zipped.
   * @return The ZIP output stream.
   * @throws IOException
   *           If any I/O exception occurred.
   */
  public static ZipOutputStream zipDirectory(File dir2zip, ZipOutputStream zoStream,
          File referenceDir, File[] excludeFiles) throws IOException {
    byte[] block = new byte[4096];
    int inBytes = 0;
    FileInputStream iStream = null;
    try {
      // get list of all files/dirs in the given directory
      File[] dirFileList = dir2zip.listFiles();
      // compress all files and sub-dirs
      for (int i = 0; i < dirFileList.length; i++) {
        File entry = dirFileList[i];
        // check if this entry is not in the list of exclusions
        boolean isExcluded = false;
        for (int n = 0; n < excludeFiles.length; n++) {
          if (entry.equals(excludeFiles[n])) {
            isExcluded = true;
            break;
          }
        }
        if (isExcluded)
          continue;
        // for each file - add ZipEntry and compress the file
        if (entry.isFile()) {
          // open input stream
          iStream = new FileInputStream(entry);
          // put ZipEntry for the file
          String zipEntryName = (referenceDir != null) ? getRelativePath(referenceDir, entry
                  .getAbsolutePath()) : getRelativePath(dir2zip, entry.getAbsolutePath());
          ZipEntry zipEntry = new ZipEntry(zipEntryName);
          zoStream.putNextEntry(zipEntry);
          // read input stream and write to output stream
          while ((inBytes = iStream.read(block)) > 0)
            zoStream.write(block, 0, inBytes);
          // close input stream
          iStream.close();
        } else if (entry.isDirectory()) // zip sub-dir recursively
          zipDirectory(entry, zoStream, referenceDir, excludeFiles);
      }
    } finally {
      if (iStream != null) {
        try {
          iStream.close();
        } catch (Exception e) {
        }
      }
    }
    return zoStream;
  }

  /**
   * Zips a given file. The output ZIP file, by default, is created in the same directory, as the
   * given input file, and has the same name, as the given input file with 'zip' extension.
   * 
   * @param file2zip
   *          The file to be zipped.
   * @return The output ZIP file.
   * @throws IOException
   *           If any I/O exception occurred.
   */
  public static File zipFile(File file2zip) throws IOException {
    // construct zipped file path
    String zipFileName = file2zip.getName();
    int extIndex = zipFileName.lastIndexOf('.');
    zipFileName = (extIndex >= 0) ? zipFileName.substring(0, extIndex) + ".zip" : zipFileName
            + ".zip";
    File zipFile = new File(file2zip.getParentFile(), zipFileName);
    return zipFile(file2zip, zipFile);
  }

  /**
   * Zips a given file to a given output ZIP file.
   * 
   * @param file2zip
   *          The file to be zipped.
   * @param zippedFile
   *          The given output ZIP file.
   * @return The output ZIP file.
   * @throws IOException
   *           If any I/O exception occurred.
   */
  public static File zipFile(File file2zip, File zippedFile) throws IOException {
    byte[] block = new byte[4096];
    int inBytes = 0;
    FileInputStream iStream = null;
    ZipOutputStream oStream = null;
    try {
      // open input stream
      iStream = new FileInputStream(file2zip);
      // create ZipEntry, using input file name
      ZipEntry zipEntry = new ZipEntry(file2zip.getName());
      // open compressed output stream
      oStream = new ZipOutputStream(new FileOutputStream(zippedFile));
      // add new ZipEntry
      oStream.putNextEntry(zipEntry);
      // read input stream and write to output stream
      while ((inBytes = iStream.read(block)) > 0)
        oStream.write(block, 0, inBytes);
    } finally {
      if (iStream != null) {
        try {
          iStream.close();
        } catch (Exception e) {
        }
      }
      if (oStream != null) {
        try {
          oStream.close();
        } catch (Exception e) {
        }
      }
    }
    return zippedFile;
  }
}
