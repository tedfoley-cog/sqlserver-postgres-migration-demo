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
@Table(name = "labor_rate_schedule")
public class LaborRateSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rate_id")
    private Integer rateId;

    @Column(name = "region_code", length = 10, nullable = false)
    private String regionCode;

    @Column(name = "dealer_tier", length = 5, nullable = false)
    private String dealerTier;

    @Column(name = "labor_type", length = 20, nullable = false)
    private String laborType;

    @Column(name = "hourly_rate", precision = 8, scale = 2, nullable = false)
    private BigDecimal hourlyRate;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "effective_date", nullable = false)
    private Date effectiveDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "expiration_date")
    private Date expirationDate;

    public LaborRateSchedule() {}

    public Integer getRateId() { return rateId; }
    public void setRateId(Integer rateId) { this.rateId = rateId; }

    public String getRegionCode() { return regionCode; }
    public void setRegionCode(String regionCode) { this.regionCode = regionCode; }

    public String getDealerTier() { return dealerTier; }
    public void setDealerTier(String dealerTier) { this.dealerTier = dealerTier; }

    public String getLaborType() { return laborType; }
    public void setLaborType(String laborType) { this.laborType = laborType; }

    public BigDecimal getHourlyRate() { return hourlyRate; }
    public void setHourlyRate(BigDecimal hourlyRate) { this.hourlyRate = hourlyRate; }

    public Date getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(Date effectiveDate) { this.effectiveDate = effectiveDate; }

    public Date getExpirationDate() { return expirationDate; }
    public void setExpirationDate(Date expirationDate) { this.expirationDate = expirationDate; }
}
