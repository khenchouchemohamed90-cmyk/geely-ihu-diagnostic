#!/usr/bin/env python3
"""Explore le DEX original de Jarvis en lecture seule: références aux chaînes/ appels HVAC."""
import zipfile,struct,sys,json,re,pathlib,collections
P=pathlib.Path(__file__).resolve().parent.parent
APK=pathlib.Path(sys.argv[1]) if len(sys.argv)>1 else pathlib.Path('/mnt/data/jarvis_perm_review/jarvis-v2.8-Lite-build260605.apk')
B=zipfile.ZipFile(APK).read('classes.dex')
u16=lambda i:struct.unpack_from('<H',B,i)[0]
u32=lambda i:struct.unpack_from('<I',B,i)[0]
def leb(i):
 v=0;shift=0
 for _ in range(5):
  x=B[i];i+=1;v|=(x&127)<<shift
  if not x&128:return v,i
  shift+=7
 raise ValueError('ULEB bad')
def sstring(i):
 off=u32(u32(0x3c)+i*4);_,off=leb(off)
 return B[off:B.index(b'\0',off)].replace(b'\xc0\x80',b'\0').decode('utf-8','replace')
STR=[sstring(i) for i in range(u32(0x38))]
TYPE=[STR[u32(u32(0x44)+i*4)] for i in range(u32(0x40))]
PROTOS=[]
for i in range(u32(0x48)):
 o=u32(0x4c)+12*i;po=u32(o+8)
 ps=[TYPE[u16(po+4+j*2)] for j in range(u32(po))] if po else []
 PROTOS.append('('+','.join(ps)+')->'+TYPE[u32(o+4)])
METHODS=[]
for i in range(u32(0x58)):
 o=u32(0x5c)+i*8
 METHODS.append(TYPE[u16(o)]+'->'+STR[u32(o+4)]+PROTOS[u16(o+2)])
# apk may include op codes with simple sizes; full method stream decoder from prior verified patcher
DEFS={}
for k in range(u32(0x60)):
 o=u32(0x64)+32*k;cls=TYPE[u32(o)];p=u32(o+24)
 if not p:continue
 nf,p=leb(p);ni,p=leb(p);nd,p=leb(p);nv,p=leb(p)
 for j in range(nf+ni):_,p=leb(p);_,p=leb(p)
 for n in (nd,nv):
  ix=0
  for j in range(n):
   delta,p=leb(p);ix+=delta;_,p=leb(p);co,p=leb(p)
   if co:DEFS[ix]=(co,cls)
def oplen(code,q):
 v=u16(code+2*q);op=v&255;hi=v>>8
 if op==0:
  if hi==0:return 1
  if hi==1:return 4+2*u16(code+2*(q+1))
  if hi==2:return 2+4*u16(code+2*(q+1))
  if hi==3:return 4+(u16(code+2*(q+1))*u32(code+2*(q+2))+1)//2
  raise ValueError(('bad payload',v,q))
 if op in (0x03,0x06,0x09,0x14,0x17,0x1b,0x24,0x25,0x26,0x2a,0x2b,0x2c):return 3
 if op==0x18:return 5
 if op in (0x02,0x05,0x08,0x13,0x15,0x16,0x19,0x1a,0x1c,0x1f,0x20,0x22,0x23,0x29):return 2
 if op in (0xfa,0xfb):return 4
 if op in (0xfc,0xfd):return 3
 if op in (0xfe,0xff):return 2
 if 0x2d<=op<=0x3d or 0x44<=op<=0x6d or 0x90<=op<=0xaf or 0xd0<=op<=0xe2:return 2
 if 0x6e<=op<=0x72 or 0x74<=op<=0x78:return 3
 if op in (0x73,0x79,0x7a) or 0x3e<=op<=0x43 or 0xe3<=op<=0xf9:raise ValueError(('unused',op,q))
 return 1
