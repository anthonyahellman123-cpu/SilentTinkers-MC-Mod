package com.anthonyahellman.silenttinkers.material;

import com.anthonyahellman.silenttinkers.SilentTinkersMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Generates FTB Quests material circles from the authoritative runtime scan. */
public final class FtbQuestMaterialExporter {
    private static final String GUIDE_UNLOCK_ID = "51A17E17A5C0DE02";
    private static final String CORE_CHAPTER_ID = "51A17E17A5C0DE20";
    private static final String ADDON_CHAPTER_ID = "51A17E17A5C0DE30";

    private FtbQuestMaterialExporter() {}

    public static boolean writeIfAvailable(UnifiedMaterialDiscovery.Snapshot snapshot) {
        if (!ModList.get().isLoaded("ftbquests")) return false;

        List<Entry> core = new ArrayList<>();
        Map<String, List<Entry>> addons = new LinkedHashMap<>();
        snapshot.evaluations().stream()
                .filter(MaterialGenerationEvaluation::readyForMutation)
                .map(MaterialGenerationEvaluation::request)
                .filter(request -> DynamicBridgeItemPolicy.isOneUnitMaterial(request.physicalItem()))
                .map(Entry::from)
                .sorted(Comparator.comparing(entry -> entry.item().toString()))
                .forEach(entry -> {
                    if (entry.isCore()) core.add(entry);
                    else addons.computeIfAbsent(entry.addonNamespace(), ignored -> new ArrayList<>()).add(entry);
                });

        Path chapters = FMLPaths.CONFIGDIR.get().resolve("ftbquests/quests/chapters");
        try {
            Files.createDirectories(chapters);
            boolean coreChanged = writeIfChanged(chapters.resolve("silenttinkers_available_materials.snbt"),
                    buildCoreChapter(core));
            boolean addonsChanged = writeIfChanged(chapters.resolve("silenttinkers_addon_materials.snbt"),
                    buildAddonChapter(addons));
            if (coreChanged || addonsChanged) {
                SilentTinkersMod.LOGGER.info(
                        "[SilentTinkers:FTBQUESTS_GENERATED] coreMaterials={} addonNamespaces={} addonMaterials={} reloadRequested=true",
                        core.size(), addons.size(), addons.values().stream().mapToInt(List::size).sum());
            }
            return coreChanged || addonsChanged;
        } catch (IOException exception) {
            SilentTinkersMod.LOGGER.error(
                    "[SilentTinkers:FTBQUESTS_FAILED] Could not update generated material chapters", exception);
            return false;
        }
    }

    private static String buildCoreChapter(List<Entry> materials) {
        String icon = materials.isEmpty() ? "minecraft:iron_ingot" : materials.get(0).item().toString();
        StringBuilder out = chapterStart("silenttinkers_available_materials", CORE_CHAPTER_ID,
                icon, "Available Materials", 1);
        appendMaterialGrid(out, materials, 0.0, 0.0, 8);
        return chapterEnd(out);
    }

    private static String buildAddonChapter(Map<String, List<Entry>> groups) {
        String icon = groups.values().stream().flatMap(List::stream).findFirst()
                .map(entry -> entry.item().toString())
                .orElse("silenttinkers:composite_alloy_sample");
        StringBuilder out = chapterStart("silenttinkers_addon_materials", ADDON_CHAPTER_ID,
                icon, "Modded Add-on Materials", 2);
        double y = 0.0;
        for (Map.Entry<String, List<Entry>> group : groups.entrySet()) {
            List<Entry> entries = group.getValue();
            Entry first = entries.get(0);
            out.append("\t\t{\n")
                    .append("\t\t\tdependencies: [\"").append(GUIDE_UNLOCK_ID).append("\"]\n")
                    .append("\t\t\thide_until_deps_complete: true\n")
                    .append("\t\t\ticon: \"").append(first.item()).append("\"\n")
                    .append("\t\t\tid: \"").append(id("header:" + group.getKey())).append("\"\n")
                    .append("\t\t\tshape: \"gear\"\n")
                    .append("\t\t\tsize: 1.5d\n")
                    .append("\t\t\ttitle: \"").append(escape(humanize(group.getKey()))).append("\"\n")
                    .append("\t\t\tx: -5.0d\n")
                    .append("\t\t\ty: ").append(decimal(y)).append("d\n")
                    .append("\t\t}\n");
            appendMaterialGrid(out, entries, 1.0, y, 7);
            y += Math.max(3.0, Math.ceil(entries.size() / 7.0) * 2.0 + 1.0);
        }
        return chapterEnd(out);
    }

