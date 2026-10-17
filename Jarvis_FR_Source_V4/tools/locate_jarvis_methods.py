#!/usr/bin/env python3
"""Récupère les blocs bytecode des méthodes ciblées : outil d'analyse HORS LIGNE."""
import zipfile,struct,sys,json,pathlib

def u16(b,i):return struct.unpack_from('<H',b,i)[0]
def u32(b,i):return struct.unpack_from('<I',b,i)[0]
def leb(b,i):
 val=0;sh=0
 for _ in range(5):
  x=b[i];i+=1;val|=(x&127)<<sh
  if x<128:break
  sh+=7
 return val,i

def run(apk):
 b=zipfile.ZipFile(apk).read('classes.dex')
 cls=json.load(open(pathlib.Path(__file__).resolve().parent.parent/'analysis'/'classes_jarvis.json'))
 methods=json.load(open(pathlib.Path(__file__).resolve().parent.parent/'analysis'/'methodes_jarvis.json'))
 cdef=next(x for x in cls if x['class']=='Lorg/jarvis/car/EcarxApiHelper;')
 off=cdef['class_data_offset'];n=[]
 for _ in range(4):v,off=leb(b,off);n.append(v)
 # skip static/instance fields
 for _ in range(n[0]+n[1]):
  _,off=leb(b,off);_,off=leb(b,off)
 code={};ix=0
 for count in (n[2],n[3]):
  ix=0
  for _ in range(count):
   delta,off=leb(b,off);ix+=delta;_,off=leb(b,off);co,off=leb(b,off)
   if co:
    length=u32(b,co+12);code[ix]=b[co+16:co+16+2*length]
 focus=['setAcOn','setAcAuto','setClimateSystemOn','setFanSpeed','isAcOn','setWindow']
 results=[]
 for m in methods:
  if m['class']==cdef['class'] and m['name'] in focus:
   payload=code.get(m['method_index'],b'')
   hexes=['0x10010100','0x10010300','0x21030300']
   found=[h for h in hexes if struct.pack('<I',int(h,16)) in payload]
   results.append({'name':m['name'],'signature':m['signature'],'dex_method_index':m['method_index'],'code_bytes':len(payload),'literal_proprietes_dans_code':found})
 print(json.dumps(results,ensure_ascii=False,indent=2))
if __name__=='__main__':run(sys.argv[1])
