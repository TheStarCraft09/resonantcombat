#!/usr/bin/env python3
"""Generates ability JSON, class heavy_animations, lang keys and the animation catalog (ANIMATIONS.md / animations.csv).
Single source of truth for ability tuning + animation requirements. Run from the repo root: python3 tools/generate_abilities.py"""
import json, os, re, csv

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DATA = f"{ROOT}/src/main/resources/data/resonantcombat/resonantcombat"
LANG = f"{ROOT}/src/main/resources/assets/resonantcombat/lang/en_us.json"
os.makedirs(f"{DATA}/ability", exist_ok=True)

def slug(s): return re.sub(r"[^a-z0-9]+", "_", s.lower().replace("’", "").replace("'", "")).strip("_")

# id, display name, kind, type, resonance_cost, stamina, cooldown_ticks, params, (max_charges, recharge_ticks) for echoes
A = []
def ab(name, kind, type_, rc=0, sc=0, cd=0, charges=(1, 1200), **params):
    A.append(dict(id=slug(name), name=name, kind=kind, type=type_, rc=rc, sc=sc, cd=cd, charges=charges, params=params))

# ---- skills ----
ab("Breaker Step","skill","dash_strike",30,0,160,dash_ticks=6,dash_speed=0.9,damage_mult=1.3,posture_mult=2.0,knockback=0.5)
ab("Iron Reversal","skill","counter",40,0,240,window=24,damage_mult=2.2,posture_mult=4.0)
ab("Brand Cutter","skill","dash_strike",35,0,200,dash_ticks=5,dash_speed=1.0,damage_mult=1.6,posture_mult=1.5,knockback=0.5)
ab("Seismic Cleave","skill","area_burst",45,0,280,delay=8,radius=3.0,forward=3.0,damage_mult=1.6,posture_mult=3.5,knockback=0.8)
ab("War Cry","skill","stance",30,0,300,duration=200,damage_mult=1.15,cast_ticks=12)
ab("Bulwark Crash","skill","dash_strike",40,0,240,dash_ticks=5,dash_speed=0.8,damage_mult=1.2,posture_mult=3.0,knockback=1.2,range=1.8)
ab("Hunter’s Lunge","skill","dash_strike",35,0,200,dash_ticks=8,dash_speed=1.1,damage_mult=1.4,posture_mult=1.5,knockback=0.4)
ab("Falcon Rise","skill","dash_strike",30,0,180,dash_ticks=7,dash_speed=0.35,dash_vy=0.55,damage_mult=1.2,posture_mult=1.5)
ab("Hold the Line","skill","stance",40,0,300,duration=120,taken_mult=0.6,cast_ticks=10)
ab("Gale Dash","skill","dash_strike",25,0,180,dash_ticks=6,dash_speed=0.9,dir=-1.0,damage_mult=0.0)
ab("Hunter’s Mark","skill","mark",30,0,240,range=30.0,duration=300,bonus=1.3)
ab("Veil Step","skill","dash_strike",30,0,240,dash_ticks=5,dash_speed=0.8,dir=-1.0,damage_mult=0.0,invis_ticks=80)
ab("Crescent Counter","skill","counter",35,0,200,window=22,damage_mult=2.0,posture_mult=3.5)
ab("Red Lotus","skill","dash_strike",40,0,240,dash_ticks=6,dash_speed=1.0,damage_mult=1.8,posture_mult=1.8,knockback=0.4)
ab("Petal Step","skill","dash_strike",30,0,160,dash_ticks=6,dash_speed=0.7,dir=-1.0,dash_vy=0.25,damage_mult=0.0)
# ---- ultimates (cost = 100 Liberation Energy, handled in code) ----
ab("Stormbound Arsenal","ultimate","stance",duration=240,damage_mult=1.25,heavy_free=1.0,cast_ticks=14)
ab("Last Light","ultimate","stance",duration=120,taken_mult=0.5,damage_mult=1.1,cast_ticks=14)
ab("Sovereign Break","ultimate","area_burst",delay=14,radius=4.0,forward=2.5,damage_mult=3.0,posture_mult=6.0,knockback=1.0)
ab("Earthshatter Oath","ultimate","area_burst",delay=22,radius=5.0,damage_mult=3.5,posture_mult=8.0,knockback=0.8,launch=0.3)
ab("Unbroken Fury","ultimate","stance",duration=240,damage_mult=1.5,taken_mult=1.1,cast_ticks=14)
ab("Fortress Unleashed","ultimate","stance",duration=200,taken_mult=0.4,damage_mult=1.15,cast_ticks=14)
ab("Thousandfold Thrust","ultimate","area_burst",delay=10,repeats=6,interval=3,radius=2.2,forward=3.2,damage_mult=0.65,posture_mult=1.2,knockback=0.2)
ab("Heavenfall Spear","ultimate","area_burst",delay=26,radius=4.0,damage_mult=3.2,posture_mult=6.0,knockback=0.8,launch=0.4)
ab("Impenetrable Formation","ultimate","stance",duration=160,taken_mult=0.5,cast_ticks=14)
ab("Tempest Barrage","ultimate","area_burst",delay=8,repeats=5,interval=4,radius=2.6,forward=4.0,damage_mult=0.7,posture_mult=1.2,knockback=0.3)
ab("Solar Ruin","ultimate","area_burst",delay=24,radius=3.0,forward=6.0,damage_mult=3.5,posture_mult=5.0,knockback=0.8)
ab("Nightfall Hunt","ultimate","stance",duration=200,damage_mult=1.3,invis_ticks=80,cast_ticks=14)
ab("Silent Moon Verdict","ultimate","area_burst",delay=10,repeats=5,interval=4,radius=3.5,damage_mult=0.8,posture_mult=1.5,knockback=0.2)
ab("Scarlet Tempest","ultimate","dash_strike",dash_ticks=12,dash_speed=1.0,damage_mult=2.4,posture_mult=3.0,range=2.0,knockback=0.3)
ab("Thousand Petals","ultimate","area_burst",delay=8,repeats=8,interval=3,radius=3.5,damage_mult=0.55,posture_mult=1.0,knockback=0.2)
# ---- echoes ----
ab("Ravager Echo","echo","dash_strike",0,10,40,(1,1200),dash_ticks=6,dash_speed=1.0,damage_mult=1.0,posture_mult=2.0,knockback=1.5)
ab("Phantom Echo","echo","stance",0,8,60,(2,900),duration=60,invis_ticks=60,cast_ticks=6)
ab("Iron Golem Echo","echo","area_burst",0,0,80,(1,1500),delay=4,radius=3.5,forward=1.5,damage_mult=0.4,posture_mult=3.0,knockback=1.2)
ab("Blaze Echo","echo","area_burst",0,0,60,(2,1200),delay=4,radius=3.2,forward=1.5,damage_mult=1.0,posture_mult=1.0,ignite=80.0)
ab("Enderman Echo","echo","blink",0,5,40,(2,1000),distance=12.0)
ab("Spider Echo","echo","dash_strike",0,0,40,(2,900),dash_ticks=6,dash_speed=0.5,dash_vy=0.45,damage_mult=0.0)
ab("Vex Echo","echo","dash_strike",0,0,60,(1,1500),dash_ticks=8,dash_speed=1.1,damage_mult=0.8,posture_mult=1.0,knockback=0.3)

