def buckets = "gsutil ls".execute().text.split("\n").findAll{ it.startsWith('gs://jenkinsx-dev') && it.endsWith('-terraform-state/') }
buckets.each { bucket ->
	def clusterName = bucket.replaceAll('gs://jenkinsx-dev-','').replaceAll('-terraform-state/','')
	def json = "gcloud container clusters list --filter=${clusterName} --format=json".execute().text.trim()
	if (json == "[]") {
		println clusterName
		def files = "gsutil ls ${bucket}**".execute().text.split("\n")
		files.each { file ->
			if (file != "") { 
				println "gsutil rm ${file}".execute().text
			}
		}
		println "gsutil rb ${bucket}".execute().text
    }
}
