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

package org.apache.uima.pear.tools;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.xml.parsers.SAXParser;

import org.apache.uima.pear.tools.InstallationDescriptor.ActionInfo;
import org.apache.uima.pear.tools.InstallationDescriptor.ArgInfo;
import org.apache.uima.pear.tools.InstallationDescriptor.ComponentInfo;
import org.apache.uima.pear.util.XMLUtil;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * The <code>InstallationDescriptorHandler</code> class allows parsing XML installation descriptor
 * files, creating instances of the <code>InstallationDescriptor</code> class. The
 * <code>InstallationDescriptorHandler</code> class also allows to save existing
 * <code>InstallationDescriptor</code> objects as XML files.
 * 
 */

public class InstallationDescriptorHandler extends DefaultHandler {

  /*
   * XML tags
   */
  protected static final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

  public static final String ROOT_TAG = "COMPONENT_INSTALLATION_DESCRIPTOR";

  public static final String OS_TAG = "OS";

  public static final String NAME_TAG = "NAME";

  public static final String VERSION_TAG = "VERSION";

  public static final String TOOLKITS_TAG = "TOOLKITS";

  public static final String JDK_VERSION_TAG = "JDK_VERSION";

  public static final String UIMA_FRAMEWORK_TAG = "UIMA_FRAMEWORK";

  public static final String UIMA_VERSION_TAG = "UIMA_VERSION";

  public static final String TAF_VERSION_TAG = "TAF_VERSION";

  public static final String SUBMITTED_COMPONENT_TAG = "SUBMITTED_COMPONENT";

  public static final String ID_TAG = "ID";

  public static final String DESC_TAG = "DESC";

  public static final String DEPLOYMENT_TAG = "DEPLOYMENT";

  public static final String STANDARD_TAG = "standard";

  public static final String SERVICE_TAG = "service";

  public static final String NETWORK_TAG = "network";

  public static final String SERVICE_COMMAND_TAG = "SERVICE_COMMAND";

  public static final String SERVICE_WORKING_DIR_TAG = "SERVICE_WORKING_DIR";

  public static final String SERVICE_COMMAND_ARGS_TAG = "SERVICE_COMMAND_ARGS";

  public static final String ARGUMENT_TAG = "ARGUMENT";

  public static final String VALUE_TAG = "VALUE";

  public static final String NETWORK_PARAMETERS_TAG = "NETWORK_PARAMETERS";

  public static final String COMMENTS_TAG = "COMMENTS";

  public static final String COLLECTION_READER_TAG = "COLLECTION_READER";

  public static final String COLLECTION_ITERATOR_DESC_TAG = "COLLECTION_ITERATOR_DESC";

  public static final String CAS_INITIALIZER_DESC_TAG = "CAS_INITIALIZER_DESC";

  public static final String CAS_CONSUMER_TAG = "CAS_CONSUMER";

  public static final String INSTALLATION_TAG = "INSTALLATION";

  public static final String DELEGATE_COMPONENT_TAG = "DELEGATE_COMPONENT";

  public static final String PROCESS_TAG = "PROCESS";

  public static final String ACTION_TAG = "ACTION";

  public static final String PARAMETERS_TAG = "PARAMETERS";

  public static final String FILE_TAG = "FILE";

  public static final String FIND_STRING_TAG = "FIND_STRING";

  public static final String REPLACE_WITH_TAG = "REPLACE_WITH";

  public static final String VAR_NAME_TAG = "VAR_NAME";

  public static final String VAR_VALUE_TAG = "VAR_VALUE";

  /*
   * Attributes
   */
  private SAXParser _parser = null;

  private StringBuffer _activeBuffer = new StringBuffer();

  private String _mainTag = null;

  private InstallationDescriptor _insdObject = null;

  private String _activeSection = "";

  private String _activeSubSection = "";

  private String _activeComponentId = null;

  private InstallationDescriptor.ActionInfo _activeAction = null;

  private InstallationDescriptor.ServiceInfo _activeService = null;

  private InstallationDescriptor.ArgInfo _activeArg = null;

  private boolean _insdLoaded = false;

