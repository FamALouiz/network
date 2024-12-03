import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;

public class ConnectedClient {
    private Socket clientSocket;
    private DataInputStream in;
    private int id;

    public ConnectedClient(Socket clientSocket, int id) {
        this.clientSocket = clientSocket;
        this.id = id;
        try {
            System.out.println("Client " + id + ": Client Connected");
            this.in = new DataInputStream(new BufferedInputStream(clientSocket.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void readMessages() {
        String line = "";
        while (!line.equals(Server.STOP_STRING)) {
            try {
                line = in.readUTF();
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("Client " + id + ": " + line);
        }
        System.out.println("Client " + id + ": Client Disconnected");
    }

    public void close() {
        try {
            clientSocket.close();
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}