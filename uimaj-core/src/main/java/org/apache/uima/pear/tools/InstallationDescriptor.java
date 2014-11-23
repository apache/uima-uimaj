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

import java.io.File;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.uima.pear.util.FileUtil;

/**
 * The <code>InstallationDescriptor</code> class encapsulates all elements and parameters included
 * in the XML Installation Descriptor file.
 * 
 * @see org.apache.uima.pear.tools.InstallationDescriptorHandler
 */

public class InstallationDescriptor implements Serializable {

  private static final long serialVersionUID = 4884186903126810934L;

  /**
   * The <code>ActionInfo</code> class defines 'installation action' attributes.
   * 
   */
  public static class ActionInfo implements Serializable {

    private static final long serialVersionUID = -3459024334454685063L;

    /*
     * Action IDs
     */
    public static final String FIND_AND_REPLACE_PATH_ACT = "find_and_replace_path";

    public static final String SET_ENV_VARIABLE_ACT = "set_env_variable";

    /**
     * Constructor that takes 'action' name as an argument.
     * 
     * @param name
     *          The given action name.
     */
    public ActionInfo(String name) {
      _name = name;
    }

    protected ActionInfo(ActionInfo anAction) {
      _name = anAction.getName();
      params = (Properties) anAction.params.clone();
    }

    public String getName() {
      return _name;
    }

    // private attributes
    private String _name;

    // public attributes
    public Properties params = new Properties();
  }

  /**
   * The <code>ArgInfo</code> class defines attributes of a service command argument.
   * 
   */
  public static class ArgInfo {
    // public attributes
    public String value;

    public String comments;
  }

  /**
   * The <code>ServiceInfo</code> class defines attributes of a 'service' component.
   * 
   */
  public static class ServiceInfo {
    // private attributes
    
    private ArrayList<ArgInfo> _args = new ArrayList<ArgInfo>();

    // public attributes
    public String command;

    public String workingDirPath;

    public synchronized void addArg(ArgInfo arg) {
      _args.add(arg);
    }

    /**
     * @return <code>Collection</code> of ArgInfo objects.
     */
    public Collection<ArgInfo> getArgs() {
      return _args;
    }
  }

  /**
   * The <code>ComponentInfo</code> class defines UIMA component attributes.
   * 
   */
  public static class ComponentInfo implements Serializable {

    private static final long serialVersionUID = 3269238133625161794L;

    /**
     * Constructor that takes component ID as an argument.
     * 
     * @param id
     *          The given component ID.
     */
    public ComponentInfo(String id) {
      _id = id;
    }

    /**
     * @return The specified component ID.
     */
    public String getId() {
      return _id;
    }

    // private attributes
    private String _id;

    // public attributes
    public String name = null;

    public String descFilePath = null;

    public String rootDirPath = null;

    public String deploymentType = InstallationDescriptorHandler.STANDARD_TAG;

    public ServiceInfo serviceInfo = null;

    public Hashtable<String, Properties> networkParams = null;

    public String collIteratorDescFilePath = null;

    public String casInitializerDescFilePath = null;

    public String casConsumerDescFilePath = null;

    public Properties props = new Properties();
  }

  /*
   * Miscellaneous public constants
   */
  public static final String PROPERTY_DELIMITER = "\n";

  public static final String VNS_SPECS = "VNS_SPECS";

  public static final String VNS_HOST = "VNS_HOST";

  public static final String VNS_PORT = "VNS_PORT";

  // Attributes
  private File _insdFile = null;

  private Properties _osSpecs = new Properties();

  private Properties _toolkitsSpecs = new Properties();

  private Properties _frameworkSpecs = new Properties();

  private ComponentInfo _mainComponent = null;

  private Hashtable<String, ComponentInfo> _delegateComponents = new Hashtable<String, ComponentInfo>();

  private ArrayList<ActionInfo> _installationActions = new ArrayList<ActionInfo>();

