package org.ju.model;

public class DnsHeader {

    private final int id;        // Transaction ID
    private final int flags;     // Flags (QR, OpCode, AA, TC, RD, RA, Z, RCODE)
    private final int qdCount;   // Question Count
    private final int anCount;   // Answer Record Count
    private final int nsCount;   // Authority Record Count
    private final int arCount;   // Additional Record Count

    public DnsHeader(int id, int flags, int qdCount, int anCount, int nsCount, int arCount) {
        this.id = id;
        this.flags = flags;
        this.qdCount = qdCount;
        this.anCount = anCount;
        this.nsCount = nsCount;
        this.arCount = arCount;
    }

    // --- Getters ---

    public int getId() {
        return id;
    }

    public int getFlags() {
        return flags;
    }

    public int getQdCount() {
        return qdCount;
    }

    public int getAnCount() {
        return anCount;
    }

    public int getNsCount() {
        return nsCount;
    }

    public int getArCount() {
        return arCount;
    }

    @Override
    public String toString() {
        return "DnsHeader{" +
                "id=" + id +
                ", flags=0x" + Integer.toHexString(flags) +
                ", qdCount=" + qdCount +
                ", anCount=" + anCount +
                ", nsCount=" + nsCount +
                ", arCount=" + arCount +
                '}';
    }
}