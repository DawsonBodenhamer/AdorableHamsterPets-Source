# Give the player the Hamster Guide Book book.
# The component 'patchouli:book' links this item to the book defined at 'data/adorablehamsterpets/patchouli_books/hamster_tips_guide_book/'.
give @s adorablehamsterpets:hamster_guide_book{"patchouli:book":"adorablehamsterpets:hamster_tips_guide_book"} 1

# Revoke the calling advancement immediately so this function doesn't run in a loop.
advancement revoke @s only adorablehamsterpets:technical/receive_guide_book