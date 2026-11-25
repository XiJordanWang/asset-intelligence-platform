package io.github.xijordanwang.assetintel;

import io.github.xijordanwang.assetintel.approval.service.ApprovalPolicy;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static io.github.xijordanwang.assetintel.approval.entity.ApproverRole.*;
import static org.assertj.core.api.Assertions.assertThat;

class ApprovalPolicyTest {

    @Test
    void approversGrowWithAmount() {
        assertThat(ApprovalPolicy.requiredApprovers(new BigDecimal("50000"))).containsExactly(PLANT_MANAGER);
        assertThat(ApprovalPolicy.requiredApprovers(new BigDecimal("50000.01")))
                .containsExactly(PLANT_MANAGER, FINANCE_CONTROLLER);
        assertThat(ApprovalPolicy.requiredApprovers(new BigDecimal("750000")))
                .containsExactly(PLANT_MANAGER, FINANCE_CONTROLLER, CFO);
    }
}
