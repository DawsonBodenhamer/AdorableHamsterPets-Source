# Give the player the Hamster Guide Book book.
# The component 'patchouli:book' links this item to the book defined at 'data/adorablehamsterpets/patchouli_books/adh_guide/'.
give @s adorablehamsterpets:hamster_guide_book[patchouli:book="adorablehamsterpets:adh_guide"] 1

# Revoke the calling advancement immediately so this function doesn't run in a loop.
advancement revoke @s only adorablehamsterpets:technical/receive_guide_book