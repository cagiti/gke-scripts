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
def teamName = "release"
def teamId = 0

orgs.each { org -> 
	def team = "curl https://${gitAuth.user}:${gitAuth.oauth_token}@api.github.com/orgs/${org}/teams/${teamName}".execute().text.json()
	if (!team) {
		println "Team ${teamName} does not exist for org ${org}"
		System.exit(1)
	}
	teamId = team.id	
	def repos = "curl https://${gitAuth.user}:${gitAuth.oauth_token}@api.github.com/orgs/${org}/repos?per_page=${per_page}&page=${page}".execute().text.json()
	repos.findAll{ repo -> !repo.archived }.each{ repo -> 
		println "${org}/${repo.name}"
 
		def teams = "curl https://${gitAuth.user}:${gitAuth.oauth_token}@api.github.com/repos/${org}/${repo.name}/teams".execute().text.json()
		def releaseTeam = teams.find{ it.name == 'release' && it.permission == 'push' }
		if (!releaseTeam) {
			println repo.name 
			def req = ["curl", "-d", "{\"permission\":\"push\"}", "-X", "PUT", "https://${gitAuth.user}:${gitAuth.oauth_token}@api.github.com/teams/${teamId}/repos/${org}/${repo.name}"]
			def resp = req.execute().text
			println resp
		}
	}
}
