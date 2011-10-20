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

package org.apache.uima.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.apache.uima.UIMARuntimeException;

/**
 * An input source used by the {@link XMLParser} to read XML documents for parsing.
 * <p>
 * The application uses the {@link #XMLInputSource(File)} constructor to create an
 * <code>XMLInputSource</code> from a descriptor <code>File</code>. Alternatively, if the
 * source of the XML is not a file, the {@link #XMLInputSource(InputStream,File)} constructor may be
 * used to read the XML from an input stream. The second argument to this constructor is the
 * relative path base, to be used if the descriptor contains imports with relative paths. It is
 * acceptable to set this to null if it is known that the descriptor does not contain any such
 * imports.
 */
public class XMLInputSource {

  /**
   * InputStream from which the XML document is read.
   */
  private InputStream mInputStream;

  /**
   * URL that we're parsing from
   */
  private URL mURL;

  /**
   * Creates an <code>XMLInputSource</code> from a descriptor file.
   * 
   * @param aFile
   *          file to read from
   * 
   * @throws IOException
   *           if an I/O error occurs
   */
  public XMLInputSource(File aFile) throws IOException {
    this(aFile.toURL());
  }

  /**
   * Creates an <code>XMLInputSource</code> from a descriptor file.
   * 
   * @param aUrlOrFileName
   *          a URL or a file name to read from
   * 
   * @throws IOException
   *           if an I/O error occurs
   */
  public XMLInputSource(String aUrlOrFileName) throws IOException {
    //try as URL first, then as file name
    try {
      mURL = new URL(aUrlOrFileName);
    }
    catch (MalformedURLException e) {
      mURL = new File(aUrlOrFileName).toURL();
    }
    mInputStream = mURL.openStream();
  }

  /**
   * Creates an XMLInputSource from an existing InputStream.
   * 
   * @param aInputStream
   *          input stream from which to read
   * @param aRelativePathBase
   *          base for resolving relative paths. This must be a directory.
   */
  public XMLInputSource(InputStream aInputStream, File aRelativePathBase) {
    mInputStream = aInputStream;
    try {
      mURL = aRelativePathBase == null ? null : aRelativePathBase.toURL();
    } catch (MalformedURLException e) {
      throw new UIMARuntimeException(e);
    }
  }

  /**
   * Creates an <code>XMLInputSource</code> from a URL.
   * 
   * @param aURL
   *          URL to read from
   * 
   * @throws IOException
   *           if an I/O error occurs
   */
  public XMLInputSource(URL aURL) throws IOException {
    mURL = aURL;
    // replace openStream which is openConnection().getInputStream() with
    // version that allows setting caching(false)
    // See https://issues.apache.org/jira/browse/UIMA-1746
    URLConnection urlConnection= aURL.openConnection();
    urlConnection.setUseCaches(false);
    mInputStream = urlConnection.getInputStream();
//    mInputStream = aURL.openStream();
  }

  /**
   * Gets the InputStream from which to read an XML document.
   * 
   * @return an InputStream from which an XML document may be read
   */
  public InputStream getInputStream() {
    return mInputStream;
  }

  /**
   * Gets the base for resolving relative paths. This must be a directory.
   * 
   * @return the base for resolving relative paths, <code>null</code> if none has been specified.
   * 
   * @deprecated Use {@link #getURL()} instead.
   */
  @Deprecated
  public File getRelativePathBase() {
    // use the parent directory as the base for relative path resolution
    String path = mURL.getPath();
    return new File(path).getParentFile();
  }

  /**
   * Gets the base for resolving relative paths. This must be a directory.
   * 
   * @return the base for resolving relative paths, <code>null</code> if none has been specified.
   */
  public URL getURL() {
    return mURL;
  }

  /**
   * Closes the underlying <code>InputStream</code>.
   *
   * @throws IOException if an I/O error occurs.
   */
  public void close()
      throws IOException
  {
      if (mInputStream != null)
      {
          mInputStream.close();
      }

      mURL = null;
  }
}
