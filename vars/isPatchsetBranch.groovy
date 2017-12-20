
// isPatchsetBranch ?

Boolean call(String branch){
    branch =~ /^patchset\/[0-9]*\/[0-9]*\/[0-9]*/
}
