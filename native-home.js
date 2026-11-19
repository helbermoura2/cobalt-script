/* Cobalt HX Native Preview 0.1.1. Original TV UI; Cobalt playback and blocking unchanged.
Design: Taste Skill hierarchy, Transitions.dev short reversible motion, React Bits focus cues;
plain CSS, no borrowed commercial assets, no autoplay previews. */
(() => {
'use strict';
const VERSION='0.1.1';
const prev=window.CobaltNative;
if(prev && prev.version===VERSION)return;
if(prev && typeof prev.destroy==='function')prev.destroy();
const css="\n#hx-native-shell{position:fixed;inset:0;z-index:2147483100;background:#090a0c;color:#f6f2ed;font-family:Arial,Helvetica,sans-serif;overflow:hidden;box-sizing:border-box}\n#hx-native-shell *{box-sizing:border-box}\n#hx-native-shell[hidden]{display:none!important}\n#hx-native-shell button{font:inherit;color:inherit;cursor:pointer}\n#hx-native-shell button:focus{outline:3px solid #eac18e;outline-offset:2px}\n#hx-native-shell .side{position:absolute;left:0;top:0;bottom:0;width:265px;padding:32px 18px;background:linear-gradient(160deg,#292018,#101217 70%);border-right:2px solid #c3945e}\n#hx-native-shell .logo{color:#edc894;font-size:32px;font-weight:900;letter-spacing:2px}\n#hx-native-shell .caption{font-size:11px;letter-spacing:2px;color:#c7a87f;margin:9px 0 34px}\n#hx-native-shell .nav{display:block;width:100%;border:1px solid transparent;border-radius:11px;text-align:left;background:transparent;padding:13px 14px;margin-bottom:6px;font-size:20px;color:#d5cec5}\n#hx-native-shell .nav.selected{background:#503c28;border-color:#ac8151;color:#ffdfad}\n#hx-native-shell .nav:focus{background:#7f5a34;outline:2px solid #edbf8b}\n#hx-native-shell .body{position:absolute;top:0;bottom:0;left:265px;right:0;padding:31px 38px 12px;background:radial-gradient(ellipse at 92% 0%,#30241b 0%,#121418 51%,#090b0e)}\n#hx-native-shell .header{height:103px;border-bottom:1px solid #876541;display:flex;justify-content:space-between;align-items:center}\n#hx-native-shell .eyebrow{font-size:12px;letter-spacing:2px;color:#d9ad77}\n#hx-native-shell .heading{font-size:38px;font-weight:800;margin-top:4px}\n#hx-native-shell .pill{background:#35291d;border:1px solid #a17c4d;border-radius:16px;color:#e5c598;padding:10px;font-size:13px}\n#hx-native-shell .desc{color:#c1b6a6;margin:19px 0;font-size:17px}\n#hx-native-shell .grid{height:calc(100% - 196px);overflow-y:auto;display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:20px;padding:7px 4px 50px;align-content:start}\n#hx-native-shell .card{border:1px solid #82613c;background:#1e1e20;border-radius:13px;display:flex;flex-direction:column;min-width:0;overflow:hidden;text-align:left;padding:0 0 12px;box-shadow:0 8px 21px #0009;transition:transform .19s ease,border .19s ease,box-shadow .19s ease}\n#hx-native-shell .card:focus{transform:translateY(-4px) scale(1.01);border:2px solid #dfae72;outline:none;box-shadow:0 0 0 4px #dca76770,0 17px 32px #000c}\n#hx-native-shell .poster{position:relative;width:100%;aspect-ratio:16/9;background:linear-gradient(145deg,#342d23,#17191d);overflow:hidden;display:block}\n#hx-native-shell .poster img{width:100%;height:100%;object-fit:cover;display:block}\n#hx-native-shell .fallback{position:absolute;top:40%;width:100%;text-align:center;letter-spacing:2px;font-size:16px;color:#dab17e}\n#hx-native-shell .ctitle{font-size:17px;line-height:1.3;font-weight:bold;padding:11px 10px 0;min-height:57px;overflow:hidden}\n#hx-native-shell .meta{font-size:12px;color:#caa675;padding:4px 10px}\n#hx-native-shell .settings{display:block;width:100%;text-align:left;font-size:20px;padding:15px 20px;border-radius:12px;background:#242326;border:1px solid #94734b;margin:10px 0}\n#hx-native-shell .settings:focus{background:#694a30}\n#hx-native-shell .state{color:#e9c79a;font-size:16px;border-left:4px solid #c0935f;padding:15px;margin:20px 0;background:#2d251e}\n#hx-native-shell .status{position:absolute;right:35px;bottom:9px;color:#caae8a;font-size:14px}\n#hx-native-shell .foot{position:absolute;bottom:22px;font-size:12px;color:#ae9678}\nhtml.hn-graphite #hx-native-shell .body{background:radial-gradient(ellipse at 92% 0%,#1d2d34,#0b1117 54%,#080a0c)}\nhtml.hn-graphite #hx-native-shell .side{background:linear-gradient(165deg,#1d2c34,#111418 75%);border-color:#629aa4}\nhtml.hn-graphite #hx-native-shell .card{border-color:#6396a0}\nhtml.hn-nomotion #hx-native-shell *{transition:none!important;transform:none!important}\n@media(max-width:1200px){#hx-native-shell .side{width:225px}#hx-native-shell .body{left:225px}#hx-native-shell .grid{grid-template-columns:repeat(3,minmax(0,1fr))}}\n@media(prefers-reduced-motion:reduce){#hx-native-shell *{transition:none!important}}\n";
const tabs=['Início','Pesquisar','Inscrições','Ao Vivo','Histórico','Biblioteca','Configurações'];
let root,style,nav=[],catalog=[],screen='Início',tab=0,chosen=0,focus='nav',theme='bronze',motion=true;
let liveMode=false,first=false,interval,keyFn,lastHash='',status='Prévia 0.1.0 • motor de reprodução Cobalt';
const create=(tag,cls,text)=>{
 const node=document.createElement(tag);if(cls)node.className=cls;
 if(typeof text==='string')node.textContent=text;return node;
};
function watching(){
 try{const v=document.querySelector('video'),url=(location.hash||'')+' '+(location.pathname||'');
 return !!(v && v.readyState && /(?:\/watch[?#\/]|\/watch$)/.test(url));}
 catch(_){return false}
}
function mini(){return window.isPipPlaying===true}
function cardsFromNative(){
 let els=[];try{els=document.querySelectorAll('ytlr-grid-video-renderer,ytlr-video-renderer,ytlr-tile-renderer,ytlr-rich-grid-media')}catch(_){}
 const found=[],seen=new Set();
 for(let i=0;i<els.length&&found.length<64;i++){
  const tile=els[i];if(tile.closest&&tile.closest('#hx-native-shell,ytlr-player'))continue;
  const titleNode=tile.querySelector('#title,[id*="video-title"],[role="heading"],h3,[aria-label]');
  const title=((titleNode && (titleNode.textContent||titleNode.getAttribute('aria-label'))) || tile.getAttribute('aria-label') || tile.textContent||'').replace(/\s+/g,' ').trim().slice(0,110);
  if(title.length<3)continue;
  let poster='';const images=tile.querySelectorAll('img');
  for(let j=0;j<images.length;j++){const src=images[j].currentSrc||images[j].src||images[j].getAttribute('src');if(src && /^https?:\/\//.test(src)){poster=src;break}}
  if(!poster){
   const styled=tile.querySelectorAll('[style*="background"],ytlr-thumbnail-renderer');
   for(let j=0;j<Math.min(styled.length,9);j++){
    let bg=styled[j].style&&styled[j].style.backgroundImage||'';
    if(!bg&&typeof getComputedStyle==='function')try{bg=getComputedStyle(styled[j]).backgroundImage||''}catch(_){}
    const m=bg&&bg.match(/url\(["']?(https?:[^"')]+)["']?\)/);
    if(m){poster=m[1];break}
   }
  }
  const key=title+'|'+poster;if(seen.has(key))continue;seen.add(key);
  found.push({title,poster,tile});
 }
 return found;
}
function updateStatus(s){
 status=String(s);if(root){const x=root.querySelector('.status');if(x)x.textContent=status}
}
function makeShell(){
 if(root||!document.body||!document.head)return;
 style=create('style');style.id='hx-native-style';
 const nonce=document.querySelector('style[nonce],script[nonce]');if(nonce&&nonce.nonce)style.nonce=nonce.nonce;
 style.textContent=css;document.head.appendChild(style);
 root=create('div');root.id='hx-native-shell';root.hidden=true;
 // Inline fallback for stricter YouTube TV content-security policies.
 Object.assign(root.style,{position:'fixed',inset:'0',zIndex:'2147483100',background:'#090a0c',color:'#f6f2ed',fontFamily:'Arial,sans-serif',overflow:'hidden'});
 const left=create('aside','side');Object.assign(left.style,{position:'absolute',left:'0',top:'0',bottom:'0',width:'265px',background:'#1b1815',borderRight:'2px solid #bc8c57',padding:'32px 18px'});
 const brand=create('div','logo','COBALT HX');Object.assign(brand.style,{color:'#edc894',fontSize:'28px',fontWeight:'900',letterSpacing:'1px',lineHeight:'1.2',whiteSpace:'nowrap'});left.appendChild(brand);
 const subtitle=create('div','caption','NATIVE EXPERIENCE • PRÉVIA');Object.assign(subtitle.style,{color:'#d9b88e',fontSize:'13px',letterSpacing:'1px',margin:'10px 0 34px'});left.appendChild(subtitle);
 tabs.forEach((name,i)=>{
  const b=create('button','nav',name);b.type='button';
  Object.assign(b.style,{display:'block',width:'100%',margin:'6px 0',padding:'13px 14px',textAlign:'left',fontSize:'19px',borderRadius:'12px',background:'transparent',border:'1px solid transparent',color:'#e1d9ca'});
  b.addEventListener('focus',()=>{b.style.background='#705031';b.style.borderColor='#e0b17d'});
  b.addEventListener('blur',()=>{b.style.background=i===tab?'#483724':'transparent';b.style.borderColor=i===tab?'#a58055':'transparent'});
  b.addEventListener('click',()=>openTab(i));left.appendChild(b);nav.push(b);
 });
 const foot=create('div','foot','Motor Cobalt • SponsorBlock • sem prévias');Object.assign(foot.style,{position:'absolute',bottom:'24px',fontSize:'12px',color:'#b9a17f'});left.appendChild(foot);
 const main=create('main','body');Object.assign(main.style,{position:'absolute',left:'265px',top:'0',right:'0',bottom:'0',padding:'32px 38px',background:'radial-gradient(ellipse at 90% 0%,#30251c,#101215 60%,#090a0c)'});
 const head=create('div','header');Object.assign(head.style,{height:'102px',borderBottom:'1px solid #86643e',display:'flex',alignItems:'center',justifyContent:'space-between'});
 const group=create('div');
 const eyebrow=create('div','eyebrow','SUA TV. SEU JEITO.');Object.assign(eyebrow.style,{color:'#d4aa7c',fontSize:'14px',letterSpacing:'2px'});group.appendChild(eyebrow);
 const headerText=create('div','heading','Destaques');Object.assign(headerText.style,{color:'#fff5e8',fontSize:'35px',fontWeight:'bold',marginTop:'4px'});group.appendChild(headerText);
 head.appendChild(group);const ver=create('div','pill','PRÉVIA 0.1.1');Object.assign(ver.style,{color:'#f2cf9c',fontSize:'14px',border:'1px solid #a77d4a',borderRadius:'17px',padding:'8px 14px',background:'#302519'});head.appendChild(ver);
 main.appendChild(head);const desc=create('div','desc','Sem prévias automáticas. OK abre o vídeo selecionado.');Object.assign(desc.style,{color:'#e2d4c1',fontSize:'18px',margin:'20px 0'});main.appendChild(desc);
 const grid=create('div','grid');Object.assign(grid.style,{height:'calc(100% - 190px)',overflowY:'auto',display:'grid',gridTemplateColumns:'repeat(4,minmax(0,1fr))',gap:'20px',alignContent:'start',padding:'8px 5px 46px'});
 main.appendChild(grid);
 const settings=create('div','panel');main.appendChild(settings);
 const msg=create('div','status');Object.assign(msg.style,{position:'absolute',right:'36px',bottom:'9px',color:'#c6a67e',fontSize:'14px'});main.appendChild(msg);
 root.appendChild(left);root.appendChild(main);document.body.appendChild(root);
 keyFn=e=>key(e);window.addEventListener('keydown',keyFn,true);
 interval=setInterval(tick,1600);tick();
}
function section(title,description){
 root.querySelector('.heading').textContent=title;
 root.querySelector('.desc').textContent=description;
}
function setNav(){
 nav.forEach((b,i)=>{b.classList.toggle('selected',i===tab);b.style.background=i===tab?'#483724':'transparent'});
 root.querySelector('.grid').style.display=screen==='Configurações'?'none':'grid';
 root.querySelector('.panel').style.display=screen==='Configurações'?'block':'none';
}
function goNav(i){focus='nav';if(nav[i])nav[i].focus()}
function goCard(i){
 const buttons=root.querySelectorAll('.card');
 if(!buttons.length){goNav(tab);return}
 chosen=Math.max(0,Math.min(i,buttons.length-1));
 focus='cards';buttons[chosen].focus();
 if(buttons[chosen].scrollIntoView)try{buttons[chosen].scrollIntoView({block:'nearest'})}catch(_){}
}
function draw(){
 const grid=root.querySelector('.grid');grid.textContent='';
 let data=catalog;
 if(screen==='Ao Vivo'){
  const matches=data.filter(x=>/\bao vivo\b|\blive\b/i.test(x.title));
  if(matches.length)data=matches;
 }
 if(!data.length){const msg=create('p','','Carregando vídeos do YouTube TV...');msg.style.color='#fff0db';msg.style.fontSize='22px';grid.appendChild(msg);return}
 data.forEach(c=>{
  const card=create('button','card');card.type='button';
  Object.assign(card.style,{textAlign:'left',minWidth:'0',padding:'0 0 10px',color:'#f6f1e8',background:'#1d1d20',border:'1px solid #986f43',borderRadius:'13px',overflow:'hidden'});
  const poster=create('div','poster');Object.assign(poster.style,{width:'100%',aspectRatio:'16/9',position:'relative',background:'linear-gradient(145deg,#352e26,#181a20)'});
  if(c.poster){
   const image=create('img');image.src=c.poster;image.alt='';Object.assign(image.style,{width:'100%',height:'100%',objectFit:'cover',display:'block'});
   image.addEventListener('error',()=>{image.remove();poster.appendChild(create('span','fallback','COBALT HX'))});
   poster.appendChild(image);
  }else poster.appendChild(create('span','fallback','COBALT HX'));
  card.appendChild(poster);
  const t=create('div','ctitle',c.title);Object.assign(t.style,{padding:'12px 10px 0',color:'#f6f1e8',fontWeight:'bold',fontSize:'19px',minHeight:'61px',lineHeight:'1.32',textShadow:'0 1px 2px #0006'});
  card.appendChild(t);const meta=create('div','meta','OK • Reproduzir');Object.assign(meta.style,{color:'#d5b48a',fontSize:'13px',padding:'4px 10px'});card.appendChild(meta);
  card.addEventListener('focus',()=>{card.style.border='3px solid #dfb47f';card.style.background='#382c20';card.style.transform=motion?'translateY(-4px)':'none'});
  card.addEventListener('blur',()=>{card.style.border='1px solid #986f43';card.style.background='#1d1d20';card.style.transform='none'});
  card.addEventListener('click',()=>play(c));
  grid.appendChild(card);
 });
 if(focus==='cards')goCard(chosen);
}
function showSettings(){
 const p=root.querySelector('.panel');p.textContent='';
 section('Configurações','Visual, reprodução e atualizações do HX Native.');
 const opts=[
  ['↻ Atualizar interface agora',()=>refresh()],
  ['Tema: '+(theme==='bronze'?'BRONZE IMPERIAL':'GRAFITE AZUL'),()=>{
    theme=theme==='bronze'?'graphite':'bronze';document.documentElement.classList.toggle('hn-graphite',theme!=='bronze');showSettings()}],
  ['Transições: '+(motion?'SUAVES':'DESATIVADAS'),()=>{
    motion=!motion;document.documentElement.classList.toggle('hn-nomotion',!motion);showSettings()}],
  ['Abrir interface original temporariamente',()=>nativeFallback()],
  ['Voltar ao Início',()=>openTab(0)]
 ];
 opts.forEach(([name,handler])=>{
  const b=create('button','settings',name);b.type='button';
  Object.assign(b.style,{display:'block',width:'100%',padding:'16px 22px',margin:'12px 0',textAlign:'left',color:'#f8f1e8',fontSize:'22px',border:'1px solid #a17a4c',background:'#262327',borderRadius:'13px'});
  b.addEventListener('focus',()=>{b.style.background='#795334'});b.addEventListener('blur',()=>{b.style.background='#262327'});
  b.addEventListener('click',handler);p.appendChild(b);
 });
 const note=create('div','state','AdBlock, SponsorBlock, 4K, miniplayer e OK longo continuam sob responsabilidade do Cobalt. O botão atualiza a interface; APKs futuros exigem instalador Android e assinatura de atualização fixa.');Object.assign(note.style,{color:'#ebc999',fontSize:'18px',padding:'16px',background:'#29221b',borderLeft:'4px solid #d2a26c',marginTop:'22px'});p.appendChild(note);
 updateStatus(status);
}
function openTab(i){
 if(!root)return;
 tab=i;screen=tabs[i];setNav();
 if(i===6){showSettings();const b=root.querySelector('.settings');if(b){b.focus();focus='settings'}return}
 if([1,2,4,5].includes(i)){nativeFallback(screen);return}
 section(i===3?'Ao Vivo':'Destaques',i===3?'Transmissões identificadas na lista atual.':'Conteúdo real do YouTube TV; sem prévias automáticas.');
 draw();goNav(i);updateStatus(status);
}
function nativeFallback(category){
 liveMode=true;root.hidden=true;
 updateStatus('Modo original • VOLTAR retorna à interface HX');
 const map={Pesquisar:['pesquisar','search'],Inscrições:['inscrições','subscriptions'],Histórico:['histórico','history'],Biblioteca:['biblioteca','library']};
 const words=map[category]||[];
 if(words.length){
  const elems=document.querySelectorAll('ytlr-guide-entry-renderer,ytlr-guide-item-renderer,[role="menuitem"]');
  for(let i=0;i<elems.length;i++){
   const text=(elems[i].textContent||elems[i].getAttribute('aria-label')||'').toLowerCase();
   if(words.some(w=>text.includes(w))){try{elems[i].focus();elems[i].click()}catch(_){}break}
  }
 }
}
function sendEnter(node){
 for(const type of ['keydown','keyup']){
  try{const ev=document.createEvent('Event');ev.initEvent(type,true,true);
   Object.defineProperty(ev,'keyCode',{value:13});Object.defineProperty(ev,'which',{value:13});
   Object.defineProperty(ev,'key',{value:'Enter'});node.dispatchEvent(ev)}catch(_){}
 }
}
function play(c){
 if(!c.tile||!c.tile.isConnected){updateStatus('Vídeo alterado. Recarregando lista.');tick();return}
 const before=(location.hash||'')+' '+(location.pathname||'');
 liveMode=true;root.hidden=true;
 try{
  const target=c.tile.querySelector('[role="button"],[tabindex="0"],a')||c.tile;
  target.focus();if(target.click)target.click();
  setTimeout(()=>{
   const now=(location.hash||'')+' '+(location.pathname||'');
   if(!watching()&&!mini()&&now===before)sendEnter(target);
  },650);
 }catch(err){liveMode=false;root.hidden=false;updateStatus('Falha ao abrir vídeo: '+String(err))}
}
function tick(){
 if(!root||document.visibilityState==='hidden')return;
 if(watching()||mini()){root.hidden=true;return}
 const route=(location.hash||'')+' '+(location.pathname||'');
 const found=cardsFromNative();
 if(found.length && (found.length!==catalog.length||found[0].title!==catalog[0]?.title)){
  catalog=found;draw();
 }
 if(!liveMode&&catalog.length){
  root.hidden=false;
  if(!first){first=true;openTab(0)}
 }
 lastHash=route;
}
function refresh(){
 updateStatus('Verificando atualização visual...');
 const f=window.TTBackExternal&&window.TTBackExternal.refreshNativeUI;
 if(typeof f!=='function'){updateStatus('Atualizador indisponível nesta compilação.');return}
 try{f()}catch(e){updateStatus('Falha ao atualizar: '+String(e))}
}
function key(e){
 const back=[4,27,166,461,10009].includes(e.keyCode)||['Escape','BrowserBack','GoBack'].includes(e.key);
 if(root&&root.hidden&&liveMode&&!watching()&&!mini()&&back){
  liveMode=false;root.hidden=false;openTab(0);e.preventDefault();e.stopImmediatePropagation();return;
 }
 if(!root||root.hidden)return;
 if(back){if(screen!=='Início')openTab(0);else nativeFallback();e.preventDefault();e.stopImmediatePropagation();return}
 const up=[19,38].includes(e.keyCode)||e.key==='ArrowUp';
 const down=[20,40].includes(e.keyCode)||e.key==='ArrowDown';
 const left=[21,37].includes(e.keyCode)||e.key==='ArrowLeft';
 const right=[22,39].includes(e.keyCode)||e.key==='ArrowRight';
 if(!(up||down||left||right))return;
 if(focus==='nav'){
  if(up||down){
   let i=nav.indexOf(document.activeElement);if(i<0)i=tab;
   goNav(Math.max(0,Math.min(nav.length-1,i+(down?1:-1))));
  }else if(right){
   if(screen==='Configurações'){const a=root.querySelector('.settings');if(a){a.focus();focus='settings'}}
   else goCard(0);
  }
 }else if(focus==='cards'){
  if(left&&chosen%4===0)goNav(tab);
  else goCard(chosen+(right?1:left?-1:down?4:-4));
 }else if(focus==='settings'){
  const bs=Array.from(root.querySelectorAll('.settings'));
  let i=bs.indexOf(document.activeElement);if(i<0)i=0;
  if(left)goNav(tab);
  else if(up||down)bs[Math.max(0,Math.min(bs.length-1,i+(down?1:-1)))].focus();
 }
 e.preventDefault();e.stopImmediatePropagation();
}
function openSettings(){liveMode=false;if(root){root.hidden=false;openTab(6)}}
function openHome(){liveMode=false;if(root){root.hidden=false;openTab(0)}}
function destroy(){
 if(interval)clearInterval(interval);
 if(keyFn)window.removeEventListener('keydown',keyFn,true);
 if(root&&root.parentNode)root.parentNode.removeChild(root);
 if(style&&style.parentNode)style.parentNode.removeChild(style);
 document.documentElement.classList.remove('hn-graphite','hn-nomotion');
}
window.CobaltNative={version:VERSION,openSettings,openHome,destroy,setStatus:updateStatus,refresh};
if(document.body&&document.head)makeShell();
else document.addEventListener('DOMContentLoaded',makeShell,{once:true});
})();
