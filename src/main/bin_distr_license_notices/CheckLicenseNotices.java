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

/*
 * Keeps the binary distribution's third-party license information in sync with
 * what is actually shipped, following the ASF policy at
 * https://infra.apache.org/licensing-howto.html
 *
 * The binary distribution bundles a fixed set of third-party JARs in lib/ (see
 * the assembly descriptor src/main/assembly/bin.xml). For each of those JARs
 * this tool:
 *
 *   - extracts the LICENSE / NOTICE files the JAR carries in its META-INF folder
 *     VERBATIM. These are NOT checked into the repository; they are emitted at
 *     build time (--emit) into the assembly's work directory, from where the
 *     assembly ships them under the distribution's licenses/ directory;
 *   - regenerates the pointer section of the top-level LICENSE.txt (one entry per
 *     bundled dependency: name, version, license type, and a pointer to its
 *     licenses/ folder), delimited by stable BEGIN/END markers; and
 *   - ASSISTS with the curated NOTICE file by reporting which bundled JARs carry
 *     a NOTICE, which ones are new/removed since the last run, and printing their
 *     NOTICE text so a maintainer can bubble up the required portions by hand.
 *     It never edits NOTICE.txt.
 *
 * The bundled set and its versions are read from the build itself (the assembly
 * descriptor and the POMs), so there is no second list to keep in sync and the
 * emitted licenses/ tree can never over- or under-disclose relative to lib/.
 *
 * Zero install, single solution for Linux/Mac/Windows: run with the JDK
 * single-file source launcher (Java 11+), which the project already requires to
 * build. The build invokes --emit via exec-maven-plugin at prepare-package.
 *
 *   Emit the verbatim licenses/ tree into <dir> (used by the build):
 *     java src/main/bin_distr_license_notices/CheckLicenseNotices.java --emit <dir>
 *
 *   Verify the LICENSE.txt pointer block and NOTICE manifest (exit 1 on drift):
 *     java src/main/bin_distr_license_notices/CheckLicenseNotices.java
 *
 *   Regenerate the LICENSE.txt pointer block and the NOTICE manifest:
 *     java src/main/bin_distr_license_notices/CheckLicenseNotices.java --apply
 *
 *   Print only the NOTICE assist report:
 *     java src/main/bin_distr_license_notices/CheckLicenseNotices.java --notice-report
 *
 *   Optional overrides:
 *     -Dbasedir=<repo-root>            (default: current directory)
 *     -Dmaven.repo.local=<repo-path>   (default: $M2_REPO or ~/.m2/repository)
 */

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;

public class CheckLicenseNotices {

  enum Mode { VERIFY, APPLY, NOTICE_REPORT, EMIT }

  static final String BEGIN_MARKER =
      "=== BEGIN GENERATED THIRD-PARTY DEPENDENCY POINTERS (do not edit by hand) ===";
  static final String END_MARKER =
      "=== END GENERATED THIRD-PARTY DEPENDENCY POINTERS ===";

  static final String LICENSE_TXT = "src/main/bin_distr_license_notices/LICENSE.txt";
  static final String NOTICE_MANIFEST = "src/main/bin_distr_license_notices/verified-notice-deps.txt";
  static final String BIN_XML = "src/main/assembly/bin.xml";
  static final String PARENT_POM = "uimaj-parent-internal/pom.xml";
  static final String ROOT_POM = "pom.xml";

  // A bundled third-party dependency.
  record Dep(String groupId, String artifactId, String version) {
    String key() { return artifactId + "-" + version; }
  }

  // One verbatim license/notice file extracted from a JAR.
  record LicenseFile(String depKey, String fileName, boolean isNotice, byte[] bytes) {
    String relPath() { return depKey + "/" + fileName; }
  }

  static Path baseDir;
  static Path localRepo;