  /**
   * Adds a property specified by given name and value to a given <code>Properties</code> object.
   * If the given object already contains a property with the given name, adds a new value to this
   * property using PROPERTY_DELIMITER as the delimiter, unless the existing property value list
   * already contains the given new value.
   * 
   * @param props
   *          The given <code>Properties</code> object to be modified.
   * @param name
   *          The given property name.
   * @param value
   *          The given new value of the property.
   * @return The modified <code>Properties</code> object.
   */
  protected static Properties addProperty(Properties props, String name, String value) {
    String curValue = props.getProperty(name);
    if (curValue == null) // set new property value
      props.setProperty(name, value.trim());
    else {
      // check that specified value does not appear in the old list
      boolean alreadyThere = false;
      StringTokenizer curValueList = new StringTokenizer(curValue, PROPERTY_DELIMITER);
      while (curValueList.hasMoreTokens()) {
        String token = curValueList.nextToken().trim();
        if (token.equalsIgnoreCase(value.trim())) {
          alreadyThere = true;
          break;
        }
      }
      if (!alreadyThere) {
        // add new value to the list
        String newValue = curValue + PROPERTY_DELIMITER + value.trim();
        props.setProperty(name, newValue);
      }
    }
    return props;
  }

  /**
   * Deletes a property specified by given name and value from a given <code>Properties</code>
   * object. If the given object contains one or more PROPERTY_DELIMITER separated values under the
   * given property name, removes the value that is equal to the specified value. If no values
   * remain under the given name, removes the property associated with the specified name from the
   * given object.
   * 
   * @param props
   *          The given <code>Properties</code> object to be modified.
   * @param name
   *          The given property name.
   * @param value
   *          The given new value of the property.
   * @return The modified <code>Properties</code> object.
   */
  protected static Properties deleteProperty(Properties props, String name, String value) {
    String curValue = props.getProperty(name);
    if (curValue != null) {
      StringBuffer newBuffer = new StringBuffer();
      StringTokenizer curValueList = new StringTokenizer(curValue, PROPERTY_DELIMITER);
      while (curValueList.hasMoreTokens()) {
        String token = curValueList.nextToken().trim();
        if (token.length() > 0 && !token.equals(value)) {
          if (newBuffer.length() > 0)
            newBuffer.append(PROPERTY_DELIMITER);
          newBuffer.append(token);
        }
      }
      if (newBuffer.length() > 0)
        props.setProperty(name, newBuffer.toString());
      else
        props.remove(name);
    }
    return props;
  }

  /**
   * Constructs a relative path of a given component object, based on its absolute path.
   * 
   * @param absolutePath
   *          The given absolute path of the object.
   * @param component
   *          The given component instance.
   * @return The relative path of the given component object.
   */
  protected static String getRelativePathForComponentObject(String absolutePath,
          ComponentInfo component) {
    String path = absolutePath;
    if (component.rootDirPath != null && path != null) {
      File rootDir = new File(component.rootDirPath);
      path = FileUtil.getRelativePath(rootDir, path);
    }
    return path;
  }

  /**
   * Default constructor.
   */
  public InstallationDescriptor() {
  }

  /**
   * Constructor that takes a given original InsD file as an argument.
   * 
   * @param insdFile
   *          The given original InsD file.
   */
  public InstallationDescriptor(File insdFile) {
    _insdFile = insdFile;
  }

  /**
   * Creates and adds a delegate component specification to the list (for aggregate component).
   * 
   * @param id
   *          The given delegate component ID.
   * @param name
   *          The given delegate component name.
   */
  public synchronized void addDelegateComponent(String id, String name) {
    String dlgId = id.trim();
    if (_delegateComponents.get(dlgId) == null) {
      ComponentInfo compInfo = new ComponentInfo(dlgId);
      compInfo.name = name.trim();
      _delegateComponents.put(dlgId, compInfo);
    }
  }

  /**
   * Adds a specification defined by given name and value to the set of Framework specifications. If
   * the Framework specifications already contain the given name, adds a new value using
   * PROPERTY_DELIMITER as the delimiter.
   * 
   * @param specName
   *          The given specification name.
   * @param specValue
   *          The given specification value.
   */
  public synchronized void addFrameworkSpec(String specName, String specValue) {
    addProperty(_frameworkSpecs, specName, specValue);
  }

