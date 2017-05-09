import com.lbg.workflow.sandbox.deploy.UtilsBluemix

def call(bluemixConfig) {
	UtilsBluemix utils = new UtilsBluemix()
	def envs = utils.buildBluemixEnv(bluemixConfig)
	return utils.toWithEnv(envs)
}
