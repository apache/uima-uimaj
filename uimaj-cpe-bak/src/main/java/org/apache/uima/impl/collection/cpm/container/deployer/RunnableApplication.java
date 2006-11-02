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

package org.apache.uima.impl.collection.cpm.container.deployer;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import org.apache.uima.UIMAFramework;
import org.apache.uima.collection.base_cpm.container.CasProcessorConfiguration;
import org.apache.uima.collection.metadata.CasProcessorExecArg;
import org.apache.uima.collection.metadata.CasProcessorRunInSeperateProcess;
import org.apache.uima.collection.metadata.CasProcessorRuntimeEnvParam;
import org.apache.uima.collection.metadata.CpeCasProcessor;
import org.apache.uima.collection.metadata.CpeLocalCasProcessor;
import org.apache.uima.impl.collection.cpm.utils.CPMUtils;
import org.apache.uima.impl.collection.cpm.utils.Execute;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.util.Level;
import org.apache.uima.impl.util.SystemEnvReader;


public class RunnableApplication
{
	protected String executable;
	protected Execute exec;
	protected ArrayList environment = new ArrayList();
	protected List argList = new ArrayList();


	/**
	 * Sets up command line used to launch Cas Processor in a seperate process. 
	 * Combines environment variables setup in the CPE descriptor with a
	 * System environment variables.
	 * 
	 * @param aCasProcessorConfiguration - access to Cas Processor configuration
	 * @param aJaxbCasProcessorConfig 
	 * @throws ResourceConfigurationException
	 */
	protected void addApplicationInfo(CasProcessorConfiguration aCasProcessorConfiguration, CpeCasProcessor aCasProcessor)
	throws ResourceConfigurationException
	{
		try
		{
			if ( aCasProcessor instanceof CpeLocalCasProcessor )
			{
				CasProcessorRunInSeperateProcess rip =
					((CpeLocalCasProcessor)aCasProcessor).getRunInSeperateProcess();
				if ( UIMAFramework.getLogger().isLoggable(Level.FINEST) )
				{
				UIMAFramework.getLogger(this.getClass()).log(Level.FINEST,"+++++++++++++++++++++++++++++++++++++++++++++++++");
				UIMAFramework.getLogger(this.getClass()).log(Level.FINEST,"+++++++++++++++++++++++++++++++++++++++++++++++++");
				UIMAFramework.getLogger(this.getClass()).log(Level.FINEST,"Operating System is::::" + System.getProperty("os.name"));
				UIMAFramework.getLogger(this.getClass()).log(Level.FINEST,"+++++++++++++++++++++++++++++++++++++++++++++++++");
				UIMAFramework.getLogger(this.getClass()).log(Level.FINEST,"+++++++++++++++++++++++++++++++++++++++++++++++++");
				}
				executable = rip.getExecutable().getExecutable();
				List envList = rip.getExecutable().getEnvs();
				
				exec = new Execute();
		
				Properties sysEnv = null;
				try
				{
					sysEnv = SystemEnvReader.getEnvVars();
					if ( System.getProperty("DEBUG") != null )
					{
						printSysEnvironment();		
					}
				}
				catch( Throwable e)
				{
					e.printStackTrace();
				}
		
		
				if (envList != null)
				{
					//	Make sure the env array has sufficient capacity 
					environment.clear();
					//	First copy all env vars from the CPE Descriptor treating PATH and CLASSPATH as special cases
					int i = 0;   // need this here so that we know where to append vars from the env Property object
					for (; envList != null && i < envList.size(); i++)
					{
						CasProcessorRuntimeEnvParam envType = (CasProcessorRuntimeEnvParam) envList.get(i);
						String key = envType.getEnvParamName();
						String value = envType.getEnvParamValue();
				
						//	Special Cases for PATH and CLASSPATH
						if ( key.equalsIgnoreCase("PATH") )
						{
							String path = getEnvVarValue( key );
							if ( path != null )
							{
								environment.add( key + "=" + value+System.getProperty("path.separator")+path);
							}
							else
							{
								environment.add(key + "=" + value+System.getProperty("path.separator"));
							}
							continue;
						}
				
						if ( key.equalsIgnoreCase("CLASSPATH") && sysEnv != null )
						{
							String classpath = getEnvVarValue( key );
							if (  classpath != null  )
							{
								environment.add(key + "=" + value+System.getProperty("path.separator")+classpath);
							}
							else
							{
								environment.add(key + "=" + value+System.getProperty("path.separator"));
							}
							continue;
						}
						environment.add(key + "=" + value);
					}

					//	Now, copy all env vars from the current environment
					if ( sysEnv != null )
					{
						Enumeration envKeys = sysEnv.keys();
				
						while( envKeys.hasMoreElements())
						{
							String key = (String)envKeys.nextElement();
							//	Skip those vars that we've already setup above
							if ( key.equalsIgnoreCase("PATH") || key.equalsIgnoreCase("CLASSPATH"))
							{
								continue;
							}
							environment.add(key + "=" + (String)sysEnv.getProperty(key));
						}
					}
					String[] envArray = new String[environment.size()];
					environment.toArray(envArray);
					exec.setEnvironment(envArray);
				}
				CasProcessorExecArg[] args = rip.getExecutable().getAllCasProcessorExecArgs();
				for( int i=0; args != null && i < args.length; i++ )
				{
					argList.add(args[i]);
				}
				
			}
		}
		catch( Exception e)
		{
			throw new ResourceConfigurationException(e);
		}
	}
	/**
	 * Displays current system environment settings
	 *
	 */
	private void printSysEnvironment()
	{
		Properties sysEnv = null;
		try
		{
			sysEnv = SystemEnvReader.getEnvVars();
			Enumeration sysKeys = sysEnv.keys();
			while( sysKeys.hasMoreElements() )
			{
				String key = (String)sysKeys.nextElement();
				if ( UIMAFramework.getLogger().isLoggable(Level.FINEST) )
				{
					UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(), "initialize",
					        CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_launching_with_service_env__FINEST",
					        new Object[] {Thread.currentThread().getName(),key, sysEnv.getProperty(key)});				
				}
			}
		}
		catch( Throwable e)
		{
			e.printStackTrace();
		}
	}
	/**
	 * Returns a value of a given environment variable
	 * 
	 * @param aKey - name of the environment variable
	 * @return - value correspnding to environment variable
	 */
	protected String getEnvVarValue( String aKey )
	{
		Properties sysEnv = null;
		try
		{
			//	Retrieve all env variables
			sysEnv = SystemEnvReader.getEnvVars();
			Enumeration sysKeys = sysEnv.keys();
			while( sysKeys.hasMoreElements() )
			{
				String key = (String)sysKeys.nextElement();
				if ( aKey.equalsIgnoreCase(key) )
				{
					return sysEnv.getProperty(key);
				}
			}
		}
		catch( Throwable e)
		{
			e.printStackTrace();
		}
		return null;
	}

}
