#!/usr/bin/env python3
"""Lecture statique de la table des chaînes et des signatures d'un DEX. Aucune écriture vers le véhicule."""
import json, struct, csv, zipfile, sys, pathlib

def u32(b,o): return struct.unpack_from('<I',b,o)[0]
def u16(b,o): return struct.unpack_from('<H',b,o)[0]
def read_uleb(b,o):
 n=0;shift=0
 for _ in range(5):
  v=b[o];o+=1;n|=(v&0x7f)<<shift
  if v<128:break
  shift+=7
 return n,o

def parse(apk, output):
 with zipfile.ZipFile(apk) as z: b=z.read('classes.dex')
 assert b[:4]==b'dex\n','Fichier DEX invalide'
 strs=[]
 for i in range(u32(b,0x38)):
  off=u32(b,u32(b,0x3c)+4*i)
  _,off=read_uleb(b,off)
  end=b.index(b'\0',off)
  strs.append(b[off:end].decode('utf-8','replace'))
 types=[strs[u32(b,u32(b,0x44)+4*i)] for i in range(u32(b,0x40))]
 def typ(idx):return types[idx] if idx<len(types) else '?'
 proto_off=u32(b,0x4c); protos=[]
 for i in range(u32(b,0x48)):
  off=proto_off+12*i;ret=typ(u32(b,off+4));par_off=u32(b,off+8);pars=[]
  if par_off:
   cnt=u32(b,par_off);pars=[typ(u16(b,par_off+4+2*j)) for j in range(cnt)]
  protos.append({'return':ret,'params':pars})
 method_off=u32(b,0x5c);methods=[]
 for i in range(u32(b,0x58)):
  off=method_off+8*i; cl=u16(b,off);pr=u16(b,off+2); nm=u32(b,off+4)
  cls=typ(cl)
  if cls.startswith('Lorg/jarvis/'):
   methods.append({'class':cls,'name':strs[nm],'signature':protos[pr],'method_index':i})
 classes=[]
 for i in range(u32(b,0x60)):
  off=u32(b,0x64)+32*i;cls=typ(u32(b,off))
  if cls.startswith('Lorg/jarvis/'):
   classes.append({'class':cls,'super':typ(u32(b,off+8)),'annotations_offset':u32(b,off+20),'class_data_offset':u32(b,off+24)})
 out=pathlib.Path(output);out.mkdir(parents=True,exist_ok=True)
 (out/'classes_jarvis.json').write_text(json.dumps(classes,ensure_ascii=False,indent=2),encoding='utf-8')
 (out/'methodes_jarvis.json').write_text(json.dumps(methods,ensure_ascii=False,indent=2),encoding='utf-8')
 with (out/'chaines_cyrilliques.tsv').open('w',encoding='utf-8',newline='') as f:
  w=csv.writer(f,delimiter='\t');w.writerow(['id_dex','chaine_originale','traduction_fr_a_reviser'])
  for i,s in enumerate(strs):
   if any('\u0400'<=c<='\u04ff' for c in s):w.writerow([i,s,''])
 wanted=('EcarxApiHelper','CarCommand','Voice','Vosk','Hvac','MainActivity','Notification','Ac')
 selected=[m for m in methods if any(w.lower() in m['class'].lower() or w.lower() in m['name'].lower() for w in wanted)]
 (out/'methodes_prioritaires.json').write_text(json.dumps(selected,ensure_ascii=False,indent=2),encoding='utf-8')
 summary={'dex_strings':len(strs),'jarvis_classes':len(classes),'jarvis_methods':len(methods),'priority_methods':len(selected),'cyrillic_strings':sum(any('\u0400'<=c<='\u04ff' for c in s) for s in strs)}
 (out/'resume.json').write_text(json.dumps(summary,ensure_ascii=False,indent=2),encoding='utf-8')
 print(json.dumps(summary,ensure_ascii=False))

if __name__=='__main__':
 if len(sys.argv)!=3:sys.exit('Utilisation: python tools/inventory_dex.py JARVIS.apk repertoire_sortie')
 parse(sys.argv[1],sys.argv[2])
