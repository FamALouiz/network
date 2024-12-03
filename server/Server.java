import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    private ServerSocket server;
    public static final int PORT = 3030;
    public static final String STOP_STRING = "##";
    private int index = 0;

    public Server() {
        try {
            server = new ServerSocket(PORT);
            while (true)
                iniConnections();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void iniConnections() throws IOException {
        Socket clientSocket = server.accept();

        if (clientSocket.isConnected())
            new Thread(() -> {
                index++;
                ConnectedClient client = new ConnectedClient(clientSocket, index);
                client.readMessages();
                client.close();
            }).start();
    }

    public static void main(String[] args) {
        new Server();
    }
}