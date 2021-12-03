import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * 服务端 负责接收数据
 *
 * @author : LiuYi
 * @version : 2.0
 * @date : 2021/11/20 16:19
 */
public class Server implements Runnable {
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
    private InetAddress targetIp;
    /**
     * 目的端口
     **/
    private String targetPort;
    /**
     * 源文件路径
     **/
    private final File fileSource;
    /**
     * seq序号
     **/
    private int seqNum;
    /**
     * 应答ack序号
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
     * 文件输入缓冲流
     **/
    BufferedOutputStream receiver = null;

    private long now;
    private long start;
    private long time;

    @Override
    public void run() {
        try {
            start = System.currentTimeMillis();
            if (shakeHands()) {
                if (receiveFile2()) {
                    now = System.currentTimeMillis();
                    time = now - start;
                    System.out.println("耗时：" + time + "ms");
                    if (waveHands()) {
                        System.out.println(state + "  与 " + UDPutils.getStringIp(targetIp) + " 成功断开连接！\n");
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * 初始化Server 初始化Server的Socket，以及IO流，并且设置窗口大小为4
     *
     * @param sourceIp   源IP
     * @param sourcePort 源端口
     * @param fileSource 源文件路径
     * @throws UnknownHostException 初始化异常
     */
    public Server(String sourceIp, String sourcePort, String fileSource) throws UnknownHostException {
        this.sourceIp = InetAddress.getByName(sourceIp);
        this.sourcePort = sourcePort;
        this.fileSource = new File(fileSource);
        this.windowSize = 8;
        try {
            socket = new DatagramSocket(Integer.parseInt(sourcePort));
        } catch (SocketException e) {
            e.printStackTrace();
            socket.close();
        }
    }

    /**
     * Server开始等待Client的握手请求
     *
     * @return 握手成功返回 true 失败返回 false
     * @throws IOException 握手异常
     * @throws ClassNotFoundException 握手异常
     */
    public boolean shakeHands() throws IOException, ClassNotFoundException {
        UDP shakeHandsUdp;
        receiver = new BufferedOutputStream(new FileOutputStream(fileSource));
        state = "LISTEN";
        while (true) {
            switch (state) {
                case "LISTEN":
                    System.out.println("等待连接-----");
                    seqNum = UDPutils.getSeqNum();

                    // 等待客户端的请求连接
                    byte[] udpBytes1 = new byte[512];
                    DatagramPacket receivePacket = new DatagramPacket(udpBytes1, 0, udpBytes1.length);
                    try {
                        socket.setSoTimeout(0);
                        socket.receive(receivePacket);
                    } catch (IOException e) {
                        System.out.println(state + "  等待连接超时。");
                    }
                    shakeHandsUdp = (UDP) UDPutils.byteToObject(udpBytes1);

                    // 收到Client的握手请求，判断各项数值是否合理，合理即设置目的IP和目的端口，并且响应Client的请求，同时进入SYN-RCVD状态
                    //                                        不合理，重新进入 LISTEN 状态，等待Client重新请求握手
                    ack = Integer.parseInt(shakeHandsUdp.getSequence_Number());
                    if ("1".equals(shakeHandsUdp.getSyn())) {
                        System.out.println("开始与 " + UDPutils.getStringIp(sourceIp) + " 建立连接。");
                        System.out.println(state + "  接收：SYN=1，seq=" + ack + "  return successful");
                        targetIp = InetAddress.getByName(shakeHandsUdp.getSource_IP());
                        targetPort = shakeHandsUdp.getSource_Port();
                        ack++;

                        // 发送：SYN=1，ACK=1，seq=y，ack=x+1
                        shakeHandsUdp = new UDP(sourcePort, targetPort, Integer.toString(seqNum), "1", Integer.toString(ack), "1", "0", Integer.toString(windowSize), null);
                        byte[] udpBytes2 = UDPutils.objectToByte(shakeHandsUdp);
                        DatagramPacket sendPacket = new DatagramPacket(udpBytes2, 0, udpBytes2.length, targetIp, Integer.parseInt(targetPort));
                        System.out.println(state + "  发送：SYN=1，ACK=1，seq=" + seqNum + "，ack=" + ack);
                        socket.send(sendPacket);
                        seqNum++;
                        ack++;
                        state = "SYN-RCVD";
                    } else {
                        System.out.println(state + "  接收：SYN=1，seq=" + ack + "  return failed");
                        state = "LISTEN";
                        continue;
                    }

                case "SYN-RCVD":
                    // 等待Client的确认
                    byte[] rec2 = new byte[512];
                    DatagramPacket receivePacket2 = new DatagramPacket(rec2, 0, rec2.length);
                    try {
                        socket.setSoTimeout(4000);
                        socket.receive(receivePacket2);
                    } catch (SocketTimeoutException e) {
                        System.out.println(state + "  等待ACK超时。。。连接失败。。。");
                        state = "LISTEN";
                        continue;
                    }
                    // 收到确认，判断各项数值是否符合要求,成功进入 ESTAB-LISHEN 状态
                    shakeHandsUdp = (UDP) UDPutils.byteToObject(rec2);
                    int recAck = Integer.parseInt(shakeHandsUdp.getAck());
                    int recSeq = Integer.parseInt(shakeHandsUdp.getSequence_Number());
                    if ("1".equals(shakeHandsUdp.getACK()) && seqNum == recAck) {
                        System.out.println(state + "  接收：ACK=1，seq=" + recAck + "，ack=" + recSeq + "  request successful");
                        seqNum++;
                        state = "ESTAB-LISHEN";
                    } else {
                        System.out.println(state + "  接收：SYN=1，ACK=1，seq=" + recAck + "，ack=" + recSeq + "  request failed ");
                        state = "LISTEN";
                        continue;
                    }

                case "ESTAB-LISHEN":
                    // 进入 ESTAB-LISHEN 状态，准备开始接收文件
                    System.out.println(state + "  与 " + UDPutils.getStringIp(targetIp) + " 连接建立成功！");
                    return true;
                default:
                    System.out.println("与  " + UDPutils.getStringIp(targetIp) + "  连接建立失败！！！");
                    return false;
            }
        }
    }

    /**
     * Server接收Client发送的文件
     *
     * @return 接收成功返回true，反之返回false
     */
    public boolean receiveFile1() {
        System.out.println("\n等待接收文件。。。。");
        byte[] data = new byte[0];
        UDP udp;
        try {
            receiver = new BufferedOutputStream(new FileOutputStream(fileSource));
            while (true) {
                byte[] temp = new byte[2060];
                DatagramPacket receivePacket = new DatagramPacket(temp, 0, temp.length);
                try {
                    // 等待100ms，若100ms未再接收到文件，则说明文件传输结束，并且将数据输入文件中
                    socket.setSoTimeout(100);
                    socket.receive(receivePacket);
                } catch (IOException e) {
                    System.out.println(state + "  文件接收完毕！ 共接收了 " + data.length + " bit的数据。");
                    System.out.println("接收成功！");
                    // 数据写入文件
                    receiver.write(data);
                    receiver.flush();
                    receiver.close();
                    return true;
                }
                // 设置丢包率为0.1
                if (Math.random() <= 0.1) {
                    continue;
                }
                udp = (UDP) UDPutils.byteToObject(temp);
                if (udp == null) {
                    continue;
                }
                temp = udp.getData();
                if ((temp == null) || !UDPutils.compareChecksum(udp.getChecksum(), temp)) {
                    System.out.println(state + "  接收 Seq = " + udp.getSequence_Number() + " 数据报出现错误丢弃，等待重传。");
                    continue;
                }

                System.out.println(state + "  接收 Seq = " + udp.getSequence_Number() + "  回复 Seq = " + seqNum + "，Ack = " + ack);
                // 拼接文件块
                data = UDPutils.joinByteArray(data, temp);

                udp = new UDP(sourcePort, targetPort, Integer.toString(seqNum), "1", String.valueOf(ack), "0", "0", "4", null);
                temp = UDPutils.objectToByte(udp);
                DatagramPacket dp = new DatagramPacket(temp, 0, temp.length, targetIp, Integer.parseInt(targetPort));
                socket.send(dp);
                seqNum++;
                ack++;
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * Client和Server采用滑动窗口方式发送文件 固定窗口大小为4
     *
     * @return 文件接收成功后返回true，反之返回false
     * @throws IOException 接收文件异常
     * @throws ClassNotFoundException 接收文件异常
     */
    public boolean receiveFile2() throws IOException, ClassNotFoundException {
        System.out.println("\n等待接收文件。。。。");
        byte[] data = new byte[0];
        UDP udp;
        try {
            receiver = new BufferedOutputStream(new FileOutputStream(fileSource));
            Map<Integer, byte[]> map = new HashMap<>();
            while (true) {
                byte[] temp = new byte[2060];
                DatagramPacket receivePacket = new DatagramPacket(temp, 0, temp.length);
                try {
                    // 等待100ms，若100ms未再接收到文件，则说明文件传输结束，并且将数据输入文件中
                    socket.setSoTimeout(100);
                    socket.receive(receivePacket);
                } catch (IOException e) {
                    break;
                }
                // 设置丢包率为0.1
                if (Math.random() <= 0.1) {
                    continue;
                }
                udp = (UDP) UDPutils.byteToObject(temp);
                if (udp == null) {
                    continue;
                }
                temp = udp.getData();
                int seq = Integer.parseInt(udp.getSequence_Number());
                // 利用校验和判断接收到的数据报中数据是否发生错误
                if ((temp == null) || !UDPutils.compareChecksum(udp.getChecksum(), temp)) {
                    System.out.println(state + "  接收 Seq = " + seq + " 数据报出现错误丢弃，等待重传。");
                    continue;
                }
                int ack = seq + 1;

                // 判断该数据报是否已经接收过了，接收过，数据报丢弃只回复ACK，反之保存将数据报放入缓存，再回复ACK
                if (!map.containsKey(seq)) {
                    map.put(seq, temp);
                    System.out.println(state + "  接收 Seq = " + seq + "  回复 Seq = " + seqNum + "，Ack = " + ack);
                } else {
                    System.out.println(state + "  接收 Seq = " + seq + "  数据报重复，丢弃。");
                }
                udp = new UDP(sourcePort, targetPort, Integer.toString(seqNum), "1", String.valueOf(ack), "0", "0", "4", null);
                temp = UDPutils.objectToByte(udp);
                DatagramPacket dp = new DatagramPacket(temp, 0, temp.length, targetIp, Integer.parseInt(targetPort));
                socket.send(dp);
                seqNum++;
            }
            // 按顺序获取接收数据报的seq序号
            Set<Integer> keySet = map.keySet();
            // 获取seq的迭代器
            // 将接收到的数据报中数据按照顺序输入文件中
            for (Integer sq : keySet) {
                byte[] dataTemp = map.get(sq);
                data = UDPutils.joinByteArray(data, dataTemp);
            }
            System.out.println(state + "  文件接收完毕！ 共接收了 " + data.length + " bit的数据。");
            System.out.println("接收成功！");
            // 数据写入文件
            receiver.write(data);
            receiver.flush();
            receiver.close();
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 文件接收完毕，等待Client断开连接
     *
     * @return 断开成功返回true，反之返回false
     * @throws ClassNotFoundException 挥手异常
     * @throws IOException 挥手异常
     */
    public boolean waveHands() throws ClassNotFoundException, IOException {
        System.out.println("\n等待Client发起断开连接请求。。。");
        UDP udp;
        while (true) {
            switch (state) {
                case "ESTAB-LISHEN":
                    // 文件接收完毕，服务器等待客户端发出断开连接请求
                    byte[] udpBytes1 = new byte[512];
                    DatagramPacket receivePacket = new DatagramPacket(udpBytes1, 0, udpBytes1.length);
                    try {
                        socket.setSoTimeout(0);
                        socket.receive(receivePacket);
                    } catch (IOException e) {
                        System.out.println(state + "  socket出现异常");
                    }
                    udp = (UDP) UDPutils.byteToObject(udpBytes1);
                    ack = Integer.parseInt(udp.getSequence_Number());

                    // 接收后，查看报文是否合理，合理即发送应答，并且进入 CLOSE-WAIT 状态
                    if ("1".equals(udp.getFin())) {
                        System.out.println(state + "  开始与 " + UDPutils.getStringIp(sourceIp) + " 断开建立连接。");
                        System.out.println(state + "  接收：FIN=1，seq=" + ack + "  return successful");
                        ack++;
                        udp = new UDP(sourcePort, targetPort, Integer.toString(seqNum), "1", Integer.toString(ack), "0", "0", "0", null);
                        byte[] udpBytes2 = UDPutils.objectToByte(udp);
                        DatagramPacket sendPacket = new DatagramPacket(udpBytes2, 0, udpBytes2.length, targetIp, Integer.parseInt(targetPort));
                        System.out.println(state + "  发送：ACK=1，seq=" + seqNum + "，ack=" + ack);
                        socket.send(sendPacket);

                        state = "CLOSE-WAIT";
                        seqNum++;
                    } else {
                        System.out.println(state + "  接收：FIN=1，seq=" + ack + "  return failed");
                        state = "ESTAB-LISHEN";
                        continue;
                    }

                case "CLOSE-WAIT":
                    // 进入 CLOSE-WAIT 状态，向Client发起断开连接请求，同时进入 LAST-ACK 状态，等待Client的确认
                    udp = new UDP(sourcePort, targetPort, Integer.toString(seqNum), "1", Integer.toString(ack), "0", "1", "0", null);
                    byte[] udpBytes3 = UDPutils.objectToByte(udp);
                    DatagramPacket sendPacket2 = new DatagramPacket(udpBytes3, 0, udpBytes3.length, targetIp, Integer.parseInt(targetPort));
                    System.out.println(state + "  发送：FIN=1，ACK=1，seq=" + seqNum + "，ack=" + ack);
                    socket.send(sendPacket2);
                    state = "LAST-ACK";
                    seqNum++;

                case "LAST-ACK":
                    // 进入 LAST-ACK状态，等待Client的确认

                    byte[] udpBytes4 = new byte[512];
                    DatagramPacket receivePacket2 = new DatagramPacket(udpBytes4, 0, udpBytes4.length);
                    try {
                        socket.setSoTimeout(4000);
                        socket.receive(receivePacket2);
                    } catch (SocketTimeoutException e) {
                        System.out.println(state + "  等待ACK超时。。。连接断开失败。。。");
                        state = "LAST-ACK";
                        continue;
                    }

                    // 判断 Client 的确认，合理则进入CLOSED状态，此次连接结束
                    udp = (UDP) UDPutils.byteToObject(udpBytes4);
                    int recAck = Integer.parseInt(udp.getAck());
                    int recSeq = Integer.parseInt(udp.getSequence_Number());
                    if ("1".equals(udp.getACK()) && seqNum == recAck) {
                        System.out.println(state + "  接收：ACK=1，seq=" + recSeq + "，ack=" + recAck + "  request successful");
                        state = "CLOSED";
                        seqNum++;
                    } else {
                        System.out.println(state + "  接收：ACK=1，seq=" + recSeq + "，ack=" + recAck + "  request failed ");
                        state = "ESTAB-LISHEN";
                        continue;
                    }

                case "CLOSED":
                    socket.close();
                    return true;
                default:
                    break;
            }
        }
    }
}
