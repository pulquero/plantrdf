d3sparql.update = function(endpoint, sparql, callback) {
  var url = endpoint
  if (d3sparql.debug) { console.log(endpoint) }
  if (d3sparql.debug) { console.log(url) }
  d3.xhr(url)
  .header("Content-Type", "application/x-www-form-urlencoded")
  .post("update=" + encodeURIComponent(sparql), callback)
}

d3sparql.ask = function(endpoint, sparql, callback) {
  var url = endpoint + "?query=" + encodeURIComponent(sparql)
  if (d3sparql.debug) { console.log(endpoint) }
  if (d3sparql.debug) { console.log(url) }
  var mime = "text/boolean"
  d3.xhr(url, mime, function(request) {
    var txt = request.responseText
    if (d3sparql.debug) { console.log(txt) }
    callback(txt == 'true')
  })
}

function localName(uri) {
	return uri.substring(uri.lastIndexOf('#')+1);
}

queries = {
	getPlants: function(graph) {
		return `
		prefix p: <http://plantrdf-morethancode.rhcloud.com/schema#>
	
		select
			?plant
			(coalesce(?zprovenance,'') as ?provenance)
			(coalesce(?zspecies,'') as ?species)
			(coalesce(?zhardiness,'') as ?hardiness)
		from <${graph}>
		where {
			?plant a p:Plant .
			optional
			{
				?plant p:provenance ?zprovenance .
			}
			optional
			{
				?plant p:lsid ?zspecies .
				optional
				{
					?zspecies p:hardiness ?zhardiness .
				}
			}
		}
		order by ?plant
		`;
	},

	getProvenances: function() {
		return `
		prefix p: <http://plantrdf-morethancode.rhcloud.com/schema#>
	
		select distinct
			?p
		where {
			?s p:provenance ?p .
		}
		order by ?p
		`;
	},

	getHardinessRatings: function() {
		return `
		prefix p: <http://plantrdf-morethancode.rhcloud.com/schema#>
	
		select
			?h
		where {
			?h a p:HardinessRating .
		}
		order by ?h
		`;
	},

	replaceResource: function(oldResource, newResource, type, graph) {
		if(oldResource == '') {
			return `
			insert data {
				graph <${graph}> {
					<${newResource}> a <${type}> .
				}
			}
			`;
		}
		else if(newResource == '') {
			return `
			delete {
				graph <${graph}> {
					<${oldResource}> ?p ?o .
					?s ?p <${oldResource}> .
				}
			}
			where {
				graph <${graph}> {
					{
						<${oldResource}> ?p ?o .
					}
					union
					{
						?s ?p <${oldResource}> .
					}
				}
			}
			`;
		}
		else {
			if(oldResource.startsWith(graph)) {
				// resource owner - global change
				return `
				delete {
					graph ?g {
						<${oldResource}> ?p ?o .
						?s ?p <${oldResource}> .
					}
				}
				insert {
					graph ?g {
						<${newResource}> ?p ?o .
						?s ?p <${newResource}> .
					}
				}
				where {
					graph ?g {
						{
							<${oldResource}> ?p ?o .
						}
						union
						{
							?s ?p <${oldResource}> .
						}
					}
				}
				`;
			}
			else {
				// not resource owner - local change
				return `
				delete {
					graph <${graph}> {
						<${oldResource}> ?p ?o .
						?s ?p <${oldResource}> .
					}
				}
				insert {
					graph <${graph}> {
						<${newResource}> ?p ?o .
						?s ?p <${newResource}> .
					}
				}
				where {
					graph <${graph}> {
						{
							<${oldResource}> ?p ?o .
						}
						union
						{
							?s ?p <${oldResource}> .
						}
					}
				}
				`;
			}
		}
	},

	checkExists: function(resource, graph) {
		return `
		ask {
			graph <${graph}> {
				{
					<${resource}> ?p ?o .
				}
				union
				{
					?s ?p <${resource}> .
				}
			}
		}
		`;
	},

	replaceProperty: function(subj, pred, oldResource, newResource, graph) {
		let oldResourceLiteral = this.toLiteral(oldResource);
		let newResourceLiteral = this.toLiteral(newResource);
		if(oldResource == '') {
			// insert only
			return `
			insert data {
				graph <${graph}> {
					<${subj}> <${pred}> ${newResourceLiteral} .
				}
			}
			`;
		}
		else if(newResource == '') {
			// delete only
			return `
			delete data {
				graph <${graph}> {
					<${subj}> <${pred}> ${oldResourceLiteral} .
				}
			}
			`;
		}
		else {
			// replace
			return `
			with <${graph}>
			delete {
				<${subj}> <${pred}> ${oldResourceLiteral} .
			}
			insert {
				<${subj}> <${pred}> ${newResourceLiteral} .
			}
			where {
			}
			`;
		}
	},

	replaceProperty2: function(subj, pred1, pred2, oldResource, newResource, graph) {
		let oldResourceLiteral = this.toLiteral(oldResource);
		let newResourceLiteral = this.toLiteral(newResource);
		if(oldResource == '') {
			return `
			with <${graph}>
			insert {
				?o <${pred2}> ${newResourceLiteral} .
			}
			where {
				<${subj}> <${pred1}> ?o .
			}
			`;
		}
		else if(newResource == '') {
			return `
			with <${graph}>
			delete {
				?o <${pred2}> ${oldResourceLiteral} .
			}
			where {
				<${subj}> <${pred1}> ?o .
			}
			`;
		}
		else {
			return `
			with <${graph}>
			delete {
				?o <${pred2}> ${oldResourceLiteral} .
			}
			insert {
				?o <${pred2}> ${newResourceLiteral} .
			}
			where {
				<${subj}> <${pred1}> ?o .
			}
			`;
		}
	},

	toLiteral: function(resource) {
		let pos = resource.indexOf(':');
		if(pos != -1) {
			return "<"+resource+">";
		}
		else {
			return "\"\"\""+resource+"\"\"\"";
		}
	}
};
