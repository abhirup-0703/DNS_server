package org.ju;

import org.ju.model.*;
import org.ju.util.DnsMessageCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.List;

public class SimpleDnsServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleDnsServer.class);
    private static final int DNS_PORT = 5354;
    private static final DnsMessageCodec codec = new DnsMessageCodec();

    public static void main(String[] args) {
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
                } catch (Exception e) {
                    LOGGER.error("Error processing packet", e);
                }
            }
        } catch (IOException e) {
            LOGGER.error("Server error", e);
        }
    }

    private static DnsMessage processQuery(DnsMessage query) {
        DnsQuestion question = query.getQuestions().get(0);
        String requestedDomain = question.getQName();
        
        LOGGER.info("Received Query for: {}", requestedDomain);

        // Look for the best match in our "Zone" store
        List<DnsResourceRecord> foundRecords = DnsRecordStore.findClosestMatch(requestedDomain);

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
}