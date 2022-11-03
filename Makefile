build:
	./gradlew clean build

up: build
	docker-compose up -d

down:
	docker-compose down