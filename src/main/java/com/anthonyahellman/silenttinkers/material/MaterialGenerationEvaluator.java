package com.anthonyahellman.silenttinkers.material;

import java.util.Optional;

/** Resolves generation requests into safe, auditable pre-mutation states. */
public final class MaterialGenerationEvaluator {
    private MaterialGenerationEvaluator() {}

    public static MaterialGenerationEvaluation evaluate(MaterialGenerationRequest request) {
        if (request.action() == MaterialBridgePlan.Action.PRESERVE) {
            return new MaterialGenerationEvaluation(request,
                    MaterialGenerationEvaluation.Status.PRESERVED,
                    Optional.empty(), Optional.empty(), "native compatibility already present");
        }

        if (request.action() == MaterialBridgePlan.Action.BOOTSTRAP) {
            return new MaterialGenerationEvaluation(request,
                    MaterialGenerationEvaluation.Status.BOOTSTRAP_PENDING,
                    Optional.empty(), Optional.empty(), "external bootstrap conversion not yet implemented");
        }

        MaterialProfile.Ecosystem source = request.source().orElse(null);
        if (source == null) {
            return quarantined(request, "bridge request has no source ecosystem");
        }

        if (source == MaterialProfile.Ecosystem.SILENT_GEAR) {
            Optional<TranslatedMaterialStats> translated = MaterialStatTranslator.translate(request);
            if (translated.isEmpty()) return quarantined(request, "Silent Gear source stats could not be translated");
            HeadMaterialEligibility.Result eligibility = HeadMaterialEligibility.evaluate(translated.orElseThrow());
            if (!eligibility.eligible()) {
                return quarantined(request, "Silent Gear source is not eligible as a Tinkers head: "
                        + eligibility.detail());
            }
            return new MaterialGenerationEvaluation(request,
                    MaterialGenerationEvaluation.Status.READY_FOR_TINKERS,
                    translated, Optional.empty(), "Silent Gear source evaluated and translated");
        }

        var tinkers = MaterialStatTranslator.readNativeTinkers(request);
        if (tinkers.isEmpty()) return quarantined(request, "Tinkers source stats could not be read");
        return new MaterialGenerationEvaluation(request,
                MaterialGenerationEvaluation.Status.TINKERS_SOURCE_READY,
                Optional.empty(), tinkers, "Tinkers source captured; reverse semantic conversion pending");
    }

    private static MaterialGenerationEvaluation quarantined(MaterialGenerationRequest request, String detail) {
        return new MaterialGenerationEvaluation(request,
                MaterialGenerationEvaluation.Status.QUARANTINED,
                Optional.empty(), Optional.empty(), detail);
    }
}
