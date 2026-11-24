package io.github.xijordanwang.assetintel.approval;

import java.math.BigDecimal;
import java.util.List;

/**
 * Delegation of authority: which roles must approve an investment, in order, by amount (NZD).
 * Thresholds are illustrative and not taken from any real company.
 */
public final class ApprovalPolicy {

    public static final BigDecimal PLANT_MANAGER_LIMIT = new BigDecimal("50000");
    public static final BigDecimal FINANCE_CONTROLLER_LIMIT = new BigDecimal("500000");

    private ApprovalPolicy() {
    }

    public static List<ApproverRole> requiredApprovers(BigDecimal amount) {
        if (amount.compareTo(PLANT_MANAGER_LIMIT) <= 0) {
            return List.of(ApproverRole.PLANT_MANAGER);
        }
        if (amount.compareTo(FINANCE_CONTROLLER_LIMIT) <= 0) {
            return List.of(ApproverRole.PLANT_MANAGER, ApproverRole.FINANCE_CONTROLLER);
        }
        return List.of(ApproverRole.PLANT_MANAGER, ApproverRole.FINANCE_CONTROLLER, ApproverRole.CFO);
    }
}
