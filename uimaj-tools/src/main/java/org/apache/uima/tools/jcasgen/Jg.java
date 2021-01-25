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

package org.apache.uima.tools.jcasgen;

/*
 * One Jg instance per use. --> GUI instance (if GUI used) which is a JFrame
 */

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.UIManager;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.impl.BuiltinTypeKinds;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.impl.TypeImpl_string;
import org.apache.uima.cas.impl.TypeSystemImpl;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.metadata.FeatureDescription;
import org.apache.uima.resource.metadata.FsIndexDescription;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypePriorities;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.apache.uima.util.XMLizable;


/**
 * Class holds type plugin-wide collections and static methods. Also implements the runnable that is
 * called to do the processing
 */

public class Jg {

  private final static boolean IS_TRACE_LIMITED = false;
  /**
   * Interface implemented by JCAS code generation's templates.
   */
  public interface IJCasTypeTemplate {
    
    /**
     * Generate.
     *
     * @param argument the argument
     * @return the string
     */
    public String generate(Object argument);
  }

  /** The Constant jControlModel. */
  static final String jControlModel = "jMergeCtl.xml";

  /** The Constant featureDescriptionArray0. */
  static final FeatureDescription[] featureDescriptionArray0 = new FeatureDescription[0];

  /** The Constant reservedFeatureNames. */
  static final Collection<String> reservedFeatureNames = new ArrayList<>();
  {

    reservedFeatureNames.add("Address");
    reservedFeatureNames.add("CAS");
    reservedFeatureNames.add("CASImpl");
    reservedFeatureNames.add("Class");
    reservedFeatureNames.add("FeatureValue");
    reservedFeatureNames.add("FeatureValueAsString");
    reservedFeatureNames.add("FeatureValueFromString");
    reservedFeatureNames.add("FloatValue");
    reservedFeatureNames.add("IntValue");
    reservedFeatureNames.add("LowLevelCas");
    reservedFeatureNames.add("StringValue");
    reservedFeatureNames.add("Type");
    reservedFeatureNames.add("View");
    reservedFeatureNames.add("TypeIndexID");
  }

  /**
   * Set of types not generated from the CAS type set because they're already in existence as
   * builtins in the JCas impl. and if they're generated, the generated forms are wrong.
   */
  static final Set<String> noGenTypes = new HashSet<>();

  /** The Constant casCreateProperties. */
  public static final Properties casCreateProperties = new Properties();
  static {
    casCreateProperties.setProperty(UIMAFramework.CAS_INITIAL_HEAP_SIZE, "200");
  }

  /** The Constant extendableBuiltInTypes. */
  static final Map<String, FeatureDescription[]> extendableBuiltInTypes = new HashMap<>();

  // create a hash map of built-in types, where the
  // key is the fully-qualified name "uima.tcas.Annotation"
  // and the value is a Collection of FeatureDescription objects,
  // representing the features. These will be merged with the
  // specified features. Supertype features are not included.

  /** The Constant emptyFds. */
  static final FeatureDescription[] emptyFds = new FeatureDescription[0];

  /** The built in type system. */
  static TypeSystem builtInTypeSystem;

  static {
    CAS tcas = null;
    try {
      tcas = CasCreationUtils.createCas((TypeSystemDescription) null, null,
              new FsIndexDescription[0], casCreateProperties);

    } catch (ResourceInitializationException e1) {
      // never get here
    }

    ((CASImpl) tcas).commitTypeSystem();
    builtInTypeSystem = ((CASImpl) tcas).getTypeSystemImpl();  // follow commit because commit may reuse existing type system

    // setup noGen for semibuiltin types 
    noGenTypes.add("org.apache.uima.jcas.cas.FSArrayList");
    noGenTypes.add("org.apache.uima.jcas.cas.IntegerArrayList");
    noGenTypes.add("org.apache.uima.jcas.cas.FSHashSet");
    
    for (Iterator<Type> it = builtInTypeSystem.getTypeIterator(); it.hasNext();) {
      Type type = it.next();
      if (type.isFeatureFinal()) {
        noGenTypes.add(type.getName());
        continue;  // skip if feature final
      }
      String typeName = type.getName();
      List<Feature> fs = type.getFeatures();
      List<Feature> features = new ArrayList<>(fs.size());
      // get list of features defined in this type excluding those defined in supertypes
      for (int i = 0; i < fs.size(); i++) {
        Feature f = fs.get(i);
        String fName = f.getName();
        String fTypeName = fName.substring(0, fName.indexOf(':'));
        if (typeName.equals(fTypeName))
          features.add(f);
      }
      FeatureDescription[] fds = new FeatureDescription[features.size()];
      for (int i = 0; i < features.size(); i++) {
        FeatureDescription fd = UIMAFramework.getResourceSpecifierFactory()
                .createFeatureDescription();
        Feature f = features.get(i);
        fd.setName(f.getShortName());
        fd.setRangeTypeName(f.getRange().getName());
        fds[i] = fd;
      }
      extendableBuiltInTypes.put(typeName, fds);
    }
  }

  /** The built in types. */
  // table builtInTypes initialized inside TypeInfo constructor
  static Map<String, TypeInfo> builtInTypes = new HashMap<>();

  /**
   * Adds the built in type info.
   *
   * @param casName the cas name
   * @param javaName the java name
   * @param casElementName the cas element name
   */
  static private void addBuiltInTypeInfo(String casName, String javaName, String casElementName) {
    TypeInfo ti = new TypeInfo(casName, javaName, casElementName);
    builtInTypes.put(casName, ti);
  }

  /**
   * Adds the built in type info.
   *
   * @param casName the cas name
   * @param javaName the java name
   */
  static private void addBuiltInTypeInfo(String casName, String javaName) {
    addBuiltInTypeInfo(casName, javaName, null);
  }

