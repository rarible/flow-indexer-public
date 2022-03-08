@Library('shared-library@v2022.03.05-1') _

def pipelineConfig = [
    "stackName": "protocol-flow",
    "services": [[name: 'backend-api', path: './backend-api'], [name: 'scanner', path: './scanner']],
    "slackChannel": "flow-build"
]

serviceCI(pipelineConfig)
