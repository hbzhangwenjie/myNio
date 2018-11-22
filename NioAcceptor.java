package com.zwj.myNio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Set;

public class NioAcceptor {
    private volatile Selector selector;
    private final ServerSocketChannel serverSocketChannel;
    private String iP;
    private int port;
    // 对于以字符方式读取和处理的数据必须要进行字符集编码和解码
    String encoding = System.getProperty("file.encoding");
    // 加载字节编码集
    Charset charse = Charset.forName(encoding);
    private final NioReactor[] nioReactors;
    private volatile int nextReactor;

    public NioAcceptor(String ip, int port, int threadN) {
        if (ip == null || ip == "") {
            throw new IllegalArgumentException();
        }
        this.iP = ip;
        if (port < 1) {
            throw new IllegalArgumentException();
        }
        this.port = port;

        if (threadN < 1) {
            throw new IllegalArgumentException();
        }
        //新建处理读写的线程池
        nioReactors = new NioReactor[threadN];
        for (int i = 0; i < threadN; i++) {
            nioReactors[i] = new NioReactor();
            nioReactors[i].start();
        }
        try {
            this.selector = Selector.open();
            this.serverSocketChannel = ServerSocketChannel.open();
            this.serverSocketChannel.configureBlocking(false);
            /** 设置TCP属性 */
            serverSocketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            serverSocketChannel.setOption(StandardSocketOptions.SO_RCVBUF, 1024 * 16 * 2);
            // backlog=100
            serverSocketChannel.bind(new InetSocketAddress(ip, port), 100);
            this.serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println("server start!");
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalArgumentException();
        }
    }

    private NioReactor nextNioReactor() {

        int i = nextReactor++;
        if (i >= nioReactors.length) {
            i = nextReactor = 0;
        }
        return nioReactors[i];
    }

    public void run() {
        while (true) {
            try {
                selector.select();
                Set<SelectionKey> keys = selector.selectedKeys();
                if (!keys.isEmpty()) {
                    for (SelectionKey key : keys) {
                        keys.remove(key);
                        if (key.isValid() && key.isAcceptable()) {
                            SocketChannel ch = serverSocketChannel.accept();
                            ch.configureBlocking(false);
                            String tomessage = "welcome,this is server!";
                            try {
                                ch.write(charse.encode(tomessage));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            //把这个通道交给Reactor处理，获取NioReactor的方式非常原始就是轮询这个数组，从0开始那，到了末尾就又从0开始
                            NioReactor nioReactor = nextNioReactor();
                            nioReactor.postRegister(ch);
                        } else {
                            //acceptor 只接受 accept事件，如果有其他事件 把这个通道从selector 移除
                            key.cancel();
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
