// needed when hosted outside of portal as is the case
// when hosting in figwheel. use the appropriate port number
// on which the portal service is listening.
// var portalSiteRoot = "http://" + location.hostname + ":3000";
var portalSiteRoot = "https://topsoedev-portal.azurewebsites.net";


// Must be the last line!!
// use appropriate build
document.write("<script src='js/dev/app.js' type='text/javascript'></script>");
