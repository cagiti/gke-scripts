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

def reservedEmails = ["warren@warrenbailey.net", "wrefvem@cloudbees.com", "rawlingsj80@gmail.com", "james.strachan@gmail.com", "cosmin.cojocar@gmx.ch", "pmuir@cloudbees.com", "gareth@bryncynfelin.co.uk"]

def usersToUpdate = list.findAll{ it.role == 'cluster-admin' }.findAll{ !reservedEmails.contains(it.email) }

usersToUpdate.each{ user ->
	println "kubectl delete clusterrolebinding ${user.sa}".execute().text
	println "kubectl delete rolebinding ${user.sa}".execute().text
	def filePath = ""
	File.createTempFile("temp-${user.sa}",".yaml").with {
    	deleteOnExit()
	    write """---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: ${user.sa}
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: ClusterRole
  name: view
subjects:
- kind: ServiceAccount
  name: ${user.sa}
  namespace: jx
---
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: ${user.sa}
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: Role
  name: jx-view
subjects:
- kind: ServiceAccount
  name: ${user.sa}
  namespace: jx
"""
    	filePath = absolutePath
	}
	println "kubectl apply -f ${filePath}".execute().text
}
