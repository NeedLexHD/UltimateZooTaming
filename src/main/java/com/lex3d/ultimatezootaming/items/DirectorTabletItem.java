package com.lex3d.ultimatezootaming.items;

import com.lex3d.ultimatezootaming.capability.CapabilityHandler;
import com.lex3d.ultimatezootaming.core.network.NetworkHandler;
import com.lex3d.ultimatezootaming.core.network.OpenAnimalCardS2CPacket;
import com.lex3d.ultimatezootaming.core.network.SyncAnimalsS2CPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.PacketDistributor;

/**
 * Tablette du directeur : clic droit sur un animal apprivoise = sa FICHE +
 * le CALIN du directeur (+satisfaction, +confiance). Remplace le sneak+clic
 * (occupe par un autre mod de transport).
 */
public class DirectorTabletItem extends Item {

    public DirectorTabletItem(Properties props) { super(props); }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, net.minecraft.world.entity.player.Player player,
                                                  LivingEntity target, InteractionHand hand) {
        if (player.level().isClientSide()) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;
        if (!(target instanceof Animal animal)) return InteractionResult.PASS;

        var data = animal.getCapability(CapabilityHandler.TAMING_DATA).resolve().orElse(null);
        if (data == null || !data.isTamed()) {
            sp.displayClientMessage(Component.translatable(
                    "message.ultimatezootaming.tablet_wild"), true);
            return InteractionResult.CONSUME;
        }

        // Calin du directeur (cooldown 5 min par animal)
        long now = sp.serverLevel().getGameTime();
        if (now - data.getLastPet() > 6000) {
            data.setLastPet(now);
            data.setSatisfaction(Math.min(100, data.getSatisfaction() + 5));
            data.addTrust(2f);
            com.lex3d.ultimatezootaming.saveddata.ZooLedger.get(sp.serverLevel()).addMissionProgress(
                    com.lex3d.ultimatezootaming.progression.DailyMission.CUDDLE_3, 1);
            sp.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.HEART,
                    animal.getX(), animal.getY() + animal.getBbHeight() + 0.3, animal.getZ(),
                    3, 0.3, 0.2, 0.3, 0.0);
            sp.displayClientMessage(Component.translatable(
                    "message.ultimatezootaming.director_pet", animal.getName()), true);
        }

        NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp),
                new OpenAnimalCardS2CPacket(
                        SyncAnimalsS2CPacket.describe(animal, data, sp.serverLevel())));
        return InteractionResult.CONSUME;
    }

    /**
     * Clic droit dans le vide : ouvre le FLUX SOCIAL du zoo.
     * La tablette sert donc a deux choses : viser un animal pour sa fiche,
     * ou consulter ZooTok pour prendre la temperature du parc.
     */
    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(
            net.minecraft.world.level.Level level,
            net.minecraft.world.entity.player.Player player,
            net.minecraft.world.InteractionHand hand) {
        // sendToServer part forcement du CLIENT : la condition inversee faisait
        // que le clic droit ne declenchait rien du tout.
        if (level.isClientSide()) {
            com.lex3d.ultimatezootaming.core.network.NetworkHandler.CHANNEL.sendToServer(
                    new com.lex3d.ultimatezootaming.core.network.RequestFeedC2SPacket());
        }
        return net.minecraft.world.InteractionResultHolder.success(player.getItemInHand(hand));
    }
}
