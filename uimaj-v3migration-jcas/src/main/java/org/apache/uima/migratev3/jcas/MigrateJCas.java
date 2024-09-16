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
import java.io.Writer;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.apache.uima.UIMARuntimeException;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.impl.TypeSystemImpl;
import org.apache.uima.internal.util.CommandLineParser;
import org.apache.uima.internal.util.Misc;
import org.apache.uima.internal.util.UIMAClassLoader;
import org.apache.uima.internal.util.function.Runnable_withException;
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
import com.github.javaparser.ast.comments.Comment;
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
 * <p>
 * A driver that scans given roots for source and/or class Java files that contain JCas classes
 * 
 * <ul>
 * <li>identifies which ones appear to be JCas classes (heuristic)
 * <ul>
 * <li>of these, identifies which ones appear to be v2
 * <ul>
 * <li>converts these to v3</li>
 * </ul>
 * </li>
 * </ul>
 * 
 * <li>also can receive a list of individual class names</li>
 * <li>also can do a single source file</li>
 * </ul>
 * 
 * <p>
 * Creates summary and detailed reports of its actions.
 * 
 * <p>
 * Files representing JCas classes to convert are discovered by walking file system directories from
 * various roots, specified as input. The tool operates in 1 of two exclusive "modes": migrating
 * from sources (e.g., .java files) and migrating using compiled classes.
 * 
 * <p>
 * Compiled classes are decompiled and then migrated. This decompilation step usually requires a
 * java classpath, which is supplied using the -migrateClasspath parameter. Exception: migrating
 * PEAR files, which contain their own specification for a classpath.
 * 
 * <p>
 * The same JCas class may be encountered multiple times while walking the directory tree from the
 * roots, with the same or different definition. All of these definitions are migrated.
 * 
 * <p>
 * Copies of the original and the converted files are put into the output file tree.
 * 
 * <p>
 * Directory structure, starting at -outputDirectory (which if not specified, is a new temp
 * directory). The "a0", "a1" represent non-identical alternative definitions for the same class.
 * 
 * <pre>
 *     converted/      
 *       v2/    these are the decompiled or "found" source files
 *         a0/x/y/z/javapath/.../Classname.java   root-id + fully qualified java class + package as slashified name
 *                             /Classname2.java etc.
 *         a1/x/y/z/javapath/.../Classname.java  if there are different root-ids
 *         ...
 *       v3/
 *         a0/x/y/z/javapath/.../Classname.java   fully qualified java class + package as slashified name
 *                             /Classname2.java etc.
 *         a1/x/y/z/javapath/.../Classname.java   if there are different root-ids
 *         ...
 *        
 *       v3-classes - the compiled form if from classes and a java compiler was available
 *                    The first directory is the id of the Jar or PEAR container.
 *                    The second directory is the alternative.
 *                  
 *         23/a0/fully/slashified/package/class-name.class  &lt;&lt; parallel structure as v3/       
 *         
 *       jars/ - copies of the original JARs with the converted JCas classes
 *               The first directory is the id of the Jar or PEAR container
 *         7/jar-file-name-last-part.jar
 *         12/jar-file-name-last-part.jar
 *         14/   etc.
 *       
 *       pears - copies of the original PEARs with the converted JCas classes, if there were no duplicates
 *         8/pear-file-name-last-art.pear
 *         9/   etc.
 *       
 *     not-converted/   (skipped)
 *     logs/
 *       jar-map.txt   list of index to paths
 *       pear-map.txt  list of index to paths
 *       processed.txt
 *       duplicates.txt
 *       builtinsNotExtended.txt
 *       failed.txt
 *       skippedBuiltins.txt
 *       nonJCasFiles.txt
 *       woraroundDir.txt
 *       deletedCheckModified.txt
 *       manualInspection.txt
 *       pearFileUpdates.txt
 *       jarFileUpdates.txt
 *       ...
 * </pre>
 * 
 * <p>
 * Operates in one of two modes:
 * 
 * <pre>
 *   Mode 1: Given classes-roots and/or individual class names, and a migrateClasspath, 
 *     scans the classes-routes looking for classes candidates
 *       - determines the class name,
 *       - decompiles that
 *       - migrates that decompiled source.
 *      
 *     if a Java compiler (JDK) is available,
 *       - compiles the results
 *       - does reassembly for Jars and PEARs, replacing the JCas classes.   
 *       
 *   Mode 2: Given sources-roots or a single source java file
 *     scans the sources-routes looking for candidates
 *       - migrates that decompiled source.
 * </pre>
 * 
 * <p>
 * Note: Each run clears the output directory before starting the migration.
 * 
 * <p>
 * Note: classpath may be specified using -migrateClassPath or as the class path used to run this
 * tool.
 */
public class MigrateJCas extends VoidVisitorAdapter<Object> {

  // @formatter:off
  /*
   * ***************************************************** Internals
   * 
   * Unique IDs of v2 and v3 artifacts: RootId + classname
   * 
   * RootIdContainers (Set<RootId>) hold all discovered rootIds, at each Jar/Pear nesting level
   * including outer level (no Jar/Pear). These are kept in a push-down stack
   * 
   * 
   * Processing roots collection: done for source or class - iterate, for all roots --
   * processCollection for candidates rooted at that root --- candidate is .java or .class, with
   * path, with pearClasspath string ---- migrate called on each candidate ----- check to see if
   * already done, and if so, skip. ------ means: same byte or source code associated with same fqcn
   * 
   * Root-ids: created for each unique pathpart in front of fully-qualified class name created for
   * each unique path to Jar or PEAR
   * 
   * Caching to speed up duplicate processing: - decompiling: if the byte[] is already done, use
   * other value (if augmented migrateClasspath is the same) - source-migrating: if the source
   * strings are the same.
   * 
   * Multiple sources for single class: classname2multiSources: TreeMap from fqcn to CommonConverted
   * (string or bytes) CommonConverted: supports multiple paths having identical string/bytes.
   * 
   *   Unique IDs of v2 and v3 artifacts:
   *     RootId + classname
   *     
   *   RootIdContainers (Set<RootId>) hold all discovered rootIds, at each Jar/Pear nesting level
   *     including outer level (no Jar/Pear).
   *     These are kept in a push-down stack
   *     
   *       
   *   Processing roots collection: done for source or class
   *     - iterate, for all roots
   *       -- processCollection for candidates rooted at that root 
   *         --- candidate is .java or .class, with path, with pearClasspath string
   *           ---- migrate called on each candidate
   *             ----- check to see if already done, and if so, skip.
   *               ------ means: same byte or source code associated with same fqcn  
   * 
   *   Root-ids: created for each unique pathpart in front of fully-qualified class name
   *             created for each unique path to Jar or PEAR
   *   
   *   Caching to speed up duplicate processing:
   *     - decompiling: if the byte[] is already done, use other value (if augmented migrateClasspath is the same)
   *     - source-migrating: if the source strings are the same.
   *       
   *   Multiple sources for single class:
   *     classname2multiSources: TreeMap from fqcn to CommonConverted (string or bytes)
   *     CommonConverted: supports multiple paths having identical string/bytes.
   *     
   *   Compiling: driven from c2ps array of fqcn, path
   *      - may have multiple entries for same fqcn, with different paths, 
   *        -- only if different values for the impl
   *      - set when visiting top-level compilation unit non-built-in type
   *     
   */
    // @formatter:on
  /** manange the indention of printing routines */
  private static final int[] indent = new int[1];

  private static StringBuilder si(StringBuilder sb) {
    return Misc.indent(sb, indent);
  }

  private static StringBuilder flush(StringBuilder sb) {
    System.out.print(sb);
    sb.setLength(0);
    return sb;
  }

  private static final Integer INTEGER0 = 0;

  private static int nextContainerId = 0;

  // @formatter:off
  /******************************************************************
   * Container - exists in tree structure, has super, sub containers
   *              -- subcontainers: has path to it
   *           - holds set of rootIds in that container
   *           - topmost one has null parent, and null pathToJarOrPear
   ******************************************************************/
  // @formatter:on
  private static class Container implements Comparable<Container> {
    final int id = nextContainerId++;
    final Container parent; // null if at top level
    // @formatter:off
    /**  root to scan from.  
     *     Pears: is the loc in temp space of installed pear
     *     Jars: is the file system mounted on the Jar
     *             -- for inner Jars, the Jar is copied out into temp space. */
    // @formatter:on
    Path root;
    final Path rootOrig; // for Jars and Pears, the original path ending in jar or pear
    final Set<Container> subContainers = new TreeSet<>(); // tree set for better ordering
    final List<Path> candidates = new ArrayList<>();
    final List<CommonConverted> convertedItems = new ArrayList<>();
    final List<V3CompiledPathAndContainerItemPath> v3CompiledPathAndContainerItemPath = new ArrayList<>();
    final boolean isPear;
    final boolean isJar;
    final boolean isSingleJavaSource;
    /** can't use Path as the type, because the equals for Path is object == */
    final Set<String> _Types = new HashSet<>(); // has the non_Type path only if the _Type is found
    boolean haveDifferentCapitalizedNamesCollidingOnWindows = false;

    String pearClasspath; // not final - set by subroutine after defaulting

    /**
     * Cache of already done compiled classes, to avoid redoing them Kept by container, because the
     * classpath could change the decompile
     */
    private Map<byte[], CommonConverted> origBytesToCommonConverted = new HashMap<>();

    Container(Container parent, Path root) {
      this.parent = parent;
      if (parent != null) {
        parent.subContainers.add(this);
        pearClasspath = parent.pearClasspath; // default, when expanding Jars.
      }
      rootOrig = root;
      String s = root.toString().toLowerCase();
      isJar = s.endsWith(".jar");
      isPear = s.endsWith(".pear");
      isSingleJavaSource = s.endsWith(".java");
      this.root = (isPear || isJar) ? installJarOrPear() : root;
      // // debug
      // if (!isPear && isJar) {
      // System.out.println("debug prepare jar: " + this);
      // }
    }

    /**
     * Called when a new container is created
     * 
     * @param container
     * @param path
     * @return install directory
     */
    private Path installJarOrPear() {
      try {
        Path theJarOrPear = rootOrig;
        if (!theJarOrPear.getFileSystem().equals(FileSystems.getDefault())) {
          // pear is embedded in another pear or jar, so copy the Jar (intact) to a temp spot so
          // it's no longer embedded
          theJarOrPear = getTempOutputPathForJarOrPear(theJarOrPear);
          Files.copy(rootOrig, theJarOrPear, StandardCopyOption.REPLACE_EXISTING);
        }

        if (isPear) {
          // extract the pear just to get the classpath
          File pearInstallDir = Files.createTempDirectory(getTempDir(), "installedPear").toFile();
          PackageBrowser ip = PackageInstaller.installPackage(pearInstallDir, rootOrig.toFile(),
                  false);
          String newClasspath = ip.buildComponentClassPath();
          String parentClasspath = parent.pearClasspath;
          pearClasspath = (null == parentClasspath || 0 == parentClasspath.length()) ? newClasspath
                  : newClasspath + File.pathSeparator + parentClasspath;
        }

        FileSystem pfs = FileSystems.newFileSystem(theJarOrPear, (ClassLoader) null);
        return pfs.getPath("/");
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
      StringBuilder sb = toString1();
      indent[0] += 2;
      try {
        si(sb); // new line + indent
        sb.append("subContainers=");
        Misc.addElementsToStringBuilder(indent, sb, Misc.setAsList(subContainers), -1,
                (sbx, i) -> sbx.append(i.id)).append(',');
        si(sb).append("paths migrated="); // new line + indent
        Misc.addElementsToStringBuilder(indent, sb, candidates, -1, StringBuilder::append)
                .append(',');
        // si(sb).append("v3CompilePath="); // new line + indent
        // Misc.addElementsToStringBuilder(indent, sb, v3CompiledPathAndContainerItemPath, 100,
        // StringBuilder::append);
      } finally {
        indent[0] -= 2;
        si(sb).append(']');
      }
      return sb.toString();
    }

    public StringBuilder toString1() {
      StringBuilder sb = new StringBuilder();
      si(sb); // initial nl and indentation
      sb.append(isJar ? "Jar " : isPear ? "PEAR " : "");
      sb.append("container [id=").append(id).append(", parent.id=")
              .append((null == parent) ? "null" : parent.id).append(", root or pathToJarOrPear=")
              .append(rootOrig).append(',');
      return sb;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
      return 31 * id;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      Container other = (Container) obj;
      if (id != other.id) {
        return false;
      }
      return true;
    }

    @Override
    public int compareTo(Container o) {
      return Integer.compare(id, o.id);
    }
  }

  /**
   * A path to a .java or .class file in some container, for the v2 version For Jars and Pears, the
   * path is relative to the zip "/" dir
   */
  private static class ContainerAndPath implements Comparable<ContainerAndPath> {
    final Path path;
    final Container container;

    ContainerAndPath(Path path, Container container) {
      this.path = path;
      this.container = container;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(ContainerAndPath o) {
      int r = path.compareTo(o.path);
      if (r != 0) {
        return r;
      }
      return Integer.compare(container.id, o.container.id);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("ContainerAndPath [path=").append(path).append(", container=").append(container.id)
              .append("]");
      return sb.toString();
    }
  }

  // @formatter:off
  /**
   * This class holds information used to replace compiled items in Jars and Pears.
   * 
   * a pair of the v3CompiledPath (which is the container nbr/a0/ + the package-class-name slash + ".class"
   *   and the Container origRoot up to the start of the package and class name
   * for the item being compiled.
   *   - Note: if a Jar has the same compiled class at multiple nesting levels, each one will have 
   *     an instance of this class 
   */
  // @formatter:on
  private static class V3CompiledPathAndContainerItemPath {
    final Path v3CompiledPath;
    final String pathInContainer;

    public V3CompiledPathAndContainerItemPath(Path v3CompiledPath, String pathInContainer) {
      this.v3CompiledPath = v3CompiledPath;
      this.pathInContainer = pathInContainer;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      si(sb).append("v3CompiledPathAndContainerPartPath [");
      indent[0] += 2;
      try {
        si(sb).append("v3CompiledPath=").append(v3CompiledPath);
        si(sb).append("pathInContainer=").append(pathInContainer);
      } finally {
        indent[0] -= 2;
        si(sb).append("]");
      }
      return sb.toString();
    }

  }

  private static final JavaCompiler javaCompiler = ToolProvider.getSystemJavaCompiler();

