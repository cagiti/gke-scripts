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

images.groupBy{ it.app }.sort{ it.key }.each{ k, v -> 
	println "${k}"
	v.sort().each {
		println "\t${it.image}"
	}
}
