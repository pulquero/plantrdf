
d3sparql.update = function(endpoint, sparql, callback) {
  var url = endpoint
  if (d3sparql.debug) { console.log(endpoint) }
  if (d3sparql.debug) { console.log(url) }
  d3.xhr(url)
  .header("Content-Type", "application/x-www-form-urlencoded")
  .post("update=" + encodeURIComponent(sparql.replace(/\s+/g, ' ')), callback)
};

d3sparql.ask = function(endpoint, sparql, callback) {
  var url = endpoint + "?query=" + encodeURIComponent(sparql.replace(/\s+/g, ' '))
  if (d3sparql.debug) { console.log(endpoint) }
  if (d3sparql.debug) { console.log(url) }
  var mime = "text/boolean"
  d3.xhr(url, mime, function(request) {
    var txt = request.responseText
    if (d3sparql.debug) { console.log(txt) }
    callback(txt == 'true')
  })
};