  /****************************************************************
   * Command line parameters
   ****************************************************************/
  private static final String SOURCE_FILE_ROOTS = "-sourcesRoots";

  private static final String CLASS_FILE_ROOTS = "-classesRoots";

  private static final String OUTPUT_DIRECTORY = "-outputDirectory";

  // private static final String SKIP_TYPE_CHECK = "-skipTypeCheck";

  private static final String MIGRATE_CLASSPATH = "-migrateClasspath";

  // private static final String CLASSES = "-classes"; // individual classes to migrate, get from
  // supplied classpath

  private static final Type intType = PrimitiveType.intType();

  private static final Type callSiteType = JavaParser.parseType("CallSite");

  private static final Type methodHandleType = JavaParser.parseType("MethodHandle");

  private static final Type stringType = JavaParser.parseType("String");

  private static final EnumSet<Modifier> public_static_final = EnumSet.of(Modifier.PUBLIC,
          Modifier.STATIC, Modifier.FINAL);
  private static final EnumSet<Modifier> private_static_final = EnumSet.of(Modifier.PRIVATE,
          Modifier.STATIC, Modifier.FINAL);

  private static final PrettyPrinterConfiguration printWithoutComments = new PrettyPrinterConfiguration();
  static {
    printWithoutComments.setPrintComments(false);
  }

  private static final PrettyPrinterConfiguration printCu = new PrettyPrinterConfiguration();
  static {
    printCu.setIndent("  ");
  }

  private static final String ERROR_DECOMPILING = "!!! ERROR:";

  private static boolean isSource = false;

  private static Path tempDir = null;

  /***************************************************************************************************/

  private String packageName; // with dots?
  private String className; // (omitting package)
  private String packageAndClassNameSlash;

  // next 3 set at start of migrate for item being migrated
  private CommonConverted current_cc;
  private Path current_path;
  private Container current_container;

  /** includes trailing / */
  private String outputDirectory;
  /** includes trailing / */
  private String outDirConverted;
  /** includes trailing / */
  private String outDirSkipped;
  /** includes trailing / */
  private String outDirLog;

  private Container[] sourcesRoots = null; // only one of these has 1 or more Container instances
  private Container[] classesRoots = null;

  private CompilationUnit cu;

  // save this value in the class instance to avoid recomputing it
  private ClassLoader cachedMigrateClassLoader = null;

  private String migrateClasspath = null;

  // private String individualClasses = null; // to decompile

  /**
   * CommonConverted next id, by fqcn key: fqcn_slashes value: next id
   */
  private Map<String, Integer> nextCcId = new HashMap<>();

  // @formatter:off
  /**
   * Common info about a particular source-code instance of a class
   *   Used to avoid duplicate work for the same JCas definition
   *   Used to track identical and non-identical duplicate defs
   * 
   *   When processing from sourcesRoots: 
   *     use map: origSourceToCommonConverted  key = source string
   *     if found, skip conversion, use previous converted result.
   *     
   *   When processing from classesRoots:
   *     use map: origBytesToCommonConverted  key = byte[], kept by container in container
   *     if found, use previous converted results
   */
  // @formatter:on
  private class CommonConverted {
    /**
     * starts at 0, incr for each new instance for a particular fqcn_slash can't be assigned until
     * fqcn known
     */
    int id = -1; // temp value
    final String v2Source; // remembered original source
    final byte[] v2ByteCode; // remembered original bytes

    /**
     * all paths + their containers having the same converted result Need container because might
     * change classpath for compiling - path is to v2 source or compiled class
     */
    final Set<ContainerAndPath> containersAndV2Paths = new HashSet<>();

    String v3Source; // if converted, the result
    /** converted/v3/id-of-cc/pkg/name/classname.java */
    Path v3SourcePath; // path to converted source or null

    String fqcn_slash; // full name of the class e.g. java/util/Foo. unknown for sources at first

    CommonConverted(String origSource, byte[] v2ByteCode, Path path, Container container,
            String fqcn_slash) {
      v2Source = origSource;
      this.v2ByteCode = v2ByteCode;
      containersAndV2Paths.add(new ContainerAndPath(path, container));
      this.fqcn_slash = fqcn_slash;
    }

    /**
     * 
     * @param container
     *          having this commonConverted instance
     * @return the path to .java or .class file. If the container is a Jar or PEAR, it is the path
     *         within that Jar or Pear FileSystem
     */
    Path getV2SourcePath(Container container) {
      for (ContainerAndPath cp : containersAndV2Paths) {
        if (cp.container == container) {
          return cp.path;
        }
      }
      throw new RuntimeException("internalError");
    }

    int getId() {
      if (id < 0) {
        Integer nextId = nextCcId.computeIfAbsent(fqcn_slash, s -> INTEGER0);
        nextCcId.put(fqcn_slash, nextId + 1);
        id = nextId;
      }
      return id;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
      return v2Source == null ? 0 : v2Source.hashCode();
    }

    /*
     * equal if the v2source is equal
     */
    @Override
    public boolean equals(Object obj) {
      return obj instanceof CommonConverted && v2Source != null
              && v2Source.equals(((CommonConverted) obj).v2Source);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      int maxLen = 10;
      si(sb).append("CommonConverted [v2Source=").append(Misc.elide(v2Source, 100));
      indent[0] += 2;
      try {
        si(sb).append("v2ByteCode=");
        sb.append(v2ByteCode != null
                ? Arrays.toString(Arrays.copyOf(v2ByteCode, Math.min(v2ByteCode.length, maxLen)))
                : "null").append(',');
        si(sb).append("containersAndPaths=")
                .append(containersAndV2Paths != null ? Misc.ppList(indent,
                        Misc.setAsList(containersAndV2Paths), -1, StringBuilder::append) : "null")
                .append(',');
        si(sb).append("v3SourcePath=").append(v3SourcePath).append(',');
        si(sb).append("fqcn_slash=").append(fqcn_slash).append("]").append('\n');
      } finally {
        indent[0] -= 2;
      }
      return sb.toString();
    }
  }

  // @formatter:off
  /** Cache of already converted source classes, to avoid redoing them; 
   *    - key is the actual source
   *    - value is CommonConverted 
   *  This cache is over all containers 
   */
  // @formatter:on
  private Map<String, CommonConverted> sourceToCommonConverted = new HashMap<>();

  /**
   * A map from fqcn_slash to a list of converted sources one per non-duplicated source
   */
  private Map<String, List<CommonConverted>> classname2multiSources = new TreeMap<>();

  /************************************
   * Reporting
   ************************************/
  // private final List<Path> v2JCasFiles = new ArrayList<>(); // unused
  // private final List<Path> v3JCasFiles = new ArrayList<>(); // unused

  private final List<PathContainerAndReason> nonJCasFiles = new ArrayList<>(); // path, reason
  private final List<PathContainerAndReason> failedMigration = new ArrayList<>(); // path, reason
  private final List<PathContainerAndReason> skippedBuiltins = new ArrayList<>(); // path,
                                                                                  // "built-in"
  private final List<PathContainerAndReason> deletedCheckModified = new ArrayList<>(); // path,
                                                                                       // deleted
                                                                                       // check
                                                                                       // string
  private final List<String1AndString2> pathWorkaround = new ArrayList<>(); // original, workaround
  private final List<String1AndString2> pearClassReplace = new ArrayList<>(); // pear, classname
  private final List<String1AndString2> jarClassReplace = new ArrayList<>(); // jar, classname

  private final List<PathContainerAndReason> manualInspection = new ArrayList<>(); // path, reason
  // private final List<PathAndPath> embeddedJars = new ArrayList<>(); // source, temp

  private boolean isV2JCas; // false at start of migrate, set to true if a v2 class candidate is
                            // discovered
  private boolean isConvert2v3; // true at start of migrate, set to false if conversion fails, left
                                // true if already a v3
  private boolean isBuiltinJCas; // false at start of migrate, set to true if a built-in class is
                                 // discovered

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

  private boolean error_decompiling = false;

  private boolean badClassName;

  private int itemCount;

  /**
   * set if getAndProcessCasndidatesInContainer encounters a class where it cannot do the compile
   */
  private boolean unableToCompile;

  private final StringBuilder psb = new StringBuilder();

  public MigrateJCas() {
  }

  public static void main(String[] args) {
    (new MigrateJCas()).run(args);
  }

  /***********************************
   * Main
   * 
   * @param args
   *          -
   ***********************************/
  void run(String[] args) {
    CommandLineParser clp = parseCommandArgs(args);

    System.out.format("Output top directory: %s%n", outputDirectory);

    // clear output dir
    FileUtils.deleteRecursive(new File(outputDirectory));

    isSource = sourcesRoots != null;
    boolean isOk;
    if (isSource) {
      isOk = processRootsCollection("source", sourcesRoots, clp);
    } else {
      if (javaCompiler == null) {
        System.out.println("The migration tool cannot compile the migrated files, \n"
                + "  because no Java compiler is available.\n"
                + "  To make one available, run this tool using a Java JDK, not JRE");
      }
      isOk = processRootsCollection("classes", classesRoots, clp);
    }

    // if (individualClasses != null) {
    // processCollection("individual classes: ", new Iterator<String>() {
    // Iterator<String> it = Arrays.asList(individualClasses.split(File.pathSeparator)).iterator();
    // public boolean hasNext() {return it.hasNext();}
    // public String next() {
    // return prepareIndividual(it.next());}
    // });
    // }

    if (error_decompiling) {
      isOk = false;
    }

    isOk = report() && isOk;

    System.out.println("Migration finished " + (isOk ? "with no unusual conditions."
            : "with 1 or more unusual conditions that need manual checking."));
  }

