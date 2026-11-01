# Auto Shulker Pickup (Minecraft 26.2 / Fabric)

This feature is part of Item Scroller, **not a separate mod**.

## Behavior

- `autoShulkerPickup` is a Generic config boolean, **ON by default**.
- On a real player's ground-item pickup, the integrated server attempts to put the item into a carried shulker **before** vanilla inventory insertion, even if normal inventory slots are free.
- First fill matching stacks in shulkers that already contain the item, then empty slots in those matching shulkers, then vacant slots in shulkers with other items, then completely empty shulkers.
- Existing items are never removed, converted, or overwritten; only free slots can receive a new item type.
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
3. **Mixed items**: carry a shulker with 16 dirt in one slot and 26 empty slots, then pick up 32 cobblestone. The cobblestone should be added to a separate slot in that same shulker.
4. **Partially filled priority**: carry a mixed shulker and an empty shulker. Pick up a new item type; it should use the mixed shulker's free slots before the empty shulker.
5. **Matching across boxes**: carry a shulker with matching cobblestone, a partly filled dirt shulker, and an empty one. Cobblestone should go into the matching shulker first.
6. **Overflow**: with no room in eligible shulkers, the remainder enters inventory; if inventory is full, leftover item entity remains in the world.
7. **Non-nesting**: picking up a shulker never inserts it inside another.
8. **Toggle**: switch `autoShulkerPickup` off in config and pick up cobblestone; normal inventory receives it. Re-enable to resume shulker storage.
9. **Fake player**: spawn a GCA/Carpet fake player carrying an empty shulker; it must use vanilla pickup instead.
10. **Regression**: keep `ShulkerBoxItemHandling` and `directShulkerCrafting` enabled and confirm mass craft/refill still works.

Note: source compilation is not a substitute for launching the game and checking the pickup/discard path; test the scenarios above before relying on the feature in a valuable world. Back up the world first.
