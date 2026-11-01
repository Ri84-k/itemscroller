# Auto Shulker Pickup (Minecraft 26.2 / Fabric)

This feature is part of Item Scroller, **not a separate mod**.

## Behavior

- `autoShulkerPickup` is a Generic config boolean, **ON by default**.
- On a real player's ground-item pickup, the integrated server attempts to put the item into a carried shulker **before** vanilla inventory insertion, even if normal inventory slots are free.
- Carpet Org Addition 26.2 compatibility: when `blockDropsDirectlyEnterInventory` routes mined blocks straight to `Inventory.add`, the same shulker-first behavior applies to the scoped block-breaking player's drops.
- Storage mode is `autoShulkerPickupMode`, **OVERFLOW by default**:
  - **OVERFLOW**: fill matching stacks (up to maximum stack size); then free slots in matching boxes; then partially filled unrelated boxes; then empty boxes; finally normal inventory. Each box is reused before an empty one.
  - **FIXED**: use existing boxes containing that item type first (including matching stacks and empty slots); if there is no capacity, open an empty box for that type; **never add a new item type to a partially filled box**; finally normal inventory.
- **Per-shulker locks**: hover a shulker in your player inventory and press the **Toggle Shulker Lock** hotkey (default **K**). Gold padlock = explicitly locked, blue padlock = global Fixed Mode. Lock metadata stays on that shulker item when you move it. An empty box locked manually automatically assigns the type of the first item that gets stored; a nonempty box can be locked only if it contains one item type. Unlock to permit mixed items again in Overflow Mode. Locks are enforced in both modes.
- Existing items are never removed, converted, or overwritten; only free slots can receive a new item type.
- The original item stack's components (e.g. name, enchantments, damage) must match when combining stacks.
- If all eligible shulkers are full, vanilla inventory pickup handles the entire remainder. If normal inventory also cannot accept it, the remainder stays on the ground.
- Shulker boxes and other items which forbid nested storage are never inserted into shulkers.
- Carpet/GCA fake players are excluded: only a plain `ServerPlayer` class is eligible. Fake players normally subclass `ServerPlayer`.
- The client sends its current toggle value to the integrated server at connection time and whenever it changes. Disabling the toggle restores vanilla pickup behavior.
- The direct-drop compatibility uses Carpet Org Addition's `BLOCK_BREAKER` scoped value (via optional reflection) and does not intercept unrelated `Inventory.add` calls. It gracefully no-ops when Carpet Org Addition is absent.

## Where to change it

Open Item Scroller's config -> Generic -> `autoShulkerPickup` and `autoShulkerPickupMode`. The saved config contains `Generic.autoShulkerPickup` and `Generic.autoShulkerPickupMode` in `config/itemscroller.json`. Press the **Toggle Shulker Lock** hotkey (default **K**) while hovering over a player-owned shulker in the inventory to lock/unlock it. This setting is client-synchronized to the server, where all box mutations are performed.

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
10. **Carpet direct drops**: enable `/carpet blockDropsDirectlyEnterInventory true`, carry a partially filled shulker, then mine or excavate a block. Its drops should appear inside the shulker **without ever touching the ground**.
11. **Carpet overflow**: fill all carried shulkers, mine another block, and check that the remainder enters normal inventory without duplication or lost items.
12. **Direct-drop toggle**: turn `autoShulkerPickup` off with Carpet's direct-drop rule still on, then mine a block. Its drops should go to normal inventory.
13. **Unrelated inventory additions**: with Carpet's rule on and shulker available, use `/give` and ordinary crafting. Both must still put their outputs in regular inventory.
14. **Normal pickup**: with Carpet's direct-drop rule turned off, ground-item pickup must still route into shulkers.
15. **Regression**: keep `ShulkerBoxItemHandling` and `directShulkerCrafting` enabled and confirm mass craft/refill still works.

Note: source compilation is not a substitute for launching the game and checking the pickup/discard path; test the scenarios above before relying on the feature in a valuable world. Back up the world first.

## New mode / lock tests (.7)

16. **Overflow baseline:** carry A (partly filled with cobblestone), B and C empty. Pick up dirt, stone, and more cobblestone. Both new item types should share A's empty slots; cobblestone first fills A's existing stack. B and C stay unused until A is full.
17. **Fixed three item types:** carry three empty boxes A, B, C and pick up 3 cobblestone, 3 stone and 3 dirt. A, B and C should each have a different single item type. Picking up another 3 of each should return to its matching box.
18. **Fixed capacity overflow:** fill the matching fixed-type box, then add more of that type. Use the next empty shulker; do NOT use a partly filled box of another item type. With no empty box, send the extra items to regular inventory.
19. **Manual lock in Overflow:** lock a stone-only shulker with the **Toggle Shulker Lock** hotkey (default **K**); confirm gold padlock and matching tooltip. Dirt must not be routed into that shulker even if it has empty slots. Other unlocked boxes remain eligible.
20. **Empty lock:** lock an empty box, pick up 3 dirt, then pick up 3 stone. The box should be assigned permanently to dirt, refusing stone. Unlock it and confirm the gold padlock disappears.
21. **Mixed-item lock refusal:** attempt the **Toggle Shulker Lock** hotkey (default **K**) on a shulker that already holds cobblestone and dirt; it must refuse the lock and preserve all contents.
22. **Carpet direct drops:** repeat Overflow and Fixed tests with `/carpet blockDropsDirectlyEnterInventory true`; items broken by the player must follow the same routing priorities.
23. **No regressions:** /give and crafting must still go to normal inventory; toggling pickup OFF must fully bypass shulker routing; fake players must not use the feature; existing directShulkerCrafting must remain unchanged.

IMPORTANT: A passing Gradle build is not an in-game safety guarantee. Test on a backed-up world before using this experimental version.


## Lock hotkey and menu-slot correction (.8)

The keybind is configured in **Item Scroller > Hotkeys > Toggle Shulker Lock** (default: **K**, inventory screens only). While hovering an empty or single-item-type shulker in *your own* inventory, press K once to lock (gold padlock), once again to unlock. Blue padlocks in global Fixed Mode are visual mode indicators; the actual persistent lock is gold. Mixed-item boxes refuse a lock request and show a status message. The lock request now sends the hovered slot's index within the open menu, **not** the backing inventory's local index (hotbar slots especially differ). Test both hotbar and main inventory slots, including inventory views inside a chest/crafting menu.

## Tooltip and lock badge rendering correction (.9)

- Inventory padlocks are now extracted after screen contents but **before** native and modded tooltip strata, avoiding yellow icons over preview text and item grids.
- The noisy `Item Scroller Hotkeys > Toggle Shulker Lock` reminder is no longer added to every shulker tooltip. Manually locked boxes show just one short lock-status line; unlocked boxes preserve their usual tooltip.
- The Auto Shulker Pickup and Toggle Shulker Lock config descriptions use proper newline escapes rather than visible literal backslash-n sequences.
- The configurable lock hotkey (default K), packet validation, persistent locks and both pickup priority modes are unchanged.
- Test with Shulker Box Tooltip, Quick Shulker and Simple Shulker Preview installed; hover ordinary, mixed and locked shulkers and confirm padlocks stay behind popups.
