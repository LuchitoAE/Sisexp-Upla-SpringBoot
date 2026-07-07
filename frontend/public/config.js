window.__SISEXP_CONFIG__ = {
  API_URL: (function() {
    var host = window.location.hostname;
    if (host === "localhost" || host === "127.0.0.1" || host.endsWith(".local")) {
      return "/api";
    }
    return "https://api-gateway-production-e01a.up.railway.app/api";
  })()
};
