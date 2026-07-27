package com.lex3d.ultimatezootaming.items;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Une croquette = 1 Regime x 1 Tier.
 * 9 combinaisons au total (voir ModItems).
 */
public class KibbleItem extends Item {

    public enum Diet {
        CARNIVORE,
        HERBIVORE,
        PISCIVORE;

        /** Heuristique simple pour savoir si une croquette correspond au regime de la cible. */
        public boolean matches(LivingEntity target) {
            // Regime force par l'admin (/zootame habitats) : prioritaire.
            int ov = com.lex3d.ultimatezootaming.welfare.HabitatManager.dietOverrideOf(target.getType());
            if (ov == 4) return true;                       // omnivore : accepte tout
            if (ov == 1) return this == HERBIVORE;
            if (ov == 2) return this == CARNIVORE;
            if (ov == 3) return this == PISCIVORE;

            return switch (this) {
                case CARNIVORE -> target instanceof Wolf || target instanceof Fox || target instanceof Cat
                        || target instanceof Ocelot || target instanceof net.minecraft.world.entity.monster.Monster;
                case HERBIVORE -> target instanceof Animal && !(target instanceof Wolf) && !(target instanceof Fox);
                case PISCIVORE -> target instanceof net.minecraft.world.entity.animal.AbstractFish
                        || target instanceof Dolphin || target instanceof Squid;
            };
        }
    }

    public enum Tier {
        BASIQUE(0.18f, 8f, 30f),
        SUPERIEUR(0.32f, 16f, 60f),
        APEX(0.5f, 28f, 90f);

        private final float baseChance;
        private final float trustGain;
        private final float trustRequired;

        Tier(float baseChance, float trustGain, float trustRequired) {
            this.baseChance = baseChance;
            this.trustGain = trustGain;
            this.trustRequired = trustRequired;
        }

        /** Chance de base de taming en un clic (avant modificateurs de trust/regime). */
        public float getBaseChance() {
            return baseChance;
        }

        /** Confiance gagnee par utilisation. */
        public float getTrustGain() {
            return trustGain;
        }

        /** Confiance minimale requise avant qu'un taming reussi soit valide. */
        public float getTrustRequired() {
            return trustRequired;
        }
    }

    private final Diet diet;
    private final Tier tier;

    public KibbleItem(Diet diet, Tier tier, Properties properties) {
        super(properties);
        this.diet = diet;
        this.tier = tier;
    }

    public Diet getDiet() {
        return diet;
    }

    public Tier getTier() {
        return tier;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.ultimatezootaming.kibble.diet." + diet.name().toLowerCase()));
        tooltip.add(Component.translatable("tooltip.ultimatezootaming.kibble.tier." + tier.name().toLowerCase()));
    }
}
