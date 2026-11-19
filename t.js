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
  const hxAction = 'TT_PERSONAL_OPEN_HX';
  const menuHook = Symbol('tt-personal-native-menu');
  const hookedMenus = new WeakSet();
  const rememberedItems = new WeakSet();
  const returnItems = new WeakMap();
  const hxItems = new WeakMap();
  const cachedMenus = new Set();
  let menuPoll;
  let visualPolicy;
  function loadVisual() {
    if (state.visualLoading) return;
    const base = 'https://cdn.jsdelivr.net/gh/helbermoura2/cobalt-script@main/hx-ui.js';
    const url = base + '?v=' + Date.now();
    const before = window.CobaltHX && window.CobaltHX.version;
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
      if (window.CobaltHX && before) window.CobaltHX.setStatus(
        after === before ? 'Nenhuma nova versão visual detectada (v' + after + ').'
          : 'Visual atualizado para a versão ' + after + ', sem reinstalar APK.');
    };
    script.onerror = () => {
      state.visualLoading = false;
      state.visualError = 'CDN unavailable';
      if (window.CobaltHX) window.CobaltHX.setStatus('Não foi possível buscar a atualização. Versão atual preservada.');
    };
    (document.head || document.documentElement).appendChild(script);
  }
  state.refreshVisual = loadVisual;
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
  function decorateMenus(value) {
    if (!value || typeof value !== 'object') return value;
    const stack = [value], seen = new WeakSet();
    while (stack.length) {
      const node = stack.pop();
      if (!node || typeof node !== 'object' || seen.has(node)) continue;
      seen.add(node);
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
          if (window.CobaltHX && typeof window.CobaltHX.open === 'function') window.CobaltHX.open();
          else notice('Painel Cobalt HX não disponível.');
          return true;
        }
        if (isRestoreCommand(command)) return returnFromMenu(true);
        if (isHXCommand(command)) return openHX(true);
        const commands = command && command.commandExecutorCommand && command.commandExecutorCommand.commands;
        if (Array.isArray(commands) && commands.some(child => isRestoreCommand(child) || isHXCommand(child))) {
          let closed = false;
          for (const child of commands) {
            if (isRestoreCommand(child)) returnFromMenu(!closed);
            else if (isHXCommand(child)) openHX(!closed);
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
          if (value !== url && !/^https:\/\/cdn\.jsdelivr\.net\/gh\/helbermoura2\/cobalt-script@main\/hx-ui\.js\?v=\d+$/.test(value)) throw new TypeError('Unexpected script URL');
          return value;
        },
      });
      script.src = visualPolicy.createScriptURL(url);
    } else script.src = url;
    script.onload = () => {
      state.upstreamLoaded = true;
      installMenuHooks();
      menuPoll = setInterval(installMenuHooks, 500);
      loadVisual();
    };
    script.onerror = () => { state.upstreamError = true; notice('Falha ao carregar o TizenTube.', 15000); };
    (document.head || document.documentElement).appendChild(script);
  } catch (error) {
    state.upstreamError = String(error && error.message || error);
    notice('Falha ao iniciar o TizenTube.', 15000);
  }
})();



