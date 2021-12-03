import java.io.*;
import java.net.*;
import java.util.Arrays;

/**
 * 客户端 负责发送数据
 *
 * @author : LiuYi
 * @version : 2.0
 * @date : 2021/11/20 16:18
 */
public class Client implements Runnable {
    /**
     * 源IP
     **/
    private final InetAddress sourceIp;
    /**
     * 源端口
     **/
    private final String sourcePort;
    /**
     * 目的IP
     **/
    private final InetAddress targetIp;
    /**
     * 目的端口
     **/
    private final String targetPort;
    /**
     * 源文件路径
     **/
    private final File fileSource;
    /**
     * seq序号
     **/
    private int seqNum;
    /**
     * ack序号
     **/
    private int ack;
    /**
     * 窗口大小
     **/
    private int windowSize;
    /**
     * Client状态 默认为CLOSED
     **/
    private String state = "CLOSED";
    /**
     * UDP Socket
     **/
    DatagramSocket socket = null;
    /**
     * 文件输出缓冲流
     **/
    BufferedInputStream reader = null;

    private long now;
    private long start;
    private long time;

    @Override
    public void run() {
        try {
            start = System.currentTimeMillis();
            if (shakeHands()) {
                if (sendFile2()) {
                    now = System.currentTimeMillis();
                    time = now - start;
                    System.out.println("耗时：" + time + "ms");
                    // 等待10s，让服务器将数据写入文件中，再与服务器开始断开连接,可以根据要传输文件大小，适当更换
                    Thread.sleep(10000);
                    if (waveHands()) {
                        System.out.println(state + " 与" + UDPutils.getStringIp(targetIp) + "断开成功！");
                    }
                }
            }
        } catch (IOException | ClassNotFoundException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 初始化Client 初始化Client的Socket，以及IO流
     *
     * @param sourceIp   源IP
     * @param sourcePort 源端口
     * @param targetIp   目的IP
     * @param targetPort 目的端口
     * @param fileSource 源文件路径
     * @throws UnknownHostException 初始化异常
     */
    public Client(String sourceIp, String sourcePort, String targetIp, String targetPort, String fileSource) throws UnknownHostException {
        this.sourceIp = InetAddress.getByName(String.valueOf(sourceIp));
        this.sourcePort = sourcePort;
        this.targetIp = InetAddress.getByName(targetIp);
        this.targetPort = targetPort;
        this.fileSource = new File(fileSource);
        try {
            socket = new DatagramSocket(Integer.parseInt(sourcePort));
            reader = new BufferedInputStream(new FileInputStream(fileSource));
        } catch (SocketException | FileNotFoundException e) {
            e.printStackTrace();
            socket.close();
        }
    }

    /**
     * Client开始发起与服务器的握手操作
     *
     * @return 握手成功返回 true 失败返回 false
     * @throws IOException 握手异常
     * @throws ClassNotFoundException 对象序列化异常
     */
    public boolean shakeHands() throws IOException, ClassNotFoundException {
        int shakeHandTimes = 0;
        UDP shakeHandsUdp;
        while (true) {
            // 设置最大请求次数，此处设为3次，握手请求超过三次即握手失败
            if (3 == shakeHandTimes) {
                System.out.println("\n与 " + UDPutils.getStringIp(targetIp) + " 连接超时，连接建立失败！！！");
                return false;
            }
            switch (state) {
                case "CLOSED":
                    // 向服务器发起握手请求
                    System.out.println("开始与 " + UDPutils.getStringIp(targetIp) + " 建立连接：");

                    // 发送SYN = 1，seq = x的请求
                    seqNum = UDPutils.getSeqNum();
                    shakeHandsUdp = new UDP(sourcePort, targetPort, Integer.toString(seqNum), "0", "0", "1", "0", "0", null);
                    shakeHandsUdp.setSource_IP(UDPutils.getStringIp(sourceIp));
                    byte[] datas = UDPutils.objectToByte(shakeHandsUdp);
                    DatagramPacket sendUdpPacket = new DatagramPacket(datas, 0, datas.length, targetIp, Integer.parseInt(targetPort));
                    System.out.println(state + "  发送：SYN=1，seq=" + seqNum);
                    socket.send(sendUdpPacket);

                    // 发送该请求后，Client进入SYN-SENT状态
                    state = "SYN-SENT";

                case "SYN-SENT":
                    // 等待服务端对请求的响应,设置等待时间4s，超过等待时间，就立即重新发送
                    byte[] rec = new byte[512];
                    DatagramPacket receiveUdpPacket = new DatagramPacket(rec, 0, rec.length);
                    socket.setSoTimeout(4000);
                    try {
                        socket.receive(receiveUdpPacket);
                    } catch (SocketTimeoutException e) {
                        System.out.println(state + "  等待ACK超时，重新请求。。。\n");
                        state = "CLOSED";
                        shakeHandTimes++;
                        continue;
                    }

                    // 判断接收是否发生了错误，如果出现错误，即重新发起握手请求
                    if (UDPutils.byteToObject(rec) != null) {
                        shakeHandsUdp = (UDP) UDPutils.byteToObject(rec);
                    } else {
                        System.out.println("接收发生错误，连接失败！");
                        state = "CLOSED";
                        continue;
                    }

                    // 收到服务器的响应，判断各项数值是否合理,如果合理，接收服务器设置的窗口值，并且向服务器给出确认
                    ack = Integer.parseInt(shakeHandsUdp.getSequence_Number());
                    int seqTemp = seqNum + 1;
                    if ("1".equals(shakeHandsUdp.getSyn()) && "1".equals(shakeHandsUdp.getACK()) && seqTemp == Integer.parseInt(shakeHandsUdp.getAck())) {
                        System.out.println(state + "  接收：SYN=1，ACK=1，seq=" + ack + "，ack=" + seqTemp + "  request successful");
                        windowSize = Integer.parseInt(shakeHandsUdp.getWindow_Size());
                        seqNum++;
                        ack++;
                    } else {
                        System.out.println(state + "  接收：SYN=1，ACK=1，seq=" + ack + "，ack=" + seqTemp + "  request failed ");
                        state = "CLOSED";
                        continue;
                    }

                    // 向服务器给出确认，和服务器进入 ESTAB-LISHEN 状态双方开始收发文件
                    shakeHandsUdp = new UDP(sourcePort, targetPort, Integer.toString(seqNum), "1", Integer.toString(ack), "0", "0", Integer.toString(windowSize), null);
                    byte[] dataTwo = UDPutils.objectToByte(shakeHandsUdp);
                    DatagramPacket sendPacket2 = new DatagramPacket(dataTwo, 0, dataTwo.length, targetIp, Integer.parseInt(targetPort));
                    System.out.println(state + "  发送：ACK=1，seq=" + seqNum + "，ack=" + ack);
                    socket.send(sendPacket2);
                    seqNum++;
                    state = "ESTAB-LISHEN";

                case "ESTAB-LISHEN":
                    // 进入 ESTAB-LISHEN 状态，准备开始发送文件
                    System.out.println(state + "  与 " + UDPutils.getStringIp(targetIp) + " 连接建立成功！");
                    return true;
                default:
                    System.out.println("与 " + UDPutils.getStringIp(targetIp) + " 连接建立失败！！！");
                    return false;
            }
        }
    }

    /**
     * Client 采用停等机制 开始向服务器发送文件
     *
     * @return 成功返回true，反之返回false
     */
    public boolean sendFile1() {
        UDP udp;
        try {
            reader = new BufferedInputStream(new FileInputStream(fileSource));
            int fileLength = reader.available();
            byte[] allData = new byte[fileLength];
            int sizeOfRead = reader.read(allData, 0, fileLength);
            int times = (fileLength / (2048 - 361)) + 1;
            System.out.println("\n文件大小为：" + sizeOfRead + "bit");
            System.out.println("文件将分为：" + times + " 个数据报进行发送。");

            byte[][] temp = UDPutils.splitBytes(allData, 1687);
            for (int i = 0; i < times; i++) {
                // 开始分块发送文件
                byte[] dataTemp = temp[i];
                udp = new UDP(sourcePort, targetPort, Integer.toString(seqNum), "0", "0", "0", "0", "4", dataTemp);
                byte[] udpByte = UDPutils.objectToByte(udp);
                DatagramPacket packet = new DatagramPacket(udpByte, 0, udpByte.length, targetIp, Integer.parseInt(targetPort));
                int biaoZhi;
                do {
                    socket.send(packet);
                    int j = i + 1;
                    System.out.println(state + "  发送分块 " + j + " Seq = " + seqNum);
                    // 等待每块文件的ACK
                    biaoZhi = UDPutils.receiveAck(socket, seqNum, state);
                } while (biaoZhi != 1);
                seqNum++;
            }
            System.out.println("文件发送成功！！！");
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * Client和Server采用滑动窗口方式发送文件 固定窗口大小为4
     *
     * @return 成功返回 true 失败返回 false
     */
    public boolean sendFile2() {
        UDP udp;
        try {
            reader = new BufferedInputStream(new FileInputStream(fileSource));
            int fileLength = reader.available();
            byte[] allData = new byte[fileLength];
            int times = (fileLength / (2048)) + 1;
            System.out.println("\n文件大小为：" + reader.read(allData, 0, fileLength) + "bit");
            System.out.println("文件将分为：" + times + " 个数据报进行发送。");
            int tempSeq = seqNum;

            // 将文件拆分成单个数据报
            byte[][] temp = UDPutils.splitBytes(allData, 1687);
            DatagramPacket[] packet = new DatagramPacket[times];
            for (int i = 0; i < times; i++) {
                byte[] dataTemp = temp[i];
                udp = new UDP(sourcePort, targetPort, Integer.toString(tempSeq), "0", "0", "0", "0", "4", dataTemp);
                tempSeq++;
                byte[] udpByte = UDPutils.objectToByte(udp);
                packet[i] = new DatagramPacket(udpByte, 0, udpByte.length, targetIp, Integer.parseInt(targetPort));
            }
            // 为每一个数据报设置一个标志状态位
            int[] packetState = new int[times];
            // 记录发送成功数据报个数
            int sendPacketNums = 0;

            // 开始发送
            int head = 0, tail = head + windowSize - 1;
            tempSeq = seqNum;
            int x = 0;
            while (true) {
                // 如果所有数据报发送完毕，就开始准备断开连接
                if (sendPacketNums == times) {
                    System.out.println("文件发送成功！！！");
                    return true;
                }
                // 判断窗口中所有数据报的状态，将未发送的全部发送出去
                for (int i = head; i <= tail; i++) {
                    x = seqNum + i;
                    if (packetState[head] == 1) {
                        head++;
                        if (tail != (times - 1)) {
                            tail++;
                        }
                    }
                    if (packetState[i] == 0 && x < tempSeq) {
                        socket.send(packet[i]);
                        System.out.println(state + "  重新发送分块 " + (i + 1) + " Seq = " + x);
                    } else if (packetState[i] == 0) {
                        socket.send(packet[i]);
                        System.out.println(state + "  发送分块 " + (i + 1) + " Seq = " + x);
                        tempSeq++;
                    }
                }

                // 等待ACK，当收到窗口头部对应的ACK时，窗口右移，等待超时则进行重传
                int headTemp = head, tailTemp = tail;
                for (int i = headTemp; i <= tailTemp; i++) {
                    byte[] rec = new byte[512];
                    DatagramPacket receivePacket = new DatagramPacket(rec, 0, rec.length);

                    try {
                        // 定时10ms
                        socket.setSoTimeout(10);
                        socket.receive(receivePacket);
                    } catch (IOException e) {
                        continue;
                    }
                    if (UDPutils.byteToObject(rec) == null) {
                        continue;
                    }
                    udp = (UDP) UDPutils.byteToObject(rec);
                    // 判断收到的 ACK 是响应哪个数据报的，若是窗口头部的数据报，则窗口右移一个单位，若该 ACK 响应的数据报不在当前窗口，则不做相应操作
                    int ackTemp = (Integer.parseInt(udp.getAck()) - 1) - seqNum;
                    if (ackTemp >= head && ackTemp <= tail) {
                        System.out.println(state + "  接收ACK ack = " + udp.getAck());
                        packetState[ackTemp] = 1;
                        sendPacketNums++;
                        if (ackTemp == head) {
                            head++;
                            if (tail != (times - 1)) {
                                tail++;
                            }
                        }
                    }
                }
//                System.out.println(Arrays.toString(packetState));
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 文件发送完毕，开始与Server断开连接，进行四次挥手
     *
     * @return 断开成功返回true，反之返回false
     * @throws IOException 挥手异常
     * @throws ClassNotFoundException 挥手异常
     */
    public boolean waveHands() throws IOException, ClassNotFoundException {
        UDP udp;
        while (true) {
            switch (state) {
                case "ESTAB-LISHEN":
                    // 发送 FIN=1，seq=u, 并且进入 FIN-WAIT-1 等待Server响应
                    System.out.println("\n开始与 " + UDPutils.getStringIp(targetIp) + " 断开连接。");
                    udp = new UDP(sourcePort, targetPort, Integer.toString(seqNum), "0", "0", "0", "1", "0", null);
                    byte[] udpBytes1 = UDPutils.objectToByte(udp);
                    DatagramPacket sendPacket = new DatagramPacket(udpBytes1, 0, udpBytes1.length, targetIp, Integer.parseInt(targetPort));

                    System.out.println(state + " 发送：FIN=1，seq=" + seqNum);
                    socket.send(sendPacket);
                    seqNum++;
                    state = "FIN-WAIT-1";

                case "FIN-WAIT-1":
                    // 客户端进入 FIN-WAIT-1 状态等待Server响应
                    byte[] udpBytes2 = new byte[512];
                    DatagramPacket receivePacket = new DatagramPacket(udpBytes2, 0, udpBytes2.length);
                    socket.setSoTimeout(4000);
                    try {
                        socket.receive(receivePacket);
                    } catch (SocketTimeoutException e) {
                        System.out.println(state + "  等待ACK超时，重新请求断开连接。。。");
                        state = "ESTAB-LISHEN";
                        continue;
                    }

                    if (UDPutils.byteToObject(udpBytes2) != null) {
                        udp = (UDP) UDPutils.byteToObject(udpBytes2);
                    } else {
                        System.out.println("接收发生错误，连接失败！");
                        state = "ESTAB-LISHEN";
                        continue;
                    }
                    // 收到响应，判断各项数值是否符合要求,符合进入 FIN-WAIT-2 状态，等待Server发起连接断开请求，反之Client重新发起，断开请求
                    if ("1".equals(udp.getACK()) && seqNum == Integer.parseInt(udp.getAck())) {
                        System.out.println(state + "  接收：ACK=1，seq=" + udp.getSequence_Number() + "，ack=" + udp.getAck() + " request successful");
                        state = "FIN-WAIT-2";
                    } else {
                        System.out.println(state + "  接收：ACK=1，seq=" + udp.getSequence_Number() + "，ack=" + udp.getAck() + " request failed");
                        state = "ESTAB-LISHEN";
                        continue;
                    }

                case "FIN-WAIT-2":
                    // 进入 FIN-WAIT-2 状态，等待Server发起连接断开请求
                    byte[] udpBytes3 = new byte[512];
                    DatagramPacket receivePacket2 = new DatagramPacket(udpBytes3, 0, udpBytes3.length);
                    try {
                        socket.receive(receivePacket2);
                    } catch (SocketTimeoutException e) {
                        System.out.println(state + "  等待Server端的断开请求超时，Client重新进行断开请求。。。\n");
                        state = "ESTAB-LISHEN";
                        continue;
                    }

                    if (UDPutils.byteToObject(udpBytes3) != null) {
                        udp = (UDP) UDPutils.byteToObject(udpBytes3);
                    } else {
                        System.out.println("接收发生错误，连接失败！");
                        state = "ESTAB-LISHEN";
                        continue;
                    }

                    // 对接收到请求作出判断，判断请求是否合理，合理则，发出响应，并且进入CLOSED状态
                    ack = Integer.parseInt(udp.getSequence_Number()) + 1;
                    if ("1".equals(udp.getACK()) && "1".equals(udp.getFin()) && seqNum == Integer.parseInt(udp.getAck())) {
                        System.out.println(state + "  接收：FIN=1，ACK=1，seq=" + udp.getSequence_Number() + "，ack=" + udp.getAck());
                    } else {
                        System.out.println(state + "  接收：SYN=1，ACK=1，seq=" + udp.getSequence_Number() + "，ack=" + udp.getAck());
                        state = "ESTAB-LISHEN";
                        continue;
                    }

                    // 响应Server发起的断开请求,并且进入CLOSED状态，此次连接结束
                    udp = new UDP(sourcePort, targetPort, Integer.toString(seqNum), "1", Integer.toString(ack), "0", "0", "0", null);
                    byte[] udpBytes4 = UDPutils.objectToByte(udp);
                    DatagramPacket sendPacket2 = new DatagramPacket(udpBytes4, 0, udpBytes4.length, targetIp, Integer.parseInt(targetPort));
                    System.out.println(state + " 发送：ACK=1，seq=" + seqNum + "，ack=" + ack + " return successful");
                    socket.send(sendPacket2);
                    seqNum++;
                    state = "CLOSED";

                case "CLOSED":
                    socket.close();
                    return true;
                default:
                    break;
            }
        }
    }

}
