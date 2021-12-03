import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Objects;

/**
 * UDP数据包
 *
 * @author : LiuYi
 * @version : 2.0
 * @date : 2021/11/25 15:23
 */
public class UDP implements Serializable {
    private static final long serialVersionUID = -7421277184322913905L;

    /**
     * source_Port：         源端口 2字节
     * destination_Port：    目的端口 2字节
     * length：              数据报总长度 2字节
     * Checksum：            检验和 2字节
     * <p>
     * sequence_Number：     确认号 4字节
     * ACK：                 确认ACK 1字节
     * ack：                 ack对应seq
     * SYN：                 同步SYN
     * Fin：                 终止FIN
     * Window_Size：         窗口大小
     */
    private byte[] source_Port = new byte[2];
    private byte[] destination_Port = new byte[2];
    private byte[] source_IP = new byte[2];
    private int length = 0;
    private byte[] Checksum = new byte[2];

    private byte[] sequence_Number = new byte[4];
    private byte[] ACK = new byte[1];
    private byte[] ack = new byte[4];
    private byte[] Syn = new byte[1];
    private byte[] Fin = new byte[1];
    private byte[] Window_Size = new byte[2];
    private byte[] data;

    public UDP() throws IOException {
    }

    /**
     * 初始化
     *
     * @param source_Port      源端口
     * @param destination_Port 目的端口
     * @throws IOException 初始化错误
     */
    public UDP(String source_Port, String destination_Port) throws IOException {
        this.source_Port = source_Port.getBytes();
        this.destination_Port = destination_Port.getBytes();
    }

    /**
     * 初始化
     *
     * @param source_Port      源端口
     * @param destination_Port 目的端口
     * @param sequence_Number  确认号
     * @param ACK              确认ACK
     * @param ack              回复ack序号
     * @param syn              同步SYN
     * @param fin              终止FIN
     * @param window_Size      窗口大小
     * @param data             数据
     * @throws IOException 初始化错误
     */
    public UDP(String source_Port, String destination_Port, String sequence_Number, String ACK, String ack, String syn, String fin, String window_Size, byte[] data) throws IOException {
        this.source_Port = source_Port.getBytes();
        this.destination_Port = destination_Port.getBytes();
        this.sequence_Number = sequence_Number.getBytes();
        this.ACK = ACK.getBytes();
        this.ack = ack.getBytes();
        Syn = syn.getBytes();
        Fin = fin.getBytes();
        Window_Size = window_Size.getBytes();
        this.data = data;
        setChecksum(data);
        setLength();
    }

    public String getSource_IP() throws UnsupportedEncodingException {
        return new String(source_IP, "utf-8");
    }

    public void setSource_IP(String source_IP) {
        this.source_IP = source_IP.getBytes();
    }

    public String getSource_Port() throws UnsupportedEncodingException {
        return new String(source_Port, "utf-8");
    }

    public void setSource_Port(String source_Port) {
        this.source_Port = source_Port.getBytes();
    }

    public String getDestination_Port() throws UnsupportedEncodingException {
        return new String(destination_Port, "utf-8");
    }

    public void setDestination_Port(String destination_Port) {
        this.destination_Port = destination_Port.getBytes();
    }

    public int getLength() {
        return length;
    }

    public void setLength() {
        if (data == null) {
            this.length = 35;
        } else {
            this.length = 35 + data.length;
        }
    }

    public byte[] getChecksum() throws UnsupportedEncodingException {
        return Checksum;
    }

    public void setChecksum(byte[] data) {
        if (data == null) {
            this.Checksum = new byte[0];
        } else {
            this.Checksum = UDPutils.getChecksum(data);
        }
    }

    public String getSequence_Number() throws UnsupportedEncodingException {
        return new String(sequence_Number, "utf-8");
    }

    public void setSequence_Number(String sequence_Number) {
        this.sequence_Number = sequence_Number.getBytes();
    }

    public String getACK() throws UnsupportedEncodingException {
        return new String(ACK, "utf-8");
    }

    public void setACK(String ACK) {
        this.ACK = ACK.getBytes();
    }

    public String getAck() throws UnsupportedEncodingException {
        return new String(ack, "utf-8");
    }

    public void setAck(String ack) {
        this.ack = ack.getBytes();
    }

    public String getSyn() throws UnsupportedEncodingException {
        return new String(Syn, "utf-8");
    }

    public void setSyn(String syn) {
        Syn = syn.getBytes();
    }

    public String getFin() throws UnsupportedEncodingException {
        return new String(Fin, "utf-8");
    }

    public void setFin(String fin) {
        Fin = fin.getBytes();
    }

    public String getWindow_Size() throws UnsupportedEncodingException {
        return new String(Window_Size, "utf-8");
    }

    public void setWindow_Size(String window_Size) {
        Window_Size = window_Size.getBytes();
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UDP udp = (UDP) o;
        return length == udp.length &&
                Arrays.equals(source_Port, udp.source_Port) &&
                Arrays.equals(destination_Port, udp.destination_Port) &&
                Arrays.equals(Checksum, udp.Checksum) &&
                Arrays.equals(sequence_Number, udp.sequence_Number) &&
                Arrays.equals(ACK, udp.ACK) &&
                Arrays.equals(ack, udp.ack) &&
                Arrays.equals(Syn, udp.Syn) &&
                Arrays.equals(Fin, udp.Fin) &&
                Arrays.equals(Window_Size, udp.Window_Size) &&
                Arrays.equals(data, udp.data);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(length);
        result = 31 * result + Arrays.hashCode(source_Port);
        result = 31 * result + Arrays.hashCode(destination_Port);
        result = 31 * result + Arrays.hashCode(Checksum);
        result = 31 * result + Arrays.hashCode(sequence_Number);
        result = 31 * result + Arrays.hashCode(ACK);
        result = 31 * result + Arrays.hashCode(ack);
        result = 31 * result + Arrays.hashCode(Syn);
        result = 31 * result + Arrays.hashCode(Fin);
        result = 31 * result + Arrays.hashCode(Window_Size);
        result = 31 * result + Arrays.hashCode(data);
        return result;
    }

    @Override
    public String toString() {
        String temp = null;
        try {
            temp = "UDP{" +
                    "source_IP = " + getSource_IP() +
                    ", \n    source_Port = " + getSource_Port() +
                    ", \n    destination_Port = " + getDestination_Port() +
                    ", \n    length = " + getLength() +
                    ", \n    Checksum = " + new String(Checksum, "utf-8") +
                    ", \n    sequence_Number = " + getSequence_Number() +
                    ", \n    ACK = " + getACK() +
                    ", \n    ack = " + getAck() +
                    ", \n    Syn = " + getSyn() +
                    ", \n    Fin = " + getFin() +
                    ", \n    Window_Size = " + getWindow_Size() +
                    ", \n    data = " + data +
                    '}';
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return temp;
    }
}
