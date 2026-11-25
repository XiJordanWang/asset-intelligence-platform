package io.github.xijordanwang.assetintel.approval.dto;

import io.github.xijordanwang.assetintel.approval.entity.ApprovalStep;
import io.github.xijordanwang.assetintel.approval.entity.ApproverRole;
import io.github.xijordanwang.assetintel.approval.entity.Decision;
import io.github.xijordanwang.assetintel.approval.entity.InvestmentRequest;
import io.github.xijordanwang.assetintel.approval.entity.RequestStatus;
import io.github.xijordanwang.assetintel.asset.entity.AssetCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class ApprovalDtos {

    private ApprovalDtos() {
    }

    public record CreateRequest(
            @NotBlank @Size(max = 200) String title,
            @Size(max = 4000) String justification,
            @NotNull AssetCategory category,
            @NotNull @DecimalMin("1.00") BigDecimal amount,
            @NotBlank String siteCode,
            String requestedBy) { // ignored: the signed-in user is the requester
    }

    public record DecisionRequest(
            ApproverRole role,      // ignored: the step waiting for a decision decides the role
            String decidedBy,       // ignored: the signed-in user decides
            @Size(max = 1000) String comment) {
    }

    public record StepView(int order, ApproverRole role, Decision decision, String decidedBy,
                           String comment, Instant decidedAt) {
        public static StepView of(ApprovalStep s) {
            return new StepView(s.getStepOrder(), s.getApproverRole(), s.getDecision(), s.getDecidedBy(),
                    s.getComment(), s.getDecidedAt());
        }
    }

    public record RequestView(String reference, String title, String justification, AssetCategory category,
                              BigDecimal amount, String siteCode, String requestedBy, RequestStatus status,
                              ApproverRole waitingOn, String capitalisedAssetTag,
                              Instant createdAt, Instant updatedAt, List<StepView> steps) {
        public static RequestView of(InvestmentRequest r) {
            return new RequestView(r.getReference(), r.getTitle(), r.getJustification(), r.getCategory(),
                    r.getAmount(), r.getSite().getCode(), r.getRequestedBy(), r.getStatus(),
                    r.getStatus() == RequestStatus.PENDING_APPROVAL
                            ? r.nextPendingStep().map(ApprovalStep::getApproverRole).orElse(null) : null,
                    r.getCapitalisedAsset() != null ? r.getCapitalisedAsset().getTag() : null,
                    r.getCreatedAt(), r.getUpdatedAt(),
                    r.getSteps().stream().map(StepView::of).toList());
        }

        public static RequestView summary(InvestmentRequest r) {
            return new RequestView(r.getReference(), r.getTitle(), null, r.getCategory(), r.getAmount(),
                    r.getSite().getCode(), r.getRequestedBy(), r.getStatus(),
                    r.getStatus() == RequestStatus.PENDING_APPROVAL
                            ? r.nextPendingStep().map(ApprovalStep::getApproverRole).orElse(null) : null,
                    null, r.getCreatedAt(), r.getUpdatedAt(), List.of());
        }
    }
}
