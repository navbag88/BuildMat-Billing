#!/bin/bash
# Run this once to generate Gradle wrapper files
# After this, use ./gradlew on Linux/Mac or gradlew.bat on Windows

gradle wrapper --gradle-version 8.5
echo "Gradle wrapper generated. Use 'gradlew.bat fatJar' on Windows."
