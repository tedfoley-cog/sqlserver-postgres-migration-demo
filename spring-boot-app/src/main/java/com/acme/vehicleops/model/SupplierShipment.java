package com.acme.vehicleops.model;

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
@Table(name = "SUPPLIERSHIPMENT")
public class SupplierShipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SHIPMENTID")
    private Integer shipmentId;

    @Column(name = "SUPPLIERCODE", length = 20, nullable = false)
    private String supplierCode;

    @Column(name = "PURCHASEORDERNO", length = 20)
    private String purchaseOrderNo;

    @Column(name = "PARTNUMBER", length = 25, nullable = false)
    private String partNumber;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "SHIPDATE", nullable = false)
    private Date shipDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "RECEIVEDDATE")
    private Date receivedDate;

    @Column(name = "ORDEREDQTY", nullable = false)
    private Integer orderedQty;

    @Column(name = "RECEIVEDQTY")
    private Integer receivedQty;

    @Column(name = "REJECTEDQTY")
    private Integer rejectedQty;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "DUEDATE", nullable = false)
    private Date dueDate;

    @Column(name = "ISONTIME")
    private Boolean isOnTime;

    @Column(name = "INSPECTIONRESULT", length = 15)
    private String inspectionResult;

    public SupplierShipment() {}

    public Integer getShipmentId() { return shipmentId; }
    public void setShipmentId(Integer shipmentId) { this.shipmentId = shipmentId; }

    public String getSupplierCode() { return supplierCode; }
    public void setSupplierCode(String supplierCode) { this.supplierCode = supplierCode; }

    public String getPurchaseOrderNo() { return purchaseOrderNo; }
    public void setPurchaseOrderNo(String purchaseOrderNo) { this.purchaseOrderNo = purchaseOrderNo; }

    public String getPartNumber() { return partNumber; }
    public void setPartNumber(String partNumber) { this.partNumber = partNumber; }

    public Date getShipDate() { return shipDate; }
    public void setShipDate(Date shipDate) { this.shipDate = shipDate; }

    public Date getReceivedDate() { return receivedDate; }
    public void setReceivedDate(Date receivedDate) { this.receivedDate = receivedDate; }

    public Integer getOrderedQty() { return orderedQty; }
    public void setOrderedQty(Integer orderedQty) { this.orderedQty = orderedQty; }

    public Integer getReceivedQty() { return receivedQty; }
    public void setReceivedQty(Integer receivedQty) { this.receivedQty = receivedQty; }

    public Integer getRejectedQty() { return rejectedQty; }
    public void setRejectedQty(Integer rejectedQty) { this.rejectedQty = rejectedQty; }

    public Date getDueDate() { return dueDate; }
    public void setDueDate(Date dueDate) { this.dueDate = dueDate; }

    public Boolean getIsOnTime() { return isOnTime; }
    public void setIsOnTime(Boolean isOnTime) { this.isOnTime = isOnTime; }

    public String getInspectionResult() { return inspectionResult; }
    public void setInspectionResult(String inspectionResult) { this.inspectionResult = inspectionResult; }
}
