PURP = "#5b2a86"
def icon(name, col):
    d = {
     "pause": f'<svg viewBox="0 0 24 24" width="26" height="26"><rect x="6" y="5" width="4" height="14" rx="1" fill="{col}"/><rect x="14" y="5" width="4" height="14" rx="1" fill="{col}"/></svg>',
     "restart": f'<svg viewBox="0 0 24 24" width="28" height="28"><path d="M4 12a8 8 0 1 0 2.4-5.7" fill="none" stroke="{col}" stroke-width="2.8" stroke-linecap="round"/><path d="M4 4v5h5" fill="none" stroke="{col}" stroke-width="2.8" stroke-linecap="round" stroke-linejoin="round"/></svg>',
     "bulb": '<svg viewBox="0 0 24 24" width="30" height="30"><path d="M9 18h6M10 21h4" stroke="#fff" stroke-width="2" stroke-linecap="round"/><path d="M12 3a6 6 0 0 0-3.5 10.9c.6.5 1 1.2 1 2.1h5c0-.9.4-1.6 1-2.1A6 6 0 0 0 12 3z" fill="#ffe27a" stroke="#fff" stroke-width="1.5"/></svg>',
     "play": '<svg viewBox="0 0 24 24" width="34" height="34"><path d="M8 5v14l11-7z" fill="#fff"/></svg>',
     "star": '<svg viewBox="0 0 24 24" width="22" height="22"><path d="M12 2l3 6.5 7 .8-5.2 4.8 1.5 7L12 17.8 5.7 21l1.5-7L2 9.3l7-.8z" fill="#ffd54a" stroke="#fff" stroke-width="1"/></svg>',
     "heart": '<svg viewBox="0 0 24 24" width="22" height="22"><path d="M12 21s-8-5.2-8-11a4.5 4.5 0 0 1 8-2.8A4.5 4.5 0 0 1 20 10c0 5.8-8 11-8 11z" fill="#ff5c7a" stroke="#fff" stroke-width="1"/></svg>',
    }
    return d[name]
def page(body_html, art, dark):
    tint = "rgba(20,8,40,.55)" if dark else "rgba(255,246,235,.25)"
    card = "rgba(20,10,40,.72)" if dark else "rgba(255,255,255,.86)"
    ink = "#fff" if dark else "#2a1747"
    glyph = "#fff" if dark else PURP
    return f"""<!doctype html><html><head><meta charset="utf-8"><style>
*{{box-sizing:border-box}}html,body{{margin:0;height:844px;width:390px;overflow:hidden;font-family:system-ui,sans-serif;color:{ink}}}
body{{background:url({art}) center/cover no-repeat;background-color:#1b1030;position:relative}}
body::before{{content:"";position:absolute;inset:0;background:{tint}}}
.wrap{{position:relative;height:844px;padding:54px 18px 24px;display:flex;flex-direction:column}}
.hud{{display:flex;gap:10px;align-items:center}}
.chip{{height:52px;min-width:52px;padding:0 14px;border-radius:26px;display:flex;align-items:center;justify-content:center;gap:4px;background:{card};box-shadow:0 0 0 2px #8b5cf655,0 6px 16px #0005;border:0}}
.chip.wide{{flex:1}}.chip.glyph svg path,.chip.glyph svg rect{{}}
h1{{margin:10px 2px 0;font-size:26px;font-weight:900;text-shadow:0 2px 0 #0007,0 0 14px #b98cffaa}}
.sub{{margin:2px 2px 10px;font-size:14px;text-shadow:0 1px 4px #000a}}
.board{{background:{card};border-radius:28px;padding:14px;box-shadow:0 0 30px #b98cff77,inset 0 0 0 2px #b98cff88}}
.cw{{display:grid;grid-template-columns:repeat(5,1fr);grid-auto-rows:50px;gap:4px}}
.cell{{aspect-ratio:1;border-radius:8px;background:#fff;color:#2a1747;font-weight:900;font-size:18px;display:flex;align-items:center;justify-content:center;box-shadow:inset 0 0 0 1px #8b5cf6}}
.cell.dark{{background:transparent;box-shadow:none}}.cell.ok{{background:linear-gradient(160deg,#8bc34a,#2e7d32);color:#fff}}
.wheel{{position:relative;width:200px;height:200px;margin:14px auto 0;border-radius:50%;background:radial-gradient(circle,#ffffffcc,#ffffff44 70%);box-shadow:0 0 30px #b98cff99,inset 0 0 0 2px #b98cff}}
.wl{{position:absolute;width:52px;height:52px;margin:-26px;border-radius:50%;background:linear-gradient(160deg,#b98cff,#5b2a86);color:#fff;font-weight:900;font-size:20px;display:flex;align-items:center;justify-content:center;box-shadow:0 5px 0 #2d1450,0 0 14px #b98cff99}}
.ans{{display:grid;grid-template-columns:1fr 1fr;gap:12px;margin-top:12px}}
.tileA{{min-height:78px;border-radius:24px;border:0;background:linear-gradient(165deg,#b98cff,#5b2a86);color:#fff;font-size:20px;font-weight:900;box-shadow:0 7px 0 #2d1450,0 0 20px #b98cff88;display:flex;align-items:center;justify-content:center;text-shadow:0 2px 0 #0006}}
.row{{display:flex;justify-content:center;align-items:center;gap:22px;margin-top:auto}}
.big{{width:84px;height:84px;border-radius:30px;border:0;display:flex;align-items:center;justify-content:center;background:linear-gradient(165deg,#ffb347,#ff6a00);box-shadow:0 0 26px #ff8a1f99,0 7px 0 #8a3600}}
.ghost{{width:60px;height:60px;border-radius:22px;border:0;display:flex;align-items:center;justify-content:center;background:{card};box-shadow:0 0 0 2px #8b5cf666,0 5px 0 #0006}}
.hintbtn{{width:100%;height:58px;border-radius:24px;border:0;display:flex;align-items:center;justify-content:center;gap:10px;background:linear-gradient(165deg,#ffb347,#ff6a00);color:#fff;font-weight:800;font-size:17px;box-shadow:0 0 22px #ff8a1f88,0 6px 0 #8a3600;margin-top:12px}}
</style></head><body><div class="wrap">{body_html}</div></body></html>"""
def hud(n, dark, glyph_col):
    st = "".join(f'<span style="margin:0 2px;opacity:{1 if i<n else .3}">{icon("star",None) if False else ""}</span>' for i in range(3))
    stars = "".join(f'<span style="margin:0 2px;opacity:{1 if i<n else .3}">{icon("star","")}</span>' for i in range(3))
    return f'<div class="hud"><button class="chip">{icon("pause",glyph_col)}</button><div class="chip wide">{stars}</div><div class="chip">{icon("heart","")}{icon("heart","")}{icon("heart","")}</div></div>'
