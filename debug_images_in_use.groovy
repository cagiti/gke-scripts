#!/usr/bin/env groovy

@Grapes([
	@Grab('org.yaml:snakeyaml:1.17')
])

import org.yaml.snakeyaml.Yaml

String.metaClass.yaml = { new Yaml().load(delegate) }

def pods = "kubectl get pods -oyaml".execute().text.yaml()
images = pods.items.collect{ pod -> 
    pod.spec.containers.collect{ container -> container.image }
}.flatten().unique()

jxImages = images.
	findAll{ image -> image.contains('builder') || image.contains('jx') }.
	findAll{ image -> !image.contains('abayer') }.
	findAll{ image -> !image.contains('jxui') }

jxImages.each{ 
	if (it.contains('builder')) {
		def version = "docker run ${it} jx --version".execute().text.trim()
		println "${it} -> ${version}"
	} else {
		println "${it}"
	} 
}
