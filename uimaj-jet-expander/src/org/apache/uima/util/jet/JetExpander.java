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
package org.apache.uima.util.jet;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class JetExpander {
  
  static final String APACHE_2_LICENSE_HEADER = 
    "/*\n" +
" * Licensed to the Apache Software Foundation (ASF) under one\n" +
" * or more contributor license agreements.  See the NOTICE file\n" +
" * distributed with this work for additional information\n" +
" * regarding copyright ownership.  The ASF licenses this file\n" +
" * to you under the Apache License, Version 2.0 (the\n" +
" * \"License\"); you may not use this file except in compliance\n" +
" * with the License.  You may obtain a copy of the License at\n" +
" * \n" +
" *   http://www.apache.org/licenses/LICENSE-2.0\n" +
" * \n" +
" * Unless required by applicable law or agreed to in writing,\n" +
" * software distributed under the License is distributed on an\n" +
" * \"AS IS\" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY\n" +
" * KIND, either express or implied.  See the License for the\n" +
" * specific language governing permissions and limitations\n" +
" * under the License.\n" +
" */\n\n";
 
  static class ErrorExit extends RuntimeException {
    private static final long serialVersionUID = 1L;
    String msg;
    ErrorExit(String msg) {
      super();
      this.msg = msg;
      System.err.println("JetExpander error: " + msg);
    }
  }
  
  FileWriter fileWriter;
  String outDir;
  String in;  
  String rootDir;
  
	public static void main(String[] args) {
		// arg 1 = source
		// arg 2 = dir where output goes
		JetExpander je = new JetExpander();
		je.main1(args);
	}
	
	void main1(String[] args) {
		try {
			if (args.length != 2) 
			  throw new ErrorExit("Bad Arguments - need 2, source, and output directory.");
			
			File inputFile = new File(args[0]);
			outDir = args[1];
			try {
				rootDir = pathOnly(inputFile.getCanonicalPath());
			} catch (IOException e1) {
				e1.printStackTrace();
				throw new ErrorExit("trouble getting input file canonical path.");
			}
					
	    in = readFile(args[0]);
	    int i;
			try {
				i = outputStart();
				expand(i);
				fileWriter.write("\n    return stringBuffer.toString();\n  }\n}");
				fileWriter.close();
			} catch (IOException e2) {
				e2.printStackTrace();
				throw new ErrorExit("IO error writing output file startup.");
			}
		}
		catch (ErrorExit e) {
		}
	}
	
	String readFile(String fileName) {
    FileReader fileReader = null;
    try {
			File file = new File(fileName);
			file = file.getCanonicalFile();
      fileReader = new FileReader(file);
      
			int fileLength = (int)file.length(); // length in bytes >= length in chars due to char encoding
			char[] buffer = new char [fileLength];
      int read_so_far = 0;
      while (read_so_far < fileLength) {
			  int count = fileReader.read(buffer, read_so_far, fileLength - read_so_far);	
        if (0 > count)
          break;
        read_so_far += count;
      }     
			return new String(buffer, 0, read_so_far).replaceAll("\\r","");  // for linux/unix 
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			throw new ErrorExit("Bad Input File - can't read it: '" + fileName + "'");	  
		} catch (IOException e) {
			e.printStackTrace();
			throw new ErrorExit("IO Error reading input file: '" + fileName + "'");
		} finally {
      if (null != fileReader) 
        try {
          fileReader.close();
        } catch (IOException e) {
        }
    }
	}
	
	String pathOnly(String f) {
		int lastSep = f.lastIndexOf(File.separatorChar);
		return f.substring(0, lastSep);
	}
	
	int outputStart() throws IOException {

    String p1 = "<%@ jet package=\"";
		int i = in.indexOf(p1);
	  if (i < 0 )
	    throw new ErrorExit("Cant find the <% jet package= sequence.");
    i = i + p1.length();
	  int nextQuotePos = in.indexOf('"',i);	  
	  String pkg = in.substring(i,nextQuotePos);
	  
    String p2 = "imports=\"";
	  i = in.indexOf(p2, nextQuotePos) + p2.length();
	  nextQuotePos = in.indexOf('"',i);  
	  String [] imports = in.substring(i, nextQuotePos).split("\\s+");
	  
		String p3 = "class=\"";
		i = in.indexOf(p3, nextQuotePos) + p3.length();
		nextQuotePos = in.indexOf('"',i);  
		String className = in.substring(i, nextQuotePos);

    String p4 = "implements=\"";
    String implementsName = null;
    i = in.indexOf(p4, nextQuotePos) + p4.length();
    if (i > 0) {
      nextQuotePos = in.indexOf('"',i);  
      implementsName = in.substring(i, nextQuotePos);
    }

		String outFileName = null;
		try {
			outFileName = outDir + File.separator + pkg.replaceAll("\\.", File.separator) + File.separator + className + ".java";
			(new File(pathOnly(outFileName))).mkdirs();			
			fileWriter = new FileWriter(outFileName);
		} catch (IOException e1) {
			e1.printStackTrace();
			throw new ErrorExit(
				"Bad outputFile - can't open for writing: '" + outFileName + "'");
		}
    fileWriter.write(APACHE_2_LICENSE_HEADER);
		fileWriter.write("package " + pkg + ";\n\n");
		
		for (int j = 0; j < imports.length; j++) {
			fileWriter.write("import " + imports[j] + ";\n");
		}

    fileWriter.write("\npublic class " + className);
    if (implementsName != null) {
      fileWriter.write(" implements " + implementsName);
    }
    fileWriter.write(" {\n\n");
    fileWriter.write("  public String generate(Object argument) {\n");
    fileWriter.write("    StringBuffer stringBuffer = new StringBuffer();\n");
    
		return in.indexOf('\n', nextQuotePos);
	}

  String fixupStr (int start, int end) {
    // for linux - have to remove the backslash-r sequence
    return in.substring(start, end).replaceAll("\n","\\\\n").replaceAll("\r","")
  	.replaceAll("\"","\\\\\"")
    ;	
  }
  
	void expand (int i) throws IOException{
		for (; i < in.length(); ) {
      int trigger = in.indexOf("<%",i);
      if (trigger >= 0) {
      	
//      	String [] lines = in.substring(i,trigger).split("\n",-1);
//      	for (int j = 0; j < lines.length; j++) {
//      		fileWriter.write("\n    stringBuffer.append(\"" + lines[j] + 
//                            ((j < lines.length-1) ? "\\n" : "") + "\");");
//      	}
				fileWriter.write("\n    stringBuffer.append(\"" + fixupStr(i, trigger) + "\");");
                 
        if (in.charAt(trigger+2) == '@') {
        	i = doInclude(trigger+3);
        	continue;
        }
        
        if (in.charAt(trigger+2) == '=') {
        	i = doInsert(trigger+3);
        	continue;
        }
        
        //fileWriter.write("\n  ");
        int triggerEnd = in.indexOf("%>",trigger+2);
        fileWriter.write("\n  " + in.substring(trigger+2, triggerEnd));
        i = triggerEnd+2;
		    if (in.charAt(i) == '\n')
		      i += 1;

        continue;
      }
      else {
      	fileWriter.write("\n    stringBuffer.append(\"" + fixupStr(i, in.length()) + "\");");
      	break;
      }
		}		  
	}
	
	int doInclude(int i) throws IOException {
		final String p1 = " include file=\"";
		String savedIn = in;
		
		if (!in.substring(i, i + p1.length()).equals(p1)) {
			throw new ErrorExit("bad include: " + in.substring(i, i+100));
		}		
		i = i + p1.length();
		int includeEnd = in.indexOf("\"", i);
		String includeFileName = in.substring(i, includeEnd);
		String savedUserDir = System.getProperty("user.dir");
		System.setProperty("user.dir", rootDir);
		in = readFile(includeFileName);
    // strip off headers
    int endLoc = in.indexOf("*/");
    if (endLoc < 0)
      throw new ErrorExit("Missing initial comment in included file " + includeFileName);
    in = in.substring(endLoc + 3);  // skip over */ plus nl
		expand(0);
		System.setProperty("user.dir", savedUserDir);
	  in = savedIn;
		i = in.indexOf("%>", includeEnd) + 2;
		if (in.charAt(i) == '\n') 
		  i += 1;
		return i;
	}
	
	int doInsert(int i) throws IOException {
		int insertEnd = in.indexOf("%>", i);
		fileWriter.write("\n    stringBuffer.append(" + in.substring(i, insertEnd) + ");");
		return insertEnd + 2;
	}
}
