import com.lbg.workflow.sandbox.BuildContext
import com.lbg.workflow.sandbox.Utils
import com.lbg.workflow.sandbox.JobStats

def call(String application, handlers, String configuration) {
    this.call(application, handlers, configuration, 'lloydscjtdevops@sapient.com', 120)
}

def call(String application, handlers, String configuration, String notifyList) {
    this.call(application, handlers, configuration, notifyList, 120)
}

def call(String application, handlers, String configuration, Integer timeoutInMinutes) {
    this.call(application, handlers, configuration, 'lloydscjtdevops@sapient.com', timeoutInMinutes)
}

def call(String application,
         handlers,
         String configuration,
         String notifyList,
         Integer timeoutInMinutes) {

    try {
        echo "Start BuildPipelineHawk for ${configuration} / ${notifyList} / ${timeoutInMinutes}"

        timeout(timeoutInMinutes) {
            this.callHandler(application, handlers, configuration)
            currentBuild.result = 'SUCCESS'
        }

    } catch (error) {
        currentBuild.result = 'FAILURE'
        echo "BuildPipelineHawk caught exception [" + error.getMessage() + "]."
        throw error

    } finally {
        if (notifyList?.trim()) {
            emailNotify { to = notifyList }
        }
        echo "Finally invoke Splunk after " + currentBuild.result
        def jobStats = new JobStats()
        jobStats.toSplunk(env.BUILD_TAG, env.BUILD_URL, "jenkins-read-all", currentBuild.result, "")
    }

}

def callHandler(String application, handlers, String configuration) {
    def targetCommit
    def branch
    BuildContext context
    Utils utils = new Utils()

    echo "Cleanup.."
    step([$class: 'WsCleanup', notFailBuild: true])

    // TODO: better way to fix this ? no matching key exchange method found. Their offer: diffie-hellman-group1-sha1
    fixGerritKeyExchangeIssue()
    configureMavenSettings()

    echo "Checking out from scm.."
    checkout scm

    // env.BRANCH_NAME is only available in multibranch pipeline jobs
    // to support scheduled pipeline jobs, we define and use local branch name

    branch = env.BRANCH_NAME
    if (branch == null) {
        branch = sh(returnStdout: true, script: "git rev-parse --abbrev-ref HEAD").trim()
    }
    echo "Determine commit id.."
    targetCommit = sh(returnStdout: true, script: 'git rev-parse HEAD').trim()

    context = new BuildContext(application, readFile(configuration))

    if (isPatchsetBranch(branch)) {

        echo "PatchsetWorkflow for ${targetCommit}.."
        swiftPatchsetWorkflow(context, handlers, targetCommit)

    } else if (isFeatureBranch(branch)) {

        branch1 = "ft-" + utils.friendlyName(branch, 20)
        echo "FeatureWorkFlow for ${branch1}.."
        swiftFeatureWorkflow(context, handlers, branch1)

    } else if (isIntegrationBranch(branch)) {

        branch1 = utils.friendlyName(branch, 40)
        echo "IntegrationWorkFlow for ${branch1}.."
        swiftIntegrationWorkflow(context, handlers, branch1)

    } else {
        error "No known git-workflow rule for branch called ${branch}"
    }
    echo "End BuildPipelineHawk for ${branch}"
}

private fixGerritKeyExchangeIssue() {
    sh 'mkdir -p $HOME/.ssh && touch $HOME/.ssh/config && \
        echo -en  "Host gerrit.sandbox.extranet.group\nKexAlgorithms +diffie-hellman-group1-sha1" > $HOME/.ssh/config'
}

private configureMavenSettings() {
    sh('mkdir -p $HOME/.m2')
    sh("echo -en '${mavenSettingsText()}' > \$HOME/.m2/settings.xml")
}

private mavenSettingsText() {
    '''
<settings>
    <servers>
        <server>
            <id>nexus-releases</id>
            <username>${nexus.user}</username>
            <password>${nexus.password}</password>
        </server>
        <server>
            <id>nexus-snapshots</id>
            <username>${nexus.user}</username>
            <password>${nexus.password}</password>
        </server>
    </servers>
    <mirrors>
        <mirror>
            <id>mirror</id>
            <mirrorOf>external:*</mirrorOf>
            <url>https://nexus.sandbox.extranet.group/nexus/content/groups/maven</url>
        </mirror>
    </mirrors>
</settings>
'''
}

return this
