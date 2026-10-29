#!/usr/bin/env bash
# Builds the course submission (section 7.2 of the brief):
#   dist/FlightReservationSystem-submission.zip
#     ├── FlightReservationSystem.jar   runnable JAR (java -jar)
#     └── source.zip                    all .java sources, SQL, icons, tests and pom.xml
set -euo pipefail
cd "$(dirname "$0")/.."

mvn -q clean package
rm -rf dist && mkdir -p dist/stage
cp target/FlightReservationSystem.jar dist/stage/
zip -qr dist/stage/source.zip pom.xml README.md src scripts -x '*.DS_Store'
(cd dist/stage && zip -q ../FlightReservationSystem-submission.zip FlightReservationSystem.jar source.zip)
rm -rf dist/stage
echo "Created dist/FlightReservationSystem-submission.zip"
