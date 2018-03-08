package com.tierconnect.riot.iot.entities;

import javax.persistence.*;
import javax.annotation.Generated;
import java.util.List;

@Entity

@Table(name="reportEntryOption")@Generated("com.tierconnect.riot.appgen.service.GenModel")
public class ReportEntryOption extends ReportEntryOptionBase 
{
    @OneToMany(mappedBy="reportEntryOption", fetch= FetchType.EAGER, cascade = CascadeType.ALL)
    protected List<ReportEntryOptionProperty> reportEntryOptionProperties;

    public List<ReportEntryOptionProperty> getReportEntryOptionProperties() {
        return reportEntryOptionProperties;
    }

    public void setReportEntryOptionProperties(List<ReportEntryOptionProperty> reportEntryOptionProperties) {
        this.reportEntryOptionProperties = reportEntryOptionProperties;
    }
}

