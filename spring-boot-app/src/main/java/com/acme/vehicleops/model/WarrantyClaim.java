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
@Table(name = "WARRANTYCLAIM")
public class WarrantyClaim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CLAIMID")
    private Integer claimId;

    @Column(name = "CLAIMNUMBER", length = 20, unique = true)
    private String claimNumber;

    @Column(name = "VEHICLEID")
    private Integer vehicleId;

    @Column(name = "DEALERCODE", length = 10)
    private String dealerCode;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "CLAIMDATE")
    private Date claimDate;

    @Column(name = "MILEAGEATCLAIM")
    private Integer mileageAtClaim;

    @Column(name = "SYMPTOMCODE", length = 10)
    private String symptomCode;

    @Column(name = "CAUSALPARTNUMBER", length = 25)
    private String causalPartNumber;

    @Column(name = "LABOROPERATIONCODE", length = 15)
    private String laborOperationCode;

    @Column(name = "LABORHOURS", precision = 6, scale = 2)
    private BigDecimal laborHours;

    @Column(name = "LABORRATE", precision = 8, scale = 2)
    private BigDecimal laborRate;

    @Column(name = "PARTSCOST", precision = 10, scale = 2)
    private BigDecimal partsCost;

    @Column(name = "SUBLETCOST", precision = 10, scale = 2)
    private BigDecimal subletCost;

    @Column(name = "DEDUCTIBLE", precision = 8, scale = 2)
    private BigDecimal deductible;

    @Column(name = "TOTALAMOUNT", precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "COVERAGETYPE", length = 20)
    private String coverageType;

    @Column(name = "CLAIMSTATUS", length = 15)
    private String claimStatus;

    @Column(name = "DENIALREASONCODE", length = 10)
    private String denialReasonCode;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "APPROVEDDATE")
    private Date approvedDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "SETTLEDDATE")
    private Date settledDate;

    @Column(name = "SUBMITTEDBY", length = 50)
    private String submittedBy;

    public WarrantyClaim() {}

    // Getters and setters
    public Integer getClaimId() { return claimId; }
    public void setClaimId(Integer claimId) { this.claimId = claimId; }

    public String getClaimNumber() { return claimNumber; }
    public void setClaimNumber(String claimNumber) { this.claimNumber = claimNumber; }

    public Integer getVehicleId() { return vehicleId; }
    public void setVehicleId(Integer vehicleId) { this.vehicleId = vehicleId; }

    public String getDealerCode() { return dealerCode; }
    public void setDealerCode(String dealerCode) { this.dealerCode = dealerCode; }

    public Date getClaimDate() { return claimDate; }
    public void setClaimDate(Date claimDate) { this.claimDate = claimDate; }

    public Integer getMileageAtClaim() { return mileageAtClaim; }
    public void setMileageAtClaim(Integer mileageAtClaim) { this.mileageAtClaim = mileageAtClaim; }

    public String getSymptomCode() { return symptomCode; }
    public void setSymptomCode(String symptomCode) { this.symptomCode = symptomCode; }

    public String getCausalPartNumber() { return causalPartNumber; }
    public void setCausalPartNumber(String causalPartNumber) { this.causalPartNumber = causalPartNumber; }

    public String getLaborOperationCode() { return laborOperationCode; }
    public void setLaborOperationCode(String laborOperationCode) { this.laborOperationCode = laborOperationCode; }

    public BigDecimal getLaborHours() { return laborHours; }
    public void setLaborHours(BigDecimal laborHours) { this.laborHours = laborHours; }

    public BigDecimal getLaborRate() { return laborRate; }
    public void setLaborRate(BigDecimal laborRate) { this.laborRate = laborRate; }

    public BigDecimal getPartsCost() { return partsCost; }
    public void setPartsCost(BigDecimal partsCost) { this.partsCost = partsCost; }

    public BigDecimal getSubletCost() { return subletCost; }
    public void setSubletCost(BigDecimal subletCost) { this.subletCost = subletCost; }

    public BigDecimal getDeductible() { return deductible; }
    public void setDeductible(BigDecimal deductible) { this.deductible = deductible; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public String getCoverageType() { return coverageType; }
    public void setCoverageType(String coverageType) { this.coverageType = coverageType; }

    public String getClaimStatus() { return claimStatus; }
    public void setClaimStatus(String claimStatus) { this.claimStatus = claimStatus; }

    public String getDenialReasonCode() { return denialReasonCode; }
    public void setDenialReasonCode(String denialReasonCode) { this.denialReasonCode = denialReasonCode; }

    public Date getApprovedDate() { return approvedDate; }
    public void setApprovedDate(Date approvedDate) { this.approvedDate = approvedDate; }

    public Date getSettledDate() { return settledDate; }
    public void setSettledDate(Date settledDate) { this.settledDate = settledDate; }

    public String getSubmittedBy() { return submittedBy; }
    public void setSubmittedBy(String submittedBy) { this.submittedBy = submittedBy; }
}
