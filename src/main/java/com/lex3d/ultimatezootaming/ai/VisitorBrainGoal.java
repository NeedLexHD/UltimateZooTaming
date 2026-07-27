package com.lex3d.ultimatezootaming.ai;

import com.lex3d.ultimatezootaming.entities.VisitorEntity;
import com.lex3d.ultimatezootaming.saveddata.ZooSavedData;
import com.lex3d.ultimatezootaming.zones.ZooZone;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * CERVEAU du visiteur : au lieu de laisser les goals se battre (ce qui faisait
 * tourner en rond), ce goal maitre planifie un VRAI PARCOURS de visite. A chaque
 * etape terminee, il decide de la suivante selon l'etat du visiteur :
 *   - soif haute -> va boire
 *   - faim haute -> va manger
 *   - sinon -> va voir un enclos pas encore visite
 *   - de temps en temps -> un souvenir
 *   - enclos tous vus / joie faite / temps ecoule -> il part.
 * Il MARCHE reellement d'un point a l'autre (navigation de parc), il ne
 * teleporte pas et ne sautille pas sur place.
 */
public class VisitorBrainGoal extends Goal {

    public enum Step { VIEW, DRINK, EAT, SOUVENIR, ACTIVITY, REST, LEAVE }

    private final VisitorEntity visitor;
    private Step step = Step.VIEW;
    private BlockPos target;
    private int stepTimer;
    private int stuckTimer;
    private final List<java.util.UUID> visitedZones = new ArrayList<>();
    private int enclosuresSeen = 0;
    private int minEnclosures = 3;
    private net.minecraft.core.BlockPos lastEnclosureCenter;
    private final java.util.Set<Long> usedStations = new java.util.HashSet<>();
    private int lastViewedWelfare = 60;
    private boolean viewedSpecial = false;
    private int repathCd;

    public VisitorBrainGoal(VisitorEntity visitor) {
        this.visitor = visitor;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return !visitor.isLeaving() && visitor.hasTicket()
                && visitor.level() instanceof ServerLevel;
    }

    @Override
    public boolean canContinueToUse() {
        return !visitor.isLeaving();
    }

    @Override
    public void start() {
        if (visitor.level() instanceof ServerLevel level) {
            int total = 0;
            for (ZooZone z : ZooSavedData.get(level).getAllZones()) if (z.isAnimalZone()) total++;
            minEnclosures = Math.max(2, Math.min(6, total));
        }
        planNextStep();
    }

    @Override
    public void tick() {
        if (!(visitor.level() instanceof ServerLevel level)) return;
        stepTimer++;

        if (stepTimer % 40 == 0) {
            emitEmote(level);
        }

        if (target == null) { planNextStep(); return; }

        boolean serviceStep = step == Step.DRINK || step == Step.EAT
                || step == Step.SOUVENIR || step == Step.ACTIVITY;
        double arriveDist = 4.0;
        if (serviceStep && visitor.level() instanceof ServerLevel lvl) {
            int ahead = queueAhead(lvl);
            arriveDist = Math.max(4.0, Math.pow(1.2 + ahead * 1.3, 2));
        }

        visitor.getLookControl().setLookAt(target.getX() + 0.5, target.getY() + 1, target.getZ() + 0.5);
        double dist = visitor.distanceToSqr(target.getX() + 0.5, target.getY(), target.getZ() + 0.5);

        if (dist > arriveDist) {
            boolean queuing = serviceStep && dist < 36.0;
            if (visitor.getNavigation().isDone() && --repathCd <= 0) {
                visitor.navigateVia(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 1.0);
                repathCd = queuing ? 20 : 30;
            }
            if (visitor.getNavigation().isDone()) {
                int limit = queuing ? 300 : 100;
                if (++stuckTimer > limit) { stuckTimer = 0; planNextStep(); }
            } else {
                stuckTimer = 0;
            }
            return;
        }

        visitor.getNavigation().stop();
        resolveStep(level);
    }

