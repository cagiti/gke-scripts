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
def gitAuth = git.'github.com'.get(0)

def issues = []
def moreIssues = true
def page = 1
while (moreIssues) {
	def ghIssues = "https://${gitAuth.user}:${gitAuth.oauth_token}@api.github.com/repos/jenkins-x/jx/issues?per_page=100&page=${page}".toURL().text.json()
	ghIssues.findAll{ !it.pull_request }.each{ 
		def issueKind = it.labels.find{ label -> label.name.startsWith('kind')}?.name
		def issuePriority = it.labels.find{ label -> label.name.startsWith('priority')}?.name
		def issueArea = it.labels.find{ label -> label.name.startsWith('area')}?.name
		issues << [id:it.html_url, title: it.title, state:it.state, kind:issueKind, priority:issuePriority, area:issueArea]
	}

	page++

	if (ghIssues.size() < 100) {
		moreIssues = false
	}
}

issues.findAll{ !it.priority }.each{ println it }

def numberOfIssues = issues.size()
def toTriage = issues.findAll{ !it.priority }.size()

println "Total issue(s) ${numberOfIssues}"
println "To triage ${toTriage}"
println "Triage ${((numberOfIssues-toTriage)/numberOfIssues)*100}%"
