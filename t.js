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
  const menuHook = Symbol('tt-personal-native-menu');
  const hookedMenus = new WeakSet();
  const rememberedItems = new WeakSet();
  const returnItems = new WeakMap();
  const cachedMenus = new Set();
  let menuPoll;
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
      return show ? [own, ...native] : native;
    }
    const positions = [];
    for (let i = 0; i < items.length; i++) if (isRestoreItem(items[i])) positions.push(i);
    if (show && positions.length === 1 && positions[0] === 0) return items;
    for (let i = positions.length - 1; i >= 0; i--) items.splice(positions[i], 1);
    if (show) items.unshift(own);
    return items;
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
        if (isRestoreCommand(command)) return returnFromMenu(true);
        const commands = command && command.commandExecutorCommand && command.commandExecutorCommand.commands;
        if (Array.isArray(commands) && commands.some(isRestoreCommand)) {
          let closed = false;
          for (const child of commands) {
            if (isRestoreCommand(child)) returnFromMenu(!closed);
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
      const policy = window.trustedTypes.createPolicy('tt-personal-upstream-v4', {
        createScriptURL(value) {
          if (value !== url) throw new TypeError('Unexpected upstream URL');
          return value;
        },
      });
      script.src = policy.createScriptURL(url);
    } else script.src = url;
    script.onload = () => {
      state.upstreamLoaded = true;
      installMenuHooks();
      menuPoll = setInterval(installMenuHooks, 500);
    };
    script.onerror = () => { state.upstreamError = true; notice('Falha ao carregar o TizenTube.', 15000); };
    (document.head || document.documentElement).appendChild(script);
  } catch (error) {
    state.upstreamError = String(error && error.message || error);
    notice('Falha ao iniciar o TizenTube.', 15000);
  }
})();
