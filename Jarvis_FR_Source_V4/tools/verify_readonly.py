#!/usr/bin/env python3
"""Garde-fou statique : interdit tout appel direct aux opérations d'écriture ECARX.

Ne garantit pas que l'application a tous les droits, mais permet d'éviter
l'introduction accidentelle d'une commande physique dans le prototype.
"""
from pathlib import Path
import re

root = Path(__file__).resolve().parent.parent / 'android/app/src/main/java'
for file in root.rglob('*.java'):
    data = file.read_text(encoding='utf-8')
    for pattern in (r'\.setFunctionValue\s*\(', r'\.setCustomizeFunctionValue\s*\('):
        if re.search(pattern, data):
            raise SystemExit(f'ERREUR: écriture ECARX potentielle dans {file}')
print('Contrôle statique : aucun appel direct aux écritures ECARX dans les sources V4')
