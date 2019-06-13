pipeline {
    agent any
    stages {
        stage('Build') {
          steps {
            checkout([
              $class: 'GitSCM',
              branches: [[name: 'develop']],
              doGenerateSubmoduleConfigurations: false,
              extensions: [],
              submoduleCfg: [],
              userRemoteConfigs: [[
                credentialsId: 'bc074014-bab1-4fb0-b5a4-4cfa9ded5e66',
                url: 'git@bitbucket.org:citeck/ecos-uiserv.git'
              ]]
            ])
          }
          steps {
            sh "mvn clean package -Pdev,logToFile -DskipTests=true -Djib.docker.image.tag=develop jib:dockerBuild"
          }
        }
        stage('Push docker images') {
          steps {
            withCredentials([usernamePassword(credentialsId: '3400f5ec-0ef3-4944-b59a-97e67680777a', passwordVariable: 'pass', usernameVariable: 'user')]) {
              sh "docker login -u $user -p $pass nexus.citeck.ru"
              sh "docker push nexus.citeck.ru/uiserv"
            }
          }
        }
    }
}
