package com.lex3d.ultimatezootaming.events;

import com.lex3d.ultimatezootaming.capability.CapabilityHandler;
import com.lex3d.ultimatezootaming.capability.TamingData;
import com.lex3d.ultimatezootaming.entities.VisitorEntity;
import com.lex3d.ultimatezootaming.zones.ZooZone;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.phys.AABB;

/** Evasions : alerte, panique des visiteurs, et etat global (bloque l'affluence). */
public final class EscapeHandler {

    /** Animaux actuellement echappes, par monde (evite tout scan global). */
    private static final java.util.Map<net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level>,
            java.util.Set<java.util.UUID>> ESCAPED = new java.util.concurrent.ConcurrentHashMap<>();

    private EscapeHandler() {}

    public static void onEscape(ServerLevel level, LivingEntity mob, ZooZone zone) {
        ESCAPED.computeIfAbsent(level.dimension(), k -> java.util.concurrent.ConcurrentHashMap.newKeySet())
                .add(mob.getUUID());
        for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
            p.sendSystemMessage(Component.literal("\u26A0 ").withStyle(ChatFormatting.RED)
                    .append(Component.translatable("message.ultimatezootaming.escape",
                            mob.getName(), zone.getName()).withStyle(ChatFormatting.RED)));
        }
        // Les visiteurs proches paniquent et quittent le zoo
        for (VisitorEntity v : level.getEntitiesOfClass(VisitorEntity.class,
                mob.getBoundingBox().inflate(24))) {
            v.setLeaving(true);
            v.spendJoy(40);
        }
        level.playSound(null, mob.blockPosition(),
                net.minecraft.sounds.SoundEvents.GOAT_SCREAMING_AMBIENT,
                net.minecraft.sounds.SoundSource.NEUTRAL, 1.0f, 0.8f);
    }

    public static void onRecapture(ServerLevel level, LivingEntity mob) {
        java.util.Set<java.util.UUID> s = ESCAPED.get(level.dimension());
        if (s != null) s.remove(mob.getUUID());
        for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
            p.displayClientMessage(Component.translatable(
                    "message.ultimatezootaming.escape_over", mob.getName())
                    .withStyle(ChatFormatting.GREEN), false);
        }
    }

    /** Une evasion est-elle en cours ? (coupe l'arrivee des visiteurs) — O(1). */
    public static boolean anyEscapeActive(ServerLevel level) {
        java.util.Set<java.util.UUID> s = ESCAPED.get(level.dimension());
        if (s == null || s.isEmpty()) return false;
        // Purge : animal mort ou decharge = evasion terminee
        s.removeIf(id -> {
            var e = level.getEntity(id);
            if (e == null || !e.isAlive()) return true;
            TamingData d = e.getCapability(CapabilityHandler.TAMING_DATA).resolve().orElse(null);
            return d == null || !d.isEscaped();
        });
        return !s.isEmpty();
    }
}
