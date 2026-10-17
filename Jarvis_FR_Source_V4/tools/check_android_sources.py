#!/usr/bin/env python3
"""Compile contre le véritable SDK Android et Vosk (remplace les anciennes interfaces simulées).

JDK 17 et SDK 34 requis. Cette compilation ne valide ni microphone ni véhicule.
Les simulations métier restent dans tools/test_parser.sh.
"""
from pathlib import Path
import os
import subprocess
import sys

base = Path(__file__).resolve().parent.parent
gradle = os.environ.get("JARVIS_GRADLE", str(base / "android/gradlew"))
result = subprocess.run([gradle, "--no-daemon", ":app:compileDebugJavaWithJavac"], cwd=base / "android")
sys.exit(result.returncode)
