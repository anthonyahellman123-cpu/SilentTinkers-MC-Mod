package com.anthonyahellman.silenttinkers.material;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HexFormat;

/**
 * Stable short fingerprint of a completed bridge plan. It is deliberately based
 * on logical evaluation identities and resolved stats rather than object hash
 * codes, making it useful in server logs when comparing restarts, configs, or
 * modpack changes.
 */
public final class MaterialPlanFingerprint {
    private static final int DISPLAY_HEX_LENGTH = 16;

    private MaterialPlanFingerprint() {}

    public static String of(UnifiedMaterialDiscovery.Snapshot snapshot) {
        return ofEvaluations(snapshot.evaluations());
    }

    static String ofEvaluations(Collection<MaterialGenerationEvaluation> evaluations) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            evaluations.stream()
                    .map(MaterialPlanFingerprint::canonicalLine)
                    .sorted()
                    .forEach(line -> digest.update((line + "\n").getBytes(StandardCharsets.UTF_8)));
            String full = HexFormat.of().formatHex(digest.digest());
            return full.substring(0, DISPLAY_HEX_LENGTH);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 must exist on Java 17", exception);
        }
    }

    private static String canonicalLine(MaterialGenerationEvaluation evaluation) {
        MaterialGenerationRequest request = evaluation.request();
        return request.physicalItem()
                + "|" + request.action()
                + "|" + request.source().map(Enum::name).orElse("NONE")
                + "|" + request.sourceMaterialId().map(Object::toString).orElse("NONE")
                + "|" + request.target().map(Enum::name).orElse("BOTH")
                + "|" + request.bootstrapProfile().map(Object::toString).orElse("NONE")
                + "|" + evaluation.status()
                + "|translated=" + evaluation.translatedStats().map(Object::toString).orElse("NONE")
                + "|tinkers=" + evaluation.tinkersSourceStats().map(Object::toString).orElse("NONE");
    }
}
