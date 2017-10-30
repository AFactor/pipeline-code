/*
 * Author: Abhay Chrungoo <achrungoo@sapient.com>
 * Contributing HOWTO: TODO
 */

Boolean call(String branch){
        if (branch =~ /^release-prod.*$/ || branch =~ /^releases\/.*$/ || branch =~ /^release\/.*$/ ) {
		return true ;
        } else if (branch =~ /^master$/ ) {
		return true ;
        } else if (branch =~ /^hotfix\/.*$/ ) {
            return true ;
        } else if (branch =~ /^hotfixes.*$/ ) {
		return true ;
        } else if (branch =~ /^develop$/ ) {
		return true ;
        } else if ( branch =~ /^bugfix\/.*$/ ) {
		return true ;
        } else {
		return false;
        }
}
