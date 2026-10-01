# Resonant Combat (NeoForge 1.21.1, Epic Fight addon)

Status: **Phases 0-2 + Phase 3 resource hooks**. Not yet compiled or run - see "First build" below.

## What is implemented
| Phase | Content |
|---|---|
| 0 | `EpicFightBridge` (battle mode, weapon category) + `/resonantcombat debug state <player>` |
| 1 | `PlayerProfile` attachment (persistent, copy-on-death), transient `CombatRuntime`, manual sync payload |
| 2 | Class selection screen (+confirm dialog), server-side weighted Circuit roll, starter kit (once), reveal screen |
| 3 (partial) | Resonance/Liberation gain on hostile hits while the Circuit is active, placeholder HUD bars |
| 7 (partial) | Weapon classes + Circuits are **datapack registries**; item -> class is a **data map** |

All 5 classes and all 15 Circuits from the design doc exist as datapack JSON (roll/reveal only; their combat content is Phase 3+).

## Not implemented (needs Phase 0 answers first)
Basic-combo override, Heavy charge, double jump, Plunge, Skills, Ultimates, Echoes, posture, respec items, key actions.
Keys R/V/G/K are registered; only K (inspect Circuit) does anything.

## First build
1. Copy `gradlew`, `gradlew.bat` and `gradle/wrapper/` from the official NeoForge 1.21.1 MDK (or run `gradle wrapper`).
2. Check `gradle.properties`: `neo_version`, `epicfight_version` (Modrinth *Version Number*), and the `net.neoforged.moddev` plugin version in `build.gradle`.
3. `./gradlew runClient`. If Epic Fight APIs differ, errors will be confined to `EpicFightBridge`.

## Phase 0 test script
1. New world -> class screen opens -> pick Sword -> confirm -> reveal screen shows a Sword Circuit; inventory has iron sword + bread.
2. Relog / die / respawn: no screen, same Circuit, no second kit.
3. `/resonantcombat debug state @s` holding an Epic Fight weapon in battle mode: verify `EF battle mode`, `EF category`, `resolved class`, `circuit active`.
4. Hit a zombie with a Sword-class weapon in battle mode: Resonance +6, Liberation +4 on the HUD.
5. `/resonantcombat circuit reset @s` reopens onboarding; starter kit is NOT granted again.
6. Dedicated server: `./gradlew runServer`, join with a client, repeat 1-2.

## Known design decisions
- Circuit `weight` is per Circuit (tiers 60/30/10 are just the defaults used in the JSON).
- Epic Fight categories are matched by lower-cased `toString()`; EF 21.17 added WeaponCategory inheritance, so this is the first thing to verify.
- Full-profile sync on every Resonance change is a placeholder; replace with a delta payload in Phase 5.
