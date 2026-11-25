package io.github.xijordanwang.assetintel.change.entity;

public enum ChangeStatus {
    DRAFT,
    PENDING_APPROVAL,
    APPROVED,
    REJECTED,
    CANCELLED;

    public boolean isOpen() {
        return this == DRAFT || this == PENDING_APPROVAL;
    }
}
