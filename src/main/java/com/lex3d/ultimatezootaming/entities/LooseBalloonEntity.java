package com.lex3d.ultimatezootaming.entities;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * BALLON ECHAPPE : quand un enfant lache son ballon, il s'envole vraiment.
 *
 * Entite volontairement minimaliste : pas d'IA, pas de collision, pas de
 * pathfinding. Elle monte, derive un peu, et DISPARAIT au bout de 15 secondes.
 * Cette duree de vie stricte est indispensable : sans elle, chaque ballon lache
 * resterait a grimper indefiniment et on accumulerait les entites.
 */
public class LooseBalloonEntity extends Entity {

    private static final EntityDataAccessor<Integer> COLOR =
            SynchedEntityData.defineId(LooseBalloonEntity.class, EntityDataSerializers.INT);

    /** Duree de vie en ticks (15 s). */
    private static final int LIFETIME = 300;

    /** Derive horizontale, tiree une fois au spawn pour un vol naturel. */
    private double driftX, driftZ;

    public LooseBalloonEntity(EntityType<? extends LooseBalloonEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;      // il traverse le decor, il ne reste jamais coince
        this.blocksBuilding = false;
    }

    public int getColor() { return this.entityData.get(COLOR); }
    public void setColor(int c) { this.entityData.set(COLOR, c); }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(COLOR, 0);
    }

    @Override
    public void tick() {
        super.tick();

        // Derive tiree une seule fois
        if (tickCount == 1) {
            driftX = (random.nextDouble() - 0.5) * 0.02;
            driftZ = (random.nextDouble() - 0.5) * 0.02;
        }

        // Montee reguliere avec un leger balancement
        double sway = Math.sin(tickCount * 0.08) * 0.006;
        setDeltaMovement(driftX + sway, 0.09, driftZ);
        move(net.minecraft.world.entity.MoverType.SELF, getDeltaMovement());

        // DUREE DE VIE STRICTE : il finit toujours par disparaitre
        if (tickCount > LIFETIME || getY() > level().getMaxBuildHeight() + 20) {
            if (!level().isClientSide()) discard();
        }
    }

    /** Rien a sauvegarder : un ballon en vol ne survit pas a un rechargement. */
    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("Color")) setColor(tag.getInt("Color"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Color", getColor());
    }

    @Override
    public boolean isPickable() { return false; }

    @Override
    public boolean isPushable() { return false; }

    /** Insensible a tout : ce n'est qu'un decor volant. */
    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        return false;
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getAddEntityPacket() {
        return new net.minecraft.network.protocol.game.ClientboundAddEntityPacket(this);
    }
}
