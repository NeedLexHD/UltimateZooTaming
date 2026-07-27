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
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.entity.animal.Dolphin;
import net.minecraft.world.entity.animal.Squid;
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

import javax.annotation.Nullable;
import java.util.Random;
import java.util.UUID;

/**
 * Filet aquatique ANIME (GeckoLib) : la nappe ondule dans l'eau en continu, le
 * socle pulse quand un appat est charge, et a la detection d'un poisson la nappe
 * SE REFERME (0.35s) avant la resolution -- meme sequence de game feel que la
 * porte guillotine de la cage.
 *
 * TOUTE la logique existante est conservee : multiblock master/part NxN, regime
 * strict (croquette obligatoirement Piscivore), attraction filtree, tame auto,
 * usure du contenant (sourceDamage), sync client de l'appat.
 */
public class NetTrapBlockEntity extends BlockEntity implements GeoBlockEntity {

    private static final Random RNG = new Random();
    private static final int BACKUP_TICK_INTERVAL = 5;
    private static final int CAPTURE_DELAY_TICKS = 7; // = 0.35s, duree de l'anim "trigger"

    private static final RawAnimation IDLE =
            RawAnimation.begin().thenLoop("animation.net_trap.idle");
    private static final RawAnimation IDLE_BAITED =
            RawAnimation.begin().thenLoop("animation.net_trap.idle_baited");
    private static final RawAnimation TRIGGER_ANIM =
            RawAnimation.begin().then("animation.net_trap.trigger", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation FAIL_ANIM =
            RawAnimation.begin().then("animation.net_trap.fail", Animation.LoopType.PLAY_ONCE);

    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);

    private final NetTrapBlock.NetTier tier;
    private BlockPos masterPos;
    private ItemStack bait = ItemStack.EMPTY;
    private UUID baiterUUID;
    private int sourceDamage; // usure de l'item qui a pose ce piege (rendue a la liberation)
    private int backupCounter;

    // Sequence de capture en cours (master uniquement)
    private UUID pendingTargetUUID;
    private int pendingTicks;

    public NetTrapBlockEntity(BlockPos pos, BlockState state, NetTrapBlock.NetTier tier) {
        super(ModBlockEntities.NET_TRAP.get(), pos, state);
        this.tier = tier;
        this.masterPos = pos;
    }

    public NetTrapBlockEntity(BlockPos pos, BlockState state) {
        this(pos, state, state.getBlock() instanceof NetTrapBlock netBlock
                ? netBlock.getTier() : NetTrapBlock.NetTier.SMALL);
    }

