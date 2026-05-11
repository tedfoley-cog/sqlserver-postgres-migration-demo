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
@Table(name = "PART")
public class Part {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PARTID")
    private Integer partId;

    @Column(name = "PARTNUMBER", length = 25, unique = true)
    private String partNumber;

    @Column(name = "DESCRIPTION", length = 100)
    private String description;

    @Column(name = "PARTGROUPCODE", length = 10)
    private String partGroupCode;

    @Column(name = "UNITCOST", precision = 10, scale = 2)
    private BigDecimal unitCost;

    @Column(name = "LISTPRICE", precision = 10, scale = 2)
    private BigDecimal listPrice;

    @Column(name = "STATUS", length = 15)
    private String status;

    @Column(name = "SUPERSEDEDBYPARTNO", length = 25)
    private String supersededByPartNo;

    @Column(name = "SUPPLIERCODE", length = 20)
    private String supplierCode;

    @Column(name = "LEADTIMEDAYS")
    private Integer leadTimeDays;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "CREATEDDATE")
    private Date createdDate;

    public Part() {}

    public Integer getPartId() { return partId; }
    public void setPartId(Integer partId) { this.partId = partId; }

    public String getPartNumber() { return partNumber; }
    public void setPartNumber(String partNumber) { this.partNumber = partNumber; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPartGroupCode() { return partGroupCode; }
    public void setPartGroupCode(String partGroupCode) { this.partGroupCode = partGroupCode; }

    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }

    public BigDecimal getListPrice() { return listPrice; }
    public void setListPrice(BigDecimal listPrice) { this.listPrice = listPrice; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getSupersededByPartNo() { return supersededByPartNo; }
    public void setSupersededByPartNo(String supersededByPartNo) { this.supersededByPartNo = supersededByPartNo; }

    public String getSupplierCode() { return supplierCode; }
    public void setSupplierCode(String supplierCode) { this.supplierCode = supplierCode; }

    public Integer getLeadTimeDays() { return leadTimeDays; }
    public void setLeadTimeDays(Integer leadTimeDays) { this.leadTimeDays = leadTimeDays; }

    public Date getCreatedDate() { return createdDate; }
    public void setCreatedDate(Date createdDate) { this.createdDate = createdDate; }
}
