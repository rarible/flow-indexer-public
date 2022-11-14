@Library('shared-library') _

def pipelineConfig = [
    "services": [[name: 'backend-api', path: './backend-api'], [name: 'scanner', path: './scanner']],
    "slackChannel": "flow-build"
]

pipelineAppCI(pipelineConfig)
