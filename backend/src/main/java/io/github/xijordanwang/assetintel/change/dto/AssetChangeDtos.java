package io.github.xijordanwang.assetintel.change.dto;

import io.github.xijordanwang.assetintel.change.entity.AssetChange;
import io.github.xijordanwang.assetintel.change.entity.AssetChangeStep;
import io.github.xijordanwang.assetintel.change.entity.ChangeStatus;
import io.github.xijordanwang.assetintel.change.entity.ChangeType;
import io.github.xijordanwang.assetintel.change.entity.DisposalReason;
import io.github.xijordanwang.assetintel.approval.entity.ApproverRole;
import io.github.xijordanwang.assetintel.approval.entity.Decision;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class AssetChangeDtos {

    private AssetChangeDtos() {
    }

    public record CreateChange(
            @NotNull ChangeType type,
            @NotBlank String assetTag,
            String requestedBy, // ignored: the signed-in user is the requester
            @Size(max = 1000) String reason,
            String targetSiteCode,
            DisposalReason disposalReason,
            @Size(max = 200) String newName,
            @Size(max = 80) String newCustodian,
            @Size(max = 120) String newSupplier) {
    }

    /** Old and new master-data values; only set for MODIFICATION. */
    public record FieldChange(String field, String oldValue, String newValue) {
    }

    public record CancelChange(String requestedBy) { // ignored: the signed-in user cancels
    }

    public record StepView(int order, ApproverRole role, Decision decision, String decidedBy,
                           String comment, Instant decidedAt) {
        public static StepView of(AssetChangeStep s) {
            return new StepView(s.getStepOrder(), s.getApproverRole(), s.getDecision(), s.getDecidedBy(),
                    s.getComment(), s.getDecidedAt());
        }
    }

    public record ChangeView(String reference, ChangeType type, ChangeStatus status, String assetTag, String assetName,
                             String fromSiteCode, String targetSiteCode, DisposalReason disposalReason,
                             BigDecimal netBookValue, String requestedBy, String reason, ApproverRole waitingOn,
                             Instant createdAt, Instant updatedAt, List<StepView> steps,
                             List<FieldChange> fieldChanges) {

        public static ChangeView of(AssetChange c, boolean withSteps) {
            return new ChangeView(c.getReference(), c.getType(), c.getStatus(), c.getAsset().getTag(),
                    c.getAsset().getName(), c.getAsset().getSite().getCode(),
                    c.getTargetSite() != null ? c.getTargetSite().getCode() : null,
                    c.getDisposalReason(), c.getNetBookValue(), c.getRequestedBy(), c.getReason(),
                    c.getStatus() == ChangeStatus.PENDING_APPROVAL
                            ? c.nextPendingStep().map(AssetChangeStep::getApproverRole).orElse(null) : null,
                    c.getCreatedAt(), c.getUpdatedAt(),
                    withSteps ? c.getSteps().stream().map(StepView::of).toList() : List.of(),
                    fieldChanges(c));
        }

        private static List<FieldChange> fieldChanges(AssetChange c) {
            if (c.getType() != ChangeType.MODIFICATION) {
                return List.of();
            }
            boolean submitted = c.getStatus() != ChangeStatus.DRAFT;
            var out = new java.util.ArrayList<FieldChange>();
            if (c.getNewName() != null) {
                out.add(new FieldChange("name", submitted ? c.getOldName() : c.getAsset().getName(), c.getNewName()));
            }
            if (c.getNewCustodian() != null) {
                out.add(new FieldChange("custodian", submitted ? c.getOldCustodian() : c.getAsset().getCustodian(), c.getNewCustodian()));
            }
            if (c.getNewSupplier() != null) {
                out.add(new FieldChange("supplier", submitted ? c.getOldSupplier() : c.getAsset().getSupplier(), c.getNewSupplier()));
            }
            return out;
        }
    }
}
