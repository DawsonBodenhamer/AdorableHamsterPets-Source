# Function to check if a nearby hamster was just named "Sweet Potato"
# This function is called as a reward from the sweet_potato_named advancement.
# It runs as the player who performed the interaction.

# Check if the nearest hamster (likely the one just interacted with) is named "Sweet Potato" (case-insensitive)
# We'll check common capitalizations.
# This approach checks the most likely inputs.
execute if entity @e[type=adorablehamsterpets:hamster,name="Sweet Potato",distance=..3,limit=1,sort=nearest] run function adorablehamsterpets:technical/sweet_potato_effects
execute if entity @e[type=adorablehamsterpets:hamster,name="sweet potato",distance=..3,limit=1,sort=nearest] run function adorablehamsterpets:technical/sweet_potato_effects
execute if entity @e[type=adorablehamsterpets:hamster,name="Sweet potato",distance=..3,limit=1,sort=nearest] run function adorablehamsterpets:technical/sweet_potato_effects
execute if entity @e[type=adorablehamsterpets:hamster,name="sweetpotato",distance=..3,limit=1,sort=nearest] run function adorablehamsterpets:technical/sweet_potato_effects
execute if entity @e[type=adorablehamsterpets:hamster,name="Sweet-Potato",distance=..3,limit=1,sort=nearest] run function adorablehamsterpets:technical/sweet_potato_effects
execute if entity @e[type=adorablehamsterpets:hamster,name="sweetpotato",distance=..3,limit=1,sort=nearest] run function adorablehamsterpets:technical/sweet_potato_effects

# Revoke the calling advancement so this check can run again if another hamster is named.
# This makes the *check* repeatable, but the effects function will only grant its own "flag" once.
advancement revoke @s only adorablehamsterpets:technical/sweet_potato_named