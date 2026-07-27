package com.lex3d.ultimatezootaming.blocks;

import com.lex3d.ultimatezootaming.capability.TamingUtil;
import com.lex3d.ultimatezootaming.core.init.ModBlockEntities;
import com.lex3d.ultimatezootaming.core.init.ModItems;
import com.lex3d.ultimatezootaming.core.init.ModSounds;
import com.lex3d.ultimatezootaming.items.KibbleItem;
import com.lex3d.ultimatezootaming.items.OccupiedContainerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.Animation;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * TOUTES les fonctions de base des versions precedentes sont conservees :
 * multiblock master/part, appat visible (blockstate BAITED), detection par
 * collision reelle + backup tick, attraction via BaitedTrapRegistry, tame
 * automatique a la capture, particules/sons succes-echec, anti-duplication.
 *
 * NOUVEAU (V2) :
 * - GeckoLib : modele 3D anime (cage guillotine). Animations : idle_baited
 *   (socle qui pulse), trigger (porte qui claque), fail (porte qui remonte).
 * - SEQUENCE de capture : collision -> la porte claque (0.35s) -> resolution.
 *   Le resultat n'est plus instantane, on VOIT le piege se declencher.
 * - CHARGES d'appat : un appat = plusieurs tentatives (3/4/5 selon le tier de
 *   croquette, 2 pour un item quelconque) au lieu d'etre consomme au 1er echec.
 * - COOLDOWN de fuite : un mob qui vient d'echouer est repousse et ignore par
 *   la cage pendant 30s (plus de spam d'echecs en boucle).
 */
public class TrappingCageBlockEntity extends BlockEntity implements GeoBlockEntity {

    private static final Random RNG = new Random();
    private static final int BACKUP_TICK_INTERVAL = 5;
    private static final int CAPTURE_DELAY_TICKS = 7;   // = 0.35s, duree de l'anim "trigger"
    private static final long FAIL_COOLDOWN_TICKS = 600; // 30s

    private static final RawAnimation IDLE_BAITED =
            RawAnimation.begin().thenLoop("animation.trapping_cage.idle_baited");
    private static final RawAnimation TRIGGER_ANIM =
            RawAnimation.begin().then("animation.trapping_cage.trigger", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation FAIL_ANIM =
            RawAnimation.begin().then("animation.trapping_cage.fail", Animation.LoopType.PLAY_ONCE);

    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);

    private final TrappingCageBlock.CageSize size;
    private BlockPos masterPos;
    private ItemStack bait = ItemStack.EMPTY;
    private UUID baiterUUID;
    private int sourceDamage; // usure de l'item qui a pose ce piege (rendue a la liberation)
    private int baitCharges;
    private int backupCounter;

    // Sequence de capture en cours (master uniquement)
    private UUID pendingTargetUUID;
    private int pendingTicks;

    // Cooldown de fuite par mob (master uniquement, transitoire, pas persiste)
    private final Map<UUID, Long> recentFailures = new HashMap<>();

    public TrappingCageBlockEntity(BlockPos pos, BlockState state, TrappingCageBlock.CageSize size) {
        super(ModBlockEntities.TRAPPING_CAGE.get(), pos, state);
        this.size = size;
        this.masterPos = pos;
    }

    public TrappingCageBlockEntity(BlockPos pos, BlockState state) {
        this(pos, state, state.getBlock() instanceof TrappingCageBlock cageBlock
                ? cageBlock.getSize() : TrappingCageBlock.CageSize.SMALL);
    }

    // ------------------------------------------------------------------
    // GeckoLib
    // ------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 0, state -> {
            // L'anim d'idle est pilotee par le blockstate BAITED (sync automatiquement
            // au client, pas besoin de reseau custom pour ca).
            BlockState blockState = getBlockState();
            if (blockState.hasProperty(TrappingCageBlock.BAITED)
                    && blockState.getValue(TrappingCageBlock.BAITED)) {
                return state.setAndContinue(IDLE_BAITED);
            }
            return PlayState.STOP;
        })
                .triggerableAnim("trigger", TRIGGER_ANIM)
                .triggerableAnim("fail", FAIL_ANIM));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animCache;
    }

    /** Culling correct pour le modele scale NxN rendu depuis le master (voir TrappingCageRenderer). */
    @Override
    public AABB getRenderBoundingBox() {
        int n = size.getRadius();
        return new AABB(worldPosition).expandTowards(n - 1, n + 1, n - 1).inflate(1);
    }

    // ------------------------------------------------------------------
    // Multiblock / etat (identique aux versions precedentes)
    // ------------------------------------------------------------------

    public void setMasterPos(BlockPos masterPos) {
        this.masterPos = masterPos;
        setChanged();
    }

    public BlockPos getMasterPos() {
        return masterPos;
    }

    public boolean isMaster() {
        return masterPos.equals(worldPosition);
    }

    public TrappingCageBlockEntity resolveMaster() {
        if (isMaster()) return this;
        if (level == null) return null;
        if (level.getBlockEntity(masterPos) instanceof TrappingCageBlockEntity master) {
            return master;
        }
        return null;
    }

    public TrappingCageBlock.CageSize getSize() {
        return size;
    }

    public void setSourceDamage(int damage) {
        this.sourceDamage = damage;
        setChanged();
    }

    public boolean hasBait() {
        return !bait.isEmpty();
    }

    /** Pour le renderer : l'appat reellement pose, affiche en 3D sur le socle. */
    public ItemStack getBait() {
        return bait;
    }

    public void setBait(ItemStack stack, UUID placedBy) {
        this.bait = stack;
        this.baiterUUID = placedBy;

        // CHARGES : croquette = 3/4/5 selon tier ; item quelconque = 2 tentatives
        if (stack.getItem() instanceof KibbleItem kibble) {
            this.baitCharges = 3 + kibble.getTier().ordinal();
        } else {
            this.baitCharges = 2;
        }
        setChanged();

        if (level != null) {
            updateBaitedState(true);
            BaitedTrapRegistry.register(level, worldPosition, getBaitDiet(), size.getMaxMobSize());
            syncToClient();
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.CRIT,
                        worldPosition.getX() + 0.5, worldPosition.getY() + 0.3, worldPosition.getZ() + 0.5,
                        10, 0.2, 0.1, 0.2, 0.05);
            }
            level.playSound(null, worldPosition, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1.0f, 1.3f);
        }
    }


    /** Regime de l'appat pose (null si ce n'est pas une croquette -> n'attire personne). */
    @javax.annotation.Nullable
    private KibbleItem.Diet getBaitDiet() {
        return bait.getItem() instanceof KibbleItem kibble ? kibble.getDiet() : null;
    }

    private void updateBaitedState(boolean baited) {
        if (level == null) return;
        BlockState current = level.getBlockState(worldPosition);
        if (current.hasProperty(TrappingCageBlock.BAITED) && current.getValue(TrappingCageBlock.BAITED) != baited) {
            level.setBlock(worldPosition, current.setValue(TrappingCageBlock.BAITED, baited), 3);
        }
    }

    // ------------------------------------------------------------------
    // Detection (collision principale + backup) et sequence de capture
    // ------------------------------------------------------------------


    /**
     * Regle stricte des regimes : une croquette ne peut capturer QUE les mobs de
     * SON regime (une croquette Piscivore ne fait rien sur un mouton, ni porte ni
     * charge consommee). Un appat generique (non-croquette) reste permissif.
     */
    private boolean baitWorksOn(LivingEntity target) {
        if (bait.getItem() instanceof KibbleItem kibble) {
            return kibble.getDiet().matches(target);
        }
        return true;
    }

    /** La cage ne peut contenir qu'un mob dont la hitbox tient dans sa capacite. */
    private boolean fitsInTrap(LivingEntity target) {
        return Math.max(target.getBbWidth(), target.getBbHeight()) <= size.getMaxMobSize();
    }

    /** Detection PRINCIPALE, appelee par TrappingCageBlock#entityInside (collision reelle). */
    public void onEntityCollide(Entity entity) {
        if (level == null || level.isClientSide() || bait.isEmpty()) return;
        if (pendingTargetUUID != null) return; // sequence deja en cours
        if (!(entity instanceof LivingEntity target) || entity instanceof Player) return;
        if (!target.isAlive()) return;
        if (isOnFailCooldown(target)) return;
        if (!baitWorksOn(target)) return; // mauvais regime : le piege ignore ce mob
        if (!fitsInTrap(target)) return;  // trop gros pour cette cage : ignore

        startCaptureSequence(target);
    }

    /** Tick serveur du master : resolution des sequences en cours + detection de secours. */
    public void serverTick() {
        if (level == null || level.isClientSide() || !isMaster()) return;

        // 1. Resolution d'une sequence en cours (la porte a claque, on attend 0.35s)
        if (pendingTargetUUID != null) {
            pendingTicks--;
            if (pendingTicks <= 0) {
                resolvePendingCapture();
            }
            return; // pas de nouvelle detection tant que la sequence n'est pas resolue
        }

        // 2. Detection de secours (filet de securite en plus de entityInside)
        if (bait.isEmpty()) return;
        backupCounter++;
        if (backupCounter < BACKUP_TICK_INTERVAL) return;
        backupCounter = 0;

        int n = size.getRadius();
        AABB zone = new AABB(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                worldPosition.getX() + n, worldPosition.getY() + 1, worldPosition.getZ() + n);

        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, zone,
                e -> !(e instanceof Player) && e.isAlive() && !isOnFailCooldown(e) && baitWorksOn(e) && fitsInTrap(e));

        if (!candidates.isEmpty()) {
            startCaptureSequence(candidates.get(0));
        }
    }

    private boolean isOnFailCooldown(Entity entity) {
        Long until = recentFailures.get(entity.getUUID());
        return until != null && level.getGameTime() < until;
    }

    /** La porte claque : anim + son, la resolution arrive CAPTURE_DELAY_TICKS plus tard. */
    private void startCaptureSequence(LivingEntity target) {
        // Jamais capturer un Soigneur
        if (target instanceof com.lex3d.ultimatezootaming.entities.ZooKeeperEntity) return;
        pendingTargetUUID = target.getUUID();
        pendingTicks = CAPTURE_DELAY_TICKS;

        triggerAnim("main", "trigger");
        level.playSound(null, worldPosition, ModSounds.CAGE_SLAM.get(), SoundSource.BLOCKS, 1.0f, 1.0f);
    }

    private void resolvePendingCapture() {
        UUID targetUUID = pendingTargetUUID;
        pendingTargetUUID = null;

        if (!(level instanceof ServerLevel serverLevel)) return;

        Entity entity = serverLevel.getEntity(targetUUID);
        // Cible partie/morte pendant le claquement : la porte remonte, aucune charge consommee
        int n = size.getRadius();
        AABB captureZone = new AABB(worldPosition.getX() - 0.5, worldPosition.getY() - 0.5, worldPosition.getZ() - 0.5,
                worldPosition.getX() + n + 0.5, worldPosition.getY() + 1.5, worldPosition.getZ() + n + 0.5);
        if (!(entity instanceof LivingEntity target) || !target.isAlive()
                || !captureZone.intersects(target.getBoundingBox())) {
            triggerAnim("main", "fail");
            return;
        }

        attemptCapture(target);
    }

    private void attemptCapture(LivingEntity target) {
        boolean isKibbleBait = bait.getItem() instanceof KibbleItem;
        float dietBonus;
        if (isKibbleBait) {
            KibbleItem kibble = (KibbleItem) bait.getItem();
            dietBonus = kibble.getDiet().matches(target) ? kibble.getTier().getBaseChance() : kibble.getTier().getBaseChance() * 0.5f;
        } else {
            dietBonus = 0.2f;
        }

        float toughnessMalus = (float) Math.min(0.3, target.getMaxHealth() / 300.0);
        float sizeBonus = size.getRadius() * 0.05f;

        float chance = 0.4f + dietBonus + sizeBonus - toughnessMalus;
        // Multiplicateur d'equilibrage global (config serveur), comme le taming a la main
        chance *= (float) (double) com.lex3d.ultimatezootaming.config.ZooServerConfig.GLOBAL_CAPTURE_MULTIPLIER.get();
        chance = Math.max(0.15f, Math.min(0.95f, chance));

        boolean success = RNG.nextFloat() < chance;

        ServerLevel serverLevel = (ServerLevel) level;

        double px = target.getX();
        double py = target.getY() + target.getBbHeight() / 2.0;
        double pz = target.getZ();

        if (success) {
            if (target instanceof Mob mob && baiterUUID != null) {
                TamingUtil.tame(mob, baiterUUID, true);
            }

            ItemStack filledCage = OccupiedContainerItem.capture(
                    ModItems.OCCUPIED_CONTAINER.get().getDefaultInstance(), target, true, baiterUUID,
                    getBlockState().getBlock().asItem(), sourceDamage);

            serverLevel.sendParticles(ParticleTypes.HEART, px, py, pz, 8, 0.3, 0.3, 0.3, 0.02);
            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, px, py, pz, 6, 0.4, 0.4, 0.4, 0.02);

            target.discard();
            clearBaitState();

            Block.popResource(level, worldPosition, filledCage);
            clearStructure();
            level.playSound(null, worldPosition, ModSounds.CAGE_SUCCESS.get(), SoundSource.BLOCKS, 1.0f, 1.0f);
        } else {
            // ECHEC : porte remonte, fumee, le mob est repousse et mis en cooldown,
            // et on consomme UNE charge (l'appat n'est vide qu'a 0 charge).
            triggerAnim("main", "fail");
            serverLevel.sendParticles(ParticleTypes.SMOKE, px, py, pz, 10, 0.3, 0.3, 0.3, 0.02);
            level.playSound(null, worldPosition, ModSounds.CAGE_FAIL.get(), SoundSource.BLOCKS, 1.0f, 1.0f);

            recentFailures.put(target.getUUID(), level.getGameTime() + FAIL_COOLDOWN_TICKS);
            double dx = target.getX() - (worldPosition.getX() + size.getRadius() / 2.0);
            double dz = target.getZ() - (worldPosition.getZ() + size.getRadius() / 2.0);
            target.knockback(0.6, -dx, -dz);

            baitCharges--;
            if (baitCharges <= 0) {
                clearBaitState();
                updateBaitedState(false);
            }
            setChanged();
            syncToClient();
        }
    }

    private void clearBaitState() {
        bait = ItemStack.EMPTY;
        baiterUUID = null;
        baitCharges = 0;
        if (level != null) {
            BaitedTrapRegistry.unregister(level, worldPosition);
            syncToClient();
        }
    }

    private void clearStructure() {
        int n = size.getRadius();
        for (int dx = 0; dx < n; dx++) {
            for (int dz = 0; dz < n; dz++) {
                BlockPos cellPos = worldPosition.offset(dx, 0, dz);
                if (level.getBlockState(cellPos).getBlock() instanceof TrappingCageBlock) {
                    level.setBlock(cellPos, Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && hasBait()) {
            BaitedTrapRegistry.unregister(level, worldPosition);
        }
    }

    // ------------------------------------------------------------------
    // Persistence + sync client (pour le rendu 3D de l'appat)
    // ------------------------------------------------------------------

    private void syncToClient() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (!bait.isEmpty()) {
            tag.put("Bait", bait.save(new CompoundTag()));
        }
        if (baiterUUID != null) {
            tag.putUUID("BaiterUUID", baiterUUID);
        }
        tag.putInt("BaitCharges", baitCharges);
        tag.putInt("SourceDamage", sourceDamage);
        tag.putInt("MasterX", masterPos.getX());
        tag.putInt("MasterY", masterPos.getY());
        tag.putInt("MasterZ", masterPos.getZ());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        bait = tag.contains("Bait") ? ItemStack.of(tag.getCompound("Bait")) : ItemStack.EMPTY;
        baiterUUID = tag.hasUUID("BaiterUUID") ? tag.getUUID("BaiterUUID") : null;
        baitCharges = tag.getInt("BaitCharges");
        sourceDamage = tag.getInt("SourceDamage");
        if (tag.contains("MasterX")) {
            masterPos = new BlockPos(tag.getInt("MasterX"), tag.getInt("MasterY"), tag.getInt("MasterZ"));
        }
    }

    @Override
    public void setLevel(net.minecraft.world.level.Level level) {
        super.setLevel(level);
        if (!level.isClientSide() && hasBait() && isMaster()) {
            BaitedTrapRegistry.register(level, worldPosition, getBaitDiet(), size.getMaxMobSize());
        }
    }
}
