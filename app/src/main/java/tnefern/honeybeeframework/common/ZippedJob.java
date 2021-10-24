package tnefern.honeybeeframework.common;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ZippedJob {


    private final String zipName;
    private final List<String> filesInZip;
    private final long zipStartTime;
    private final long zipEndTime;
    private final long averageZipTime;
    private long transmitStartTime;
    private long transmitEndTime;
    private long averageTransmissionTime;

    public ZippedJob(String zipName, File[] filesInZip, long zipStartTime, long zipEndTime) {
        this.zipName = zipName;
        this.filesInZip = new ArrayList<>();
        for (File file : filesInZip) {
            this.filesInZip.add(file.getName());
        }
        this.zipStartTime = zipStartTime;
        this.zipEndTime = zipEndTime;
        this.averageZipTime = (zipEndTime - zipStartTime) / filesInZip.length;
    }

    public String getZipName() {
        return zipName;
    }

    public List<String> getFilesInZip() {
        return filesInZip;
    }

    public long getZipStartTime() {
        return zipStartTime;
    }

    public long getZipEndTime() {
        return zipEndTime;
    }

    public long getAverageZipTime() {
        return averageZipTime;
    }

    public long getTransmitStartTime() {
        return transmitStartTime;
    }

    public void setTransmitStartTime(long transmitStartTime) {
        this.transmitStartTime = transmitStartTime;
    }

    public long getTransmitEndTime() {
        return transmitEndTime;
    }

    public void setTransmitEndTime(long transmitEndTime) {
        this.transmitEndTime = transmitEndTime;
        this.averageTransmissionTime = (transmitEndTime - transmitStartTime) / filesInZip.size();
    }

    public long getAverageTransmissionTime() {
        return averageTransmissionTime;
    }
}
