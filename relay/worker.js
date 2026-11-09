/**
 * Perazim Mission Church — Generative AI Breakthrough Guide Relay
 * Cloudflare Worker (Free Plan).
 * 
 * Functions:
 * 1. Protects Gemini API key at the edge (never exposed to Android APK).
 * 2. Requires a short-lived Express-signed service grant.
 * 3. Applies a defensive KV cap; authoritative atomic quota enforcement remains
 *    an Express prerequisite before activation.
 * 4. Checks a limited set of crisis keywords and returns general immediate-help guidance.
 * 5. Calls Google Gemini 3.5 Flash Lite with Bible-grounded system prompt.
 * 6. Zero logging of user question text (privacy-preserving).
 */

const MAX_QUESTION_CHARS = 500;
const DAILY_LIMIT = 3;
const UPSTREAM_TIMEOUT_MS = 30000;
const UUID_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;
const SYSTEM_INSTRUCTION = `You are the pastoral assistant for Perazim Mission Church in Embu, Kenya.
Church Motto: "The Place of Great Breakthrough".
Cornerstone Scriptures: 2 Samuel 5:20 ("The Lord has broken through mine enemies before me, like a breach of waters") and Micah 2:13 ("The breaker goes before them").
MANDATORY GUIDELINES:
1. Speak naturally and directly to the question. Usually use 50–150 words, with more detail only when asked. Do not use a fixed answer template.
2. Treat every request as a separate interaction. Do not imply access to previous conversations or memory.
3. Avoid generic encouragement and scripted closings. Respond to the specific concern first, give one practical next step when helpful, and ask at most one relevant follow-up question. Do not force every answer into advice or a Scripture quotation.
4. Ground your answer in Bible faith.
5. Do not invent Bible quotations, references, or official Perazim positions. If you cannot verify a source, say so clearly.
6. Never claim to be an ordained pastor, elder, doctor, lawyer, or financial consultant. Give no medical, legal, or financial advice.
7. If the user asks off-topic secular questions (e.g., sports, football scores, politics, pop culture, entertainment), politely and warmly decline and redirect them back to spiritual growth, church life, and faith in Jesus Christ.`;

// Crisis detection keywords for immediate pastoral intervention
const CRISIS_PATTERNS = [
  /\b(suicide|suicidal|kill\s+myself|end\s+my\s+life|want\s+to\s+die|hang\s+myself)\b/i,
  /\b(self[\s-]?harm|cutting\s+myself|hurt\s+myself)\b/i,
  /\b(end\s+it\s+all|hopeless.*die|feel\s+like\s+giving\s+up\s+on\s+life)\b/i,
  /\b(abused|physically\s+abused|sexual\s+assault|domestic\s+violence)\b/i
];

