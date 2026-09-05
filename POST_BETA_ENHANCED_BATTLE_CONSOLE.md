# DualDex Post-Beta Enhanced Battle Console

> **First major post-beta feature candidate. This is not part of the `0.9.0-beta.1` release scope.**
>
> Build this only after the current GBA beta is stable enough that battle lifecycle, ROM/version profiles, calculator rules, controller actions, and save/core ownership are trustworthy.

## Summary

For systems with only one original game surface—especially **GB/GBC/GBA**, and eventually Switch—DualDex can use the Thor's second physical display as an optional **Enhanced Battle Console**.

Instead of showing only passive companion information, the bottom display can become a modernized battle interface inspired by later Pokémon games while still controlling the original game through normal emulator input.

The key principle is:

> **DualDex enhances the interface around the game; it does not need to patch or write battle decisions directly into game memory.**

The original game remains authoritative. When the user taps a move or chooses a party member in DualDex, the app should send the same controller inputs the user would otherwise enter manually.

This is technically feasible and is a strong candidate for the **first major feature after the GBA public beta**, because it builds directly on work already required for the beta: reliable battle snapshots, profile-aware mechanics, exact ROM identity, controller actions, and calculator integration.

---

# Why This Should Come Before Multi-System Expansion

The future platform roadmap eventually adds GB/GBC, DS, 3DS, and Switch support. The Enhanced Battle Console should come first because it provides immediate value on the existing GBA foundation while forcing several abstractions to become mature enough for every later generation.

It exercises:

- reliable battle lifecycle detection,
- active attacker/defender tracking,
- complete move metadata,
- field-state parsing,
- stat-stage parsing,
- version-aware damage calculations,
- controller/action routing,
- safe UI-state detection,
- generic battle snapshots,
- and a reusable modern battle UI.

If this is designed generically, later Pokémon integrations can provide the same data and reuse the same interface.

---

# Core UX Concept

For GBA and other single-game-surface platforms:

```text
TOP DISPLAY
┌────────────────────────────────────────────────────────────┐
│                                                            │
│                         GAME                               │
│                                                            │
│                  normal emulator output                    │
│                                                            │
└────────────────────────────────────────────────────────────┘

BOTTOM DISPLAY — ENHANCED BATTLE CONSOLE
┌────────────────────────────────────────────────────────────┐
│ Your Pokémon                         Opponent               │
│ Charizard Lv.50   143/156 HP         Venusaur Lv.50        │
│ +1 SpA  -1 Def                       Poisoned              │
│                                                            │
│ ┌───────────────────┐  ┌───────────────────┐              │
│ │ Flamethrower      │  │ Air Slash         │              │
│ │ SUPER EFFECTIVE   │  │ EFFECTIVE         │              │
│ │ 90 BP · 100%      │  │ 75 BP · 95%       │              │
│ │ 58–69%            │  │ 31–37%            │              │
│ └───────────────────┘  └───────────────────┘              │
│ ┌───────────────────┐  ┌───────────────────┐              │
│ │ Dragon Claw       │  │ Roost             │              │
│ │ NOT VERY EFFECTIVE│  │ STATUS            │              │
│ │ 80 BP · 100%      │  │ Restore HP        │              │
│ └───────────────────┘  └───────────────────┘              │
│                                                            │
│ Battle │ Party │ Field │ Details                           │
└────────────────────────────────────────────────────────────┘
```

The exact visual design should follow the eventual DualDex design system rather than clone a specific official Pokémon UI.

---

# Capability Levels

Do not make the entire feature all-or-nothing. A ROM/profile should advertise the highest verified battle-console capability it supports.

## Level A — Enhanced Move Display

Requires reliable static game data but not complete live battle memory.

Can show:

- move name,
- type,
- physical / special / status category,
- base power,
- accuracy,
- current/max PP where available,
- priority,
- move description,
- important secondary effects,
- type-only effectiveness against a manually or reliably identified target.

This can work for more hacks because most of the data can come from `GameDataPack`.

## Level B — Verified Live Battle Assistant

Requires an exact supported ROM/version and trusted `BattleSnapshot`.

Adds:

- automatic active opponent,
- actual damage range,
- speed comparison,
- current HP/status,
- active stat stages,
- weather,
- terrain where applicable,
- Reflect / Light Screen / Aurora Veil equivalents where supported,
- Tailwind-style side effects,
- Trick Room-style global effects,
- turn counters when the game exposes enough state to calculate them safely,
- special mechanics such as Mega/Z/Dynamax/Tera where relevant in later generations or hacks.

## Level C — Verified Interactive Battle Controls

Requires reliable battle-UI state detection in addition to Level B.

Adds:

