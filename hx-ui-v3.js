/* Cobalt HX 0.4.0 — original, lightweight TV identity (no third-party UI libraries). */
(() => {
  'use strict';
  const VERSION = '0.4.0';
  const previous = window.CobaltHX;
  // A stale cached module must never replace a newer version.
  function compareVersion(a, b) {
    const x = String(a || '0').split('.').map(v => parseInt(v, 10) || 0);
    const y = String(b || '0').split('.').map(v => parseInt(v, 10) || 0);
    for (let i = 0; i < 3; i++) {
      if ((x[i] || 0) > (y[i] || 0)) return 1;
      if ((x[i] || 0) < (y[i] || 0)) return -1;
    }
    return 0;
  }
  if (previous && compareVersion(previous.version, VERSION) >= 0) return;
  if (previous && typeof previous.destroy === 'function') previous.destroy();
  try {
    const orphan = document.getElementById && document.getElementById('cobalt-hx');
    if (orphan && orphan.parentNode) orphan.parentNode.removeChild(orphan);
    const staleStyle = document.getElementById && document.getElementById('cobalt-hx-css');
    if (staleStyle && staleStyle.parentNode) staleStyle.parentNode.removeChild(staleStyle);
  } catch (_) {}

  const STORAGE = 'cobalt-hx-preferences';
  const KEY = 'cobalt-hx';
  const THEME = { bronze: 'bronze', cyan: 'cyan' };
  let settings = { effects: true, theme: 'bronze', intensity: 'cinema' };
  try {
    const saved = JSON.parse(window.localStorage.getItem(STORAGE) || '{}');
    if (saved && typeof saved === 'object') {
      settings.effects = saved.effects !== false;
      settings.theme = saved.theme === 'cyan' ? 'cyan' : 'bronze';
      settings.intensity = saved.intensity === 'leve' ? 'leve' : 'cinema';
    }
  } catch (_) { /* Optional local storage can be disabled by TV privacy settings. */ }

  const css = [
    ':root{--hx-bronze:#d6a46e;--hx-cyan:#69e6dc;--hx-glass:#08131dde;--hx-border:#d6a46e80}',
    'html.hx-theme-cyan{--hx-bronze:#6edbdc;--hx-border:#69e6dc80}',
    '#cobalt-hx{position:fixed;inset:0;z-index:2147483000;pointer-events:none;font-family:Arial,Helvetica,sans-serif;color:#f5f6f7}',
    '#cobalt-hx *{box-sizing:border-box}',
    '#cobalt-hx button{cursor:pointer;font-family:inherit;pointer-events:auto}',
    '#cobalt-hx button:focus{outline:4px solid var(--hx-cyan)!important;outline-offset:3px}',
    '#cobalt-hx .hx-signature{position:absolute;top:18px;left:52%;transform:translateX(-50%);padding:9px 20px;display:flex;gap:13px;align-items:center;white-space:nowrap;border:1px solid var(--hx-border);border-radius:0 0 15px 15px;background:linear-gradient(120deg,#080e17ec,#17232bd9);box-shadow:0 10px 28px #0008;pointer-events:none}',
    '#cobalt-hx .hx-name{font-size:22px;letter-spacing:2px;font-weight:800;color:#f4f2ed}',
    '#cobalt-hx .hx-name b{color:var(--hx-cyan)}',
    '#cobalt-hx .hx-tag{font-size:11px;letter-spacing:1.5px;font-weight:700;color:var(--hx-bronze);border-left:1px solid #b5c1c35c;padding-left:13px}',
    '#cobalt-hx .hx-accent{position:absolute;left:10px;top:19%;height:55%;width:4px;border-radius:5px;background:linear-gradient(0deg,transparent,var(--hx-bronze),var(--hx-cyan),transparent);opacity:.65}',
    '#cobalt-hx .hx-launch{position:absolute;right:34px;bottom:28px;border:2px solid var(--hx-border);border-radius:18px;background:linear-gradient(130deg,#091822,#1c3441);color:#efffff;font-size:23px;font-weight:700;padding:15px 23px;box-shadow:0 12px 38px #000b}',
    '#cobalt-hx .hx-launch strong{display:inline-block;padding:3px 7px;margin-right:12px;border-radius:7px;background:#113c41;color:var(--hx-cyan);letter-spacing:1px}',
    '#cobalt-hx .hx-launch:focus{background:#234955}',
    '#cobalt-hx .hx-overlay{position:absolute;inset:0;background:#0009;pointer-events:auto}',
    '#cobalt-hx .hx-overlay[hidden],#cobalt-hx .hx-launch[hidden],#cobalt-hx .hx-signature[hidden]{display:none!important}',
    '#cobalt-hx .hx-panel{position:absolute;left:50%;top:50%;transform:translate(-50%,-50%);width:660px;max-width:88vw;max-height:90vh;overflow:auto;padding:34px 42px 28px;border:2px solid var(--hx-border);border-radius:25px;background:linear-gradient(140deg,#09141df9,#172732fa 75%,#221d1af7);box-shadow:0 30px 100px #000e}',
    '#cobalt-hx .hx-panel:before{content:"";position:absolute;top:0;left:40px;right:40px;height:4px;background:linear-gradient(90deg,var(--hx-bronze),var(--hx-cyan))}',
    '#cobalt-hx .hx-title{font-size:38px;margin:0 0 5px;font-weight:800;letter-spacing:.8px}',
    '#cobalt-hx .hx-title span{color:var(--hx-cyan)}',
    '#cobalt-hx .hx-subtitle{font-size:19px;color:#b6c9cb;margin:0 0 22px}',
    '#cobalt-hx .hx-state{font-size:19px;color:#e6e5e1;border-left:4px solid var(--hx-bronze);padding:10px 13px;margin:0 0 14px;min-height:42px;background:#25394670}',
    '#cobalt-hx .hx-action{display:block;width:100%;text-align:left;color:#edf7f8;background:#1a303d;border:1px solid #7a959d77;border-radius:12px;font-size:23px;font-weight:650;margin-top:10px;padding:15px 19px}',
    '#cobalt-hx .hx-action.primary{background:#15484c;border-color:#74ddd4a8}',
    '#cobalt-hx .hx-action:focus{background:#28606c;transform:translateX(3px)}',
    '#cobalt-hx .hx-footer{font-size:15px;color:#9eb5b9;margin:19px 0 0;line-height:1.5}',
    'html.hx-effects-on ytlr-video-renderer:focus-within,html.hx-effects-on ytlr-grid-video-renderer:focus-within,html.hx-effects-on ytlr-rich-grid-media:focus-within,html.hx-effects-on ytlr-tile-renderer:focus-within{outline:3px solid var(--hx-cyan)!important;outline-offset:5px;border-radius:14px;box-shadow:0 0 0 8px #69e6dc26,0 16px 32px #0009!important}',
    'html.hx-effects-on ytlr-video-renderer:focus-within img,html.hx-effects-on ytlr-grid-video-renderer:focus-within img{filter:saturate(1.17) contrast(1.06);border-radius:13px}',
    'html.hx-effects-on ytlr-guide-entry-renderer:focus-within,html.hx-effects-on ytlr-guide-item-renderer:focus-within{background:#69e6dc22!important;border-left:4px solid var(--hx-bronze);border-radius:8px}',
    'html.hx-effects-on ytlr-search-input:focus-within,html.hx-effects-on [role=searchbox]:focus{box-shadow:0 0 0 2px var(--hx-bronze)!important;border-radius:20px}',
    'html.hx-effects-on ytlr-menu-service-item-renderer:focus-within,html.hx-effects-on ytlr-menu-navigation-item-renderer:focus-within{background:#69e6dc25!important;border-radius:8px}',
    'html.hx-effects-on ytlr-miniplayer-renderer,html.hx-effects-on ytlr-miniplayer{box-shadow:0 0 0 2px var(--hx-bronze),0 14px 48px #000b;border-radius:13px}',
    'html.hx-effects-on.hx-cinema ytlr-video-renderer:focus-within,html.hx-effects-on.hx-cinema ytlr-grid-video-renderer:focus-within{transform:translateY(-4px);transition:transform .2s ease-out,box-shadow .2s ease-out}',
    '@media(prefers-reduced-motion:reduce){#cobalt-hx .hx-action:focus,html.hx-effects-on.hx-cinema ytlr-video-renderer:focus-within,html.hx-effects-on.hx-cinema ytlr-grid-video-renderer:focus-within{transition:none;transform:none}}'
  ].join('\n');

  const extraCSS = "\nhtml.hx-v4 body,html.hx-v4 ytlr-app{background:radial-gradient(ellipse at 80% 0%,#29221a,#101115 52%,#090a0c)!important}\nhtml.hx-v4 ytlr-tile-renderer,html.hx-v4 ytlr-video-renderer,html.hx-v4 ytlr-grid-video-renderer,html.hx-v4 ytlr-rich-grid-media{background:linear-gradient(145deg,#222123,#121417)!important;border:1px solid #9f7b51!important;border-radius:20px!important;box-shadow:0 10px 32px #0009!important;transition:transform .19s ease-out,box-shadow .19s ease-out!important}\nhtml.hx-v4 ytlr-tile-renderer img,html.hx-v4 ytlr-video-renderer img,html.hx-v4 ytlr-grid-video-renderer img{border-radius:17px!important}\nhtml.hx-v4 ytlr-tile-renderer:focus-within,html.hx-v4 ytlr-video-renderer:focus-within,html.hx-v4 ytlr-grid-video-renderer:focus-within{border:2px solid #e2b47b!important;box-shadow:0 0 0 4px #d0a371,0 18px 46px #000c!important;transform:translateY(-4px) scale(1.015)}\nhtml.hx-v4 ytlr-guide-entry-renderer,html.hx-v4 ytlr-guide-item-renderer{border-left:3px solid #c59a62!important;background:#181819!important;border-radius:12px!important}\nhtml.hx-v4 ytlr-guide-entry-renderer:focus-within,html.hx-v4 ytlr-guide-item-renderer:focus-within{background:#58422e!important;outline:2px solid #edc38d!important}\nhtml.hx-v4 ytlr-masthead,html.hx-v4 ytlr-masthead-renderer{background:#111214!important;border-bottom:2px solid #ac8353!important}\nhtml.hx-v4 ytlr-shelf-renderer h2{color:#f0d2ac!important;letter-spacing:.4px!important}\n@media(prefers-reduced-motion:reduce){html.hx-v4 ytlr-tile-renderer,html.hx-v4 ytlr-video-renderer,html.hx-v4 ytlr-grid-video-renderer{transform:none!important;transition:none!important}}\n";
  let root, style, signature, accent, launcher, overlay, panel, status, settingsEntry, settingsHost, focusedTile, focusedTileStyle, buttons = [];
  let poll, keyListener, focusListener, blurListener, retryTimer, returnFocus, sidebar;
  const element = (tag, cls, value) => {
    const node = document.createElement(tag);
    if (cls) node.className = cls;
    if (typeof value === 'string') node.textContent = value;
    return node;
  };
  const save = () => {
    try { window.localStorage.setItem(STORAGE, JSON.stringify(settings)); } catch (_) {}
  };
  const report = message => {
    if (status) status.textContent = String(message || '');
  };
  const watchingFullscreen = () => {
    try {
      const video = document.querySelector('video');
      return !!(video && video.readyState && !video.ended && window.isPipPlaying !== true &&
        /(?:^#?\/|\/)watch(?:[?/#]|$)/.test(location.hash || ''));
    } catch (_) { return false; }
  };
  const hasMini = () => {
    try { return window.isPipPlaying === true; } catch (_) { return false; }
  };

  // Native settingActionRenderer entry is injected by t.js, not by HTML.
  function removeSettingsEntry() {
    if (settingsEntry && settingsEntry.parentNode) settingsEntry.parentNode.removeChild(settingsEntry);
    settingsEntry = null;
    settingsHost = null;
  }
  // Native YouTube may reject user CSS style tags under Content Security Policy:
  // explicit inline styles ensure the UI still has television-grade layout.
  function applyInlineLayout() {
    if (!root || !panel) return;
    Object.assign(root.style, { position: 'fixed', inset: '0', zIndex: '2147483000',
      pointerEvents: 'none', fontFamily: 'Arial, sans-serif', color: '#f6f6f7' });
    Object.assign(signature.style, { position: 'absolute', top: '15px', left: '50%',
      transform: 'translateX(-50%)', display: 'flex', flexDirection: 'row',
      gap: '15px', alignItems: 'center', whiteSpace: 'nowrap', padding: '9px 18px',
      border: '1px solid #c89d6c', borderRadius: '0 0 12px 12px',
      background: '#111d27', pointerEvents: 'none', color: '#f4f5f5' });
    Object.assign(signature.children[0].style, { fontSize: '19px', fontWeight: 'bold', letterSpacing: '2px' });
    Object.assign(signature.children[0].children[0].style, { color: '#6fe5dc' });
    Object.assign(signature.children[1].style, { fontSize: '12px', letterSpacing: '1px',
      color: '#d8ac78', borderLeft: '1px solid #718186', paddingLeft: '15px' });
    Object.assign(accent.style, { position: 'absolute', top: '20%', left: '13px', width: '3px',
      height: '49%', background: '#af895d', opacity: '.66', pointerEvents: 'none' });
    // Do not place an update control over recommended videos: it lives in Settings.
    launcher.hidden = true;
    launcher.style.display = 'none';
    Object.assign(overlay.style, { position: 'absolute', inset: '0', background: 'rgba(0,0,0,.78)',
      pointerEvents: 'auto' });
    Object.assign(panel.style, { position: 'absolute', left: '50%', top: '50%',
      transform: 'translate(-50%,-50%)', boxSizing: 'border-box',
      width: '615px', maxWidth: '85vw', maxHeight: '88vh', overflowY: 'auto',
      padding: '24px 32px 25px', border: '2px solid #cfa06a', borderRadius: '18px',
      background: '#101e2a', color: '#eef4f5', boxShadow: '0 25px 90px rgba(0,0,0,.85)',
      pointerEvents: 'auto', fontFamily: 'Arial, sans-serif', textAlign: 'left' });
    const heading = panel.children[0], subtitle = panel.children[1], footer = panel.querySelector('.hx-footer');
    Object.assign(heading.style, { display: 'block', fontSize: '31px', lineHeight: '38px',
      fontWeight: 'bold', letterSpacing: '1px', padding: '0', margin: '0 0 7px',
      color: '#fff' });
    Object.assign(heading.children[0].style, { color: '#6fe5dc' });
    Object.assign(subtitle.style, { display: 'block', fontSize: '16px', lineHeight: '21px',
      margin: '0 0 16px', padding: '0', color: '#b9ccce' });
    Object.assign(status.style, { display: 'block', fontSize: '16px', lineHeight: '23px',
      minHeight: '36px', padding: '7px 11px', margin: '0 0 14px',
      background: '#233844', color: '#f5f6f7', borderLeft: '4px solid #d0a371' });
    buttons.forEach((button, i) => {
      Object.assign(button.style, { display: 'block', boxSizing: 'border-box', width: '100%',
        minHeight: '48px', margin: '8px 0', padding: '10px 17px', textAlign: 'left',
        color: '#f4f9fa', background: i===0 ? '#15525a' : '#223a47',
        border: i===0 ? '2px solid #69ded6' : '1px solid #7d979c',
        borderRadius: '11px', fontSize: '19px', fontWeight: 'normal',
        lineHeight: '26px', cursor: 'pointer', pointerEvents: 'auto' });
      button.addEventListener('focus', () => {
        button.style.background = '#2a6973'; button.style.outline = '3px solid #74e7dc';
      });
      button.addEventListener('blur', () => {
        button.style.background = i===0 ? '#15525a' : '#223a47';
        button.style.outline = 'none';
      });
    });
    Object.assign(footer.style, { display: 'block', fontSize: '13px', lineHeight: '19px',
      color: '#9eb5b9', margin: '14px 0 0', padding: '0', maxWidth: '100%' });

    // A dedicated Settings screen rather than a floating panel over old Settings.
    Object.assign(overlay.style,{inset:'0',background:'#0a0b0e',pointerEvents:'auto'});
    Object.assign(panel.style,{left:'0',top:'0',transform:'none',width:'100vw',maxWidth:'100vw',
      height:'100vh',maxHeight:'100vh',boxSizing:'border-box',border:'none',borderRadius:'0',
      padding:'7vh 8vw 4vh 36vw',overflowY:'auto',boxShadow:'none',
      background:'radial-gradient(ellipse at 92% 8%,#2a221a,#101114 64%,#08090b)'});
    Object.assign(heading.style,{fontSize:'clamp(35px,3.7vw,60px)',lineHeight:'1.1',
      letterSpacing:'2px',margin:'0 0 12px',color:'#f5e7d5'});
    if(heading.children[0])heading.children[0].style.color='#e0b47c';
    Object.assign(subtitle.style,{fontSize:'clamp(17px,1.5vw,24px)',marginBottom:'26px',color:'#c6ad8b'});
    Object.assign(status.style,{background:'#28231e',color:'#f1dfc8',fontSize:'18px',
      borderLeft:'5px solid #d7a56a',padding:'12px 16px',marginBottom:'22px'});
    buttons.forEach((button,i)=>{
      Object.assign(button.style,{fontSize:'clamp(17px,1.5vw,24px)',minHeight:'clamp(49px,6vh,72px)',
        padding:'12px 19px',margin:'0 0 12px',borderRadius:'14px',
        background:i===0?'#d5a46b':'#242326',border:i===0?'2px solid #efc18b':'1px solid #877057',
        color:i===0?'#16110b':'#f4e9d9'});
      button.addEventListener('focus',()=>{button.style.background='#f0c58f';
        button.style.color='#17130e';button.style.outline='3px solid #f9d9a9'});
      button.addEventListener('blur',()=>{button.style.background=i===0?'#d5a46b':'#242326';
        button.style.color=i===0?'#16110b':'#f4e9d9';button.style.outline='none'});
    });
    if(footer)Object.assign(footer.style,{color:'#bda787',fontSize:'15px',lineHeight:'1.45'});
    if(sidebar)Object.assign(sidebar.style,{position:'absolute',top:'0',left:'0',bottom:'0',
      width:'29vw',boxSizing:'border-box',padding:'10vh 4vw',
      background:'linear-gradient(160deg,#2a2119,#111216 65%,#090a0c)',
      borderRight:'3px solid #cf9f63',color:'#f3d7ab',pointerEvents:'none'});
    const brand=sidebar&&sidebar.querySelector('.hx-brand');
    if(brand)Object.assign(brand.style,{fontSize:'clamp(33px,4.2vw,72px)',
      fontWeight:'900',letterSpacing:'3px',lineHeight:'1.1',color:'#eac38d'});
    const message=sidebar&&sidebar.querySelector('.hx-message');
    if(message)Object.assign(message.style,{fontSize:'clamp(17px,1.45vw,23px)',
      lineHeight:'1.65',marginTop:'44px',color:'#e3d2bb',whiteSpace:'pre-line'});
    const version=sidebar&&sidebar.querySelector('.hx-edition');
    if(version)Object.assign(version.style,{fontSize:'17px',marginTop:'38px',
      color:'#d0a46e',borderLeft:'3px solid #bb8b52',padding:'12px'});
    Object.assign(signature.style,{left:'auto',right:'32px',top:'15px',
      transform:'none',background:'#242019',border:'1px solid #c99a5e',
      borderRadius:'0 0 14px 14px',boxShadow:'0 7px 28px #000a'});
    if(signature.children[0]&&signature.children[0].children[0])
      signature.children[0].children[0].style.color='#f0bd80';
    accent.hidden=true;accent.style.display='none';

  }
  function restoreTileFocus() {
    if (focusedTile && focusedTileStyle && focusedTile.style) {
      Object.assign(focusedTile.style, focusedTileStyle);
    }
    focusedTile = null;
    focusedTileStyle = null;
  }
  function highlightTile(event) {
    if (!settings.effects) return;
    let tile;
    try { tile = event.target && event.target.closest &&
      event.target.closest('ytlr-tile-renderer, ytlr-video-renderer, ytlr-grid-video-renderer, ytlr-rich-grid-media'); }
    catch (_) { tile = null; }
    if (!tile || !tile.style || tile===focusedTile) return;
    restoreTileFocus();
    focusedTile = tile;
    focusedTileStyle = { outline: tile.style.outline, outlineOffset: tile.style.outlineOffset,
      boxShadow: tile.style.boxShadow, borderRadius: tile.style.borderRadius };
    Object.assign(tile.style, { outline: '3px solid #e3b67d', outlineOffset: '4px',
      boxShadow: '0 0 0 6px rgba(215,164,107,.25)', borderRadius: '12px' });
  }
  function updateVisibility() {
    if (!root || !overlay) return;
    const hide = watchingFullscreen() || hasMini();
    if (signature) { signature.hidden = hide; signature.style.display = hide ? 'none' : 'flex'; }
    if (launcher) { launcher.hidden = true; launcher.style.display = 'none'; }
    if (accent) { accent.hidden = true; accent.style.display = 'none'; }
    // No floating Settings DOM button: controller injects a native Settings item.
  }

  // Native YouTube TV home components. Reversible inline fallback for CSP.
  const modifiedNodes = new Map();
  function resetNativeSkin(){
    for(const [node,original] of modifiedNodes){
      try{if(original===null)node.removeAttribute('style');else node.setAttribute('style',original);}catch(_){}
    }
    modifiedNodes.clear();
  }
  function decorateNative(){
    if(!document.body||watchingFullscreen()||hasMini())return;
    if(modifiedNodes.size>450)for(const [node] of modifiedNodes)if(!node.isConnected)modifiedNodes.delete(node);
    const color=settings.theme==='bronze'?'#b28b5b':'#5dbeb8';
    function skin(selector,max,properties){
      let nodes=[];try{nodes=document.querySelectorAll(selector);}catch(_){return;}
      for(let i=0;i<Math.min(nodes.length,max);i++){
        const node=nodes[i];if(modifiedNodes.has(node)||!node.style)continue;
        try{modifiedNodes.set(node,node.getAttribute('style'));Object.assign(node.style,properties)}
        catch(_){modifiedNodes.delete(node)}
      }
    }
    skin('ytlr-tile-renderer,ytlr-grid-video-renderer,ytlr-video-renderer,ytlr-rich-grid-media',150,
      {background:'#1e1e20',border:'1px solid '+color,borderRadius:'19px',padding:'5px',
       boxShadow:'0 9px 30px rgba(0,0,0,.55)'});
    skin('ytlr-tile-renderer img,ytlr-grid-video-renderer img,ytlr-video-renderer img',130,
      {borderRadius:'16px',filter:'contrast(1.05) saturate(1.12)'});
    skin('ytlr-guide-entry-renderer,ytlr-guide-item-renderer',60,
      {background:'#18191d',borderLeft:'3px solid '+color,borderRadius:'12px'});
    skin('ytlr-masthead,ytlr-masthead-renderer',5,{background:'#111215',borderBottom:'2px solid '+color});
    skin('ytlr-shelf-renderer h2',35,{color:'#f1d2ad',fontWeight:'bold'});
  }

  function repaint() {
    resetNativeSkin();
    const html = document.documentElement;
    if (!html || !html.classList) return;
    html.classList.toggle('hx-effects-on', settings.effects);
    html.classList.add('hx-v4');
    html.classList.toggle('hx-cinema', settings.effects && settings.intensity === 'cinema');
    html.classList.toggle('hx-theme-cyan', settings.theme === 'cyan');
    if (!settings.effects) restoreTileFocus();
    if (panel) panel.style.borderColor = settings.theme === 'bronze' ? '#cfa06a' : '#6fe5dc';
    if (signature) signature.style.borderColor = settings.theme === 'bronze' ? '#c89d6c' : '#6fe5dc';
    if (buttons[1]) buttons[1].textContent = 'Efeitos visuais: ' + (settings.effects ? 'LIGADOS' : 'DESLIGADOS');
    if (buttons[2]) buttons[2].textContent = 'Paleta: ' + (settings.theme === 'bronze' ? 'BRONZE IMPERIAL' : 'TURQUESA');
    if (buttons[3]) buttons[3].textContent = 'Animações: ' + (settings.intensity === 'cinema' ? 'CINEMA' : 'LEVES');
    decorateNative();
  }
  function open() {
    if (!overlay) return;
    returnFocus = document.activeElement;
    overlay.hidden = false;
    overlay.style.display = 'block';
    updateVisibility();
    report('Cobalt HX ' + VERSION + ' • edição personalizada ativa.');
    if (buttons[0] && typeof buttons[0].focus === 'function') buttons[0].focus();
  }
  function close() {
    if (!overlay) return;
    overlay.hidden = true;
    overlay.style.display = 'none';
    updateVisibility();
    const destination = settingsEntry && settingsEntry.parentNode ? settingsEntry : returnFocus;
    if (destination && typeof destination.focus === 'function') destination.focus();
  }
  function refresh() {
    const control = window.TTBackExternal;
    if (!control || typeof control.refreshVisual !== 'function') {
      report('Atualização visual indisponível nesta instalação.');
      return;
    }
    report('Verificando a última versão do visual...');
    try { control.refreshVisual(); } catch (_) { report('Falha na verificação. Visual atual preservado.'); }
  }
  function action(text, handler, primary) {
    const button = element('button', 'hx-action' + (primary ? ' primary' : ''), text);
    button.type = 'button';
    button.addEventListener('click', handler);
    panel.appendChild(button);
    buttons.push(button);
    return button;
  }
  function mount() {
    if (!document.body || !document.head) return false;
    style = element('style');
    style.id = 'cobalt-hx-css';
    const nonceSource = document.querySelector('style[nonce],script[nonce]');
    if (nonceSource && nonceSource.nonce) style.nonce = nonceSource.nonce;
    style.textContent = css + '\n' + extraCSS;
    document.head.appendChild(style);

    root = element('div');
    root.id = KEY;
    signature = element('div', 'hx-signature');
    const name = element('span', 'hx-name', 'COBALT ');
    name.appendChild(element('b', '', 'HX'));
    signature.appendChild(name);
    signature.appendChild(element('span', 'hx-tag', 'PERSONAL TV'));
    accent = element('div', 'hx-accent');
    launcher = element('button', 'hx-launch');
    launcher.type = 'button';
    launcher.appendChild(element('strong', '', 'HX'));
    launcher.appendChild(document.createTextNode(' Atualizar e personalizar'));
    launcher.setAttribute('aria-label', 'Abrir configurações do Cobalt HX');
    launcher.addEventListener('click', open);
    overlay = element('div', 'hx-overlay');
    overlay.hidden = true;
    overlay.setAttribute('role', 'dialog');
    overlay.setAttribute('aria-label', 'Configurações Cobalt HX');
    panel = element('section', 'hx-panel');
    const title = element('h2', 'hx-title');
    title.appendChild(document.createTextNode('COBALT '));
    title.appendChild(element('span', '', 'HX'));
    panel.appendChild(title);
    panel.appendChild(element('p', 'hx-subtitle', 'Sua experiência de TV, do seu jeito • v' + VERSION));
    status = element('div', 'hx-state', 'Visual personalizado ativo');
    status.setAttribute('role', 'status');
    panel.appendChild(status);
    action('↻ Atualizar interface agora', refresh, true);
    action('', () => { settings.effects = !settings.effects; save(); repaint(); report('Efeitos atualizados.'); });
    action('', () => { settings.theme = settings.theme === 'bronze' ? 'cyan' : 'bronze'; save(); repaint(); report('Paleta aplicada.'); });
    action('', () => { settings.intensity = settings.intensity === 'cinema' ? 'leve' : 'cinema'; save(); repaint(); report('Intensidade de animação alterada.'); });
    action('Reiniciar aplicativo', () => {
      report('Recarregando a interface do Cobalt...');
      if (typeof location.reload === 'function') location.reload();
    });
    action('Fechar painel', close);
    sidebar=element('aside','hx-sidebar');
    sidebar.appendChild(element('div','hx-brand','COBALT HX'));
    sidebar.appendChild(element('div','hx-message','PERSONAL TV\n\nA sua central de entretenimento com visual Bronze Imperial e navegação por controle remoto.'));
    sidebar.appendChild(element('div','hx-edition','EDIÇÃO 0.4.0 | ANDROID TV'));
    panel.appendChild(sidebar);
    panel.appendChild(element('p', 'hx-footer', 'Acesse este painel em Configurações → Cobalt HX. Atualizações visuais são remotas; atualizações do Android exigem APK compatível. OK seleciona; Voltar fecha o painel.'));
    overlay.appendChild(panel);
    root.appendChild(signature);
    root.appendChild(accent);
    root.appendChild(launcher);
    root.appendChild(overlay);
    document.body.appendChild(root);

    // Critical fallbacks when the YouTube TV Content Security Policy blocks style sheets.
    Object.assign(root.style, { position: 'fixed', inset: '0', zIndex: '2147483000', pointerEvents: 'none' });
    Object.assign(launcher.style, { position: 'absolute', right: '34px', bottom: '28px', padding: '15px 23px',
      fontSize: '23px', color: 'white', background: '#102e3b', border: '2px solid #69e6dc', borderRadius: '18px',
      pointerEvents: 'auto' });
    Object.assign(signature.style, { position: 'absolute', top: '18px', left: '52%', color: 'white',
      padding: '10px 20px', background: '#101d27', border: '1px solid #d6a46e' });
    Object.assign(overlay.style, { position: 'absolute', inset: '0', background: '#000a', pointerEvents: 'auto' });
    // Additional explicit typography/spacing overrides for YouTube TV CSP.
    applyInlineLayout();
    // HTML hidden must be respected even if CSS is blocked.
    overlay.style.display = 'none';

    repaint();
    updateVisibility();
    decorateNative();
    poll = setInterval(() => { updateVisibility(); decorateNative(); }, 2300);
    focusListener = event => highlightTile(event);
    blurListener = event => {
      if (focusedTile && focusedTile.contains && !focusedTile.contains(event.relatedTarget)) restoreTileFocus();
    };
    document.addEventListener('focusin', focusListener, true);
    document.addEventListener('focusout', blurListener, true);
    keyListener = event => {
      if (event.type !== 'keydown') return;
      const back = [4, 27, 166, 461, 10009].includes(event.keyCode) ||
        ['Escape', 'BrowserBack', 'GoBack'].includes(event.key);
      if (!overlay.hidden) {
        if (back) { event.preventDefault(); event.stopImmediatePropagation(); close(); return; }
        if ([19, 20, 38, 40].includes(event.keyCode) || ['ArrowUp','ArrowDown'].includes(event.key)) {
          event.preventDefault(); event.stopImmediatePropagation();
          let i = buttons.indexOf(document.activeElement);
          if (i < 0) i = 0;
          const step = event.keyCode === 19 || event.keyCode === 38 || event.key === 'ArrowUp' ? -1 : 1;
          i = (i + step + buttons.length) % buttons.length;
          buttons[i].focus();
          return;
        }
      }
    };
    window.addEventListener('keydown', keyListener, true);
    return true;
  }
  function destroy() {
    if (poll) clearInterval(poll);
    if (retryTimer) clearTimeout(retryTimer);
    if (keyListener) window.removeEventListener('keydown', keyListener, true);
    if (focusListener && typeof document.removeEventListener === 'function') document.removeEventListener('focusin', focusListener, true);
    if (blurListener && typeof document.removeEventListener === 'function') document.removeEventListener('focusout', blurListener, true);
    restoreTileFocus();
    resetNativeSkin();
    removeSettingsEntry();
    if (root && root.parentNode) root.parentNode.removeChild(root);
    if (style && style.parentNode) style.parentNode.removeChild(style);
    const html = document.documentElement;
    if (html && html.classList) {
      html.classList.remove('hx-effects-on');
      html.classList.remove('hx-v4');
      html.classList.remove('hx-cinema');
      html.classList.remove('hx-theme-cyan');
    }
  }
  const api = window.CobaltHX = { version: VERSION, open, close, destroy, isOpen: () => !!(overlay && !overlay.hidden), setStatus: report, refresh,
    get preferences() { return { effects: settings.effects, theme: settings.theme, intensity: settings.intensity }; } };
  if (!mount()) {
    document.addEventListener('DOMContentLoaded', () => { if (window.CobaltHX === api) mount(); }, { once: true });
  }
})();
