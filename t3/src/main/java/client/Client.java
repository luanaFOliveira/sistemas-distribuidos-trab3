package client;

import StableMulticast.IStableMulticast;
import StableMulticast.StableMulticast;
import java.util.Scanner;

public class Client implements IStableMulticast {
    private StableMulticast stableMulticast;
    private String clientName;

    private Client(String clientName) {
        this.clientName = clientName;
    }

    private void init(String clientIp, Integer clientPort) {
        stableMulticast = new StableMulticast(clientIp, clientPort, this);
    }

    @Override
    public void deliver(String msg) {
        System.out.println(msg);
    }

    public void sendMessage(String msg) {
        stableMulticast.msend(msg);
    }

    //    TODO: método não presente na especificação do trabalho. Portanto, não estará presente no cliente de teste e o middleware não pode depender de sua existência. Remover todas referências
    @Override
    public String getClientName() {
        return this.clientName;
    }

    public static void main(String[] args) {
        var clientIp = args[0];
        var clientPort = Integer.parseInt(args[1]);

        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter client name: ");
        String clientName = "";
        while (clientName.equals("")) {
            clientName = scanner.nextLine();
        }

        Client client = new Client(clientName);
        client.init(clientIp, clientPort);

        System.out.print("Para enviar, digite e pressione enter: \n");
        while (true) {
            String msg = scanner.nextLine();
            client.sendMessage(msg);
        }
    }
}