def vine(dark):
    gc = "#fff" if dark else "#5b2a86"
    cw = ["G","R","A","P","E","","","","E","","","","V","I","N","E","","","",""]
    cells = "".join(f'<div class="cell {"dark" if c=="" else ("ok" if i in (0,1,2,3,4,8,12,13,14,18) else "")}">{c}</div>' for i,c in enumerate(cw))
    letters = ["G","R","A","P","E","V","I","N"]
    import math
    wl = "".join(f'<div class="wl" style="left:{100+78*math.cos(2*math.pi*i/8-math.pi/2):.0f}px;top:{100+78*math.sin(2*math.pi*i/8-math.pi/2):.0f}px">{l}</div>' for i,l in enumerate(letters))
    body = f'{hud(2,dark,gc)}<h1>Vine Words</h1><div class="sub">Swipe letters from the wheel to fill the grid</div><div class="board"><div class="cw">{cells}</div></div><div class="wheel">{wl}</div><div class="row"><button class="ghost">{icon("bulb","")}</button><button class="big">{icon("play","")}</button><button class="ghost">{icon("restart",gc)}</button></div>'
    return page(body, "art/vine_bg.png", dark)
def lamp(dark):
    gc = "#fff" if dark else "#5b2a86"
    lamps = "".join(f'<div style="aspect-ratio:1;border-radius:50%;background:{"radial-gradient(circle,#fff6c4,#ffb84d 55%,#c4621a)" if i in (1,4,6) else "radial-gradient(circle,#4a3a66,#1b1030)"};box-shadow:{"0 0 22px #ffb84d" if i in (1,4,6) else "inset 0 0 0 2px #fff2"}"></div>' for i in range(9))
    body = f'{hud(2,dark,gc)}<h1>Lamp Slide</h1><div class="sub">Slide the lamps into light</div><div class="board" style="display:grid;grid-template-columns:repeat(3,1fr);gap:12px">{lamps}</div><div class="row"><button class="ghost">{icon("bulb","")}</button><button class="big">{icon("play","")}</button><button class="ghost">{icon("restart",gc)}</button></div>'
    return page(body, "art/lamp_bg.png", dark)
def quiz(dark):
    gc = "#fff" if dark else "#5b2a86"
    answers = ["Nicodemus","Peter","Thomas","Zacchaeus"]
    tiles = "".join(f'<button class="tileA">{a}</button>' for a in answers)
    body = f'{hud(0,dark,gc)}<h1>Bible Quiz</h1><div class="sub">Question 1 of 5</div><div class="board" style="font-size:22px;font-weight:800;text-align:center;padding:22px 16px">Who climbed a sycamore tree to see Jesus?</div><div class="ans">{tiles}</div><button class="hintbtn">{icon("bulb","")}Show a hint</button><div class="row" style="margin-top:14px"><button class="ghost">{icon("restart",gc)}</button></div>'
    return page(body, "art/story_bg.png", dark)
import os
for name, fn in [("vine_words", vine), ("lamp_slide", lamp), ("bible_quiz", quiz)]:
    for m in ("light","dark"):
        open(f"{name}_{m}.html","w").write(fn(m=="dark"))
