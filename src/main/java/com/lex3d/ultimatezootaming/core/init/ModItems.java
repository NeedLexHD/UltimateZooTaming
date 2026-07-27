package com.lex3d.ultimatezootaming.core.init;

import com.lex3d.ultimatezootaming.UltimateZooTame;
import com.lex3d.ultimatezootaming.items.KibbleItem;
import com.lex3d.ultimatezootaming.items.OccupiedContainerItem;
import com.lex3d.ultimatezootaming.core.init.ModEntities;
import com.lex3d.ultimatezootaming.items.AnimalRemedyItem;
import com.lex3d.ultimatezootaming.items.SurveyorStaffItem;
import com.lex3d.ultimatezootaming.items.WhistleItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, UltimateZooTame.MODID);

    // ---- Les 9 Croquettes (3 Regimes x 3 Tiers) ----
    public static final RegistryObject<Item> KIBBLE_CARNIVORE_BASIQUE = registerKibble(
            "kibble_carnivore_basique", KibbleItem.Diet.CARNIVORE, KibbleItem.Tier.BASIQUE);
    public static final RegistryObject<Item> KIBBLE_CARNIVORE_SUPERIEUR = registerKibble(
            "kibble_carnivore_superieur", KibbleItem.Diet.CARNIVORE, KibbleItem.Tier.SUPERIEUR);
    public static final RegistryObject<Item> KIBBLE_CARNIVORE_APEX = registerKibble(
            "kibble_carnivore_apex", KibbleItem.Diet.CARNIVORE, KibbleItem.Tier.APEX);

    public static final RegistryObject<Item> KIBBLE_HERBIVORE_BASIQUE = registerKibble(
            "kibble_herbivore_basique", KibbleItem.Diet.HERBIVORE, KibbleItem.Tier.BASIQUE);

    // Casquette et badge : modele 3D en JSON (models/item/), comme les snacks.
    // Ce rendu est fiable ; le passage par GeckoLib laissait l'objet invisible.
    public static final RegistryObject<Item> ZOO_CAP = registerProduct("zoo_cap",
            com.lex3d.ultimatezootaming.blocks.ShopBlock.ShopType.SOUVENIR, 3);
    public static final RegistryObject<Item> ZOO_BADGE = registerProduct("zoo_badge",
            com.lex3d.ultimatezootaming.blocks.ShopBlock.ShopType.SOUVENIR, 2);
    public static final RegistryObject<Item> POPCORN = registerProduct("popcorn",
            com.lex3d.ultimatezootaming.blocks.ShopBlock.ShopType.MEAL, 2);
    public static final RegistryObject<Item> ICE_CREAM = registerProduct("ice_cream",
            com.lex3d.ultimatezootaming.blocks.ShopBlock.ShopType.ICECREAM, 2);
    public static final RegistryObject<Item> LEMONADE = registerProduct("lemonade",
            com.lex3d.ultimatezootaming.blocks.ShopBlock.ShopType.DRINK, 1);

    private static RegistryObject<Item> registerProduct(String name,
            com.lex3d.ultimatezootaming.blocks.ShopBlock.ShopType type, int price) {
        return ITEMS.register(name, () -> new com.lex3d.ultimatezootaming.items.ShopProductItem(
                type, price, new Item.Properties()));
    }


    public static final RegistryObject<Item> SUPER_REMEDY = ITEMS.register("super_remedy",
            () -> new com.lex3d.ultimatezootaming.items.SuperRemedyItem(
                    new Item.Properties().stacksTo(16)));

    // La MONNAIE du parc : le Billet de parc (remplace l'emeraude dans le zoo).
    public static final RegistryObject<Item> PARK_TICKET = ITEMS.register("park_ticket",
            () -> new Item(new Item.Properties().stacksTo(64)));

    /** Photo tenue par un visiteur qui vient de poser devant la borne.
     *  Purement decoratif : pas de collection, pas de cadre, pas d'album. */
    public static final RegistryObject<Item> VISITOR_PHOTO = ITEMS.register("visitor_photo",
            () -> new com.lex3d.ultimatezootaming.items.VisitorGearItem(new Item.Properties().stacksTo(1), "visitor_photo"));

    /** Echantillon genetique : l'ADN d'un animal, preleve a la Seringue. */
    public static final RegistryObject<Item> GENETIC_SAMPLE = ITEMS.register("genetic_sample",
            () -> new com.lex3d.ultimatezootaming.items.GeneticSampleItem(
                    new Item.Properties().stacksTo(16)));

    /** Seringue de prelevement : clic droit sur un animal pour un echantillon. */
    public static final RegistryObject<Item> SAMPLING_SYRINGE = ITEMS.register("sampling_syringe",
            () -> new com.lex3d.ultimatezootaming.items.SamplingSyringeItem(
                    new Item.Properties().stacksTo(1).durability(64)));

    /** Jumelles : le visiteur qui en tient contemple les enclos plus longtemps. */
    public static final RegistryObject<Item> BINOCULARS = ITEMS.register("binoculars",
            () -> new com.lex3d.ultimatezootaming.items.VisitorGearItem(new Item.Properties().stacksTo(1), "binoculars"));

    /** Perche a selfie : le visiteur s'arrete pour se prendre en photo. */
    public static final RegistryObject<Item> SELFIE_STICK = ITEMS.register("selfie_stick",
            () -> new com.lex3d.ultimatezootaming.items.VisitorGearItem(new Item.Properties().stacksTo(1), "selfie_stick"));

    /** Dechet recyclable : obtenu en ramassant les detritus du parc.
     *  Se revend en billets a la Tresorerie (bouton Recycler). */
    public static final RegistryObject<Item> RECYCLABLE_WASTE = ITEMS.register("recyclable_waste",
            () -> new Item(new Item.Properties().stacksTo(64)));



    /** Parapluie normal : les visiteurs le tiennent sous la pluie. */
    public static final RegistryObject<Item> UMBRELLA = ITEMS.register("umbrella",
            () -> new com.lex3d.ultimatezootaming.items.UmbrellaItem(
                    new Item.Properties().stacksTo(1), false));

    /** Parapluie kawaii avec oreilles : vendu en boutique souvenir. */
    public static final RegistryObject<Item> KAWAII_UMBRELLA = ITEMS.register("kawaii_umbrella",
            () -> new com.lex3d.ultimatezootaming.items.UmbrellaItem(
                    new Item.Properties().stacksTo(1), true));

    // ===== Objets tenus par les visiteurs (soda, popcorn, glace, barbe a papa, ballons) =====
    // Les 4 snacks sont CONSOMMABLES par le joueur (nourriture/boisson).
    public static final RegistryObject<Item> VISITOR_SODA = ITEMS.register("visitor_soda",
            () -> new com.lex3d.ultimatezootaming.items.VisitorGearItem(new Item.Properties()
                    .food(new net.minecraft.world.food.FoodProperties.Builder()
                            .nutrition(2).saturationMod(0.2f).alwaysEat().build())
                    .stacksTo(16),
                    "visitor_soda", net.minecraft.world.item.UseAnim.DRINK)); // on le BOIT
    public static final RegistryObject<Item> VISITOR_POPCORN = ITEMS.register("visitor_popcorn",
            () -> new com.lex3d.ultimatezootaming.items.VisitorGearItem(new Item.Properties()
                    .food(new net.minecraft.world.food.FoodProperties.Builder()
                            .nutrition(3).saturationMod(0.3f).build()),
                    "visitor_popcorn"));
    public static final RegistryObject<Item> VISITOR_ICECREAM = ITEMS.register("visitor_icecream",
            () -> new com.lex3d.ultimatezootaming.items.VisitorGearItem(new Item.Properties()
                    .food(new net.minecraft.world.food.FoodProperties.Builder()
                            .nutrition(4).saturationMod(0.3f).alwaysEat().build()),
                    "visitor_icecream"));
    public static final RegistryObject<Item> VISITOR_COTTON = ITEMS.register("visitor_cotton_candy",
            () -> new com.lex3d.ultimatezootaming.items.VisitorGearItem(new Item.Properties()
                    .food(new net.minecraft.world.food.FoodProperties.Builder()
                            .nutrition(2).saturationMod(0.1f).alwaysEat().build()),
                    "visitor_cotton_candy"));
    // 6 ballons colores (rouge, bleu, vert, jaune, rose, violet)
    public static final RegistryObject<Item> BALLOON_RED = ITEMS.register("balloon_red",
            () -> new com.lex3d.ultimatezootaming.items.VisitorGearItem(new Item.Properties(), "balloon", "balloon_red", null));
    public static final RegistryObject<Item> BALLOON_BLUE = ITEMS.register("balloon_blue",
            () -> new com.lex3d.ultimatezootaming.items.VisitorGearItem(new Item.Properties(), "balloon", "balloon_blue", null));
    public static final RegistryObject<Item> BALLOON_GREEN = ITEMS.register("balloon_green",
            () -> new com.lex3d.ultimatezootaming.items.VisitorGearItem(new Item.Properties(), "balloon", "balloon_green", null));
    public static final RegistryObject<Item> BALLOON_YELLOW = ITEMS.register("balloon_yellow",
            () -> new com.lex3d.ultimatezootaming.items.VisitorGearItem(new Item.Properties(), "balloon", "balloon_yellow", null));
    public static final RegistryObject<Item> BALLOON_PINK = ITEMS.register("balloon_pink",
            () -> new com.lex3d.ultimatezootaming.items.VisitorGearItem(new Item.Properties(), "balloon", "balloon_pink", null));
    public static final RegistryObject<Item> BALLOON_PURPLE = ITEMS.register("balloon_purple",
            () -> new com.lex3d.ultimatezootaming.items.VisitorGearItem(new Item.Properties(), "balloon", "balloon_purple", null));

    public static final RegistryObject<Item> PLOT_SELECTOR = ITEMS.register("plot_selector",
            () -> new com.lex3d.ultimatezootaming.items.PlotSelectorItem(
                    new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> DIRECTOR_TABLET = ITEMS.register("director_tablet",
            () -> new com.lex3d.ultimatezootaming.items.DirectorTabletItem(
                    new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> ZOO_MAP = ITEMS.register("zoo_map",
            () -> new com.lex3d.ultimatezootaming.items.ZooMapItem(
                    new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> ZOO_GUIDE = ITEMS.register("zoo_guide",
            () -> new com.lex3d.ultimatezootaming.items.ZooGuideItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> FODDER = ITEMS.register("fodder",
            () -> new com.lex3d.ultimatezootaming.items.FodderItem(
                    com.lex3d.ultimatezootaming.items.KibbleItem.Diet.HERBIVORE, new Item.Properties()));

    public static final RegistryObject<Item> FODDER_MEAT = ITEMS.register("fodder_meat",
            () -> new com.lex3d.ultimatezootaming.items.FodderItem(
                    com.lex3d.ultimatezootaming.items.KibbleItem.Diet.CARNIVORE, new Item.Properties()));

    public static final RegistryObject<Item> FODDER_FISH = ITEMS.register("fodder_fish",
            () -> new com.lex3d.ultimatezootaming.items.FodderItem(
                    com.lex3d.ultimatezootaming.items.KibbleItem.Diet.PISCIVORE, new Item.Properties()));
    public static final RegistryObject<Item> KIBBLE_HERBIVORE_SUPERIEUR = registerKibble(
            "kibble_herbivore_superieur", KibbleItem.Diet.HERBIVORE, KibbleItem.Tier.SUPERIEUR);
    public static final RegistryObject<Item> KIBBLE_HERBIVORE_APEX = registerKibble(
            "kibble_herbivore_apex", KibbleItem.Diet.HERBIVORE, KibbleItem.Tier.APEX);

    public static final RegistryObject<Item> KIBBLE_PISCIVORE_BASIQUE = registerKibble(
            "kibble_piscivore_basique", KibbleItem.Diet.PISCIVORE, KibbleItem.Tier.BASIQUE);
    public static final RegistryObject<Item> KIBBLE_PISCIVORE_SUPERIEUR = registerKibble(
            "kibble_piscivore_superieur", KibbleItem.Diet.PISCIVORE, KibbleItem.Tier.SUPERIEUR);
    public static final RegistryObject<Item> KIBBLE_PISCIVORE_APEX = registerKibble(
            "kibble_piscivore_apex", KibbleItem.Diet.PISCIVORE, KibbleItem.Tier.APEX);

    // ---- Sifflet ----
    public static final RegistryObject<Item> WHISTLE = ITEMS.register("whistle",
            () -> new WhistleItem(new Item.Properties().stacksTo(1)));

    // ---- Conteneur generique (cage/filet plein) ----
    public static final RegistryObject<Item> ZOO_KEEPER_SPAWN = ITEMS.register("zoo_keeper_spawn_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(ModEntities.ZOO_KEEPER,
                    0x4E844A, 0x3C6E3C, new Item.Properties()));

    public static final RegistryObject<Item> ANIMAL_REMEDY = ITEMS.register("animal_remedy",
            () -> new AnimalRemedyItem(new Item.Properties()));

    public static final RegistryObject<Item> SURVEYOR_STAFF = ITEMS.register("surveyor_staff",
            () -> new SurveyorStaffItem(new Item.Properties()));

    public static final RegistryObject<Item> OCCUPIED_CONTAINER = ITEMS.register("occupied_container",
            () -> new OccupiedContainerItem(new Item.Properties()));

    private static RegistryObject<Item> registerKibble(String name, KibbleItem.Diet diet, KibbleItem.Tier tier) {
        return ITEMS.register(name, () -> new KibbleItem(diet, tier, new Item.Properties()));
    }
}