  public static void main(String[] args) throws IOException {
    Mode mode = Mode.VERIFY;
    String emitDir = null;
    for (int i = 0; i < args.length; i++) {
      String a = args[i];
      switch (a) {
        case "--apply", "--fix" -> mode = Mode.APPLY;
        case "--verify", "--check" -> mode = Mode.VERIFY;
        case "--notice-report" -> mode = Mode.NOTICE_REPORT;
        case "--emit" -> {
          mode = Mode.EMIT;
          if (i + 1 >= args.length) { System.err.println("--emit requires a target directory"); System.exit(64); }
          emitDir = args[++i];
        }
        case "-h", "--help" -> { printUsage(); return; }
        default -> { System.err.println("Unknown argument: " + a); printUsage(); System.exit(64); }
      }
    }

    baseDir = Path.of(System.getProperty("basedir", System.getProperty("user.dir")));
    localRepo = resolveLocalRepo();
    System.out.println("Base dir   : " + baseDir.toAbsolutePath());
    System.out.println("Local repo : " + localRepo);
    System.out.println("Mode       : " + mode);
    System.out.println();

    // 1. Determine the bundled set and resolve versions.
    var deps = bundledDependencies();
    System.out.println("Bundled third-party dependencies (" + deps.size() + "):");
    for (var d : deps) System.out.println("  " + d.groupId() + ":" + d.artifactId() + ":" + d.version());
    System.out.println();

    // 2. Extract verbatim license/notice files from each JAR.
    var files = new ArrayList<LicenseFile>();
    for (var d : deps) files.addAll(extractLicenseFiles(d));

    // 3. Build the expected artifacts: the licenses/ tree, the NOTICE manifest,
    //    and the LICENSE.txt pointer block.
    var tree = new LinkedHashMap<String, byte[]>();
    for (var f : files) tree.put(f.relPath(), f.bytes());
    var noticeDeps = new TreeSet<String>();
    for (var f : files) if (f.isNotice()) noticeDeps.add(f.depKey());
    var manifest = String.join("\n", noticeDeps) + (noticeDeps.isEmpty() ? "" : "\n");
    var pointerBlock = pointerBlock(deps, tree);

    switch (mode) {
      case EMIT -> emit(tree, Path.of(emitDir));
      case APPLY -> apply(manifest, pointerBlock);
      case VERIFY -> { boolean drift = verify(manifest, pointerBlock); assist(deps, files);
                       if (drift) { System.out.println();
                         System.out.println("LICENSE.txt pointer block / NOTICE manifest are OUT OF DATE. Run with --apply.");
                         System.exit(1); }
                       System.out.println("\nLICENSE.txt pointer block and NOTICE manifest are up to date."); }
      case NOTICE_REPORT -> assist(deps, files);
    }
  }

  // ---- 1. bundled set + versions ------------------------------------------

  // The bundled third-party set is exactly the non-UIMA <include> entries in the
  // assembly descriptor - the single source of truth for what ships in lib/.
  static List<Dep> bundledDependencies() throws IOException {
    var bin = readString(baseDir.resolve(BIN_XML));
    var parentPom = readString(baseDir.resolve(PARENT_POM));
    var rootPom = readString(baseDir.resolve(ROOT_POM));
    var deps = new ArrayList<Dep>();
    var m = Pattern.compile("<include>\\s*([^<:\\s]+):([^<:\\s]+)\\s*</include>").matcher(bin);
    while (m.find()) {
      String groupId = m.group(1), artifactId = m.group(2);
      if (groupId.equals("org.apache.uima")) continue; // first-party
      deps.add(new Dep(groupId, artifactId, resolveVersion(artifactId, parentPom, rootPom)));
    }
    return deps;
  }

