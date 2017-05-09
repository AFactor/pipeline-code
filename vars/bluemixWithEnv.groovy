import com.lbg.workflow.sandbox.deploy.UtilsBluemix

def call(bluemixConfig) {
	UtilsBluemix utils = new UtilsBluemix()
	def envs = utils.buildBluemixEnv(bluemixConfig)
	return utils.toWithEnv(envs)
}


def call(serviceBluemixConfig, globalBluemixConfig) {
    UtilsBluemix utils = new UtilsBluemix()
    def envs = utils.buildBluemixEnv(globalBluemixConfig)
    for (e in serviceBluemixConfig) {
        envs[e.key] = e.value
    }
    return utils.toWithEnv(envs)
}