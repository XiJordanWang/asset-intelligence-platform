package io.github.xijordanwang.assetintel.asset.entity;

/** The investment programme an asset was bought under. Assets bought outside a programme have none. */
public enum CapexProgramme {
    LINE_AUTOMATION("Line automation"),
    CAPACITY_EXPANSION("Capacity expansion"),
    DIGITAL_FACTORY("Digital factory"),
    FLEET_ELECTRIFICATION("Fleet electrification"),
    SOLAR_AND_ENERGY("Solar and energy"),
    HEALTH_AND_SAFETY("Health and safety"),
    WORKPLACE_REFRESH("Workplace refresh"),
    SUSTAINING("Sustaining capex");

    private final String label;

    CapexProgramme(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
