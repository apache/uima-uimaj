/* 
 Copyright 2009-2010	Regents of the University of Colorado.  
 All rights reserved. 

 Licensed under the Apache License, Version 2.0 (the "License"); 
 you may not use this file except in compliance with the License. 
 You may obtain a copy of the License at 

 http://www.apache.org/licenses/LICENSE-2.0 

 Unless required by applicable law or agreed to in writing, software 
 distributed under the License is distributed on an "AS IS" BASIS, 
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 See the License for the specific language governing permissions and 
 limitations under the License.
 */

package org.apache.uima.fit.testing.util;

import java.io.PrintStream;

import org.apache.commons.io.output.NullOutputStream;

/**
 * This class provides a way to hide output sent to System.out and System.err. This may be useful
 * when testing code that creates a lot of noisy output that can be ignored for testing purposes.
 * This class works by redirecting System.out and System.err - note that this may have unintended
 * side effects if you are not careful to call {@link #restoreOutput()} after the noisy code.
 * Intended usage:
 * 
 * <code>
 * HideOutput hider = new HideOutput();
 * try {
 *     ... noisy code ...
 * } finally {
 *     hider.restoreOutput();
 * }
 * </code>
 * 
 * @author Steven Bethard, Philip Ogren
 */
public class HideOutput {
	protected PrintStream out;

	protected PrintStream err;

	/**
	 * calling this constructor will silence System.out and System.err until
	 * {@link #restoreOutput()} is called by setting them to this OutputStream
	 */
	public HideOutput() {
		this.out = System.out;
		this.err = System.err;
		System.setOut(new PrintStream(NullOutputStream.NULL_OUTPUT_STREAM));
		System.setErr(new PrintStream(NullOutputStream.NULL_OUTPUT_STREAM));
	}

	/**
	 * this method restores System.out and System.err
	 */
	public void restoreOutput() {
		System.setOut(this.out);
		System.setErr(this.err);
	}
}
