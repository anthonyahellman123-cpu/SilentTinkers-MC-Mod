package com.anthonyahellman.silenttinkers.material;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class PriorityMaterialAuditTest {
    @Test
    void priorityEcosystemsReceiveExplicitCoverageStates() {
        PriorityMaterialAudit.Report report = PriorityMaterialAudit.evaluate(List.of(
                evaluation("silentgear:iron", MaterialGenerationEvaluation.Status.READY_FOR_TINKERS),
                evaluation("silentcompat:elementium", MaterialGenerationEvaluation.Status.ROLE_LIMITED),
                evaluation("tinkers_advanced:antimony", MaterialGenerationEvaluation.Status.TINKERS_SOURCE_READY),
                evaluation("iceandfire:dragonsteel_fire", MaterialGenerationEvaluation.Status.QUARANTINED)));

        assertEquals(1, report.count(PriorityMaterialAudit.PriorityEcosystem.SILENT_GEAR_CORE,
                PriorityMaterialAudit.Coverage.SUPPORTED));
        assertEquals(1, report.count(PriorityMaterialAudit.PriorityEcosystem.SILENT_COMPAT,
                PriorityMaterialAudit.Coverage.ROLE_LIMITED));
        assertEquals(1, report.count(PriorityMaterialAudit.PriorityEcosystem.TINKERS_ADVANCED,
                PriorityMaterialAudit.Coverage.SUPPORTED_WITH_LIMITATIONS));
        assertEquals(1, report.count(PriorityMaterialAudit.PriorityEcosystem.ICE_AND_FIRE_DRAGONSTEEL,
                PriorityMaterialAudit.Coverage.QUARANTINED));
    }

    @Test
    void duplicatePhysicalAliasesCountOneLogicalMaterialAtMostRestrictiveState() {
        PriorityMaterialAudit.Report report = PriorityMaterialAudit.evaluate(List.of(
                evaluation("silentcompat:elementium", MaterialGenerationEvaluation.Status.READY_FOR_TINKERS),
                evaluation("silentcompat:elementium", MaterialGenerationEvaluation.Status.QUARANTINED)));

        assertEquals(1, report.total(PriorityMaterialAudit.PriorityEcosystem.SILENT_COMPAT));
        assertEquals(1, report.count(PriorityMaterialAudit.PriorityEcosystem.SILENT_COMPAT,
                PriorityMaterialAudit.Coverage.QUARANTINED));
    }

    @Test
    void silentCompatDragonsteelIsAuditedAsDragonsteelPriority() {
        PriorityMaterialAudit.Report report = PriorityMaterialAudit.evaluate(List.of(
                evaluation("silentcompat:dragonsteel_ice", MaterialGenerationEvaluation.Status.READY_FOR_TINKERS)));

        assertEquals(1, report.count(PriorityMaterialAudit.PriorityEcosystem.ICE_AND_FIRE_DRAGONSTEEL,
                PriorityMaterialAudit.Coverage.SUPPORTED));
        assertEquals(0, report.total(PriorityMaterialAudit.PriorityEcosystem.SILENT_COMPAT));
    }

    private static MaterialGenerationEvaluation evaluation(String material,
                                                            MaterialGenerationEvaluation.Status status) {
        ResourceLocation materialId = new ResourceLocation(material);
        MaterialGenerationRequest request = new MaterialGenerationRequest(
                new ResourceLocation(materialId.getNamespace(), materialId.getPath() + "_ingot"),
                MaterialBridgePlan.Action.BRIDGE,
                Optional.of(MaterialProfile.Ecosystem.SILENT_GEAR),
                Optional.of(MaterialProfile.Ecosystem.TINKERS_CONSTRUCT),
                Optional.of(materialId),
                Optional.of(GeneratedMaterialOwnership.idFor(MaterialProfile.Ecosystem.TINKERS_CONSTRUCT, materialId)),
                Optional.empty());
        return new MaterialGenerationEvaluation(request, status, Optional.empty(), Optional.empty(), "test");
    }
}