INS={};CALLS=collections.defaultdict(list);STRREFS=collections.defaultdict(list)
for ix,(co,cls) in DEFS.items():
 off=co+16;n=u32(co+12);q=0;lst=[]
 while q<n:
  op=B[off+2*q];step=oplen(off,q)
  if op in (0x1a,0x1b):
   si=(u16(off+2*(q+1)) if op==0x1a else u32(off+2*(q+1)))
   STRREFS[si].append((ix,q));lst.append({'offset':q,'op':'string','idx':si,'text':STR[si]})
  if 0x6e<=op<=0x72 or 0x74<=op<=0x78:
   target=u16(off+2*(q+1));CALLS[target].append((ix,q));lst.append({'offset':q,'op':'call','idx':target,'method':METHODS[target]})
  if op==0x14:
   val=struct.unpack_from('<i',B,off+2*(q+1))[0];
   if val in (0x10010100,0x10010300,0x21030300):lst.append({'offset':q,'op':'const-int','value':hex(val)})
  q+=step
 assert q==n
 INS[ix]=lst
out={}
key_methods=['setAcOn','setClimateSystemOn','isAcOn','setFanSpeed','setAcAuto']
targets=[i for i,s in enumerate(METHODS) if 'Lorg/jarvis/car/EcarxApiHelper;' in s and any('->'+x+'(' in s for x in key_methods)]
out['targets']=[{'idx':i,'method':METHODS[i],'ins':INS.get(i,[]),'directCallers':[{'idx':j,'method':METHODS[j],'offset':o} for j,o in CALLS.get(i,[])]} for i in targets]
# phrases connected to AC or HVAC
vocabulary=[]
for i,s in enumerate(STR):
 if re.search(r'кондици|климат|обдув|вентилят|температур|компрессор|A/C|climat|air condition|ac on',s,re.I):
  refs=[{'idx':j,'method':METHODS[j],'offset':o} for j,o in STRREFS.get(i,[])]
  vocabulary.append({'idx':i,'text':s[:350],'references':refs})
out['vocabulary']=vocabulary
# Extraits des routeurs internes qui traduisent les chaînes vocales en appels ECARX
router_names=('->T()','->l(Ljava/lang/String;)','->u(Ljava/lang/String;)','->t(Ljava/lang/String;)','->h(Li4/a;','->b(Ljava/lang/String;)')
router_indices=[i for i,m in enumerate(METHODS) if m.startswith('Lh4/m;') and any(r in m for r in router_names)]
out['router_instructions']={METHODS[i]:INS.get(i,[]) for i in router_indices}
# route expansion: trace up to 3 levels with candidate regex
front=set(targets);seen=set(targets);levels=[]
for n in range(4):
 callers=[];nextset=set()
 for ix in front:
  for ci,off in CALLS.get(ix,[]):
   callers.append({'method':METHODS[ci],'idx':ci,'calls':METHODS[ix],'offset':off,'near_strings':[v['text'] for v in INS.get(ci,[]) if v['op']=='string'][:15]})
   if ci not in seen:nextset.add(ci)
 levels.append(callers);seen|=nextset;front=nextset
out['reverse_levels']=levels
outfile=P/'analysis'/'trace_commandes_hvac.json';outfile.write_text(json.dumps(out,ensure_ascii=False,indent=2))
print('APK',str(APK),'methods',len(METHODS),'defined',len(DEFS),'ref_strings',len(STRREFS),'hvac_strings',len(vocabulary))
print('Targets:')
for t in out['targets']:
 print(t['idx'],t['method'], 'direct callers',len(t['directCallers']))
 for c in t['directCallers'][:15]:print('  CALLED BY',c['method'])
print('Relevant phrases:')
for item in vocabulary[:70]:
 if item['references']:
  print('STR',item['idx'],repr(item['text'][:100]),'used by',[(v['idx'],v['method'][:96]) for v in item['references'][:8]])
print('Call-chain')
for d,links in enumerate(levels):
 print('LEVEL',d+1,'n=',len(links))
 for x in links[:40]:print(' ',x['method'][:140],'<-',x['calls'].split('->')[-1][:80], 'str',str(x['near_strings'])[:150])
print('OUTPUT',outfile)
