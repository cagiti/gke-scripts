@Grapes([
	@Grab('org.yaml:snakeyaml:1.17')
])

import groovy.json.*
import org.yaml.snakeyaml.Yaml

String.metaClass.yaml = {
	new Yaml().load(delegate)
}

println "jenkins-x-versions: latest"
def jxPlatformUrl = 'https://raw.githubusercontent.com/jenkins-x/jenkins-x-versions/master/charts/jenkins-x/jenkins-x-platform.yml'
def jxPlatformVersion = "curl ${jxPlatformUrl}".execute().text.yaml().version
println "jenkins-x-platform: v${jxPlatformVersion}"

def jxPlatformValuesUrl = "https://raw.githubusercontent.com/jenkins-x/jenkins-x-platform/v${jxPlatformVersion}/jenkins-x-platform/values.yaml"
def mavenImageVersion = "curl ${jxPlatformValuesUrl}".execute().text.yaml().jenkins.Agent.PodTemplates.Maven.Containers.Maven.Image
println "jenkins-x-builders: ${mavenImageVersion}"

println "-----"

def skaffoldVersion = "docker run ${mavenImageVersion} skaffold version".execute().text.trim()
println "skaffold: ${skaffoldVersion}"
def helmVersion = "docker run ${mavenImageVersion} helm version --short".execute().text.trim()
println "helm: ${helmVersion}"
def jxVersion = "docker run ${mavenImageVersion} jx --version".execute().text.trim()
println "jx: ${jxVersion}"