  static private List<String> genericFeatureDescriptorTypes = Arrays.asList("uima.cas.FSArray", "uima.cas.FSList");
  // first type needed by fsArrayType; in hash map will be overwritten, though
  static {
//    addBuiltInTypeInfo("uima.cas.TOP", "org.apache.uima.cas.FeatureStructure"); // overridden below
    addBuiltInTypeInfo("uima.cas.Integer", "int");
    addBuiltInTypeInfo("uima.cas.Float", "float");
    addBuiltInTypeInfo("uima.cas.String", "String");
    addBuiltInTypeInfo("uima.cas.Byte", "byte");
    addBuiltInTypeInfo("uima.cas.Short", "short");
    addBuiltInTypeInfo("uima.cas.Long", "long");
    addBuiltInTypeInfo("uima.cas.Double", "double");
    addBuiltInTypeInfo("uima.cas.Boolean", "boolean");

    addBuiltInTypeInfo("uima.cas.TOP", "org.apache.uima.jcas.cas.TOP");
    addBuiltInTypeInfo("uima.cas.FSArray", "org.apache.uima.jcas.cas.FSArray", "uima.cas.TOP");
    addBuiltInTypeInfo("uima.cas.IntegerArray", "org.apache.uima.jcas.cas.IntegerArray",
            "uima.cas.Integer");
    addBuiltInTypeInfo("uima.cas.FloatArray", "org.apache.uima.jcas.cas.FloatArray",
            "uima.cas.Float");
    addBuiltInTypeInfo("uima.cas.StringArray", "org.apache.uima.jcas.cas.StringArray",
            "uima.cas.String");
    addBuiltInTypeInfo("uima.cas.BooleanArray", "org.apache.uima.jcas.cas.BooleanArray",
            "uima.cas.Boolean");
    addBuiltInTypeInfo("uima.cas.ByteArray", "org.apache.uima.jcas.cas.ByteArray", "uima.cas.Byte");
    addBuiltInTypeInfo("uima.cas.ShortArray", "org.apache.uima.jcas.cas.ShortArray",
            "uima.cas.Short");
    addBuiltInTypeInfo("uima.cas.LongArray", "org.apache.uima.jcas.cas.LongArray", "uima.cas.Long");
    addBuiltInTypeInfo("uima.cas.DoubleArray", "org.apache.uima.jcas.cas.DoubleArray",
            "uima.cas.Double");
    addBuiltInTypeInfo("uima.cas.AnnotationBase", "org.apache.uima.jcas.cas.AnnotationBase");
    addBuiltInTypeInfo("uima.tcas.Annotation", "org.apache.uima.jcas.tcas.Annotation");
    addBuiltInTypeInfo("uima.tcas.DocumentAnnotation",
            "org.apache.uima.jcas.tcas.DocumentAnnotation");
    addBuiltInTypeInfo("uima.cas.EmptyFloatList", "org.apache.uima.jcas.cas.EmptyFloatList");
    addBuiltInTypeInfo("uima.cas.EmptyFSList", "org.apache.uima.jcas.cas.EmptyFSList");
    addBuiltInTypeInfo("uima.cas.EmptyIntegerList", "org.apache.uima.jcas.cas.EmptyIntegerList");
    addBuiltInTypeInfo("uima.cas.EmptyStringList", "org.apache.uima.jcas.cas.EmptyStringList");
    addBuiltInTypeInfo("uima.cas.FloatList", "org.apache.uima.jcas.cas.FloatList");
    addBuiltInTypeInfo("uima.cas.FSList", "org.apache.uima.jcas.cas.FSList");
    addBuiltInTypeInfo("uima.cas.IntegerList", "org.apache.uima.jcas.cas.IntegerList");
    addBuiltInTypeInfo("uima.cas.StringList", "org.apache.uima.jcas.cas.StringList");
    addBuiltInTypeInfo("uima.cas.NonEmptyFloatList", "org.apache.uima.jcas.cas.NonEmptyFloatList");
    addBuiltInTypeInfo("uima.cas.NonEmptyFSList", "org.apache.uima.jcas.cas.NonEmptyFSList");
    addBuiltInTypeInfo("uima.cas.NonEmptyIntegerList",
            "org.apache.uima.jcas.cas.NonEmptyIntegerList");
    addBuiltInTypeInfo("uima.cas.NonEmptyStringList", "org.apache.uima.jcas.cas.NonEmptyStringList");
    addBuiltInTypeInfo("uima.cas.Sofa", "org.apache.uima.jcas.cas.Sofa");
    // not built-in
//    addBuiltInTypeInfo("uima.cas.IntegerArrayList", "org.apache.uima.jcas.cas.IntegerArrayList");
//    addBuiltInTypeInfo("uima.cas.FSArrayList", "org.apache.uima.jcas.cas.FSArrayList");
//    addBuiltInTypeInfo("uima.cas.FSHashSet", "org.apache.uima.jcas.cas.FSHashSet");
  }

  /** The resource bundle. */
  // Resource bundle.
  private static ResourceBundle resourceBundle;
  static {
    try {
      resourceBundle = ResourceBundle
              .getBundle("org.apache.uima.tools.jcasgen.jcasgenpPluginResources");
    } catch (MissingResourceException x) {
      resourceBundle = null;
    }
  }

  /** The imports. */
  // Instance fields
  final Map<String, String> imports = new HashMap<>(); // can't be final - one per instance running

  /** The imports. */
  final Map<String, String> _imports = new HashMap<>();

  /** The class path. */
  String classPath = "";

  /** The xml source file name. */
  String xmlSourceFileName;

  /** The cas. */
  CAS cas;

  /** The gui. */
  GUI gui;

  /** The merger. */
  IMerge merger;

  /** The progress monitor. */
  IProgressMonitor progressMonitor;

  /** The error. */
  public IError error; // referenced by the plugin

  /** The waiter. */
  Waiter waiter;

  /** The package name. */
  String packageName;

  /** The simple class name. */
  String simpleClassName;

  /** The type system. */
  private TypeSystem typeSystem = null;

  /** The cas string type. */
  private Type casStringType;

  /** The tcas annotation type. */
  private Type tcasAnnotationType;

  /** The merged types adding features. */
  private Map<String, Set<String>> mergedTypesAddingFeatures = new TreeMap<>(); // a Map of types and the xml files that were merged to create them

  /** The project path dir. */
  private String projectPathDir;  

  /** The limit J cas gen to project scope. */
  private boolean limitJCasGenToProjectScope;

  /**
   * Instantiates a new jg.
   */
  public Jg() { // default constructor
  }

  /**
   * Returns the string from the plugin's resource bundle, or 'key' if not found.
   *
   * @param key the key
   * @return the resource string
   */
  public String getResourceString(String key) {
    ResourceBundle bundle = getResourceBundle();
    try {
      return bundle.getString(key);
    } catch (MissingResourceException e) {
      return key;
    }
  }

  /**
   * Gets the string.
   *
   * @param key the key
   * @param substitutions the substitutions
   * @return the string
   */
  public String getString(String key, Object[] substitutions) {
    return MessageFormat.format(getResourceString(key), substitutions);
  }

  /**
   * Returns the plugin's resource bundle,.
   *
   * @return the resource bundle
   */
  public ResourceBundle getResourceBundle() {
    return resourceBundle;
  }

  /**
   * The Class ErrorExit.
   */
  public static class ErrorExit extends RuntimeException {
    
    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = -3314235749649859540L;
  }

  // ************
  // * driveGui *
  /**
   * Drive gui.
   */
  // ************
  public void driveGui() {
    // usingGui = true;
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (Exception e) {
      System.err.println("Could not set look and feel: " + e.getMessage());
    }

    gui = new GUI(this);
    gui.addWindowListener(new WindowAdapter() {
      @Override
    public void windowClosing(WindowEvent e) {
        Prefs.set(gui);
        waiter.finished();
      }
    });
    Prefs.get(gui);
    gui.pnG.taStatus.setLineWrap(true);
    gui.pnG.taStatus.setWrapStyleWord(true);
    gui.setVisible(true);
    waiter = new Waiter();
    waiter.waitforGUI();
  }

  /**
   * The main method.
   *
   * @param args the arguments
   */
  // exits with -1 if failure
  public static void main(String[] args) {
    int rc = (new Jg()).main0(args, null, null, new LogThrowErrorImpl());
    System.exit(rc);
  }

