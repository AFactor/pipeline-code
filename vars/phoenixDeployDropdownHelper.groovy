import com.lbg.workflow.sandbox.deploy.UtilsUCD


def call(String artifactBaseName, String nexusURL) {
    def utils = new UtilsUCD()
    def artifactNames = utils.getNexusArtifactNameFromMetadata(artifactBaseName, nexusURL)

    return artifactNames
}

return this;
