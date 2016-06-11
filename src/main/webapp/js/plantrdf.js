
function getQueryEndpoint(garden) {
	return SESAME_URL + "repositories/" + garden;
}

function getUpdateEndpoint(garden) {
	return SESAME_URL + "repositories/" + garden + "/statements";
}

function localName(uri) {
	let pos = uri.lastIndexOf('#');
	if(pos == -1) {
		pos = uri.lastIndexOf(':');
	}
	if(pos == -1) {
		pos = uri.lastIndexOf('/');
	}
	return uri.substring(pos+1);
}

queries = {
	getPlants: function(graph) {
		return `
		prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>
		prefix p: <http://plantrdf-morethancode.rhcloud.com/schema#>
	
		select
			?plant
			(coalesce(?zlabel,'') as ?label)
			(coalesce(?zprovenance,'') as ?provenance)
			(coalesce(?zspecies,'') as ?species)
			(coalesce(?zhardiness,'') as ?hardiness)
		from <${graph}>
		where {
			?plant a p:Plant .
			optional
			{
				?plant rdfs:label ?zlabel .
			}
			optional
			{
				?plant p:provenance ?zprovenance .
			}
			optional
			{
				?plant p:scientificName ?zspecies .
				optional
				{
					?zspecies p:hardiness ?zhardiness .
				}
			}
			optional
			{
				?plant p:endedOn ?endDate .
			}
			filter(!bound(?endDate))
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

	replaceProperty: function(subj, pred, type, oldValue, newValue, graph) {
		let oldSparqlValue = this.toSparqlValue(oldValue, type);
		let newSparqlValue = this.toSparqlValue(newValue, type);
		if(oldValue == '') {
			// insert only
			return `
			insert data {
				graph <${graph}> {
					<${subj}> <${pred}> ${newSparqlValue} .
				}
			}
			`;
		}
		else if(newValue == '') {
			// delete only
			return `
			delete data {
				graph <${graph}> {
					<${subj}> <${pred}> ${oldSparqlValue} .
				}
			}
			`;
		}
		else {
			// replace
			return `
			with <${graph}>
			delete {
				<${subj}> <${pred}> ${oldSparqlValue} .
			}
			insert {
				<${subj}> <${pred}> ${newSparqlValue} .
			}
			where {
			}
			`;
		}
	},

	replaceProperty2: function(subj, pred1, pred2, type, oldValue, newValue, graph) {
		let oldSparqlValue = this.toSparqlValue(oldValue, type);
		let newSparqlValue = this.toSparqlValue(newValue, type);
		if(oldValue == '') {
			return `
			with <${graph}>
			insert {
				?o <${pred2}> ${newSparqlValue} .
			}
			where {
				<${subj}> <${pred1}> ?o .
			}
			`;
		}
		else if(newValue == '') {
			return `
			with <${graph}>
			delete {
				?o <${pred2}> ${oldSparqlValue} .
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
				?o <${pred2}> ${oldSparqlValue} .
			}
			insert {
				?o <${pred2}> ${newSparqlValue} .
			}
			where {
				<${subj}> <${pred1}> ?o .
			}
			`;
		}
	},

	toSparqlValue: function(value, type) {
		if(type == "uri") {
			return "<"+value+">";
		}
		else if(type == "literal") {
			let isUri = value.startsWith("http://") || value.startsWith("urn:");
			if(isUri) {
				return "\""+value+"\"^^xsd:anyURI";
			}
			else {
				return "\"\"\""+value+"\"\"\"";
			}
		}
	}
};
