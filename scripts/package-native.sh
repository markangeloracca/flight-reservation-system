#!/usr/bin/env bash
# Optional: builds a native installer with a bundled Java runtime (jpackage, JDK 17+).
# Produces a .dmg on macOS, .msi on Windows (needs WiX), or .deb on Linux in dist/native.
set -euo pipefail
cd "$(dirname "$0")/.."

mvn -q clean package -DskipTests
rm -rf dist/native target/jpackage-input && mkdir -p target/jpackage-input
cp target/FlightReservationSystem.jar target/jpackage-input/
jpackage \
  --name "Flight Reservation System" \
  --app-version 1.0.0 \
  --input target/jpackage-input \
  --main-jar FlightReservationSystem.jar \
  --main-class ca.ucalgary.frs.FlightReservationApp \
  --java-options "-Dawt.useSystemAAFontSettings=on -Dswing.aatext=true" \
  --dest dist/native
echo "Installer written to dist/native"