  /**
   * called for compiled input when a compiler is available and don't have name collision if the
   * container is a PEAR or a Jar Updates a copy of the Pear or Jar
   * 
   * @param container
   */
  private void postProcessPearOrJar(Container container) {
    Path outDir = Paths.get(outputDirectory, container.isJar ? "jars" : "pears",
            Integer.toString(container.id));
    withIOX(() -> Files.createDirectories(outDir));
    si(psb).append("Replacing .class files in copy of ").append(container.rootOrig);
    flush(psb);
    try {
      // copy the pear or jar so we don't change the original
      Path lastPartOfPath = container.rootOrig.getFileName();
      if (null == lastPartOfPath) {
        throw new RuntimeException("Internal Error");
      }
      Path pearOrJarCopy = Paths.get(outputDirectory, container.isJar ? "jars" : "pears",
              Integer.toString(container.id), lastPartOfPath.toString());

      Files.copy(container.rootOrig, pearOrJarCopy);

      // put up a file system on the pear or jar
      FileSystem pfs = FileSystems.newFileSystem(pearOrJarCopy, (ClassLoader) null);

      // replace the .class files in this PEAR or Jar with corresponding v3 ones
      indent[0] += 2;
      String[] previousSkip = { "" };
      container.v3CompiledPathAndContainerItemPath.forEach(c_p -> {
        if (Files.exists(c_p.v3CompiledPath)) {
          withIOX(() -> Files.copy(c_p.v3CompiledPath, pfs.getPath(c_p.pathInContainer),
                  StandardCopyOption.REPLACE_EXISTING));
          reportPearOrJarClassReplace(pearOrJarCopy.toString(), c_p.v3CompiledPath.toString(),
                  container);
        } else {
          String pstr = c_p.v3CompiledPath.toString();
          String pstr2 = pstr;
          if (previousSkip[0] != "") {
            int cmn = findFirstCharDifferent(previousSkip[0], pstr);
            pstr2 = cmn > 5 ? ("..." + pstr.substring(cmn)) : pstr;
          }
          previousSkip[0] = pstr;
          si(psb).append("Skipping replacing ").append(pstr2)
                  .append(" because it could not be found, perhaps due to compile errors.");
          flush(psb);
        }
      });
      indent[0] -= 2;
      // for (CommonConverted cc : container.convertedItems) {
      // Map<Container, Path> v3ccs = cc.v3CompiledResultPaths;
      // v3ccs.forEach((v3ccc, v3cc_path) ->
      // {
      // if (v3ccc == container) {
      // String path_in_v3_classes = cc.v3CompiledResultPaths.get(container).toString();
      //
      // withIOX(() -> Files.copy(v3cc_path, pfs.getPath(path_in_v3_classes)));
      // reportPearOrJarClassReplace(pearOrJarCopy.toString(), path_in_v3_classes, container);
      // }
      // });
      // }

      pfs.close();

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  // @formatter:off
  /**
   * Compile all the migrated JCas classes in this container, adjusting the classpath if the
   * container is a Jar or Pear to include the Jar or PEAR.
   * 
   * As a side effect, it saves in the container, a list of all the compiled things together with
   * the path in container part, for use by a subsequent step to update copies of the jars/pears.
   * 
   * The items in the container are broken into batches of multiple classes to be compiled together.
   *   - The grouping is to group by alternative number.  This insures that multiple 
   *     definitions of the same class are done separately (otherwise the compiler complains 
   *     about multiple definitions).
   *     
   * As a side effect. compiling update the container, adding all the compiled items
   * to v3CompiledPathAndContainerItemPath
   *    
   * @param container -
   * @return true if compiled 1 or more sources, false if nothing was compiled
   */
  // @formatter:on
  private boolean compileV3SourcesCommon2(Container container) {

    String classesBaseDir = outDirConverted + "v3-classes/" + container.id;

    // specify the classpath. For PEARs use a class loader that loads first.
    String classpath = getCompileClassPath(container);

    // // debug
    // String[] cpa = classpath.split(File.pathSeparator);
    // System.out.println("debug - compilation classpath");
    // int j = 0;
    // for (String s : cpa) System.out.println("debug classpath: " + (++j) + " " + s);

    // get a list of compilation unit path strings to the converted/v3/nnn/path
    /**
     * containerRoot is rootOrig or for Jars/Pears the Path to "/" in the zip file system
     */
    Path containerRoot = null;

    // @formatter:off
    /**
     * The Cu Path Strings for one container might have multiple instances of the class.
     * These might be for identical or different sources. 
     *   - This happens when a root has multiple paths to instances of the same class.
     *   - Multiple compiled-paths might be for the same classname
     *   
     * For non-identical sources, the commonContainer instance "id" is spliced into the 
     *   v3 migrated source path:  see getBaseOutputPath,  e.g. converted/2/a3/fqcn/slashed/name.java  
     *   
     * The compiler will complain if you feed it the same compilation unit classname twice, with
     *   different paths saying "duplicate class definition".  
     *   - Fix:  do compilation in batches, one for each different commonConverted id.   
     */
    // @formatter:on
    Map<Integer, ArrayList<String>> cu_path_strings_by_ccId = new TreeMap<>(); // tree map to have
                                                                               // nice order of keys

    indent[0] += 2;
    boolean isEmpty = true;
    for (CommonConverted cc : container.convertedItems) {
      if (cc.v3SourcePath == null) {
        continue; // skip items that failed migration
      }
      isEmpty = false;
      // relativePathInContainer = the whole path with the first part (up to the end of the
      // container root) stripped off
      /**
       * itemPath is the original path in the container to where the source or class file is For
       * Jars and PEARs, it is relative to the Jar or PEAR
       */
      Path itemPath = cc.getV2SourcePath(container);
      if (null == containerRoot) {
        // lazy setup on first call
        // for Pears, must use the == filesystem, otherwise get
        // ProviderMismatchException
        containerRoot = (container.isJar || container.isPear)
                ? itemPath.getFileSystem().getPath("/")
                : container.rootOrig;
      }
      /**
       * relativePathInContainer might be x/y/z/a/b/c/name.class (ends in .class because we only get
       * here when the input is class files)
       */
      String relativePathInContainer = containerRoot.relativize(itemPath).toString();

      container.v3CompiledPathAndContainerItemPath.add(new V3CompiledPathAndContainerItemPath(
              Paths.get(classesBaseDir, "a" + cc.id,
                      cc.fqcn_slash + ".class" /* relativePathInContainer */),
              relativePathInContainer));
      ArrayList<String> items = cu_path_strings_by_ccId.computeIfAbsent(cc.id,
              x -> new ArrayList<>());
      items.add(cc.v3SourcePath.toString());
    }

    if (isEmpty) {
      si(psb).append("Skipping compiling for container ").append(container.id).append(" ")
              .append(container.rootOrig);
      si(psb).append("  because non of the v2 classes were migrated (might have been built-ins)");
      flush(psb);
      return false;
    } else {
      si(psb).append("Compiling for container ").append(container.id).append(" ")
              .append(container.rootOrig);
      flush(psb);
    }

    // List<String> cu_path_strings = container.convertedItems.stream()
    // .filter(cc -> cc.v3SourcePath != null)
    // .peek(cc -> container.v3CompiledPathAndContainerItemPath.add(
    // new V3CompiledPathAndContainerItemPath(
    // Paths.get(classesBaseDir, cc.v3SourcePath.toString()),
    // getPathInContainer(container, cc).toString())))
    // .map(cc -> cc.v3SourcePath.toString())
    // .collect(Collectors.toList());

    boolean resultOk = true;

    for (int ccId = 0;; ccId++) { // do each version of classes separately
      List<String> cups = cu_path_strings_by_ccId.get(ccId);
      if (cups == null) {
        break;
      }

      StandardJavaFileManager fileManager = javaCompiler.getStandardFileManager(null, null,
              Charset.forName("UTF-8"));

      Iterable<? extends JavaFileObject> compilationUnits = fileManager
              .getJavaFileObjectsFromStrings(cups);

      // //debug
      // System.out.println("Debug: list of compilation unit strings for iteration " + i);
      // int[] k = new int[] {0};
      // cups.forEach(s -> System.out.println(Integer.toString(++(k[0])) + " " + s));
      // System.out.println("debug end");

      String classesBaseDirN = classesBaseDir + "/a" + ccId;
      withIOX(() -> Files.createDirectories(Paths.get(classesBaseDirN)));
      Iterable<String> options = Arrays.asList("-d", classesBaseDirN, "-classpath", classpath);

      si(psb).append("Compiling for commonConverted version ").append(ccId).append(", ")
              .append(cups.size()).append(" classes");
      flush(psb);
      DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

      /*********** Compile ***********/
      resultOk = javaCompiler
              .getTask(null, fileManager, diagnostics, options, null, compilationUnits).call()
              && resultOk;
      /********************************/

      indent[0] += 2;
      for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
        JavaFileObject s = diagnostic.getSource();
        si(psb).append(diagnostic.getKind());
        int lineno = (int) diagnostic.getLineNumber();
        if (lineno != Diagnostic.NOPOS) {
          psb.append(" on line ").append(diagnostic.getLineNumber());
        }
        int pos = (int) diagnostic.getPosition();
        if (pos != Diagnostic.NOPOS) {
          psb.append(", position: ").append(diagnostic.getColumnNumber());
        }
        if (s != null) {
          psb.append(" in ").append(s.toUri());
        }
        si(psb).append("  ").append(diagnostic.getMessage(null));
        flush(psb);
      }
      withIOX(() -> fileManager.close());
      indent[0] -= 2;
      si(psb).append("Compilation finished")
              .append(resultOk ? " with no errors." : "with some errors.");
      flush(psb);
    }
    indent[0] -= 2;
    unableToCompile = !resultOk;
    return true;
  }

  // @formatter:off
  /**
   * The classpath used to compile is (in precedence order)
   *   - the classpath for this migration app  (first in order to pick up v3 support, overriding others)
   *   - any Pears, going up the parent chain, closest ones first
   *   - any Jars, going up the parent chain, closest ones last
   *   - passed in migrate classpath
   * @return the classpath to use in compiling the jcasgen'd sources
   */
  // @formatter:on
  private String getCompileClassPath(Container container) {

    // start with this (the v3migration tool) app's classpath to a cp string
    URLClassLoader systemClassLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
    URL[] urls = systemClassLoader.getURLs();
    StringBuilder cp = new StringBuilder();

    boolean firstTime = true;
    for (URL url : urls) {
      if (!firstTime) {
        cp.append(File.pathSeparatorChar);
      } else {
        firstTime = false;
      }
      cp.append(url.getPath());
    }

    // pears up the classpath, closest first
    Container c = container;
    while (c != null) {
      if (c.isPear) {
        cp.append(File.pathSeparator).append(c.pearClasspath);
      }
      c = c.parent;
    }

    // add the migrateClasspath, expanded

    if (null != migrateClasspath) {
      cp.append(File.pathSeparator).append(Misc.expandClasspath(migrateClasspath));
    }

    // add the Jars, closest last
    c = container;
    List<String> ss = new ArrayList<>();
    while (c != null) {
      if (c.isJar) {
        ss.add(c.root.toString());
      }
      c = c.parent;
    }
    Collections.reverse(ss);
    ss.forEach(s -> cp.append(File.pathSeparator).append(s));

    // System.out.println("debug: compile classpath = " + cp.toString());
    return cp.toString();
  }

  /**
   * iterate to process collections from all roots Called once, to process either sources or classes
   * 
   * @return false if unable to compile, true otherwise
   */
  private boolean processRootsCollection(String kind, Container[] roots, CommandLineParser clp) {
    unableToCompile = false; // preinit

    psb.setLength(0);
    indent[0] = 0;
    itemCount = 1;

    for (Container rootContainer : roots) {
      showWorkStart(rootContainer);

      // adds candidates to root containers, and adds sub containers for Jars and Pears
      getAndProcessCandidatesInContainer(rootContainer);

      // for (Path path : rootContainer.candidates) {
      //
      // CommonConverted cc = getSource(path, rootContainer);
      // migrate(cc, rootContainer, path);
      //
      // if ((i % 50) == 0) System.out.format("%4d%n ", Integer.valueOf(i));
      // i++;
      // }
    }
    si(psb).append("Total number of candidates processed: ").append(itemCount - 1);
    flush(psb);
    indent[0] = 0;
    return !unableToCompile;
  }

  private void showWorkStart(Container rootContainer) {
    si(psb).append("Migrating " + rootContainer.rootOrig.toString());
    indent[0] += 2;
    si(psb).append("Each character is one class");
    si(psb).append("  . means normal class");
    si(psb).append("  b means built in");
    si(psb).append("  i means identical duplicate");
    si(psb).append("  d means non-identical definition for the same JCas class");
    si(psb).append("  nnn at the end of the line is the number of classes migrated\n");
    flush(psb);
  }

  /**
   * parse command line args
   * 
   * @param args
   *          -
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

  private Container[] getRoots(CommandLineParser clp, String kind) {
    String[] paths = clp.getParamArgument(kind).split("\\" + File.pathSeparator);
    Container[] cs = new Container[paths.length];
    int i = 0;
    for (String path : paths) {
      cs[i++] = new Container(null, Paths.get(path));
    }
    return cs;
  }

  /**
   * @param p
   *          the path to the compiled or non-compiled source
   * @param container
   *          the container
   * @return the instance of the CommonConverted object, and update the container's convertedItems
   *         list if needed to include it
   */
  private CommonConverted getSource(Path p, Container container) {
    try {
      byte[] localV2ByteCode = null;

      CommonConverted cc;
      String v2Source;

      if (!isSource) {
        localV2ByteCode = Files.readAllBytes(p);

        // only use prev decompiled if same container
        cc = container.origBytesToCommonConverted.get(localV2ByteCode);
        if (null != cc) {
          return cc;
        }
        // decompile side effect: sets fqcn
        try {
          v2Source = decompile(localV2ByteCode, container.pearClasspath);
        } catch (RuntimeException e) {
          badClassName = true;
          e.printStackTrace();
          v2Source = null;
        }
        if (badClassName) {
          System.err.println("Candidate with bad Class Name is: " + p.toString());
          return null;
        }
        final byte[] finalbc = localV2ByteCode;
        cc = sourceToCommonConverted.computeIfAbsent(v2Source,
                src -> new CommonConverted(src, finalbc, p, container, packageAndClassNameSlash));
        // cc = new CommonConverted(v2Source, localV2ByteCode, p, container,
        // packageAndClassNameSlash);
        container.origBytesToCommonConverted.put(localV2ByteCode, cc);
      } else {

        v2Source = FileUtils.reader2String(Files.newBufferedReader(p));
        cc = sourceToCommonConverted.get(v2Source);
        if (null == cc) {
          cc = new CommonConverted(v2Source, null, p, container, "unknown");
          sourceToCommonConverted.put(v2Source, cc);
        } else {
          // add this new path + container to set of pathsAndContainers kept by this CommonConverted
          // object
          cc.containersAndV2Paths.add(new ContainerAndPath(p, container));
        }
      }

      // Containers have list of CommonConverted, which, in turn
      // have Set of ContainerAndPath elements.
      // (the same JCas class might appear in two different paths in a container)
      if (!container.convertedItems.contains(cc)) {
        container.convertedItems.add(cc);
      }

      return cc;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  // @formatter:off
  /**
   * Migrate one JCas definition, writes to Sysem.out 1 char to indicate progress.
   * 
   * The source is either direct, or a decompiled version of a .class file (missing comments, etc.).
   * 
   * This method only called if heuristics indicate this is a V2 JCas class definition.
   * 
   * Skips the migration if already done.
   * Skips if decompiling, and it failed.
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
  // @formatter:on
  private void migrate(CommonConverted cc, Container container, Path path) {

    if (null == cc) {
      System.err.println("Skipping this component due to decompile failure: " + path.toString());
      System.err.println("  in container: " + container);
      isConvert2v3 = false;
      error_decompiling = true;
      return;
    }

    if (cc.v3Source != null) {
      // next updates classname2multiSources for tracking non-identical defs
      boolean identical = collectInfoForReports(cc);
      assert identical;
      psb.append("i");
      flush(psb);
      cc.containersAndV2Paths.add(new ContainerAndPath(path, container));
      return;
    }

    assert cc.v2Source != null;

    packageName = null;
    className = null;
    packageAndClassNameSlash = null;
    cu = null;

    String source = cc.v2Source;
    isConvert2v3 = true; // preinit, set false if convert fails
    isV2JCas = false; // preinit, set true by reportV2Class, called by visit to
                      // ClassOrInterfaceDeclaration,
                      // when it has v2 constructors, and the right type and type_index_id field
                      // declares
    isBuiltinJCas = false;
    featNames.clear();
    fi_fields.clear();

    try { // to reset the next 3 items
      current_cc = cc;
      current_container = container;
      current_path = path;

      // System.out.println("Migrating source before migration:\n");
      // System.out.println(source);
      // System.out.println("\n\n\n");
      if (source.startsWith(ERROR_DECOMPILING)) {
        System.err.println("Decompiling failed for class: " + cc.toString() + "\n got: "
                + Misc.elide(source, 300, false));
        System.err.println("Please check the migrateClasspath");
        if (null == migrateClasspath) {
          System.err.println("classpath of this app is");
          System.err.println(System.getProperty("java.class.path"));
        } else {
          System.err.println(" first part of migrateClasspath argument was: "
                  + Misc.elide(migrateClasspath, 300, false));
          System.err.println("  Value used was:");
          URL[] urls = Misc.classpath2urls(migrateClasspath);
          for (URL url : urls) {
            System.err.println("    " + url.toString());
          }
        }
        System.err.println("Skipping this component");
        isConvert2v3 = false;
        error_decompiling = true;
        return;
      }

      StringReader sr = new StringReader(source);
      try {
        cu = JavaParser.parse(sr);

        addImport("java.lang.invoke.CallSite");
        addImport("java.lang.invoke.MethodHandle");
        addImport("org.apache.uima.cas.impl.CASImpl");
        addImport("org.apache.uima.cas.impl.TypeImpl");
        addImport("org.apache.uima.cas.impl.TypeSystemImpl");

        this.visit(cu, null); // side effect: sets the className, packageAndClassNameSlash,
                              // packageName

        new removeEmptyStmts().visit(cu, null);
        if (isConvert2v3) {
          removeImport("org.apache.uima.jcas.cas.TOP_Type");
        }

        if (isConvert2v3 && fi_fields.size() > 0) {
          NodeList<BodyDeclaration<?>> classMembers = cu.getTypes().get(0).getMembers();
          int positionOfFirstConstructor = findConstructor(classMembers);
          if (positionOfFirstConstructor < 0) {
            throw new RuntimeException();
          }
          classMembers.addAll(positionOfFirstConstructor, fi_fields);
        }

        ImportDeclaration firstImport = cu.getImports().get(0);
        String transformedMessage = String.format(
                " Migrated by uimaj-v3-migration-jcas, %s%n" + " Container: %s%n"
                        + " Path in container: %s%n",
                new Date(), container.toString1(), path.toString()).replace('\\', '/');

        Optional<Comment> existingComment = firstImport.getComment();
        if (existingComment.isPresent()) {
          Comment comment = existingComment.get();
          comment.setContent(comment.getContent() + "\n" + transformedMessage);
        } else {
          firstImport.setBlockComment(transformedMessage);
        }

        if (isSource) {
          sourceToCommonConverted.put(source, cc);
        }

        boolean identicalFound = collectInfoForReports(cc);
        assert !identicalFound;

        if (isV2JCas) {
          writeV2Orig(cc, isConvert2v3);
        }
        if (isConvert2v3) {
          cc.v3Source = new PrettyPrinter(printCu).print(cu);
          writeV3(cc);
        }

        psb.append(isBuiltinJCas ? "b"
                : (classname2multiSources.get(cc.fqcn_slash).size() == 1) ? "." : "d"); // means
                                                                                        // non-identical
                                                                                        // duplicate
        flush(psb);
      } catch (IOException e) {
        e.printStackTrace();
        throw new UIMARuntimeException(e);
      } catch (Exception e) {
        System.out.println("debug: exception caught, source was\n" + source);
        throw new UIMARuntimeException(e);
      }
    } finally {
      current_cc = null;
      current_container = null;
      current_path = null;
    }
  }

  /**
   * Called when have already converted this exact source or when we just finished converting this
   * source. Add this instance to the tracking information for multiple versions (identical or not)
   * of a class
   * 
   * @return true if this is an identical duplicate of one already done
   */

  private boolean collectInfoForReports(CommonConverted cc) {
    String fqcn_slash = cc.fqcn_slash;

    // track, by fqcn, all duplicates (identical or not)

    // // for a given fully qualified class name (slashified),
    // // find the list of CommonConverteds - one per each different version
    // // create it if null
    List<CommonConverted> commonConverteds = classname2multiSources.computeIfAbsent(fqcn_slash,
            k -> new ArrayList<>());

    // search to see if this instance already in the set
    // if so, add the path to the set of identicals
    // For class sources case, we compare the decompiled version
    boolean found = commonConverteds.contains(cc);
    if (!found) {
      commonConverteds.add(cc);
    }

    return found;
  }

  /******************
   * Visitors
   ******************/
  /**
   * Capture the type name from all top-level types AnnotationDeclaration, Empty, and Enum
   */

  @Override
  public void visit(AnnotationDeclaration n, Object ignore) {
    updateClassName(n);
    super.visit(n, ignore);
  }

  // @Override
  // public void visit(EmptyTypeDeclaration n, Object ignore) {
  // updateClassName(n);
  // super.visit(n, ignore);
  // }

  @Override
  public void visit(EnumDeclaration n, Object ignore) {
    updateClassName(n);
    super.visit(n, ignore);
  }

  // @formatter:off
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
  // @formatter:on
  @Override
  public void visit(ClassOrInterfaceDeclaration n, Object ignore) {
    // do checks to see if this is a JCas class; if not report skipped

    Optional<Node> maybeParent = n.getParentNode();
    if (maybeParent.isPresent()) {
      Node parent = maybeParent.get();
      if (parent instanceof CompilationUnit) {
        updateClassName(n);
        if (isBuiltinJCas) {
          // is a built-in class, skip it
          super.visit(n, ignore);
          return;
        }
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
          super.visit(n, ignore);
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

    super.visit(n, ignore);
    return;

  }

  @Override
  public void visit(PackageDeclaration n, Object ignored) {
    packageName = n.getNameAsString();
    super.visit(n, ignored);
  }

  /***************
   * Constructors - modify the 2 arg constructor - changing the args and the body
   * 
   * @param n
   *          - the constructor node
   * @param ignored
   *          -
   */
  @Override
  public void visit(ConstructorDeclaration n, Object ignored) {
    super.visit(n, ignored); // processes the params
    if (!isConvert2v3) { // for enums, annotations
      return;
    }
    List<Parameter> ps = n.getParameters();

    if (ps.size() == 2 && getParmTypeName(ps, 0).equals("int")
            && getParmTypeName(ps, 1).equals("TOP_Type")) {

      /**
       * public Foo(TypeImpl type, CASImpl casImpl) { super(type, casImpl); readObject();
       */
      setParameter(ps, 0, "TypeImpl", "type");
      setParameter(ps, 1, "CASImpl", "casImpl");

      // Body: change the 1st statement (must be super)
      NodeList<Statement> stmts = n.getBody().getStatements();
      if (!(stmts.get(0) instanceof ExplicitConstructorInvocationStmt)) {
        recordBadConstructor("missing super call");
        return;
      }
      NodeList<Expression> args = ((ExplicitConstructorInvocationStmt) (stmts.get(0)))
              .getArguments();
      args.set(0, new NameExpr("type"));
      args.set(1, new NameExpr("casImpl"));

      // leave the rest unchanged.
    }
  }

  private static final Pattern refGetter = Pattern
          .compile("(ll_getRef(Array)?Value)|" + "(ll_getFSForRef)");
  private static final Pattern word1 = Pattern.compile("\\A(\\w*)"); // word chars starting at
                                                                     // beginning \\A means
                                                                     // beginning

  // @formatter:off
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
  // @formatter:on
  @Override
  public void visit(MethodDeclaration n, Object ignore) {
    String name = n.getNameAsString();
    isGetter = isArraySetter = false;
    do { // to provide break exit

      if (name.length() >= 4 && ((isGetter = name.startsWith("get")) || name.startsWith("set"))
              && Character.isUpperCase(name.charAt(3)) && !name.equals("getTypeIndexID")) {
        List<Parameter> ps = n.getParameters();
        if (isGetter) {
          if (ps.size() > 1) {
            break;
          }
        } else { // is setter
          if (ps.size() > 2 || ps.size() == 0) {
            break;
          }
          if (ps.size() == 2) {
            if (!getParmTypeName(ps, 0).equals("int")) {
              break;
            }
            isArraySetter = true;
          }
        }

        // get the range-part-name and convert to v3 range ("Ref" changes to "Feature")
        String bodyString = n.getBody().get().toString(printWithoutComments);
        int i = bodyString.indexOf("jcasType.ll_cas.ll_");
        if (i < 0) {
          break;
        }
        String s = bodyString.substring(i + "jcasType.ll_cas.ll_get".length()); // also for
                                                                                // ...ll_set - same
                                                                                // length!
        if (s.startsWith("FSForRef(")) { // then it's the wrapper and the wrong instance.
          i = s.indexOf("jcasType.ll_cas.ll_");
          if (i < 0) {
            reportUnrecognizedV2Code(
                    "Found \"jcasType.ll_cas.ll_[set or get]...FSForRef(\" but didn't find following \"jcasType.ll_cas_ll_\"\n"
                            + n.toString());
            break;
          }
          s = s.substring(i + "jcasType.ll_cas.ll_get".length());
        }
        i = s.indexOf("Value");
        if (i < 0) {
          reportUnrecognizedV2Code(
                  "Found \"jcasType.ll_cas.ll_[set or get]\" but didn't find following \"Value\"\n"
                          + n.toString());
          break; // give up
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
        Matcher m = word1.matcher(bodyString.substring(i + "jcasType).casFeatCode_".length()));
        if (!m.find()) {
          reportUnrecognizedV2Code(
                  "Found \"...jcasType).casFeatCode_\" but didn't find subsequent word\n"
                          + n.toString());
          break;
        }
        featName = m.group(1);
        String fromMethod = Character.toLowerCase(name.charAt(3)) + name.substring(4);
        if (!featName.equals(fromMethod)) {
          // don't report if the only difference is the first letter captialization
          if (!(Character.toLowerCase(featName.charAt(0)) + featName.substring(1))
                  .equals(fromMethod)) {
            reportMismatchedFeatureName(String.format("%-25s %s", featName, name));
          }
        }
        // add _FI_xxx = TypeSystemImpl.getAdjustedFeatureOffset("xxx");
        // replaced Sept 2017
        // NodeList<Expression> args = new NodeList<>();
        // args.add(new StringLiteralExpr(featName));
        // VariableDeclarator vd = new VariableDeclarator(
        // intType,
        // "_FI_" + featName,
        // new MethodCallExpr(new NameExpr("TypeSystemImpl"), new
        // SimpleName("getAdjustedFeatureOffset"), args));
        // if (featNames.add(featName)) { // returns true if it was added, false if already in the
        // set of featNames
        // fi_fields.add(new FieldDeclaration(public_static_final, vd));
        // }

        // add _FC_xxx = TypeSystemImpl.createCallSite(ccc.class, "xxx");
        // add _FH_xxx = _FC_xxx.dynamicInvoker();
        // add _FeatName_xxx = "xxx" // https://issues.apache.org/jira/browse/UIMA-5575

        if (featNames.add(featName)) { // returns true if it was added, false if already in the set
                                       // of featNames
          // _FC_xxx = TypeSystemImpl.createCallSite(ccc.class, "xxx");
          MethodCallExpr initCallSite = new MethodCallExpr(new NameExpr("TypeSystemImpl"),
                  "createCallSite");
          initCallSite.addArgument(new FieldAccessExpr(new NameExpr(className), "class"));
          initCallSite.addArgument(new StringLiteralExpr(featName));
          VariableDeclarator vd_FC = new VariableDeclarator(callSiteType, "_FC_" + featName,
                  initCallSite);
          fi_fields.add(new FieldDeclaration(private_static_final, vd_FC));

          // _FH_xxx = _FC_xxx.dynamicInvoker();
          MethodCallExpr initInvoker = new MethodCallExpr(new NameExpr(vd_FC.getName()),
                  "dynamicInvoker");
          VariableDeclarator vd_FH = new VariableDeclarator(methodHandleType, "_FH_" + featName,
                  initInvoker);
          fi_fields.add(new FieldDeclaration(private_static_final, vd_FH));

          // _FeatName_xxx = "xxx" // https://issues.apache.org/jira/browse/UIMA-5575
          VariableDeclarator vd_fn = new VariableDeclarator(stringType, "_FeatName_" + featName,
                  new StringLiteralExpr(featName));
          fi_fields.add(new FieldDeclaration(public_static_final, vd_fn));

        }

        /**
         * add missing cast stmt for return stmts where the value being returned: - doesn't have a
         * cast already - has the expression be a methodCallExpr with a name which looks like:
         * ll_getRefValue or ll_getRefArrayValue
         */
        if (isGetter && "Feature".equals(rangeNamePart)) {
          for (Statement stmt : n.getBody().get().getStatements()) {
            if (stmt instanceof ReturnStmt) {
              Expression e = getUnenclosedExpr(((ReturnStmt) stmt).getExpression().get());
              if ((e instanceof MethodCallExpr)) {
                String methodName = ((MethodCallExpr) e).getNameAsString();
                if (refGetter.matcher(methodName).matches()) { // ll_getRefValue or
                                                               // ll_getRefArrayValue
                  addCastExpr(stmt, n.getType());
                }
              }
            }
          }
        }

        get_set_method = n; // used as a flag during inner "visits" to signal
                            // we're inside a likely feature setter/getter

      } // end of test for getter or setter method
    } while (false); // do once, provide break exit

    super.visit(n, ignore);
    get_set_method = null; // after visiting, reset the get_set_method to null
  }

  /**
   * Visitor for if stmts - removes feature missing test
   */
  @Override
  public void visit(IfStmt n, Object ignore) {
    do {
      // if (get_set_method == null) break; // sometimes, these occur outside of recogn.
      // getters/setters

      Expression c = n.getCondition(), e;
      BinaryExpr be, be2;
      List<Statement> stmts;
      if ((c instanceof BinaryExpr) && ((be = (BinaryExpr) c).getLeft() instanceof FieldAccessExpr)
              && ((FieldAccessExpr) be.getLeft()).getNameAsString().equals("featOkTst")) {
        // remove the feature missing if statement

        // verify the remaining form
        if (!(be.getRight() instanceof BinaryExpr)
                || !((be2 = (BinaryExpr) be.getRight()).getRight() instanceof NullLiteralExpr)
                || !(be2.getLeft() instanceof FieldAccessExpr)

                || !((e = getExpressionFromStmt(n.getThenStmt())) instanceof MethodCallExpr)
                || !(((MethodCallExpr) e).getNameAsString()).equals("throwFeatMissing")) {
          reportDeletedCheckModified("The featOkTst was modified:\n" + n.toString() + '\n');
        }

        BlockStmt parent = (BlockStmt) n.getParentNode().get();
        stmts = parent.getStatements();
        stmts.set(stmts.indexOf(n), new EmptyStmt()); // dont remove
        // otherwise iterators fail
        // parent.getStmts().remove(n);
        return;
      }
    } while (false);

    super.visit(n, ignore);
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
      if (get_set_method == null) {
        break;
      }

      /** remove checkArraybounds statement **/
      if (n.getNameAsString().equals("checkArrayBounds")
              && ((p1 = n.getParentNode()).isPresent() && p1.get() instanceof ExpressionStmt)
              && ((p2 = p1.get().getParentNode()).isPresent() && p2.get() instanceof BlockStmt)
              && ((p3 = p2.get().getParentNode()).isPresent() && p3.get() == get_set_method)) {
        NodeList<Statement> stmts = ((BlockStmt) p2.get()).getStatements();
        stmts.set(stmts.indexOf(p1.get()), new EmptyStmt());
        return;
      }

      // convert simpleCore expression ll_get/setRangeValue
      boolean useGetter = isGetter || isArraySetter;
      if (n.getNameAsString()
              .startsWith("ll_" + (useGetter ? "get" : "set") + rangeNameV2Part + "Value")) {
        args = n.getArguments();
        if (args.size() != (useGetter ? 2 : 3)) {
          break;
        }
        String suffix = useGetter ? "Nc" : rangeNamePart.equals("Feature") ? "NcWj" : "Nfc";
        String methodName = "_" + (useGetter ? "get" : "set") + rangeNamePart + "Value" + suffix;
        args.remove(0); // remove the old addr arg
        // arg 0 converted when visiting args FieldAccessExpr
        n.setScope(null);
        n.setName(methodName);
      }

      // convert array sets/gets
      String z = "ll_" + (isGetter ? "get" : "set");
      String nname = n.getNameAsString();
      if (nname.startsWith(z) && nname.endsWith("ArrayValue")) {

        String s = nname.substring(z.length());
        s = s.substring(0, s.length() - "Value".length()); // s = "ShortArray", etc.
        if (s.equals("RefArray")) {
          s = "FSArray";
        }
        if (s.equals("IntArray")) {
          s = "IntegerArray";
        }
        EnclosedExpr ee = new EnclosedExpr(
                new CastExpr(new ClassOrInterfaceType(s), n.getArguments().get(0)));

        n.setScope(ee); // the getter for the array fs
        n.setName(isGetter ? "get" : "set");
        n.getArguments().remove(0);
      }

      /** remove ll_getFSForRef **/
      /** remove ll_getFSRef **/
      if (n.getNameAsString().equals("ll_getFSForRef")
              || n.getNameAsString().equals("ll_getFSRef")) {
        updatedNode = replaceInParent(n, n.getArguments().get(0));
      }

    } while (false);

    if (updatedNode != null) {
      updatedNode.accept(this, null);
    } else {
      super.visit(n, null);
    }
  }

  /**
   * visitor for field access expressions - convert ((...type_Type)jcasType).casFeatCode_XXXX to
   * _FI_xxx
   * 
   * @param n
   *          -
   * @param ignore
   *          -
   */
  @Override
  public void visit(FieldAccessExpr n, Object ignore) {
    Expression e;
    Optional<Expression> oe;
    String nname = n.getNameAsString();

    if (get_set_method != null) {
      if (nname.startsWith("casFeatCode_") && ((oe = n.getScope()).isPresent())
              && ((e = getUnenclosedExpr(oe.get())) instanceof CastExpr)
              && ("jcasType".equals(getName(((CastExpr) e).getExpression())))) {
        String featureName = nname.substring("casFeatCode_".length());
        // replaceInParent(n, new NameExpr("_FI_" + featureName)); // repl last in List<Expression>
        // (args)

        MethodCallExpr getint = new MethodCallExpr(null, "wrapGetIntCatchException");
        getint.addArgument(new NameExpr("_FH_" + featureName));
        replaceInParent(n, getint);

        return;
      } else if (nname.startsWith("casFeatCode_")) {
        reportMigrateFailed(
                "Found field casFeatCode_ ... without a previous cast expr using jcasType");
      }
    }
    super.visit(n, ignore);
  }

  private class removeEmptyStmts extends VoidVisitorAdapter<Object> {
    @Override
    public void visit(BlockStmt n, Object ignore) {
      n.getStatements().removeIf(statement -> statement instanceof EmptyStmt);
      super.visit(n, ignore);
    }

    // @Override
    // public void visit(MethodDeclaration n, Object ignore) {
    // if (n.getNameAsString().equals("getModifiablePrimitiveNodes")) {
    // System.out.println("debug");
    // }
    // super.visit(n, ignore);
    // if (n.getNameAsString().equals("getModifiablePrimitiveNodes")) {
    // System.out.println("debug");
    // }
    // }
  }

  // @formatter:off
  /**
   * converted files: 
   *    java name, path  (sorted by java name, v3 name only)
   * not-converted:
   *    java name, path  (sorted by java name)
   * duplicates:
   *    java name, path  (sorted by java name)
   * @return true if it's likely everything converted OK.
   */
  // @formatter:on
  private boolean report() {
    System.out.println("\n\nMigration Summary");
    System.out.format("Output top directory: %s%n", outputDirectory);
    System.out.format("Date/time: %tc%n", new Date());
    pprintRoots("Sources", sourcesRoots);
    pprintRoots("Classes", classesRoots);

    boolean isOk2 = true;
    try {
      // these reports, if non-empty items, imply something needs manual checking, so reset isOk2
      isOk2 = reportPaths("Workaround Directories", "workaroundDir.txt", pathWorkaround) && isOk2;
      isOk2 = reportPaths("Reports of converted files where a deleted check was customized",
              "deletedCheckModified.txt", deletedCheckModified) && isOk2;
      isOk2 = reportPaths("Reports of converted files needing manual inspection",
              "manualInspection.txt", manualInspection) && isOk2;
      isOk2 = reportPaths("Reports of files which failed migration", "failed.txt", failedMigration)
              && isOk2;
      isOk2 = reportPaths("Reports of non-JCas files", "NonJCasFiles.txt", nonJCasFiles) && isOk2;
      isOk2 = reportPaths(
              "Builtin JCas classes - skipped - need manual checking to see if they are modified",
              "skippedBuiltins.txt", skippedBuiltins) && isOk2;

      // these reports, if non-empty, do not imply OK issues
      reportPaths("Reports of updated Jars", "jarFileUpdates.txt", jarClassReplace);
      reportPaths("Reports of updated PEARs", "pearFileUpdates.txt", pearClassReplace);

      // computeDuplicates();
      // reportPaths("Report of duplicates - not identical", "nonIdenticalDuplicates.txt",
      // nonIdenticalDuplicates);
      // reportPaths("Report of duplicates - identical", "identicalDuplicates.txt",
      // identicalDuplicates);
      // isOk2 = reportDuplicates() && isOk2; // false if non-identical duplicates

      return isOk2;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void pprintRoots(String kind, Container[] roots) {
    if (roots != null && roots.length > 0) {
      try {
        try (BufferedWriter bw = Files.newBufferedWriter(makePath(outDirLog + "ItemsProcessed"),
                StandardOpenOption.CREATE)) {
          logPrintNl(kind + " Roots:", bw);
          indent[0] += 2;
          try {
            for (Container container : roots) {
              pprintContainer(container, bw);
            }
            logPrintNl("", bw);
          } finally {
            indent[0] -= 2;
          }
        }
      } catch (IOException e) {
        throw new UIMARuntimeException(e);
      }
    }
  }

  private void pprintContainer(Container container, BufferedWriter bw) throws IOException {
    logPrintNl(container.toString(), bw);
    if (container.subContainers.size() > 1) {
      logPrintNl("", bw);
      indent[0] += 2;
      for (Container subc : container.subContainers) {
        pprintContainer(subc, bw);
      }
    }

  }

  // private void computeDuplicates() {
  // List<ClassnameAndPath> toCheck = new ArrayList<>(c2ps);
  // toCheck.addAll(extendableBuiltins);
  // sortReport2(toCheck);
  // ClassnameAndPath prevP = new ClassnameAndPath(null, null);
  // List<ClassnameAndPath> sameList = new ArrayList<>();
  // boolean areAllEqual = true;
  //
  // for (ClassnameAndPath p : toCheck) {
  // if (!p.getFirst().equals(prevP.getFirst())) {
  //
  // addToIdenticals(sameList, areAllEqual);
  // sameList.clear();
  // areAllEqual = true;
  //
  // prevP = p;
  // continue;
  // }
  //
  // // have 2nd or subsequent same class
  // if (sameList.size() == 0) {
  // sameList.add(prevP);
  // }
  // sameList.add(p);
  // if (areAllEqual) {
  // if (isFilesMiscompare(p.path, prevP.path)) {
  // areAllEqual = false;
  // }
  // }
  // }
  //
  // addToIdenticals(sameList, areAllEqual);
  // }

  // /**
  // * Compare two java source or class files
  // * @param p1
  // * @param p2
  // * @return
  // */
  // private boolean isFilesMiscompare(Path p1, Path p2) {
  // String s1 = (p1);
  // String s2 = (p2);
  // return !s1.equals(s2);
  // }

  // private void addToIdenticals(List<ClassnameAndPath> sameList, boolean areAllEqual) {
  // if (sameList.size() > 0) {
  // if (areAllEqual) {
  // identicalDuplicates.addAll(sameList);
  // } else {
  // nonIdenticalDuplicates.addAll(sameList);
  // }
  // }
  // }

  /**
   * 
   * @param name
   * @return a path made from name, with directories created
   * @throws IOException
   */
  private Path makePath(String name) throws IOException {
    Path p = Paths.get(name);
    Path parent = p.getParent(); // all the parts of the path up to the final segment
    if (parent == null) {
      return p;
    }
    try {
      Files.createDirectories(parent);
    } catch (FileAlreadyExistsException e) { // parent already exists but is not a directory!
      // caused by running on Windows system which ignores "case"
      // there's a file at /x/y/ named "z", but the path wants to be /x/y/Z/
      // Workaround: change "z" to "z_c" c for capitalization issue
      current_container.haveDifferentCapitalizedNamesCollidingOnWindows = true;
      Path fn = parent.getFileName();
      if (fn == null) {
        throw new IllegalArgumentException();
      }
      String newDir = fn.toString() + "_c";
      Path parent2 = parent.getParent();

      Path p2 = parent2 == null ? Paths.get(newDir) : Paths.get(parent2.toString(), newDir);
      try {
        Files.createDirectories(p2);
      } catch (FileAlreadyExistsException e2) { // parent already exists but is not a directory!
        throw new RuntimeException(e2);
      }

      reportPathWorkaround(parent.toString(), p2.toString());
      Path lastPartOfPath = p.getFileName();
      if (null == lastPartOfPath) {
        throw new RuntimeException();
      }
      return Paths.get(p2.toString(), lastPartOfPath.toString());
    }
    return p;
  }

  private void logPrint(String msg, Writer bw) throws IOException {
    System.out.print(msg);
    bw.write(msg);
  }

  private void logPrintNl(String msg, Writer bw) throws IOException {
    logPrint(msg, bw);
    logPrint("\n", bw);
  }

  /**
   * prints "There were no xxx" if there are no items. prints a title, followed by a
   * ================== underneath it
   * 
   * prints a sorted report of two fields.
   * 
   * @param title
   *          title of report
   * @param fileName
   *          file name to save the report in (as well as print to sysout
   * @param items
   *          the set of items to report on-
   * @return true if items were empty
   * @throws IOException
   *           -
   */
  private <T, U> boolean reportPaths(String title, String fileName,
          List<? extends Report2<T, U>> items) throws IOException {
    if (items.size() == 0) {
      System.out.println("There were no " + title);
      return true;
    }
    System.out.println("\n" + title);
    for (int i = 0; i < title.length(); i++) {
      System.out.print('=');
    }
    System.out.println("");

    try (BufferedWriter bw = Files.newBufferedWriter(makePath(outDirLog + fileName),
            StandardOpenOption.CREATE)) {
      List<Report2<T, U>> sorted = new ArrayList<>(items);

      sortReport2(sorted);
      int max = 0;
      int nbrFirsts = 0;
      Object prevFirst = null;
      for (Report2<T, U> p : sorted) {
        max = Math.max(max, p.getFirstLength());
        Comparable<T> first = p.getFirst();
        if (first != prevFirst) {
          prevFirst = first;
          nbrFirsts++;
        }
      }

      /**
       * Two styles. Style 1: where nbrFirst &lt;= 25% nbr: first on separate line, seconds indented
       * Style 2: firsts and seconds on same line.
       */
      int i = 1;
      boolean style1 = nbrFirsts <= sorted.size() / 4;
      prevFirst = null;
      for (Report2<T, U> p : sorted) {
        if (style1) {

          if (prevFirst != p.getFirst()) {
            prevFirst = p.getFirst();
            logPrintNl(String.format("\n  For: %s", p.getFirst()), bw);
          }
          logPrintNl(String.format("    %5d   %s", i, p.getSecond()), bw);
        } else {
          logPrintNl(String.format("%5d %-" + max + "s %s", i, p.getFirst(), p.getSecond()), bw);
        }
        i++;
      }
      System.out.println("");
    } // end of try-with-resources
    return false;
  }

  private boolean isZipFs(Object o) {
    // Surprise! sometimes the o is not an instance of FileSystem but is the zipfs anyways
    return o.getClass().getName().contains("zipfs"); // java 8 and 9
  }

  /**
   * Sort the items on first, then second
   * 
   * @param items
   */
  private <T, U> void sortReport2(List<? extends Report2<T, U>> items) {
    items.sort((o1, o2) -> {
      int r = protectedCompare(o1.getFirst(), o2.getFirst());
      if (r == 0) {
        r = protectedCompare(o1.getSecond(), o2.getSecond());
      }
      return r;
    });
  }

  /**
   * protect against comparing zip fs with non-zip fs - these are not comparable to each other in
   * IBM Java 8
   * 
   * @return -
   */
  private <T> int protectedCompare(Comparable<T> comparable, Comparable<T> comparable2) {
    // debug
    try {
      if (isZipFs(comparable)) {
        if (isZipFs(comparable2)) {
          return comparable.compareTo((T) comparable2); // both zip
        } else {
          return 1;
        }
      } else {
        if (isZipFs(comparable2)) {
          return -1;
        } else {
          return comparable.compareTo((T) comparable2); // both not zip
        }
      }
    } catch (ClassCastException e) {
      // debug
      System.out.format("Internal error: c1: %b  c2: %b%n c1: %s%n c2: %s%n", isZipFs(comparable),
              isZipFs(comparable2), comparable.getClass().getName(),
              comparable2.getClass().getName());
      throw e;
    }
  }

  // @formatter:off
  /**
   * Called only for top level roots.  Sub containers recurse getCandidates_processFile2.
   * 
   * Walk the directory tree rooted at root
   *   - descend subdirectories
   *   - descend Jar file
   *     -- descend nested Jar files (!)
   *        by extracting these to a temp dir, and keeping a back reference to where they were extracted from.
   *   
   * output the paths representing the classes to migrate:  
   *      classes having a _Type partner
   *      excluding things other than .java or .classes, and excluding anything with "$" in the name
   *   - the path includes the "file system".
   * @param root
   * @throws IOException
   */
  // @formatter:on
  private void getAndProcessCandidatesInContainer(Container container) {

    // current_paths2RootIds = top_paths2RootIds; // don't do lower, that's called within Jars etc.
    if (container.isSingleJavaSource) {
      getCandidates_processFile2(container.root, container);
    } else {
      try (Stream<Path> stream = Files.walk(container.root, FileVisitOption.FOLLOW_LINKS)) { // needed
                                                                                             // to
                                                                                             // release
                                                                                             // file
                                                                                             // handles
        stream.forEachOrdered(
                // only puts into the RootIds possible Fqcn (ending in either .class or .java)
                p -> getCandidates_processFile2(p, container));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    // walk from root container, remove items not JCas candidates
    // prunes empty rootIds and subContainer nodes
    removeNonJCas(container);

    if (container.candidates.size() == 0 && container.subContainers.size() == 0) { // above call
                                                                                   // might remove
                                                                                   // all candidates
      Container parent = container.parent;
      if (parent != null) {
        // System.out.println("No Candidates found, removing container: " + container.toString() );
        // // debug
        // System.out.println("debug: " + container.rootOrig.toString());
        parent.subContainers.remove(container);
      }
      return;
    }

    si(psb).append("Migrating JCas files ");
    psb.append(container.isJar ? "in Jar: " : container.isPear ? "in Pear: " : "from root: ");
    psb.append(container.rootOrig);

    indent[0] += 2;
    si(psb);
    flush(psb);
    try {
      for (Path path : container.candidates) {

        CommonConverted cc = getSource(path, container);
        // migrate checks to see if already done, outputs a "." or some other char for the candidate
        migrate(cc, container, path);

        // defer any compilation to container level

        if ((itemCount % 50) == 0) {
          psb.append(" ").append(itemCount);
          si(psb);
          flush(psb);
        }
        itemCount++;
      }

      psb.append(" ").append(itemCount - 1);
      flush(psb);

      if (isSource) {
        return; // done
      }

      if (!isSource && !container.haveDifferentCapitalizedNamesCollidingOnWindows
              && javaCompiler != null) {
        boolean somethingCompiled = compileV3SourcesCommon2(container);
        if (container.isPear || container.isJar) {
          if (somethingCompiled) {
            postProcessPearOrJar(container);
          }
        }
        return;
      }

      unableToCompile = true;
      return; // unable to do post processing or compiling
    } finally {
      indent[0] -= 2;
    }
  }

  // removes nonJCas candidates
  private void removeNonJCas(Container container) {
    Iterator<Path> it = container.candidates.iterator();
    while (it.hasNext()) {
      String candidate = it.next().toString();
      // remove non JCas classes
      // //debug
      // System.out.println("debug, testing to remove: " + candidate);
      // if (candidate.indexOf("Corrected") >= 0) {
      // if (!container._Types.contains(candidate)) {
      // System.out.println("debug dumping _Types map keys to see why ... Corrected.class not
      // there");
      // System.out.println("debug key is=" + candidate);
      // System.out.println("keys are:");
      // int i = 0;
      // for (String k : container._Types) {
      // if (i == 4) {
      // i = 0;
      // System.out.println("");
      // }
      // System.out.print(k + ", ");
      // }
      // } else {
      // System.out.println("debug container._Types did contain " + candidate);
      // }
      // }
      if (!container.isSingleJavaSource && !container._Types.contains(candidate)) {
        it.remove();
      }
    }
  }

  // @formatter:off
  /**
   * Called from Stream walker starting at a root or starting at an imbedded Jar or Pear.
   * 
   * adds all the .java or .class files to the candidates, including _Type if not skipping the _Type check
   * 
   * Handling embedded jar files
   *   - single level Jar (at the top level of the default file system)
   *     -- handle using an overlayed file system
   *   - embedded Jars within Jars: 
   *     - not supported by Zip File System Provider (it only supports one level)
   *     - handle by extracting to a temp dir, and then using the Zip File System Provider
   * 
   * For PEARs, check for and disallow nested PEARs; install the PEAR, set the pear classpath for
   *   recursive processing with the Pear.
   *   
   * For Jar and PEAR files, use local variable + recursive call to update current_paths2RootIds map
   *   to new one for the Jar / Pear, and then process recursiveloy
   *    
   * @param path the path to a .java or .class or .jar or .pear that was walked to
   * @param pearClasspath - a string representing a path to the pear's classpath if there is one, or null
   * @param container the container for the 
   *                    - rootIds (which have the JCas candidates) and
   *                    - subContainers for imbedded Pears and Jars
   */
  // @formatter:on
  private void getCandidates_processFile2(Path path, Container container) {

    String pathString = path.toString();
    final boolean isPear = pathString.endsWith(".pear"); // path.endsWith does not mean this !!
    final boolean isJar = pathString.endsWith(".jar");

    if (isPear || isJar) {
      Container subc = new Container(container, path);
      getAndProcessCandidatesInContainer(subc);
      return;
    }

    if (pathString.endsWith(isSource ? ".java" : ".class")) {
      // Skip candidates except .java or .class
      addToCandidates(path, container);
    }
  }

  // @formatter:off
  /**
   * if _Type kind, add artifactId to set kept in current rootIdContainer 
   * If currently scanning within a PEAR, 
   *    record 2-way map from unzipped path to internal path inside pear
   *    Used when doing pear reassembly.
   *    
   * If currently scanning within a Jar or a PEAR,
   *    add unzipped path to list of all subparts for containing Jar or PEAR
   *    These paths are used as unique ids to things needing to be replaced in the Jar or PEAR,
   *    when doing re-assembly.  For compiled classes migration, only, since source migration
   *    doesn't do re-assembly.
   *               
   * @param path
   * @param pearClassPath
   */
  // @formatter:on
  private void addToCandidates(Path path, Container container) {
    String ps = path.toString();

    if (ps.endsWith(isSource ? "_Type.java" : "_Type.class")) {
      container._Types.add(isSource ? (ps.substring(0, ps.length() - 10) + ".java")
              : (ps.substring(0, ps.length() - 11) + ".class"));
      // if (container.isJar) {
      // System.out.println("debug add container._Types " + Paths.get(ps.substring(0, ps.length() -
      // 11)).toString() + ".class".toString() + " for Jar " +
      // container.rootOrig.getFileName().toString());
      // }

      return;
    }

    if (ps.contains("$")) {
      return; // don't add these kinds of things, they're not JCas classes
    }

    // debug
    // if (container.isJar) {
    // System.out.println("debug add candidate " + path.toString() + " for Jar " +
    // container.rootOrig.getFileName().toString());
    // }
    container.candidates.add(path);
  }

  /**
   * For Jars inside other Jars, we copy the Jar to a temp spot in the default file system Extracted
   * Jar is marked delete-on-exit
   * 
   * @param path
   *          embedded Jar to copy (only the last name is used, in constructing the temp dir)
   * @return a temporary file in the local temp directory that is a copy of the Jar
   * @throws IOException
   *           -
   */
  private static Path getTempOutputPathForJarOrPear(Path path) throws IOException {
    Path localTempDir = getTempDir();
    if (path == null) {
      throw new IllegalArgumentException();
    }
    Path fn = path.getFileName();
    if (fn == null) {
      throw new IllegalArgumentException();
    }
    Path tempPath = Files.createTempFile(localTempDir, fn.toString(), "");
    tempPath.toFile().deleteOnExit();
    return tempPath;
  }

  private static Path getTempDir() throws IOException {
    if (tempDir == null) {
      tempDir = Files.createTempDirectory("migrateJCas");
      tempDir.toFile().deleteOnExit();
    }
    return tempDir;
  }

  private static final CommandLineParser createCmdLineParser() {
    CommandLineParser parser = new CommandLineParser();
    parser.addParameter(SOURCE_FILE_ROOTS, true);
    parser.addParameter(CLASS_FILE_ROOTS, true);
    parser.addParameter(OUTPUT_DIRECTORY, true);
    // parser.addParameter(SKIP_TYPE_CHECK, false);
    parser.addParameter(MIGRATE_CLASSPATH, true);
    // parser.addParameter(CLASSES, true);
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

    if (!clp.isInArgsList(SOURCE_FILE_ROOTS) && !clp.isInArgsList(CLASS_FILE_ROOTS)) {
      System.err.println(
              "Neither sources file roots nor classes file roots parameters specified; please specify just one.");
      return false;
    }

    if (clp.isInArgsList(SOURCE_FILE_ROOTS) && clp.isInArgsList(CLASS_FILE_ROOTS)) {
      System.err.println(
              "both sources file roots and classes file roots parameters specified; please specify just one.");
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
        System.err.println("WARNING: classes file roots is specified, but the\n"
                + "       migrateClasspath parameter is missing\n");
      }
    }

    // if (clp.isInArgsList(CLASSES)) {
    // individualClasses = clp.getParamArgument(CLASSES);
    // }
    return true;
  }

  // called to decompile a string of bytes.
  // - first get the class name (fully qualified)
  // and skip decompiling if already decompiled this class
  // for this pearClasspath
  // - this handles multiple class definitions, insuring
  // only one decompile happens per pearClasspath (including null)
  /**
   * Caller does any caching to avoid this method.
   * 
   * @param b
   *          bytecode to decompile
   * @param pearClasspath
   *          to prepend to the classpath
   * @return
   */
  private String decompile(byte[] b, String pearClasspath) {
    badClassName = false;
    String classNameWithSlashes = Misc.classNameFromByteCode(b);
    packageAndClassNameSlash = classNameWithSlashes;
    ClassLoader cl = getClassLoader(pearClasspath);

    UimaDecompiler ud = new UimaDecompiler(cl, null);

    if (classNameWithSlashes == null || classNameWithSlashes.length() < 2) {
      System.err.println("Failed to extract class name from binary code, " + "name found was \""
              + ((classNameWithSlashes == null) ? "null" : classNameWithSlashes)
              + "\"\n  byte array was:");
      System.err.println(Misc.dumpByteArray(b, 2000));
      badClassName = true;
    }

    return ud.decompileToString(classNameWithSlashes, b);
  }

  /**
   * The classloader to use in decompiling, if it is provided, is one that delegates first to the
   * parent. This may need fixing for PEARs
   * 
   * @return classloader to use for migrate decompiling
   */
  private ClassLoader getClassLoader(String pearClasspath) {
    if (null == pearClasspath) {
      if (null == cachedMigrateClassLoader) {
        cachedMigrateClassLoader = (null == migrateClasspath) ? this.getClass().getClassLoader()
                : new UIMAClassLoader(Misc.classpath2urls(migrateClasspath));
      }
      return cachedMigrateClassLoader;
    } else {
      try {
        return new UIMAClassLoader((null == migrateClasspath) ? pearClasspath
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
    Optional<Node> maybeParent = n.getParentNode();
    if (maybeParent.isPresent()) {
      Node parent = n.getParentNode().get();
      if (parent instanceof EnclosedExpr) {
        ((EnclosedExpr) parent).setInner(v);
      } else if (parent instanceof MethodCallExpr) { // args in the arg list
        List<Expression> args = ((MethodCallExpr) parent).getArguments();
        args.set(args.indexOf(n), v);
        v.setParentNode(parent);
      } else if (parent instanceof ExpressionStmt) {
        ((ExpressionStmt) parent).setExpression(v);
      } else if (parent instanceof CastExpr) {
        ((CastExpr) parent).setExpression(v);
      } else if (parent instanceof ReturnStmt) {
        ((ReturnStmt) parent).setExpression(v);
      } else if (parent instanceof AssignExpr) {
        ((AssignExpr) parent).setValue(v);
      } else if (parent instanceof VariableDeclarator) {
        ((VariableDeclarator) parent).setInitializer(v);
      } else if (parent instanceof ObjectCreationExpr) {
        List<Expression> args = ((ObjectCreationExpr) parent).getArguments();
        int i = args.indexOf(n);
        if (i < 0) {
          throw new RuntimeException();
        }
        args.set(i, v);
      } else {
        System.out.println(parent.getClass().getName());
        throw new RuntimeException();
      }
      return v;
    }
    System.out.println(
            "internal error replacing in parent: no parent for node: " + n.getClass().getName());
    System.out.println("   node: " + n.toString());
    System.out.println("   expression replacing: " + v.toString());
    throw new RuntimeException();
  }

  /**
   * 
   * @param p
   *          the parameter to modify
   * @param t
   *          the name of class or interface
   * @param name
   *          the name of the variable
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
        FieldDeclaration f = (FieldDeclaration) bd;
        EnumSet<Modifier> m = f.getModifiers();
        if (m.contains(Modifier.PUBLIC) && m.contains(Modifier.STATIC) && m.contains(Modifier.FINAL)
        // &&
        // getTypeName(f.getType()).equals("int")
        ) {
          List<VariableDeclarator> vds = f.getVariables();
          for (VariableDeclarator vd : vds) {
            if (vd.getType().equals(intType)) {
              String n = vd.getNameAsString();
              if (n.equals("type")) {
                hasType = true;
              }
              if (n.equals("typeIndexID")) {
                hasTypeId = true;
              }
              if (hasTypeId && hasType) {
                return true;
              }
            }
          }
        }
      }
    } // end of for
    return false;
  }

  // @formatter:off
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
   * 
   * @param members
   */
  // @formatter:on
  private void setHasJCasConstructors(NodeList<BodyDeclaration<?>> members) {
    boolean has0ArgConstructor = false;
    boolean has1ArgJCasConstructor = false;
    boolean has2ArgJCasConstructorV2 = false;
    boolean has2ArgJCasConstructorV3 = false;

    for (BodyDeclaration<?> bd : members) {
      if (bd instanceof ConstructorDeclaration) {
        List<Parameter> ps = ((ConstructorDeclaration) bd).getParameters();
        if (ps.size() == 0) {
          has0ArgConstructor = true;
        }
        if (ps.size() == 1 && getParmTypeName(ps, 0).equals("JCas")) {
          has1ArgJCasConstructor = true;
        }
        if (ps.size() == 2) {
          if (getParmTypeName(ps, 0).equals("int") && getParmTypeName(ps, 1).equals("TOP_Type")) {
            has2ArgJCasConstructorV2 = true;
          } else if (getParmTypeName(ps, 0).equals("TypeImpl")
                  && getParmTypeName(ps, 1).equals("CASImpl")) {
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
    // if (t instanceof ReferenceType) {
    // t = ((ReferenceType<?>)t).getType();
    // }

    if (t instanceof PrimitiveType) {
      return ((PrimitiveType) t).toString();
    }
    if (t instanceof ClassOrInterfaceType) {
      return ((ClassOrInterfaceType) t).getNameAsString();
    }
    Misc.internalError();
    return null;
  }

  /**
   * Get the name of a field
   * 
   * @param e
   *          -
   * @return the field name or null
   */
  private String getName(Expression e) {
    e = getUnenclosedExpr(e);
    if (e instanceof NameExpr) {
      return ((NameExpr) e).getNameAsString();
    }
    if (e instanceof FieldAccessExpr) {
      return ((FieldAccessExpr) e).getNameAsString();
    }
    return null;
  }

  /**
   * Called on Annotation Decl, Class/intfc decl, empty type decl, enum decl Does nothing unless at
   * top level of compilation unit
   * 
   * Otherwise, adds an entry to c2ps for the classname and package, plus full path
   * 
   * @param n
   *          type being declared
   */
  private void updateClassName(TypeDeclaration<?> n) {
    Optional<Node> pnode = n.getParentNode();
    Node node;
    if (pnode.isPresent() && (node = pnode.get()) instanceof CompilationUnit) {
      CompilationUnit cu2 = (CompilationUnit) node;
      className = cu2.getType(0).getNameAsString();
      String packageAndClassName = (className.contains(".")) ? className
              : packageName + '.' + className;
      packageAndClassNameSlash = packageAndClassName.replace('.', '/');
      // assert current_cc.fqcn_slash == null; // for decompiling, already set
      assert (current_cc.fqcn_slash != null)
              ? current_cc.fqcn_slash.equals(packageAndClassNameSlash)
              : true;
      current_cc.fqcn_slash = packageAndClassNameSlash;

      TypeImpl ti = TypeSystemImpl.staticTsi
              .getType(Misc.javaClassName2UimaTypeName(packageAndClassName));
      if (null != ti) {
        // is a built-in type
        // ContainerAndPath p = new ContainerAndPath(
        // current_path,
        // current_container,packageAndClassNameSlash,
        // current_cc.,
        // current_cc.pearClasspath);
        skippedBuiltins
                .add(new PathContainerAndReason(current_path, current_container, "built-in"));
        isBuiltinJCas = true;
        isConvert2v3 = false;
        return;
      } else {
        VariableDeclarator vd_typename = new VariableDeclarator(stringType, "_TypeName",
                new StringLiteralExpr(packageAndClassName));
        fi_fields.add(new FieldDeclaration(public_static_final, vd_typename));
      }

      return;
    }
    return;
  }

  private Expression getExpressionFromStmt(Statement stmt) {
    stmt = getStmtFromStmt(stmt);
    if (stmt instanceof ExpressionStmt) {
      return getUnenclosedExpr(((ExpressionStmt) stmt).getExpression());
    }
    return null;
  }

  private Expression getUnenclosedExpr(Expression e) {
    while (e instanceof EnclosedExpr) {
      e = ((EnclosedExpr) e).getInner().get();
    }
    return e;
  }

  /**
   * Unwrap (possibly nested) 1 statement blocks
   * 
   * @param stmt
   *          -
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
    CastExpr ce = new CastExpr(castType, expr);
    rstmt.setExpression(ce); // removes the parent link from expr
    if (expr != null) {
      expr.setParentNode(ce); // restore it
    }
  }

  /********************
   * Recording results
   ********************/

  private void recordBadConstructor(String msg) {
    reportMigrateFailed("Constructor is incorrect, " + msg);
  }

  // private void reportParseException() {
  // reportMigrateFailed("Unparsable Java");
  // }

  private void migrationFailed(String reason) {
    failedMigration.add(new PathContainerAndReason(current_path, current_container, reason));
    isConvert2v3 = false;
  }

  private void reportMigrateFailed(String m) {
    System.out.format("Skipping this file due to error: %s, path: %s%n", m, current_path);
    migrationFailed(m);
  }

  private void reportV2Class() {
    // v2JCasFiles.add(current_path);
    isV2JCas = true;
  }

  private void reportV3Class() {
    // v3JCasFiles.add(current_path);
    isConvert2v3 = true;
  }

  private void reportNotJCasClass(String reason) {
    nonJCasFiles.add(new PathContainerAndReason(current_path, current_container, reason));
    isConvert2v3 = false;
  }

  private void reportNotJCasClassMissingTypeFields() {
    reportNotJCasClass("missing required type and/or typeIndexID static fields");
  }

  private void reportDeletedCheckModified(String m) {
    deletedCheckModified.add(new PathContainerAndReason(current_path, current_container, m));
  }

  private void reportMismatchedFeatureName(String m) {
    manualInspection.add(new PathContainerAndReason(current_path, current_container,
            "This getter/setter name doesn't match internal feature name: " + m));
  }

  private void reportUnrecognizedV2Code(String m) {
    migrationFailed("V2 code not recognized:\n" + m);
  }

  private void reportPathWorkaround(String orig, String modified) {
    pathWorkaround.add(new String1AndString2(orig, modified));
  }

  private void reportPearOrJarClassReplace(String pearOrJar, String classname, Container kind) { // pears
                                                                                                 // or
                                                                                                 // jars
    if (kind.isPear) {
      pearClassReplace.add(new String1AndString2(pearOrJar, classname));
    } else {
      jarClassReplace.add(new String1AndString2(pearOrJar, classname));
    }
  }

  /***********************************************/
  /**
   * Output directory for source and migrated files Consisting of converted/skipped, v2/v3, a+cc.id,
   * slashified classname
   * 
   * @param cc
   *          -
   * @param isV2
   *          -
   * @param wasConverted
   *          -
   * @return converted/skipped, v2/v3, a+cc.id, slashified classname
   */
  private String getBaseOutputPath(CommonConverted cc, boolean isV2, boolean wasConverted) {
    StringBuilder sb = new StringBuilder();
    sb.append(wasConverted ? outDirConverted : outDirSkipped);
    sb.append(isV2 ? "v2/" : "v3/");
    sb.append("a").append(cc.getId()).append('/');
    sb.append(cc.fqcn_slash).append(".java");
    return sb.toString();
  }

  private void writeV2Orig(CommonConverted cc, boolean wasConverted) throws IOException {
    String base = getBaseOutputPath(cc, true, wasConverted); // adds numeric suffix if dupls
    FileUtils.writeToFile(makePath(base), cc.v2Source);
  }

  private void writeV3(CommonConverted cc) throws IOException {
    String base = getBaseOutputPath(cc, false, true);
    cc.v3SourcePath = makePath(base);
    String data = fixImplementsBug(cc.v3Source);
    FileUtils.writeToFile(cc.v3SourcePath, data);
  }

  private void printUsage() {
    System.out.println("Usage: java org.apache.uima.migratev3.jcas.MigrateJCas \n"
            + "  [-sourcesRoots <One-or-more-directories-or-jars-separated-by-Path-separator, or a path to a single JCas source class>]\n"
            + "  [-classesRoots <One-or-more-directories-or-jars-or-pears-separated-by-Path-separator>]\n"
            + "  [-outputDirectory a-writable-directory-path (optional)\n"
            + "     if omitted, a temporary directory is used\n"
            + "     if not omitted, the directory contents WILL BE ERASED at the start.\n"
            + "  [-migrateClasspath a-class-path to use in decompiling, when -classesRoots is specified\n"
            + "                     also used when compiling the migrated classes.\n"
            + "  NOTE: either -sourcesRoots or -classesRoots is required, but only one may be specified.\n"
            + "  NOTE: classesRoots are scanned for JCas classes, which are then decompiled, and the results processed like sourcesRoots\n");
  }

  private static final Pattern implementsEmpty = Pattern.compile("implements  \\{");

  private String fixImplementsBug(String data) {
    return implementsEmpty.matcher(data).replaceAll("{");
  }

  /*********************************************************************
   * Reporting classes
   *********************************************************************/

  private abstract static class Report2<T, U> {
    public abstract Comparable<T> getFirst(); // Eclipse on linux complained if not public, was OK
                                              // on windows

    public abstract Comparable<U> getSecond();

    abstract int getFirstLength();
  }

  private static class PathContainerAndReason extends Report2<ContainerAndPath, String> {
    final ContainerAndPath cap;
    final String reason;

    PathContainerAndReason(ContainerAndPath cap, String reason) {
      this.cap = cap;
      this.reason = reason;
    }

    PathContainerAndReason(Path path, Container container, String reason) {
      this(new ContainerAndPath(path, container), reason);
    }

    @Override
    public Comparable<ContainerAndPath> getFirst() {
      return cap;
    }

    @Override
    public Comparable<String> getSecond() {
      return reason;
    }

    @Override
    int getFirstLength() {
      return cap.toString().length();
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
    public Comparable<String> getFirst() {
      return s1;
    }

    @Override
    public Comparable<String> getSecond() {
      return s2;
    }

    @Override
    int getFirstLength() {
      return s1.toString().length();
    }
  }

  private static void withIOX(Runnable_withException r) {
    try {
      r.run();
    } catch (Exception e) {
      throw new UIMARuntimeException(e);
    }
  }

  private int findFirstCharDifferent(String s1, String s2) {
    int s1l = s1.length();
    int s2l = s2.length();
    for (int i = 0;; i++) {
      if (i == s1l || i == s2l) {
        return i;
      }
      if (s1.charAt(i) != s2.charAt(i)) {
        return i;
      }
    }
  }
  // private String drop_Type(String s) {
  // return s.substring(0, isSource ? "_Type.java".length()
  // : "_Type.class".length()) +
  // (isSource ? ".java" : ".class");
  // }

  /// *****************
  // * Root-id
  // *****************/
  // private static int nextRootId = 1;
  //
  /// ***********************************************************************
  // * Root-id - this is the path part up to the start of the package name.
  // * - it is relative to container
  // * - has the collection of artifacts that might be candidates, having this rootId
  // * - has the collection of _Type things having this rootId
  // * - "null" path is OK - means package name starts immediately
  // * There is no Root-id for path ending in Jar or PEAR - these created containers instead
  // ***********************************************************************/
  // private static class RootId {
  // final int id = nextRootId++;
  // /**
  // * The path relative to the the container (if any) (= Jar or Pear)
  // * - for Pears, the path is as if it was not installed, but within the PEAR file
  // */
  // final Path path;
  //
  // /** The container holding this RootId */
  // final Container container;
  // /**
  // * For this rootId, all of the fully qualified classnames that are migration eligible.
  // * - not all might be migrated, if upon further inspection they are not JCas class files.
  // */
  // final Set<Fqcn> fqcns = new HashSet<>();
  // final Set<String> fqcns_ignore_case = new HashSet<>();
  // boolean haveDifferentCapitalizedNamesCollidingOnWindows = false;
  //
  // RootId(Path path, Container container) {
  // this.path = path;
  // this.container = container;
  // }
  //
  // /* (non-Javadoc)
  // * @see java.lang.Object#toString()
  // */
  // @Override
  // public String toString() {
  // return "RootId [id="
  // + id
  // + ", path="
  // + path
  // + ", container="
  // + container.id
  // + ", fqcns="
  // + Misc.ppList(Misc.setAsList(fqcns))
  // + ", fqcns_Type="
  // + Misc.ppList(Misc.setAsList(fqcns_Type))
  // + "]";
  // }
  //
  // void add(Fqcn fqcn) {
  // boolean wasNotPresent = fqcns.add(fqcn);
  // boolean lc = fqcns_ignore_case.add(fqcn.fqcn_dots.toLowerCase());
  // if (!lc && wasNotPresent) {
  // haveDifferentCapitalizedNamesCollidingOnWindows = true;
  // }
  // }
  //
  // boolean hasMatching_Type(Fqcn fqcn) {
  //
  // }
  // }
  /// **
  // * Called from Stream walker starting at a root or starting at an imbedded Jar or Pear.
  // *
  // * adds all the .java or .class files to the candidates, including _Type if not skipping the
  /// _Type check
  // * Handling embedded jar files
  // * - single level Jar (at the top level of the default file system)
  // * -- handle using an overlayed file system
  // * - embedded Jars within Jars:
  // * - not supported by Zip File System Provider (it only supports one level)
  // * - handle by extracting to a temp dir, and then using the Zip File System Provider
  // * @param path the path to a .java or .class or .jar or .pear
  // * @param pearClasspath - a string representing a path to the pear's classpath if there is one,
  /// or null
  // */
  // private void getCandidates_processFile(Path path, String pearClasspath) {
  //// if (path.toString().contains("commons-httpclient-3.1.jar"))
  //// System.out.println("Debug: " + path.toString());
  //// System.out.println("debug processing " + path);
  // try {
  //// URI pathUri = path.toUri();
  // String pathString = path.toString();
  // final boolean isPear = pathString.endsWith(".pear"); // path.endsWith does not mean this !!
  // final boolean isJar = pathString.endsWith(".jar");
  //
  // if (isJar || isPear) {
  // if (!path.getFileSystem().equals(FileSystems.getDefault())) {
  // // embedded Pear or Jar: extract to temp
  // Path out = getTempOutputPathForJar(path);
  // Files.copy(path, out, StandardCopyOption.REPLACE_EXISTING);
  //// embeddedJars.add(new PathAndPath(path, out));
  // path = out; // path points to pear or jar
  // }
  //
  // Path start;
  // final String localPearClasspath;
  // if (isPear) {
  // if (pearClasspath != null) {
  // throw new UIMARuntimeException("Nested PEAR files not supported");
  // }
  //
  //// pear_current = new PearOrJar(path);
  //// pears.add(pear_current);
  // // add pear classpath info
  // File pearInstallDir = Files.createTempDirectory(getTempDir(), "installedPear").toFile();
  // PackageBrowser ip = PackageInstaller.installPackage(pearInstallDir, path.toFile(), false);
  // localPearClasspath = ip.buildComponentClassPath();
  // String[] children = pearInstallDir.list();
  // if (children == null || children.length != 1) {
  // Misc.internalError();
  // }
  // pearResolveStart = Paths.get(pearInstallDir.getAbsolutePath(), children[0]);
  //
  // start = pearInstallDir.toPath();
  // } else {
  // if (isJar) {
  // PearOrJar jarInfo = new PearOrJar(path);
  // pear_or_jar_current_stack.push(jarInfo);
  // jars.add(jarInfo);
  // }
  //
  // localPearClasspath = pearClasspath;
  // FileSystem jfs = FileSystems.newFileSystem(Paths.get(path.toUri()), null);
  // start = jfs.getPath("/");
  // }
  //
  // try (Stream<Path> stream = Files.walk(start)) { // needed to release file handles
  // stream.forEachOrdered(
  // p -> getCandidates_processFile(p, localPearClasspath));
  // }
  // if (isJar) {
  // pear_or_jar_current_stack.pop();
  // }
  // if (isPear) {
  // pear_current = null;
  // }
  // } else {
  // // is not a .jar or .pear file. add .java or .class files to initial candidate set
  // // will be filtered additionally later
  //// System.out.println("debug path ends with java or class " + pathString.endsWith(isSource ?
  /// ".java" : ".class") + " " + pathString);
  // if (pathString.endsWith(isSource ? ".java" : ".class")) {
  // candidates.add(new Candidate(path, pearClasspath));
  // if (!isSource && null != pear_current) {
  // // inside a pear, which has been unzipped into pearInstallDir;
  // path2InsidePearOrJarPath.put(path.toString(), pearResolveStart.relativize(path).toString());
  // pear_current.pathsToCandidateFiles.add(path.toString());
  // }
  //
  // if (!isSource && pear_or_jar_current_stack.size() > 0) {
  // // inside a jar, not contained in a pear
  // pear_or_jar_current_stack.getFirst().pathsToCandidateFiles.add(path.toString());
  // }
  // }
  // }
  // } catch (IOException e) {
  // throw new RuntimeException(e);
  // }
  // }
  // private void postProcessPearsOrJars(String kind, List<PearOrJar> pearsOrJars,
  /// List<String1AndString2> classReplace) { // pears or jars
  // try {
  // Path outDir = Paths.get(outputDirectory, kind);
  // FileUtils.deleteRecursive(outDir.toFile());
  // Files.createDirectories(outDir);
  // } catch (IOException e) {
  // throw new RuntimeException(e);
  // }
  //
  //// pearsOrJars may have entries with 0 candidate paths. This happens when we scan them
  //// but find nothing to convert.
  //// eliminate these.
  //
  // Iterator<PearOrJar> it = pearsOrJars.iterator();
  // while (it.hasNext()) {
  // PearOrJar poj = it.next();
  // if (poj.pathsToCandidateFiles.size() == 0) {
  // it.remove();
  // } else {
  //// //debug
  //// if (poj.pathToPearOrJar.toString().contains("commons-httpclient-3.1")) {
  //// System.err.println("debug found converted things inside commons-httpclient");;
  //// for (String x : poj.pathsToCandidateFiles) {
  //// System.err.println(x);
  //// }
  //// System.err.println("");
  //// }
  // }
  // }
  //
  // it = pearsOrJars.iterator();
  // while (it.hasNext()) {
  // PearOrJar poj = it.next();
  // if (poj.pathsToCandidateFiles.size() == 0) {
  // System.err.print("debug failed to remove unconverted Jar");
  // }
  // }
  //
  // if (pearsOrJars.size() == 0) {
  // System.out.format("No .class files were replaced in %s.%n", kind);
  // } else {
  // System.out.format("replacing .class files in %,d %s%n", pearsOrJars.size(), kind);
  // for (PearOrJar p : pearsOrJars) {
  // pearOrJarPostProcessing(p, kind);
  // }
  // try {
  // reportPaths("Reports of updated " + kind, kind + "FileUpdates.txt", classReplace);
  //
  // } catch (IOException e) {
  // throw new RuntimeException(e);
  // }
  // }
  //
  // }
  /// **
  // * When running the compiler to compile v3 sources, we need a classpath that at a minimum
  // * includes uimaj-core. The strategy is to use the invoker of this tool's classpath as
  // * specified from the application class loader
  // * @return true if no errors
  // */
  // private boolean compileV3SourcesCommon(List<ClassnameAndPath> items, String msg, String
  /// pearClasspath) {
  //
  // if (items.size() == 0) {
  // return true;
  // }
  // System.out.format("Compiling %,d classes %s -- This may take a while!%n", c2ps.size(), msg);
  // StandardJavaFileManager fileManager = javaCompiler.getStandardFileManager(null, null,
  /// Charset.forName("UTF-8"));
  //
  // List<String> cus = items.stream()
  // .map(c -> outDirConverted + "v3/" + c.classname + ".java")
  // .collect(Collectors.toList());
  //
  // Iterable<String> compilationUnitStrings = cus;
  //
  // Iterable<? extends JavaFileObject> compilationUnits =
  // fileManager.getJavaFileObjectsFromStrings(compilationUnitStrings);
  //
  // // specify where the output classes go
  // String classesBaseDir = outDirConverted + "v3-classes";
  // try {
  // Files.createDirectories(Paths.get(classesBaseDir));
  // } catch (IOException e) {
  // throw new UIMARuntimeException(e);
  // }
  // // specify the classpath
  // String classpath = getCompileClassPath(pearClasspath);
  // Iterable<String> options = Arrays.asList("-d", classesBaseDir,
  // "-classpath", classpath);
  // return javaCompiler.getTask(null, fileManager, null, options, null, compilationUnits).call();
  // }
  /// **
  // * Called after class is migrated
  // * Given a path to a class (source or class file),
  // * return the URL to the class as found in the classpath.
  // * This returns the "first" one found in the classpath, in the case of duplicates.
  // * @param path
  // * @return the location of the class in the class path
  // */
  // private URL getPathForClass(Path path) {
  // return (null == packageAndClassNameSlash)
  // ? null
  // : migrateClassLoader.getResource(packageAndClassNameSlash + ".class");
  // }
  // private void getBaseOutputPath() {
  // String s = packageAndClassNameSlash;
  // int i = 0;
  // while (!usedPackageAndClassNames.add(s)) {
  // i = i + 1;
  // s = packageAndClassNameSlash + "_dupid_" + i;
  // }
  // packageAndClassNameSlash_i = i;
  // }
  // private String prepareIndividual(String classname) {
  // candidate = new Candidate(Paths.get(classname)); // a pseudo path
  // packageName = null;
  // className = null;
  // packageAndClassNameSlash = null;
  // cu = null;
  // return decompile(classname); // always look up in classpath
  // // to decompile individual source - put in sourcesRoots
  // }
  // if (!isSource) { // skip this recording if source
  // if (null != pear_current) {
  // // inside a pear, which has been unzipped into a temporary pearInstallDir;
  // // we don't want that temporary dir to be part of the path.
  // path2InsidePearOrJarPath.put(path.toString(), pearResolveStart.relativize(path).toString());
  // pear_current.pathsToCandidateFiles.add(path.toString());
  // }
  //
  // if (!isSource && pear_or_jar_current_stack.size() > 0) {
  // // inside a jar, not contained in a pear
  // pear_or_jar_current_stack.getFirst().pathsToCandidateFiles.add(path.toString());
  // }
  // }
  // }
  /// **
  // * For a given candidate, use its path:
  // * switch the ...java to ..._Type.java, or ...class to ..._Type.class
  // * look thru all the candidates
  // * @param cand
  // * @param start
  // * @return
  // */
  // private boolean has_Type(Candidate cand, int start) {
  // if (start >= candidates.size()) {
  // return false;
  // }
  //
  // String sc = cand.p.toString();
  // String sc_minus_suffix = sc.substring(0, sc.length() - ( isSource ? ".java".length() :
  /// ".class".length()));
  // String sc_Type = sc_minus_suffix + ( isSource ? "_Type.java" : "_Type.class");
  // // a string which sorts beyond the candidate + a suffix of "_"
  // String s_end = sc_minus_suffix + (char) (((int)'_') + 1);
  // for (Candidate c : candidates.subList(start, candidates.size())) {
  // String s = c.p.toString();
  // if (s_end.compareTo(s) < 0) {
  // return false; // not found, we're already beyond where it would be found
  // }
  // if (s.equals(sc_Type)) {
  // return true;
  // }
  // }
  // return false;
  // }
  // private final static Comparator<Candidate> pathComparator = new Comparator<Candidate>() {
  // @Override
  // public int compare(Candidate o1, Candidate o2) {
  // return o1.p.toString().compareTo(o2.p.toString());
  // }
  // };

  //// there may be several same-name roots not quite right
  //// xxx_Type$1.class
  //
  // private void addIfPreviousIsSameName(List<Path> c, int i) {
  // if (i == 0) return;
  // String _Type = candidates.get(i).toString();
  //// String prev = r.get(i-1).getPath();
  // String prefix = _Type.substring(0, _Type.length() - ("_Type." + (isSource ? "java" :
  //// "class")).length());
  // i--;
  // while (i >= 0) {
  // String s = candidates.get(i).toString();
  // if ( ! s.startsWith(prefix)) {
  // break;
  // }
  // if (s.substring(prefix.length()).equals((isSource ? ".java" : ".class"))) {
  // c.add(candidates.get(i));
  // break;
  // }
  // i--;
  // }
  // }

  //
  // for (int i = 0; i < pearOrJar.pathsToCandidateFiles.size(); i++) {
  // String candidatePath = pearOrJar.pathsToCandidateFiles.get(i);
  // String path_in_v3_classes = isPear
  // ? getPath_in_v3_classes(candidatePath)
  // : candidatePath;
  //
  // Path src = Paths.get(outputDirectory, "converted/v3-classes", path_in_v3_classes
  // + (isPear ? ".class" : ""));
  // Path tgt = pfs.getPath(
  // "/",
  // isPear
  // ? path2InsidePearOrJarPath.get(candidatePath) // needs to be bin/org/... etc
  // : candidatePath); // needs to be org/... etc
  // if (Files.exists(src)) {
  // Files.copy(src, tgt, StandardCopyOption.REPLACE_EXISTING);
  // reportPearOrJarClassReplace(pearOrJarCopy.toString(), path_in_v3_classes, kind);
  // }
  // }

  /// ** for compiled mode, do recompiling and reassembly of Jars and Pears */
  //
  // private boolean compileAndReassemble(CommonConverted cc, Container container, Path path) {
  // boolean noErrors = true;
  // if (javaCompiler != null) {
  // if (container.haveDifferentCapitalizedNamesCollidingOnWindows) {
  // System.out.println("Skipping compiling / reassembly because class " + container.toString() + "
  /// has multiple names differing only in capitalization, please resolve first.");
  // } else {
  //
  //
  // noErrors = compileV3PearSources(container, path);
  // noErrors = noErrors && compileV3NonPearSources(container, path);
  //
  // postProcessPearsOrJars("jars" , jars , jarClassReplace);
  // postProcessPearsOrJars("pears", pears, pearClassReplace);
  //
  ////
  //// try {
  //// Path pearOutDir = Paths.get(outputDirectory, "pears");
  //// FileUtils.deleteRecursive(pearOutDir.toFile());
  //// Files.createDirectories(pearOutDir);
  //// } catch (IOException e) {
  //// throw new RuntimeException(e);
  //// }
  ////
  //// System.out.format("replacing .class files in %,d PEARs%n", pears.size());
  //// for (PearOrJar p : pears) {
  //// pearOrJarPostProcessing(p);
  //// }
  //// try {
  //// reportPaths("Reports of updated Pears", "pearFileUpdates.txt", pearClassReplace);
  //// } catch (IOException e) {
  //// throw new RuntimeException(e);
  //// }
  // }
  // }
  //
  // return noErrors;
  // }

  /// **
  // * @return true if no errors
  // */
  // private boolean compileV3PearSources() {
  // boolean noError = true;
  // Map<String, List<ClassnameAndPath>> p2c = c2ps.stream()
  // .filter(c -> c.pearClasspath != null)
  // .collect(Collectors.groupingBy(c -> c.pearClasspath));
  //
  // List<Entry<String, List<ClassnameAndPath>>> ea = p2c.entrySet().stream()
  // .sorted(Comparator.comparing(Entry::getKey)) //(e1, e2) -> e1.getKey().compareTo(e2.getKey())
  // .collect(Collectors.toList());
  //
  // for (Entry<String, List<ClassnameAndPath>> e : ea) {
  // noError = noError && compileV3SourcesCommon(e.getValue(), "for Pear " + e.getKey(), e.getKey()
  /// );
  // }
  // return noError;
  // }
  //
  /// **
  // * @return true if no errors
  // */
  // private boolean compileV3NonPearSources() {
  //
  // List<ClassnameAndPath> cnps = c2ps.stream()
  // .filter(c -> c.pearClasspath == null)
  // .collect(Collectors.toList());
  //
  // return compileV3SourcesCommon(cnps, "(non PEAR)", null);
  // }

  /// **
  // * @param pathInPear a complete path to a class inside an (installed) pear
  // * @return the part starting after the top node of the install dir
  // */
  // private String getPath_in_v3_classes(String pathInPear) {
  // return path2classname.get(pathInPear);
  // }

  // private boolean reportDuplicates() throws IOException {
  // List<List<CommonConverted>> nonIdenticals = new ArrayList<>();
  // List<CommonConverted> onlyIdenticals = new ArrayList<>();
  //
  // classname2multiSources.forEach(
  // (classname, ccs) -> {
  // if (ccs.size() > 1) {
  // nonIdenticals.add(ccs);
  // } else {
  // CommonConverted cc = ccs.get(0);
  // if (cc.containersAndV2Paths.size() > 1)
  // onlyIdenticals.add(cc); // the same item in multiple containers and/or paths
  // }
  // }
  // );
  //
  // if (nonIdenticals.size() == 0) {
  // if (onlyIdenticals.size() == 0) {
  // System.out.println("There were no duplicates found.");
  // } else {
  // // report identical duplicates
  // try (BufferedWriter bw = Files.newBufferedWriter(makePath(outDirLog +
  // "identical_duplicates.txt"), StandardOpenOption.CREATE)) {
  // logPrintNl("Report of Identical duplicates:", bw);
  // for (CommonConverted cc : onlyIdenticals) {
  // int i = 0;
  // logPrintNl("Class: " + cc.fqcn_slash, bw);
  // for (ContainerAndPath cp : cc.containersAndV2Paths) {
  // logPrintNl(" " + (++i) + " " + cp, bw);
  // }
  // logPrintNl("", bw);
  // }
  // }
  // }
  // return true;
  // }
  //
  //// non-identicals, print out all of them
  // try (BufferedWriter bw = Files.newBufferedWriter(makePath(outDirLog +
  // "nonIdentical_duplicates.txt"), StandardOpenOption.CREATE)) {
  // logPrintNl("Report of non-identical duplicates", bw);
  // for (List<CommonConverted> nonIdentical : nonIdenticals) {
  // String fqcn = nonIdentical.get(0).fqcn_slash;
  // logPrintNl(" classname: " + fqcn, bw);
  // int i = 1;
  // // for each cc, and within each cc, for each containerAndPath
  // for (CommonConverted cc : nonIdentical) {
  //// logPrintNl(" version " + i, bw);
  // assert fqcn.equals(cc.fqcn_slash);
  // int j = 1;
  // boolean isSame = cc.containersAndV2Paths.size() > 1;
  // boolean isFirstTime = true;
  // for (ContainerAndPath cp : cc.containersAndV2Paths) {
  // String first = isSame && isFirstTime
  // ? " same: "
  // : isSame
  // ? " "
  // : " ";
  // isFirstTime = false;
  // logPrintNl(first + i + "." + (j++) + " " + cp, bw);
  // }
  // indent[0] -= 6;
  //// logPrintNl("", bw);
  // i++;
  // }
  //// logPrintNl("", bw);
  // }
  // }
  // return false;
  // }

  // private static class PathAndReason extends Report2<Path, String> {
  // Path path;
  // String reason;
  // PathAndReason(Path path, String reason) {
  // this.path = path;
  // this.reason = reason;
  // }
  // @Override
  // public Comparable<Path> getFirst() { return path; }
  // @Override
  // public Comparable<String> getSecond() { return reason; }
  // @Override
  // int getFirstLength() { return path.toString().length(); }
  // }

}
