package io.github.xijordanwang.assetintel.approval.entity;

import io.github.xijordanwang.assetintel.approval.service.ApprovalPolicy;
import io.github.xijordanwang.assetintel.asset.entity.Asset;
import io.github.xijordanwang.assetintel.asset.entity.AssetCategory;
import io.github.xijordanwang.assetintel.common.exception.BusinessRuleException;
import io.github.xijordanwang.assetintel.site.entity.Site;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A capital investment request moving through DRAFT -> PENDING_APPROVAL -> APPROVED -> CAPITALISED,
 * or ending in REJECTED. The state rules live here so they cannot be bypassed by a controller.
 */
@Entity
@Table(name = "investment_request")
public class InvestmentRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String reference;

    @Column(nullable = false)
    private String title;

    private String justification;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssetCategory category;

    @Column(nullable = false)
    private BigDecimal amount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "site_id")
    private Site site;

    @Column(name = "requested_by", nullable = false)
    private String requestedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status = RequestStatus.DRAFT;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "capitalised_asset_id")
    private Asset capitalisedAsset;

    @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stepOrder asc")
    private List<ApprovalStep> steps = new ArrayList<>();

    protected InvestmentRequest() {
    }

    public InvestmentRequest(String reference, String title, String justification, AssetCategory category,
                             BigDecimal amount, Site site, String requestedBy, Instant now) {
        this.reference = reference;
        this.title = title;
        this.justification = justification;
        this.category = category;
        this.amount = amount;
        this.site = site;
        this.requestedBy = requestedBy;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void submit(Instant now) {
        requireStatus(RequestStatus.DRAFT, "submit");
        List<ApproverRole> roles = ApprovalPolicy.requiredApprovers(amount);
        for (int i = 0; i < roles.size(); i++) {
            steps.add(new ApprovalStep(this, i + 1, roles.get(i)));
        }
        status = RequestStatus.PENDING_APPROVAL;
        updatedAt = now;
    }

    /** Records a decision for the next pending step. Approvals must happen in order. */
    public void decide(ApproverRole role, Decision decision, String decidedBy, String comment, Instant now) {
        requireStatus(RequestStatus.PENDING_APPROVAL, "decide on");
        if (decision == Decision.PENDING) {
            throw new BusinessRuleException("A decision must be APPROVED or REJECTED");
        }
        ApprovalStep next = nextPendingStep()
                .orElseThrow(() -> new BusinessRuleException("No approval step is waiting on " + reference));
        if (next.getApproverRole() != role) {
            throw new BusinessRuleException(reference + " is waiting on " + next.getApproverRole() + ", not " + role);
        }
        if (decidedBy.equalsIgnoreCase(requestedBy)) {
            throw new BusinessRuleException("The requester cannot approve their own request");
        }
        next.decide(decision, decidedBy, comment, now);
        if (decision == Decision.REJECTED) {
            status = RequestStatus.REJECTED;
        } else if (nextPendingStep().isEmpty()) {
            status = RequestStatus.APPROVED;
        }
        updatedAt = now;
    }

    public void capitalise(Asset asset, Instant now) {
        requireStatus(RequestStatus.APPROVED, "capitalise");
        this.capitalisedAsset = asset;
        status = RequestStatus.CAPITALISED;
        updatedAt = now;
    }

    public Optional<ApprovalStep> nextPendingStep() {
        return steps.stream().filter(s -> s.getDecision() == Decision.PENDING).findFirst();
    }

    private void requireStatus(RequestStatus expected, String action) {
        if (status != expected) {
            throw new BusinessRuleException("Cannot " + action + " " + reference + " while it is " + status);
        }
    }

    public Long getId() { return id; }
    public String getReference() { return reference; }
    public String getTitle() { return title; }
    public String getJustification() { return justification; }
    public AssetCategory getCategory() { return category; }
    public BigDecimal getAmount() { return amount; }
    public Site getSite() { return site; }
    public String getRequestedBy() { return requestedBy; }
    public RequestStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Asset getCapitalisedAsset() { return capitalisedAsset; }
    public List<ApprovalStep> getSteps() { return List.copyOf(steps); }
}
