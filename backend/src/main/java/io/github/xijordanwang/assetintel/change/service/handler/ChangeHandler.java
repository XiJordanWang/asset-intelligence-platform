package io.github.xijordanwang.assetintel.change.service.handler;

import io.github.xijordanwang.assetintel.change.entity.AssetChange;
import io.github.xijordanwang.assetintel.change.entity.ChangeType;
import io.github.xijordanwang.assetintel.approval.entity.ApproverRole;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Everything that differs between change types. The service picks the handler by type, so adding a
 * new form type (for example a split or a master-data change) means adding one class.
 */
public interface ChangeHandler {

    public ChangeType type();

    /** Checks the request makes sense before it is saved. */
    public void validate(AssetChange change);

    /** Who must approve, in order. */
    public List<ApproverRole> route(AssetChange change, BigDecimal netBookValue);

    /** Applies the change to the asset register after final approval. */
    public void apply(AssetChange change, LocalDate today);
}
