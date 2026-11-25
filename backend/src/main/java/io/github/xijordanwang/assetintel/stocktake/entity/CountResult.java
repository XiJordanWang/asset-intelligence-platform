package io.github.xijordanwang.assetintel.stocktake.entity;

public enum CountResult {
    PENDING,
    /** Seen, in normal condition. */
    FOUND,
    /** Seen, but needs repair. */
    DAMAGED,
    /** Not found. Completing the campaign raises a draft disposal (reason LOST) for finance to review. */
    MISSING
}
