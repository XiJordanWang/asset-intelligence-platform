#!/usr/bin/env sh
# Runs the whole platform without Docker. Needs only Java 21.
# Builds the UI (Maven downloads Node itself), starts Spring Boot with an embedded database,
# and seeds the fictional company on first run. Then open http://localhost:8080
set -e
cd "$(dirname "$0")/backend"
./mvnw -Pui -DskipTests spring-boot:run -Dspring-boot.run.profiles=local
