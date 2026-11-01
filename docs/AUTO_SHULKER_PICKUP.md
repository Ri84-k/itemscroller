# Auto Shulker Pickup (Minecraft 26.2 / Fabric)

This feature is part of Item Scroller, **not a separate mod**.

## Behavior

- `autoShulkerPickup` is a Generic config boolean, **ON by default**.
- On a real player's ground-item pickup, the integrated server attempts to put the item into a carried shulker **before** vanilla inventory insertion, even if normal inventory slots are free.
- First fill matching stacks in shulkers that already contain the item, then the empty slots of those same matching shulkers, then completely empty shulker boxes.
- Shulkers containing only unrelated items are never repurposed.
- The original item stack's components (e.g. name, enchantments, damage) must match when combining stacks.
- If all eligible shulkers are full, vanilla inventory pickup handles the entire remainder. If normal inventory also cannot accept it, the remainder stays on the ground.
- Shulker boxes and other items which forbid nested storage are never inserted into shulkers.
- Carpet/GCA fake players are excluded: only a plain `ServerPlayer` class is eligible. Fake players normally subclass `ServerPlayer`.
- The client sends its current toggle value to the integrated server at connection time and whenever it changes. Disabling the toggle restores vanilla pickup behavior.

## Where to change it

Open Item Scroller's config -> Generic -> `autoShulkerPickup`. The saved config also contains `Generic.autoShulkerPickup` under `config/itemscroller.json`.

## Manual checks after `./gradlew build`

1. **Empty box / free inventory**: pickup 64 cobblestone with an empty shulker carried; shulker receives all 64.
2. **Matching priority**: with a partial cobblestone stack in one shulker and an empty second box, pickup cobblestone; existing stack is filled before the empty box is used.
3. **Mixed items**: only a shulker containing dirt is carried; picked-up cobblestone goes to normal inventory.
4. **Overflow**: with no room in eligible shulkers, the remainder enters inventory; if inventory is full, leftover item entity remains in the world.
5. **Non-nesting**: picking up a shulker never inserts it inside another.
6. **Toggle**: switch `autoShulkerPickup` off in config and pick up cobblestone; normal inventory receives it. Re-enable to resume shulker storage.
7. **Fake player**: spawn a GCA/Carpet fake player carrying an empty shulker; it must use vanilla pickup instead.
8. **Regression**: keep `ShulkerBoxItemHandling` and `directShulkerCrafting` enabled and confirm mass craft/refill still works.

Note: source compilation is not a substitute for launching the game and checking the pickup/discard path; test the scenarios above before relying on the feature in a valuable world. Back up the world first.
