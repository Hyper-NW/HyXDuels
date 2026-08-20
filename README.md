# HyXDuels

HyXDuels is a multi-arena duel plugin with AdvancedSlimePaper-backed arena worlds, aggregate combat statistics, and configurable per-kit win divisions.

The Maven release line starts at **1.0.0** under `me.alphatct3209`; the current release is **1.1.0**. Versioning follows the project policy: feature releases increment the middle number, fixes/minor updates increment the patch number, and exceptionally large compatibility generations increment the first number.

## Runtime requirements

- Minecraft/AdvancedSlimePaper **1.21.11**
- Java **21 or newer** (Java 25 is supported; the plugin emits Java 21 bytecode)
- AdvancedSlimePaper API **4.x**; the build uses stable API and file-loader version **4.1.0**
- Arena `.slime` files in `World-Loader.Slime-World-Directory` (`slime_worlds` by default)

AdvancedSlimePaper is a Paper server fork, not a plugin. Run the server on AdvancedSlimePaper rather than placing an ASP jar in the plugins directory.

## Configuration layout

Fresh installations use a purpose-based layout:

- `config.yml` — concise world-loader and statistics-storage settings.
- `advanced/` — administrator-authored mode, menu, division, hologram, display, social, and message definitions.
- `data/` — runtime-owned `arenas.yml`, `kits.yml`, `statistics.yml`, and `player-data.yml` state.

When upgrading from the former flat layout, HyXDuels copies each legacy root file into its new folder and retains the original as a backup. Existing `Kits` and `Arenas` sections in `config.yml` are imported into `data/kits.yml` and `data/arenas.yml`. New writes target only the organized files.
## Arena world lifecycle

Every nonblank world referenced by a saved arena's `Spawn-One`, `Spawn-Two`, or `Lobby` is validated and preloaded from the configured AdvancedSlimePaper file loader before arenas are enabled. Both gameplay spawns of an arena must use the same world; that world is the arena's map template, while the lobby may use a different ASP world. Runtime-created arenas are accepted only when every referenced world is a currently loaded ASP world with a matching immutable `.slime` template.

After every completed `PLAYING` duel (death, forfeit, or quit), the map enters `REGENERATING` and cannot accept players. If several arenas share that map, new admissions are locked immediately, queued/countdown occupants are restored, and existing matches may finish. Once drained, HyXDuels evacuates remaining players, requires a successful `Bukkit.unloadWorld(world, false)`, rereads the same read-only `.slime` template, loads and verifies a fresh Bukkit world, rebinds all arena locations by canonical world name, and only then returns affected arenas to `IDLE`. Failures keep the map unavailable and retry persistently with a capped, log-throttled delay; investigate the actionable server error rather than expecting a modified runtime map to be saved.

Every `/duel join` admission additionally requires each saved location to reference the exact current Bukkit `World` instance owned by ASP—not merely a world with the same name. Immediately before teleport, HyXDuels synchronously probes the destination chunk and rechecks both the world identity and regeneration lock. A stale location or shut-down chunk system rejects and rolls back the admission without moving the player. Unexpected external unload requests for managed worlds are cancelled and rerouted through the same locked drain/regeneration lifecycle.

On disable, retries and challenges are stopped, countdowns are cancelled, arena players are restored, and managed worlds are evacuated where a safe non-managed destination exists before unloading without saving.

## Statistics and commands

Overall wins and kills are lifetime aggregate combat statistics. A completed win increments the winner's current win streak and updates their highest win streak when a new personal best is reached; the loser's current streak resets to zero. Losses and deaths remain tracked, while division progress is independent for each configured mode.

- `/duels stats` — your aggregate statistics and selected kit; falls back to `Default`.
- `/duels stats <player>` — preserves the existing player lookup form and uses that online player's selected kit or `Default` for an offline player.
- `/duels stats <player> <gamemode>` — aggregate statistics plus the requested gamemode's wins, current division, next division, and wins required for the next division.
- `/duels stats <gamemode>` — when no player with that name exists, shows your own statistics for that gamemode.
- `/duels top wins` and `/duels top kills` — aggregate top-ten leaderboards.
- `/duels top divisions <gamemode>` — top ten by that gamemode's wins, displaying each player's division and wins.

