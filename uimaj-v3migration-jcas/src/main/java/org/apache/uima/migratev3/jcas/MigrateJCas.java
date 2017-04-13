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
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.apache.uima.UIMARuntimeException;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.impl.TypeSystemImpl;
import org.apache.uima.cas.impl.UimaDecompiler;
import org.apache.uima.internal.util.CommandLineParser;
import org.apache.uima.internal.util.Misc;
import org.apache.uima.internal.util.UIMAClassLoader;
import org.apache.uima.pear.tools.PackageBrowser;
import org.apache.uima.pear.tools.PackageInstaller;
import org.apache.uima.util.FileUtils;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.SimpleName;
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
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.printer.PrettyPrinter;
import com.github.javaparser.printer.PrettyPrinterConfiguration;

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
 *         1/                                   &lt;&lt; for duplicates, each set is for identical dups, different sets for non-identical
 *           x/y/z/javapath/.../Classname.java  &lt;&lt; for duplicates, each set is for identical dups, different sets for non-identical  
 *           x/y/z/javapath/.../Classname.java  &lt;&lt; for duplicates, each set is for identical dups, different sets for non-identical
 *           ...                                    
 *         2/                                   &lt;&lt; for duplicates, each set is for identical dups, different sets for non-identical  
 *           x/y/z/javapath/.../Classname.java  &lt;&lt; for duplicates, each set is for identical dups, different sets for non-identical
 *           x/y/z/javapath/.../Classname.java  &lt;&lt; for duplicates, each set is for identical dups, different sets for non-identical
 *           ...
 *       
 *       v3-classes - the compiled form if a java compiler was available, and no duplicates
 *     
 *     jars - copies of the original JARs with the converted JCas classes, if there were no duplicates
 *     pears - copies of the original PEARs with the converted JCas classes, if there were no duplicates
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
 *     if no duplicates, and if a Java compiler (JRE) is available,
 *       - compiles the results
 *       - copies the JARs and the PEARs, with replacement with the migrated versions of the JCas classes.   
 *       
 *   Mode 2: Given sources-roots 
 *     Duplicates are migrated, results are put into a v3/nnn/ rest-of-path-identical
 * 
 * <p>Note: Each run clears the output directory before starting the migration.
 * <p>Note: classpath may be specified using -migrateClassPath or as the class path used to run this tool. 
 */
public class MigrateJCas extends VoidVisitorAdapter<Object> {
  
  /* *****************************************************
   * Internals
   * 
   *   Processing roots collection: done for source or class
   *     - iterate, for all roots
   *       -- processCollection for candidates rooted at that root 
   *         --- candidate is .java or .class, with path, with pearClasspath string
   *           ---- migrate called on each candidate
   *             ----- check to see if already done, and if so, skip.
   *               ------ means: same byte or source code associated with same fqcn  
   *     
   *   Multiple sources for single class:
   *     classname2multiSources: TreeMap from fqcn to ConvertedSource (string or bytes)
   *     ConvertedSource: supports multiple paths having identical string/bytes.
   *     
   *   Compiling: driven from c2ps array of fqcn, path
   *      - c2ps  -- inverse of -- path2classname
   *      - may have multiple entries for same fqcn, with different paths, 
   *        -- only if different values for the impl
   *      - set when visiting top-level compilation unit non-built-in type
   *     
   *   
   */
  
  private static final String SOURCE_FILE_ROOTS = "-sourcesRoots";

  private static final String CLASS_FILE_ROOTS = "-classesRoots";

  private static final String OUTPUT_DIRECTORY = "-outputDirectory";
  
  private static final String SKIP_TYPE_CHECK = "-skipTypeCheck";
  
  private static final String MIGRATE_CLASSPATH = "-migrateClasspath"; 
  
  private static final String CLASSES = "-classes"; // individual classes to migrate, get from supplied classpath
  
  private static final Type intType = PrimitiveType.intType();
  
  private static final EnumSet<Modifier> public_static_final = 
      EnumSet.of(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL); 
  
  private static final PrettyPrinterConfiguration printWithoutComments = 
      new PrettyPrinterConfiguration();
  static { printWithoutComments.setPrintComments(false); }

  private static final PrettyPrinterConfiguration printCu = 
      new PrettyPrinterConfiguration();
  static { printCu.setIndent("  "); }
  
  private static final String ERROR_DECOMPILING = "!!! ERROR:";
    
  /*****************
   * Candidate
   *****************/
  private static final class Candidate {
    /** 
     * path to the .class or .java file
     */
    final Path p;
    
    /**
     *  null or (if in a Pear), the pear's classpath (from installing and then getting the classpath)
     *    - includes jars in lib/ dir and the bin/ classes
     */
    final String pearClasspath;
    Candidate(Path p) {
      this.p = p;
      pearClasspath = null;
    }
    
    Candidate(Path p, String pearClasspath) {
      this.p = p;
      this.pearClasspath = pearClasspath;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
      String pear = (pearClasspath == null) ? "" :
                      ", pearClasspath=" + pearClasspath;
      return "Candidate [p=" + p + pear + "]";
      
    }
    
    
  }
    
  /*****************
   *  P E A R or J A R 
   *  
   * Information for each PEAR or JAR that is processed
   * Used when post-processing pears
   *****************/
  private static final class PearOrJar {
    
    /**
     * path to original .pear or .jar file among the roots
     */
    final Path pathToPearOrJar;
    
    /** 
     * path to .class file in pear e.g. bin/org/apache/uima/examples/tutorial/Sentence.class
     *                  or in jar  e.g. org/apache/uima/examples/tutorial/Sentence.class 
     */
    
    final List<String> pathsToCandidateFiles = new ArrayList<>();
        
    PearOrJar(Path pathToPearOrJar) {
      this.pathToPearOrJar = pathToPearOrJar; 
    }
  }
  
