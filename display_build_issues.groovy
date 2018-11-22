def hideCompleted = args.contains("--hide-completed")
def hideErrored = args.contains("--hide-errored")

def pods = []
def numberOfBuilds = 14

def podDescriptions = "kubectl get pods --sort-by=.status.startTime".execute().text.split('\n').reverse().toList().findAll{ it ==~ /^\w{8}-\w{4}-\w{4}-\w{4}-\w{12}-\w{5}.*/ }

if (hideCompleted) {
	podDescriptions = podDescriptions.findAll{ !it.contains("Completed") }
}

if (hideErrored) {
	podDescriptions = podDescriptions.findAll{ !it.contains("Init:Error") }
}

if (podDescriptions.size() > numberOfBuilds) {
	podDescriptions = podDescriptions.subList(0, numberOfBuilds)
}

podDescriptions.each{ line ->
	def tokens = line.split()
	pods << [name:tokens[0], status:tokens[2], time:tokens[4]]
}

pods.each { pod ->
	println "${pod}"
	def description = "kubectl describe pod ${pod.name}".execute().text
	def repo = (description.split('\n').find{ it.contains('REPO_NAME') } ?: "").replace("REPO_NAME:","").trim()
	def branch = (description.split('\n').find{ it.contains('BRANCH_NAME') } ?: "").replace("BRANCH_NAME:","").trim()
	def buildNumber = (description.split('\n').find{ it.contains('JX_BUILD_NUMBER') } ?: "").replace("JX_BUILD_NUMBER:","").trim()

    if (repo == "" && branch == "" ) {
		println "\tupgrade"
    } else {
		println "\t${repo ?: 'unknown'}/${branch ?: 'unknown'}${branch == 'master' ? '' : '/' + buildNumber}"
    }
	def build_steps = description.split('\n').findAll{ it.contains('build-step') }.reverse().toList()
	if (build_steps.size() > 0) {
		def build_step = description.split('\n').findAll{ it.contains('build-step') }.reverse().toList().subList(0,1).get(0).replace(":","").trim()
		println "\tkubectl logs -f ${pod.name} -c ${build_step}"
	}
}

