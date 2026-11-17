# Nexus-Core

A core plugin to add basic useful functionality to extend off of for plugin usage/building.

Nexus-Core is a modular foundation plugin for Paper and Folia. Each piece of functionality is a
self-contained **feature** that can be switched on or off, hot-reloaded, and used by other plugins through
a small public API.

| Feature | What it does |
|---|---|
| **Sidebar scoreboards** | Per-player boards chosen by priority and conditions, with live placeholders and animations. Players can toggle their board, and that choice persists. |
| **Tab list** | Per-player header and footer, using the same conditions, placeholders and animations. |

Both features support:

- **Adventure / MiniMessage** formatting: gradients, hex colours, rainbow and so on.
- **PlaceholderAPI** for any `%placeholder%`, plus built-in `%nexus_*%` placeholders that work without it.
- **Hot reload**: `/nexuscore reload` applies changes without dropping anyone's display. If a config file is broken, that part keeps its previous working state.

## Requirements

| | |
|---|---|
| Server | Paper **26.3** or Folia **26.3** |
| Java | 25 |
| Optional | [PlaceholderAPI](https://modrinth.com/plugin/placeholderapi) 2.12+ |

## Installation

1. Drop `NexusCore-<version>.jar` into `plugins/`.
2. Start the server once to generate the config files in `plugins/NexusCore/`.
3. Edit them, then run `/nexuscore reload`.

## Configuration files

| File | Purpose |
|---|---|
| `config.yml` | Feature switches, PlaceholderAPI expansion toggle, sidebar default visibility |
| `scoreboards.yml` | Sidebar boards |
| `tablist.yml` | Tab list layouts |
| `animations.yml` | Named animations usable anywhere as `<animation:name>` |
| `messages.yml` | Chat messages for commands |

Each file has comments explaining every option. The short version:

```yaml
# scoreboards.yml
boards:
  default:
    priority: 0              # highest matching priority wins
    update-interval: 20      # ticks (20 = 1s); only changed lines are sent
    title: "<animation:title>"
    lines:
      - "<gray>Player <dark_gray>» <white>%nexus_player%"
      - "<gray>Balance <dark_gray>» <green>%vault_eco_balance%"   # via PlaceholderAPI
  nether:
    priority: 10
    conditions:
      worlds: [world_nether]
    title: "<red><bold>THE NETHER"
    lines: [ ... ]
```

### Conditions

All listed conditions must match. An unknown or invalid condition **never matches** and is reported in the
console. A typo therefore hides a board instead of showing a staff-only board to everyone.

| Condition | Example | Matches when |
|---|---|---|
| `worlds` | `[world, world_nether]` | the player is in one of the worlds |
| `exclude-worlds` | `[minigames]` | the player is in none of the worlds |
| `permission` | `vip.board` or a list | the player has every permission |
| `gamemodes` | `[survival, adventure]` | the player is in one of the game modes |

### Placeholders

| Syntax | Resolved by |
|---|---|
| `%nexus_<key>%` | Nexus-Core itself. Keys: `player`, `displayname`, `uuid`, `world`, `online`, `max_players`, `ping`, `x`, `y`, `z`, `health`, `food`, `level`, `gamemode` |
| `%anything_else%` | PlaceholderAPI, when installed. Legacy `&` and `&#rrggbb` colours in the result are kept. |

- Unknown placeholders are shown exactly as written, so typos are easy to spot.
- Placeholder values are inserted as **text**. A nickname containing `<red>` or a click tag cannot inject formatting.
- With PlaceholderAPI installed, the `%nexus_*%` placeholders are also available to every other plugin. You can turn this off in `config.yml`.

## Commands & permissions

| Command | Permission | Default |
|---|---|---|
| `/sidebar [on\|off\|toggle]` (alias `/sb`) | `nexuscore.command.sidebar` | everyone |
| `/nexuscore reload` | `nexuscore.command.admin` | op |
| `/nexuscore info` | `nexuscore.command.admin` | op |
| `/nexuscore sidebar <player> <board\|auto>` | `nexuscore.command.admin` | op |
| `/nexuscore tablist <player> <layout\|auto>` | `nexuscore.command.admin` | op |

`/nexus` is an alias for `/nexuscore`. Forcing a board or layout lasts until the player quits; `auto` returns
them to automatic selection.

## For developers

### Using the API

Install the API locally with `./gradlew :nexus-api:publishToMavenLocal`, then:

```kotlin
repositories { mavenLocal() }
dependencies { compileOnly("com.zombiehdgaming.nexuscore:nexus-api:0.1.0-SNAPSHOT") }
```

Declare a dependency on `NexusCore` in your plugin descriptor, then:

```java
NexusCoreApi nexus = NexusCore.get();

// Add a placeholder, usable as %nexus_kills% in any Nexus-Core text (and in PlaceholderAPI).
// It is removed automatically when your plugin disables.
nexus.placeholders().register(this, "kills", player -> Component.text(stats.kills(player)));

// Pin a player to a specific board during a minigame, then hand control back.
nexus.scoreboards().forceProfile(player, "bedwars");
nexus.scoreboards().forceProfile(player, null);

// Update immediately instead of waiting for the next interval.
nexus.scoreboards().refresh(player);
```

API methods taking a `Player` are safe to call from any thread.

### Building

```bash
./gradlew build          # plugin jar: nexus-core/build/libs/NexusCore-<version>.jar
./gradlew runServer      # local Paper 26.3 test server with PlaceholderAPI
./gradlew runFolia       # same on Folia
```

### Project layout

```
nexus-api/    Public, stable API (published). No implementation details.
nexus-core/   The plugin.
  display/      Shared engine: profiles, priority selection, per-player sessions & scheduling
  scoreboard/   Sidebar feature; packet/ is the only code touching server internals
  tablist/      Tab list feature
  text/         Template compilation, MiniMessage rendering, animations
  placeholder/  %nexus_*% registry, built-ins, PlaceholderAPI bridge
  condition/    Condition parsing (extensible via ConditionParser#register)
  feature/      Feature lifecycle & config-driven enable/disable/reload
```

### Design notes

- **Sidebars are sent as packets.** The objective exists only on the client, so Nexus-Core never replaces a
  player's Bukkit `Scoreboard`. Other plugins' teams (nametags, prefixes, glow colours) keep working. This is
  also required for Folia, which does not support per-player Bukkit scoreboards. Lines use per-score display
  names (no 40-character limit), score numbers are hidden, and each refresh sends only the lines that changed.
  All of this is in `scoreboard/packet/NmsSidebarPacketSender`, the one class to revisit on a protocol change.
- **One code path for Paper and Folia.** All per-player work runs on the player's entity scheduler. That is
  the main thread on Paper and the owning region thread on Folia. Shared state is immutable and swapped
  atomically on reload.
- **Templates are compiled once.** Placeholder tokens are rewritten to MiniMessage tags at load time. Lines
  without placeholders or animations are rendered once and reused for every player.
- **Animations are server-wide.** They follow a global tick clock, so all players see the same frame. A
  display refreshes at least as fast as the fastest animation it uses, including nested ones.

### Adding a new feature

1. Implement `feature.Feature`. For a per-player, condition-driven display, extend `display.DisplayFeature`
   to get selection, scheduling, reload and events for free.
2. Register it in `NexusCorePlugin#onEnable` with `features.register(...)`.
3. Add `features.<id>: true` to `config.yml`.

## License

[MIT](LICENSE)
