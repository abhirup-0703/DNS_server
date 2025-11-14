package org.ju;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.ju.model.DnsType;

/**
 * A GUI dashboard to manage all SimpleDnsServer instances.
 * This class replaces the old God.java.
 */
public class God extends JFrame {

    // --- Core Server State ---
    private final List<SimpleDnsServer> servers = new ArrayList<>();
    private final List<Thread> serverThreads = new ArrayList<>();
    
    // --- GUI Components ---
    private final JTextArea logArea;
    private final JButton startPredefinedButton;
    private final JComboBox<SimpleDnsServer> serverSelectorCombo;
    private final DefaultComboBoxModel<SimpleDnsServer> serverComboModel;
    
    private final JComboBox<String> recordTypeCombo;
    private final JTextField recordNameField;
    private final JTextField recordIpField;
    private final JTextField recordPortField;
    private final JLabel recordPortLabel;
    
    private final JTextField newServerNameField;
    private final JTextField newServerPortField;

    public God() {
        setTitle("God (Server Manager Dashboard)");
        setSize(700, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(5, 5));

        // 1. --- Main Controls Panel (GridBagLayout) ---
        JPanel controlsPanel = new JPanel(new GridBagLayout());
        controlsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // --- Section 1: Start Predefined Servers ---
        startPredefinedButton = new JButton("Start All Predefined Servers");
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2; // Span 2 columns
        controlsPanel.add(startPredefinedButton, gbc);

        gbc.gridwidth = 1; // Reset span
        gbc.gridy = 1;
        controlsPanel.add(new JSeparator(), gbc);
        controlsPanel.add(new JSeparator(), gbc);

        // --- Section 2: Add Records ---
        gbc.gridy = 2;
        gbc.gridx = 0;
        controlsPanel.add(new JLabel("Add Record to Server:"), gbc);
        
        gbc.gridx = 1;
        serverComboModel = new DefaultComboBoxModel<>();
        serverSelectorCombo = new JComboBox<>(serverComboModel);
        controlsPanel.add(serverSelectorCombo, gbc);

        gbc.gridy = 3;
        gbc.gridx = 0;
        controlsPanel.add(new JLabel("Record Name:"), gbc);
        gbc.gridx = 1;
        recordNameField = new JTextField("www.newzone.com.");
        controlsPanel.add(recordNameField, gbc);

        gbc.gridy = 4;
        gbc.gridx = 0;
        controlsPanel.add(new JLabel("Record Type:"), gbc);
        gbc.gridx = 1;
        recordTypeCombo = new JComboBox<>(new String[]{"A", "NS"});
        controlsPanel.add(recordTypeCombo, gbc);

        gbc.gridy = 5;
        gbc.gridx = 0;
        controlsPanel.add(new JLabel("Data (IP):"), gbc);
        gbc.gridx = 1;
        recordIpField = new JTextField("10.0.0.1");
        controlsPanel.add(recordIpField, gbc);

        gbc.gridy = 6;
        gbc.gridx = 0;
        recordPortLabel = new JLabel("Data (Port):");
        controlsPanel.add(recordPortLabel, gbc);
        gbc.gridx = 1;
        recordPortField = new JTextField("5010");
        controlsPanel.add(recordPortField, gbc);
        
        // Hide port field initially
        recordPortLabel.setEnabled(false);
        recordPortField.setEnabled(false);

        gbc.gridy = 7;
        gbc.gridx = 1; // Align button to the right
        JButton addRecordButton = new JButton("Add Record");
        controlsPanel.add(addRecordButton, gbc);
        
        gbc.gridy = 8;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        controlsPanel.add(new JSeparator(), gbc);

        // --- Section 3: Create New Server ---
        gbc.gridy = 9;
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        controlsPanel.add(new JLabel("New Server Name (Zone):"), gbc);
        gbc.gridx = 1;
        newServerNameField = new JTextField("newzone.com.");
        controlsPanel.add(newServerNameField, gbc);

        gbc.gridy = 10;
        gbc.gridx = 0;
        controlsPanel.add(new JLabel("New Server Port:"), gbc);
        gbc.gridx = 1;
        newServerPortField = new JTextField("5010");
        controlsPanel.add(newServerPortField, gbc);

        gbc.gridy = 11;
        gbc.gridx = 1; // Align button to the right
        JButton createServerButton = new JButton("Create & Start Server");
        controlsPanel.add(createServerButton, gbc);

        // Add the main controls to the frame
        add(controlsPanel, BorderLayout.NORTH);

        // 2. --- Log Area ---
        logArea = new JTextArea();
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Server Logs"));
        add(scrollPane, BorderLayout.CENTER);

        // 3. --- Attach Action Listeners ---
        startPredefinedButton.addActionListener(e -> startPredefinedServers());
        addRecordButton.addActionListener(e -> addNewRecord());
        createServerButton.addActionListener(e -> createNewServer());

        // Show/hide port field based on record type
        recordTypeCombo.addActionListener(e -> {
            boolean isNs = "NS".equals(recordTypeCombo.getSelectedItem());
            recordPortLabel.setEnabled(isNs);
            recordPortField.setEnabled(isNs);
        });

        setLocationByPlatform(true);
        setVisible(true);
    }

