pipeline {
  agent any

    options {
        disableConcurrentBuilds()
    }

  environment {
    SWARM_MANAGER_HOST = credentials('swarm-manager-ip-address')

    REGISTRY_ACCOUNT = credentials('registry-login')
    REGISTRY_PASSWORD = credentials('registry-password')

    STACK_DIR = 'ci-provision'

    BLOCKCHAIN = 'flow'
    STACK_NAME = 'protocol-ethereum'
  }
  stages {
    stage('test') {
      steps {
        sh './gradlew clean test --no-daemon --stacktrace'
      }
      post {
        always {
          junit allowEmptyResults: true, testResults: '**/test-results/test/*.xml'
        }
      }
    }
    stage('package') {
      when {
        branch 'main'
      }
      steps {
        sh './gradlew build -x test --no-daemon --stacktrace'
        script {
          env.IMAGE_TAG = "1.0.${env.BUILD_NUMBER}"
          env.VERSION = "${env.IMAGE_TAG}"
        }
      }
    }
    stage('build images and publish') {
      when {
        branch 'main'
      }
      steps {
        sh 'docker login -u ${REGISTRY_ACCOUNT} -p ${REGISTRY_PASSWORD}'

        sh '''
          export IMAGE_NAME=flow-indexer-api
          
          docker build \
           -t ${REGISTRY_ACCOUNT}/${IMAGE_NAME}:$IMAGE_TAG backend-api

          docker push ${REGISTRY_ACCOUNT}/${IMAGE_NAME}:$IMAGE_TAG
        '''

        sh '''
          export IMAGE_NAME=flow-indexer-listener

          docker build \
           -t ${REGISTRY_ACCOUNT}/${IMAGE_NAME}:$IMAGE_TAG backend-listener

          docker push ${REGISTRY_ACCOUNT}/${IMAGE_NAME}:$IMAGE_TAG
        '''

        sh '''
          export IMAGE_NAME=flow-indexer-scanner

          docker build \
           -t ${REGISTRY_ACCOUNT}/${IMAGE_NAME}:$IMAGE_TAG scanner

          docker push ${REGISTRY_ACCOUNT}/${IMAGE_NAME}:$IMAGE_TAG
        '''

        script {
          env.DOCKER_HOST = "ssh://jenkins@${SWARM_MANAGER_HOST}"
        }
      }
      post {
        always {
          sh 'docker logout'
        }
      }
    }
    stage('deploy dev') {
      when {
        branch 'main'
      }
      environment {
        APPLICATION_ENVIRONMENT = 'dev'
      }
      steps {
        sh '''
          docker login -u ${REGISTRY_ACCOUNT} -p ${REGISTRY_PASSWORD} ${REGISTRY_URL}
          
          test -f ${STACK_DIR}/${APPLICATION_ENVIRONMENT}-variables.env && export $(cat ${STACK_DIR}/${APPLICATION_ENVIRONMENT}-variables.env)
          envsubst < ${STACK_DIR}/template/docker-stack.tmpl > ${STACK_DIR}/docker-stack.yml
          docker stack deploy -c ${STACK_DIR}/docker-stack.yml --with-registry-auth ${APPLICATION_ENVIRONMENT}-${STACK_NAME}
        '''
      }
      post {
        always {
          sh 'docker logout'
        }
      }
    }
  }
}