  /**
   * Adds a specified 'installation action' to the list.
   * 
   * @param action
   *          The specified 'installation action' object.
   */
  public synchronized void addInstallationAction(ActionInfo action) {
    if (_mainComponent != null && action.params != null) {
      // duplicate action object to modify the values
      ActionInfo mAction = new ActionInfo(action);
      // during PEAR installation:
      // a) substitute $main_root and $comp_id$root in action fields
      // b) substitute standard path separator (';') with OS dependent
      // in FILE_TAG, REPLACE_WITH_TAG and VAR_VALUE_TAG values
      Enumeration<Object> keys = mAction.params.keys();
      while (keys.hasMoreElements()) {
        String key = (String) keys.nextElement();
        String value = mAction.params.getProperty(key);
        if (key.equals(InstallationDescriptorHandler.FILE_TAG)
                || key.equals(InstallationDescriptorHandler.REPLACE_WITH_TAG)
                || key.equals(InstallationDescriptorHandler.VAR_VALUE_TAG)) {
          if (_mainComponent.rootDirPath != null) {
            // substitute '$main_root' macros
            value = InstallationProcessor.substituteMainRootInString(value,
                    _mainComponent.rootDirPath);
            mAction.params.setProperty(key, value);
          }
          Iterator<Map.Entry<String, ComponentInfo>> dlgEntries = getDelegateComponents().entrySet().iterator();
          while (dlgEntries.hasNext()) {
            Map.Entry<String, ComponentInfo> dlgEntry = dlgEntries.next();
            ComponentInfo dlgInfo = dlgEntry.getValue();
            if (dlgInfo.rootDirPath != null) {
              // substitute '$dlg_comp_id$root' macros
              value = InstallationProcessor.substituteCompIdRootInString(value, dlgInfo.getId(),
                      dlgInfo.rootDirPath);
              mAction.params.setProperty(key, value);
            }
          }
          // replace '\' with '/'
          value = mAction.params.getProperty(key);
          mAction.params.setProperty(key, value.replace('\\', '/'));
          if (_mainComponent.rootDirPath != null) {
            // replace all ';' with OS-dependent separator
            // in VAR_VALUE_TAG value
            if (key.equals(InstallationDescriptorHandler.VAR_VALUE_TAG)) {
              value = mAction.params.getProperty(key);
              value = value.replace(';', File.pathSeparatorChar);
              mAction.params.setProperty(key, value);
            }
          }
        }
      }
      _installationActions.add(mAction);
    } else
      _installationActions.add(action);
  }

  /**
   * Adds a specification defined by given name and value to the set of OS specifications. If the OS
   * specifications already contain the given name, adds a new value using PROPERTY_DELIMITER as the
   * delimiter.
   * 
   * @param specName
   *          The given specification name.
   * @param specValue
   *          The given specification value.
   */
  public synchronized void addOSSpec(String specName, String specValue) {
    addProperty(_osSpecs, specName, specValue);
  }

  /**
   * Adds a specification defined by given name and value to the set of Toolkits specifications. If
   * the Toolkits specifications already contain the given name, adds a new value using
   * PROPERTY_DELIMITER as the delimiter.
   * 
   * @param specName
   *          The given specification name.
   * @param specValue
   *          The given specification value.
   */
  public synchronized void addToolkitsSpec(String specName, String specValue) {
    addProperty(_toolkitsSpecs, specName, specValue);
  }

  /**
   * Removes all specified delegate components.
   */
  public synchronized void clearDelegateComponents() {
    _delegateComponents.clear();
  }

  /**
   * Removes all Framework specifications.
   */
  public synchronized void clearFrameworkSpecs() {
    _frameworkSpecs.clear();
  }

  /**
   * Removes all specified installation actions.
   */
  public synchronized void clearInstallationActions() {
    _installationActions.clear();
  }

  /**
   * Removes all OS specifications.
   */
  public synchronized void clearOSSpecs() {
    _osSpecs.clear();
  }

  /**
   * Removes all Toolkits specifications.
   */
  public synchronized void clearToolkitsSpecs() {
    _toolkitsSpecs.clear();
  }

  /**
   * Removes a specified delegate component associated with a given component ID.
   * 
   * @param id
   *          The given delegate component ID to be removed.
   */
  public synchronized void deleteDelegateComponent(String id) {
    _delegateComponents.remove(id);
  }

  /**
   * Removes a specification defined by given name and value from the set of Framework
   * specifications.
   * 
   * @param specName
   *          The given specification name.
   * @param specValue
   *          The given specification value.
   */
  public synchronized void deleteFrameworkSpec(String specName, String specValue) {
    deleteProperty(_frameworkSpecs, specName, specValue);
  }

