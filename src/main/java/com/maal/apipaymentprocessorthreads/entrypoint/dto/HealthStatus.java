package com.maal.apipaymentprocessorthreads.entrypoint.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class HealthStatus {
    private final boolean failing;
    private final int minResponseTime;

    @JsonCreator
    public HealthStatus(@JsonProperty("failing") boolean failing, 
                       @JsonProperty("minResponseTime") int minResponseTime) {
        this.failing = failing;
        this.minResponseTime = minResponseTime;
    }

    public boolean failing() {
        return failing;
    }

    public int minResponseTime() {
        return minResponseTime;
    }

    @Override
    public String toString() {
        return "HealthStatus[failing=" + failing + ", minResponseTime=" + minResponseTime + "]";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        HealthStatus that = (HealthStatus) obj;
        return failing == that.failing && minResponseTime == that.minResponseTime;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(failing, minResponseTime);
    }
}
