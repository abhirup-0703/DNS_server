package org.ju;

import org.ju.model.*;
import org.ju.util.DnsMessageCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * The main DNS Server class.
 * Listens on UDP port 5353 and responds to A-record queries.
 */
public class SimpleDnsServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleDnsServer.class);
    private static final int DNS_PORT = 5353;
    private static final int BUFFER_SIZE = 512; // Standard DNS UDP buffer size

    // Our encoder/decoder
    private static final DnsMessageCodec codec = new DnsMessageCodec();

    public static void main(String[] args) {
        LOGGER.info("Starting Simple DNS Server on UDP port {}...", DNS_PORT);

        try (DatagramSocket socket = new DatagramSocket(DNS_PORT)) {
            while (true) {
                // 1. Wait for an incoming packet
                byte[] inBuffer = new byte[BUFFER_SIZE];
                DatagramPacket inPacket = new DatagramPacket(inBuffer, inBuffer.length);
                socket.receive(inPacket); // Blocks until a packet arrives

                InetAddress clientAddress = inPacket.getAddress();
                int clientPort = inPacket.getPort();
                LOGGER.info("Received query from {}:{}", clientAddress.getHostAddress(), clientPort);

                try {
                    // 2. Decode the query
                    DnsMessage query = codec.decode(inPacket.getData());
                    LOGGER.info("Query: {}", query);

                    // 3. Process the query and build a response
                    DnsMessage response = processQuery(query);

                    // 4. Encode the response
                    byte[] outBuffer = codec.encode(response);

                    // 5. Send the response
                    DatagramPacket outPacket = new DatagramPacket(outBuffer, outBuffer.length, clientAddress, clientPort);
                    socket.send(outPacket);
                    LOGGER.info("Sent response to {}:{}", clientAddress.getHostAddress(), clientPort);

                } catch (Exception e) {
                    LOGGER.error("Failed to process query from {}:{}", clientAddress.getHostAddress(), clientPort, e);
                }
            }
        } catch (IOException e) {
            LOGGER.error("Could not start server on port {}", DNS_PORT, e);
        }
    }

    /**
     * Creates a DnsMessage response based on the query.
     */
    private static DnsMessage processQuery(DnsMessage query) {
        // Get the first (and likely only) question
        DnsQuestion question = query.getQuestions().get(0);

        // --- 1. Prepare Response Header ---
        int queryId = query.getHeader().getId();
        int flags = 0;
        // QR=1 (Response), AA=1 (Authoritative), RD=0 (Recursion Not Desired)
        // RCODE will be 0 (NoError) or 3 (NXDOMAIN)
        flags |= (1 << 15); // QR: 1 (Response)
        flags |= (1 << 10); // AA: 1 (Authoritative)

        int qdCount = 1;
        int anCount = 0; // We'll set this to 1 if we find a record
        int nsCount = 0;
        int arCount = 0;

        List<DnsResourceRecord> answers = new ArrayList<>();

        // --- 2. Find the Answer ---
        // We only support A-records
        if (question.getQType() == DnsType.A && question.getQClass() == DnsClass.IN) {
            byte[] ipBytes = DnsRecordStore.getIpAddress(question.getQName());

            if (ipBytes != null) {
                // We found a record!
                anCount = 1; // We have one answer
                DnsResourceRecord answer = new DnsResourceRecord(
                        question.getQName(),
                        DnsType.A,
                        DnsClass.IN,
                        3600, // TTL (1 hour)
                        ipBytes.length,
                        ipBytes
                );
                answers.add(answer);
                LOGGER.info("Found record for {}: {}", question.getQName(), DnsRecordStore.getIpAddress(question.getQName()));
            } else {
                // Not found: Set RCODE to 3 (NXDOMAIN)
                LOGGER.warn("No record found for {}", question.getQName());
                flags |= 3; // RCODE 3: Name Error (NXDOMAIN)
            }
        } else {
            // Not implemented: Set RCODE to 4 (Not Implemented)
            LOGGER.warn("Unsupported query type {} or class {}",
                    DnsType.toString(question.getQType()), DnsClass.toString(question.getQClass()));
            flags |= 4; // RCODE 4: Not Implemented
        }

        // --- 3. Build the final message ---
        DnsHeader responseHeader = new DnsHeader(queryId, flags, qdCount, anCount, nsCount, arCount);
        // The response must include the original question
        List<DnsQuestion> questions = List.of(question);

        return new DnsMessage(responseHeader, questions, answers);
    }
}