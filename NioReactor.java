package com.zwj.myNio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

public class NioReactor {

    final String encoding = System.getProperty("file.encoding");
    final Charset charse = Charset.forName(encoding);
    private final Worker worker;

    public NioReactor() {
        //新建一个worker，worker 是一个 任务，实现 了Runnable接口他的功能就是不断的轮训注册在他的selector 上的通过（channel）就绪读事件
        this.worker = new Worker();
    }

    public void postRegister(SocketChannel socketChannel) {
        //把 通道放到worker 的register队列上面，每次worker轮训的时候会去检查这个队列，如果有新的通道，就把通道注册到自己的selector
        this.worker.registerQueue.add(socketChannel);
        this.worker.selector.wakeup();
    }

    public void start() {
        //启动一个线程来执行worker
        new Thread(worker).start();
    }

    private class Worker implements Runnable {

        private volatile Selector selector;
        private ConcurrentLinkedQueue<SocketChannel> registerQueue = new ConcurrentLinkedQueue<SocketChannel>();
        public Worker() {
            try {
                this.selector = Selector.open();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        private void register(Selector selector) {
            if (registerQueue.isEmpty()) {
                return;
            }
            SocketChannel socketChannel = null;
            while ((socketChannel = registerQueue.poll()) != null) {
                try {
                    socketChannel.register(selector, SelectionKey.OP_READ);
                } catch (ClosedChannelException e) {
                    e.printStackTrace();
                }
            }
        }
        @Override
        public void run() {
            while (true) {
                try {
                    register(selector);
                    selector.select();
                    Set<SelectionKey> keys = selector.selectedKeys();
                    for (SelectionKey key : keys) {
                        keys.remove(key);
                        if (key.isValid() && key.isReadable()) {
                            SocketChannel rchannel = null;
                            try {
                                rchannel = (SocketChannel) key.channel();
                                ByteBuffer readByteBuffer = ByteBuffer.allocate(2048);
                                String content = "";
                                int result= rchannel.read(readByteBuffer);
                                if(result == -1 ){
                                    rchannel.close();
                                }else{
                                    readByteBuffer.flip();
                                    content += charse.decode(readByteBuffer);
                                    //to do business
                                    System.out.println("server rec:" + content);
                                    String tomessage = "this is server!i have rec you mess";
                                    rchannel.write(charse.encode(tomessage));
                                }

                            } catch (IOException e) {
                                if (rchannel != null) {
                                    key.cancel();
                                    rchannel.close();
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }


}
