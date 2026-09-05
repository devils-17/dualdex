# DualDex Future Platform Roadmap

> **Long-term roadmap only. This document is explicitly not part of the AYN Thor `0.9.0-beta.1` release scope.**
>
> The current GBA beta should be stabilized, released, and validated with real users before work begins here.

## Vision

DualDex should eventually become more than a GBA emulator with a Pokémon companion. The longer-term architecture can support multiple emulator cores and console families while preserving the thing that makes DualDex different: **game-aware, real-time companion tools that run alongside the game**.

Nintendo DS is the most logical first platform expansion because:

- the AYN Thor is already physically suited to DS emulation,
- Pokémon has a large DS-era ROM-hack ecosystem,
- the second screen creates interesting companion-layout opportunities rather than merely duplicating a normal DS emulator,
- Gen 4 already has mature reverse-engineering resources and community calculator/documentation tooling,
- and Pokémon Platinum Kaizo is a strong first target because its difficulty makes live calculations, trainer information, encounter data, and documentation unusually valuable.

The long-term goal is therefore **not simply to add a DS emulator**. The goal is to evolve DualDex into a **multi-system libretro frontend plus game-integration platform**, with Nintendo DS / Platinum Kaizo as the first new system and game integration.

---

# Feasibility Summary

| Capability | Feasibility | Notes |
|---|---|---|
| Nintendo DS emulation on AYN Thor | **High** | Maintained libretro DS cores support Android arm64. Thor Base/Pro/Max hardware is Snapdragon 8 Gen 2; Lite uses Snapdragon 865, so both tiers should be benchmarked. |
| Two DS screens across the Thor's two physical displays | **High** | DS libretro cores expose configurable screen layouts. A composite framebuffer can be cropped into separate top/bottom textures if necessary. |
| DS touchscreen input | **High** | Libretro supports pointer/touch-style input; the bottom physical display can map to DS stylus coordinates. |
| Real-time Gen 4 party data | **Medium-High** | Gen 4 Pokémon structures are well documented, and existing Platinum/Platinum Kaizo Lua tools already read live state. Exact RAM mapping still needs implementation and version validation. |
| Real-time battle/opponent tracking | **Medium** | Feasible, but more complex than party parsing because Platinum uses dynamic overlays and battle structures. Must be based on exact game/version layouts and coherent battle snapshots. |
| Platinum Kaizo calculator/docs integration | **High** | The hack has official/community documentation, calculators, trainer data, and live sync tooling. ROM-driven data extraction is also feasible. |
| Generic multi-system architecture | **High** | DualDex already uses libretro, but the current frontend has several GBA-specific assumptions that need to be generalized. |
| Platinum Kaizo on melonDS specifically | **Unverified / risk** | Platinum Kaizo's current FAQ recommends DeSmuME and warns that melonDS-core emulators have save problems. This must be tested against the exact current melonDS DS core/version rather than assumed to work. |
| Bundling a DS core under the current licensing model | **Requires review** | DeSmuME is GPLv2; melonDS DS is GPLv3+. Do not assume loading a core dynamically removes copyleft/distribution obligations. |

**Overall verdict:** technically feasible and a good long-term fit for DualDex, but the correct first step is a core/platform abstraction and compatibility prototype—not immediately writing a Platinum Kaizo parser.

---

# Research Findings

## 1. DS emulator core options

### melonDS DS

The current libretro `melonDS DS` core is the technically attractive long-term option.

Relevant capabilities include:

- Android arm64 support,
- multiple screen layouts and runtime layout switching,
- touch / virtual cursor input,
- hardware rendering support,
- save files and save states,
- memory monitoring support,
- optional dynarec capability,
- DS and DSi support,
- microphone and other modern libretro interfaces.

However, **Platinum Kaizo's current official FAQ specifically recommends DeSmuME and states that emulators using the melonDS core cannot save correctly for the hack**. That warning should be treated as a real compatibility blocker until proven otherwise on the exact `melonDS DS` build DualDex would ship.

### DeSmuME

DeSmuME is therefore the safest first compatibility candidate for Platinum Kaizo itself.

Advantages:

