package io.github.xijordanwang.assetintel.security.entity;

/** What a signed-in user may do. Plant managers and site coordinators are limited to their own plant. */
public enum UserRole {
    REQUESTER("Raises investment requests and asset change forms"),
    PLANT_MANAGER("Approves the first step for their plant, and transfers into it"),
    FINANCE_CONTROLLER("Approves larger amounts, capitalises assets and runs stocktakes"),
    CFO("Approves the largest amounts"),
    SITE_COORDINATOR("Counts assets at the sites of their plant during a stocktake"),
    ADMIN("Manages users and roles; can act for any site in a stocktake");

    private final String description;

    UserRole(String description) {
        this.description = description;
    }

    public String description() {
        return description;
    }
}
