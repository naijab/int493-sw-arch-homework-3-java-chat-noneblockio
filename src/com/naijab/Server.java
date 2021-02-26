package com.naijab;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;

public class Server {

    private Selector selector;
    private Charset charset = Charset.forName("UTF-8");

    public static void main(String[] args) throws IOException {
        new Server().start();
    }

    public void start() throws IOException {
        selector = Selector.open();
        ServerSocketChannel server = ServerSocketChannel.open();
        server.socket().bind(new InetSocketAddress("127.0.0.1", 9000));
        server.configureBlocking(false);
        server.register(selector, SelectionKey.OP_ACCEPT);

        System.out.println("Listening ... ");

        while (selector.select() > 0) {
            for (SelectionKey key : selector.selectedKeys()) {
                selector.selectedKeys().remove(key);

                if (key.isAcceptable()) {
                    SocketChannel socketChannel = server.accept();
                    socketChannel.configureBlocking(false);
                    socketChannel.register(selector, SelectionKey.OP_READ);
                    key.interestOps(SelectionKey.OP_ACCEPT);

                    System.out.println("New Client: " + socketChannel.getRemoteAddress());
                }

                if (key.isReadable()) {
                    SocketChannel socketChannel = (SocketChannel) key.channel();
                    ByteBuffer buffer = ByteBuffer.allocate(1024);
                    StringBuilder message = new StringBuilder();

                    try {
                        while (socketChannel.read(buffer) > 0) {
                            buffer.flip();
                            message.append(charset.decode(buffer));
                            buffer.clear();
                        }
                        System.out.println(message);
                        key.interestOps(SelectionKey.OP_READ);
                    } catch (IOException e) {
                        key.cancel();
                        if (key.channel() != null) {
                            key.channel().close();
                        }
                    }

                    // Broadcast to All client
                    if (!message.toString().isEmpty()) {
                        for (SelectionKey sk : selector.keys()) {
                            Channel targetChannel = sk.channel();
                            if (targetChannel instanceof SocketChannel) {
                                SocketChannel dest = (SocketChannel) targetChannel;
                                String sentMessage = message.toString();
                                dest.write(charset.encode(sentMessage));
                            }
                        }
                    } else {
                        socketChannel.close();
                        key.cancel();
                    }
                }
            }
        }
    }
}
