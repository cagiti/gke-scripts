@Grapes([
	@Grab('org.yaml:snakeyaml:1.17')
])

import groovy.json.*
import org.yaml.snakeyaml.Yaml

String.metaClass.json = {
	new JsonSlurper().parseText(delegate)
}

Yaml parser = new Yaml()

def home = new File('/Users/garethjevans')
def git = parser.load(new File(home,".config/hub").text)
def gitAuth = git.'github.com'.get(0)

def page = 1
def per_page = 100
def orgs = ['jenkins-x','jenkins-x-charts','jenkins-x-apps','jenkins-x-buildpacks','jenkins-x-images','jenkins-x-quickstarts']

def data = []
def showUrl = false

orgs.each { org -> 
	def repos = "curl https://${gitAuth.user}:${gitAuth.oauth_token}@api.github.com/orgs/${org}/repos?per_page=${per_page}&page=${page}".execute().text.json()
	repos.findAll{ repo -> !repo.archived }.each{ repo -> 
		if (repo.open_issues) {
			//println repo.name
			def hasMore = true
			def issuePage = 1
			def totalNumberOfIssues = 0

			while (hasMore) {
				def url = "curl https://${gitAuth.user}:${gitAuth.oauth_token}@api.github.com/repos/${org}/${repo.name}/issues?per_page=${per_page}&page=${issuePage}"
				def issues = url.execute().text.json()
				def numberOfIssuesAndPrs = issues.size()
				def numberOfIssues = issues.findAll{ !it.pull_request }.size()
				if (numberOfIssuesAndPrs < 100) {
					hasMore = false
				}
				issuePage++
				totalNumberOfIssues += numberOfIssues
			}
			//println totalNumberOfIssues
			data << [org:org, repo: repo.name, issues: totalNumberOfIssues]
		}
	}
}

data.groupBy{ it.org }
	.each { k,v -> 
		println "${k} - ${v.sum{ it.issues } }"
        v.findAll{ it.issues }.each {
			println "\t${it.repo} - ${it.issues}"
			if (showUrl) {
				println "\thttps://github.com/${it.org}/${it.repo}/issues"
			}
		} 
	}