  /**
   * Removes all installation actions associated with a given action name (FIND_AND_REPLACE_PATH_ACT
   * or SET_ENV_VARIABLE_ACT).
   * 
   * @param actionName
   *          The given action name.
   */
  public synchronized void deleteInstallationActions(String actionName) {
    Iterator<ActionInfo> actionList = _installationActions.iterator();
    while (actionList.hasNext()) {
      ActionInfo action = actionList.next();
      if (action.getName().equals(actionName)) {
        _installationActions.remove(action);
        actionList = _installationActions.iterator();
      }
    }
  }

  /**
   * Removes a specification defined by given name and value from the set of OS specifications.
   * 
   * @param specName
   *          The given specification name.
   * @param specValue
   *          The given specification value.
   */
  public synchronized void deleteOSSpec(String specName, String specValue) {
    deleteProperty(_osSpecs, specName, specValue);
  }

  /**
   * Removes a specification defined by given name and value from the set of Toolkits
   * specifications.
   * 
   * @param specName
   *          The given specification name.
   * @param specValue
   *          The given specification value.
   */
  public synchronized void deleteToolkitsSpec(String specName, String specValue) {
    deleteProperty(_toolkitsSpecs, specName, specValue);
  }

  /**
   * @return The list of the <code>ComponentInfo</code> objects that encapsulate specifications of
   *         the registered delegate components (for aggregate component).
   */
  public Hashtable <String, ComponentInfo>getDelegateComponents() {
    return _delegateComponents;
  }

  /**
   * @return The specifications of the UIMA framework - (key, value) pairs. Value may contain one
   *         string or a list of strings, separated by PROPERTY_DELIMITER.
   */
  public Properties getFrameworkSpecs() {
    return _frameworkSpecs;
  }

  /**
   * @return The list of the <code>ActionInfo</code> objects that encapsulate specifications of
   *         all requested installation actions.
   */
  public Collection<ActionInfo> getInstallationActions() {
    return _installationActions;
  }

  /**
   * Returns the list of specified <code>ActionInfo</code> objects that have a given action name.
   * 
   * @param actionName
   *          The given action name.
   * 
   * @return The list of the <code>ActionInfo</code> objects that have the given action name.
   */
  public Collection<ActionInfo> getInstallationActions(String actionName) {
    ArrayList<ActionInfo> selActions = new ArrayList<ActionInfo>();
    Iterator<ActionInfo> allActions = getInstallationActions().iterator();
    while (allActions.hasNext()) {
      ActionInfo actInfo = allActions.next();
      if (actInfo.getName().equals(actionName))
        selActions.add(actInfo);
    }
    return selActions;
  }

  /**
   * @return The InsD file associated with this object.
   */
  public synchronized File getInstallationDescriptorFile() {
    return _insdFile;
  }

  /**
   * @return Absolute path to the specified CAS Consumer descriptor for the main (submitted)
   *         component, or <code>null</code>, if the main component was not specified.
   */
  public String getMainCasConsumerDesc() {
    return getMainCasConsumerDesc(false);
  }

  /**
   * Returns absolute or relative path to the specified CAS Consumer descriptor for the main
   * (submitted) component, or <code>null</code>, if the main component was not specified. If the
   * relative path is requested, returns the path relative to the main component root dir.
   * 
   * @param relativePath
   *          If <code>true</code>, returns relative path, otherwise returns absolute path.
   * @return Absolute or relative path to the specified CAS Consumer descriptor for the main
   *         (submitted) component, or <code>null</code>, if the main component was not
   *         specified.
   */
  public synchronized String getMainCasConsumerDesc(boolean relativePath) {
    if (_mainComponent != null)
      return relativePath ? getRelativePathForComponentObject(
              _mainComponent.casConsumerDescFilePath, _mainComponent)
              : _mainComponent.casConsumerDescFilePath;
    return null;
  }

  /**
   * @return Absolute path to the specified CAS Initializer descriptor for the main (submitted)
   *         component, or <code>null</code> if the main component was not specified.
   */
  public String getMainCasInitializerDesc() {
    return getMainCasInitializerDesc(false);
  }

  /**
   * Returns absolute or relative path to the specified CAS Initializer descriptor for the main
   * (submitted) component, or <code>null</code>, if the main component was not specified. If the
   * relative path is requested, returns the path relative to the main component root dir.
   * 
   * @param relativePath
   *          If <code>true</code>, returns relative path, otherwise returns absolute path.
   * @return Absolute or relative path to the specified CAS Initializer descriptor for the main
   *         (submitted) component, or <code>null</code>, if the main component was not
   *         specified.
   */
  public synchronized String getMainCasInitializerDesc(boolean relativePath) {
    if (_mainComponent != null)
      return relativePath ? getRelativePathForComponentObject(
              _mainComponent.casInitializerDescFilePath, _mainComponent)
              : _mainComponent.casInitializerDescFilePath;
    return null;
  }

