#!/usr/bin/env groovy

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

def NORMAL = "\u001B[0m"
def BOLD = "\u001B[1m"
def ITALIC = "\u001B[3m"
def UNDERLINE = "\u001B[4m"
def BLINK = "\u001B[5m"
def RAPID_BLINK = "\u001B[6m"
def REVERSE_VIDEO = "\u001B[7m"
def INVISIBLE_TEXT = "\u001B[8m"

def BLACK = "\u001B[30m"
def RED = "\u001B[31m"
def GREEN = "\u001B[32m"
def YELLOW = "\u001B[33m"
def BLUE = "\u001B[34m"
def MAGENTA = "\u001B[35m"
def CYAN = "\u001B[36m"
def WHITE = "\u001B[37m"

def DARK_GRAY = "\u001B[1;30m"
def LIGHT_RED = "\u001B[1;31m"
def LIGHT_GREEN = "\u001B[1;32m"
def LIGHT_YELLOW = "\u001B[1;33m"
def LIGHT_BLUE = "\u001B[1;34m"
def LIGHT_PURPLE = "\u001B[1;35m"
def LIGHT_CYAN = "\u001B[1;36m"

String.metaClass.color = { ansiValue ->
    ansiValue + delegate + NORMAL
}

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


def home = new File(System.getenv('HOME'))
def config = new File(home,".config/hub")
if (!config.exists()) {
	println "${config.color(RED)} doesn't exist"
	println "Please ensure you have the hub cli installed and configured"
	System.exit(1)
}

def git = parser.load(config.text)
def gitAuth = git.'github.com'.get(0)

def prs = []
def morePRs = true
def page = 1
def per_page = 100
def printRepo = true
def ignoredLabels = ["do-not-merge/hold", "do-not-merge/work-in-progress"]

def repos = ["cloudbees/arcalos",
    "cloudbees/arcalos-config",
    "cloudbees/arcalos-setup",
    "cloudbees/arcalos-instance-template",
    "cloudbees/arcalos-boot-config",
    "cloudbees/arcalos-jenkins-x-versions",
    "cloudbees/jx-tenant-service",
    "cloudbees/lighthouse-githubapp",
    "cloudbees/jx-segment-controller",
    "cloudbees/jx-repository-controller",
    "cloudbees/jx-arcalos-role-controller",
    "arcalos-environments/environment-raccoonshimmer-dev",
    "arcalos-environments/environment-hornberyl-dev",
    "arcalos-management/environment-arcalos-prod-mgmt-dev"]

repos.each{ repo ->
    morePRs = true
    printRepo = true
    page = 1
    
    while (morePRs) {
        def ghIssues = "curl https://${gitAuth.user}:${gitAuth.oauth_token}@api.github.com/repos/${repo}/pulls?state=open&per_page=${per_page}&page=${page}".execute().text.json()

        ghIssues.each{ 

            def statuses_url = it.statuses_url.replaceAll('https://', '').replaceAll('statuses','commits') + "/status"
            def title = it.title
            def user = it.user.login
            def labels = it.labels.collect{ it.name }
            def isFiltered = labels.any { ignoredLabels.contains( it ) }
            
            if (!isFiltered) {            
                def statuses = "curl https://${gitAuth.user}:${gitAuth.oauth_token}@${statuses_url}".execute().text.json()
                if (printRepo) {
                    println "https://github.com/${repo}/pulls".color(BLUE)
                    printRepo = false
                } 

                def pr = "#${it.number}".color(GREEN)
                def state = statuses.state
                if (state == "pending") {
                    def nonTideStatus = statuses.statuses.findAll{ it.context != "tide" }.collect{ it.state }.unique()
                    if (!nonTideStatus.contains('pending')) {
                        state = "success"    
                    }
                }

                def colouredTitle = title
                if (state == "failure") {
                    colouredTitle = title.color(RED)
                } else if (state == "pending") {
                    colouredTitle = title.color(YELLOW)
                } else if (state == "success") {
                    colouredTitle = title.color(GREEN)
                }
                println "${pr}\t${user}\t${colouredTitle}\t${labels.join(", ")}"
            }
        }

        page++

        if (ghIssues.size() < per_page) {
            morePRs = false
        }
    }
}
