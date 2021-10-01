# Development Guide

## To use Alto Clef as a library and quick start making custom commands, check out the [Example Repo Here](https://github.com/gaucho-matrero/altoclef-example)

## Get it Running (IDE)
1) Clone project and import. I'd suggest using JetBrain's IntelliJ to import the project. Make sure you're using jdk 16 in your Project & Gradle Settings!

2) Run gradle task runClient (In IntelliJ open up the Gradle window and run altoclef/Tasks/fabric/runClient)

## Get it Running (Command line, Linux & maxOS)

1) Git Clone project
2) `cd` into cloned local repo
3) `sudo / doas chmod +x gradlew`
4) `./gradlew build` or `./gradlew runClient`


## Rough Video Guide/Overview
[Watch a Rough AltoClef Tutorial Video Here](https://youtu.be/giBjHDZ7HvY)

## Modifying Baritone (dev mode)

Alto Clef uses a custom fork of baritone that gives you more control over how baritone works.
If you wish to make edits to that fork you can do so locally if you follow these steps:

1) Clone [The baritone fork](https://github.com/gaucho-matrero/baritone) into the same directory as `altoclef`.
    Your root `altoclef` folder (containing `build.gradle`) should now also contain a folder called `baritone`.
2) Run `gradle build` within the fork you just cloned. You may open the folder in an IDE and run the `build` task.
3) There should now be various `.jar` files starting with `baritone` in the following folder: `<your altoclef directory>/baritone/build/libs`
4) Now within `altoclef`, pass `-Paltoclef.development` as a parameter when running `gradle build`
   (In IntelliJ, go to the build dropdown -> `Edit Configurations`, then duplicate the `altoclef [build]` configuration.
   In this duplicate, paste `-Paltoclef.development` into the Arguments text field.)
5) When you build and pass `-Paltoclef.development`, Alto Clef should now use the jar file inside 
   of your custom `baritone` fork instead of pulling from online. This lets you rapidly test local changes to baritone.
