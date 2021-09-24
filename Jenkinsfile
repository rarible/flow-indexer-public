@Library('shared-library@no-tests') _

def prefix = 'flow'
def stackName = 'protocol-flow'
def credentialsId = 'nexus-ci'
def services = [[name: 'backend-api', path: './backend-api'], [name: 'backend-listener', path: './backend-listener'], [name: 'scanner', path: './scanner']]

pipeline {
    agent none

    options {
        disableConcurrentBuilds()
    }

    stages {
        stage('test') {
            agent any
            steps {
                sh './gradlew clean test coverage --no-daemon --info --refresh-dependencies'
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: '**/surefire-reports/*.xml'
                    step([ $class: 'JacocoPublisher', execPattern: '**/build/jacoco/coverageMerge.exec' ])
                }
            }
        }
        stage('package') {
            agent any
            steps {
                sh './gradlew build -x test --no-daemon --info'
            }
        }
        stage('publish docker images') {
            agent any
            steps {
                script {
                    env.IMAGE_TAG = "1.0.${env.BUILD_NUMBER}"
                    env.VERSION = "${env.IMAGE_TAG}"
                    env.BRANCH_NAME = "${env.GIT_BRANCH}"
                }
                publishDockerImages(prefix, credentialsId, env.IMAGE_TAG, services)
            }
        }
        stage("deploy to dev") {
            agent any
            when {
                allOf {
                    expression {
                        return env.BRANCH_NAME == 'origin/main' || env.BRANCH_NAME == 'main'
                    }
                }
                beforeAgent true
            }
            environment {
                APPLICATION_ENVIRONMENT = 'dev'
            }
            steps {
                deployStack("dev", stackName, prefix, env.IMAGE_TAG, services)
            }
        }

        stage("deploy to staging") {
            agent any
            when {
                allOf {
                    expression {
                        return env.BRANCH_NAME == 'origin/main' || env.BRANCH_NAME == 'main'
                    }
                }
                beforeAgent true
            }
            environment {
                APPLICATION_ENVIRONMENT = 'staging'
            }
            steps {
                deployStack('staging', stackName, prefix, env.IMAGE_TAG, services)
            }
        }

        stage("deploy scanner to prod") {
            agent any
            when {
                allOf {
                    expression {
                        return env.BRANCH_NAME == 'origin/main' || env.BRANCH_NAME == 'main'
                    }
                }
                beforeAgent true
            }
            environment {
                APPLICATION_ENVIRONMENT = 'prod'
            }
            steps {
                deployStack('prod', stackName, prefix, env.IMAGE_TAG, [[name: 'scanner', path: './scanner']])
            }
        }
    }
}
