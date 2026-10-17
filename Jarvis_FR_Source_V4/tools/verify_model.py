#!/usr/bin/env python3
"""Vérifie le modèle fusionné avec les empreintes fournies dans V4, sans réseau."""
import argparse
import hashlib
import json
from pathlib import Path, PurePosixPath
import sys


def verify(project):
    model = project / "android/app/src/main/assets/model-fr"
    manifest = project / "analysis/SHA256_MODELE_FR.txt"
    prefix = "Jarvis_FR_Source_V4/android/app/src/main/assets/model-fr/"
    checked, errors, seen = [], [], set()
    for line in manifest.read_text(encoding="utf-8").splitlines():
        if not line.strip():
            continue
        expected, name = line.split(maxsplit=1)
        name = name.lstrip("*")
        if not name.startswith(prefix) or ".." in PurePosixPath(name).parts:
            raise ValueError("Chemin inattendu dans le manifeste : " + name)
        relative = name[len(prefix):]
        if relative in seen:
            raise ValueError("Fichier dupliqué dans le manifeste : " + relative)
        seen.add(relative)
        path = model / relative
        if not path.is_file():
            errors.append("Fichier absent : " + relative)
            continue
        digest_state = hashlib.sha256()
        with path.open("rb") as stream:
            for chunk in iter(lambda: stream.read(65536), b""):
                digest_state.update(chunk)
        digest = digest_state.hexdigest()
        item = {"path": relative, "bytes": path.stat().st_size,
                "sha256": digest, "matches": digest == expected}
        checked.append(item)
        if digest != expected:
            errors.append("Empreinte différente : " + relative)
        if path.stat().st_size == 0 and relative != "ivector/online_cmvn.conf":
            errors.append("Fichier vide inattendu : " + relative)
    if len(seen) != 15:
        errors.append("Le manifeste V4 doit contenir 15 fichiers")
    actual = {p.relative_to(model).as_posix() for p in model.rglob("*") if p.is_file()}
    extra = sorted(actual - seen)
    if extra:
        errors.append("Fichiers non référencés : " + ", ".join(extra))
    config_path = model / "conf/mfcc.conf"
    config = config_path.read_text(encoding="utf-8") if config_path.is_file() else ""
    if "--sample-frequency=16000" not in config:
        errors.append("La configuration n'annonce pas 16 kHz")
    uuid_path = model / "uuid"
    uuid = uuid_path.read_text(encoding="utf-8").strip() if uuid_path.is_file() else ""
    if not uuid:
        errors.append("Identifiant uuid manquant pour StorageService.unpack")
    return {"ok": not errors, "model": str(model), "files_checked": len(checked),
            "total_bytes": sum(item["bytes"] for item in checked), "uuid": uuid,
            "intentional_empty_file": "ivector/online_cmvn.conf",
            "source_of_hashes": str(manifest), "files": checked, "errors": errors}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--project", type=Path, default=Path(__file__).resolve().parents[1])
    args = parser.parse_args()
    try:
        result = verify(args.project.resolve())
    except (OSError, ValueError) as error:
        result = {"ok": False, "errors": [str(error)]}
    print(json.dumps(result, ensure_ascii=False, indent=2))
    return 0 if result["ok"] else 1


if __name__ == "__main__":
    sys.exit(main())
