package com.zwj.myNio;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Scanner;

public class NIOClient {
    private static final int SIZE = 1024;
    private static NIOClient instance = new NIOClient();
    public String IP = "127.0.0.1";// 10.50.200.120
    public int CLIENT_PORT = 8090;// 4444 9666
    private SocketChannel channel;
    private Selector selector = null;

    String encoding = System.getProperty("file.encoding");
    Charset charset = Charset.forName(encoding);

    private NIOClient() {
    }

    public static NIOClient getInstance() {
        return instance;
    }

    public void send(String content) throws IOException {
        selector = Selector.open();
        channel = SocketChannel.open();
        // channel = SocketChannel.open(new InetSocketAddress(IP,CLIENT_PORT));
        InetSocketAddress remote = new InetSocketAddress(IP, CLIENT_PORT);
        channel.connect(remote);
        // 设置该sc以非阻塞的方式工作
        channel.configureBlocking(false);
        // 将SocketChannel对象注册到指定的Selector
        // SelectionKey.OP_READ | SelectionKey.OP_WRITE | SelectionKey.OP_CONNECT
        channel.register(selector, SelectionKey.OP_READ);//这里注册的是read读，即从服务端读数据过来
        // 启动读取服务器数据端的线程
        new ClientThread().start();
        channel.write(charset.encode(content));
        // 创建键盘输入流
        Scanner scan = new Scanner(System.in);//这里向服务端发送数据，同时启动了一个键盘监听器
        while (scan.hasNextLine()) {
            System.out.println("输入数据:\n");
            // 读取键盘的输入
            String line = scan.nextLine();
            // 将键盘的内容输出到SocketChanenel中
            channel.write(charset.encode(line));
        }
        scan.close();
    }

    /**
     * 从服务端读入数据的线程

     *
     * @author 王俊伟 wjw.happy.love@163.com
     * @date 2016年10月20日 下午9:59:11
     */
    private class ClientThread extends Thread {
        @Override
        public void run() {
            try {
                while (selector.select() > 0) {
                    // 遍历每个有可能的IO操作的Channel对银行的SelectionKey
                    for (SelectionKey sk : selector.selectedKeys()) {
                        // 删除正在处理的SelectionKey
                        selector.selectedKeys().remove(sk);
                        // 如果该SelectionKey对应的Channel中有可读的数据
                        if (sk.isReadable()) {
                            // 使用NIO读取Channel中的数据
                            SocketChannel sc = (SocketChannel) sk.channel();
                            String content = "";
                            ByteBuffer bff = ByteBuffer.allocate(SIZE);
                            while (sc.read(bff) > 0) {
                                sc.read(bff);
                                bff.flip();
                                content += charset.decode(bff);
                            }
                            // 打印读取的内容
                            System.out.println("服务端返回数据:" + content);
                            // 处理下一次读
                            sk.interestOps(SelectionKey.OP_READ);
                        }
                    }
                }

            } catch (IOException io) {
                io.printStackTrace();
            }
        }
    }

    /**
     * 接受服务端的数据
     *
     * @param channel
     * @return
     * @throws Exception
     */
    protected void receiveData(SocketChannel channel) throws Exception {
        ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
        int count = 0;
        while ((count = channel.read(buffer)) != -1) {
            if (count == 0) {
                Thread.sleep(100); // 等等一下
                continue;
            }
            // 转到最开始
            buffer.flip();
            while (buffer.remaining() > 0) {
                System.out.print((char) buffer.get());
            }
            buffer.clear();
        }
    }

    public static void main(String[] args) {
        try {
            NIOClient nio = new NIOClient();
            nio.send("test");//向服务端发送数据
            //nio.send("metrics:memory:	swap:	cpu:	network i/o:	disks i/o:	tcp:\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
