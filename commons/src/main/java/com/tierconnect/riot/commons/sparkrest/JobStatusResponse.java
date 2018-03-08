package com.tierconnect.riot.commons.sparkrest;

/**
 * SparkResponse class.
 */
class JobStatusResponse extends SparkResponse {

    private DriverState driverState;
    private String workerHostPort;
    private String workerId;

    /**
     * Gets the driver state.
     *
     * @return the driver state
     */
    public DriverState getDriverState() {
        return driverState;
    }

    /**
     * Gets the worker host port.
     *
     * @return the worker host and port
     */
    public String getWorkerHostPort() {
        return workerHostPort;
    }

    /**
     * Gets the worker id.
     *
     * @return the worker id
     */
    public String getWorkerId() {
        return workerId;
    }
}