  /**
   * @return Absolute path to the specified Collection Iterator descriptor for the main (submitted)
   *         component, or <code>null</code> if the main component was not specified.
   */
  public String getMainCollIteratorDesc() {
    return getMainCollIteratorDesc(false);
  }

  /**
   * Returns absolute or relative path to the specified Collection Iterator descriptor for the main
   * (submitted) component, or <code>null</code>, if the main component was not specified. If the
   * relative path is requested, returns the path relative to the main component root dir.
   * 
   * @param relativePath
   *          If <code>true</code>, returns relative path, otherwise returns absolute path.
   * @return Absolute or relative path to the specified Collection Iterator descriptor for the main
   *         (submitted) component, or <code>null</code>, if the main component was not
   *         specified.
   */
  public synchronized String getMainCollIteratorDesc(boolean relativePath) {
    if (_mainComponent != null)
      return relativePath ? getRelativePathForComponentObject(
              _mainComponent.collIteratorDescFilePath, _mainComponent)
              : _mainComponent.collIteratorDescFilePath;
    return null;
  }

  /**
   * @return The specified main component deployment type, or default deployment type (<code>standard</code>),
   *         if no deployment type specified.
   */
  public synchronized String getMainComponentDeployment() {
    return (_mainComponent != null) ? _mainComponent.deploymentType : null;
  }

  /**
   * @return Absolute path to the specified XML AE descriptor for the main (submitted) component, or
   *         <code>null</code> if the main component was not specified.
   */
  public String getMainComponentDesc() {
    return getMainComponentDesc(false);
  }

  /**
   * Returns absolute or relative path to the specified XML AE descriptor for the main (submitted)
   * component, or <code>null</code>, if the main component was not specified. If the relative
   * path is requested, returns the path relative to the main component root dir.
   * 
   * @param relativePath
   *          If <code>true</code>, returns relative path, otherwise returns absolute path.
   * @return Absolute or relative path to the specified XML AE descriptor for the main (submitted)
   *         component, or <code>null</code>, if the main component was not specified.
   */
  public synchronized String getMainComponentDesc(boolean relativePath) {
    if (_mainComponent != null) {
      if (_mainComponent.descFilePath != null && _mainComponent.descFilePath.length() > 0) {
        if (_mainComponent.descFilePath.charAt(0) == '$') {
          if (relativePath) {
            int relPathIndex = _mainComponent.descFilePath.indexOf('/') + 1;
            return _mainComponent.descFilePath.substring(relPathIndex);
          } else
            return _mainComponent.descFilePath;
        } else {
          return relativePath ? getRelativePathForComponentObject(_mainComponent.descFilePath,
                  _mainComponent) : _mainComponent.descFilePath;
        }
      } else
        return _mainComponent.descFilePath;
    }
    return null;
  }

  /**
   * @return The specified ID for the main (submitted) component, or <code>null</code> if the main
   *         component was not specified.
   */
  public synchronized String getMainComponentId() {
    return (_mainComponent != null) ? _mainComponent.getId() : null;
  }

  /**
   * @return The specified name for the main (submitted) component, or <code>null</code> if the
   *         main component was not specified.
   */
  public synchronized String getMainComponentName() {
    return (_mainComponent != null) ? _mainComponent.name : null;
  }

  /**
   * @param paramName
   *          The given network component parameter name.
   * @return The specifications of the given network component parameter.
   */
  public synchronized Properties getMainComponentNetworkParam(String paramName) {
    return (_mainComponent != null && _mainComponent.networkParams != null) ? _mainComponent.networkParams
            .get(paramName)
            : null;
  }

  /**
   * @return The <code>Set</code> of the network component parameter names.
   */
  public synchronized Set<String> getMainComponentNetworkParamNames() {
    return (_mainComponent != null && _mainComponent.networkParams != null) ? _mainComponent.networkParams
            .keySet()
            : null;
  }

  /**
   * @return The specified additional properties of the main (submitted) component, or
   *         <code>null</code> if the main component was not specified.
   */
  public synchronized Properties getMainComponentProps() {
    return (_mainComponent != null) ? _mainComponent.props : null;
  }

