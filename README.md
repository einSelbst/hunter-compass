# HunterCompass

[![Latest release](https://img.shields.io/github/v/release/einSelbst/hunter-compass?label=release)](https://github.com/einSelbst/hunter-compass/releases/latest)
[![Build](https://github.com/einSelbst/hunter-compass/actions/workflows/build.yml/badge.svg?branch=main)](https://github.com/einSelbst/hunter-compass/actions/workflows/build.yml)
[![Paper 1.21.11](https://img.shields.io/badge/Paper-1.21.11-3498db)](https://papermc.io/)
[![Java 21](https://img.shields.io/badge/Java-21-f89820)](https://adoptium.net/)
[![MIT License](https://img.shields.io/badge/license-MIT-green)](LICENSE)

**A dimension-aware tracking compass for Paper hunting events.**

HunterCompass gives every hunter a personal compass and continuously shows the
target player's exact block coordinates and dimension in the action bar. The
target is never outlined, made visible through walls, or enrolled as a hunter.
Everything runs on the server; players need no client mod or resource pack.

## Features

- Exact X, Y, and Z block coordinates in a persistent, spam-free action bar
- Accurate compass direction while hunter and target share a dimension
- Honest Overworld/Nether portal projection using Minecraft's 8:1 coordinate ratio
- No invented direction for The End or unrelated custom dimensions
- Persistent event target and hunter list across server restarts
- Automatic enrollment for eligible players who join during an active event
- Owner-bound compasses marked with PersistentDataContainer data
- Idempotent compass issuing: repeated commands remove extras instead of duplicating them
- Configurable update rate, item text, action-bar text, dimension names, and behavior
- Winner announcement and optional automatic stop when a hunter kills the target
- Admin commands with permissions and tab completion

## Requirements

| Component | Requirement |
| --- | --- |
| Server | Paper 1.21.11 |
| Java | 21 |
| Client mod | None |
| Dependencies | None |

## Installation

1. Download `HunterCompass-x.y.z.jar` from the [latest release](https://github.com/einSelbst/hunter-compass/releases/latest).
2. Stop the Paper server and create a backup.
3. Copy the JAR into the server's `plugins/` directory.
4. Start the server. HunterCompass creates `plugins/HunterCompass/config.yml`.
5. Start an event with `/huntercompass start <target>`.

## Event workflow

1. Make sure the target and initial hunters are online.
2. Run `/hc start <target>`. Every eligible online player except the target is
   enrolled and receives one personal Hunter Compass.
3. Players who join later are enrolled automatically by default. Use
   `/hc give <player>` or `/hc give all` whenever a compass needs to be restored.
4. Check the current target and hunter count with `/hc status`.
5. When an enrolled hunter kills the target, HunterCompass announces the winner
   and stops the event by default.
6. Run `/hc stop` at any time to end the hunt and remove online players' Hunter Compasses.

Hunter records persist across restarts. If an enrolled player is offline when
the event stops, any remaining marked compass is removed the next time another
event enrolls that player; marked compasses never become valid for a different owner.

## Dimension behavior

The coordinate display always reports the target's real position and dimension.
The compass needle follows these rules:

| Hunter and target | Needle behavior |
| --- | --- |
| Same dimension | Points to the target's exact X/Z position |
| Hunter in Overworld, target in Nether | Points to target X/Z multiplied by 8: a useful portal-area direction |
| Hunter in Nether, target in Overworld | Points to target X/Z divided by 8 using block-floor rounding |
| The End or unrelated dimensions | Spins and the action bar says `direction unavailable` |
| Target offline | Spins and the action bar says `target offline` |

The Overworld/Nether projection identifies a useful horizontal portal area, not
an exact portal block. Y is deliberately not scaled. Portal linking still obeys
Minecraft's normal world border, search radius, and portal placement rules.

For The End and custom dimension pairs without a meaningful coordinate mapping,
HunterCompass uses a mismatched lodestone dimension so the client renders a
spinning needle. This avoids presenting world spawn or another unrelated point
as the target direction.

## Commands and permissions

`/huntcompass` and `/hc` are aliases for `/huntercompass`.

| Command | Description | Permission |
| --- | --- | --- |
| `/hc start <target>` | Start a hunt and enroll eligible online players | `huntercompass.admin` |
| `/hc stop` | Stop the hunt and remove online compasses | `huntercompass.admin` |
| `/hc status` | Show the target, online state, and hunter count | `huntercompass.admin` |
| `/hc give <player\|all>` | Enroll players and ensure exactly one compass each | `huntercompass.admin` |
| `/hc remove <player\|all>` | Remove hunters and their marked compasses | `huntercompass.admin` |
| `/hc reload` | Reload `config.yml` and the update interval | `huntercompass.admin` |

| Permission | Default | Description |
| --- | --- | --- |
| `huntercompass.admin` | Server operators | Manage the event and configuration |
| `huntercompass.hunter` | Everyone | Allow automatic hunter enrollment |

Permission plugins can deny `huntercompass.hunter` to spectators or other
players who should not join the hunt automatically.

## Configuration

The default configuration is stored in
[`src/main/resources/config.yml`](src/main/resources/config.yml).

| Setting | Default | Description |
| --- | ---: | --- |
| `update-interval-ticks` | `10` | Coordinate and needle update interval |
| `auto-enroll-eligible-players` | `true` | Enroll permitted players on start and join |
| `stop-on-hunter-kill` | `true` | Announce the winner and end the event |
| `compass.prevent-dropping` | `true` | Prevent deliberate compass transfers |
| `compass.prevent-container-storage` | `true` | Prevent storage that could create replacement copies |
| `compass.name` / `compass.lore` | See file | MiniMessage item text |
| `action-bar.*` | See file | MiniMessage display templates |
| `dimensions.*` | See file | Display names for dimensions |
| `direction-labels.*` | See file | Direction status text |
| `messages.*` | See file | Command and event messages |

Run `/hc reload` after changing the configuration. Mutable event data is kept
separately in `plugins/HunterCompass/data.yml` and should be managed through commands.

## Compass integrity

Only compasses carrying HunterCompass's PersistentDataContainer marker are
updated or removed. Each one also contains its owner's UUID. The update loop
keeps one valid compass per enrolled hunter and removes extra or foreign marked
copies from that player's inventory. Dropping is blocked by default, and another
player cannot pick up an owner-bound compass.

## Troubleshooting

**The compass was not issued**

- The player must be online and have a free inventory slot.
- The target cannot be enrolled as a hunter.
- Run `/hc give <player>` after freeing a slot.

**The needle spins**

- Read the action bar. A spinning needle is intentional when the target is
  offline or no honest cross-dimension direction exists.
- Coordinates and the target dimension remain exact whenever the target is online.

**A late-joining player should be a spectator**

- Deny `huntercompass.hunter` for that player with your permission plugin, or
  disable `auto-enroll-eligible-players` and issue compasses explicitly.

For reproducible bugs, [open an issue](https://github.com/einSelbst/hunter-compass/issues)
and include the HunterCompass version, Paper build, Java version, relevant log
lines, steps to reproduce, and expected behavior.

## Building from source

JDK 21 is required. The included Gradle wrapper downloads the remaining build
dependencies.

```bash
./gradlew clean test build
```

The server JAR is written to `build/libs/HunterCompass-<version>.jar`.

## License

HunterCompass is available under the [MIT License](LICENSE).
