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
@Table(name = "VEHICLE")
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "VEHICLEID")
    private Integer vehicleId;

    @Column(name = "VIN", nullable = false, unique = true, length = 17)
    private String vin;

    @Column(name = "MODELYEAR")
    private Integer modelYear;

    @Column(name = "MODELCODE", length = 10)
    private String modelCode;

    @Column(name = "MODELNAME", length = 50)
    private String modelName;

    @Column(name = "TRIMLEVEL", length = 20)
    private String trimLevel;

    @Column(name = "ENGINECODE", length = 15)
    private String engineCode;

    @Column(name = "TRANSMISSIONTYPE", length = 10)
    private String transmissionType;

    @Column(name = "PLANTCODE", length = 5)
    private String plantCode;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "ASSEMBLYDATE")
    private Date assemblyDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "SHIPDATE")
    private Date shipDate;

    @Column(name = "DEALERCODE", length = 10)
    private String dealerCode;

    @Column(name = "CURRENTMILEAGE")
    private Integer currentMileage;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "WARRANTYSTARTDATE")
    private Date warrantyStartDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "WARRANTYENDDATE")
    private Date warrantyEndDate;

    @Column(name = "STATUS", length = 20)
    private String status;

    public Vehicle() {}

    // Getters and setters
    public Integer getVehicleId() { return vehicleId; }
    public void setVehicleId(Integer vehicleId) { this.vehicleId = vehicleId; }

    public String getVin() { return vin; }
    public void setVin(String vin) { this.vin = vin; }

    public Integer getModelYear() { return modelYear; }
    public void setModelYear(Integer modelYear) { this.modelYear = modelYear; }

    public String getModelCode() { return modelCode; }
    public void setModelCode(String modelCode) { this.modelCode = modelCode; }

    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }

    public String getTrimLevel() { return trimLevel; }
    public void setTrimLevel(String trimLevel) { this.trimLevel = trimLevel; }

    public String getEngineCode() { return engineCode; }
    public void setEngineCode(String engineCode) { this.engineCode = engineCode; }

    public String getTransmissionType() { return transmissionType; }
    public void setTransmissionType(String transmissionType) { this.transmissionType = transmissionType; }

    public String getPlantCode() { return plantCode; }
    public void setPlantCode(String plantCode) { this.plantCode = plantCode; }

    public Date getAssemblyDate() { return assemblyDate; }
    public void setAssemblyDate(Date assemblyDate) { this.assemblyDate = assemblyDate; }

    public Date getShipDate() { return shipDate; }
    public void setShipDate(Date shipDate) { this.shipDate = shipDate; }

    public String getDealerCode() { return dealerCode; }
    public void setDealerCode(String dealerCode) { this.dealerCode = dealerCode; }

    public Integer getCurrentMileage() { return currentMileage; }
    public void setCurrentMileage(Integer currentMileage) { this.currentMileage = currentMileage; }

    public Date getWarrantyStartDate() { return warrantyStartDate; }
    public void setWarrantyStartDate(Date warrantyStartDate) { this.warrantyStartDate = warrantyStartDate; }

    public Date getWarrantyEndDate() { return warrantyEndDate; }
    public void setWarrantyEndDate(Date warrantyEndDate) { this.warrantyEndDate = warrantyEndDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
