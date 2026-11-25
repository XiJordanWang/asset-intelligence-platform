package io.github.xijordanwang.assetintel.change.entity;

import io.github.xijordanwang.assetintel.change.service.handler.ChangeHandler;
import io.github.xijordanwang.assetintel.approval.entity.ApproverRole;
import io.github.xijordanwang.assetintel.approval.entity.Decision;
import io.github.xijordanwang.assetintel.asset.entity.Asset;
import io.github.xijordanwang.assetintel.common.exception.BusinessRuleException;
import io.github.xijordanwang.assetintel.site.entity.Site;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A change to an existing asset (transfer, disposal or master-data modification). Every type shares one state machine:
 * DRAFT -> PENDING_APPROVAL -> APPROVED | REJECTED, and DRAFT or PENDING_APPROVAL -> CANCELLED.
 * What differs per type (validation, approval route, effect) lives in a {@link ChangeHandler}.
 */
@Entity
@Table(name = "asset_change")
public class AssetChange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String reference;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false)
    private ChangeType type;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id")
    private Asset asset;

    @Column(name = "requested_by", nullable = false)
    private String requestedBy;

    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChangeStatus status = ChangeStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_site_id")
    private Site targetSite;

    @Enumerated(EnumType.STRING)
    @Column(name = "disposal_reason")
    private DisposalReason disposalReason;

    @Column(name = "old_name") private String oldName;
    @Column(name = "new_name") private String newName;
    @Column(name = "old_custodian") private String oldCustodian;
    @Column(name = "new_custodian") private String newCustodian;
    @Column(name = "old_supplier") private String oldSupplier;
    @Column(name = "new_supplier") private String newSupplier;

    /** Net book value captured at submission; it decides the disposal approval route. */
    @Column(name = "net_book_value")
    private BigDecimal netBookValue;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Optimistic locking: two approvers clicking at once cannot both succeed. */
    @Version
    private long version;

    @OneToMany(mappedBy = "change", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stepOrder asc")
    private List<AssetChangeStep> steps = new ArrayList<>();

    protected AssetChange() {
    }

    public AssetChange(String reference, ChangeType type, Asset asset, String requestedBy, String reason,
                       Site targetSite, DisposalReason disposalReason, Instant now) {
        this.reference = reference;
        this.type = type;
        this.asset = asset;
        this.requestedBy = requestedBy;
        this.reason = reason;
        this.targetSite = targetSite;
        this.disposalReason = disposalReason;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** Proposed master-data values. Null means "leave unchanged". */
    public void propose(String newName, String newCustodian, String newSupplier) {
        this.newName = blankToNull(newName);
        this.newCustodian = blankToNull(newCustodian);
        this.newSupplier = blankToNull(newSupplier);
    }

    public void submit(List<ApproverRole> route, BigDecimal netBookValue, Instant now) {
        requireStatus(ChangeStatus.DRAFT, "submit");
        asset.lockFor(reference);
        if (type == ChangeType.MODIFICATION) {
            // Snapshot what the record looked like, so reviewers and auditors see old next to new.
            this.oldName = asset.getName();
            this.oldCustodian = asset.getCustodian();
            this.oldSupplier = asset.getSupplier();
        }
        for (int i = 0; i < route.size(); i++) {
            steps.add(new AssetChangeStep(this, i + 1, route.get(i)));
        }
        this.netBookValue = netBookValue;
        this.status = ChangeStatus.PENDING_APPROVAL;
        this.updatedAt = now;
    }

    /** Records the next step's decision and returns true when the change is now fully approved. */
    public boolean decide(ApproverRole role, Decision decision, String decidedBy, String comment, Instant now) {
        requireStatus(ChangeStatus.PENDING_APPROVAL, "decide on");
        if (decision == Decision.PENDING) {
            throw new BusinessRuleException("A decision must be APPROVED or REJECTED");
        }
        AssetChangeStep next = nextPendingStep()
                .orElseThrow(() -> new BusinessRuleException("No approval step is waiting on " + reference));
        if (next.getApproverRole() != role) {
            throw new BusinessRuleException(reference + " is waiting on " + next.getApproverRole() + ", not " + role);
        }
        if (decidedBy.equalsIgnoreCase(requestedBy)) {
            throw new BusinessRuleException("The requester cannot approve their own change");
        }
        next.decide(decision, decidedBy, comment, now);
        updatedAt = now;
        if (decision == Decision.REJECTED) {
            status = ChangeStatus.REJECTED;
            asset.unlock(reference);
            return false;
        }
        if (nextPendingStep().isEmpty()) {
            status = ChangeStatus.APPROVED;
            asset.unlock(reference);
            return true;
        }
        return false;
    }

    public void cancel(String by, Instant now) {
        if (!status.isOpen()) {
            throw new BusinessRuleException("Cannot cancel " + reference + " while it is " + status);
        }
        if (!by.equalsIgnoreCase(requestedBy)) {
            throw new BusinessRuleException("Only the requester can cancel " + reference);
        }
        status = ChangeStatus.CANCELLED;
        asset.unlock(reference);
        updatedAt = now;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    public Optional<AssetChangeStep> nextPendingStep() {
        return steps.stream().filter(s -> s.getDecision() == Decision.PENDING).findFirst();
    }

    private void requireStatus(ChangeStatus expected, String action) {
        if (status != expected) {
            throw new BusinessRuleException("Cannot " + action + " " + reference + " while it is " + status);
        }
    }

    public Long getId() { return id; }
    public String getReference() { return reference; }
    public ChangeType getType() { return type; }
    public Asset getAsset() { return asset; }
    public String getRequestedBy() { return requestedBy; }
    public String getReason() { return reason; }
    public ChangeStatus getStatus() { return status; }
    public Site getTargetSite() { return targetSite; }
    public DisposalReason getDisposalReason() { return disposalReason; }
    public BigDecimal getNetBookValue() { return netBookValue; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<AssetChangeStep> getSteps() { return List.copyOf(steps); }
    public String getOldName() { return oldName; }
    public String getNewName() { return newName; }
    public String getOldCustodian() { return oldCustodian; }
    public String getNewCustodian() { return newCustodian; }
    public String getOldSupplier() { return oldSupplier; }
    public String getNewSupplier() { return newSupplier; }
}