export default {
  async fetch(request, env, ctx) {
    // 1. Method check
    if (request.method !== "POST") {
      return new Response(JSON.stringify({ error: "Method not allowed" }), {
        status: 405,
        headers: { "Content-Type": "application/json" }
      });
    }

    const url = new URL(request.url);
    if (url.pathname !== "/chat" && url.pathname !== "/") {
      return new Response(JSON.stringify({ error: "Not found" }), {
        status: 404,
        headers: { "Content-Type": "application/json" }
      });
    }

    // 2. Authenticate Express; no Android token or caller identity authorizes this endpoint.
    const grant = await verifyServiceGrant(request.headers.get("Authorization"), env);
    if (!grant) {
      return new Response(JSON.stringify({ error: "Unauthorized" }), {
        status: 401,
        headers: { "Content-Type": "application/json" }
      });
    }

    // 3. Kill switch check
    if (env.AI_ENABLED !== "true") {
      return new Response(JSON.stringify({ error: "AI service currently disabled" }), {
        status: 503,
        headers: { "Content-Type": "application/json" }
      });
    }

    // 4. Parse Body
    let body;
    try {
      body = await request.json();
    } catch (e) {
      return new Response(JSON.stringify({ error: "Invalid JSON body" }), {
        status: 400,
        headers: { "Content-Type": "application/json" }
      });
    }

    if (!body || typeof body !== "object" || Array.isArray(body) ||
        Object.keys(body).some(key => key !== "question" && key !== "requestId") ||
        typeof body.requestId !== "string" || !UUID_PATTERN.test(body.requestId) ||
        body.requestId !== grant.jti) {
      return new Response(JSON.stringify({ error: "Invalid request" }), {
        status: 400,
        headers: { "Content-Type": "application/json" }
      });
    }
    const rawQuestion = typeof body.question === "string" ? body.question.trim() : "";

    if (!rawQuestion) {
      return new Response(JSON.stringify({ error: "Missing question" }), {
        status: 400,
        headers: { "Content-Type": "application/json" }
      });
    }

    // Rejects questions > 500 chars
    if (rawQuestion.length > MAX_QUESTION_CHARS) {
      return new Response(JSON.stringify({ 
        error: `Question too long (${rawQuestion.length} chars). Maximum allowed is ${MAX_QUESTION_CHARS} characters.` 
      }), {
        status: 400,
        headers: { "Content-Type": "application/json" }
      });
    }

    // 5. Immediate Crisis Intervention
    for (const pattern of CRISIS_PATTERNS) {
      if (pattern.test(rawQuestion)) {
        const crisisResponse = {
          answer: "You matter, and you do not have to face this alone. If you or someone else is in immediate danger, contact local emergency services or go to the nearest hospital now. If you can, reach a trusted person who can stay with you and help you get support.",
          remaining: DAILY_LIMIT
        };
        return new Response(JSON.stringify(crisisResponse), {
          status: 200,
          headers: { "Content-Type": "application/json" }
        });
      }
    }

    // 6. Defensive daily cap. Missing/failing KV fails closed; Express must
    // enforce the authoritative atomic quota before this Worker is enabled.
    if (!env.PERAZIM_KV) {
      return new Response(JSON.stringify({ error: "AI service unavailable" }), { status: 503, headers: { "Content-Type": "application/json" } });
    }
    const nairobiDate = getNairobiDateString();
    const kvKey = `cap:${grant.sub}:${nairobiDate}`;
    let currentUsage = 0;
    try {
      const val = await env.PERAZIM_KV.get(kvKey);
      currentUsage = val === null ? 0 : Number(val);
      if (!Number.isSafeInteger(currentUsage) || currentUsage < 0) throw new Error("invalid usage state");
    } catch (_) {
      return new Response(JSON.stringify({ error: "AI service unavailable" }), { status: 503, headers: { "Content-Type": "application/json" } });
    }

    if (currentUsage >= DAILY_LIMIT) {
      return new Response(JSON.stringify({
        error: "Daily question cap reached",
        remaining: 0,
      }), {
        status: 429,
        headers: { "Content-Type": "application/json" }
      });
    }

    // Reserve conservatively before provider dispatch. KV remains non-atomic;
    // the authenticated Express endpoint must enforce the authoritative quota.
    const newUsage = currentUsage + 1;
    try {
      await env.PERAZIM_KV.put(kvKey, newUsage.toString(), { expirationTtl: 86400 });
    } catch (_) {
      return new Response(JSON.stringify({ error: "AI service unavailable" }), { status: 503, headers: { "Content-Type": "application/json" } });
    }

    // 7. Upstream Call to Gemini 3.5 Flash Lite. Keep trusted instructions
    // application-controlled; the question belongs only in user content.
    if (!env.GEMINI_API_KEY) {
      return new Response(JSON.stringify({ error: "AI service unavailable" }), {
        status: 503,
        headers: { "Content-Type": "application/json" }
      });
    }

    const geminiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:generateContent";
    const geminiPayload = {
      systemInstruction: { parts: [{ text: SYSTEM_INSTRUCTION }] },
      contents: [
        { role: "user", parts: [{ text: rawQuestion }] }
      ],
      generationConfig: {
        maxOutputTokens: 600,
        temperature: 0.65
      }
    };

    let answer = "";
    try {
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), UPSTREAM_TIMEOUT_MS);
      let upstreamResp;
      try {
        upstreamResp = await fetch(geminiUrl, {
          method: "POST",
          headers: { "Content-Type": "application/json", "x-goog-api-key": env.GEMINI_API_KEY },
          body: JSON.stringify(geminiPayload),
          signal: controller.signal
        });
      } finally { clearTimeout(timeoutId); }

      if (!upstreamResp.ok) {
        console.error(`Gemini upstream error HTTP ${upstreamResp.status}`);
        return new Response(JSON.stringify({ error: "AI service unavailable" }), {
          status: 502,
          headers: { "Content-Type": "application/json" }
        });
      }

      const data = await upstreamResp.json();
      const candidate = data?.candidates?.[0]?.content?.parts?.[0]?.text;
      if (!candidate) {
        return new Response(JSON.stringify({ error: "AI service unavailable" }), {
          status: 502,
          headers: { "Content-Type": "application/json" }
        });
      }
      answer = candidate.trim();
    } catch (fetchErr) {
      const isTimeout = fetchErr.name === "AbortError";
      console.error(`Gemini fetch failed (timeout=${isTimeout})`);
      return new Response(JSON.stringify({ error: "AI service unavailable" }), {
        status: 504,
        headers: { "Content-Type": "application/json" }
      });
    }

    // The reservation was written before upstream dispatch.
    const remaining = Math.max(0, DAILY_LIMIT - newUsage);
    return new Response(JSON.stringify({
      answer: answer,
      remaining: remaining
    }), {
      status: 200,
      headers: { "Content-Type": "application/json" }
    });
  }
};

