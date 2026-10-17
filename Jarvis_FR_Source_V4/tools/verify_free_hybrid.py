#!/usr/bin/env python3
"""Assertions de non facturation intégrée (n'atteste pas le statut du compte Gemini)."""
from pathlib import Path
root = Path(__file__).resolve().parents[1] / "android" / "app" / "src" / "main" / "java" / "com" / "aztech" / "jarvisfr"
hybrid = (root / "HybridAiClient.java").read_text()
ui = (root / "MainActivity.java").read_text()
manifest = (Path(__file__).resolve().parents[1] / "android" / "app" / "src" / "main" / "AndroidManifest.xml").read_text()
assert "gemini-3.1-flash-lite:generateContent" in hybrid
assert "gemini-3.1-flash-tts-preview:generateContent" in hybrid
assert "x-goog-api-key" in hybrid
assert "api.openai.com" not in hybrid
assert "billing" not in hybrid.lower()
assert "setFunctionValue" not in hybrid
assert "setFunctionValue" not in ui
assert "android.permission.INTERNET" in manifest
assert "usesCleartextTraffic=\"false\"" in manifest
assert "setChecked(false);" in ui and "cloudEnabled" in ui
assert "Quota gratuit atteint" in hybrid
assert "privateKeys.clear()" in ui
assert "Aucune commande ECARX" in ui
print("10 contrôles hybride gratuit/passif PASSES (compte Gemini et quotas à valider sur appareil)")
