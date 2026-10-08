"""Run the exact Buck slab-source resolver against a minimal registry fixture."""
from pathlib import Path
from tempfile import TemporaryDirectory
import subprocess

source = Path("src/main/java/dev/buck/registry/BuckContent.java").read_text(encoding="utf-8")
start = source.index("    public static SlabBlock getSourceSlab(String id) {")
end = source.index("\n    }", start) + len("\n    }")
method = source[start:end]
entity_source = Path("src/main/java/dev/buck/block/entity/MixedSlabBlockEntity.java").read_text(encoding="utf-8")
entity_methods = []
for name, return_type in (("loadIds", "void"), ("firstId", "String"), ("secondId", "String")):
    index = entity_source.index(f"    private {return_type} {name}(")
    closing = entity_source.index("\n    }", index) + len("\n    }")
    entity_methods.append(entity_source[index:closing])
entity_methods = "\n".join(entity_methods)

harness = """
import java.util.*;
import java.util.stream.Stream;

public class ResolverCheck {
    static class Block {}
    static class SlabBlock extends Block {}
    static class OtherBlock extends Block {}
    static class FakeRegistry {
        final Map<Block, String> keys = new LinkedHashMap<>();
        void put(String id, Block block) { keys.put(block, id); }
        Stream<Block> stream() { return keys.keySet().stream(); }
        String getKey(Block block) { return keys.get(block); }
    }
    static class BuiltInRegistries {
        static final FakeRegistry BLOCK = new FakeRegistry();
    }
    static final Map<String, SlabBlock> SOURCES_BY_ID = new HashMap<>();

    %s

    static class BuckContent {
        static SlabBlock getSourceSlab(String id) { return ResolverCheck.getSourceSlab(id); }
        static String getSourceId(SlabBlock slab) { return BuiltInRegistries.BLOCK.getKey(slab); }
    }
    static class MixedSlabBlockEntity {
        SlabBlock first;
        SlabBlock second;
        String savedFirstId = "";
        String savedSecondId = "";
        %s
    }

    static void check(boolean condition, String name) {
        if (!condition) throw new AssertionError(name);
    }
    public static void main(String[] args) {
        SlabBlock oak = new SlabBlock();
        SlabBlock modded = new SlabBlock();
        OtherBlock stone = new OtherBlock();
        BuiltInRegistries.BLOCK.put("minecraft:oak_slab", oak);
        BuiltInRegistries.BLOCK.put("samplemod:marble_slab", modded);
        BuiltInRegistries.BLOCK.put("samplemod:stone", stone);
        SOURCES_BY_ID.put("minecraft:oak_slab", oak);

        check(getSourceSlab("minecraft:oak_slab") == oak, "vanilla source");
        check(getSourceSlab("samplemod:marble_slab") == modded, "modded source");
        check(getSourceSlab("samplemod:stone") == null, "non-slab rejected");
        check(getSourceSlab("samplemod:missing_slab") == null, "missing mod rejected");
        check(getSourceSlab("") == null, "missing saved ID rejected");
        MixedSlabBlockEntity saved = new MixedSlabBlockEntity();
        saved.loadIds("minecraft:oak_slab", "samplemod:marble_slab");
        check(saved.first == oak && saved.second == modded, "mixed sources on normal load");
        check(saved.firstId().equals("minecraft:oak_slab"), "first source round-trip");
        check(saved.secondId().equals("samplemod:marble_slab"), "second source round-trip");

        BuiltInRegistries.BLOCK.keys.remove(modded);
        MixedSlabBlockEntity missing = new MixedSlabBlockEntity();
        missing.loadIds("minecraft:oak_slab", "samplemod:marble_slab");
        check(missing.second == null, "unavailable mod remains unresolved");
        check(missing.secondId().equals("samplemod:marble_slab"), "unresolved ID preserved");
        BuiltInRegistries.BLOCK.put("samplemod:marble_slab", modded);
        MixedSlabBlockEntity restored = new MixedSlabBlockEntity();
        restored.loadIds(missing.firstId(), missing.secondId());
        check(restored.second == modded, "restore after mod reinstalled");
        System.out.println("mixed source persistence: 11 scenarios passed");
    }
}
""" % (method, entity_methods)

with TemporaryDirectory() as temp:
    path = Path(temp) / "ResolverCheck.java"
    path.write_text(harness, encoding="utf-8")
    subprocess.run(["javac", "-encoding", "UTF-8", str(path)], check=True)
    subprocess.run(["java", "-cp", temp, "ResolverCheck"], check=True)
