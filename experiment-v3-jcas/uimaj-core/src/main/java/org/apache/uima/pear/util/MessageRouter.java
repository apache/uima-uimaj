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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The <code>MessageRouter</code> class facilitates intra-process message routing. It provides
 * application classes with convenient access to the message channels via the
 * <code>PrintWriter</code> class. The <code>MessageRouter</code> class, by default, defines 2
 * standard message channels - for standard output and standard error messages. Applications can
 * publish their standard output and standard error messages using the <code>outWriter()</code>
 * and <code>errWriter()</code> methods correspondingly. <br>The <code>MessageRouter</code>
 * class distributes the messages to a number of message channel listeners, added by applications.
 * Standard message channel listeners should implement the
 * <code>MessageRouter.StdChannelListener</code> interface. The <code>MessageRouter</code> class
 * collects all published messages. When a new message channel listener is added, it receives all
 * collected messages from the message history.
 * <p>
 * The <code>MessageRouter</code> code runs in a separate thread that should be started and
 * terminated by applications. Applications should use the <code>start()</code> and
 * <code>terminate()</code> methods to start and terminate the <code>MessageRouter</code> thread
 * correspondingly. <br>For terminology see the <a
 * href="http://www.eaipatterns.com/MessageRouter.html"> Enterprise Integration Patterns</a> book.
 * </p>
 * 
 */

public class MessageRouter implements Runnable {
  /**
   * The <code>StdChannelListener</code> interface declares methods that should be implemented by
   * each standard message channel listener.
   * 
   */
  public static interface StdChannelListener {
    public void errMsgPosted(String errMsg);

    public void outMsgPosted(String outMsg);
  }

  // constants
  private static final String OUT_MSG_ID = "OUT";

  private static final String ERR_MSG_ID = "ERR";

  private static final long WAITING_TIME = 5;

  // attributes
  private StringWriter _errStream;

  private StringBuffer _errBuffer;

  private PrintWriter _errWriter;

  private int _errOffset;

  private StringWriter _outStream;

  private StringBuffer _outBuffer;

  private PrintWriter _outWriter;

  private int _outOffset;

  private boolean _terminated = false;

  private Thread _thread;

  private List<String> _stdHistory = new ArrayList<String>();

  private List<StdChannelListener> _stdListeners = new ArrayList<StdChannelListener>();

  /**
   * Default constructor for the <code>MessageRouter</code> class. This constructor allocates all
   * resources, but does not start the main service thread. Applications should start the
   * <code>MessageRouter</code> thread using the <code>start()</code> method.
   */
  public MessageRouter() {
    _errStream = new StringWriter();
    _errBuffer = _errStream.getBuffer();
    _errWriter = new PrintWriter(_errStream);
    _errOffset = _errBuffer.length();
    _outStream = new StringWriter();
    _outBuffer = _outStream.getBuffer();
    _outWriter = new PrintWriter(_outStream);
    _outOffset = _outBuffer.length();
    _thread = new Thread(this, "MessageRouter");
  }

  /**
   * Adds a given object, implementing the <code>StdChannelListener</code> interface, to the list
   * of standard message channel listeners. Sends to the new listener all previously collected
   * messages for this channel.
   * 
   * @param listener
   *          The given new standard message channel listener.
   */
  public synchronized void addChannelListener(StdChannelListener listener) {
    if (!_stdListeners.contains(listener)) {
      if (_stdHistory.size() > 0) {
        // send previous messages from the queue
        Iterator<String> list = _stdHistory.iterator();
        while (list.hasNext()) {
          String entry = list.next();
          // extract message itself
          String message = entry.substring(4);
          // send message to appropriate channel
          if (entry.startsWith(ERR_MSG_ID))
            listener.errMsgPosted(message);
          else
            listener.outMsgPosted(message);
        }
      }
      _stdListeners.add(listener);
    }
  }

  /**
   * @return Current number of standard channel listeners.
   */
  public int countStdChannelListeners() {
    return _stdListeners.size();
  }

  /**
   * @return <code>true</code>, if the router thread is running, <code>false</code> otherwise.
   */
  public boolean isRunning() {
    return _thread.isAlive();
  }

  /**
   * Removes a given <code>StdChannelListener</code> object from the list of standard channel
   * listeners.
   * 
   * @param listener
   *          The <code>StdChannelListener</code> object to be removed from the list.
   */
  public synchronized void removeChannelListener(StdChannelListener listener) {
    _stdListeners.remove(listener);
  }

  /**
   * Implements the main service method that runs in a separate thread.
   */
  public void run() {
    boolean terminated = false;
    String errMessage = null;
    String outMessage = null;
    while (!terminated) {
      // check ERR message
      synchronized (_errStream) {
        if (_errBuffer.length() > _errOffset) {
          errMessage = _errBuffer.substring(_errOffset);
          _errOffset = _errBuffer.length();
        }
      }
      // check OUT message
      synchronized (_outStream) {
        if (_outBuffer.length() > _outOffset) {
          outMessage = _outBuffer.substring(_outOffset);
          _outOffset = _outBuffer.length();
        }
      }
      synchronized (this) {
        // distribute standard messages
        if (errMessage != null || outMessage != null) {
          // add messages to history list
          if (errMessage != null)
            _stdHistory.add(ERR_MSG_ID + "^" + errMessage);
          if (outMessage != null)
            _stdHistory.add(OUT_MSG_ID + "^" + outMessage);
          // send messages to listeners
          Iterator<StdChannelListener> list = _stdListeners.iterator();
          while (list.hasNext()) {
            StdChannelListener client = list.next();
            if (errMessage != null)
              client.errMsgPosted(errMessage);
            if (outMessage != null)
              client.outMsgPosted(outMessage);
          }
          errMessage = null;
          outMessage = null;
        }
        // check termination
        terminated = _terminated;
      }
      if (!terminated) {
        // sleep WAITING_TIME
        try {
          Thread.sleep(WAITING_TIME);
        } catch (Exception e) {
        }
      }
    }
  }

  /**
   * @return The standard error message channel writer.
   */
  public PrintWriter errWriter() {
    return _errWriter;
  }

  /**
   * @return The standard output message channel writer.
   */
  public PrintWriter outWriter() {
    return _outWriter;
  }

  /**
   * Starts the main service thread.
   */
  public void start() {
    _thread.start();
  }

  /**
   * Terminates the main service thread.
   */
  public void terminate() {
    _errWriter.flush();
    _outWriter.flush();
    synchronized (this) {
      _terminated = true;
    }
    try {
      _thread.join();
    } catch (Exception e) {
    }
  }
}
