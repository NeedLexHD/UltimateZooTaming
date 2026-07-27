package com.lex3d.ultimatezootaming.client.gui.menu;

import com.lex3d.ultimatezootaming.blocks.ShopBlockEntity;
import com.lex3d.ultimatezootaming.core.init.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Menu de la caisse enregistreuse : 9 slots de stock + inventaire joueur.
 * ContainerData [0..8] = prix de chaque slot (0 = invendable), [9] = type de commerce.
 */
public class ShopMenu extends AbstractContainerMenu {

    private final Container shop;
    private final ContainerData data;
    private final BlockPos pos;

    /** Cote client (fromNetwork). */
    public static ShopMenu fromNetwork(int id, Inventory playerInv, FriendlyByteBuf buf) {
        return new ShopMenu(id, playerInv, new SimpleContainer(9),
                new SimpleContainerData(10), buf.readBlockPos());
    }

    public ShopMenu(int id, Inventory playerInv, Container shop, ContainerData data, BlockPos pos) {
        super(ModMenuTypes.SHOP.get(), id);
        this.shop = shop;
        this.data = data;
        this.pos = pos;
        checkContainerSize(shop, 9);
        shop.startOpen(playerInv.player);

        // Stock : 1 rangee de 9
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(shop, col, 8 + col * 18, 32));
        }
        // Inventaire joueur
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, 8 + col * 18, 142));
        }
        addDataSlots(data);
    }

    public BlockPos getPos() { return pos; }

    /** Prix du slot i (synchronise), 0 = invendable. */
    public int priceAt(int i) { return data.get(i); }

    public int getShopType() { return data.get(9); }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index < 9) {
                if (!moveItemStackTo(stack, 9, 45, true)) return ItemStack.EMPTY;
            } else {
                if (!moveItemStackTo(stack, 0, 9, false)) return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return shop.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        shop.stopOpen(player);
    }

    /** Cote serveur : donnees vivantes de la caisse. */
    public static class ShopData implements ContainerData {
        private final ShopBlockEntity shop;

        public ShopData(ShopBlockEntity shop) { this.shop = shop; }

        @Override
        public int get(int index) {
            if (index == 9) return shop.getShopTypeEnum().ordinal();
            if (shop.getLevel() instanceof net.minecraft.server.level.ServerLevel sl) {
                return com.lex3d.ultimatezootaming.saveddata.PriceRegistry.get(sl)
                        .priceOf(shop.getItem(index));
            }
            return 0;
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() { return 10; }
    }
}
