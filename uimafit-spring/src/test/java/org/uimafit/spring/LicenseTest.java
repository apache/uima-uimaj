/*
 Copyright 2009-2010	Regents of the University of Colorado.
 All rights reserved.

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */
package org.uimafit.spring;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.uima.util.FileUtils;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Steven Bethard, Philip Ogren
 */

public class LicenseTest {

	@Test
	public void testLicenseStatedInSource() throws Exception {
		test(new File("src/main/java"));
	}

	@Test
	public void testLicenseStatedInTestSource() throws Exception {
		test(new File("src/test/java"));

	}

	private void test(File directory) throws IOException {
		List<String> filesMissingLicense = new ArrayList<String>();

		Iterator<?> files = org.apache.commons.io.FileUtils.iterateFiles(directory,
				new SuffixFileFilter(".java"), TrueFileFilter.INSTANCE);

		while (files.hasNext()) {
			File file = (File) files.next();
			if (file.getParentFile().getName().equals("type")
					|| file.getName().equals("Files.java")) {
				continue;
			}

			String fileText = FileUtils.file2String(file);

			boolean hasCopyright = fileText.indexOf("Copyright") != -1;
			boolean hasLicense = fileText.indexOf("Licensed under the Apache License, Version 2.0")
				!= -1;
			boolean hasAuthor = fileText.indexOf("@author") != -1;
			boolean isPackageDoc = "package-info.java".equals(file.getName());
			if ( !hasCopyright || !hasLicense || (!isPackageDoc && !hasAuthor) ) {
				filesMissingLicense.add(file.getPath());
			}
		}

		if (filesMissingLicense.size() > 0) {
			StringBuilder sb = new StringBuilder();
			sb.append(filesMissingLicense.size());
			sb.append(" source file missing license or author attribution:\n");
			Collections.sort(filesMissingLicense);
			for (String path : filesMissingLicense) {
				sb.append(path).append('\n');
			}
			Assert.fail(sb.toString());
		}
	}

}
