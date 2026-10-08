package dev.buck.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.buck.Buck;
import dev.buck.block.MixedSlabBlock;
import dev.buck.block.VerticalSlabBlock;
import dev.buck.block.WeatheringVerticalSlabBlock;
import dev.buck.block.entity.MixedSlabBlockEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class BuckContent {
    private static final Logger LOGGER = LoggerFactory.getLogger(Buck.MOD_ID);
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Buck.MOD_ID, Registries.BLOCK);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Buck.MOD_ID, Registries.ITEM);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Buck.MOD_ID, Registries.BLOCK_ENTITY_TYPE);

    public static final List<RegistrySupplier<Block>> VERTICAL_SLABS = new ArrayList<>();

    private static final Map<SlabBlock, RegistrySupplier<Block>> VERTICAL_BLOCKS_BY_SOURCE = new HashMap<>();
    private static final Map<String, SlabBlock> SOURCES_BY_ID = new HashMap<>();

    public static final RegistrySupplier<Block> MIXED_SLAB = BLOCKS.register(
            "mixed_slab",
            () -> new MixedSlabBlock(mixedSlabProperties())
    );

    public static final RegistrySupplier<BlockEntityType<MixedSlabBlockEntity>> MIXED_SLAB_ENTITY =
            BLOCK_ENTITIES.register("mixed_slab", BuckContent::createMixedSlabEntityType);

    static {
        BuiltInRegistries.BLOCK.stream()
                .filter(block -> BuiltInRegistries.BLOCK.getKey(block).getNamespace().equals("minecraft"))
                .filter(block -> block instanceof SlabBlock)
                .sorted(Comparator.comparing(block -> BuiltInRegistries.BLOCK.getKey(block).getPath()))
                .forEach(block -> registerVerticalSlab(
                        BuiltInRegistries.BLOCK.getKey(block).getPath(),
                        (SlabBlock) block
                ));
    }

    private BuckContent() {
    }

    public static void init() {
        BLOCKS.register();
        ITEMS.register();
        BLOCK_ENTITIES.register();
        LOGGER.info("Prepared {} vertical slabs and 1 generic mixed slab block", VERTICAL_SLABS.size());
    }

    public static SlabBlock getSourceSlab(String id) {
        SlabBlock vanilla = SOURCES_BY_ID.get(id);
        if (vanilla != null) {
            return vanilla;
        }
        // External slab blocks are registered after our vanilla source map is prepared.
        return BuiltInRegistries.BLOCK.stream()
                .filter(block -> block instanceof SlabBlock)
                .filter(block -> BuiltInRegistries.BLOCK.getKey(block).toString().equals(id))
                .map(block -> (SlabBlock) block)
                .findFirst()
                .orElse(null);
    }

    public static String getSourceId(SlabBlock source) {
        return BuiltInRegistries.BLOCK.getKey(source).toString();
    }

    public static VerticalSlabBlock getVerticalSlabBlock(SlabBlock source) {
        RegistrySupplier<Block> supplier = VERTICAL_BLOCKS_BY_SOURCE.get(source);
        return supplier == null ? null : (VerticalSlabBlock) supplier.get();
    }

    private static void registerVerticalSlab(String sourceId, SlabBlock source) {
        String name = "vertical_" + sourceId;

        RegistrySupplier<Block> block = BLOCKS.register(name, () -> {
            BlockBehaviour.Properties properties = copyBlockProperties(source, name);
            if (source instanceof WeatheringCopper weathering
                    && weathering.getAge() != WeatheringCopper.WeatherState.OXIDIZED) {
                properties.randomTicks();
            }
            return source instanceof WeatheringCopper
                    ? new WeatheringVerticalSlabBlock(source, properties)
                    : new VerticalSlabBlock(source, properties);
        });
        RegistrySupplier<Item> item = ITEMS.register(name, () -> {
            Item.Properties properties = new Item.Properties().arch$tab(CreativeModeTabs.BUILDING_BLOCKS);
            setRegistryIdIfRequired(properties, Registries.ITEM, name);
            return new BlockItem(block.get(), properties);
        });

        VERTICAL_SLABS.add(block);
        VERTICAL_BLOCKS_BY_SOURCE.put(source, block);
        SOURCES_BY_ID.put(BuiltInRegistries.BLOCK.getKey(source).toString(), source);
    }

    private static BlockBehaviour.Properties mixedSlabProperties() {
        BlockBehaviour.Properties properties = BlockBehaviour.Properties.of().strength(1.5F).noOcclusion().randomTicks();
        setRegistryIdIfRequired(properties, Registries.BLOCK, "mixed_slab");
        return properties;
    }

    private static BlockEntityType<MixedSlabBlockEntity> createMixedSlabEntityType() {
        //? if >=26.3 {
        /*return new BlockEntityType<>(MixedSlabBlockEntity::new, Set.of(MIXED_SLAB.get()));
        *///?} else {
        return BlockEntityType.Builder.of(MixedSlabBlockEntity::new, MIXED_SLAB.get()).build(null);
        //?}
    }

    private static BlockBehaviour.Properties copyBlockProperties(Block source, String name) {
        try {
            Method copier;
            try {
                copier = BlockBehaviour.Properties.class.getMethod("ofFullCopy", BlockBehaviour.class);
            } catch (NoSuchMethodException ignored) {
                try {
                    copier = BlockBehaviour.Properties.class.getMethod("ofLegacyCopy", BlockBehaviour.class);
                } catch (NoSuchMethodException legacyMissing) {
                    copier = BlockBehaviour.Properties.class.getMethod("copy", BlockBehaviour.class);
                }
            }

            BlockBehaviour.Properties properties =
                    (BlockBehaviour.Properties) copier.invoke(null, source);
            resetIdentityDependentProperties(properties);
            setRegistryIdIfRequired(properties, Registries.BLOCK, name);
            return properties;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to copy block properties for " + name, exception);
        }
    }

    private static void resetIdentityDependentProperties(BlockBehaviour.Properties properties)
            throws ReflectiveOperationException {
        BlockBehaviour.Properties defaults = BlockBehaviour.Properties.of();
        for (String fieldName : List.of("drops", "descriptionId")) {
            try {
                Field field = BlockBehaviour.Properties.class.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(properties, field.get(defaults));
            } catch (NoSuchFieldException ignored) {
                // The field does not exist in this Minecraft version.
            }
        }
    }

    private static void setRegistryIdIfRequired(Object properties,
                                                ResourceKey<? extends Registry<?>> registry,
                                                String name) {
        Method setId = null;
        for (Method method : properties.getClass().getMethods()) {
            if (method.getName().equals("setId")
                    && method.getParameterCount() == 1
                    && method.getParameterTypes()[0] == ResourceKey.class) {
                setId = method;
                break;
            }
        }

        if (setId == null) {
            return;
        }

        try {
            Class<?> identifierClass = Class.forName("net.minecraft.resources.Identifier");
            Object identifier = identifierClass
                    .getMethod("fromNamespaceAndPath", String.class, String.class)
                    .invoke(null, Buck.MOD_ID, name);

            Method createKey = null;
            for (Method method : ResourceKey.class.getMethods()) {
                if (method.getName().equals("create")
                        && Modifier.isStatic(method.getModifiers())
                        && method.getParameterCount() == 2
                        && method.getParameterTypes()[0] == ResourceKey.class
                        && method.getParameterTypes()[1].isInstance(identifier)) {
                    createKey = method;
                    break;
                }
            }

            if (createKey == null) {
                throw new NoSuchMethodException("ResourceKey.create for " + identifierClass.getName());
            }

            Object key = createKey.invoke(null, registry, identifier);
            setId.invoke(properties, key);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to assign registry id for " + name, exception);
        }
    }
}
