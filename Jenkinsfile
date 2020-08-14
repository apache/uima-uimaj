pipeline {
  agent any
  tools { 
    maven 'Maven (latest)' 
    jdk 'JDK 1.8 (latest)' 
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
        sh 'printenv'
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
      }
    }
    
    // Perform a SNAPSHOT build of a main branch. This stage is typically executed after a
    // merge request has been merged. On success, it deploys the generated artifacts to the
    // Maven repository server.
    stage("SNAPSHOT build") {
      when { branch pattern: "master", comparator: "REGEXP" }
      
      steps {
        withMaven() {
          sh script: 'mvn ' +
            params.extraMavenArguments +
            ' -U -Dmaven.test.failure.ignore=true clean deploy'
        }
      }
    }
  }
}
