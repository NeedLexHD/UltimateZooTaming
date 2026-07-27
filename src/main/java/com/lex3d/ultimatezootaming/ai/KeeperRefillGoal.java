package com.lex3d.ultimatezootaming.ai;

import com.lex3d.ultimatezootaming.blocks.FeederBlockEntity;
import com.lex3d.ultimatezootaming.entities.ZooKeeperEntity;
import com.lex3d.ultimatezootaming.items.KibbleItem;
import com.lex3d.ultimatezootaming.saveddata.ZooSavedData;
import com.lex3d.ultimatezootaming.zones.ZooZone;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Ravitaillement autonome : quand une Mangeoire de l'enclos a besoin de croquettes,
 * le Soigneur va chercher dans les COFFRES de l'enclos les bonnes croquettes et les
 * verse dans la mangeoire. Cycle : trouver mangeoire vide -> trouver coffre avec le
 * bon appat -> aller au coffre -> aller a la mangeoire -> transferer.
 */
public class KeeperRefillGoal extends Goal {

    private static final int TRANSFER_AMOUNT = 16; // par voyage
    private static int maxTravel() { int t = com.lex3d.ultimatezootaming.config.ZooServerConfig.KEEPER_TP_TIMEOUT.get(); return t <= 0 ? Integer.MAX_VALUE : t; }
    private static final int DEPOSIT_DELAY = 20;     // ~1s d'animation avant de deposer

    private int travelTicks;
    private int depositTicks;
    private int refillCooldown; // pause apres un remplissage pour eviter la boucle
    private final ZooKeeperEntity keeper;

    private FeederBlockEntity targetFeeder;
    private BlockPos chestPos;
    private ItemStack carried = ItemStack.EMPTY;
    private int phase; // 0 = va au coffre, 1 = va a la mangeoire

