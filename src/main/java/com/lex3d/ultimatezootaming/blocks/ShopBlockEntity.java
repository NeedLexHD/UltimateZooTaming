package com.lex3d.ultimatezootaming.blocks;

import com.lex3d.ultimatezootaming.core.init.ModBlockEntities;
import com.lex3d.ultimatezootaming.core.init.ModItems;
import com.lex3d.ultimatezootaming.items.ShopProductItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Stock d'une boutique (9 slots), ouvrable comme un coffre + vente aux visiteurs. */
public class ShopBlockEntity extends BlockEntity
        implements net.minecraft.world.Container, software.bernie.geckolib.animatable.GeoBlockEntity {

    private final software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache animCache =
            software.bernie.geckolib.util.GeckoLibUtil.createInstanceCache(this);

    @Override
    public void registerControllers(software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new software.bernie.geckolib.core.animation.AnimationController<>(this, "main", 0, state ->
                state.setAndContinue(software.bernie.geckolib.core.animation.RawAnimation.begin()
                        .thenLoop("animation.cash_register.idle")))
                .triggerableAnim("sell", software.bernie.geckolib.core.animation.RawAnimation.begin()
                        .thenPlay("animation.cash_register.sell")));
    }

    @Override
    public software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache getAnimatableInstanceCache() {
        return animCache;
    }

    private final NonNullList<ItemStack> items = NonNullList.withSize(9, ItemStack.EMPTY);

    public ShopBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SHOP.get(), pos, state);
    }

    /** Type de commerce choisi par le joueur (sneak+clic). */
    private int shopType = 0;

    public ShopBlock.ShopType getShopTypeEnum() {
        return ShopBlock.ShopType.values()[Math.floorMod(shopType, 5)];
    }

    public void cycleType() {
        shopType = (shopType + 1) % 5;
        setChanged();
    }

    public void setShopType(int type) {
        shopType = Math.floorMod(type, 5);
        setChanged();
    }

    // ---- Container : la caisse s'ouvre comme un coffre (1 rangee) ----
    @Override
    public int getContainerSize() { return items.size(); }

    @Override
    public boolean isEmpty() {
        for (ItemStack s : items) if (!s.isEmpty()) return false;
        return true;
    }

    @Override
    public ItemStack getItem(int slot) { return items.get(slot); }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack r = ContainerHelper.removeItem(items, slot, amount);
        if (!r.isEmpty()) setChanged();
        return r;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        setChanged();
    }

    @Override
    public boolean stillValid(net.minecraft.world.entity.player.Player player) {
        return net.minecraft.world.Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        items.clear();
        setChanged();
    }

    /** Accepte tout item VENDABLE (prix connu) de la bonne categorie. */
    public boolean accepts(ItemStack stack) {
        if (!(level instanceof net.minecraft.server.level.ServerLevel sl)) return false;
        var reg = com.lex3d.ultimatezootaming.saveddata.PriceRegistry.get(sl);
        return reg.priceOf(stack) > 0 && reg.shopTypeOf(stack) == shopType;
    }

    public ItemStack insert(ItemStack incoming) {
        if (!accepts(incoming)) return incoming;
        for (int i = 0; i < items.size() && !incoming.isEmpty(); i++) {
            ItemStack slot = items.get(i);
            if (slot.isEmpty()) {
                items.set(i, incoming.copy());
                incoming.setCount(0);
            } else if (ItemStack.isSameItemSameTags(slot, incoming)) {
                int move = Math.min(incoming.getCount(), slot.getMaxStackSize() - slot.getCount());
                slot.grow(move);
                incoming.shrink(move);
            }
        }
        setChanged();
        return incoming;
    }

    /** La caisse a-t-elle de la place pour au moins un article de plus ? */
    public boolean hasRoom() {
        for (ItemStack s : items) {
            if (s.isEmpty()) return true;
            if (s.getCount() < s.getMaxStackSize()) return true;
        }
        return false;
    }

    /**
     * Cherche un COFFRE D'APPROVISIONNEMENT autour de la caisse : n'importe quel
     * conteneur (coffre, baril, ou conteneur d'un autre mod) situe dans un rayon
     * de 5 blocs et contenant au moins un article vendable par CETTE boutique.
     * Le vendeur ira s'y servir pour regarnir la caisse.
     *
     * @return la position du conteneur, ou null si aucun ne convient
     */
    @javax.annotation.Nullable
    public BlockPos findSupplyContainer() {
        if (!(level instanceof net.minecraft.server.level.ServerLevel sl)) return null;
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        // Scan plat autour de la caisse (dy -2..+2) : jamais de scan de volume
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                for (int dy = -2; dy <= 2; dy++) {
                    BlockPos p = worldPosition.offset(dx, dy, dz);
                    if (p.equals(worldPosition)) continue;
                    var be = sl.getBlockEntity(p);
                    if (!(be instanceof net.minecraft.world.Container cont)) continue;
                    if (be instanceof ShopBlockEntity) continue; // pas une autre caisse
                    if (!containerHasSellable(cont)) continue;
                    double d = worldPosition.distSqr(p);
                    if (d < bestD) { bestD = d; best = p.immutable(); }
                }
            }
        }
        return best;
    }

    /** Ce conteneur contient-il un article vendable par cette boutique ? */
    private boolean containerHasSellable(net.minecraft.world.Container cont) {
        for (int i = 0; i < cont.getContainerSize(); i++) {
            ItemStack s = cont.getItem(i);
            if (!s.isEmpty() && accepts(s)) return true;
        }
        return false;
    }

    /**
     * Prend une pile vendable dans le conteneur et la met dans la caisse.
     * @return true si un transfert a eu lieu
     */
    public boolean restockFrom(net.minecraft.world.Container cont) {
        if (!hasRoom()) return false;
        for (int i = 0; i < cont.getContainerSize(); i++) {
            ItemStack s = cont.getItem(i);
            if (s.isEmpty() || !accepts(s)) continue;
            // On ne prend qu'une petite quantite a la fois (voyage credible)
            int take = Math.min(s.getCount(), 16);
            ItemStack moved = s.copy();
            moved.setCount(take);
            ItemStack left = insert(moved);
            int actuallyMoved = take - left.getCount();
            if (actuallyMoved > 0) {
                s.shrink(actuallyMoved);
                cont.setChanged();
                setChanged();
                return true;
            }
        }
        return false;
    }

    public int countStock() {
        int n = 0;
        for (ItemStack s : items) n += s.getCount();
        return n;
    }

    public boolean hasStock() { return countStock() > 0; }

    /**
     * Un visiteur achete un produit : retire 1 du stock et verse le prix dans la
     * Caisse du Zoo la plus proche. Retourne le prix encaisse (0 si echec).
     */
    /** Un vendeur (pas en greve) tient-il cette caisse ? Sans lui, pas de vente. */
    public boolean hasVendor() {
        if (level == null) return false;
        return !level.getEntitiesOfClass(
                com.lex3d.ultimatezootaming.entities.ZooKeeperEntity.class,
                new net.minecraft.world.phys.AABB(worldPosition).inflate(3),
                k -> k.isAlive() && !k.isOnStrike()).isEmpty();
    }

    /** Stand PHOTOS : il faut une espece vedette a moins de 16 blocs pour vendre. */
    public boolean starNearby() {
        if (!(level instanceof net.minecraft.server.level.ServerLevel sl)) return false;
        var stars = com.lex3d.ultimatezootaming.config.ZooServerConfig.STAR_SPECIES.get();
        if (stars == null || stars.isEmpty()) return false;
        return !sl.getEntitiesOfClass(net.minecraft.world.entity.animal.Animal.class,
                new net.minecraft.world.phys.AABB(worldPosition).inflate(16),
                a -> {
                    var id = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(a.getType());
                    return id != null && stars.contains(id.toString()) && a.isAlive();
                }).isEmpty();
    }

    public int sellOne() {
        if (level == null) return 0;
        if (!hasVendor()) return 0; // personne a la caisse : pas de vente
        if (getShopTypeEnum() == ShopBlock.ShopType.PHOTO && !starNearby()) return 0;
        for (int i = 0; i < items.size(); i++) {
            ItemStack slot = items.get(i);
            if (slot.isEmpty()) continue;
            int price = level instanceof net.minecraft.server.level.ServerLevel sl
                    ? com.lex3d.ultimatezootaming.saveddata.PriceRegistry.get(sl).priceOf(slot)
                    : 0;
            if (price <= 0) continue; // pas de prix : le vendeur ne vend pas cet item
            ZooVaultBlockEntity vault = ZooVaultBlock.nearestVault(level, worldPosition, 128);
            if (vault == null) vault = ZooVaultBlock.scanForVault(level, worldPosition, 32);
            if (vault == null) return 0; // pas de caisse : pas de vente
            slot.shrink(1);
            setChanged();
            vault.deposit(price);
            if (level instanceof net.minecraft.server.level.ServerLevel sl) {
                com.lex3d.ultimatezootaming.saveddata.ZooLedger.get(sl).addSales(price);
                com.lex3d.ultimatezootaming.saveddata.ZooLedger.get(sl).addMissionProgress(
                        com.lex3d.ultimatezootaming.progression.DailyMission.SELL_ITEMS, 1);
            }
            triggerAnim("main", "sell"); // le tiroir s'ouvre, la clochette sonne
            // Le VENDEUR reagit : se tourne vers le client et fait un geste content
            if (level instanceof net.minecraft.server.level.ServerLevel sl2) {
                var vendors = sl2.getEntitiesOfClass(
                        com.lex3d.ultimatezootaming.entities.ZooKeeperEntity.class,
                        new net.minecraft.world.phys.AABB(worldPosition).inflate(3),
                        k -> k.isAlive() && !k.isOnStrike());
                var clients = sl2.getEntitiesOfClass(
                        com.lex3d.ultimatezootaming.entities.VisitorEntity.class,
                        new net.minecraft.world.phys.AABB(worldPosition).inflate(4),
                        v -> v.isAlive());
                for (var vendor : vendors) {
                    if (!clients.isEmpty()) {
                        vendor.getLookControl().setLookAt(clients.get(0)); // regarde le client
                    }
                    vendor.addXp(1); // Vente = 1 XP
                    // petit geste content au-dessus du vendeur
                    sl2.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                            vendor.getX(), vendor.getEyeY() + 0.5, vendor.getZ(), 3, 0.2, 0.2, 0.2, 0.0);
                }
                // le client aussi est content de son achat
                for (var client : clients) {
                    sl2.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                            client.getX(), client.getEyeY() + 0.5, client.getZ(), 2, 0.15, 0.15, 0.15, 0.0);
                }
            }
            return price;
        }
        return 0;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, items);
        tag.putInt("ShopType", shopType);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        ContainerHelper.loadAllItems(tag, items);
        shopType = tag.getInt("ShopType");
    }
}
