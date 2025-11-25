package com.yourcompany.uac.checks.context;

/**
 * Lightweight view of runtime conditions (ping/TPS/protocol) to help checks
 * avoid false positives during lag spikes or when older client versions behave
 * differently.
 */
public record EnvironmentSnapshot(double serverTps, int ping, int protocolVersion) {

    public boolean isLagging(double tpsFloor, int pingCeiling) {
        return serverTps < tpsFloor || ping > pingCeiling;
    }

    public double lagFactor(double baselineTps, int baselinePing) {
        double tpsFactor = baselineTps <= 0 ? 1.0 : Math.max(1.0, baselineTps / Math.max(serverTps, 1.0));
        double pingFactor = baselinePing <= 0 ? 1.0 : Math.max(1.0, ping / (double) baselinePing);
        return tpsFactor * pingFactor;
    }
}
