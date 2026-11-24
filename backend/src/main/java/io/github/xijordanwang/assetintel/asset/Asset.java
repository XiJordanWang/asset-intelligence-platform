package io.github.xijordanwang.assetintel.asset;

import io.github.xijordanwang.assetintel.site.Site;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "asset")
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String tag;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssetCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssetStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "site_id")
    private Site site;

    private String supplier;

    @Column(name = "original_value", nullable = false)
    private BigDecimal originalValue;

    @Column(nullable = false)
    private BigDecimal impairment = BigDecimal.ZERO;

    @Column(name = "in_service_date")
    private LocalDate inServiceDate;

    @Column(name = "last_verified_date")
    private LocalDate lastVerifiedDate;

    protected Asset() {
    }

    public Asset(String tag, String name, AssetCategory category, AssetStatus status, Site site,
                 String supplier, BigDecimal originalValue, LocalDate inServiceDate) {
        this.tag = tag;
        this.name = name;
        this.category = category;
        this.status = status;
        this.site = site;
        this.supplier = supplier;
        this.originalValue = originalValue;
        this.inServiceDate = inServiceDate;
    }

    public void recordImpairment(BigDecimal amount) {
        this.impairment = this.impairment.add(amount);
    }

    public void markVerified(LocalDate date) {
        this.lastVerifiedDate = date;
    }

    public Long getId() { return id; }
    public String getTag() { return tag; }
    public String getName() { return name; }
    public AssetCategory getCategory() { return category; }
    public AssetStatus getStatus() { return status; }
    public Site getSite() { return site; }
    public String getSupplier() { return supplier; }
    public BigDecimal getOriginalValue() { return originalValue; }
    public BigDecimal getImpairment() { return impairment; }
    public LocalDate getInServiceDate() { return inServiceDate; }
    public LocalDate getLastVerifiedDate() { return lastVerifiedDate; }
}
