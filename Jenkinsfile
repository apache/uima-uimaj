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
  
pipeline {
  agent any
  
  tools { 
    maven 'maven_latest' 
    jdk 'jdk_11_latest' 
  }

  options {
    buildDiscarder(logRotator(
      numToKeepStr: '25', 
      artifactNumToKeepStr: '5'
    ))
    
    // Seems not to be working reliably yet: https://issues.jenkins-ci.org/browse/JENKINS-48556
    // timestamps()
  }
  
  parameters {
    string(
      name: 'extraMavenArguments',
      defaultValue: "",
      description: "Extra arguments to be passed to maven (for testing)")
  }

  stages {
    // Display information about the build environemnt. This can be useful for debugging
    // build issues.
    stage("Build info") {
      steps {
        echo '=== Environment variables ==='
        script {
          if (isUnix()) {
            sh 'printenv'
          }
          else {
            bat 'set'
          }
        }
      }
    }
        
    // Perform a merge request build. This is a conditional stage executed with the GitLab
    // sources plugin triggers a build for a merge request. To avoid conflicts with other
    // builds, this stage should not deploy artifacts to the Maven repository server and
    // also not install them locally.
    stage("Pull request build") {
      when { branch 'PR-*' }
    
      steps {
        script {
          currentBuild.description = 'Triggered by: <a href="' + CHANGE_URL + '">' + BRANCH_NAME +
            ': ' + env.CHANGE_BRANCH + '</a> (' +  env.CHANGE_AUTHOR_DISPLAY_NAME + ')'
        }

        withMaven() {
          sh script: 'mvn ' +
            params.extraMavenArguments +
            ' -U -Dmaven.test.failure.ignore=true clean verify'
        }
        
        script {
          def mavenConsoleIssues = scanForIssues tool: mavenConsole()
          def javaIssues = scanForIssues tool: java()
          def javaDocIssues = scanForIssues tool: javaDoc()
          publishIssues issues: [mavenConsoleIssues, javaIssues, javaDocIssues]
        }
      }
    }
    
    // Perform a SNAPSHOT build of a main branch. This stage is typically executed after a
    // merge request has been merged. On success, it deploys the generated artifacts to the
    // Maven repository server.
    stage("SNAPSHOT build") {
      when { branch pattern: "master|master-v2", comparator: "REGEXP" }
      
      steps {
        withMaven() {
          sh script: 'mvn ' +
            params.extraMavenArguments +
            ' -U -Dmaven.test.failure.ignore=true clean deploy'
        }
        
        script {
          def mavenConsoleIssues = scanForIssues tool: mavenConsole()
          def javaIssues = scanForIssues tool: java()
          def javaDocIssues = scanForIssues tool: javaDoc()
          publishIssues issues: [mavenConsoleIssues, javaIssues, javaDocIssues]
        }
      }
    }
  }
}
