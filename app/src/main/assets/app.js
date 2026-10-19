'use strict';
const native=typeof window.CopperDex!=='undefined';
const demo=[{name:'Netflix',pkg:'',icon:''},{name:'YouTube',pkg:'',icon:''},{name:'Prime Video',pkg:'',icon:''},{name:'Disney+',pkg:'',icon:''},{name:'Spotify',pkg:'',icon:''},{name:'Plex',pkg:'',icon:''},{name:'Browser',pkg:'',icon:''},{name:'Games',pkg:'',icon:''},{name:'Photos',pkg:'',icon:''},{name:'Files',pkg:'',icon:''},{name:'Cast',pkg:'',icon:''}];
let apps=[],focused=0,area='grid',buttonIndex=0;
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
function launch(){if(area==='buttons'){const id=['settings','wifi','inputs'][buttonIndex];if(native&&typeof CopperDex[id]==='function')CopperDex[id]();return}const a=apps[focused];if(a&&a.pkg&&native)CopperDex.open(a.pkg)}
function key(k){
 if(k==='back'){area='grid';focused=0;el('appgrid').scrollTop=0;updateFocus();return}
 if(k==='ok'){launch();return}
 if(area==='buttons'){
  if(k==='left')buttonIndex=Math.max(0,buttonIndex-1);
  if(k==='right')buttonIndex=Math.min(2,buttonIndex+1);
  if(k==='down')area='grid';
  updateFocus();return;
 }
 if(k==='up'&&focused<4){area='buttons';updateFocus();return}
 let next=focused;const col=focused%4;
 if(k==='left'&&col>0)next--;
 if(k==='right'&&col<3&&next+1<apps.length)next++;
 if(k==='up')next-=4;
 if(k==='down')next+=4;
 if(next>=0&&next<apps.length)focused=next;
 updateFocus();
}
window.copperKey=key;window.copperBack=()=>key('back');
document.addEventListener('keydown',e=>{const k={ArrowLeft:'left',ArrowRight:'right',ArrowUp:'up',ArrowDown:'down',Enter:'ok',' ':'ok',Escape:'back',Backspace:'back'}[e.key];if(k){e.preventDefault();key(k)}});
document.querySelectorAll('.system').forEach((b,i)=>b.addEventListener('click',()=>{area='buttons';buttonIndex=i;launch();updateFocus()}));
el('replay').addEventListener('click',()=>{const d=el('device');d.classList.remove('open');setTimeout(()=>d.classList.add('open'),120)});
addEventListener('resize',scale);scale();clock();setInterval(clock,30000);load();setTimeout(()=>el('device').classList.add('open'),500);