- directly recommended by the Platinum Kaizo project,
- mature Platinum compatibility,
- libretro support for screen layouts, memory monitoring, touch input, saves, and states,
- existing Platinum Kaizo community tools already target DeSmuME's Lua environment.

Potential drawback:

- libretro documentation notes that DeSmuME JIT availability is limited on Android, which could make it less efficient than melonDS DS.

### Recommended core strategy

Do **not** choose one core permanently before prototyping.

Build the architecture so that a Nintendo DS game integration is independent of the selected core, then benchmark:

1. **DeSmuME** — compatibility-first Platinum Kaizo target.
2. **melonDS DS** — preferred modern Android core if Platinum Kaizo save behavior can be validated/fixed upstream or shown not to affect the current core/version.

The same Platinum/Gen 4 companion adapter should ideally work on both once they expose a normalized DS memory map.

---

## 2. Platinum / Gen 4 live-memory reading is practical

Generation 4 Pokémon data is well understood. Party Pokémon use the documented Gen 4 encrypted/shuffled Pokémon format with extra party fields; public reverse-engineering references describe the 136-byte boxed and 236-byte party structures.

More importantly, the feasibility is already demonstrated by the Platinum community:

- Platinum Kaizo has an official Lua sync tool for DeSmuME that can automatically import live Pokémon into its calculator.
- Other Platinum Lua tooling performs battle tracking, party/box export, and calculator synchronization.
- The `pret/pokeplatinum` decompilation exposes readable Platinum battle-system and party structures and can be used as a reference for understanding vanilla engine behavior.

This means DualDex's Gen 4 work is primarily an **integration and version-mapping problem**, not a question of whether the data can be read at all.

### Important difference from GBA

Nintendo DS memory is more complicated than the current GBA EWRAM model:

- multiple CPU/memory regions exist,
- Platinum uses dynamically loaded overlays,
- battle code/data may live in overlay-specific regions,
- hack revisions can change overlay content and addresses,
- absolute RAM addresses therefore should not become another set of unversioned constants.

The DS implementation should build on the planned engine/layout-profile architecture from the GBA beta rather than recreating hard-coded per-game offsets.

---

## 3. Static ROM data can power docs and calculators

DualDex does not need to obtain every piece of game information through live RAM.

For Nintendo DS Pokémon games, much of the useful companion data can be extracted from the user-supplied `.nds` image:

- species/base stats,
- types,
- abilities,
- moves and move properties,
- learnsets,
- trainer rosters,
- encounter tables,
- items/TMs,
- other ROM-hack-specific static data.

There is already a useful proof of concept: `hzla/ddex` can load Gen 4 `.nds` ROM hacks and export local calculator/dex data, including permanently hosted profiles for several DS hacks and Platinum Kaizo.

### Long-term recommendation: `GameDataPack`

Create a normalized internal data layer:

```text
GameDataPack
- game/version identity
- species
- moves
- abilities
- items
- trainer sets
- encounters
- level caps
- mechanics overrides
- documentation entries
- calculator rules
```

A data pack could be produced from:

1. **local extraction from the user's ROM** where technically practical, or
2. **explicitly licensed community data packs** bundled/downloaded separately.

Prefer local extraction where possible. It automatically follows the exact ROM build and avoids relying on stale third-party spreadsheets.

Do not redistribute ROMs, BIOS/firmware, or copyrighted game assets. Community datasets/code should only be reused after checking their licenses/permission.

---

# Proposed Multi-System Architecture

The current architecture treats mGBA/GBA as the system. Before adding DS, separate the following concepts.

## 1. Generic emulator core session

Replace mGBA-specific assumptions with a generic core interface.

```text
EmulatorCoreSession
- coreId
- systemId
- loadCore()
- loadGame()
- unloadGame()
- runFrame()
- reset()
- saveState()/loadState()
- saveRam()
- coreOptions
- videoInfo
- audioInfo
- memoryRegions
- inputCapabilities
```

The implementation can still use libretro internally.

### Libretro frontend capabilities that need to be added/generalized

The current frontend is intentionally minimal and GBA-oriented. DS support will require fuller handling of:

