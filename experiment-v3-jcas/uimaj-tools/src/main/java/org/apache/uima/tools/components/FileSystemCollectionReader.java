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

package org.apache.uima.tools.components;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.impl.XCASDeserializer;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.collection.CollectionReader_ImplBase;
import org.apache.uima.examples.SourceDocumentInformation;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.FileUtils;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.Level;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;
import org.apache.uima.util.XMLInputSource;
import org.xml.sax.SAXException;

/**
 * A simple collection reader that reads documents from a directory in the filesystem. It can be
 * configured with the following parameters:
 * <ul>
 * <li><code>InputDirectory</code> - path to directory containing files</li>
 * <li><code>Encoding</code> (optional) - character encoding of the input files</li>
 * <li><code>Language</code> (optional) - language of the input documents</li>
 * </ul>
 * 
 * 
 */
public class FileSystemCollectionReader extends CollectionReader_ImplBase {
  /**
   * Name of configuration parameter that must be set to the path of a directory containing input
   * files.
   */
  public static final String PARAM_INPUTDIR = "InputDirectory";

  /**
   * Name of configuration parameter that contains the character encoding used by the input files.
   * If not specified, the default system encoding will be used.
   */
  public static final String PARAM_ENCODING = "Encoding";

  /**
   * Name of optional configuration parameter that contains the language of the documents in the
   * input directory. If specified this information will be added to the CAS.
   */
  public static final String PARAM_LANGUAGE = "Language";

  /**
   * Optional configuration parameter that specifies XCAS input files
   */
  public static final String PARAM_XCAS = "XCAS";

  /**
   * Name of the configuration parameter that must be set to indicate if the
   * execution proceeds if an encountered type is unknown
   */
  public static final String PARAM_LENIENT = "LENIENT";

  private ArrayList mFiles;

  private String mEncoding;

  private String mLanguage;

  private int mCurrentIndex;

  private boolean mTEXT;

  private String mXCAS;
  
  private boolean lenient;

  /**
   * @see org.apache.uima.collection.CollectionReader_ImplBase#initialize()
   */
  public void initialize() throws ResourceInitializationException {
    String dirPath = ((String) getConfigParameterValue(PARAM_INPUTDIR)).trim();
    File directory = new File(dirPath);
    mEncoding = (String) getConfigParameterValue(PARAM_ENCODING);
    mLanguage = (String) getConfigParameterValue(PARAM_LANGUAGE);
    mXCAS = (String) getConfigParameterValue(PARAM_XCAS);
    //XCAS parameter can be set to "xcas" or "xmi", as well as "true" (which for historical reasons
    //means the same as "xcas").  Any other value will cause the input file to be treated as a text document.
    mTEXT = !("xcas".equalsIgnoreCase(mXCAS) || "xmi".equalsIgnoreCase(mXCAS) || "true".equalsIgnoreCase(mXCAS));
    String mLenient = (String) getConfigParameterValue(PARAM_LENIENT);
    lenient = "true".equalsIgnoreCase(mLenient);

    mCurrentIndex = 0;

    // if input directory does not exist or is not a directory, throw exception
    if (!directory.exists() || !directory.isDirectory()) {
      throw new ResourceInitializationException(ResourceConfigurationException.DIRECTORY_NOT_FOUND,
              new Object[] { PARAM_INPUTDIR, this.getMetaData().getName(), directory.getPath() });
    }

    // get list of files (not subdirectories) in the specified directory
    mFiles = new ArrayList();
    File[] files = directory.listFiles();
    for (int i = 0; i < files.length; i++) {
      if (!files[i].isDirectory()) {
        mFiles.add(files[i]);
      }
    }
  }

  /**
   * @see org.apache.uima.collection.CollectionReader#hasNext()
   */
  public boolean hasNext() {
    return mCurrentIndex < mFiles.size();
  }

  /**
   * @see org.apache.uima.collection.CollectionReader#getNext(org.apache.uima.cas.CAS)
   */
  public void getNext(CAS aCAS) throws IOException, CollectionException {
    JCas jcas;
    try {
      jcas = aCAS.getJCas();
    } catch (CASException e) {
      throw new CollectionException(e);
    }

    // open input stream to file
    File file = (File) mFiles.get(mCurrentIndex++);
    FileInputStream fis = new FileInputStream(file);
    if (mTEXT) {
      try {
        // if there's a CAS Initializer, call it
        if (getCasInitializer() != null) {
          getCasInitializer().initializeCas(fis, aCAS);
        } else // No CAS Initiliazer, so read file and set document text ourselves
        {
          String text = FileUtils.file2String(file, mEncoding);      
          // put document text in JCas
          jcas.setDocumentText(text);
        }
      } finally {
        if (fis != null)
          fis.close();
      }

      // set language if it was explicitly specified as a configuration parameter
      if (mLanguage != null) {
        jcas.setDocumentLanguage(mLanguage);
      }

      // Also store location of source document in CAS. This information is critical
      // if CAS Consumers will need to know where the original document contents are located.
      // For example, the Semantic Search CAS Indexer writes this information into the
      // search index that it creates, which allows applications that use the search index to
      // locate the documents that satisfy their semantic queries.
      SourceDocumentInformation srcDocInfo = new SourceDocumentInformation(jcas);
      srcDocInfo.setUri(file.getAbsoluteFile().toURL().toString());
      srcDocInfo.setOffsetInSource(0);
      srcDocInfo.setDocumentSize((int) file.length());
      srcDocInfo.setLastSegment(mCurrentIndex == mFiles.size());
      srcDocInfo.addToIndexes();
    }
    // XCAS input files
    else {
      try {
        if (mXCAS.equalsIgnoreCase("xmi")) {
          XmiCasDeserializer.deserialize(fis, aCAS, lenient);
        }
        else {
          XCASDeserializer.deserialize(fis, aCAS, lenient);
        }
      } catch (SAXException e) {
        UIMAFramework.getLogger(FileSystemCollectionReader.class).log(Level.WARNING,
                "Problem with XML input file: " + file.getAbsolutePath());
        throw new CollectionException(e);
      } finally {
        fis.close();
      }
    }
  }

  /**
   * @see org.apache.uima.collection.base_cpm.BaseCollectionReader#close()
   */
  public void close() throws IOException {
  }

  /**
   * @see org.apache.uima.collection.base_cpm.BaseCollectionReader#getProgress()
   */
  public Progress[] getProgress() {
    return new Progress[] { new ProgressImpl(mCurrentIndex, mFiles.size(), Progress.ENTITIES) };
  }

  /**
   * Gets the total number of documents that will be returned by this collection reader. This is not
   * part of the general collection reader interface.
   * 
   * @return the number of documents in the collection
   */
  public int getNumberOfDocuments() {
    return mFiles.size();
  }

  /**
   * Parses and returns the descriptor for this collection reader. The descriptor is stored in the
   * uima.jar file and located using the ClassLoader.
   * 
   * @return an object containing all of the information parsed from the descriptor.
   * 
   * @throws InvalidXMLException
   *           if the descriptor is invalid or missing
   */
  public static CollectionReaderDescription getDescription() throws InvalidXMLException {
    InputStream descStream = FileSystemCollectionReader.class
            .getResourceAsStream("FileSystemCollectionReader.xml");
    return UIMAFramework.getXMLParser().parseCollectionReaderDescription(
            new XMLInputSource(descStream, null));
  }
  
  public static URL getDescriptorURL() {
    return FileSystemCollectionReader.class.getResource("FileSystemCollectionReader.xml");
  }
}
