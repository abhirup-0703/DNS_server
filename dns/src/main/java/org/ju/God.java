package org.ju;

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
    public static void main(String[] args){
        addServer(5000, ".");
        addServer(5001, "com.");
        addServer(5002, "org.");
        addServer(5003, "google.com.");
        addServer(5004, "example.com.");
        addServer(5005, "test.org.");

        servers.get(3).addRecord("www.google.com", DnsType.A, "8.8.8.8");
        servers.get(4).addRecord("www.example.com", DnsType.A, "192.168.1.10");
        servers.get(5).addRecord("mail.example.com", DnsType.A, "192.168.1.20");
        servers.get(5).addRecord("www.test.org", DnsType.A, "10.0.0.50");
    }
}
