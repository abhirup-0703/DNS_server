package org.ju;

import org.ju.model.*;
import org.ju.util.DnsMessageCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * A simple DNS client to test our SimpleDnsServer.
 * Sends a query to localhost:5353.
 */
public class SimpleDnsClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleDnsClient.class);
    private static final String SERVER_ADDRESS = "127.0.0.1";
    private static final int SERVER_PORT = 5353;
    private static final int CLIENT_TIMEOUT_MS = 5000; // 5 seconds

    private static final DnsMessageCodec codec = new DnsMessageCodec();
    private static final Random random = new Random();

    public static void main(String[] args) {
        if (args.length == 0) {
            LOGGER.error("Usage: SimpleDnsClient <domain.name.to.query>");
            System.err.println("Usage: SimpleDnsClient <domain.name.to.query>");
            System.exit(1);
        }

        // DNS domain names must be fully qualified (end with a dot)
        String domainToQuery = args[0].endsWith(".") ? args[0] : args[0] + ".";

        LOGGER.info("Querying for: {}", domainToQuery);

        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(CLIENT_TIMEOUT_MS);

            // 1. Build the Query Message
            DnsMessage queryMessage = buildQuery(domainToQuery);

            // 2. Encode the query
            byte[] queryBytes = codec.encode(queryMessage);

            // 3. Send the query
            InetAddress serverAddr = InetAddress.getByName(SERVER_ADDRESS);
            DatagramPacket queryPacket = new DatagramPacket(queryBytes, queryBytes.length, serverAddr, SERVER_PORT);
            socket.send(queryPacket);
            LOGGER.info("Query sent to {}:{}", SERVER_ADDRESS, SERVER_PORT);

            // 4. Wait for the response
            byte[] responseBuffer = new byte[512];
            DatagramPacket responsePacket = new DatagramPacket(responseBuffer, responseBuffer.length);
            socket.receive(responsePacket);

            // 5. Decode the response
            DnsMessage response = codec.decode(responsePacket.getData());
            LOGGER.info("Received response: {}", response);

            // 6. Process and print the response
            processResponse(response);

        } catch (SocketTimeoutException e) {
            LOGGER.error("Query timed out. No response from {}:{}", SERVER_ADDRESS, SERVER_PORT);
        } catch (IOException e) {
            LOGGER.error("Network error", e);
        }
    }

    private static DnsMessage buildQuery(String domainName) {
        // --- Header ---
        int id = random.nextInt(0xFFFF); // Random 16-bit ID
        int flags = 0x0100; // Standard query (QR=0, RD=1)
        int qdCount = 1;
        int anCount = 0;
        int nsCount = 0;
        int arCount = 0;
        DnsHeader header = new DnsHeader(id, flags, qdCount, anCount, nsCount, arCount);

        // --- Question ---
        DnsQuestion question = new DnsQuestion(domainName, DnsType.A, DnsClass.IN);
        List<DnsQuestion> questions = List.of(question);

        // --- Answers (empty for a query) ---
        List<DnsResourceRecord> answers = Collections.emptyList();

        return new DnsMessage(header, questions, answers);
    }

    private static void processResponse(DnsMessage response) {
        // Check RCODE in flags (last 4 bits)
        int rcode = response.getHeader().getFlags() & 0x0F;
        if (rcode == 3) {
            System.err.println("Error: Domain not found (NXDOMAIN)");
            return;
        } else if (rcode != 0) {
            System.err.println("Error: Server returned RCODE " + rcode);
            return;
        }

        if (response.getAnswers().isEmpty()) {
            System.out.println("No answers found.");
            return;
        }

        // Print all answers
        System.out.println("Answers:");
        for (DnsResourceRecord record : response.getAnswers()) {
            // We only know how to parse A-records
            if (record.getType() == DnsType.A) {
                try {
                    String ip = InetAddress.getByAddress(record.getRData()).getHostAddress();
                    System.out.printf("  %s \t%d \t%s \t%s\n",
                            record.getName(),
                            record.getTtl(),
                            DnsClass.toString(record.getRClass()),
                            ip);
                } catch (UnknownHostException e) {
                    LOGGER.error("Failed to parse IP from RDATA", e);
                }
            }
        }
    }
}