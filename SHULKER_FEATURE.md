# Item Scroller 26.2 - ShulkerBoxItemHandling (experimental)

Based on Sakura Ryoko's LTS/26.2, Item Scroller 0.32.2. This feature branch leaves the original fork default branch and LTS/26.2 untouched.

## New feature

Generic configuration: `ShulkerBoxItemHandling` (default OFF).
When using the existing `massCraft` hotkey with a stored recipe, low-stock ingredients can be drawn from shulker boxes carried in your inventory, without opening the boxes. Nearby containers are not used.

This is **experimental**. Client and Fabric dedicated server must both run this modified mod and the matching Fabric API; the client also requires MaLiLib 0.29.6. The mod JAR must replace the existing Item Scroller JAR (do not install both).

## Testing

Back up your world. Test crafting with one ingredient from a single shulker and count items before and after. Then test multiple ingredients, partially-full inventory, full inventory, and multiplayer synchronization. Confirm OFF has vanilla Item Scroller behavior. Review GitHub Actions build results before trying to load the JAR.

## Build

Use Java 25, then `./gradlew build`. The upstream GitHub Actions build also runs on this feature branch.

## Implementation caveats

This is a first experimental implementation, not verified in game. Item matching requires exact components. It does not support retrieving materials from placed storage or other mods' containers.

## Version 0.32.2-shulker.2

- Guard recipe-book mass crafting so a partial/intermediate output cannot be quick-moved or dropped.
- Limit the safe mass-craft path to one verified quick-move per iteration tick instead of unguarded back-to-back result clicks.
- Stop buffering inventory update packets while waiting for a shulker refill.
- Remove the artificial three-cycle post-refill delay; acknowledgments follow vanilla slot broadcasts.
- Refill more than one stack per material when possible (target: 32 crafts' worth, up to 256 items per ingredient).
- The safe path transfers finished items to the player inventory. A full inventory can stop crafting rather than dropping the result.

This safety guard protects the client path; high-latency multiplayer and custom crafting recipes still need in-game testing. 