  /**
   * Returns the content of the installation descriptor XML file for a given intallation descriptor
   * object as <code>InputStream</code> object (for use in Eclipse plug-in).
   * 
   * @param insdObject
   *          The given intallation descriptor object.
   * @return The <code>InputStream</code> object that contains the content of the installation
   *         descriptor XML file.
   * @throws IOException
   *           if any I/O exception occurred.
   */
  public static InputStream getInstallationDescriptorAsStream(InstallationDescriptor insdObject)
          throws IOException {
    InputStream iStream = null;
    StringBuffer xmlBuffer = new StringBuffer();
    xmlBuffer.append(XML_HEADER);
    xmlBuffer.append('\n');
    xmlBuffer.append(insdObject.toString());
    byte[] xmlContentBytes = xmlBuffer.toString().getBytes("UTF-8");
    iStream = new ByteArrayInputStream(xmlContentBytes);
    return iStream;
  }

  /**
   * Prints a given <code>InstallationDescriptor</code> object in XML format to a given
   * <code>PrintWriter</code>.
   * 
   * @param insdObject
   *          The given <code>InstallationDescriptor</code> object.
   * @param oWriter
   *          The given <code>PrintWriter</code> object.
   * @throws IOException
   *           if any I/O exception occurred.
   */
  public static void printInstallationDescriptor(InstallationDescriptor insdObject,
          PrintWriter oWriter) throws IOException {
    String[] tagOrder = null;
    // ROOT - 0th level
    XMLUtil.printXMLTag(ROOT_TAG, oWriter, false, 0);
    oWriter.println();
    // OS specs - 1st level
    XMLUtil.printXMLTag(OS_TAG, oWriter, false, 1);
    oWriter.println();
    // 2nd level elements (multi-value properties) - NAME first
    tagOrder = new String[1];
    tagOrder[0] = NAME_TAG;
    if (insdObject.getOSSpecs().size() > 0)
      XMLUtil.printAllXMLElements(insdObject.getOSSpecs(),
              InstallationDescriptor.PROPERTY_DELIMITER, tagOrder, oWriter, 2);
    // OS specs end - 1st level
    XMLUtil.printXMLTag(OS_TAG, oWriter, true, 1);
    oWriter.println();
    // TOOLKITS specs - 1st level
    XMLUtil.printXMLTag(TOOLKITS_TAG, oWriter, false, 1);
    oWriter.println();
    // 2nd level elements (multi-value properties) - no order
    if (insdObject.getToolkitsSpecs().size() > 0)
      XMLUtil.printAllXMLElements(insdObject.getToolkitsSpecs(),
              InstallationDescriptor.PROPERTY_DELIMITER, null, oWriter, 2);
    // TOOLKITS specs end - 1st level
    XMLUtil.printXMLTag(TOOLKITS_TAG, oWriter, true, 1);
    oWriter.println();
    // UIMA_FRAMEWORK specs - 1st level
    XMLUtil.printXMLTag(UIMA_FRAMEWORK_TAG, oWriter, false, 1);
    oWriter.println();
    // 2nd level elements (multi-value properties) - no order
    if (insdObject.getFrameworkSpecs().size() > 0)
      XMLUtil.printAllXMLElements(insdObject.getFrameworkSpecs(),
              InstallationDescriptor.PROPERTY_DELIMITER, null, oWriter, 2);
    // UIMA_FRAMEWORK specs end - 1st level
    XMLUtil.printXMLTag(UIMA_FRAMEWORK_TAG, oWriter, true, 1);
    oWriter.println();
    // SUBMITTED_COMPONENT specs - 1st level
    XMLUtil.printXMLTag(SUBMITTED_COMPONENT_TAG, oWriter, false, 1);
    oWriter.println();
    if (insdObject.getMainComponentId() != null) {
      // 2nd level elements - ID, ...
      XMLUtil.printXMLElement(ID_TAG, insdObject.getMainComponentId(), oWriter, 2);
      oWriter.println();
      // NAME
      XMLUtil.printXMLElement(NAME_TAG, insdObject.getMainComponentName(), oWriter, 2);
      oWriter.println();
      // DESC
      XMLUtil.printXMLElement(DESC_TAG, insdObject.getMainComponentDesc(), oWriter, 2);
      oWriter.println();
      // DEPLOYMENT
      XMLUtil.printXMLElement(DEPLOYMENT_TAG, insdObject.getMainComponentDeployment(), oWriter, 2);
      oWriter.println();
      // SERVICE block
      InstallationDescriptor.ServiceInfo service = insdObject.getMainComponentService();
      if (service != null) {
        // SERVICE_COMMAND
        XMLUtil.printXMLElement(SERVICE_COMMAND_TAG, service.command, oWriter, 2);
        oWriter.println();
        // SERVICE_WORKING_DIR
        XMLUtil.printXMLElement(SERVICE_WORKING_DIR_TAG, service.workingDirPath, oWriter, 2);
        oWriter.println();
        // SERVICE_COMMAND_ARGS
        XMLUtil.printXMLTag(SERVICE_COMMAND_ARGS_TAG, oWriter, false, 2);
        oWriter.println();
        Iterator<ArgInfo> argList = service.getArgs().iterator();
        while (argList.hasNext()) {
          // ARGUMENT - 3rd level
          XMLUtil.printXMLTag(ARGUMENT_TAG, oWriter, false, 3);
          oWriter.println();
          InstallationDescriptor.ArgInfo arg = argList.next();
          // VALUE - 4th level elements
          XMLUtil.printXMLElement(VALUE_TAG, arg.value, oWriter, 4);
          oWriter.println();
          // COMMENTS
          XMLUtil.printXMLElement(COMMENTS_TAG, arg.comments, oWriter, 4);
          oWriter.println();
          // ARGUMENT end - 3rd level
          XMLUtil.printXMLTag(ARGUMENT_TAG, oWriter, true, 3);
          oWriter.println();
        }
        // SERVICE_COMMAND_ARGS end
        XMLUtil.printXMLTag(SERVICE_COMMAND_ARGS_TAG, oWriter, true, 2);
        oWriter.println();
      }
      // network component parameters block
      Set<String> netParamNames = insdObject.getMainComponentNetworkParamNames();
      if (netParamNames != null) {
        // NETWORK_PARAMETERS
        XMLUtil.printXMLTag(NETWORK_PARAMETERS_TAG, oWriter, false, 2);
        oWriter.println();
        Iterator<String> nameList = netParamNames.iterator();
        while (nameList.hasNext()) {
          String name = nameList.next();
          Properties attributes = insdObject.getMainComponentNetworkParam(name);
          XMLUtil.printXMLElement(name, attributes, null, oWriter, 3);
          oWriter.println();
        }
        // NETWORK_PARAMETERS end
        XMLUtil.printXMLTag(NETWORK_PARAMETERS_TAG, oWriter, true, 2);
        oWriter.println();
      }
      if (insdObject.getMainComponentProps().size() > 0)
        XMLUtil.printAllXMLElements(insdObject.getMainComponentProps(), oWriter, 2);
      String collIterDesc = insdObject.getMainCollIteratorDesc();
      String casInitDesc = insdObject.getMainCasInitializerDesc();
      if (collIterDesc != null || casInitDesc != null) {
        // COLLECTION_READER specs - 2nd level
        XMLUtil.printXMLTag(COLLECTION_READER_TAG, oWriter, false, 2);
        oWriter.println();
        // 3rd level elements
        if (collIterDesc != null) {
          XMLUtil.printXMLElement(COLLECTION_ITERATOR_DESC_TAG, collIterDesc, oWriter, 3);
          oWriter.println();
        }
        if (casInitDesc != null) {
          XMLUtil.printXMLElement(CAS_INITIALIZER_DESC_TAG, casInitDesc, oWriter, 3);
          oWriter.println();
        }
        // COLLECTION_READER specs end - 2nd level
        XMLUtil.printXMLTag(COLLECTION_READER_TAG, oWriter, true, 2);
        oWriter.println();
      }
      String casConsDesc = insdObject.getMainCasConsumerDesc();
      if (casConsDesc != null) {
        // CAS_CONSUMER specs - 2nd level
        XMLUtil.printXMLTag(CAS_CONSUMER_TAG, oWriter, false, 2);
        oWriter.println();
        // 3rd level elements
        XMLUtil.printXMLElement(DESC_TAG, casConsDesc, oWriter, 3);
        oWriter.println();
        // CAS_CONSUMER specs end - 2nd level
        XMLUtil.printXMLTag(CAS_CONSUMER_TAG, oWriter, true, 2);
        oWriter.println();
      }
    }
    // SUBMITTED_COMPONENT specs end - 1st level
    XMLUtil.printXMLTag(SUBMITTED_COMPONENT_TAG, oWriter, true, 1);
    oWriter.println();
    // INSTALLATION specs - 1st level
    XMLUtil.printXMLTag(INSTALLATION_TAG, oWriter, false, 1);
    oWriter.println();
    // delegate components, if specified
    Hashtable<String, ComponentInfo> dlgTable = insdObject.getDelegateComponents();
    Iterator<String> dlgList = dlgTable.keySet().iterator();
    while (dlgList.hasNext()) {
      // DELEGATE_COMPONENT specs - 2nd level
      XMLUtil.printXMLTag(DELEGATE_COMPONENT_TAG, oWriter, false, 2);
      oWriter.println();
      // 3rd level elements
      String dlgId = dlgList.next();
      InstallationDescriptor.ComponentInfo dlgInfo = dlgTable
              .get(dlgId);
      XMLUtil.printXMLElement(ID_TAG, dlgId, oWriter, 3);
      oWriter.println();
      XMLUtil.printXMLElement(NAME_TAG, dlgInfo.name, oWriter, 3);
      oWriter.println();
      // DELEGATE_COMPONENT specs end - 2nd level
      XMLUtil.printXMLTag(DELEGATE_COMPONENT_TAG, oWriter, true, 2);
      oWriter.println();
    }
    // installation actions, if specified
    Iterator<ActionInfo> actList = insdObject.getInstallationActions().iterator();
    while (actList.hasNext()) {
      ActionInfo actInfo = actList.next();
      // PROCESS specs - 2nd level
      XMLUtil.printXMLTag(PROCESS_TAG, oWriter, false, 2);
      oWriter.println();
      // 3rd level elements
      XMLUtil.printXMLElement(ACTION_TAG, actInfo.getName(), oWriter, 3);
      oWriter.println();
      // PARAMETERS specs - 3rd level
      XMLUtil.printXMLTag(PARAMETERS_TAG, oWriter, false, 3);
      oWriter.println();
      // 4th level elements - no order
      if (actInfo.params != null && actInfo.params.size() > 0)
        XMLUtil.printAllXMLElements(actInfo.params, oWriter, 4);
      // PARAMETERS specs end - 3rd level
      XMLUtil.printXMLTag(PARAMETERS_TAG, oWriter, true, 3);
      oWriter.println();
      // PROCESS specs end - 2nd level
      XMLUtil.printXMLTag(PROCESS_TAG, oWriter, true, 2);
      oWriter.println();
    }
    // INSTALLATION specs end - 1st level
    XMLUtil.printXMLTag(INSTALLATION_TAG, oWriter, true, 1);
    oWriter.println();
    // ROOT end - 0th level
    XMLUtil.printXMLTag(ROOT_TAG, oWriter, true, 0);
    oWriter.println();
  }

