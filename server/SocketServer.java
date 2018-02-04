package server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class SocketServer implements Runnable {
    private Selector selector;
    private Map<SocketChannel, List> dataMapper;
    private String address;
    private int port;
    private InetSocketAddress listenAddress;
    ByteBuffer buffer;

    public SocketServer(String address, int port) throws IOException {
        listenAddress = new InetSocketAddress(address, port);
        dataMapper = new HashMap<>();
        this.address = address;
        this.port = port;
    }

    private void establishConnection() throws IOException {
        //establish server channel
        this.selector = Selector.open();
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);

        serverSocketChannel.socket().bind(listenAddress);
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println("Starting a server...");
        buffer = ByteBuffer.allocate(2048);

        startServer();
    }

    @Override
    public void run() {
        try {
            new SocketServer(address, port).establishConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startServer() throws IOException {
        while(true) {
            //catch events
            selector.selectNow();

            //perform selected tasks
            Iterator keys = selector.selectedKeys().iterator();
            while (keys.hasNext()) {
                SelectionKey key = (SelectionKey)keys.next();

                keys.remove();

                //check if the key is valid
                if (!key.isValid()) {
                    continue;
                }

                if (key.isAcceptable()) {
                    accept(key);
                }
                if (key.isReadable()) {
                    try {
                        read(key);
                    }catch (Exception e) {
                        e.printStackTrace();
                        System.out.println("Connection refused");
                        break;
                    }
                }
            }
        }
    }

    //establish connection with remote socket channel
    private void accept(SelectionKey key) throws IOException {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel)key.channel();
        SocketChannel channel = serverSocketChannel.accept();
        channel.configureBlocking(false);

        System.out.println("Connected to remote address");

        dataMapper.put(channel, new ArrayList());
        channel.register(selector, SelectionKey.OP_READ);
    }

    //establish reading socket channel
    private void read(SelectionKey key) throws Exception {
        SocketChannel channel = (SocketChannel)key.channel();
        buffer.clear();
        int readBytes = -1;
        try {
            readBytes = channel.read(buffer);
        }catch (Exception e) {
            System.out.println("CLOSED WITH ERROR");
            channel.close();
        }

        if (readBytes == -1) {
            //remove channel from the active channels list
            dataMapper.remove(channel);
            System.out.println("Connection has been closed by remote socket\n\r");

            String mes = channel.getRemoteAddress().toString() + " left the chat\n\r";
            buffer.flip();
            buffer = ByteBuffer.wrap(mes.getBytes());
            broadcast(channel, mes);
            channel.finishConnect();
            key.cancel();
        } else {
            byte[]read = new byte[readBytes];
            try {
                System.arraycopy(buffer.array(), 0, read, 0, readBytes);
            } catch (IndexOutOfBoundsException e) {
                System.out.println("Too long sentence");
            }
            buffer.flip();
            //ip command
            String input = StandardCharsets.UTF_8.decode(buffer).toString();
            if (input.contains("::ip")) {
                String cmd = input.substring(5, input.length());
                for (SocketChannel channel1 : dataMapper.keySet()) {
                    if (channel1.getRemoteAddress().toString().contains(cmd)) {
                        channel1.write(buffer.wrap("You were banned".getBytes()));
                        channel1.finishConnect();
                        System.out.println("User was banned");
                        break;
                    }
                }
            }
            //send message to all channels
            broadcast(channel, input);
        }
    }

    private void broadcast(SocketChannel fromChannel, String mes) throws Exception {
        for (SocketChannel channel : dataMapper.keySet()) {
            try {
                if (channel != fromChannel) {
                    System.out.println("Message is sent: " + mes);
                    System.out.println("Broadcasting to client: " + channel);
                    buffer.flip();
                    channel.write(buffer);
                }
                } catch(Exception e){
                    System.out.println("Couldn't write to clients");
                    e.printStackTrace();
            }
        }
    }
}