  /**
   * Main for cde.
   *
   * @param aMerger the a merger
   * @param aProgressMonitor the a progress monitor
   * @param aError the a error
   * @param inputFile the input file
   * @param outputDirectory the output directory
   * @param tds the tds
   * @param aCas the a cas
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public void mainForCde(IMerge aMerger, IProgressMonitor aProgressMonitor, IError aError,
          String inputFile, String outputDirectory, TypeDescription[] tds, CASImpl aCas)
          throws IOException {
    mainForCde(aMerger, aProgressMonitor, aError,
               inputFile, outputDirectory, tds, aCas, "", false, null);
  }
  
  /**
   * Main for cde.
   *
   * @param aMerger the a merger
   * @param aProgressMonitor the a progress monitor
   * @param aError the a error
   * @param inputFile the input file
   * @param outputDirectory the output directory
   * @param tds the tds
   * @param aCas the a cas
   * @param pProjectPathDir the project path dir
   * @param limitToProjectScope the limit J cas gen to project scope
   * @param pMergedTypesAddingFeatures the merged types adding features
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public void mainForCde(IMerge aMerger, IProgressMonitor aProgressMonitor, IError aError,
          String inputFile, String outputDirectory, TypeDescription[] tds, CASImpl aCas, 
          String pProjectPathDir, boolean limitToProjectScope, 
          Map<String, Set<String>> pMergedTypesAddingFeatures)
          throws IOException {
    try {
      // Generate type classes by using DEFAULT templates
      mainGenerateAllTypesFromTemplates(aMerger, aProgressMonitor, aError, inputFile,
              outputDirectory, tds, aCas, JCasTypeTemplate.class,
              pProjectPathDir, limitToProjectScope, pMergedTypesAddingFeatures);
      // convert thrown things to IOExceptions to avoid changing API for this
      // FIXME later
    } catch (InstantiationException e) {
      throw new IOException(e.toString());
    } catch (IllegalAccessException e) {
      throw new IOException(e.toString());
    }    
  }

  /**
   * Main generate all types from templates.
   *
   * @param aMerger the a merger
   * @param aProgressMonitor the a progress monitor
   * @param aError the a error
   * @param inputFile the input file
   * @param outputDirectory the output directory
   * @param tds the tds
   * @param aCas the a cas
   * @param jcasTypeClass the jcas type class
   * @param jcas_TypeClass the jcas type class
   * @throws IOException Signals that an I/O exception has occurred.
   * @throws InstantiationException the instantiation exception
   * @throws IllegalAccessException the illegal access exception
   */
  // use template classes to generate code
  public void mainGenerateAllTypesFromTemplates(IMerge aMerger, IProgressMonitor aProgressMonitor,
      IError aError, String inputFile, String outputDirectory, TypeDescription[] tds,
      CASImpl aCas, Class<JCasTypeTemplate> jcasTypeClass, // Template class
      Class<JCasTypeTemplate> jcas_TypeClass) // Template class
      throws IOException, InstantiationException, IllegalAccessException {
    mainGenerateAllTypesFromTemplates(aMerger, aProgressMonitor, 
             aError, inputFile, outputDirectory, tds, aCas, 
             jcasTypeClass, "", false, null);
  }
  
  /**
   * Main generate all types from templates.
   *
   * @param aMerger the a merger
   * @param aProgressMonitor the a progress monitor
   * @param aError the a error
   * @param inputFile the input file
   * @param outputDirectory the output directory
   * @param tds the tds
   * @param aCas the a cas
   * @param jcasTypeClass the jcas type class
   * @param pProjectPathDir the project path dir
   * @param limitToProjectScope the limit J cas gen to project scope
   * @param pMergedTypesAddingFeatures the merged types adding features
   * @throws IOException Signals that an I/O exception has occurred.
   * @throws InstantiationException the instantiation exception
   * @throws IllegalAccessException the illegal access exception
   */
  public void mainGenerateAllTypesFromTemplates(IMerge aMerger, IProgressMonitor aProgressMonitor,
          IError aError, String inputFile, String outputDirectory, TypeDescription[] tds,
          CASImpl aCas, Class<JCasTypeTemplate> jcasTypeClass, // Template class
          String pProjectPathDir, boolean limitToProjectScope,
          Map<String, Set<String>> pMergedTypesAddingFeatures) // Template class
          throws IOException, InstantiationException, IllegalAccessException {
    this.merger = aMerger;
    this.error = aError;
    this.progressMonitor = aProgressMonitor;
    xmlSourceFileName = inputFile.replaceAll("\\\\", "/");
    this.projectPathDir = pProjectPathDir;
    this.limitJCasGenToProjectScope = limitToProjectScope;
    this.mergedTypesAddingFeatures = pMergedTypesAddingFeatures;

    // Generate type classes by using SPECIFIED templates
    generateAllTypesFromTemplates(outputDirectory, tds, aCas, jcasTypeClass);
  }

  /**
   * Main 0.
   *
   * @param args the args
   * @param aMerger the a merger
   * @param aProgressMonitor the a progress monitor
   * @param aError the a error
   * @return the int
   */
  public int main0(String[] args, IMerge aMerger, IProgressMonitor aProgressMonitor, IError aError) {
    this.merger = aMerger;
    this.error = aError;
    this.progressMonitor = aProgressMonitor;
    boolean foundInput = false;
    for (int i = 0; i < args.length; i++) {
      if (args[i].equals("-jcasgeninput")) {
        if (i == args.length - 1) {
          driveGui();
          return 0;
        } else {
          foundInput = true;
          break;
        }
      }
    }
    if (foundInput)
      return main1(args);
    else {
      driveGui();
      return 0;
    }
  }