  /**
   * Map 
   *   key
   */
  final List<String> classnames = new ArrayList<>();

  private Path tempDir = null;
    
  private boolean isSource = false;
  
  private Candidate candidate;
  private List<Candidate> candidates;
  
  private PearOrJar pear_current;
  private Deque<PearOrJar> jar_current_stack = new ArrayDeque<>();  
  private List<PearOrJar> pears = new ArrayList<>();
  private List<PearOrJar> jars = new ArrayList<>();
  
  /**
   * current Pear install path + 1 more dir in temp dir
   * used in candidate generation to relativize the path to just the part inside the pear
   */
  private Path pearResolveStart;
  
  /**
   * Map created when adding a pear's .class/.source file to candidates
   *   Key: path string to .class or .java file in an installed Pear
   *   Value: path part corresponding to inside pear - delete install dir + 1 more dir from front
   */
  private Map<String, String> path2InsidePearOrJarPath = new HashMap<>();
  
  /**
   * Map created when adding a (pear's or any) .class/.source file to candidates
   *   Key: path string to .class or .java file in an installed Pear
   *   Value: path part corresponding to just the classname
   *   
   * inverse of c2ps
   */
  private Map<String, String> path2classname = new HashMap<>();
  /**
   * inverse of path2classname
   */
  private final List<ClassnameAndPath> c2ps = new ArrayList<>();            // class, path
 
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
  private final List<ClassnameAndPath> skippedBuiltins = new ArrayList<>();  // class, path
  private final List<PathAndReason> deletedCheckModified = new ArrayList<>();  // path, deleted check string
  private final List<String1AndString2> pathWorkaround = new ArrayList<>(); // original, workaround
  private final List<String1AndString2> pearClassReplace = new ArrayList<>(); // pear, classname
  private final List<String1AndString2> jarClassReplace  = new ArrayList<>(); // jar, classname
  
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
  private NodeList<BodyDeclaration<?>> fi_fields = new NodeList<>();
  private Set<String> featNames = new HashSet<>();

  private boolean hasV2Constructors;
  private boolean hasV3Constructors;

  private boolean isSkipTypeCheck = false;

  
  private byte[] origBytes;   // set by getSource()
  private String origSource;  // set by getSource()
  private String alreadyDone; // the slashifiedClassName

  /** true if 1 or more classes have duplicate, non-identical values in the scan */
  private boolean haveNonIdenticalDuplicates = false;

  private boolean error_decompiling = false;


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
    
    System.out.format("Output top directory: %s%n", outputDirectory);
    
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

    boolean isOk = ! error_decompiling;
    
    isOk = isOk && report();
    
    boolean noErrors = postProcessing();
    
    isOk = isOk && noErrors;
    
