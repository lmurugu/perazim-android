/**
 * Perazim Mission Church — Generative AI Breakthrough Guide Relay
 * Cloudflare Worker (Free Plan).
 * 
 * Functions:
 * 1. Protects Gemini API key at the edge (never exposed to Android APK).
 * 2. Requires shared X-App-Token header.
 * 3. Enforces 3 questions/day per deviceId via Cloudflare KV.
 * 4. Checks crisis/self-harm keywords and routes to pastoral hotline (+254 710 772 227).
 * 5. Calls Google Gemini 3.5 Flash Lite with Bible-grounded system prompt.
 * 6. Zero logging of user question text (privacy-preserving).
 */

const MAX_QUESTION_CHARS = 500;
const DAILY_LIMIT = 3;
const UPSTREAM_TIMEOUT_MS = 30000;
const PASTORAL_HOTLINE = "+254 710 772 227";

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
    if (request.method === "OPTIONS") {
      return handleCors();
    }
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

    // 2. Auth Header: X-App-Token
    const clientToken = request.headers.get("X-App-Token") || "";
    const expectedToken = env.APP_TOKEN || "";
    if (!expectedToken || clientToken !== expectedToken) {
      return new Response(JSON.stringify({ error: "Unauthorized: Invalid or missing X-App-Token" }), {
        status: 401,
        headers: { "Content-Type": "application/json" }
      });
    }

    // 3. Kill switch check
    if (env.AI_ENABLED === "false") {
      return new Response(JSON.stringify({ error: "AI service currently disabled", fallback: true }), {
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

    const deviceId = (body.deviceId || "").trim();
    const rawQuestion = (body.question || "").trim();
    const firstName = (body.firstName || "").trim();
    const context = body.context || {};

    if (!deviceId) {
      return new Response(JSON.stringify({ error: "Missing deviceId" }), {
        status: 400,
        headers: { "Content-Type": "application/json" }
      });
    }

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
          answer: `You are deeply precious and valued by God and by Perazim Mission Church. You do not have to carry this burden alone. Please reach out immediately to our pastoral care team at ${PASTORAL_HOTLINE} (Call, SMS, or WhatsApp), or contact emergency services immediately. Bishop Dr. David Mutweri and the church family are here to stand with you.`,
          remaining: DAILY_LIMIT
        };
        return new Response(JSON.stringify(crisisResponse), {
          status: 200,
          headers: { "Content-Type": "application/json", ...corsHeaders() }
        });
      }
    }

    // 6. Rate Limit & Daily Cap (Nairobi Timezone)
    const nairobiDate = getNairobiDateString();
    const kvKey = `cap:${deviceId}:${nairobiDate}`;
    let currentUsage = 0;

    if (env.PERAZIM_KV) {
      try {
        const val = await env.PERAZIM_KV.get(kvKey);
        currentUsage = val ? parseInt(val, 10) : 0;
      } catch (kvErr) {
        // Fail-soft: if KV read fails, allow request
        console.warn("KV get error, proceeding fail-soft", kvErr.message);
      }
    }

    if (currentUsage >= DAILY_LIMIT) {
      return new Response(JSON.stringify({
        error: "Daily question cap reached",
        remaining: 0,
        fallback: true
      }), {
        status: 429,
        headers: { "Content-Type": "application/json", ...corsHeaders() }
      });
    }

    // 7. Prompt Assembly
    const passagesText = (Array.isArray(context.passages) && context.passages.length > 0)
      ? context.passages.slice(0, 3).map((p, idx) => `[Passage ${idx + 1}]: ${p}`).join("\n")
      : "None supplied in context.";

    const systemInstruction = `You are the pastoral assistant for Perazim Mission Church in Embu, Kenya.
Church Motto: "The Place of Great Breakthrough".
Cornerstone Scriptures: 2 Samuel 5:20 ("The Lord has broken through mine enemies before me, like a breach of waters") and Micah 2:13 ("The breaker goes before them").
MANDATORY GUIDELINES:
1. Warm, uplifting, concise tone. Output strictly under 120 words.
2. Ground your answer in Bible faith.
3. Quote Scripture ONLY from the passages provided in Context below. If none fit the user's question, explicitly state: "Please explore the in-app Bible reader for related verses." NEVER invent, fabricate, or misattribute Scripture verses.
4. Never claim to be an ordained pastor, elder, doctor, lawyer, or financial consultant. Give no medical, legal, or financial advice.
5. If the user asks off-topic secular questions (e.g., sports, football scores, politics, pop culture, entertainment), politely and warmly decline and redirect them back to spiritual growth, church life, and faith in Jesus Christ.`;

    const userPromptText = `Context:
- Member Name: ${firstName || "Fellow Pilgrim"}
- Daily Reflection Streak: ${context.streak || 0} days
- Today's Cornerstone Scripture: ${context.verse || "2 Samuel 5:20"}
- Available Scripture Passages:
${passagesText}

Member Question: ${rawQuestion}`;

    // 8. Upstream Call to Gemini 3.5 Flash Lite
    if (!env.GEMINI_API_KEY) {
      return new Response(JSON.stringify({ error: "Worker misconfigured: GEMINI_API_KEY missing", fallback: true }), {
        status: 500,
        headers: { "Content-Type": "application/json" }
      });
    }

    const geminiUrl = `https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:generateContent?key=${env.GEMINI_API_KEY}`;
    const geminiPayload = {
      contents: [
        {
          role: "user",
          parts: [{ text: `${systemInstruction}\n\n---\n\n${userPromptText}` }]
        }
      ],
      generationConfig: {
        maxOutputTokens: 300,
        temperature: 0.3
      }
    };

    let answer = "";
    try {
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), UPSTREAM_TIMEOUT_MS);

      const upstreamResp = await fetch(geminiUrl, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(geminiPayload),
        signal: controller.signal
      });
      clearTimeout(timeoutId);

      if (!upstreamResp.ok) {
        const errText = await upstreamResp.text();
        console.error(`Gemini upstream error HTTP ${upstreamResp.status}: ${errText.substring(0, 100)}`);
        return new Response(JSON.stringify({ error: `Upstream error ${upstreamResp.status}`, fallback: true }), {
          status: 502,
          headers: { "Content-Type": "application/json", ...corsHeaders() }
        });
      }

      const data = await upstreamResp.json();
      const candidate = data?.candidates?.[0]?.content?.parts?.[0]?.text;
      if (!candidate) {
        return new Response(JSON.stringify({ error: "Empty model response", fallback: true }), {
          status: 502,
          headers: { "Content-Type": "application/json", ...corsHeaders() }
        });
      }
      answer = candidate.trim();
    } catch (fetchErr) {
      const isTimeout = fetchErr.name === "AbortError";
      console.error(`Gemini fetch error (timeout=${isTimeout}):`, fetchErr.message);
      return new Response(JSON.stringify({ 
        error: isTimeout ? "Request timed out after 15s" : "Network connection error", 
        fallback: true 
      }), {
        status: 504,
        headers: { "Content-Type": "application/json", ...corsHeaders() }
      });
    }

    // 9. Update Usage Counter in KV (TTL: 86400s)
    const newUsage = currentUsage + 1;
    if (env.PERAZIM_KV) {
      try {
        await env.PERAZIM_KV.put(kvKey, newUsage.toString(), { expirationTtl: 86400 });
      } catch (kvPutErr) {
        console.warn("KV put error:", kvPutErr.message);
      }
    }

    const remaining = Math.max(0, DAILY_LIMIT - newUsage);
    return new Response(JSON.stringify({
      answer: answer,
      remaining: remaining
    }), {
      status: 200,
      headers: { "Content-Type": "application/json", ...corsHeaders() }
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

function handleCors() {
  return new Response(null, {
    status: 204,
    headers: corsHeaders()
  });
}

function corsHeaders() {
  return {
    "Access-Control-Allow-Origin": "*",
    "Access-Control-Allow-Methods": "POST, OPTIONS",
    "Access-Control-Allow-Headers": "Content-Type, X-App-Token",
    "Access-Control-Max-Age": "86400"
  };
}
