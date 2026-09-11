package com.anthonyahellman.silenttinkers.command;

import com.anthonyahellman.silenttinkers.config.SilentTinkersConfig;
import com.anthonyahellman.silenttinkers.material.CompositeBridgeHealth;
import com.anthonyahellman.silenttinkers.material.MaterialDiscoveryState;
import com.anthonyahellman.silenttinkers.material.MaterialGenerationEvaluation;
import com.anthonyahellman.silenttinkers.material.MaterialPlanFingerprint;
import com.anthonyahellman.silenttinkers.material.RuntimeBridgeHealth;
import com.anthonyahellman.silenttinkers.material.StarChargeBridgeHealth;
import com.anthonyahellman.silenttinkers.material.TranslatedMaterialStats;
import com.anthonyahellman.silenttinkers.material.TraitAdapterPlan;
import com.anthonyahellman.silenttinkers.material.UnifiedMaterialDiscovery;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;

import java.util.List;

/** Small operator-facing diagnostics so server owners do not need to read logs for basic bridge health. */
public final class SilentTinkersCommands {
    private SilentTinkersCommands() {}

    public static void register(RegisterCommandsEvent event) { register(event.getDispatcher()); }

    static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("silenttinkers")
                .then(Commands.literal("status").executes(context -> showStatus(context.getSource())))
                .then(Commands.literal("inspect").executes(context -> inspectHeldItem(context.getSource()))));
    }

    private static int showStatus(CommandSourceStack source) {
        UnifiedMaterialDiscovery.Snapshot snapshot = MaterialDiscoveryState.current().orElse(null);
        if (snapshot == null) {
            source.sendFailure(Component.literal("SilentTinkers: no completed material scan is available."));
            return 0;
        }

        long preserved = count(snapshot, MaterialGenerationEvaluation.Status.PRESERVED);
        long readyForTinkers = count(snapshot, MaterialGenerationEvaluation.Status.READY_FOR_TINKERS);
        long tinkersSourceReady = count(snapshot, MaterialGenerationEvaluation.Status.TINKERS_SOURCE_READY);
        long roleLimited = count(snapshot, MaterialGenerationEvaluation.Status.ROLE_LIMITED);
        long bootstrapPending = count(snapshot, MaterialGenerationEvaluation.Status.BOOTSTRAP_PENDING);
        long quarantined = count(snapshot, MaterialGenerationEvaluation.Status.QUARANTINED);
        int runtimeReady = MaterialDiscoveryState.readyForTinkersCount();
        long deferred = Math.max(0L, readyForTinkers - runtimeReady);
        String fingerprint = MaterialPlanFingerprint.of(snapshot);
        var traitThresholds = SilentTinkersConfig.traitThresholds();

        source.sendSuccess(() -> Component.literal(
                "SilentTinkers plan " + fingerprint
                        + " | SG " + snapshot.silentGear().materials()
                        + " | TCon " + snapshot.tinkers().materials()
                        + " | correlated aliases " + snapshot.correlatedPhysicalItems()), false);
        source.sendSuccess(() -> Component.literal(
                "Preserved " + preserved
                        + " | bridge→TCon " + readyForTinkers
                        + " | runtime-ready " + runtimeReady
                        + " | deferred forms " + deferred), false);
        source.sendSuccess(() -> Component.literal(
                "TCon→SG ready " + tinkersSourceReady
                        + " | role-limited " + roleLimited
                        + " | bootstrap pending " + bootstrapPending
                        + " | quarantined " + quarantined), false);
        source.sendSuccess(() -> Component.literal(
                "Trait gates primary " + traitThresholds.primaryPercent() + "%"
                        + " | secondary " + traitThresholds.secondaryPercent() + "%"
                        + " | full " + traitThresholds.fullPercent() + "%"), false);
        source.sendSuccess(() -> Component.literal(
                "SG trait catalog: " + snapshot.silentGear().traitBearingMaterials()
                        + " materials | " + snapshot.silentGear().traitReferences() + " trait references"), false);
        source.sendSuccess(() -> Component.literal(
                "Composite hook " + CompositeBridgeHealth.status()
                        + " | assembled-tool stats "
                        + (RuntimeBridgeHealth.compositeStatsApplied() ? "VALIDATED THIS SESSION" : "NOT YET OBSERVED")), false);
        source.sendSuccess(() -> Component.literal(
                "Native starlight charger bridge " + StarChargeBridgeHealth.status()), false);
        return 1;
    }

    private static int inspectHeldItem(CommandSourceStack source) throws CommandSyntaxException {
        UnifiedMaterialDiscovery.Snapshot snapshot = MaterialDiscoveryState.current().orElse(null);
        if (snapshot == null) {
            source.sendFailure(Component.literal("SilentTinkers: no completed material scan is available."));
            return 0;
        }

        ServerPlayer player = source.getPlayerOrException();
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            source.sendFailure(Component.literal("SilentTinkers: hold a material item in your main hand first."));
            return 0;
        }

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        List<MaterialGenerationEvaluation> matches = snapshot.evaluations().stream()
                .filter(evaluation -> evaluation.request().physicalItem().equals(itemId))
                .toList();
        if (matches.isEmpty()) {
            source.sendFailure(Component.literal("SilentTinkers: " + itemId + " is not part of the current bridge plan."));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("SilentTinkers inspect: " + itemId), false);
        for (MaterialGenerationEvaluation evaluation : matches) {
            var request = evaluation.request();
            source.sendSuccess(() -> Component.literal(
                    "Status " + evaluation.status()
                            + " | action " + request.action()
                            + " | source " + request.source().map(Enum::name).orElse("NONE")
                            + " | material " + request.sourceMaterialId().map(Object::toString).orElse("NONE")
                            + " | target " + request.target().map(Enum::name).orElse("BOTH")), false);
            if (!evaluation.detail().isBlank()) source.sendSuccess(() -> Component.literal("Detail: " + evaluation.detail()), false);
            evaluation.translatedStats().ifPresent(stats -> sendTranslatedStats(source, stats));
            request.sourceMaterialId().ifPresent(materialId -> {
                List<ResourceLocation> traits = MaterialDiscoveryState.silentGearTraits(materialId);
                if (!traits.isEmpty()) {
                    source.sendSuccess(() -> Component.literal("Silent Gear traits: " + traits), false);
                    List<TraitAdapterPlan.Decision> adapters = TraitAdapterPlan.create(traits, 4);
                    source.sendSuccess(() -> Component.literal("100% trait adapters: " + adapters), false);
                }
            });
        }

        boolean runtimeReady = MaterialDiscoveryState.readyForTinkers(itemId).isPresent();
        source.sendSuccess(() -> Component.literal(
                "Dynamic smeltery bridge: " + (runtimeReady ? "READY" : "NOT ACTIVE FOR THIS PHYSICAL FORM")), false);
        return 1;
    }

    private static void sendTranslatedStats(CommandSourceStack source, TranslatedMaterialStats stats) {
        source.sendSuccess(() -> Component.literal(
                "Translated head stats: durability " + stats.durability()
                        + " | mining speed " + stats.miningSpeed()
                        + " | melee damage " + stats.meleeDamage()
                        + " | attack speed " + stats.attackSpeed()
                        + " | tier " + stats.harvestTier()), false);
    }

    private static long count(UnifiedMaterialDiscovery.Snapshot snapshot, MaterialGenerationEvaluation.Status status) {
        return snapshot.evaluations().stream().filter(evaluation -> evaluation.status() == status).count();
    }
}
