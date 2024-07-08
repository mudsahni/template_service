.PHONY: build docker-build docker-run clean

# Variables
APP_NAME = spring_boot_template_service
VERSION = latest
GRADLE = ./gradlew
DOCKER = docker

build:
	$(GRADLE) build

docker-build: build
	$(DOCKER) build -t $(APP_NAME):$(VERSION) .

docker-run:
	$(DOCKER) run -p 8080:8080 $(APP_NAME):$(VERSION)

clean:
	$(GRADLE) clean
	$(DOCKER) rmi $(APP_NAME):$(VERSION) || true