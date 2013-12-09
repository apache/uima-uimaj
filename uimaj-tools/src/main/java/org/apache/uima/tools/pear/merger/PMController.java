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

package org.apache.uima.tools.pear.merger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.jar.JarFile;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.pear.tools.InstallationDescriptor;
import org.apache.uima.pear.tools.PackageBrowser;
import org.apache.uima.pear.util.FileUtil;

/**
 * The <code>PMController</code> class allows to merge several input PEAR files in one PEAR file
 * and generate an aggregate analysis engine from the components encapsulated in the input PEARs.
 * 
 * @see org.apache.uima.tools.pear.merger.PMControllerHelper
 * @see org.apache.uima.tools.pear.merger.PMUimaAgent
 */

public class PMController {
  // utility name
  static final String PEAR_MERGER = "PEAR Merger";

  // log file name
  static final String LOG_FILE = "pm.log";

  // command line argument names
  static final String AGGREGATE_NAME_ARG = "-n";

  static final String AGGREGATE_PEAR_FILE_ARG = "-f";
  
  // command line parameters
  private static File[] __pearFiles = null;

  private static String __aggregateName = null;

  private static File __aggregatePearFile = null;

  /**
   * The <code>PMLogFormatter</code> class formats messages for the log file.
   */
  static class PMLogFormatter extends SimpleFormatter {
    private boolean _firstTime = true;

    public String format(LogRecord record) {
      if (_firstTime) {
        // 1st time - print with header
        _firstTime = false;
        record.setSourceMethodName("");
        return super.format(record);
      } else {
        // print level: message
        StringBuffer buffer = new StringBuffer();
        buffer.append(record.getLevel());
        String message = record.getMessage();
        // for multi-line msg - start msg in new line
        if (message.indexOf('\n') >= 0)
          buffer.append(": \n");
        else
          // for single line msg - start msg in the same line
          buffer.append(": ");
        buffer.append(record.getMessage());
        buffer.append('\n');
        return buffer.toString();
      }
    }
  }

  // static attributes
  private static Logger __logger = null;

  private static boolean __logFileEnabled = false;
  // static Logger initialization
  static {
    // create default logger
    __logger = Logger.getLogger(PMController.class.getName());
    __logger.setUseParentHandlers(false);
  }

  // internal attributes
  private File[] _inpPearFiles = null;

  private String _outAggCompName = null;

  private File _outAggPearFile = null;

  private File _tempWorkDir = null;

  private File _outAggRootDir = null;

  private File[] _outDlgRootDirs = null;

  private InstallationDescriptor[] _dlgInstDescs = null;

  private InstallationDescriptor _outAggInstDesc = null;

  /**
   * The command line application entry point. This method expects the following command line
   * arguments:
   * <ul>
   * <li>pear_file_1 ... pear_file_n - input PEAR files (required)</li>
   * <li>-n agg_name - a name of the output aggregate analysis engine (required) </li>
   * <li>-f agg_pear_file - output aggregate PEAR file (optional). <br>
   * If the output PEAR file is not specified, the default output PEAR file is created in the
   * current working directory.</li>
    * </ul>
   * 
   * @param args
   *          pear_file_1 ... pear_file_n -n agg_name [-f agg_pear_file]
   */
  public static void main(String[] args) {
    // enable log file
    setLogFileEnabled(true);
    // parse and validate command line
    if (!parseCommandLine(args)) {
      logErrorMessage(PEAR_MERGER + " terminated: command line error");
      return;
    }
    PMController controller = null;
    try {
      // create controller object
      controller = new PMController(__pearFiles, __aggregateName, __aggregatePearFile);
      // merge input PEARs and create merged aggregate PEAR file
      if (controller.mergePears()) {
        logInfoMessage("[" + PEAR_MERGER + "]: " + "operation completed successfully");
      } else
        logInfoMessage("[" + PEAR_MERGER + "]: " + "operation failed");
    } catch (Throwable err) {
      logErrorMessage("Error in " + PEAR_MERGER + ": " + err.toString());
    } finally {
      if (controller != null) {
        try {
          controller.cleanUp();
        } catch (Exception e) {
        }
      }
    }
  }