  // Version comes from a property in the parent POM where one exists, otherwise
  // from the hard-coded <version> beside the <artifactId> in the root POM.
  static String resolveVersion(String artifactId, String parentPom, String rootPom) {
    var prop = switch (artifactId) {
      case "jackson-core" -> "jackson-version";
      case "commons-io" -> "commons-io-version";
      case "commons-lang3" -> "commons-lang3-version";
      default -> artifactId.startsWith("spring-") ? "spring-version"
               : artifactId.startsWith("slf4j-") ? "slf4j-version"
               : null;
    };
    if (prop != null) {
      var v = group(parentPom, "<" + Pattern.quote(prop) + ">([^<]+)</" + Pattern.quote(prop) + ">");
      if (v != null) return v.strip();
    }
    var v = group(rootPom,
        "<artifactId>\\s*" + Pattern.quote(artifactId) + "\\s*</artifactId>\\s*<version>([^<]+)</version>");
    if (v != null) return v.strip();
    System.err.println("Could not resolve a version for " + artifactId
        + " (no property in parent POM, no hard-coded <version> in root POM)");
    System.exit(2);
    return null;
  }

  // ---- 2. extraction -------------------------------------------------------

  static List<LicenseFile> extractLicenseFiles(Dep d) throws IOException {
    var jar = jar(d.groupId(), d.artifactId(), d.version());
    var out = new ArrayList<LicenseFile>();
    try (var zf = new ZipFile(jar.toFile())) {
      var entries = zf.entries();
      while (entries.hasMoreElements()) {
        var e = entries.nextElement();
        if (e.isDirectory()) continue;
        var name = e.getName();
        if (!name.startsWith("META-INF/")) continue;
        var fn = name.substring("META-INF/".length());
        if (fn.contains("/") || fn.contains("\\") || fn.equals(".") || fn.equals("..")) continue;
        if (!isLicenseOrNotice(fn)) continue;
        try (var in = zf.getInputStream(e)) {
          out.add(new LicenseFile(d.key(), fn, isNotice(fn), in.readAllBytes()));
        }
      }
    }
    return out;
  }

  static boolean isLicenseOrNotice(String fn) {
    var l = fn.toLowerCase();
    return l.contains("license") || l.contains("notice");
  }

  static boolean isNotice(String fn) { return fn.toLowerCase().contains("notice"); }

  // ---- 3. pointer block ----------------------------------------------------

  static String licenseType(String artifactId) {
    return artifactId.startsWith("slf4j-") ? "MIT License" : "Apache License 2.0";
  }

  static String displayName(String artifactId) {
    return switch (artifactId) {
      case "jackson-core" -> "Jackson Core";
      case "procyon-core" -> "Procyon Core";
      case "procyon-compilertools" -> "Procyon CompilerTools";
      case "javaparser-core" -> "JavaParser Core";
      case "slf4j-api" -> "SLF4J API";
      case "slf4j-jdk14" -> "SLF4J JDK14 binding";
      case "commons-io" -> "Apache Commons IO";
      case "commons-lang3" -> "Apache Commons Lang";
      default -> artifactId.startsWith("spring-") ? "Spring Framework (" + artifactId + ")" : artifactId;
    };
  }

  static String pointerBlock(List<Dep> deps, Map<String, byte[]> tree) {
    var b = new StringBuilder();
    b.append(BEGIN_MARKER).append("\n\n");
    b.append("This distribution bundles the following third-party dependencies in the lib/\n");
    b.append("directory. Verbatim copies of the LICENSE and NOTICE files supplied by these\n");
    b.append("dependencies are provided under the licenses/ directory of this distribution.\n\n");
    for (var d : deps) {
      var hasFolder = tree.keySet().stream().anyMatch(k -> k.startsWith(d.key() + "/"));
      var name = displayName(d.artifactId()) + " " + d.version();
      var pointer = hasFolder
          ? "-> licenses/" + d.key() + "/"
          : "(no separate license/notice file bundled; see the license text above)";
      b.append(String.format("  %-44s %-19s %s%n", name, licenseType(d.artifactId()), pointer));
    }
    b.append("\n").append(END_MARKER).append("\n");
    return b.toString();
  }

  // ---- emit (build time) ---------------------------------------------------

