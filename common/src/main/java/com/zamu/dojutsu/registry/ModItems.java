package com.zamu.dojutsu.registry;

import com.zamu.dojutsu.DojutsuMod;
import com.zamu.dojutsu.domain.bijuu.TailedBeast;
import com.zamu.dojutsu.domain.items.MaskType;
import com.zamu.dojutsu.domain.items.ScrollType;
import com.zamu.dojutsu.domain.throwing.ThrowableType;
import com.zamu.dojutsu.items.BeastSpawnEggItem;
import com.zamu.dojutsu.items.ThrowableItem;
import com.zamu.dojutsu.items.ContractItem;
import com.zamu.dojutsu.items.DnaItem;
import com.zamu.dojutsu.items.DropUseItem;
import com.zamu.dojutsu.items.JutsuItem;
import com.zamu.dojutsu.items.MaskItem;
import com.zamu.dojutsu.items.RamenItem;
import com.zamu.dojutsu.items.RationPillItem;
import com.zamu.dojutsu.items.LearnerItem;
import com.zamu.dojutsu.items.ScrollItem;
import com.zamu.dojutsu.items.WeaponComponent;
import com.zamu.dojutsu.items.WeaponItem;
import com.zamu.dojutsu.platform.PlatformServices;
import com.zamu.dojutsu.platform.Registrar;
import com.zamu.dojutsu.platform.RegistryEntry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.Foods;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Registro de items del mod: el placeholder de F0 ({@code ninja_debug}) y los items reales de contenido
 * (F3+: el Skill Learner).
 */
public final class ModItems {

    private static final Registrar<Item> ITEMS = PlatformServices.REGISTRAR.create(Registries.ITEM);

    /** Item de depuracion, obtenible con {@code /give @s dojutsu:ninja_debug}. */
    public static final RegistryEntry<Item> NINJA_DEBUG =
            ITEMS.register(DojutsuMod.id("ninja_debug"), () -> new Item(new Item.Properties()));

    /** Skill Learner (F3/DJT-055): ensena los jutsus de un elemento; no apilable, como el carrot original. */
    public static final RegistryEntry<LearnerItem> LEARNER =
            ITEMS.register(DojutsuMod.id("learner"), () -> new LearnerItem(new Item.Properties().stacksTo(1)));

    /** Item de jutsu (F4/DJT-075): porta una tecnica aprendida; no apilable, como el carrot original. */
    public static final RegistryEntry<JutsuItem> JUTSU =
            ITEMS.register(DojutsuMod.id("jutsu"), () -> new JutsuItem(new Item.Properties().stacksTo(1)));

    /** Chakra Blade (F4/DJT-082): arma base, daño +3 ({@code give/weapons/chakra_blade}). */
    public static final RegistryEntry<WeaponItem> CHAKRA_BLADE =
            ITEMS.register(DojutsuMod.id("chakra_blade"), () -> new WeaponItem(new Item.Properties().stacksTo(1)));

    /** Kubikiribocho (F4/DJT-082): espadon, daño +8 ({@code give/weapons/kubikiribocho}). */
    public static final RegistryEntry<WeaponItem> KUBIKIRIBOCHO =
            ITEMS.register(DojutsuMod.id("kubikiribocho"), () -> new WeaponItem(new Item.Properties().stacksTo(1)));

    /** Samehada (F4/DJT-084): espada viva que absorbe chakra al golpear; daño +6, req kenjutsu 250. */
    public static final RegistryEntry<WeaponItem> SAMEHADA =
            ITEMS.register(DojutsuMod.id("samehada"), () -> new WeaponItem(new Item.Properties().stacksTo(1)));

    /**
     * Muestra de ADN (F8/DJT-170): un solo item para los 27 estados (sin identificar + 26 tipos). Apilable: son
     * muestras, y en el datapack tampoco lo impedia nada.
     */
    public static final RegistryEntry<DnaItem> DNA =
            ITEMS.register(DojutsuMod.id("dna"), () -> new DnaItem(new Item.Properties().stacksTo(16)));

    /** Pildoras de Racion Militar (F8/DJT-170): 5 cargas en la durabilidad, no cinco items distintos. */
    public static final RegistryEntry<RationPillItem> RATION_PILLS =
            ITEMS.register(DojutsuMod.id("ration_pills"), () -> new RationPillItem(new Item.Properties()));

    /** Las seis mascaras (F8/DJT-170): un item por mascara; el tipo ES el item, no un componente. */
    public static final Map<MaskType, RegistryEntry<MaskItem>> MASKS = registerMasks();

    /**
     * Pergamino de contrato de invocacion (F8/DJT-175): un solo item para los 28 estados (en blanco + 27
     * especies). No apilable: un contrato firmado es unico y reutilizable.
     */
    public static final RegistryEntry<ContractItem> CONTRACT =
            ITEMS.register(DojutsuMod.id("contract"), () -> new ContractItem(new Item.Properties().stacksTo(1)));

