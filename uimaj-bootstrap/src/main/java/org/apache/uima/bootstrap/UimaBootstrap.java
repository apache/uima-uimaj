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

package org.apache.uima.bootstrap;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;


/**
 * Run an arbitrary "main" method in a class, after adding classes to the classpath
 * that are specified as libraries, or as jar files
 * 
 *  Uses arguments: 
 *   -Dorg.apache.uima.jarpath=XXXX where XXXX is
 *    a string of file paths to directories connected using File.pathSeparator; each
 *    directory's contained JARs will be added to the class path.
 *    If the directory has no Jars, then it is put in the class path directly.
 *    
 *    The paths can also contain jar files.
 *    
 *    The paths added are added in an arbitrary order.
 *    The normal parent-first delegation is done
 *   
 *   The first argument is taken to be the name of the class to launch;
 *   that is passed the rest of the arguments. 
 *
 */
public class UimaBootstrap {

  private static boolean suppressClassPathDisplay;

  public static void main(String[] args) throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException, MalformedURLException, IOException, URISyntaxException {
    if (args == null || args.length == 0) {
      System.err.println("Usage: specify -Dorg.apache.uima.jarpath=XXXX, where");
      System.err.println("  XXXX is a string of file paths to directories or jar files, separated using the proper path separator character.");
      System.err.println("  For directories, all of the JARs found in these directories will be added to the classpath.");
      System.err.println("  If the directory has no Jars, then it is put in the class path directly.");
      System.err.println("  Normal \"parent-first\" delegation is done.");
      System.err.println("  The first argument is taken to be the name of the class whose \"main\" method will be called and passed the rest of the arguments.");
      System.err.println(" Set -DUimaBootstrapSuppressClassPathDisplay to suppress the display of the resulting classpath");
      
      System.exit(1);
    }    
    suppressClassPathDisplay = System.getProperty("UimaBootstrapSuppressClassPathDisplay") != null;
    URL[] urls = getUrls();
//    URLClassLoader cl = new ParentFirstWithResourceClassLoader(urls);
//    Thread.currentThread().setContextClassLoader(cl);
    addUrlsToSystemLoader(urls);
    
    Class<?> classToLaunch = null;
    try {
      classToLaunch = ClassLoader.getSystemClassLoader().loadClass(args[0]);
    } catch (ClassNotFoundException e) {
     System.err.println("Cannot find class to launch");
     System.exit(1);
    } 
    Method mainMethod = classToLaunch.getMethod("main", String[].class);
    int args2length = args.length - 1;
    String [] args2 = new String[args2length];
    System.arraycopy(args, 1, args2, 0, args2length);
    mainMethod.invoke(null, (Object)args2);
  }
  
  private static URL[] getUrls() throws MalformedURLException, IOException, URISyntaxException {
    String jps = System.getProperty("org.apache.uima.jarpath");
    if (null == jps) {
      System.err.println("Missing the -Dorg.apache.uima.jarpath=XXXX property");
      System.exit(1);
    }
    if (!suppressClassPathDisplay) {
      System.out.println("UimaBootstrap ClassPath:");
    }
    List<URL> urls = new ArrayList<URL>();
    String[] jpaths = jps.split(File.pathSeparator);
    for (String p : jpaths) {
      addUrlsFromPath(p, urls);
    }
    return urls.toArray(new URL[urls.size()]);
  }

  private static FilenameFilter jarFilter = new FilenameFilter() {
    public boolean accept(File dir, String name) {
      name = name.toLowerCase();
      return (name.endsWith(".jar"));
    }
  };
  
  private static void addUrlsFromPath(String p, List<URL> urls) throws MalformedURLException, IOException, URISyntaxException {
    // handle case where the path part is written x/y/z/*  by dropping the /* at the end
    // This is the form used by Java itself for classpath
    if (p.endsWith("*") && p.length() > 2 && p.charAt(p.length() - 2) == File.separatorChar) {
      p = p.substring(0, p.length() - 2);
    }
    File pf = new File(p);
    if (pf.isDirectory()) {
      File[] jars = pf.listFiles(jarFilter);
      if (jars.length == 0) {
        // this is the case where the user wants to include
        // a directory containing non-jar'd .class files
        add(urls, pf); 
      } else {
      for (File f : jars) {
        add(urls, f);
      }
      }
    } else if (p.toLowerCase().endsWith(".jar")) {
      add(urls, pf);
    }
  }
  
  private static void add(List<URL> urls, File cp) throws MalformedURLException {
    URL url = cp.toURI().toURL();
    if (!suppressClassPathDisplay) {
      System.out.format( " %s%n", url.toString());
    }
    urls.add(url);
  }

  private static void addUrlsToSystemLoader(URL[] urls) throws IOException {
    URLClassLoader systemClassLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
    try {
       Method method = URLClassLoader.class.getDeclaredMethod("addURL", new Class[]{URL.class});
       method.setAccessible(true); // is normally "protected"
       for (URL url : urls) {
         method.invoke(systemClassLoader, new Object[]{url});
       }
    } catch (Throwable t) {
       t.printStackTrace();
       throw new IOException("Error, could not add URL to system classloader");
    } 
  }
  
  
//  private static class ParentFirstWithResourceClassLoader extends URLClassLoader {
// 
//    /**
//     * Creates a new ParentFirstWithResourceClassLoader 
//     * 
//     * @param urls
//     *          an array of URLs representing JAR files
//     * 
//     * @throws MalformedURLException
//     *           if a malformed URL has occurred in the classpath string.
//     */
//    public ParentFirstWithResourceClassLoader(URL[] urls) {
//      super(urls);
//    }
//
//
//    @SuppressWarnings("unchecked")
//    protected synchronized Class loadClass(String name, boolean resolve)
//            throws ClassNotFoundException {
//      // First, check if the class has already been loaded
//      Class c = findLoadedClass(name);
//      if (c == null) {
//        // delegate class loading for class
//        try {
//          c = super.loadClass(name, false);
//        } catch (ClassNotFoundException e) {
//          // try to load class
//          c = findClass(name);
//        }
//      }
//      if (resolve) {
//        resolveClass(c);
//      }
//      return c;
//    }
//
//    // make sure resources are looked up first in this loader
//    // ASSUMES that getResourceAsStream calls getResource
////    @Override
////    public URL getResource(String resName) {
////      URL r = findResource(resName);
////      if (r != null) 
////        return r;
////      return super.getResource(resName);  
////    }    
//  } 
}
