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

package org.apache.uima.migratev3.jcas;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Modifier;
import java.net.URLClassLoader;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.impl.TypeSystemImpl;
import org.apache.uima.cas.impl.UimaDecompiler;
import org.apache.uima.internal.util.CommandLineParser;
import org.apache.uima.internal.util.Misc;
import org.apache.uima.util.FileUtils;

import com.github.javaparser.ASTHelper;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EmptyTypeDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.body.VariableDeclaratorId;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.EmptyStmt;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

/**
 * A driver that scans given roots for source and/or class Java files that contain JCas classes
 * 
 *   - identifies which ones appear to be JCas classes (heuristic)
 *     -- identifies which ones appear to be v2
 *       --- converts these to v3
 *
 *   - also can receive a list of individual class names
 *   
 * Creates summary and detailed reports of its actions.
 * 
 * Outputs converted files to an output file tree.
 * 
 *   - includes reporting on multiple definitions of the same class
 *   
 *   Directory structure, starting at -outputDirectory
 *     converted/
 *       v2/
 *         x/y/z/javapath/.../Classname.java
 *         x/y/z/javapath/.../Classname.java
 *         ...
 *       v3/
 *         x/y/z/javapath/.../Classname.java
 *         x/y/z/javapath/.../Classname.java
 *         ...
 *         1/                                   << for duplicates, each set is for identical dups, different sets for non-identical
 *           x/y/z/javapath/.../Classname.java  << for duplicates, each set is for identical dups, different sets for non-identical  
 *           x/y/z/javapath/.../Classname.java  << for duplicates, each set is for identical dups, different sets for non-identical
 *           ...                                    
 *         2/                                   << for duplicates, each set is for identical dups, different sets for non-identical  
 *           x/y/z/javapath/.../Classname.java  << for duplicates, each set is for identical dups, different sets for non-identical
 *           x/y/z/javapath/.../Classname.java  << for duplicates, each set is for identical dups, different sets for non-identical
 *           ...
 *         
 *     not-converted/
 *     logs/
 *       processed.txt
 *       builtinsNotExtended.txt
 *       ...
 *  
 * Operates in one of two modes:
 *   Mode 1: Given classes-roots and/or individual class names, and a classpath, 
 *     scans the classes-routes looking for classes candidates, or iterate through the individual class names
 *       - determines the class name,
 *       - looks up the right "version" in the provided classpath, and decompiles that
 *       - migrates that decompiled source.
 *       -- duplicates are also processed. If different they are put into v3/nnn/etc.   
 *       
 *   Mode 2: Given sources-roots 
 *     Duplicates are migrated, results are put into a v3/nnn/ rest-of-path-identical
 * 
 * Note: Each run clears the output directory before starting the migration.
 */
public class MigrateJCas extends VoidVisitorAdapter<Object> {
  
  private static final String SOURCE_FILE_ROOTS = "-sourcesRoots";

  private static final String CLASS_FILE_ROOTS = "-classesRoots";

  private static final String OUTPUT_DIRECTORY = "-outputDirectory";
  
  private static final String SKIP_TYPE_CHECK = "-skipTypeCheck";
  
  private static final String MIGRATE_CLASSPATH = "-migrateClasspath"; 
  
  private static final String CLASSES = "-classes"; // individual classes to migrate, get from supplied classpath
  
  private Path tempDir = null;
    
  private boolean isSource = false;
  
  private Path candidate;
  private List<Path> candidates;
  
  private String packageName;
  private String className;  // (omitting package)
  private String packageAndClassNameSlash;  
  private final Set<String> usedPackageAndClassNames = new HashSet<>();
  private int packageAndClassNameSlash_i;
  
  /** includes trailing / */
  private String outputDirectory;
  /** includes trailing / */
  private String outDirConverted;
  /** includes trailing / */
  private String outDirSkipped;
  /** includes trailing / */
  private String outDirLog;
   
  private String[] sourcesRoots = new String[0];
  private String[] classesRoots = new String[0];
  
  private CompilationUnit cu;
  
  // save this value in the class instance to avoid recomputing it
  private ClassLoader cachedMigrateClassLoader = null;
  
  private String migrateClasspath = null;
  
  private String individualClasses = null;  // to decompile
  
  private class ConvertedSource {
    String rOrigSource;  // remembered original source
    byte[] rOrigBytes;   // remembered original bytes
    List<Path> paths;
    ConvertedSource(String origSource, byte[] origBytes, Path path) {
      this.rOrigSource = origSource;
      this.rOrigBytes = origBytes;
      paths = new ArrayList<>();
      add(path);
    }
    
    void add(Path path) {
      paths.add(path);
    }
  }
  
  /**
   * A map from classnames (fully qualified, with slashes) to a list of converted sources
   *   one per non-duplicated source
   */
  private Map<String, List<ConvertedSource>> classname2multiSources = new TreeMap<>();
  
  private Map<byte[], String> origBytesToClassName = new HashMap<>();
  private Map<String, String> origSourceToClassName = new HashMap<>();
    
  /************************************
   * Reporting
   ************************************/
  private final List<Path> v2JCasFiles = new ArrayList<>();
  private final List<Path> v3JCasFiles = new ArrayList<>();
  
  private final List<PathAndReason> nonJCasFiles = new ArrayList<>();  // path, reason
  private final List<PathAndReason> failedMigration = new ArrayList<>(); // path, reason
  private final List<ClassnameAndPath> c2ps = new ArrayList<>();            // class, path
  private final List<ClassnameAndPath> skippedBuiltins = new ArrayList<>();  // class, path
  private final List<PathAndReason> deletedCheckModified = new ArrayList<>();  // path, deleted check string
  private final List<String1AndString2> pathWorkaround = new ArrayList<>(); // original, workaround
  private final List<PathAndReason> manualInspection = new ArrayList<>(); // path, reason
//  private final List<PathAndPath> embeddedJars = new ArrayList<>(); // source, temp   
  
  private boolean v2;  // false at start of migrate, set to true if a v2 class candidate is discovered
  private boolean v3;  // true at start of migrate, set to false if no conversion done
      
  /************************************ 
   * Context for visits    
   ************************************/

  /**
   * if non-null, we're inside the ast for a likely JCas getter or setter method
   */
  private MethodDeclaration get_set_method; 
  private String featName;
  private boolean isGetter;
  private boolean isArraySetter; 

  /**
   * the range name part for _getXXXValue.. calls
   */
  private Object rangeNamePart;
  
  /**
   * the range name part for _getXXXValue.. calls without converting Ref to Feature
   */
  private String rangeNameV2Part;
  
  /**
   * temp place to insert static final int feature declarations
   */
  private List<BodyDeclaration> fi_fields = new ArrayList<>();
  private Set<String> featNames = new HashSet<>();

  private boolean hasV2Constructors;
  private boolean hasV3Constructors;

  private boolean isSkipTypeCheck = false;

  
  private byte[] origBytes;   // set by getSource()
  private String origSource;  // set by getSource()
  private String alreadyDone; // the slashifiedClassName

  public MigrateJCas() {
  }

  public static void main(String[] args) {
    (new MigrateJCas()).run(args);
  }
  
  /***********************************
   * Main
   * @param args -
   ***********************************/
  void run(String[] args) {
    CommandLineParser clp = parseCommandArgs(args);
    
    // clear output dir
    FileUtils.deleteRecursive(new File(outputDirectory));
    
    isSource = true;
    processRootsCollection("source", sourcesRoots, clp);
    
    isSource = false;
    processRootsCollection("classes", classesRoots, clp);
    
    if (individualClasses != null) {
      processCollection("individual classes: ", new Iterator<String>() {
        Iterator<String> it = Arrays.asList(individualClasses.split(File.pathSeparator)).iterator();
        public boolean hasNext() {return it.hasNext();}
        public String next() {
          return prepareIndividual(it.next());}
      });
    }

    report();
  }

