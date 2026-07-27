package com.lex3d.ultimatezootaming.events;

import com.lex3d.ultimatezootaming.UltimateZooTame;
import com.lex3d.ultimatezootaming.core.init.ModItems;
import com.lex3d.ultimatezootaming.items.SurveyorStaffItem;
import com.lex3d.ultimatezootaming.saveddata.ZooSavedData;
import com.lex3d.ultimatezootaming.zones.ZooZone;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

import java.util.UUID;

/**
 * Tant que le joueur tient le Baton d'arpenteur, les bordures de TOUTES ses zones
 * s'affichent en continu en particules colorees :
 *   - zone SELECTIONNEE (celle stockee dans le staff) -> vert vif
 *   - autres zones -> bleu-gris
 * Rendu cote serveur (sendParticles), donc visible sans code client dedie.
 */
@Mod.EventBusSubscriber(modid = UltimateZooTame.MODID)
public class ZoneVisualizationHandler {

    private static final int SHOW_INTERVAL = 10; // toutes les 0.5s (les particules durent ~1s)
    private static final ParticleOptions SELECTED_DUST =
            new DustParticleOptions(new Vector3f(0.2f, 0.9f, 0.45f), 1.3f); // vert
    private static final ParticleOptions OTHER_DUST =
            new DustParticleOptions(new Vector3f(0.45f, 0.6f, 0.9f), 1.0f);  // bleu-gris

    private static int tickCounter;

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (++tickCounter < SHOW_INTERVAL) return;
        tickCounter = 0;

        for (ServerLevel level : event.getServer().getAllLevels()) {
            for (ServerPlayer player : level.players()) {
                if (!isHoldingStaff(player)) continue;

                ZooSavedData data = ZooSavedData.get(level);
                UUID selectedId = getSelectedZone(player);

                for (ZooZone zone : data.getZones(player.getUUID())) {
                    boolean selected = zone.getId().equals(selectedId);
                    ParticleOptions dust = selected ? SELECTED_DUST : OTHER_DUST;

                    for (BlockPos pos : zone.borderColumns()) {
                        // Ne dessine que les bordures proches (perf + lisibilite)
                        if (player.blockPosition().distSqr(pos) > 64 * 64) continue;

                        // Remonte au-dessus de l'eau si la colonne est immergee
                        // (les particules redstone sont invisibles sous l'eau).
                        net.minecraft.core.BlockPos.MutableBlockPos cursor =
                                new net.minecraft.core.BlockPos.MutableBlockPos(pos.getX(), pos.getY() + 1, pos.getZ());
                        int guard = 0;
                        while (guard++ < 12 && level.getFluidState(cursor).is(net.minecraft.tags.FluidTags.WATER)) {
                            cursor.move(0, 1, 0);
                        }
                        double py = cursor.getY() + 0.1;

                        // Colonne de 2 particules empilees = bordure bien visible
                        level.sendParticles(dust, pos.getX() + 0.5, py, pos.getZ() + 0.5,
                                1, 0.05, 0.1, 0.05, 0.0);
                        level.sendParticles(dust, pos.getX() + 0.5, py + 0.6, pos.getZ() + 0.5,
                                1, 0.05, 0.1, 0.05, 0.0);
                    }
                }
            }
        }
    }

    private static boolean isHoldingStaff(ServerPlayer player) {
        return player.getMainHandItem().getItem() instanceof SurveyorStaffItem
                || player.getOffhandItem().getItem() instanceof SurveyorStaffItem;
    }

    private static UUID getSelectedZone(ServerPlayer player) {
        ItemStack staff = player.getMainHandItem().getItem() instanceof SurveyorStaffItem
                ? player.getMainHandItem() : player.getOffhandItem();
        if (staff.hasTag() && staff.getTag().hasUUID("SelectedZone")) {
            return staff.getTag().getUUID("SelectedZone");
        }
        return null;
    }
}