lang = json.load(open(LANG))
for a in A:
    d = {"kind": a["kind"], "type": a["type"], "resonance_cost": float(a["rc"]), "stamina_cost": float(a["sc"]),
         "cooldown_ticks": a["cd"], "params": a["params"]}
    if a["kind"] == "echo": d["max_charges"], d["recharge_ticks"] = a["charges"]
    json.dump(d, open(f"{DATA}/ability/{a['id']}.json", "w"), indent=2)
    lang[f"ability.resonantcombat.{a['id']}"] = a["name"]
for k, v in {
    "message.resonantcombat.reject.wrong_weapon": "Hold a weapon of your class in Epic Fight mode",
    "message.resonantcombat.reject.cooldown": "Still on cooldown",
    "message.resonantcombat.reject.no_resonance": "Not enough Resonance",
    "message.resonantcombat.reject.no_liberation": "Liberation Energy is not full",
    "message.resonantcombat.reject.no_charges": "No Echo charges left",
    "message.resonantcombat.reject.no_echo": "No Echo equipped",
    "message.resonantcombat.reject.no_target": "No target in sight",
    "message.resonantcombat.reject.no_space": "No safe space to blink to",
}.items(): lang[k] = v
json.dump(lang, open(LANG, "w"), indent=2, ensure_ascii=False)

