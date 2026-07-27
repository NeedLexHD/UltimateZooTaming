package com.lex3d.ultimatezootaming.blocks;

import com.lex3d.ultimatezootaming.capability.CapabilityHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.animal.Animal;

/**
 * Effet visuel/sonore d'une borne d'interaction (flash photo / nourrissage /
 * jet d'eau). Extrait ici pour etre declenche AUSSI bien par le visiteur (IA)
 * que par le CLIC DROIT du joueur (pour tester). L'effet suit l'orientation
 * (FACING) de la borne : le jet part vers l'avant.
 */
public final class StationEffect {

    private StationEffect() {}

    /** Joue l'effet de la borne a la position donnee. Retourne l'animal touche
     *  (pour nourrissage/eau) ou null. */
    public static void play(ServerLevel level, BlockPos station) {
        if (!(level.getBlockState(station).getBlock() instanceof InteractionStationBlock st)) return;
        var facing = level.getBlockState(station).getValue(InteractionStationBlock.FACING);
        BlockPos front = station.relative(facing);
        // Missions journalieres : chaque utilisation de borne compte
        var ledger = com.lex3d.ultimatezootaming.saveddata.ZooLedger.get(level);
        // PANNE D'ELECTRICITE : les bornes sont HS ce jour-la
        if (ledger.getDailyEvent() == com.lex3d.ultimatezootaming.events.dynamic.DynamicEvent.POWER_OUTAGE.ordinal()) {
            level.sendParticles(ParticleTypes.SMOKE, station.getX() + 0.5,
                    station.getY() + 1.2, station.getZ() + 0.5, 5, 0.2, 0.1, 0.2, 0.02);
            level.playSound(null, station, SoundEvents.LEVER_CLICK,
                    SoundSource.BLOCKS, 0.3f, 0.6f);
            return;
        }
        ledger.addMissionProgress(com.lex3d.ultimatezootaming.progression.DailyMission.STATION_USES, 1);
        switch (st.getKind()) {
            case PHOTO -> {
                ledger.addMissionProgress(com.lex3d.ultimatezootaming.progression.DailyMission.TAKE_PHOTOS, 1);
                level.sendParticles(ParticleTypes.FLASH, station.getX() + 0.5,
                        station.getY() + 1.2, station.getZ() + 0.5, 1, 0, 0, 0, 0);
                level.sendParticles(ParticleTypes.END_ROD, station.getX() + 0.5,
                        station.getY() + 1.3, station.getZ() + 0.5, 10, 0.35, 0.25, 0.35, 0.03);
                level.playSound(null, station, SoundEvents.SPYGLASS_USE, SoundSource.NEUTRAL, 0.7f, 1.4f);
            }
            case FEED -> {
                Animal a = nearestAnimal(level, front, 6);
                if (a != null) {
                    ledger.addMissionProgress(
                            com.lex3d.ultimatezootaming.progression.DailyMission.FEED_ANIMALS, 1);
                    level.sendParticles(ParticleTypes.HEART, a.getX(), a.getEyeY() + 0.3, a.getZ(),
                            3, 0.25, 0.25, 0.25, 0);
                    a.getCapability(CapabilityHandler.TAMING_DATA).ifPresent(d ->
                            d.setSatisfaction(Math.min(100, d.getSatisfaction() + 2)));
                    level.playSound(null, a.blockPosition(), SoundEvents.GENERIC_EAT,
                            SoundSource.NEUTRAL, 0.6f, 1.0f);
                } else {
                    level.sendParticles(net.minecraft.core.particles.ParticleTypes.ITEM_SLIME,
                            front.getX() + 0.5, front.getY() + 0.6, front.getZ() + 0.5,
                            5, 0.2, 0.1, 0.2, 0);
                }
            }
            case WATER -> {
                Animal wa = animalInFront(level, station, facing, 8);
                if (wa != null) {
                    ledger.addMissionProgress(
                            com.lex3d.ultimatezootaming.progression.DailyMission.WATER_ANIMALS, 1);
                }
                playWaterJet(level, station, facing);
            }
        }
    }

