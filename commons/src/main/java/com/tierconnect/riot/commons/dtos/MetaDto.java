package com.tierconnect.riot.commons.dtos;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.base.Objects;

import java.io.Serializable;
import java.util.Arrays;

/**
 * MetaDto class.
 *
 * @author jantezana
 * @author achambi
 * @version 2017/01/25
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MetaDto implements Serializable {
    private static final long serialVersionUID = 1L;
    public String bridgeCode;
    public String tenantCode;
    public Long sqn;
    public String specName;
    public Double[] origin;
    public String units;
    public Long partition;
    public Long numPartitions;
    public Boolean reblinked;
    public Boolean outOfOrder;
    public Boolean newBlink;
    public Boolean runRules;
    public Boolean skipMainFlow;

    public String getBridgeCode() {
        return bridgeCode;
    }

    public String getTenantCode() {
        return tenantCode;
    }

    public Long getSqn() {
        return sqn;
    }

    public String getSpecName() {
        return specName;
    }

    public Double[] getOrigin() {
        return origin;
    }

    public String getUnits() {
        return units;
    }

    public Long getPartition() {
        return partition;
    }

    public Long getNumPartitions() {
        return numPartitions;
    }

    public Boolean getReblinked() {
        return reblinked;
    }

    public Boolean getOutOfOrder() {
        return outOfOrder;
    }

    public Boolean getNewBlink() {
        return newBlink;
    }

    public Boolean getRunRules() {
        return runRules;
    }

    @JsonIgnore
    public Boolean getSkipMainFlow() {
        return skipMainFlow;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        MetaDto metaDto = (MetaDto) o;
        return Objects.equal(bridgeCode, metaDto.bridgeCode) && Objects.equal(sqn, metaDto.sqn) && Objects.equal(specName, metaDto.specName)
                && Arrays.equals(origin, metaDto.origin) && Objects.equal(units, metaDto.units) && Objects.equal(partition, metaDto.partition)
                && Objects.equal(numPartitions, metaDto.numPartitions) && Objects.equal(reblinked, metaDto.reblinked) && Objects.equal(outOfOrder,
                metaDto.outOfOrder)
                && Objects.equal(newBlink, metaDto.newBlink);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(bridgeCode, sqn, specName, origin, units, partition, numPartitions, reblinked, outOfOrder, newBlink);
    }
}
