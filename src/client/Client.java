package client;

import StableMulticast.*;
import java.util.Scanner;

public class Client implements IStableMulticast {
    private StableMulticast stableMulticast;
    private String clientName;

    private Client(String clientName) {
        this.clientName = clientName;
    }

    private void init() {
        stableMulticast = new StableMulticast("230.0.0.0", 4446, this);
    }
//acho q o deliver tem q ser usado no stableMulticast, a partir do this.client.deliver algo assim
    @Override
    public void deliver(String msg) {
        System.out.println("Delivered message: " + msg);
    }

    public void sendMessage(String msg) {
        stableMulticast.msend(msg);
    }

    @Override
    public String getClientName() {
        return this.clientName;
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter client name: ");
        String clientName = scanner.nextLine();

        Client client = new Client(clientName);
        client.init();

        while (true) {
            System.out.print("Enter message to send: ");
            String msg = scanner.nextLine();
            client.sendMessage(msg);
        }
    }
}
