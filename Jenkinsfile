@Library('shared-library@v20211117-3') _

def pipelineConfig = [
    "stackName": "protocol-flow",
    "services": [[name: 'backend-api', path: './backend-api'], [name: 'scanner', path: './scanner']]
]

serviceCI(pipelineConfig)