  // Write the verbatim license/notice tree into the given directory (typically
  // the assembly's work directory under target/). These files are never checked
  // in; the build regenerates them from the bundled JARs on every package.
  static void emit(Map<String, byte[]> tree, Path dir) throws IOException {
    if (Files.exists(dir)) deleteRecursively(dir);
    Files.createDirectories(dir);
    var root = dir.toAbsolutePath().normalize();
    for (var e : tree.entrySet()) {
      var target = root.resolve(e.getKey()).normalize();
      if (!target.startsWith(root)) {
        throw new IOException("Refusing to write outside the target directory: " + e.getKey());
      }
      Files.createDirectories(target.getParent());
      Files.write(target, e.getValue());
    }
    System.out.println("Wrote " + tree.size() + " verbatim license/notice files to " + dir.toAbsolutePath());
  }

  // ---- apply ---------------------------------------------------------------

  static void apply(String manifest, String pointerBlock) throws IOException {
    var manifestPath = baseDir.resolve(NOTICE_MANIFEST);
    Files.createDirectories(manifestPath.getParent());
    Files.writeString(manifestPath, manifest, StandardCharsets.UTF_8);
    System.out.println("Wrote NOTICE-bearing dependency manifest " + NOTICE_MANIFEST);

    var licenseTxt = baseDir.resolve(LICENSE_TXT);
    var oldText = readString(licenseTxt);
    var newText = replaceManagedBlock(oldText, pointerBlock);
    if (!newText.equals(oldText)) {
      Files.writeString(licenseTxt, newText, StandardCharsets.UTF_8);
      System.out.println("Updated the generated pointer block in " + LICENSE_TXT);
    } else {
      System.out.println("Pointer block in " + LICENSE_TXT + " already up to date");
    }
    System.out.println("\nReview the diff, then update NOTICE.txt by hand using the assist report below.\n");
    System.out.println("Done.");
  }

  static String replaceManagedBlock(String text, String pointerBlock) {
    var block = pointerBlock.stripTrailing();
    var m = Pattern.compile(Pattern.quote(BEGIN_MARKER) + ".*?" + Pattern.quote(END_MARKER),
        Pattern.DOTALL).matcher(text);
    if (m.find()) return m.replaceFirst(Matcher.quoteReplacement(block));
    // First run: markers absent - append at end.
    var sep = text.endsWith("\n") ? "\n" : "\n\n";
    System.out.println("No markers found in LICENSE.txt - appending generated block at end of file.");
    return text + sep + block + "\n";
  }

  // ---- verify --------------------------------------------------------------

  // The verbatim licenses/ tree is no longer checked in (it is emitted at build
  // time), so verification covers the two artifacts that ARE checked in: the
  // LICENSE.txt pointer block and the NOTICE-bearing dependency manifest.
  static boolean verify(String manifest, String pointerBlock) throws IOException {
    var drift = false;

    // NOTICE manifest?
    var manifestPath = baseDir.resolve(NOTICE_MANIFEST);
    var currentManifest = Files.exists(manifestPath) ? readString(manifestPath) : "";
    if (!currentManifest.equals(manifest)) { System.out.println("CHANGED:  " + NOTICE_MANIFEST); drift = true; }

    // LICENSE.txt managed block?
    var license = readString(baseDir.resolve(LICENSE_TXT));
    var m = Pattern.compile(Pattern.quote(BEGIN_MARKER) + ".*?" + Pattern.quote(END_MARKER),
        Pattern.DOTALL).matcher(license);
    if (!m.find()) { System.out.println("MISSING:  generated pointer block markers in LICENSE.txt"); drift = true; }
    else if (!m.group().equals(pointerBlock.stripTrailing())) {
      System.out.println("CHANGED:  generated pointer block in LICENSE.txt");
      printLineDiff(m.group(), pointerBlock.stripTrailing());
      drift = true;
    }
    if (!drift) System.out.println("LICENSE.txt pointer block and NOTICE manifest match the bundled JARs.");
    return drift;
  }

  // ---- NOTICE assist -------------------------------------------------------

