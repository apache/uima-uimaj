package org.apache.uima.internal.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MiscTest {

  @Test
  public void test() {
    assertEquals(8, Misc.nextHigherPowerOfX(0, 8));
    assertEquals(8, Misc.nextHigherPowerOfX(-0, 8));
    assertEquals(8, Misc.nextHigherPowerOfX(1, 8));
    assertEquals(8, Misc.nextHigherPowerOfX(7, 8));
    assertEquals(8, Misc.nextHigherPowerOfX(8, 8));
    assertEquals(16, Misc.nextHigherPowerOfX(9, 8));
    System.out.println(Misc.nextHigherPowerOfX(10 * 1024 * 1024 * 8 / 3 / 50, 4096)); // == 561152
    System.out.println( Misc.nextHigherPowerOfX(Math.max(512, 561152/1000), 32));
  

  }

}