    /**
     * Helper method to log to the GUI's text area.
     */
    private void log(String message) {
        // Ensure log updates happen on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    /**
     * Core logic to create a server, add it to lists, and update the GUI.
     * Does NOT start the thread.
     */
    private SimpleDnsServer addServer(int port, String name) {
        SimpleDnsServer server = new SimpleDnsServer(port, name);
        servers.add(server);
        serverThreads.add(new Thread(server::start));
        
        // Update the JComboBox
        serverComboModel.addElement(server);
        
        log("Created server: " + server);
        return server;
    }

    /**
     * ACTION: Creates and starts the 6 default servers and records.
     */
    private void startPredefinedServers() {
        log("--- Creating predefined servers... ---");
        
        // 1. Create servers
        SimpleDnsServer root = addServer(5000, ".");
        SimpleDnsServer com = addServer(5001, "com.");
        SimpleDnsServer org = addServer(5002, "org.");
        SimpleDnsServer google = addServer(5003, "google.com.");
        SimpleDnsServer example = addServer(5004, "example.com.");
        SimpleDnsServer test = addServer(5005, "test.org.");

        // 2. Add records
        log("Adding records to servers...");
        root.addRecord("com.", DnsType.NS, "127.0.0.1", 5001);
        root.addRecord("org.", DnsType.NS, "127.0.0.1", 5002);
        
        com.addRecord("google.com.", DnsType.NS, "127.0.0.1", 5003);
        com.addRecord("example.com.", DnsType.NS, "127.0.0.1", 5004);
        
        org.addRecord("test.org.", DnsType.NS, "127.0.0.1", 5005);
        
        google.addRecord("www.google.com.", DnsType.A, "8.8.8.8");
        
        example.addRecord("www.example.com.", DnsType.A, "192.168.1.10");
        example.addRecord("mail.example.com.", DnsType.A, "192.168.1.20");
        
        test.addRecord("www.test.org.", DnsType.A, "10.0.0.50");

        // 3. Start all threads
        log("Starting all server threads...");
        for (Thread t : serverThreads) {
            t.start();
        }
        
        log("--- All predefined servers are running! ---");
        startPredefinedButton.setEnabled(false); // Only run once
    }

    /**
     * ACTION: Creates and starts a single new server.
     */
    private void createNewServer() {
        try {
            String name = newServerNameField.getText().trim();
            int port = Integer.parseInt(newServerPortField.getText().trim());
            
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Server Name cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Create the server and add it to lists
            SimpleDnsServer newServer = addServer(port, name);
            
            // Start the *new* server's thread
            // It's the last one added to the list
            serverThreads.get(serverThreads.size() - 1).start();
            
            log("--- New server " + newServer + " is running! ---");

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid Port Number.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * ACTION: Adds a new A or NS record to the selected server.
     */
    private void addNewRecord() {
        SimpleDnsServer selectedServer = (SimpleDnsServer) serverSelectorCombo.getSelectedItem();
        if (selectedServer == null) {
            JOptionPane.showMessageDialog(this, "No server selected.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            String name = recordNameField.getText().trim();
            String ip = recordIpField.getText().trim();
            String type = (String) recordTypeCombo.getSelectedItem();
            
            if (name.isEmpty() || ip.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Record Name and IP cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if ("A".equals(type)) {
                selectedServer.addRecord(name, DnsType.A, ip);
                log("Added A record to " + selectedServer.getName() + ": " + name + " -> " + ip);
            } else if ("NS".equals(type)) {
                int port = Integer.parseInt(recordPortField.getText().trim());
                selectedServer.addRecord(name, DnsType.NS, ip, port);
                log("Added NS record to " + selectedServer.getName() + ": " + name + " -> " + ip + ":" + port);
            }

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid Port Number for NS record.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | UnsupportedLookAndFeelException e) {
            System.err.println("Nimbus Look and Feel not available, using default.");
        }
        
        SwingUtilities.invokeLater(God::new);
    }
}