package com.tierconnect.riot.commons.dao.mongo;

import com.tierconnect.riot.commons.ViZixThreadGroup;
import org.apache.log4j.Logger;

import java.util.concurrent.PriorityBlockingQueue;

/**
 * Created by cvertiz on 9/10/16.
 */

public class SnapshotFixerJobCommons extends Thread {

    private static Logger logger = Logger.getLogger(SnapshotFixerJobCommons.class);

    private PriorityBlockingQueue<SnapshotFixEntryCommons> queue;

    private int zoneTypeCount;
    private int zoneGroupCount;
    private int facilityMapCount;

    public SnapshotFixerJobCommons(PriorityBlockingQueue<SnapshotFixEntryCommons> queue, String name, int zoneTypeCount, int zoneGroupCount, int facilityMapCount) {
        super(workerThreads, name);
        this.queue = queue;
        this.setName(name);
        this.zoneGroupCount = zoneGroupCount;
        this.zoneTypeCount = zoneTypeCount;
        this.facilityMapCount = facilityMapCount;

    }

    public static final ThreadGroup workerThreads =
            new ViZixThreadGroup("snapshotFixerThreads");

    @Override
    public void run() {
        logger.info("***** Job Started ");
        while (true) {
            SnapshotFixEntryCommons entry = null;
            try {
                entry = queue.take();
            } catch (InterruptedException e) {
                logger.warn("An exception occurred when getting an element from the queue.", e);
            }
            fixEntry(entry);
        }
    }

    private void fixEntry(SnapshotFixEntryCommons entry) {
        if (entry != null) {
            String serialNumber = entry.getSerialNumber();
            logger.warn("Fixing snapshot thing=" + serialNumber);
            entry.fixSnapshot(zoneTypeCount, zoneGroupCount, facilityMapCount);
            //Just in case
            entry = null;
            logger.warn("Finished fixing snapshot thing=" + serialNumber);
        }
    }

}
