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

package org.apache.uima.tools.annot_view;

import java.io.File;

import javax.swing.ImageIcon;
import javax.swing.JWindow;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.apache.uima.resource.RelativePathResolver;
import org.apache.uima.tools.images.Images;
import org.apache.uima.internal.util.CommandLineParser;

/**
 * The main class for the CAS Visual Debugger.
 * 
 * 
 */
public class Gladis {

   private static final String TEXT_FILE_PARAM = "-text";

   private static final String DESC_FILE_PARAM = "-desc";

   private static final String EXECUTE_SWITCH = "-exec";

   private static final String DATA_PATH_PARAM = "-datapath";

   private Gladis() {
      super();
   }

   public static MainFrame createMainFrame() throws ClassNotFoundException,
                                            InstantiationException,
                                            IllegalAccessException,
                                            UnsupportedLookAndFeelException {
      // Set look-and-feel.
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
      final MainFrame frame = new MainFrame();
      // Set icon.
      ImageIcon icon = Images.getImageIcon(Images.MICROSCOPE);
      if (icon != null) {
         frame.setIconImage(icon.getImage());
      }
      javax.swing.SwingUtilities.invokeLater(new Runnable() {

         public void run() {
            frame.pack();
            frame.show();
         }
      });
      return frame;
   }

   private static final CommandLineParser createCmdLineParser() {
      CommandLineParser parser = new CommandLineParser();
      parser.addParameter(TEXT_FILE_PARAM, true);
      parser.addParameter(DESC_FILE_PARAM, true);
      parser.addParameter(DATA_PATH_PARAM, true);
      parser.addParameter(EXECUTE_SWITCH);
      return parser;
   }

   private static final void printUsage() {
      System.out
               .println("Usage: java org.apache.uima.annot_view.Gladis [-text <TextFile>] [-desc <XmlDescriptor>] [-datapath <DataPath>] [-exec]");
   }

   private static final boolean checkCmdLineSyntax(CommandLineParser clp) {
      if (clp.getRestArgs().length > 0) {
         System.err
                  .println("Error parsing CVD command line: unknown argument(s):");
         String[] args = clp.getRestArgs();
         for (int i = 0; i < args.length; i++) {
            System.err.print(" ");
            System.err.print(args[i]);
         }
         System.err.println();
         return false;
      }
      if (clp.isInArgsList(EXECUTE_SWITCH)
          && !clp.isInArgsList(DESC_FILE_PARAM)) {
         System.err
                  .println("Error parsing CVD command line: -exec switch requires -desc parameter.");
         return false;
      }
      return true;
   }

   public static void main(String[] args) {
      try {        
         CommandLineParser clp = createCmdLineParser();
         clp.parseCmdLine(args);
         if (!checkCmdLineSyntax(clp)) {
            printUsage();
            System.exit(2);
         }
         SplashScreen splashScreen = new SplashScreen(new JWindow());
         splashScreen.start();
         MainFrame frame = createMainFrame();
         splashScreen.close();
         splashScreen.join();
         splashScreen = null;
         if (clp.isInArgsList(TEXT_FILE_PARAM)) {
            frame.loadTextFile(new File(clp.getParamArgument(TEXT_FILE_PARAM)));
         }
         if (clp.isInArgsList(DATA_PATH_PARAM)) {
            frame.setDataPath(clp.getParamArgument(DATA_PATH_PARAM));
         } else {
            String dataProp = System
                     .getProperty(RelativePathResolver.UIMA_DATAPATH_PROP);
            if (dataProp != null) {
               frame.setDataPath(dataProp);
            }
         }
         if (clp.isInArgsList(DESC_FILE_PARAM)) {
            frame.loadAEDescriptor(new File(clp
                     .getParamArgument(DESC_FILE_PARAM)));
         }
         if (clp.isInArgsList(EXECUTE_SWITCH)) {
            frame.runAE(true);
         }
      } catch (Exception e) {
         e.printStackTrace();
         System.exit(1);
      }
   }


}