- tap one of four moves to select it in the original game,
- party screen with switch controls,
- optional Fight / Pokémon / Bag / Run-style controls where safe,
- controller focus/navigation through the enhanced interface.

DualDex should send **normal emulated controller/touch input**, not directly edit the game's battle state.

---

# Modern Move Information

The move buttons should provide information that older games and many hacks do not display themselves.

Recommended presentation:

```text
MOVE NAME
Type · Category
BP · Accuracy · PP
Effectiveness label
Optional predicted damage range
Short effect summary
```

Possible effectiveness labels:

- **No Effect** — 0×
- **Not Very Effective** — below 1×
- **Effective** — 1×
- **Super Effective** — 2×
- **Extremely Effective** — 4× or greater, if DualDex chooses to expose an additional high-multiplier label

The UI should distinguish **type-chart effectiveness** from a **fully verified battle calculation**.

For example, a move may look super effective by typing but still fail because of an ability, immunity, field effect, item, or hack-specific mechanic. Do not present the stronger label as authoritative unless the current profile/calculator understands those mechanics.

Recommended internal result:

```text
MoveRecommendationInfo
- moveId
- displayType
- category
- basePower
- accuracy
- pp
- priority
- shortDescription
- typeEffectiveness
- calculatedDamageRange?
- targetHpPercentRange?
- calculationConfidence
- relevantWarnings
```

`@smogon/calc` already accepts generation-specific attacker, defender, move, boosts and field state and returns damage ranges/results, making it suitable as the basis for the richer verified view once DualDex supplies accurate profile data.

---

# Move Details View

Tapping or long-pressing a move should optionally open a details panel instead of immediately selecting it.

Suggested information:

- move type,
- physical / special / status,
- base power,
- accuracy,
- PP,
- priority,
- target type,
- contact flag,
- relevant move flags,
- short description,
- secondary-effect chance,
- recoil/drain where applicable,
- multi-hit behavior where applicable,
- current target effectiveness,
- predicted damage range,
- notable battle modifiers currently affecting the result.

Avoid dumping implementation-level calculator internals into the normal view. Advanced explanation can live behind a Details action.

---

# Battle Status / Field View

The enhanced console should include a dedicated **Field** or **Status** view that summarizes battle state the original game may make difficult to inspect.

Example:

```text
FIELD
Rain                         3 turns
Trick Room                   2 turns

YOUR SIDE
Tailwind                     1 turn
Reflect                      4 turns

YOUR CHARIZARD
Attack                       +0
Defense                      -1
Sp. Atk                      +1
Sp. Def                      +0
Speed                        +2
Accuracy                     +0
Evasion                      +0

OPPONENT VENUSAUR
Attack                       -1
Defense                      +2
...
```

### Important rule for turn counters

Only show an exact remaining-turn count when DualDex can derive it reliably for that exact engine/version.

Different games/hacks may represent effects differently. Some may expose a remaining-turn counter directly; others may require DualDex to observe activation and decrement it from verified battle transitions.

The fallback should be:

```text
Rain — active
```

not an invented turn count.

---

# Battle Snapshot Expansion

The long-term generic `BattleSnapshot` should become rich enough to power this interface across generations.

Recommended shape:

```text
BattleSnapshot
- isInBattle
- battleFormat
- battlePhase
- turnNumber?
- playerBattlers[]
- enemyBattlers[]
- playerActivePartyIndexes[]
- enemyActivePartyIndexes[]
- weather
- terrain
- globalEffects[]
- playerSideEffects[]
- enemySideEffects[]
- specialMechanicState
- uiState?
- validity/confidence metadata
```

Each active battler should include battle-only state:

```text
BattlerSnapshot
- partyIndex
- species/form
- current/max HP
- status
- moves/PP
- effective ability/item where known
- statStages
  - atk
  - def
  - spa
  - spd
  - spe
  - accuracy
  - evasion
- volatile states where useful and safely readable
```

Field effects should be normalized:

```text
FieldEffectSnapshot
- effectId
- scope: GLOBAL | PLAYER_SIDE | ENEMY_SIDE | BATTLER
- active
- remainingTurns?
- source?
- confidence
```

This allows a GBA hack, Platinum, Scarlet/Violet, etc. to map their own internal structures into the same companion UI.

---

# Battle UI State Model

Interactive controls require more than knowing that a battle exists.

DualDex needs to know what the original game is currently expecting.

Recommended abstraction:

```text
BattleUiSnapshot
- state
  - COMMAND_MENU
  - MOVE_MENU
  - PARTY_MENU
  - TARGET_SELECT
  - BAG_MENU
  - ANIMATION_OR_TEXT
  - UNKNOWN
- selectedMoveIndex?
- selectedPartyIndex?
- validActions[]
- confidence
```

