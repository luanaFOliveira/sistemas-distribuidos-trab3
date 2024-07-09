package client;

import StableMulticast.*;
import java.util.Scanner;

public class Client implements IStableMulticast {
    public static void main(String[] args) {
        StableMulticast sm = new StableMulticast("224.0.0.1", 5000, new Client(), 0);

        // Enviar mensagem do usuário
        Scanner scanner = new Scanner(System.in);
        System.out.print("Digite a mensagem a ser enviada: ");
        String msg = scanner.nextLine();
        sm.msend(msg);
    }

    @Override
    public void deliver(String msg) {
        System.out.println("Mensagem recebida: " + msg);
    }
}
