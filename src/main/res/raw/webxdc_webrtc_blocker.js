(function () {
  const message = "WebRTC is disabled in webxdc";
  const blockedConstructors = [
    "RTCPeerConnection",
    "webkitRTCPeerConnection",
    "RTCDataChannel",
    "RTCRtpSender",
    "RTCRtpReceiver",
    "RTCSessionDescription",
    "RTCIceCandidate",
  ];
  const windowMarker = "__webxdcWebRtcBlocked";
  const getterMarker = "__webxdcWebRtcGetterHardened";

  const reject = function () {
    if (typeof DOMException === "function") {
      return Promise.reject(new DOMException(message, "NotAllowedError"));
    }
    return Promise.reject(new Error(message));
  };

  const define = function (scope, name, value) {
    if (!scope) {
      return;
    }
    try {
      Object.defineProperty(scope, name, {
        configurable: false,
        writable: false,
        value: value,
      });
    } catch (e) {}
  };

  const hardenGetter = function (proto, name, callback) {
    if (!proto) {
      return;
    }
    try {
      const desc = Object.getOwnPropertyDescriptor(proto, name);
      if (!desc || !desc.get || desc.get[getterMarker]) {
        return;
      }
      const wrappedGetter = function () {
        return callback(desc.get.call(this), this);
      };
      define(wrappedGetter, getterMarker, true);
      Object.defineProperty(proto, name, {
        configurable: desc.configurable,
        enumerable: desc.enumerable,
        get: wrappedGetter,
        set: desc.set,
      });
    } catch (e) {}
  };

  const hardenDocument = function (doc) {
    if (!doc) {
      return doc;
    }
    try {
      hardenGetter(Object.getPrototypeOf(doc), "defaultView", function (win) {
        return blockOnScope(win);
      });
    } catch (e) {}
    try {
      blockOnScope(doc.defaultView);
    } catch (e) {}
    return doc;
  };

  const hardenFrameAccessors = function (scope) {
    if (!scope || !scope.HTMLIFrameElement || !scope.HTMLIFrameElement.prototype) {
      return;
    }
    const proto = scope.HTMLIFrameElement.prototype;
    hardenGetter(proto, "contentWindow", function (win) {
      return blockOnScope(win);
    });
    hardenGetter(proto, "contentDocument", function (doc) {
      return hardenDocument(doc);
    });
  };

  const blockOnScope = function (scope) {
    if (!scope || scope[windowMarker]) {
      return scope;
    }
    define(scope, windowMarker, true);

    blockedConstructors.forEach(function (name) {
      const blocked = function () {
        throw new TypeError(message + ": " + name);
      };
      define(scope, name, blocked);
    });

    try {
      if (scope.navigator && scope.navigator.mediaDevices) {
        define(scope.navigator.mediaDevices, "getUserMedia", function () {
          return reject();
        });
        define(scope.navigator.mediaDevices, "getDisplayMedia", function () {
          return reject();
        });
      }
    } catch (e) {}

    try {
      hardenGetter(scope.Document && scope.Document.prototype, "defaultView", function (win) {
        return blockOnScope(win);
      });
    } catch (e) {}

    hardenFrameAccessors(scope);
    return scope;
  };

  const blockIframeNode = function (node) {
    try {
      node.addEventListener("load", function () {
        try {
          blockOnScope(node.contentWindow);
        } catch (e) {}
      });
      blockOnScope(node.contentWindow);
    } catch (e) {}
  };

  const observeFrames = function (scope) {
    if (!scope || !scope.document || !scope.MutationObserver) {
      return;
    }
    try {
      new scope.MutationObserver(function (mutations) {
        mutations.forEach(function (mutation) {
          mutation.addedNodes.forEach(function (node) {
            if (node.tagName === "IFRAME") {
              blockIframeNode(node);
            } else if (node.querySelectorAll) {
              node.querySelectorAll("iframe").forEach(blockIframeNode);
            }
          });
        });
      }).observe(scope.document.documentElement || scope.document, {
        subtree: true,
        childList: true,
      });
    } catch (e) {}
  };

  blockOnScope(globalThis);
  blockOnScope(window);
  blockOnScope(self);
  observeFrames(window);
})();