    private static StringBuilder chapterStart(String filename, String chapterId, String icon,
                                              String title, int order) {
        return new StringBuilder("{\n")
                .append("\tdefault_hide_dependency_lines: true\n")
                .append("\tdefault_quest_shape: \"circle\"\n")
                .append("\tfilename: \"").append(filename).append("\"\n")
                .append("\ticon: \"").append(icon).append("\"\n")
                .append("\tid: \"").append(chapterId).append("\"\n")
                .append("\torder_index: ").append(order).append("\n")
                .append("\tprogression_mode: \"flexible\"\n")
                .append("\tquest_links: [ ]\n")
                .append("\tquests: [\n")
                .append("\t\t{\n")
                .append("\t\t\tdependencies: [\"").append(GUIDE_UNLOCK_ID).append("\"]\n")
                .append("\t\t\tdescription: [\"Generated automatically from SilentTinkers' current material scan.\"]\n")
                .append("\t\t\thide_until_deps_complete: true\n")
                .append("\t\t\ticon: \"").append(icon).append("\"\n")
                .append("\t\t\tid: \"").append(id("intro:" + filename)).append("\"\n")
                .append("\t\t\tshape: \"gear\"\n")
                .append("\t\t\tsize: 2.0d\n")
                .append("\t\t\ttitle: \"").append(title).append("\"\n")
                .append("\t\t\tx: 0.0d\n")
                .append("\t\t\ty: -3.0d\n")
                .append("\t\t}\n");
    }

    private static void appendMaterialGrid(StringBuilder out, List<Entry> materials,
                                           double startX, double startY, int columns) {
        for (int index = 0; index < materials.size(); index++) {
            Entry entry = materials.get(index);
            double x = startX + (index % columns) * 2.0;
            double y = startY + (index / columns) * 2.0;
            out.append("\t\t{\n")
                    .append("\t\t\tdependencies: [\"").append(GUIDE_UNLOCK_ID).append("\"]\n")
                    .append("\t\t\tdescription: [\n")
                    .append("\t\t\t\t\"Physical item: ").append(escape(entry.item().toString())).append("\"\n")
                    .append("\t\t\t\t\"Silent Gear material: ").append(escape(entry.material().toString())).append("\"\n")
                    .append("\t\t\t\t\"This material can be melted into dynamic composite alloy and cast into supported Tinkers parts.\"\n")
                    .append("\t\t\t]\n")
                    .append("\t\t\thide_until_deps_complete: true\n")
                    .append("\t\t\ticon: \"").append(entry.item()).append("\"\n")
                    .append("\t\t\tid: \"").append(id("material:" + entry.item())).append("\"\n")
                    .append("\t\t\ttitle: \"").append(escape(humanize(entry.item().getPath()))).append("\"\n")
                    .append("\t\t\tx: ").append(decimal(x)).append("d\n")
                    .append("\t\t\ty: ").append(decimal(y)).append("d\n")
                    .append("\t\t}\n");
        }
    }

    private static String chapterEnd(StringBuilder out) {
        return out.append("\t]\n\ttitle: \"")
                .append(out.indexOf("addon_materials") >= 0 ? "Modded Add-on Materials" : "Available Materials")
                .append("\"\n}\n").toString();
    }

    private static boolean writeIfChanged(Path destination, String content) throws IOException {
        if (Files.exists(destination) && Files.readString(destination).equals(content)) return false;
        Path temporary = destination.resolveSibling(destination.getFileName() + ".tmp");
        Files.writeString(temporary, content, StandardCharsets.UTF_8);
        try {
            Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
            Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
        }
        return true;
    }

    private static String id(String key) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(("silenttinkers:" + key).getBytes(StandardCharsets.UTF_8));
            StringBuilder value = new StringBuilder(16);
            for (int index = 0; index < 8; index++) value.append(String.format("%02X", digest[index]));
            return value.toString();
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is required by Java", impossible);
        }
    }

    private static String humanize(String value) {
        String[] words = value.replace('-', '_').split("_");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (!result.isEmpty()) result.append(' ');
            result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return result.toString();
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("&", "\\\\&");
    }

    private static String decimal(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private record Entry(ResourceLocation item, ResourceLocation material) {
        private static Entry from(MaterialGenerationRequest request) {
            return new Entry(request.physicalItem(), request.sourceMaterialId().orElseThrow());
        }

        private boolean isCore() {
            return item.getNamespace().equals("minecraft") && material.getNamespace().equals("silentgear");
        }

        private String addonNamespace() {
            return item.getNamespace().equals("minecraft") ? material.getNamespace() : item.getNamespace();
        }
    }
}
