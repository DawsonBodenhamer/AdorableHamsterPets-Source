# Adorable Hamster Pets - Crafted Guide Book Effects
# Plays when the player crafts the Hamster Tips Guide Book.

# --- 1. Play Sound and Particle Effects ---
playsound minecraft:block.enchantment_table.use player @s ~ ~ ~ 0.5 1.2
playsound minecraft:item.book.page_turn player @s ~ ~ ~ 0.7 1.5
particle minecraft:enchant ~ ~1 ~ 0.3 0.5 0.3 0.05 50 force @s
particle minecraft:happy_villager ~ ~1 ~ 0.5 0.5 0.5 0.02 20 force @s

# --- 2. Send Action Bar Message ---
title @s actionbar {"text":"A wealth of hamster knowledge, rediscovered.","color":"gold"}

# --- 3. Revoke Advancement to Make it Re-triggerable ---
advancement revoke @s only adorablehamsterpets:technical/crafted_guide_book