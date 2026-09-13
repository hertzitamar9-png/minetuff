# MineTuff

MineTuff is a Java 21 / Paper 1.21.11 mining-server core designed for Folia's regionized scheduler.

## Current gameplay

- 100 progression worlds generated from 20 theme families across 5 stages.
- Every world has its own deterministic materials, economy multiplier, prestige gate and structural decoration signature.
- 64 mining pods per world, spaced across regions so active groups can be scheduled independently by Folia.
- 64x32x64 resettable box mines with weighted ore progression and automatic reset at a configurable depletion percentage.
- Physical previous/next world portal pads plus `/mine [world]` travel.
- Money, tokens, backpack storage, `/sellall`, tool upgrades, world unlock costs and persistent YAML profiles.
- Prestige progression with permanent sell multipliers and repeatable 10-world milestones.
- Four mining modes: pickaxe, hammer, drill and laser.
- Common, rare and epic crate keys from mining plus token shop purchases and randomized rewards.
- `/daily`, `/stats`, `/balance`, `/worlds`, `/shop`, `/crate`, `/tool` and live mining HUD.
- Configured and enforced hard cap of 1,000 players per mining world using concurrent teleport reservations.

## Build

Requirements: Java 21 and Maven 3.9+.

```powershell
mvn -B -ntp verify
```

The plugin JAR is created under `target/`. GitHub Actions runs the same verification on every push and uploads the plugin JAR as an artifact.

## Prepare a playable Folia server on Windows

```powershell
.\scripts\setup-server.ps1
```

The setup script builds MineTuff, downloads the latest stable Folia build for Minecraft 1.21.11 through PaperMC's supported downloads API, creates `runtime/`, installs the plugin, copies the server properties and creates `runtime/eula.txt` with `eula=false`.

Read Mojang's EULA yourself. If you accept it, change `runtime/eula.txt` to `eula=true`, then start with:

```powershell
.\scripts\start-server.ps1 -MinRamGB 8 -MaxRamGB 16
```

For large load tests, use dedicated hardware and tune RAM/CPU based on measured region utilization rather than increasing heap blindly.

## Scale architecture

`max-players-per-world: 1000` is an enforced gameplay capacity. It is not a claim that 1,000 real clients have already been load-tested. MineTuff spreads players across 64 pods separated by 256 blocks and performs mine generation, reset work and multi-block mining with Folia region ownership. A release intended to advertise 1,000 concurrent players in one world still needs a real bot/client load test on the target production hardware.

## Local handcrafted worlds

The original Windows workspace contains separate handcrafted map/schematic projects created before this repository runtime was bootstrapped. Those files are not currently present in this GitHub repository. They should be imported as authored world templates after the original local workspace is reachable again; the procedural 100-world runtime works independently of them.
