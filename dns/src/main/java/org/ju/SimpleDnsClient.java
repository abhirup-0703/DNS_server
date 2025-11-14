package org.ju;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.ju.model.DnsHeader;
import org.ju.model.DnsMessage;
import org.ju.model.DnsQuestion;
import org.ju.model.DnsResourceRecord;
import org.ju.model.DnsType;
import org.ju.util.DnsMessageCodec;

/**
 * Handles the core logic of DNS iterative resolution.
 */
public class SimpleDnsClient {

    private static final int ROOT_SERVER_PORT = 5000;
    private static final String ROOT_SERVER_IP = "127.0.0.1";
    private static final DnsMessageCodec codec = new DnsMessageCodec();
    private static final Random random = new Random();

    public interface LogCallback {
        void onStepComplete(int step, String serverQueried, String logMessage);
    }

    public String resolve(String domain, LogCallback logger) throws Exception {
        String targetDomain = domain.endsWith(".") ? domain : domain + ".";
        String currentNameServerIp = ROOT_SERVER_IP;
        int nextPort = ROOT_SERVER_PORT;
        
        int loopLimit = 10;
        int stepCounter = 1; // Start at step 1

        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(2000); // 2-second timeout

            while (loopLimit > 0) {
                String serverToQuery = currentNameServerIp + ":" + nextPort;
                String logMessage; // This will hold the result of the query

                // 1. Build Query
                DnsMessage query = buildQuery(targetDomain);
                byte[] qBytes = codec.encode(query);

                // 2. Send
                InetAddress ip = InetAddress.getByName(currentNameServerIp);
                DatagramPacket packet = new DatagramPacket(qBytes, qBytes.length, ip, nextPort);
                socket.send(packet);

                // 3. Receive
                byte[] buf = new byte[512];
                DatagramPacket responsePacket = new DatagramPacket(buf, buf.length);
                
                try {
                    socket.receive(responsePacket);
                } catch (SocketTimeoutException e) {
                    logMessage = "ERROR: Server timed out.";
                    logger.onStepComplete(stepCounter++, serverToQuery, logMessage);
                    throw new Exception(logMessage);
                }

                DnsMessage response = codec.decode(responsePacket.getData());

                // 4. Analyze Response and set logMessage
                if (response.getAnswers() != null && !response.getAnswers().isEmpty()) {
                    // --- SUCCESS CASE ---
                    DnsResourceRecord ans = response.getAnswers().get(0);
                    String ipAddr = InetAddress.getByAddress(ans.getRData()).getHostAddress();
                    logMessage = "SUCCESS: Found A-Record for " + ans.getName() + " -> " + ipAddr;
                    
                    logger.onStepComplete(stepCounter++, serverToQuery, logMessage);
                    return ipAddr; // Success!
                    
                } else if (response.getAuthorities() != null && !response.getAuthorities().isEmpty()) {
                    DnsResourceRecord nsRecord = response.getAuthorities().get(0);
                    
                    byte[] rData = nsRecord.getRData();
                    byte[] rDataIP = new byte[]{rData[0], rData[1], rData[2], rData[3]};
                    
                    int portByte1 = rData[4] & 0xFF; 
                    int portByte2 = rData[5] & 0xFF;
                    int referredPort = (portByte1 << 8) | portByte2;

                    String nsIp = InetAddress.getByAddress(rDataIP).getHostAddress();
                    String nsZone = nsRecord.getName();

                    logMessage = "REFERRAL: Go to zone " + nsZone + " @ " + nsIp + ":" + referredPort;
                    
                    // Log *this* step before updating for the *next* step
                    logger.onStepComplete(stepCounter++, serverToQuery, logMessage);

                    // Update targets for the next loop
                    currentNameServerIp = nsIp;
                    nextPort = referredPort;
                    
                } else {
                    // --- NOT FOUND CASE ---
                    logMessage = "ERROR: Server returned NXDOMAIN (No such domain) or No Data.";
                    logger.onStepComplete(stepCounter++, serverToQuery, logMessage);
                    throw new Exception(logMessage);
                }
                loopLimit--;
            }
        }
        
        throw new Exception("Error: Loop limit reached. Possible circular reference.");
    }

    private DnsMessage buildQuery(String domain) {
        DnsHeader header = new DnsHeader(random.nextInt(0xFFFF), 0x0100, 1, 0, 0, 0);
        DnsQuestion q = new DnsQuestion(domain, DnsType.A, 1);
        return new DnsMessage(header, List.of(q), Collections.emptyList(), Collections.emptyList());
    }
}