# ---- class placeholder heavy animations (built-in Epic Fight animations; replaced by custom ones later) ----
PLACEHOLDER = {"sword": ["SWORD_AUTO2","SWORD_AUTO3","SWORD_DASH"], "broadblade": ["GREATSWORD_AUTO1","GREATSWORD_AUTO2","GREATSWORD_DASH"],
               "spear": ["SPEAR_TWOHAND_AUTO1","SPEAR_TWOHAND_AUTO2","SPEAR_DASH"], "bow": [],
               "saber": ["UCHIGATANA_AUTO1","UCHIGATANA_AUTO3","UCHIGATANA_DASH"]}
for cls, anims in PLACEHOLDER.items():
    p = f"{DATA}/weapon_class/{cls}.json"; d = json.load(open(p)); d["heavy_animations"] = anims
    json.dump(d, open(p, "w"), indent=2)

# ======================= animation catalog =======================
CIRCUITS = [  # class, circuit, combo, heavy, skill, ultimate
 ("sword","Tempest Duelist","Crosswind Sequence","Advancing Cyclone Cleave","Breaker Step","Stormbound Arsenal"),
 ("sword","Bastion Knight","Guarded Edge","Shield-Breaking Thrust","Iron Reversal","Last Light"),
 ("sword","Ashen Vanguard","Ember March","Delayed Execution Slash","Brand Cutter","Sovereign Break"),
 ("broadblade","Mountain Breaker","Crag Splitter","Mountainfall Smash","Seismic Cleave","Earthshatter Oath"),
 ("broadblade","Bloodwrought Reaver","Butcher’s March","Brutal Forward Chop","War Cry","Unbroken Fury"),
 ("broadblade","Iron Colossus","Fortress Cleave","Armored Shoulder Crash","Bulwark Crash","Fortress Unleashed"),
 ("spear","Horizon Lancer","Reach and Return","Piercing Charge","Hunter’s Lunge","Thousandfold Thrust"),
 ("spear","Sky Piercer","Rising Fang","Vaulting Impale","Falcon Rise","Heavenfall Spear"),
 ("spear","Sentinel Pike","Guardline Sweep","Braced Counter-Thrust","Hold the Line","Impenetrable Formation"),
 ("bow","Windrunner","Swiftshot Chain","Piercing Gale Arrow","Gale Dash","Tempest Barrage"),
 ("bow","Ashen Marksman","Marked Precision","Explosive Brand Arrow","Hunter’s Mark","Solar Ruin"),
 ("bow","Shadow Stalker","Ambush Volley","Vanishing Shot","Veil Step","Nightfall Hunt"),
 ("saber","Moonlit Ronin","Crescent Flow","Iai Draw Cut","Crescent Counter","Silent Moon Verdict"),
 ("saber","Crimson Edge","Red Lotus Chain","Execution Dash","Red Lotus","Scarlet Tempest"),
 ("saber","Falling Petal","Petal Dance","Leaping Vertical Slash","Petal Step","Thousand Petals"),
]
TEMPO = {"sword":(16,22),"broadblade":(26,34),"spear":(18,26),"bow":(12,18),"saber":(12,20)}
ABIL = {a["name"]: a for a in A}
ULT_EXTRA = {  # ultimate -> (main length ticks, [(suffix, ticks, loop, note)])
 "Stormbound Arsenal":(30,[("empowered_finisher",22,False,"Upgraded combo finisher played while the stance is active (replaces hit 3)")]),
 "Last Light":(40,[]),"Sovereign Break":(56,[]),"Earthshatter Oath":(60,[]),"Unbroken Fury":(36,[]),"Fortress Unleashed":(40,[]),
 "Thousandfold Thrust":(54,[]),"Heavenfall Spear":(20,[("plunge",40,False,"Vertical launch lands as a high-power plunge strike (second half)")]),
 "Impenetrable Formation":(44,[]),"Tempest Barrage":(60,[]),"Solar Ruin":(40,[("shot",24,False,"The charged shot itself; release at tick 6")]),
 "Nightfall Hunt":(36,[]),"Silent Moon Verdict":(70,[]),"Scarlet Tempest":(64,[]),"Thousand Petals":(68,[]),
}
rows = []
def add(prio, aid, group, kind, length, contact, loop, root, notes):
    rows.append(dict(priority=prio, id=aid, group=group, type=kind, length=length, contact=contact, loop=loop, root=root, notes=notes))

