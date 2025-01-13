# LightCutter — Break trees and earn money

## › Required

1. #### Java 17
3. #### [WorldGuard](https://dev.bukkit.org/projects/worldguard/files)
4. #### [Vault](https://github.com/MilkBowl/Vault)

### › Optional

- #### [PlayerPoints](https://www.spigotmc.org/resources/playerpoints.80745)

## › How to use it?

1. #### [Download](https://github.com/kainlighty/LightCutter/releases) this plugin.
2. #### Move plugin file to the `plugins` folder.
3. #### Start your server.
4. #### Create a region with trees in it.
5. #### Add it to the plugin database: `/lightcutter add`.

## › Features

- #### Realistic animation of a falling tree
- #### Animated tree restoration
- #### Works with the whole world or only regions
- #### Support Vault or PlayerPoints
- #### Cost customizations
- #### Message customizations

## › Review (COMING SOON)

## › Commands and Permissions

| Command                          | Description                                | Permission         |
|----------------------------------|--------------------------------------------|--------------------|
| lightcutter                      | Help by commands                           | lightcutter.help   |
| lightcutter add                  | Add region                                 | lightcutter.add    |
| lightcutter update               | Update region                              | lightcutter.update |
| lightcutter remove               | Remove region                              | lightcutter.remove |
| lightcutter info                 | Region information                         | lightcutter.info   |
| lightcutter list                 | List of regions                            | lightcutter.list   |
| lightcutter reload _(+database)_ | Reload all configurations _(and database)_ | lightcutter.reload |

| Additional permissions      | Description                             |
|-----------------------------|-----------------------------------------|
| lightcutter.cooldown.bypass | No cooldown                             |
| lightcutter.modes.bypass    | Allow earning in all gamemodes          |
| lightcutter.bypass.*        | Break, cooldown and gamemode bypass     |
| lightcutter.admin           | Full access to the plugin               |