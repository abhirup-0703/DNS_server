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
    private final DnsRecordStore recordStore; // Remove initialization here
    private static final DnsMessageCodec codec = new DnsMessageCodec();

    public SimpleDnsServer(int port, String name){
        DNS_PORT = port;
        this.name = name;
        // Pass the port to the DnsRecordStore constructor
        this.recordStore = new DnsRecordStore(port); 
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
        LOGGER.info("Starting Persistent DNS Server ({}) on port {}...", this.name, DNS_PORT);

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
            LOGGER.error("Server on port {} failed to start: {}", DNS_PORT, e.getMessage());
        }
    }

    private DnsMessage processQuery(DnsMessage query) {
        DnsQuestion question = query.getQuestions().get(0);
        String requestedDomain = question.getQName();
        
        LOGGER.info("[{}:{}] Received Query for: {}", this.name, DNS_PORT, requestedDomain);

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
                answers.addAll(foundRecords);
                flags |= 0x0400; // AA=1 (Authoritative Answer)
                LOGGER.info("  -> Found Exact A-Record match. Sending Answer.");
            } 
            else if (firstRec.getType() == DnsType.NS) {
                authorities.addAll(foundRecords);
                LOGGER.info("  -> Found Referral (NS) for zone: {}. Sending Authority.", firstRec.getName());
            } 
            else {
                LOGGER.warn("  -> Found record, but not A or NS? Type: {}", DnsType.toString(firstRec.getType()));
                rcode = 3; // NXDOMAIN
            }
        } else {
            // 4. Totally Unknown
            LOGGER.info("  -> No record found. Sending NXDOMAIN.");
            rcode = 3; // NXDOMAIN
        }

        flags |= (rcode & 0xF);

        DnsHeader header = new DnsHeader(query.getHeader().getId(), flags, 1, answers.size(), authorities.size(), 0);
        
        return new DnsMessage(header, List.of(question), answers, authorities);
    }

    public void addRecord(String name, int type, String dataIp, int port){
        try{
            recordStore.addRecord(name, type, dataIp, port);
        }
        catch(Exception e){
            LOGGER.error("Failed to add NS record: {}", e.getMessage());
        }
    }

    public void addRecord(String name, int type, String dataIp){
        try{
            recordStore.addRecord(name, type, dataIp);
        }
        catch(Exception e){
            LOGGER.error("Failed to add A record: {}", e.getMessage());
        }
    }

    public static void main(String[] args) {
        int port = 5000;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                LOGGER.error("Invalid port specified, using default 5000.");
            }
        }
        SimpleDnsServer server = new SimpleDnsServer(port, "DefaultServer-" + port);
        server.start();
    }
}