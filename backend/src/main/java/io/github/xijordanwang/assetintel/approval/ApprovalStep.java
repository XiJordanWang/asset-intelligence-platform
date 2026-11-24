package io.github.xijordanwang.assetintel.approval;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "approval_step")
public class ApprovalStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id")
    private InvestmentRequest request;

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

    protected ApprovalStep() {
    }

    ApprovalStep(InvestmentRequest request, int stepOrder, ApproverRole approverRole) {
        this.request = request;
        this.stepOrder = stepOrder;
        this.approverRole = approverRole;
    }

    void decide(Decision decision, String decidedBy, String comment, Instant at) {
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
