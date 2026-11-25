package io.github.xijordanwang.assetintel.change.service.handler;

import io.github.xijordanwang.assetintel.change.entity.AssetChange;
import io.github.xijordanwang.assetintel.change.entity.ChangeType;
import io.github.xijordanwang.assetintel.change.entity.DisposalReason;
import io.github.xijordanwang.assetintel.approval.entity.ApproverRole;
import io.github.xijordanwang.assetintel.common.exception.BusinessRuleException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Retires an asset. The higher its net book value (the write-off), the more senior the approval.
 * Lost assets always need finance, whatever their value. Thresholds are illustrative.
 */
@Component
public class DisposalHandler implements ChangeHandler {

    static final BigDecimal PLANT_MANAGER_LIMIT = new BigDecimal("10000");
    static final BigDecimal FINANCE_CONTROLLER_LIMIT = new BigDecimal("250000");

    @Override
    public ChangeType type() {
        return ChangeType.DISPOSAL;
    }

    @Override
    public void validate(AssetChange change) {
        if (change.getDisposalReason() == null) {
            throw new BusinessRuleException("A disposal needs a reason");
        }
    }

    @Override
    public List<ApproverRole> route(AssetChange change, BigDecimal netBookValue) {
        List<ApproverRole> route = new ArrayList<>(List.of(ApproverRole.PLANT_MANAGER));
        boolean overPlantLimit = netBookValue.compareTo(PLANT_MANAGER_LIMIT) > 0;
        if (overPlantLimit || change.getDisposalReason() == DisposalReason.LOST) {
            route.add(ApproverRole.FINANCE_CONTROLLER);
        }
        if (netBookValue.compareTo(FINANCE_CONTROLLER_LIMIT) > 0) {
            route.add(ApproverRole.CFO);
        }
        return route;
    }

    @Override
    public void apply(AssetChange change, LocalDate today) {
        change.getAsset().dispose(today);
    }
}
