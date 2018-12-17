#!/usr/bin/env groovy

@Grapes([
	@Grab('org.yaml:snakeyaml:1.17')
])

import org.yaml.snakeyaml.Yaml

def reservedEmails = ["wrefvem@cloudbees.com", "rawlingsj80@gmail.com", "james.strachan@gmail.com", "cosmin.cojocar@gmx.ch", "pmuir@cloudbees.com", "gareth@bryncynfelin.co.uk"]

def yamlRaw = "kubectl get users -oyaml".execute().text
Yaml parser = new Yaml()

def users = parser.load(yamlRaw).items.collect { [key: it.spec.serviceAccount, email: it.spec.email ] }

def usersToUpdate = users.findAll{ it.key }.findAll{ !reservedEmails.contains(it.email) }

usersToUpdate.each{ user ->
	println "kubectl delete clusterrolebinding ${user.key}".execute().text
	println "kubectl delete rolebinding ${user.key}".execute().text
	def filePath = ""
	File.createTempFile("temp-${user.key}",".yaml").with {
    	deleteOnExit()
	    write """---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: ${user.key}
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: ClusterRole
  name: view
subjects:
- kind: ServiceAccount
  name: ${user.key}
  namespace: jx
---
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: ${user.key}
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: Role
  name: jx-view
subjects:
- kind: ServiceAccount
  name: ${user.key}
  namespace: jx
"""
    	filePath = absolutePath
	}
	println "kubectl apply -f ${filePath}".execute().text
}
