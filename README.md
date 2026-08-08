# ZombieZ Companion — V2

Client-side, read-only quality-of-life companion mod for the **ZombieZ** Minecraft server.
Provides modular overlays and helpers (minimap, waypoints, event beacons/timers, HUD, etc.);
it never automates gameplay.

- **Minecraft:** 1.21.4
- **Mod loader:** Fabric Loader ≥ 0.16.9
- **Java:** 21
- **Mappings:** Yarn (`net.fabricmc.yarn` — see `gradle.properties`)
- **Build:** Fabric Loom 1.9.2

## Building

```bash
./gradlew build
```

The packaged mod jar is written to `build/libs/`.

## What's new in V2

This is a full rewrite that adds support for the server's second, instanced map
(zones 51+, dimension `minecraft:world2`) alongside the primary map
(zones 1–50, dimension `minecraft:overworld`):

- **Second map / instanced world support.** Events and waypoints are now aware of which
  map they belong to. Map 2 has a single refuge (*La Cité Brumeuse*) reached via
  `refuge tp w2`.
- **New merchant & world boss chat formats.** Handles the updated *Super Marchand* spawn
  header, the coordinates/timer line, and the new departure message, plus zone parsing
  (`Zone N` / `Localisé en Zone N`) to route each event to the correct map.
- **Dimension tag on waypoints.** Every waypoint — automatic *and* manually placed —
  stores its dimension and is only shown in that dimension. Waypoints saved before this
  change (no dimension) stay visible everywhere for backward compatibility.
- **Smarter quick-teleport.** Uses the active event's dimension (or the player's current
  dimension when no event is active) to choose between `refuge tp <n>` (map 1) and
  `refuge tp w2` (map 2).
- **Debug logging** for chat parsing and quick-tp, gated behind the existing `debugMode` flag.

## Repository layout

- `src/main/java` — mod sources (Yarn-named).
- `src/main/resources` — `fabric.mod.json`, mixins, lang files, and map tile assets.
- `patch-kit/` — reference material used for the rewrite (diagnostic notes and the
  decompiled sources of the original 1.0.2 build).

## License

MIT — see [LICENSE](LICENSE).
