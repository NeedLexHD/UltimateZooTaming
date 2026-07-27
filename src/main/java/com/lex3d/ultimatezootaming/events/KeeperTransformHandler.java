package com.lex3d.ultimatezootaming.events;

import com.lex3d.ultimatezootaming.UltimateZooTame;
import com.lex3d.ultimatezootaming.blocks.FeederBlockEntity;
import com.lex3d.ultimatezootaming.core.init.ModEntities;
import com.lex3d.ultimatezootaming.entities.ZooKeeperEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Transformation "facon vanilla" : un Villageois SANS METIER qui reste pres d'une
 * Mangeoire se transforme en Soigneur (comme un villageois prend un metier pres
 * d'un etabli). Verifie de temps en temps (pas chaque tick), avec un delai de
 * presence pour eviter les transformations accidentelles.
 */
@Mod.EventBusSubscriber(modid = UltimateZooTame.MODID)
public class KeeperTransformHandler {

    private static final int CHECK_INTERVAL = 40;     // verifie toutes les 2s
    private static final double FEEDER_RANGE = 4.0;   // doit etre a 4 blocs d'une mangeoire
    private static final int REQUIRED_TICKS = 100;    // ~5s de presence continue

    @SubscribeEvent
    public static void onVillagerTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Villager villager)) return;
        if (villager.level().isClientSide()) return;
        if (!(villager.level() instanceof ServerLevel level)) return;

        // Seulement les villageois SANS metier (nitwit exclu : il ne peut pas travailler)
        VillagerProfession prof = villager.getVillagerData().getProfession();
        if (prof != VillagerProfession.NONE) return;
        if (villager.isBaby()) return;

        // Throttle : on ne verifie que toutes les 2s, decale par entite
        if ((level.getGameTime() + villager.getId()) % CHECK_INTERVAL != 0) return;

        if (isNearFeeder(level, villager.blockPosition())) {
            int progress = villager.getPersistentData().getInt("ZooKeeperProgress") + CHECK_INTERVAL;
            if (progress >= REQUIRED_TICKS) {
                transform(level, villager);
            } else {
                villager.getPersistentData().putInt("ZooKeeperProgress", progress);
                // petit signe visuel que quelque chose se prepare
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                        villager.getX(), villager.getY() + 2.0, villager.getZ(), 2, 0.3, 0.3, 0.3, 0.02);
            }
        } else {
            // S'eloigne : on remet le compteur a zero
            if (villager.getPersistentData().getInt("ZooKeeperProgress") > 0) {
                villager.getPersistentData().putInt("ZooKeeperProgress", 0);
            }
        }
    }

    private static boolean isNearFeeder(ServerLevel level, BlockPos pos) {
        int r = (int) Math.ceil(FEEDER_RANGE);
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-r, -2, -r), pos.offset(r, 2, r))) {
            if (level.getBlockEntity(p) instanceof FeederBlockEntity) {
                if (pos.distSqr(p) <= FEEDER_RANGE * FEEDER_RANGE) return true;
            }
        }
        return false;
    }

    private static void transform(ServerLevel level, Villager villager) {
        ZooKeeperEntity keeper = ModEntities.ZOO_KEEPER.get().create(level);
        if (keeper == null) return;

        keeper.moveTo(villager.getX(), villager.getY(), villager.getZ(),
                villager.getYRot(), villager.getXRot());
        keeper.setNoAi(villager.isNoAi());
        if (villager.hasCustomName()) {
            keeper.setCustomName(villager.getCustomName());
            keeper.setCustomNameVisible(villager.isCustomNameVisible());
        }

        keeper.setSkin(level.getRandom().nextInt(com.lex3d.ultimatezootaming.entities.ZooKeeperEntity.SKIN_COUNT));
        level.addFreshEntity(keeper);
        villager.discard();

        // Effets de transformation
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                keeper.getX(), keeper.getY() + 1.0, keeper.getZ(), 30, 0.4, 0.8, 0.4, 0.05);
        level.playSound(null, keeper.blockPosition(), SoundEvents.PLAYER_LEVELUP,
                SoundSource.NEUTRAL, 1.0f, 1.2f);
    }
}
