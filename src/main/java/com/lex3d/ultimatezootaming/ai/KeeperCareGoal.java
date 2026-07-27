package com.lex3d.ultimatezootaming.ai;

import com.lex3d.ultimatezootaming.capability.CapabilityHandler;
import com.lex3d.ultimatezootaming.capability.TamingData;
import com.lex3d.ultimatezootaming.entities.ZooKeeperEntity;
import com.lex3d.ultimatezootaming.saveddata.ZooSavedData;
import com.lex3d.ultimatezootaming.zones.ZooZone;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

/**
 * Le soigneur cherche l'animal le MOINS satisfait de son enclos, s'en approche,
 * et le soigne : +satisfaction reguliere, guerison des malades. Reutilise la
 * detection de zone (ZooZone). Anime "care" pendant le soin.
 */
public class KeeperCareGoal extends Goal {

    private static int careInterval() { return com.lex3d.ultimatezootaming.config.ZooServerConfig.KEEPER_CARE_INTERVAL.get(); }
    private final ZooKeeperEntity keeper;
    private Animal target;
    private int careTimer;

    public KeeperCareGoal(ZooKeeperEntity keeper) {
        this.keeper = keeper;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    /**
     * L'enclos a traiter. Avec le multi-enclos, on renvoie EN PRIORITE celui qui
     * contient un animal malade ; sinon le premier enclos valide de sa charge.
     */
    private ZooZone getZone() {
        if (!(keeper.level() instanceof ServerLevel level)) return null;
        var owned = keeper.getAssignedZones();
        if (owned.isEmpty()) return null;
        ZooZone fallback = null;
        for (var id : owned) {
            ZooZone z = ZooSavedData.get(level).getZone(id);
            if (z == null) continue;
            if (fallback == null) fallback = z;
            if (zoneHasSickAnimal(level, z)) return z; // urgence : cet enclos d'abord
        }
        return fallback;
    }

    /** Y a-t-il un animal malade dans cet enclos ? */
    private boolean zoneHasSickAnimal(ServerLevel level, ZooZone zone) {
        for (var a : level.getEntitiesOfClass(net.minecraft.world.entity.animal.Animal.class,
                zone.boundingBox(), an -> zone.contains(an.blockPosition()))) {
            // Les compagnons vanilla ne sont pas soignes par le personnel du zoo
            if (com.lex3d.ultimatezootaming.capability.PetSpecies.isPet(a)) continue;
            var d = a.getCapability(
                    com.lex3d.ultimatezootaming.capability.CapabilityHandler.TAMING_DATA)
                    .resolve().orElse(null);
            if (d != null && d.isSick()) return true;
        }
        return false;
    }

    @Override
    public boolean canUse() {
        if (keeper.isOnStrike()) return false; // impaye : greve
        int job = keeper.getJob();
        if (job == 2 || job == 3 || job == 4) return false; // Nourrisseur / Gardien / Vendeur : pas de soins
        if (job == 1) {
            // Veterinaire : priorite aux malades de SON enclos assigne. S'il n'y a
            // rien a soigner chez lui, il intervient en SECOURS sur les malades
            // proches d'autres enclos (au lieu de rester inactif).
            ZooZone vetZone = getZone();
            if (vetZone != null) {
                target = findNeediest(vetZone);
                if (target == null) target = findSickNearby(32); // secours elargi
            } else {
                target = findSickNearby(24);
            }
            return target != null;
        }
        ZooZone zone = getZone();
        if (zone == null) return false;
        target = findNeediest(zone);
        return target != null;
    }

    /** Cherche l'animal apprivoise MALADE le plus proche, toutes zones confondues. */
    private net.minecraft.world.entity.animal.Animal findSickNearby(double radius) {
        var list = keeper.level().getEntitiesOfClass(net.minecraft.world.entity.animal.Animal.class,
                keeper.getBoundingBox().inflate(radius, 16, radius),
                a -> a.isAlive() && a.getCapability(
                        com.lex3d.ultimatezootaming.capability.CapabilityHandler.TAMING_DATA)
                        .resolve().map(d -> d.isTamed() && d.isSick() && !d.isSevereSick()).orElse(false));
        net.minecraft.world.entity.animal.Animal best = null;
        double bd = Double.MAX_VALUE;
        for (var a : list) {
            double d = keeper.distanceToSqr(a);
            if (d < bd) { bd = d; best = a; }
        }
        return best;
    }

    @Override
    public boolean canContinueToUse() {
        // On continue tant que la cible existe ET est toujours malade
        return target != null && target.isAlive() && getZone() != null
                && target.getCapability(CapabilityHandler.TAMING_DATA)
                    .resolve().map(TamingData::isSick).orElse(false);
    }

    private Animal findNeediest(ZooZone zone) {
        if (!(keeper.level() instanceof ServerLevel level)) return null;
        // On ne cible QUE les animaux malades (le soigneur soigne, il ne "buff" pas
        // le bonheur). Si aucun malade, le soigneur se contente de patrouiller.
        List<Animal> sick = level.getEntitiesOfClass(Animal.class, zone.boundingBox(),
                a -> a.isAlive() && zone.contains(a.blockPosition())
                        && a.getCapability(CapabilityHandler.TAMING_DATA)
                            .resolve().map(d -> d.isTamed() && d.isSick() && !d.isSevereSick()).orElse(false));
        // Le plus proche malade d'abord
        return sick.stream()
                .min(Comparator.comparingDouble(a -> a.distanceToSqr(keeper)))
                .orElse(null);
    }

    @Override
    public void start() {
        keeper.setTask(com.lex3d.ultimatezootaming.entities.KeeperTask.HEALING);
        careTimer = 0;
        travelTicks = 0;
        lastDistSq = Double.MAX_VALUE;
        stuckTicks = 0;
    }

    /** Ticks passes a essayer de rejoindre l'animal (secours si trop long). */
    private int travelTicks = 0;
    /** Distance au dernier controle : sert a detecter un blocage sur place. */
    private double lastDistSq = Double.MAX_VALUE;
    /** Ticks consecutifs sans progression reelle vers la cible. */
    private int stuckTicks = 0;

    /** Delai avant teleportation de secours (config partagee avec le nourrisseur). */
    private static int maxTravel() {
        int t = com.lex3d.ultimatezootaming.config.ZooServerConfig.KEEPER_TP_TIMEOUT.get();
        return t <= 0 ? Integer.MAX_VALUE : t;
    }

    @Override
    public void stop() {
        keeper.setTask(com.lex3d.ultimatezootaming.entities.KeeperTask.IDLE);
        keeper.setWorking(false);
        keeper.getNavigation().stop();
        target = null;
        travelTicks = 0;
        stuckTicks = 0;
        lastDistSq = Double.MAX_VALUE;
    }

    @Override
    public void tick() {
        if (target == null) return;
        keeper.getLookControl().setLookAt(target, 30f, 30f);

        double dist = keeper.distanceToSqr(target);
        if (dist > 4.0) {
            keeper.setWorking(false);
            keeper.getNavigation().moveTo(target, 1.0);

            // --- SECOURS : le veterinaire reste coince (eau, barriere, denivele) ---
            travelTicks++;
            // Progression reelle ? On compare la distance toutes les 20 ticks.
            if (travelTicks % 20 == 0) {
                if (dist >= lastDistSq - 0.5) stuckTicks += 20; // n'avance plus
                else stuckTicks = 0;                            // il progresse
                lastDistSq = dist;
            }
            // Coince 5 s d'affilee, ou trajet trop long : on le pose a cote du malade.
            boolean pathFailed = keeper.getNavigation().isDone() && dist > 4.0;
            if (stuckTicks >= 100 || travelTicks > maxTravel() || (pathFailed && travelTicks > 60)) {
                teleportNextTo(target);
                travelTicks = 0;
                stuckTicks = 0;
                lastDistSq = Double.MAX_VALUE;
            }
        } else {
            // A portee : soin
            keeper.getNavigation().stop();
            keeper.setWorking(true);
            if (++careTimer >= careInterval()) {
                careTimer = 0;
                applyCare();
            }
        }
    }

    private void applyCare() {
        target.getCapability(CapabilityHandler.TAMING_DATA).ifPresent(data -> {
            // Le soigneur GUERIT les maladies uniquement. Il ne gonfle PAS la
            // satisfaction : celle-ci reflete les vraies conditions de l'enclos
            // (espace, nourriture, habitat, compagnie) et se remonte en ameliorant
            // l'enclos, pas par un buff gratuit.
            if (data.isSick()) {
                boolean needsRemedy = com.lex3d.ultimatezootaming.config.ZooServerConfig.KEEPER_NEEDS_REMEDY.get();
                if (!needsRemedy || consumeRemedyFromZone()) {
                    data.setSick(false);
                    data.setMiseryTimer(0);
                    keeper.addXp(2); // Soin = 2 XP
                    if (keeper.level() instanceof ServerLevel lvl) {
                        lvl.playSound(null, target.blockPosition(),
                                net.minecraft.sounds.SoundEvents.HONEY_BLOCK_SLIDE,
                                net.minecraft.sounds.SoundSource.NEUTRAL, 1.0f, 1.4f);
                    }
                }
            }
        });
        if (keeper.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    target.getX(), target.getY() + target.getBbHeight() * 0.8, target.getZ(),
                    3, 0.2, 0.2, 0.2, 0.02);
        }
    }

