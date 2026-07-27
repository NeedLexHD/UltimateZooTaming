package com.lex3d.ultimatezootaming.events;

import com.lex3d.ultimatezootaming.UltimateZooTame;
import com.lex3d.ultimatezootaming.saveddata.ZooTerritory;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Force le chargement des chunks REVENDIQUES sur la Carte : le zoo continue de
 * tourner (bien-etre, ventes, faim des visiteurs, naissances) meme quand aucun
 * joueur n'est a proximite. Sinon les chunks se dechargent et les animaux
 * "disparaissent" / se retrouvent empiles au rechargement.
 */
@Mod.EventBusSubscriber(modid = UltimateZooTame.MODID)
public class ZooChunkLoader {

    private static int tick = 0;

    /** Re-applique les forceload toutes les 10s (et au chargement du monde). */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (++tick % 200 != 0) return;
        for (ServerLevel level : event.getServer().getAllLevels()) {
            refresh(level);
        }
    }

    public static void refresh(ServerLevel level) {
        ZooTerritory territory = ZooTerritory.get(level);
        for (long packed : territory.chunkKeys()) {
            ChunkPos pos = new ChunkPos(packed);
            level.setChunkForced(pos.x, pos.z, true);
        }
    }
}
