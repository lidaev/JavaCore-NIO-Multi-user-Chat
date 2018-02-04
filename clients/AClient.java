package clients;

import java.io.*;
import java.net.Socket;

public abstract  class AClient {
    public static void startClient(int id) throws IOException {
        Socket socket = new Socket("localhost",8080);

        InputStream sin = socket.getInputStream();

        DataInputStream dataInputStream = new DataInputStream(sin);
        DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());

        InputStreamReader keyInputStream = new InputStreamReader(System.in);
        BufferedReader bufferedReader = new BufferedReader(keyInputStream);
        String mes="";

        //read data from input or write into outputstream
        while(true) {
            //non-blocking checking if there is data to write
            if (bufferedReader.ready()) {
                mes = bufferedReader.readLine();
                if (mes.equals("::exit")) {
                    try {
                        dataOutputStream.writeUTF("User " + id + " left the chat");
                        dataOutputStream.flush();
                        dataInputStream.close();
                        dataOutputStream.close();
                        System.out.println("You left the chat");
                        break;
                    } catch (Exception e) {
                        System.out.println("Exiting with error");
                        e.printStackTrace();
                    }
                }
                dataOutputStream.writeUTF("User " + id + ": " + mes);
                dataOutputStream.flush();
            }
            //non-blocking checking if there is data to read
            if(dataInputStream.available() > 0) {
                System.out.println(dataInputStream.readUTF());
            }
        }
    }
}
