package org.apache.uima.cas.impl;

import java.util.Comparator;
import java.util.ConcurrentModificationException;

import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FeatureStructure;

public abstract class FsIterator_singletype<T extends FeatureStructure>
                    implements LowLevelIterator<T>, 
                               Comparable<FsIterator_singletype<T>> {

  private int modificationSnapshot; // to catch illegal modifications

  /**
   * This is a ref to the shared value in the FSIndexRepositoryImpl
   * OR it may be null which means skip the checking (done for some internal routines
   * which know they are not updating the index, and assume no other thread is)
   */
  final protected int[] detectIllegalIndexUpdates; // shared copy with Index Repository

  protected final int typeCode;  // needed for the detect concurrent modification
  
  /**
   * The generic type is FeatureStructure to allow comparing between
   * an instance of T and some other template type which can be a supertype of T, as long as
   * the keys are defined in both.
   */
  final protected Comparator<FeatureStructure> comparator;

  public FsIterator_singletype(int[] detectConcurrentMods, int typeCode, Comparator<FeatureStructure> comparator){
    this.comparator = comparator;
    this.detectIllegalIndexUpdates = detectConcurrentMods;
    this.typeCode = typeCode;
    resetConcurrentModification();
    // subtypes do moveToFirst after they finish initialization
  }
    
  protected <I extends FSIterator<T>> I checkConcurrentModification() {
    if ((null != detectIllegalIndexUpdates) && (modificationSnapshot != detectIllegalIndexUpdates[typeCode])) {
      throw new ConcurrentModificationException();
    }
    return (I) this;
  }
  
  protected void resetConcurrentModification() {  
    this.modificationSnapshot = (null == this.detectIllegalIndexUpdates) ? 0 : this.detectIllegalIndexUpdates[typeCode];
  }

  @Override
  public int compareTo(FsIterator_singletype<T> o) {
    if (comparator != null) {
      return comparator.compare(this.get(), o.get());
    } 
    return Integer.compare(this.get().id()(), o.get().id()());
  }
   
  @Override
  public abstract FsIterator_singletype<T> copy();
}
