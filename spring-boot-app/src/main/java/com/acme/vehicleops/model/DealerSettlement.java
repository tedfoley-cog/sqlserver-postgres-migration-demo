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
@Table(name = "dealer_settlement")
public class DealerSettlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "settlement_id")
    private Integer settlementId;

    @Column(name = "settlement_batch_no", length = 20, nullable = false)
    private String settlementBatchNo;

    @Column(name = "dealer_code", length = 10, nullable = false)
    private String dealerCode;

    @Column(name = "claim_id")
    private Integer claimId;

    @Column(name = "labor_amount", precision = 10, scale = 2)
    private BigDecimal laborAmount;

    @Column(name = "parts_amount", precision = 10, scale = 2)
    private BigDecimal partsAmount;

    @Column(name = "parts_markup", precision = 10, scale = 2)
    private BigDecimal partsMarkup;

    @Column(name = "sublet_amount", precision = 10, scale = 2)
    private BigDecimal subletAmount;

    @Column(name = "deductible_credit", precision = 8, scale = 2)
    private BigDecimal deductibleCredit;

    @Column(name = "net_amount", precision = 10, scale = 2)
    private BigDecimal netAmount;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "settlement_date")
    private Date settlementDate;

    @Column(name = "payment_status", length = 15)
    private String paymentStatus;

    public DealerSettlement() {}

    public Integer getSettlementId() { return settlementId; }
    public void setSettlementId(Integer settlementId) { this.settlementId = settlementId; }

    public String getSettlementBatchNo() { return settlementBatchNo; }
    public void setSettlementBatchNo(String settlementBatchNo) { this.settlementBatchNo = settlementBatchNo; }

    public String getDealerCode() { return dealerCode; }
    public void setDealerCode(String dealerCode) { this.dealerCode = dealerCode; }

    public Integer getClaimId() { return claimId; }
    public void setClaimId(Integer claimId) { this.claimId = claimId; }

    public BigDecimal getLaborAmount() { return laborAmount; }
    public void setLaborAmount(BigDecimal laborAmount) { this.laborAmount = laborAmount; }

    public BigDecimal getPartsAmount() { return partsAmount; }
    public void setPartsAmount(BigDecimal partsAmount) { this.partsAmount = partsAmount; }

    public BigDecimal getPartsMarkup() { return partsMarkup; }
    public void setPartsMarkup(BigDecimal partsMarkup) { this.partsMarkup = partsMarkup; }

    public BigDecimal getSubletAmount() { return subletAmount; }
    public void setSubletAmount(BigDecimal subletAmount) { this.subletAmount = subletAmount; }

    public BigDecimal getDeductibleCredit() { return deductibleCredit; }
    public void setDeductibleCredit(BigDecimal deductibleCredit) { this.deductibleCredit = deductibleCredit; }

    public BigDecimal getNetAmount() { return netAmount; }
    public void setNetAmount(BigDecimal netAmount) { this.netAmount = netAmount; }

    public Date getSettlementDate() { return settlementDate; }
    public void setSettlementDate(Date settlementDate) { this.settlementDate = settlementDate; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
}
