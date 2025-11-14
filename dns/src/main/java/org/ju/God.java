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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List; // Add this import

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
        startPredefinedButton = new JButton("Start Servers From Storage"); // <-- Text Changed
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
        startPredefinedButton.addActionListener(e -> startServersFromStorage()); // <-- Method name changed
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
     * ACTION: Scans the 'dns_storage' directory and starts servers for any
     * existing .dns files.
     */
    private void startServersFromStorage() {
        log("--- Scanning 'dns_storage' for existing servers... ---");
        Path storageDir = Paths.get("dns_storage");

        if (!Files.exists(storageDir) || !Files.isDirectory(storageDir)) {
            log("INFO: 'dns_storage' directory not found. No servers to start.");
            // Button's job is done, even if it found nothing.
            startPredefinedButton.setEnabled(false);
            return;
        }

        int serversFound = 0;
        try (var stream = Files.list(storageDir)) {
            List<Path> files = stream
                                .filter(p -> p.getFileName().toString().endsWith(".dns"))
                                .toList();
            
            for (Path path : files) {
                String fileName = path.getFileName().toString();
                try {
                    String portStr = fileName.substring(0, fileName.length() - 4);
                    int port = Integer.parseInt(portStr);
                    
                    // We don't know the "name" (e.g., "google.com.") from the file,
                    // so we'll just name it based on its port.
                    String serverName = "Server-" + port; 
                    
                    // This helper adds to 'servers', 'serverThreads', and GUI combo box
                    addServer(port, serverName); 
                    serversFound++;
                    
                } catch (NumberFormatException e) {
                    log("Skipping invalid file name: " + fileName);
                }
            }
        } catch (IOException e) {
            log("Error reading dns_storage directory: " + e.getMessage());
            return;
        }

        if (serversFound == 0) {
            log("No .dns files found in storage. No servers started.");
        } else {
            // 3. Start all threads
            log("Starting all " + serversFound + " server threads...");
            for (Thread t : serverThreads) {
                t.start();
            }
            log("--- All " + serversFound + " stored servers are running! ---");
        }
        
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