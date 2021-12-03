import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

/**
 * UDP工具类
 *
 * @author : LiuYi
 * @version : 2.0
 * @date : 2021/11/25 16:35
 */
public class UDPutils {

    /**
     * 将类转换为字节流
     *
     * @param data 需要转换的类
     * @return 返回转换成功的字节流
     * @throws IOException 序列化异常
     */
    public static byte[] objectToByte(UDP data) throws IOException {
        // object to byte
        byte[] bytes = null;
        try {
            ByteArrayOutputStream otb = new ByteArrayOutputStream();
            ObjectOutputStream objectToByte = new ObjectOutputStream(otb);
            objectToByte.writeObject(data);
            bytes = otb.toByteArray();

            objectToByte.close();
            otb.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bytes;
    }

    /**
     * 将字节流数据转换为类
     *
     * @param data 需要转换的字节流
     * @return 返回转换成功的类
     * @throws ClassNotFoundException 反序列化异常
     */
    public static Object byteToObject(byte[] data) throws ClassNotFoundException {
        // byte to object
        Object obj = null;
        try {
            ByteArrayInputStream bi = new ByteArrayInputStream(data);
            ObjectInputStream oi = new ObjectInputStream(bi);

            obj = oi.readObject();
            bi.close();
            oi.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return obj;
    }

    /**
     * CRC32计算传入字节数组的校验和
     *
     * @param bytes 目标字节数组
     * @return 返回校验和的字节流
     */
    public static byte[] getChecksum(byte[] bytes) {
        Checksum crc32 = new CRC32();
        crc32.update(bytes, 0, bytes.length);
        return Long.toString(crc32.getValue()).getBytes();
    }

    /**
     * 利用校验和对数据报进行校验
     *
     * @param oldChecksum 数据报中的校验和
     * @param data        该服务根据数据区新生成的校验和
     * @return 相同返回true 不相同返回false
     */
    public static boolean compareChecksum(byte[] oldChecksum, byte[] data) {
        byte[] newChecksum = getChecksum(data);
        return Arrays.equals(oldChecksum, newChecksum);
    }

    /**
     * 生成Seq随机数
     *
     * @return 返回一个随机数
     */
    public static int getSeqNum() {
        Random r = new Random();
        return r.nextInt(100);
    }

    /**
     * 将IP转换为字符串形式
     *
     * @param Ip 目的IP
     * @return 返回转换成功的IP字符串
     */
    public static String getStringIp(InetAddress Ip) {
        String temp = Ip.toString();
        temp = temp.substring(temp.lastIndexOf("/") + 1);
        return temp;
    }

    /**
     * 拆分byte数组
     *
     * @param bytes 要拆分的数组
     * @param size  要按几个组成一份
     * @return 返回拆分好的数组
     */
    public static byte[][] splitBytes(byte[] bytes, int size) {
        double splitLength = Double.parseDouble(size + "");
        int arrayLength = (int) Math.ceil(bytes.length / splitLength);
        byte[][] result = new byte[arrayLength][];
        int from, to;
        for (int i = 0; i < arrayLength; i++) {
            from = (int) (i * splitLength);
            to = (int) (from + splitLength);
            if (to > bytes.length) {
                to = bytes.length;
            }
            result[i] = Arrays.copyOfRange(bytes, from, to);
        }
        return result;
    }

    /**
     * 合并byte数组
     *
     * @param byte1 原数组
     * @param byte2 要添加的数组
     * @return 返回合并后的数组
     */
    public static byte[] joinByteArray(byte[] byte1, byte[] byte2) {
        return ByteBuffer.allocate(byte1.length + byte2.length)
                .put(byte1)
                .put(byte2)
                .array();

    }

    /**
     * Client 发送文件后等待ACK
     *
     * @param socket socket
     * @param seqNum 序列号
     * @param state  状态
     * @return 返回状态
     * @throws IOException            接收数据报出错
     * @throws ClassNotFoundException 接收数据报出错
     */
    public static int receiveAck(DatagramSocket socket, int seqNum, String state) throws IOException, ClassNotFoundException {
        // 接收应答ACK
        byte[] rec = new byte[512];
        DatagramPacket receivePacket = new DatagramPacket(rec, 0, rec.length);
        UDP udp = null;
        try {
            // 定时10毫秒
            socket.setSoTimeout(1);
            socket.receive(receivePacket);
        } catch (IOException e) {
            System.out.println(state + "  Seq = " + seqNum + " 数据报丢失，重新发送分块");
            return 0;
        }
        // 对ACK进行判断
        if (UDPutils.byteToObject(rec) != null) {
            udp = (UDP) UDPutils.byteToObject(rec);
        }
        assert udp != null;
        int ack = Integer.parseInt(udp.getAck());
        if ("1".equals(udp.getACK()) && (seqNum + 1) == ack) {
            System.out.println(state + "  接收ACK ack = " + ack);
            return 1;
        }
        return 0;
    }

}

