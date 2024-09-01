build:
	mvn clean package -Dquarkus.container-image.build=true
push:
	quarkus build -Dquarkus.container-image.build=true -Dquarkus.container-image.push=true
run:
	docker run -p 8080:8080 nherbaut/maven-project-factory:1.0.0-SNAPSHOT
