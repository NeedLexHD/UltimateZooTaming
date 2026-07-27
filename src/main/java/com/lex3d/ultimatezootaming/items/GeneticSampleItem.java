package com.lex3d.ultimatezootaming.items;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * ECHANTILLON GENETIQUE : preleve sur un animal avec la Seringue, il conserve
 * son patrimoine (espece, trait, rarete, generation) et sert de "parent" dans
 * l'Incubateur.
 *
 * Interet : tu peux constituer une banque genetique, croiser deux animaux qui
 * ne se rencontreront jamais, et garder l'ADN d'une bete exceptionnelle meme
 * apres sa mort.
 */
public class GeneticSampleItem extends Item {

    public GeneticSampleItem(Properties p) { super(p); }

    /** Cree un echantillon a partir des donnees d'un animal. */
    public static ItemStack create(String speciesId, String donorName, String trait,
                                   int rarity, int generation) {
        ItemStack s = new ItemStack(
                com.lex3d.ultimatezootaming.core.init.ModItems.GENETIC_SAMPLE.get());
        CompoundTag tag = new CompoundTag();
        tag.putString("Species", speciesId);
        tag.putString("Donor", donorName == null ? "" : donorName);
        tag.putString("Trait", trait == null ? "NONE" : trait);
        tag.putInt("Rarity", rarity);
        tag.putInt("Generation", generation);
        s.setTag(tag);
        return s;
    }

    public static String speciesOf(ItemStack s) {
        CompoundTag t = s.getTag();
        return t == null ? "" : t.getString("Species");
    }

    public static String traitOf(ItemStack s) {
        CompoundTag t = s.getTag();
        return t == null ? "NONE" : t.getString("Trait");
    }

    public static int rarityOf(ItemStack s) {
        CompoundTag t = s.getTag();
        return t == null ? 0 : t.getInt("Rarity");
    }

    public static int generationOf(ItemStack s) {
        CompoundTag t = s.getTag();
        return t == null ? 0 : t.getInt("Generation");
    }

    /** Un echantillon sans espece est vide et inutilisable. */
    public static boolean isValid(ItemStack s) {
        return !s.isEmpty()
                && s.is(com.lex3d.ultimatezootaming.core.init.ModItems.GENETIC_SAMPLE.get())
                && !speciesOf(s).isEmpty();
    }

    @Override
    public Component getName(ItemStack stack) {
        String sp = speciesOf(stack);
        if (sp.isEmpty()) return super.getName(stack);
        var type = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES
                .getValue(net.minecraft.resources.ResourceLocation.tryParse(sp));
        Component species = type != null
                ? Component.translatable(type.getDescriptionId())
                : Component.literal(sp);
        return Component.translatable("item.ultimatezootaming.genetic_sample.named", species);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        CompoundTag t = stack.getTag();
        if (t == null) {
            tooltip.add(Component.translatable("item.ultimatezootaming.genetic_sample.empty")
                    .withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        // Donneur
        String donor = t.getString("Donor");
        if (!donor.isEmpty()) {
            tooltip.add(Component.translatable("item.ultimatezootaming.genetic_sample.donor", donor)
                    .withStyle(ChatFormatting.GRAY));
        }
        // Trait
        String trait = t.getString("Trait");
        if (!"NONE".equals(trait) && !trait.isEmpty()) {
            tooltip.add(Component.translatable("trait.ultimatezootaming." + trait.toLowerCase())
                    .withStyle(ChatFormatting.AQUA));
        }
        // Rarete
        int rarity = t.getInt("Rarity");
        if (rarity > 0) {
            int col = switch (rarity) {
                case 1 -> 0xC0C0C0; case 2 -> 0xE0B94F; default -> 0xFFF0F5;
            };
            tooltip.add(Component.literal("\u2726 ").append(
                            Component.translatable("gui.ultimatezootaming.rarity." + rarity))
                    .withStyle(s -> s.withColor(col)));
        }
        tooltip.add(Component.translatable("item.ultimatezootaming.genetic_sample.gen",
                t.getInt("Generation")).withStyle(ChatFormatting.DARK_GRAY));
    }
}
