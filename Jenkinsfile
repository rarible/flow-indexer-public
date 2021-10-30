@Library('shared-library@no-tests') _

def prefix = 'flow'
def stackName = 'protocol-flow'
def credentialsId = 'nexus-ci'
def services = [[name: 'backend-api', path: './backend-api'], [name: 'scanner', path: './scanner']]

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
                    env.IMAGE_TAG = "${env.GIT_BRANCH.replace("/", "_")}-1.0.${env.BUILD_NUMBER}"
                    env.VERSION = "${env.IMAGE_TAG}"
                    env.BRANCH_NAME = "${env.GIT_BRANCH}"
                }

                publishImages(prefix, env.VERSION)
            }
        }
        stage("deploy to dev") {
            agent any
            when {
                anyOf { branch 'main'; branch 'origin/main' }
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
                anyOf { branch 'release/*'; branch 'origin/release/*' }
                beforeAgent true
            }
            environment {
                APPLICATION_ENVIRONMENT = 'staging'
            }
            steps {
                deployStack('staging', stackName, prefix, env.IMAGE_TAG, services)
            }
        }

        stage("deploy to prod") {
            agent any
            when {
               allOf {
                   anyOf { branch 'release/*'; branch 'origin/release/*' }
                   expression {
                       input message: "Deploy to prod?"
                       return true
                   }
               }
               beforeAgent true
            }
            environment {
                APPLICATION_ENVIRONMENT = 'prod'
            }
            steps {
                deployStack('prod', stackName, prefix, env.IMAGE_TAG, services)
            }
        }
    }
}

@NoCPS
def publishImages(prefix, version) {
    services.each {
        def image = docker.build("rarible/${prefix}-${it.name}:${version}", it.path)
        docker.withRegistry('', 'rarible-docker-hub') {
            image.push()
        }
    }
}
