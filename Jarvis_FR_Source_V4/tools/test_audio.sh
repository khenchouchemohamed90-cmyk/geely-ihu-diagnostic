#!/bin/sh
set -eu
BASE="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
javac -encoding UTF-8 -d "$WORK" \
  "$BASE"/android/app/src/main/java/com/aztech/jarvisfr/Pcm16Resampler.java \
  "$BASE"/android/app/src/main/java/com/aztech/jarvisfr/PcmAudioMetrics.java \
  "$BASE"/tools/TestPcmAudio.java
java -cp "$WORK" TestPcmAudio
# Scénarios d'erreur et cycle de vie simulés : ce ne sont pas des tests matériels.
javac -encoding UTF-8 -cp "$WORK" -d "$WORK" \
  "$BASE"/tools/fake_audio/android/annotation/*.java \
  "$BASE"/tools/fake_audio/android/media/*.java \
  "$BASE"/tools/fake_audio/android/os/*.java \
  "$BASE"/tools/fake_audio/android/util/*.java \
  "$BASE"/tools/fake_audio/org/vosk/*.java \
  "$BASE"/tools/fake_audio/org/vosk/android/*.java \
  "$BASE"/android/app/src/main/java/com/aztech/jarvisfr/DiagnosticSpeechService.java \
  "$BASE"/tools/TestDiagnosticSpeechService.java
java -cp "$WORK" TestDiagnosticSpeechService
