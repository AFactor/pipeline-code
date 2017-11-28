var system = require('system');
var env = system.env;

var resourceWait  = 300,
    maxRenderWait = 10000;

var page          = require('webpage').create(),
    count         = 5,
    forcedRenderTimeout,
    transitions	  = 0,
    renderTimeout;

var url = env['JENKINS_URL'] +'j_acegi_security_check'
var buildPath = env['BUILD_PATH']
var jenkinsuser = env['JENKINS_USER']
var jenkinspass = env['JENKINS_PASS']
var imagefile = env['IMAGEFILE']
//console.log('buildPath: ' + buildPath)
var postBody = 'j_username='+ jenkinsuser +'&j_password='+ jenkinspass +'&remember_me=on&json=init&from=/'+ buildPath


page.viewportSize = { width: 1200, height : 50000 };
function doRender(filename){
	console.log('rendering png ' + filename);

	page.zoomFactor = 1;
	page.clipRect = {
		top: 8,
		left: 0,
		width: 1200,
		height: 2400
	};
    page.render(filename, {format: 'png', quality: '0'});
};

page.onInitialized = function(){
     if(page.injectJs('browser.js')){
         console.log("Babel-Polyfill loaded");
     }
};

page.onResourceRequested = function (req){
   count += 1;
  // console.log('> ' + req.id + ' - ' + req.url);
   clearTimeout(renderTimeout);
};

page.onResourceReceived = function (res){
    if (!res.stage || res.stage === 'end'){
        count -= 1;
      //  console.log(res.id + ' ' + res.status + ' - ' + res.url);
        if (count === 0) {
            renderTimeout = setTimeout(doRender('onReceived_' + imagefile), resourceWait);
        }
    }
};

page.onLoadFinished = function(status){
	console.log('Load Finished');
	 if (status !== "success") {
        console.log('Unable to load url');
        phantom.exit();
     } else if (transitions === 0){
    //	console.log('Remaining Transitions ' + transitions);
     	doRender('onLoaded_' + imagefile);
    	//phantom.exit();
     } else {
     //	console.log('Remaining Transitions ' + transitions);
     	transitions -= 1;
     }
};

page.onPageReady = function(status){
       	doRender('onReady_' + imagefile);
};

page.open(url, 'POST', postBody, function (status) {
    if (status !== "success") {
        console.log('Unable to load url');
        phantom.exit();
    } else {
   	 console.log('success');
        forcedRenderTimeout = setTimeout(function () {
           doRender(imagefile);
           phantom.exit();
        }, maxRenderWait);
    }
});
