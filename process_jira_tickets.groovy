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
        println "Found ${json.total} issue(s) in Jira"
		json.issues.each { issues << [id:it.key, gh:it.fields.customfield_12684, status:it.fields.status.name] }
    }
}

def int complete, requireClosing, requireTriage

def enhancedIssues = []

issues.each {
	def updatedIssue = [:] 
	def issueUrl = it.gh.replaceAll('https://github.com/','')
	
	updatedIssue['id'] = it.id
	updatedIssue['github_url'] = it.gh

	if (!issueUrl.contains('pull')) { 
		def ghIssue = "https://${gitAuth.user}:${gitAuth.oauth_token}@api.github.com/repos/${issueUrl}".toURL().text.json()
		def issueKind = ghIssue.labels.find{ label -> label.name.startsWith('kind')}?.name
		def issuePriority = ghIssue.labels.find{ label -> label.name.startsWith('priority')}?.name
		updatedIssue['kind'] = issueKind
		updatedIssue['priority'] = issuePriority
		updatedIssue['title'] = ghIssue.title
		updatedIssue['github_state'] = ghIssue.state
		
		if (ghIssue.state == 'closed' && it.status == 'Done') {
			updatedIssue['action'] = 'complete'
		} else if (ghIssue.state == 'closed') {
			updatedIssue['action'] = 'requires_closing'
		} else {
 			if (issuePriority) {
				updatedIssue['action'] = 'open'
			} else {
				updatedIssue['action'] = 'requires_triage'
			}
		}
	} else {
		//println "--------------------------"
		def pullUrl = it.gh.replaceAll('https://github.com/','')
		def ghPull = "https://${gitAuth.user}:${gitAuth.oauth_token}@api.github.com/repos/${pullUrl}".replace('/pull/','/pulls/').toURL().text.json()
		//println it
		//println ghPull.title
		//println ghPull.state
	}
	enhancedIssues << updatedIssue
}

println "---------- COMPLETE ----------"
enhancedIssues.findAll{ it.action == 'complete' }.each{ println it }

println ""
println "---------- REQUIRES CLOSING ----------"
enhancedIssues.findAll{ it.action == 'requires_closing' }.each{ println it }

println ""
println "---------- REQUIRES TRIAGE ----------"
enhancedIssues.findAll{ it.action == 'requires_triage' }.each{ println it }

println ""
println "---------- OPEN ----------"
enhancedIssues.findAll{ it.action == 'open' }.each{ println it }

