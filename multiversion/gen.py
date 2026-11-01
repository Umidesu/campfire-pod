import json,os,re,shutil,sys
MV=os.path.dirname(os.path.abspath(__file__)); ROOT=os.path.dirname(MV); RES=ROOT+'/src/main/resources'
# JDK install dirs for Gradle toolchains: comma-separated, from env (e.g. JDK_PATHS=/usr/lib/jvm/java-17,/usr/lib/jvm/java-21)
JDKS=os.environ.get('JDK_PATHS') or ','.join(x for x in (os.environ.get(k) for k in ('JDK17_HOME','JDK21_HOME','JDK25_HOME')) if x)
T=json.load(open(MV+'/targets.json'))
def vt(s): return tuple(int(x) for x in s.split('.'))
def pp(text,v):
    out=[];stack=[]  # each: [active_now, any_taken, parent_active]
    act=lambda: all(s[0] for s in stack)
    for line in text.split('\n'):
        s=line.strip()
        m=re.match(r'//#(if|elif|else|endif)\s*(.*)',s)
        if m:
            k,c=m.groups()
            if k=='if': r=eval(c,{'v':v}); stack.append([r,r])
            elif k=='elif': r=(not stack[-1][1]) and eval(c,{'v':v}); stack[-1][0]=r; stack[-1][1]|=r
            elif k=='else': stack[-1][0]=not stack[-1][1]; stack[-1][1]=True
            else: stack.pop()
            continue
        if not act(): continue
        out.append(re.sub(r'^(\s*)//\$',r'\1',line))
    return '\n'.join(out)
def gen(name):
    t=T[name]; v=vt(t['mc']); d=f'{MV}/build/{name}'
    shutil.rmtree(d+'/src',ignore_errors=True)
    jd=d+'/src/main/java/com/deniz/campfirepod'; os.makedirs(jd)
    for f in os.listdir(MV+'/template/java'):
        open(f'{jd}/{f}','w').write(pp(open(f'{MV}/template/java/{f}').read(),v))
    rd=d+'/src/main/resources'
    shutil.copytree(RES+'/assets',rd+'/assets')
    plural=v<(1,21)
    dd=rd+'/data'
    def w(rel,o):
        p=f'{dd}/{rel}'; os.makedirs(os.path.dirname(p),exist_ok=True); json.dump(o,open(p,'w'),indent=2)
    for sub,pl in [('recipe','recipes'),('loot_table/blocks','loot_tables/blocks'),('advancement/recipes/misc','advancements/recipes/misc')]:
        src=f'{RES}/data/campfire_pod/{sub}'
        for f in os.listdir(src):
            o=json.load(open(f'{src}/{f}'))
            if sub=='recipe':
                if v<(1,20,5): o['result']={'item':o['result']['id'],'count':1}
                if v>=(1,21,2): o['key']={k:('#'+x['tag'] if 'tag' in x else x['item']) for k,x in o['key'].items()}
            if sub.startswith('adv'):
                for c in o['criteria'].values():
                    for it in c.get('conditions',{}).get('items',[]):
                        if isinstance(it['items'],str): it['items']=[it['items']]
                    if v>=(26,3) and 'recipe' in c.get('conditions',{}): c['conditions']['recipes']=c['conditions'].pop('recipe')
            w(f'campfire_pod/{pl if plural else sub}/{f}',o)
    w(('minecraft/tags/blocks/' if plural else 'minecraft/tags/block/')+'mineable/axe.json',json.load(open(RES+'/data/minecraft/tags/block/mineable/axe.json')))
    if v>=(1,21,4):
        for f in os.listdir(RES+'/assets/campfire_pod/models/item'):
            p=f'{rd}/assets/campfire_pod/items/{f}'; os.makedirs(os.path.dirname(p),exist_ok=True)
            json.dump({'model':{'type':'minecraft:model','model':'campfire_pod:item/'+f[:-5]}},open(p,'w'),indent=2)
    fmj=json.load(open(RES+'/fabric.mod.json'))
    fmj['version']='1.1.0+'+name
    fmj['depends']={'fabricloader':'>=0.18.4' if v>=(26,) else '>=0.15.0','minecraft':t['range'],'fabric-api':'*','toughasnails':'*'}
    json.dump(fmj,open(rd+'/fabric.mod.json','w'),indent=2)
    unobf=v>=(26,)
    plugin="id 'net.fabricmc.fabric-loom' version '1.19.3'" if unobf else "id 'net.fabricmc.fabric-loom-remap' version '1.19.3'"
    mod='implementation' if unobf else 'modImplementation'
    maps='' if unobf else "  mappings loom.officialMojangMappings()\n"
    open(d+'/build.gradle','w').write(f"""plugins {{ {plugin} }}
version = '1.1.0+{name}'
group = 'com.deniz'
base {{ archivesName = 'campfire-pod' }}
dependencies {{
  minecraft 'com.mojang:minecraft:{t['mc']}'
{maps}  {mod} 'net.fabricmc:fabric-loader:0.18.4'
  {mod} 'net.fabricmc.fabric-api:fabric-api:{t['fapi']}'
}}
java {{ toolchain.languageVersion = JavaLanguageVersion.of({t['java']}) }}
tasks.withType(JavaCompile).configureEach {{ it.options.release = {t['java']} }}
""")
    open(d+'/settings.gradle','w').write("pluginManagement { repositories { maven { url = 'https://maven.fabricmc.net/' }; gradlePluginPortal() } }\nrootProject.name='campfire-pod'\n")
    props="org.gradle.jvmargs=-Xmx3G\n"
    if JDKS: props+=f"org.gradle.java.installations.paths={JDKS}\norg.gradle.java.installations.auto-download=false\n"
    open(d+'/gradle.properties','w').write(props)
for n in (sys.argv[1:] or T): gen(n); print('generated',n)
