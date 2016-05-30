import com.sun.javafx.iio.ImageFormatDescription;
import com.sun.xml.internal.bind.v2.util.StackRecorder;

import java.io.*;
import java.net.*;

/**
 * Created by franck on 5/19/16.
 */
public class Client {

    //Attributes

    private DatagramSocket clientSocket;
    private DatagramPacket clientPacket;
    private int serverPort;

    //Constructor

    public Client (){
        try {
            this.clientSocket = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
        }
        this.serverPort = 69;
        this.clientPacket = new DatagramPacket(new byte[512], 512);
    }

    //Methods

    public void write (String fileName){

        byte[] host = { (byte)134, (byte)214, (byte)119, (byte)116};
        InetAddress ias = null;

        try {
            ias = InetAddress.getByAddress(host);
            System.out.println(ias);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }


        //Open file

        File fic = new File(new String(fileName));
        FileInputStream stream = null;
        System.out.println("Opening file");

        try {
            stream = new FileInputStream(fic);
            System.out.println("Creating stream");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }


        //Create WRQ DatagramPacket

        String str = "\0"+"\2"+fileName+"\0"+"octet"+"\0";
        DatagramPacket wrqPacket = new DatagramPacket(str.getBytes(), str.length(), ias, serverPort);

        //Connection

        do {
            try {
                clientSocket.send(wrqPacket);
                System.out.println("Sending WRQ");
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
        } while(clientPacket.getData()[1] != 4 && clientPacket.getData()[3] != 0);

        System.out.println("ACK "+ clientPacket.getData()[3] +" received" );

        //Running

        serverPort = clientPacket.getPort();
        System.out.println(serverPort);
        //byte fileContent[] = new byte[512];
        byte[] strData = new byte[516];
        int cpt = 0;
        int numPacket = 1;
        int sizePacket = 512;

        System.out.println("File length :" + fic.length());

        while (cpt < fic.length()) {

            //Reading 512 bytes in file

            try {
                sizePacket = stream.read(strData, 4, 512);
                //System.out.println(new String(fileContent));
                cpt += sizePacket;
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Exception while reading file " + e);
            }

            //Sending packet

            //"\0"+"\3"+ (byte)numPacket/256 + (byte)numPacket%256+new String(fileContent);

            strData[0] = 0;
            strData[1] = 3;
            strData[2] = (byte) (numPacket / 256);
            strData[3] = (byte) (numPacket % 256);
            sizePacket += 4;

            System.out.println(strData);

            DatagramPacket filePacket = new DatagramPacket(strData, sizePacket, ias, serverPort);

            byte[] ack;
            do {

                try {
                    clientSocket.send(filePacket);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                //Receiving ACK

                clientPacket = new DatagramPacket(new byte[4], 4);
                ack = new byte[4];

                try {
                    clientSocket.receive(clientPacket);
                    //System.out.println(new String(clientPacket.getData()));
                    ack = clientPacket.getData();
                } catch (SocketTimeoutException e) {
                    System.out.println("Connection timed out - Retry");
                    continue;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } while (! (ack[0] == 0 && ack[1] == 4 && ack[2] == (byte) (numPacket / 256) && ack[3] == (byte) (numPacket % 256)));

            numPacket++;
        }

        // Closing stream
        try {
            if (stream != null) {
                stream.close();
            }
        }
        catch (IOException ioe) {
            System.out.println("Error while closing stream: " + ioe);
        }
    }

}


