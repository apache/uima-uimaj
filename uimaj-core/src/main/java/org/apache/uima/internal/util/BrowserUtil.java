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

/**
 * The <code>BrowserUtil</code> class provides one static method - 
 * <code>openUrlInDefaultBrowser</code>, which opens the given URL in the 
 * default web browser for the current user of the system. Current 
 * implementation supports Windows, Linux and some Unix systems.
 *
 * 
 */

public class BrowserUtil {

	/** The internal ID of the OS we are running on */
	private static int __osId;

	/** The command that launches system browser */
	private static String __browserLauncher;

	/** JVM constant for any Windows NT JVM */
	private static final int WINDOWS_NT = 0;
	
	/** JVM constant for any Windows 9x JVM */
	private static final int WINDOWS_9x = 1;

	/** JVM constant for Linux JVM */
	private static final int LINUX = 2;

	/** JVM constant for any other platform */
	private static final int OTHER = -1;

	/**
	 * The first parameter that needs to be passed into Runtime.exec() to open 
	 * the default web browser on Windows.
	 */
    private static final String FIRST_WINDOWS_PARAMETER = "/c";
    
    /** The second parameter for Runtime.exec() on Windows. */
    private static final String SECOND_WINDOWS_PARAMETER = "start";
    
    /**
     * The third parameter for Runtime.exec() on Windows.  This is a "title"
     * parameter that the command line expects.  Setting this parameter allows
     * URLs containing spaces to work.
     */
    private static final String THIRD_WINDOWS_PARAMETER = "\"\"";
	
    /**
     * The shell parameters for Mozilla, which assumes that the 'default' 
     * Mozilla profile exists.
     */
    private static final String MOZILLA_REMOTE_PARAMETER = "-remote";
    private static final String MOZILLA_OPEN_PARAMETER_START = "openURL(";
    private static final String MOZILLA_OPEN_PARAMETER_END = ")";
    
	/**
	 * The shell parameters for Netscape that opens a given URL in an 
	 * already-open copy of Netscape on many command-line systems.
	 */
	private static final String NETSCAPE_REMOTE_PARAMETER = "-remote";
	private static final String NETSCAPE_OPEN_PARAMETER_START = "'openURL(";
	private static final String NETSCAPE_OPEN_PARAMETER_END = ")'";
	
	/**
	 * An initialization block that determines the operating system and the 
	 * browser launcher command.
	 */
	static {
		String osName = System.getProperty( "os.name" );
		if( osName.startsWith( "Windows" ) ) {
			if( osName.indexOf( "9" ) > -1 ) {
				__osId = WINDOWS_9x;
				__browserLauncher = "command.com";
			} else {
				__osId = WINDOWS_NT;
				__browserLauncher = "cmd.exe";
			}
		} else if( osName.startsWith( "Linux" ) ) {
			__osId = LINUX;
			__browserLauncher = "mozilla";
		} else {
			__osId = OTHER;
			__browserLauncher = "netscape";
		}
	}

	/**
	 * For testing only.
	 * 
	 * @param args [url_to_open]
	 */
	public static void main( String args[] ) {
		String url = (args.length > 0) ? args[0] : "http://www.ibm.com";
		try {
			Process process = BrowserUtil.openUrlInDefaultBrowser( url );
			process.waitFor();
		} catch( Exception e ) {
			System.err.println( "Error in BrowserUtil.main():" );
			e.printStackTrace( System.err );
		}
	}
	/**
	 * This class should be never be instantiated; this just ensures so.
	 */
	private BrowserUtil() { }
	/**
	 * Attempts to open the default web browser to the given URL.
	 * @param url The URL to open
	 * @throws IOException If the web browser could not be located or does 
	 *         not run
	 */
	public static Process openUrlInDefaultBrowser( String url ) 
		throws IOException {
		
		Process process = null;
		
		switch( __osId ) {
		    case WINDOWS_NT:
		    case WINDOWS_9x:
		    	// Add quotes around the URL to allow ampersands and other 
		    	// special characters to work.
				process = Runtime.getRuntime().exec(
					new String[] { __browserLauncher,
						FIRST_WINDOWS_PARAMETER,
						SECOND_WINDOWS_PARAMETER,
						THIRD_WINDOWS_PARAMETER,
						'"' + url + '"' });
				// This avoids a memory leak on some versions of Java on 
				// Windows. That's hinted at in 
				// <http://developer.java.sun.com/developer/qow/archive/68/>.
				try {
					process.waitFor();
					process.exitValue();
				} catch (InterruptedException ie) {
					throw new IOException(
						"InterruptedException while launching browser: " + 
						ie.getMessage());
				}
				break;
			case LINUX:
				// Assume that Mozilla is installed
				// First, attempt to open the URL in a currently running 
				// session of Mozilla
				process = Runtime.getRuntime().exec(
					new String[] { __browserLauncher,
						MOZILLA_REMOTE_PARAMETER,
						MOZILLA_OPEN_PARAMETER_START +
						url +
						MOZILLA_OPEN_PARAMETER_END 
					} );
				try {
					int exitCode = process.waitFor();
					if( exitCode != 0 ) { 
						// Mozilla was not open
						// Try opening new browser window
						process = Runtime.getRuntime().exec(
							new String[] { __browserLauncher, url });
/*
						// Attempt to open the URL using 'default' Mozilla 
						// profile
						process = Runtime.getRuntime().exec(
							new String[] { (String)browser,
								MOZILLA_PROFILE_OPTION,
								MOZILLA_DEFAULT_PROFILE,
								url
							} );
 */
					}
				} catch( InterruptedException ie ) {
					throw new IOException(
						"InterruptedException while launching browser: " +	
						ie.getMessage());
				}
				break;
			case OTHER:
				// Assume that we're on Unix and that Netscape is installed
				// First, attempt to open the URL in a currently running 
				// session of Netscape
				process = Runtime.getRuntime().exec(
					new String[] { __browserLauncher,
						NETSCAPE_REMOTE_PARAMETER,
						NETSCAPE_OPEN_PARAMETER_START +
						url +
						NETSCAPE_OPEN_PARAMETER_END });
				try {
					int exitCode = process.waitFor();
					if (exitCode != 0) {	// if Netscape was not open
						Runtime.getRuntime().exec(
							new String[] { __browserLauncher, url });
					}
				} catch (InterruptedException ie) {
					throw new IOException(
						"InterruptedException while launching browser: " + 
						ie.getMessage());
				}
				break;
			default:
				// This should never occur, but if it does, we'll try the 
				// simplest thing possible
				process = Runtime.getRuntime().exec(
					new String[] { __browserLauncher, url });
				break;
		}
		return process;
	}
}
