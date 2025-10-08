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
import net.minecraft.advancement.criterion.AbstractCriterion;
import net.minecraft.predicate.entity.EntityPredicate;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Optional;

public class HamsterCreeperAlertCriterion extends AbstractCriterion<HamsterCreeperAlertCriterion.Conditions> {

    public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    EntityPredicate.LOOT_CONTEXT_PREDICATE_CODEC.optionalFieldOf("player").forGetter(Conditions::player)
            ).apply(instance, Conditions::new)
    );

    /**
     * Triggers the criterion when a shoulder hamster alerts to a creeper.
     * @param player The player who received the alert.
     */
    public void trigger(ServerPlayerEntity player) {
        this.trigger(player, conditions -> conditions.matches(player));
    }

    @Override
    public Codec<Conditions> getConditionsCodec() {
        return CODEC;
    }

    /**
     * Conditions for the HamsterCreeperAlertCriterion.
     */
    public record Conditions(Optional<LootContextPredicate> player)
            implements AbstractCriterion.Conditions {
        public boolean matches(ServerPlayerEntity playerEntity) {
            return this.player.isEmpty() || this.player.get().test(EntityPredicate.createAdvancementEntityLootContext(playerEntity, playerEntity));
        }
    }
}