Enhanced controls should only become clickable when the state is positively verified.

If the game transitions unexpectedly, DualDex should immediately stop sending macro input and return control to the normal emulator.

---

# Input Strategy

## Do not write battle choices directly to RAM

The preferred implementation is:

```text
Enhanced Battle UI
      ↓
Verified Battle Action
      ↓
ControllerActionRouter
      ↓
Normal D-pad / A / B input sequence
      ↓
Emulator core
      ↓
Original game handles the choice
```

This keeps DualDex closer to an alternate controller/UI than a game-state editor.

## Move selection

For a verified game/profile, the adapter can map an enhanced move index to the controller sequence needed by that game's move menu.

Do not assume the cursor always starts in one position unless the current UI state/cursor is known.

Possible profile API:

```text
BattleInputAdapter
- selectMove(slot)
- openParty()
- choosePartyMember(slot)
- confirmSwitch(slot)
- cancel()
```

The implementation can emit ordinary emulator input commands through the core command/input layer.

## Party / switching

The Party tab can initially be **view-only**:

- six party members,
- HP/status,
- level,
- typing,
- known moves,
- key stat-stage context for the currently active Pokémon.

Interactive switching should be added only after the game-specific party-menu flow is robust.

A tap on a party member should never blindly emit input while the game is in an unknown menu state.

---

# Compatibility with ROM Hacks

This feature should inherit DualDex's exact-version compatibility model.

A profile should declare features independently:

```text
BattleConsoleCapabilities
- moveMetadata: VERIFIED | PARTIAL | NONE
- typeEffectiveness: VERIFIED | TYPE_ONLY | NONE
- damageCalculation: VERIFIED | UNVERIFIED | NONE
- statStages: VERIFIED | NONE
- fieldEffects: VERIFIED | PARTIAL | NONE
- turnCounters: VERIFIED | ACTIVE_ONLY | NONE
- moveSelectionControl: VERIFIED | NONE
- partySwitchControl: VERIFIED | NONE
```

This is important because a FireRed hack may:

- use the same party structure,
- change the type chart,
- add Fairy,
- use a physical/special split,
- add abilities/items,
- completely replace battle structs,
- or change the menus.

The enhanced UI should degrade feature-by-feature rather than pretending the entire hack is equivalent to vanilla FireRed.

---

# Profile / Data Responsibilities

## `GameDataPack`

Provides mostly static information:

- move names,
- types,
- categories,
- power,
- accuracy,
- PP,
- descriptions,
- move flags/effects,
- species/types,
- abilities/items,
- type chart,
- mechanics metadata.

## Engine/Layout Profile

Provides runtime mappings:

- battle lifecycle,
- battler structures,
- active party indexes,
- stat stages,
- field effects,
- effect timers,
- menu/UI state,
- cursor/selection state where required.

## Calculator Rules

Defines how the current game interprets the data:

- generation mechanics,
- physical/special behavior,
- type chart,
- abilities/items,
- weather/terrain,
- screens,
- critical hits,
- special mechanics,
- hack-specific overrides.

---

# Recommended First Post-Beta Implementation Sequence

## Phase EBC-0 — Prerequisites

Do not begin interactive controls until the relevant beta foundations are complete.

Required first:

- [ ] Battle lifecycle / active battler issue is reliable.
- [ ] Emulator core mutations/actions are serialized safely.
- [ ] Unsupported/unverified ROMs fail safely.
- [ ] Engine/layout profiles are authoritative.
- [ ] Calculator rules are profile/version aware.
- [ ] Controller action routing exists and advertised shortcuts work.

---

## Phase EBC-1 — Read-only modern battle screen

Goal: deliver immediate value without automating the original game's UI.

- [ ] Create four-move battle layout.
- [ ] Show move type/category/BP/accuracy/PP.
- [ ] Add short move description/details sheet.
- [ ] Show type-chart effectiveness labels.
- [ ] Show verified damage ranges where available.
- [ ] Show active attacker/opponent summary.
- [ ] Add Party tab as read-only summary.
- [ ] Add Field/Status tab.

This should be the first shippable iteration.

---

## Phase EBC-2 — Rich live battle state

- [ ] Expand `BattleSnapshot` with stat stages.
- [ ] Add side/global field effects.
- [ ] Add remaining-turn information where verified.
- [ ] Add speed comparison.
- [ ] Add relevant ability/item/modifier explanation.
- [ ] Add doubles-ready battler representation even if the first UI remains singles-focused.
- [ ] Add golden tests for battle-state transitions.

