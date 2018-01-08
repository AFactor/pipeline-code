
// isIntegrationBranch ?

Boolean call(String branch){
    branch =~ /^release-prod.*$/ ||
    branch =~ /^releases\/.*$/   ||
    branch =~ /^release\/.*$/    ||
    branch =~ /^release-.*$/     ||
    branch =~ /^master$/         ||
    branch =~ /^hotfix\/.*$/     ||
    branch =~ /^hotfixes.*$/     ||
    branch =~ /^develop$/        ||
    branch =~ /^bugfix\/.*$/     ||
    branch =~ /^feature\/.*$/    ||
    branch =~ /^feat-.*$/
}
