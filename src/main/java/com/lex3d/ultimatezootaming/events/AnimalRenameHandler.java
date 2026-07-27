package com.lex3d.ultimatezootaming.events;

import com.lex3d.ultimatezootaming.UltimateZooTame;
import com.lex3d.ultimatezootaming.capability.CapabilityHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Baptiser un animal du zoo : clic droit avec une ETIQUETTE (name tag) nommee
 * sur un animal apprivoise. Le nom est applique a l'entite ET enregistre dans
 * sa fiche (TamingData.customName), pour apparaitre dans la tablette et les
 * listes d'enclos meme si le nom d'affichage change plus tard.
 *
 * On intercepte avant le comportement vanilla pour pouvoir aussi ecrire dans la
 * capability et annoncer le bapteme.
 */
@Mod.EventBusSubscriber(modid = UltimateZooTame.MODID)
public class AnimalRenameHandler {

    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) return;
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(event.getTarget() instanceof Animal animal)) return;

        ItemStack held = event.getItemStack();
        if (!held.is(Items.NAME_TAG)) return;
        if (!held.hasCustomHoverName()) return; // etiquette vierge : rien a faire

        var data = animal.getCapability(CapabilityHandler.TAMING_DATA).resolve().orElse(null);
        if (data == null || !data.isTamed()) return; // on ne baptise que ses pensionnaires

        String newName = held.getHoverName().getString();
        if (newName.isBlank()) return;

        // Applique le nom a l'entite (affichage au-dessus de la tete) ET a la fiche
        animal.setCustomName(Component.literal(newName));
        animal.setCustomNameVisible(true);
        animal.setPersistenceRequired(); // securite (la persistance est deja acquise
                                         // des l apprivoisement / l assignation d enclos)
        data.setCustomName(newName);

        if (!player.isCreative()) held.shrink(1);

        player.serverLevel().sendParticles(
                net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                animal.getX(), animal.getY() + animal.getBbHeight() * 0.9, animal.getZ(),
                6, 0.3, 0.2, 0.3, 0.02);
        player.serverLevel().playSound(null, animal.blockPosition(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 0.4f, 1.6f);
        player.displayClientMessage(Component.translatable(
                "message.ultimatezootaming.animal_named", newName)
                .withStyle(ChatFormatting.GREEN), true);

        // On a tout fait : on empeche le renommage vanilla de rejouer par-dessus
        event.setCanceled(true);
        event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
    }
}
