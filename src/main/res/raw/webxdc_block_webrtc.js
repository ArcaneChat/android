(function() {
    const windowMarker = "__webxdcWebRtcBlocked";
    const getterMarker = "__webxdcWebRtcGetterHardened";

    function define(scope, name, value) {
        try {
            Object.defineProperty(scope, name, {
                configurable: false,
                writable: false,
                value: value
            });
        } catch (e) {}
    }

    function hardenGetter(proto, name, callback) {
        if (!proto) { return; }
        try {
            const desc = Object.getOwnPropertyDescriptor(proto, name);
            if (!desc || !desc.get || desc.get[getterMarker]) {
                return;
            }
            const wrappedGetter = function () {
                return callback(desc.get.call(this));
            };
            define(wrappedGetter, getterMarker, true);
            Object.defineProperty(proto, name, {
                configurable: desc.configurable,
                enumerable: desc.enumerable,
                get: wrappedGetter,
                set: desc.set,
            });
        } catch (e) {}
    }

    function blockOnScope(scope) {
        try {
            if (!scope || scope[windowMarker]) { return scope; }
            define(scope, windowMarker, true);
            define(scope, 'RTCPeerConnection', ()=>{});
            define(scope, 'webkitRTCPeerConnection', ()=>{});

            hardenGetter(scope.Document && scope.Document.prototype, "defaultView", blockOnScope);
            if (scope.HTMLIFrameElement && scope.HTMLIFrameElement.prototype) {
                const proto = scope.HTMLIFrameElement.prototype;
                hardenGetter(proto, "contentWindow", blockOnScope);
                hardenGetter(proto, "contentDocument", function (doc) {
                    if (!doc) { return doc; }
                    hardenGetter(Object.getPrototypeOf(doc), "defaultView", blockOnScope);
                    blockOnScope(doc.defaultView);
                    return doc;
                });
            }
        } catch (e) {}
        return scope;
    }

    blockOnScope(globalThis);
    blockOnScope(window);
    blockOnScope(self);
})();
