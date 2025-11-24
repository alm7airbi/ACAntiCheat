package com.yourcompany.uac.storage;

import java.util.List;
import java.util.Map;

public record PlayerSnapshot(double trustScore, Map<String, Integer> flagCounts, List<String> mitigationHistory) {
}
