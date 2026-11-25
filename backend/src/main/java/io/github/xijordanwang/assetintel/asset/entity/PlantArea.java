package io.github.xijordanwang.assetintel.asset.entity;

/** Where in a plant an asset is used. The plant view draws one zone of the site plan per area. */
public enum PlantArea {
    MACHINING("Machining"),
    MOULDING("Moulding"),
    ASSEMBLY("Assembly"),
    FINISHING("Finishing"),
    QUALITY_LAB("Quality lab"),
    WAREHOUSE("Warehouse & dispatch"),
    OFFICES("Offices"),
    SITE_SERVICES("Site services");

    private final String label;

    PlantArea(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** A sensible area for a new asset of this class, used until someone assigns a better one. */
    public static PlantArea defaultFor(AssetCategory category) {
        return switch (category) {
            case PLANT_MACHINERY -> ASSEMBLY;
            case IT_EQUIPMENT, FURNITURE -> OFFICES;
            case VEHICLE -> WAREHOUSE;
            case BUILDING, LAND, CONSTRUCTION_IN_PROGRESS -> SITE_SERVICES;
        };
    }
}