  /**
   * Arguments are:
   *   -jcasgeninput xxxx
   *   -jcasgenoutput  xxxx
   *   -jcasgenclasspath xxxx
   *   
   *
   * @param arguments the arguments
   * @return the int
   */
  public int main1(String[] arguments) {
    boolean hadError = false;
    try {
      try {
        if (null == progressMonitor) {
          progressMonitor = new UimaLoggerProgressMonitor();
        }
        if (null == error)
          error = new LogThrowErrorImpl();

        String inputFile = null;
        String outputDirectory = null;

        TypeSystemDescription typeSystemDescription = null;
        TypeDescription[] tds = null;
        
        projectPathDir = "";  // init to default value
        limitJCasGenToProjectScope = false;

        for (int i = 0; i < arguments.length - 1; i++) {
          if (arguments[i].equalsIgnoreCase("-jcasgeninput")) {
            inputFile = arguments[++i];
            continue;
          }
          if (arguments[i].equalsIgnoreCase("-jcasgenoutput")) {
            outputDirectory = arguments[++i];
            continue;
          }
          // This is used by the jcasgen maven plugin 
          if (arguments[i].equalsIgnoreCase("=jcasgenclasspath") || // https://issues.apache.org/jira/browse/UIMA-3044
              arguments[i].equalsIgnoreCase("-jcasgenclasspath")) { // https://issues.apache.org/jira/browse/UIMA-3044
            classPath = arguments[++i];
            continue;
          }
          if (arguments[i].equalsIgnoreCase("-limitToDirectory")) {
            projectPathDir = arguments[++i];
            limitJCasGenToProjectScope = (projectPathDir.length() > 0);
            continue;
          }
        }

        xmlSourceFileName = inputFile.replaceAll("\\\\", "/");
        URL url;
        if(inputFile.substring(0, 4).equalsIgnoreCase("jar:")) {
          // https://issues.apache.org/jira/browse/UIMA-1793 get things out of Jars
        	try {
        		url = new URL(inputFile);
//          	if (null == url) {     // is never null from above line
//          		error.newError(IError.ERROR, getString("fileNotFound", new Object[] { inputFile }), null);
//          	}
          	if(null == outputDirectory || outputDirectory.equals("")) {
          		error.newError(IError.ERROR, getString("sourceArgNeedsDirectory", new Object[] { inputFile }), null);
          	}
        	} catch (MalformedURLException e) {
        		error.newError(IError.ERROR, getString("fileNotFound", new Object[] { inputFile }), null);
        		url = null;  // never get here, the previous statement throws.  Needed, though for java path analysis.
        	}
        } else {
        	File file = new File(inputFile);
          if (!file.exists()) {
              error.newError(IError.ERROR, getString("fileNotFound", new Object[] { inputFile }), null);
          }
          if (null == outputDirectory || outputDirectory.equals("")) {
            File dir = file.getParentFile();
            if (null == dir) {
              error.newError(IError.ERROR, getString("sourceArgNeedsDirectory",
                      new Object[] { inputFile }), null);
            }
            outputDirectory = dir.getPath() + File.separator + "JCas"
                    + ((null != merger) ? "" : "New");
          }
          url = file.toURI().toURL();
        }

        progressMonitor.beginTask("", 5);
        progressMonitor.subTask("Output going to '" + outputDirectory + "'");
        progressMonitor.subTask(getString("ReadingDescriptorAndCreatingTypes",
                new Object[] { inputFile }));
        // code to read xml and make cas type instance
        CASImpl casLocal = null;
        // handle classpath
        try {
          XMLInputSource in = new XMLInputSource(url);
          XMLizable specifier = UIMAFramework.getXMLParser().parse(in);

          mergedTypesAddingFeatures.clear();
          if (specifier instanceof AnalysisEngineDescription) {
            AnalysisEngineDescription aeSpecifier = (AnalysisEngineDescription) specifier;
            if (!aeSpecifier.isPrimitive())
              typeSystemDescription = CasCreationUtils.mergeDelegateAnalysisEngineTypeSystems(
                      aeSpecifier, createResourceManager(), mergedTypesAddingFeatures);
            else
              typeSystemDescription = mergeTypeSystemImports(aeSpecifier
                      .getAnalysisEngineMetaData().getTypeSystem());

          } else if (specifier instanceof TypeSystemDescription)
            typeSystemDescription = mergeTypeSystemImports(((TypeSystemDescription) specifier));
          else {
            error.newError(IError.ERROR, getString("fileDoesntParse", new Object[] { inputFile }),
                    null);
          }
          if (mergedTypesAddingFeatures.size() > 0) {
            error.newError(IError.WARN, getString("typesHaveFeaturesAdded",
                    new Object[] { makeMergeMessage(mergedTypesAddingFeatures) }), null);
          }
          TypePriorities typePriorities = null;
          FsIndexDescription[] fsIndexDescription = null;
          try {

            // no ResourceManager, since everything has been
            // imported/merged by previous actions
            casLocal = (CASImpl) CasCreationUtils.createCas(typeSystemDescription, typePriorities,
                    fsIndexDescription);
          } catch (ResourceInitializationException e) {
            error.newError(IError.WARN, getString("resourceInitializationException",
                    new Object[] { e.getLocalizedMessage() }), e);
            casLocal = null; // continue with null cas, anyway
          }

        } catch (IOException e) {
          e.printStackTrace();
        } catch (InvalidXMLException e) {
          error.newError(IError.ERROR, getString("invalidXML", new Object[] { inputFile }), e);
        } catch (ResourceInitializationException e) {
          error.newError(IError.ERROR, getString("resourceInitializationExceptionError",
                  new Object[] {}), e);
        }

        progressMonitor.worked(1);
        tds = typeSystemDescription.getTypes();

        // Generate type classes from DEFAULT templates
        generateAllTypesFromTemplates(outputDirectory, tds, casLocal, JCasTypeTemplate.class);

      } catch (IOException e) {
        error.newError(IError.ERROR, getString("IOException", new Object[] {}), e);
      } catch (ErrorExit e) { 
        hadError = true;
      } catch (InstantiationException e) {
        error.newError(IError.ERROR, getString("InstantiationException", new Object[] {}), e);
      } catch (IllegalAccessException e) {
        error.newError(IError.ERROR, getString("IllegalAccessException", new Object[] {}), e);
      }
    } finally {
      progressMonitor.done();
    }
    return (hadError) ? -1 : 0;    
  }

  /**
   * Make merge message.
   *
   * @param m the m
   * @return the string
   */
  // message: TypeName = ".....", URLs defining this type = "xxxx", "xxxx", ....
  private String makeMergeMessage(Map<String, Set<String>> m) {
    StringBuffer sb = new StringBuffer();
    for (Map.Entry<String, Set<String>> entry :  m.entrySet()) {
      String typeName = entry.getKey();
      sb.append("\n  ");
      sb.append("TypeName having merged features = ").append(typeName).append(
              "\n    URLs defining this type =");
      Set<String> urls = entry.getValue();
      boolean afterFirst = false;
      for (String url : urls) {
        sb.append(afterFirst ? ',' : "")
          .append("\n        \"");
        afterFirst = true;
        sb.append('"').append(url).append('"');
      }
    }
    return sb.toString();
  }

