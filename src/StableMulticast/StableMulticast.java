package StableMulticast;

import java.net.*;
import java.util.*;

public class StableMulticast {
    private String ip;
    private int port;
    private IStableMulticast client;
    private List<String> groupMembers;
    private Map<String, String> messageBuffer;
    private Map<String, int[]> logicalClock;

    public StableMulticast(String ip, Integer port, IStableMulticast client) {
        this.ip = ip;
        this.port = port;
        this.client = client;
        this.groupMembers = new ArrayList<>();
        this.messageBuffer = new HashMap<>();
        this.logicalClock = new HashMap<>();

        // Initialize the discovery service
        discoverGroupMembers();
    }

    private void discoverGroupMembers() {
        new Thread(() -> {
            try {
                MulticastSocket multicastSocket = new MulticastSocket(port);
                InetAddress group = InetAddress.getByName(ip);
                NetworkInterface netIf = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
                SocketAddress groupAddress = new InetSocketAddress(group, port);
                multicastSocket.joinGroup(groupAddress, netIf);

                while (true) {
                    byte[] buffer = new byte[256];
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    multicastSocket.receive(packet);
                    String received = new String(packet.getData(), 0, packet.getLength());
                    if (!groupMembers.contains(received)) {
                        groupMembers.add(received);
                        System.out.println("New group member: " + received);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void msend(String msg) {
        if (groupMembers.isEmpty()) {
            System.out.println("No group members available to send the message.");
            return;
        }

        // Update the logical clock
        int[] myClock = logicalClock.getOrDefault(this.client.getClientName(), new int[groupMembers.size()]);
        myClock[getIndex(this.client.getClientName())]++;
        logicalClock.put(this.client.getClientName(), myClock);

        msg = msg + "|" + Arrays.toString(myClock) + "|" + this.client.getClientName();

        for (String member : groupMembers) {
            sendUnicast(msg, member);
        }
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
        String[] parts = msg.split("\\|");
        String message = parts[0];
        String clockString = parts[1];
        String sender = parts[2];

        int[] senderClock = parseClock(clockString);

        // Update the logical clock
        logicalClock.put(sender, senderClock);

        int[] myClock = logicalClock.getOrDefault(this.client.getClientName(), new int[groupMembers.size()]);
        if (!sender.equals(this.client.getClientName())) {
            myClock[getIndex(sender)]++;
        }
        logicalClock.put(this.client.getClientName(), myClock);

        // Deliver the message to the client
        client.deliver(message);

        // Check for stable messages to discard
        discardStableMessages();
    }

    private int[] parseClock(String clockString) {
        clockString = clockString.replace("[", "").replace("]", "").replace(" ", "");
        String[] parts = clockString.split(",");
        int[] clock = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            clock[i] = Integer.parseInt(parts[i]);
        }
        return clock;
    }

    private int getIndex(String member) {
        return groupMembers.indexOf(member);
    }

    private void discardStableMessages() {
        for (Map.Entry<String, String> entry : messageBuffer.entrySet()) {
            String message = entry.getValue();
            String[] parts = message.split("\\|");
            String clockString = parts[1];
            int[] msgClock = parseClock(clockString);
            String sender = parts[2];

            boolean stable = true;
            for (int[] clock : logicalClock.values()) {
                if (msgClock[getIndex(sender)] > clock[getIndex(sender)]) {
                    stable = false;
                    break;
                }
            }

            if (stable) {
                messageBuffer.remove(entry.getKey());
                System.out.println("Discarded stable message: " + entry.getKey());
            }
        }
    }
}
