/* Personal Cobalt controller. Loads the unchanged official TizenTube userscript.
 * No account data or diagnostics are transmitted by this controller.
 */
(() => {
  if (window.TTBackExternal) return;
  const state = window.TTBackExternal = { version: 4, revision: 11, parseCalls: 0, commandCalls: 0, menusSeen: 0, menusHooked: 0, upstreamLoaded: false, lastBack: null };
  const vendor = 'https://cdn.jsdelivr.net/npm/@foxreis/tizentube/dist/userScript.js';
  let backPress = null;
  let replayingBack = false;
  const backHoldMs = 900;
  let badge;
  let badgeTimer;
  function notice(text, milliseconds = 5000) {
    if (!badge) {
      badge = document.createElement('div');
      badge.id = 'tt-personal-back-status';
      badge.setAttribute('role', 'status');
      Object.assign(badge.style, { position: 'fixed', top: '24px', right: '24px',
        zIndex: '2147483647', color: '#fff', background: '#202124', padding: '16px',
        borderRadius: '8px', font: '24px sans-serif', maxWidth: '80vw', pointerEvents: 'none' });
      (document.body || document.documentElement).appendChild(badge);
    }
    clearTimeout(badgeTimer);
    badge.textContent = text;
    badge.style.display = 'block';
    badgeTimer = setTimeout(() => { badge.style.display = 'none'; }, milliseconds);
  }
  function router() {
    return Object.values(window._yttv || {}).map(x => x && x.instance)
      .find(x => x && typeof x.resolveCommand === 'function');
  }
  function watchPage(player) {
    if (/(?:^#?\/|\/)watch(?:[?/#]|$)/.test(location.hash)) return true;
    return player && ['ytLrPlayerEnabled', 'ExC9Fb'].some(name => player.classList.contains(name));
  }
  function visibleDialog(player) {
    return [...document.querySelectorAll('[role="dialog"],ytlr-overlay-section-renderer')]
      .some(node => node !== player && !node.contains(player) && node.getClientRects().length);
  }
  function restoreInfo() {
    if (window.isPipPlaying !== true || document.hidden) return null;
    const video = document.querySelector('video');
    const client = router();
    if (!video || !video.readyState || video.ended || video.isConnected === false || !Number.isFinite(video.currentTime) || !client) return null;
    for (const registry of Object.values(window._yttv || {})) {
      const mappings = registry && registry.mappings;
      const locator = registry && typeof registry.get === 'function' ? registry : mappings;
      if (!locator || typeof locator.get !== 'function') continue;
      try {
        const service = locator.get('PlayerService');
        const config = service && service.loadedPlaybackConfig;
        if (config && config.watchEndpoint && config.watchEndpoint.videoId) return { video, client, config };
      } catch (_) { /* Another registry may contain the service. */ }
    }
    return null;
  }
  function restoreVideo(expectedVideo) {
    const info = restoreInfo();
    if (!info || info.video !== expectedVideo) return false;
    const { video, client, config } = info;
    const player = document.querySelector('ytlr-player');
    const previousBackground = player && player.style.backgroundColor;
    if (player) player.style.backgroundColor = 'rgb(0, 0, 0)';
    try {
      const result = client.resolveCommand({ clickTrackingParams: config.clickTrackingParams,
        commandMetadata: config.commandMetadata,
        watchEndpoint: { ...config.watchEndpoint, startTimeSeconds: Math.floor(video.currentTime) } });
      if (result === false) throw new Error('Fullscreen command rejected');
      window.isPipPlaying = false;
      return true;
    } catch (error) {
      if (player) player.style.backgroundColor = previousBackground;
      state.restoreError = String(error && error.message || error);
      return false;
    }
  }
  const restoreAction = 'TT_PERSONAL_RESTORE_MP';
  const hxSettingsAction = 'TT_PERSONAL_OPEN_HX_SETTINGS';
  const hxSettingsCategoryId = 'cobalt_hx_settings_category';
  const hxSettingsItemId = 'cobalt_hx_settings_open';
  state.settingsSeen = 0;
  state.settingsInjected = 0;
  state.settingsSelections = 0;
  const hxAction = 'TT_PERSONAL_OPEN_HX';
  const menuHook = Symbol('tt-personal-native-menu');
  const hookedMenus = new WeakSet();
  const rememberedItems = new WeakSet();
  const returnItems = new WeakMap();
  const hxItems = new WeakMap();
  const cachedMenus = new Set();
  let menuPoll;
  let visualPolicy;
  // Immutable recovery for 0.4.1: branch refs on CDNs may still serve 0.3.1.
  // Once recovery is loaded, the Settings button checks the normal moving channel.
  const minimumVisual = '0.4.1';
  function compareVisual(a, b) {
    const x = String(a || '0').split('.').map(v => parseInt(v,10) || 0);
    const y = String(b || '0').split('.').map(v => parseInt(v,10) || 0);
    for (let i=0;i<3;i++) { if ((x[i]||0)!==(y[i]||0)) return (x[i]||0)>(y[i]||0)?1:-1; }
    return 0;
  }
  function loadVisual() {
    if (state.visualLoading) return;
    const before = window.CobaltHX && window.CobaltHX.version;
    const recovery = compareVisual(before, minimumVisual) < 0;
    const base = recovery
      ? 'https://cdn.jsdelivr.net/gh/helbermoura2/cobalt-script@8d678d145f2fc98dfc256822b7864ca63680e08f/hx-ui-v3.js'
      : 'https://cdn.jsdelivr.net/gh/helbermoura2/cobalt-script@main/hx-ui-v3.js';
    const url = base + '?v=' + Date.now();
    const script = document.createElement('script');
    const nonceSource = document.querySelector('script[nonce]');
    if (nonceSource && nonceSource.nonce) script.nonce = nonceSource.nonce;
    try {
      script.src = visualPolicy ? visualPolicy.createScriptURL(url) : url;
    } catch (error) {
      state.visualError = String(error && error.message || error);
      if (window.CobaltHX) window.CobaltHX.setStatus('Atualização bloqueada pela política da página.');
      return;
    }
    state.visualLoading = true;
    script.onload = () => {
      state.visualLoading = false;
      const after = window.CobaltHX && window.CobaltHX.version;
      if (window.CobaltHX && typeof window.CobaltHX.setStatus === 'function') {
        if (compareVisual(after, minimumVisual)<0) window.CobaltHX.setStatus(
          'Versão antiga detectada. O CDN ainda pode estar entregando a interface anterior.');
        else if (after === before) window.CobaltHX.setStatus(
          'Interface ' + after + ' carregada. Sem nova versão disponível.');
        else window.CobaltHX.setStatus(
          'Interface atualizada para ' + after + '. Reinicie para confirmar a persistência.');
      }
    };
    script.onerror = () => {
      state.visualLoading = false;
      state.visualError = 'CDN unavailable';
      if (window.CobaltHX) window.CobaltHX.setStatus('Não foi possível buscar a atualização. Versão atual preservada.');
    };
    (document.head || document.documentElement).appendChild(script);
  }
  state.refreshVisual = loadVisual;

  function refreshNativeUI(){
    if(state.nativeLoading)return;
    state.nativeLoading=true;
    const before=window.CobaltNative&&window.CobaltNative.version;
    const url='https://cdn.jsdelivr.net/gh/helbermoura2/cobalt-script@main/native-home.js?v='+Date.now();
    const script=document.createElement('script');
    const nonceSource=document.querySelector('script[nonce]');
    if(nonceSource&&nonceSource.nonce)script.nonce=nonceSource.nonce;
    try{script.src=visualPolicy?visualPolicy.createScriptURL(url):url}
    catch(error){
      state.nativeLoading=false;
      if(window.CobaltNative)window.CobaltNative.setStatus('Política da TV bloqueou a consulta de atualização.');
      return;
    }
    script.onload=()=>{
      state.nativeLoading=false;
      const after=window.CobaltNative&&window.CobaltNative.version;
      if(window.CobaltNative)window.CobaltNative.setStatus(after && after!==before
        ?'Interface atualizada para v'+after+'. Sem reinstalar APK.'
        :'Nenhuma nova versão visual detectada (v'+(after||'indisponível')+').');
      script.remove();
    };
    script.onerror=()=>{
      state.nativeLoading=false;
      if(window.CobaltNative)window.CobaltNative.setStatus('Servidor indisponível. Interface atual preservada.');
      script.remove();
    };
    (document.head||document.documentElement).appendChild(script);
  }
  state.refreshNativeUI=refreshNativeUI;

  function isRestoreCommand(command) {
    const action = command && (command.customAction
      || command.playlistEditEndpoint && command.playlistEditEndpoint.customAction
      || command.signalAction && command.signalAction.customAction);
    return !!action && action.action === restoreAction;
  }
  function isRestoreItem(item) {
    const endpoint = item && item.menuServiceItemRenderer && item.menuServiceItemRenderer.serviceEndpoint;
    const commands = endpoint && endpoint.commandExecutorCommand && endpoint.commandExecutorCommand.commands;
    return isRestoreCommand(endpoint) || Array.isArray(commands) && commands.some(isRestoreCommand);
  }
  function isHXCommand(command) {
    const action = command && (command.customAction
      || command.playlistEditEndpoint && command.playlistEditEndpoint.customAction
      || command.signalAction && command.signalAction.customAction);
    return !!action && action.action === hxAction;
  }
  function isHXItem(item) {
    const endpoint = item && item.menuServiceItemRenderer && item.menuServiceItemRenderer.serviceEndpoint;
    const commands = endpoint && endpoint.commandExecutorCommand && endpoint.commandExecutorCommand.commands;
    return isHXCommand(endpoint) || Array.isArray(commands) && commands.some(isHXCommand);
  }
  function hxMenuItem() {
    return { menuServiceItemRenderer: {
      text: { runs: [{ text: 'Atualizar / personalizar Cobalt HX' }] },
      serviceEndpoint: { clickTrackingParams: null,
        playlistEditEndpoint: { customAction: { action: hxAction } },
      }, trackingParams: null,
    } };
  }
  function withHX(items) {
    if (!Array.isArray(items)) return items;
    if (!window.CobaltHX || typeof window.CobaltHX.open !== 'function') {
      if (!items.some(isHXItem)) return items;
      if (!Object.isExtensible(items) || Object.isSealed(items)) return items.filter(item => !isHXItem(item));
      for (let i = items.length - 1; i >= 0; i--) if (isHXItem(items[i])) items.splice(i, 1);
      return items;
    }
    let own = hxItems.get(items);
    if (!own) { own = hxMenuItem(); hxItems.set(items, own); }
    if (!Object.isExtensible(items) || Object.isSealed(items)) {
      const native = items.filter(item => !isHXItem(item));
      return native[0] && isRestoreItem(native[0]) ? [native[0], own, ...native.slice(1)] : [own, ...native];
    }
    for (let i = items.length - 1; i >= 0; i--) if (isHXItem(items[i])) items.splice(i, 1);
    items.splice(items[0] && isRestoreItem(items[0]) ? 1 : 0, 0, own);
    return items;
  }
  function restoreMenuItem() {
    return { menuServiceItemRenderer: {
      text: { runs: [{ text: 'Voltar ao vídeo em reprodução' }] },
      // Use the same endpoint type as the visible native Add to Queue item.
      serviceEndpoint: { clickTrackingParams: null,
        playlistEditEndpoint: { customAction: { action: restoreAction } },
      }, trackingParams: null,
    } };
  }
  function rememberItems(items) {
    if (!Array.isArray(items) || rememberedItems.has(items)) return;
    rememberedItems.add(items);
    const weak = typeof WeakRef === 'function';
    // Prefer weak references: native menus may outlive their source renderer.
    // Bound the fallback on older engines without retaining an entire browse history.
    const limit = weak ? 1000 : 128;
    if (cachedMenus.size >= limit) cachedMenus.delete(cachedMenus.values().next().value);
    cachedMenus.add(weak ? new WeakRef(items) : { deref: () => items });
  }
  function currentItems(items, show) {
    if (!Array.isArray(items)) return items;
    let own = returnItems.get(items);
    if (!own) { own = restoreMenuItem(); returnItems.set(items, own); }
    if (!Object.isExtensible(items) || Object.isSealed(items)
      || !Object.getOwnPropertyDescriptor(items, 'length').writable) {
      const native = items.filter(item => !isRestoreItem(item));
      return withHX(show ? [own, ...native] : native);
    }
    const positions = [];
    for (let i = 0; i < items.length; i++) if (isRestoreItem(items[i])) positions.push(i);
    if (show && positions.length === 1 && positions[0] === 0) return withHX(items);
    for (let i = positions.length - 1; i >= 0; i--) items.splice(positions[i], 1);
    if (show) items.unshift(own);
    return withHX(items);
  }
  function refreshCachedMenus() {
    const show = !!restoreInfo();
    for (const reference of cachedMenus) {
      const items = reference.deref();
      if (!items) { cachedMenus.delete(reference); continue; }
      try { currentItems(items, show); }
      catch (error) { state.menuError = String(error && error.message || error); }
    }
  }
  function attachMenu(menu) {
    const renderer = menu && menu.menu && menu.menu.menuRenderer;
    if (!renderer || !Array.isArray(renderer.items)) return;
    state.menusSeen++;
    // Some Cobalt tiles omit contentId, while their Play endpoint has the ID.
    const videoMenu = menu.contentId || renderer.items.some(item => {
      const link = item && item.menuNavigationItemRenderer;
      return link && link.navigationEndpoint && link.navigationEndpoint.watchEndpoint
        && link.navigationEndpoint.watchEndpoint.videoId;
    });
    if (!videoMenu) return;
    if (hookedMenus.has(renderer)) return;
    let items = renderer.items;
    rememberItems(items);
    try {
      Object.defineProperty(renderer, 'items', { enumerable: true, configurable: true,
        get() { return currentItems(items, !!restoreInfo()); },
        set(value) { items = value; rememberItems(value); } });
      hookedMenus.add(renderer);
      state.menusHooked++;
    } catch (_) {
      // Preserve non-configurable native data and add only when it is mutable.
      try { currentItems(items, !!restoreInfo()); } catch (_) {}
    }
  }
  function isHXSettingsCommand(command) {
    const action = command && (command.customAction
      || command.playlistEditEndpoint && command.playlistEditEndpoint.customAction
      || command.signalAction && command.signalAction.customAction);
    return !!action && action.action === hxSettingsAction;
  }
  function hxSettingsCategory() {
    // Match TizenTube's official SettingsCategory + SettingActionRenderer shape.
    return { settingCategoryCollectionRenderer: {
      categoryId: hxSettingsCategoryId,
      title: { runs: [{ text: 'Cobalt HX' }] },
      focused: false,
      trackingParams: null,
      items: [{ settingActionRenderer: {
        itemId: hxSettingsItemId,
        title: { runs: [{ text: 'Configurações HX Native' }] },
        actionLabel: { runs: [{ text: 'Configurações HX Native' }] },
        summary: { runs: [{ text: 'Reprodução Cobalt • interface HX Native v' + (window.CobaltNative && window.CobaltNative.version || '0.1.0') }] },
        thumbnail: { thumbnails: [{ url: 'https://www.gstatic.com/ytlr/img/parent_code.png' }] },
        trackingParams: null,
        serviceEndpoint: { customAction: { action: hxSettingsAction, parameters: [] } },
      } }],
    } };
  }
  function attachHXSettings(value) {
    if (!value || typeof value !== 'object' || !Array.isArray(value.items)
      || !value.title || !Array.isArray(value.title.runs)) return;
    // Do not decorate unrelated shelves or video menus. Only real native Settings categories.
    if (!value.items.some(item => item && item.settingCategoryCollectionRenderer
      && Array.isArray(item.settingCategoryCollectionRenderer.items))) return;
    state.settingsSeen++;
    const found = value.items.some(item => item && item.settingCategoryCollectionRenderer
      && item.settingCategoryCollectionRenderer.categoryId === hxSettingsCategoryId);
    if (found) return;
    if (!Object.isExtensible(value.items) || Object.isFrozen(value.items)) return;
    value.items.unshift(hxSettingsCategory());
    state.settingsInjected++;
  }
  function decorateMenus(value) {
    if (!value || typeof value !== 'object') return value;
    const stack = [value], seen = new WeakSet();
    while (stack.length) {
      const node = stack.pop();
      if (!node || typeof node !== 'object' || seen.has(node)) continue;
      seen.add(node);
      attachHXSettings(node);
      if (node.showMenuCommand) attachMenu(node.showMenuCommand);
      if (node.menu && node.menu.menuRenderer) attachMenu(node);
      for (const key of Object.keys(node)) {
        const child = node[key];
        if (child && typeof child === 'object') stack.push(child);
      }
    }
    return value;
  }
  function decorateSafely(value) {
    try { return decorateMenus(value); }
    catch (error) { state.menuError = String(error && error.message || error); return value; }
  }
  function wrapParser(owner) {
    if (!owner || typeof owner.parse !== 'function' || owner.parse[menuHook]) return;
    const original = owner.parse;
    const wrapped = function () { state.parseCalls++; return decorateSafely(Reflect.apply(original, this, arguments)); };
    Object.defineProperty(wrapped, menuHook, { value: true });
    owner.parse = wrapped;
  }
  function installMenuHooks() {
    wrapParser(JSON);
    for (const entry of Object.values(window._yttv || {})) {
      if (!entry) continue;
      if (entry.JSON) wrapParser(entry.JSON);
      const client = entry.instance;
      if (!client || typeof client.resolveCommand !== 'function' || client.resolveCommand[menuHook]) continue;
      const original = client.resolveCommand;
      const wrapped = function (command) {
        state.commandCalls++;
        function returnFromMenu(closeMenu) {
          if (closeMenu) client.resolveCommand({ signalAction: { signal: 'POPUP_BACK' } });
          const info = restoreInfo();
          if (!info || !restoreVideo(info.video)) notice('Não foi possível voltar ao vídeo.');
          return true;
        }
        function openHX(closeMenu) {
          if (closeMenu) client.resolveCommand({ signalAction: { signal: 'POPUP_BACK' } });
          if (window.CobaltNative && typeof window.CobaltNative.openSettings === 'function') window.CobaltNative.openSettings();
          else if (window.CobaltHX && typeof window.CobaltHX.open === 'function') window.CobaltHX.open();
          else notice('Configurações HX Native não disponíveis.');
          return true;
        }
        if (isRestoreCommand(command)) return returnFromMenu(true);
        if (isHXSettingsCommand(command)) { state.settingsSelections++; return openHX(false); }
        if (isHXCommand(command)) return openHX(true);
        const commands = command && command.commandExecutorCommand && command.commandExecutorCommand.commands;
        if (Array.isArray(commands) && commands.some(child => isRestoreCommand(child) || isHXCommand(child) || isHXSettingsCommand(child))) {
          let closed = false;
          for (const child of commands) {
            if (isRestoreCommand(child)) returnFromMenu(!closed);
            else if (isHXCommand(child)) openHX(!closed);
            else if (isHXSettingsCommand(child)) { state.settingsSelections++; openHX(false); }
            else client.resolveCommand(child);
            if (child && child.signalAction && child.signalAction.signal === 'POPUP_BACK') closed = true;
          }
          return true;
        }
        decorateSafely(command);
        return Reflect.apply(original, this, arguments);
      };
      Object.defineProperty(wrapped, menuHook, { value: true });
      client.resolveCommand = wrapped;
    }
    decorateSafely(window.ytInitialData);
    refreshCachedMenus();
  }
  function canMinimize() {
    const player = document.querySelector('ytlr-player');
    const video = document.querySelector('video');
    return window.isPipPlaying === false && video && video.readyState && !video.ended
      && !document.hidden
      && !document.querySelector('input:focus,textarea:focus,[contenteditable="true"]:focus')
      && watchPage(player) && !visibleDialog(player) && !!router();
  }
  function cancelBackPress() {
    if (backPress) clearTimeout(backPress.timer);
    backPress = null;
  }
  function minimizeHeldBack(press) {
    if (backPress !== press || press.attempted) return;
    press.attempted = true;
    if (!canMinimize()) return;
    try {
      press.minimized = !!router().resolveCommand({ customAction: { action: 'ENTER_MP' } });
      if (press.minimized) state.lastBack.gesture = 'long';
    } catch (error) {
      state.lastBack.error = String(error && error.message || error);
      notice('Voltar: não foi possível abrir o miniplayer.');
    }
  }
  function replayShortBack(press) {
    // TizenTube itself uses createEvent/initEvent for its Escape action.
    // Replay on release so the native app decides how to dismiss suggestions,
    // controls and navigation. Do not substitute a history command here.
    const target = press.target && press.target.isConnected !== false
      && typeof press.target.dispatchEvent === 'function' ? press.target : document;
    replayingBack = true;
    try {
      for (const type of press.keypress ? ['keydown', 'keypress', 'keyup'] : ['keydown', 'keyup']) {
        const event = document.createEvent('Event');
        event.initEvent(type, true, true);
        for (const [name, value] of Object.entries({ keyCode: press.keyCode, which: press.keyCode,
          key: press.key, code: press.code, repeat: false })) {
          Object.defineProperty(event, name, { configurable: true, value });
        }
        target.dispatchEvent(event);
      }
      state.lastBack.gesture = 'short';
    } catch (error) {
      state.lastBack.error = String(error && error.message || error);
      notice('Não foi possível executar Voltar.');
    } finally { replayingBack = false; }
  }
  function back(event) {
    if (replayingBack) return;
    if (window.CobaltNative && typeof window.CobaltNative.isOpen === 'function' && window.CobaltNative.isOpen()) return;
    if (window.CobaltHX && typeof window.CobaltHX.isOpen === 'function' && window.CobaltHX.isOpen()) return;
    if (![4, 27, 166, 461, 10009].includes(event.keyCode)
      && !['Escape', 'BrowserBack', 'GoBack'].includes(event.key)) return;
    const stop = () => { event.preventDefault(); event.stopImmediatePropagation(); };
    if (backPress) {
      stop();
      if (event.type === 'keypress') backPress.keypress = true;
      if (event.type === 'keyup') {
        const press = backPress;
        clearTimeout(press.timer);
        if (Date.now() - press.started >= backHoldMs) minimizeHeldBack(press);
        backPress = null;
        if (!press.minimized) replayShortBack(press);
      }
      return;
    }
    if (event.type !== 'keydown' || event.repeat || !canMinimize()) return;
    const player = document.querySelector('ytlr-player');
    const video = document.querySelector('video');
    state.lastBack = { keyCode: event.keyCode, key: event.key,
      playerClass: player && player.className, hash: location.hash,
      pip: window.isPipPlaying, videoReady: video && video.readyState };
    const press = backPress = { started: Date.now(), keyCode: event.keyCode,
      key: event.key, code: event.code, target: event.target, keypress: false,
      attempted: false, minimized: false };
    press.timer = setTimeout(() => minimizeHeldBack(press), backHoldMs);
    stop();
  }
  addEventListener('blur', cancelBackPress, true);
  document.addEventListener('visibilitychange', () => { if (document.hidden) cancelBackPress(); }, true);
  for (const type of ['keydown', 'keypress', 'keyup']) addEventListener(type, back, true);
  // Refresh cached native arrays before the app opens the menu; never consume OK.
  addEventListener('keydown', event => {
    if (event.keyCode === 13 || event.keyCode === 23 || event.key === 'Enter') refreshCachedMenus();
  }, true);
  try {
    const url = vendor + '?v=' + Date.now();
    const script = document.createElement('script');
    const nonceSource = document.querySelector('script[nonce]');
    if (nonceSource && nonceSource.nonce) script.nonce = nonceSource.nonce;
    if (window.trustedTypes) {
      visualPolicy = window.trustedTypes.createPolicy('tt-personal-upstream-v4', {
        createScriptURL(value) {
          if (value !== url && !/^https:\/\/cdn\.jsdelivr\.net\/gh\/helbermoura2\/cobalt-script@(?:main|[a-f0-9]{40})\/(?:hx-ui-v3|native-home)\.js\?v=\d+$/.test(value)) throw new TypeError('Unexpected script URL');
          return value;
        },
      });
      script.src = visualPolicy.createScriptURL(url);
    } else script.src = url;
    script.onload = () => {
      state.upstreamLoaded = true;
      installMenuHooks();
      menuPoll = setInterval(installMenuHooks, 500);
      // Recover older cached APK visual modules automatically after startup.
      // Version comparison in the UI prevents downgrades from stale CDN scripts.
      setTimeout(() => {
        if (!state.visualLoading && compareVisual(window.CobaltHX && window.CobaltHX.version, minimumVisual) < 0) {
          loadVisual();
        }
      }, 1800);
    };
    script.onerror = () => { state.upstreamError = true; notice('Falha ao carregar o TizenTube.', 15000); };
    (document.head || document.documentElement).appendChild(script);
  } catch (error) {
    state.upstreamError = String(error && error.message || error);
    notice('Falha ao iniciar o TizenTube.', 15000);
  }
})();





/* HX Native v0.1.0: TV catalogue. Cobalt playback unchanged. */
try {
  const canMountHX = !!(document && document.createElement && document.documentElement);
  if(canMountHX) {
/* Cobalt HX Native Preview 0.1.0. Original TV UI; Cobalt playback and blocking unchanged.
Design: Taste Skill hierarchy, Transitions.dev short reversible motion, React Bits focus cues;
plain CSS, no borrowed commercial assets, no autoplay previews. */
(() => {
'use strict';
const VERSION='0.1.0';
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
 left.appendChild(create('div','logo','COBALT HX'));left.appendChild(create('div','caption','NATIVE EXPERIENCE • PRÉVIA'));
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
 const group=create('div');group.appendChild(create('div','eyebrow','SUA TV. SEU JEITO.'));group.appendChild(create('div','heading','Destaques'));
 head.appendChild(group);head.appendChild(create('div','pill','TESTE 0.1.0'));
 main.appendChild(head);main.appendChild(create('div','desc','Sem prévias automáticas. OK abre o vídeo selecionado.'));
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
 if(!data.length){grid.appendChild(create('p','','Carregando vídeos do YouTube TV...'));return}
 data.forEach(c=>{
  const card=create('button','card');card.type='button';
  Object.assign(card.style,{textAlign:'left',minWidth:'0',padding:'0 0 10px',background:'#1d1d20',border:'1px solid #986f43',borderRadius:'13px',overflow:'hidden'});
  const poster=create('div','poster');Object.assign(poster.style,{width:'100%',aspectRatio:'16/9',position:'relative',background:'linear-gradient(145deg,#352e26,#181a20)'});
  if(c.poster){
   const image=create('img');image.src=c.poster;image.alt='';Object.assign(image.style,{width:'100%',height:'100%',objectFit:'cover',display:'block'});
   image.addEventListener('error',()=>{image.remove();poster.appendChild(create('span','fallback','COBALT HX'))});
   poster.appendChild(image);
  }else poster.appendChild(create('span','fallback','COBALT HX'));
  card.appendChild(poster);
  const t=create('div','ctitle',c.title);Object.assign(t.style,{padding:'12px 10px 0',fontWeight:'bold',fontSize:'17px',minHeight:'56px',lineHeight:'1.3'});
  card.appendChild(t);card.appendChild(create('div','meta','OK • Reproduzir'));
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
  Object.assign(b.style,{display:'block',width:'100%',padding:'16px 22px',margin:'12px 0',textAlign:'left',fontSize:'21px',border:'1px solid #a17a4c',background:'#262327',borderRadius:'13px'});
  b.addEventListener('focus',()=>{b.style.background='#795334'});b.addEventListener('blur',()=>{b.style.background='#262327'});
  b.addEventListener('click',handler);p.appendChild(b);
 });
 p.appendChild(create('div','state','AdBlock, SponsorBlock, 4K, miniplayer e OK longo continuam sob responsabilidade do Cobalt. O botão atualiza a interface; APKs futuros exigem instalador Android e assinatura de atualização fixa.'));
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


  }
} catch (error) {
  if (window.TTBackExternal) window.TTBackExternal.visualError = String(error && error.message || error);
}
