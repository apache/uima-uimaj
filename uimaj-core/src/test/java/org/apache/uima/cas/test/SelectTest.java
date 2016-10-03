package org.apache.uima.cas.test;

import static org.junit.Assert.*;

import org.apache.uima.cas.Type;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class SelectTest {
  
  private static Type A1;

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    /* Setup the type system, the CAS with data 
     *   Annotation types:  
     *     A1
     *       A1a  - subtype
     *     A2
     *     A3
     *       type order A1, A2, A3
     *   
     *   non-annotation types:
     *     T1
     *       T1a
     *     T2
     *     T3
     *       type order T1, T2, T3   sortkey: int
     *  
     *   Indexes over non-annotation types:
     *     bag, set, sorted
     *      
     *   Instances: some have same keys so set is tested.  
     *     
     *     
     */
    
    
    
  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
  }

  @Test
  public void test() {
    
    /* Select testing
     *   Test with cas, jcas, fsArray, fsList, index
     *   
     *   Test with type as positional, as keyword
     *   
     *   
     */
    
    /* non-annotation testing
     *   bag, set, sorted - count, try T1, T1a
     * 
     *   For set and sorted, test order and backwards
     *     For bag, confirm backwards is ignored  
     * 
     *   
     */
  }

}