  static void assist(List<Dep> deps, List<LicenseFile> files) throws IOException {
    System.out.println("\n================ NOTICE assist (informational) ================");
    var manifestPath = baseDir.resolve(NOTICE_MANIFEST);
    var previous = new TreeSet<String>();
    if (Files.exists(manifestPath))
      for (var line : readString(manifestPath).split("\\R")) if (!line.isBlank()) previous.add(line.strip());
    var current = new TreeSet<String>();
    for (var f : files) if (f.isNotice()) current.add(f.depKey());

    var added = new TreeSet<>(current); added.removeAll(previous);
    var removed = new TreeSet<>(previous); removed.removeAll(current);
    if (!added.isEmpty()) System.out.println("NEW NOTICE-bearing dependencies since last --apply: " + added);
    if (!removed.isEmpty()) System.out.println("REMOVED NOTICE-bearing dependencies since last --apply: " + removed);
    if (added.isEmpty() && removed.isEmpty())
      System.out.println("No change in the set of NOTICE-bearing dependencies since last --apply.");

    System.out.println("\nBundled NOTICE content to bubble up (curate into NOTICE.txt by");
    System.out.println("hand - keep it minimal, only what is required):");
    for (var f : files) {
      if (!f.isNotice()) continue;
      System.out.println("\n----- " + f.depKey() + " : META-INF/" + f.fileName() + " -----");
      System.out.println(new String(f.bytes(), StandardCharsets.UTF_8).strip());
    }
    System.out.println("\n===============================================================");
  }

  // ---- helpers -------------------------------------------------------------

  static void printLineDiff(String oldText, String newText) {
    String[] o = oldText.split("\n", -1), n = newText.split("\n", -1);
    for (int i = 0; i < Math.max(o.length, n.length); i++) {
      String a = i < o.length ? o[i] : "", b = i < n.length ? n[i] : "";
      if (!a.equals(b)) { System.out.println("    - " + a); System.out.println("    + " + b); }
    }
  }

  static void deleteRecursively(Path dir) throws IOException {
    try (var walk = Files.walk(dir)) {
      for (var p : (Iterable<Path>) walk.sorted((x, y) -> y.compareTo(x))::iterator) Files.delete(p);
    }
  }

  static Path resolveLocalRepo() {
    var prop = System.getProperty("maven.repo.local");
    if (prop != null && !prop.isBlank()) return Path.of(prop);
    var env = System.getenv("M2_REPO");
    if (env != null && !env.isBlank()) return Path.of(env);
    return Path.of(System.getProperty("user.home"), ".m2", "repository");
  }

  static Path jar(String groupId, String artifactId, String version) {
    var p = localRepo;
    for (var seg : groupId.split("\\.")) p = p.resolve(seg);
    p = p.resolve(artifactId).resolve(version).resolve(artifactId + "-" + version + ".jar");
    if (!Files.exists(p)) {
      System.err.println("Artifact not found: " + p);
      System.err.println("Resolve dependencies first, e.g.: mvn -q dependency:resolve");
      System.exit(3);
    }
    return p;
  }

  static String group(String text, String regex) {
    var m = Pattern.compile(regex).matcher(text);
    return m.find() ? m.group(1) : null;
  }

  static String readString(Path p) throws IOException {
    return Files.readString(p, StandardCharsets.UTF_8);
  }

  static void printUsage() {
    System.out.println("Usage: java src/main/bin_distr_license_notices/CheckLicenseNotices.java"
        + " [--emit <dir> | --verify | --apply | --notice-report]");
    System.out.println("  --emit <dir>     extract the verbatim licenses/ tree into <dir> (used by the build)");
    System.out.println("  --verify         (default) check the LICENSE.txt block and NOTICE manifest; exit 1 on drift");
    System.out.println("  --apply          regenerate the LICENSE.txt pointer block and the NOTICE manifest");
    System.out.println("  --notice-report  print only the NOTICE assist report");
  }
}
