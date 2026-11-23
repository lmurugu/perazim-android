ART = {"vine_words": "art/vine_bg.png", "lamp_slide": "art/lamp_bg.png", "story_mosaic": "art/story_bg.png"}
TITLE = {"vine_words": ("Vine Words", "Build the word from the vine"), "lamp_slide": ("Lamp Slide", "Slide the lamps into light"), "story_mosaic": ("Story Mosaic", "Put the scene back together")}
ACC = {"vine_words": ("#8bc34a", "#2e7d32"), "lamp_slide": ("#ffb84d", "#c4621a"), "story_mosaic": ("#ff8a1f", "#7a2fc0")}
# Drawn SVG icons (no emoji)
ICO = {
 "hint": '<svg viewBox="0 0 24 24" width="30" height="30"><path d="M9 18h6M10 21h4" stroke="#fff" stroke-width="2" stroke-linecap="round"/><path d="M12 3a6 6 0 0 0-3.5 10.9c.6.5 1 1.2 1 2.1h5c0-.9.4-1.6 1-2.1A6 6 0 0 0 12 3z" fill="#ffe27a" stroke="#fff" stroke-width="1.5"/></svg>',
 "play": '<svg viewBox="0 0 24 24" width="34" height="34"><path d="M8 5v14l11-7z" fill="#fff"/></svg>',
 "restart": '<svg viewBox="0 0 24 24" width="30" height="30"><path d="M4 12a8 8 0 1 0 2.4-5.7" fill="none" stroke="#fff" stroke-width="2.6" stroke-linecap="round"/><path d="M4 4v5h5" fill="none" stroke="#fff" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round"/></svg>',
 "star": '<svg viewBox="0 0 24 24" width="22" height="22"><path d="M12 2l3 6.5 7 .8-5.2 4.8 1.5 7L12 17.8 5.7 21l1.5-7L2 9.3l7-.8z" fill="#ffd54a" stroke="#fff" stroke-width="1"/></svg>',
 "heart": '<svg viewBox="0 0 24 24" width="22" height="22"><path d="M12 21s-8-5.2-8-11a4.5 4.5 0 0 1 8-2.8A4.5 4.5 0 0 1 20 10c0 5.8-8 11-8 11z" fill="#ff5c7a" stroke="#fff" stroke-width="1"/></svg>',
 "pause": '<svg viewBox="0 0 24 24" width="26" height="26"><rect x="6" y="5" width="4" height="14" rx="1" fill="#fff"/><rect x="14" y="5" width="4" height="14" rx="1" fill="#fff"/></svg>',
 "eye": '<svg viewBox="0 0 24 24" width="28" height="28"><path d="M2 12s3.5-6 10-6 10 6 10 6-3.5 6-10 6S2 12 2 12z" fill="none" stroke="#fff" stroke-width="2"/><circle cx="12" cy="12" r="3" fill="#fff"/></svg>',
}
def stars(n): return "".join(f'<span class="{"on" if i<n else "off"}">{ICO["star"]}</span>' for i in range(3))
def hud(n): return f'<div class="hud"><button class="chip">{ICO["pause"]}</button><div class="chip wide"><span class="stars">{stars(n)}</span></div><div class="chip"><span class="lives">{ICO["heart"]}{ICO["heart"]}{ICO["heart"]}</span></div></div>'
def body(key, dark):
    a, b = ACC[key]; t, s = TITLE[key]
    tint = "rgba(20,8,40,.55)" if dark else "rgba(255,246,235,.25)"
    card = "rgba(20,10,40,.72)" if dark else "rgba(255,255,255,.86)"
    ink = "#fff" if dark else "#2a1747"
    tiles = ""
    if key == "vine_words":
        letters = ["G","R","A","P","E","V","I","N"]
        tiles = "".join(f'<div class="tile" style="--a:{a};--b:{b}"><span>{l}</span></div>' for l in letters)
        mech = f'<div class="board grid4">{tiles}</div>'
    elif key == "lamp_slide":
        mech = '<div class="board lamps">' + "".join(f'<div class="lamp {"lit" if i in (1,4,6) else ""}" style="--a:{a};--b:{b}"></div>' for i in range(9)) + '</div>'
    else:
        mech = '<div class="board grid3">' + "".join(f'<div class="tile mosaic" style="--a:{a};--b:{b};opacity:{0.35 if i>=4 else 1}"></div>' for i in range(9)) + '</div>'
    return f"""<!doctype html><html><head><meta charset="utf-8"><style>
*{{box-sizing:border-box}}html,body{{margin:0;height:844px;width:390px;overflow:hidden;font-family:system-ui,sans-serif;color:{ink}}}
body{{background:url({ART[key]}) center/cover no-repeat;background-color:#1b1030}}
body::before{{content:"";position:absolute;inset:0;background:{tint}}}
.wrap{{position:relative;height:844px;padding:54px 18px 24px;display:flex;flex-direction:column}}
.hud{{display:flex;gap:10px;justify-content:space-between;align-items:center}}
.chip{{height:52px;min-width:52px;padding:0 14px;border-radius:26px;display:flex;align-items:center;justify-content:center;gap:4px;background:{card};box-shadow:0 0 0 2px {a}55,0 6px 16px #0005;border:0}}
.chip.wide{{flex:1}}.stars span{{display:inline-block;margin:0 2px}}.stars .off{{opacity:.3;filter:grayscale(1)}}
h1{{margin:14px 2px 0;font-size:28px;font-weight:900;text-shadow:0 2px 0 #0007,0 0 14px {a}aa}}
.sub{{margin:2px 2px 12px;font-size:14px;opacity:.95;text-shadow:0 1px 4px #000a}}
.board{{background:{card};border-radius:28px;padding:14px;box-shadow:0 0 30px {a}77,inset 0 0 0 2px {a}88}}
.grid4{{display:grid;grid-template-columns:repeat(4,1fr);gap:10px}}.grid3{{display:grid;grid-template-columns:repeat(3,1fr);gap:10px}}
.tile{{aspect-ratio:1;border-radius:18px;background:linear-gradient(160deg,var(--a),var(--b));display:flex;align-items:center;justify-content:center;font-size:26px;font-weight:900;color:#fff;box-shadow:0 6px 0 #0007,0 0 16px var(--a)88;text-shadow:0 2px 0 #0006}}
.lamps{{display:grid;grid-template-columns:repeat(3,1fr);gap:12px}}.lamp{{aspect-ratio:1;border-radius:50%;background:radial-gradient(circle,#4a3a66,#1b1030);box-shadow:inset 0 0 0 2px #fff2}}
.lamp.lit{{background:radial-gradient(circle,#fff6c4,var(--a) 55%,var(--b));box-shadow:0 0 22px var(--a)}}
.mid{{flex:1;display:flex;flex-direction:column;justify-content:center}}
.prompt{{align-self:stretch;margin:14px 0 0;padding:12px 14px;border-radius:20px;background:{card};font-size:16px;font-weight:700;text-align:center}}
.row{{display:flex;justify-content:center;align-items:center;gap:22px;margin-top:auto}}
.big{{width:84px;height:84px;border-radius:30px;border:0;display:flex;align-items:center;justify-content:center;background:linear-gradient(165deg,#ffb347,#ff6a00);box-shadow:0 0 26px #ff8a1f99,0 7px 0 #8a3600;}}
.big.purple{{background:linear-gradient(165deg,#b98cff,#5b2a86);box-shadow:0 0 26px #b98cff99,0 7px 0 #2d1450}}
.ghost{{width:60px;height:60px;border-radius:22px;border:0;display:flex;align-items:center;justify-content:center;background:{card};box-shadow:0 0 0 2px {a}66,0 5px 0 #0006}}
.ghost svg path,.ghost svg rect,.ghost svg circle{{}}
</style></head><body><div class="wrap">
{hud(2)}
<h1>{t}</h1><div class="sub">{s}</div>
<div class="mid">{mech}<div class="prompt">{"Tap the word hidden in the vine" if key=="vine_words" else ("Slide each lamp to light the path" if key=="lamp_slide" else "Tap the tiles in story order")}</div></div>
<div class="row"><button class="ghost">{ICO["hint"]}</button><button class="big">{ICO["play"]}</button><button class="ghost">{ICO["restart"]}</button></div>
</div></body></html>"""
for k in ART:
  for m in ("light","dark"):
    open(f"{k}_{m}.html","w").write(body(k, m=="dark"))
