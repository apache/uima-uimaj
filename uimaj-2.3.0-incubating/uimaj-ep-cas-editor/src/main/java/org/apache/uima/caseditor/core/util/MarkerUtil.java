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

package org.apache.uima.caseditor.core.util;

import org.apache.uima.util.InvalidXMLException;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.xml.sax.SAXParseException;

/**
 * TODO: add javadoc here
 */
public final class MarkerUtil {
  /**
   * ID of the problem marker.
   */
  public static final String PROBLEM_MARKER = "org.apache.uima.caseditor.problem";

  private MarkerUtil() {
    // util class
  }

  /**
   * Clears all markers of the given id for the given resource.
   * 
   * @param resource
   * @param markerId
   * @throws CoreException
   */
  public static void clearMarkers(IResource resource, String markerId) throws CoreException {
    if (resource.exists()) {
      IMarker[] markers = resource.findMarkers(markerId, false, 0);

      for (IMarker marker : markers) {
        marker.delete();
      }
    }
  }

  /**
   * Creates a new marker.
   * 
   * @param resource
   * @param errorMessage
   * @throws CoreException
   */
  public static void createMarker(IResource resource, String errorMessage) throws CoreException {
    IMarker marker = resource.createMarker(PROBLEM_MARKER);

    marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
    marker.setAttribute(IMarker.MARKER, IMarker.PROBLEM);

    marker.setAttribute(IMarker.MESSAGE, errorMessage);
    marker.setAttribute(IMarker.LINE_NUMBER, 1);
  }

  /**
   * Creates a new marker.
   * 
   * @param resource
   * @param xmlException
   * @throws CoreException
   */
  public static void createMarker(IResource resource, InvalidXMLException xmlException)
          throws CoreException {
    IMarker marker = resource.createMarker(PROBLEM_MARKER);

    marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
    marker.setAttribute(IMarker.MARKER, IMarker.PROBLEM);

    Throwable causeException = xmlException.getCause();

    if (causeException instanceof SAXParseException) {
      SAXParseException parseException = (SAXParseException) causeException;

      marker.setAttribute(IMarker.MESSAGE, parseException.getMessage());
      marker.setAttribute(IMarker.LINE_NUMBER, parseException.getLineNumber());
    } else {
      marker.setAttribute(IMarker.MESSAGE, xmlException.getMessage());
    }
  }
}
