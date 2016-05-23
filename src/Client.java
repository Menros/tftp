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

        byte[] host = { (byte)134, (byte)214, (byte)117, (byte)185};
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

        System.out.println("ACK received " + clientPacket.getData()[3]);

        //Running

        serverPort = clientPacket.getPort();
        System.out.println(serverPort);
        byte fileContent[] = new byte[512];
        int cpt = 0;
        short numPacket = 1;

        while (cpt < fic.length()){

            //Reading 512 bytes in file

            try {
                stream.read(fileContent, cpt, 512);
                System.out.println(new String(fileContent));
                cpt += 512;
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Exception while reading file " + e);
            }

            //Sending packet

            String strData = "\0"+"\3"+numPacket+fileContent;
            DatagramPacket filePacket = new DatagramPacket(strData.getBytes(), strData.length(), ias, serverPort);

            do {

                try {
                    clientSocket.send(filePacket);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                //Receiving ACK

                clientPacket = new DatagramPacket(new byte[4], 4);

                try {
                    clientSocket.receive(clientPacket);
                } catch (SocketTimeoutException e) {
                    System.out.println("Connection timed out - Retry");
                    continue;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } while(! new String("\0\4"+numPacket).equals(new String(clientPacket.getData())));

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


