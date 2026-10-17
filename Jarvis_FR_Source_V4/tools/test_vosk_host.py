#!/usr/bin/env python3
"""Charge Vosk sur l'hôte et décode des WAV PCM16 mono 16 kHz.

Installer vosk==0.3.45 dans un venv. Ce test utilise libvosk Linux et ne valide
ni JNI Android, ni le microphone IHU007B, ni une action physique du véhicule.
Sans --case, il charge le modèle et vérifie uniquement le rejet du silence.
Le décodage est libre par défaut, ou guidé par une liste JSON avec --grammar.
Exemple : python tools/test_vosk_host.py --case /tmp/navigation.wav='ouvre la navigation'
"""
import argparse
import hashlib
from importlib.metadata import version
import json
from pathlib import Path
import platform
import sys
import unicodedata
import wave


def canonical(text):
    return " ".join(unicodedata.normalize("NFC", text).casefold().split())


def decode(vosk, model, pcm, grammar=None):
    recognizer = (vosk.KaldiRecognizer(model, 16000, json.dumps(grammar, ensure_ascii=False))
                  if grammar is not None else vosk.KaldiRecognizer(model, 16000))
    texts = []
    for offset in range(0, len(pcm), 8000):
        if recognizer.AcceptWaveform(pcm[offset:offset + 8000]):
            texts.append(json.loads(recognizer.Result()).get("text", ""))
    texts.append(json.loads(recognizer.FinalResult()).get("text", ""))
    return " ".join(text for text in texts if text)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--model", type=Path, default=Path(__file__).resolve().parents[1]
                        / "android/app/src/main/assets/model-fr")
    parser.add_argument("--case", action="append", default=[], metavar="WAV=ATTENDU")
    parser.add_argument("--grammar", type=Path, help="Liste JSON de phrases, incluant [unk]")
    parser.add_argument("--report", type=Path)
    args = parser.parse_args()
    grammar = json.loads(args.grammar.read_text(encoding="utf-8")) if args.grammar else None
    if grammar is not None and (not isinstance(grammar, list)
                                or not all(isinstance(phrase, str) for phrase in grammar)
                                or "[unk]" not in grammar):
        raise ValueError("La grammaire doit être une liste de chaînes incluant [unk]")
    import vosk
    native = Path(vosk.__file__).parent / "libvosk.so"
    result = {"vosk_python_version": version("vosk"), "host": platform.platform(),
              "native_library": str(native), "native_sha256": hashlib.sha256(native.read_bytes()).hexdigest(),
              "model": str(args.model.resolve()), "sample_rate": 16000,
              "grammar": grammar if grammar is not None else "unrestricted", "cases": [],
              "limitation": "Bibliothèque Linux : ne valide pas Android/JNI/microphone/ECARX"}
    model = vosk.Model(str(args.model))
    result["model_loaded"] = True
    text = decode(vosk, model, bytes(32000 * 3), grammar)
    result["cases"].append({"name": "silence_3s", "expected": "", "recognized": text,
                             "passed": text == ""})
    for spec in args.case:
        filename, expected = spec.split("=", 1)
        path = Path(filename)
        with wave.open(str(path), "rb") as source:
            if (source.getnchannels(), source.getsampwidth(), source.getframerate(),
                    source.getcomptype()) != (1, 2, 16000, "NONE"):
                raise ValueError("WAV PCM16 mono 16 kHz nécessaire : " + filename)
            pcm = source.readframes(source.getnframes())
        text = decode(vosk, model, pcm, grammar)
        result["cases"].append({"name": filename, "audio_sha256": hashlib.sha256(path.read_bytes()).hexdigest(),
                                 "duration_seconds": len(pcm) / 32000,
                                 "expected": expected, "recognized": text,
                                 "passed": canonical(text) == canonical(expected)})
    result["ok"] = all(case["passed"] for case in result["cases"])
    serialized = json.dumps(result, ensure_ascii=False, indent=2) + "\n"
    print(serialized, end="")
    if args.report:
        args.report.write_text(serialized, encoding="utf-8")
    return 0 if result["ok"] else 1


if __name__ == "__main__":
    sys.exit(main())
