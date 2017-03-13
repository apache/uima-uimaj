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

package org.apache.uima.uimacpp;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * Use this class to signalize a fatal (internal) error to the API user.
 */
public class UimacppException extends Exception {
  private static final long serialVersionUID = 378698033899907645L;

  /**
   * Nested exception to hold wrapped exception.
   */
  Exception detail = null;

  /**
   * Constructs a <code>JTafRuntimeException</code> with the specified nested exception.
   * 
   * @param ex
   *          the nested exception
   */
  public UimacppException(Exception ex) {
    super();
    detail = ex;
  }

  /**
   * Returns a detail message. If present, this will also include the nested exception.
   */
  public String getMessage() {
    if (detail == null) {
      return super.getMessage();
    }
    return super.getMessage() + ";\n\t" + detail.toString();
  }

  /**
   * Prints the composite message and the embedded stack trace to the specified print stream,
   * <code>ps</code>.
   * 
   * @param ps
   *          the print stream
   */
  public void printStackTrace(PrintStream ps) {
    if (detail == null) {
      super.printStackTrace(ps);
    } else {
      synchronized (ps) {
        ps.println(this);
        detail.printStackTrace(ps);
      }
    }
  }

  /**
   * Prints the composite message to <code>System.err</code>.
   */
  public void printStackTrace() {
    printStackTrace(System.err);
  }

  /**
   * Prints the composite message and the embedded stack trace to the specified print writer,
   * <code>pw</code>
   * 
   * @param pw
   *          the print writer
   */
  public void printStackTrace(PrintWriter pw) {
    if (detail == null) {
      super.printStackTrace(pw);
    } else {
      synchronized (pw) {
        pw.println(this);
        detail.printStackTrace(pw);
      }
    }
  }

  /**
   * get the embedded exception, if any.
   * @return the embedded exception
   */
  public Exception getEmbeddedException() {
    return detail;
  }

}