    /** Las cinco armas arrojadizas (F8/DJT-180): un item por tipo; el tipo ES el item. */
    public static final Map<ThrowableType, RegistryEntry<ThrowableItem>> THROWABLES = registerThrowables();

    /** Durabilidad del tanto ({@code MaxDurability:1000}). */
    public static final int TANTO_DURABILITY = 1000;

    /** Kenjutsu que anuncia el tanto, en escala del mod (datapack 100 ÷ 4). */
    public static final int TANTO_KENJUTSU = 25;

    /**
     * Tanto (F10/DJT-225): la cuarta arma ({@code give/weapons/tanto}). Daño +6, requisito de kenjutsu 100 en el
     * datapack = {@value #TANTO_KENJUTSU} aqui (÷4) y {@value #TANTO_DURABILITY} de durabilidad — la unica arma
     * del mod que se gasta, porque es la unica que el datapack marcaba con {@code HasCustomDurability}.
     */
    public static final RegistryEntry<WeaponItem> TANTO =
            ITEMS.register(DojutsuMod.id("tanto"),
                    () -> new WeaponItem(new Item.Properties().stacksTo(1).durability(TANTO_DURABILITY)));

    /** Bomba de Humo (F10/DJT-225): cortina de 6 s al soltarla. No aplica ningun efecto de estado. */
    public static final RegistryEntry<DropUseItem> SMOKE_BOMB =
            ITEMS.register(DojutsuMod.id("smoke_bomb"),
                    () -> new DropUseItem(DropUseItem.Kind.SMOKE_BOMB, new Item.Properties().stacksTo(16)));

    /** Sello Explosivo suelto (F10/DJT-225): mina que se coloca al soltarla y tarda 10 s en armarse. */
    public static final RegistryEntry<DropUseItem> EXPLOSIVE_TAG =
            ITEMS.register(DojutsuMod.id("explosive_tag"),
                    () -> new DropUseItem(DropUseItem.Kind.EXPLOSIVE_TAG, new Item.Properties().stacksTo(16)));

    /** Ramen (F10/DJT-225): la nutricion de la ternera cocinada y +150 de chakra al comerlo. */
    public static final RegistryEntry<RamenItem> RAMEN =
            ITEMS.register(DojutsuMod.id("ramen"), () -> new RamenItem(ramenProperties()));

    /** Shoyu Ramen (F10/DJT-225): identico al ramen salvo por el modelo; cuesta un alga seca mas. */
    public static final RegistryEntry<RamenItem> SHOYU_RAMEN =
            ITEMS.register(DojutsuMod.id("shoyu_ramen"), () -> new RamenItem(ramenProperties()));

    /** Los tres pergaminos de un solo uso (F10/DJT-225): un item por tipo; el tipo ES el item. */
    public static final Map<ScrollType, RegistryEntry<ScrollItem>> SCROLLS = registerScrolls();

    /** Los <b>nueve</b> huevos de invocacion de Bestia con Cola (F10/DJT-225). Ver {@link #registerBeastEggs()}. */
    public static final Map<TailedBeast, RegistryEntry<BeastSpawnEggItem>> BEAST_EGGS = registerBeastEggs();

    /**
     * <b>Vehiculo de render</b> del cuerpo de las Bestias con Cola (F10/DJT-228). No se obtiene, no se usa y no
     * sale en ninguna pestaña creativa: existe porque Minecraft <b>solo hornea modelos de item que pertenecen a
     * un item registrado</b>, y el cuerpo de Gyuki y Kurama es un modelo de item de mil cubos, no una malla de
     * entidad.
     *
     * <p>Es la misma maniobra del datapack, que cuelga sus modelos de un {@code knowledge_book} vanilla. La
     * diferencia es que aqui el vehiculo es propio y se llama por lo que hace, en vez de secuestrar un item del
     * juego: {@code beast_body} con {@code CustomModelData} = numero de colas.
     *
     * <p>La alternativa era registrar los modelos como <i>sueltos</i>, pero eso no lo cubre Architectury 13.0.8
     * y habria costado un puerto de plataforma por loader para ahorrarse una entrada de registro.
     */
    public static final RegistryEntry<Item> BEAST_BODY =
            ITEMS.register(DojutsuMod.id("beast_body"), () -> new Item(new Item.Properties().stacksTo(1)));

    private ModItems() {
    }

    /** Nutricion del ramen: la del {@code cooked_beef} sobre el que lo montaba el datapack. */
    private static Item.Properties ramenProperties() {
        return new Item.Properties().food(Foods.COOKED_BEEF);
    }

