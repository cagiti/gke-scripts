#!/usr/bin/env groovy

@Grapes([
	@Grab('org.yaml:snakeyaml:1.17')
])

import org.yaml.snakeyaml.Yaml

String.metaClass.yaml = { new Yaml().load(delegate) }

def list = []

def users = "kubectl get users -oyaml".execute().text.yaml()
users.items.each{ user -> 
	def binding = "kubectl get clusterrolebinding ${user.spec.serviceAccount} -oyaml".execute().text.yaml()
	list << [name: user.spec.name, email: user.spec.email, role: binding?.roleRef?.name, sa: user.spec.serviceAccount]
}

println "Viewers"
println "----------------------------------------"
list.findAll{ it.role == "view" }.each{ println it }

println "\n\nAdmin"
println "----------------------------------------"
list.findAll{ it.role == "cluster-admin" }.each{ println it }

println "\n\nNo Role"
println "----------------------------------------"
list.findAll{ !it.role }.each{ println it }
