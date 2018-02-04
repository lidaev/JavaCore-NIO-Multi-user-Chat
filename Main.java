import server.SocketServer;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws InterruptedException, IOException {
        new Thread(new SocketServer("localhost", 8080)).start();
    }
}