    /** Cherche un Remede animal dans les coffres/tonneaux de l'enclos et en consomme 1. */
    private boolean consumeRemedyFromZone() {
        ZooZone zone = getZone();
        if (zone == null || !(keeper.level() instanceof ServerLevel level)) return false;

        net.minecraft.world.phys.AABB bb = zone.boundingBox();
        for (net.minecraft.core.BlockPos pos : net.minecraft.core.BlockPos.betweenClosed(
                (int) bb.minX, (int) bb.minY, (int) bb.minZ,
                (int) bb.maxX, (int) bb.maxY, (int) bb.maxZ)) {
            if (!zone.contains(pos)) continue;
            if (level.getBlockEntity(pos) instanceof net.minecraft.world.Container container) {
                for (int i = 0; i < container.getContainerSize(); i++) {
                    net.minecraft.world.item.ItemStack slot = container.getItem(i);
                    if (slot.getItem() == com.lex3d.ultimatezootaming.core.init.ModItems.ANIMAL_REMEDY.get()) {
                        slot.shrink(1);
                        container.setChanged();
                        return true;
                    }
                }
            }
        }
        // Fallback : le vestiaire de secteur (stock de service)
        net.minecraft.core.BlockPos center = net.minecraft.core.BlockPos.containing(
                (bb.minX + bb.maxX) / 2, bb.minY, (bb.minZ + bb.maxZ) / 2);
        net.minecraft.core.BlockPos lockerPos = com.lex3d.ultimatezootaming.blocks.KeeperLockerBlock
                .nearestLocker(level, center, 128);
        if (lockerPos != null && level.getBlockEntity(lockerPos)
                instanceof com.lex3d.ultimatezootaming.blocks.KeeperLockerBlockEntity lk) {
            for (int i = 0; i < lk.getContainerSize(); i++) {
                net.minecraft.world.item.ItemStack slot = lk.getItem(i);
                if (slot.getItem() == com.lex3d.ultimatezootaming.core.init.ModItems.ANIMAL_REMEDY.get()) {
                    slot.shrink(1);
                    lk.setChanged();
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Pose le soigneur juste a cote de l'animal, sur un bloc SEC et solide.
     * On evite l'eau (il ne doit pas atterrir dans un bassin) et on cherche une
     * case libre autour de la cible plutot que sa position exacte.
     */
    private void teleportNextTo(net.minecraft.world.entity.animal.Animal animal) {
        if (!(keeper.level() instanceof ServerLevel level)) return;
        net.minecraft.core.BlockPos base = animal.blockPosition();
        net.minecraft.core.BlockPos best = null;

        // Cases candidates : anneau de 1 a 2 blocs autour de l'animal
        int[][] ring = {{1,0},{-1,0},{0,1},{0,-1},{1,1},{1,-1},{-1,1},{-1,-1},
                        {2,0},{-2,0},{0,2},{0,-2}};
        for (int[] off : ring) {
            for (int dy = 0; dy >= -2 && best == null; dy--) {
                net.minecraft.core.BlockPos p = base.offset(off[0], dy, off[1]);
                if (isSafeStand(level, p)) { best = p; break; }
            }
            if (best != null) break;
        }
        // Rien de sec autour : on prend la case de l'animal si elle est praticable
        if (best == null && isSafeStand(level, base)) best = base;
        if (best == null) return; // vraiment rien de bon : on renonce au TP

        keeper.moveTo(best.getX() + 0.5, best.getY(), best.getZ() + 0.5,
                keeper.getYRot(), keeper.getXRot());
        keeper.getNavigation().stop();
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF,
                keeper.getX(), keeper.getY() + 1.0, keeper.getZ(), 4, 0.2, 0.2, 0.2, 0.01);
    }

    /** Case ou l'on peut se tenir : sol solide, pas d'eau, deux blocs libres au-dessus. */
    private boolean isSafeStand(ServerLevel level, net.minecraft.core.BlockPos p) {
        var below = level.getBlockState(p.below());
        if (!below.isSolidRender(level, p.below())) return false;      // pas de sol
        if (!level.getFluidState(p).isEmpty()) return false;            // pieds dans l'eau
        if (!level.getFluidState(p.above()).isEmpty()) return false;    // tete sous l'eau
        if (!level.getBlockState(p).getCollisionShape(level, p).isEmpty()) return false;
        if (!level.getBlockState(p.above()).getCollisionShape(level, p.above()).isEmpty()) return false;
        return true;
    }
}