    /**
     * Registra los tres pergaminos de un barrido: identicos salvo por su tipo. Lo que cambia es el efecto, y ese
     * vive en {@code ScrollService}.
     */
    private static Map<ScrollType, RegistryEntry<ScrollItem>> registerScrolls() {
        Map<ScrollType, RegistryEntry<ScrollItem>> scrolls = new EnumMap<>(ScrollType.class);
        for (ScrollType type : ScrollType.values()) {
            scrolls.put(type, ITEMS.register(DojutsuMod.id(type.itemPath()),
                    () -> new ScrollItem(type, new Item.Properties().stacksTo(1))));
        }
        return Collections.unmodifiableMap(scrolls);
    }

    /**
     * Registra los <b>nueve</b> huevos de bestia. El Diez Colas se queda fuera a proposito: el datapack le da
     * huevo por comando pero <b>no receta</b> ({@code main/internal/crafting/tb/} tiene nueve archivos, y las
     * nueve son las que declara {@code data/dojutsu/drop_crafting/}), asi que registrarlo crearia un item
     * inobtenible. Su equivalente sigue siendo {@code /dojutsubeast summon ten_tails}.
     */
    private static Map<TailedBeast, RegistryEntry<BeastSpawnEggItem>> registerBeastEggs() {
        Map<TailedBeast, RegistryEntry<BeastSpawnEggItem>> eggs = new EnumMap<>(TailedBeast.class);
        for (TailedBeast beast : TailedBeast.values()) {
            if (beast.isTenTails()) {
                continue;
            }
            eggs.put(beast, ITEMS.register(DojutsuMod.id("spawn_egg_" + beast.id()),
                    () -> new BeastSpawnEggItem(beast, new Item.Properties().stacksTo(1))));
        }
        return Collections.unmodifiableMap(eggs);
    }

    /** Registra las cinco arrojadizas; el daño en mano va en el atributo, no en codigo. */
    private static Map<ThrowableType, RegistryEntry<ThrowableItem>> registerThrowables() {
        Map<ThrowableType, RegistryEntry<ThrowableItem>> throwables = new EnumMap<>(ThrowableType.class);
        for (ThrowableType type : ThrowableType.values()) {
            throwables.put(type, ITEMS.register(DojutsuMod.id(type.id()),
                    () -> new ThrowableItem(type, new Item.Properties().stacksTo(16))));
        }
        return Collections.unmodifiableMap(throwables);
    }

    /** Registra las seis mascaras de un barrido: identicas salvo por su tipo. */
    private static Map<MaskType, RegistryEntry<MaskItem>> registerMasks() {
        Map<MaskType, RegistryEntry<MaskItem>> masks = new EnumMap<>(MaskType.class);
        for (MaskType mask : MaskType.values()) {
            masks.put(mask, ITEMS.register(DojutsuMod.id("mask_" + mask.id()),
                    () -> new MaskItem(mask, new Item.Properties().stacksTo(1))));
        }
        return Collections.unmodifiableMap(masks);
    }

    /** Crea el stack de un arma con su componente y nombre localizado (el daño lo aplica el servicio). */
    public static ItemStack createWeapon(RegistryEntry<WeaponItem> item, String type, int kenjutsuReq, double baseDamage) {
        ItemStack stack = new ItemStack(item.get());
        stack.set(com.zamu.dojutsu.registry.ModDataComponents.WEAPON.get(),
                new WeaponComponent(type, kenjutsuReq, baseDamage));
        stack.set(DataComponents.ITEM_NAME, Component.translatable("item.dojutsu." + type));
        return stack;
    }

    /** Chakra Blade listo (tipo, sin req de kenjutsu, daño base 3). */
    public static ItemStack chakraBlade() {
        return createWeapon(CHAKRA_BLADE, "chakra_blade", 0, 3.0D);
    }

    /** Kubikiribocho listo (tipo, sin req de kenjutsu, daño base 8). */
    public static ItemStack kubikiribocho() {
        return createWeapon(KUBIKIRIBOCHO, "kubikiribocho", 0, 8.0D);
    }

    /** Tanto listo (tipo, kenjutsu {@value #TANTO_KENJUTSU} anunciado, daño base 6). */
    public static ItemStack tanto() {
        return createWeapon(TANTO, "tanto", TANTO_KENJUTSU, 6.0D);
    }

    /** Samehada lista (tipo, req kenjutsu 250 para absorber, daño base 6). */
    public static ItemStack samehada() {
        return createWeapon(SAMEHADA, "samehada",
                com.zamu.dojutsu.jutsu.kenjutsu.SamehadaService.KENJUTSU_REQ, 6.0D);
    }

    /** Confirma el registro. Llamar durante la inicializacion del mod. */
    public static void register() {
        ITEMS.commit();
    }
}
