/*
 * Author: Abhay Chrungoo <achrungoo@sapient.com>
 * Contributing HOWTO: TODO
 */

Boolean call(String branch){
        if (branch =~ /^patchset\/[0-9]*\/[0-9]*\/[0-9]*/ ) {
		return true;
	} else 
		return false;
}
