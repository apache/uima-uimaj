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
package org.apache.uima;

import org.apache.uima.resource.ConfigurationManager;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.util.Logger;

public class UimaContextAdminBuilder {
  private Logger logger;
  private ResourceManager resourceManager;
  private ConfigurationManager configManager;

  public UimaContextAdminBuilder withLogger(Logger aLogger) {
    logger = aLogger;
    return this;
  }

  public UimaContextAdminBuilder withResourceManager(ResourceManager aResourceManager) {
    resourceManager = aResourceManager;
    return this;
  }

  public UimaContextAdminBuilder withConfigurationManager(ConfigurationManager aConfigManager) {
    configManager = aConfigManager;
    return this;
  }

  public UimaContextAdmin build() {
    var actualLogger = logger != null ? logger : UIMAFramework.getLogger();
    var actualResMgr = resourceManager != null ? resourceManager
            : UIMAFramework.newDefaultResourceManager();
    var actualCfgMgr = configManager != null ? configManager
            : UIMAFramework.newConfigurationManager();

    return UIMAFramework.newUimaContext(actualLogger, actualResMgr, actualCfgMgr);
  }
}
