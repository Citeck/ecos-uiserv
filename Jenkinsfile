pipeline {
    agent any
    stages {
        stage('Build') {
          steps {
            sh "mvn clean package -Pdev,logToFile -DskipTests=true -Djib.docker.image.tag=develop jib:dockerBuild"
          }
            }
        }
    }
}