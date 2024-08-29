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

package org.apache.uima.internal.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.AbstractSequentialList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import org.apache.uima.UIMAFramework;
import org.apache.uima.UIMARuntimeException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.impl.BuiltinTypeKinds;
import org.apache.uima.internal.util.MsgLocalizationClassLoader.CallStack;
import org.apache.uima.internal.util.function.Runnable_withException;
import org.apache.uima.util.Level;
import org.apache.uima.util.Logger;

public class Misc {

  public static final boolean isJava9ea = System.getProperty("java.version").startsWith("9-ea");

  public static final String blanks = new String(new char[1000]).replace('\0', ' ');
  public static final String dots = "...";
  // public static final String ls = System.lineSeparator(); // n or r n
  public static final int[] INT0 = new int[] { 0 };

  private static final Pattern whitespace = Pattern.compile("\\s");

  public static String replaceWhiteSpace(String s, String replacement) {
    return whitespace.matcher(s).replaceAll(replacement);
  }

  public static String null2str(String s) {
    return (s == null) ? "" : s;
  }

  public static byte[] hex_string_to_bytearray(String s) {
    int len2 = s.length();
    int len = len2 >> 1;
    byte[] out = new byte[len];
    for (int out_i = 0, str_i = 0; out_i < len; out_i++, str_i += 2) {
      out[out_i] = (byte) ((Character.digit(s.charAt(str_i), 16) << 4)
              + Character.digit(s.charAt(str_i + 1), 16));
    }
    return out;
  }

  public static String dumpByteArray(byte[] b, int limit) {
    if (b == null) {
      return "null";
    }
    if (b.length == 0) {
      return "0-length";
    }
    StringBuilder sb = new StringBuilder(b.length * 3);
    for (int i = 0; i < b.length; i++) {
      if ((i % 100) == 0) {
        sb.append('\n');
      }
      sb.append(String.format("%02X", b[i])); // 0 means 0 padding, 2 means width
      if ((i % 2) == 1) {
        sb.append(' ');
      }
      if (i > limit) {
        sb.append("\n Hit the limit: ").append(limit);
        break;
      }
    }
    return sb.toString();
  }

  /**
   * @param s
   *          starting frames above invoker
   * @param n
   *          max number of callers to return
   * @return x called by: y ...
   */
  public static StringBuilder getCallers(final int s, final int n) {
    return dumpCallers(Thread.currentThread().getStackTrace(), s, n);
  }

  public static StringBuilder dumpCallers(final StackTraceElement[] e, final int s, final int n) {
    StringBuilder sb = new StringBuilder();
    for (int i = s + 2; i < s + n + 3; i++) {
      if (i >= e.length)
        break;
      if (i != s + 2) {
        sb.append("\n  called_by: ");
      }
      sb.append(formatcaller(e[i]));
    }
    return sb;
  }

  /**
   * @return the name of the caller in the stack and their caller
   */
  public static String getCaller() {
    StackTraceElement[] e = Thread.currentThread().getStackTrace();
    return formatcaller(e[5]) + " => " + formatcaller(e[4]);
  }

  private static String formatcaller(StackTraceElement e) {
    String n = e.getClassName();
    return n.substring(1 + n.lastIndexOf('.')) + "." + e.getMethodName() + "[" + e.getLineNumber()
            + "]";
  }

  public static String formatcaller(String className, String methodName, int lineNumber) {
    return className.substring(1 + className.lastIndexOf('.')) + "." + methodName + "[" + lineNumber
            + "]";
  }

  public static ClassLoader[] getCallingClass_classLoaders() {

    // get the call stack; "new" is needed to get the current context call stack

    final Class<?>[] cs = new CallStack().getCallStack();
    // start at the caller of the caller's class loader
    // cs[0] is getClassContext
    // cs[1] is getCallStack
    // cs[2] is this method, getCallingClass_classLoaders
    ArrayList<ClassLoader> cls = new ArrayList<>();
    for (int i = 3; i < cs.length; i++) {
      Class<?> callingClass = cs[i];
      ClassLoader cl = callingClass.getClassLoader();
      if (null == cl) { // means system class loader
        cl = ClassLoader.getSystemClassLoader();
      }
      if (!cls.contains(cl)) {
        cls.add(cl);
      }
    }
    return cls.toArray(new ClassLoader[cls.size()]);
  }

  /**
   * @param s
   *          the string to possibly elide
   * @param n
   *          the length, after which, elision happens
   * @return the elided string, padded on the left to length n
   */
  public static String elide(String s, int n) {
    return elide(s, n, true);
  }

