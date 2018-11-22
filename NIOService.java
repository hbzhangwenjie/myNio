package com.zwj.myNio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;

public class NIOService {

    public String IP = "127.0.0.1";// 10.50.200.120
    public static final int PORT = 4444;
    private static final int SIZE = 256;

    // 对于以字符方式读取和处理的数据必须要进行字符集编码和解码
    String encoding = System.getProperty("file.encoding");
    // 加载字节编码集
    Charset charse = Charset.forName(encoding);

    public NIOService() throws IOException {
        // NIO的通道channel中内容读取到字节缓冲区ByteBuffer时是字节方式存储的，
        // 分配两个字节大小的字节缓冲区
        ByteBuffer buffer = ByteBuffer.allocate(SIZE);
        SocketChannel ch = null;
        Selector selector = null;
        ServerSocketChannel serverChannel = null;

        try {
            // 打开通道选择器
            selector = Selector.open();
            // 打开服务端的套接字通道
            serverChannel = ServerSocketChannel.open();
            // 将服务端套接字通道连接方式调整为非阻塞模式
            serverChannel.configureBlocking(false);
            // serverChannel.socket().setReuseAddress(true);
            // 将服务端套接字通道绑定到本机服务端端口
            serverChannel.socket().bind(new InetSocketAddress(IP, PORT));
            // 将服务端套接字通道OP_ACCEP事件注册到通道选择器上
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println("Server on port:" + PORT);
            while (true) {
                // 通道选择器开始轮询通道事件
                selector.select();
                Iterator it = selector.selectedKeys().iterator();
                while (it.hasNext()) {
                    // 获取通道选择器事件键
                    SelectionKey skey = (SelectionKey) it.next();
                    it.remove();
                    // 服务端套接字通道发送客户端连接事件，客户端套接字通道尚未连接
                    if (skey.isAcceptable()) {
                        // 获取服务端套接字通道上连接的客户端套接字通道
                        ch = serverChannel.accept();
                        System.out.println("Accepted connection from:" + ch.socket());
                        // 将客户端套接字通过连接模式调整为非阻塞模式
                        ch.configureBlocking(false);
                        // 将客户端套接字通道OP_READ事件注册到通道选择器上
                        ch.register(selector, SelectionKey.OP_READ);
                    }
                    // 如果sk对应的Channel有数据需要读取
                    if (skey.isReadable()) {
                        // 获取该SelectionKey对银行的Channel，该Channel中有刻度的数据
                        SocketChannel sc = (SocketChannel) skey.channel();
                        String content = "";
                        // 开始读取数据
                        try {
                            content = receiverFromClient(sc,buffer);
                            // 将sk对应的Channel设置成准备下一次读取
                            skey.interestOps(SelectionKey.OP_READ);
                        } catch (IOException e) {// 如果捕获到该sk对银行的Channel出现了异常，表明
                            // Channel对应的Client出现了问题，所以从Selector中取消
                            // 从Selector中删除指定的SelectionKey
                            skey.cancel();
                            if (skey.channel() != null) {
                                skey.channel().close();
                            }
                        }
                        // 如果content的长度大于0,则处理信息返回给客户端
                        if (content.length() > 0) {
                            System.out.println("接受客户端数据：" + content);
                            // 处理信息返回给客户端
                            sendToClient(selector,content);
                        }
                        //ch.write((ByteBuffer)buffer.rewind());
                        //buffer.clear();
                    }
                    if(skey.isWritable()){

                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (ch != null)
                ch.close();
            serverChannel.close();
            selector.close();
        }
    }

    /**
     * 向客户端发送数据
     * @param selector
     * @param content
     * @throws IOException
     */
    public void sendToClient(Selector selector,String content) throws IOException{
        // 遍历selector里注册的所有SelectionKey
        for (SelectionKey key1 : selector.keys()) {
            // 获取该key对应的Channel
            Channel targerChannel = key1.channel();
            // 如果该Channel是SocketChannel对象
            if (targerChannel instanceof SocketChannel) {
                // 将读取到的内容写入该Channel中
                SocketChannel dest = (SocketChannel) targerChannel;
                sendToClient(dest,content);
            }
        }
    }

    /**
     * 向指定频道发送数据
     * @param channel
     * @param data
     * @throws IOException
     */
    public void sendToClient(SocketChannel channel, String data) throws IOException {
        channel.write(charse.encode(data));
        //channel.socket().shutdownOutput();
    }

    /**
     * 接受来自客户端数据
     * @param channel
     * @param buffer
     * @return
     * @throws Exception
     */
    private String receiverFromClient(SocketChannel channel,ByteBuffer buffer) throws Exception {
        String content = "";
        //* 取客户端发送的数据两个方法任选其一即可
        // 开始读取数据
        // 法一
        channel.read(buffer);
        CharBuffer cb = charse.decode((ByteBuffer) buffer.flip());
        content = cb.toString();
        // 法二
		/*
		while (sc.read(buffer) > 0) {
			buffer.flip();
			content += charse.decode(buffer);
		}//*/
        buffer.clear();
        return content;
    }

    public static void main(String[] args) {
        try {
            new NIOService();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
