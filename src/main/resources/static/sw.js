// Simple service worker for offline caching of static assets
const CACHE_NAME = 'resumeopt-cache-v1';
const ASSETS = [
  '/', '/css/styles.css',
  '/js/realtime.js',
  'https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css',
  'https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js',
  'https://cdn.jsdelivr.net/npm/stompjs@2.3.3/lib/stomp.min.js'
];

self.addEventListener('install', (event) => {
  event.waitUntil(caches.open(CACHE_NAME).then(cache => cache.addAll(ASSETS)));
});

self.addEventListener('activate', (event) => {
  event.waitUntil(caches.keys().then(keys => Promise.all(keys.filter(k=>k!==CACHE_NAME).map(k=>caches.delete(k)))));
});

self.addEventListener('fetch', (event) => {
  const req = event.request;
  event.respondWith(
    caches.match(req).then(res => res || fetch(req).catch(()=>caches.match('/')))
  );
});