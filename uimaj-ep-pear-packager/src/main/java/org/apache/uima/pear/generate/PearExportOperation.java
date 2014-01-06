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

package org.apache.uima.pear.generate;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;

/**
 * {@link IRunnableWithProgress} that creates a zip file containing a given set of resources *
 */
public class PearExportOperation implements IRunnableWithProgress {

  /** An array of files that should be included in the zip file */
  protected final IFile[] fExports;

  /** Prefix to strip off from the provided files paths */
  protected final IContainer fRoot;

  /** Filename for the destination zip file */
  protected final String fFilename;

  /** whether zip contents should be compressed */
  protected final boolean fCompress;

  /**
   * Constructor
   * 
   * @param exports
   *          An array of files that should be included in the zip file
   * @param virtualRoot
   *          Prefix to strip off from the provided files paths, e.g. if '/foo/bar/baz.txt' is
   *          included and '/foo' is the virtual root, than '/bar/baz.txt' will be created in the
   *          zip file
   * @param filename
   *          Filename for the destination zip file
   * @param compress
   *          <code>true</code> if the zip contents should be compressed, <code>false</code>
   *          otherwise
   */
  public PearExportOperation(final IFile[] exports, final IContainer virtualRoot,
          final String filename, final boolean compress) {
    fExports = exports;
    fRoot = virtualRoot;
    fFilename = filename;
    fCompress = compress;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.operation.IRunnableWithProgress#run(org.eclipse.core.runtime.IProgressMonitor)
   */
  public void run(final IProgressMonitor monitor) throws InvocationTargetException,
          InterruptedException {
    try {
      final int SCALE = 100;
      final String jobName = "Exporting " + fExports.length + " files to Pear file " + fFilename
              + "...";
      monitor.beginTask(jobName, 2 * SCALE * fExports.length);

      // Create output zip stream
      final File pearFile = new File(fFilename);
      FileOutputStream fOut;
      try {
        fOut = new FileOutputStream(pearFile);
      } catch (final FileNotFoundException exception) {
        throw new InvocationTargetException(exception);
      }
      final ZipOutputStream stream = new ZipOutputStream(fOut);

      if (monitor.isCanceled()) {
        // Nothing written, just close and cancel
        stream.close();
        throw new OperationCanceledException();
      }

      // Add each file to the zip stream
      for (int i = 0; i < fExports.length; ++i) {
        final IFile file = fExports[i];
        // Verify file is child of root
        if (!fRoot.getFullPath().isPrefixOf(file.getFullPath())) {
          throw new IllegalArgumentException(fRoot + " is not a parent of " + file); //$NON-NLS-1$
        }

        // Read file contents
        String filename = file.getFullPath().toPortableString();
        final String cutPath = fRoot.getFullPath().addTrailingSeparator().toPortableString();
        filename = filename.substring(cutPath.length());

        monitor.subTask("Adding " + filename);
        final byte[] fileContents = readContents(file, new SubProgressMonitor(monitor, SCALE));

        // Add contents to zip file
        final ZipEntry entry = new ZipEntry(filename);
        // file.getModificationStamp() seems to use a different semantic
        // for the timestamp
        entry.setTime(file.getLocation().toFile().lastModified());
        if (fCompress) {
          entry.setMethod(ZipEntry.DEFLATED);
        } else {
          entry.setMethod(ZipEntry.STORED);
          entry.setSize(fileContents.length);
          final CRC32 checksumCalculator = new CRC32();
          checksumCalculator.update(fileContents);
          entry.setCrc(checksumCalculator.getValue());
        }
        stream.putNextEntry(entry);
        stream.write(fileContents);
        monitor.worked(SCALE);

        if (monitor.isCanceled()) {
          // Close zip file and remove it, then terminate
          stream.close();
          pearFile.delete();
          throw new OperationCanceledException();
        }
      }

      stream.close();
    } catch (final FileNotFoundException exception) {
      throw new InvocationTargetException(exception);
    } catch (final IOException exception) {
      throw new InvocationTargetException(exception);
    } finally {
      monitor.done();
    }

  }

  /**
   * @param file
   *          The file to read
   * @param monitor
   * @return A byte array with the file contents
   * @throws IOException -
   */
  private byte[] readContents(final IFile file, IProgressMonitor monitor) throws IOException {
    final ByteArrayOutputStream output = new ByteArrayOutputStream();
    final File localFile = file.getLocation().toFile();
    final int bufferSize = 4096;
    int count = (int) (localFile.length() / bufferSize);
    try {
      monitor.beginTask("Reading file " + file.getFullPath().toPortableString(), count);
      final FileInputStream in = new FileInputStream(localFile);
      final BufferedInputStream bIn = new BufferedInputStream(in);
      int length = 0;
      byte[] buffer = new byte[bufferSize];
      byte[] bufferCopy = new byte[bufferSize];
      while ((length = bIn.read(buffer, 0, bufferSize)) != -1) {
        bufferCopy = new byte[length];
        System.arraycopy(buffer, 0, bufferCopy, 0, length);
        output.write(bufferCopy);
        monitor.worked(1);
      }
      bIn.close();
    } finally {
      monitor.done();
      output.close();
    }

    return output.toByteArray();
  }

}
