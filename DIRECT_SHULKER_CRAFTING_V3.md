# Item Scroller — Direct Shulker Crafting v3 (experimental)

**Branch:** `feature/26.2-direct-shulker-crafting-v3`
**Version:** `0.32.2-shulker.3`
**Base:** stable tested shulker.2 commit `95b4073`.

This branch does not alter the stable version's branch, and the new mode is OFF by default.

## New mode

- Enable `ShulkerBoxItemHandling` and **`directShulkerCrafting`** in Item Scroller's Generic settings.
- With the crafting table open, select/store a normal recipe and use the existing massCraft shortcut.
- The server checks the exact recipe against its own registry, computes whether **all** ingredient slots can be filled, and transfers matching items **straight from carried shulkers into the crafting grid**, not into ordinary player inventory.
- Existing compatible grid ingredients are retained. The server refuses to replace unrelated items.
- If even one full recipe cannot be assembled, the server leaves the shulkers and crafting grid unchanged.
- Results are checked for an exact match to the saved recipe before the normal vanilla result-slot THROW action is sent. Intended to work with a full inventory.

## Important limitations and safeguards

- Experimental. Java compilation isn't proof of in-game correctness, especially with high latency.
- **Does not** pull from nearby containers, bundles, or remote storage.
- Does not work with recipes whose saved layout differs from the standard 2x2 or 3x3 grid.
- The exact item and components must match the selected recipe; ingredient alternatives require storing the matching choice.
- No recipe output is artificially generated. Crafting output must be produced by the vanilla server after the ingredient transfer.
- Test first with a fresh crafting table and completely empty grid.
- Test one piston craft, then a few stacks, then a fully occupied inventory; **count all ingredients and outputs** for loss/duplication.
- Do not install shulker.2 and shulker.3 together. Install only the normal shulker.3 mod JAR on **both** Fabric client and server, with Fabric API; client also needs MaLiLib.
- Set `directShulkerCrafting = false` to restore the working shulker.2 inventory-refill algorithm without switching JARs.
- Keep a backed-up world.

## Build

GitHub Actions Java 25 workflow: `./gradlew build`. Download the build-artifacts ZIP if green and choose the normal (non-sources/non-dev) JAR.