- dynamic framebuffer geometry instead of GBA-sized assumptions,
- libretro core options / variables,
- system/save/state directories,
- `RETRO_ENVIRONMENT_SET_MEMORY_MAPS`,
- hardware-render context negotiation if used,
- pointer/touch input,
- potentially microphone input,
- runtime geometry/layout changes,
- more general memory-region discovery,
- per-core save extensions and behavior.

The emulator-thread command queue planned for the GBA beta should remain the owner of every core mutation.

---

## 2. Console/system adapter

```text
ConsoleAdapter
- systemId: GBA | NDS | ...
- supportedCoreIds
- videoLayoutCapabilities
- inputMapping
- memoryRegionNormalization
- save conventions
```

For Nintendo DS this adapter should normalize concepts such as:

```text
NDS_MAIN_RAM
NDS_ARM7_RAM
NDS_VRAM / other regions if ever needed
```

Game parsers should request normalized regions rather than depend directly on emulator-core-specific pointers.

---

## 3. Game integration adapter

```text
GameIntegration
- profile/version detector
- runtime parser
- BattleSnapshot provider
- PlayerSnapshot provider
- GameDataPack provider
- calculator rules
- documentation provider
- optional UI actions/macros
```

Examples:

```text
Gen3PokemonIntegration
PlatinumGen4Integration
PlatinumKaizoIntegration
```

A ROM hack should extend/version a shared engine integration rather than create a totally separate emulator path.

---

## 4. Core-independent snapshots

Companion UI should eventually consume generic data models:

```text
PlayerSnapshot
- party
- inventory summary (optional)
- location

BattleSnapshot
- isInBattle
- player battlers
- enemy battlers
- active party indexes
- HP/status/stat stages
- weather/field state
- trainer identity if known

PokemonSnapshot
- species/form
- level
- stats
- IVs/EVs where applicable
- moves/PP
- item
- ability
- status
```

The Party and Calculator screens should not care whether those snapshots came from GBA mGBA memory or DS DeSmuME memory.

---

# Nintendo DS Display / UX Concept

DS support should not simply imitate a stock two-screen emulator. The Thor has enough physical screen space to preserve the original game while still showing DualDex information.

## Mode A — Native DS + Companion Sidecar

**Top physical display (1920×1080):**

- Render the DS top screen at a full-height 4:3 ratio.
- A 1080px-tall 4:3 DS viewport is approximately **1440×1080**, leaving about **480 horizontal pixels** unused on the Thor's 1920px-wide top display.
- Use that otherwise-empty side area for a compact live companion sidebar.

Possible sidebar content:

- active opponent,
- HP/status,
- speed comparison,
- selected move damage range,
- weaknesses/resistances,
- next trainer/battle notes,
- small context-sensitive documentation hints.

This may be the most valuable DS-specific DualDex UX idea: **use the pillarbox space instead of shrinking or obscuring the actual DS game.**

**Bottom physical display (1240×1080):**

- Render the DS touch screen at the largest correct 4:3 size (approximately 1240×930),
- use the remaining strip for DualDex mode/navigation controls,
- map physical touch coordinates to the 256×192 DS stylus coordinate space.

---

## Mode B — Full Original DS

- Top display: DS top screen only.
- Bottom display: DS touch screen only.
- Companion UI hidden.

This should always be available when the user wants a completely traditional DS experience.

---

## Mode C — Companion Bottom Screen

- Top physical display continues showing the DS top screen plus optional sidecar.
- Bottom physical display switches from the emulated DS touchscreen to DualDex tabs:
  - Battle / Calc
  - Party
  - Docs
  - Trainers
  - Encounters
  - Planner

The DS bottom screen can be:

- hidden until toggled back,
- shown in a small picture-in-picture preview,
- or temporarily summoned with a controller shortcut.

A quick-toggle action is essential because many DS games require touchscreen interaction outside battle as well.

---

## Mode D — Battle Control Mode (advanced)

This is a later enhancement, not an initial DS requirement.

When DualDex has positively identified that Platinum is on the battle command/move screen, the bottom physical screen could present a custom move-selection interface showing richer information than the original DS UI:

```text
Thunderbolt
90 BP · Special · Electric
63–75% vs current target
[SELECT]
```

