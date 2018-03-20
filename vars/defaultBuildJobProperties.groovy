// Returns default job properties that we use in most of the build jobs
// usage - in Jenkinsfile: properties(defaultBuildJobProperties())

def call(){
    [
        buildDiscarder(logRotator(artifactDaysToKeepStr: '30', artifactNumToKeepStr: '10', daysToKeepStr: '30', numToKeepStr: '15')),
        [$class: 'RebuildSettings', autoRebuild: true, rebuildDisabled: false]
    ]
}
