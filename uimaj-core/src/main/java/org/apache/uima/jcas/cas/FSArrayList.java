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
// @formatter:off
/* Apache UIMA v3 - First created by JCasGen Fri Jan 20 11:55:59 EST 2017 */

package org.apache.uima.jcas.cas;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.RandomAccess;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.uima.UimaSerializableFSs;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.CommonArrayFS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.FeatureStructureImplC;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.impl.TypeSystemImpl;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.util.impl.Constants;


/** An expandable array of Feature Structures, implementing the ArrayList API.
 * Updated by JCasGen Fri Jan 20 11:55:59 EST 2017
 * XML source: C:/au/svnCheckouts/branches/uimaj/v3-alpha/uimaj-types/src/main/descriptors/java_object_type_descriptors.xml
 * @generated */

/**
 * <p>An ArrayList type containing Feature Structures, for UIMA
 *   <ul><li>Has all the methods of List
 *       <li>Implements the select(...) APIs 
 *   </ul>
 *   
 * <p>Implementation notes:
 *   <ul>
 *     <li>Uses UimaSerializable APIs
 *     <li>two implementations of the array list:
 *     <ul><li>one uses the original FSArray, via an asList wrapper
 *         <li>This is used until an add or remove operation;
 *         <li>switches to ArrayList, resetting the original FSArray to null
 *     </ul>
 *       
 *     <li>This enables operation without creating the Java Object in use cases of deserializing and
 *     referencing when updating is not being used.
 *     
 *     <li>The values stored internally are non-PEAR ones.
 *     <li>The get/set/add operations convert to/from PEAR ones as needed
 *   </ul>
 *
 * @param <T> the generic type
 */