  /**
   * Saves a given <code>InstallationDescriptor</code> object in a given XML file.
   * 
   * @param insdObject
   *          The given <code>InstallationDescriptor</code> object.
   * @param xmlFile
   *          The given XML file.
   * @throws IOException
   *           if any I/O exception occurred.
   */
  public static void saveInstallationDescriptor(InstallationDescriptor insdObject, File xmlFile)
          throws IOException {
    PrintWriter oWriter = null;
    try {
      oWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(xmlFile), "UTF-8"));
      oWriter.println(XML_HEADER);
      printInstallationDescriptor(insdObject, oWriter);
    } catch (IOException exc) {
      throw exc;
    } finally {
      if (oWriter != null) {
        try {
          oWriter.close();
        } catch (Exception e) {
        }
      }
    }
  }

  /**
   * Receives notification of character data inside an element.
   * 
   * @param ch
   *          The characters.
   * @param start
   *          The start position in the character array.
   * @param length
   *          The number of characters to use from the character array.
   * @exception org.xml.sax.SAXException
   *              Any SAX exception, possibly wrapping another exception.
   */
  public void characters(char ch[], int start, int length) throws SAXException {
    _activeBuffer.append(ch, start, length);
  }

  /**
   * Receives notification of the end of the document.
   * 
   * @exception org.xml.sax.SAXException
   *              Any SAX exception, possibly wrapping another exception.
   */
  public void endDocument() throws SAXException {
    _insdLoaded = true;
  }

  /**
   * Receive notification of the end of an element.
   * 
   * @param uri
   *          The element URI.
   * @param localName
   *          The element type name.
   * @param qName
   *          The qualified name of the element.
   * @exception org.xml.sax.SAXException
   *              Any SAX exception, possibly wrapping another exception.
   */
  public void endElement(String uri, String localName, String qName) throws SAXException {
    String elemValue = _activeBuffer.toString().trim();
    if (OS_TAG.equals(_activeSection)) {
      if (localName.equalsIgnoreCase(OS_TAG))
        _activeSection = "";
      else
        _insdObject.addOSSpec(localName, elemValue);
    } else if (TOOLKITS_TAG.equals(_activeSection)) {
      if (localName.equalsIgnoreCase(TOOLKITS_TAG))
        _activeSection = "";
      else
        _insdObject.addToolkitsSpec(localName, elemValue);
    } else if (UIMA_FRAMEWORK_TAG.equals(_activeSection)) {
      if (localName.equalsIgnoreCase(UIMA_FRAMEWORK_TAG))
        _activeSection = "";
      else
        _insdObject.addFrameworkSpec(localName, elemValue);
    } else if (SUBMITTED_COMPONENT_TAG.equals(_activeSection)) {
      if (localName.equalsIgnoreCase(SUBMITTED_COMPONENT_TAG)) {
        _activeSection = "";
        if (_activeService != null)
          _insdObject.setMainComponentService(_activeService);
        _activeService = null;
      } else if (localName.equalsIgnoreCase(SERVICE_COMMAND_ARGS_TAG))
        _activeSubSection = "";
      else if (localName.equalsIgnoreCase(COLLECTION_READER_TAG))
        _activeSubSection = "";
      else if (localName.equalsIgnoreCase(CAS_CONSUMER_TAG))
        _activeSubSection = "";
      else if (localName.equalsIgnoreCase(ID_TAG)) {
        _activeComponentId = elemValue;
        _insdObject.setMainComponent(_activeComponentId, "");
        _activeService = new InstallationDescriptor.ServiceInfo();
      } else if (localName.equalsIgnoreCase(NAME_TAG)) {
        if (_activeComponentId != null) {
          _insdObject.setMainComponentName(elemValue);
          _activeComponentId = null;
        }
      } else if (localName.equalsIgnoreCase(DESC_TAG)) {
        if (CAS_CONSUMER_TAG.equals(_activeSubSection))
          _insdObject.setMainCasConsumerDesc(elemValue);
        else
          _insdObject.setMainComponentDesc(elemValue);
      } else if (localName.equalsIgnoreCase(DEPLOYMENT_TAG)) {
        _insdObject.setMainComponentDeployment(elemValue);
      } else if (localName.equalsIgnoreCase(SERVICE_COMMAND_TAG)) {
        if (_activeService != null)
          _activeService.command = elemValue;
      } else if (localName.equalsIgnoreCase(SERVICE_WORKING_DIR_TAG)) {
        if (_activeService != null)
          _activeService.workingDirPath = elemValue;
      } else if (localName.equalsIgnoreCase(VALUE_TAG)) {
        if (SERVICE_COMMAND_ARGS_TAG.equals(_activeSubSection) && _activeArg != null)
          _activeArg.value = elemValue;
      } else if (localName.equalsIgnoreCase(ARGUMENT_TAG)) {
        if (SERVICE_COMMAND_ARGS_TAG.equals(_activeSubSection) && _activeService != null
                && _activeArg != null && _activeArg.value != null && _activeArg.value.length() > 0)
          _activeService.addArg(_activeArg);
        _activeArg = null;
      } else if (localName.equalsIgnoreCase(NETWORK_PARAMETERS_TAG)) {
        if (NETWORK_PARAMETERS_TAG.equals(_activeSubSection))
          _activeSubSection = "";
      } else if (localName.equalsIgnoreCase(COMMENTS_TAG)) {
        if (SERVICE_COMMAND_ARGS_TAG.equals(_activeSubSection)) {
          if (_activeArg != null)
            _activeArg.comments = elemValue;
        } else
          _insdObject.setMainComponentProperty(COMMENTS_TAG, elemValue);
      } else if (localName.equalsIgnoreCase(COLLECTION_ITERATOR_DESC_TAG)) {
        _insdObject.setMainCollIteratorDesc(elemValue);
      } else if (localName.equalsIgnoreCase(CAS_INITIALIZER_DESC_TAG)) {
        _insdObject.setMainCasInitializerDesc(elemValue);
      } else
        _insdObject.setMainComponentProperty(localName, elemValue);
    } else if (INSTALLATION_TAG.equals(_activeSection)) {
      if (localName.equalsIgnoreCase(INSTALLATION_TAG))
        _activeSection = "";
      else if (localName.equalsIgnoreCase(DELEGATE_COMPONENT_TAG))
        _activeSubSection = "";
      else if (localName.equalsIgnoreCase(PROCESS_TAG)) {
        if (_activeAction != null) {
          _insdObject.addInstallationAction(_activeAction);
          _activeAction = null;
        }
        _activeSubSection = "";
      } else if (localName.equalsIgnoreCase(PARAMETERS_TAG)) {
        // do nothing
      } else if (localName.equalsIgnoreCase(ID_TAG)) {
        _activeComponentId = elemValue;
      } else if (localName.equalsIgnoreCase(NAME_TAG)) {
        if (DELEGATE_COMPONENT_TAG.equals(_activeSubSection)) {
          if (_activeComponentId != null) {
            _insdObject.addDelegateComponent(_activeComponentId, elemValue);
            _activeComponentId = null;
          }
        }
      } else if (localName.equalsIgnoreCase(ACTION_TAG)) {
        if (PROCESS_TAG.equals(_activeSubSection))
          _activeAction = new InstallationDescriptor.ActionInfo(elemValue);
      } else {
        if (_activeAction != null)
          _activeAction.params.setProperty(localName, elemValue);
      }
    }
  }

  /**
   * XML parser error handler.
   */
  public void error(SAXParseException ex) throws SAXException {
    XMLUtil.printError("Error", ex);
  }

  /**
   * XML parser fatal error handler.
   */
  public void fatalError(SAXParseException ex) throws SAXException {
    XMLUtil.printError("Fatal Error", ex);
    throw ex;
  }

  /**
   * @return <code>InstallationDescriptor</code> object after the installation descriptor file has
   *         been loaded, or <code>null</code>, if the file was not loaded.
   */
  public synchronized InstallationDescriptor getInstallationDescriptor() {
    return _insdLoaded ? _insdObject : null;
  }

  /**
   * Starts parsing a given XML file. After parsing is completed, the application may access parsing
   * results using convenient methods.
   * 
   * @param xmlFile
   *          The given XML file.
   * @exception java.io.IOException
   *              Any I/O exception.
   * @exception org.xml.sax.SAXException
   *              Any SAX exception, possibly wrapping another exception.
   */
  public synchronized void parse(File xmlFile) throws IOException, SAXException {
    if (_parser == null)
      _parser = XMLUtil.createSAXParser();
    _insdObject = new InstallationDescriptor(xmlFile);
    _insdLoaded = false;
    _parser.parse(xmlFile, this);
  }

  /**
   * Starts parsing XML content from a given input stream.
   * 
   * @param xmlStream
   *          The given XML input stream.
   * @throws IOException
   *           if any I/O exception occurred.
   * @throws SAXException
   *           Any SAX exception, possibly wrapping another exception.
   */
  public synchronized void parse(InputStream xmlStream) throws IOException, SAXException {
    if (_parser == null)
      _parser = XMLUtil.createSAXParser();
    _insdObject = new InstallationDescriptor();
    _insdLoaded = false;
    _parser.parse(xmlStream, this);
  }

  /**
   * Parses XML installation descriptor automatically extracting it from a given PEAR (JAR) file.
   * 
   * @param pearFile
   *          The given PEAR (JAR) file.
   * @throws IOException
   *           if any I/O exception occurred.
   * @throws SAXException
   *           Any SAX exception, possibly wrapping another exception.
   */
  public synchronized void parseInstallationDescriptor(JarFile pearFile) throws IOException,
          SAXException {
    String insdFilePath = InstallationProcessor.INSD_FILE_PATH;
    JarEntry insdJarEntry = pearFile.getJarEntry(insdFilePath);
    if (insdJarEntry != null) {
      parse(pearFile.getInputStream(insdJarEntry));
    } else
      throw new IOException("installation drescriptor not found");
  }

  /**
   * Saves created <code>InstallationDescriptor</code> object to a given XML file.
   * 
   * @param xmlFile
   *          The given XML file.
   * @throws IOException
   *           if any I/O exception occurred.
   */
  public synchronized void saveInstallationDescriptor(File xmlFile) throws IOException {
    if (_insdLoaded)
      saveInstallationDescriptor(_insdObject, xmlFile);
  }

  /**
   * Receives notification of the beginning of the document.
   * 
   * @exception org.xml.sax.SAXException
   *              Any SAX exception, possibly wrapping another exception.
   */
  public void startDocument() throws SAXException {
  }

  /**
   * Receives notification of the start of an element.
   * 
   * @param uri
   *          The element URI.
   * @param localName
   *          The element type name.
   * @param qName
   *          The qualified name of the element.
   * @param attributes
   *          The specified or defaulted attributes.
   * @exception org.xml.sax.SAXException
   *              Any SAX exception, possibly wrapping another exception.
   */
  public void startElement(String uri, String localName, String qName, Attributes attributes)
          throws SAXException {
    _mainTag = localName;
    _activeBuffer.setLength(0);
    if (_mainTag.equalsIgnoreCase(OS_TAG)) {
      _activeSection = OS_TAG;
    } else if (_mainTag.equalsIgnoreCase(TOOLKITS_TAG)) {
      _activeSection = TOOLKITS_TAG;
    } else if (_mainTag.equalsIgnoreCase(UIMA_FRAMEWORK_TAG)) {
      _activeSection = UIMA_FRAMEWORK_TAG;
    } else if (_mainTag.equalsIgnoreCase(SUBMITTED_COMPONENT_TAG)) {
      _activeSection = SUBMITTED_COMPONENT_TAG;
      _activeComponentId = null;
    } else if (_mainTag.equalsIgnoreCase(SERVICE_COMMAND_ARGS_TAG)) {
      if (SUBMITTED_COMPONENT_TAG.equals(_activeSection))
        _activeSubSection = SERVICE_COMMAND_ARGS_TAG;
    } else if (_mainTag.equalsIgnoreCase(ARGUMENT_TAG)) {
      if (SUBMITTED_COMPONENT_TAG.equals(_activeSection)
              && SERVICE_COMMAND_ARGS_TAG.equals(_activeSubSection))
        _activeArg = new InstallationDescriptor.ArgInfo();
    } else if (_mainTag.equalsIgnoreCase(NETWORK_PARAMETERS_TAG)) {
      if (SUBMITTED_COMPONENT_TAG.equals(_activeSection))
        _activeSubSection = NETWORK_PARAMETERS_TAG;
    } else if (_mainTag.equalsIgnoreCase(CAS_CONSUMER_TAG)) {
      if (SUBMITTED_COMPONENT_TAG.equals(_activeSection))
        _activeSubSection = CAS_CONSUMER_TAG;
    } else if (_mainTag.equalsIgnoreCase(INSTALLATION_TAG)) {
      _activeSection = INSTALLATION_TAG;
    } else if (_mainTag.equalsIgnoreCase(DELEGATE_COMPONENT_TAG)) {
      if (INSTALLATION_TAG.equals(_activeSection)) {
        _activeSubSection = DELEGATE_COMPONENT_TAG;
        _activeComponentId = null;
      }
    } else if (_mainTag.equalsIgnoreCase(PROCESS_TAG)) {
      if (INSTALLATION_TAG.equals(_activeSection)) {
        _activeSubSection = PROCESS_TAG;
        _activeAction = null;
      }
    } else if (NETWORK_PARAMETERS_TAG.equals(_activeSubSection)) {
      // set network component parameters
      String paramName = _mainTag;
      int attrCount = attributes.getLength();
      Properties paramSpecs = new Properties();
      for (int i = 0; i < attrCount; i++) {
        String name = attributes.getLocalName(i);
        String value = attributes.getValue(i);
        paramSpecs.setProperty(name, value);
      }
      if (paramSpecs.size() > 0)
        _insdObject.setMainComponentNetworkParam(paramName, paramSpecs);
    }
  }

  /**
   * XML parser warning handler.
   */
  public void warning(SAXParseException ex) throws SAXException {
    XMLUtil.printError("Warning", ex);
  }
}
