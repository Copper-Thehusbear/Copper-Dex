'use strict';
const native=typeof window.CopperDex!=='undefined';
const demo=[{name:'Netflix',pkg:'',icon:''},{name:'YouTube',pkg:'',icon:''},{name:'Prime Video',pkg:'',icon:''},{name:'Disney+',pkg:'',icon:''},{name:'Spotify',pkg:'',icon:''},{name:'Plex',pkg:'',icon:''},{name:'Browser',pkg:'',icon:''},{name:'Games',pkg:'',icon:''},{name:'Photos',pkg:'',icon:''},{name:'Files',pkg:'',icon:''},{name:'Cast',pkg:'',icon:''}];
let apps=[],focused=0,area='grid',buttonIndex=0,state='closed',settingsFocus=0,animationEnabled=true,launchTimer=null;
const settingsList=[{label:'Animation',type:'toggle'},{label:'Network & Wi-Fi',type:'wifi'},{label:'TV Inputs',type:'inputs'},{label:'Android System Settings',type:'settings'},{label:'Close Pokédex',type:'close'}];
const el=id=>document.getElementById(id);
function scale(){const s=Math.min(innerWidth/1920,innerHeight/1080);el('scaler').style.transform='translate('+((innerWidth-1920*s)/2)+'px,'+((innerHeight-1080*s)/2)+'px) scale('+s+')'}
function clock(){const d=new Date();el('clock').textContent=d.toLocaleTimeString('en-GB',{hour:'2-digit',minute:'2-digit'})+'  '+d.toLocaleDateString('en-GB',{weekday:'short',day:'numeric',month:'short'})}
function load(){if(native){try{const raw=JSON.parse(CopperDex.apps());if(Array.isArray(raw))apps=raw.filter(a=>a&&a.pkg&&a.name)}catch(e){el('status').textContent='App discovery unavailable'}}else{apps=demo;el('demo').textContent='Browser preview · Demo applications'}if(!apps.length){el('status').textContent='No launchable apps found';apps=native?[]:demo}render()}
function render(){
 const list=el('appgrid');list.replaceChildren();
 apps.forEach((app,i)=>{
  const t=document.createElement('button');t.type='button';t.className='tile';t.dataset.index=String(i);
  if(app.icon){const img=document.createElement('img');img.src=app.icon;img.alt='';t.append(img)}
  else{const p=document.createElement('div');p.className='placeholder';p.textContent=(app.name||'?').charAt(0);t.append(p)}
  const label=document.createElement('span');label.textContent=app.name;t.append(label);
  t.onclick=()=>{focused=i;area='grid';updateFocus();launch()};list.append(t);
 });
 updateFocus();
}
function updateFocus(){
 const list=el('appgrid');
 Array.from(list.children).forEach((t,i)=>t.classList.toggle('selected',area==='grid'&&focused===i));
 const a=apps[focused];
 el('previewName').textContent=a?a.name:'Copper Dex';
 const icon=el('previewIcon');icon.style.display=a&&a.icon?'block':'none';if(a&&a.icon)icon.src=a.icon;
 el('status').textContent=apps.length?'Apps '+(focused+1)+' / '+apps.length+' · OK to launch · ↑ for system controls':'Install TV apps to populate your Dex';
 document.querySelectorAll('.system').forEach((b,i)=>b.classList.toggle('selected',area==='buttons'&&buttonIndex===i));
 if(area==='grid'&&a){
  const tile=list.children[focused];
  if(tile){
   const top=tile.offsetTop-list.offsetTop, bottom=top+tile.offsetHeight;
   const viewTop=list.scrollTop,viewBottom=viewTop+list.clientHeight;
   if(top<viewTop)list.scrollTop=top;
   else if(bottom>viewBottom)list.scrollTop=bottom-list.clientHeight;
  }
 }
}