Gamemode command arguments accept either the configured kit name or its safe mode key. Tab completion offers both.

## Player menus and challenges

`/duel menu` (`duels.gui`, granted by default) and the default glowing diamond sword in hotbar slot 0 open the mode queue. Selecting a single-kit mode such as Bed Wars immediately enters matchmaking; selecting a multi-kit mode such as Classic opens the kit menu first. Matchmaking keys include the canonical mode, kit, and combat profile, and `/duel leave` removes a waiting player from the queue. Two matching players are admitted atomically into a completely empty compatible arena.

`/duel <player>` and `/duel challenge <player>` open the same mode→kit flow, but completion sends that exact visible online player a request instead of joining random matchmaking. The request captures mode, kit, optional arena, and combat profile until `/duel accept`, `/duel deny`, expiry, or disconnect cleanup.

`advanced/menus.yml` independently configures menu titles, slots, materials, names, lore, party action commands, messages, settings entries, kit-editor controls, and tagged opener items. Openers support `DUEL_MENU`, `MODE_SELECTOR`, `MAP_SELECTOR`, `SETTINGS`, and `PARTY` plus material, slot, name, lore, glow, lock, force-slot, and custom-model-data settings. Items use a plugin PDC tag rather than their display name. Forced lobby slots temporarily preserve and later restore their original contents before duel admission, logout, or plugin shutdown; non-forced items fall back to the first empty inventory slot. Locked openers cannot be moved, swapped, or dropped. The shipped lobby hotbar uses Queue Duels in slot 0, Create / Manage Party (lectern) in slot 4, and Player Settings in slot 8.

`block-break` and `block-place` are administrator-owned per-arena settings, default false, and may be chosen while creating an arena with `/duel createarena <name> [block-break] [block-place]`. They remain editable under `Arenas.<id>.Settings` and `/duel arenasettings <id> <flag> <true|false>`, and still require the selected mode's `Combat.block-damage` capability. Specialized Bed Wars and SkyWars construction remains available. Legacy 1.8 PvP is not an arena rule: it is the only player-selectable gameplay toggle in the mode menu. It removes modern attack cooldown/sweep behavior, is captured into queues and requests, and only matches equal preferences. Original attack speed and no-damage ticks are restored after every duel.

## First-class modes

`advanced/modes.yml` is copied independently at startup and strictly defines the stable 17-mode roster: `bed_wars`, `blitz`, `bow`, `boxing`, `bridge`, `classic`, `combo`, `duel_arena`, `mega_walls`, `nodebuff`, `op`, `parkour`, `quakecraft`, `skywars`, `spleef`, `sumo`, and `uhc`. Each immutable profile configures its display/icon, objective handler, world/cell reset policy, combat flags, target score, maximum duration/timeout policy, reusable allowed/default kits, aliases, enabled state, and leaderboard eligibility. Mode keys are lowercase ASCII words separated by single underscores and are the unchanged values stored in existing YAML `Gamemodes.<key>.Wins` and SQL `duels_gamemode_stats.Gamemode` fields.

Use `/duel modes list` or `/duel modes select <mode> [kit]` (`duels.modes.list` and `duels.modes.select`). Kits have their own unique canonical keys and may be reused by any number of modes; a kit referenced by a mode cannot be deleted. Fresh installations receive in-memory, 1.21-native loadouts for every stable mode (weapons, armor, healing, projectiles, blocks, and mode tools as appropriate). A configured kit with the same canonical key overrides its built-in loadout, so server owners can customize equipment without changing `advanced/modes.yml`. Duel Arena reuses the `classic`, `bow`, `nodebuff`, `op`, and `uhc` kits. The compatibility alias `default` resolves to `classic`. Legacy kit-derived modes are synthesized only in memory when their kit key is not otherwise claimed; no mode file or statistics bucket is merged, renamed, or dropped.

