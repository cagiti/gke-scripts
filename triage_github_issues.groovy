@Grapes([
	@Grab('org.yaml:snakeyaml:1.17'),
	@Grab('commons-beanutils:commons-beanutils:1.9.3'),
	@Grab('org.codehaus.groovy.modules.http-builder:http-builder:0.7.1')
])

import groovy.time.*
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

String.metaClass.toDate {
	Date.parse("yyyy-MM-dd'T'HH:mm", delegate)
}

Yaml parser = new Yaml()

def home = new File('/Users/garethjevans')
def git = parser.load(new File(home,".config/hub").text)
def gitAuth = git.'github.com'.get(0)

def issues = []
def moreIssues = true
def page = 1
def per_page = 100

while (moreIssues) {
	println "Getting page ${page}"
	def ghIssues = "curl https://${gitAuth.user}:${gitAuth.oauth_token}@api.github.com/repos/jenkins-x/jx/issues?state=all&per_page=${per_page}&page=${page}".execute().text.json()
	ghIssues.findAll{ !it.pull_request }.each{ 
		def issueKind = it.labels.find{ label -> label.name.startsWith('kind')}?.name
		def issuePriority = it.labels.find{ label -> label.name.startsWith('priority')}?.name
		def issueArea = it.labels.find{ label -> label.name.startsWith('area')}?.name
		def issueLifecycle = it.labels.find{ label -> label.name.startsWith('lifecycle')}?.name
		issues << [id:it.html_url, title: it.title, state:it.state, kind:issueKind, priority:issuePriority, area:issueArea, lifecycle:issueLifecycle, created_at: it.created_at?.toDate(), closed_at: it.closed_at?.toDate()]
	}

	page++

	if (ghIssues.size() < per_page) {
		moreIssues = false
	}
}

def numberOfIssues = issues.size()
def numberOfOpenIssues = issues.findAll{ it.state == "open" }.size()
def numberOfOpenIssuesExcludingEnhancements = issues.findAll{ it.state == "open" }.findAll{ it.kind != "kind/enhancement" }.size()
def toTriage = issues.findAll{ it.state == "open" }.findAll{ !it.priority }.size()

def timeToResolve = issues.findAll{ it.state == 'closed' }.findAll{ it.kind != "kind/enhancement" }.collect{ 
	TimeCategory.minus(it.closed_at, it.created_at).days
}

println ""
println "Average time to close ${timeToResolve.sum()/timeToResolve.size()} day(s)"

def filter = issues.collect{ it.priority }.findAll{ it }.unique().collect{ "-label%3A${it.replace('/','%2F')}" }.join('+')

println ""
println "Total issue(s) ${numberOfIssues}"
println "Total open issue(s) ${numberOfOpenIssues}"
println "To triage ${toTriage} - https://github.com/jenkins-x/jx/issues?q=is%3Aissue+is%3Aopen+${filter}"
println "Triage ${((numberOfOpenIssues-toTriage)/numberOfOpenIssues)*100}%"
println "% of open issues ${((numberOfOpenIssuesExcludingEnhancements)/numberOfIssues)*100}%"
println ""

issues.findAll{ it.state == "open" }.
       findAll{ it.priority }.
       groupBy{ it.priority }.
       each{ k,v -> println "${k} - ${v.size()} - https://github.com/jenkins-x/jx/issues?q=is%3Aissue+is%3Aopen+label%3A${k.replace('/','%2F')}" }

println ""
issues.findAll{ it.state == "open" }.
       findAll{ it.kind }.
       groupBy{ it.kind }.
       each{ k,v -> println "${k} - ${v.size()} - https://github.com/jenkins-x/jx/issues?q=is%3Aissue+is%3Aopen+label%3A${k.replace('/','%2F')}" }

println ""
issues.findAll{ it.state == "open" }.
       groupBy{ it.lifecycle }.
       each{ k,v -> println "${k} - ${v.size()} - https://github.com/jenkins-x/jx/issues?q=is%3Aissue+is%3Aopen+label%3A${k?:'Open'.replace('/','%2F')}" }
