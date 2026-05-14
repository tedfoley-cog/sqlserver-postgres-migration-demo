package com.acme.vehicleops.model;

import java.math.BigDecimal;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "PRODUCTIONSTATION")
public class ProductionStation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "STATIONID")
    private Integer stationId;

    @Column(name = "STATIONCODE", length = 15, nullable = false)
    private String stationCode;

    @Column(name = "STATIONNAME", length = 50, nullable = false)
    private String stationName;

    @Column(name = "PLANTCODE", length = 5, nullable = false)
    private String plantCode;

    @Column(name = "SEQUENCEORDER", nullable = false)
    private Integer sequenceOrder;

    @Column(name = "CYCLETIMEMINUTES", precision = 8, scale = 2)
    private BigDecimal cycleTimeMinutes;

    @Column(name = "ISQUALITYGATE")
    private Boolean isQualityGate;

    public ProductionStation() {}

    public Integer getStationId() { return stationId; }
    public void setStationId(Integer stationId) { this.stationId = stationId; }

    public String getStationCode() { return stationCode; }
    public void setStationCode(String stationCode) { this.stationCode = stationCode; }

    public String getStationName() { return stationName; }
    public void setStationName(String stationName) { this.stationName = stationName; }

    public String getPlantCode() { return plantCode; }
    public void setPlantCode(String plantCode) { this.plantCode = plantCode; }

    public Integer getSequenceOrder() { return sequenceOrder; }
    public void setSequenceOrder(Integer sequenceOrder) { this.sequenceOrder = sequenceOrder; }

    public BigDecimal getCycleTimeMinutes() { return cycleTimeMinutes; }
    public void setCycleTimeMinutes(BigDecimal cycleTimeMinutes) { this.cycleTimeMinutes = cycleTimeMinutes; }

    public Boolean getIsQualityGate() { return isQualityGate; }
    public void setIsQualityGate(Boolean isQualityGate) { this.isQualityGate = isQualityGate; }
}
