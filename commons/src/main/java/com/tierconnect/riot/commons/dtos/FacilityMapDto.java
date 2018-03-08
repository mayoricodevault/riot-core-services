package com.tierconnect.riot.commons.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.base.Objects;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by vramos on 2/12/17.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FacilityMapDto implements Serializable {
    public Long id;
    public String name;
    public String code;
    public Date time;
    public String description;
    public boolean blinked;
    public Boolean modified;

    public Double lonOrigin;
    public Double latOrigin;
    public Double altOrigin;
    public Double declination;
    public Double imageWidth;
    public Double imageHeight;
    public Double xNominal;
    public Double yNominal;
    public Double latOriginNominal;
    public Double lonOriginNominal;
    public String imageUnit;
    public Double lonmin;
    public Double lonmax;
    public Double latmin;
    public Double latmax;
    public Long modifiedTime;

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    public Date getTime() {
        return time;
    }

    public String getDescription() {
        return description;
    }

    public boolean isBlinked() {
        return blinked;
    }

    public Boolean getModified() {
        return modified;
    }

    public Double getLonOrigin() {
        return lonOrigin;
    }

    public Double getLatOrigin() {
        return latOrigin;
    }

    public Double getAltOrigin() {
        return altOrigin;
    }

    public Double getDeclination() {
        return declination;
    }

    public Double getImageWidth() {
        return imageWidth;
    }

    public Double getImageHeight() {
        return imageHeight;
    }

    public Double getxNominal() {
        return xNominal;
    }

    public Double getyNominal() {
        return yNominal;
    }

    public Double getLatOriginNominal() {
        return latOriginNominal;
    }

    public Double getLonOriginNominal() {
        return lonOriginNominal;
    }

    public String getImageUnit() {
        return imageUnit;
    }

    public Double getLonmin() {
        return lonmin;
    }

    public Double getLonmax() {
        return lonmax;
    }

    public Double getLatmin() {
        return latmin;
    }

    public Double getLatmax() {
        return latmax;
    }

    public Long getModifiedTime() {
        return modifiedTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FacilityMapDto that = (FacilityMapDto) o;
        return Objects.equal(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(code);
    }
}
