.PHONY: build test

# Clean, compile, test and package everything.
build:
	./gradlew clean build

# Clean and run the tests only.
test:
	./gradlew clean test