  /**
   * @return The specified root directory path for the main (submitted) component, or
   *         <code>null</code> if the main component was not specified.
   */
  public synchronized String getMainComponentRoot() {
    return (_mainComponent != null) ? _mainComponent.rootDirPath : null;
  }

  /**
   * @return Main component service specifications, if specified.
   */
  public synchronized ServiceInfo getMainComponentService() {
    return (_mainComponent != null) ? _mainComponent.serviceInfo : null;
  }

  /**
   * @return The specified attributes of the OS environment - (name, value) pairs. Value may contain
   *         one string or a list of strings, separated by PROPERTY_DELIMITER.
   */
  public Properties getOSSpecs() {
    return _osSpecs;
  }

  /**
   * @return The specified attributes of the standard system toolkits - (name, value) pairs. Value
   *         may contain one string or a list of strings, separated by PROPERTY_DELIMITER.
   */
  public Properties getToolkitsSpecs() {
    return _toolkitsSpecs;
  }

  /**
   * Assignes a given installation descriptor file to this object. This method does not perform
   * parsing of the file.
   * 
   * @param insdFile
   *          The given installation descriptor file.
   */
  public synchronized void setInstallationDescriptorFile(File insdFile) {
    _insdFile = insdFile;
  }

  /**
   * Sets a given descriptor file path to a given delegate component.
   * 
   * @param id
   *          The given delegate component ID.
   * @param descFilePath
   *          The given descriptor file path.
   */
  public synchronized void setDelegateComponentDesc(String id, String descFilePath) {
    String dlgId = id.trim();
    ComponentInfo dCompInfo = _delegateComponents.get(dlgId);
    if (dCompInfo != null) {
      if (dCompInfo.rootDirPath == null) // set relative path
        dCompInfo.descFilePath = descFilePath.trim().replace('\\', '/');
      else {
        // substitute '$dlg_comp_id$root' macros
        dCompInfo.descFilePath = InstallationProcessor.substituteCompIdRootInString(
                dCompInfo.descFilePath, dlgId, dCompInfo.rootDirPath);
      }
    }
  }

  /**
   * Assignes a given name to a given delegate component.
   * 
   * @param id
   *          The given delegate component ID.
   * @param name
   *          The given delegate component name.
   */
  public synchronized void setDelegateComponentName(String id, String name) {
    String dlgId = id.trim();
    ComponentInfo dCompInfo = _delegateComponents.get(dlgId);
    if (dCompInfo != null)
      dCompInfo.name = name.trim();
  }

  /**
   * Assignes a given property to a given delegate component.
   * 
   * @param id
   *          The given delegate component ID.
   * @param propName
   *          The given property name.
   * @param propValue
   *          The given property value.
   */
  public synchronized void setDelegateComponentProperty(String id, String propName, String propValue) {
    String dlgId = id.trim();
    ComponentInfo dCompInfo = _delegateComponents.get(dlgId);
    if (dCompInfo != null) {
      if (dCompInfo.props == null)
        dCompInfo.props = new Properties();
      dCompInfo.props.setProperty(propName, propValue.trim());
    }
  }

  /**
   * Sets a given directory path as the root path for a given delegate component.
   * 
   * @param id
   *          The given delegate component ID.
   * @param rootDirPath
   *          The given root directory path.
   */
  public synchronized void setDelegateComponentRoot(String id, String rootDirPath) {
    String dlgId = id.trim();
    ComponentInfo dCompInfo = _delegateComponents.get(dlgId);
    if (dCompInfo != null) {
      dCompInfo.rootDirPath = rootDirPath.trim().replace('\\', '/');
      // substitute $dlg_comp_id$root macros in the
      // delegate descriptor path
      if (dCompInfo.descFilePath != null) {
        dCompInfo.descFilePath = InstallationProcessor.substituteCompIdRootInString(
                dCompInfo.descFilePath, dlgId, dCompInfo.rootDirPath);
      }
    }
    // substitute $dlg_comp_id$root macros in apropriate
    // action fields
    Iterator<ActionInfo> list = getInstallationActions().iterator();
    while (list.hasNext()) {
      ActionInfo action = list.next();
      Enumeration<Object> keys = action.params.keys();
      while (keys.hasMoreElements()) {
        String key = (String) keys.nextElement();
        String value = action.params.getProperty(key);
        if (key.equals(InstallationDescriptorHandler.FILE_TAG)
                || key.equals(InstallationDescriptorHandler.REPLACE_WITH_TAG)
                || key.equals(InstallationDescriptorHandler.VAR_VALUE_TAG)) {
          value = InstallationProcessor.substituteCompIdRootInString(value, dlgId,
                  dCompInfo.rootDirPath);
          action.params.setProperty(key, value);
        }
      }
    }
  }

