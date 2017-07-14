def call(targetDir, envConfig) {
    try {
        echo "replaceTokens: targetDir:$targetDir, envConfig: $envConfig"
        String envConfigArray = "("
        if (envConfig != null) {
            for (e in envConfig) {
                envConfigArray += "\n[${e.key}]=\"${e.value}\""
            }
        }
        envConfigArray += "\n)"
        sh "mkdir -p pipelines/scripts/"
        writeFile file: "pipelines/scripts/replaceTokens.sh", text: replaceTokensScript(targetDir, envConfigArray)
        sh "source pipelines/scripts/replaceTokens.sh; replaceTokens"
    }catch (error) {
        echo "Failed to replace tokens $error.message"
        throw error
    }


}

private String replaceTokensScript(targetDir, envConfigArray) {
    return """
#!/usr/bin/env bash
set -e
targetDir="$targetDir"
declare -A envConfig=$envConfigArray
function replaceTokens() {
    for i in "\${!envConfig[@]}"
    do
        key=\$i
        val=\${envConfig[\$i]}
        echo "\$key : \$val"
        find \${targetDir} -type f -exec sed -i "s~%%\$key%%~\${val}~g;s~&&\$key&&~\${val}~g;s~##\$key##~\${val}~g;" {} +
    done
}
    """
}
return this;