Selecting the move would **send normal emulated DS stylus/button input** to the game's corresponding UI coordinate rather than modify game memory directly.

Important safety/reliability rule:

- only enable profile-specific touch macros when the current game/UI state has been verified,
- never blindly send a touch coordinate because DualDex merely thinks the player is in battle,
- always keep a one-button path back to the real DS touchscreen.

This could later extend to battle actions such as Fight / Pokémon / Bag / Run, but should remain optional and profile-specific.

---

# Platinum Kaizo Integration Plan

## Phase 0 — Do nothing until the GBA beta is stable

Prerequisites:

- GBA `0.9.x` beta is in users' hands,
- save identity/storage schema is stable,
- emulator-core thread ownership is stable,
- engine/layout profiles are authoritative,
- the UI has been refactored enough that console-specific rendering is not baked into every screen.

Do not let this roadmap delay the GBA beta.

---

## Phase 1 — Multi-core frontend refactor

Goal: boot the existing GBA core through a generalized frontend without changing user-visible behavior.

Tasks:

- [ ] Introduce `EmulatorCoreSession` / core descriptor abstraction.
- [ ] Remove GBA framebuffer-size assumptions.
- [ ] Generalize audio timing/sample-rate handling.
- [ ] Implement core options/variables.
- [ ] Implement system/save/state directories.
- [ ] Implement libretro memory-map capture.
- [ ] Add pointer/touch input capability.
- [ ] Preserve emulator-thread command ownership.
- [ ] Keep mGBA as the regression test for the generalized host.

Success criterion: current GBA functionality behaves identically through the generic layer.

---

## Phase 2 — Nintendo DS core spike

Goal: prove basic DS emulation on real Thor hardware before writing Pokémon integration.

Test **vanilla Pokémon Platinum** first.

Prototype both:

- DeSmuME libretro,
- melonDS DS libretro.

Test:

- [ ] boot/reboot,
- [ ] battery save + restore,
- [ ] save states,
- [ ] audio,
- [ ] full-speed gameplay,
- [ ] frame pacing,
- [ ] suspend/resume,
- [ ] touch input,
- [ ] controller input,
- [ ] both DS screens,
- [ ] screen-layout changes,
- [ ] memory-map access,
- [ ] 60+ minute soak session.

Benchmark at least:

- an 8 Gen 2 Thor model,
- Thor Lite / Snapdragon 865 if access to one is practical.

### Core selection gate

Do not choose the production DS core until Platinum Kaizo itself passes battery-save tests.

Because the current Platinum Kaizo FAQ warns about melonDS-core saves, **DeSmuME should be treated as the compatibility baseline** even if melonDS DS benchmarks better.

---

## Phase 3 — Dual-display DS renderer

Goal: make DS gameplay feel native on Thor before adding companion features.

- [ ] Split/crop the libretro DS framebuffer into logical top and bottom images if the core returns a combined framebuffer.
- [ ] Render DS top screen on Thor top display.
- [ ] Render DS bottom screen on Thor bottom display.
- [ ] Correctly transform touchscreen coordinates.
- [ ] Add Full DS / Sidecar / Companion toggle modes.
- [ ] Handle display reconnect/sleep/orientation lifecycle.
- [ ] Make screen geometry data-driven so later DS/3DS cores can reuse the renderer.

---

## Phase 4 — Vanilla Platinum Gen 4 parser

Do this against an exact verified vanilla Platinum revision before Platinum Kaizo.

- [ ] Implement Gen 4 Pokémon decrypt/unshuffle/checksum parser.
- [ ] Read player party and party count.
- [ ] Read live HP/status/current stats.
- [ ] Identify explicit battle lifecycle.
- [ ] Resolve active player/enemy battlers.
- [ ] Read enemy battle data safely.
- [ ] Support singles first; doubles after singles are stable.
- [ ] Produce core-independent `PlayerSnapshot` and `BattleSnapshot` objects.
- [ ] Add fixtures/golden tests based on known Platinum states.

Use `pret/pokeplatinum` and documented Gen 4 structures as references rather than guessing layouts.

---

## Phase 5 — DS ROM data extraction / GameDataPack

Goal: make calculator and documentation data version-aware and mostly local.

