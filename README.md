# DualDex ⚡

**DualDex** is an open-source, dual-screen Game Boy Advance emulator and real-time companion application built specifically for the **AYN Thor** handheld running Android.

By utilizing the AYN Thor's secondary $3.92''$ $1240 \times 1080$ AMOLED display, DualDex turns the bottom screen into a live companion Pokédex, battle calculator, type matchup engine, and AI assistant—all synchronized in real time with the running GBA game via direct memory reading.

```
┌─────────────────────────────────────────────────────────────┐
│                 TOP SCREEN: 6.0" AMOLED (1920x1080)         │
│  ┌───────────────────────────────────────────────────────┐  │
│  │                                                       │  │
│  │            mGBA Emulation (3:2 Native Ratio)          │  │
│  │            Retro Shaders: CRT / LCD / Sharp Bilinear  │  │
│  │                                                       │  │
│  └───────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                               ▲
                 Shared Memory │ (Zero-Copy EWRAM Polling)
                               ▼
┌─────────────────────────────────────────────────────────────┐
│               BOTTOM SCREEN: 3.92" AMOLED (1240x1080)        │
│  ┌───────────────────────────────────────────────────────┐  │
│  │  👥 Party  │  ⚔️ Calc  │  🛡️ Types  │  📖 Docs  │  🤖 LLM │  │
│  │───────────────────────────────────────────────────────│  │
│  │  🔥 Blaziken Lv.67 ♂              HP: 198/214 ███████ │  │
│  │  Nature: Adamant (+Atk, -SpA)     Item: Life Orb      │  │
│  │  IVs: 31/31/28/31/14/31           EVs: 4/252/0/252/0/0│  │
│  │  Known Moves: Flare Blitz, Close Combat, Swords Dance │  │
│  │  ⚠️ Weak: Water, Ground, Flying, Psychic (2x)         │  │
│  └───────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

---

## ✨ Features

- **🎮 mGBA Emulation Core**:
  - Embedded `mgba_libretro.so` cross-compiled for `arm64-v8a` (AYN Thor) and `x86_64`.
  - OpenGL ES 2.0 textured quad with exact GBA $3:2$ pixel aspect ratio correction.
  - Low-latency $32,768\text{ Hz}$ stereo audio streaming via Android `AudioTrack`.
  - Fast-forward speed toggle ($1\times, 2\times, 3\times, 4\times$).

- **📺 Retro Display Shaders**:
  - **Pixel Perfect (Nearest)**: Crisp 1:1 raw pixel art.
  - **Sharp Bilinear**: Smooth, integer-scale pixel boundary anti-aliasing.
  - **GBA LCD Grid**: Authentic Game Boy Advance TFT LCD subpixel matrix simulation.
  - **CRT Scanlines**: Retro phosphor scanline darkening.

- **🧠 Real-Time Gen 3 Memory Parser**:
  - Zero-copy EWRAM pointer extraction (`retro_get_memory_data()`).
  - Native C Gen 3 decryption (`PID ^ OTID` XOR key) and 24-permutation substructure unscrambling.
  - 16-bit checksum verification to prevent flickering during battle transitions.
  - Live $10\text{Hz}$ background polling loop reading both player party and active in-battle opponent.

- **📱 Adaptive Companion UI (Bottom Screen)**:
  - **Ghost Grey Mode**: Automatically hides IVs and EVs, displays flat base stats, and applies pre-Gen 6 Steel type chart overrides (Steel resisting Ghost and Dark).
  - **Radical Red / Vanilla Mode**: Shows exact 0–31 IVs, 0–252 EVs, and total EV counters.
  - **Type Matchup Matrix**: 18-type dual-typing defense breakdown ($4\times, 2\times, 0.5\times, 0.25\times, 0\times$).
  - **Save State Manager**: 5 multi-slot save states with timestamps, quicksave/quickload, and auto-save on pause.

- **⚔️ Embedded Damage Calculator**:
  - Full `@smogon/calc` engine embedded via native **QuickJS-NG** C runtime.
  - Sub-millisecond calculation ($< 0.8\text{ms}$) with zero network dependencies.
  - Defender search with autocomplete, field conditions (Sun, Rain, Sand, Hail, Reflect, Light Screen, Critical Hits), and automatic defender pre-fill when an opponent Pokémon appears in battle.

- **🤖 ROM Hack Assistant (Gemini LLM + Google Search Grounding)**:
  - Integrated with **Google Gemini 2.5 Flash** with live **Google Search Grounding**.
  - Injects active ROM title, base game, engine rules, and current party composition into prompt.
  - Clickable citation links (`[🔗 PokeCommunity]`, `[🔗 Reddit]`) for Google API TOS compliance.
  - Built-in smart offline knowledge base for item locations (Fly, Surf, Exp Share) and Ghost Grey custom regional evolutions (*Lichtoise*, *Spectrasaur*, *Phantomander*).

---

## 🎮 AYN Thor Physical Controller Mapping

| Handheld Control | DualDex Function |
|---|---|
| **D-Pad / Left Stick** | GBA Directional Movement |
| **Button A** | GBA Button A (Confirm / Talk) |
| **Button B** | GBA Button B (Cancel / Run) |
| **Button X / Y** | Menu Shortcut / Fast-Forward Toggle |
| **L1 / R1** | GBA Left / Right Shoulder Triggers |
| **L2 / R2** | Quick Save State (L2) / Quick Load State (R2) |
| **Start / Select** | GBA Start / Select Buttons |
| **Bottom Screen Touch** | Navigate companion tabs, tap moves to calculate, search Pokémon |

---

## 📦 Supported ROM Hacks

| Hack | Base Game | Engine | EV / IV Support | Custom Features |
|---|---|---|---|---|
| **Pokemon Ghost Grey** | FireRed v1.0 | HexManiacAdvance | Flat Stats (No EVs/IVs) | Steel resists Ghost/Dark, 120+ regional species |
| **Pokemon Radical Red** | FireRed v1.0 | CFRU | Full EVs & IVs | Gen 9 split, Mega Evolutions, `dex.radicalred.net` docs |
| **Vanilla FireRed** | FireRed v1.0 | Vanilla | Full EVs & IVs | Standard Gen 3 rules |
| **Vanilla Emerald** | Emerald v1.0 | Vanilla | Full EVs & IVs | Standard Gen 3 rules |

---

## 🛠️ Building & Running

### Requirements
- Android SDK Platform 34 (`android-34`)
- Android NDK `27.2.12479018`
- CMake `3.22.1`
- OpenJDK 17

### Build Commands
```bash
# Clone repository
git clone https://github.com/dualdex/dualdex.git
cd dualdex

# Run native C memory parser and damage calc test suites
gcc -O2 -I native/include native/src/pokemon_reader.c native/src/pokemon_text.c native/tests/test_pokemon_reader.c -o native/test_runner && ./native/test_runner

# Run Kotlin unit tests
./gradlew testDebugUnitTest

# Assemble Debug APK (outputs to app/build/outputs/apk/debug/app-debug.apk)
./gradlew assembleDebug
```

---

## 📚 Documentation
- [ARCHITECTURE.md](ARCHITECTURE.md) — Technical deep-dive on Libretro host, zero-copy memory access, and QuickJS integration.
- [CONTRIBUTING.md](CONTRIBUTING.md) — Step-by-step guide to adding new ROM hack JSON profiles.

---

## 📄 License

DualDex is licensed under the [MIT License](LICENSE).  
The mGBA Libretro core is licensed under the [Mozilla Public License 2.0 (MPL-2.0)](https://www.mozilla.org/MPL/2.0/).
