import java.net.UnknownHostException;

/**
 * 启动Client
 *
 * @author : LiuYi
 * @version : 2.0
 * @date : 2021/11/24 21:15
 */
public class ClientStart {
    public static void main(String[] args) throws UnknownHostException {
        new Thread(new Client("127.0.0.1", "1111", "127.0.0.1", "2222", "E:\\非编程\\学校作业（word）\\网络协议分析\\大作业\\ClientFile\\test.txt")).start();
//            new Thread(new Client("192.168.43.215","1111","192.168.43.3","2222","E:\\非编程\\学校作业（word）\\网络协议分析\\大作业\\ClientFile\\大作业报告.doc")).start();
    }
}