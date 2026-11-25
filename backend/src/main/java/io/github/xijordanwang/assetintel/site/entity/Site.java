package io.github.xijordanwang.assetintel.site.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

/** A physical location of the fictional company. Each site belongs to one plant. */
@Entity
@Table(name = "site")
public class Site {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(name = "plant_code", nullable = false)
    private String plantCode;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String region;

    private double latitude;
    private double longitude;

    @Column(name = "annual_capex_budget", nullable = false)
    private BigDecimal annualCapexBudget;

    /** Person who runs stocktakes at this site. */
    private String coordinator;

    protected Site() {
    }

    public Site(String code, String name, String plantCode, String city, String region,
                double latitude, double longitude, BigDecimal annualCapexBudget) {
        this.code = code;
        this.name = name;
        this.plantCode = plantCode;
        this.city = city;
        this.region = region;
        this.latitude = latitude;
        this.longitude = longitude;
        this.annualCapexBudget = annualCapexBudget;
    }

    public void assignCoordinator(String coordinator) {
        this.coordinator = coordinator;
    }

    public String getCoordinator() { return coordinator; }
    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getPlantCode() { return plantCode; }
    public String getCity() { return city; }
    public String getRegion() { return region; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public BigDecimal getAnnualCapexBudget() { return annualCapexBudget; }
}
