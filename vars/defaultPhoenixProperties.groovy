// Returns default phoenix pipeline properties that we use in most of the jobs
// usage - in Jenkinsfile:
// def processes = 'Stage\nStage and Activate\nUnstage\nOther process'
// properties(defaultPhoenixProperties(processes))

def call(String processes){
    defaultBuildJobProperties() +
    parameters([
        string(choices: '', description: 'Name of the artifact you wish to run process for', name: 'artifactName'),
        choice(defaultValue: 'no',  choices: 'no\nyes', description: 'Do you wish to deploy?', name: 'deploy'),
        choice(defaultValue: 'yes', choices: 'yes\nno', description: 'Upload the artifact from nexus to UrbanCode?', name: 'upload'),
        // choice(defaultValue: 'no',  choices: 'no\nyes', description: 'Execute PreBDD phase?', name: 'pre_bdd'),
        // choice(defaultValue: 'no',  choices: 'no\nyes', description: 'Execute PostBDD phase?', name: 'post_bdd'),
        choice(choices: processes, description: 'Which process do you wish to execute?', name: 'process'),
        choice(defaultValue: 'yes', choices: 'yes\nno', description: 'Deploy changes only?', name: 'onlyChanged')
    ])
}