function showSettings(){state='settings';settingsFocus=0;el('settings-overlay').hidden=false;drawSettings()}
function drawSettings(){const target=el('settings-options');target.replaceChildren();settingsList.forEach((s,i)=>{const b=document.createElement('button');b.type='button';b.className='setting-row'+(i===settingsFocus?' active':'');b.textContent=s.label;if(s.type==='toggle'){const label=document.createElement('small');label.textContent=animationEnabled?'ON':'OFF';b.append(label)}b.onclick=()=>{settingsFocus=i;selectSetting()};target.append(b)})}
function selectSetting(){const type=settingsList[settingsFocus].type;if(type==='toggle'){animationEnabled=!animationEnabled;drawSettings();return}if(type==='close'){closeSettings();closeDex();return}if(native&&typeof CopperDex[type]==='function')CopperDex[type]()}
function closeSettings(){el('settings-overlay').hidden=true;state='open';updateFocus()}
function openDex(){if(state!=='closed')return;state='opening';el('device').classList.add('activating');setTimeout(()=>{el('device').classList.add('open');state='open';setTimeout(()=>el('device').classList.remove('activating'),700);updateFocus()},animationEnabled?250:0)}
function closeDex(){if(state==='launching')return;state='closed';el('device').classList.remove('open','activating');document.body.classList.remove('launching');el('status').textContent='Press OK to open'}
function openSystem(id){if(id==='settings'){showSettings();return}if(native&&typeof CopperDex[id]==='function')CopperDex[id]()}
function launch(){if(state!=='open')return;if(area==='buttons'){openSystem(['settings','wifi','inputs'][buttonIndex]);return}const a=apps[focused];if(!a)return;if(!native||!a.pkg){el('status').textContent='Preview only — install on Android TV to launch apps';return}state='launching';document.body.classList.add('launching');el('device').classList.add('activating');clearTimeout(launchTimer);launchTimer=setTimeout(()=>{CopperDex.open(a.pkg);setTimeout(()=>{document.body.classList.remove('launching');el('device').classList.remove('activating');state='open'},900)},animationEnabled?900:0)}

function key(k){
 if(state==='closed'){if(k==='ok')openDex();return}
 if(state==='opening'||state==='launching')return;
 if(state==='settings'){
  if(k==='back'){closeSettings();return}
  if(k==='up')settingsFocus=Math.max(0,settingsFocus-1);
  if(k==='down')settingsFocus=Math.min(settingsList.length-1,settingsFocus+1);
  if(k==='ok'){selectSetting();return}drawSettings();return
 }
 if(k==='back'){closeDex();return}
 if(k==='ok'){launch();return}
 if(area==='buttons'){
  if(k==='left')buttonIndex=Math.max(0,buttonIndex-1);
  if(k==='right')buttonIndex=Math.min(2,buttonIndex+1);
  if(k==='down')area='grid';
  updateFocus();return
 }
 if(k==='up'&&focused<4){area='buttons';updateFocus();return}
 let next=focused;const col=focused%4;
 if(k==='left'&&col>0)next--;
 if(k==='right'&&col<3&&next+1<apps.length)next++;
 if(k==='up')next-=4;
 if(k==='down')next+=4;
 if(next>=0&&next<apps.length)focused=next;
 updateFocus()
}
window.copperKey=key;window.copperBack=()=>key('back');
document.addEventListener('keydown',e=>{const k={ArrowLeft:'left',ArrowRight:'right',ArrowUp:'up',ArrowDown:'down',Enter:'ok',' ':'ok',Escape:'back',Backspace:'back'}[e.key];if(k){e.preventDefault();key(k)}});
document.querySelectorAll('.system').forEach((b,i)=>b.addEventListener('click',()=>{area='buttons';buttonIndex=i;if(state==='closed')openDex();else if(state==='open')launch();updateFocus()}));
el('replay').addEventListener('click',()=>{if(state==='settings')closeSettings();closeDex();setTimeout(openDex,450)});
addEventListener('resize',scale);scale();clock();setInterval(clock,30000);load();closeDex();