SLICE = "Tempest Duelist"
for cls, circuit, combo, heavy, skill, ult in CIRCUITS:
    c = slug(circuit); base = f"resonantcombat:biped/{cls}/{c}"; hit, fin = TEMPO[cls]
    p = "P0" if circuit == SLICE else "P1"
    ranged = cls == "bow"
    for i in (1, 2, 3):
        L = fin if i == 3 else hit; s0 = round(L*0.38); w = 5 if i == 3 else 3
        add(p, f"{base}/combo_{i}", f"{circuit} - basic combo '{combo}'",
            "RangedAttack (shot)" if ranged else "ComboAttackAnimation", L, f"{s0}-{s0+w}" if not ranged else f"release @ {s0}", False,
            "0.3-0.8 blocks forward (in animation)", "Hit 3 is the finisher" if i == 3 else f"Must chain into combo_{i+1} within 14 ticks")
    add(p, f"{base}/heavy_charge", f"{circuit} - heavy '{heavy}'", "StaticAnimation (loop, upper body)", 20, "-", True, "none", "Hold pose while attack is held; player moves at 35% speed so keep legs free for walking")
    for t, (L, c0, cw, rm) in enumerate([(20,6,3,0.8),(26,8,4,1.2),(34,10,6,2.0)], start=1):
        add(p, f"{base}/heavy_{t}", f"{circuit} - heavy '{heavy}' tier {t}", "AttackAnimation", L, f"{c0}-{c0+cw}", False,
            f"{rm} blocks forward (in animation)", f"Total length = windup+recovery of the code timers (tier {t})")
    sk = ABIL[skill]; ptype = sk["type"]; prm = sk["params"]
    if ptype == "counter":
        add(p, f"{base}/skill_ready", f"{circuit} - skill '{skill}' (ready)", "StaticAnimation (loop)", int(prm['window']), "-", True, "none", "Counter stance held for the counter window")
        add(p, f"{base}/skill_strike", f"{circuit} - skill '{skill}' (counter strike)", "AttackAnimation", 16, "5-9", False, "0.5 blocks forward", "Plays when the counter triggers")
    elif ptype == "dash_strike":
        L = int(prm['dash_ticks']) + 8; dist = prm['dash_speed']*prm['dash_ticks']
        hits = prm.get('damage_mult', 0) > 0
        add(p, f"{base}/skill", f"{circuit} - skill '{skill}'", "AttackAnimation" if hits else "ActionAnimation", L, f"2-{int(prm['dash_ticks'])+2}" if hits else "-", False,
            "NONE (code moves the player)", f"Player travels {dist:.1f} blocks{' backwards' if prm.get('dir',1)<0 else ''} in {int(prm['dash_ticks'])} ticks")
    elif ptype == "area_burst":
        L = int(prm['delay']) + 14
        add(p, f"{base}/skill", f"{circuit} - skill '{skill}'", "AttackAnimation", L, f"{int(prm['delay'])}-{int(prm['delay'])+3}", False, "0-1 block forward", f"Ground impact at tick {int(prm['delay'])}")
    elif ptype == "stance":
        add(p, f"{base}/skill", f"{circuit} - skill '{skill}'", "ActionAnimation", int(prm.get('cast_ticks', 10)) + 6, "-", False, "none", "Buff cast, no hit")
    elif ptype == "mark":
        add(p, f"{base}/skill", f"{circuit} - skill '{skill}'", "ActionAnimation", 14, "-", False, "none", "Aim-and-mark gesture; mark triggers at tick 6")
    ml, extras = ULT_EXTRA[ult]
    hits = ABIL[ult]["type"] in ("area_burst", "dash_strike")
    add(p, f"{base}/ultimate", f"{circuit} - ultimate '{ult}'", "AttackAnimation" if hits else "ActionAnimation", ml, "see notes" if hits else "-", False,
        "NONE for dash ultimates (code moves the player)" if ABIL[ult]["type"] == "dash_strike" else "as needed", (f"Ultimate (stance): buff cast, no hit; the effect lasts {int(ABIL[ult]['params'].get('duration', 0))} ticks" if ABIL[ult]["type"] == "stance"
         else f"Ultimate ({ABIL[ult]['type']}); first impact at tick {int(ABIL[ult]['params'].get('delay', 6))}; may be interrupted before impact"))
    for suffix, L, loop, note in extras:
        add(p, f"{base}/ultimate_{suffix}", f"{circuit} - ultimate '{ult}' ({suffix})", "AttackAnimation", L, "-", loop, "as needed", note)

