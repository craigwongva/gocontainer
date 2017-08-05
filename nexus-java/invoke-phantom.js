var webpage = require('webpage');
var page = webpage.create()
page.open('http://CHANGEME:8080/green/timer/dots', function(status) {
  page.close();
});
