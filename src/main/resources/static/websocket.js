function Websocket() {
    this.socket = null;
    this.events = {};

}

Websocket.prototype.init = function (wsEndpoint) {
    if (!window.WebSocket) window.WebSocket = window.MozWebSocket;
    if (!window.WebSocket) throw "Your browser does not support Web Socket.";

    this.socket = new WebSocket(wsEndpoint);
    return new Promise((resolve, reject) => {
        this.socket.addEventListener('open', () => resolve(this.socket));
        this.socket.addEventListener('error', (event) => {
            console.log(event.message);
            reject(event);
        });
        this.socket.addEventListener('close', () => {
            console.log("Web Socket closed");
            reject(null);
        });
        this.socket.addEventListener('message', (evt) => {
            const eventData = JSON.parse(evt.data);
            console.log(`Message ${eventData.type} accepted`);
            if (this.events[eventData.type] !== undefined) {
                const arr = this.events[eventData.type]
                arr[1].call(arr[0], eventData.data);
            }
        });
    });
}

Websocket.prototype.createEvent = function (eventType, payload) {
    const obj = {type: eventType, data: null};
    if (payload) obj.data = payload
    return JSON.stringify(obj);
}

Websocket.prototype.on = function (type, handler, thisArg) {
    this.events[type] = [thisArg, handler];
}

Websocket.prototype.send = function (type, data) {
    if (this.socket.readyState !== WebSocket.OPEN) throw "Socket is not ready";
    this.socket.send(this.createEvent(type, data));
}