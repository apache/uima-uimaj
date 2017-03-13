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

import java.io.IOException;
import java.lang.reflect.Method;

/**
 * The <code>BrowserUtil</code> class provides one static method -
 * <code>openUrlInDefaultBrowser</code>, which opens the given URL in the default web browser for
 * the current user of the system. Current implementation supports Windows, Linux and some Unix
 * systems.
 * 
 * 
 */

public class BrowserUtil {

  /** The internal ID of the OS we are running on */
  private static int __osId;

  /** The command that launches system browser */
  private static String[] __browserLauncher;

  /** JVM constant for any Windows NT JVM */
  private static final int WINDOWS_NT = 0;

  /** JVM constant for any Windows 9x JVM */
  private static final int WINDOWS_9x = 1;

  /** JVM constant for MacOS JVM */
  private static final int MAC_OS = 2;

  /** JVM constant for any other platform */
  private static final int OTHER = 3;

  /**
   * The first parameter that needs to be passed into Runtime.exec() to open the default web browser
   * on Windows.
   */
  private static final String FIRST_WINDOWS_PARAMETER = "/c";

  /** The second parameter for Runtime.exec() on Windows. */
  private static final String SECOND_WINDOWS_PARAMETER = "start";

  /**
   * The third parameter for Runtime.exec() on Windows. This is a "title" parameter that the command
   * line expects. Setting this parameter allows URLs containing spaces to work.
   */
  private static final String THIRD_WINDOWS_PARAMETER = "\"\"";

  /**
   * An initialization block that determines the operating system and the browser launcher command.
   */
  static {
    String osName = System.getProperty("os.name");
    if (osName.startsWith("Windows")) {
      if (osName.indexOf("9") > -1) {
        __osId = WINDOWS_9x;
        __browserLauncher = new String[] { "command.com" };
      } else {
        __osId = WINDOWS_NT;
        __browserLauncher = new String[] { "cmd.exe" };
      }
    } else if (osName.startsWith("Mac OS")) {
      __osId = MAC_OS;
      __browserLauncher = null;
    } else {
      __osId = OTHER;
      __browserLauncher = new String[] { "mozilla", "firefox", "konqueror", "opera", "netscape" };
    }
  }

  /**
   * For testing only.
   * 
   * @param args
   *          [url_to_open]
   */
  public static void main(String args[]) {
    String url = (args.length > 0) ? args[0] : "http://apache.org";
    try {
      Process process = BrowserUtil.openUrlInDefaultBrowser(url);
      if(process != null) {
        process.waitFor();
      }
    } catch (Exception e) {
      System.err.println("Error in BrowserUtil.main():");
      e.printStackTrace(System.err);
    }
  }

  /**
   * This class should be never be instantiated; this just ensures so.
   */
  private BrowserUtil() {
  }

  /**
   * Attempts to open the default web browser to the given URL.
   * 
   * @param url
   *          The URL to open
   * @return
   *        Returns the process browser object or null if no browser could be found. 
   *        On MacOs null is returned in any case.
   *        
   * @throws Exception
   *           If the available web browser does not run
   */
  public static Process openUrlInDefaultBrowser(String url) throws Exception {

    Process process = null;

    switch (__osId) {
      case WINDOWS_NT:
      case WINDOWS_9x:
        // Add quotes around the URL to allow ampersands and other
        // special characters to work.
        process = Runtime.getRuntime().exec(
                new String[] { __browserLauncher[0], FIRST_WINDOWS_PARAMETER,
                    SECOND_WINDOWS_PARAMETER, THIRD_WINDOWS_PARAMETER, '"' + url + '"' });
        // This avoids a memory leak on some versions of Java on
        // Windows. That's hinted at in
        // <http://developer.java.sun.com/developer/qow/archive/68/>.
        try {
          process.waitFor();
          process.exitValue();
        } catch (InterruptedException ie) {
          throw new IOException("InterruptedException while launching browser: " + ie.getMessage());
        }
        break;
      case MAC_OS:
        Class<?> fileMgr = Class.forName("com.apple.eio.FileManager");
        Method openURL = fileMgr.getDeclaredMethod("openURL", new Class[] { String.class });
        openURL.invoke(null, new Object[] { url });
        break;
      case OTHER:
        // check for available browsers
        boolean browserAvailableBrowser = false;
        int i = 0;
        while (i < __browserLauncher.length && !browserAvailableBrowser) {
          // check if current browser is available
          process = Runtime.getRuntime().exec(new String[] { "which", __browserLauncher[i] });
          try {
            int exitCode = process.waitFor();
            if (exitCode == 0) {
              browserAvailableBrowser = true;
              process = Runtime.getRuntime().exec(new String[] { __browserLauncher[i], url });
            }
          } catch (InterruptedException ie) {
            throw new IOException("InterruptedException while launching browser: "
                    + ie.getMessage());
          }
        }
        // no browser found, return null
        if(!browserAvailableBrowser) {
          process = null;
        }
        break;
    }
    return process;
  }
}
