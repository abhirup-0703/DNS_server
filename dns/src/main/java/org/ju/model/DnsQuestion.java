package org.ju.model;


public class DnsQuestion {

    private final String qName;

    private final int qType;

    private final int qClass;

    public DnsQuestion(String qName, int qType, int qClass) {
        this.qName = qName;
        this.qType = qType;
        this.qClass = qClass;
    }

    // --- Getters ---

    public String getQName() {
        return qName;
    }

    public int getQType() {
        return qType;
    }

    public int getQClass() {
        return qClass;
    }

    @Override
    public String toString() {
        return "DnsQuestion{" +
                "qName='" + qName + '\'' +
                ", qType=" + DnsType.toString(qType) +
                ", qClass=" + DnsClass.toString(qClass) +
                '}';
    }
}