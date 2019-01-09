#!/usr/bin/env groovy

@Grapes([
	@Grab('org.yaml:snakeyaml:1.17'),
	@Grab('commons-beanutils:commons-beanutils:1.9.3'),
	@Grab('org.codehaus.groovy.modules.http-builder:http-builder:0.7.1')
])

import groovy.json.*
import org.yaml.snakeyaml.Yaml
import groovyx.net.http.HTTPBuilder
import static groovyx.net.http.ContentType.URLENC
import groovyx.net.http.ContentType
import static groovyx.net.http.ContentType.*
import static groovyx.net.http.Method.*

String.metaClass.json = {
	new JsonSlurper().parseText(delegate)
}

String.metaClass.encode = { 
	java.net.URLEncoder.encode(delegate, "UTF-8")
}

Yaml parser = new Yaml()

def home = new File('/Users/garethjevans')
def git = parser.load(new File(home,".config/hub").text)
def jira = parser.load(new File(home, ".jx/jiraAuth.yaml").text)

def gitAuth = git.'github.com'.get(0)
def jiraAuth = jira.servers[0]


def auth = java.net.URLEncoder.encode("${jiraAuth.users[0].username}:${jiraAuth.users[0].apitoken}", "UTF-8")
def jqlQuery = 'project in ("Cloud Native Jenkins", "Core Platform") and "Github Issue" is not EMPTY'
def http = new HTTPBuilder( "https://${auth}@cloudbees.atlassian.net/rest/api/3/search" )
http.handler.failure = { resp ->
    println "Unexpected failure: ${resp.statusLine}"
    println "${resp.data}"
}

def issues = []

http.request(POST, JSON) {
    uri.path = '/rest/api/3/search'
    body = [jql:jqlQuery]
    requestContentType = ContentType.JSON

    response.success = { resp, json ->
        println "Found ${json.total} issue(s)"
		json.issues.each { issues << [id:it.key, gh:it.fields.customfield_12684, status:it.fields.status.name] }
    }
}

def int complete, requireClosing, requireTriage

issues.each { 
	def issueUrl = it.gh.replaceAll('https://github.com/','')
	if (!issueUrl.contains('pull')) { 
		def ghIssue = "https://${gitAuth.user}:${gitAuth.oauth_token}@api.github.com/repos/${issueUrl}".toURL().text.json()
		def issueKind = ghIssue.labels.find{ label -> label.name.startsWith('kind')}?.name
		def issuePriority = ghIssue.labels.find{ label -> label.name.startsWith('priority')}?.name
		
		if (ghIssue.state == 'closed' && it.status == 'Done') {
			complete++
		} else if (ghIssue.state == 'closed') {
			println "--------------------------"
			println "ISSUE NEEDS UPDATING - https://cloudbees.atlassian.net/browse/${it.id}"
			println "${it.gh} is marked as ${ghIssue.state}"
			requireClosing++
		} else {
			println "--------------------------"
			println it
			println ghIssue.title
			println ghIssue.state
			if (issueKind) {
				println issueKind
			}
 			if (issuePriority) {
				println issuePriority
			} else {
				println "ISSUE NEEDS TRIAGE"
				requireTriage++
			}
		}
	} else {
		println "--------------------------"
		def pullUrl = it.gh.replaceAll('https://github.com/','')
		def ghPull = "https://${gitAuth.user}:${gitAuth.oauth_token}@api.github.com/repos/${pullUrl}".replace('/pull/','/pulls/').toURL().text.json()
		println it
		println ghPull.title
		println ghPull.state
	}
}

println "${complete} issue(s) complete"
println "${requireClosing} issue(s) requires closing"
println "${requireTriage} issue(s) requires triage"