for cls, (hit, fin) in TEMPO.items():
    base = f"resonantcombat:biped/{cls}/shared"
    p = "P0" if cls == "sword" else "P1"
    name = "stormfall" if cls == "sword" else f"{cls}_plunge"
    add(p, f"{base}/double_jump", f"{cls} - airborne dodge double jump", "ActionAnimation", 12, "-", False, "NONE (code applies +0.42 vertical / +0.18 forward)", "Blends into the fall pose; weapon hand stays free for the plunge")
    add(p, f"{base}/{name}_fall", f"{cls} - plunge descent", "StaticAnimation (loop)", 8, "-", True, "NONE (code drives 1.1 blocks/tick downward)", "Looping dive pose, weapon pointed down")
    add(p, f"{base}/{name}_land", f"{cls} - plunge landing", "AttackAnimation", 30, "0-3", False, "none", "Impact at tick 0 (radius 2.5); the remaining ~27 ticks are the 1.5 s landing recovery")

ECHO = [("ravager_echo","Ravager Echo - forward charge",12,"AttackAnimation","2-8","NONE (code dashes 6 blocks)"),
        ("phantom_echo","Phantom Echo - vanish",10,"ActionAnimation","-","none"),
        ("iron_golem_echo","Iron Golem Echo - guard pulse",14,"AttackAnimation","4-7","none"),
        ("blaze_echo","Blaze Echo - fire burst",12,"AttackAnimation","4-7","none"),
        ("enderman_echo_out","Enderman Echo - blink out",8,"ActionAnimation","-","none"),
        ("enderman_echo_in","Enderman Echo - blink in",8,"ActionAnimation","-","none"),
        ("spider_echo_climb","Spider Echo - wall climb",16,"StaticAnimation (loop)","-","none"),
        ("spider_echo_leap","Spider Echo - leap",12,"ActionAnimation","-","NONE (code applies the leap)"),
        ("vex_echo","Vex Echo - aerial dash",12,"AttackAnimation","2-10","NONE (code dashes 8.8 blocks)")]
for eid, nm, L, kind, contact, root in ECHO:
    add("P0" if eid == "ravager_echo" else "P2", f"resonantcombat:biped/echo/{eid}", nm, kind, L, contact, "loop" in kind, root, "Class-agnostic: must look right with any held weapon")

# ---- write outputs ----
with open(f"{ROOT}/animations.csv", "w", newline="") as f:
    w = csv.writer(f); w.writerow(["priority","id","group","type","length_ticks","contact_ticks","loop","root_motion","status","notes"])
    for r in rows: w.writerow([r["priority"], r["id"], r["group"], r["type"], r["length"], r["contact"], r["loop"], r["root"], "todo", r["notes"]])

