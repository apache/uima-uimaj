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
package org.apache.uima.jcas;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

import org.apache.uima.jcas.cas.TOP;

//@formatter:off
/**
 * Maintains a registry of JCas cover classes that have been loaded in order to be able to assign a
 * unique low-value positive int index to each loaded JCas class. Note that the same JCas class
 * loaded under two different class loaders may get two different numbers.
 * 
 * The internals maintain a weak reference to the loaded class in order to allow the index value to
 * be reused if the associated JCas class is garbaged collected.
 * 
 * This could happen if the JCas classes are loaded under a class loader which is a child of the
 * class loader of this framework class, and that child classloader later gets GC'd.
 * 
 * The register method is called from JCas cover class static initialization and returns the unique
 * index value for this class.
 * 
 * The associated int index is used in a lookup on a jcas registry array associated with a particular
 * type system, to get the associated Type.  This supports the use cases of
 *   - different type systems being used with the same loaded JCas classes, either
 *      -- sequentially by a single instance of UIMA or
 *      -- multiple instances of pipelines running in one JVM each with different type systems 
 */
//@formatter:on
public class JCasRegistry {

  private JCasRegistry() {
  }; // never instantiated.

  // @formatter:off
  /**
   * A WeakReference class holding 
   *   - a ref to a JCas class 
   *   - an assigned int for that class 
   */
  // @formatter:on
  private static class WeakRefInt<T> extends WeakReference<T> {
    int index;

    WeakRefInt(T item, ReferenceQueue<? super T> q, int index) {
      super(item, q);
      this.index = index;
    }
  }

  /**
   * The <type> argument say the type is a class, which extends TOP
   */
  private static final ArrayList<WeakRefInt<Class<? extends TOP>>> loadedJCasClasses = new ArrayList<>();
  private static final ReferenceQueue<Class<? extends TOP>> releasedQueue = new ReferenceQueue<>();

  // private static int nextFeatureIndex = 0;
  // /**
  // * accessed under class lock
  // */
  // final private static Deque<Integer> availableFeatureIndexes = new ArrayDeque<>();

  /**
   * Registers a JCas cover class with this registry. The registry will assign it a unique index,
   * which is then used by the cover-class to identify itself to the JCas implementation.
   * 
   * Before adding the class to the array list, see if there are any "free" slots in the array list
   * 
   * @param aJCasCoverClass
   *          the class to register
   * @return the unique index value for this class.
   */
  public static synchronized int register(Class<? extends TOP> aJCasCoverClass) {
    WeakRefInt<Class<? extends TOP>> releasedWeakRefInt = (WeakRefInt<Class<? extends TOP>>) releasedQueue
            .poll();

    if (releasedWeakRefInt != null) {
      int i = releasedWeakRefInt.index; // an index number that can be reused

      // IntListIterator it = releasedWeakRef.featureIndexes.iterator();
      // while (it.hasNext()){
      // availableFeatureIndexes.addLast(it.next());
      // }

      loadedJCasClasses.set(i, new WeakRefInt<>(aJCasCoverClass, releasedQueue, i));
      return i;
    }

    int i = loadedJCasClasses.size();
    loadedJCasClasses.add(new WeakRefInt<>(aJCasCoverClass, releasedQueue, i));
    return i;
  }

  /**
   * For a particular type, return true if that type should have run-time checking for use of fields
   * defined in the JCas Model which are not present in the CAS. If false, all fields in the JCas
   * must be in the CAS type system at instantiation time, or an exception is thrown; this allows
   * the runtime to skip this test.
   * <p>
   * This is reserved for future use; it currently always returns true.
   * 
   * @param fullyQualTypeName
   *          fully qualified type name
   * @return true if that type should have run-time checking for use of fields defined in the JCas
   *         Model which are not present in the CAS. If false, all fields in the JCas must be in the
   *         CAS type system at instantiation time, or an exception is thrown; this allows the
   *         runtime to skip this test.
   */
  public static boolean getFeatOkTst(String fullyQualTypeName) {
    return true;
  }

  /**
   * NOT CURRENTLY USED
   * 
   * Gets the number of cover classes that have been registered.
   * 
   * @return the number of registered JCas cover classes
   */
  public static synchronized int getNumberOfRegisteredClasses() {
    return loadedJCasClasses.size();
  }

//@formatter:off
  /**
   * Used for error message:
   *   When a particular loaded type system is missing the type that corresponds to a loaded JCas class
   *     (perhaps that class was loaded when another type system was being used, or 
   *      it was just referred to in Java code (which causes it to be loaded)
   *   then the error message uses this to get the class to be able to print the class name
   *   
   * Gets the JCas cover class for a given index.
   * 
   * @param aIndex
   *          the index
   * 
   * @return the JCas cover class that was assigned the value <code>aIndex</code> during its
   *         registration, <code>null</code> if none.
   */
//@formatter:on
  public static synchronized Class<? extends TOP> getClassForIndex(int aIndex) {
    if (aIndex >= 0 && aIndex < loadedJCasClasses.size())
      return loadedJCasClasses.get(aIndex).get();
    else
      return null;
  }
}
