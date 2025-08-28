(() => {
  if (window.jigglContentInjected) return;
  window.jigglContentInjected = true;

  const addListener = (handler) => {
    if (typeof browser !== 'undefined' && browser.runtime && browser.runtime.onMessage) {
      browser.runtime.onMessage.addListener(handler);
    } else if (typeof chrome !== 'undefined' && chrome.runtime && chrome.runtime.onMessage) {
      chrome.runtime.onMessage.addListener((msg, sender, sendResponse) => {
        const p = handler(msg, sender);
        if (p && typeof p.then === 'function') {
          p.then(sendResponse).catch(err => sendResponse({ error: String(err) }));
          return true;
        } else {
          sendResponse(p);
        }
      });
    }
  };

  addListener(async (msg) => {
    if (!msg || msg.type !== 'jiggl/logWork') return;
    const { issue, body } = msg;
    try {
      const resp = await fetch(`/rest/api/latest/issue/${encodeURIComponent(issue)}/worklog`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        credentials: 'include',
        body
      });
      const text = await resp.text();
      return { status: resp.status, body: text };
    } catch (e) {
      return { error: String(e) };
    }
  });
})();
