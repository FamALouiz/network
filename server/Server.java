import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    private final ServerSocket server;
    public static final int PORT = 3033;
    public static final String STOP_STRING = "##";
    private final Map<Integer, List<String>> clientData = new HashMap<>();
    private final Map<Integer, Map<String, Integer>> clientThresholds = new HashMap<>();
    private volatile boolean isAlertTriggered = false; // Flag for resource threshold alert

    public Server() throws IOException {
        server = new ServerSocket(PORT);
        System.out.println("Server started on port " + PORT);

        // Accept client connections in a separate thread
        new Thread(this::acceptClients).start();

        // Serve the web page
        serveWebPage();
    }

    private void acceptClients() {
        int index = 0;
        while (true) {
            try {
                Socket clientSocket = server.accept();
                index++;
                synchronized (clientData) {
                    clientData.put(index, new ArrayList<>()); // Initialize chat history for the client , can also change it to no chat history
                }
                int finalIndex = index;
                new Thread(() -> {
                    ConnectedClient client = new ConnectedClient(clientSocket, finalIndex, clientData, clientThresholds, this);
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


    public void checkClientData(int clientId, int cpuUsage, int ramUsage, int gpuUsage) {
        // Retrieve the thresholds set for the specific client
        Map<String, Integer> thresholds = clientThresholds.get(clientId);
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
        }
    }


    public void setClientThreshold(int clientId, int cpuThreshold, int ramThreshold, int gpuThreshold) {
        Map<String, Integer> thresholds = new HashMap<>();
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
