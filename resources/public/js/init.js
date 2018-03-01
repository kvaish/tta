// some global settings placeholder to collaborate with cljs
var htAppEnv = {
    mode: null, //set at runtime by setup.cljs, "dev" for dev mode
    // use this to disable prompting on navigating away
    leaveSilently : false
};

window.addEventListener("beforeunload", function(e) {
    if(!htAppEnv.leaveSilently) {
        var dialogText = "Do you really want to leave this page?\n\nPress OK to continue or Cancel to stay on the current page.";
        e.returnValue = dialogText;
        return dialogText;
    }
    // e.returnValue = undefined;
    // return undefined;
    // TODO:
});

document.onreadystatechange = function() {
    if(document.readyState === "interactive") {
        console.log("DOM ready!");

        var body = document.getElementsByTagName("body")[0];
        body.addEventListener("keydown", function(e) {
            var rx = /INPUT|TEXTAREA/i; //|SELECT
            var tx = /checkbox|radio/i;
            // capture BACKSPACE (keyCode=8) for elements other than input, contentEditables, textarea
            // console.log(e.target);
            if(e.keyCode == 8
               && (!rx.test(e.target.tagName) || tx.test(e.target.type)
                   || e.target.disabled || e.target.readOnly)) {
                e.preventDefault();
                e.stopPropagation();
            }
        });
    }
};

// Must be the last line!!
// use appropriate build
document.write("<script src='js/"+ htAppConfig.buildId +"/app.js' type='text/javascript'></script>");
