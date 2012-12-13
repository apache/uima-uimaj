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

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * When you are running a suite of unit tests it is often nice to turn off logging. This class helps
 * you do this. However, it only works if logging is being done by the Java logger, not by, say,
 * log4j.
 * 
 * @author Steven Bethard, Philip Ogren
 */
public final class DisableLogging {
	private DisableLogging() {
		// This class is not meant to be instantiated
	}

	/**
	 * Disable all logging.
	 * 
	 * @return The original logging level.
	 */
	public static Level disableLogging() {
		Logger logger = Logger.getLogger("");
		Level level = logger.getLevel();
		logger.setLevel(Level.OFF);
		return level;
	}

	/**
	 * Enable all logging.
	 * 
	 * @param level
	 *            The logging level to be restored. Usually this is the result returned by
	 *            disableLogging().
	 */
	public static void enableLogging(Level level) {
		Logger logger = Logger.getLogger("");
		logger.setLevel(level);
	}

}