    public KeeperRefillGoal(ZooKeeperEntity keeper) {
        this.keeper = keeper;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    private ZooZone getZone() {
        if (!(keeper.level() instanceof ServerLevel level)) return null;
        var owned = keeper.getAssignedZones();
        if (owned.isEmpty()) return null;
        // Multi-enclos : le premier enclos valide de sa charge (le scan complet
        // des enclos a ravitailler se fait dans canUse()).
        for (var id : owned) {
            ZooZone z = ZooSavedData.get(level).getZone(id);
            if (z != null) return z;
        }
        return null;
    }

    /** Cherche du fourrage dans les coffres des zones de SALLE DE REPOS (type 1,
     *  qui sert aussi de stockage) ou des anciennes zones de stockage (type 3). */
    private BlockPos findChestInStorageZones(ServerLevel level, FeederBlockEntity feeder) {
        for (ZooZone z : ZooSavedData.get(level).getAllZones()) {
            if (z.getZoneType() != 1 && z.getZoneType() != 3) continue;
            for (BlockPos p : blocksInZone(z)) {
                if (level.getBlockEntity(p) instanceof net.minecraft.world.Container chest
                        && containerHasKibble(chest, feeder.storedItem())) {
                    return p;
                }
            }
        }
        return null;
    }

    /** Les zones que ce soigneur ravitaille : TOUT le zoo (pas besoin de l'assigner). */
    private java.util.List<ZooZone> zonesToServe() {
        if (!(keeper.level() instanceof ServerLevel level)) return java.util.List.of();
        // Tout soigneur ravitailleur agit dans TOUTES les zones a animaux. On les
        // trie par PROXIMITE (le nourrisseur traite le besoin le plus proche
        // d'abord au lieu d'un ordre arbitraire) = tournee plus efficace.
        java.util.List<ZooZone> all = new java.util.ArrayList<>();
        var owned = keeper.getAssignedZones();
        if (!owned.isEmpty()) {
            // MULTI-ENCLOS : s'il a une charge definie, il ne ravitaille QUE ses
            // enclos (jusqu'a 3). C'est ce qui permet de repartir le travail
            // entre plusieurs nourrisseurs au lieu que tous fassent tout le parc.
            for (var id : owned) {
                ZooZone z = ZooSavedData.get(level).getZone(id);
                if (z != null && z.isAnimalZone()) all.add(z);
            }
        } else {
            // Aucun enclos assigne : il couvre tout le zoo (comportement d'origine)
            for (ZooZone z : ZooSavedData.get(level).getAllZones()) if (z.isAnimalZone()) all.add(z);
        }
        net.minecraft.core.BlockPos kp = keeper.blockPosition();
        all.sort((a, b) -> Double.compare(
                a.boundingBox().getCenter().distanceToSqr(kp.getX(), kp.getY(), kp.getZ()),
                b.boundingBox().getCenter().distanceToSqr(kp.getX(), kp.getY(), kp.getZ())));
        return all;
    }

    @Override
    public boolean canUse() {
        if (refillCooldown > 0) {
            refillCooldown--;
            return false;
        }
        if (keeper.isOnStrike()) return false; // impaye : greve
        int job = keeper.getJob();
        if (job == 1 || job == 3 || job == 4) return false; // Vet / Gardien / Vendeur : pas de ravitaillement
        if (!(keeper.level() instanceof ServerLevel level)) return false;

        // Parcourt sa zone (ou TOUTES les zones si Nourrisseur)
        for (ZooZone zone : zonesToServe()) {
            // 0. Un enclos sans animaux n'a pas besoin de nourriture
            boolean hasAnimals = !level.getEntitiesOfClass(net.minecraft.world.entity.animal.Animal.class,
                    zone.boundingBox(), a -> a.isAlive() && zone.contains(a.blockPosition())
                            && a.getCapability(com.lex3d.ultimatezootaming.capability.CapabilityHandler.TAMING_DATA)
                                .resolve().map(com.lex3d.ultimatezootaming.capability.TamingData::isTamed).orElse(false)).isEmpty();
            if (!hasAnimals) continue;

            // 1. Une mangeoire de l'enclos a-t-elle VRAIMENT besoin d'etre ravitaillee ?
            FeederBlockEntity feeder = findFeederNeedingRefill(level, zone);
            if (feeder == null) continue;

            // 2. Un coffre de l'enclos contient-il du fourrage compatible ?
            BlockPos chest = findChestWithKibble(level, zone, feeder);
            if (chest == null) {
                // Fallback 1 : les coffres d'une ZONE DE STOCKAGE
                chest = findChestInStorageZones(level, feeder);
            }
            if (chest == null) {
                // Fallback 2 : le vestiaire QUI CONTIENT VRAIMENT le fourrage
                // (avant on prenait le plus proche meme vide -> jamais de ravitaillement)
                var bb = zone.boundingBox();
                BlockPos center = BlockPos.containing((bb.minX + bb.maxX) / 2, bb.minY, (bb.minZ + bb.maxZ) / 2);
                chest = com.lex3d.ultimatezootaming.blocks.KeeperLockerBlock
                        .nearestLockerWith(level, center, 128,
                                c -> containerHasKibble(c, feeder.storedItem()));
            }
            if (chest == null) continue;

            this.targetFeeder = feeder;
            this.chestPos = chest;
            this.phase = 0;
            return true;
        }
        // Rien a ravitailler : on attend 1s avant de re-scanner tous les enclos
        // (evite de parcourir tout le zoo a chaque tick = perte de perf).
        refillCooldown = 20;
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        return targetFeeder != null && !targetFeeder.isRemoved() && chestPos != null;
    }

    @Override
    public void stop() {
        keeper.setTask(com.lex3d.ultimatezootaming.entities.KeeperTask.IDLE);
        // Si on portait des croquettes sans les avoir deposees, on les remet au coffre
        if (!carried.isEmpty() && chestPos != null && keeper.level().getBlockEntity(chestPos) instanceof Container chest) {
            returnToContainer(chest, carried);
        }
        carried = ItemStack.EMPTY;
        targetFeeder = null;
        chestPos = null;
        keeper.setAction(0);
        keeper.getNavigation().stop();
    }

    @Override
    public void start() {
        keeper.setTask(com.lex3d.ultimatezootaming.entities.KeeperTask.FEEDING);
        travelTicks = 0;
        depositTicks = 0;
    }

    @Override
    public void tick() {
        if (phase == 0) {
            // Aller au coffre
            keeper.getNavigation().moveTo(chestPos.getX() + 0.5, chestPos.getY(), chestPos.getZ() + 0.5, 1.0);
            keeper.getLookControl().setLookAt(chestPos.getX() + 0.5, chestPos.getY() + 0.5, chestPos.getZ() + 0.5);
            keeper.setAction(0); // pas d'animation de versement en allant chercher

            double dxc = keeper.getX() - (chestPos.getX() + 0.5);
            double dzc = keeper.getZ() - (chestPos.getZ() + 0.5);
            if (dxc * dxc + dzc * dzc <= 6.25) {
                takeFromChest();
                travelTicks = 0;
                if (carried.isEmpty()) { // coffre vide/mauvais type : on arrete
                    refillCooldown = 100;
                    targetFeeder = null;
                    chestPos = null;
                }
            } else if (++travelTicks > maxTravel()) {
                // Filet de securite : teleportation discrete au coffre
                keeper.moveTo(chestPos.getX() + 0.5, chestPos.getY() + 1, chestPos.getZ() + 0.5,
                        keeper.getYRot(), keeper.getXRot());
                keeper.getNavigation().stop();
                travelTicks = 0;
            }
        } else {
            // Phase 1 : porter les croquettes a la mangeoire
            if (carried.isEmpty()) {
                refillCooldown = 100;
                keeper.setAction(0);
                targetFeeder = null;
                chestPos = null;
                return;
            }
            BlockPos fp = targetFeeder.getBlockPos();
            double dx = keeper.getX() - (fp.getX() + 0.5);
            double dz = keeper.getZ() - (fp.getZ() + 0.5);
            if (dx * dx + dz * dz <= 6.25) { // ~2.5 blocs (distance horizontale)
                keeper.getNavigation().stop();
                keeper.getLookControl().setLookAt(fp.getX() + 0.5, fp.getY() + 0.5, fp.getZ() + 0.5);
                keeper.setAction(2); // animation de versement
                if (++depositTicks >= DEPOSIT_DELAY) {
                    depositInFeeder();
                    depositTicks = 0;
                }
            } else {
                keeper.setAction(0);
                depositTicks = 0;
                // MANGEOIRE IMMERGEE : la navigation terrestre ne descend pas sous
                // l'eau. On bascule alors sur une nage dirigee, avec surveillance
                // de l'oxygene.
                if (isSubmerged(fp)) {
                    swimToward(fp);
                } else {
                    keeper.getNavigation().moveTo(fp.getX() + 0.5, fp.getY(), fp.getZ() + 0.5, 1.0);
                    keeper.getLookControl().setLookAt(fp.getX() + 0.5, fp.getY() + 0.5, fp.getZ() + 0.5);
                }
                if (++travelTicks > maxTravel()) {
                    keeper.moveTo(fp.getX() + 0.5, fp.getY() + 1, fp.getZ() + 0.5,
                            keeper.getYRot(), keeper.getXRot());
                    keeper.getNavigation().stop();
                    travelTicks = 0;
                }
            }
        }
    }



    private void takeFromChest() {
        if (!(keeper.level().getBlockEntity(chestPos) instanceof Container chest)) {
            chestPos = null;
            return;
        }
        net.minecraft.world.item.Item wanted = targetFeeder.storedItem(); // null si vide
        for (int i = 0; i < chest.getContainerSize(); i++) {
            ItemStack slot = chest.getItem(i);
            boolean feedable = slot.getItem() instanceof com.lex3d.ultimatezootaming.items.FodderItem;
            if (feedable && (wanted == null || slot.getItem() == wanted)) {
                int take = Math.min(TRANSFER_AMOUNT, slot.getCount());
                carried = slot.copyWithCount(take);
                slot.shrink(take);
                chest.setChanged();
                phase = 1;
                return;
            }
        }
        // Rien trouve (le coffre a change) : on abandonne ce cycle
        chestPos = null;
    }

    private void depositInFeeder() {
        if (carried.isEmpty()) { phase = 0; return; }
        int inserted = targetFeeder.insertKibble(carried);
        carried.shrink(inserted);
        if (inserted > 0) keeper.addXp(1); // Nourrissage = 1 XP
        keeper.level().playSound(null, targetFeeder.getBlockPos(),
                net.minecraft.sounds.SoundEvents.COMPOSTER_FILL,
                net.minecraft.sounds.SoundSource.BLOCKS, 0.8f, 1.1f);
        if (keeper.level() instanceof ServerLevel sl) {
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                    targetFeeder.getBlockPos().getX() + 0.5, targetFeeder.getBlockPos().getY() + 0.5,
                    targetFeeder.getBlockPos().getZ() + 0.5, 4, 0.2, 0.2, 0.2, 0.02);
        }
        // Si la mangeoire est pleine mais qu'il reste des croquettes en main, on les rapporte
        if (!carried.isEmpty() && keeper.level().getBlockEntity(chestPos) instanceof Container chest) {
            returnToContainer(chest, carried);
        }
        carried = ItemStack.EMPTY;
        keeper.setAction(0);
        // Pause de 5s avant un eventuel prochain remplissage : evite le spam d'animation
        refillCooldown = com.lex3d.ultimatezootaming.config.ZooServerConfig.KEEPER_REFILL_COOLDOWN.get();
        // Fin du voyage : canUse relancera un cycle si besoin
        targetFeeder = null;
        chestPos = null;
    }

