
// isPullRequestBranch ?

Boolean call(String branch){
    branch =~ /^PR-[0-9]+	  ||
    branch =~ /^PR-[0-9]+-head/   ||
    branch =~ /^PR-[0-9]+-merge/   
}
