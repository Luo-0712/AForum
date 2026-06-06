package com.example.aforum.content.service;

import java.math.BigDecimal;
import java.util.List;

public record AuditDecision(
        boolean approved,
        String decision,
        String riskLevel,
        BigDecimal riskScore,
        String reason,
        String suggestion,
        List<String> suggestedTags
) {
}
