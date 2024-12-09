import java.io.*;
import java.net.*;
import java.util.List;
import java.util.Map;

public class ConnectedClient {
    private final Socket clientSocket;
    private final BufferedReader in;
    private final int id;
    // Map to store each client's message history.
    // Key: Client ID (Integer), Value: List of messages (List<String>) sent by the
    // client.
    private final Map<Integer, List<String>> clientData;
    // Map to store resource usage thresholds set by each client.
    // Key: Client ID (Integer), Value: Map of resource thresholds (e.g., "CPU",
    // "RAM", "GPU").
    // The inner map contains resource names as keys and their corresponding
    // threshold values as integers.
    private final Map<Integer, Map<String, Double>> clientThresholds;
    private final Server server;

    public ConnectedClient(Socket clientSocket, int id, Map<Integer, List<String>> clientData,
            Map<Integer, Map<String, Double>> clientThresholds, Server server) {
        this.clientSocket = clientSocket;
        this.id = id;
        this.clientData = clientData;
        this.clientThresholds = clientThresholds;
        this.server = server;
        try {
            System.out.println("Client " + id + ": Client Connected");
            this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void readMessages() {
        String line;
        while (true) {
            try {
                line = in.readLine(); // Read plain text input
                if (line == null || line.equals(Server.STOP_STRING))
                    break; // Handle disconnection
                synchronized (clientData) {
                    clientData.get(id).add(line); // Append the message to the client's history
                }
                System.out.println("Client " + id + ": " + line);

                if (line.startsWith("SET_THRESHOLD")) {
                    try {
                        String[] parts = line.split(" ");
                        if (parts.length == 4) {
                            double cpuThreshold = Double.parseDouble(parts[1].split(":")[1]);
                            double ramThreshold = Double.parseDouble(parts[2].split(":")[1]);
                            double gpuThreshold = Double.parseDouble(parts[3].split(":")[1]);
                            server.setClientThreshold(id, cpuThreshold, ramThreshold, gpuThreshold);
                            System.out.println("Client " + id + " set thresholds: CPU=" + cpuThreshold + " RAM="
                                    + ramThreshold + " GPU=" + gpuThreshold);
                        } else {
                            System.out.println("Invalid threshold format.");
                        }
                    } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                        System.out.println("Error parsing thresholds: " + e.getMessage());
                    }
                } else if (line.startsWith("CPU")) {
                    try {
                        String[] parts = line.split(" ");
                        double cpuUsage = Double.parseDouble(parts[1].split(":")[1].trim());
                        double ramUsage = Double.parseDouble(parts[2].split(":")[1].trim());
                        double gpuUsage = Double.parseDouble(parts[3].split(":")[1].trim());
                        server.checkClientData(id, cpuUsage, ramUsage, gpuUsage);
                    } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                        System.out.println("Error parsing resource data: " + e.getMessage());
                    }
                } else {
                    System.out.println("Unrecognized command: " + line);
                }

            } catch (IOException e) {
                System.out.println("Error reading from client: " + e.getMessage());
                break;
            }
        }
        synchronized (clientData) {
            clientData.remove(id); // Remove client data on disconnect
        }
        System.out.println("Client " + id + ": Client Disconnected");
    }

    public void close() {
        try {
            in.close();
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
