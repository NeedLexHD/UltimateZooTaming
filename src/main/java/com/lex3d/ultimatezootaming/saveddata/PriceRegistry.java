package com.lex3d.ultimatezootaming.saveddata;

import com.lex3d.ultimatezootaming.blocks.ShopBlock;
import com.lex3d.ultimatezootaming.items.ShopProductItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;

/**
 * Les prix de vente du zoo : le joueur peut donner un prix (et une categorie de
 * boutique) a N'IMPORTE quel item du modpack via /zootame price. Les produits du
 * mod ont un prix integre, les peluches (Plushie Mod & co) un prix par defaut.
 */
public class PriceRegistry extends SavedData {

    private static final String NAME = "ultimatezootame_prices";

    public record Entry(int price, int shopType) {}

    private final Map<ResourceLocation, Entry> prices = new HashMap<>();

    public static PriceRegistry get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                PriceRegistry::load, PriceRegistry::new, NAME);
    }

    public void setPrice(Item item, int price, int shopType) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        if (id == null) return;
        if (price <= 0) prices.remove(id);
        else prices.put(id, new Entry(price, shopType));
        setDirty();
    }

    /** Le prix de vente d'un stack : produit du mod, prix custom, ou peluche. 0 = invendable. */
    public int priceOf(ItemStack stack) {
        if (stack.getItem() instanceof ShopProductItem p) return p.getPrice();
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id != null) {
            Entry e = prices.get(id);
            if (e != null) return e.price();
            if (isPlush(id)) {
                return com.lex3d.ultimatezootaming.config.ZooServerConfig.PLUSH_PRICE.get();
            }
        }
        return 0;
    }

    /** La categorie de boutique d'un stack (0 souvenir, 1 repas, 2 glace, 3 boisson), -1 si invendable. */
    public int shopTypeOf(ItemStack stack) {
        if (stack.getItem() instanceof ShopProductItem p) return p.getShopType().ordinal();
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id != null) {
            Entry e = prices.get(id);
            if (e != null) return e.shopType();
            if (isPlush(id)) return ShopBlock.ShopType.SOUVENIR.ordinal();
        }
        return -1;
    }

    /** Une peluche d'un mod de peluches (Plushie Mod, Plushables, etc.). */
    private static boolean isPlush(ResourceLocation id) {
        return id.getNamespace().contains("plush") || id.getPath().contains("plush");
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (var e : prices.entrySet()) {
            CompoundTag t = new CompoundTag();
            t.putString("Item", e.getKey().toString());
            t.putInt("Price", e.getValue().price());
            t.putInt("Type", e.getValue().shopType());
            list.add(t);
        }
        tag.put("Prices", list);
        return tag;
    }

    public static PriceRegistry load(CompoundTag tag) {
        PriceRegistry r = new PriceRegistry();
        for (Tag t : tag.getList("Prices", Tag.TAG_COMPOUND)) {
            CompoundTag ct = (CompoundTag) t;
            ResourceLocation id = ResourceLocation.tryParse(ct.getString("Item"));
            if (id != null) r.prices.put(id, new Entry(ct.getInt("Price"), ct.getInt("Type")));
        }
        return r;
    }
}
