package org.apache.uima.jcas.cas;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.SelectFSs;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.SelectFSs_impl;

/** 
 * Classes which provide a toArray() method that returns 
 * a FeatureStructure[] can implement this to enable the 
 * class to be used as a "select" source
 */



public interface SelectViaCopyToArray {
  
  FeatureStructure[] toArray();
  CASImpl _getView(); 
  
  default <T extends FeatureStructure> SelectFSs_impl<T> select() {
    return new SelectFSs_impl<T>(toArray(), this._getView());
  }

  /**
   * Treat an FSArray as a source for SelectFSs. 
   * @param filterByType only includes elements of this type
   * @return a new instance of SelectFSs
   */
  default <T extends FeatureStructure> SelectFSs<T> select(Type filterByType) {
    return select().type(filterByType);
  }

  /**
   * Treat an FSArray as a source for SelectFSs.  
   * @param filterByType only includes elements of this JCas class
   * @return a new instance of SelectFSs
   */
  default <T extends FeatureStructure> SelectFSs<T> select(Class<T> filterByType) {
    return select().type(filterByType);
  }
  
  /**
   * Treat an FSArray as a source for SelectFSs. 
   * @param filterByType only includes elements of this JCas class's type
   * @return a new instance of SelectFSs
   */
  default <T extends FeatureStructure> SelectFSs<T> select(int filterByType) {
    return select().type(filterByType);
  }
  
  /**
   * Treat an FSArray as a source for SelectFSs. 
   * @param filterByType only includes elements of this type (fully qualifined type name)
   * @return a new instance of SelectFSs
   */
  default <T extends FeatureStructure> SelectFSs<T> select(String filterByType) {
    return select().type(filterByType);
  }
}
