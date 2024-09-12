# LightCheck — Break trees and earn money

## › Required
1. #### Java 17
2. #### [PlaceholderAPI](https://github.com/PlaceholderAPI/PlaceholderAPI) + [WorldGuard Extension](https://api.extendedclip.com/expansions/worldguard)
3. #### [WorldGuard](https://dev.bukkit.org/projects/worldguard/files)
4. #### [Vault](https://github.com/MilkBowl/Vault)

### › Optional
- #### [PlayerPoints](https://www.spigotmc.org/resources/playerpoints.80745)

## › How to use it?

1. #### [Download](https://github.com/kainlighty/LightCutter/releases) this plugin
2. #### Move plugin file to the `plugins` folder
3. #### Start your server
4. #### Use command `/papi ecloud download WorldGuard`
5. #### Reload PlaceholderAPI extensions `/papi reload`

_**IMPORTANT**: If you have more than one region intersecting, give higher priority to where the region with the tree will be located_

## › Features

- #### Realistic animation of a falling tree
- #### Works with the whole world or only regions
- #### Support Vault or PlayerPoints
- #### Cost customizations
- #### Message customizations

## › Review (COMING SOON)

## › Commands and Permissions
| Command            | Description                     | Permission                    |
|--------------------|---------------------------------|-------------------------------|
| lightcutter        | Help by commands                | lightcutter.help _(required)_ |
| lightcutter add    | Add region                      | lightcutter.add               |
| lightcutter update | Update region                   | lightcutter.update            |
| lightcutter remove | Remove region                   | lightcutter.remove            |
| lightcutter info   | Region information              | lightcutter.info              |
| lightcutter list   | List of regions                 | lightcutter.list              |
| lightcutter reload | Reload configurations           | *ONLY CONSOLE*                |

| Additional permissions      | Description                             |
|-----------------------------|-----------------------------------------|
| lightcutter.break.bypass    | Nothing happens when the tree is broken |
| lightcutter.cooldown.bypass | No cooldown                             |
| lightcutter.modes.bypass    | Allow earning in all gamemodes          |
| lightcutter.bypass.*        | Break, cooldown and gamemode bypass     |
| lightcheck.admin            | Full access to the plugin               |