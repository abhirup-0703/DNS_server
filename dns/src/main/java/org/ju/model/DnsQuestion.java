package org.ju.model;


public class DnsQuestion {

    // e.g., "www.example.com"
    private final String qName;

    // e.g., 1 for "A" record (DnsType.A)
    private final int qType;

    // e.g., 1 for "IN" class (DnsClass.IN)
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