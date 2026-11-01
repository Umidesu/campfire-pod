# Campfire Pod

A Fabric mod that adds a hanging pot you place over a campfire. The pot boils and purifies water for [Tough As Nails](https://modrinth.com/mod/tough-as-nails).

It comes in 10 log types: spruce, oak, birch, jungle, acacia, dark oak, mangrove, cherry, crimson and warped. The wood uses the vanilla log textures.

Modrinth: https://modrinth.com/mod/campfire-pod

## Supported versions

- Loader: Fabric
- Minecraft: 1.20.1 through 26.3, the same versions Tough As Nails supports (see `multiversion/targets.json`)
- Requires: Fabric API and Tough As Nails

## Building

All versions are built from one shared source. `multiversion/template/java` holds the Java code with `//#if` preprocessor blocks, and `src/main/resources` holds the assets and data.

```sh
python3 multiversion/gen.py 1.21.1     # one target (no argument = all targets)
cd multiversion/build/1.21.1
gradle build                           # jar ends up in build/libs/
```

Alternatively, run `GRADLE=/path/to/gradle multiversion/build.sh 1.21.1`.

You need Gradle 9.x running on JDK 25, because the Loom plugin requires it (set `JAVA_HOME` to a JDK 25). Each target is then compiled with this JDK:

| Targets | JDK |
|---|---|
| 1.20.1, 1.20.4 | 17 |
| 1.20.6 – 1.21.11 | 21 |
| 26.x | 25 |

Gradle selects the JDK through toolchains. To point it at your local JDKs, set `JDK_PATHS` (a comma-separated list) or `JDK17_HOME` / `JDK21_HOME` / `JDK25_HOME` before running `gen.py`.

## Credits

The code was written with AI assistance. The block models, textures and item icons are the author's own handmade work.

## License

MIT. See [LICENSE](LICENSE).