public class FSArrayList <T extends TOP> extends TOP implements 
                         UimaSerializableFSs, CommonArrayFS<T>, SelectViaCopyToArray<T>, 
                         List<T>, RandomAccess, Cloneable {
 
  /** The Constant EMPTY_LIST. */
  private static final List<? extends TOP> EMPTY_LIST = Arrays.asList(Constants.EMPTY_TOP_ARRAY);

  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding") public static final String _TypeName = "org.apache.uima.jcas.cas.FSArrayList";
  
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding") public static final int typeIndexID = JCasRegistry.register(FSArrayList.class);
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding") public static final int type = typeIndexID;
  /** @generated
   * @return index of the type  
   */
  @Override
  public              int getTypeIndexID() {return typeIndexID;}
 
  /** lifecycle   - starts as empty array list   - becomes non-empty when updated (add)       -- used from that point on. */
  private final ArrayList<T> fsArrayList;
  
  /** lifecycle   - starts as the empty list   - set when _init_from_cas_data()   - set to null when update (add/remove) happens. */
  @SuppressWarnings("unchecked")
  private List<T> fsArray_asList = (List<T>) EMPTY_LIST;

  /* *******************
   *   Feature Offsets *
   * *******************/ 
   
  public static final String _FeatName_fsArray = "fsArray";


  /* Feature Adjusted Offsets */
//  public final static int _FI_fsArray = TypeSystemImpl.getAdjustedFeatureOffset("fsArray");
  private static final CallSite _FC_fsArray = TypeSystemImpl.createCallSiteForBuiltIn(FSArrayList.class, "fsArray");
  private static final MethodHandle _FH_fsArray = _FC_fsArray.dynamicInvoker();

   
  /** Never called.  Disable default constructor
   * @generated */
  protected FSArrayList() {
    fsArrayList = null;
  }
    
  /** Internal - constructor used by generator 
   * @generated
   * @param casImpl the CAS this Feature Structure belongs to
   * @param type the type of this Feature Structure 
   */
  public FSArrayList(TypeImpl type, CASImpl casImpl) {
    super(type, casImpl);
    if (CASImpl.traceFSs) { // tracing done after array setting, skipped in super class
      _casView.traceFSCreate(this);
    }
    fsArrayList = new ArrayList<>();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public FSArrayList(JCas jcas) {
    super(jcas);
    fsArrayList = new ArrayList<>();

    if (CASImpl.traceFSs) { // tracing done after array setting, skipped in super class
                            // because this impls CommonArrayFS
      _casView.traceFSCreate(this);
    }   
  } 

  /**
   * Make a new ArrayList with an initial size .
   *
   * @param jcas The JCas
   * @param length initial size
   */
  public FSArrayList(JCas jcas, int length) {
    super(jcas);
    _casView.validateArraySize(length);
    fsArrayList = new ArrayList<>(length);

    if (CASImpl.traceFSs) { // tracing done after array setting, skipped in super class
      _casView.traceFSCreate(this);
    }
  }     
 
    
  //*--------------*
  //* Feature: fsArray

  /** getter for fsArray - gets internal use - holds the contents
   * @generated
   * @return value of the feature 
   */
  private FSArray getFsArray() { return (FSArray)(_getFeatureValueNc(wrapGetIntCatchException(_FH_fsArray)));}
    
  /** setter for fsArray - sets internal use - holds the contents 
   * @generated
   * @param v value to set into the feature 
   */
  private void setFsArray(FSArray v) {
    _setFeatureValueNcWj(wrapGetIntCatchException(_FH_fsArray), v);
  }    
    
  /**
   * Maybe start using array list.
   */
  private void maybeStartUsingArrayList() {
    if (fsArray_asList != null) {
      fsArrayList.clear();
      fsArrayList.addAll(fsArray_asList);
      fsArray_asList = null;  // stop using this one
    }
  }
    
  /* (non-Javadoc)
   * @see org.apache.uima.UimaSerializable#_init_from_cas_data()
   */
  @Override
  public void _init_from_cas_data() {
    // special handling to have getter and setter honor pear trampolines
    final FSArray fsa = getFsArray();
    if (null == fsa) {
      fsArray_asList = Collections.emptyList();
    } else {
    
      fsArray_asList = new AbstractList<T>() {
        int i = 0;
        @Override
        public T get(int index) {  
          return (T) fsa.get_without_PEAR_conversion(i); 
        }
  
        @Override
        public int size() {
          return fsa.size();
        }

        /* (non-Javadoc)
         * @see java.util.AbstractList#set(int, java.lang.Object)
         */
        @Override
        public T set(int index, T element) {
          T prev = get(index);
          fsa.set_without_PEAR_conversion(index, element);
          return prev;
        } 
      };
    }
  }
  
  /* (non-Javadoc)
   * @see org.apache.uima.UimaSerializable#_save_to_cas_data()
   */
  @Override
  public void _save_to_cas_data() {
    // if fsArraysAsList is not null, then the cas data form is still valid, do nothing
    if (null != fsArray_asList) {
      return;
    }
    
    // reallocate fsArray if wrong size
    final int sz = size();
    FSArray fsa = getFsArray();
    if (fsa == null || fsa.size() != sz) {
      setFsArray(fsa = new FSArray(_casView.getJCasImpl(), sz));
    }
    
    //   in case fsa was preallocated and right size, may need journaling
    int i = 0;
    for (TOP fs : fsArrayList) {  // getting non-PEAR values
      TOP currentValue = fsa.get_without_PEAR_conversion(i);  
      if (currentValue != fs) {
        fsa.set_without_PEAR_conversion(i, fs); // done this way to record for journaling for delta CAS
      }
      i++;
    }
  }
  
  /**
   * gets either the array list object, or a list operating over the original FS Array.
   * Note: these forms will get/set the non Pear form of elements
   * @return the list
   */
  private List<T> gl () {
    return (null == fsArray_asList) 
      ? fsArrayList
      : fsArray_asList;
  }
  
  /**
   * Supports reading only, no update or remove 
   * @return a list backed by gl(), where the items are pear converted
   */
  private List<T> gl_read_pear(List<T> baseItems) {
    
    return new List<T>() {
      /*
       * @see java.lang.Iterable#forEach(java.util.function.Consumer)
       */
      @Override
      public void forEach(Consumer<? super T> action) {
        baseItems.forEach(item -> {
            T pearedItem = _maybeGetPearFs((T) item);
            action.accept(pearedItem);
        });
      }

      /*
       * @see java.util.List#size()
       */
      @Override
      public int size() {
        return baseItems.size();
      }

      /*
       * @see java.util.List#isEmpty()
       */
      @Override
      public boolean isEmpty() {
        return baseItems.isEmpty();
      }

      /*
       * @see java.util.List#contains(java.lang.Object)
       */
      @Override
      public boolean contains(Object o) {
        return (o instanceof TOP) 
                 ? baseItems.contains(_maybeGetBaseForPearFs((TOP)o))
                 : false;
      }

      /*
       * @see java.util.List#iterator()
       */
      @Override
      public Iterator<T> iterator() {
        return new Iterator<T>() {
          
          Iterator<T> outerIt = baseItems.iterator();

          /*
           * @see java.util.Iterator#hasNext()
           */
          @Override
          public boolean hasNext() {
            return outerIt.hasNext();
          }

          /*
           * @see java.util.Iterator#next()
           */
          @Override
          public T next() {
            return _maybeGetPearFs(outerIt.next());
          }

        };
      }

      /*
       * @see java.util.List#toArray()
       */
      @Override
      public Object[] toArray() {
        Object[] a = baseItems.toArray();
        FSArrayList.this._casView.swapInPearVersion(a);
        return a;
      }
      
      /*
       * @see java.util.List#toArray(java.lang.Object[])
       */
      @Override
      public <U> U[] toArray(U[] a) {
        U[] aa = baseItems.toArray(a);
        FSArrayList.this._casView.swapInPearVersion(aa);
        return aa;    
      }

      /*
       * @see java.util.List#add(java.lang.Object)
       */
      @Override
      public boolean add(T e) {
        throw new UnsupportedOperationException();
     }

      /*
       * @see java.util.List#remove(java.lang.Object)
       */
      @Override
      public boolean remove(Object o) {
        throw new UnsupportedOperationException();
      }

      /*
       * @see java.util.List#containsAll(java.util.Collection)
       */
      @Override
      public boolean containsAll(Collection<?> c) {
        for (Object item : c) {
          if (!contains(item)) {
            return false;
          }
        }
        return true;
      }

      /*
       * @see java.util.List#addAll(java.util.Collection)
       */
      @Override
      public boolean addAll(Collection<? extends T> c) {
        throw new UnsupportedOperationException();
      }

      /*
       * @see java.util.List#addAll(int, java.util.Collection)
       */
      @Override
      public boolean addAll(int index, Collection<? extends T> c) {
        throw new UnsupportedOperationException();
      }

      /*
       * @see java.util.List#removeAll(java.util.Collection)
       */
      @Override
      public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException();
      }

      /*
       * @see java.util.List#retainAll(java.util.Collection)
       */
      @Override
      public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
      }

      /*
       * @see java.util.List#replaceAll(java.util.function.UnaryOperator)
       */
      @Override
      public void replaceAll(UnaryOperator<T> operator) {
        for (int i = size() - 1; i >= 0; i--) {
          baseItems.set(i, _maybeGetBaseForPearFs(
              operator.apply(_maybeGetPearFs(baseItems.get(i)))));
        }
      }

      /*
       * @see java.util.Collection#removeIf(java.util.function.Predicate)
       */
      @Override
      public boolean removeIf(Predicate<? super T> filter) {
        throw new UnsupportedOperationException();
      }

      /*
       * @see java.util.List#sort(java.util.Comparator)
       */
      @Override
      public void sort(Comparator<? super T> c) {
        baseItems.sort((o1, o2) -> c.compare(_maybeGetPearFs(o1), _maybeGetPearFs(o2))); 
      }

      /*
       * @see java.util.List#clear()
       */
      @Override
      public void clear() {
        throw new UnsupportedOperationException();
      }

      /*
       * @see java.util.List#equals(java.lang.Object)
       */
      @Override
      public boolean equals(Object o) {
        return baseItems.equals(o);
      }

      /*
       * @see java.util.List#hashCode()
       */
      @Override
      public int hashCode() {
        return baseItems.hashCode();
      }

      /*
       * @see java.util.List#get(int)
       */
      @Override
      public T get(int index) {
        return _maybeGetPearFs(baseItems.get(index));
      }

      /*
       * @see java.util.List#set(int, java.lang.Object)
       */
      @Override
      public T set(int index, T element) {
        return baseItems.set(index, _maybeGetBaseForPearFs(element));
      }

      /*
       * @see java.util.List#add(int, java.lang.Object)
       */
      @Override
      public void add(int index, T element) {
        throw new UnsupportedOperationException();
      }

      /*
       * @see java.util.Collection#stream()
       */
      @Override
      public Stream<T> stream() {
        return baseItems.stream().map(item -> _maybeGetPearFs(item));
      }

      /*
       * @see java.util.List#remove(int)
       */
      @Override
      public T remove(int index) {
        throw new UnsupportedOperationException();
      }

      /*
       * @see java.util.Collection#parallelStream()
       */
      @Override
      public Stream<T> parallelStream() {
        return baseItems.parallelStream().map(item -> _maybeGetPearFs(item));
      }

      /*
       * @see java.util.List#indexOf(java.lang.Object)
       */
      @Override
      public int indexOf(Object o) {
        return baseItems.indexOf((o instanceof TOP) ? _maybeGetBaseForPearFs((TOP)o) : o);
      }

      /*
       * @see java.util.List#lastIndexOf(java.lang.Object)
       */
      @Override
      public int lastIndexOf(Object o) {
        return baseItems.lastIndexOf((o instanceof TOP) ? _maybeGetBaseForPearFs((TOP)o) : o);
      }

      /*
       * @see java.util.List#listIterator()
       */
      @Override
      public ListIterator<T> listIterator() {
        return listIterator(0);
      }

      /*
       * @see java.util.List#listIterator(int)
       */
      @Override
      public ListIterator<T> listIterator(int index) {
        return new ListIterator<T>() {
          
          ListIterator<T> baseIt = baseItems.listIterator(index);
          
          /*
           * @see java.util.ListIterator#hasNext()
           */
          @Override
          public boolean hasNext() {
            return baseIt.hasNext();
          }

          /**
           * @return
           * @see java.util.ListIterator#next()
           */
          @Override
          public T next() {
            return _maybeGetPearFs(baseIt.next());
          }

          /*
           * @see java.util.ListIterator#hasPrevious()
           */
          @Override
          public boolean hasPrevious() {
            return baseIt.hasPrevious();
          }

          /*
           * @see java.util.Iterator#forEachRemaining(java.util.function.Consumer)
           */
          @Override
          public void forEachRemaining(Consumer<? super T> action) {
            baseIt.forEachRemaining(item -> action.accept(_maybeGetPearFs(item)));
          }

          /**
           * @return
           * @see java.util.ListIterator#previous()
           */
          @Override
          public T previous() {
            return baseIt.previous();
          }

          /**
           * @return
           * @see java.util.ListIterator#nextIndex()
           */
          @Override
          public int nextIndex() {
            return baseIt.nextIndex();
          }

          /**
           * @return
           * @see java.util.ListIterator#previousIndex()
           */
          @Override
          public int previousIndex() {
            return baseIt.previousIndex();
          }

          /**
           * 
           * @see java.util.ListIterator#remove()
           */
          @Override
          public void remove() {
            throw new UnsupportedOperationException();
          }

          /**
           * @param e
           * @see java.util.ListIterator#set(java.lang.Object)
           */
          @Override
          public void set(T e) {
            baseIt.set(_maybeGetBaseForPearFs(e));
          }

          /**
           * @param e
           * @see java.util.ListIterator#add(java.lang.Object)
           */
          @Override
          public void add(T e) {
            throw new UnsupportedOperationException();
          }
        };
      }

      /*
       * @see java.util.List#subList(int, int)
       */
      @Override
      public List<T> subList(int fromIndex, int toIndex) {
        return gl_read_pear(baseItems.subList(fromIndex, toIndex));
      }

      /*
       * @see java.util.List#spliterator()
       */
      @Override
      public Spliterator<T> spliterator() {
        return FSArrayList.this._casView.makePearAware(baseItems.spliterator());
      }
    };
  }
  /* (non-Javadoc)
   * @see java.util.List#get(int)
   */
  @Override
  public T get(int i) {
    return _maybeGetPearFs(gl().get(i));
  }

  /**
   * updates the i-th value of the FSArrayList.
   *
   * @param i the i
   * @param v the v
   * @return the t
   */
  @Override
  public T set(int i, T v) {
    
    if (v != null && _casView.getBaseCAS() != v._casView.getBaseCAS()) {
      /** Feature Structure {0} belongs to CAS {1}, may not be set as the value of an array or list element in a different CAS {2}.*/
      throw new CASRuntimeException(CASRuntimeException.FS_NOT_MEMBER_OF_CAS, v, v._casView, _casView);
    }
    return _maybeGetPearFs(gl().set(i,  _maybeGetBaseForPearFs(v)));
  }
  
  /**
   *  return the size of the array.
   *
   * @return the int
   */
  @Override
  public int size() {
    return gl().size();
  }

  /**
   * Copy from array.
   *
   * @param src -
   * @param srcPos -
   * @param destPos -
   * @param length -
   * @param <E> the type of the source array being copied from
   * @see org.apache.uima.cas.ArrayFS#copyFromArray(FeatureStructure[], int, int, int)
   */
  public <E extends FeatureStructure> void copyFromArray(E[] src, int srcPos, int destPos, int length) {
    int srcEnd = srcPos + length;
    int destEnd = destPos + length;
    if (srcPos < 0 ||
        srcEnd > src.length ||
        destEnd > size()) {
      throw new ArrayIndexOutOfBoundsException(
          String.format("FSArrayList.copyFromArray, srcPos: %,d destPos: %,d length: %,d",  srcPos, destPos, length));
    }
    for (;srcPos < srcEnd && destPos < destEnd;) {
      set(destPos++, (T) src[srcPos++]);
    }
  }

  /**
   * Copy to array.
   *
   * @param srcPos -
   * @param dest -
   * @param destPos -
   * @param length -
   * @param <E> the type of the elements of the Array being copied into
   * @see org.apache.uima.cas.ArrayFS#copyToArray(int, FeatureStructure[], int, int)
   */
  public <E extends FeatureStructure> void copyToArray(int srcPos, E[] dest, int destPos, int length) {
    int srcEnd = srcPos + length;
    int destEnd = destPos + length;
    if (srcPos < 0 ||
        srcEnd > size() ||
        destEnd > dest.length) {
      throw new ArrayIndexOutOfBoundsException(
          String.format("FSArrayList.copyToArray, srcPos: %,d destPos: %,d length: %,d",  srcPos, destPos, length));
    }
    for (;srcPos < srcEnd && destPos < destEnd;) {
      dest[destPos++] = (E) get(srcPos++);
    }
  }

  /**
   * returns TOP[] because can't make array of T
   * Note: converts to pear trampolines.
   */
  @Override
  public TOP[] toArray() {
    TOP[] r = new TOP[size()];
    copyToArray(0, r, 0, size());
    return r;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.jcas.cas.SelectViaCopyToArray#_toArrayForSelect()
   */
  @Override
  public FeatureStructure[] _toArrayForSelect() { return toArray(); }

  /**
   * Not supported, will throw UnsupportedOperationException.
   *
   * @param src the src
   * @param srcPos the src pos
   * @param destPos the dest pos
   * @param length the length
   */
  @Override
  public void copyFromArray(String[] src, int srcPos, int destPos, int length) {
    throw new UnsupportedOperationException();
  }
    
  /**
   * Copies an array of Feature Structures to an Array of Strings.
   * The strings are the "toString()" representation of the feature structures.
   * If in Pear context, the Pear form is used. 
   * 
   * @param srcPos
   *                The index of the first element to copy.
   * @param dest
   *                The array to copy to.
   * @param destPos
   *                Where to start copying into <code>dest</code>.
   * @param length
   *                The number of elements to copy.
   * @exception ArrayIndexOutOfBoundsException
   *                    If <code>srcPos &lt; 0</code> or
   *                    <code>length &gt; size()</code> or
   *                    <code>destPos + length &gt; destArray.length</code>.
   */
  @Override
  public void copyToArray(int srcPos, String[] dest, int destPos, int length) {
    _casView.checkArrayBounds(size(), srcPos, length);
    int i = 0;
    for (T fs : this) {
      dest[i + destPos] = (fs == null) ? null : fs.toString();
      i++;
    }
  }
  
  /* 
   * 
   * (non-Javadoc)
   * @see org.apache.uima.jcas.cas.CommonArray#copyValuesFrom(org.apache.uima.jcas.cas.CommonArray)
   * The spliterators return PEAR objects, potentially.  The add operation expects PEAR objects,
   * and converts them to base when storing.
   */
  @Override
  public void copyValuesFrom(CommonArrayFS v) {
    clear();
    Spliterator<T> si;
    if (v instanceof FSArrayList) {
      si = ((FSArrayList<T>) v).spliterator();
    } else if (v instanceof FSArray) {
      si = (Spliterator<T>) ((FSArray)v).spliterator();
    } else {
      throw new ClassCastException("argument must be of class FSArray or FSArrayList");
    } 
    
    si.forEachRemaining(fs -> add(fs));
  }
  
  /**
   * Convenience - create a FSArrayList from an existing Array.
   *
   * @param <E> generic type of returned FS
   * @param <F> generic type of the elements of the array argument
   * @param jcas -
   * @param a -
   * @return -
   */
  public static <E extends TOP, F extends FeatureStructure> FSArrayList<E> create(JCas jcas, F[] a) {
    FSArrayList<E> fsa = new FSArrayList<>(jcas, a.length);
    fsa.copyFromArray(a, 0, 0, a.length);  // does pear and journaling actions as needed
    return fsa;
  }
  
  /* (non-Javadoc)
   * @see org.apache.uima.UimaSerializable#_superClone()
   */
  @Override
  public FeatureStructureImplC _superClone() {return clone();}  // enable common clone
  
  /*
   * @see java.util.AbstractCollection#containsAll(java.util.Collection)
   */
  @Override
  public boolean containsAll(Collection<?> c) {
    return gl_read_pear(gl()).containsAll(c);
  }

  /*
   * @see java.util.ArrayList#isEmpty()
   */
  @Override
  public boolean isEmpty() {
    return gl().isEmpty();
  }

  /*
   * @see java.util.ArrayList#contains(java.lang.Object)
   */
  @Override
  public boolean contains(Object o) {
    if (!(o instanceof TOP)) return false;
    TOP fs = (TOP) o;    
    return gl().contains(_maybeGetBaseForPearFs(fs));
  }

  /*
   * @see java.util.ArrayList#indexOf(java.lang.Object)
   */
  @Override
  public int indexOf(Object o) {
    if (!(o instanceof TOP)) return -1;
    TOP fs = (TOP) o;    
    return gl().indexOf(_maybeGetBaseForPearFs(fs));
  }

  /*
   * @see java.util.ArrayList#lastIndexOf(java.lang.Object)
   */
  @Override
  public int lastIndexOf(Object o) {
    if (!(o instanceof TOP)) return -1;
    TOP fs = (TOP) o;    
    return gl().lastIndexOf(_maybeGetBaseForPearFs(fs));
  }

  /*
   * @see java.util.ArrayList#toArray(java.lang.Object[])
   */
  @Override
  public <U> U[] toArray(U[] a) {    
    return gl_read_pear(gl()).toArray(a);
  }


  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    final int maxLen = 10;
    return "FSArrayList [size="
        + size()
        + ", fsArrayList="
        + (fsArrayList != null ? fsArrayList.subList(0, Math.min(fsArrayList.size(), maxLen))
            : null)
        + ", fsArray_asList=" + (fsArray_asList != null
            ? fsArray_asList.subList(0, Math.min(fsArray_asList.size(), maxLen)) : null)
        + "]";
  }

  /*
   * @see java.util.ArrayList#add(java.lang.Object)
   */
  @Override
  public boolean add(T e) {
    maybeStartUsingArrayList();
    return fsArrayList.add(_maybeGetBaseForPearFs(e));
  }

  /*
   * @see java.util.AbstractList#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object o) {
    if (!(o instanceof FSArrayList)) return false;
    FSArrayList<T> other = (FSArrayList<T>) o;
    if (size() != other.size()) return false;
    
    List<T> items = gl();  // non-pear form
    List<T> other_items = other.gl(); // non-pear form
    
    Iterator<T> it_other = other_items.iterator();
    for (T item : items) {
      if (!item.equals(it_other.next())) return false;
    }
    return true;
  }

  /*
   * @see java.util.ArrayList#add(int, java.lang.Object)
   */
  @Override
  public void add(int index, T element) {
    maybeStartUsingArrayList();
    fsArrayList.add(index, _maybeGetBaseForPearFs(element));
  }

  /*
   * @see java.util.ArrayList#remove(int)
   */
  @Override
  public T remove(int index) {
    maybeStartUsingArrayList();
    return fsArrayList.remove(index);
  }

  /*
   * @see java.util.ArrayList#remove(java.lang.Object)
   */
  @Override
  public boolean remove(Object o) {
    maybeStartUsingArrayList();
    if (!(o instanceof TOP)) {
      return false;
    }
    return fsArrayList.remove(_maybeGetBaseForPearFs((TOP) o));
  }

  /*
   * want hashcode to depend only on equal items, regardless of what format.
   *
   * @see java.util.AbstractList#hashCode()
   */
  @Override
  public int hashCode() {
    int hc = 1;
    final int prime = 31;
    for (T item : gl()) {  // non pear form
      hc = hc * prime + item.hashCode();
    }
    return hc;
  }

  /*
   * @see java.util.ArrayList#clear()
   */
  @Override
  public void clear() {
    maybeStartUsingArrayList();
    fsArrayList.clear();
  }

  /*
   * @see java.util.ArrayList#addAll(java.util.Collection)
   */
  @Override
  public boolean addAll(Collection<? extends T> c) {
    return addAll(size(), c);
  }

  /*
   * @see java.util.ArrayList#addAll(int, java.util.Collection)
   */
  @Override
  public boolean addAll(int index, Collection<? extends T> c) {
    if (c.size() == 0) {
      return false;
    }
    maybeStartUsingArrayList();
//    return fsArrayList.addAll(index, c); // doesn't do pear conversion
    fsArrayList.ensureCapacity(fsArrayList.size() + c.size());
    List<T> baseItems = c.stream().map(item -> 
        _maybeGetBaseForPearFs(item)).collect(Collectors.toList());
    fsArrayList.addAll(index, baseItems); 
    return true;
  }

  /*
   * @see java.util.ArrayList#removeAll(java.util.Collection)
   */
  @Override
  public boolean removeAll(Collection<?> c) {
    boolean changed = false;
    maybeStartUsingArrayList();
//    return fsArrayList.removeAll(c); // doesn't do pear conversion
    for (Object item : c) {
      if (!(item instanceof TOP)) {
        continue;
      }
                // order important! 
      changed = fsArrayList.remove(_maybeGetBaseForPearFs((TOP)item)) || changed;
    }
    return changed;
  }

  /*
   * @see java.util.ArrayList#retainAll(java.util.Collection)
   */
  @Override
  public boolean retainAll(Collection<?> c) {
    Collection<?> cc = _casView.collectNonPearVersions(c);
    maybeStartUsingArrayList();    
    return fsArrayList.retainAll(cc);
  }

  /*
   * @see java.util.Collection#stream()
   */
  @Override
  public Stream<T> stream() {
    return gl().stream().map(item -> _maybeGetPearFs(item));
  }

  /*
   * @see java.util.Collection#parallelStream()
   */
  @Override
  public Stream<T> parallelStream() {
    return gl().parallelStream().map(item -> _maybeGetPearFs(item));
  }

  /*
   * @see java.util.ArrayList#listIterator(int)
   */
  @Override
  public ListIterator<T> listIterator(int index) {
    return gl_read_pear(gl()).listIterator(index);
  }

  /*
   * @see java.util.ArrayList#listIterator()
   */
  @Override
  public ListIterator<T> listIterator() {
    return listIterator(0);
  }

  /*
   * @see java.util.ArrayList#iterator()
   */
  @Override
  public Iterator<T> iterator() {
    return gl_read_pear(gl()).iterator();
  }

  /*
   * @see java.util.ArrayList#subList(int, int)
   */
  @Override
  public List<T> subList(int fromIndex, int toIndex) {
    return gl_read_pear(gl().subList(fromIndex, toIndex));
  }

  /*
   * @see java.util.ArrayList#forEach(java.util.function.Consumer)
   */
  @Override
  public void forEach(Consumer<? super T> action) {
    gl_read_pear(gl()).forEach(action);
  }

  /*
   * @see java.util.ArrayList#spliterator()
   */
  @Override
  public Spliterator<T> spliterator() {
    return gl_read_pear(gl()).spliterator();
  }

  /*
   * @see java.util.ArrayList#removeIf(java.util.function.Predicate)
   */
  @Override
  public boolean removeIf(Predicate<? super T> filter) {
    maybeStartUsingArrayList();
    return fsArrayList.removeIf(item -> filter.test(_maybeGetPearFs(item)));
  }

  /*
   * @see java.util.List#replaceAll(java.util.function.UnaryOperator)
   */
  @Override
  public void replaceAll(UnaryOperator<T> operator) {
    gl_read_pear(gl()).replaceAll(operator);
  }

  /*
   * @see java.util.ArrayList#sort(java.util.Comparator)
   */
  @Override
  public void sort(Comparator<? super T> c) {
    gl_read_pear(gl()).sort(c);
  }
      
}