n = {k: sum(1 for r in rows if r["priority"] == k) for k in ("P0","P1","P2")}
md = ["# Resonant Combat - animation catalog", "",
      f"**{len(rows)} animations** total: **P0 {n['P0']}** (vertical slice: Tempest Duelist + sword class + Ravager Echo), **P1 {n['P1']}** (all other Circuits and classes), **P2 {n['P2']}** (remaining Echoes).",
      "Machine-readable copy with a status column: `animations.csv`. Regenerate both with `python3 tools/generate_abilities.py`.", "",
      "## Authoring rules", "",
      "- **Tooling: Blender only.** Use the *Epic Fight Player Animation Rig* in Blender and export with the *Epic Fight Blender JSON exporter* (github.com/Epic-Fight/blender-json-exporter, supports Blender 2.8 to 5.0; File > Export > Animated Minecraft Model). Blockbench has no Epic Fight animation export; use it at most for reference/concept work. Bake IK before exporting (the exporter ignores IK bones).",
      "- All animations are biped, weapon in the right hand (`toolR`); two-handed for broadblade and spear.",
      "- **Timing is in game ticks (20 per second).** `length` is the whole animation, `contact` is the tick range where the hit collider is active. If your Blender timeline is 30 FPS, multiply ticks by 1.5 for frames.",
      "- **Root motion:** animations marked `NONE (code ...)` must have no root motion, because the server moves the player (dashes, double jump, plunge). Heavy attacks and combo hits carry their forward motion inside the animation.",
      "- **Lengths are constraints, not suggestions:** heavy tiers 20/26/34 ticks, plunge landing 30 ticks, skills as listed. They equal the code's lock-out timers; if you want a different length, tell me and I change the timer.",
      "- **Damage numbers live in code/datapacks**, not in the animation. Animations only define *when* and *where* the hit collider is active; Resonant Combat scales the damage (heavy tiers, stance, marks).",
      "- IDs are the target Epic Fight animation keys. Export each as its own `.json`; the exact folder layout is confirmed once probe v2 is back (see the roadmap).",
      "- Types are Epic Fight classes from `api/animation/types`: ComboAttackAnimation, AttackAnimation, ActionAnimation, StaticAnimation, RangedAttack (bow shots use Epic Fight's ranged/aim system and need a separate review).", "",
      "## Plunge + double jump: how many?", "",
      "Per class: `double_jump` (1) + `<plunge>_fall` loop (1) + `<plunge>_land` (1) = **3**. Five classes = **15**.",
      "Cheaper options: share one `double_jump` and one `plunge_fall` across all classes and make only the 5 landings per class = **7**; share everything = **3** (weapon grip will look wrong on some classes).",
      "", "## Production order", "",
      "1. **P0** (14 animations) unlocks a complete, playable vertical slice of the Sword class.",
      "2. **P1 by class**: Sword -> Broadblade -> Saber -> Spear. Bow last, because ranged animations depend on Epic Fight's aim/shot system.",
      "3. **P2** Echoes: each is short and reusable by every class.", ""]
current = None
for r in rows:
    parts = r["id"].split("/")
    grp = "echoes (shared)" if parts[1] == "echo" else f"{parts[1]} / {parts[2]}"
    if grp != current:
        current = grp; md += ["", f"### {grp}", "", "| P | ID suffix | What | Type | Len | Contact | Loop | Root motion | Notes |", "|---|---|---|---|---:|---|---|---|---|"]
    md.append(f"| {r['priority']} | `{r['id'].split('/')[-1]}` | {r['group'].split(' - ',1)[-1]} | {r['type']} | {r['length']} | {r['contact']} | {'yes' if r['loop'] else ''} | {r['root']} | {r['notes']} |")
open(f"{ROOT}/ANIMATIONS.md", "w").write("\n".join(md) + "\n")
print(f"{len(A)} abilities, {len(rows)} animations", n)
