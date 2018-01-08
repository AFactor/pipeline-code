
// isFeatureBranch ?

Boolean call(String branch){
    branch =~ /^sprint[0-9]+\/.+$/   ||
    branch =~ /^epic\/.+$/           ||
    branch =~ /^pr\/[0-9]+\/head$/   ||
    branch =~ /^pr\/[0-9]+\/merge$/
}
