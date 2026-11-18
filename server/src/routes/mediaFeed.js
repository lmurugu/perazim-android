'use strict';

// Public, read-only feed of the church's YouTube uploads.
// The API key stays on the server (YOUTUBE_API_KEY); the app only receives the list.

const express = require('express');

const CHANNEL_ID = 'UC2Yv5gJArnHSuXApBupkv-A';
const CACHE_MS = 15 * 60 * 1000;
const MAX_ITEMS = 25;

function uploadsPlaylistId(channelId) {
  if (!/^UC[A-Za-z0-9_-]{22}$/.test(channelId)) throw new Error('invalid channel id');
  return 'UU' + channelId.slice(2);
}

function mapPlaylistItems(json) {
  const items = Array.isArray(json && json.items) ? json.items : [];
  return items
    .map((it) => it && it.snippet && it.snippet.resourceId && it.snippet.resourceId.videoId
      ? {
          videoId: it.snippet.resourceId.videoId,
          title: String(it.snippet.title || ''),
          publishedAt: String(it.snippet.publishedAt || ''),
          thumbnailUrl: String((it.snippet.thumbnails && it.snippet.thumbnails.medium && it.snippet.thumbnails.medium.url) || ''),
          watchUrl: 'https://www.youtube.com/watch?v=' + it.snippet.resourceId.videoId,
        }
      : null)
    .filter(Boolean);
}

function createMediaFeedHandler({ apiKey, fetchImpl = fetch, now = () => Date.now() } = {}) {
  let cache = null; // { at, items }

  return async function churchFeed(req, res) {
    if (!apiKey) {
      return res.status(503).json({ error: 'Media feed is not configured on this server.' });
    }
    if (cache && now() - cache.at < CACHE_MS) {
      return res.json({ channelId: CHANNEL_ID, cached: true, items: cache.items });
    }
    const url = new URL('https://www.googleapis.com/youtube/v3/playlistItems');
    url.searchParams.set('part', 'snippet');
    url.searchParams.set('maxResults', String(MAX_ITEMS));
    url.searchParams.set('playlistId', uploadsPlaylistId(CHANNEL_ID));
    url.searchParams.set('key', apiKey);
    try {
      const upstream = await fetchImpl(url.toString());
      if (!upstream.ok) {
        // Do not forward the upstream body: it may echo request details.
        return res.status(502).json({ error: 'Video feed is temporarily unavailable.' });
      }
      const items = mapPlaylistItems(await upstream.json());
      cache = { at: now(), items };
      return res.json({ channelId: CHANNEL_ID, cached: false, items });
    } catch (err) {
      return res.status(502).json({ error: 'Video feed is temporarily unavailable.' });
    }
  };
}

function createMediaFeedRouter(options = {}) {
  const router = express.Router();
  router.get('/church-feed', createMediaFeedHandler({
    apiKey: process.env.YOUTUBE_API_KEY || '',
    ...options,
  }));
  return router;
}

module.exports = { createMediaFeedRouter, createMediaFeedHandler, mapPlaylistItems, uploadsPlaylistId, CHANNEL_ID };
