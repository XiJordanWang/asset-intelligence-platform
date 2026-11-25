package io.github.xijordanwang.assetintel.change.entity;

public enum ChangeType {
    TRANSFER,
    DISPOSAL,
    /** Change of master data (name, custodian, supplier), with old and new values kept side by side. */
    MODIFICATION
}