  /**
   * Generate all types from templates.
   *
   * @param outputDirectory the output directory
   * @param tds the tds
   * @param aCas the a cas
   * @param jcasTypeClass the jcas type class
   * @throws IOException Signals that an I/O exception has occurred.
   * @throws InstantiationException the instantiation exception
   * @throws IllegalAccessException the illegal access exception
   */
  // This is also the interface for CDE
  private void generateAllTypesFromTemplates(String outputDirectory, TypeDescription[] tds,
          CASImpl aCas, Class<JCasTypeTemplate> jcasTypeClass) throws IOException,
          InstantiationException, IllegalAccessException {

    // Create instances of Template classes
    IJCasTypeTemplate jcasTypeInstance = (IJCasTypeTemplate) jcasTypeClass.newInstance();

    Set<String> generatedBuiltInTypes = new TreeSet<>();

    this.cas = aCas;
    this.typeSystem = cas.getTypeSystem();
    this.casStringType = typeSystem.getType(CAS.TYPE_NAME_STRING);
    this.tcasAnnotationType = typeSystem.getType(CAS.TYPE_NAME_ANNOTATION);

    for (int i = 0; i < tds.length; i++) {
      TypeDescription td = tds[i];
      // System.out.println("Description: " + td.getDescription() );
      if (noGenTypes.contains(td.getName()))
        continue;
      if (td.getSupertypeName().equals("uima.cas.String"))
        continue;
      if (limitJCasGenToProjectScope && 
          isOutOfScope(td, projectPathDir)) {
        Set<String> mt = mergedTypesAddingFeatures.get(td.getName());
        if (null == mt) {
          continue;
        }
        StringBuilder sb = new StringBuilder("\n");
        for (String p : mt) {
          sb.append("  ").append(p).append('\n');
        }
        error.newError(IError.ERROR, getString("limitingButTypeWasExtended", new Object[] { td.getName(), sb.toString()}), null);
        continue;
      }

      // if the type is built-in - augment it with the built-in's features
      FeatureDescription[] builtInFeatures = (FeatureDescription[]) extendableBuiltInTypes.get(td
              .getName());
      if (null != builtInFeatures) {
        generatedBuiltInTypes.add(td.getName());
        List<FeatureDescription> newFeatures = setDifference(td.getFeatures(), builtInFeatures);
        int newFeaturesSize = newFeatures.size();
        if (newFeaturesSize > 0) {
          int newSize = builtInFeatures.length + newFeaturesSize;
          FeatureDescription[] newFds = new FeatureDescription[newSize];
          System.arraycopy(builtInFeatures, 0, newFds, 0, builtInFeatures.length);
          for (int j = builtInFeatures.length, k = 0; k < newFeaturesSize; j++, k++)
            newFds[j] = (FeatureDescription) newFeatures.get(k);
          td.setFeatures(newFds);
        } else {
          // The only built-in type which is extensible is DocumentAnnotation.
          // If we get here, the user defined DocumentAnnotation, but did not add any features
          //   In this case, skip generation
          continue;
        }
      }
      generateClassesFromTemplate(td, outputDirectory, jcasTypeInstance);
    }

    /* 
     * This code was supposed to generate extendable built-in types that were not 
     * extended by the input types
     * But the only extendable built-in type is DocumentAnnotation, and it should
     * not be generated by default - there's one provided by the framework.
     * 
     for (Iterator it = extendableBuiltInTypes.entrySet().iterator(); it.hasNext();) {
     Map.Entry entry = (Map.Entry) it.next();
     String typeName = (String) entry.getKey();
     if (noGenTypes.contains(typeName) || generatedBuiltInTypes.contains(typeName))
     continue;
     TypeDescription td = createTdFromType(typeName);
     generateClasses(td, outputDirectory);
     }
     */
  }

  /* This code was only called by above commented out section 
   private TypeDescription createTdFromType(String typeName) {
   TypeDescription td = UIMAFramework.getResourceSpecifierFactory().createTypeDescription();
   Type type = builtInTypeSystem.getType(typeName);
   td.setName(typeName);
   td.setSupertypeName(builtInTypeSystem.getParent(type).getName());

   ArrayList featuresOfType = new ArrayList();
   final List vFeatures = type.getFeatures();
   for (int i = 0; i < vFeatures.size(); i++) {
   Feature f = (Feature) vFeatures.get(i);
   if (f.getDomain().equals(type)) {
   FeatureDescription fd = UIMAFramework.getResourceSpecifierFactory()
   .createFeatureDescription();
   fd.setName(f.getShortName());
   fd.setRangeTypeName(f.getRange().getName());
   featuresOfType.add(fd);
   }
   }
   td.setFeatures((FeatureDescription[]) featuresOfType
   .toArray(new FeatureDescription[featuresOfType.size()]));
   return td;
   }
   */

  /**
   * return true if td is not defined in this project, or
   *   it is defined, but is also in merged and any of the other
   *   merged urls are not defined in this project.
   *
   * @param td the td
   * @param projectDirPath the project dir path
   * @return true, if is out of scope
   */
  private boolean isOutOfScope(TypeDescription td, String projectDirPath) {
    URI typeDefinitionUri;
    try {
      typeDefinitionUri = new URI (td.getSourceUrlString());
    } catch (URISyntaxException e) {
      if (IS_TRACE_LIMITED) {
        error.newError(IError.INFO, "debug isOutOfScope: got URISyntaxException, td.getSourceUrlstring: " + ((td.getSourceUrlString() == null) ? "null" : td.getSourceUrlString()), e);
      }
      return true; // may be overkill - but if td's source can't be parsed ... likely out of project
    }
    String tdPath = typeDefinitionUri.getPath();
    
    // Issue UIMA-4080 - If a type system resides in a JAR, then the path is null and it is
    // certainly out of scope.
    if (tdPath == null) {
      if (IS_TRACE_LIMITED) {
        error.newError(IError.INFO, "debug isOutOfScope: typeDefinitionUri had null path. " + typeDefinitionUri.toString(), null);
      }
        return true;
    }

    // UIMA-4119 - On windows, the default path representation and the URI path representation
    // differ: "/C:/..." vs. "C:\...". If the projectDirPath does not start with a /, we can be sure
    // that it is not an absolute path in URI notation, so we try to resolve it.
    // In this way, we can handle clients that use the URI notation (e.g. the Eclipse plugin)
    // as well as clients that use file-system notation (e.g. jcasgen-maven-plugin or a simple
    // invocation from the command line.
    String resolvedProjectPath;
    if (!projectDirPath.startsWith("/")) { 
        resolvedProjectPath = new File(projectDirPath).getAbsoluteFile().toURI().getPath();
    }
    else {
        resolvedProjectPath = projectDirPath;
    }
    
    // UIMA-4131 - Make sure that paths end in a slash so that "/my/project1" is not considered
    // to be in the scope of "/my/project"
    if (!tdPath.endsWith("/")) {
        tdPath += "/";
    }

    if (!resolvedProjectPath.endsWith("/")) {
        resolvedProjectPath += "/";
    }

    boolean r = !tdPath.startsWith(resolvedProjectPath);
    if (r) {
      if (IS_TRACE_LIMITED) {
        error.newError(IError.INFO, "debug isOutOfScope: tdPath doesn't start with resolved ProjectPath, tdPath: "
             + tdPath + ", resolvedProjectPath: " + resolvedProjectPath, null);
      }
      return true;
    }
    Set<String> mergedPaths = mergedTypesAddingFeatures.get(td.getName());
    if (null != mergedPaths) {
      for (String p : mergedPaths) {
        URI tempURI;
        try {
          tempURI = new URI(p);
        } catch (URISyntaxException e) {
          return true; //because a merged path is out of the project
        }
        String tempPath = tempURI.getPath();
        if (!tempPath.startsWith(resolvedProjectPath)) {
          if (IS_TRACE_LIMITED) {
            error.newError(IError.INFO, "debug isOutOfScope due to mergedType adding feature", null);
          }
          return true; 
        }
      }
    }
    return false; 
  }
  
  /**
   *  Generate type classes from the specified templates.
   *
   * @param td                        TypeDescription object
   * @param outputDirectory           output directory
   * @param jcasTypeInstance          Template instance used to generate class
   * @return void
   * @throws IOException -
   */
  private void generateClassesFromTemplate(TypeDescription td, String outputDirectory,
          IJCasTypeTemplate jcasTypeInstance)
          throws IOException {
    simpleClassName = removePkg(getJavaName(td));
    generateClass(progressMonitor, outputDirectory, td, jcasTypeInstance.generate(new Object[] {
        this, td }), getJavaName(td), merger);
//    simpleClassName = removePkg(getJavaName_Type(td));
//    generateClass(progressMonitor, outputDirectory, td, jcas_TypeInstance.generate(new Object[] {
//        this, td }), getJavaName_Type(td), merger);
  }

