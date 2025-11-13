package org.ju;

import org.ju.model.*;
import org.ju.util.DnsMessageCodec;

import java.net.*;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

public class SimpleDnsClient {
    private static final int SERVER_PORT = 5354;
    private static final DnsMessageCodec codec = new DnsMessageCodec();
    private static final Random random = new Random();

    public static void main(String[] args) throws Exception {
        // if (args.length == 0) {
        //     System.out.println("Usage: java SimpleDnsClient <domain>");
        //     return;
        // }

        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter domain to resolve: ");
        String domain = scanner.nextLine();
        scanner.close();

        if (domain.isEmpty()) {
            System.out.println("Usage: java SimpleDnsClient <domain>");
            return;
        }

        String targetDomain = domain.endsWith(".") ? domain : domain + ".";
        
        // We start by querying the "Root" (which is just our localhost server in this sim)
        String currentNameServerIp = "127.0.0.1"; 
        boolean resolved = false;
        int loopLimit = 5; // Prevent infinite loops

        System.out.println("--- Starting Iterative Search for: " + targetDomain + " ---");

        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(2000);

            while (!resolved && loopLimit > 0) {
                System.out.println("\n[Step] Querying Server: " + currentNameServerIp);

                // 1. Build Query
                DnsMessage query = buildQuery(targetDomain);
                byte[] qBytes = codec.encode(query);

                // 2. Send
                InetAddress ip = InetAddress.getByName(currentNameServerIp);
                DatagramPacket packet = new DatagramPacket(qBytes, qBytes.length, ip, SERVER_PORT);
                socket.send(packet);

                // 3. Receive
                byte[] buf = new byte[512];
                DatagramPacket responsePacket = new DatagramPacket(buf, buf.length);
                try {
                    socket.receive(responsePacket);
                } catch (SocketTimeoutException e) {
                    System.out.println("timeout.");
                    break;
                }

                DnsMessage response = codec.decode(responsePacket.getData());

                // 4. Analyze Response
                if (response.getAnswers().size() > 0) {
                    // --- SUCCESS CASE ---
                    System.out.println("-> STATUS: FOUND A-RECORD!");
                    for (DnsResourceRecord ans : response.getAnswers()) {
                        String ipAddr = InetAddress.getByAddress(ans.getRData()).getHostAddress();
                        System.out.println("   " + ans.getName() + " maps to " + ipAddr);
                    }
                    resolved = true;
                } 
                else if (response.getAuthorities() != null && !response.getAuthorities().isEmpty()) {
                    // --- REFERRAL CASE ---
                    DnsResourceRecord nsRecord = response.getAuthorities().get(0);
                    String nsIp = InetAddress.getByAddress(nsRecord.getRData()).getHostAddress();
                    String nsZone = nsRecord.getName();
                    
                    System.out.println("-> STATUS: REFERRAL (Delegation)");
                    System.out.println("   Server doesn't know answer, but referred us to Zone: " + nsZone);
                    System.out.println("   Next Name Server IP: " + nsIp);
                    
                    // UPDATE TARGET IP FOR NEXT LOOP
                    currentNameServerIp = nsIp; 
                    
                } else {
                    System.out.println("-> STATUS: NXDOMAIN / No Data");
                    break;
                }
                loopLimit--;
            }
        }
    }

    private static DnsMessage buildQuery(String domain) {
        DnsHeader header = new DnsHeader(random.nextInt(0xFFFF), 0x0100, 1, 0, 0, 0);
        DnsQuestion q = new DnsQuestion(domain, DnsType.A, 1);
        return new DnsMessage(header, List.of(q), Collections.emptyList(), Collections.emptyList());
    }
}