    System.out.println("Migration finished " + 
       (isOk 
           ? "with no unusual conditions." 
           : "with 1 or more unusual conditions that need manual checking."));
  }
  
  private boolean postProcessing() {
    boolean noErrors = true;
    if (javaCompilerAvailable() && ! haveNonIdenticalDuplicates) {
      noErrors = compileV3PearSources();
      noErrors = noErrors && compileV3NonPearSources();
      
      postProcessPearsOrJars("jars" , jars ,  jarClassReplace);
      postProcessPearsOrJars("pears", pears, pearClassReplace);
      
//    
//      try {
//        Path pearOutDir = Paths.get(outputDirectory, "pears");
//        FileUtils.deleteRecursive(pearOutDir.toFile());
//        Files.createDirectories(pearOutDir);
//      } catch (IOException e) {
//        throw new RuntimeException(e);
//      }
//        
//      System.out.format("replacing .class files in %,d PEARs%n", pears.size());
//      for (PearOrJar p : pears) {
//        pearOrJarPostProcessing(p);
//      }
//      try {
//        reportPaths("Reports of updated Pears", "pearFileUpdates.txt", pearClassReplace);
//      } catch (IOException e) {
//        throw new RuntimeException(e);
//      }
    }
    return noErrors;
  }
  
  private void postProcessPearsOrJars(String kind, List<PearOrJar> pearsOrJars, List<String1AndString2> classReplace) {  // pears or jars
    try {
      Path outDir = Paths.get(outputDirectory, kind);
      FileUtils.deleteRecursive(outDir.toFile());
      Files.createDirectories(outDir);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    
    // pearsOrJars may have entries with 0 candidate paths.  This happens when we scan them
    // but find nothing to convert.  
    // eliminate these.
    
    Iterator<PearOrJar> it = pearsOrJars.iterator();
    while (it.hasNext()) {
      PearOrJar poj = it.next();
      if (poj.pathsToCandidateFiles.size() == 0) {
        it.remove();
      }
    }
    
    if (pearsOrJars.size() == 0) {
      System.out.format("No .class files were replaced in %s.%n", kind);
    } else {
      System.out.format("replacing .class files in %,d %s%n", pearsOrJars.size(), kind);
      for (PearOrJar p : pearsOrJars) {
        pearOrJarPostProcessing(p, kind);
      }
      try {
        reportPaths("Reports of updated " + kind, kind + "FileUpdates.txt", classReplace);
       
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    
  }
  
  private void pearOrJarPostProcessing(PearOrJar pearOrJar, String kind) { // pears or jars
    try {
      final boolean isPear = kind.equals("pears");
      // copy the pear so we don't change the original
      Path pearOrJarCopy = Paths.get(outputDirectory, kind, pearOrJar.pathToPearOrJar.getFileName().toString());
     
      Files.copy(pearOrJar.pathToPearOrJar, pearOrJarCopy);    

      // put up a file system on the pear or jar
      FileSystem pfs = FileSystems.newFileSystem(pearOrJarCopy, null);
    
      // replace the .class files in this pear with corresponding v3 ones
      for (int i = 0; i < pearOrJar.pathsToCandidateFiles.size(); i++) {
        String candidatePath = pearOrJar.pathsToCandidateFiles.get(i);
        String path_in_v3_classes = isPear
                                      ? getPath_in_v3_classes(candidatePath)
                                      : candidatePath;
      
        Path src = Paths.get(outputDirectory, "converted/v3-classes", path_in_v3_classes 
            + (isPear ? ".class" : ""));
        Path tgt = pfs.getPath(
            "/", 
            isPear 
              ? path2InsidePearOrJarPath.get(candidatePath) // needs to be bin/org/... etc
              : candidatePath);  // needs to be org/... etc
        if (Files.exists(src)) {
          Files.copy(src, tgt, StandardCopyOption.REPLACE_EXISTING);
          reportPearOrJarClassReplace(pearOrJarCopy.toString(), path_in_v3_classes, kind);
        }
      }
      
      pfs.close();     
      
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  /**
   * @param pathInPear a complete path to a class inside an (installed) pear
   * @return the part starting after the top node of the install dir
   */
  private String getPath_in_v3_classes(String pathInPear) { 
    return path2classname.get(pathInPear);  
  }
  
  /**
   * @return true if no errors
   */
  private boolean compileV3PearSources() {
    boolean noError = true;
    Map<String, List<ClassnameAndPath>> p2c = c2ps.stream()
      .filter(c -> c.pearClasspath != null)
      .collect(Collectors.groupingBy(c -> c.pearClasspath));
    
    List<Entry<String, List<ClassnameAndPath>>> ea = p2c.entrySet().stream()
            .sorted((e1, e2) -> e1.getKey().compareTo(e2.getKey()))
            .collect(Collectors.toList());

    for (Entry<String, List<ClassnameAndPath>> e : ea) {
      noError = noError && compileV3SourcesCommon(e.getValue(), "for Pear " + e.getKey(), e.getKey() );
    }
    return noError;
  }

  /**
   * @return true if no errors
   */
  private boolean compileV3NonPearSources() {
    
    List<ClassnameAndPath> cnps = c2ps.stream()
                                      .filter(c -> c.pearClasspath == null)
                                      .collect(Collectors.toList());
    
    return compileV3SourcesCommon(cnps, "(non PEAR)", null);
  }

  /**
   * When running the compiler to compile v3 sources, we need a classpath that at a minimum
   * includes uimaj-core.  The strategy is to use the invoker of this tool's classpath as
   * specified from the application class loader
   * @return true if no errors
   */
  private boolean compileV3SourcesCommon(List<ClassnameAndPath> items, String msg, String pearClasspath) {
    
    if (items.size() == 0) {
      return true;
    }
    System.out.format("Compiling %,d classes %s -- This may take a while!%n", c2ps.size(), msg);
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, Charset.forName("UTF-8"));
    
    List<String> cus = items.stream()
                            .map(c -> outDirConverted + "v3/" + c.classname + ".java")
                            .collect(Collectors.toList());
    
    Iterable<String> compilationUnitStrings = cus;

    Iterable<? extends JavaFileObject> compilationUnits = 
        fileManager.getJavaFileObjectsFromStrings(compilationUnitStrings);
    
    // specify where the output classes go
    String classesBaseDir = outDirConverted + "v3-classes";
    try {
      Files.createDirectories(Paths.get(classesBaseDir));
    } catch (IOException e) {
      throw new UIMARuntimeException(e);
    }
    // specify the classpath
    String classpath = getCompileClassPath(pearClasspath);
    Iterable<String> options = Arrays.asList("-d", classesBaseDir,
                                             "-classpath", classpath);
    return compiler.getTask(null, fileManager, null, options, null, compilationUnits).call();    
  }
  
  
  /**
   * The classpath used to compile is
   *   - the classpath for this migration app
   *   - any passed in migrate classpath
   *   - any jars found in the source
   * @return the classpath to use in compiling the jcasgen'd sources
   */
  private String getCompileClassPath(String pearClasspath) {
 
    // start with this (the v3migration tool) app's classpath to a cp string
    StringBuilder cp = new StringBuilder(System.getProperty("java.class.path"));

    // if there is a pear classpath, add that
    
    if (null != pearClasspath) {
      cp = cp.append(File.pathSeparator).append(pearClasspath);
    }
    
    // add the migrateClasspath, expanded
    
    if (null != migrateClasspath) {
      cp.append(File.pathSeparator).append(Misc.expandClasspath(migrateClasspath));
    }
    
    // add all 1st level JARs found in scan, in arbitrary order, following.
    //   (Java compiler might or might not include Jars contained within those 1st level Jars)
    try {
    for (PearOrJar jar : jars) {
      cp.append(File.pathSeparator).append(jar.pathToPearOrJar.toRealPath());        
    }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    
//    System.out.println("debug: classpath = " + cp.toString());
    return cp.toString();
  }
    
  private boolean javaCompilerAvailable() {
    if (null == ToolProvider.getSystemJavaCompiler()) {
      System.out.println("The migration tool would like to compile the migrated files, \n"
          + "  but no Java compiler is available.\n"
          + "  To make one available, run this tool using a Java JDK, not JRE");
      return false;
    }
    return true;
  }
  
  private void processRootsCollection(String kind, String[] roots, CommandLineParser clp) {
    for (String root : roots) {
      processCollection("from " + kind + "  root: " + root, new Iterator<String>() { 
        Iterator<Candidate> it = getCandidates(clp, root).iterator();
        public boolean hasNext() {return it.hasNext();}
        public String next() {
          return prepare(it.next());}
      });
    }
  }
  
  
  private void processCollection(String sourceName, Iterator<String> sourceIterator) {
    System.out.println("Migrating " + sourceName);
    System.out.println("number of classes migrated:");
    System.out.flush();
    int i = 1;
    System.out.print("    ");
    
    while (sourceIterator.hasNext()) {
      migrate(sourceIterator.next());  // prepare() is run on each item
                                       // sets field candidate
      if ((i % 50) == 0) System.out.format("%4d%s", Integer.valueOf(i), "\r");
      i++;
    }
    System.out.format("%4d%n", Integer.valueOf(i - 1));   
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
    
  private String[] getRoots(CommandLineParser clp, String kind) {
    return clp.getParamArgument(kind).split("\\" + File.pathSeparator);
  }
  
  /**
   * If working with .class files:
   *   - read the byte array
   *   - see if it has already been decompiled; if so, return false
   *   
   * Side effect, set origSource and origBytes and alreadyDone
   * @param c
   * @return the source to maybe migrate
   */
  private String getSource(Candidate c) {
    try {
      origSource = null;
      origBytes = null;
      if (isSource) {
        origSource = FileUtils.reader2String(Files.newBufferedReader(c.p));
        alreadyDone = origSourceToClassName.get(origSource);
//        System.out.println("debug read " + s.length());
      } else {
        origBytes = Files.readAllBytes(c.p);
        alreadyDone = origBytesToClassName.get(origBytes);
        if (null == alreadyDone) {
          origSource = decompile(origBytes, c.pearClasspath);
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
      if (source.startsWith(ERROR_DECOMPILING)) {
        System.err.println("Decompiling failed, got: " + source);
        System.err.println("Please check the migrateClasspath");
        System.err.println(" argument was: " + migrateClasspath);
        if (null == migrateClasspath) {
          System.err.println("classpath of this app is");
          System.err.println(System.getProperty("java.class.path"));
        } else {
          System.err.println("  Value used was:");
          URL[] urls = Misc.classpath2urls(migrateClasspath);
          for (URL url : urls) {
            System.err.println("    " + url.toString());
          }
        }
        System.err.println("Skipping this component");
        error_decompiling  = true;
        return;
      }
      
      StringReader sr = new StringReader(source);
      try {
        cu = JavaParser.parse(sr);
        
        addImport("org.apache.uima.cas.impl.CASImpl");
        addImport("org.apache.uima.cas.impl.TypeImpl");
        addImport("org.apache.uima.cas.impl.TypeSystemImpl");
        
        this.visit(cu, null);      
        new removeEmptyStmts().visit(cu, null);
        if (v3) {
          removeImport("org.apache.uima.jcas.cas.TOP_Type");
        }
        
        if (v3 && fi_fields.size() > 0) {
          NodeList<BodyDeclaration<?>> classMembers = cu.getTypes().get(0).getMembers();
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
            String s = new PrettyPrinter(printCu).print(cu);
            writeV3(s);  
          }
          System.out.print(".");
        } else {
          System.out.print("d");
        }
      } catch (IOException e) {
        e.printStackTrace();
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
        cs.add(candidate.p);
        found = true;
        break;
      }
    }

    
    if (!found) {
      if (convertedSources.size() > 0) {
        haveNonIdenticalDuplicates = true;
      }
      ConvertedSource cs = new ConvertedSource(origSource, origBytes, candidate.p);
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

//  @Override
//  public void visit(EmptyTypeDeclaration n, Object ignore) {
//    updateClassName(n);
//    super.visit(n,  ignore);
//  }

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
   * @param n -
   * @param ignore -
   */
  @Override
  public void visit(ClassOrInterfaceDeclaration n, Object ignore) {
    // do checks to see if this is a JCas class; if not report skipped
   
    Optional<Node> maybeParent = n.getParentNode();
    if (maybeParent.isPresent()) {
      Node parent = maybeParent.get();
      if (parent instanceof CompilationUnit) {
        updateClassName(n);
        NodeList<ClassOrInterfaceType> supers = n.getExtendedTypes();
        if (supers == null || supers.size() == 0) {
          reportNotJCasClass("class doesn't extend a superclass");
          super.visit(n, ignore);
          return; 
        }
        
        NodeList<BodyDeclaration<?>> members = n.getMembers();
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
      }
    }
    
    super.visit(n,  ignore);
    return;
    
  }
  
  @Override
  public void visit(PackageDeclaration n, Object ignored) {
    packageName = n.getNameAsString();
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
      NodeList<Statement> stmts = n.getBody().getStatements();
      if (!(stmts.get(0) instanceof ExplicitConstructorInvocationStmt)) {
        recordBadConstructor("missing super call");
        return;
      }
      NodeList<Expression> args = ((ExplicitConstructorInvocationStmt)(stmts.get(0))).getArguments();
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
    String name = n.getNameAsString();
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
        String bodyString = n.getBody().get().toString(printWithoutComments);
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
        NodeList<Expression> args = new NodeList<>();
        args.add(new StringLiteralExpr(featName));
        VariableDeclarator vd = new VariableDeclarator(
            intType, 
            "_FI_" + featName, 
            new MethodCallExpr(new NameExpr("TypeSystemImpl"), new SimpleName("getAdjustedFeatureOffset"), args));
        if (featNames.add(featName)) {
          fi_fields.add(new FieldDeclaration(public_static_final, vd));
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
          for (Statement stmt : n.getBody().get().getStatements()) {
            if (stmt instanceof ReturnStmt) {
              Expression e = getUnenclosedExpr(((ReturnStmt)stmt).getExpression().get());
              if ((e instanceof MethodCallExpr)) {
                String methodName = ((MethodCallExpr)e).getNameAsString();
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
          ((FieldAccessExpr)be.getLeft()).getNameAsString().equals("featOkTst")) {
        // remove the feature missing if statement
        
        // verify the remaining form 
        if (! (be.getRight() instanceof BinaryExpr) 
         || ! ((be2 = (BinaryExpr)be.getRight()).getRight() instanceof NullLiteralExpr) 
         || ! (be2.getLeft() instanceof FieldAccessExpr)

         || ! ((e = getExpressionFromStmt(n.getThenStmt())) instanceof MethodCallExpr)
         || ! (((MethodCallExpr)e).getNameAsString()).equals("throwFeatMissing")) {
          reportDeletedCheckModified("The featOkTst was modified:\n" + n.toString() + '\n');
        }
              
        BlockStmt parent = (BlockStmt) n.getParentNode().get();
        stmts = parent.getStatements();
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
    Optional<Node> p1, p2, p3 = null; 
    Node updatedNode = null;
    NodeList<Expression> args;
    
    do {
      if (get_set_method == null) break;
     
      /** remove checkArraybounds statement **/
      if (n.getNameAsString().equals("checkArrayBounds") &&
          ((p1 = n.getParentNode()).isPresent() && p1.get() instanceof ExpressionStmt) &&
          ((p2 = p1.get().getParentNode()).isPresent() && p2.get() instanceof BlockStmt) &&
          ((p3 = p2.get().getParentNode()).isPresent() && p3.get() == get_set_method)) {
        NodeList<Statement> stmts = ((BlockStmt)p2.get()).getStatements();
        stmts.set(stmts.indexOf(p1), new EmptyStmt());
        return;
      }
           
      // convert simpleCore expression ll_get/setRangeValue
      boolean useGetter = isGetter || isArraySetter;
      if (n.getNameAsString().startsWith("ll_" + (useGetter ? "get" : "set") + rangeNameV2Part + "Value")) {
        args = n.getArguments();
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
      String nname = n.getNameAsString();
      if (nname.startsWith(z) &&
          nname.endsWith("ArrayValue")) {
        
        String s = nname.substring(z.length());
        s = s.substring(0,  s.length() - "Value".length()); // s = "ShortArray",  etc.
        if (s.equals("RefArray")) s = "FSArray";
        if (s.equals("IntArray")) s = "IntegerArray";
        EnclosedExpr ee = new EnclosedExpr(
            new CastExpr(new ClassOrInterfaceType(s), n.getArguments().get(0)));
        
        n.setScope(ee);    // the getter for the array fs
        n.setName(isGetter ? "get" : "set");
        n.getArguments().remove(0);
      }
      
      /** remove ll_getFSForRef **/
      /** remove ll_getFSRef **/
      if (n.getNameAsString().equals("ll_getFSForRef") ||
          n.getNameAsString().equals("ll_getFSRef")) {
        updatedNode = replaceInParent(n, n.getArguments().get(0));        
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
    Optional<Expression> oe;
    String nname = n.getNameAsString();
    
    if (get_set_method != null) {  
      if (nname.startsWith("casFeatCode_") &&
          ((oe = n.getScope()).isPresent()) &&
          ((e = getUnenclosedExpr(oe.get())) instanceof CastExpr) &&
          ("jcasType".equals(getName(((CastExpr)e).getExpression())))) {
        String featureName = nname.substring("casFeatCode_".length());
        replaceInParent(n, new NameExpr("_FI_" + featureName)); // repl last in List<Expression> (args)
        return;
      } else if (nname.startsWith("casFeatCode_")) {
        reportMigrateFailed("Found field casFeatCode_ ... without a previous cast expr using jcasType");
      }
    }
    super.visit(n,  ignore);      
  }
  
  private class removeEmptyStmts extends VoidVisitorAdapter<Object> {
    @Override
    public void visit(BlockStmt n, Object ignore) {
      Iterator<Statement> it = n.getStatements().iterator();
      while (it.hasNext()) {
        if (it.next() instanceof EmptyStmt) {
          it.remove();
        }
      }
      super.visit(n,  ignore);
    }
    
//    @Override
//    public void visit(MethodDeclaration n, Object ignore) {
//      if (n.getNameAsString().equals("getModifiablePrimitiveNodes")) {
//        System.out.println("debug");
//      }
//      super.visit(n,  ignore);
//      if (n.getNameAsString().equals("getModifiablePrimitiveNodes")) {
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
   * @return true if it's likely everything converted OK.
   */
  private boolean report() {
    System.out.println("\n\nMigration Summary");
    System.out.format("Output top directory: %s%n", outputDirectory);
    System.out.format("Date/time: %tc%n", new Date());
    pprintRoots("Sources", sourcesRoots);
    pprintRoots("Classes", classesRoots);

    boolean isOk2 = true;
    try {
      isOk2 = isOk2 && reportPaths("Workaround Directories", "workaroundDir.txt", pathWorkaround);
      isOk2 = isOk2 && reportPaths("Reports of converted files where a deleted check was customized", "deletedCheckModified.txt", deletedCheckModified);
      isOk2 = isOk2 && reportPaths("Reports of converted files needing manual inspection", "manualInspection.txt", manualInspection);
      isOk2 = isOk2 && reportPaths("Reports of files which failed migration", "failed.txt", failedMigration);
      isOk2 = isOk2 && reportPaths("Reports of non-JCas files", "NonJCasFiles.txt", nonJCasFiles);
      isOk2 = isOk2 && reportPaths("Builtin JCas classes - skipped - need manual checking to see if they are modified",
          "skippedBuiltins.txt", skippedBuiltins);
      // can't do the pear report here - post processing not yet done.      
//      computeDuplicates();
//      reportPaths("Report of duplicates - not identical", "nonIdenticalDuplicates.txt", nonIdenticalDuplicates);
//      reportPaths("Report of duplicates - identical", "identicalDuplicates.txt", identicalDuplicates);
      isOk2 = isOk2 && reportDuplicates();
      
      reportPaths("Report of processed files", "processed.txt", c2ps);
      return isOk2;
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
  
  private boolean reportDuplicates() {
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
        System.out.println("There were no duplicates found.");
      } else {
        // report identical duplicates
        System.out.println("Identical duplicates (only):");
        for (ConvertedSource cs : onlyIdenticals) {
          for (Path path : cs.paths) {
            System.out.println("  " + vWithFileSys(path));
          }
        }
      }
      return true;
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
    return false;
  }
  
  /**
   * 
   * @param title -
   * @param fileName -
   * @param items -
   * @return true if likely ok
   * @throws IOException -
   */
  private <T, U> boolean reportPaths(String title, String fileName, List<? extends Report2<T, U>> items) throws IOException {
    if (items.size() == 0) {
      System.out.println("There were no " + title);
      return true;
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
    return false;
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
  private List<Candidate> getCandidates(CommandLineParser clp, String root) {
    isSkipTypeCheck  = clp.isInArgsList(SKIP_TYPE_CHECK);
    Path startPath = Paths.get(root);
    candidates = new ArrayList<>();
    
    try (Stream<Path> stream = Files.walk(startPath, FileVisitOption.FOLLOW_LINKS)) {  // needed to release file handles
        stream.forEachOrdered(
          p -> getCandidates_processFile(p, null));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    
    Collections.sort(candidates, pathComparator);
   
    // the collection potentially includes inner class files
    
    List<Candidate> c = new ArrayList<>();  // candidates
    final int nbrOfPaths = candidates.size();
    if (nbrOfPaths == 0) {
      return c;
    }
    for (int i = 0; i < nbrOfPaths; i++) {
     
      // skip files that end with _Type or 
      //   appear to be inner files: have names with a "$" char
      candidate = candidates.get(i);
      String lastPartOfPath = candidate.p.getFileName().toString();
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
  
  private final static Comparator<Candidate> pathComparator = new Comparator<Candidate>() {
    @Override
    public int compare(Candidate o1, Candidate o2) {
      return o1.p.toString().compareTo(o2.p.toString());
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
   * @param path the path to a .java or .class or .jar or .pear
   * @param pearClasspath - a string representing a path to the pear's classpath if there is one, or null
   */
  private void getCandidates_processFile(Path path, String pearClasspath) {
//    System.out.println("debug processing " + path);
    try {
//      URI pathUri = path.toUri();
      String pathString = path.toString();
      final boolean isPear = pathString.endsWith(".pear");
      final boolean isJar = pathString.endsWith(".jar");
            
      if (isJar || isPear) {  // path.endsWith does not mean this !!
        if (!path.getFileSystem().equals(FileSystems.getDefault())) {        
          // embedded Jar: extract to temp
          Path out = getTempOutputPath(path);
          Files.copy(path, out, StandardCopyOption.REPLACE_EXISTING);
//          embeddedJars.add(new PathAndPath(path, out));
          path = out;
        }
        
        Path start;
        final String localPearClasspath; 
        if (isPear) {
          if (pearClasspath != null) {
            throw new UIMARuntimeException("Nested PEAR files not supported");
          }
          
          pear_current = new PearOrJar(path);
          pears.add(pear_current);
          // add pear classpath info
          File pearInstallDir = Files.createTempDirectory(getTempDir(), "installedPear").toFile();
          PackageBrowser ip = PackageInstaller.installPackage(pearInstallDir, path.toFile(), false);
          localPearClasspath = ip.buildComponentClassPath();
          String[] children = pearInstallDir.list();
          if (children == null || children.length != 1) {
            Misc.internalError();
          }
          pearResolveStart = Paths.get(pearInstallDir.getAbsolutePath(), children[0]);
          
          start = pearInstallDir.toPath();
        } else {
          if (isJar) {
            PearOrJar jarInfo = new PearOrJar(path);
            jar_current_stack.push(jarInfo);
            jars.add(jarInfo);
          }
          
          localPearClasspath = pearClasspath;
          FileSystem jfs = FileSystems.newFileSystem(Paths.get(path.toUri()), null);
          start = jfs.getPath("/");
        }
        
        try (Stream<Path> stream = Files.walk(start)) {  // needed to release file handles
          stream.forEachOrdered(
            p -> getCandidates_processFile(p, localPearClasspath));
        }
        if (isJar) {
          jar_current_stack.pop();
        }
        if (isPear) {
          pear_current = null;
        }
      } else {
        // is not a .jar or .pear file.  add .java or .class files to initial candidate set
        //    will be filtered additionally later
//        System.out.println("debug path ends with java or class " + pathString.endsWith(isSource ? ".java" : ".class") + " " + pathString);
        if (pathString.endsWith(isSource ? ".java" : ".class")) {
          candidates.add(new Candidate(path, pearClasspath));
          if (!isSource && null != pear_current) {
            // inside a pear, which has been unzipped into pearInstallDir;
            path2InsidePearOrJarPath.put(path.toString(), pearResolveStart.relativize(path).toString());                
            pear_current.pathsToCandidateFiles.add(path.toString());           
          }
          
          if (!isSource && jar_current_stack.size() > 0) {
            // inside a jar, not contained in a pear                
            jar_current_stack.getFirst().pathsToCandidateFiles.add(path.toString());    
          }
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  /**
   * 
   * @param path used to get the last name from the path
   * @return a temporary file in the local temp directory that has some name parts from the path's file, and ends in .jar
   * @throws IOException -
   */
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
  
  private boolean has_Type(Candidate cand, int start) {
    if (start >= candidates.size()) {
      return false;
    }

    String sc = cand.p.toString();
    String sc_minus_suffix = sc.substring(0,  sc.length() - ( isSource ? ".java".length() : ".class".length())); 
    String sc_Type = sc_minus_suffix + ( isSource ? "_Type.java" : "_Type.class");
    // a string which sorts beyond the candidate + a suffix of "_"
    String s_end = sc_minus_suffix + (char) (((int)'_') + 1);
    for (Candidate c : candidates.subList(start,  candidates.size())) {
      String s = c.p.toString();
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
    } else {
      try {
        outputDirectory = Files.createTempDirectory("migrateJCasOutput").toString() + "/";
      } catch (IOException e) {
        e.printStackTrace();
        throw new RuntimeException(e);
      }
    }
    outDirConverted = outputDirectory + "converted/";
    outDirSkipped = outputDirectory + "not-converted/";
    outDirLog = outputDirectory + "logs/";

    
    if (clp.isInArgsList(MIGRATE_CLASSPATH)) {
      migrateClasspath = clp.getParamArgument(MIGRATE_CLASSPATH);
    } else {
      if (clp.isInArgsList(CLASS_FILE_ROOTS)) {
        System.err.println("ERROR: classes file roots is specified, but the\n"
                         + "       migrateClasspath parameter is missing;\n"
                         + "       it's required in order to decompile the classes.");
        return false;
      }
    }
    
    if (clp.isInArgsList(CLASSES)) {
      individualClasses = clp.getParamArgument(CLASSES);
    }
    return true;
  }
  
  private String decompile(byte[] b, String pearClasspath) {
    ClassLoader cl = getClassLoader(pearClasspath);
    UimaDecompiler ud = new UimaDecompiler(cl, null);
    return ud.decompile(b);
  }
  
  private String decompile(String classname) {
    ClassLoader cl = getClassLoader(null);
    UimaDecompiler ud = new UimaDecompiler(cl, null);
    return ud.decompileToString(classname);
  }
  
//  private String getFullyQualifiedClassNameWithSlashes(byte[] b) {
//    ClassLoader cl = getClassLoader();
//    UimaDecompiler ud = new UimaDecompiler(cl, null);
//    return ud.extractClassNameSlashes(b);    
//  }
  
  /**
   * The classloader to use in decompiling, if it is provided, is one that delegates first
   * to the parent.  This may need fixing for PEARs
   * @return classloader to use for migrate decompiling
   */
  private ClassLoader getClassLoader(String pearClasspath) {
    if (null == pearClasspath) {
      if (null == cachedMigrateClassLoader) {
        cachedMigrateClassLoader = (null == migrateClasspath)
                        ? this.getClass().getClassLoader()
                        : new URLClassLoader(Misc.classpath2urls(migrateClasspath));
      }
      return cachedMigrateClassLoader;
    } else {
      try {
        return new UIMAClassLoader((null == migrateClasspath) 
                                     ? pearClasspath
                                     : (pearClasspath + File.pathSeparator + migrateClasspath));
      } catch (MalformedURLException e) {
        throw new UIMARuntimeException(e);
      }
    }
  }
  
  private void addImport(String s) {
    cu.getImports().add(new ImportDeclaration(new Name(s), false, false));
  }
  
  private void removeImport(String s) {
    Iterator<ImportDeclaration> it = cu.getImports().iterator();
    while (it.hasNext()) { 
      ImportDeclaration impDcl = it.next();
      if (impDcl.getNameAsString().equals(s)) {
        it.remove();
        break;
      }
    } 
  }

  /******************
   * AST Utilities
   ******************/
  
  private Node replaceInParent(Node n, Expression v) {
    Node parent = n.getParentNode().get();
    if (parent instanceof EnclosedExpr) {
      ((EnclosedExpr)parent).setInner(v);
    } else if (parent instanceof MethodCallExpr) { // args in the arg list
      List<Expression> args = ((MethodCallExpr)parent).getArguments();
      args.set(args.indexOf(n), v);
      v.setParentNode(parent);
    } else if (parent instanceof ExpressionStmt) { 
      ((ExpressionStmt)parent).setExpression(v);
    } else if (parent instanceof CastExpr) { 
      ((CastExpr)parent).setExpression(v);
    } else if (parent instanceof ReturnStmt) { 
      ((ReturnStmt)parent).setExpression(v);
    } else if (parent instanceof AssignExpr) {
      ((AssignExpr)parent).setValue(v);
    } else if (parent instanceof VariableDeclarator) {
      ((VariableDeclarator)parent).setInitializer(v);      
    } else if (parent instanceof ObjectCreationExpr) {
      List<Expression> args = ((ObjectCreationExpr)parent).getArguments();
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
    p.setName(new SimpleName(name));
  }
  
  private int findConstructor(NodeList<BodyDeclaration<?>> classMembers) {
    int i = 0;
    for (BodyDeclaration<?> bd : classMembers) {
      if (bd instanceof ConstructorDeclaration) {
        return i;
      }
      i++;
    }
    return -1;
  }
  
  private boolean hasTypeFields(NodeList<BodyDeclaration<?>> members) {
    boolean hasType = false;
    boolean hasTypeId = false;
    for (BodyDeclaration<?> bd : members) {
      if (bd instanceof FieldDeclaration) {
        FieldDeclaration f = (FieldDeclaration)bd;
        EnumSet<Modifier> m = f.getModifiers();
        if (m.contains(Modifier.PUBLIC) &&
            m.contains(Modifier.STATIC) &&
            m.contains(Modifier.FINAL) 
//            &&
//            getTypeName(f.getType()).equals("int")
            ) {
          List<VariableDeclarator> vds = f.getVariables();
          for (VariableDeclarator vd : vds) {
            if (vd.getType().equals(intType)) {
              String n = vd.getNameAsString();
              if (n.equals("type")) hasType = true;
              if (n.equals("typeIndexID")) hasTypeId = true;
              if (hasTypeId && hasType) break;
            }
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
  private void setHasJCasConstructors(NodeList<BodyDeclaration<?>> members) {
    boolean has0ArgConstructor = false;
    boolean has1ArgJCasConstructor = false;
    boolean has2ArgJCasConstructorV2 = false;
    boolean has2ArgJCasConstructorV3 = false;
    
    for (BodyDeclaration<?> bd : members) {
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
//    if (t instanceof ReferenceType) {
//      t = ((ReferenceType<?>)t).getType();
//    }
    
    if (t instanceof PrimitiveType) {
      return ((PrimitiveType)t).toString(); 
    }
    if (t instanceof ClassOrInterfaceType) {
      return ((ClassOrInterfaceType)t).getNameAsString();
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
      return ((NameExpr)e).getNameAsString();
    }
    if (e instanceof FieldAccessExpr) {
      return ((FieldAccessExpr)e).getNameAsString();
    }
    return null;
  }
  
  /**
   * Called on Annotation Decl, Class/intfc decl, empty type decl, enum decl
   * Does nothing unless at top level of compilation unit
   * 
   * Otherwise, adds an entry to c2ps for the classname and package, plus full path
   * 
   * @param n type being declared
   */
  private void updateClassName(TypeDeclaration<?> n) {
    Optional<Node> pnode = n.getParentNode();
    Node node;
    if (pnode.isPresent() && 
        (node = pnode.get()) instanceof CompilationUnit) {
      CompilationUnit cu2 = (CompilationUnit) node;
      className = cu2.getType(0).getNameAsString();
      String packageAndClassName = 
          (className.contains(".")) 
            ? className 
            : packageName + '.' + className;
      packageAndClassNameSlash = packageAndClassName.replace('.', '/');
      
      TypeImpl ti = TypeSystemImpl.staticTsi.getType(Misc.javaClassName2UimaTypeName(packageAndClassName));
      if (null != ti) {
        // is a built-in type
        ClassnameAndPath p = new ClassnameAndPath(
            packageAndClassNameSlash, 
            candidate.p, 
            candidate.pearClasspath);
        skippedBuiltins.add(p);
        v3 = false;   // skip further processing of this class
        return;  
      }

      c2ps.add(new ClassnameAndPath(packageAndClassNameSlash, candidate.p, candidate.pearClasspath));
      path2classname.put(candidate.p.toString(), packageAndClassNameSlash);
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
      e = ((EnclosedExpr)e).getInner().get();
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
      NodeList<Statement> stmts = ((BlockStmt) stmt).getStatements();
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
    Optional<Expression> o_expr = rstmt.getExpression(); 
    Expression expr = o_expr.isPresent() ? o_expr.get() : null;
    rstmt.setExpression(new CastExpr(castType, expr));
  }
  
  /********************
   * Recording results
   ********************/

  private void recordBadConstructor(String msg) {
    reportMigrateFailed("Constructor is incorrect, " + msg);
  }
      
//  private void reportParseException() {
//    reportMigrateFailed("Unparsable Java");
//  }
  
  private void migrationFailed(String reason) {
    failedMigration.add(new PathAndReason(candidate.p, reason));
    v3 = false;    
  }
  
  private void reportMigrateFailed(String m) {
    System.out.format("Skipping this file due to error: %s, path: %s%n", m, candidate);
    migrationFailed(m);
  }
  
  private void reportV2Class() {
    v2JCasFiles.add(candidate.p);
    v2 = true;
  }
  
  private void reportV3Class() {
    v3JCasFiles.add(candidate.p);
    v3 = true;
  }
  
  private void reportNotJCasClass(String reason) {
    nonJCasFiles.add(new PathAndReason(candidate.p, reason));
    v3 = false;
  }
  
  private void reportNotJCasClassMissingTypeFields() {
    reportNotJCasClass("missing required type and/or typeIndexID static fields");
  }
  
  private void reportDeletedCheckModified(String m) {
    deletedCheckModified.add(new PathAndReason(candidate.p, m));
  }
  
  private void reportMismatchedFeatureName(String m) {
    manualInspection.add(new PathAndReason(candidate.p, "This getter/setter name doesn't match internal feature name: " + m));
  }
  
  private void reportUnrecognizedV2Code(String m) {
    migrationFailed("V2 code not recognized:\n" + m);
  }
  
  private void reportPathWorkaround(String orig, String modified) {
    pathWorkaround.add(new String1AndString2(orig, modified));
  }
  
  private void reportPearOrJarClassReplace(String pearOrJar, String classname, String kind) { // pears or jars
    if (kind.equals("pears")) {
      pearClassReplace.add(new String1AndString2(pearOrJar, classname));
    } else {
      jarClassReplace.add(new String1AndString2(pearOrJar, classname));
    }
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
        + "  [-classesRoots <One-or-more-directories-or-jars-or-pears-separated-by-Path-separator>]\n"
        + "  [-classes <one-or-more-fully-qualified-class-names-separated-by-Path-separator]\n"
        + "            example:  -classes mypkg.foo:pkg2.bar\n"
        + "  [-outputDirectory a-writable-directory-path (optional)\n"
        + "     if omitted, a temporary directory is used\n"
        + "  [-migrateClasspath a-class-path to use in decompiling, used if -classesRoots is specified\n"
        + "                     also used when compiling the migrated classes.\n"
        + "                     PEAR processing augments this with the PEAR's classpath information               "
        + "  [-skipTypeCheck if specified, skips validing a found item by looking for the corresponding _Type file"
        + "  NOTE: either -sourcesRoots or -classesRoots is required, but only one may be specified.\n"
        + "  NOTE: classesRoots are scanned for JCas classes, which are then decompiled, and the results processed like sourcesRoots\n"
        + "        The decompiling requires that the classes being scanned be on the migrateClasspath when this is invoked.\n"
       );
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
  private String  prepare(Candidate c) {
    candidate = c;
    packageName = null;
    className = null;
    packageAndClassNameSlash = null;
    cu = null;
    return getSource(c);
  }
  
  private String prepareIndividual(String classname) {
    candidate = new Candidate(Paths.get(classname)); // a pseudo path
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
    final String classname;
    final Path path;
    final String pearClasspath;
    
    ClassnameAndPath(String classname, Path path, String pearClasspath) {
      this.classname = classname;
      this.path = path;
      this.pearClasspath = pearClasspath;
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
