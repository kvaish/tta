var htAppConfig = {
  appId: "truetemp",

  // set to "" when hosted in portal itself
  portalUri: "",

  serviceUri: "", //TODO: set to correct url

  languages: [
    { code: 'en', flag: 'gb', name: 'English' },
    { code: 'es', flag: 'es', name: 'Español' },
    { code: 'ru', flag: 'ru', name: 'русский' },
  ],

}

// Must be the last line!!
// use appropriate build
document.write("<script src='js/min/app.js' type='text/javascript'></script>");