  /**
   * @param s
   *          the string to possibly elide
   * @param n
   *          the length, after which, elision happens
   * @param pad
   *          true to include left padding to length n
   * @return the elided string, padded on the left to length n
   */
  public static String elide(String s, int n, boolean pad) {
    if (s == null) {
      s = "null";
    }
    int sl = s.length();
    if (sl <= n) {
      return s + (pad ? blanks.substring(0, n - sl) : "");
    }
    int dl = 1; // (n < 11) ? 1 : (n < 29) ? 2 : 3; // number of dots
    int ss = (n - dl) / 2;
    int ss2 = ss + ((ss * 2 == (n - dl)) ? 0 : 1);
    return s.substring(0, ss) + dots.substring(0, dl) + s.substring(sl - ss2);
  }

  /**
   * @param sb
   *          the stringBuilder to indent
   * @param indent
   *          the indent amount (passed as array of 1 item, to allow it to be final for lambdas
   * @return the stringBuilder, with nl if needed, and indention
   */
  public static StringBuilder indent(StringBuilder sb, int[] indent) {
    return indent(sb, indent[0]);
  }

  /**
   * @param sb
   *          the stringBuilder to indent
   * @param indent
   *          the indent amount
   * @return the stringBuilder, with nl if needed, and indention
   */
  public static StringBuilder indent(StringBuilder sb, int indent) {
    // if the current sb doesn't end with a new line and indent > 0, add a new line
    if (!endsWithNl(sb) && indent > 0) {
      sb.append('\n');
    }
    return sb.append(blanks, 0, Math.min(blanks.length(), indent));
  }

  public static void addNlIfMissing(StringBuilder sb) {
    if (!endsWithNl(sb)) {
      sb.append('\n');
    }
  }

  public static void addNlIfMissing(StringBuffer sb) {
    if (!endsWithNl(sb)) {
      sb.append('\n');
    }
  }

  private static boolean endsWithNl(StringBuilder sb) {
    int l = sb.length();
    return (l >= 1) && sb.charAt(l - 1) == '\n';
  }

  private static boolean endsWithNl(StringBuffer sb) {
    int l = sb.length();
    return (l >= 1) && sb.charAt(l - 1) == '\n';
  }

  public static final MethodHandles.Lookup UIMAlookup = MethodHandles.lookup();

  private static FilenameFilter jarFilter = new FilenameFilter() {
    @Override
    public boolean accept(File dir, String name) {
      name = name.toLowerCase();
      return (name.endsWith(".jar"));
    }
  };

  public static URL[] getURLs(String s)
          throws MalformedURLException, IOException, URISyntaxException {
    List<URL> urls = new ArrayList<>();
    String[] spaths = s.split(File.pathSeparator);
    for (String p : spaths) {
      addUrlsFromPath(p, urls);
    }
    return urls.toArray(new URL[urls.size()]);
  }

  /**
   * Given a String corresponding to one file path, which may be a directory, or may end in *, add
   * the URLS it represents to the urls argument.
   * 
   * @param p
   *          a Jar path, or a Directory, or a directory ending with a directory-separator and a
   *          single * p may be relative or absolute, following the definition of same in the Java
   *          File class.
   * @param urls
   *          the list to add the URLs to
   * @throws MalformedURLException
   *           -
   * @throws IOException
   *           -
   * @throws URISyntaxException
   *           -
   */
  public static void addUrlsFromPath(String p, List<URL> urls)
          throws MalformedURLException, IOException, URISyntaxException {
    boolean mustBeDirectory = false;
    if (p.endsWith("*")) {
      if (p.length() < 2 || p.charAt(p.length() - 2) != File.separatorChar) {
        UIMAFramework.getLogger().error("Path Specification \"{}\" invalid.", p);
        throw new MalformedURLException();
      }
      p = p.substring(0, p.length() - 2);
      mustBeDirectory = true;
    }

    File pf = new File(p);
    if (pf.isDirectory()) {
      File[] jars = pf.listFiles(jarFilter);
      if (jars == null || jars.length == 0) {
        // this is the case where the user wants to include
        // a directory containing non-jar'd .class files
        addPathToURLs(urls, pf);
      } else {
        for (File f : jars) {
          addPathToURLs(urls, f);
        }
      }
    } else if (mustBeDirectory) {
      UIMAFramework.getLogger().error("Path Specification \"{}\" must be a directory.", p);
      throw new MalformedURLException();
    } else if (p.toLowerCase().endsWith(".jar")) {
      addPathToURLs(urls, pf);
    } else {
      // have a segment which does not denote a jar - skip it but note that
      UIMAFramework.getLogger()
              .warn("Skipping adding \"{}\" to URLs because it is not a directory or a JAR", p);
    }
  }

  private static void addPathToURLs(List<URL> urls, File cp) throws MalformedURLException {
    URL url = cp.toURI().toURL();
    urls.add(url);
  }

