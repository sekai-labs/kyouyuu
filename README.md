# Kyouyuu (共有)

Paper plugin for linking physical chests across your server to shared inventory channels. Opening any chest linked to a channel shows the same inventory.

Supports Paper 1.21+ (Java 21+).

## Features

- **Shared Channels**: Link chests, trapped chests, and barrels to named channels.
- **Double Chests**: Automatically links both halves of double chests.
- **Database Support**: Embedded SQLite (default), MySQL, and PostgreSQL via HikariCP.
- **Permission Control**: Fine-grained permissions per channel (`access`, `deposit`, `withdraw`), with native LuckPerms integration.
- **Hopper Automation**: Supports hopper insertion and extraction on linked containers synced with shared channels.
- **Safety Checks**: Channels with items cannot be deleted or downsized without `--force`.

## Installation

1. Download the latest jar and put it into your server's `plugins/` folder.
2. (Optional) Install LuckPerms.
3. Restart the server.

## Quick Start

```bash
/kyo channel create vault 54

/kyo link vault

/kyo link vault
```

Items placed into one chest appear in all other chests linked to `vault`.

## Commands

| Command | Permission | Description |
| :--- | :--- | :--- |
| `/kyo link <channel>` | `kyouyuu.chest.link` | Enter link mode (right-click a chest within 30s) |
| `/kyo unlink` | `kyouyuu.chest.unlink` | Unlink targeted chest or enter unlink mode |
| `/kyo info` | `kyouyuu.use` | Check link status of targeted chest |
| `/kyo channel create <id> [size]` | `kyouyuu.channel.manage` | Create channel (default 54 slots) |
| `/kyo channel delete <id> [--force]` | `kyouyuu.channel.manage` | Delete channel |
| `/kyo channel resize <id> <size> [--force]` | `kyouyuu.channel.manage` | Resize channel |
| `/kyo channel list` | `kyouyuu.use` | List channels and linked chest counts |
| `/kyo channel info <id>` | `kyouyuu.use` | Show channel details |
| `/kyo reload` | `kyouyuu.admin` | Reload configuration |

Alias: `/kyouyuu`

## Permissions

| Node | Default | Description |
| :--- | :--- | :--- |
| `kyouyuu.admin` | `op` | Full access and reload command |
| `kyouyuu.use` | `true` | General command usage and opening linked chests |
| `kyouyuu.channel.manage` | `op` | Create, resize, and delete channels |
| `kyouyuu.chest.link` | `op` | Link chests to channels |
| `kyouyuu.chest.unlink` | `op` | Unlink chests or break linked chest blocks |
| `kyouyuu.channel.<channel>.access` | `false` | Open a specific channel |
| `kyouyuu.channel.<channel>.deposit` | `false` | Place items into a specific channel |
| `kyouyuu.channel.<channel>.withdraw` | `false` | Take items from a specific channel |
| `kyouyuu.channel.*.<access|deposit|withdraw>` | `false` | Wildcard access for all channels |

## Configuration

`plugins/Kyouyuu/config.yml`:

```yaml
storage:
  type: SQLITE
  autosave-interval-seconds: 30

  mysql:
    host: "localhost"
    port: 3306
    database: "kyouyuu"
    username: "root"
    password: ""
    ssl: false

  postgresql:
    host: "localhost"
    port: 5432
    database: "kyouyuu"
    username: "postgres"
    password: ""
    ssl: false

linking:
  session-timeout-seconds: 30

hopper:
  enabled: true
```

## Building

```bash
./gradlew clean build
```

The compiled jar will be at `build/libs/kyouyuu-1.1.0.jar`.

## License

MIT
