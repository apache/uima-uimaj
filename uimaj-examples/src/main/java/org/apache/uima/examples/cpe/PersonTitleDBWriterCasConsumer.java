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

package org.apache.uima.examples.cpe;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.collection.CasConsumer_ImplBase;
import org.apache.uima.examples.SourceDocumentInformation;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.util.ProcessTrace;

import example.PersonTitle;

/**
 * A simple CAS consumer that creates a Derby (Cloudscape) database in the file system. You can
 * obtain this database from http://incubator.apache.org/derby/ *
 * <p>
 * This CAS Consumer takes one parameters:
 * <ul>
 * <li><code>OutputDirectory</code> - path to directory which is the "System" directory for the
 * derby DB. </li>
 * </ul>
 * 
 * It deletes all the databases at the system location (!!!), Creates a new database (takes the most
 * time - order of 10+ seconds) creates a table in the database to hold instances of the PersonTitle
 * annotation Adds entries for each PersonTitle annotation in each CAS to the database
 * 
 * To use - add derby.jar to the classpath when you start the CPE GUI - run the CPE Gui and select
 * the Name Recognizer and Person Title Annotator aggregate. - a good sample collection reader is
 * the FileSystemCollectionReader, and - a good sample data is the <UIMA_HOME>/examples/data
 * 
 * The processing is set up to handle multiple CASes. The end is indicated by using the
 * CollectionProcessComplete call.
 * 
 * Batching of updates to the database is done. The batch size is set to 50. The larger size takes
 * more Java heap space, but perhaps runs more efficiently.
 * 
 * The Table is populated with a slightly denormalized form of the data: the URI of the document is
 * included with every record.
 * 
 * 
 */
public class PersonTitleDBWriterCasConsumer extends CasConsumer_ImplBase {
  /**
   * Name of configuration parameter that must be set to the path of a directory into which the
   * Derby Database will be written.
   */
  public static final String PARAM_OUTPUTDIR = "OutputDirectory";

  public static final int MAX_URI_LENGTH = 80;

  public static final int MAX_TITLE_LENGTH = 20;

  public static final int DB_LOAD_BATCH_SIZE = 50;

  private int batchCounter = DB_LOAD_BATCH_SIZE;

  private File mOutputDir;

  private boolean firstCall = true;

  private static boolean firstEverCall = true;

  private PreparedStatement stmt;

  private Connection con;

  private long startTime;

  public void initialize() throws ResourceInitializationException {
    startTime = System.currentTimeMillis();
    System.out
            .println("Time: " + (System.currentTimeMillis() - startTime) + " initialize() called");
    mOutputDir = new File((String) getConfigParameterValue(PARAM_OUTPUTDIR));
    if (!mOutputDir.exists()) {
      mOutputDir.mkdirs();
    }

    // make this the derby home by setting system property
    System.setProperty("derby.system.home", mOutputDir.toString());
    System.out.println("Time: " + (System.currentTimeMillis() - startTime)
            + " DB Writer: Set derby system home to: '" + mOutputDir.toString() + "'");
  }

