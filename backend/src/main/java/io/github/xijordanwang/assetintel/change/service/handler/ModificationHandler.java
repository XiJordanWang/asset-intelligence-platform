package io.github.xijordanwang.assetintel.change.service.handler;

import io.github.xijordanwang.assetintel.change.entity.AssetChange;
import io.github.xijordanwang.assetintel.change.entity.ChangeType;
import io.github.xijordanwang.assetintel.approval.entity.ApproverRole;
import io.github.xijordanwang.assetintel.common.exception.BusinessRuleException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/** Corrects master data. The plant manager confirms the facts, then finance checks the register entry. */
@Component
public class ModificationHandler implements ChangeHandler {

    @Override
    public ChangeType type() {
        return ChangeType.MODIFICATION;
    }

    @Override
    public void validate(AssetChange change) {
        var asset = change.getAsset();
        boolean changesSomething =
                (change.getNewName() != null && !change.getNewName().equals(asset.getName()))
                || (change.getNewCustodian() != null && !Objects.equals(change.getNewCustodian(), asset.getCustodian()))
                || (change.getNewSupplier() != null && !Objects.equals(change.getNewSupplier(), asset.getSupplier()));
        if (!changesSomething) {
            throw new BusinessRuleException("A master-data change must change at least one value");
        }
    }

    @Override
    public List<ApproverRole> route(AssetChange change, BigDecimal netBookValue) {
        return List.of(ApproverRole.PLANT_MANAGER, ApproverRole.FINANCE_CONTROLLER);
    }

    @Override
    public void apply(AssetChange change, LocalDate today) {
        change.getAsset().updateMasterData(change.getNewName(), change.getNewCustodian(), change.getNewSupplier());
    }
}
