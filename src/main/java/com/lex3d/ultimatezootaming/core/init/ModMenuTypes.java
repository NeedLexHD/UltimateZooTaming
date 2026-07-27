package com.lex3d.ultimatezootaming.core.init;

import com.lex3d.ultimatezootaming.UltimateZooTame;
import com.lex3d.ultimatezootaming.client.gui.menu.ShopMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMenuTypes {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, UltimateZooTame.MODID);

    public static final RegistryObject<MenuType<ShopMenu>> SHOP =
            MENUS.register("shop", () -> IForgeMenuType.create(ShopMenu::fromNetwork));
}
