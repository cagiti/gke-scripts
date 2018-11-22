#!/usr/bin/env groovy

String.metaClass.json = { new groovy.json.JsonSlurper().parseText(delegate) }

def serviceAccounts = "gcloud iam service-accounts list --format=json".execute().text.json().
	collect{ it.email }.
	findAll{ it.startsWith("jx") }.
	findAll{ !it.contains('vault') }.
	findAll{ !it.contains('jxce') }.
	findAll{ !it.contains('212') }

println serviceAccounts.size()

serviceAccounts.each { sa ->
	def clusterName = sa.replaceAll('@jenkinsx-dev.iam.gserviceaccount.com','').replaceAll('^jx-','')
	//println clusterName
	def json = "gcloud container clusters list --filter=${clusterName} --format=json".execute().text.trim()
	if (json == "[]") {
		println sa
		println "gcloud iam service-accounts delete ${sa} --quiet".execute().text
    }
}