  /**
   * Returns the instance of the class-specific <code>Logger</code> object.
   * 
   * @return The instance of the class-specific <code>Logger</code> object.
   */
  public static Logger getLogger() {
    return __logger;
  }

  /**
   * Logs a given error message to the log file and prints it to the standard error console stream.
   * 
   * @param message
   *          The given error message.
   */
  public static void logErrorMessage(String message) {
    if (__logFileEnabled)
      getLogger().severe(message);
    System.err.println(message);
  }

  /**
   * Logs a given info message to the log file and prints it to the standard output console stream.
   * 
   * @param message
   *          The given info message.
   */
  public static void logInfoMessage(String message) {
    if (__logFileEnabled)
      getLogger().info(message);
    System.out.println(message);
  }

  /**
   * Logs a given warning message to the log file and prints it to the standard error console
   * stream.
   * 
   * @param message
   *          The given warning message.
   */
  public static void logWarningMessage(String message) {
    if (__logFileEnabled)
      getLogger().warning(message);
    System.err.println(message);
  }

  /**
   * Parses and validates the command line and initializes static attributes.
   * 
   * @param args
   *          The given command line arguments.
   * @return <code>true</code>, if the command line arguments are valid, <code>false</code>
   *         otherwise.
   */
  private static boolean parseCommandLine(String[] args) {
    // verify command line args (min 4 args: p_1 p_2 -n name)
    if (args.length < 4) {
      logErrorMessage(PEAR_MERGER + " args: " + "pear_file_1 ... pear_file_n -n agg_name "
              + "[-f agg_pear_file]");
      return false;
    }
    ArrayList listOfPears = new ArrayList();
    // parse command line
    for (int i = 0; i < args.length; i++) {
      if (args[i].equals(AGGREGATE_NAME_ARG)) {
        // set aggregate name
        if (++i < args.length)
          __aggregateName = args[i];
      } else if (args[i].equals(AGGREGATE_PEAR_FILE_ARG)) {
        // set aggregate PEAR file
        if (++i < args.length)
          __aggregatePearFile = new File(args[i]);
      } else if (args[i].startsWith( "-" )) {
        logErrorMessage(PEAR_MERGER + " error: " + "unknown flag '" + args[i] + "'");
        return false;
      } else {
        // add next input PEAR file
        File inPear = new File(args[i]);
        if (inPear.isFile()) {
          // make sure this is not duplicated input file
          for (int n = 0; n < listOfPears.size(); n++) {
            try {
              String fPath = ((File) listOfPears.get(n)).getCanonicalPath();
              String inPath = inPear.getCanonicalPath();
              if (fPath.equals(inPath)) {
                logErrorMessage(PEAR_MERGER + " error: " + "duplicated input file " + args[i]);
                return false;
              }
            } catch (IOException e) {
              logErrorMessage(PEAR_MERGER + " error: " + e.toString());
              return false;
            }
          }
          listOfPears.add(inPear);
        } else {
          logErrorMessage(PEAR_MERGER + " error: " + "cannot find input file " + args[i]);
          return false;
        }
      }
    }
    // validate command line parameters and set input files
    if (listOfPears.size() < 2) {
      logErrorMessage(PEAR_MERGER + " error: " + "input PEAR files not specified");
      return false;
    } else {
      __pearFiles = new File[listOfPears.size()];
      listOfPears.toArray(__pearFiles);
    }
    if (__aggregateName == null) {
      logErrorMessage(PEAR_MERGER + " error: " + "output aggregate name not specified");
      return false;
    }
    return true;
  }

