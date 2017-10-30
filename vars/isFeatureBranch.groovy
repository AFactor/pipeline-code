/*
 * Author: Abhay Chrungoo <achrungoo@sapient.com>
 * Contributing HOWTO: TODO
 */

Boolean call(String branch){
	if (branch =~ /^sprint[0-9]+\/.+$/ || branch =~ /^epic\/.+$/ || branch =~ /^feature\/.*$/ ) {
		return true ;
	} else {
		return false ;
	}
}
