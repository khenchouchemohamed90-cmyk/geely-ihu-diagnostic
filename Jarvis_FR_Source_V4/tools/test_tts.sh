#!/bin/sh
set -eu
BASE="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
TASK_WORK="$(mktemp -d)"
trap 'rm -rf "$TASK_WORK"' EXIT
javac -encoding UTF-8 -source 8 -target 8 -d "$TASK_WORK" \
  "$BASE"/tools/fake_tts/android/content/Context.java \
  "$BASE"/tools/fake_tts/android/os/Looper.java \
  "$BASE"/tools/fake_tts/android/os/Handler.java \
  "$BASE"/tools/fake_tts/android/media/AudioAttributes.java \
  "$BASE"/tools/fake_tts/android/speech/tts/Voice.java \
  "$BASE"/tools/fake_tts/android/speech/tts/UtteranceProgressListener.java \
  "$BASE"/tools/fake_tts/android/speech/tts/TextToSpeech.java \
  "$BASE"/android/app/src/main/java/com/aztech/jarvisfr/FrenchVoiceSelector.java \
  "$BASE"/android/app/src/main/java/com/aztech/jarvisfr/FrenchTtsController.java \
  "$BASE"/tools/TestFrenchTts.java
java -cp "$TASK_WORK" TestFrenchTts
