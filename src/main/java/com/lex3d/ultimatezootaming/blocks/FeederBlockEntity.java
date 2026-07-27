package com.lex3d.ultimatezootaming.blocks;

import com.lex3d.ultimatezootaming.capability.CapabilityHandler;
import com.lex3d.ultimatezootaming.capability.TamingData;
import com.lex3d.ultimatezootaming.core.init.ModBlockEntities;
import com.lex3d.ultimatezootaming.items.KibbleItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FeederBlockEntity extends BlockEntity implements software.bernie.geckolib.animatable.GeoBlockEntity {

    private final software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache animCache =
            software.bernie.geckolib.util.GeckoLibUtil.createInstanceCache(this);

    @Override
    public void registerControllers(software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new software.bernie.geckolib.core.animation.AnimationController<>(this, "main", 0, state ->
                state.setAndContinue(software.bernie.geckolib.core.animation.RawAnimation.begin()
                        .thenLoop("animation.feeder.idle"))));
    }

    @Override
    public software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache getAnimatableInstanceCache() {
        return animCache;
    }


    private static final int FEED_INTERVAL = 100; // 5 secondes
    // Croissance des bebes : on ne les nourrit qu'1 cycle sur 3 (donc toutes les
    // ~15s), et chaque croquette avance leur age de 1200 ticks (~1 min de croissance),
    // pour un effet visible sans vider la mangeoire en quelques secondes.
    private static int babyFeedEvery() { return com.lex3d.ultimatezootaming.config.ZooServerConfig.BABY_FEED_EVERY.get(); }
    private static int babyGrowthPerFeed() { return com.lex3d.ultimatezootaming.config.ZooServerConfig.BABY_GROWTH_PER_FEED.get(); }
    private int babyCounter;
    private static double radius() { return com.lex3d.ultimatezootaming.config.ZooServerConfig.FEEDER_RADIUS.get(); }
    private static final int MAX_STORED = 64;

    private ItemStack stored = ItemStack.EMPTY;
    private int cooldown;

    public FeederBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FEEDER.get(), pos, state);
    }

    /** Clic droit : croquette = remplir ; main vide + sneak = tout recuperer ; main vide = etat. */
    public InteractionResult interact(Player player, ItemStack held) {
        if (level == null) return InteractionResult.PASS;

        if (held.getItem() instanceof com.lex3d.ultimatezootaming.items.FodderItem) {
            if (!stored.isEmpty() && stored.getItem() != held.getItem()) {
                player.displayClientMessage(
                        Component.translatable("message.ultimatezootaming.feeder_wrong_type"), true);
                return InteractionResult.sidedSuccess(level.isClientSide());
            }
            if (level.isClientSide()) return InteractionResult.SUCCESS;

            int space = MAX_STORED - stored.getCount();
            int moved = Math.min(space, held.getCount());
            if (moved <= 0) {
                player.displayClientMessage(
                        Component.translatable("message.ultimatezootaming.feeder_full"), true);
                return InteractionResult.SUCCESS;
            }
            if (stored.isEmpty()) {
                stored = held.copyWithCount(moved);
            } else {
                stored.grow(moved);
            }
            if (!player.getAbilities().instabuild) {
                held.shrink(moved);
            }
            updateLevelProperty();
            setChanged();
            level.playSound(null, worldPosition, SoundEvents.COMPOSTER_FILL, SoundSource.BLOCKS, 1.0f, 1.0f);
            announceContents(player);
            return InteractionResult.SUCCESS;
        }

        if (held.isEmpty()) {
            if (player.isShiftKeyDown() && !stored.isEmpty()) {
                if (!level.isClientSide()) {
                    if (!player.getInventory().add(stored)) {
                        player.drop(stored, false);
                    }
                    stored = ItemStack.EMPTY;
                    updateLevelProperty();
                    setChanged();
                }
                return InteractionResult.sidedSuccess(level.isClientSide());
            }
            if (!level.isClientSide()) announceContents(player);
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        return InteractionResult.PASS;
    }

    private void announceContents(Player player) {
        if (stored.isEmpty()) {
            player.displayClientMessage(
                    Component.translatable("message.ultimatezootaming.feeder_empty"), true);
        } else {
            player.displayClientMessage(
                    Component.translatable("message.ultimatezootaming.feeder_contents",
                            stored.getCount(), stored.getHoverName()), true);
        }
    }

    private void updateLevelProperty() {
        if (level == null) return;
        int lvl = stored.isEmpty() ? 0 : (stored.getCount() < MAX_STORED / 2 ? 1 : 2);
        int type = 0; // 0=rien/croquettes-vert, 1=vegetal, 2=viande, 3=poisson
        if (stored.getItem() instanceof com.lex3d.ultimatezootaming.items.FodderItem fodder) {
            type = switch (fodder.getDiet()) {
                case HERBIVORE -> 1;
                case CARNIVORE -> 2;
                case PISCIVORE -> 3;
            };
        }
        BlockState state = level.getBlockState(worldPosition);
        if (state.hasProperty(FeederBlock.LEVEL)
                && (state.getValue(FeederBlock.LEVEL) != lvl
                    || (state.hasProperty(FeederBlock.FOOD_TYPE) && state.getValue(FeederBlock.FOOD_TYPE) != type))) {
            BlockState next = state.setValue(FeederBlock.LEVEL, lvl);
            if (state.hasProperty(FeederBlock.FOOD_TYPE)) next = next.setValue(FeederBlock.FOOD_TYPE, type);
            level.setBlock(worldPosition, next, 3);
        }
    }

    /** La mangeoire contient-elle des croquettes ? (pour le calcul de bien-etre) */
    public boolean hasFood() {
        return !stored.isEmpty();
    }

    /** A-t-elle besoin d'etre ravitaillee ? (moins de la moitie, pour le Soigneur) */
    public boolean needsRefill() {
        return stored.getCount() < MAX_STORED / 2;
    }

    /** Type de croquette deja dedans (null si vide) : le Soigneur doit apporter le meme. */
    @javax.annotation.Nullable
    /**
     * Mode FOURRAGE : les animaux apprivoises proches viennent "manger" — une
     * unite consommee de temps en temps, particules de miettes. Contribue au
     * bonheur (score Nourriture) mais ne declenche ni amour ni croissance.
     */
    private void feedFodder() {
        if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) return;
        // Consommation lente : ~1 fourrage toutes les 60s s'il y a des animaux autour
        if (serverLevel.getGameTime() % 1200 != 0) return;

        double RADIUS = radius();
        AABB area = new AABB(worldPosition).inflate(RADIUS, 4, RADIUS);
        java.util.List<net.minecraft.world.entity.animal.Animal> animals =
                serverLevel.getEntitiesOfClass(net.minecraft.world.entity.animal.Animal.class, area,
                        a -> a.isAlive()
                                && ((com.lex3d.ultimatezootaming.items.FodderItem) stored.getItem()).getDiet().matches(a)
                                && a.getCapability(
                                com.lex3d.ultimatezootaming.capability.CapabilityHandler.TAMING_DATA)
                                .resolve().map(com.lex3d.ultimatezootaming.capability.TamingData::isTamed).orElse(false));
        if (animals.isEmpty()) return;

        stored.shrink(1);
        setChanged();
        updateLevelProperty();
        // Miettes sur quelques animaux proches (feedback visuel du repas)
        int shown = 0;
        for (net.minecraft.world.entity.animal.Animal a : animals) {
            serverLevel.sendParticles(
                    new net.minecraft.core.particles.ItemParticleOption(
                            net.minecraft.core.particles.ParticleTypes.ITEM,
                            new ItemStack(com.lex3d.ultimatezootaming.core.init.ModItems.FODDER.get())),
                    a.getX(), a.getY() + a.getBbHeight() * 0.6, a.getZ(), 4, 0.2, 0.15, 0.2, 0.02);
            if (++shown >= 4) break;
        }
    }

    /** L'item actuellement stocke (croquette OU fourrage), null si vide. */
    public net.minecraft.world.item.Item storedItem() {
        return stored.isEmpty() ? null : stored.getItem();
    }

    public KibbleItem storedKibble() {
        return stored.getItem() instanceof KibbleItem k ? k : null;
    }

    /**
     * Insere des croquettes depuis un stack source (ex: coffre via Soigneur).
     * N'accepte que des KibbleItem, et du meme type si deja rempli. Retourne le
     * nombre reellement insere (pour retirer d'autant du coffre).
     */
    public int insertKibble(ItemStack incoming) {
        if (level == null || !(incoming.getItem() instanceof com.lex3d.ultimatezootaming.items.FodderItem)) return 0;
        if (!stored.isEmpty() && stored.getItem() != incoming.getItem()) return 0;

        int space = MAX_STORED - stored.getCount();
        int moved = Math.min(space, incoming.getCount());
        if (moved <= 0) return 0;

        if (stored.isEmpty()) {
            stored = incoming.copyWithCount(moved);
        } else {
            stored.grow(moved);
        }
        updateLevelProperty();
        setChanged();
        return moved;
    }

    public void dropContents() {
        if (level != null && !stored.isEmpty()) {
            Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5,
                    worldPosition.getZ() + 0.5, stored);
            stored = ItemStack.EMPTY;
        }
    }

    public void serverTick() {
        if (level == null || level.isClientSide()) return;
        if (++cooldown < FEED_INTERVAL) return;
        cooldown = 0;

        // Fourrage : les animaux mangent (bonheur) mais AUCUNE reproduction/croissance.
        if (stored.getItem() instanceof com.lex3d.ultimatezootaming.items.FodderItem) {
            feedFodder();
            return;
        }
        if (!(stored.getItem() instanceof KibbleItem kibble)) return;
        if (!(level instanceof ServerLevel serverLevel)) return;

        double RADIUS = radius();
        AABB zone = new AABB(worldPosition).inflate(RADIUS, 4, RADIUS);
        List<Animal> candidates = level.getEntitiesOfClass(Animal.class, zone,
                a -> a.isAlive() && isTamedFamiliar(a) && kibble.getDiet().matches(a));
        if (candidates.isEmpty()) return;

        // 1. Un bebe ? Croissance acceleree, mais PAS a chaque cycle (sinon la
        // mangeoire se vide en quelques secondes). On ne nourrit un bebe qu'une
        // fois sur BABY_FEED_EVERY cycles, et chaque croquette avance vraiment
        // sa croissance d'une tranche (~1 min), pour un effet visible sans gaspillage.
        babyCounter++;
        if (babyCounter >= babyFeedEvery()) {
            for (Animal animal : candidates) {
                if (animal.isBaby()) {
                    babyCounter = 0;
                    animal.ageUp(babyGrowthPerFeed(), true); // avance la croissance d'une vraie tranche
                    consume(1);
                    feedbackAt(serverLevel, animal);
                    return;
                }
            }
        }

        if (stored.getCount() < 2) return;

        // 2. Un couple d'adultes du meme type, prets a se reproduire (2 croquettes)
        Map<EntityType<?>, Animal> firstOfType = new HashMap<>();
        for (Animal animal : candidates) {
            if (animal.isBaby() || animal.getAge() != 0 || !animal.canFallInLove() || animal.isInLove()) continue;

            Animal partner = firstOfType.get(animal.getType());
            if (partner == null) {
                firstOfType.put(animal.getType(), animal);
            } else {
                partner.setInLove(null);
                animal.setInLove(null);
                consume(2);
                feedbackAt(serverLevel, animal);
                feedbackAt(serverLevel, partner);
                level.playSound(null, worldPosition, SoundEvents.GENERIC_EAT, SoundSource.BLOCKS, 0.8f, 1.0f);
                return; // 1 couple max par cycle, pour ne pas vider la mangeoire d'un coup
            }
        }
    }

    private boolean isTamedFamiliar(Animal animal) {
        return animal.getCapability(CapabilityHandler.TAMING_DATA)
                .map(TamingData::isTamed).orElse(false);
    }

    private void consume(int count) {
        stored.shrink(count);
        updateLevelProperty();
        setChanged();
    }

    private void feedbackAt(ServerLevel serverLevel, Animal animal) {
        serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                animal.getX(), animal.getY() + animal.getBbHeight() * 0.7, animal.getZ(),
                4, 0.3, 0.3, 0.3, 0.02);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (!stored.isEmpty()) {
            tag.put("Stored", stored.save(new CompoundTag()));
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        stored = tag.contains("Stored") ? ItemStack.of(tag.getCompound("Stored")) : ItemStack.EMPTY;
    }
}
