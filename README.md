# altoclef
Plays block game.

Powered by Baritone.

A client side bot that can accomplish any Minecraft task that is relatively simple and can be split into smaller tasks. "Relatively Simple" is a vague term, so check the list of current capabilities to see examples.

Became [the first bot to beat Minecraft fully autonomously](https://youtu.be/baAa6s8tahA) on May 24, 2021.

**Join the [Discord Server](https://discord.gg/fUUEHeNmXb)** for discussions/updates/goofs & gaffs

## How it works

Take a look at this [Guide from the wiki](https://github.com/toccatina/altoclef/wiki/1:-Documentation:-Big-Picture) or this [Video explanation](https://youtu.be/q5OmcinQ2ck?t=387)


## Current capabilities, Examples:
- Obtain 400+ Items from a fresh survival world, like diamond armor, cake, and nether brick stairs
- Dodge mob projectiles and force field mobs away while accomplishing arbitrary tasks
- Collect + smelt food from animals, hay, & crops
- Receive commands from chat whispers via /msg. Whitelist + Blacklist configurable (hereby dubbed the Butler System). Here's a [Butler system demo video](https://drive.google.com/file/d/1axVYYMJ5VjmVHaWlCifFHTwiXlFssOUc/view?usp=sharing)
- Simple config file that can be reloaded via command (check .minecraft directory)
- Beat the entire game on its own (no user input.) This includes:
    - Building a nether portal
    - Finding a nether fortress + collecting blaze rods from a blaze
    - Trade with piglins for ender pearls, collecting + crafting gold in the nether
    - Leave nether
    - Craft + throw eyes of ender and follow direction until stronghold portal is discovered.
    - Setting spawnpoint near stronghold portal
    - Entering end portal, destroying crystals and killing the dragon as it perches.
- Print the entire bee movie script with signs in a straight line, automatically collecting signs + bridging materials along the way.
- Become the terminator: Run away from players while unarmed, gather diamond gear in secret, then return and wreak havoc.


## Download

Check [releases](https://github.com/toccatina/altoclef/releases). Note you will need to copy over both jar files for the mod to work.

This is a **fabric only** mod, currently only available for **Minecraft 1.16.5**.



## TODO's/Future Features:
- Smart tasks that are user customizable. Give resources json-assignable descriptions on how they CAN be obtained, and the system automatically tries to obtain it in the fastest way.
- Given any schematic, Collect ALL resources and BUILD the schematic, ideally from a fresh survival world if possible.
- Initialize and control multiple bots over a network. Kind of like an RTS game.
- Make this bot able to survive anarchy
- All acheivements, fully autonomous (not happening any time soon lol)

## Building the Project Manually

1) Clone project and import. I'd suggest using JetBrain's IntelliJ to import the project.

2) Run gradle task runClient (In IntelliJ open up the Gradle window and run altoclef/Tasks/fabric/runClient)

3) For building a jar to use as a fabric mod, run `altoclef/Tasks/build/build`. Then clone [my fork of fabritone](https://gitlab.com/adrisj7/fabritone) and perform the same build instructions/steps on that project's `fabric/1.16.3` branch.
