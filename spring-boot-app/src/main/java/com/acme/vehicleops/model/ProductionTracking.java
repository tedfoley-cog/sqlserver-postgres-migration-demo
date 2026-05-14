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
@Table(name = "production_tracking")
public class ProductionTracking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tracking_id")
    private Integer trackingId;

    @Column(name = "vehicle_id", nullable = false)
    private Integer vehicleId;

    @Column(name = "station_id", nullable = false)
    private Integer stationId;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "entry_time", nullable = false)
    private Date entryTime;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "exit_time")
    private Date exitTime;

    @Column(name = "operator_id", length = 20)
    private String operatorId;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "defects_found")
    private Integer defectsFound;

    @Column(name = "notes", length = 500)
    private String notes;

    public ProductionTracking() {}

    public Integer getTrackingId() { return trackingId; }
    public void setTrackingId(Integer trackingId) { this.trackingId = trackingId; }

    public Integer getVehicleId() { return vehicleId; }
    public void setVehicleId(Integer vehicleId) { this.vehicleId = vehicleId; }

    public Integer getStationId() { return stationId; }
    public void setStationId(Integer stationId) { this.stationId = stationId; }

    public Date getEntryTime() { return entryTime; }
    public void setEntryTime(Date entryTime) { this.entryTime = entryTime; }

    public Date getExitTime() { return exitTime; }
    public void setExitTime(Date exitTime) { this.exitTime = exitTime; }

    public String getOperatorId() { return operatorId; }
    public void setOperatorId(String operatorId) { this.operatorId = operatorId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getDefectsFound() { return defectsFound; }
    public void setDefectsFound(Integer defectsFound) { this.defectsFound = defectsFound; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
