package org.ju;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.ju.model.DnsType;

public class God {
    private final static List<SimpleDnsServer> servers = new ArrayList<>();
    private final static List<Thread> serverThreads = new ArrayList<>();

    private static void addServer(int port, String name){
        SimpleDnsServer server = new SimpleDnsServer(port, name);
        servers.add(server);
        serverThreads.add(new Thread(()->{server.start();}));
    }

    private static void startServers(){
        for(Thread t : serverThreads){
            t.start();
        }
    }

    public static void main(String[] args){
        addServer(5000, ".");
        addServer(5001, "com.");
        addServer(5002, "org.");
        addServer(5003, "google.com.");
        addServer(5004, "example.com.");
        addServer(5005, "test.org.");

        servers.get(0).addRecord("com.", DnsType.NS, "127.0.0.1", 5001);
        servers.get(0).addRecord("org.", DnsType.NS, "127.0.0.1", 5002);

        servers.get(1).addRecord("google.com.", DnsType.NS, "127.0.0.1", 5003);
        servers.get(1).addRecord("example.com.", DnsType.NS, "127.0.0.1", 5004);

        servers.get(2).addRecord("test.org.", DnsType.NS, "127.0.0.1", 5005);

        servers.get(3).addRecord("www.google.com.", DnsType.A, "8.8.8.8");
        servers.get(4).addRecord("www.example.com.", DnsType.A, "192.168.1.10");
        servers.get(4).addRecord("mail.example.com.", DnsType.A, "192.168.1.20");
        servers.get(5).addRecord("www.test.org.", DnsType.A, "10.0.0.50");

        startServers();
        System.out.println("[GOD] All DNS servers started.");

        System.out.println("[GOD] Press Enter to exit...");
        try {
            System.in.read();
            for(Thread t : serverThreads){
                t.interrupt();
                t.join();
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
