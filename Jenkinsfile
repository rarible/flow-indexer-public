@Library('shared-library@v21.11.30-1') _

def pipelineConfig = [
    "stackName": "protocol-flow",
    "services": [[name: 'backend-api', path: './backend-api'], [name: 'scanner', path: './scanner']],
    "slackChannel": "flow-build"
]

serviceCI(pipelineConfig)
