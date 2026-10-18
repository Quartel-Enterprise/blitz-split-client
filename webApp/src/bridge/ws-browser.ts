// Ktor's JS client loads the Node "ws" package with a dynamic import, but only under Node; the
// browser takes its native WebSocket. Aliasing "ws" here keeps Vite from looking for a Node module.
export default globalThis.WebSocket
