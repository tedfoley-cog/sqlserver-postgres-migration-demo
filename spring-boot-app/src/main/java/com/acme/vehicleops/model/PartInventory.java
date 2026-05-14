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
@Table(name = "PARTINVENTORY")
public class PartInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "INVENTORYID")
    private Integer inventoryId;

    @Column(name = "PARTNUMBER", length = 25, nullable = false)
    private String partNumber;

    @Column(name = "WAREHOUSECODE", length = 10, nullable = false)
    private String warehouseCode;

    @Column(name = "ONHANDQTY")
    private Integer onHandQty;

    @Column(name = "REORDERPOINT")
    private Integer reorderPoint;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "LASTCOUNTDATE")
    private Date lastCountDate;

    public PartInventory() {}

    public Integer getInventoryId() { return inventoryId; }
    public void setInventoryId(Integer inventoryId) { this.inventoryId = inventoryId; }

    public String getPartNumber() { return partNumber; }
    public void setPartNumber(String partNumber) { this.partNumber = partNumber; }

    public String getWarehouseCode() { return warehouseCode; }
    public void setWarehouseCode(String warehouseCode) { this.warehouseCode = warehouseCode; }

    public Integer getOnHandQty() { return onHandQty; }
    public void setOnHandQty(Integer onHandQty) { this.onHandQty = onHandQty; }

    public Integer getReorderPoint() { return reorderPoint; }
    public void setReorderPoint(Integer reorderPoint) { this.reorderPoint = reorderPoint; }

    public Date getLastCountDate() { return lastCountDate; }
    public void setLastCountDate(Date lastCountDate) { this.lastCountDate = lastCountDate; }
}
