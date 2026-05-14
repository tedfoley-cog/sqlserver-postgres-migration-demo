package com.acme.vehicleops.model;

import java.math.BigDecimal;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "warranty_coverage")
public class WarrantyCoverage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "coverage_id")
    private Integer coverageId;

    @Column(name = "coverage_type", length = 20, nullable = false)
    private String coverageType;

    @Column(name = "model_year", nullable = false)
    private Integer modelYear;

    @Column(name = "months_from_sale", nullable = false)
    private Integer monthsFromSale;

    @Column(name = "mileage_limit", nullable = false)
    private Integer mileageLimit;

    @Column(name = "description", length = 100)
    private String description;

    @Column(name = "deductible_amount", precision = 8, scale = 2)
    private BigDecimal deductibleAmount;

    @Column(name = "part_group_codes", length = 200)
    private String partGroupCodes;

    public WarrantyCoverage() {}

    public Integer getCoverageId() { return coverageId; }
    public void setCoverageId(Integer coverageId) { this.coverageId = coverageId; }

    public String getCoverageType() { return coverageType; }
    public void setCoverageType(String coverageType) { this.coverageType = coverageType; }

    public Integer getModelYear() { return modelYear; }
    public void setModelYear(Integer modelYear) { this.modelYear = modelYear; }

    public Integer getMonthsFromSale() { return monthsFromSale; }
    public void setMonthsFromSale(Integer monthsFromSale) { this.monthsFromSale = monthsFromSale; }

    public Integer getMileageLimit() { return mileageLimit; }
    public void setMileageLimit(Integer mileageLimit) { this.mileageLimit = mileageLimit; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getDeductibleAmount() { return deductibleAmount; }
    public void setDeductibleAmount(BigDecimal deductibleAmount) { this.deductibleAmount = deductibleAmount; }

    public String getPartGroupCodes() { return partGroupCodes; }
    public void setPartGroupCodes(String partGroupCodes) { this.partGroupCodes = partGroupCodes; }
}
