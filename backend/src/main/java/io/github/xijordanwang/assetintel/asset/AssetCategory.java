package io.github.xijordanwang.assetintel.asset;

/**
 * Asset classes with a straight-line useful life in months.
 * Land and construction in progress (CIP) are not depreciated.
 */
public enum AssetCategory {
    BUILDING(40 * 12),
    PLANT_MACHINERY(10 * 12),
    IT_EQUIPMENT(4 * 12),
    VEHICLE(6 * 12),
    FURNITURE(8 * 12),
    LAND(0),
    CONSTRUCTION_IN_PROGRESS(0);

    private final int usefulLifeMonths;

    AssetCategory(int usefulLifeMonths) {
        this.usefulLifeMonths = usefulLifeMonths;
    }

    public int usefulLifeMonths() {
        return usefulLifeMonths;
    }

    public boolean isDepreciable() {
        return usefulLifeMonths > 0;
    }
}
