package com.lex3d.ultimatezootaming.events;

import com.lex3d.ultimatezootaming.UltimateZooTame;
import com.lex3d.ultimatezootaming.capability.CapabilityHandler;
import com.lex3d.ultimatezootaming.capability.TamingData;
import com.lex3d.ultimatezootaming.saveddata.ZooSavedData;
import com.lex3d.ultimatezootaming.zones.ZooZone;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.Animal;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Alerte "tycoon" : toutes les 30s, verifie les enclos ; si un enclos passe en
 * difficulte (malades OU bien-etre moyen < 25), previent son proprietaire en
 * ligne d'un message discret. Anti-spam : 5 min de silence par enclos.
 */
@Mod.EventBusSubscriber(modid = UltimateZooTame.MODID)
public class ZooAlertHandler {

    private static final int CHECK_INTERVAL = 600;      // 30s
    private static final long ALERT_COOLDOWN_MS = 300_000; // 5 min
    private static final Map<UUID, Long> LAST_ALERT = new HashMap<>();

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.getServer().getTickCount() % CHECK_INTERVAL != 0) return;

        for (ServerLevel level : event.getServer().getAllLevels()) {
            for (ZooZone zone : ZooSavedData.get(level).getAllZones()) {
                if (!zone.isAnimalZone()) continue;
                ServerPlayer owner = event.getServer().getPlayerList().getPlayer(zone.getOwnerUUID());
                if (owner == null) continue; // proprietaire hors ligne

                int sum = 0, count = 0, sick = 0;
                for (Animal a : level.getEntitiesOfClass(Animal.class, zone.boundingBox(),
                        an -> an.isAlive() && zone.contains(an.blockPosition())
                                && an.getCapability(CapabilityHandler.TAMING_DATA)
                                    .resolve().map(TamingData::isTamed).orElse(false))) {
                    var d = a.getCapability(CapabilityHandler.TAMING_DATA).resolve().orElse(null);
                    if (d == null) continue;
                    sum += d.getSatisfaction();
                    count++;
                    if (d.isSick()) sick++;
                }
                if (count == 0) continue;
                int avg = sum / count;
                boolean trouble = sick > 0 || avg < 25;
                if (!trouble) continue;

                long now = System.currentTimeMillis();
                Long last = LAST_ALERT.get(zone.getId());
                if (last != null && now - last < ALERT_COOLDOWN_MS) continue;
                LAST_ALERT.put(zone.getId(), now);

                String key = sick > 0 ? "message.ultimatezootaming.alert_sick"
                                      : "message.ultimatezootaming.alert_welfare";
                owner.sendSystemMessage(Component.literal("\u26A0 ")
                        .withStyle(ChatFormatting.YELLOW)
                        .append(Component.translatable(key, zone.getName(),
                                sick > 0 ? sick : avg).withStyle(ChatFormatting.GRAY)));
            }
        }
    }
}
