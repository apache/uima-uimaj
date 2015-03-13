package org.apache.uima.cas.impl;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import junit.framework.TestCase;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.XMLInputSource;

public class BinaryCasSerDesPerformance extends TestCase {

  // Note: This test case requires an input directory that has
  //   1) a file typesystem.xml which has the type system for the serialized files
  //   2) a bunch of files having compressed serialized CASes
  //   Set the directory in the 2nd line below
  public void testBinaryCasDeserialization6Performance() throws Exception {
    
    File dir = new File("" /*"/au/t/data/bin-compr-6/shakespeare.txt_40_processed"*/);
    
    if (!dir.exists()) return;
    
    File typeSystemFile = new File(dir, "typesystem.xml");
    XMLInputSource in = new XMLInputSource(typeSystemFile);
    TypeSystemDescription typeSystemDescription = UIMAFramework.getXMLParser().parseTypeSystemDescription(in);
    CAS cas = CasCreationUtils.createCas(typeSystemDescription, null, null);
    
    long accumDeser = 0;
    long accumSer = 0;
    for (int i = 0; i <10; i++) {
    for (final File f : dir.listFiles()) {
      if (f.getName().equals("typesystem.xml")) {
        continue;
      }
      InputStream inputStream = new BufferedInputStream(new FileInputStream(f));
      cas.reset();
      long ist = System.nanoTime();
      Serialization.deserializeCAS(cas, inputStream);
      accumDeser += System.nanoTime() - ist;  
      
      ByteArrayOutputStream baos = new ByteArrayOutputStream(1024*512);
      ist = System.nanoTime();
      Serialization.serializeWithCompression(cas, baos, cas.getTypeSystem());
      accumSer += System.nanoTime() - ist;
//      System.out.format("Time to deserialize was %,d milliseconds, size = %d%n", 
//          (System.nanoTime() - ist) / 1000000L, ((CASImpl)cas).getHeap().getHeapSize());
    }
    }
    System.out.format("Time to deserialize all files was %,d milliseconds%n", accumDeser / 1000000); // (System.nanoTime() - startTime) / 1000000L);
    System.out.format("Time to serialize   all files was %,d milliseconds%n", accumSer / 1000000);
  }

}
