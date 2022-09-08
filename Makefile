build:
	mvn clean package -Dquarkus.container-image.build=true
push:
	docker push nherbaut/maven-project-factory:1.0.0-SNAPSHOT