Bed Wars and SkyWars are gameplay modes rather than fixed-kit aliases. A 1v1 Bed Wars arena requires `bed_1`, `bed_2`, `generator_1`, `generator_2`, `shop_1`, and `shop_2` points. Players begin with a wooden sword and team-colored leather leggings/boots, collect generated iron/gold, buy blocks, weapons, permanent armor, persistent shears, tiered tools, and utility from the runtime quick shop, build, respawn after the configured delay while their bed survives, and are finally eliminated after bed loss. Permanent armor and shears survive death; axes and pickaxes downgrade one tier. Optional `diamond_generator[_N]` and `emerald_generator[_N]` points add shared generators. Right-click a block or entity within three blocks of your own shop marker to open the shop.

A 1v1 SkyWars arena requires `chest_1`, `chest_2`, and `mid_chest` points within two blocks of real containers. Players start empty, loot randomized island chests, bridge or fight across the void, contest stronger center loot, and receive upgraded recurring chest refills every `skywars-refill-seconds` (300 by default). Extra `chest_1_N`, `chest_2_N`, and `mid_chest_N` markers are supported. Both modes automatically enable required building and item pickup inside their arena objective region; all chest, drop, and terrain mutations are discarded by ASP regeneration after the match.

UHC gives each participant three custom Golden Heads rendered with that participant's own player skin. Right-clicking a tagged head during an active UHC duel consumes exactly one and immediately grants Regeneration III for 5 seconds plus Absorption I for 2 minutes. The effects are removed when the duel restores the player's pre-match state.

Arena routing writes `Arenas.<id>.Allowed-Modes` through `/duel arenamodes <id> list|add|remove|clear [mode]` (`duels.arenamodes`). The global matrix is unchanged: a listed mode uses only arenas listing it, while modes not listed anywhere use empty-route arenas. Legacy `Allowed-Kits` is read only when `Allowed-Modes` is absent. A legacy entry resolves as a mode key/alias first; otherwise its kit must belong to exactly one mode or startup rejects the ambiguity. If both fields exist, `Allowed-Modes` is authoritative and the old field is left untouched. `/duel arenakits` remains a deprecated command alias. The first queue entrant claims the arena mode; entrant two must match it, while each participant's exact kit is captured independently.

Use `/duel <player>` or `/duel challenge <player>` for the shared configurable mode and kit picker. The player fallback is exact-name only, includes only online players visible to the sender, and never overrides a known subcommand. GUI pages cancel all top/bottom inventory clicks and drags. `/duel accept` starts the incoming challenge and `/duel deny` rejects it.

Only one pending challenge may involve a player at a time. `Challenges.Timeout-Seconds` defaults to 60 seconds; expiry and quit cleanup use unique request tokens so stale tasks cannot affect later requests. Acceptance revalidates online state, queue state, mode, kit, combat profile, and a completely empty compatible arena. If no arena is free, the request remains pending. `Messages.Challenge-*` accepts either a scalar string or a YAML list, as do all other `Messages.*` entries. Round and kill messages are participant-only and expose the documented score, mode, kit, round, player, opponent, and health placeholders.

## Parties

`/party` (alias `/p`, permission `duels.party`) provides `/p invite`, `/p kick`, `/p promote`, `/p demote`, `/p transfer`, and `/p disband`, plus the required `/p accept`, `/p deny`, `/p leave`, `/p list`, and `/p menu` flows. Parties have one leader, moderators, members, configurable size/invite expiry, and leader-controlled visibility. Moderators may invite and remove ordinary members; promotion, demotion, transfer, disband, visibility, and the management GUI remain leader-only.

The leader GUI in `advanced/menus.yml` contains Party FFA, Red vs Blue Team Battle, visibility, host duel, and invite-friends actions. Visibility and invites are native. Host duel opens the same mode/kit queue. FFA and Team Battle run their independently configurable console command lists (`<leader>`, `<party_size>`, and `<action>` placeholders), allowing the buttons to integrate with the server's chosen multi-player arena implementation without pretending the core two-player arena model supports arbitrary team sizes.

## Player settings, friends, and messages