    /** Le vrai jet d'eau balistique : un arc qui part de la buse vers l'avant/l'animal. */
    private static void playWaterJet(ServerLevel level, BlockPos station,
                                     net.minecraft.core.Direction facing) {
        BlockPos front = station.relative(facing);
        // Ne vise QUE les animaux reellement DEVANT la borne (dans la direction du
        // jet), pas ceux sur les cotes/derriere. Evite que le jet parte vers le joueur.
        Animal a = animalInFront(level, station, facing, 8);
        double sx = station.getX() + 0.5, sy = station.getY() + 1.15, sz = station.getZ() + 0.5;
        // depart depuis la BUSE (avancee d'un demi-bloc vers l'avant)
        sx += facing.getStepX() * 0.5;
        sz += facing.getStepZ() * 0.5;
        double tx, ty, tz;
        if (a != null) { tx = a.getX(); ty = a.getY() + 0.2; tz = a.getZ(); }
        else { tx = sx + facing.getStepX() * 4; ty = sy - 1.2; tz = sz + facing.getStepZ() * 4; }
        double dx = tx - sx, dz = tz - sz;
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist < 0.1) dist = 0.1;
        int steps = 18;
        double arc = 1.8;
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            double px = sx + dx * t;
            double pz = sz + dz * t;
            double py = sy + arc * (t - t * t) * 4 - (sy - ty) * t;
            level.sendParticles(ParticleTypes.SPLASH, px, py, pz, 3, 0.06, 0.06, 0.06, 0.01);
            if (i % 2 == 0) {
                level.sendParticles(ParticleTypes.FALLING_WATER, px, py, pz, 1, 0.08, 0.02, 0.08, 0);
            }
        }
        level.playSound(null, station, SoundEvents.PLAYER_SPLASH, SoundSource.NEUTRAL, 0.7f, 1.2f);
        if (a != null) {
            level.sendParticles(ParticleTypes.SPLASH, a.getX(), a.getY() + 0.3, a.getZ(),
                    14, 0.4, 0.2, 0.4, 0.1);
            level.sendParticles(ParticleTypes.FALLING_WATER, a.getX(), a.getEyeY() + 0.4, a.getZ(),
                    8, 0.3, 0.3, 0.3, 0);
            a.getCapability(CapabilityHandler.TAMING_DATA).ifPresent(d ->
                    d.setSatisfaction(Math.min(100, d.getSatisfaction() + 3)));
        }
    }

    /** Cherche l'animal le plus proche DEVANT la borne (dans la direction du jet),
     *  en ignorant ce qui est sur les cotes ou derriere. */
    private static Animal animalInFront(ServerLevel level, BlockPos station,
                                        net.minecraft.core.Direction facing, double range) {
        // Boite de recherche etendue vers l'AVANT uniquement (pas centree sur la borne)
        BlockPos near = station.relative(facing, (int) (range / 2));
        var box = new net.minecraft.world.phys.AABB(near).inflate(range / 2.0 + 1);
        Animal best = null;
        double bd = Double.MAX_VALUE;
        double fx = facing.getStepX(), fz = facing.getStepZ();
        for (Animal a : level.getEntitiesOfClass(Animal.class, box)) {
            if (!a.isAlive()) continue;
            // vecteur borne -> animal
            double vx = a.getX() - (station.getX() + 0.5);
            double vz = a.getZ() - (station.getZ() + 0.5);
            // produit scalaire avec le facing : positif = l'animal est DEVANT
            double dot = vx * fx + vz * fz;
            if (dot <= 0.5) continue; // derriere ou juste a cote : on ignore
            // ecart lateral : l'animal doit rester a peu pres dans l'axe du jet
            double lateral = Math.abs(vx * fz - vz * fx); // composante perpendiculaire
            if (lateral > 2.0) continue; // trop sur le cote
            double d = vx * vx + vz * vz;
            if (d < bd && d <= range * range) { bd = d; best = a; }
        }
        return best;
    }

    private static Animal nearestAnimal(ServerLevel level, BlockPos near, double r) {
        Animal best = null;
        double bd = Double.MAX_VALUE;
        var box = new net.minecraft.world.phys.AABB(near).inflate(r);
        for (Animal a : level.getEntitiesOfClass(Animal.class, box)) {
            if (!a.isAlive()) continue;
            double d = a.distanceToSqr(near.getX() + 0.5, near.getY(), near.getZ() + 0.5);
            if (d < bd) { bd = d; best = a; }
        }
        return best;
    }
}
