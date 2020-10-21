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
   
/* Apache UIMA v3 - First created by JCasGen Fri Jan 20 11:55:59 EST 2017 */

package org.apache.uima.jcas.cas;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.RandomAccess;
import java.util.Set;
import java.util.Spliterator;

import org.apache.uima.UimaSerializableFSs;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.FeatureStructureImplC;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.impl.TypeSystemImpl;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.util.impl.Constants;


/** a hash set of Feature Structures
 * Is Pear aware - stores non-pear versions but may return pear version in pear contexts
 * Updated by JCasGen Fri Jan 20 11:55:59 EST 2017
 * XML source: C:/au/svnCheckouts/branches/uimaj/v3-alpha/uimaj-types/src/main/descriptors/java_object_type_descriptors.xml
 * @generated */
public class FSHashSet <T extends TOP> extends TOP implements 
                            UimaSerializableFSs, SelectViaCopyToArray, 
                            Set<T>, RandomAccess, Cloneable {
  
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static String _TypeName = "org.apache.uima.jcas.cas.FSHashSet";
  
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(FSHashSet.class);
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int type = typeIndexID;
  /** @generated
   * @return index of the type  
   */
  @Override
  public              int getTypeIndexID() {return typeIndexID;}
 
  /** lifecycle   
   *   - starts as empty array list   
   *   - becomes non-empty when updated (add)       
   *   -- used from that point on. */
  
  private boolean isPendingInit = false;
  private boolean isSaveNeeded = false;
  
  private final HashSet<T> fsHashSet; // not set here to allow initial size version

  /* *******************
   *   Feature Offsets *
   * *******************/ 
   
  public final static String _FeatName_fsArray = "fsArray";


  /* Feature Adjusted Offsets */
//  public final static int _FI_fsArray = TypeSystemImpl.getAdjustedFeatureOffset("fsArray");
  private final static CallSite _FC_fsArray = TypeSystemImpl.createCallSiteForBuiltIn(FSHashSet.class, "fsArray");
  private final static MethodHandle _FH_fsArray = _FC_fsArray.dynamicInvoker();

   
  /** Never called.  Disable default constructor
   * @generated */
  protected FSHashSet() {
    fsHashSet = null;
  }
    
  /** Internal - constructor used by generator 
   * @generated
   * @param casImpl the CAS this Feature Structure belongs to
   * @param type the type of this Feature Structure 
   */
  public FSHashSet(TypeImpl type, CASImpl casImpl) {
    this(new HashSet<>(), type, casImpl);
  }
  
  public FSHashSet(HashSet<T> set, TypeImpl type, CASImpl casImpl) {
    super(type, casImpl);
    fsHashSet = set;

    if (CASImpl.traceFSs) { // tracing done after array setting, skipped in super class
      _casView.traceFSCreate(this);
    }    
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public FSHashSet(JCas jcas) {
    this(new HashSet<>(), jcas);
  } 

  public FSHashSet(HashSet<T> set, JCas jcas) {
    super(jcas);
    fsHashSet = set;

    if (CASImpl.traceFSs) { // tracing done after array setting, skipped in super class
      _casView.traceFSCreate(this);
    }   
  } 

  /**
   * Make a new HashSet with an initial size .
   *
   * @param jcas The JCas
   * @param length initial size
   */
  public FSHashSet(JCas jcas, int length) {
    this (new HashSet<>(length), jcas, length);
  }
    
  public FSHashSet(HashSet<T> set, JCas jcas, int length) {
    super(jcas);
    _casView.validateArraySize(length);
    fsHashSet = set;

    if (CASImpl.traceFSs) { // tracing done after array setting, skipped in super class
      _casView.traceFSCreate(this);
    }
  }
  
  //*--------------*
  //* Feature: fsArray

  /** getter for fsArray - gets internal use - holds the set of Feature Structures
   * @generated
   * @return value of the feature 
   */
  private FSArray<T> getFsArray() { return (FSArray<T>)(_getFeatureValueNc(wrapGetIntCatchException(_FH_fsArray)));}
    
  /** setter for fsArray - sets internal use - holds the set of Feature Structures 
   * @generated
   * @param v value to set into the feature 
   */
  private void setFsArray(FSArray<T> v) {
    _setFeatureValueNcWj(wrapGetIntCatchException(_FH_fsArray), v);
  }    
    
  /* (non-Javadoc)
   * @see org.apache.uima.UimaSerializable#_init_from_cas_data()
   */
  @Override
  public void _init_from_cas_data() {
    isPendingInit = true;
  }

  private void maybeLazyInit() {
    if (isPendingInit) {
      lazyInit();
    }
  }
  
  private void lazyInit() {  
    isPendingInit = false;
    fsHashSet.clear();
    FSArray<T> a = getFsArray();

    for (T fs : a) {
      fsHashSet.add((T) fs);
    }
  }
    
  /* (non-Javadoc)
   * @see org.apache.uima.UimaSerializable#_save_to_cas_data()
   */
  @Override
  public void _save_to_cas_data() {
    if (isSaveNeeded) {
      isSaveNeeded = false;
      FSArray<T> fsa = getFsArray();
      if (fsa == null || fsa.size() != fsHashSet.size()) {
        fsa = new FSArray(_casView.getJCasImpl(), fsHashSet.size());
        setFsArray(fsa);
      }
 
      // using element by element instead of bulk operations
      //   in case fsa was preallocated and right size, may need journaling
      
      int i = 0;
      for (TOP fs : fsHashSet) {
        TOP currentValue = fsa.get(i);
        if (currentValue != fs) {
          fsa.set_without_PEAR_conversion(i, fs); // done this way to record for journaling for delta CAS
        }
        i++;
      }
    }
  }

  /* (non-Javadoc)
   * @see org.apache.uima.jcas.cas.SelectViaCopyToArray#_toArrayForSelect()
   */
  @Override
  public T[] _toArrayForSelect() {
    return toArray(); 
  }

  /* (non-Javadoc)
   * @see org.apache.uima.UimaSerializable#_superClone()
   */
  @Override
  public FeatureStructureImplC _superClone() { return clone();}  // enable common clone
  
  private TOP[] gta() {
    FSArray fsa = getFsArray();
    if (null == fsa) {
      return Constants.EMPTY_TOP_ARRAY;
    }
    return fsa._getTheArray();
  }
  
  /*
   * @see java.util.AbstractSet#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object o) {
    if (!(o instanceof FSHashSet)) return false;
    FSHashSet<?> other = (FSHashSet<?>) o;
    if (size() != other.size()) return false;
    if (size() == 0) return true;
    
    maybeLazyInit();
    other.maybeLazyInit();
    
    return fsHashSet.equals(other.fsHashSet);
  }

  /*
   * @see java.util.AbstractSet#hashCode()
   */
  @Override
  public int hashCode() {
    // hash code needs to be the same for both styles
    // fsHashSet adds element hashcodes, Arrays combines using 31*.
    
//    return isSaveNeeded
//        ? fsHashSet.hashCode()    // no good - hash codes different
//        : Arrays.hashCode(gta());
    maybeLazyInit();
    return fsHashSet.hashCode();
  }

  /*
   * @see java.util.AbstractCollection#toArray()
   */
  @Override
  public T[] toArray() {
    if (isSaveNeeded) {
      T[] r = (T[]) new TOP[size()];
      fsHashSet.toArray(r);
      
      return r;
    }
    return (T[]) Arrays.copyOf(gta(), gta().length);
  }

  /*
   * @see java.util.AbstractSet#removeAll(java.util.Collection)
   */
  @Override
  public boolean removeAll(Collection<?> c) {
    maybeLazyInit();
    boolean r = fsHashSet.removeAll(c); 
    if (r) isSaveNeeded = true;
    return r;
  }

  /*
   * @see java.util.AbstractCollection#toArray(Object[])
   */
  @Override
  public <N> N[] toArray(N[] a) {
    if (isSaveNeeded) {
      N[] aa = fsHashSet.toArray(a);
      _casView.swapInPearVersion(aa);
      return aa;
    }
    
    final int sz = size();
    if (a.length < sz) {
      a = (N[]) Array.newInstance(a.getClass().getComponentType(), sz);
    }
    
    TOP[] d = gta();
    System.arraycopy(d, 0, a, 0, d.length);
    _casView.swapInPearVersion(a);
    return a;
  }

  /*
   * @see java.util.HashSet#iterator()
   */
  @Override
  public Iterator<T> iterator() {
    if (size() == 0) {
      return Collections.emptyIterator();
    }
    
    return new Iterator<T>() {

      final private Iterator<T> baseIt = isSaveNeeded 
          ? fsHashSet.iterator()
          : gtaIterator();

      @Override
      public boolean hasNext() {
        return baseIt.hasNext();
      }

      @Override
      public T next() {
        return _maybeGetPearFs(baseIt.next());
      }      
    };
  }
  
  /**
   * 
   * @return iterator over non-pear versions
   */
  private Iterator<T> gtaIterator() {
    return (Iterator<T>) getFsArray().iterator(); 
  }

  /**
   * Size.
   *
   * @return the int
   * @see java.util.HashSet#size()
   */
  @Override
  public int size() {
    return isSaveNeeded 
        ? fsHashSet.size()
        : gta().length;
  }

  /**
   * Checks if is empty.
   *
   * @return true, if is empty
   * @see java.util.HashSet#isEmpty()
   */
  @Override
  public boolean isEmpty() {
    return size() == 0;
  }

  /**
   * Contains.
   *
   * @param o the o
   * @return true, if successful
   * @see java.util.HashSet#contains(java.lang.Object)
   */
  @Override
  public boolean contains(Object o) {
    maybeLazyInit();
    return fsHashSet.contains((o instanceof TOP) ? _maybeGetBaseForPearFs((TOP)o) : o);
  }

  /**
   * Adds the element to the set.
   *
   * @param e the element to add
   * @return true, if the set didn't already contain this element
   * @see java.util.HashSet#add(java.lang.Object)
   */
  @Override
  public boolean add(T e) {
    maybeLazyInit();
    boolean r = fsHashSet.add(_maybeGetBaseForPearFs(e)); 
    if (r) isSaveNeeded = true;
    return r;
  }

  /**
   * Removes the element.
   *
   * @param o the o
   * @return true, if the set contained the element
   * @see java.util.HashSet#remove(java.lang.Object)
   */
  @Override
  public boolean remove(Object o) {
    maybeLazyInit();
    boolean r = fsHashSet.remove((o instanceof TOP) ? _maybeGetBaseForPearFs((TOP)o) : o);
    if (r) isSaveNeeded = true;
    return r;
  }

  /**
   * Clear.
   *
   * @see java.util.HashSet#clear()
   */
  @Override
  public void clear() {
    if (size() == 0) return;
    maybeLazyInit();
    isSaveNeeded = true;
    fsHashSet.clear();
  }

  /**
   * Contains all.
   *
   * @param c the c
   * @return true, if set contains all of the elements in c
   * @see java.util.AbstractCollection#containsAll(java.util.Collection)
   */
  @Override
  public boolean containsAll(Collection<?> c) {
    maybeLazyInit();
    for (Object o : c) {
      if (!contains(o)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Adds all the elements .
   *
   * @param c the c
   * @return true, if set changed
   * @see java.util.AbstractCollection#addAll(java.util.Collection)
   */
  @Override
  public boolean addAll(Collection<? extends T> c) {
    if (c.size() == 0) {
      return false;
    }
    
    ArrayList<T> a = new ArrayList<>(c.size());
    for (T item : c) {
      a.add(_maybeGetBaseForPearFs(item));
    }
    maybeLazyInit();
    boolean r = fsHashSet.addAll(a);
    if (r) isSaveNeeded = true;
    return r;
  }

  /**
   * Spliterator.
   *
   * @return the spliterator
   * @see java.util.HashSet#spliterator()
   */
  @Override
  public Spliterator<T> spliterator() {
    Spliterator<T> baseSi =  isSaveNeeded
        ? fsHashSet.spliterator()
        : (Spliterator<T>) Arrays.asList(gta()).spliterator();
        
    return _casView.makePearAware(baseSi);   
  }

  /**
   * Retain all.
   *
   * @param c the c
   * @return true, if collection changed
   * @see java.util.AbstractCollection#retainAll(java.util.Collection)
   */
  @Override
  public boolean retainAll(Collection<?> c) {
    if (c.size() == 0) {
      boolean wasNotEmpty = !isEmpty();
      clear();
      return wasNotEmpty;
    }
    
    Collection<?> cc = _casView.collectNonPearVersions(c);
    maybeLazyInit();
    boolean r = fsHashSet.retainAll(cc);
    if (r) isSaveNeeded = true;
    return r;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    final int maxLen = 10;
    return "FSHashSet [isPendingInit=" + isPendingInit + ", isSaveNeeded=" + isSaveNeeded
        + ", fsHashSet=" + (fsHashSet != null ? toString(fsHashSet, maxLen) : null) + "]";
  }

  /**
   * To string.
   *
   * @param collection the collection
   * @param maxLen the max len
   * @return the string
   */
  private String toString(Collection<?> collection, int maxLen) {
    StringBuilder builder = new StringBuilder();
    builder.append("[");
    int i = 0;
    for (Iterator<?> iterator = collection.iterator(); iterator.hasNext() && i < maxLen; i++) {
      if (i > 0)
        builder.append(", ");
      builder.append(iterator.next());
    }
    builder.append("]");
    return builder.toString();
  }

}

    