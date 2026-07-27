package com.lex3d.ultimatezootaming.events;

import com.lex3d.ultimatezootaming.UltimateZooTame;
import com.lex3d.ultimatezootaming.entities.VisitorEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Ambiance sonore : un zoo vivant murmure (foule discrete quand il y a du monde). */
@Mod.EventBusSubscriber(modid = UltimateZooTame.MODID)
public class AmbianceSoundHandler {

    private static final SoundEvent[] CROWD = {
            SoundEvents.VILLAGER_AMBIENT, SoundEvents.VILLAGER_YES,
            SoundEvents.PARROT_AMBIENT, SoundEvents.NOTE_BLOCK_BELL.get()
    };

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.getServer().getTickCount() % 500 != 0) return; // ~25s
        for (ServerLevel level : event.getServer().getAllLevels()) {
            for (ServerPlayer p : level.players()) {
                int nearby = level.getEntitiesOfClass(VisitorEntity.class,
                        p.getBoundingBox().inflate(28)).size();
                if (nearby < 2) continue;
                SoundEvent s = CROWD[level.random.nextInt(CROWD.length)];
                double dx = p.getX() + (level.random.nextDouble() - 0.5) * 16;
                double dz = p.getZ() + (level.random.nextDouble() - 0.5) * 16;
                level.playSound(null, dx, p.getY(), dz, s, SoundSource.NEUTRAL,
                        0.25f, 0.9f + level.random.nextFloat() * 0.3f);
            }
        }
    }
}
