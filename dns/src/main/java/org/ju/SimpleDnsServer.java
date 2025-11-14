package org.ju;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.List;

import org.ju.model.DnsHeader;
import org.ju.model.DnsMessage;
import org.ju.model.DnsQuestion;
import org.ju.model.DnsResourceRecord;
import org.ju.model.DnsType;
import org.ju.util.DnsMessageCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleDnsServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleDnsServer.class);
    private final int DNS_PORT;
    private final String name;
    private final DnsRecordStore recordStore = new DnsRecordStore();
    private static final DnsMessageCodec codec = new DnsMessageCodec();

    public SimpleDnsServer(int port, String name){
        DNS_PORT = port;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public int getPort() {
        return DNS_PORT;
    }

    @Override
    public String toString() {
        return name + " (Port: " + DNS_PORT + ")";
    }

    public void start() {
        LOGGER.info("Starting Iterative DNS Server (Root/TLD/Auth Simulator) on port {}...", DNS_PORT);

        try (DatagramSocket socket = new DatagramSocket(DNS_PORT)) {
            while (true) {
                byte[] inBuffer = new byte[512];
                DatagramPacket inPacket = new DatagramPacket(inBuffer, inBuffer.length);
                socket.receive(inPacket);

                try {
                    DnsMessage query = codec.decode(inPacket.getData());
                    DnsMessage response = processQuery(query);
                    byte[] outBuffer = codec.encode(response);

                    DatagramPacket outPacket = new DatagramPacket(outBuffer, outBuffer.length, inPacket.getAddress(), inPacket.getPort());
                    socket.send(outPacket);
                } catch (IOException e) {
                    LOGGER.error("Error processing packet", e);
                }
            }
        } catch (IOException e) {
            LOGGER.error("Server error", e);
        }
    }

    private DnsMessage processQuery(DnsMessage query) {
        DnsQuestion question = query.getQuestions().get(0);
        String requestedDomain = question.getQName();
        
        LOGGER.info("Received Query for: {}", requestedDomain);

        //Look for the best match in our "Zone" store
        List<DnsResourceRecord> foundRecords = recordStore.findClosestMatch(requestedDomain);

        List<DnsResourceRecord> answers = new ArrayList<>();
        List<DnsResourceRecord> authorities = new ArrayList<>(); // For NS records
        int flags = 0x8000; // QR=1 (Response)
        int rcode = 0;

        if (foundRecords != null && !foundRecords.isEmpty()) {
            // Check what kind of records we found
            DnsResourceRecord firstRec = foundRecords.get(0);

            if (firstRec.getName().equals(requestedDomain) && firstRec.getType() == DnsType.A) {
                // 1. Exact Match (A Record) -> We are Authoritative
                answers.addAll(foundRecords);
                flags |= 0x0400; // AA=1 (Authoritative Answer)
                LOGGER.info("  -> Found Exact A-Record match.");
            } 
            else if (firstRec.getType() == DnsType.NS) {
                // 2. Found NS Record -> We are delegating (Referral)
                authorities.addAll(foundRecords);
                // AA flag is NOT set because we are referring, not answering.
                LOGGER.info("  -> Found Referral (NS) for zone: {}", firstRec.getName());
            } 
            else {
                // 3. Found something else (unlikely in this sim)
                rcode = 3; // NXDOMAIN
            }
        } else {
            // 4. Totally Unknown
            LOGGER.info("  -> No record found.");
            rcode = 3; // NXDOMAIN
        }

        flags |= (rcode & 0xF);

        DnsHeader header = new DnsHeader(query.getHeader().getId(), flags, 1, answers.size(), authorities.size(), 0);
        
        // Note: We pass 'authorities' list to the DnsMessage constructor now
        // You might need to update DnsMessage constructor to accept Authority list if you haven't yet.
        // Assuming DnsMessage constructor: (header, questions, answers, authorities, additionals)
        // If your DnsMessage only accepts answers, update DnsMessage.java (see below).
        return new DnsMessage(header, List.of(question), answers, authorities);
    }

    public void addRecord(String name, int type, String dataIp, int port){
        try{
            recordStore.addRecord(name, type, dataIp, port);
        }
        catch(Exception e){}
    }

    public void addRecord(String name, int type, String dataIp){
        try{
            recordStore.addRecord(name, type, dataIp);
        }
        catch(Exception e){}
    }

    public static void main(String[] args) {
        int port = 5000;
        SimpleDnsServer server = new SimpleDnsServer(port, "DefaultServer");
        server.start();
    }
}