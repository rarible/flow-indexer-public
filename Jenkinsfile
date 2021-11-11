@Library('shared-library@new-cicd') _

def pipelineConfig = [
    "stackName": "protocol-flow",
    "services": [[name: 'backend-api', path: './backend-api'], [name: 'scanner', path: './scanner']]
]

serviceCI(pipelineConfig)