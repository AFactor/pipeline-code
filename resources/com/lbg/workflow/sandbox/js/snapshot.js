var system = require('system');
var env = system.env;

var resourceWait  = 300,
    maxRenderWait = 10000,
    url           = 'http://jenkins.sandbox.extranet.group/jenkins2/j_acegi_security_check';

var page          = require('webpage').create(),
    count         = 5,
    forcedRenderTimeout,
    transitions	  = 0,
    renderTimeout;
var jobURL = env.jobURLPath
var jenkinsuser = env.JENKINS_USER
var jenkinspass = env.JENKINS_PASS
console.log('jobURL: ' + jobURL)
var postBody = 'j_username='+ jenkinsuser  +'&j_password='+ jenkinspass +'&remember_me=on&json=init&from=' + jobURL + 'display/redirect'


page.viewportSize = { width: 875, height : 50000 };
function doRender(filename) {
	console.log('rendering png ' + filename);

	page.zoomFactor = 0.75;
	page.clipRect = {
		top: 8,
		left: 0,
		width: 875,
		height: 480
	};
    page.render(filename, {format: 'png', quality: '100'});
};

page.onInitialized = function() {
     if(page.injectJs('node_modules/babel-polyfill/browser.js')){
         console.log("Babel-Polyfill loaded");
     }    
};

page.onResourceRequested = function (req) {
   count += 1;
   console.log('> ' + req.id + ' - ' + req.url);
   clearTimeout(renderTimeout);
};

page.onResourceReceived = function (res) {
    if (!res.stage || res.stage === 'end') {
        count -= 1;
        console.log(res.id + ' ' + res.status + ' - ' + res.url);
        if (count === 0) {
            renderTimeout = setTimeout(doRender('jenkins_temp.png'), resourceWait);
        }
    }
};

 page.onLoadFinished = function(status){
	console.log('Load Finished');
	 if (status !== "success") {
        console.log('Unable to load url');
        phantom.exit();
     } else if (transitions === 0){
    	console.log('Remaining Transitions ' + transitions);
     	doRender('jenkins_finished.png');
    	phantom.exit();
     } else { 
     	console.log('Remaining Transitions ' + transitions);
     	transitions -= 1;
     }
};
 page.onPageReady = function(status){
       	doRender('jenkins_golden.png');
      	phantom.exit();
};   

page.open(url, 'POST', postBody, function (status) {
    if (status !== "success") {
        console.log('Unable to load url');
        phantom.exit();
    } else {
   	 console.log('success');
        forcedRenderTimeout = setTimeout(function () {
           doRender('jenkins_timeout.png');      
        }, maxRenderWait);
    }
});