    private void resolveStep(ServerLevel level) {
        switch (step) {
            case VIEW -> {
                int base = switch (visitor.getPace()) {
                    case 0 -> 50; case 2 -> 180; default -> 100;
                };
                int watchTime = base + (lastViewedWelfare >= 70 ? 60 : 0)
                        + (lastViewedWelfare < 0 ? -30 : 0)
                        + (viewedSpecial ? 80 : 0)
                        + (visitor.getHeldItem() == com.lex3d.ultimatezootaming.entities
                                .VisitorEntity.HELD_BINOCULARS ? 40 : 0);
                if (viewedSpecial && stepTimer % 30 == 10) {
                    level.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                            visitor.getX(), visitor.getEyeY() + 0.8, visitor.getZ(), 3, 0.2, 0.1, 0.2, 0.0);
                    level.sendParticles(net.minecraft.core.particles.ParticleTypes.HEART,
                            visitor.getX(), visitor.getEyeY() + 0.9, visitor.getZ(), 1, 0.15, 0.1, 0.15, 0.0);
                }
                if (visitor.getHeldItem() == VisitorEntity.HELD_SELFIE
                        && stepTimer == Math.max(20, watchTime / 2)) {
                    level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLASH,
                            visitor.getX(), visitor.getEyeY() + 0.9, visitor.getZ(), 1, 0, 0, 0, 0);
                    level.sendParticles(net.minecraft.core.particles.ParticleTypes.HEART,
                            visitor.getX(), visitor.getEyeY() + 1.1, visitor.getZ(), 2, 0.2, 0.1, 0.2, 0.0);
                    level.playSound(null, visitor.blockPosition(),
                            net.minecraft.sounds.SoundEvents.SPYGLASS_USE,
                            net.minecraft.sounds.SoundSource.NEUTRAL, 0.5f, 1.7f);
                    visitor.addJoy(8);
                    if (lastViewedWelfare >= 50) {
                        com.lex3d.ultimatezootaming.social.PostGenerator.postFrom(level, visitor);
                    }
                }
                if (stepTimer > Math.max(30, watchTime)) {
                    boolean happy = lastViewedWelfare >= 50;
                    visitor.addJoy(viewedSpecial ? 18 : happy ? 10 : 2);
                    String key = viewedSpecial ? "star_animal" : happy ? "happy_animals" : "sad_animals";
                    VisitorOpinion.say(level, visitor, key);
                    planNextStep();
                }
            }
            case DRINK -> {
                visitor.satisfyThirst(); visitor.addJoy(3);
                visitor.setHeldItem(VisitorEntity.HELD_SODA);
                planNextStep();
            }
            case EAT -> {
                visitor.satisfyHunger(); visitor.addJoy(3);
                int[] snacks = {VisitorEntity.HELD_POPCORN, VisitorEntity.HELD_ICECREAM, VisitorEntity.HELD_COTTON};
                visitor.setHeldItem(snacks[visitor.getRandom().nextInt(snacks.length)]);
                planNextStep();
            }
            case SOUVENIR -> {
                visitor.addJoy(5);
                visitor.setHeldItem(VisitorEntity.HELD_BALLOON);
                visitor.setBalloonColor(visitor.getRandom().nextInt(VisitorEntity.BALLOON_COLORS));
                VisitorOpinion.say(level, visitor, "learned");
                planNextStep();
            }
            case ACTIVITY -> {
                visitor.getNavigation().stop();
                visitor.getLookControl().setLookAt(
                        target.getX() + 0.5, target.getY() + 1, target.getZ() + 0.5);
                if (stepTimer % 20 == 5) {
                    com.lex3d.ultimatezootaming.blocks.StationEffect.play(level, target);
                    if (level.getBlockEntity(target)
                            instanceof com.lex3d.ultimatezootaming.blocks.StationBlockEntity sbe) {
                        sbe.triggerUse();
                    }
                    com.lex3d.ultimatezootaming.saveddata.ZooLedger.get(level).addSales(2);
                }
                if (stepTimer > 60) {
                    visitor.addJoy(12);
                    if (level.getBlockState(target).getBlock()
                            instanceof com.lex3d.ultimatezootaming.blocks.InteractionStationBlock stb) {
                        String key = switch (stb.getKind()) {
                            case PHOTO -> "photo_fun"; case FEED -> "feed_fun"; case WATER -> "water_fun";
                        };
                        VisitorOpinion.say(level, visitor, key);
                    }
                    planNextStep();
                }
            }
            case REST -> {
                visitor.getNavigation().stop();
                if (stepTimer == 50 && visitor.getRandom().nextInt(3) == 0
                        && visitor.level() instanceof ServerLevel psl) {
                    com.lex3d.ultimatezootaming.social.PostGenerator.postFrom(psl, visitor);
                    psl.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                            visitor.getX(), visitor.getEyeY() + 0.1, visitor.getZ(),
                            3, 0.1, 0.05, 0.1, 0.0);
                }
                if (stepTimer > 100) {
                    visitor.rest();
                    visitor.addJoy(4);
                    planNextStep();
                }
            }
            case LEAVE -> visitor.setLeaving(true);
        }
    }

    private int queueAhead(ServerLevel level) {
        if (target == null) return 0;
        net.minecraft.world.phys.Vec3 t = new net.minecraft.world.phys.Vec3(
                target.getX() + 0.5, target.getY(), target.getZ() + 0.5);
        double myDist = visitor.position().distanceToSqr(t);
        int ahead = 0;
        var box = visitor.getBoundingBox().inflate(8);
        for (VisitorEntity other : level.getEntitiesOfClass(VisitorEntity.class, box)) {
            if (other == visitor || other.isLeaving()) continue;
            double d = other.position().distanceToSqr(t);
            if (d < myDist && d < 36.0) ahead++;
        }
        return ahead;
    }

    private void emitEmote(ServerLevel level) {
        net.minecraft.core.particles.ParticleOptions p = null;
        double y = visitor.getEyeY() + 0.8;
        if (visitor.getThirst() > 70 || visitor.getHunger() > 70) {
            p = net.minecraft.core.particles.ParticleTypes.SPLASH;
        } else if (step == Step.VIEW && lastViewedWelfare >= 60) {
            p = net.minecraft.core.particles.ParticleTypes.HEART;
        } else if (step == Step.VIEW && lastViewedWelfare < 0) {
            p = net.minecraft.core.particles.ParticleTypes.SMOKE;
        } else if (step == Step.ACTIVITY || step == Step.SOUVENIR || visitor.getJoy() > 60) {
            p = net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER;
        }
        if (p != null) {
            level.sendParticles(p, visitor.getX(), y, visitor.getZ(), 1, 0.1, 0.1, 0.1, 0.0);
        }
    }

    private void planNextStep() {
        stepTimer = 0;
        stuckTimer = 0;
        target = null;
        if (!(visitor.level() instanceof ServerLevel level)) return;

        if (visitor.getFatigue() > 75) {
            BlockPos bench = com.lex3d.ultimatezootaming.blocks.ZooAmenityBlock.nearest(
                    com.lex3d.ultimatezootaming.blocks.ZooAmenityBlock.Kind.BENCH,
                    level, visitor.blockPosition(), 30);
            if (bench != null) { step = Step.REST; target = bench; return; }
        }
        if (visitor.getThirst() > 70) {
            BlockPos shop = findShop(level, com.lex3d.ultimatezootaming.blocks.ShopBlock.ShopType.DRINK);
            if (shop != null) { step = Step.DRINK; target = shop; return; }
            VisitorOpinion.say(level, visitor, "no_drink");
        }
        if (visitor.getHunger() > 70) {
            BlockPos shop = findShop(level, com.lex3d.ultimatezootaming.blocks.ShopBlock.ShopType.MEAL);
            if (shop == null) shop = findShop(level, com.lex3d.ultimatezootaming.blocks.ShopBlock.ShopType.ICECREAM);
            if (shop != null) { step = Step.EAT; target = shop; return; }
            VisitorOpinion.say(level, visitor, "no_food");
        }

        BlockPos enclosure = findUnseenEnclosure(level);
        if (enclosure != null) { step = Step.VIEW; target = enclosure; return; }

        if (visitor.getRandom().nextBoolean()) {
            BlockPos station = findInteractionStation(level);
            if (station != null) { step = Step.ACTIVITY; target = station; return; }
        }

        if (visitor.getJoy() > 40 && visitor.getRandom().nextInt(3) == 0) {
            BlockPos shop = findShop(level, com.lex3d.ultimatezootaming.blocks.ShopBlock.ShopType.SOUVENIR);
            if (shop != null) { step = Step.SOUVENIR; target = shop; return; }
        }

        if (enclosuresSeen < minEnclosures) {
            visitedZones.clear();
            BlockPos again = findUnseenEnclosure(level);
            if (again != null) { step = Step.VIEW; target = again; return; }
        }

        step = Step.LEAVE;
        visitor.setLeaving(true);
    }

    private BlockPos findUnseenEnclosure(ServerLevel level) {
        List<ZooZone> zones = new ArrayList<>();
        for (ZooZone z : ZooSavedData.get(level).getAllZones()) {
            if (z.isAnimalZone() && !visitedZones.contains(z.getId())) zones.add(z);
        }
        if (zones.isEmpty()) { return null; }
        var perso = visitor.getPersonalityEnum();
        java.util.Map<ZooZone, Double> scores = new java.util.HashMap<>();
        boolean raining = level.isRaining() || level.isThundering();
        for (ZooZone z : zones) {
            double dist = Math.sqrt(z.boundingBox().getCenter().distanceToSqr(visitor.position()));
            double score = 100.0 - Math.min(80.0, dist);
            score += profileBonus(level, z, perso);
            if (raining && isSheltered(level, z)) score += 55;
            scores.put(z, score);
        }
        zones.sort((a, b) -> Double.compare(scores.get(b), scores.get(a)));
        int pick = Math.min(zones.size(), 3);
        ZooZone best = zones.get(visitor.getRandom().nextInt(pick));
        if (best == null) return null;
        final ZooZone chosen = best;
        visitedZones.add(chosen.getId());
        enclosuresSeen++;
        lastEnclosureCenter = net.minecraft.core.BlockPos.containing(chosen.boundingBox().getCenter());
        var animals = level.getEntitiesOfClass(net.minecraft.world.entity.animal.Animal.class,
                chosen.boundingBox(), a -> a.isAlive() && chosen.contains(a.blockPosition()));
        viewedSpecial = false;
        var stars = com.lex3d.ultimatezootaming.config.ZooServerConfig.STAR_SPECIES.get();
        for (var a : animals) {
            if (a.isBaby()) { viewedSpecial = true; break; }
            if (stars != null && !stars.isEmpty()) {
                var id = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(a.getType());
                if (id != null && stars.contains(id.toString())) { viewedSpecial = true; break; }
            }
        }
        if (animals.isEmpty()) {
            lastViewedWelfare = -1;
        } else {
            int sum = 0, n = 0;
            for (var a : animals) {
                var d = a.getCapability(com.lex3d.ultimatezootaming.capability.CapabilityHandler.TAMING_DATA)
                        .resolve().orElse(null);
                if (d != null && d.isTamed()) { sum += d.getSatisfaction(); n++; }
            }
            lastViewedWelfare = n > 0 ? sum / n : -1;
        }
        var borders = chosen.borderColumns();
        if (borders.isEmpty()) return chosen.nearestFloorPos(visitor.blockPosition());
        BlockPos closest = null; double cd = Double.MAX_VALUE;
        for (BlockPos b : borders) {
            double d = b.distSqr(visitor.blockPosition());
            if (d < cd) { cd = d; closest = b; }
        }
        return closest;
    }

    private BlockPos findInteractionStation(ServerLevel level) {
        BlockPos best = com.lex3d.ultimatezootaming.blocks.InteractionStationBlock
                .nearestStation(level, visitor.blockPosition(), 30,
                        p -> !usedStations.contains(p.asLong()) && queueLength(level, p) < 3);
        if (best == null) return null;
        usedStations.add(best.asLong());
        return best;
    }

    private int queueLength(ServerLevel level, BlockPos station) {
        return level.getEntitiesOfClass(VisitorEntity.class,
                new net.minecraft.world.phys.AABB(station).inflate(3.0),
                v -> v != visitor && !v.isLeaving()).size();
    }

    private BlockPos findShop(ServerLevel level, com.lex3d.ultimatezootaming.blocks.ShopBlock.ShopType type) {
        BlockPos origin = visitor.blockPosition();
        List<BlockPos> found = new ArrayList<>();
        for (BlockPos p : com.lex3d.ultimatezootaming.blocks.ShopBlock.allShops(level)) {
            if (p.distSqr(origin) > 40 * 40) continue;
            if (level.getBlockEntity(p) instanceof com.lex3d.ultimatezootaming.blocks.ShopBlockEntity shop
                    && shop.getShopTypeEnum() == type) {
                found.add(p.immutable());
            }
        }
        if (found.isEmpty()) return null;
        found.sort((a, b) -> Double.compare(a.distSqr(origin), b.distSqr(origin)));
        int pick = Math.min(found.size(), 3);
        return found.get(visitor.getRandom().nextInt(pick));
    }

    /**
     * Reset complet a l'arret du goal.
     *
     * Sans ce reset, si le goal redemarre (reload de chunk, interruption),
     * enclosuresSeen garde son ancienne valeur : planNextStep() conclut
     * immediatement a LEAVE et le visiteur repart sans visiter. Meme probleme
     * pour visitedZones (enclos semblent deja vus) et usedStations.
     */
    @Override
    public void stop() {
        target = null;
        stepTimer = 0;
        stuckTimer = 0;
        enclosuresSeen = 0;
        visitedZones.clear();
        usedStations.clear();
        visitor.getNavigation().stop();
    }

    private double profileBonus(ServerLevel level, ZooZone zone,
                                com.lex3d.ultimatezootaming.entities.VisitorPersonality perso) {
        return switch (perso) {
            case PHOTOGRAPHER -> com.lex3d.ultimatezootaming.welfare.AmbianceScore.of(level, zone) * 8.0;
            case FAMILY -> {
                double b = 0;
                if (hasBaby(level, zone)) b += 45;
                if (hasStationNear(level, zone)) b += 25;
                yield b;
            }
            case LONE_CHILD -> hasStationNear(level, zone) ? 40 : 0;
            case BIRD_FAN -> hasBird(level, zone) ? 60 : -15;
            case CELEBRITY -> hasRare(level, zone) ? 50 : 0;
            default -> 0;
        };
    }

    private boolean hasBaby(ServerLevel level, ZooZone zone) {
        return !level.getEntitiesOfClass(net.minecraft.world.entity.animal.Animal.class,
                zone.boundingBox(),
                a -> a.isBaby() && zone.contains(a.blockPosition())).isEmpty();
    }

    private boolean hasBird(ServerLevel level, ZooZone zone) {
        for (var a : level.getEntitiesOfClass(net.minecraft.world.entity.animal.Animal.class,
                zone.boundingBox(), an -> zone.contains(an.blockPosition()))) {
            if (a instanceof net.minecraft.world.entity.animal.FlyingAnimal) return true;
            var id = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(a.getType());
            if (id == null) continue;
            String p = id.getPath();
            if (p.contains("parrot") || p.contains("chicken") || p.contains("bird")
                    || p.contains("owl") || p.contains("penguin") || p.contains("duck")
                    || p.contains("flamingo") || p.contains("eagle")) return true;
        }
        return false;
    }

    private boolean hasRare(ServerLevel level, ZooZone zone) {
        for (var a : level.getEntitiesOfClass(net.minecraft.world.entity.animal.Animal.class,
                zone.boundingBox(), an -> zone.contains(an.blockPosition()))) {
            var d = a.getCapability(
                    com.lex3d.ultimatezootaming.capability.CapabilityHandler.TAMING_DATA)
                    .resolve().orElse(null);
            if (d != null && d.getRarity() > 0) return true;
        }
        return false;
    }

    private boolean hasStationNear(ServerLevel level, ZooZone zone) {
        var center = net.minecraft.core.BlockPos.containing(zone.boundingBox().getCenter());
        for (int dx = -14; dx <= 14; dx += 2) {
            for (int dz = -14; dz <= 14; dz += 2) {
                for (int dy = -2; dy <= 2; dy++) {
                    if (level.getBlockState(center.offset(dx, dy, dz)).getBlock()
                            instanceof com.lex3d.ultimatezootaming.blocks.InteractionStationBlock) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isSheltered(ServerLevel level, ZooZone zone) {
        var borders = zone.borderColumns();
        if (borders.isEmpty()) return false;
        int checked = 0;
        for (BlockPos b : borders) {
            if (!level.canSeeSky(b.above())) return true;
            if (++checked >= 8) break;
        }
        return false;
    }
}
