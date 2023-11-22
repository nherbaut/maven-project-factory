build:
	mvn clean package -Dquarkus.container-image.build=true
push:
	docker push nherbaut/maven-project-factory:1.0.0-SNAPSHOT
run:
	docker run -p 8080:8080 nherbaut/maven-project-factory:1.0.0-SNAPSHOT
