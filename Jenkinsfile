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
        sh 'mvn clean test -U'
      }
      post {
		always {
		  junit allowEmptyResults: true, testResults: '**/surefire-reports/*.xml'
		}
      }
    }
    stage('package') {
    	when {
				branch 'master'
			}
      steps {
        sh 'mvn clean package deploy -DskipTests'
        script {
          env.IMAGE_TAG = "1.0.${env.BUILD_NUMBER}"
          env.VERSION = "${env.IMAGE_TAG}"
        }
      }
    }
    stage('build images and publish') {
    	when {
				branch 'master'
			}
      steps {
        sh 'docker login -u ${REGISTRY_ACCOUNT} -p ${REGISTRY_PASSWORD}'

        sh '''
          export IMAGE_NAME=protocol-erc20-api
          
          docker build \
           -t ${REGISTRY_ACCOUNT}/${IMAGE_NAME}:$IMAGE_TAG api

          docker push ${REGISTRY_ACCOUNT}/${IMAGE_NAME}:$IMAGE_TAG
        '''

        sh '''
          export IMAGE_NAME=protocol-erc20-listener
          
          docker build \
           -t ${REGISTRY_ACCOUNT}/${IMAGE_NAME}:$IMAGE_TAG listener

          docker push ${REGISTRY_ACCOUNT}/${IMAGE_NAME}:$IMAGE_TAG
        '''

        sh '''
          export IMAGE_NAME=protocol-erc20-events-importer
          
          docker build \
           -t ${REGISTRY_ACCOUNT}/${IMAGE_NAME}:$IMAGE_TAG events-importer

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
				branch 'master'
			}
      environment {
        APPLICATION_ENVIRONMENT = 'dev'
      }
      steps {
				input('Deploy to dev')
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
    stage('deploy e2e') {
    	when {
				branch 'master'
			}
      environment {
        APPLICATION_ENVIRONMENT = 'e2e'
      }
      steps {
        input('Deploy to e2e')
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
    stage('deploy staging') {
    	when {
				branch 'master'
			}
      environment {
        APPLICATION_ENVIRONMENT = 'staging'
      }
      steps {
        input('Deploy to staging')
        sh '''
          docker login -u ${REGISTRY_ACCOUNT} -p ${REGISTRY_PASSWORD}
          
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
    stage('deploy prod') {
    	when {
				branch 'master'
			}
      environment {
        APPLICATION_ENVIRONMENT =  'prod'
      }
      steps {
        input('Deploy to prod')
        sh '''
          docker login -u ${REGISTRY_ACCOUNT} -p ${REGISTRY_PASSWORD}
          
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
