package com.naijab;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Scanner;

public class Client {

    private String clientName;
    private Selector selector;
    private SocketChannel socketChannel;
    private Charset charset = Charset.forName("UTF-8");

    public static void main(String[] args) throws Exception {
        new Client().start();
    }

    public void start() throws IOException {
        selector = Selector.open();
        socketChannel = SocketChannel.open(new InetSocketAddress("127.0.0.1", 9000));
        socketChannel.configureBlocking(false);
        socketChannel.register(selector, SelectionKey.OP_READ);

        System.out.println("Connected to Server ... ");

        new Thread(() -> {
            // Read from Server
            try {
                while(selector.select() > 0) {
                    for (SelectionKey key : selector.selectedKeys()) {
                        selector.selectedKeys().remove(key);

                        if (key.isReadable()) {
                            SocketChannel sc = (SocketChannel) key.channel();
                            ByteBuffer buff = ByteBuffer.allocate(1024);
                            StringBuilder message = new StringBuilder();
                            while (sc.read(buff) > 0) {
                                sc.read(buff);
                                buff.flip();
                                message.append(charset.decode(buff));
                                buff.clear();
                            }
                            String serverMessage = message.toString();
                            String serverMessagePort = serverMessage.split("@")[1];
                            serverMessagePort = serverMessagePort.split(":")[0];
                            if (clientName != null) {
                                String myPort = clientName.split("@")[1];
                                if (!serverMessagePort.equals(myPort)) {
                                    System.out.println("[SERVER] " + serverMessage);
                                }
                            }
                            key.interestOps(SelectionKey.OP_READ);
                        }
                    }
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        // Input to Server
        Scanner scan = new Scanner(System.in);
        if (clientName == null) {
            System.out.print("Input Nickname: ");
            String name = scan.nextLine();
            clientName = name + "@" + socketChannel.getLocalAddress().toString().split(":")[1];
        }
        System.out.println("Input Message and Enter ---> ");
        while (scan.hasNextLine()) {
            String input = scan.nextLine();
            String sentMessage = clientName + ": " +input;
            socketChannel.write(charset.encode(sentMessage));
        }
    }
}