`/settings` (aliases `/preferences` and `/prefs`, permission `duels.settings`) opens the configurable settings GUI; the default comparator opener is placed in hotbar slot 8. All nine entries in `advanced/menus.yml` have independent slot, material, name, lore, and glow settings. Preferences default on/public/anyone and persist by UUID in atomically replaced `data/player-data.yml`: Show Own Tier, Scoreboard, Profile Kits Public/Private, Friend Join Notifier, Blast Particles, Duel Requests Anyone/Friends Only, Direct Messages Anyone/Friends Only, Party Invites Anyone/Friends Only, and Friend Requests On/Off.

`/friend add|accept|deny|remove|list [player]` manages symmetric persistent UUID friendships. Last-known names and pending incoming requests persist across restarts. Friend-request blocking is checked before creating a request; join notifications are sent only to online friends who enabled their own notifier. Duel requests, party invitations, and direct messages are checked against the recipient's privacy setting. `/message <player> <message>` has `/msg`, `/tell`, and `/w` aliases. Viewing another player's stats renders private tier fields or selected kit as `Private`; the owner still sees their own values. Disabling Scoreboard restores the scoreboard HyXDuels replaced. Blast Particles controls only HyXDuels' participant-visible kill burst and does not claim to suppress vanilla client effects. All new responses accept a scalar or multiline YAML list under `Messages`.

## Safe kit layout editor

Operators use `/kiteditor <kit>` (`duels.kits.edit`) to edit cloned kit items in a 54-slot inventory: storage slots 0-35, boots/leggings/chestplate/helmet in 36-39, and offhand in 40. Save, reset, and cancel controls are customizable in `advanced/menus.yml`. The editor requires an empty cursor, blocks bottom-inventory access, outside clicks, shift/number/offhand/drop/double/creative actions, and allows drags only wholly within editable top slots. Closing or cancelling clears any editor-derived cursor clone, so the editor never mutates the player's real inventory or lets cloned equipment escape.

Saving always writes positional `Format-Version: 2`. Existing configured kits retain their non-negative ID; editing a negative-ID built-in creates a positive-ID configured override with the same canonical key and immediately replaces that runtime kit. Legacy packed kits can therefore be safely converted through the editor.

## Divisions and rewards

`advanced/divisions.yml` defines ordered tiers and levels. Each tier's `wins-per-step` is added cumulatively for every level. A player is promoted when their wins in one gamemode reach a threshold.

Each level can contain zero or more console commands under `rewards`. Reward placeholders are `{player}`, `{uuid}`, `{gamemode}`, `{division}`, `{level}`, and `{wins}`. For example:

```yaml
tiers:
  Rookie:
    wins-per-step: 50
    levels:
      1:
        rewards:
          - 'give {player} minecraft:diamond 1'
```

Every crossed division is rewarded, including when more than one threshold is crossed at once. `Messages.Division-Progress` and `Messages.Division-Promotion` in `advanced/messages.yml` control match messages; set either to an empty string to disable it. Their available placeholders are documented directly above those settings.

Invalid division configuration is treated as a startup failure and disables the plugin rather than running with partial thresholds or rewards.

## PlaceholderAPI

The identifier is `duels`. A **safe mode key** is lowercase ASCII with non-alphanumeric runs normalized to `_`; for example, `NoDebuff-Ranked` and `nodebuff_ranked` both resolve to `nodebuff_ranked`. Parsing uses the complete suffix, so keys containing hyphens or underscores are supported.

Aggregate placeholders:

- `%duels_your_wins%`
- `%duels_your_overall_wins%` — explicit alias for lifetime wins
- `%duels_your_kills%`
- `%duels_your_overall_kills%` — explicit alias for lifetime kills
- `%duels_your_winstreak%` or `%duels_your_win_streak%`
- `%duels_your_highest_winstreak%` or `%duels_your_highest_win_streak%`
- `%duels_top_wins_<1-10>%` — compact `Player (N wins)` form
- `%duels_top_kills_<1-10>%` — compact `Player (N kills)` form
- `%duels_lb_overall_wins_<1-10>_player%` and `%duels_lb_overall_wins_<1-10>_value%`
- `%duels_lb_overall_kills_<1-10>_player%` and `%duels_lb_overall_kills_<1-10>_value%`

Per-gamemode placeholders (replace `<safe_mode_key>` and omit the angle brackets):

