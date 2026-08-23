package com.anthonyahellman.silenttinkers.command;

import com.anthonyahellman.silenttinkers.material.MaterialDiscoveryState;
import com.anthonyahellman.silenttinkers.material.MaterialGenerationEvaluation;
import com.anthonyahellman.silenttinkers.material.UnifiedMaterialDiscovery;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;

/** Small operator-facing diagnostics so server owners do not need to read logs for basic bridge health. */
public final class SilentTinkersCommands {
    private SilentTinkersCommands() {}

    public static void register(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("silenttinkers")
                .then(Commands.literal("status")
                        .executes(context -> showStatus(context.getSource()))));
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
        long bootstrapPending = count(snapshot, MaterialGenerationEvaluation.Status.BOOTSTRAP_PENDING);
        long quarantined = count(snapshot, MaterialGenerationEvaluation.Status.QUARANTINED);
        int runtimeReady = MaterialDiscoveryState.readyForTinkersCount();
        long deferred = Math.max(0L, readyForTinkers - runtimeReady);

        source.sendSuccess(() -> Component.literal(
                "SilentTinkers: SG " + snapshot.silentGear().materials()
                        + " | TCon " + snapshot.tinkers().materials()
                        + " | correlated aliases " + snapshot.correlatedPhysicalItems()), false);
        source.sendSuccess(() -> Component.literal(
                "Preserved " + preserved
                        + " | bridge→TCon " + readyForTinkers
                        + " | runtime-ready " + runtimeReady
                        + " | deferred forms " + deferred), false);
        source.sendSuccess(() -> Component.literal(
                "TCon→SG ready " + tinkersSourceReady
                        + " | bootstrap pending " + bootstrapPending
                        + " | quarantined " + quarantined), false);
        return 1;
    }

    private static long count(UnifiedMaterialDiscovery.Snapshot snapshot,
                              MaterialGenerationEvaluation.Status status) {
        return snapshot.evaluations().stream().filter(evaluation -> evaluation.status() == status).count();
    }
}
