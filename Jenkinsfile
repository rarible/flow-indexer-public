@Library('shared-library@v20211111-1') _

def pipelineConfig = [
    "stackName": "protocol-flow",
    "services": [[name: 'backend-api', path: './backend-api'], [name: 'scanner', path: './scanner']]
]

serviceCI(pipelineConfig)