  /**
   * Gets the pkg.
   *
   * @param td the td
   * @return the pkg
   */
  String getPkg(TypeDescription td) {
    return getPkg(td.getName());
  }

  /**
   * Gets the pkg.
   *
   * @param nameWithPkg the name with pkg
   * @return the pkg
   */
  String getPkg(String nameWithPkg) {
    int lastDot = nameWithPkg.lastIndexOf('.');
    if (lastDot >= 0)
      return nameWithPkg.substring(0, lastDot);
    return "";
  }

  /**
   * Generate class.
   *
   * @param progressMonitorGenerateClass the progress monitor generate class
   * @param outputDirectory the output directory
   * @param td the td
   * @param sourceContents the source contents
   * @param className the class name
   * @param mergerGenerateClass the merger generate class
   * @throws IOException Signals that an I/O exception has occurred.
   */
  private void generateClass(IProgressMonitor progressMonitorGenerateClass, String outputDirectory,
          TypeDescription td, String sourceContents, String className, IMerge mergerGenerateClass)
          throws IOException {

    String pkgName = getJavaPkg(td);
    String qualifiedClassName = (0 != pkgName.length()) ? pkgName + "." + className : className;
    String targetContainer = outputDirectory + '/' + pkgName.replace('.', '/');
    String targetPath = targetContainer + "/" + className + ".java";
    File targetFile = new File(targetPath);

    if (null != mergerGenerateClass) {
      mergerGenerateClass.doMerge(this, progressMonitorGenerateClass, sourceContents,
              targetContainer, targetPath, qualifiedClassName, targetFile);
    } else {
      if (targetFile.exists()) {
        progressMonitorGenerateClass.subTask(getString("replacingTarget",
                new Object[] { qualifiedClassName }));
      } else
        progressMonitorGenerateClass.subTask(getString("creatingTarget",
                new Object[] { qualifiedClassName }));
      new File(targetContainer).mkdirs();
      try (Writer fw = new FileWriter(targetPath)) {
        fw.write(sourceContents);
      }
    }
  }

  /**
   * Removes the pkg.
   *
   * @param name the name
   * @return the string
   */
  public static String removePkg(String name) {
    int lastDot = name.lastIndexOf('.');
    String simpleName = name;
    if (lastDot >= 0)
      simpleName = name.substring(lastDot + 1);
    return simpleName;
  }

  // **************************************************
  // * sc (jTname) = return word that *
  // * goes into generated Cas get/set *
  // **************************************************

  /**
   * Sc.
   *
   * @param v the v
   * @return the string
   */
  String sc(String v) {
    // return part of word that goes in calls like get<insert>Value
    // input is Java type spec
    if (v.equals("int"))
      return "Int";
    if (v.equals("float"))
      return "Float";
    if (v.equals("String"))
      return "String";
    if (v.equals("boolean"))
      return "Boolean";
    if (v.equals("byte"))
      return "Byte";
    if (v.equals("short"))
      return "Short";
    if (v.equals("long"))
      return "Long";
    if (v.equals("double"))
      return "Double";
    return "Feature"; // for user defined features and other built-ins which are FSs
  }

  // * Functions that convert between CAS fully-qualified names and Java names.
  // * Handles both import issues and switching packages for some built-ins.

  /**
   * Gets the java pkg.
   *
   * @param td the td
   * @return the java pkg
   */
  String getJavaPkg(TypeDescription td) {
    TypeInfo bi = Jg.builtInTypes.get(td.getName());
    if (null == bi)
      return getPkg(td);
    return getPkg(bi.javaNameWithPkg);
  }

  /**
   * Gets the java name with pkg.
   *
   * @param casTypeName the cas type name
   * @return the java name with pkg
   */
  String getJavaNameWithPkg(String casTypeName) {
    TypeInfo bi = Jg.builtInTypes.get(casTypeName);
    return (null == bi) ? casTypeName : bi.javaNameWithPkg;
  }

  /**
   * Checks for pkg prefix.
   *
   * @param name the name
   * @return true, if successful
   */
  boolean hasPkgPrefix(String name) {
    return name.lastIndexOf('.') >= 0;
  }

  /**
   * Gets the java name.
   *
   * @param td the td
   * @return the java name
   */
  String getJavaName(TypeDescription td) {
    return getJavaName(td.getName());
  }

  /**
   * Gets the java name type.
   *
   * @param td the td
   * @return the java name type
   */
  String getJavaName_Type(TypeDescription td) {
    return getJavaName(td) + "_Type";
  }

  /**
   * Gets the java name.
   *
   * @param name the name
   * @return the java name
   */
  String getJavaName(String name) {

    if (!hasPkgPrefix(name))
      return name;
    String javaNameWithPkg = getJavaNameWithPkg(name);
    String simpleName = removePkg(javaNameWithPkg);
    if (getPkg(javaNameWithPkg).equals(packageName))
      return simpleName;
    if (javaNameWithPkg.equals(imports.get(simpleName)))
      return simpleName;
    return javaNameWithPkg;
  }

  /** The non importable java names. */
  private static ArrayList<String> nonImportableJavaNames = new ArrayList<>(8);
  static {
    nonImportableJavaNames.add("String");
    nonImportableJavaNames.add("float");
    nonImportableJavaNames.add("int");
    nonImportableJavaNames.add("boolean");
    nonImportableJavaNames.add("byte");
    nonImportableJavaNames.add("short");
    nonImportableJavaNames.add("long");
    nonImportableJavaNames.add("double");
  }

  /**
   * Collect import.
   *
   * @param casName the cas name
   * @param _Type the type
   */
  void collectImport(String casName, boolean _Type) {
    if (!hasPkgPrefix(casName))
      return;
    String javaNameWithPkg = getJavaNameWithPkg(casName);
    if (nonImportableJavaNames.contains(javaNameWithPkg))
      return;
    String pkg = getPkg(javaNameWithPkg);
    if (pkg.equals(packageName))
      return;
    if (_Type)
      javaNameWithPkg += "_Type";
    String simpleName = removePkg(javaNameWithPkg);
    if (simpleName.equals(simpleClassName))
      return;
    if (null == imports.get(simpleName)) {
      if (_Type)
        _imports.put(simpleName, javaNameWithPkg);
      else
        imports.put(simpleName, javaNameWithPkg);
    }
  }

