@Library('shared-library') _

def pipelineConfig = [
    "stackName": "protocol-flow",
    "services": [[name: 'backend-api', path: './backend-api'], [name: 'scanner', path: './scanner']],
    "slackChannel": "flow-build"
]

serviceCI(pipelineConfig)