    private void returnToContainer(Container container, ItemStack stack) {
        for (int i = 0; i < container.getContainerSize() && !stack.isEmpty(); i++) {
            ItemStack slot = container.getItem(i);
            if (slot.isEmpty()) {
                container.setItem(i, stack.copy());
                stack.setCount(0);
            } else if (ItemStack.isSameItemSameTags(slot, stack)) {
                int space = slot.getMaxStackSize() - slot.getCount();
                int moved = Math.min(space, stack.getCount());
                slot.grow(moved);
                stack.shrink(moved);
            }
        }
        container.setChanged();
    }

    private FeederBlockEntity findFeederNeedingRefill(ServerLevel level, ZooZone zone) {
        for (BlockPos pos : blocksInZone(zone)) {
            if (level.getBlockEntity(pos) instanceof FeederBlockEntity feeder && feeder.needsRefill()) {
                return feeder;
            }
        }
        return null;
    }

    private BlockPos findChestWithKibble(ServerLevel level, ZooZone zone, FeederBlockEntity feeder) {
        net.minecraft.world.item.Item wanted = feeder.storedItem();
        for (BlockPos pos : blocksInZone(zone)) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ChestBlockEntity || be instanceof net.minecraft.world.level.block.entity.BarrelBlockEntity) {
                if (be instanceof Container chest && containerHasKibble(chest, wanted)) {
                    return pos;
                }
            }
        }
        return null;
    }

    private boolean containerHasKibble(Container chest, net.minecraft.world.item.Item wanted) {
        for (int i = 0; i < chest.getContainerSize(); i++) {
            ItemStack slot = chest.getItem(i);
            boolean feedable = slot.getItem() instanceof com.lex3d.ultimatezootaming.items.FodderItem;
            if (feedable && (wanted == null || slot.getItem() == wanted)) return true;
        }
        return false;
    }

    /** Positions a scanner pour trouver mangeoires et coffres : uniquement la
     *  SURFACE de l'enclos (colonnes du sol +/- quelques blocs), PAS tout le
     *  volume de 40 blocs de haut. Mangeoires et coffres sont poses au sol.
     *  Indispensable : scanner tout le volume gelait le serveur sur un grand enclos. */
    private List<BlockPos> blocksInZone(ZooZone zone) {
        List<BlockPos> result = new ArrayList<>();
        // Les colonnes du sol de l'enclos (un Set<Long> compact, deja calcule)
        for (long packed : zone.floorColumns()) {
            BlockPos floor = BlockPos.of(packed);
            // mangeoires/coffres reposent juste au-dessus du sol : on regarde sol..+2
            for (int dy = -2; dy <= 3; dy++) {
                result.add(floor.above(dy));
            }
        }
        return result;
    }

    /** La mangeoire est-elle sous l'eau ? */
    private boolean isSubmerged(net.minecraft.core.BlockPos p) {
        return !keeper.level().getFluidState(p).isEmpty()
                || !keeper.level().getFluidState(p.above()).isEmpty();
    }

    /**
     * Nage dirigee vers une mangeoire immergee.
     *
     * On n'utilise pas la navigation : elle est terrestre et refuse de plonger.
     * On pousse directement l'employe vers la cible, en trois dimensions.
     *
     * SURVEILLANCE DE L'OXYGENE : sous un tiers d'air restant, il abandonne la
     * plongee et remonte respirer. Le ravitaillement reprendra au cycle suivant.
     */
    private void swimToward(net.minecraft.core.BlockPos target) {
        keeper.getNavigation().stop();

        int air = keeper.getAirSupply();
        int maxAir = keeper.getMaxAirSupply();
        if (air < maxAir / 3) {
            // Plus assez de souffle : on remonte a la verticale
            keeper.setDeltaMovement(keeper.getDeltaMovement().add(0, 0.06, 0));
            keeper.getLookControl().setLookAt(keeper.getX(), keeper.getY() + 4, keeper.getZ());
            return;
        }

        double dx = (target.getX() + 0.5) - keeper.getX();
        double dy = (target.getY() + 0.5) - keeper.getY();
        double dz = (target.getZ() + 0.5) - keeper.getZ();
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 0.001) return;

        // Poussee douce : on ajoute au mouvement plutot que de l'ecraser, pour
        // que la physique de l'eau garde son effet d'amortissement.
        double speed = 0.035;
        keeper.setDeltaMovement(keeper.getDeltaMovement().add(
                dx / len * speed, dy / len * speed + 0.005, dz / len * speed));
        keeper.getLookControl().setLookAt(
                target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5);

        // Quelques bulles pour rendre la plongee lisible
        if (keeper.level() instanceof ServerLevel sl && keeper.tickCount % 8 == 0) {
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.BUBBLE,
                    keeper.getX(), keeper.getEyeY(), keeper.getZ(), 2, 0.15, 0.1, 0.15, 0.01);
        }
    }
}