  /**
   * Collect imports.
   *
   * @param td the td
   * @param _Type the type
   * @return the collection
   */
  Collection<String> collectImports(TypeDescription td, boolean _Type) {
    if (_Type)
      _imports.clear();
    else
      imports.clear();
    collectImport(td.getName(), _Type);
    collectImport(td.getSupertypeName(), _Type);

    if (!_Type) {

      FeatureDescription[] fds = td.getFeatures();
      for (int i = 0; i < fds.length; i++) {
        FeatureDescription fd = fds[i];
        if (null != typeSystem) {
          String rangeTypeNameCAS = fd.getRangeTypeName();
          Type rangeCasType = typeSystem.getType(rangeTypeNameCAS);
          if (typeSystem.subsumes(casStringType, rangeCasType))
            continue;
        }
        collectImport(fd.getRangeTypeName(), false);
        if (isRangeTypeGeneric(fd)) {
          collectImport(getJavaRangeArrayElementType(fd), false);
        }
      }
    }
    return (_Type) ? _imports.values() : imports.values();
  }

  /**
   * Gets the java range type.
   *
   * @param fd the fd
   * @return the java range type
   */
  String getJavaRangeType(FeatureDescription fd) {
    String rangeTypeNameCAS = fd.getRangeTypeName();
    if (null != typeSystem) {
      Type rangeCasType = typeSystem.getType(rangeTypeNameCAS);
      if (rangeCasType instanceof TypeImpl_string) {
        // type is a subtype of string, make its java type = to string
        return "String";
      }
    }
    return getJavaName(rangeTypeNameCAS);
  }

  /**
   * Gets the java range type, with generic types in <> as required.
   *
   * @param fd the fd
   * @return the java range type
   */
  String getJavaRangeType2(FeatureDescription fd) {
	  if (isRangeTypeGeneric(fd)) {
		  String generic = getJavaRangeArrayElementType2(fd);
		  return getJavaRangeType(fd) + "<" + (generic == null || generic.trim().isEmpty() ? "?" : generic) + ">";
	  } else
		  return getJavaRangeType(fd);
  }

  /**
   * Checks if is sub type of annotation.
   *
   * @param td the td
   * @return true, if is sub type of annotation
   */
  boolean isSubTypeOfAnnotation(TypeDescription td) {
    if (null == cas)
      return false;
    Type type = typeSystem.getType(td.getName());
    if (null == type) // happens when type hasn't been defined
      return false;
    return typeSystem.subsumes(tcasAnnotationType, type);
  }

  /**
   * Checks for array range.
   *
   * @param fd the fd
   * @return true, if successful
   */
  boolean hasArrayRange(FeatureDescription fd) {
    TypeInfo bi = Jg.builtInTypes.get(fd.getRangeTypeName());
    if (null == bi)
      return false;
    return bi.isArray;
  }
  
  /**
   * Checks if is possible index key.
   *
   * @param fd the fd
   * @return true, if is possible index key
   */
  boolean isPossibleIndexKey(FeatureDescription fd) {
    String rangeTypeName = fd.getRangeTypeName();
    // keys are primitives + string + string subtypes
    TypeImpl rangeType = (null == typeSystem) ? null : (TypeImpl) typeSystem.getType(rangeTypeName);
    return (null == typeSystem) ||          // default is to do checking
           rangeType.isStringSubtype() ||   
           BuiltinTypeKinds.primitiveTypeNames_contains(rangeTypeName);  // includes String
  }
  
  /**
   * Checks if is string subtype.
   *
   * @param fd the fd
   * @return true, if is string subtype
   */
  boolean isStringSubtype(FeatureDescription fd) {
    if (null != typeSystem) {
      String rangeTypeName = fd.getRangeTypeName();
      TypeImpl rangeType = (TypeImpl) typeSystem.getType(rangeTypeName);
      return rangeType.getSuperType() == ((TypeSystemImpl)typeSystem).stringType;
    }
    return false;
  }

  /**
   * Gets the java range array element type.
   *
   * @param fd the fd
   * @return the java range array element type
   */
  String getJavaRangeArrayElementType(FeatureDescription fd) {
    String arrayElementCasNameWithNameSpace = fd.getElementType();
    TypeInfo bi = Jg.builtInTypes.get(fd.getRangeTypeName());
    if (null == bi) {
      if (null == arrayElementCasNameWithNameSpace)
        return "";
      return getJavaName(arrayElementCasNameWithNameSpace);
    }
    if (null != arrayElementCasNameWithNameSpace && !"".equals(arrayElementCasNameWithNameSpace)) {
      return getJavaName(arrayElementCasNameWithNameSpace);
    }
    return getJavaName(bi.arrayElNameWithPkg);
  }

  /**
   * Gets the java range array element type, with generic type or ? in <> as needed
   *
   * @param fd the fd
   * @return the java range array element type
   */
  String getJavaRangeArrayElementType2(FeatureDescription fd) {
	if(this.isElementTypeGeneric(fd))
		return getJavaRangeArrayElementType(fd) + "<?>";
	else
		return getJavaRangeArrayElementType(fd);
  }

  // **************************************************
  // * uc1(featurename) make uppercase feature name for use by getters/setters
  /**
   * Uc 1.
   *
   * @param name the name
   * @return the string
   */
  // **************************************************
  String uc1(String name) { // upper case first letter
    return name.substring(0, 1).toUpperCase() + name.substring(1);
  }

  /**
   * Gets the date.
   *
   * @return the date
   */
  String getDate() {
    return (new Date()).toString();
  }

  // *******************************
  // * castResult *
  /**
   * Cast result.
   *
   * @param resultType the result type
   * @param core the core
   * @return the string
   */
  // *******************************
  String castResult(String resultType, String core) {
    if ("Feature".equals(sc(resultType)) && resultType != null
            && !resultType.equals("FeatureStructure"))
      return "(" + resultType + ")(" + core + ")";
    return core;
  }

  // instance generate get/setRefValue int
  // get/setIntValue
  // get/setStringValue String
  // get/setFloatValue float
  // range = Feature, String, Float, Int

  /**
   * Wrap to get FS.
   *
   * @param core string representing the get or set code
   * @param range Boolean, Byte, Short, Int, Long, Float, Double, String, or Feature
   * @return -
   */
  String wrapToGetFS(String core, String range) {
    if (range.equals("Feature"))
      return "jcasType.ll_cas.ll_getFSForRef(" + core + ")";
    return core;
  }

  /**
   * Simple core.
   *
   * @param get_set get or set
   * @param range Boolean, Byte, Short, Int, Long, Float, Double, String, or Feature
   * @param fname feature name (e.g. "begin"
   * @return the string
   */
  String simpleCore(String get_set, String range, String fname) {
    String v = ", v";
//    if (get_set.equals("set") && range.equals("Feature"))
//      v = ", jcasType.ll_cas.ll_getFSRef(v)";
//    boolean isInInt = ! (range.equals("String") || range.equals("Feature") || range.equals("JavaObject"));
    String chksfx = getCheckSuffix(get_set, range);
    //wrapGetIntCatchException(_FH_begin)
    String featOrOffset = "wrapGetIntCatchException(_FH_" + fname + ")";
    return "_" + get_set + range + "Value" + chksfx + "(" + featOrOffset  +
        ((get_set.equals("set")) ? v : "") + ")";
  }