  /**
   * Enables/disables PM log file. By default, the log file is disabled.
   * 
   * @param enable
   *          if <code>true</code>, the log file is enabled, otherwise it is disabled.
   */
  public static void setLogFileEnabled(boolean enable) {
    if (enable) {
      Handler[] handlers = getLogger().getHandlers();
      if (handlers == null || handlers.length == 0) {
        // add default file handler
        try {
          FileHandler fileHandler = new FileHandler(LOG_FILE, false);
          fileHandler.setLevel(Level.ALL);
          fileHandler.setFormatter(new PMLogFormatter());
          getLogger().addHandler(fileHandler);
        } catch (Throwable err) {
          System.err.println("Error initializing log file " + PMController.class.getName() + ": "
                  + err.toString());
        }
      }
    }
    __logFileEnabled = enable;
  }

  /**
   * Constructor that takes a given array of input PEAR files, a given output component name (ID)
   * and a given output PEAR file.
   * 
   * @param inpPearFiles
   *          The given array of input PEAR files.
   * @param outCompName
   *          The given output component name (ID).
   * @param outPearFile
   *          The given output PEAR file.
   * @exception IOException
   *              If any I/O exception occurred during initialization.
   */
  public PMController(File[] inpPearFiles, String outCompName, File outPearFile) throws IOException {
    // initialize task attributes
    _inpPearFiles = inpPearFiles;
    _outAggCompName = outCompName;
    _outAggPearFile = outPearFile;
    initializeTaskAttributes();
    // log PEAR Merger task parameters
    logInfoMessage("[" + PEAR_MERGER + "]: task parameters =>");
    logInfoMessage("> Input PEARs =>");
    for (int i = 0; i < _inpPearFiles.length; i++)
      logInfoMessage(">> " + _inpPearFiles[i].getAbsolutePath());
    logInfoMessage("> Output PEAR =>");
    logInfoMessage(">> Name = " + _outAggCompName);
    logInfoMessage(">> File = " + _outAggPearFile.getAbsolutePath());
    logInfoMessage("> Output root dir: " + _outAggRootDir.getAbsolutePath());
  }

  /**
   * Deletes all temporary directories and files after the merging operation is completed.
   * 
   * @throws IOException
   *           If an I/O exception occurred.
   */
  public void cleanUp() throws IOException {
    logInfoMessage("[" + PEAR_MERGER + "]: " + "deleting temporary files");
    FileUtil.deleteDirectory(_outAggRootDir);
  }

  /**
   * Extracts all specified input PEARs to their corresponding folders inside the output root
   * folder. Returns the total size (bytes) of extracted files.
   * 
   * @return The total size of extracted files.
   * @throws IOException
   *           If any I/O exception occurred during extraction.
   */
  private long extractInputPears() throws IOException {
    long totalSize = 0;
    for (int i = 0; i < _inpPearFiles.length; i++) {
      JarFile pearFile = new JarFile(_inpPearFiles[i]);
      totalSize += FileUtil.extractFilesFromJar(pearFile, _outDlgRootDirs[i]);
    }
    return totalSize;
  }

  /**
   * Intializes the 'PEAR Merger' task attributes, based on the specified input.
   * 
   * @exception IOException
   *              If any I/O exception occurred during initialization.
   */
  private void initializeTaskAttributes() throws IOException {
    if (_outAggPearFile == null) // set default output file
      _outAggPearFile = new File(_outAggCompName + ".pear");
    // set temporary working directory
    String userHomePath = System.getProperty("user.home");
    _tempWorkDir = new File(userHomePath);
    if (!_tempWorkDir.isDirectory())
      throw new IOException(userHomePath + " directory not found");
    // set output aggregate root directory
    _outAggRootDir = new File(_tempWorkDir, _outAggCompName);
    // if output aggregate root directory exists, remove it
    if (_outAggRootDir.isDirectory()) {
      if (!FileUtil.deleteDirectory(_outAggRootDir))
        throw new IOException("cannot delete existing folder " + _outAggRootDir.getAbsolutePath());
    } else if (_outAggRootDir.isFile()) {
      if (!_outAggRootDir.delete())
        throw new IOException("cannot delete existing file " + _outAggRootDir.getAbsolutePath());
    }
    // set output delegate root directories
    _outDlgRootDirs = new File[_inpPearFiles.length];
    for (int i = 0; i < _outDlgRootDirs.length; i++) {
      String fileName = _inpPearFiles[i].getName();
      String sdirName = fileName.substring(0, fileName.lastIndexOf('.'));
      _outDlgRootDirs[i] = new File(_outAggRootDir, sdirName);
    }
    // prepare array for delegate installation descriptors
    _dlgInstDescs = new InstallationDescriptor[_inpPearFiles.length];
  }

