package io.github.xijordanwang.assetintel.change.entity;

import io.github.xijordanwang.assetintel.approval.entity.ApproverRole;
import io.github.xijordanwang.assetintel.approval.entity.Decision;
import jakarta.persistence.*;

import java.time.Instant;

/** One approval step. Once decided it records who acted, what they did and when, giving an audit trail. */
@Entity
@Table(name = "asset_change_step")
public class AssetChangeStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "change_id")
    private AssetChange change;

    @Column(name = "step_order", nullable = false)
    private int stepOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "approver_role", nullable = false)
    private ApproverRole approverRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Decision decision = Decision.PENDING;

    @Column(name = "decided_by")
    private String decidedBy;

    private String comment;

    @Column(name = "decided_at")
    private Instant decidedAt;

    protected AssetChangeStep() {
    }

    AssetChangeStep(AssetChange change, int stepOrder, ApproverRole approverRole) {
        this.change = change;
        this.stepOrder = stepOrder;
        this.approverRole = approverRole;
    }

    public void decide(Decision decision, String decidedBy, String comment, Instant at) {
        this.decision = decision;
        this.decidedBy = decidedBy;
        this.comment = comment;
        this.decidedAt = at;
    }

    public int getStepOrder() { return stepOrder; }
    public ApproverRole getApproverRole() { return approverRole; }
    public Decision getDecision() { return decision; }
    public String getDecidedBy() { return decidedBy; }
    public String getComment() { return comment; }
    public Instant getDecidedAt() { return decidedAt; }
}
