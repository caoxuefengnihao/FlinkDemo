package Time;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

public class MySocket {
    private static Scanner sc = new Scanner(System.in);
    private static DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss S");
    private static ServerSocket server ;
    private static Socket socket ;
    private static final int PORT = 9999;
    private static OutputStream os ;
    private static InputStream is ;
    private static BufferedWriter bw ;
    private static BufferedReader br ;

    public static void initServer() throws Exception{
        server = new ServerSocket(PORT);
        socket = server.accept();
        os = socket.getOutputStream();
        is = socket.getInputStream();
        bw = new BufferedWriter(new OutputStreamWriter(os));
        br = new BufferedReader(new InputStreamReader(is));
    }


    public static void main(String[] args) throws Exception {
        initServer();
        InetAddress address = socket.getInetAddress();
        System.out.println("\t\t"+address.getHostAddress()+" "+address.getHostName());
        System.out.println("*******************    服务端    *******************");

        while( true ){
            // 收
//            read();

            // 发
            writer();
        }

    }


        /**
         * 发送消息至客户端
         *
         * @throws IOException
         */
    public static void writer() throws IOException {
        bw.write(getResult());
        bw.newLine();
        bw.flush();
    }

        /**
         * 接收客户端的消息
         * @throws IOException
         */
    public static void read() throws IOException{
        String line = br.readLine();
        System.out.println("接收消息  : "+line);
    }

        /**
         * 键盘录入消息 附带时间
         * @return
         */
    public static String getResult(){
        String result = sc.nextLine();
//        String date = df.format(new Date());
//        result += "\t"+date ;
        System.out.println("发送消息 : "+result);
        return result;
    }

}
