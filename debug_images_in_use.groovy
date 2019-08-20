#!/usr/bin/env groovy

@Grapes([
	@Grab('org.yaml:snakeyaml:1.17')
])

import org.yaml.snakeyaml.Yaml

String.metaClass.yaml = { new Yaml().load(delegate) }

def pods = "kubectl get pods -oyaml".execute().text.yaml()
images = pods.items.findAll{ pod -> 
    //!pod.metadata.labels.'created-by-prow'
    true
}.collect{ pod ->
    pod.spec.containers.collect{ container -> 
	[ app: pod.metadata.labels.app, image: container.image ]}
}.flatten().unique()

//jxImages = images.
//	findAll{ image -> image.image.contains('builder') || image.image.contains('jx') }.
//	findAll{ image -> !image.image.contains('abayer') }.
//	findAll{ image -> !image.image.contains('jxui') }

images.groupBy{ it.app }.each{ k, v -> 
	println "${k}"
	//if (it.image.contains('builder')) {
	//	def version = "docker run ${it.image} jx --version".execute().text.trim()
	//	println "\t${it.image} -> ${version}"
	//} else {
	v.each {
		println "\t${it.image}"
	}
	//} 
}
