package com.acme.vehicleops.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "part_fitment")
public class PartFitment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fitment_id")
    private Integer fitmentId;

    @Column(name = "part_number", length = 25, nullable = false)
    private String partNumber;

    @Column(name = "model_code", length = 10, nullable = false)
    private String modelCode;

    @Column(name = "model_year_from", nullable = false)
    private Integer modelYearFrom;

    @Column(name = "model_year_to", nullable = false)
    private Integer modelYearTo;

    @Column(name = "trim_level", length = 20)
    private String trimLevel;

    @Column(name = "engine_code", length = 15)
    private String engineCode;

    @Column(name = "notes", length = 200)
    private String notes;

    public PartFitment() {}

    public Integer getFitmentId() { return fitmentId; }
    public void setFitmentId(Integer fitmentId) { this.fitmentId = fitmentId; }

    public String getPartNumber() { return partNumber; }
    public void setPartNumber(String partNumber) { this.partNumber = partNumber; }

    public String getModelCode() { return modelCode; }
    public void setModelCode(String modelCode) { this.modelCode = modelCode; }

    public Integer getModelYearFrom() { return modelYearFrom; }
    public void setModelYearFrom(Integer modelYearFrom) { this.modelYearFrom = modelYearFrom; }

    public Integer getModelYearTo() { return modelYearTo; }
    public void setModelYearTo(Integer modelYearTo) { this.modelYearTo = modelYearTo; }

    public String getTrimLevel() { return trimLevel; }
    public void setTrimLevel(String trimLevel) { this.trimLevel = trimLevel; }

    public String getEngineCode() { return engineCode; }
    public void setEngineCode(String engineCode) { this.engineCode = engineCode; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
