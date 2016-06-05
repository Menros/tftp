import com.sun.javafx.iio.ImageFormatDescription;
import com.sun.xml.internal.bind.v2.util.StackRecorder;
import org.omg.PortableInterceptor.SUCCESSFUL;

import java.io.*;
import java.net.*;

import static java.lang.System.err;
import static java.lang.System.exit;

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
            this.clientSocket.setSoTimeout(10000);
        } catch (SocketException e) {
            System.out.println("Socket error. Aborting...");
            exit(-3);
        }
        this.serverPort = 69;
        this.clientPacket = new DatagramPacket(new byte[512], 512);
    }

    //Methods

    public int detectError(byte[] test){
        int errorCode;
        if(test[0] == 0 && test[1] == 5){
            errorCode = (int)(test[3]);

            switch(errorCode){
                case 0:
                    System.out.println("Undefined error");
                    break;
                case 1:
                    System.out.println("File not found");
                    break;
                case 2:
                    System.out.println("Access violation");
                    break;
                case 3:
                    System.out.println("Disk full or allocation exceeded");
                    break;
                case 4:
                    System.out.println("Illegal TFTP operation");
                    break;
                case 5:
                    System.out.println("Unknown transfer ID");
                    break;
                case 6:
                    System.out.println("File already exists");
                    break;
                case 7:
                    System.out.println("No such user");
                    break;
            }
            return errorCode;
        }
        return -1;
    }

    public void closeStream(FileInputStream stream){
        try {
            if (stream != null) {
                stream.close();
            }
        }
        catch (IOException ioe) {
            System.out.println("Error while closing stream");
            exit(0);
        }
    }

    public void sendFile (String fileName, int first, int sec, int third, int fourth){

        byte[] host = { (byte)first, (byte)sec, (byte)third, (byte)fourth};
        InetAddress ias = null;
        int timedoutIndex;
        int errorCode;

        try {
            ias = InetAddress.getByAddress(host);
            //System.out.println(ias);
        } catch (UnknownHostException e) {
            System.out.println("Exception reached while getting InetAddress. Aborting...");
            exit(-7);
        }


        //Open file

        File fic = new File(new String(fileName));
        FileInputStream stream = null;
        System.out.println("Opening file");

        try {
            stream = new FileInputStream(fic);
            System.out.println("Creating stream");
        } catch (FileNotFoundException e) {
            System.out.println("Error while opening file. Aborting...");
            exit(-1);
        }


        //Create WRQ DatagramPacket

        String str = "\0"+"\2"+fileName+"\0"+"octet"+"\0";
        DatagramPacket wrqPacket = new DatagramPacket(str.getBytes(), str.length(), ias, serverPort);
        timedoutIndex = 0;

        //Connection

        do {
            try {
                clientSocket.send(wrqPacket);
                System.out.println("Sending WRQ");
            } catch (IOException e) {
                System.out.println("Failed. Aborting...");
                closeStream(stream);
                exit(-5);
            }

            //Receive

            clientPacket = new DatagramPacket(new byte[4], 4);

            try {
                clientSocket.receive(clientPacket);
                errorCode = detectError(clientPacket.getData());
                if(errorCode != -1){
                    closeStream(stream);
                    exit(errorCode);
                }

            } catch (SocketTimeoutException e) {
                if(timedoutIndex >= 5){
                    System.out.println("Cannot connect to host. Aborting...");
                    closeStream(stream);
                    exit(-4);
                }
                else{
                    System.out.println("Connection timed out - Retry");
                    timedoutIndex++;
                }
            } catch (IOException e) {
                System.out.println("Exception reached while receiving packet. Aborting...");
                closeStream(stream);
                exit(-6);
            }
        } while((clientPacket.getData()[1] != 4 && clientPacket.getData()[3] != 0) || timedoutIndex != 0);

        System.out.println("ACK "+ clientPacket.getData()[3] +" received" );

        //Running

        serverPort = clientPacket.getPort();
        System.out.println("Port : "+ serverPort);
        byte[] strData = new byte[516];
        int cpt = 0;
        int numPacket = 1;
        int sizePacket = 512;

        System.out.println("File length :" + fic.length());

        while (cpt < fic.length()) {

            //Reading 512 bytes in file

            try {
                sizePacket = stream.read(strData, 4, 512);
                cpt += sizePacket;
            } catch (IOException e) {
                System.out.println("Exception reached while reading file. Aborting...");
                closeStream(stream);
                exit(-2);
            }

            //Sending packet

            strData[0] = 0;
            strData[1] = 3;
            strData[2] = (byte) (numPacket / 256);
            strData[3] = (byte) (numPacket % 256);
            sizePacket += 4;

            DatagramPacket filePacket = new DatagramPacket(strData, sizePacket, ias, serverPort);

            byte[] ack;
            timedoutIndex = 0;
            do {

                try {
                    clientSocket.send(filePacket);
                } catch (IOException e) {
                    System.out.println("Exception reached while sending packet. Aborting...");
                    closeStream(stream);
                    exit(-5);
                }

                //Receiving ACK

                clientPacket = new DatagramPacket(new byte[1024], 1024);
                ack = new byte[1024];

                try {
                    clientSocket.receive(clientPacket);
                    ack = clientPacket.getData();
                    errorCode = detectError(ack);
                    if(errorCode != -1){
                        closeStream(stream);
                        exit(errorCode);
                    }
                } catch (SocketTimeoutException e) {
                    if(timedoutIndex >= 5){
                        System.out.println("Cannot connect to host. Aborting...");
                        closeStream(stream);
                        exit(-4);
                    }
                    else{
                        System.out.println("Connection timed out - Retry");
                        timedoutIndex++;
                    }
                } catch (IOException e) {
                    System.out.println("Exception reached while reveiving packet. Aborting...");
                    closeStream(stream);
                    exit(-6);
                }
            } while ((! (ack[0] == 0 && ack[1] == 4 && ack[2] == (byte) (numPacket / 256) && ack[3] == (byte) (numPacket % 256))) || timedoutIndex != 0);

            System.out.println("ACK "+ clientPacket.getData()[3] +" received" );
            numPacket++;
        }

        // Closing stream
        closeStream(stream);

        System.out.println("File successfuly sended");
    }

}


