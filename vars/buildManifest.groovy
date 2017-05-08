import com.lbg.workflow.sandbox.deploy.ManifestBuilder

def call(appName, env, bluemix) {
	ManifestBuilder manifestBuilder = new ManifestBuilder()
	return manifestBuilder.build(appName, env, bluemix)
}
