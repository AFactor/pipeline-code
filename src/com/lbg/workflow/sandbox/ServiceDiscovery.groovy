/*
 * Author: Abhay Chrungoo <achrungoo@sapient.com>
 * Contributing HOWTO: TODO
 */

package com.lbg.workflow.sandbox

class ServiceDiscovery implements Serializable{


	private String vaultUrl= 'http://10.113.140.168:8200';


	public CWABuildHandlers() {
		initialize()
	}
	private initialize(){
		//Nothing here yet
	}

	public String locate(String service){

		switch(service){
			case 'vault': return this.vaultUrl; break;
			default: return fail(service)
		}
	}
	
	private String fail(String service){
		throw new MalformedURLException("ServiceDiscovery: Cant Locate ${service}. A fake exception to signal this error")
	}
}

