package com.zwj.myNio;

public class MyNio {


    public static void main(String[] args) {
        // write your code here
        NioAcceptor nioAcceptor = new NioAcceptor("127.0.0.1", 8090, 1);
        nioAcceptor.run();
    }
}
