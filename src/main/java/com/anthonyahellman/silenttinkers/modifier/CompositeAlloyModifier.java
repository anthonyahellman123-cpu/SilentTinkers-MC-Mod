package com.anthonyahellman.silenttinkers.modifier;

import com.anthonyahellman.silenttinkers.SilentTinkersMod;
import com.anthonyahellman.silenttinkers.config.SilentTinkersConfig;
import com.anthonyahellman.silenttinkers.material.AlloyComposition;
import com.anthonyahellman.silenttinkers.material.AlloyStatSnapshot;
import com.anthonyahellman.silenttinkers.material.AlloyVariantCodec;
import com.anthonyahellman.silenttinkers.material.MaterialDiscoveryState;
import com.anthonyahellman.silenttinkers.material.RuntimeBridgeHealth;
import com.anthonyahellman.silenttinkers.material.TraitAdapterPlan;
import com.anthonyahellman.silenttinkers.material.TraitForwardingPlan;
import com.anthonyahellman.silenttinkers.material.TraitSourceResolver;
import com.anthonyahellman.silenttinkers.recipe.CompositePickHeadCastingRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Tier;
import net.minecraftforge.common.TierSortingRegistry;
import slimeknights.tconstruct.library.materials.IMaterialRegistry;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.materials.definition.MaterialVariant;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.build.ModifierTraitHook;
import slimeknights.tconstruct.library.modifiers.hook.build.ToolStatsModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;
import slimeknights.tconstruct.library.tools.stat.ModifierStatsBuilder;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import slimeknights.tconstruct.tools.stats.HeadMaterialStats;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/** Replaces the composite head's placeholder contribution with Silent Gear's evaluated stats. */
public final class CompositeAlloyModifier extends Modifier implements ToolStatsModifierHook, ModifierTraitHook {
    private static final float PLACEHOLDER_DURABILITY = 1.0f;
    private static final float PLACEHOLDER_MINING_SPEED = 1.0f;
    private static final float PLACEHOLDER_MELEE_DAMAGE = 1.0f;
    private static final Set<String> LOGGED_STAT_VARIANTS = ConcurrentHashMap.newKeySet();
    private static final Set<String> LOGGED_MISSING_STAT_VARIANTS = ConcurrentHashMap.newKeySet();
    private static final Set<String> LOGGED_CONTEXTS_WITHOUT_COMPOSITE = ConcurrentHashMap.newKeySet();
    private static final Set<String> LOGGED_TRAIT_DECISIONS = ConcurrentHashMap.newKeySet();
    private static final Set<String> LOGGED_UNRESOLVED_TRAIT_SOURCES = ConcurrentHashMap.newKeySet();
    private static final Set<String> LOGGED_TRAIT_ADAPTERS = ConcurrentHashMap.newKeySet();
    private static final Set<ResourceLocation> LOGGED_UNKNOWN_TIERS = ConcurrentHashMap.newKeySet();

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.TOOL_STATS, ModifierHooks.MODIFIER_TRAITS);
    }

    @Override
    public void addTraits(IToolContext context, ModifierEntry modifier, TraitBuilder builder,
                          boolean firstEncounter) {
        if (!firstEncounter) return;
        IMaterialRegistry registry = MaterialRegistry.getInstance();
        for (MaterialVariant material : context.getMaterials()) {
            if (!material.getVariant().getId().equals(CompositePickHeadCastingRecipe.MATERIAL)) {
                continue;
            }
            String variant = material.getVariant().getVariant();
            // TConstruct creates base/display material instances whose variant is just
            // "silenttinkers:composite_alloy". Those do not carry an alloy payload and
            // must not be fed to the v1 codec used by real cast composite parts.
            if (!AlloyVariantCodec.isEncodedVariant(variant)) {
                continue;
            }
            AlloyComposition composition;
            try {
                composition = AlloyVariantCodec.decode(variant);
            } catch (IllegalArgumentException exception) {
                continue;
            }
            Collection<ResourceLocation> registeredMaterialIds = registry.getAllMaterials().stream()
                    .map(IMaterial::getIdentifier)
                    .map(id -> new ResourceLocation(id.getNamespace(), id.getPath()))
                    .toList();
            List<TraitForwardingPlan.Decision<ModifierEntry>> decisions = TraitForwardingPlan.create(
                    composition,
                    SilentTinkersConfig.traitThresholds(),
                    sourceId -> TraitSourceResolver.resolve(sourceId, registeredMaterialIds),
                    resolvedId -> registry.getTraits(new MaterialId(resolvedId), HeadMaterialStats.ID));

            Set<ModifierId> allNativeModifierIds = decisions.stream()
                    .flatMap(decision -> decision.forwardedTraits().stream())
                    .map(ModifierEntry::getId)
                    .collect(Collectors.toSet());
            List<TraitAdapterPlan.Decision> supportedAdapters = new java.util.ArrayList<>();

            for (TraitForwardingPlan.Decision<ModifierEntry> decision : decisions) {
                String decisionKey = composition.fingerprint() + "|" + decision.sourceMaterialId()
                        + "|" + decision.access();
                Optional<TraitSourceResolver.Resolution> resolution = decision.resolution();
                if (resolution.isPresent() && !resolution.orElseThrow().resolved()) {
                    if (LOGGED_UNRESOLVED_TRAIT_SOURCES.add(decisionKey)) {
                        SilentTinkersMod.LOGGER.warn(
                                "[SilentTinkers:TRAIT_SOURCE_UNRESOLVED] composite={} source={} percent={} access={} resolution={} candidates={} -- trait forwarding refused",
                                composition.fingerprint(), decision.sourceMaterialId(), decision.materialPercent(),
                                decision.access(), resolution.orElseThrow().status(),
                                resolution.orElseThrow().candidates());
                    }
                } else {
                    // Registry-provided material traits retain their declared ModifierEntry levels.
                    // Composition-derived levels apply to Silent Gear adapters below, so this path
                    // does not flatten legitimate addon modifier semantics.
                    decision.forwardedTraits().forEach(builder::add);
                }

                for (TraitAdapterPlan.Decision adapter : TraitAdapterPlan.create(
                        MaterialDiscoveryState.silentGearTraits(decision.sourceMaterialId()),
                        decision.contributionLevel(), target ->
                                new ModifierEntry(new ModifierId(target), 1).isBound())) {
                    String adapterKey = decisionKey + "|" + adapter.sourceTrait();
                    if (adapter.status() != TraitAdapterPlan.Status.SUPPORTED) {
                        if ((adapter.status() == TraitAdapterPlan.Status.UNSUPPORTED
                                || adapter.status() == TraitAdapterPlan.Status.TARGET_UNAVAILABLE)
                                && LOGGED_TRAIT_ADAPTERS.add(adapterKey)) {
                            SilentTinkersMod.LOGGER.info(
                                    "[SilentTinkers:TRAIT_FORWARD_UNSUPPORTED] composite={} source={} trait={} kind={} percent={} level={} status={} reason={} -- material remains usable",
                                    composition.fingerprint(), decision.sourceMaterialId(), adapter.sourceTrait(),
                                    adapter.kind(), decision.materialPercent(), adapter.contributionLevel(),
                                    adapter.status(), adapter.detail());
                        }
                        continue;
                    }

                    ModifierEntry adapted = new ModifierEntry(
                            new ModifierId(adapter.targetModifier().orElseThrow()), adapter.contributionLevel());
                    if (allNativeModifierIds.contains(adapted.getId())) {
                        if (LOGGED_TRAIT_ADAPTERS.add(adapterKey)) {
                            SilentTinkersMod.LOGGER.info(
                                    "[SilentTinkers:TRAIT_ADAPTER_DUPLICATE] composite={} source={} trait={} target={} -- native material trait retained without duplicate application",
                                    composition.fingerprint(), decision.sourceMaterialId(), adapter.sourceTrait(), adapted.getId());
                        }
                        continue;
                    }
                    if (!adapted.isBound()) {
                        if (LOGGED_TRAIT_ADAPTERS.add(adapterKey)) {
                            SilentTinkersMod.LOGGER.warn(
                                    "[SilentTinkers:TRAIT_ADAPTER_UNAVAILABLE] composite={} source={} trait={} target={} -- material remains usable",
                                    composition.fingerprint(), decision.sourceMaterialId(), adapter.sourceTrait(), adapted.getId());
                        }
                        continue;
                    }
                    supportedAdapters.add(adapter);
                    if (LOGGED_TRAIT_ADAPTERS.add(adapterKey)) {
                        SilentTinkersMod.LOGGER.info(
                                "[SilentTinkers:TRAIT_FORWARD_{}] composite={} source={} trait={} target={} percent={} level={}",
                                adapter.kind() == TraitAdapterPlan.Kind.DIRECT ? "DIRECT" : "ADAPTER",
                                composition.fingerprint(), decision.sourceMaterialId(), adapter.sourceTrait(), adapted.getId(),
                                decision.materialPercent(), adapter.contributionLevel());
                    }
                }

                if (LOGGED_TRAIT_DECISIONS.add(decisionKey)) {
                    String resolved = resolution.flatMap(TraitSourceResolver.Resolution::materialId)
                            .map(Object::toString).orElse("NOT_ELIGIBLE");
                    String resolutionMode = resolution.map(value -> value.status().name())
                            .orElse("NOT_ELIGIBLE");
                    SilentTinkersMod.LOGGER.info(
                            "[SilentTinkers:TRAIT_FORWARD] composite={} source={} resolved={} resolution={} percent={} access={} level={} available={} forwarded={}",
                            composition.fingerprint(), decision.sourceMaterialId(), resolved, resolutionMode,
                            decision.materialPercent(), decision.access(), decision.contributionLevel(), decision.availableTraitCount(),
                            decision.forwardedTraits().size());
                }
            }

            TraitAdapterPlan.applications(supportedAdapters,
                            new java.util.ArrayList<net.minecraft.resources.ResourceLocation>(allNativeModifierIds))
                    .forEach((target, level) -> builder.add(new ModifierEntry(new ModifierId(target), level)));
        }
    }

    @Override
    public void addToolStats(IToolContext context, ModifierEntry modifier, ModifierStatsBuilder builder) {
        boolean foundComposite = false;
        for (MaterialVariant material : context.getMaterials()) {
            if (!material.getVariant().getId().equals(CompositePickHeadCastingRecipe.MATERIAL)) {
                continue;
            }
            foundComposite = true;
            String variant = material.getVariant().getVariant();
            if (!AlloyVariantCodec.isEncodedVariant(variant)) {
                continue;
            }
            Optional<AlloyStatSnapshot> decoded;
            try {
                decoded = AlloyVariantCodec.decodeStats(variant);
            } catch (IllegalArgumentException exception) {
                SilentTinkersMod.LOGGER.warn("[SilentTinkers:COMPOSITE_STATS_DECODE_FAILED] variant={}", material.getVariant(), exception);
                continue;
            }
            if (decoded.isEmpty()) {
                String variantKey = material.getVariant().toString();
                if (LOGGED_MISSING_STAT_VARIANTS.add(variantKey)) {
                    SilentTinkersMod.LOGGER.warn("[SilentTinkers:COMPOSITE_STATS_MISSING] variant={} -- composite material reached tool construction without encoded stats", material.getVariant());
                }
                continue;
            }
            AlloyStatSnapshot stats = decoded.orElseThrow();
            apply(stats, builder);
            RuntimeBridgeHealth.markCompositeStatsApplied();
            String variantKey = material.getVariant().toString();
            if (LOGGED_STAT_VARIANTS.add(variantKey)) {
                SilentTinkersMod.LOGGER.info("[SilentTinkers:COMPOSITE_STATS_APPLIED] variant={} durability={} miningSpeed={} meleeDamage={} attackSpeed={} tier={}",
                        material.getVariant(), stats.durability(), stats.miningSpeed(), stats.meleeDamage(), stats.attackSpeed(), stats.harvestTier());
            }
        }

        if (!foundComposite) {
            String materials = context.getMaterials().getList().stream()
                    .map(material -> material.getVariant().toString())
                    .collect(Collectors.joining(","));
            if (LOGGED_CONTEXTS_WITHOUT_COMPOSITE.add(materials)) {
                SilentTinkersMod.LOGGER.warn("[SilentTinkers:COMPOSITE_MODIFIER_WITHOUT_MATERIAL] materials=[{}] -- modifier ran but composite material variant is absent", materials);
            }
        }
    }

    private static void apply(AlloyStatSnapshot stats, ModifierStatsBuilder builder) {
        ToolStats.DURABILITY.add(builder, stats.durability() - PLACEHOLDER_DURABILITY);
        ToolStats.MINING_SPEED.add(builder, stats.miningSpeed() - PLACEHOLDER_MINING_SPEED);
        ToolStats.ATTACK_DAMAGE.add(builder, stats.meleeDamage() - PLACEHOLDER_MELEE_DAMAGE);
        ToolStats.ATTACK_SPEED.add(builder, stats.attackSpeed());

        Tier tier = TierSortingRegistry.byName(stats.harvestTier());
        if (tier != null) {
            ToolStats.HARVEST_TIER.update(builder, tier);
        } else if (LOGGED_UNKNOWN_TIERS.add(stats.harvestTier())) {
            SilentTinkersMod.LOGGER.warn("[SilentTinkers:COMPOSITE_TIER_UNKNOWN] tier={} -- falling back to the tool's existing harvest tier", stats.harvestTier());
        }
    }
}
