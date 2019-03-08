#!/usr/bin/env groovy
import groovy.json.*

String.metaClass.json = {
	new JsonSlurper().parseText(delegate)
}
def project = "gcloud config get-value project".execute().text
println "Using gcloud project: ${project}"
def addresses = "gcloud compute addresses list --filter=\"RESERVED\" --format=json".execute().text.json()
addresses.each { address ->
	def scope
	if ( address.region ) {
		def region = address.region.substring(address.region.lastIndexOf("/") + 1, address.region.length())
		scope = "--region ${region}"
	} else {
		scope = "--global"
	}
	def cmd = "gcloud compute addresses delete ${address.name} ${scope}"
	println "${cmd}"
}