function getNairobiDateString() {
  const d = new Date();
  // Africa/Nairobi is UTC+3
  const utc = d.getTime() + (d.getTimezoneOffset() * 60000);
  const nairobi = new Date(utc + (3600000 * 3));
  return nairobi.toISOString().split("T")[0];
}

async function verifyServiceGrant(authorization, env) {
  const secret = env.PERAZIM_AI_WORKER_SHARED_SECRET;
  if (!secret || secret.length < 32 || typeof authorization !== "string" || !authorization.startsWith("Bearer ")) return null;
  const token = authorization.slice(7).split(".");
  if (token.length !== 3) return null;
  try {
    const decode = part => {
      const normalized = part.replace(/-/g, "+").replace(/_/g, "/");
      return JSON.parse(new TextDecoder().decode(Uint8Array.from(atob(normalized), c => c.charCodeAt(0))));
    };
    const header = decode(token[0]);
    const claims = decode(token[1]);
    if (header.alg !== "HS256" || header.typ !== "JWT") return null;
    const key = await crypto.subtle.importKey("raw", new TextEncoder().encode(secret), { name: "HMAC", hash: "SHA-256" }, false, ["verify"]);
    const normalizedSignature = token[2].replace(/-/g, "+").replace(/_/g, "/");
    const signature = Uint8Array.from(atob(normalizedSignature), c => c.charCodeAt(0));
    const valid = await crypto.subtle.verify("HMAC", key, signature, new TextEncoder().encode(`${token[0]}.${token[1]}`));
    const now = Math.floor(Date.now() / 1000);
    if (!valid || claims.iss !== "perazim-express" || claims.aud !== "perazim-ai-worker" ||
        typeof claims.sub !== "string" || !/^[0-9a-f]{64}$/.test(claims.sub) ||
        typeof claims.jti !== "string" || !UUID_PATTERN.test(claims.jti) ||
        !Number.isInteger(claims.iat) || !Number.isInteger(claims.exp) || claims.exp <= now ||
        claims.iat > now + 30 || now - claims.iat > 90 || claims.exp - claims.iat > 60) return null;
    return claims;
  } catch (_) { return null; }
}