    // ------------------------------------------------------------------
    // GeckoLib
    // ------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 0, state -> {
            BlockState blockState = getBlockState();
            boolean baited = blockState.hasProperty(NetTrapBlock.BAITED)
                    && blockState.getValue(NetTrapBlock.BAITED);
            return state.setAndContinue(baited ? IDLE_BAITED : IDLE);
        })
                .triggerableAnim("trigger", TRIGGER_ANIM)
                .triggerableAnim("fail", FAIL_ANIM));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animCache;
    }

    /** Culling correct pour le modele scale NxN rendu depuis le master. */
    @Override
    public AABB getRenderBoundingBox() {
        int n = tier.getRadius();
        return new AABB(worldPosition).expandTowards(n - 1, 2, n - 1).inflate(1);
    }

    // ------------------------------------------------------------------
    // Multiblock / etat
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

    public NetTrapBlock.NetTier getTier() {
        return tier;
    }

    public NetTrapBlockEntity resolveMaster() {
        if (isMaster()) return this;
        if (level == null) return null;
        if (level.getBlockEntity(masterPos) instanceof NetTrapBlockEntity master) {
            return master;
        }
        return null;
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
        setChanged();

        if (level != null) {
            updateBaitedState(true);
            BaitedTrapRegistry.register(level, worldPosition, getBaitDiet(), tier.getMaxMobSize());
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.CRIT,
                        worldPosition.getX() + 0.5, worldPosition.getY() + 0.3, worldPosition.getZ() + 0.5,
                        10, 0.2, 0.1, 0.2, 0.05);
            }
            level.playSound(null, worldPosition, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1.0f, 1.3f);
            syncToClient();
        }
    }

    /** Regime de l'appat pose (null si ce n'est pas une croquette -> n'attire personne). */
    @Nullable
    private KibbleItem.Diet getBaitDiet() {
        return bait.getItem() instanceof KibbleItem kibble ? kibble.getDiet() : null;
    }

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

    private void updateBaitedState(boolean baited) {
        if (level == null) return;
        BlockState current = level.getBlockState(worldPosition);
        if (current.hasProperty(NetTrapBlock.BAITED) && current.getValue(NetTrapBlock.BAITED) != baited) {
            level.setBlock(worldPosition, current.setValue(NetTrapBlock.BAITED, baited), 3);
        }
    }

    // ------------------------------------------------------------------
    // Detection + sequence de capture (la nappe se referme AVANT la resolution)
    // ------------------------------------------------------------------

    /** Regle stricte : une croquette dans un filet doit etre PISCIVORE, sinon le filet ignore tout. */
    private boolean baitWorksOn(LivingEntity target) {
        if (bait.getItem() instanceof KibbleItem kibble) {
            return kibble.getDiet() == KibbleItem.Diet.PISCIVORE;
        }
        return true;
    }

    /** Le filet ne cible que les mobs aquatiques. */
    private boolean isAquatic(Entity entity) {
        return entity instanceof AbstractFish || entity instanceof Dolphin || entity instanceof Squid;
    }

    /** Le filet ne peut contenir qu'un mob dont la hitbox tient dans sa capacite. */
    private boolean fitsInTrap(LivingEntity target) {
        return Math.max(target.getBbWidth(), target.getBbHeight()) <= tier.getMaxMobSize();
    }

    public void onEntityCollide(Entity entity) {
        if (level == null || level.isClientSide() || bait.isEmpty()) return;
        if (pendingTargetUUID != null) return; // sequence deja en cours
        if (!isAquatic(entity)) return;
        if (!(entity instanceof LivingEntity target) || !target.isAlive()) return;
        if (!baitWorksOn(target)) return;
        if (!fitsInTrap(target)) return; // trop gros pour ce filet : ignore

        startCaptureSequence(target);
    }

    /** Tick serveur du master : resolution des sequences + detection de secours. */
    public void serverTick() {
        if (level == null || level.isClientSide() || !isMaster()) return;

        if (pendingTargetUUID != null) {
            pendingTicks--;
            if (pendingTicks <= 0) {
                resolvePendingCapture();
            }
            return;
        }

        if (bait.isEmpty()) return;
        backupCounter++;
        if (backupCounter < BACKUP_TICK_INTERVAL) return;
        backupCounter = 0;

        int n = tier.getRadius();
        AABB zone = new AABB(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                worldPosition.getX() + n, worldPosition.getY() + 1, worldPosition.getZ() + n).inflate(0.5, 0.5, 0.5);
        var candidates = level.getEntitiesOfClass(LivingEntity.class, zone,
                e -> isAquatic(e) && baitWorksOn(e) && fitsInTrap(e));
        if (!candidates.isEmpty()) {
            startCaptureSequence(candidates.get(0));
        }
    }

    /** La nappe se referme : anim + splash, resolution 0.35s plus tard. */
    private void startCaptureSequence(LivingEntity target) {
        // Jamais capturer un Soigneur
        if (target instanceof com.lex3d.ultimatezootaming.entities.ZooKeeperEntity) return;
        pendingTargetUUID = target.getUUID();
        pendingTicks = CAPTURE_DELAY_TICKS;

        triggerAnim("main", "trigger");
        level.playSound(null, worldPosition, SoundEvents.FISHING_BOBBER_SPLASH, SoundSource.BLOCKS, 1.0f, 0.8f);
    }

    private void resolvePendingCapture() {
        UUID targetUUID = pendingTargetUUID;
        pendingTargetUUID = null;

        if (!(level instanceof ServerLevel serverLevel)) return;

        Entity entity = serverLevel.getEntity(targetUUID);
        int n = tier.getRadius();
        AABB captureZone = new AABB(worldPosition.getX() - 0.5, worldPosition.getY() - 0.5, worldPosition.getZ() - 0.5,
                worldPosition.getX() + n + 0.5, worldPosition.getY() + 1.5, worldPosition.getZ() + n + 0.5);
        if (!(entity instanceof LivingEntity target) || !target.isAlive()
                || !captureZone.intersects(target.getBoundingBox())) {
            // Le poisson est parti pendant que la nappe se refermait : rien de consomme
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
            dietBonus = kibble.getTier().getBaseChance();
        } else {
            dietBonus = 0.2f;
        }

        float chance = 0.4f + dietBonus + tier.getBonus();
        chance *= (float) (double) com.lex3d.ultimatezootaming.config.ZooServerConfig.GLOBAL_CAPTURE_MULTIPLIER.get();
        chance = Math.max(0.15f, Math.min(0.95f, chance));

        boolean success = RNG.nextFloat() < chance;

        if (!(level instanceof ServerLevel serverLevel)) return;

        double px = target.getX();
        double py = target.getY() + target.getBbHeight() / 2.0;
        double pz = target.getZ();

        if (success) {
            if (target instanceof Mob mob && baiterUUID != null) {
                TamingUtil.tame(mob, baiterUUID, true);
            }

            ItemStack filledNet = OccupiedContainerItem.capture(
                    ModItems.OCCUPIED_CONTAINER.get().getDefaultInstance(), target, true, baiterUUID,
                    getBlockState().getBlock().asItem(), sourceDamage);

            serverLevel.sendParticles(ParticleTypes.HEART, px, py, pz, 8, 0.3, 0.3, 0.3, 0.02);
            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, px, py, pz, 6, 0.4, 0.4, 0.4, 0.02);

            target.discard();
            clearBaitState();

            Block.popResource(level, worldPosition, filledNet);
            clearStructure();
            level.playSound(null, worldPosition, ModSounds.CAGE_SUCCESS.get(), SoundSource.BLOCKS, 1.0f, 1.1f);
        } else {
            triggerAnim("main", "fail");
            serverLevel.sendParticles(ParticleTypes.SMOKE, px, py, pz, 10, 0.3, 0.3, 0.3, 0.02);
            level.playSound(null, worldPosition, ModSounds.CAGE_FAIL.get(), SoundSource.BLOCKS, 1.0f, 1.1f);

            clearBaitState();
            updateBaitedState(false);
            setChanged();
        }
    }

    private void clearStructure() {
        int n = tier.getRadius();
        for (int dx = 0; dx < n; dx++) {
            for (int dz = 0; dz < n; dz++) {
                BlockPos cellPos = worldPosition.offset(dx, 0, dz);
                if (level.getBlockState(cellPos).getBlock() instanceof NetTrapBlock) {
                    boolean water = level.getFluidState(cellPos).getType() == net.minecraft.world.level.material.Fluids.WATER;
                    level.setBlock(cellPos, water ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
    }

    private void clearBaitState() {
        bait = ItemStack.EMPTY;
        baiterUUID = null;
        if (level != null) {
            BaitedTrapRegistry.unregister(level, worldPosition);
            syncToClient();
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && hasBait()) {
            BaitedTrapRegistry.unregister(level, worldPosition);
        }
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
        sourceDamage = tag.getInt("SourceDamage");
        if (tag.contains("MasterX")) {
            masterPos = new BlockPos(tag.getInt("MasterX"), tag.getInt("MasterY"), tag.getInt("MasterZ"));
        }
    }

    @Override
    public void setLevel(net.minecraft.world.level.Level level) {
        super.setLevel(level);
        if (!level.isClientSide() && hasBait() && isMaster()) {
            BaitedTrapRegistry.register(level, worldPosition, getBaitDiet(), tier.getMaxMobSize());
        }
    }
}
