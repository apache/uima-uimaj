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
import java.net.MalformedURLException;
import java.text.MessageFormat;
import java.util.ArrayList;
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
import java.util.TreeSet;

import javax.swing.UIManager;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.impl.CASImpl;
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

  static final String jControlModel = "jMergeCtl.xml";

  static final FeatureDescription[] featureDescriptionArray0 = new FeatureDescription[0];

  static final Collection reservedFeatureNames = new ArrayList();
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
  static final Set noGenTypes = new HashSet();

  public static final Properties casCreateProperties = new Properties();
  static {
    casCreateProperties.setProperty(UIMAFramework.CAS_INITIAL_HEAP_SIZE, "200");
  }

  static final Map extendableBuiltInTypes = new HashMap();

  // create a hash map of built-in types, where the
  // key is the fully-qualified name "uima.tcas.Annotation"
  // and the value is a Collection of FeatureDescription objects,
  // representing the features. These will be merged with the
  // specified features. Supertype features are not included.

  static final FeatureDescription[] emptyFds = new FeatureDescription[0];

  static TypeSystem builtInTypeSystem;

  static {
    CAS tcas = null;
    try {
      tcas = CasCreationUtils.createCas((TypeSystemDescription) null, null,
              new FsIndexDescription[0], casCreateProperties);

    } catch (ResourceInitializationException e1) {
      // never get here
    }

    builtInTypeSystem = ((CASImpl) tcas).getTypeSystemImpl();
    ((CASImpl) tcas).commitTypeSystem();

    for (Iterator it = builtInTypeSystem.getTypeIterator(); it.hasNext();) {
      Type type = (Type) it.next();
      if (type.isFeatureFinal()) {
        noGenTypes.add(type.getName());
        continue;
      }
      String typeName = type.getName();
      List fs = type.getFeatures();
      List features = new ArrayList(fs.size());
      if (null != fs) {
        for (int i = 0; i < fs.size(); i++) {
          Feature f = (Feature) fs.get(i);
          String fName = f.getName();
          String fTypeName = fName.substring(0, fName.indexOf(':'));
          if (typeName.equals(fTypeName))
            features.add(f);
        }
        FeatureDescription[] fds = new FeatureDescription[features.size()];
        for (int i = 0; i < features.size(); i++) {
          FeatureDescription fd = UIMAFramework.getResourceSpecifierFactory()
                  .createFeatureDescription();
          Feature f = (Feature) features.get(i);
          fd.setName(f.getShortName());
          fd.setRangeTypeName(f.getRange().getName());
          fds[i] = fd;
        }
        extendableBuiltInTypes.put(typeName, fds);
      } else
        extendableBuiltInTypes.put(typeName, emptyFds);
    }
  }

  // table builtInTypes initialized inside TypeInfo constructor
  static Map builtInTypes = new HashMap();

  static private void addBuiltInTypeInfo(String casName, String javaName, String casElementName) {
    TypeInfo ti = new TypeInfo(casName, javaName, casElementName);
    builtInTypes.put(casName, ti);
  }

  static private void addBuiltInTypeInfo(String casName, String javaName) {
    addBuiltInTypeInfo(casName, javaName, null);
  }

  // first type needed by fsArrayType; in hash map will be overwritten, though
  static {
    addBuiltInTypeInfo("uima.cas.TOP", "org.apache.uima.cas.FeatureStructure");
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
  }

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

  // Instance fields
  final Map imports = new HashMap(); // can't be final - one per instance running

  final Map _imports = new HashMap();

  String classPath = "";

  String xmlSourceFileName;

  CAS cas;

  GUI gui;

  IMerge merger;

  IProgressMonitor progressMonitor;

  public IError error; // referenced by the plugin

  Waiter waiter;

  String packageName;

  String simpleClassName;

  private TypeSystem typeSystem = null;

  private Type casStringType;

  private Type tcasAnnotationType;

  public Jg() { // default constructor
  }

  /**
   * Returns the string from the plugin's resource bundle, or 'key' if not found.
   */
  public String getResourceString(String key) {
    ResourceBundle bundle = getResourceBundle();
    try {
      return bundle.getString(key);
    } catch (MissingResourceException e) {
      return key;
    }
  }

  public String getString(String key, Object[] substitutions) {
    return MessageFormat.format(getResourceString(key), substitutions);
  }

  /**
   * Returns the plugin's resource bundle,
   */
  public ResourceBundle getResourceBundle() {
    return resourceBundle;
  }

  public static class ErrorExit extends RuntimeException {
    private static final long serialVersionUID = -3314235749649859540L;
  }

  // ************
  // * driveGui *
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
      public void windowClosing(WindowEvent e) {
        Prefs.set(gui);
        waiter.finished();
      }
    });
    Prefs.get(gui);
    gui.pnG.taStatus.setLineWrap(true);
    gui.pnG.taStatus.setWrapStyleWord(true);
    gui.show();
    waiter = new Waiter();
    waiter.waitforGUI();
  }

  public static void main(String[] args) {
    (new Jg()).main0(args, null, null, new LogThrowErrorImpl());
  }

  public void mainForCde(IMerge aMerger, IProgressMonitor aProgressMonitor, IError aError,
          String inputFile, String outputDirectory, TypeDescription[] tds, CASImpl aCas)
          throws IOException {
    this.merger = aMerger;
    this.error = aError;
    this.progressMonitor = aProgressMonitor;
    xmlSourceFileName = inputFile.replaceAll("\\\\", "/");
    generateAllTypes(outputDirectory, tds, aCas);
  }

  public void main0(String[] args, IMerge aMerger, IProgressMonitor aProgressMonitor, IError aError) {
    this.merger = aMerger;
    this.error = aError;
    this.progressMonitor = aProgressMonitor;
    boolean foundInput = false;
    for (int i = 0; i < args.length; i++) {
      if (args[i].equals("-jcasgeninput")) {
        if (i == args.length - 1) {
          driveGui();
          return;
        } else {
          foundInput = true;
          break;
        }
      }
    }
    if (foundInput)
      main1(args);
    else
      driveGui();
  }

  public void main1(String[] arguments) {
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

        for (int i = 0; i < arguments.length - 1; i++) {
          if (arguments[i].equalsIgnoreCase("-jcasgeninput")) {
            inputFile = arguments[++i];
            continue;
          }
          if (arguments[i].equalsIgnoreCase("-jcasgenoutput")) {
            outputDirectory = arguments[++i];
            continue;
          }
          if (arguments[i].equalsIgnoreCase("=jcasgenclasspath")) {
            classPath = arguments[++i];
            continue;
          }
        }

        xmlSourceFileName = inputFile.replaceAll("\\\\", "/");
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
        progressMonitor.beginTask("", 5);
        progressMonitor.subTask("Output going to '" + outputDirectory + "'");
        progressMonitor.subTask(getString("ReadingDescriptorAndCreatingTypes",
                new Object[] { inputFile }));
        // code to read xml and make cas type instance
        CASImpl casLocal = null;
        // handle classpath
        try {
          XMLInputSource in = new XMLInputSource(file);
          XMLizable specifier = UIMAFramework.getXMLParser().parse(in);

          if (specifier instanceof AnalysisEngineDescription) {
            AnalysisEngineDescription aeSpecifier = (AnalysisEngineDescription) specifier;
            if (!aeSpecifier.isPrimitive())
              typeSystemDescription = CasCreationUtils.mergeDelegateAnalysisEngineTypeSystems(
                      aeSpecifier, createResourceManager());
            else
              typeSystemDescription = mergeTypeSystemImports(aeSpecifier
                      .getAnalysisEngineMetaData().getTypeSystem());

          } else if (specifier instanceof TypeSystemDescription)
            typeSystemDescription = mergeTypeSystemImports(((TypeSystemDescription) specifier));
          else {
            error.newError(IError.ERROR, getString("fileDoesntParse", new Object[] { inputFile }),
                    null);
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

        generateAllTypes(outputDirectory, tds, casLocal);

      } catch (IOException e) {
        error.newError(IError.ERROR, getString("IOException", new Object[] {}), e);
      } catch (ErrorExit e) { // do nothing
      }
    } finally {
      progressMonitor.done();
    }
  }

  // This is also the interface for CDE
  void generateAllTypes(String outputDirectory, TypeDescription[] tds, CASImpl aCas)
          throws IOException {
    Set generatedBuiltInTypes = new TreeSet();

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

      // if the type is built-in - augment it with the built-in's features
      FeatureDescription[] builtInFeatures = (FeatureDescription[]) extendableBuiltInTypes.get(td
              .getName());
      if (null != builtInFeatures) {
        generatedBuiltInTypes.add(td.getName());
        List newFeatures = setDifference(td.getFeatures(), builtInFeatures);
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
      generateClasses(td, outputDirectory);
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

  private void generateClasses(TypeDescription td, String outputDirectory) throws IOException {
    simpleClassName = removePkg(getJavaName(td));
    generateClass(progressMonitor, outputDirectory, td, (new JCasTypeTemplate())
            .generate(new Object[] { this, td }), getJavaName(td), merger);
    simpleClassName = removePkg(getJavaName_Type(td));
    generateClass(progressMonitor, outputDirectory, td, (new JCas_TypeTemplate())
            .generate(new Object[] { this, td }), getJavaName_Type(td), merger);
  }

  String getPkg(TypeDescription td) {
    return getPkg(td.getName());
  }

  String getPkg(String nameWithPkg) {
    int lastDot = nameWithPkg.lastIndexOf('.');
    if (lastDot >= 0)
      return nameWithPkg.substring(0, lastDot);
    return "";
  }

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
      (new File(targetContainer)).mkdirs();
      FileWriter fw = new FileWriter(targetPath);
      try {
        fw.write(sourceContents);
      } finally {
        fw.close();
      }
    }
  }

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
    return "Ref"; // for user defined features and other built-ins
  }

  // * Functions that convert between CAS fully-qualified names and Java names.
  // * Handles both import issues and switching packages for some built-ins.

  String getJavaPkg(TypeDescription td) {
    TypeInfo bi = (TypeInfo) Jg.builtInTypes.get(td.getName());
    if (null == bi)
      return getPkg(td);
    return getPkg(bi.javaNameWithPkg);
  }

  String getJavaNameWithPkg(String casTypeName) {
    TypeInfo bi = (TypeInfo) Jg.builtInTypes.get(casTypeName);
    return (null == bi) ? casTypeName : bi.javaNameWithPkg;
  }

  boolean hasPkgPrefix(String name) {
    return name.lastIndexOf('.') >= 0;
  }

  String getJavaName(TypeDescription td) {
    return getJavaName(td.getName());
  }

  String getJavaName_Type(TypeDescription td) {
    return getJavaName(td) + "_Type";
  }

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

  private static ArrayList nonImportableJavaNames = new ArrayList(8);
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

  Collection collectImports(TypeDescription td, boolean _Type) {
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
        if (hasArrayRange(fd)) {
          collectImport(getJavaRangeArrayElementType(fd), false);
        }
      }
    }
    return (_Type) ? _imports.values() : imports.values();
  }

  String getJavaRangeType(FeatureDescription fd) {
    String rangeTypeNameCAS = fd.getRangeTypeName();
    if (null != typeSystem) {
      Type rangeCasType = typeSystem.getType(rangeTypeNameCAS);
      if (typeSystem.subsumes(casStringType, rangeCasType)) {
        // type is a subtype of string, make its java type = to string
        return "String";
      }
    }
    return getJavaName(rangeTypeNameCAS);
  }

  boolean isSubTypeOfAnnotation(TypeDescription td) {
    if (null == cas)
      return false;
    Type type = typeSystem.getType(td.getName());
    if (null == type) // happens when type hasn't been defined
      return false;
    return typeSystem.subsumes(tcasAnnotationType, type);
  }

  boolean hasArrayRange(FeatureDescription fd) {
    TypeInfo bi = (TypeInfo) Jg.builtInTypes.get(fd.getRangeTypeName());
    if (null == bi)
      return false;
    return bi.isArray;
  }

  String getJavaRangeArrayElementType(FeatureDescription fd) {
    String arrayElementCasNameWithNameSpace = fd.getElementType();
    TypeInfo bi = (TypeInfo) Jg.builtInTypes.get(fd.getRangeTypeName());
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

  // **************************************************
  // * uc1(featurename) make uppercase feature name for use by getters/setters
  // **************************************************
  String uc1(String name) { // upper case first letter
    return name.substring(0, 1).toUpperCase() + name.substring(1);
  }

  String getDate() {
    return (new Date()).toString();
  }

  // *******************************
  // * castResult *
  // *******************************
  String castResult(String resultType, String core) {
    if ("Ref".equals(sc(resultType)) && resultType != null
            && !resultType.equals("FeatureStructure"))
      return "(" + resultType + ")(" + core + ")";
    return core;
  }

  // instance generate get/setRefValue int
  // get/setIntValue
  // get/setStringValue String
  // get/setFloatValue float
  // range = Ref, String, Float, Int

  String wrapToGetFS(String core, String range) {
    if (range.equals("Ref"))
      return "jcasType.ll_cas.ll_getFSForRef(" + core + ")";
    return core;
  }

  String simpleCore(String get_set, String range, String fname, String tname_Type) {
    String v = ", v";
    if (get_set.equals("set") && range.equals("Ref"))
      v = ", jcasType.ll_cas.ll_getFSRef(v)";
    return "jcasType.ll_cas.ll_" + get_set + range + "Value(addr, ((" + tname_Type
            + ")jcasType).casFeatCode_" + fname + ((get_set.equals("set")) ? v : "") + ")";
  }

  String simpleLLCore(String get_set, String range, String fname) {
    String v = ", v";
    // if (get_set.equals("set") && range.equals("Ref"))
    // v = ", ll_cas.ll_getFSRef(v)";
    return "ll_cas.ll_" + get_set + range + "Value(addr, casFeatCode_" + fname
            + ((get_set.equals("set")) ? v : "") + ")";
  }

  // return string that starts with FS whose value is not an array object, but
  // a normal CAS type, one of whose features is the array object
  String arrayCore(String get_set, String range, String fname, String tname_Type) {
    String v = ", v";
    if (get_set.equals("set") && range.equals("Ref"))
      v = ", jcasType.ll_cas.ll_getFSRef(v)";
    return "jcasType.ll_cas.ll_" + get_set + range + "ArrayValue("
            + simpleCore("get", "Ref", fname, tname_Type) + ", i"
            + ((get_set.equals("set")) ? v : "") + ")";
  }

  String arrayLLCore(String get_set, String range, String fname) {
    String v = ", v";
    return "ll_cas.ll_" + get_set + range + "ArrayValue(" + simpleLLCore("get", "Ref", fname)
            + ", i" + ((get_set.equals("set")) ? v : "") + ")";
  }

  String arrayLLCoreChk(String get_set, String range, String fname) {
    String v = ", v";
    return "ll_cas.ll_" + get_set + range + "ArrayValue(" + simpleLLCore("get", "Ref", fname)
            + ", i" + ((get_set.equals("set")) ? v : "") + ", true)";
  }

  String getFeatureValue(FeatureDescription fd, TypeDescription td) {
    String getSetNamePart = getGetSetNamePart(fd);
    String core = wrapToGetFS(simpleCore("get", getSetNamePart, fd.getName(), getJavaName(td)
            + "_Type"), getSetNamePart);
    return castResult(getJavaRangeType(fd), core);
  }

  String setFeatureValue(FeatureDescription fd, TypeDescription td) {
    return simpleCore("set", getGetSetNamePart(fd), fd.getName(), getJavaName(td) + "_Type");
  }

  String getArrayFeatureValue(FeatureDescription fd, TypeDescription td) {
    String getSetArrayNamePart = getGetSetArrayNamePart(fd);
    String core = wrapToGetFS(arrayCore("get", getSetArrayNamePart, fd.getName(), getJavaName(td)
            + "_Type"), getSetArrayNamePart);
    return castResult(getJavaRangeArrayElementType(fd), core);
  }

  String setArrayFeatureValue(FeatureDescription fd, TypeDescription td) {
    return arrayCore("set", getGetSetArrayNamePart(fd), fd.getName(), getJavaName(td) + "_Type");
  }

  String getGetSetNamePart(FeatureDescription fd) {
    return sc(getJavaRangeType(fd));
  }

  String getGetSetArrayNamePart(FeatureDescription fd) {
    return sc(getJavaRangeArrayElementType(fd));
  }

  String nullBlank(String s) {
    if (null == s)
      return "";
    return s;
  }

  public ResourceManager createResourceManager() {
    ResourceManager resourceManager = UIMAFramework.newDefaultResourceManager();

    try {
      resourceManager.setExtensionClassPath(this.getClass().getClassLoader(), classPath, true);
    } catch (MalformedURLException e1) {
      error.newError(IError.ERROR, getString("Internal Error", null), e1);
    }
    return resourceManager;
  }

  public TypeSystemDescription mergeTypeSystemImports(TypeSystemDescription tsd)
          throws ResourceInitializationException {
    Collection tsdc = new ArrayList(1);
    tsdc.add(tsd.clone());
    TypeSystemDescription mergedTsd = CasCreationUtils.mergeTypeSystems(tsdc,
            createResourceManager());
    return mergedTsd;
  }

  List setDifference(FeatureDescription[] newFeatures, FeatureDescription[] alreadyDefinedFeatures) {
    List result = new ArrayList();
    outerLoop: for (int i = 0; i < newFeatures.length; i++) {
      for (int j = 0; j < alreadyDefinedFeatures.length; j++) {
        if (isSameFeatureDescription(newFeatures[i], alreadyDefinedFeatures[j]))
          continue outerLoop;
      }
      result.add(newFeatures[i]);
    }
    return result;
  }

  private boolean isSameFeatureDescription(FeatureDescription f1, FeatureDescription f2) {
    if (!f2.getName().equals(f1.getName()))
      return false;
    if (!f2.getRangeTypeName().equals(f1.getRangeTypeName()))
      return false;
    return true;
  }

}