- `%duels_your_<safe_mode_key>_wins%` — wins in that gamemode
- `%duels_your_<safe_mode_key>_division%` — current division, or `Unranked`
- `%duels_your_<safe_mode_key>_wins_to_next%` — wins remaining, or `0` at maximum
- `%duels_top_<safe_mode_key>_wins_<1-10>%` — compact `Player (N key wins)` form
- `%duels_top_<safe_mode_key>_division_<1-10>%` — compact `Player (Division, N wins)` form
- `%duels_lb_mode_<safe_mode_key>_<1-10>_player%`
- `%duels_lb_mode_<safe_mode_key>_<1-10>_wins%`
- `%duels_lb_mode_<safe_mode_key>_<1-10>_division%`

Examples for the safe key `nodebuff_ranked` are `%duels_your_nodebuff_ranked_division%`, `%duels_top_nodebuff_ranked_division_1%`, and `%duels_lb_mode_nodebuff_ranked_1_division%`. Decomposed mode placeholders are parsed from the fixed `lb_mode_` prefix and their final rank/field, so compound keys and keys containing words such as `wins`, `player`, or `division` remain unambiguous. Invalid syntax returns no value; every syntactically valid vacant rank returns `N/A`.

Overall and configured-mode lifetime leaderboards are built into one immutable snapshot at startup, every `Leaderboard-Cache.Refresh-Ticks` (default 600, bounded to 100-72000), and after a debounced game score mutation. Viewer-filtered holograms use a separate 30-second cache keyed by viewer, mode, time, and player scope. A failed global refresh keeps the complete last successful snapshot and emits a rate-limited warning. Returning players refresh their persisted last-known names without changing their totals.

PlaceholderAPI registration occurs only after worlds, arenas, kits, statistics, and divisions have initialized.

## Scoreboard and tab displays

`Display` in `advanced/display.yml` controls private lobby/arena sidebars and the synthetic player-list display. One synchronous task refreshes all displays; `Display.Refresh-Ticks` is clamped to at least 20 ticks. Sidebars support a title and at most 15 ordered lines. The right-side numeric score column is hidden with Paper's native blank number format, and the internal score order is inverted for the requested display direction. Blank and duplicate lines are safe because every slot uses a stable, unique scoreboard entry. The 80 synthetic tab profiles use descending 1.21 list priorities so configured slot zero appears first, and their profile names are blank so internal identifiers never appear in chat or command suggestions. HyXDuels snapshots each player's prior scoreboard and header/footer, then restores an item only if it is still owned by HyXDuels, avoiding overwrites of changes made by another plugin.

The native display tokens are `<player>`, `<uuid>`, `<online>`, `<world>`, `<arena>`, `<arena_id>`, `<state>`, `<opponent>`, `<mode>`, `<mode_key>`, `<kit>`, `<countdown>`, `<score>`, `<opponent_score>`, `<time>`, `<bed>`, and `<checkpoint>`. Arena context is determined by membership in `ArenaManager`; players without an arena use lobby values. PlaceholderAPI expansion runs after these native tokens when PlaceholderAPI is installed.

`Display.Tab` configures header/footer lines and an optional 80-cell fake-player grid. Leave `Columns.Entries: []` for a normal player list with only the custom header/footer. When entries exist, each value has the form `1|Info`: the first delimiter separates a column number from 1 through 4, while all later delimiters remain part of the text. Insertion order determines rows independently within each column, with at most 20 rows per column. The synthetic profiles exist only in each viewer's player-list packets: they never log in, affect the online count, spawn entities, or expose internal identifiers. Real players are unlisted only while the custom column grid is active and are restored when columns are removed or the display is disabled.

## Storage and upgrades

Newly created kits use `Format-Version: 2`. Their `Inventory` list always contains exactly the 36 storage slots in slot order; an empty string is an empty-slot marker. Armor remains stored by its named `Helmet`, `Chestplate`, `Leggings`, and `Boots` fields, while `Offhand` is stored separately. Kits with no `Format-Version` are read as legacy v1 kits and retain their original packed, `addItem`-based behavior with an empty offhand. Unknown format versions and malformed v2 inventory lengths stop kit loading with the affected configuration path in the error.

