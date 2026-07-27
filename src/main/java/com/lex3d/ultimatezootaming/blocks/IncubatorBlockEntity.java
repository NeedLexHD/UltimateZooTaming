package com.lex3d.ultimatezootaming.blocks;

import com.lex3d.ultimatezootaming.items.GeneticSampleItem;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;

/**
 * INCUBATEUR : machine de reproduction assistee.
 *
 * On y insere DEUX echantillons genetiques de la meme espece (preleves a la
 * Seringue). La machine couve pendant un cycle complet, puis fait naitre un
 * bebe qui herite des deux donneurs, avec la meme table de mutations que la
 * reproduction naturelle.
 *
 * Trois etats visuels : au repos, en couvaison (dome lumineux, particules,
 * bourdonnement) et eclosion.
 */
public class IncubatorBlockEntity extends BlockEntity implements GeoBlockEntity {

    /** Duree d'un cycle complet de couvaison, en ticks (~60 s). */
    public static final int CYCLE = 1200;

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.incubator.idle");
    private static final RawAnimation WORK = RawAnimation.begin().thenLoop("animation.incubator.work");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    /** Les deux echantillons inseres. */
    private ItemStack sampleA = ItemStack.EMPTY;
    private ItemStack sampleB = ItemStack.EMPTY;
    /** Progression du cycle en cours (0 = a l'arret). */
    private int progress = 0;

    public IncubatorBlockEntity(BlockPos pos, BlockState state) {
        super(com.lex3d.ultimatezootaming.core.init.ModBlockEntities.INCUBATOR.get(), pos, state);
    }

    public ItemStack getSampleA() { return sampleA; }
    public ItemStack getSampleB() { return sampleB; }
    public int getProgress() { return progress; }
    public boolean isWorking() { return progress > 0; }

    /** Fraction du cycle accomplie, pour la barre et les effets. */
    public float getProgressRatio() { return progress / (float) CYCLE; }

    /**
     * Insere un echantillon dans le premier emplacement libre.
     * @return true si l'echantillon a ete accepte
     */
    public boolean insertSample(ItemStack stack) {
        if (!GeneticSampleItem.isValid(stack)) return false;
        if (sampleA.isEmpty()) {
            sampleA = stack.copyWithCount(1);
            sync();
            return true;
        }
        if (sampleB.isEmpty()) {
            // Les deux echantillons doivent porter la MEME espece
            if (!GeneticSampleItem.speciesOf(sampleA).equals(GeneticSampleItem.speciesOf(stack))) {
                return false;
            }
            sampleB = stack.copyWithCount(1);
            sync();
            return true;
        }
        return false; // machine pleine
    }

    /** Vide la machine et rend les echantillons non consommes. */
    public java.util.List<ItemStack> ejectSamples() {
        java.util.List<ItemStack> out = new java.util.ArrayList<>();
        if (!sampleA.isEmpty()) { out.add(sampleA); sampleA = ItemStack.EMPTY; }
        if (!sampleB.isEmpty()) { out.add(sampleB); sampleB = ItemStack.EMPTY; }
        progress = 0;
        sync();
        return out;
    }

    /** La machine peut-elle demarrer un cycle ? */
    public boolean canStart() {
        return GeneticSampleItem.isValid(sampleA) && GeneticSampleItem.isValid(sampleB)
                && GeneticSampleItem.speciesOf(sampleA).equals(GeneticSampleItem.speciesOf(sampleB));
    }

    /** Tick serveur : fait avancer la couvaison et declenche l'eclosion. */
    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                  IncubatorBlockEntity be) {
        if (!(level instanceof ServerLevel sl)) return;
        if (!be.canStart()) {
            if (be.progress != 0) { be.progress = 0; be.sync(); }
            return;
        }
        be.progress++;

        // Effets pendant la couvaison
        if (be.progress % 20 == 0) {
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                    pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5,
                    2, 0.15, 0.1, 0.15, 0.01);
        }
        if (be.progress % 60 == 0) {
            sl.playSound(null, pos, net.minecraft.sounds.SoundEvents.BEACON_AMBIENT,
                    net.minecraft.sounds.SoundSource.BLOCKS, 0.25f, 1.8f);
        }

        if (be.progress >= CYCLE) {
            com.lex3d.ultimatezootaming.events.IncubatorHandler.hatch(sl, pos, be);
            be.sampleA = ItemStack.EMPTY;
            be.sampleB = ItemStack.EMPTY;
            be.progress = 0;
            be.sync();
        } else if (be.progress % 40 == 0) {
            be.sync(); // rafraichit la barre cote client
        }
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // ---------------- Animation ----------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, this::animate));
    }

    private PlayState animate(AnimationState<IncubatorBlockEntity> state) {
        return state.setAndContinue(isWorking() ? WORK : IDLE);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

    // ---------------- Sauvegarde ----------------

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (!sampleA.isEmpty()) tag.put("SampleA", sampleA.save(new CompoundTag()));
        if (!sampleB.isEmpty()) tag.put("SampleB", sampleB.save(new CompoundTag()));
        tag.putInt("Progress", progress);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        sampleA = tag.contains("SampleA") ? ItemStack.of(tag.getCompound("SampleA")) : ItemStack.EMPTY;
        sampleB = tag.contains("SampleB") ? ItemStack.of(tag.getCompound("SampleB")) : ItemStack.EMPTY;
        progress = tag.getInt("Progress");
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        if (pkt.getTag() != null) load(pkt.getTag());
    }
}
