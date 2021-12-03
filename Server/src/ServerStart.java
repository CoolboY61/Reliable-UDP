import java.net.UnknownHostException;

/**
 * 启动Server
 *
 * @author : LiuYi
 * @version : 2.0
 * @date : 2021/11/24 21:15
 */
public class ServerStart {
    public static void main(String[] args) throws UnknownHostException {
        new Thread(new Server("127.0.0.1", "2222", "E:\\非编程\\学校作业（word）\\网络协议分析\\大作业\\ServerFIle\\test.txt")).start();
    }
}
