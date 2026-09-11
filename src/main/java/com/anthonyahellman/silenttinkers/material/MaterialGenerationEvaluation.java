package com.anthonyahellman.silenttinkers.material;

import com.anthonyahellman.silenttinkers.compat.tconstruct.TinkersStatSnapshot;

import java.util.Objects;
import java.util.Optional;

/**
 * Auditable result between planning and mutation. Generators should consume
 * only READY results; diagnostics and the future planner UI can display every
 * other state without re-running source API reads.
 */
public record MaterialGenerationEvaluation(
        MaterialGenerationRequest request,
        Status status,
        Optional<TranslatedMaterialStats> translatedStats,
        Optional<TinkersStatSnapshot> tinkersSourceStats,
        String detail) {

    public MaterialGenerationEvaluation {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(status, "status");
        translatedStats = Objects.requireNonNull(translatedStats, "translatedStats");
        tinkersSourceStats = Objects.requireNonNull(tinkersSourceStats, "tinkersSourceStats");
        detail = Objects.requireNonNull(detail, "detail");
    }

    public boolean readyForMutation() {
        return status == Status.READY_FOR_TINKERS;
    }

    public enum Status {
        PRESERVED,
        READY_FOR_TINKERS,
        TINKERS_SOURCE_READY,
        ROLE_LIMITED,
        BOOTSTRAP_PENDING,
        QUARANTINED
    }
}