Suggested first target: one exact vanilla FireRed or Emerald revision, followed by one verified modern-mechanics hack.

---

## Phase EBC-3 — Move-selection controls

- [ ] Implement `BattleUiSnapshot`.
- [ ] Reliably detect move menu vs text/animation/other states.
- [ ] Add `BattleInputAdapter` for one verified profile.
- [ ] Tap enhanced move → send ordinary controller sequence.
- [ ] Disable enhanced controls immediately if UI confidence is lost.
- [ ] Provide instant return to normal game controls.
- [ ] Stress-test rapid input, animations, faint/send-out transitions, and move-learning/menu interruptions.

---

## Phase EBC-4 — Party switching

- [ ] Detect/open party menu safely.
- [ ] Map party slots to original game navigation.
- [ ] Show switch legality/status.
- [ ] Tap party member → verified normal input flow.
- [ ] Handle fainted Pokémon and forced-switch states.
- [ ] Handle doubles separately.

Do not combine this with EBC-3 if doing so delays getting reliable move selection into users' hands.

---

## Phase EBC-5 — Cross-generation reuse

Once the GBA version is stable, make this UI consume only generic interfaces:

```text
BattleSnapshot
GameDataPack
CalculatorRules
BattleUiSnapshot
BattleInputAdapter
```

Then later systems can reuse it:

- **GB/GBC:** enhanced controls on Thor bottom screen.
- **DS/3DS:** optional companion battle view when the original touch screen is toggled away.
- **Switch:** ideal permanent bottom-screen battle console.

The original console's UI remains available at all times.

---

# Potential Quality-of-Life Features After the Core Version

These should not be in the first iteration:

- optional damage-roll visualization,
- KO probability where correctly calculable,
- speed-order indicator,
- expandable explanation of why a move's damage changed,
- recent-turn history,
- predicted trainer set comparison,
- quick access to encounter/trainer docs,
- user-configurable battle-console layout,
- controller-only focus mode,
- optional automatic switch between Party / Battle / Field views based on verified game state.

Avoid turning the screen into an overloaded competitive-battle dashboard. The first job is to make ordinary play clearer and faster.

---

# Testing Strategy

For every profile claiming interactive Enhanced Battle Console support, test at minimum:

- [ ] entering and leaving battle,
- [ ] all four move positions,
- [ ] disabled/0-PP moves,
- [ ] move descriptions/data accuracy,
- [ ] 0× / 0.25× / 0.5× / 1× / 2× / 4× effectiveness where supported,
- [ ] abilities/items that alter apparent effectiveness,
- [ ] player switch,
- [ ] opponent switch,
- [ ] faint + forced replacement,
- [ ] status conditions,
- [ ] every supported stat stage,
- [ ] weather start/end,
- [ ] side-effect start/end,
- [ ] turn-counter accuracy,
- [ ] move-selection control during normal battle,
- [ ] controls disabled during animations/text,
- [ ] party-switch controls,
- [ ] doubles if advertised,
- [ ] fast-forward,
- [ ] controller + touchscreen mixed input,
- [ ] pause/resume during battle.

Interactive control bugs are more serious than display-only bugs because a wrong input can cause the player to choose the wrong move or switch. Treat automation confidence as a release gate.

---

# Success Criteria for the First Release

The first public Enhanced Battle Console release should be narrow.

A good definition of done is:

> **On one or more exact verified GBA Pokémon builds, the Thor bottom display can show the player's four current moves with accurate metadata, effectiveness and profile-aware damage information; show trustworthy active battle/field state; and optionally select a move through normal emulator input without accidental commands when the original game is not on the move-selection screen.**

Party switching can follow as the next increment.

---

# Relationship to the Platform Roadmap

Recommended post-beta priority becomes:

1. **Enhanced Battle Console on the existing GBA platform.**
2. Generalize the finished GBA abstractions into the multi-system architecture.
3. Gen 1/2 through mGBA.
4. Nintendo DS + vanilla Platinum.
5. Platinum Kaizo.
6. Gen 5.
7. 3DS / Gen 6–7.
8. Switch / Gen 8–9.

This feature should therefore be treated as the bridge between the initial GBA beta and the broader Gen 1–9 roadmap, not as beta scope creep.

---

# Research / Technical References

- Smogon damage calculator / `@smogon/calc`: https://github.com/smogon/damage-calc
- Existing DualDex future platform roadmap: `FUTURE_PLATFORM_ROADMAP.md`
- Existing beta/release plan: `RELEASE_CHECKLIST.md`

The upstream calculator is designed as a programmatic damage engine across Pokémon generations and accepts attacker/defender boosts plus field/side state, which aligns well with the proposed richer `BattleSnapshot` model.
