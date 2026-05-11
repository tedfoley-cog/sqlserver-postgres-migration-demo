package com.acme.vehicleops.model;

import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name = "SUPPLIER")
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SUPPLIERID")
    private Integer supplierId;

    @Column(name = "SUPPLIERCODE", length = 20, unique = true)
    private String supplierCode;

    @Column(name = "SUPPLIERNAME", length = 100)
    private String supplierName;

    @Column(name = "REGION", length = 10)
    private String region;

    @Column(name = "COUNTRY", length = 30)
    private String country;

    @Column(name = "QUALITYRATING", length = 5)
    private String qualityRating;

    @Column(name = "ISAPPROVED")
    private Boolean isApproved;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "CERTIFICATIONEXPIRY")
    private Date certificationExpiry;

    @Column(name = "ONTIMEDELIVERY_PCT", precision = 5, scale = 2)
    private BigDecimal onTimeDeliveryPct;

    @Column(name = "DEFECTPPM")
    private Integer defectPpm;

    public Supplier() {}

    public Integer getSupplierId() { return supplierId; }
    public void setSupplierId(Integer supplierId) { this.supplierId = supplierId; }

    public String getSupplierCode() { return supplierCode; }
    public void setSupplierCode(String supplierCode) { this.supplierCode = supplierCode; }

    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getQualityRating() { return qualityRating; }
    public void setQualityRating(String qualityRating) { this.qualityRating = qualityRating; }

    public Boolean getIsApproved() { return isApproved; }
    public void setIsApproved(Boolean isApproved) { this.isApproved = isApproved; }

    public Date getCertificationExpiry() { return certificationExpiry; }
    public void setCertificationExpiry(Date certificationExpiry) { this.certificationExpiry = certificationExpiry; }

    public BigDecimal getOnTimeDeliveryPct() { return onTimeDeliveryPct; }
    public void setOnTimeDeliveryPct(BigDecimal onTimeDeliveryPct) { this.onTimeDeliveryPct = onTimeDeliveryPct; }

    public Integer getDefectPpm() { return defectPpm; }
    public void setDefectPpm(Integer defectPpm) { this.defectPpm = defectPpm; }
}
