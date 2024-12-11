import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    private final ServerSocket server;
    private final ServerSocket internalConnection;
    private Socket webSocket;
    public static final int PORT = 3033;
    public static final int INTERNAL_PORT = 4040;
    public static final String STOP_STRING = "##";
    private final Map<Integer, List<String>> clientData = new HashMap<>();
    private final Map<Integer, Map<String, Double>> clientThresholds = new HashMap<>();
    private volatile boolean isAlertTriggered = false; // Flag for resource threshold alert

    public Server() throws IOException {
        server = new ServerSocket(PORT);
        System.out.println("Server started on port " + PORT);

        internalConnection = new ServerSocket(4040);
        // Accept client connections in a separate thread
        new Thread(this::acceptClients).start();
        new Thread(this::acceptInternalWebServer).start();
        // // Serve the web page
        // serveWebPage();
    }

    private void acceptInternalWebServer() {
        while (true) {
            try {
                this.webSocket = internalConnection.accept();
                new Thread(() -> {
                    try {
                        BufferedReader in = new BufferedReader(new InputStreamReader(this.webSocket.getInputStream()));
                        while (true) {
                            String message = in.readLine();
                            if (message == null) {
                                break;
                            }
                            String[] parts = message.split(" ");
                            int clientId = Integer.parseInt(parts[0].split(":")[1]);
                            double cpuThreshold = Double.parseDouble(parts[1].split(":")[1]);
                            double ramThreshold = Double.parseDouble(parts[2].split(":")[1]);
                            double gpuThreshold = Double.parseDouble(parts[3].split(":")[1]);

                            System.out.println("Setting thresholds for client " + clientId + " CPU: " + cpuThreshold
                                    + " RAM: " + ramThreshold + " GPU: " + gpuThreshold);
                            setClientThreshold(clientId, cpuThreshold, ramThreshold, gpuThreshold);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void acceptClients() {
        int index = 0;
        while (true) {
            try {
                Socket clientSocket = server.accept();
                index++;
                synchronized (clientData) {
                    clientData.put(index, new ArrayList<>()); // Initialize chat history for the client , can also
                                                              // change it to no chat history
                }
                int finalIndex = index;
                new Thread(() -> {
                    ConnectedClient client = new ConnectedClient(clientSocket, finalIndex, clientData, clientThresholds,
                            this);
                    client.readMessages();
                    client.close();
                }).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void serveWebPage() {
        try (ServerSocket httpServer = new ServerSocket(8080)) {
            System.out.println("HTTP Server started on port 8080");
            while (true) {
                try (Socket socket = httpServer.accept();
                        PrintWriter out = new PrintWriter(socket.getOutputStream())) {

                    // Generate the webpage with the latest client data
                    String html = generateHtmlPage();
                    out.println("HTTP/1.1 200 OK");
                    out.println("Content-Type: text/html");
                    out.println("Content-Length: " + html.length());
                    out.println();
                    out.println(html);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String generateHtmlPage() {
        String backgroundColor;
        synchronized (this) {
            backgroundColor = isAlertTriggered ? "yellow" : "white"; // Check the synchronized flag
        }

        StringBuilder html = new StringBuilder("<html><head>");
        html.append("<title>Client Resource Monitoring</title>");
        html.append("<meta http-equiv=\"refresh\" content=\"2\">"); // Auto-refresh every 2 seconds
        html.append("<style>body { background-color: ").append(backgroundColor).append("; }</style>");
        html.append("</head><body>");
        html.append("<h1>Clients and Their Resource</h1>");
        synchronized (clientData) {
            clientData.forEach((id, messages) -> {
                html.append("<h2>Client ").append(id).append("</h2>");
                html.append("<ul>");
                for (String message : messages) {
                    html.append("<li>").append(message).append("</li>");
                }
                html.append("</ul>");
            });
        }
        html.append("</body></html>");
        return html.toString();
    }

    public void checkClientData(int clientId, double cpuUsage, double ramUsage, double gpuUsage) {
        // Retrieve the thresholds set for the specific client
        Map<String, Double> thresholds = clientThresholds.get(clientId);

        if (thresholds != null) {// Ensure thresholds exist for the client
            boolean alert = false;
            // Check if cpu ram or gpu usage exceeds the threshold
            if (cpuUsage > thresholds.get("CPU")) {
                alert = true;
                System.out.println("Client " + clientId + " exceeded CPU threshold");
            }
            if (ramUsage > thresholds.get("RAM")) {
                alert = true;
                System.out.println("Client " + clientId + " exceeded RAM threshold");
            }
            if (gpuUsage > thresholds.get("GPU")) {
                alert = true;
                System.out.println("Client " + clientId + " exceeded GPU threshold");
            }

            // If any alert is triggered, set the flag
            synchronized (this) {
                isAlertTriggered = alert;
            }
            try {
                PrintWriter out = new PrintWriter(this.webSocket.getOutputStream());
                System.out.println("Sending data to internal web server");
                out.println(
                        "Client " + clientId + " CPU: " + cpuUsage + " RAM: " + ramUsage + " GPU: " + gpuUsage
                                + " Alert: " + alert);
                out.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void setClientThreshold(int clientId, double cpuThreshold, double ramThreshold, double gpuThreshold) {
        Map<String, Double> thresholds = new HashMap<>();
        thresholds.put("CPU", cpuThreshold);
        thresholds.put("RAM", ramThreshold);
        thresholds.put("GPU", gpuThreshold);
        // Update the client thresholds map in a thread-safe manner
        synchronized (clientThresholds) {
            clientThresholds.put(clientId, thresholds);
        }
    }

    public static void main(String[] args) throws IOException {
        new Server();
    }
}
