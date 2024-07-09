package StableMulticast;

import java.net.*;
import java.util.*;

public class StableMulticast {
    private String ip;
    private int port;
    private IStableMulticast client;
    private List<String> groupMembers;
    private Map<String, String> messageBuffer;
    private Map<String, Integer[]> logicalClock;
    private int nodeId;

    public StableMulticast(String ip, Integer port, IStableMulticast client, int nodeId) {
        this.ip = ip;
        this.port = port;
        this.client = client;
        this.groupMembers = new ArrayList<>();
        this.messageBuffer = new HashMap<>();
        this.logicalClock = new HashMap<>();
        this.nodeId = nodeId;

        // Inicializar o vetor de relógios lógicos
        logicalClock.put(ip, new Integer[groupMembers.size()]);
        Arrays.fill(logicalClock.get(ip), 0);

        // Inicializar o serviço de descoberta
        discoverGroupMembers();
    }

    private void discoverGroupMembers() {
        try {
            MulticastSocket multicastSocket = new MulticastSocket(port);
            InetAddress group = InetAddress.getByName(ip);
            multicastSocket.joinGroup(group);

            new Thread(() -> {
                try {
                    while (true) {
                        byte[] buffer = new byte[256];
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        multicastSocket.receive(packet);
                        String received = new String(packet.getData(), 0, packet.getLength());
                        if (!groupMembers.contains(received)) {
                            groupMembers.add(received);
                            System.out.println("Novo membro do grupo: " + received);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void msend(String msg) {
        // Constrói o timestamp da mensagem
        Integer[] timestamp = logicalClock.get(ip).clone();
        timestamp[nodeId]++;

        String messageId = UUID.randomUUID().toString();
        String fullMessage = msg + ";" + Arrays.toString(timestamp) + ";" + ip + ";" + messageId;

        messageBuffer.put(messageId, fullMessage);

        for (String member : groupMembers) {
            sendUnicast(fullMessage, member);
        }

        logicalClock.get(ip)[nodeId]++;
    }

    private void sendUnicast(String msg, String member) {
        try {
            DatagramSocket socket = new DatagramSocket();
            byte[] buffer = msg.getBytes();
            InetAddress address = InetAddress.getByName(member);
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port);
            socket.send(packet);
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void deliver(String msg) {
        String[] parts = msg.split(";");
        String messageContent = parts[0];
        Integer[] messageTimestamp = Arrays.stream(parts[1].replaceAll("[\\[\\]]", "").split(","))
                .map(String::trim)
                .map(Integer::parseInt)
                .toArray(Integer[]::new);
        String senderIp = parts[2];
        String messageId = parts[3];

        deposit(msg);

        logicalClock.put(senderIp, messageTimestamp);

        if (!senderIp.equals(ip)) {
            logicalClock.get(ip)[nodeId]++;
        }

        client.deliver(messageContent);

        checkMessageStabilization();
    }

    private void deposit(String msg) {
        String[] parts = msg.split(";");
        String messageId = parts[3];
        messageBuffer.put(messageId, msg);
    }

    private void checkMessageStabilization() {
        for (Map.Entry<String, String> entry : messageBuffer.entrySet()) {
            String messageId = entry.getKey();
            String msg = entry.getValue();
            String[] parts = msg.split(";");
            Integer[] messageTimestamp = Arrays.stream(parts[1].replaceAll("[\\[\\]]", "").split(","))
                    .map(String::trim)
                    .map(Integer::parseInt)
                    .toArray(Integer[]::new);
            boolean canDiscard = true;
            for (Map.Entry<String, Integer[]> clockEntry : logicalClock.entrySet()) {
                Integer[] clock = clockEntry.getValue();
                if (messageTimestamp[nodeId] > clock[nodeId]) {
                    canDiscard = false;
                    break;
                }
            }
            if (canDiscard) {
                discard(messageId);
            }
        }
    }

    private void discard(String messageId) {
        messageBuffer.remove(messageId);
        System.out.println("Mensagem estabilizada e removida do buffer: " + messageId);
    }
}