  /**
   * Sets a given main CAS consumer descriptor file.
   * 
   * @param descFilePath
   *          The given CAS consumer descriptor file path.
   */
  public synchronized void setMainCasConsumerDesc(String descFilePath) {
    if (_mainComponent != null) {
      if (_mainComponent.rootDirPath == null) // set relative path
        _mainComponent.casConsumerDescFilePath = descFilePath.trim().replace('\\', '/');
      else { // substitute $main_root macros in descriptor path
        _mainComponent.casConsumerDescFilePath = InstallationProcessor.substituteMainRootInString(
                descFilePath, _mainComponent.rootDirPath);
      }
    }
  }

  /**
   * Sets a given main CAS initializer descriptor file.
   * 
   * @param descFilePath
   *          The given CAS initializer descriptor file path.
   */
  public synchronized void setMainCasInitializerDesc(String descFilePath) {
    if (_mainComponent != null) {
      if (_mainComponent.rootDirPath == null) // set relative path
        _mainComponent.casInitializerDescFilePath = descFilePath.trim().replace('\\', '/');
      else { // substitute $main_root macros in descriptor path
        _mainComponent.casInitializerDescFilePath = InstallationProcessor
                .substituteMainRootInString(descFilePath, _mainComponent.rootDirPath);
      }
    }
  }

  /**
   * Sets a given main Collection iterator descriptor file.
   * 
   * @param descFilePath
   *          The given Collection iterator descriptor file path.
   */
  public synchronized void setMainCollIteratorDesc(String descFilePath) {
    if (_mainComponent != null) {
      if (_mainComponent.rootDirPath == null) // set relative path
        _mainComponent.collIteratorDescFilePath = descFilePath.trim().replace('\\', '/');
      else { // substitute $main_root macros in descriptor path
        _mainComponent.collIteratorDescFilePath = InstallationProcessor.substituteMainRootInString(
                descFilePath, _mainComponent.rootDirPath);
      }
    }
  }

  /**
   * Sets a given main component using a given component ID. This method creates a new main
   * component instance, overriding all previously set attributes of the main component.
   * 
   * @param id
   *          The given main component ID.
   */
  public synchronized void setMainComponent(String id) {
    setMainComponent(id, "");
  }

  /**
   * Sets a given main component using given component ID and name. This method creates a new main
   * component instance, overriding all previously set attributes of the main component.
   * 
   * @param id
   *          The given main component ID.
   * @param name
   *          The given main component name.
   */
  public synchronized void setMainComponent(String id, String name) {
    _mainComponent = new ComponentInfo(id);
    setMainComponentName(name);
  }

  /**
   * Sets a given main component deployment type: <code>standard</code>, <code>service</code>
   * or <code>network</code>.
   * 
   * @param deplType
   *          The specified deployment type.
   */
  public synchronized void setMainComponentDeployment(String deplType) {
    if (_mainComponent != null)
      _mainComponent.deploymentType = deplType;
  }

  /**
   * Sets a given main component descriptor file.
   * 
   * @param descFilePath
   *          The given main component descriptor file path.
   */
  public synchronized void setMainComponentDesc(String descFilePath) {
    if (_mainComponent != null) {
      if (_mainComponent.rootDirPath == null) // set relative path
        _mainComponent.descFilePath = descFilePath.trim().replace('\\', '/');
      else { // substitute $main_root macros in descriptor path
        _mainComponent.descFilePath = InstallationProcessor.substituteMainRootInString(
                descFilePath, _mainComponent.rootDirPath);
      }
    }
  }

  /**
   * Replaces existing main component ID with a given new ID.
   * 
   * @param id
   *          The given new ID of the main component.
   */
  public synchronized void setMainComponentId(String id) {
    if (_mainComponent != null)
      _mainComponent._id = id.trim();
  }

  /**
   * Sets a given main component name.
   * 
   * @param name
   *          The given main component name.
   */
  public synchronized void setMainComponentName(String name) {
    if (_mainComponent != null) {
      _mainComponent.name = name.trim();
    }
  }