  /**
   * Simple LL core.
   *
   * @param get_set the get set
   * @param range the range
   * @param fname the fname
   * @return the string
   */
  String simpleLLCore(String get_set, String range, String fname) {
    String v = ", v";
    // if (get_set.equals("set") && range.equals("Feature"))
    // v = ", ll_cas.ll_getFSRef(v)";
    return "ll_cas.ll_" + get_set + range + "Value(addr, casFeatCode_" + fname
            + ((get_set.equals("set")) ? v : "") + ")";
  }

  // return string that starts with FS whose value is not an array object, but
  // a normal CAS type, one of whose features is the array object
  /**
   * Array core.
   *
   * @param get_set  get or set
   * @param range the component range: Boolean, Byte, Short, Int, Long, Float, Double, String, Feature
   * @param arrayRange the array range
   * @param fname the fname
   * @return the string
   */
  String arrayCore(String get_set, String range, String arrayRange, String fname) {
    String v = ", v";
//    if (get_set.equals("set") && range.equals("Feature"))
//      v = ", jcasType.ll_cas.ll_getFSRef(v)";
    return 
        "((" + arrayRange + ")(" + simpleCore("get", "Feature", fname) + "))." + get_set +
        "(i" + ((get_set.equals("set")) ? v : "") + ")"; 
  }

  /**
   * Array LL core.
   *
   * @param get_set the get set
   * @param range the range
   * @param fname the fname
   * @return the string
   */
  String arrayLLCore(String get_set, String range, String fname) {
    String v = ", v";
    return "ll_cas.ll_" + get_set + range + "ArrayValue(" + simpleLLCore("get", "Feature", fname)
            + ", i" + ((get_set.equals("set")) ? v : "") + ")";
  }

  /**
   * Array LL core chk.
   *
   * @param get_set the get set
   * @param range the range
   * @param fname the fname
   * @return the string
   */
  String arrayLLCoreChk(String get_set, String range, String fname) {
    String v = ", v";
    return "ll_cas.ll_" + get_set + range + "ArrayValue(" + simpleLLCore("get", "Feature", fname)
            + ", i" + ((get_set.equals("set")) ? v : "") + ", true)";
  }

  /**
   * Gets the feature value.
   *
   * @param fd the fd
   * @param td the td
   * @return the feature value
   */
  String getFeatureValue(FeatureDescription fd, TypeDescription td) {
    String getSetNamePart = getGetSetNamePart(fd);
    String core = simpleCore("get", getSetNamePart, fd.getName());
    return castResult(getJavaRangeType2(fd), core);
  }

  /**
   * Sets the feature value.
   *
   * @param fd the fd
   * @param td the td
   * @return the string
   */
  String setFeatureValue(FeatureDescription fd, TypeDescription td) {
    return simpleCore("set", getGetSetNamePart(fd), fd.getName());
  }

  /**
   * Gets the array feature value.
   *
   * @param fd the fd
   * @param td the td
   * @return the array feature value
   */
  String getArrayFeatureValue(FeatureDescription fd, TypeDescription td) {
    String getSetArrayNamePart = getGetSetArrayNamePart(fd);
    String core = arrayCore("get", getSetArrayNamePart, getJavaRangeType2(fd), fd.getName());
    return castResult(getJavaRangeArrayElementType2(fd), core);
  }

  /**
   * Sets the array feature value.
   *
   * @param fd the fd
   * @param td the td
   * @return the string
   */
  String setArrayFeatureValue(FeatureDescription fd, TypeDescription td) {
    return arrayCore("set", getGetSetArrayNamePart(fd), getJavaRangeType2(fd), fd.getName());
  }

  /**
   * Gets the gets the set name part.
   *
   * @param fd the fd
   * @return the gets the set name part
   */
  String getGetSetNamePart(FeatureDescription fd) {
    return sc(getJavaRangeType2(fd));
  }

  /**
   * Gets the gets the set array name part.
   *
   * @param fd the fd
   * @return the gets the set array name part
   */
  String getGetSetArrayNamePart(FeatureDescription fd) {
    return sc(getJavaRangeArrayElementType(fd));
  }

  /**
   * Null blank.
   *
   * @param s the s
   * @return the string
   */
  String nullBlank(String s) {
    if (null == s)
      return "";
    return s;
  }

  /**
   * Creates the resource manager.
   *
   * @return the resource manager
   */
  public ResourceManager createResourceManager() {
    ResourceManager resourceManager = UIMAFramework.newDefaultResourceManager();

    if (classPath != null && classPath.trim().length() > 0) {
      try {
        resourceManager.setExtensionClassPath(this.getClass().getClassLoader(), classPath, true);
      } catch (MalformedURLException e1) {
        error.newError(IError.ERROR, getString("Internal Error", null), e1);
      }
    }
    else {
        resourceManager.setExtensionClassLoader(this.getClass().getClassLoader(), true);
    }
    return resourceManager;
  }

  /**
   * Merge type system imports.
   *
   * @param tsd the tsd
   * @return the type system description
   * @throws ResourceInitializationException the resource initialization exception
   */
  private TypeSystemDescription mergeTypeSystemImports(TypeSystemDescription tsd)
          throws ResourceInitializationException {
    Collection<TypeSystemDescription> tsdc = new ArrayList<>(1);
    tsdc.add((TypeSystemDescription) tsd.clone());
    mergedTypesAddingFeatures.clear();
    TypeSystemDescription mergedTsd = CasCreationUtils.mergeTypeSystems(tsdc,
            createResourceManager(), mergedTypesAddingFeatures);
    return mergedTsd;
  }

  /**
   * Sets the difference.
   *
   * @param newFeatures the new features
   * @param alreadyDefinedFeatures the already defined features
   * @return the list
   */
  List<FeatureDescription> setDifference(FeatureDescription[] newFeatures, FeatureDescription[] alreadyDefinedFeatures) {
    List<FeatureDescription> result = new ArrayList<>();
    outerLoop: for (int i = 0; i < newFeatures.length; i++) {
      for (int j = 0; j < alreadyDefinedFeatures.length; j++) {
        if (isSameFeatureDescription(newFeatures[i], alreadyDefinedFeatures[j]))
          continue outerLoop;
      }
      result.add(newFeatures[i]);
    }
    return result;
  }

  /**
   * Checks if is same feature description.
   *
   * @param f1 the f 1
   * @param f2 the f 2
   * @return true, if is same feature description
   */
  private boolean isSameFeatureDescription(FeatureDescription f1, FeatureDescription f2) {
    if (!f2.getName().equals(f1.getName()))
      return false;
    if (!f2.getRangeTypeName().equals(f1.getRangeTypeName()))
      return false;
    return true;
  }
  
  /**
   * Gets the check suffix.
   *
   * @param get_set the get set
   * @param range the range
   * @return the check suffix
   */
  private String getCheckSuffix(String get_set, String range) {
    if (get_set.equals("get")) return "Nc";
    
    return (range.equals("Feature")) ? "NcWj" : "Nfc";  
  }
  
  boolean isRangeTypeGeneric(FeatureDescription fd) {
	  return fd == null ? false : genericFeatureDescriptorTypes.contains(fd.getRangeTypeName());
  }

  boolean isElementTypeGeneric(FeatureDescription fd) {
	  return fd == null ? false : genericFeatureDescriptorTypes.contains(fd.getElementType());
  }
}
