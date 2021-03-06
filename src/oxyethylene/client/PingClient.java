package oxyethylene.client;

import java.io.IOException;
import java.net.*;
import java.util.Date;
import java.util.concurrent.*;

public class PingClient {
    static ExecutorService executorService = Executors.newSingleThreadExecutor();

    public static void main(String[] args) {
        // 参数处理
        if (args.length < 2) {
            System.out.println("输入的参数过少。使用方法：PingClient host port");
            System.exit(-1);
        }
        String host = args[0];
        int port = Integer.parseInt(args[1]);
        // 创建socket
        try (DatagramSocket client = new DatagramSocket()) {
            System.out.println("ping " + host + ":" + port);
            long minTime = 1001;
            long maxTime = 0;
            long avgTime = 0;
            int lossCount = 0;
            for (int i = 0; i < 10; i++) {
                // 发送请求
                long startTime = new Date().getTime();
                byte[] payload = buildPayload(i, startTime);
                DatagramPacket requestPacket
                        = new DatagramPacket(payload, payload.length, InetAddress.getByName(host), port);
                client.send(requestPacket);

                // 接受请求与超时处理

                Future<DatagramPacket> replyPacketFuture = executorService.submit(() -> {
                    byte[] buf = new byte[1024];
                    DatagramPacket replyPacket = new DatagramPacket(buf, buf.length);
                    client.receive(replyPacket);
                    return replyPacket;
                });
                try {
                    DatagramPacket replyPacket = replyPacketFuture.get(1, TimeUnit.SECONDS);
                    long endTime = new Date().getTime();
                    long timeCost = endTime - startTime;
                    if (timeCost > maxTime) {
                        maxTime = timeCost;
                    }
                    if (timeCost < minTime) {
                        minTime = timeCost;
                    }
                    avgTime += timeCost;
                    System.out.println(replyPacket.getLength() + " bytes, " + timeCost + " ms, seq=" + i);
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                } catch (TimeoutException te) {
                    System.out.println("等待超时");
                    ++lossCount;
                }
            } // for end here
            executorService.shutdown();
            System.out.println("10 packets transmitted, "
                    + (10 - lossCount) + " packets received, "
                    + (double) lossCount / 10.0 * 100 + "% packet loss");
            System.out.println("round-trip min/avg/max = "
                    + minTime + "/"
                    + (lossCount == 10 ? "" : (avgTime / (long) (10 - lossCount) + "/"))
                    + maxTime + " ms");
        } catch (UnknownHostException | SocketException e) {
            System.out.println("无法创建Socket");
            e.printStackTrace();
            System.exit(-1);
        } catch (IOException ioe) {
            System.out.println("无法发送数据包");
            ioe.printStackTrace();
            System.exit(-1);
        }
    }


    /**
     * 将信息拼装成请求消息数组
     *
     * @param SequenceNumber 序号
     * @param TimeStamp      时间戳，发送该消息的机器时间
     */
    private static byte[] buildPayload(int SequenceNumber, long TimeStamp) {
        String payload = "PingUDP "
                + "HEAD:" + Thread.currentThread().getName()
                + " SequenceNumber:" + SequenceNumber
                + " TimeStamp:" + TimeStamp;
        return payload.getBytes();
    }
}