YAML stores lifetime values as `Wins`, `Kills`, `WinStreak`, and `HighestWinStreak` under `Statistics.<uuid>`, with per-mode wins under `Gamemodes.<safe-mode-key>.Wins`. Dated mode wins and kills live under `Periods.<yyyy-MM-dd>.<mode>` for daily, weekly, and monthly filters. SQL stores lifetime values in `duels_player`, mode wins in `duels_gamemode_stats`, and dated mode counters in `duels_period_stats`. Existing SQL tables receive streak columns automatically; missing YAML counters naturally read as zero. Historical aggregate values remain lifetime-only because old data cannot be assigned truthfully to a date or mode.

## Building

This is a Maven project. Build with `mvn clean package`. The shaded plugin jar is produced under `target/`.

## Optional DecentHolograms leaderboards

HyXDuels can create managed leaderboard holograms when both **PlaceholderAPI** and **DecentHolograms 2.10.1** are installed and enabled. Both are soft dependencies: HyXDuels remains enabled when either is absent, and the PlaceholderAPI expansion continues to work when DecentHolograms is absent. `/duel hologram status` reports the global switch, both plugin states, integration state, configured/owned counts, foreign-name conflicts, and the last integration/config error.

`advanced/holograms.yml` is independent from `config.yml`, ships with `Enabled: false` and an empty `Managed` section, and is always authoritative. HyXDuels creates each DH object with `saveToFile=false`; no managed definition is written to DecentHolograms' files. Generated lines use viewer-aware `%duels_flb_*%` placeholders and display the viewer's active filter summary. The default and per-entry update interval must be 20-72000 ticks.

Right-clicking an owned leaderboard hologram opens **Leaderboard Settings**. Left/right click cycles Mode (all enabled duel modes), Time (daily, weekly, monthly, lifetime), and Players (all, friends, best friends, guild members). Apply persists the draft to `data/leaderboard-filters.yml`; Discard closes without changing the saved view. Best friends are toggled with `/friend best <player>`. Guild filtering stays plugin-neutral: set `Leaderboard-Filters.Guild-Placeholder` in `advanced/social.yml` to a PlaceholderAPI placeholder that returns the same stable guild ID for all members.

Enable the feature by setting `Enabled: true`. A command-created entry has this shape:

```yaml
Enabled: true
Default-Update-Interval-Ticks: 100
Managed:
  wins:
    Name: hyxduels_wins
    Type: wins # wins, kills, or divisions
    Update-Interval-Ticks: 100
    Location:
      World: world
      X: 0.5
      Y: 80.0
      Z: 0.5
      Yaw: 0.0
      Pitch: 0.0
    Lines:
      - '&6&lTop Duel Wins'
      - '&e1. &f%duels_lb_overall_wins_1_player% &7- &6%duels_lb_overall_wins_1_value%'
```

Division entries additionally require a canonical `Gamemode` key and may use `%duels_lb_mode_<key>_<1-10>_player%`, `_wins%`, and `_division%`. IDs, DH names, duplicate IDs/names, type/mode combinations, loaded worlds, finite coordinates, update intervals, and nonempty lines are validated before reconcile. Every ASP-managed map, lobby, and regenerating world is rejected because those worlds are not stable hologram locations.

Administrators with `duels.holograms.admin` (operator by default) can use:

- `/duel hologram status` and `/duel hologram list`
- `/duel hologram create <id> <wins|kills|divisions> [gamemode]` (player only; saves the current location)
- `/duel hologram move <id>` (player only; persists the current location)
- `/duel hologram delete <id>`
- `/duel hologram reload` (strictly reloads and reconciles `advanced/holograms.yml`)

HyXDuels records the exact DH object returned for every exact name it creates. It updates, moves, or deletes a name only while DH still returns that same object instance. A preexisting or replaced foreign hologram is never adopted or modified. Reload removes only no-longer-configured owned objects; plugin disable removes only objects still verifiably owned by this runtime. Any DH `RuntimeException` or linkage failure disables this integration alone and leaves the duel plugin operational.
