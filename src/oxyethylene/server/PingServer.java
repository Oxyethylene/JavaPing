package oxyethylene.server;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;

public class PingServer {
    private static DatagramSocket server;

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("输入的参数过少。使用方法：PingServer port");
            System.exit(-1);
        }
        int port = Integer.parseInt(args[0]);

        try {
            // 建立连接
            server = new DatagramSocket(port);
            System.out.println("已在127.0.0.1: " + port + "创建DatagrameSocket");
            // 持续监听端口，获得数据
            while (true) {
                byte[] buf = new byte[1024];
                DatagramPacket receivedPacket = new DatagramPacket(buf, buf.length);
                server.receive(receivedPacket);
                new Thread(new RequestHandler(receivedPacket)).start();
            }
        } catch (SocketException se) {
            System.out.println("创建Socket时发生错误");
            se.printStackTrace();
            System.exit(-1);
        } catch (IOException ioe) {
            System.out.println("接受数据时出错");
            ioe.printStackTrace();
            System.exit(-1);
        }
        server.close();
    }

    static class RequestHandler implements Runnable {
        // 模拟的延迟的最大值，有500用于模拟丢失
        private static final int MAX_DELAY = 1500;
        private final DatagramPacket receivedPacket;

        public RequestHandler(DatagramPacket receivedPacket) {
            this.receivedPacket = receivedPacket;
        }

        @Override
        public void run() {
            // 接收报文的处理
            String payload = new String(receivedPacket.getData(), 0, receivedPacket.getLength());
            int port = receivedPacket.getPort();
            InetAddress address = receivedPacket.getAddress();
            System.out.println("收到来自" + address.toString() + ":" + port + "的消息");
            System.out.println("Payload: " + payload + "\n");

            // 模拟延迟
            long delay = (long) (Math.random() * MAX_DELAY);
            String replyMessage = "reply to "
                    + address.toString() + ":" + port + " "
                    + "Delay is: " + delay + " "
                    + payload.split(" ")[2];
            // 模拟数据丢失
            if (delay > 1000) {
                System.out.println("回复数据丢失\n" + "Payload is: " + replyMessage);
            }
            try {
                Thread.sleep(delay);
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }

            // 发送回复
            byte[] replyPayload = replyMessage.getBytes();
            DatagramPacket reply = new DatagramPacket(replyPayload, replyPayload.length, address, port);
            try {
                server.send(reply);
                System.out.println("已发送对" + address.toString() + ":" + port + "的回复\nPayload is: " + replyMessage + "\n");
            } catch (IOException ioe) {
                System.out.println("发送回复时出现错误\n");
                ioe.printStackTrace();
            }

        }
    }

}
