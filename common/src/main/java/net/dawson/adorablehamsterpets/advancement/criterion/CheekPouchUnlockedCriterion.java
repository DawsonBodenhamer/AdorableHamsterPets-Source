package net.dawson.adorablehamsterpets.advancement.criterion;


/*
 * All Rights Reserved
 * Copyright (c) 2025 Dawson Bodenhamer (www.ForTheKing.Design)
 * 
 * All files and assets in this repository are the exclusive property of the copyright holder.
 * Permission is NOT granted to copy, modify, merge, publish, distribute, sublicense, or sell this material.
 * Provided "AS IS" without warranty. See LICENSE for details.
 */

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.dawson.adorablehamsterpets.entity.custom.HamsterEntity;
import net.minecraft.advancement.criterion.AbstractCriterion;
import net.minecraft.loot.context.LootContext;
import net.minecraft.predicate.entity.EntityPredicate;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Optional;

public class CheekPouchUnlockedCriterion extends AbstractCriterion<CheekPouchUnlockedCriterion.Conditions> {

    public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    EntityPredicate.LOOT_CONTEXT_PREDICATE_CODEC.optionalFieldOf("player").forGetter(Conditions::player),
                    EntityPredicate.LOOT_CONTEXT_PREDICATE_CODEC.optionalFieldOf("hamster").forGetter(Conditions::hamster) // Optional: if you want to check hamster properties
            ).apply(instance, Conditions::new));

    public void trigger(ServerPlayerEntity player, HamsterEntity hamster) {
        LootContext hamsterContext = EntityPredicate.createAdvancementEntityLootContext(player, hamster);
        this.trigger(player, conditions -> conditions.matches(player, hamsterContext));
    }

    @Override
    public Codec<Conditions> getConditionsCodec() {
        return CODEC;
    }

    public record Conditions(Optional<LootContextPredicate> player, Optional<LootContextPredicate> hamster)
            implements AbstractCriterion.Conditions {
        public boolean matches(ServerPlayerEntity playerEntity, LootContext hamsterContext) {
            if (this.player.isPresent() && !this.player.get().test(EntityPredicate.createAdvancementEntityLootContext(playerEntity, playerEntity))) {
                return false;
            }
            return this.hamster.isEmpty() || this.hamster.get().test(hamsterContext);
        }
    }
}