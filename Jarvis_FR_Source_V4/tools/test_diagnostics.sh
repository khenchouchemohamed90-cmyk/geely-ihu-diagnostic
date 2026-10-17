#!/bin/sh
set -eu
BASE="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
JARVIS_TEST_DIR="$(mktemp -d)"
trap 'rm -rf "$JARVIS_TEST_DIR"' EXIT
javac -encoding UTF-8 -d "$JARVIS_TEST_DIR" \
  "$BASE"/android/app/src/main/java/com/aztech/jarvisfr/FrenchIntentParser.java \
  "$BASE"/android/app/src/main/java/com/aztech/jarvisfr/VoiceSessionGate.java \
  "$BASE"/android/app/src/main/java/com/aztech/jarvisfr/VoiceTranscriptPolicy.java \
  "$BASE"/android/app/src/main/java/com/aztech/jarvisfr/DiagnosticCommands.java \
  "$BASE"/android/app/src/main/java/com/aztech/jarvisfr/BoundedDiagnosticLog.java \
  "$BASE"/tools/TestVoiceTranscriptPolicy.java "$BASE"/tools/TestDiagnosticLog.java
java -cp "$JARVIS_TEST_DIR" TestVoiceTranscriptPolicy
java -cp "$JARVIS_TEST_DIR" TestDiagnosticLog
