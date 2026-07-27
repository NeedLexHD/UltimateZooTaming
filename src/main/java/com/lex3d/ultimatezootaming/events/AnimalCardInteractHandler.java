package com.lex3d.ultimatezootaming.events;

import com.lex3d.ultimatezootaming.UltimateZooTame;
import com.lex3d.ultimatezootaming.capability.CapabilityHandler;
import com.lex3d.ultimatezootaming.core.network.NetworkHandler;
import com.lex3d.ultimatezootaming.core.network.OpenAnimalCardS2CPacket;
import com.lex3d.ultimatezootaming.core.network.SyncAnimalsS2CPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.animal.Animal;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

/** Sneak+clic main vide sur un animal apprivoise : sa FICHE s'ouvre. */
@Mod.EventBusSubscriber(modid = UltimateZooTame.MODID)
public class AnimalCardInteractHandler {

    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) return;
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (true) return; // DESACTIVE : le calin passe par la Tablette du directeur (shift+clic pris par un autre mod)
        if (!(event.getTarget() instanceof Animal animal)) return;

        var data = animal.getCapability(CapabilityHandler.TAMING_DATA).resolve().orElse(null);
        if (data == null || !data.isTamed()) return;

        // Le CALIN DU DIRECTEUR : consulter, c'est prendre soin. Bonus que les
        // soigneurs ne donnent pas (cooldown 5 min par animal).
        long now = player.serverLevel().getGameTime();
        if (now - data.getLastPet() > 6000) {
            data.setLastPet(now);
            data.setSatisfaction(Math.min(100, data.getSatisfaction() + 5));
            data.addTrust(2f);
            player.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.HEART,
                    animal.getX(), animal.getY() + animal.getBbHeight() + 0.3, animal.getZ(),
                    3, 0.3, 0.2, 0.3, 0.0);
            player.displayClientMessage(Component.translatable(
                    "message.ultimatezootaming.director_pet", animal.getName()), true);
        }

        NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new OpenAnimalCardS2CPacket(
                        SyncAnimalsS2CPacket.describe(animal, data, player.serverLevel())));
        event.setCanceled(true);
        event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
    }
}