  /**
   * Merges specified input PEARs into one PEAR, which encapsulates aggregate AE that refers to
   * input components, as delegates. Returns <code>true</code>, if the merging operation
   * completed successfully, <code>false</code> otherwise.
   * 
   * @return <code>true</code>, if the merge operation completed successfully, <code>false</code>
   *         otherwise.
   * @throws IOException
   *           If an I/O exception occurred during the merging operation.
   */
  public boolean mergePears() throws IOException {
    boolean done = false;
    // 1st step: extract delegate PEARs to their target dirs
    logInfoMessage("[" + PEAR_MERGER + "]: " + "extracting delegate PEARs ...");
    long totalSize = extractInputPears();
    logInfoMessage("[" + PEAR_MERGER + "]: " + totalSize + " bytes extracted successfully");
    // 2nd step: process files in delegate packages and create InsD objs
    for (int i = 0; i < _outDlgRootDirs.length; i++) {
      _dlgInstDescs[i] = PMControllerHelper.processDescriptors(_outDlgRootDirs[i]);
      if (_dlgInstDescs[i] == null) {
        logErrorMessage("[" + PEAR_MERGER + "]: " + "failed to process input package in "
                + _outDlgRootDirs[i] + "directory");
        break;
      }
    }
    // 3rd step: generate merged package directory structure
    File pkgDescDir = new File(_outAggRootDir, PackageBrowser.DESCRIPTORS_DIR);
    File pkgMetadataDir = new File(_outAggRootDir, PackageBrowser.METADATA_DIR);
    if (pkgDescDir.mkdirs() && pkgMetadataDir.mkdirs())
      logInfoMessage("[" + PEAR_MERGER + "]: " + "created merged package directory structure");
    else
      throw new IOException("cannot create merged package folders");
    // 4th step: generate aggregate component descriptor
    File aggDescFile = new File(pkgDescDir, _outAggCompName + ".xml");
    AnalysisEngineDescription aggDescription = PMUimaAgent.createAggregateDescription(
            _outAggCompName, _outAggRootDir, _dlgInstDescs);
    if (aggDescription != null) {
      PMUimaAgent.saveAggregateDescription(aggDescription, aggDescFile);
      logInfoMessage("[" + PEAR_MERGER + "]: " + "generated aggregate component descriptor");
      if (System.getProperty("DEBUG") != null)
        logInfoMessage(PMUimaAgent.toXmlString(aggDescription));
    } else
      throw new IOException("cannot generate aggregate component descriptor");
    // 5th step: generate merged installation descriptor
    _outAggInstDesc = PMControllerHelper.generateMergedInstallationDescriptor(_outAggRootDir,
            _outAggCompName, aggDescFile, _dlgInstDescs, _outDlgRootDirs);
    if (_outAggInstDesc != null) {
      logInfoMessage("[" + PEAR_MERGER + "]: "
              + "generated aggregate package installation descriptor");
      if (System.getProperty("DEBUG") != null)
        logInfoMessage(_outAggInstDesc.toString());
      // 6th step: create merged PEAR file
      File outPearFile = FileUtil.zipDirectory(_outAggRootDir, _outAggPearFile);
      logInfoMessage("[" + PEAR_MERGER + "]: " + "created output aggregate PEAR file - "
              + outPearFile.getAbsolutePath());
      done = true;
    }
    return done;
  }
}