/* HX-BUNDLED-V0.2.2: TV Settings updater, reliably styled without external CSS. */
try {
  const canMountHX = !!(document.body && document.head && document.createElement &&
    typeof document.createElement('div').appendChild === 'function' &&
    document.documentElement && document.documentElement.classList);
  if (canMountHX) {
/* Cobalt HX 0.2.2 — original, lightweight TV identity (no third-party UI libraries). */
(() => {
  'use strict';
  const VERSION = '0.2.2';
  const previous = window.CobaltHX;
  if (previous && previous.version === VERSION) return;
  if (previous && typeof previous.destroy === 'function') previous.destroy();

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

  let root, style, signature, accent, launcher, overlay, panel, status, settingsEntry, settingsHost, focusedTile, focusedTileStyle, buttons = [];
  let poll, keyListener, focusListener, blurListener, retryTimer, returnFocus;
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

  // The action belongs to the TV Settings page, not to the home-screen overlay.
  // We prefer the native settings DOM; route fallback is for unknown YouTube builds.
  function settingsPage() {
    let route = '';
    try { route = String(location.hash || '') + ' ' + String(location.pathname || ''); } catch (_) {}
    const shell = document.querySelector('ytlr-settings-page, ytlr-settings-renderer, ytlr-settings-page-renderer, ytlr-settings-section-renderer, ytlr-settings-content');
    let visible = false;
    try { visible = !!(shell && (!shell.getClientRects || shell.getClientRects().length)); } catch (_) {}
    const inSettings = /settings/i.test(route) || visible;
    if (!inSettings) return null;
    return shell || document.querySelector('ytlr-app') || document.body;
  }
  function removeSettingsEntry() {
    if (settingsEntry && settingsEntry.parentNode) settingsEntry.parentNode.removeChild(settingsEntry);
    settingsEntry = null;
    settingsHost = null;
  }
  function syncSettingsEntry() {
    const host = settingsPage();
    if (!host || watchingFullscreen() || hasMini()) { removeSettingsEntry(); return; }
    if (settingsEntry && settingsEntry.parentNode === host) return;
    removeSettingsEntry();
    if (!host || typeof host.appendChild !== 'function') return;
    settingsHost = host;
    settingsEntry = element('button', 'hx-settings-entry');
    settingsEntry.type = 'button';
    settingsEntry.setAttribute('aria-label', 'Atualizar e personalizar Cobalt HX nas configurações');
    settingsEntry.appendChild(element('span', '', '↻  Atualizar Cobalt HX'));
    settingsEntry.appendChild(element('small', '', 'Versão ' + VERSION + '  ·  Aparência e atualizações'));
    Object.assign(settingsEntry.style, {
      display: 'block', boxSizing: 'border-box', width: '520px', maxWidth: '85vw', minHeight: '90px',
      padding: '14px 22px', margin: '16px 18px 24px', textAlign: 'left', cursor: 'pointer',
      background: '#172b35', color: '#eefafa', border: '2px solid #c79c69', borderRadius: '14px',
      boxShadow: '0 10px 26px rgba(0,0,0,.55)', pointerEvents: 'auto',
      fontFamily: 'Arial, sans-serif', fontSize: '22px', fontWeight: 'bold', zIndex: '2147483001'
    });
    const title = settingsEntry.children[0], subtitle = settingsEntry.children[1];
    Object.assign(title.style, { display: 'block', fontSize: '22px', lineHeight: '28px', color: '#f9f9fa' });
    Object.assign(subtitle.style, { display: 'block', paddingTop: '5px', fontSize: '15px', color: '#a8d7d5', fontWeight: 'normal' });
    if (host === document.body || String(host.tagName || '').toLowerCase() === 'ytlr-app') {
      Object.assign(settingsEntry.style, {
        position: 'fixed', top: '17%', right: '5%', margin: '0'
      });
    } else {
      Object.assign(settingsEntry.style, { position: 'relative' });
    }
    const onFocus = () => { settingsEntry.style.background = '#25606b'; settingsEntry.style.outline = '4px solid #65e2d7'; };
    const onBlur = () => { settingsEntry.style.background = '#172b35'; settingsEntry.style.outline = 'none'; };
    settingsEntry.addEventListener('focus', onFocus);
    settingsEntry.addEventListener('blur', onBlur);
    settingsEntry.addEventListener('click', open);
    // Insert first, so the action is part of Settings rather than covering home tiles.
    if (typeof host.insertBefore === 'function' && host.firstChild && host !== document.body) {
      host.insertBefore(settingsEntry, host.firstChild);
    } else host.appendChild(settingsEntry);
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
    const heading = panel.children[0], subtitle = panel.children[1], footer = panel.children[panel.children.length-1];
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
    Object.assign(tile.style, { outline: '3px solid #6be5dc', outlineOffset: '4px',
      boxShadow: '0 0 0 6px rgba(105,230,220,.24)', borderRadius: '12px' });
  }
  function updateVisibility() {
    if (!root || !overlay) return;
    const hide = watchingFullscreen() || hasMini();
    if (signature) { signature.hidden = hide; signature.style.display = hide ? 'none' : 'flex'; }
    if (launcher) { launcher.hidden = true; launcher.style.display = 'none'; }
    if (accent) { accent.hidden = hide; accent.style.display = hide ? 'none' : 'block'; }
    syncSettingsEntry();
  }
  function repaint() {
    const html = document.documentElement;
    if (!html || !html.classList) return;
    html.classList.toggle('hx-effects-on', settings.effects);
    html.classList.toggle('hx-cinema', settings.effects && settings.intensity === 'cinema');
    html.classList.toggle('hx-theme-cyan', settings.theme === 'cyan');
    if (!settings.effects) restoreTileFocus();
    if (panel) panel.style.borderColor = settings.theme === 'bronze' ? '#cfa06a' : '#6fe5dc';
    if (signature) signature.style.borderColor = settings.theme === 'bronze' ? '#c89d6c' : '#6fe5dc';
    if (buttons[1]) buttons[1].textContent = 'Efeitos visuais: ' + (settings.effects ? 'LIGADOS' : 'DESLIGADOS');
    if (buttons[2]) buttons[2].textContent = 'Paleta: ' + (settings.theme === 'bronze' ? 'BRONZE IMPERIAL' : 'TURQUESA');
    if (buttons[3]) buttons[3].textContent = 'Animações: ' + (settings.intensity === 'cinema' ? 'CINEMA' : 'LEVES');
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
    style.textContent = css;
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
    panel.appendChild(element('p', 'hx-footer', 'Atualizações visuais são remotas. Atualizações do Android exigem APK assinado compatível. OK seleciona; Voltar fecha o painel.'));
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
    poll = setInterval(updateVisibility, 1250);
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
      } else if (settingsEntry && [403, 406].includes(event.keyCode) && !watchingFullscreen() && !hasMini()) {
        event.preventDefault(); event.stopImmediatePropagation(); open();
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
    removeSettingsEntry();
    if (root && root.parentNode) root.parentNode.removeChild(root);
    if (style && style.parentNode) style.parentNode.removeChild(style);
    const html = document.documentElement;
    if (html && html.classList) {
      html.classList.remove('hx-effects-on');
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

  }
} catch (error) {
  if (window.TTBackExternal) window.TTBackExternal.visualError = String(error && error.message || error);
}
