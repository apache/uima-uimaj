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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The <code>ProcessUtil</code> class encapsulates utilities that help in dealing with
 * <code>Process</code> objects.
 * 
 */

public class ProcessUtil {
  /**
   * The <code>StdStreamListener</code> interface defines 2 methods that should be implemented by
   * a standard stream listener to get console messages ('stderr' and 'stdout' streams) printed by
   * selected process.
   * 
   */
  public static interface StdStreamListener {
    public void errMsgPrinted(String errMsg);

    public void outMsgPrinted(String outMsg);
  } // end of StdStreamListener

  /**
   * The <code>ProcessUtil.Runner</code> class allows collecting, printing and distributing
   * console output (stdout and stderr) of the specified <code>Process</code> without blocking the
   * caller. It allows adding standard stream listeners to receive messages printed to the console
   * by selected process.
   * 
   */
  public static class Runner implements Runnable {
    // Default process name
    private static final String DEF_PROC_NAME = "Process_";

    // Static attributes
    private static int __processNumber = 0;

    // Attributes
    private Process _process;

    private String _pName;

    private Thread _errThread;

    private Thread _outThread;

    private StringWriter _stdOut;

    private StringWriter _stdErr;

    private int _exitCode = Integer.MAX_VALUE;

    private boolean _printToConsole = true;

    private List<StdStreamListener> _listeners = new ArrayList<StdStreamListener>();

    /**
     * Constructor that takes a given <code>Process</code> object and assigns default process
     * name.
     * 
     * @param aProcess
     *          The given <code>Process</code> object.
     */
    public Runner(Process aProcess) {
      this(aProcess, null);
    }

    /**
     * Constructor that takes a given <code>Process</code> object and a given process name.
     * 
     * @param aProcess
     *          The given <code>Process</code> object.
     * @param procName
     *          The given process name.
     */
    public Runner(Process aProcess, String procName) {
      this(aProcess, procName, true);
    }

    /**
     * Constructor that takes a given <code>Process</code> object, a given process name, and a
     * given <code>boolean</code> flag that enables/disables console printing. If the given
     * process name is <code>null</code>, the default process name is assigned.
     * 
     * @param aProcess
     *          The given <code>Process</code> object.
     * @param procName
     *          The given process name.
     * @param printToConsole
     *          <code>boolean</code> flag that enables/disables console printing.
     */
    public Runner(Process aProcess, String procName, boolean printToConsole) {
      // get next process number
      synchronized (Runner.class) {
        __processNumber++;
      }
      _process = aProcess;
      _pName = (procName != null) ? procName : DEF_PROC_NAME + Integer.toString(__processNumber);
      _errThread = new Thread(this, "ERR@Runner");
      _outThread = new Thread(this, "OUT@Runner");
      _stdOut = new StringWriter();
      _stdErr = new StringWriter();
      _printToConsole = printToConsole;
      start();
    }

    /**
     * Adds a given object, implementing the <code>StdStreamListener</code> interface to the list
     * of standard stream listeners. Sends to the new listener previously printed standard error and
     * standard output messages.
     * 
     * @param listener
     *          The given new standard stream listener.
     */
    public synchronized void addStreamListener(StdStreamListener listener) {
      if (!_listeners.contains(listener)) {
        String errMsg = _stdErr.toString();
        if (errMsg.length() > 0)
          listener.errMsgPrinted(errMsg);
        String outMsg = _stdOut.toString();
        if (outMsg.length() > 0)
          listener.outMsgPrinted(outMsg);
        _listeners.add(listener);
      }
    }

    /**
     * @return Process exit code after the process finishes, otherwise
     *         <code>Integer.MAX_VALUE</code>.
     */
    public synchronized int getExitCode() {
      return _exitCode;
    }

    /**
     * @return Standard error messages collected during the process execution.
     */
    public synchronized String getErrOutput() {
      return _stdErr.toString();
    }

    public Process getProcess() {
      return _process;
    }

    /**
     * @return Standard output messages collected during the process execution.
     */
    public synchronized String getStdOutput() {
      return _stdOut.toString();
    }

    /**
     * Removes a given <code>StdStreamListener</code> object from the list of standard stream
     * listeners.
     * 
     * @param listener
     *          The given <code>StdStreamListener</code> object to be removed from the list.
     */
    public synchronized void removeListener(StdStreamListener listener) {
      _listeners.remove(listener);
    }

    /**
     * Implements the <code>Runnable.run()</code> method, collecting and printing standard output
     * or standard error messages during the process execution.
     */
    public void run() {
      String threadName = Thread.currentThread().getName();
      String threadId = threadName.substring(0, 3);
      BufferedReader reader = null;
      PrintWriter writer = null;
      try {
        // create appropriate standard stream reader
        reader = (threadId.equals("ERR")) ? new BufferedReader(new InputStreamReader(_process
                .getErrorStream())) : new BufferedReader(new InputStreamReader(_process
                .getInputStream()));
        // create appropriate string writer
        StringWriter sWriter = (threadId.equals("ERR")) ? _stdErr : _stdOut;
        writer = new PrintWriter(sWriter);
        // assign appropriate console stream
        PrintStream console = (threadId.equals("ERR")) ? System.err : System.out;
        String line = null;
        // read standard stream
        while ((line = reader.readLine()) != null) {
          // print to console, if specified
          if (_printToConsole)
            console.println("[" + _pName + ":" + threadId + "] " + line);
          synchronized (this) {
            // get current string writer buffer length
            int sIndex = sWriter.getBuffer().length();
            // print new line to string buffer
            writer.println(line);
            // get last printed message
            String message = sWriter.getBuffer().substring(sIndex);
            // distribute message to listeners
            Iterator<StdStreamListener> list = _listeners.iterator();
            while (message.length() > 0 && list.hasNext()) {
              StdStreamListener listener = list.next();
              if (threadId.equals("ERR"))
                listener.errMsgPrinted(message);
              else
                listener.outMsgPrinted(message);
            }
          }
        }
        int rc = _process.waitFor();
        setExitCode(rc);
        if (_printToConsole)
          System.out.println("[" + _pName + " " + threadName + "]: completed (rc = " + rc + ")");
      } catch (Throwable err) {
        if (_printToConsole)
          System.err.println("[" + _pName + " " + threadName + "]: terminated - " + err.toString());
      } finally {
        if (reader != null) {
          try {
            reader.close();
          } catch (Exception e) {
          }
        }
        if (writer != null) {
          try {
            writer.close();
          } catch (Exception e) {
          }
        }
      }
      synchronized (this) {
        this.notifyAll();
      }
    }

    /**
     * Sets a given process exit code.
     * 
     * @param exitCode
     *          The given process exit code.
     */
    private synchronized void setExitCode(int exitCode) {
      _exitCode = exitCode;
    }

    /**
     * Starts 2 threads for collecting and printing console messages during the process execution.
     */
    private void start() {
      _errThread.start();
      _outThread.start();
    }

    /**
     * Allows the caller to wait for the completion of the process.
     * 
     * @return Process exit code.
     */
    public int waitFor() {
      return waitFor(0);
    }

    /**
     * Allows the caller to wait for the completion of the process, but no longer than a given
     * timeout value.
     * 
     * @param timeout
     *          The given timeout value (ms).
     * @return Process exit code or <code>Integer.MAX_VALUE</code>, if the process has not
     *         finished yet.
     */
    public int waitFor(long timeout) {
      synchronized (this) {
        if (getExitCode() == Integer.MAX_VALUE) {
          try {
            this.wait(timeout);
          } catch (InterruptedException e) {
          }
        }
      }
      return getExitCode();
    }
  } // end of Runner
}
