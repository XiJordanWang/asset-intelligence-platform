package io.github.xijordanwang.assetintel.change.service.handler;

import io.github.xijordanwang.assetintel.change.entity.AssetChange;
import io.github.xijordanwang.assetintel.change.entity.ChangeType;
import io.github.xijordanwang.assetintel.approval.entity.ApproverRole;
import io.github.xijordanwang.assetintel.common.exception.BusinessRuleException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Moves an asset to another site. A move between plants also needs the receiving plant and finance. */
@Component
public class TransferHandler implements ChangeHandler {

    @Override
    public ChangeType type() {
        return ChangeType.TRANSFER;
    }

    @Override
    public void validate(AssetChange change) {
        if (change.getTargetSite() == null) {
            throw new BusinessRuleException("A transfer needs a target site");
        }
        if (change.getTargetSite().getId().equals(change.getAsset().getSite().getId())) {
            throw new BusinessRuleException(change.getAsset().getTag() + " is already at " + change.getTargetSite().getCode());
        }
    }

    @Override
    public List<ApproverRole> route(AssetChange change, BigDecimal netBookValue) {
        boolean samePlant = change.getTargetSite().getPlantCode().equals(change.getAsset().getSite().getPlantCode());
        return samePlant
                ? List.of(ApproverRole.PLANT_MANAGER)
                : List.of(ApproverRole.PLANT_MANAGER, ApproverRole.RECEIVING_PLANT_MANAGER, ApproverRole.FINANCE_CONTROLLER);
    }

    @Override
    public void apply(AssetChange change, LocalDate today) {
        change.getAsset().moveTo(change.getTargetSite());
    }
}
