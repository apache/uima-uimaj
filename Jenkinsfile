// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements. See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership. The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License. You may obtain a copy of the License at
// 
// http://www.apache.org/licenses/LICENSE-2.0
// 
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied. See the License for the
// specific language governing permissions and limitations
// under the License.
  
@Library('uima-build-jenkins-shared-library') _

<<<<<<< HEAD
defaultPipeline {
  extraMavenArguments = '-Pjacoco,spotbugs,pmd'
=======
        withMaven() {
          sh script: 'mvn ' +
            params.extraMavenArguments +
            ' -U -Dmaven.test.failure.ignore=true clean verify'
        }
      }
    }
    
    // Perform a SNAPSHOT build of a main branch. This stage is typically executed after a
    // merge request has been merged. On success, it deploys the generated artifacts to the
    // Maven repository server.
    stage("SNAPSHOT build") {
      when { branch pattern: "main|main-v2", comparator: "REGEXP" }
      
      steps {
        withMaven() {
          sh script: 'mvn ' +
            params.extraMavenArguments +
            ' -U -Dmaven.test.failure.ignore=true clean deploy'
        }
      }
    }
  }
>>>>>>> main
}