  /**
   * Sets a specifications of a given network component parameter.
   * 
   * @param paramName
   *          The given network component parameter name.
   * @param paramSpecs
   *          The specifications of the given network component parameter.
   */
  public synchronized void setMainComponentNetworkParam(String paramName, Properties paramSpecs) {
    if (_mainComponent != null) {
      if (_mainComponent.networkParams == null)
        _mainComponent.networkParams = new Hashtable<String, Properties>();
      _mainComponent.networkParams.put(paramName, paramSpecs);
    }
  }

  /**
   * Sets a given main component property.
   * 
   * @param propNname
   *          The given property name.
   * @param propValue
   *          The given property value.
   */
  public synchronized void setMainComponentProperty(String propNname, String propValue) {
    if (_mainComponent != null) {
      if (_mainComponent.props == null)
        _mainComponent.props = new Properties();
      _mainComponent.props.setProperty(propNname, propValue.trim());
    }
  }

  /**
   * Sets a given main component root directory.
   * 
   * @param rootDirPath
   *          The given main component root directory path.
   */
  public synchronized void setMainComponentRoot(String rootDirPath) {
    if (_mainComponent != null) {
      _mainComponent.rootDirPath = rootDirPath.trim().replace('\\', '/');
      // substitute $main_root macros in all specs of descriptor files
      if (_mainComponent.descFilePath != null)
        _mainComponent.descFilePath = InstallationProcessor.substituteMainRootInString(
                _mainComponent.descFilePath, _mainComponent.rootDirPath);
      if (_mainComponent.collIteratorDescFilePath != null)
        _mainComponent.collIteratorDescFilePath = InstallationProcessor.substituteMainRootInString(
                _mainComponent.collIteratorDescFilePath, _mainComponent.rootDirPath);
      if (_mainComponent.casInitializerDescFilePath != null)
        _mainComponent.casInitializerDescFilePath = InstallationProcessor
                .substituteMainRootInString(_mainComponent.casInitializerDescFilePath,
                        _mainComponent.rootDirPath);
      if (_mainComponent.casConsumerDescFilePath != null)
        _mainComponent.casConsumerDescFilePath = InstallationProcessor.substituteMainRootInString(
                _mainComponent.casConsumerDescFilePath, _mainComponent.rootDirPath);
      // substitute $main_root macros in all specs of actions
      Iterator<ActionInfo> list = getInstallationActions().iterator();
      while (list.hasNext()) {
        ActionInfo action = list.next();
        Enumeration<Object> keys = action.params.keys();
        while (keys.hasMoreElements()) {
          String key = (String) keys.nextElement();
          String value = action.params.getProperty(key);
          if (key.equals(InstallationDescriptorHandler.FILE_TAG)
                  || key.equals(InstallationDescriptorHandler.REPLACE_WITH_TAG)
                  || key.equals(InstallationDescriptorHandler.VAR_VALUE_TAG)) {
            value = InstallationProcessor.substituteMainRootInString(value,
                    _mainComponent.rootDirPath);
            action.params.setProperty(key, value);
          }
        }
      }
    }
  }

  /**
   * Sets a given main component service specifications. The service specifications are valid only
   * for <code>service</code> deployment type.
   * 
   * @param serviceInfo
   *          The given main component service specifications.
   */
  public synchronized void setMainComponentService(ServiceInfo serviceInfo) {
    if (_mainComponent != null)
      _mainComponent.serviceInfo = serviceInfo;
  }

  /**
   * @return String representation of the InsD object.
   * @see java.lang.Object#toString()
   */
  public String toString() {
    StringWriter sWriter = new StringWriter();
    PrintWriter oWriter = null;
    try {
      oWriter = new PrintWriter(sWriter);
      InstallationDescriptorHandler.printInstallationDescriptor(this, oWriter);
      oWriter.flush();
    } catch (Exception exc) {
    } finally {
      if (oWriter != null) {
        try {
          oWriter.close();
        } catch (Exception e) {
        }
      }
    }
    return sWriter.toString();
  }

  /**
   * Replaces existing main component ID with a given new ID.
   * 
   * @param id
   *          The given new ID of the main component.
   * @deprecated Use setMainComponentId() method instead.
   */
  @Deprecated
  public synchronized void updateMainComponentId(String id) {
    setMainComponentId(id);
  }
}
