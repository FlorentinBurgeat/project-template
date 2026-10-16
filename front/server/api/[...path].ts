// Proxies /api/** to the backend so the browser stays same-origin (refresh-token cookie, no CORS).
// The target comes from runtime config, so it can change per environment without a rebuild.
export default defineEventHandler((event) => {
  const { apiBaseUrl } = useRuntimeConfig(event)
  return proxyRequest(event, apiBaseUrl.replace(/\/$/, '') + event.path)
})