- [ ] Build or adapt an `.nds` data reader.
- [ ] Parse relevant NARC/data archives.
- [ ] Extract species, moves, abilities, trainers, encounters, items and learnsets.
- [ ] Normalize into `GameDataPack`.
- [ ] Cache generated data by ROM SHA-256.
- [ ] Verify extracted vanilla Platinum data against known references.
- [ ] Compare architecture/research with existing Gen 4 tooling such as `hzla/ddex` before reinventing formats.

This stage is highly reusable for future DS ROM hacks.

---

## Phase 6 — Platinum Kaizo profile/version support

Treat Platinum Kaizo as a separate exact ROM version on top of the shared Platinum engine.

- [ ] Record exact supported Platinum Kaizo release/version/hash.
- [ ] Verify emulator-core compatibility for that exact hash.
- [ ] Generate/verify its `GameDataPack`.
- [ ] Validate hack-specific move/type/ability/item mechanics.
- [ ] Validate trainer sets and encounters.
- [ ] Validate level-cap and other relevant game rules.
- [ ] Locate/validate runtime party and battle structures for the exact build.
- [ ] Compare findings with the official Platinum Kaizo Lua sync implementation where useful.
- [ ] Never silently treat a future Kaizo update as the same supported build.

---

## Phase 7 — Platinum Kaizo calculator and docs

Platinum Kaizo already has an unusually mature tool ecosystem:

- official/master documentation,
- trainer/encounter/item sheets,
- official/community damage calculators,
- Gen 4 AI documentation,
- DeSmuME live-sync Lua tooling.

DualDex should use these as references, but its preferred final architecture should be local and version-aware.

Features:

- [ ] automatic player import from live memory,
- [ ] automatic active-opponent import,
- [ ] hack-accurate Gen 4 damage rules,
- [ ] trainer lookup by current/selected fight,
- [ ] searchable Pokémon/move/item docs,
- [ ] encounter tables,
- [ ] level-cap display,
- [ ] context-sensitive links to official/community documentation,
- [ ] offline operation for data available from `GameDataPack`.

Do not copy community calculator/docs datasets into DualDex without confirming licensing/permission. Prefer extracting static game data from the user's ROM where reasonable.

---

## Phase 8 — Advanced Thor-specific DS companion UX

Only after the underlying emulator and parser are reliable:

- [ ] top-screen battle sidecar in the 16:9 pillarbox region,
- [ ] configurable sidecar width/content,
- [ ] bottom-screen Game / Calc / Docs / Trainer / Party toggle,
- [ ] controller shortcut to instantly restore DS bottom screen,
- [ ] optional picture-in-picture DS bottom screen while browsing docs,
- [ ] custom move-selection/battle controls using normal stylus input,
- [ ] pre-battle trainer preview/planning screen,
- [ ] automatic context switching that can always be disabled.

The game itself must remain fully playable without any companion automation.

---

## Phase 9 — General DS support

Once Platinum / Platinum Kaizo proves the architecture:

Potential next targets could include:

- other Platinum hacks,
- HeartGold/SoulSilver and hacks based on them,
- Black/White and Black 2/White 2 later,
- non-Pokémon DS games using emulator-only mode even without companion integration.

At that point the Library should identify both **system** and **game integration status**:

```text
Pokémon Platinum Kaizo
Nintendo DS · Platinum engine
Verified integration
Core: DeSmuME (example)

Unrecognized DS Game
Nintendo DS
Emulation supported · No companion integration
```

---

# Save / Version Model

Multi-system support makes the ROM identity work planned for the GBA beta even more important.

Recommended long-term identity:

```text
GameIdentity
- systemId
- contentSha256
- gameProfileId
- versionProfileId
```

Storage should be system/core-aware without making the emulator core part of the user's game identity:

```text
saves/<system>/<rom-hash>/battery.*
states/<system>/<rom-hash>/<core-id>/...
data/<system>/<rom-hash>/game-data-pack.*
```

Battery saves may sometimes migrate between cores; save states usually should be considered core/version-specific unless proven compatible.

---

# Licensing / Distribution Gate

This must be resolved **before any DS core is bundled in a public DualDex build**.

Current relevant licenses:

- libretro API: MIT
- mGBA: MPL-2.0
- DeSmuME libretro: GPLv2
- melonDS DS libretro: GPLv3+

DualDex is currently MIT licensed.

GPL software can be used commercially, but bundling/linking/distributing a GPL core can introduce source-code and license obligations for the distributed work. **Do not assume that `dlopen()` or a plugin boundary automatically avoids those obligations.**

Before choosing the shipping model:

- review the exact core license and dependency licenses,
- document core source/version/build provenance,
- determine the compliant source-distribution model,
- preserve required notices/licenses,
- ensure any future Play Store or monetized build remains license-compliant.

This is a licensing review item, not legal advice.

Also never bundle Nintendo ROMs, BIOS, firmware, NAND data, or other proprietary system files.

---

# Major Risks

## 1. Core compatibility divergence

A DS game may work in one core and not another. Game integrations must therefore be separated from core-specific behavior as much as possible.

## 2. Platinum overlays / version drift

Static absolute RAM offsets are especially fragile on Nintendo DS. Version hashes, engine profiles, memory-map metadata and validation are mandatory.

## 3. Touchscreen UX complexity

Replacing the original bottom screen with companion UI is useful only if returning to the real DS touchscreen is instant and predictable.

## 4. Scope explosion

DS support can easily turn into a general emulator rewrite. The first milestone should be narrowly defined:

> **Vanilla Platinum runs reliably on both Thor screens through a generalized DualDex core host.**

Only then add live Pokémon parsing.

## 5. Licensing

The technically best emulator core may not be the simplest core to distribute under DualDex's current project licensing. Resolve this before release packaging decisions.

---

# Recommended First Deliverable

When this roadmap eventually begins, do **not** start with calculator UI or Platinum Kaizo memory offsets.

Create a separate experimental branch whose only goal is:

> **Boot vanilla Pokémon Platinum through a DS libretro core, render the real top and bottom DS screens on the two AYN Thor displays, provide working touch/controller input, and safely save/restore the game.**

If that prototype is clean, the rest of DualDex's existing companion architecture becomes reusable.

---

# Reference / Research Links

These sources informed this roadmap and should be rechecked when implementation starts because emulator cores and Platinum Kaizo can change over time.

### Emulator / libretro

- melonDS DS libretro documentation: https://docs.libretro.com/library/melonds_ds/
- melonDS DS core repository: https://github.com/JesseTG/melonds-ds
- DeSmuME libretro documentation: https://docs.libretro.com/library/desmume/
- Libretro core development overview: https://docs.libretro.com/development/cores/developing-cores/
- Libretro license reference: https://docs.libretro.com/development/licenses/

### Pokémon Platinum / Gen 4 reverse engineering

- pret Pokémon Platinum decompilation: https://github.com/pret/pokeplatinum
- Project Pokémon Gen 4 PKM structure documentation: https://projectpokemon.org/home/docs/gen-4/pkm-structure-r65/

### Platinum Kaizo ecosystem

- Platinum Kaizo documentation/resources: https://platinumkaizo.com/documentation-resources/
- Platinum Kaizo FAQ / emulator notes: https://platinumkaizo.com/frequently-asked-questions/
- Community resource hub: https://emi.dev/platinum-kaizo/
- Official Platinum Kaizo calculator source: https://git.anastarawneh.com/anas/PKCalc
- Platinum Kaizo live-sync Lua tool: https://github.com/anastarawneh/PKLuaScript
- Gen 4 ROM data/calc tooling reference (`ddex`): https://github.com/hzla/ddex

### AYN Thor

- AYN Thor product page/specifications: https://www.ayntec.com/products/ayn-thor

---

# Relationship to the Current Beta

This roadmap should stay **outside the current release checklist's beta exit criteria**.

The current priority remains:

1. protect GBA saves,
2. stabilize the current mGBA core lifecycle,
3. make ROM/version detection trustworthy,
4. make the existing GBA companion/calculator accurate,
5. release and learn from the AYN Thor beta.

DS support should begin only after those foundations are proven, because the same work—ROM identity, core ownership, versioned engine layouts and generic snapshots—is exactly what will make this roadmap much easier later.