  private void processRootsCollection(String kind, String[] roots, CommandLineParser clp) {
    for (String root : roots) {
      processCollection("from " + kind + "  root: " + root, new Iterator<String>() { 
        Iterator<Path> it = getCandidates(clp, root).iterator();
        public boolean hasNext() {return it.hasNext();}
        public String next() {
          return prepare(it.next());}
      });
    }
  }
  
  
  private void processCollection(String sourceName, Iterator<String> sourceIterator) {
    System.out.println("Migrating " + sourceName + ", number of classes migrated:");
    int i = 1;
    System.out.print("    ");
    
    while (sourceIterator.hasNext()) {
      migrate(sourceIterator.next());
      if ((i % 50) == 0) System.out.format("%4d%s", Integer.valueOf(i), "\r");
      i++;
    }
    System.out.format("%4d%n", Integer.valueOf(i));   
  }
  
  /**
   * parse command line args
   * @param args - 
   * @return the CommandLineParser instance
   */
  private CommandLineParser parseCommandArgs(String[] args) {
    CommandLineParser clp = createCmdLineParser();
    try {
      clp.parseCmdLine(args);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    if (!checkCmdLineSyntax(clp)) {
      printUsage();
      System.exit(2);
    }
    
    if (clp.isInArgsList(CLASS_FILE_ROOTS)) {
      classesRoots = getRoots(clp, CLASS_FILE_ROOTS);
    }
    
    if (clp.isInArgsList(SOURCE_FILE_ROOTS)) {
      sourcesRoots = getRoots(clp, SOURCE_FILE_ROOTS);
    }
    
    return clp;
  }
  
  /***************
   * set roots
   * set isSource
   ***************/
  private void setRootsAndSource(CommandLineParser clp) {
    if (clp.isInArgsList(CLASS_FILE_ROOTS)) {
      classesRoots = getRoots(clp, CLASS_FILE_ROOTS);
    }
    
    if (clp.isInArgsList(SOURCE_FILE_ROOTS)) {
      sourcesRoots = getRoots(clp, SOURCE_FILE_ROOTS);
    }
  }
  
  private String[] getRoots(CommandLineParser clp, String kind) {
    return clp.getParamArgument(kind).split("\\" + File.pathSeparator);
  }
  
  /**
   * If working with .class files:
   *   - read the byte array
   *   - see if it has already been decompiled; if so, return false
   *   
   * Side effect, set origSource and origBytes and alreadyDone
   * @param path
   * @return the source to maybe migrate
   */
  private String getSource(Path path) {
    try {
      origSource = null;
      origBytes = null;
      if (isSource) {
        origSource = FileUtils.reader2String(Files.newBufferedReader(path));
        alreadyDone = origSourceToClassName.get(origSource);
//        System.out.println("debug read " + s.length());
      } else {
        origBytes = Files.readAllBytes(path);
        alreadyDone = origBytesToClassName.get(origBytes);
        if (null == alreadyDone) {
          origSource = decompile(origBytes);
        }
      }
      if (alreadyDone != null) {
        return null;
      }
            
      return origSource;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  /**
   * Migrate one JCas definition.
   * 
   * The source is either direct, or a decompiled version of a .class file (missing comments, etc.).
   * 
   * This method only called if heuristics indicate this is a V2 JCas class definition.
   * 
   * The goal is to preserve as much as possible of existing customization.
   * The general approach is to parse the source into an AST, and use visitor methods.
   *   For getter/setter methods that are for features (heurstic), set up a context for inner visitors
   *   identifying the getter / setter.
   *     - reuse method declarator, return value casts, value expressions
   *     - remove feature checking statement, array bounds checking statement, if present.
   *     - replace the simpleCore (see Jg), replace the arrayCore
   *     
   *   For constructors, replace the 2-arg one that has arguments: 
   *   addr and TOP_Type with the v3 one using TypeImpl, CasImpl.
   *   
   *   Add needed imports.
   *   Add for each feature the _FI_xxx static field declarator.
   *   
   *   Leave other top level things alone
   *     - additional constructors.
   *     - other methods not using jcasType refs
   *   
   * @param source - the source, either directly from a .java file, or a decompiled .class file
   */
  private void migrate(String source) {

    if (alreadyDone == null) {
      v3 = true;  // preinit
      v2 = false;
      featNames.clear();
      fi_fields.clear();
      
  //    System.out.println("Migrating source before migration:\n");
  //    System.out.println(source);
  //    System.out.println("\n\n\n");
  
      StringReader sr = new StringReader(source);
      try {
        cu = JavaParser.parse(sr, true);
        
        addImport("org.apache.uima.cas.impl.CASImpl");
        addImport("org.apache.uima.cas.impl.TypeImpl");
        addImport("org.apache.uima.cas.impl.TypeSystemImpl");
        
        this.visit(cu, null);      
        new removeEmptyStmts().visit(cu, null);
        if (v3) {
          removeImport("org.apache.uima.jcas.cas.TOP_Type");
        }
        
        if (v3 && fi_fields.size() > 0) {
          List<BodyDeclaration> classMembers = cu.getTypes().get(0).getMembers();
          int positionOfFirstConstructor = findConstructor(classMembers);
          if (positionOfFirstConstructor < 0) {
            throw new RuntimeException();
          }
          classMembers.addAll(positionOfFirstConstructor, fi_fields);
        }      
                
        if (isSource) {
          origSourceToClassName.put(origSource, packageAndClassNameSlash);
        } else {
          origBytesToClassName.put(origBytes, packageAndClassNameSlash);
        }

        boolean identicalFound = collectInfoForReports();

        if (!identicalFound) {  // don't write out identicals
          getBaseOutputPath();
          if (v2) { 
            writeV2Orig(source, v3);
          }
          if (v3) {
            String s = cu.toString();
            writeV3(s);  
          }
          System.out.print(".");
        } else {
          System.out.print("d");
        }
      } catch (ParseException e) {
        reportParseException();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    } else {
      collectInfoForReports();
      System.out.print("d");
    }
  }

  /**
   * Add this instance to the tracking information for multiple versions (identical or not) of a class
   * @return true if this is an identical duplicate of one already done
   */
  
  private boolean collectInfoForReports() {
    String fqcn = (alreadyDone == null)    // fully qualified class name with slashes
                    ? packageAndClassNameSlash
                    : alreadyDone;
    // for a given fully qualified class name (slashified), 
    //   find the list of ConvertedSources - one per each different version
    //     create it if null
    List<ConvertedSource> convertedSources = classname2multiSources.get(fqcn);
    if (convertedSources == null) {
      convertedSources = new ArrayList<>();
      classname2multiSources.put(fqcn, convertedSources);
    }
    
    // search to see if this instance already in the set
    //   if so, add the path to the set of identicals
    boolean found = false;
    for (ConvertedSource cs : convertedSources) {
      if ((isSource && cs.rOrigSource.equals(origSource)) ||
          (!isSource && Arrays.equals(cs.rOrigBytes, origBytes))) {
        cs.add(candidate);
        found = true;
        break;
      }
    }

    
    if (!found) {
      ConvertedSource cs = new ConvertedSource(origSource, origBytes, candidate);
      convertedSources.add(cs);
    }    
    
    return found;
  }
  
  /******************
   *  Visitors
   ******************/
  /**
   * Capture the type name from all top-level types
   *   AnnotationDeclaration, Empty, and Enum
   */
  
  @Override
  public void visit(AnnotationDeclaration n, Object ignore) {
    updateClassName(n);
    super.visit(n,  ignore);
  }

  @Override
  public void visit(EmptyTypeDeclaration n, Object ignore) {
    updateClassName(n);
    super.visit(n,  ignore);
  }

  @Override
  public void visit(EnumDeclaration n, Object ignore) {
    updateClassName(n);
    super.visit(n,  ignore);
  }

  /**
   * Check if the top level class looks like a JCas class, and report if not:
   *   has 0, 1, and 2 element constructors
   *   has static final field defs for type and typeIndexID
   *   
   *   Also check if V2 style: 2 arg constructor arg types
   *   Report if looks like V3 style due to args of 2 arg constructor
   *   
   *   if class doesn't extend anything, not a JCas class.
   *   if class is enum, not a JCas class
   * @param n
   * @param ignore
   */
  @Override
  public void visit(ClassOrInterfaceDeclaration n, Object ignore) {
    // do checks to see if this is a JCas class; if not report skipped
   
    if (n.getParentNode() instanceof CompilationUnit) {
      updateClassName(n);
      List<ClassOrInterfaceType> supers = n.getExtends();
      if (supers == null || supers.size() == 0) {
        reportNotJCasClass("class doesn't extend a superclass");
        super.visit(n, ignore);
        return; 
      }
      
      List<BodyDeclaration> members = n.getMembers();
      setHasJCasConstructors(members);
      if (hasV2Constructors && hasTypeFields(members)) {
        reportV2Class();
        super.visit(n,  ignore);
        return;
      }
      if (hasV2Constructors) {
        reportNotJCasClassMissingTypeFields();
        return;
      }
      if (hasV3Constructors) {
        reportV3Class();
        return;
      }
      reportNotJCasClass("missing v2 constructors");
      return;
      
    } else { // do standard processing for non-outer class
      super.visit(n,  ignore);
      return;
    }
  }
  
  @Override
  public void visit(PackageDeclaration n, Object ignored) {
    packageName = n.getName().toString();
    super.visit(n, ignored);
  }
    
  /***************
   * Constructors
   *   - modify the 2 arg constructor - changing the args and the body
   * @param n - the constructor node
   * @param ignored -
   */
  @Override
  public void visit(ConstructorDeclaration n, Object ignored) {
    super.visit(n, ignored);  // processes the params 
    if (!v3) {  // for enums, annotations
      return;
    }
    List<Parameter> ps = n.getParameters();
    
    if (ps.size() == 2 && 
        getParmTypeName(ps, 0).equals("int") &&
        getParmTypeName(ps, 1).equals("TOP_Type")) {
        
      /** public Foo(TypeImpl type, CASImpl casImpl) {
       *   super(type, casImpl);
       *   readObject();
       */
      setParameter(ps, 0, "TypeImpl", "type");
      setParameter(ps, 1, "CASImpl", "casImpl");
      
      // Body: change the 1st statement (must be super)
      List<Statement> stmts = n.getBlock().getStmts();
      if (!(stmts.get(0) instanceof ExplicitConstructorInvocationStmt)) {
        recordBadConstructor("missing super call");
        return;
      }
      List<Expression> args = ((ExplicitConstructorInvocationStmt)(stmts.get(0))).getArgs();
      args.set(0, new NameExpr("type"));
      args.set(1,  new NameExpr("casImpl"));
     
      // leave the rest unchanged.
    }      
  }

  private final static Pattern refGetter = Pattern.compile("(ll_getRef(Array)?Value)|"
      +                                                    "(ll_getFSForRef)");
  private final static Pattern word1 = Pattern.compile("\\A(\\w*)");  // word chars starting at beginning \\A means beginning
  /*****************************
   * Method Declaration Visitor
   *   Heuristic to determine if a feature getter or setter:
   *   - name: is 4 or more chars, starting with get or set, with 4th char uppercase
   *           is not "getTypeIndexID"
   *   - (optional - if comments are available:) 
   *       getter for xxx, setter for xxx
   *   - for getter: has 0 or 1 arg (1 arg case for indexed getter, arg must be int type)
   *   - for setter: has 1 or 2 args
   *   
   *   Workaround for decompiler - getters which return FSs might be missing the cast to the return value type
   *     
   *****************************/
  @Override
  public void visit(MethodDeclaration n, Object ignore) {
    String name = n.getName();
    isGetter = isArraySetter = false;
    do {  // to provide break exit
      if (name.length() >= 4 && 
          ((isGetter = name.startsWith("get")) || name.startsWith("set")) &&
          Character.isUpperCase(name.charAt(3)) &&
          !name.equals("getTypeIndexID")) {
        List<Parameter> ps = n.getParameters();
        if (isGetter) {
          if (ps.size() > 1) break;
        } else { // is setter
          if (ps.size() > 2 || 
              ps.size() == 0) break;
          if (ps.size() == 2) {
            if (!getParmTypeName(ps, 0).equals("int")) break;
            isArraySetter = true;
          } 
        }
        
        // get the range-part-name and convert to v3 range ("Ref" changes to "Feature")
        String bodyString = n.getBody().toStringWithoutComments();
        int i = bodyString.indexOf("jcasType.ll_cas.ll_");
        if (i < 0) break; 
        String s = bodyString.substring(i + "jcasType.ll_cas.ll_get".length()); // also for ...ll_set - same length!
        if (s.startsWith("FSForRef(")) { // then it's the wrapper and the wrong instance.
          i = s.indexOf("jcasType.ll_cas.ll_");
          if (i < 0) {
            reportUnrecognizedV2Code("Found \"jcasType.ll_cas.ll_[set or get]...FSForRef(\" but didn't find following \"jcasType.ll_cas_ll_\"\n" + n.toString());
            break;
          }
          s = s.substring(i + "jcasType.ll_cas.ll_get".length());
        }
        i = s.indexOf("Value");
        if (i < 0) {
          reportUnrecognizedV2Code("Found \"jcasType.ll_cas.ll_[set or get]\" but didn't find following \"Value\"\n" + n.toString());
          break;  // give up
        }
        s = Character.toUpperCase(s.charAt(0)) + s.substring(1, i);
        rangeNameV2Part = s;
        rangeNamePart = s.equals("Ref") ? "Feature" : s;

        // get feat name following ")jcasType).casFeatCode_xxxxx,
        i = bodyString.indexOf("jcasType).casFeatCode_");
        if (i == -1) {
          reportUnrecognizedV2Code("Didn't find \"...jcasType).casFeatCode_\"\n" + n.toString());
          break;
        }
        Matcher m = word1.matcher(bodyString.substring(i + "jcasType).casFeatCode_".length() ));
        if (!m.find()) {
          reportUnrecognizedV2Code("Found \"...jcasType).casFeatCode_\" but didn't find subsequent word\n" + n.toString());
          break;
        }
        featName = m.group(1);
        String fromMethod = Character.toLowerCase(name.charAt(3)) + name.substring(4);
        if (!featName.equals(fromMethod)) {
          // don't report if the only difference is the first letter captialization
          if (!(Character.toLowerCase(featName.charAt(0)) + featName.substring(1)).equals(fromMethod)) {
            reportMismatchedFeatureName(String.format("%-25s %s", featName, name));
          }
        }
        
        List<Expression> args = Collections.singletonList(new StringLiteralExpr(featName));
        VariableDeclarator vd = new VariableDeclarator(
            new VariableDeclaratorId("_FI_" + featName),
            new MethodCallExpr(null, "TypeSystemImpl.getAdjustedFeatureOffset", args));
        if (featNames.add(featName)) {
          fi_fields.add(new FieldDeclaration(
              Modifier.PUBLIC + Modifier.STATIC + Modifier.FINAL, 
              ASTHelper.INT_TYPE, 
              vd));
        }
        
        /**
         * add missing cast stmt for
         * return stmts where the value being returned:
         *   - doesn't have a cast already
         *   - has the expression be a methodCallExpr with a name which looks like:
         *       ll_getRefValue or 
         *       ll_getRefArrayValue  
         */
        if (isGetter && "Feature".equals(rangeNamePart)) {
          for (Statement stmt : n.getBody().getStmts()) {
            if (stmt instanceof ReturnStmt) {
              Expression e = getUnenclosedExpr(((ReturnStmt)stmt).getExpr());
              if ((e instanceof MethodCallExpr)) {
                String methodName = ((MethodCallExpr)e).getName();
                if (refGetter.matcher(methodName).matches()) { // ll_getRefValue or ll_getRefArrayValue
                  addCastExpr(stmt, n.getType());
                }
              }
            }
          }
        }

        get_set_method = n; // used as a flag during inner "visits" to signal  
                            // we're inside a likely feature setter/getter

      } // end of test for getter or setter method
    } while (false);  // do once, provide break exit
    
    super.visit(n, ignore);
    get_set_method = null; // after visiting, reset the get_set_method to null
  }
    
  /**
   * Visitor for if stmts
   *   - removes feature missing test
   */
  @Override
  public void visit(IfStmt n, Object ignore) {
    do {
      // if (get_set_method == null) break;  // sometimes, these occur outside of recogn. getters/setters
      
      Expression c = n.getCondition(), e;
      BinaryExpr be, be2; 
      List<Statement> stmts;
      if ((c instanceof BinaryExpr) &&
          ((be = (BinaryExpr)c).getLeft() instanceof FieldAccessExpr) &&
          ((FieldAccessExpr)be.getLeft()).getField().equals("featOkTst")) {
        // remove the feature missing if statement
        
        // verify the remaining form 
        if (! (be.getRight() instanceof BinaryExpr) 
         || ! ((be2 = (BinaryExpr)be.getRight()).getRight() instanceof NullLiteralExpr) 
         || ! (be2.getLeft() instanceof FieldAccessExpr)

         || ! ((e = getExpressionFromStmt(n.getThenStmt())) instanceof MethodCallExpr)
         || ! (((MethodCallExpr)e).getName()).equals("throwFeatMissing")) {
          reportDeletedCheckModified("The featOkTst was modified:\n" + n.toString() + '\n');
        }
              
        BlockStmt parent = (BlockStmt) n.getParentNode();
        stmts = parent.getStmts();
        stmts.set(stmts.indexOf(n), new EmptyStmt()); //dont remove
                                            // otherwise iterators fail
//        parent.getStmts().remove(n);
        return;
      }
    } while (false);
    
    super.visit(n,  ignore);
  }
  /** 
   * visitor for method calls
   */
  @Override
  public void visit(MethodCallExpr n, Object ignore) {
    Node p1, p2, updatedNode = null;
    List<Expression> args;
    
    do {
      if (get_set_method == null) break;

      /** remove checkArraybounds statement **/
      if (n.getName().equals("checkArrayBounds") &&
          ((p1 = n.getParentNode()) instanceof ExpressionStmt) &&
          ((p2 = p1.getParentNode()) instanceof BlockStmt) &&
          p2.getParentNode() == get_set_method) {
        List<Statement> stmts = ((BlockStmt)p2).getStmts();
        stmts.set(stmts.indexOf(p1), new EmptyStmt());
        return;
      }
           
      // convert simpleCore expression ll_get/setRangeValue
      boolean useGetter = isGetter || isArraySetter;
      if (n.getName().startsWith("ll_" + (useGetter ? "get" : "set") + rangeNameV2Part + "Value")) {
        args = n.getArgs();
        if (args.size() != (useGetter ? 2 : 3)) break;
        String suffix = useGetter ? "Nc" : rangeNamePart.equals("Feature") ? "NcWj" : "Nfc";
        String methodName = "_" + (useGetter ? "get" : "set") + rangeNamePart + "Value" + suffix; 
        args.remove(0);    // remove the old addr arg
        // arg 0 converted when visiting args FieldAccessExpr 
        n.setScope(null);
        n.setName(methodName);
      }
      
      // convert array sets/gets
      String z = "ll_" + (isGetter ? "get" : "set");
      if (n.getName().startsWith(z) &&
          n.getName().endsWith("ArrayValue")) {
        
        String s = n.getName().substring(z.length());
        s = s.substring(0,  s.length() - "Value".length()); // s = "ShortArray",  etc.
        if (s.equals("RefArray")) s = "FSArray";
        if (s.equals("IntArray")) s = "IntegerArray";
        EnclosedExpr ee = new EnclosedExpr(
            new CastExpr(new ClassOrInterfaceType(s), n.getArgs().get(0)));
        
        n.setScope(ee);    // the getter for the array fs
        n.setName(isGetter ? "get" : "set");
        n.getArgs().remove(0);
      }
      
      /** remove ll_getFSForRef **/
      /** remove ll_getFSRef **/
      if (n.getName().equals("ll_getFSForRef") ||
          n.getName().equals("ll_getFSRef")) {
        updatedNode = replaceInParent(n, n.getArgs().get(0));        
      }
      
      
      
    } while (false);
        
    if (updatedNode != null) {
      updatedNode.accept(this,  null);
    } else {
      super.visit(n, null);
    }
  }

  /**
   * visitor for field access expressions
   *   - convert ((...type_Type)jcasType).casFeatCode_XXXX to _FI_xxx
   * @param n -
   * @param ignore -
   */
  @Override
  public void visit(FieldAccessExpr n, Object ignore) {
    Expression e;
    
    if (get_set_method != null) {  
      if (n.getField().startsWith("casFeatCode_") &&
          ((e = getUnenclosedExpr(n.getScope())) instanceof CastExpr) &&
          ("jcasType".equals(getName(((CastExpr)e).getExpr())))) {
        String featureName = n.getField().substring("casFeatCode_".length());
        replaceInParent(n, new NameExpr("_FI_" + featureName)); // repl last in List<Expression> (args)
        return;
      } else if (n.getField().startsWith("casFeatCode_")) {
        reportMigrateFailed("Found field casFeatCode_ ... without a previous cast expr using jcasType");
      }
    }
    super.visit(n,  ignore);      
  }
  
  private class removeEmptyStmts extends VoidVisitorAdapter<Object> {
    @Override
    public void visit(BlockStmt n, Object ignore) {
      Iterator<Statement> it = n.getStmts().iterator();
      while (it.hasNext()) {
        if (it.next() instanceof EmptyStmt) {
          it.remove();
        }
      }
      super.visit(n,  ignore);
    }
    
//    @Override
//    public void visit(MethodDeclaration n, Object ignore) {
//      if (n.getName().equals("getModifiablePrimitiveNodes")) {
//        System.out.println("debug");
//      }
//      super.visit(n,  ignore);
//      if (n.getName().equals("getModifiablePrimitiveNodes")) {
//        System.out.println("debug");
//      }
//    }
  }
    
  /**
   * converted files: 
   *    java name, path  (sorted by java name, v3 name only)
   * not-converted:
   *    java name, path  (sorted by java name)
   * duplicates:
   *    java name, path  (sorted by java name)
   *
   */
  private void report() {
    System.out.println("\n\nMigration Summary");
    System.out.format("Output top directory: %s%n", outputDirectory);
    System.out.format("Date/time: %tc%n", new Date());
    pprintRoots("Sources", sourcesRoots);
    pprintRoots("Classes", classesRoots);

   
    try {
      reportPaths("Workaround Directories", "workaroundDir.txt", pathWorkaround);
      reportPaths("Report of converted files where a deleted check was customized", "deletedCheckModified.txt", deletedCheckModified);
      reportPaths("Report of converted files needing manual inspection", "manualInspection.txt", manualInspection);
      reportPaths("Report of files which failed migration", "failed.txt", failedMigration);
      reportPaths("Report of non-JCas files", "NonJCasFiles.txt", nonJCasFiles);
      reportPaths("Builtin JCas classes - skipped - need manual checking to see if they are modified",
          "skippedBuiltins.txt", skippedBuiltins);
      
//      computeDuplicates();
//      reportPaths("Report of duplicates - not identical", "nonIdenticalDuplicates.txt", nonIdenticalDuplicates);
//      reportPaths("Report of duplicates - identical", "identicalDuplicates.txt", identicalDuplicates);
      reportDuplicates();
      
      reportPaths("Report of processed files", "processed.txt", c2ps);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void pprintRoots(String kind, String[] roots) {
    if (roots.length > 0) {
      System.out.println(kind + " Roots:");
      for (String s : roots) {
        System.out.format("    %s%n",  s);
      }
      System.out.println("\n");
    }
  }
  
//  private void computeDuplicates() {
//    List<ClassnameAndPath> toCheck = new ArrayList<>(c2ps);
//    toCheck.addAll(extendableBuiltins);
//    sortReport2(toCheck);
//    ClassnameAndPath prevP = new ClassnameAndPath(null, null);
//    List<ClassnameAndPath> sameList = new ArrayList<>();
//    boolean areAllEqual = true;
//    
//    for (ClassnameAndPath p : toCheck) {
//      if (!p.getFirst().equals(prevP.getFirst())) {
//        
//        addToIdenticals(sameList, areAllEqual);
//        sameList.clear();
//        areAllEqual = true;
//        
//        prevP = p;
//        continue;
//      }
//      
//      // have 2nd or subsequent same class
//      if (sameList.size() == 0) {
//        sameList.add(prevP);
//      }
//      sameList.add(p);
//      if (areAllEqual) {
//        if (isFilesMiscompare(p.path, prevP.path)) {
//          areAllEqual = false;
//        }
//      }      
//    }
//    
//    addToIdenticals(sameList, areAllEqual);    
//  }
  
//  /**
//   * Compare two java source or class files
//   * @param p1
//   * @param p2
//   * @return
//   */
//  private boolean isFilesMiscompare(Path p1, Path p2) {
//      String s1 = getSource(p1);
//      String s2 = getSource(p2);
//      return !s1.equals(s2);
//  }
  
//  private void addToIdenticals(List<ClassnameAndPath> sameList, boolean areAllEqual) {
//    if (sameList.size() > 0) {
//      if (areAllEqual) {
//        identicalDuplicates.addAll(sameList);
//      } else {
//        nonIdenticalDuplicates.addAll(sameList);
//      }
//    }
//  }
 
  private Path makePath(String name) throws IOException {
    Path p = Paths.get(name);
    Path parent = p.getParent();
    try {
      Files.createDirectories(parent);
    } catch (FileAlreadyExistsException e) {
      // caused by running on Windows system which ignores "case"
      // there's a file at /x/y/  named "z", but the path wants to be /x/y/Z/
      //   Workaround: change "z" to "z_c"  c for capitalization issue
      String newDir = parent.getFileName().toString() + "_c";
      Path p2 = Paths.get(parent.getParent().toString(), newDir);
      Files.createDirectories(p2);
      reportPathWorkaround(parent.toString(), p2.toString());
      return Paths.get(p2.toString(), p.getFileName().toString());
    }
    return p;
  }
  
  private void reportDuplicates() {
    List<Entry<String, List<ConvertedSource>>> nonIdenticals = new ArrayList<>();
    List<ConvertedSource> onlyIdenticals = new ArrayList<>();
    for (Entry<String, List<ConvertedSource>> e : classname2multiSources.entrySet()) {
      List<ConvertedSource> convertedSourcesFor1class = e.getValue();
      if (convertedSourcesFor1class.size() > 1) {
        // have multiple non-identical sources for one class
        nonIdenticals.add(e);
      } else {
        ConvertedSource cs = convertedSourcesFor1class.get(0);
        if (cs.paths.size() > 1) {
          // have multiple (only) identical sources for one class
          onlyIdenticals.add(cs);
        }
      }
    }
    
    if (nonIdenticals.size() == 0) {
      if (onlyIdenticals.size() == 0) {
        System.out.println("No duplicates found.");
      } else {
        // report identical duplicates
        System.out.println("Identical duplicates (only):");
        for (ConvertedSource cs : onlyIdenticals) {
          for (Path path : cs.paths) {
            System.out.println("  " + vWithFileSys(path));
          }
        }
      }
    } else {
      System.out.println("Report of non-identical duplicates");
      for (Entry<String, List<ConvertedSource>> nonIdentical : nonIdenticals) {
        System.out.println("  classname: " + nonIdentical.getKey());
        int i = 1;
        for (ConvertedSource cs : nonIdentical.getValue()) {
          System.out.println("    version " + i);
          for (Path path : cs.paths) {
            System.out.println("      " + vWithFileSys(path));
          }
          i++;
        }
      }
    }
  }
  
  private <T, U> void reportPaths(String title, String fileName, List<? extends Report2<T, U>> items) throws IOException {
    if (items.size() == 0) {
      System.out.println("No " + title);
      return;  
    }
    System.out.println("\n" + title);
    for (int i = 0; i < title.length(); i++) System.out.print('=');
    System.out.println("");
    
    try (BufferedWriter bw = Files.newBufferedWriter(makePath(outDirLog + fileName), StandardOpenOption.CREATE)) {
      List<Report2<T, U>> sorted = new ArrayList<>(items);

      sortReport2(sorted);  
      int max = 0;
      for (Report2<T, U> p : sorted) {
        max = Math.max(max, p.getFirstLength());
      }
      
      int i = 1;
      for (Report2<T, U> p : sorted) {
        Object v = p.getSecond();
        String s = String.format("%5d %-" +max+ "s %s%n", Integer.valueOf(i), p.getFirst(), vWithFileSys(v));
        bw.write(s);
        System.out.print(s);
        i++;
      }
      System.out.println("");
    } // end of try-with-resources
  }
  
  private String vWithFileSys(Object v) {
    if (v instanceof Path) {
      Path path = (Path) v;
      FileSystem fileSystem = path.getFileSystem();
      if (fileSystem instanceof com.sun.nio.zipfs.ZipFileSystem) {
        v = v.toString() + "\t\t " + fileSystem.toString();
      }
    }
    return v.toString();
  }
  
  private <T, U> void sortReport2(List<? extends Report2<T, U>> items) {
    Collections.sort(items, 
        (o1, o2) -> {
          int r = o1.getFirst().compareTo((T) o2.getFirst());
          if (r == 0) {
            r = o1.getSecond().compareTo((U) o2.getSecond());
          }
          return r;
        });
  }

  /**
   * Walk the directory tree rooted at root
   *   - descend subdirectories
   *   - descend Jar file
   *     -- descend nested Jar files (!)
   *        by extracting these to a temp dir, and keeping a back reference to where they were extracted from.
   *   
   * output the paths representing the classes
   *   - the path includes the "file system".
   * @param root
   * @return list of Paths to walk
   * @throws IOException
   */
  private List<Path> getCandidates(CommandLineParser clp, String root) {
    isSkipTypeCheck  = clp.isInArgsList(SKIP_TYPE_CHECK);
    Path startPath = Paths.get(root);
    candidates = new ArrayList<>();
    
    try (Stream<Path> stream = Files.walk(startPath, FileVisitOption.FOLLOW_LINKS)) {  // needed to release file handles
        stream.forEachOrdered(
          p -> getCandidates_processFile(p));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    
    Collections.sort(candidates, pathComparator);
   
    // the collection potentially includes inner class files
    
    List<Path> c = new ArrayList<>();  // candidates
    final int nbrOfPaths = candidates.size();
    if (nbrOfPaths == 0) {
      return c;
    }
    for (int i = 0; i < nbrOfPaths; i++) {
      
      // skip files that end with _Type or 
      //   appear to be inner files: have names with a "$" char
      candidate = candidates.get(i);
      String lastPartOfPath = candidate.getFileName().toString();
      if (lastPartOfPath.endsWith(isSource ? "_Type.java" : "_Type.class")) {
        continue;  // skip the _Type files
      }
      
      if (lastPartOfPath.contains("$")) {
        continue;  // inner class
      }
      
      if (isSkipTypeCheck) {
        c.add(candidate);
      } else {
        // doing _Type check: only include java files if there's an associated _Type file
        //   in the sort order, these follow the file without the _Type suffix
        //   but perhaps there are other names inbetween
        boolean has_Type = has_Type(candidate, i + 1);  // look for the next _Type file starting at position i + 1
        if (!has_Type) {
          continue;  // not a JCas class
        }
        c.add(candidate);  
      }
    }
    return c;  
  }
  
  private final static Comparator<Path> pathComparator = new Comparator<Path>() {
    @Override
    public int compare(Path o1, Path o2) {
      return o1.toString().compareTo(o2.toString());
    }
  };
  
//  // there may be several same-name roots not quite right
//  //   xxx_Type$1.class
//  
//  private void addIfPreviousIsSameName(List<Path> c, int i) {
//    if (i == 0) return;
//    String _Type = candidates.get(i).toString();
////    String prev = r.get(i-1).getPath();
//    String prefix = _Type.substring(0, _Type.length() - ("_Type." + (isSource ? "java" : "class")).length());
//    i--;
//    while (i >= 0) {
//      String s = candidates.get(i).toString();
//      if ( ! s.startsWith(prefix)) {
//        break;
//      }
//      if (s.substring(prefix.length()).equals((isSource ? ".java" : ".class"))) {
//        c.add(candidates.get(i));
//        break;
//      }
//      i--;
//    }
//  }
    
  
  /**
   * adds all the .java or .class files to the candidates, including _Type if not skipping the _Type check
   * Handling embedded jar files
   *   - single level Jar (at the top level of the default file system)
   *     -- handle using an overlayed file system
   *   - embedded Jars within Jars: 
   *     - not supported by Zip File System Provider (it only supports one level)
   *     - handle by extracting to a temp dir, and then using the Zip File System Provider
   * @param path
   */
  private void getCandidates_processFile(Path path) {
//    System.out.println("debug processing " + path);
    try {
//      URI pathUri = path.toUri();
      String pathString = path.toString();
      if (pathString.endsWith(".jar") || pathString.endsWith(".pear")) {  // path.endsWith does not mean this !!
        if (!path.getFileSystem().equals(FileSystems.getDefault())) {        
          // embedded Jar: extract to temp
          Path out = getTempOutputPath(path);
          Files.copy(path, out, StandardCopyOption.REPLACE_EXISTING);
//          embeddedJars.add(new PathAndPath(path, out));
          path = out;
        }
        // experiment - see if this makes a copy
        FileSystem jfs = FileSystems.newFileSystem(Paths.get(path.toUri()), null);
        Path start = jfs.getPath("/");
        try (Stream<Path> stream = Files.walk(start)) {  // needed to release file handles
          stream.forEachOrdered(
            p -> getCandidates_processFile(p));
        }
      } else {
        // is not a .jar file.  see if it's a jcas file
//        System.out.println("debug path ends with java or class " + pathString.endsWith(isSource ? ".java" : ".class") + " " + pathString);
        if (pathString.endsWith(isSource ? ".java" : ".class")) {
          candidates.add(path);
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  private Path getTempOutputPath(Path path) throws IOException {
    Path localTempDir = getTempDir();
    Path tempFile = Files.createTempFile(localTempDir, path.getFileName().toString(),  ".jar");
    tempFile.toFile().deleteOnExit();
    return tempFile;
  }
  
  private Path getTempDir() throws IOException {
    if (tempDir == null) {
      tempDir = Files.createTempDirectory("migrateJCas");
      tempDir.toFile().deleteOnExit();
    }
    return tempDir;
  }  
  
  private boolean has_Type(Path cand, int start) {
    if (start >= candidates.size()) {
      return false;
    }

    String sc = cand.toString();
    String sc_minus_suffix = sc.substring(0,  sc.length() - ( isSource ? ".java".length() : ".class".length())); 
    String sc_Type = sc_minus_suffix + ( isSource ? "_Type.java" : "_Type.class");
    // a string which sorts beyond the candidate + a suffix of "_"
    String s_end = sc_minus_suffix + (char) (((int)'_') + 1);
    for (Path p : candidates.subList(start,  candidates.size())) {
      String s = p.toString();
      if (s_end.compareTo(s) < 0) {
        return false;  // not found, we're already beyond where it would be found
      }
      if (s.equals(sc_Type)) {
        return true;
      }
    }
    return false;
  }
  
  
  private static final CommandLineParser createCmdLineParser() {
    CommandLineParser parser = new CommandLineParser();
    parser.addParameter(SOURCE_FILE_ROOTS, true);
    parser.addParameter(CLASS_FILE_ROOTS, true);
    parser.addParameter(OUTPUT_DIRECTORY, true);
    parser.addParameter(SKIP_TYPE_CHECK, false);
    parser.addParameter(MIGRATE_CLASSPATH, true);
    parser.addParameter(CLASSES, true);
    return parser;
  }

  private final boolean checkCmdLineSyntax(CommandLineParser clp) {
    if (clp.getRestArgs().length > 0) {
      System.err.println("Error parsing CVD command line: unknown argument(s):");
      String[] args = clp.getRestArgs();
      for (int i = 0; i < args.length; i++) {
        System.err.print(" ");
        System.err.print(args[i]);
      }
      System.err.println();
      return false;
    }
    if (clp.isInArgsList(SOURCE_FILE_ROOTS) && clp.isInArgsList(CLASS_FILE_ROOTS)) {
      System.err.println("both sources file roots and classess file roots parameters specified; please specify just one.");
      return false;
    }
    
    if (clp.isInArgsList(OUTPUT_DIRECTORY)) {
      outputDirectory = Paths.get(clp.getParamArgument(OUTPUT_DIRECTORY)).toString();
      if (!outputDirectory.endsWith("/")) {
        outputDirectory = outputDirectory + "/";
      }
      outDirConverted = outputDirectory + "converted/";
      outDirSkipped = outputDirectory + "not-converted/";
      outDirLog = outputDirectory + "logs/";
    } else {
      System.err.println("-outputDirectory is a required parameter, must be a path to a writable file directory.");
      return false;
    }
    
    if (clp.isInArgsList(MIGRATE_CLASSPATH)) {
      migrateClasspath = clp.getParamArgument(MIGRATE_CLASSPATH);
    }
    
    if (clp.isInArgsList(CLASSES)) {
      individualClasses = clp.getParamArgument(CLASSES);
    }
    return true;
  }
  
  private String decompile(byte[] b) {
    ClassLoader cl = getClassLoader();
    UimaDecompiler ud = new UimaDecompiler(cl, null);
    return ud.decompile(b);
  }
  
  private String decompile(String classname) {
    ClassLoader cl = getClassLoader();
    UimaDecompiler ud = new UimaDecompiler(cl, null);
    return ud.decompileToString(classname);
  }
  
//  private String getFullyQualifiedClassNameWithSlashes(byte[] b) {
//    ClassLoader cl = getClassLoader();
//    UimaDecompiler ud = new UimaDecompiler(cl, null);
//    return ud.extractClassNameSlashes(b);    
//  }
  
  private ClassLoader getClassLoader() {
    if (null == cachedMigrateClassLoader) {
      cachedMigrateClassLoader = (migrateClasspath == null) 
                      ? this.getClass().getClassLoader()
                      : new URLClassLoader(Misc.classpath2urls(migrateClasspath));
    }
    return cachedMigrateClassLoader;
  }
  
  private void addImport(String s) {
    cu.getImports().add(new ImportDeclaration(new NameExpr(s), false, false));
  }
  
  private void removeImport(String s) {
    Iterator<ImportDeclaration> it = cu.getImports().iterator();
    while (it.hasNext()) { 
      ImportDeclaration impDcl = it.next();
      if (impDcl.getName().toString().equals(s)) {
        it.remove();
        break;
      }
    } 
  }

  /******************
   * AST Utilities
   ******************/
  
  private Node replaceInParent(Node n, Expression v) {
    Node parent = n.getParentNode();
    if (parent instanceof EnclosedExpr) {
      ((EnclosedExpr)parent).setInner(v);
    } else if (parent instanceof MethodCallExpr) { // args in the arg list
      List<Expression> args = ((MethodCallExpr)parent).getArgs();
      args.set(args.indexOf(n), v);
      v.setParentNode(parent);
    } else if (parent instanceof ExpressionStmt) { 
      ((ExpressionStmt)parent).setExpression(v);
    } else if (parent instanceof CastExpr) { 
      ((CastExpr)parent).setExpr(v);
    } else if (parent instanceof ReturnStmt) { 
      ((ReturnStmt)parent).setExpr(v);
    } else if (parent instanceof AssignExpr) {
      ((AssignExpr)parent).setValue(v);
    } else if (parent instanceof VariableDeclarator) {
      ((VariableDeclarator)parent).setInit(v);      
    } else if (parent instanceof ObjectCreationExpr) {
      List<Expression> args = ((ObjectCreationExpr)parent).getArgs();
      int i = args.indexOf(n);
      if (i < 0) throw new RuntimeException();
      args.set(i, v);      
   } else {
      System.out.println(parent.getClass().getName());
      throw new RuntimeException();
    }
    return v;
  }
    
  /**
   * 
   * @param p the parameter to modify
   * @param t the name of class or interface
   * @param name the name of the variable
   */
  private void setParameter(List<Parameter> ps, int i, String t, String name) {
    Parameter p = ps.get(i);
    p.setType(new ClassOrInterfaceType(t));
    p.setId(new VariableDeclaratorId(name));
  }
  
  private int findConstructor(List<BodyDeclaration> classMembers) {
    int i = 0;
    for (BodyDeclaration bd : classMembers) {
      if (bd instanceof ConstructorDeclaration) {
        return i;
      }
      i++;
    }
    return -1;
  }
  
  private boolean hasTypeFields(List<BodyDeclaration> members) {
    boolean hasType = false;
    boolean hasTypeId = false;
    for (BodyDeclaration bd : members) {
      if (bd instanceof FieldDeclaration) {
        FieldDeclaration f = (FieldDeclaration)bd;
        int m = f.getModifiers();
        if (Modifier.isPublic(m) &&
            Modifier.isStatic(m) &&
            Modifier.isFinal(m) &&
            getTypeName(f.getType()).equals("int")) {
          List<VariableDeclarator> vds = f.getVariables();
          for (VariableDeclarator vd : vds) {
            String n = vd.getId().getName();
            if (n.equals("type")) hasType = true;
            if (n.equals("typeIndexID")) hasTypeId = true;
            if (hasTypeId && hasType) break;
          }
        }
      }
    } // end of for
    return hasTypeId && hasType;
  }
  
  /**
   * Heuristic:
   *   JCas classes have 0, 1, and 2 arg constructors with particular arg types
   *   0 -
   *   1 - JCas
   *   2 - int, TOP_Type  (v2) or TypeImpl, CASImpl (v3)
   *   
   * Additional 1 and 2 arg constructors are permitted.
   * 
   * Sets fields hasV2Constructors, hasV3Constructors
   * @param members
   */
  private void setHasJCasConstructors(List<BodyDeclaration> members) {
    boolean has0ArgConstructor = false;
    boolean has1ArgJCasConstructor = false;
    boolean has2ArgJCasConstructorV2 = false;
    boolean has2ArgJCasConstructorV3 = false;
    
    for (BodyDeclaration bd : members) {
      if (bd instanceof ConstructorDeclaration) {
        List<Parameter> ps = ((ConstructorDeclaration)bd).getParameters();
        if (ps.size() == 0) has0ArgConstructor = true;
        if (ps.size() == 1 && getParmTypeName(ps, 0).equals("JCas")) {
          has1ArgJCasConstructor = true;
        }
        if (ps.size() == 2) {
          if (getParmTypeName(ps, 0).equals("int") &&
              getParmTypeName(ps, 1).equals("TOP_Type")) {
            has2ArgJCasConstructorV2 = true;
          } else if (getParmTypeName(ps, 0).equals("TypeImpl") &&
                     getParmTypeName(ps, 1).equals("CASImpl")) {
            has2ArgJCasConstructorV3 = true;
          }
        } // end of 2 arg constructor
      } // end of is-constructor
    } // end of for loop
    
    hasV2Constructors = has0ArgConstructor && has1ArgJCasConstructor && has2ArgJCasConstructorV2;
    hasV3Constructors = has0ArgConstructor && has1ArgJCasConstructor && has2ArgJCasConstructorV3;
  }
  
  private String getParmTypeName(List<Parameter> p, int i) {
    return getTypeName(p.get(i).getType());
  }
  
  private String getTypeName(Type t) {
    if (t instanceof ReferenceType) {
      t = ((ReferenceType)t).getType();
    }
    
    if (t instanceof PrimitiveType) {
      return ((PrimitiveType)t).toString(); 
    }
    if (t instanceof ClassOrInterfaceType) {
      return ((ClassOrInterfaceType)t).getName();
    }
    Misc.internalError(); return null;
  }
  
  /**
   * Get the name of a field
   * @param e -
   * @return the field name or null
   */
  private String getName(Expression e) {
    e = getUnenclosedExpr(e);
    if (e instanceof NameExpr) {
      return ((NameExpr)e).getName();
    }
    if (e instanceof FieldAccessExpr) {
      return ((FieldAccessExpr)e).getField();
    }
    return null;
  }
  
  private void updateClassName(TypeDeclaration n) {
    if (n.getParentNode() instanceof CompilationUnit) {
      className = n.getName();
      String packageAndClassName = 
          (className.contains(".")) 
            ? className 
            : packageName + '.' + className;
      packageAndClassNameSlash = packageAndClassName.replace('.', '/');
      
      TypeImpl ti = TypeSystemImpl.staticTsi.getType(Misc.javaClassName2UimaTypeName(packageAndClassName));
      if (null != ti) {
        // is a built-in type
        ClassnameAndPath p = new ClassnameAndPath(packageAndClassNameSlash, candidate);
        skippedBuiltins.add(p);
        v3 = false;   // skip further processing of this class
        return;  
      }

      c2ps.add(new ClassnameAndPath(packageAndClassNameSlash, candidate));
      return;
    }
    return;
  }

  private Expression getExpressionFromStmt(Statement stmt) {
    stmt = getStmtFromStmt(stmt);
    if (stmt instanceof ExpressionStmt) {
      return getUnenclosedExpr(((ExpressionStmt)stmt).getExpression());
    }
    return null;
  }
  
  private Expression getUnenclosedExpr(Expression e) {
    while (e instanceof EnclosedExpr) {
      e = ((EnclosedExpr)e).getInner();
    }
    return e;
  }
  
  /**
   * Unwrap (possibly nested) 1 statement blocks
   * @param stmt -
   * @return unwrapped (non- block) statement
   */
  private Statement getStmtFromStmt(Statement stmt) {
    while (stmt instanceof BlockStmt) {
      List<Statement> stmts = ((BlockStmt) stmt).getStmts();
      if (stmts.size() == 1) {
        stmt = stmts.get(0);
        continue;
      }
      return null;
    }
    return stmt;
  }
  
  private void addCastExpr(Statement stmt, Type castType) {
    ReturnStmt rstmt = (ReturnStmt) stmt;
    rstmt.setExpr(new CastExpr(castType, rstmt.getExpr()));
  }
  
  /********************
   * Recording results
   ********************/

  private void recordBadConstructor(String msg) {
    reportMigrateFailed("Constructor is incorrect, " + msg);
  }
      
  private void reportParseException() {
    reportMigrateFailed("Unparsable Java");
  }
  
  private void migrationFailed(String reason) {
    failedMigration.add(new PathAndReason(candidate, reason));
    v3 = false;    
  }
  
  private void reportMigrateFailed(String m) {
    System.out.format("Skipping this file due to error: %s, path: %s%n", m, candidate);
    migrationFailed(m);
  }
  
  private void reportV2Class() {
    v2JCasFiles.add(candidate);
    v2 = true;
  }
  
  private void reportV3Class() {
    v3JCasFiles.add(candidate);
    v3 = true;
  }
  
  private void reportNotJCasClass(String reason) {
    nonJCasFiles.add(new PathAndReason(candidate, reason));
    v3 = false;
  }
  
  private void reportNotJCasClassMissingTypeFields() {
    reportNotJCasClass("missing required type and/or typeIndexID static fields");
  }
  
  private void reportDeletedCheckModified(String m) {
    deletedCheckModified.add(new PathAndReason(candidate, m));
  }
  
  private void reportMismatchedFeatureName(String m) {
    manualInspection.add(new PathAndReason(candidate, "This getter/setter name doesn't match internal feature name: " + m));
  }
  
  private void reportUnrecognizedV2Code(String m) {
    migrationFailed("V2 code not recognized:\n" + m);
  }
  
  private void reportPathWorkaround(String orig, String modified) {
    pathWorkaround.add(new String1AndString2(orig, modified));
  }
  
  /***********************************************/
    
  private void getBaseOutputPath() {
    String s = packageAndClassNameSlash;
    int i = 0;
    while (!usedPackageAndClassNames.add(s)) {
      i = i + 1;
      s = packageAndClassNameSlash + "_dupid_" + i;
    }
    packageAndClassNameSlash_i = i;
  }
  
  private String getBaseOutputPath(boolean wasConverted, boolean isV2) {
    return (wasConverted ? outDirConverted : outDirSkipped) + (isV2 ? "v2/" : "v3/") 
        + ((packageAndClassNameSlash_i > 0) 
             ? (Integer.toString(packageAndClassNameSlash_i) + "/") 
             : "")
        + packageAndClassNameSlash 
        + ".java";
  }
  
  private void writeV2Orig(String data, boolean wasConverted) throws IOException {
    String base = getBaseOutputPath(wasConverted, true);  // adds numeric suffix if dupls
    FileUtils.writeToFile(makePath(base), data);
  }
  
  private void writeV3(String data) throws IOException {
    String base = getBaseOutputPath(true, false);  // adds numeric suffix if dupls
    data = fixImplementsBug(data);
    FileUtils.writeToFile(makePath(base), data);
  }
  
  private void printUsage() {
    System.out.println(
        "Usage: java org.apache.uima.migratev3.jcas.MigrateJCas \n"
        + "  [-sourcesRoots <One-or-more-directories-or-jars-separated-by-Path-separator>]\n"
        + "  [-classesRoots <One-or-more-directories-or-jars-separated-by-Path-separator>]\n"
        + "  [-classes <one-or-more-fully-qualified-class-names-separated-by-Path-separator]\n"
        + "            example:  -classes mypkg.foo:pkg2.bar\n"
        + "  [-outputDirectory a-writable-directory-path (required)\n"
        + "  [-migrateClasspath a-class-path to use in decompiling, required if -classesRoots is specified, or\n"
        + "                     "
        + "  [-skipTypeCheck if specified, skips validing a found item by looking for the corresponding _Type file"
        + "  NOTE: either -sourcesRoots or -classesRoots is required, but only one may be specified.\n"
        + "  NOTE: classesRoots are scanned for JCas classes, which are then decompiled, and the results processed like sourcesRoots\n"
        + "        The decompiling requires that the classes being scanned be on the migrateClasspath when this is invoked.\n"
        + "  NOTE: -outputDirectory is required\n");
  }

  private static final Pattern implementsEmpty = Pattern.compile("implements  \\{");
  private String fixImplementsBug(String data) {
    return implementsEmpty.matcher(data).replaceAll("{");
  }
  
//  /**
//   * Called after class is migrated
//   * Given a path to a class (source or class file), 
//   * return the URL to the class as found in the classpath.
//   *   This returns the "first" one found in the classpath, in the case of duplicates.
//   * @param path
//   * @return the location of the class in the class path
//   */
//  private URL getPathForClass(Path path) {
//    return (null == packageAndClassNameSlash) 
//             ? null 
//             : migrateClassLoader.getResource(packageAndClassNameSlash + ".class");
//  }
  
  /**
   * prepare to migrate one class
   */
  private String  prepare(Path c) {
    candidate = c;
    packageName = null;
    className = null;
    packageAndClassNameSlash = null;
    cu = null;
    return getSource(c);
  }
  
  private String prepareIndividual(String classname) {
    candidate = Paths.get(classname); // a pseudo path
    packageName = null;
    className = null;
    packageAndClassNameSlash = null;
    cu = null;
    return decompile(classname); // always look up in classpath
                                 // to decompile individual source - put in sourcesRoots
  }
  
  /*********************************************************************
   * Reporting classes
   *********************************************************************/
  
  private static abstract class Report2<T, U> {
    public abstract Comparable<T> getFirst(); // Eclipse on linux complained if not public, was OK on windows
    public abstract Comparable<U> getSecond();  
    abstract int getFirstLength();
  }
  
  private static class PathAndReason extends Report2<Path, String> {
    Path path;
    String reason;
    PathAndReason(Path path, String reason) {
      this.path = path;
      this.reason = reason;
    }
    @Override
    public Comparable<Path> getFirst() { return path; }
    @Override
    public Comparable<String> getSecond() { return reason; }
    @Override
    int getFirstLength() { return path.toString().length(); }
  }
  
  private static class ClassnameAndPath extends Report2<String, Path> {
    String classname;
    Path path;
    ClassnameAndPath(String classname, Path path) {
      this.classname = classname;
      this.path = path;
    }
    @Override
    public Comparable<String> getFirst() { return classname; }
    @Override
    public Comparable<Path> getSecond() { return path; }
    @Override
    int getFirstLength() { return classname.length(); }
    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((classname == null) ? 0 : classname.hashCode());
      result = prime * result + ((path == null) ? 0 : path.hashCode());
      return result;
    }
    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (!(obj instanceof ClassnameAndPath))
        return false;
      ClassnameAndPath other = (ClassnameAndPath) obj;
      if (classname == null) {
        if (other.classname != null)
          return false;
      } else if (!classname.equals(other.classname))
        return false;
      if (path == null) {
        if (other.path != null)
          return false;
      } else if (!path.equals(other.path))
        return false;
      return true;
    }
  }
  
  private static class String1AndString2 extends Report2<String, String> {
    String s1;
    String s2;
    String1AndString2(String s1, String s2) {
      this.s1 = s1;
      this.s2 = s2;
    }
    @Override
    public Comparable<String> getFirst() { return s1; }
    @Override
    public Comparable<String> getSecond() { return s2; }
    @Override
    int getFirstLength() { return s1.toString().length(); }
  }

}
