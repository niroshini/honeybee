package tnefern.honeybeeframework.common;

import java.util.List;

public class InZippedJob {

    private final String zipName;
    private final long stealRequestTime;
    private final long jobReceivedStartTime;
    private final long jobReceivedEndTime;
    private long avgJobWaitTime;
    private long avgJobTransmissionTime;
    private List<String> filesInZip;
    private long unzipStartTime;
    private long unzipEndTime;

    public InZippedJob(String zipName, long stealRequestTime, long jobReceivedStartTime, long jobReceivedEndTime) {
        this.zipName = zipName;
        this.stealRequestTime = stealRequestTime;
        this.jobReceivedStartTime = jobReceivedStartTime;
        this.jobReceivedEndTime = jobReceivedEndTime;
    }

    public String getZipName() {
        return zipName;
    }

    public long getStealRequestTime() {
        return stealRequestTime;
    }

    public long getJobReceivedStartTime() {
        return jobReceivedStartTime;
    }

    public long getJobReceivedEndTime() {
        return jobReceivedEndTime;
    }

    public long getAvgJobWaitTime() {
        return avgJobWaitTime;
    }

    public long getAvgJobTransmissionTime() {
        return avgJobTransmissionTime;
    }

    public void setFilesInZip(List<String> filesInZip) {
        this.filesInZip = filesInZip;
        this.avgJobWaitTime = (getJobReceivedStartTime() - getStealRequestTime()) / filesInZip.size();
        this.avgJobTransmissionTime = (getJobReceivedEndTime() - getJobReceivedStartTime()) / filesInZip.size();
    }

    public List<String> getFilesInZip() {
        return filesInZip;
    }

    public void setUnzipStartTime(long unzipStartTime) {
        this.unzipStartTime = unzipStartTime;
    }

    public long getUnzipStartTime() {
        return unzipStartTime;
    }

    public void setUnzipEndTime(long unzipEndTime) {
        this.unzipEndTime = unzipEndTime;
    }

    public long getUnzipEndTime() {
        return unzipEndTime;
    }
}