  /**
   * Processes the CasContainer which was populated by the TextAnalysisEngines. <br>
   * In this case, the CAS is assumed to contain annotations of type PersonTitle, created with the
   * PersonTitleAnnotator. These Annotations are stored in a database table called PersonTitle.
   * 
   * @param aCAS
   *          CasContainer which has been populated by the TAEs
   * 
   * @throws ResourceProcessException
   *           if there is an error in processing the Resource
   * 
   * @see org.apache.uima.collection.base_cpm.CasObjectProcessor#processCas(org.apache.uima.cas.CAS)
   */
  public void processCas(CAS aCAS) throws ResourceProcessException {
    System.out.println("Time: " + (System.currentTimeMillis() - startTime)
            + " DB Writer: ProcessCas called");
    JCas jcas;
    try {
      jcas = aCAS.getJCas();
    } catch (CASException e) {
      throw new ResourceProcessException(e);
    }

    try {
      if (firstCall) {
        firstCall = false;
        System.out.println("Time: " + (System.currentTimeMillis() - startTime)
                + " DB Writer: First Time Initiailization: ");
        // NOTE TO USERS: a better design will be to do the loading of the
        // driver in the initialize() method, where it can
        // throw a ResourceInitialization Exception if it can't
        // load (and perhaps even connect to) the database

        // load the driver
        // Depends on "derby.system.property" set in initialize
        // Different databases have different classes they load
        // This is the one for derby - for the "Embedded" database
        // (derby also has a network accessed database driver)
        if (firstEverCall) {
          firstEverCall = false;
          System.out
                  .println("Time: "
                          + (System.currentTimeMillis() - startTime)
                          + " DB Writer: Doing first process call ever (even during re-runs) initialization");
          try {
            // note: newInstance() call is needed to reinitialize properly after
            // derby has been shutdown
            Class.forName("org.apache.derby.jdbc.EmbeddedDriver").newInstance();
            System.out.println("Time: " + (System.currentTimeMillis() - startTime)
                    + " DB Writer:    Loaded derby DB driver OK");
          } catch (ClassNotFoundException e) {
            System.err.println("No driver found for derby - check class path.");
          } catch (InstantiationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
        }
        // create and connect to "ExamplePersonTitleDB" -
        // in derby, this is a directory name under the "System"
        // directory set with the above parameter
        // the System directory is passed to the JVM as a property
        // (see the derby documentation). Or - you can specify a
        // complete path, using //forward slashes on windows, such as
        // "jdbc:derby:c:/a/b/myDB".

        // NOTE TO USERS: In a real application you would probably
        // not delete the database and re-create it - this takes a long
        // time. If you want to get rid of the particular table you're
        // about to create if it already exists, use
        // the SQL "Drop" command

        // if database exists, delete it
        File db = new File(mOutputDir.toString() + "/ExamplePersonTitleDB");
        if (db.exists()) {
          System.out.println("Time: " + (System.currentTimeMillis() - startTime)
                  + " DB Writer: First Time Initiailization: Deleting Database");
          deleteDir(db);
          System.out.println("Time: " + (System.currentTimeMillis() - startTime)
                  + " DB Writer: First Time Initiailization: Database deleted");

        }

        con = DriverManager.getConnection("jdbc:derby:ExamplePersonTitleDB;create=true");
        System.out
                .println("Time: "
                        + (System.currentTimeMillis() - startTime)
                        + " DB Writer: First Time Initiailization: Created the ExamplePersonTitleDB and connected to it.");

        // Databases typically use user-names and passwords; these can
        // be passed as //properties to the getConnection method.

        // drop the table in case it's already present
        // This isn't needed because we're starting from an empty database,
        // but leave here for tutorial reasons
        Statement sqlStmt = con.createStatement();
        try {
          sqlStmt.execute("drop table PersonTitle");
        } catch (SQLException e) {
        }
        sqlStmt.execute("create table PersonTitle(" + "uri varchar(" + MAX_URI_LENGTH
                + "), spannedText varchar(" + MAX_TITLE_LENGTH
                + "), beginOffset int, endOffset int)");
        System.out.println("Time: " + (System.currentTimeMillis() - startTime)
                + " DB Writer: First Time Initiailization: Created the PersonTitle table.");

        sqlStmt.close(); // free resources associated with this
        // statement

        // Entering data: use a Statement, or a PreparedStatement
        stmt = con.prepareStatement("insert into PersonTitle values (?, ?, ?, ?)");
        // Writing out all instances of type uima.tcas.Annotation
        // Assume the variable "jcas" holds a reference to a JCas
        con.setAutoCommit(false); // need this for batch updating
      }

      // get the singleton instance of the SourceDocumentInformation
      SourceDocumentInformation sdi = (SourceDocumentInformation) 
              jcas.getAnnotationIndex(SourceDocumentInformation.type).iterator().next();

      System.out.println("Time: " + (System.currentTimeMillis() - startTime)
              + " DB Writer: Processing doc: '" + sdi.getUri() + "'");

      stmt.setString(1, truncate(sdi.getUri(), MAX_URI_LENGTH));
      for (FSIterator iter = jcas.getAnnotationIndex(PersonTitle.type).iterator(); 
           iter.hasNext();) {
        PersonTitle pt = (PersonTitle) iter.next();
        stmt.setString(2, truncate(pt.getCoveredText(), MAX_TITLE_LENGTH));
        stmt.setInt(3, pt.getBegin());
        stmt.setInt(4, pt.getEnd());
        stmt.addBatch();
        batchCounter--;
        if (batchCounter <= 0) {
          System.out.println("Time: " + (System.currentTimeMillis() - startTime)
                  + " DB Writer: Batch writing updates - process call");
          stmt.executeBatch();
          // NOTE TO USERS: Although we "commit" here, you may want
          // to delay committing until batchProcessComplete - or some
          // other logical point - to keep the DB in a more consistent
          // state (not partially updated).

          con.commit();
          batchCounter = DB_LOAD_BATCH_SIZE;
        }
      }
    } catch (SQLException e) {
      try {
        // NOTE TO USERS: depending on your error recover logic, you'll
        // probably want to do both a rollback and a clearBatch if an
        // exception occurs.
        con.rollback();
      } catch (SQLException e1) {
        // TODO Auto-generated catch block
        e1.printStackTrace();
      }
      try {
        stmt.clearBatch();
      } catch (SQLException e2) {
        // TODO Auto-generated catch block
        e2.printStackTrace();
      }
      throw new ResourceProcessException(e);
    }
  }

  public void collectionProcessComplete(ProcessTrace arg0) throws ResourceProcessException,
          IOException {
    firstCall = true;

    try {
      if (batchCounter < DB_LOAD_BATCH_SIZE) {
        System.out.println("Time: " + (System.currentTimeMillis() - startTime)
                + " DB Writer: Batch writing updates - processComplete call");
        stmt.executeBatch();
        con.commit();
        batchCounter = DB_LOAD_BATCH_SIZE;
      }

      stmt.close();
      con.close();
      System.out.println("Time: " + (System.currentTimeMillis() - startTime)
              + " DB Writer: Sucessfully closed the connection - done.");

    } catch (SQLException e) {
      System.err.println("Unexpected SQL exception");
      e.printStackTrace();
    }
    try {
      DriverManager.getConnection("jdbc:derby:ExamplePersonTitleDB;shutdown=true");
    } catch (SQLException e) {
    }

    // If we shut down the db - we get a "no suitable driver" SQL exception if rerunning
    try {
      firstEverCall = true;
      DriverManager.getConnection("jdbc:derby:;shutdown=true");
    } catch (SQLException e) {
    }
  }

  private void deleteDir(File f) {
    if (f.isDirectory()) {
      String[] contents = f.list();
      for (int i = 0; i < contents.length; i++) {
        deleteDir(new File(f.toString() + "/" + contents[i]));
      }
    }
    f.delete();
  }

  private String truncate(String s, int length) {
    if (s.length() <= length)
      return s;
    return s.substring(0, length);
  }

}
