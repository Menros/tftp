import java.io.*;
import java.net.*;

/**
 * Created by franck on 5/19/16.
 */
public class Client {

    //Attributes

    private DatagramSocket clientSocket;
    private DatagramPacket clientPacket;
    private int clientPort;

    //Constructor

    public Client (){
        try {
            this.clientSocket = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
        }
        this.clientPort = 69;
        this.clientPacket = new DatagramPacket(new byte[512], 512);
    }

    //Methods

    public void write (String fileName){

        byte[] host = { (byte)0, (byte)0, (byte)0, (byte)0};
        InetAddress ias = null;

        try {
            ias = InetAddress.getByAddress(host);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        //Open file

        File fic = new File(new String(fileName));
        try {
            FileInputStream stream = new FileInputStream(fic);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }


        //Create WRQ DatagramPacket

        String str = "\0\2"+fileName+"\0"+"octet"+"\0";
        DatagramPacket wrqPacket = new DatagramPacket(str.getBytes(), str.length(), ias, clientPort);

        //Connection

        do {
            try {
                clientSocket.send(wrqPacket);
            } catch (IOException e) {
                e.printStackTrace();
            }

            //Receive

            clientPacket = new DatagramPacket(new byte[4], 4);

            try {
                clientSocket.receive(clientPacket);
            } catch (SocketTimeoutException e) {
                System.out.println("Connection timed out - Retry");
                continue;
            } catch (IOException e) {
                e.printStackTrace();
            }
        } while(! new String("\0\4\0").equals(new String(clientPacket.getData())));

    }

}
