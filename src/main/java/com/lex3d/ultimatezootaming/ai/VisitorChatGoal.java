package com.lex3d.ultimatezootaming.ai;

import com.lex3d.ultimatezootaming.entities.VisitorEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;

/**
 * RENCONTRES : deux visiteurs qui se croisent s'arretent brievement, se
 * regardent et echangent quelques mots (emote de parole), puis reprennent leur
 * chemin.
 *
 * Volontairement OCCASIONNEL : long cooldown et tirage au sort, pour que les
 * allees ne se transforment pas en salon de discussion permanent.
 */
public class VisitorChatGoal extends Goal {

    /** Delai minimum entre deux discussions pour un meme visiteur (~50 s). */
    private static final int COOLDOWN = 1000;
    /** Chance de s'arreter quand une rencontre est possible. */
    private static final int CHANCE_IN = 6; // 1 sur 6

    private final VisitorEntity visitor;
    private VisitorEntity partner;
    private int chatTicks;
    private int cooldown;

    public VisitorChatGoal(VisitorEntity visitor) {
        this.visitor = visitor;
        this.cooldown = 200 + visitor.getRandom().nextInt(600); // desynchronise au spawn
        setFlags(EnumSet.of(Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (--cooldown > 0) return false;
        cooldown = COOLDOWN;
        if (visitor.isLeaving() || !visitor.hasTicket()) return false;
        if (!(visitor.level() instanceof ServerLevel level)) return false;
        // Une rencontre sur six seulement : ca doit rester un hasard agreable
        if (visitor.getRandom().nextInt(CHANCE_IN) != 0) return false;

        // Un autre visiteur tout proche, qui n'est pas deja en train de parler
        var others = level.getEntitiesOfClass(VisitorEntity.class,
                new AABB(visitor.blockPosition()).inflate(3.0),
                v -> v != visitor && !v.isLeaving() && v.hasTicket() && !v.isChatting());
        if (others.isEmpty()) return false;

        partner = others.get(visitor.getRandom().nextInt(others.size()));
        chatTicks = 40 + visitor.getRandom().nextInt(40); // 2 a 4 s
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return partner != null && partner.isAlive() && chatTicks > 0
                && visitor.distanceToSqr(partner) < 25.0;
    }

    @Override
    public void start() {
        visitor.setChatting(true);
        visitor.getNavigation().stop();
    }

    @Override
    public void stop() {
        visitor.setChatting(false);
        partner = null;
        chatTicks = 0;
    }

    @Override
    public void tick() {
        if (partner == null) return;
        chatTicks--;
        // Ils se font face
        visitor.getLookControl().setLookAt(partner, 30f, 30f);
        // Emote de parole de temps en temps
        if (chatTicks % 20 == 0 && visitor.level() instanceof ServerLevel level) {
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.NOTE,
                    visitor.getX(), visitor.getEyeY() + 0.4, visitor.getZ(),
                    1, 0.1, 0.1, 0.1, 0.0);
        }
    }
}
