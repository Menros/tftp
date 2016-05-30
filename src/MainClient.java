import java.util.Scanner;

/**
 * Created by franck on 5/23/16.
 */
public class MainClient {

    public static void main(String[] args) {

        //Client c = new Client ();
        //c.write("plop.txt");

        Scanner sc = new Scanner(System.in);

        System.out.println("Client TFTP - v0.01");
        System.out.println("Envoi d'un fichier");

        System.out.println("Ip du serveur : ");

        int first = Integer.parseInt(sc.nextLine());
        int sec = Integer.parseInt(sc.nextLine());
        int third = Integer.parseInt(sc.nextLine());
        int fourth = Integer.parseInt(sc.nextLine());

        System.out.println("Chemin du fichier : ");
        String file = sc.nextLine();

        Client c = new Client ();
        c.write(file, first, sec, third, fourth);
    }
}
