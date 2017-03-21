/*
 * Author: Abhay Chrungoo <achrungoo@sapient.com>
 * Contributing HOWTO: TODO
 */

package com.lbg.workflow.sandbox

public class JsonUtils implements Serializable{
	public JsonUtils(){
	}
	def parse(String text) {
		final HashMap response =  (new HashMap(
				new groovy.json.JsonSlurperClassic().
				parseText(text)
				)
				).asImmutable()
		return response
	}
}