  /**
   * Convert a classpath having multiple parts separated by the pathSeparator, expanding paths that
   * end with "*" as needed.
   * 
   * @param classpath
   *          - to scan and convert to list of URLs
   * @return the urls
   */
  public static URL[] classpath2urls(String classpath) {
    try {
      return getURLs(classpath);
    } catch (IOException | URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  public static String expandClasspath(String classpath) {
    StringBuilder sb = new StringBuilder();
    for (URL url : classpath2urls(classpath)) {
      sb.append(url.getPath()); // returns as a string just the path part of the url
      sb.append(File.pathSeparatorChar);
    }
    return sb.substring(0, sb.length() - 1); // drop trailing ":"
  }

  /**
   * 
   * @param name
   *          of property
   * @return true if property is defined, or is defined and set to anything except "false"; false if
   *         property is not defined, or is defined and set to "false".
   */
  public static boolean getNoValueSystemProperty(String name) {
    return !System.getProperty(name, "false").equals("false");
  }

  /**
   * For standardized prettyprinting, to string. Adds a collection of things (toString) separated by
   * , and surrounded by [ ], to a StringBuilder
   * 
   * @param sb
   *          where the formatted collection results are appended to
   * @param c
   *          the collection
   * @param <T>
   *          the kind of elements in the collection
   * @return the StringBuilder for chaining
   */
  public static <T> StringBuilder addElementsToStringBuilder(StringBuilder sb, Collection<T> c) {
    return addElementsToStringBuilder(sb, c, 1000);
  }

  public static <T> StringBuilder addElementsToStringBuilder(StringBuilder sb, Collection<T> c,
          int limit) {
    return addElementsToStringBuilder(sb, c, limit, StringBuilder::append);
  }

  public static <T> StringBuilder addElementsToStringBuilder(StringBuilder sb, Collection<T> c,
          BiConsumer<StringBuilder, T> appender) {
    return addElementsToStringBuilder(sb, c, 1000, appender);
  }

  public static <T> StringBuilder addElementsToStringBuilder(List<T> c, int limit) {
    return addElementsToStringBuilder(c, limit, StringBuilder::append);
  }

  public static <T> StringBuilder addElementsToStringBuilder(List<T> c, int limit,
          BiConsumer<StringBuilder, T> appender) {
    return addElementsToStringBuilder(INT0, c, limit, appender);
  }

  public static <T> StringBuilder addElementsToStringBuilder(int[] indent, List<T> c, int limit,
          BiConsumer<StringBuilder, T> appender) {
    if (c == null) {
      return indent(new StringBuilder(), indent).append("null");
    }

    int sz = Math.min(limit, c.size());
    StringBuilder sb = indent(new StringBuilder(sz * 5 + 2), indent);
    return addElementsToStringBuilder(indent, sb, c, limit, appender);
  }

//@formatter:off
  /**
   * Does two styles of list formatting:
   *   Style 1:  [ item1, item 2, item3]
   *   Style 2:  [ 
   *               item1,
   *               item2,
   *               item3
   *             ]
   * Starts as style 1, switches to style 2 when length &gt; 60             
   * @param sb where the string is assembled
   * @param c the collection to process
   * @param limit the maximum number of items, if negative, no limit
   * @param appender the appender
   * @param <T> the type of the collection
   * @return argument sb, appeneded
   */
//@formatter:on
  public static <T> StringBuilder addElementsToStringBuilder(StringBuilder sb, Collection<T> c,
          int limit, BiConsumer<StringBuilder, T> appender) {
    return addElementsToStringBuilder(INT0, sb, c, limit, appender);
  }

  public static <T> StringBuilder addElementsToStringBuilder(int[] indent, StringBuilder sb,
          Collection<T> c, int limit, BiConsumer<StringBuilder, T> appender) {

    int origLength = sb.length();
    if (c == null)
      return sb.append("<null>");
    if (c.size() == 0) { // empty case
      return sb.append("[]");
    }

    sb.append('[');
    int i = 0;
    boolean overLimit = false;
    for (T item : c) {
      if ((i++) >= limit && limit >= 0) {
        overLimit = true;
        break;
      } else {
        // sb.append(item.toString());
        appender.accept(sb, item);
        sb.append(", ");
        if (sb.length() - origLength > 60) {
          sb.setLength(origLength); // is too long to present on one line, change to multi-line
                                    // format
          return style2(indent, sb, c, limit, appender);
        }
      }
    }

    if (overLimit) {
      sb.append("...");
    } else {
      sb.setLength(sb.length() - 2); // drop the final ", "
    }

    sb.append(']');
    return sb;
  }

  public static StringBuilder addElementsToStringBuilder(StringBuilder sb, int size, int limit,
          int indent, int incr, BiConsumer<StringBuilder, Integer> appender) {
    int origLength = sb.length();

    if (size == 0) { // empty case
      return sb.append("[]");
    }

    // first try to put on one line
    sb.append('[');

    for (int i = 0; i < limit; i++) {
      if (i != 0) {
        sb.append(", ");
      }
      appender.accept(sb, i);

      if (sb.length() - origLength > 120) {
        sb.setLength(origLength); // is too long to present on one line, change to multi-line format
        return style2(sb, size, limit, indent, incr, appender);
      }
    }

    if (size > limit) {
      sb.append("...");
    }

    sb.append(']');
    return sb;
  }

  private static <T> StringBuilder style2(int[] indent, StringBuilder sb, Collection<T> c,
          int limit, BiConsumer<StringBuilder, T> appender) {
    sb.append("[");
    indent[0] += 2;
    indent(sb, indent);
    try {
      int i = 0;
      int cl = -1; // for dropping trailing end punctuation
      boolean overLimit = false;
      for (T item : c) {
        if ((i++) >= limit && limit >= 0) {
          overLimit = true;
          break;
        } else {
          appender.accept(sb, item);
          cl = sb.length();
          sb.append(",");
          indent(sb, indent);
        }
      }

      if (overLimit) {
        sb.append("...");
      } else {
        sb.setLength(cl); // drop the final ",etc "
      }
    } finally {
      indent[0] -= 2;
      indent(sb, indent).append(']');
    }
    return sb;
  }

  private static <T> StringBuilder style2(StringBuilder sb, int size, int limit, int indent,
          int incr, BiConsumer<StringBuilder, Integer> appender) {
    sb.append("[");
    indent += incr;

    for (int i = 0; i < limit; i++) {
      if (i != 0) {
        sb.append(',');
      }
      sb.append('\n');
      indent(sb, indent);
      appender.accept(sb, i);
    }

    if (size > limit) {
      sb.append(",\n");
      indent(sb, indent);
      sb.append("...");
    }

    sb.append('\n');
    indent(sb, indent - incr);
    sb.append(']');
    return sb;
  }

  /**
   * Writes a byte array output stream to a file
   * 
   * @param baos
   *          the array to write
   * @param name
   *          the name of the file
   */
  public static void toFile(ByteArrayOutputStream baos, String name) {
    try (FileOutputStream fos = new FileOutputStream(name)) {
      baos.writeTo(fos);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Writes a byte array output stream to a file
   * 
   * @param baos
   *          the array to write
   * @param file
   *          the file
   */
  public static void toFile(ByteArrayOutputStream baos, File file) {
    try (FileOutputStream fos = new FileOutputStream(file)) {
      baos.writeTo(fos);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * For multi-core machine "tuning" - the number of cores
   */
  public static final int numberOfCores = Runtime.getRuntime().availableProcessors();

  /**
   * Convert an int argument to the next higher power of 2 if not already a power of 2
   * 
   * @param i
   *          the value to convert
   * @return the next higher power of 2, or i if it is already a power of 2
   */
  public static int nextHigherPowerOf2(int i) {
    return (i < 1) ? 1 : Integer.highestOneBit(i) << ((Integer.bitCount(i) == 1 ? 0 : 1));
  }

  /**
   * Convert an int argument to the next higher power of 2 to the x power
   * 
   * @param i
   *          the value to convert
   * @param x
   *          the power of 2 to use
   * @return the next higher power of 2 to the x, or i if it is already == to 2 to the x
   */
  public static int nextHigherPowerOfX(int i, int x) {
    int shft = 31 - Integer.numberOfLeadingZeros(x); // x == 8, shft = 3
    return (i < 1) ? x : ((i + (x - 1)) >>> shft) << shft;
  }

  /**
   * Given a class, a lookup context, and a protected method and its arg classes, return the method
   * handle for that method.
   * 
   * Using that method handle is slow, but converting it to a lambda makes for JIT-able fast access.
   * 
   * @param clazz
   *          -
   * @param methodHandleAccessContext
   *          -
   * @param protectedMethod
   *          -
   * @param args
   *          -
   * @return -
   */
  public static MethodHandle getProtectedMethodHandle(Class<?> clazz,
          Lookup methodHandleAccessContext, String protectedMethod, Class<?>... args) {
    try {
      Method m = clazz.getDeclaredMethod(protectedMethod, args);
      m.setAccessible(true);
      return methodHandleAccessContext.unreflect(m);
    } catch (NoSuchMethodException | SecurityException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Given a class, and a protected method and its arg classes, return the method handle for that
   * method.
   * 
   * Note: uses the UIMA context as the lookup context
   * 
   * Using that method handle is slow, but converting it to a lambda makes for JIT-able fast access.
   * 
   * @param clazz
   *          -
   * @param protectedMethod
   *          -
   * @param args
   *          -
   * @return -
   */
  public static MethodHandle getProtectedMethodHandle(Class<?> clazz, String protectedMethod,
          Class<?>... args) {
    return getProtectedMethodHandle(clazz, UIMAlookup, protectedMethod, args);
  }

  public static MethodHandle getProtectedFieldGetter(Class<?> clazz, String protectedField) {
    try {
      Field f = clazz.getDeclaredField(protectedField);
      f.setAccessible(true);
      return UIMAlookup.unreflectGetter(f);
    } catch (NoSuchFieldException | SecurityException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Gets an int from a named field. If the field isn't present, returns Integer.MIN_VALUE;
   * 
   * @param clazz
   *          the class where the field is
   * @param fieldName
   *          the name of the field
   * @return the value or Integer.MIN_VALUE if not present
   */
  public static int getStaticIntField(Class<?> clazz, String fieldName) {
    try {
      Field f = clazz.getField(fieldName);
      return f.getInt(null);
    } catch (NoSuchFieldException e) {
      return Integer.MIN_VALUE;
    } catch (SecurityException | IllegalArgumentException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  public static int getStaticIntFieldNoInherit(Class<?> clazz, String fieldName) {
    try {
      Field f = clazz.getDeclaredField(fieldName);
      return f.getInt(null);
    } catch (NoSuchFieldException e) {
      return Integer.MIN_VALUE;
    } catch (SecurityException | IllegalArgumentException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  public static int getPrivateStaticIntFieldNoInherit(Class<?> clazz, String fieldName) {
    try {
      Field f = clazz.getDeclaredField(fieldName);
      f.setAccessible(true);
      return f.getInt(null);
    } catch (NoSuchFieldException e) {
      return Integer.MIN_VALUE;
    } catch (SecurityException | IllegalArgumentException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Takes trailing arguments of strings and adds them all the first
   * 
   * @param c
   *          the collection to add the strings to
   * @param v
   *          0 or more strings as arguments
   */
  public static void addAll(Collection<String> c, String... v) {
    for (String s : v) {
      c.add(s);
    }
  }

  public static void debug(Object o) {
    System.err.println("Debug: " + o);
  }

  /**
   * Check and throw UIMA Internal Error if false
   * 
   * @param v
   *          if false, throws
   */
  public static void assertUie(boolean v) {
    if (!v)
      throw new UIMARuntimeException(UIMARuntimeException.INTERNAL_ERROR);
  }

  public static void assertUie(boolean v, Throwable e) {
    if (!v)
      throw new UIMARuntimeException(e, UIMARuntimeException.INTERNAL_ERROR, e);
  }

  public static RuntimeException internalError() {
    assertUie(false);
    return null;
  }

  public static void internalError(Throwable e) {
    assertUie(false, e);
  }

  // The hash function is derived from murmurhash3 32 bit, which
  // carries this statement:

  // MurmurHash3 was written by Austin Appleby, and is placed in the public
  // domain. The author hereby disclaims copyright to this source code.

  // See also MurmurHash3 in wikipedia

  private static final int C1 = 0xcc9e2d51;
  private static final int C2 = 0x1b873593;
  private static final int seed = 0x39c2ab57; // arbitrary bunch of bits

  public static int hashInt(int k1) {
    k1 *= C1;
    k1 = Integer.rotateLeft(k1, 15);
    k1 *= C2;

    int h1 = seed ^ k1;
    h1 = Integer.rotateLeft(h1, 13);
    h1 = h1 * 5 + 0xe6546b64;

    h1 ^= h1 >>> 16; // unsigned right shift
    h1 *= 0x85ebca6b;
    h1 ^= h1 >>> 13;
    h1 *= 0xc2b2ae35;
    h1 ^= h1 >>> 16;
    return h1;
  }

  /**
   * a hash for strings as a long - less likely to be a collision
   * 
   * @param s
   *          - the string
   * @return a long hash
   */
  public static long hashStringLong(String s) {
    if (s == null)
      return 0;
    int l = s.length();
    if (l == 0)
      return 0;
    long c = 1;

    for (int i = 0; i < l; i++) {
      c = 31 * c + s.charAt(i);
    }
    return c;
  }

  /**
   * Get item from array list. If index is &gt; length, expand the array, and return null
   * 
   * @param a
   *          the list
   * @param i
   *          the index
   * @param <T>
   *          the type of the items in the list
   * @return the item at the index or null
   */
  public static <T> T getWithExpand(List<T> a, int i) {
    while (i >= a.size()) {
      a.add(null);
    }
    return a.get(i);
  }

  public static <T> void setWithExpand(List<T> a, int i, T value) {
    while (i >= a.size()) {
      a.add(null);
    }
    a.set(i, value);
  }

  public static boolean equalStrings(String s1, String s2) {
    if (null == s1) {
      return (null == s2);
    }
    return (null == s2) ? false : s1.equals(s2);
  }

  public static int compareStrings(String s1, String s2) {
    if (null == s1) {
      if (null == s2) {
        return 0;
      }
      return -1;
    }
    return (null == s2) ? 1 : s1.compareTo(s2);
  }

  public static String elideString(String s, int len) {
    if (s.length() <= len) {
      return s;
    }

    return (s.substring(0, len) + "...");
  }

//@formatter:off
  /**
   * Some objects can be shared, if "equal", rather than creating duplicates, if they're read-only.
   * This may in general be beneficial by reducing the size of the "working set" via more sharing of read-only objects.
   * Users should insure the read-only property.
   * This routine allows 
   *   a) creating a potentially sharable object
   *   b) checking to see if we already have an "equal" one, and 
   *   c) if so, using that and allowing the just created one to be GC'd.
   * 
   * @param obj
   *          - the object to use a cached substitute for, if one exists
   * @param cache
   *          - the cache
   * @param <T>
   *          the type of the cached object
   * @return - the object or a cached version of it.
   */
//@formatter:on
  public static <T> T shareExisting(T obj, WeakHashMap<T, WeakReference<T>> cache) {
    if (null == obj) {
      throw new IllegalArgumentException();
    }
    T v;
    synchronized (cache) {
      WeakReference<T> r = cache.get(obj);
      if (r == null || (v = r.get()) == null) {
        cache.put(obj, new WeakReference<>(obj));
        return obj;
      }
      return v;
    }
  }

  /**
   * format a list of items for pretty printing as [item1, item2, ... ]
   * 
   * @param items
   *          to print
   * @param <T>
   *          the type of elements in the list
   * @return [item1, item2, ... ]
   */
  public static <T> String ppList(List<T> items) {
    return ppList(items, 1000);
  }

  /**
   * @param items
   *          to print
   * @param max
   *          - maximum number of items to print
   * @param <T>
   *          the type of elements in the list
   * @return [item1, item2, ... ]
   */
  public static <T> String ppList(List<T> items, int max) {
    return addElementsToStringBuilder(items, max).toString();
  }

  /**
   * @param items
   *          to print
   * @param max
   *          - maximum number of items to print
   * @param appender
   *          - appender function
   * @param <T>
   *          the type of elements in the list
   * @return [item1, item2, ... ]
   */
  public static <T> String ppList(List<T> items, int max, BiConsumer<StringBuilder, T> appender) {
    return addElementsToStringBuilder(items, max, appender).toString();
  }

  /**
   * format a list of items for pretty printing as [item1, item2, ... ]
   * 
   * @param indent
   *          the amount to use as indentation
   * @param items
   *          to print
   * @param <T>
   *          the type of elements in the list
   * @return [item1, item2, ... ]
   */
  public static <T> String ppList(int[] indent, List<T> items) {
    return ppList(indent, items, 1000);
  }

  /**
   * @param indent
   *          the amount to use as indentation
   * @param items
   *          to print
   * @param max
   *          - maximum number of items to print
   * @param <T>
   *          the type of elements in the list
   * @return [item1, item2, ... ]
   */
  public static <T> String ppList(int[] indent, List<T> items, int max) {
    return addElementsToStringBuilder(items, max).toString();
  }

  /**
   * @param indent
   *          the amount to use as indentation
   * @param items
   *          to print
   * @param max
   *          - maximum number of items to print
   * @param appender
   *          - appender function
   * @param <T>
   *          the type of elements in the list
   * @return [item1, item2, ... ]
   */
  public static <T> String ppList(int[] indent, List<T> items, int max,
          BiConsumer<StringBuilder, T> appender) {
    return addElementsToStringBuilder(items, max, appender).toString();
  }

  /**
   * Convert a UIMA type name to a JCas class name (fully qualified) Normally this is the same, but
   * for two prefixes, it's slightly different
   * 
   * @param typeName
   *          the UIMA type name, fully qualified
   * @return the fully qualified JCas class name
   */
  public static String typeName2ClassName(String typeName) {
    if (typeName.startsWith(CAS.UIMA_CAS_PREFIX)) {
      return "org.apache.uima.jcas.cas." + typeName.substring(CAS.UIMA_CAS_PREFIX.length());
    }
    if (typeName.startsWith(CAS.UIMA_TCAS_PREFIX)) {
      return "org.apache.uima.jcas.tcas." + typeName.substring(CAS.UIMA_TCAS_PREFIX.length());
    }
    return typeName;
  }

  /**
   * Convert a JCas class name (fully qualified) to a UIMA type name Normally this is the same, but
   * for two prefixes, it's slightly different Also, class names for primitives (int, byte, etc. )
   * converted to int, byte, etc.
   * 
   * @param className
   *          the Java JCas class name for a UIMA type, fully qualified
   * @return the fully qualified UIMA Type name
   */
  public static String javaClassName2UimaTypeName(String className) {
    if (className.startsWith("org.apache.uima.jcas.cas.")
            && BuiltinTypeKinds.creatableBuiltinJCasClassNames.contains(className)) {
      return CAS.UIMA_CAS_PREFIX + className.substring("org.apache.uima.jcas.cas.".length());
    }
    if (className.startsWith("org.apache.uima.jcas.tcas.")
            && BuiltinTypeKinds.creatableBuiltinJCasClassNames.contains(className)) {
      return CAS.UIMA_TCAS_PREFIX + className.substring("org.apache.uima.jcas.tcas.".length());
    }

    switch (className) {
      case "boolean":
        return "uima.cas.Boolean";
      case "byte":
        return "uima.cas.Byte";
      case "short":
        return "uima.cas.Short";
      case "int":
        return "uima.cas.Integer";
      case "long":
        return "uima.cas.Long";
      case "float":
        return "uima.cas.Float";
      case "double":
        return "uima.cas.Double";
      case "java.lang.String":
        return "uima.cas.String";
      default:
        return className;
    }
  }

  public static void timeLoops(String title, int iterations, Runnable_withException r)
          throws Exception {
    long shortest = Long.MAX_VALUE;
    for (int i = 0; i < iterations; i++) {
      long startTime = System.nanoTime();
      r.run();
      long time = (System.nanoTime() - startTime) / 1000; // microseconds
      if (time < shortest) {
        shortest = time;
        System.out.format("%s: speed is %,d microseconds on iteration %,d%n", title, shortest, i);
      }
    }
  }

  public static void sleep(int milliseconds) {
    try {
      Thread.sleep(milliseconds);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  public static boolean maybeShrink(boolean secondTimeShrinkable, int size, int capacity,
          int factor, int minCapacity, IntConsumer realloc, Runnable reset) {

    if (size < (capacity >> factor)) {
      if (secondTimeShrinkable) {
        int newCapacity = Math.max(minCapacity, capacity >> 1);
        if (newCapacity < capacity) {
          realloc.accept(newCapacity);
        } else {
          reset.run();
        }
        return false;
      } else {
        reset.run();
        return true;
      }
    } else {
      reset.run();
      return false;
    }
  }

  // bad idea - the iterator would be tied to another one, not independent
  // public static <T> Iterable<T> iterable(Iterator<T> iterator) {
  // return new Iterable<T>() {
  // @Override
  // public Iterator<T> iterator() {
  // return iterator;
  // }
  // };
  // }

  public static boolean isJava9ea() {
    return isJava9ea;
  }

  /**
   * extract the slashified version of the fully qualified class name from the bytecode for a class
   * 
   * @param bytes
   *          the bytecode to extract from
   * @return the slashified class name eg. x/y/z/Myclass
   */
  public static String classNameFromByteCode(byte[] bytes) {
    ByteBuffer bb = ByteBuffer.wrap(bytes);
    int temp = (bb.getShort() & 0xffff); // constant at the beginning
    assert 0xCAFE == temp;
    temp = (bb.getShort() & 0xffff);
    assert 0xBABE == temp;
    bb.getInt(); // skip major/minor version

    int constantPoolCount = (bb.getShort() & 0xffff) - 1; // in bytecode "words"
    int[] classes = new int[constantPoolCount];
    String[] strings = new String[constantPoolCount];
    for (int i = 0; i < constantPoolCount; i++) {
      int tagByte = bb.get();
      switch (tagByte) {
        case 7:
          classes[i] = bb.getShort() & 0xffff;
          break;
        case 1:
          strings[i] = readModifiedUTF8(bb);
          break;
        case 5:
        case 6:
          bb.getLong();
          i++;
          /* skip next */ break;
        case 8:
          bb.getShort();
          break;
        default:
          bb.getInt();
      }
    }
    bb.getShort(); // go past access flags
    int indexIntoConstantPoolOfClassInfo = (bb.getShort() & 0xffff) - 1;
    return strings[classes[indexIntoConstantPoolOfClassInfo] - 1];
  }

  /**
   * read ByteBuffer modified UTF into String see
   * http://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.4
   */
  private static String readModifiedUTF8(ByteBuffer bb) {
    int len = bb.getShort() & 0xffff;
    StringBuilder sb = new StringBuilder();

    while (len > 0) {
      int c = bb.get() & 0xff;
      if ((c & 0x80) == 0) {
        sb.append((char) c);
        len--;
      } else if ((c & 0x60) == 0x40) {
        int r = (c & 0x1f) << 6;
        c = bb.get() & 0xff;
        assert 0x80 == (c & 0xc0);
        r = r | (bb.get() & 0x3f);
        sb.append((char) r);
        len -= 2;
      } else if ((c & 0xf0) == 0xe0) {
        int r = c & 0x0f << 6;
        c = bb.get() & 0xff;
        assert 0x80 == (c & 0xc0);
        r = (r | (c & 0x3f)) << 6;
        c = bb.get() & 0xff;
        assert 0x80 == (c & 0xc0);
        r = r | (c & 0x3f);
        sb.append((char) r);
        len -= 3;
      } else {
        internalError(new UnsupportedEncodingException());
      }
    }

    return sb.toString();
  }

  public static <T> List<T> setAsList(Set<T> set) {
    return new AbstractSequentialList<T>() {

      @Override
      public ListIterator<T> listIterator(int index) {
        Iterator<T> it = set.iterator();
        int[] i = { 0 };
        return new ListIterator<T>() {

          @Override
          public boolean hasNext() {
            return it.hasNext();
          }

          @Override
          public T next() {
            i[0]++;
            return it.next();
          }

          @Override
          public int nextIndex() {
            return i[0];
          }

          @Override
          public void remove() {
            it.remove();
          }

          @Override
          public boolean hasPrevious() {
            throw new UnsupportedOperationException();
          }

          @Override
          public T previous() {
            throw new UnsupportedOperationException();
          }

          @Override
          public int previousIndex() {
            throw new UnsupportedOperationException();
          }

          @Override
          public void set(T e) {
            throw new UnsupportedOperationException();
          }

          @Override
          public void add(T e) {
            throw new UnsupportedOperationException();
          }
        };
      }

      @Override
      public int size() {
        return set.size();
      }

    };
  }

  public static boolean contains(String[] strings, String item) {
    for (String string : strings) {
      if (Misc.equalStrings(string, item)) {
        return true;
      }
    }
    return false;
  }

  public static boolean contains(ClassLoader[] cls, ClassLoader cl) {
    for (ClassLoader item : cls) {
      if (item == cl) {
        return true;
      }
    }
    return false;
  }

  /**
   * Issues message at warning or fine level (fine if enabled, includes stack trace)
   * 
   * @param errorCount
   *          the count of errors used to decrease the frequency
   * @param message
   *          the message
   * @param logger
   *          the logger to use
   */
  public static void decreasingWithTrace(AtomicInteger errorCount, String message, Logger logger) {
    if (logger != null) {
      final int c = errorCount.incrementAndGet();
      final int cTruncated = Integer.highestOneBit(c);
      // log with decreasing frequency
      if (cTruncated == c) {
        if (logger.isLoggable(Level.FINE)) {
          try {
            throw new Throwable();
          } catch (Throwable e) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(baos);
            e.printStackTrace(ps);
            message = "Message count: " + c + "; " + message
                    + " Message count indicates messages skipped to avoid potential flooding.\n"
                    + baos.toString();
            logger.log(Level.FINE, message);
          }
        } else {
          message = "Message count: " + c + "; " + message
                  + " Message count indicates messages skipped to avoid potential flooding.";
          logger.log(Level.WARNING, message);
        }
      }
    }
  }

  public static void decreasingMessage(AtomicInteger errorCount, Supplier<String> messageSupplier,
          Consumer<String> publishMessage) {
    final int c = errorCount.incrementAndGet();
    final int cTruncated = Integer.highestOneBit(c); // 1, 2, 4, 8, etc.
    if (cTruncated == c) { // only happens for 1, 2, 4, etc
      String message = "Message count: " + c + "; " + messageSupplier.get()
              + " Message count indicates messages skipped to avoid potential flooding.";
      publishMessage.accept(message);
    }
  }
  // private static final Function<String, Class> uimaSystemFindLoadedClass;
  // static {
  // try {
  // MethodHandle mh = Misc.getProtectedMethodHandle(JCasImpl.class.getClassLoader().getClass(),
  // "findLoadedClass", String.class);
  // uimaSystemFindLoadedClass = (Function<String, Class>)LambdaMetafactory.metafactory(
  // lookup, // lookup context for the constructor
  // "apply", // name of the method in the Function Interface
  // methodType(Function.class), // signature of callsite, return type is functional interface, args
  // are captured args if any
  // MethodType.genericMethodType(1), // samMethodType signature and return type of method impl by
  // function object
  // mh, // method handle to call
  // methodType(Class.class, String.class)).getTarget().invokeExact();
  // } catch (Throwable e) {
  // throw new UIMARuntimeException(UIMARuntimeException.INTERNAL_ERROR, e);
  // }
  // }

  // public static void main(String[] args) {
  // foo();
  // // String s = "0123456789abcdef0123456789abcde";
  // for (int i = 3; i < 35; i++) {
  // System.out.print(Integer.toString(i) + " " + elide(s, i));
  // System.out.println("|");
  // }
  // }
  // private static void foo() {
  // System.out.format(" callers %n%s%ndone%n", getCallers(0, 2));
  //
  // }
  // System.out.println("should be false - not defined: " + getNoValueSystemProperty("foo"));
  // System.setProperty("foo", "");
  // System.out.println("should be true - defined, 0 len str value: " +
  // getNoValueSystemProperty("foo"));
  // System.setProperty("foo", "true");
  // System.out.println("should be true - defined, true value: " + getNoValueSystemProperty("foo"));
  // System.setProperty("foo", "zzz");
  // System.out.println("should be true - defined, zzz value: " + getNoValueSystemProperty("foo"));
  // System.setProperty("foo", "false");
  // System.out.println("should be false - defined, false value: " +
  // getNoValueSystemProperty("foo"));
  // }
}