package StableMulticast;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

//    TODO: método getClientName() não presente na especificação do trabalho. Portanto, não estará presente no cliente de teste e o middleware não pode depender de sua existência. Remover todas referências
public class StableMulticast {

    private final GroupMember clientGroupMember;
    private final IStableMulticast client;
    private final List<GroupMember> groupMembers;
    private final Map<String, String> messageBuffer;
    private final Map<String, int[]> logicalClock;

    public static final String MULTICAST_IP = "224.0.0.1";
    public static final Integer MULTICAST_PORT = 4446;

    private final CountDownLatch latch = new CountDownLatch(1);

    record GroupMember(String name, String ip, Integer port) implements Serializable {
    }

    public StableMulticast(String ip, Integer port, IStableMulticast client) {
        this.clientGroupMember = new GroupMember(client.getClientName(), ip, port);
        this.client = client;
        this.groupMembers = new ArrayList<>();
        this.messageBuffer = new HashMap<>();
        this.logicalClock = new HashMap<>();

        discoverGroupMembers();
        announcePresence();
        mreceive();
    }

    private void discoverGroupMembers() {
        new Thread(() -> {
//            TODO: melhorar definição do tamanho
            byte[] buffer = new byte[256];

            try (MulticastSocket multicastSocket = new MulticastSocket(MULTICAST_PORT)) {

                InetAddress group = InetAddress.getByName(MULTICAST_IP);
                NetworkInterface netIf = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
                SocketAddress groupAddress = new InetSocketAddress(group, MULTICAST_PORT);
                multicastSocket.joinGroup(groupAddress, netIf);

                latch.countDown();

                while (true) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    multicastSocket.receive(packet);

                    try (ByteArrayInputStream bis = new ByteArrayInputStream(packet.getData());
                         ObjectInputStream ois = new ObjectInputStream(bis)) {

                        GroupMember received = (GroupMember) ois.readObject();

                        synchronized (groupMembers) {
                            if (!received.equals(clientGroupMember) && !groupMembers.contains(received)) {
                                groupMembers.add(received);
                                System.out.println("MIDDLEWARE: New group member: " + received);
                                announcePresence();
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void announcePresence() {
        new Thread(() -> {
            try {
                latch.await();
                try (MulticastSocket multicastSocket = new MulticastSocket();
                     ByteArrayOutputStream bos = new ByteArrayOutputStream();
                     ObjectOutputStream oos = new ObjectOutputStream(bos)) {

                    oos.writeObject(new GroupMember(client.getClientName(), clientGroupMember.ip, clientGroupMember.port));
                    byte[] message = bos.toByteArray();

                    InetAddress group = InetAddress.getByName(MULTICAST_IP);

                    DatagramPacket packet = new DatagramPacket(message, message.length, group, MULTICAST_PORT);
                    multicastSocket.send(packet);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void msend(String msg) {
        if (groupMembers.isEmpty()) {
            System.out.println("MIDDLEWARE: No group members available to send the message.");
            return;
        }

        msg = clientGroupMember.name + ": " + msg;

        // Update the logical clock
//        int[] myClock = logicalClock.getOrDefault(this.client.getClientName(), new int[groupMembers.size()]);
//        myClock[getIndex(this.clientGroupMember)]++;
//        logicalClock.put(this.client.getClientName(), myClock);

//        msg = msg + "|" + Arrays.toString(myClock) + "|" + this.client.getClientName();

        for (GroupMember member : groupMembers) {
            sendUnicast(msg, member);
        }
    }

    private void sendUnicast(String msg, GroupMember member) {
        try (DatagramSocket socket = new DatagramSocket()) {
            byte[] message = msg.getBytes();
            InetAddress address = InetAddress.getByName(member.ip);
            DatagramPacket packet = new DatagramPacket(message, message.length, address, member.port);
            socket.send(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void mreceive() {
//        String[] parts = msg.split("\\|");
//        String message = parts[0];
//        String clockString = parts[1];
//        String sender = parts[2];
//
//        int[] senderClock = parseClock(clockString);
//
//        // Update the logical clock
//        logicalClock.put(sender, senderClock);
//
//        int[] myClock = logicalClock.getOrDefault(this.client.getClientName(), new int[groupMembers.size()]);
//        if (!sender.equals(this.client.getClientName())) {
//            myClock[getIndex(sender)]++;
//        }
//        logicalClock.put(this.client.getClientName(), myClock);

        // Deliver the message to the client
//        client.deliver(message);
//
//        // Check for stable messages to discard
//        discardStableMessages();

        new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket(clientGroupMember.port)) {
//                TODO: melhorar definição do tamanho
                byte[] buffer = new byte[256];

                while (true) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);

                    final var message = new String(packet.getData(), 0, packet.getLength());

                    client.deliver(message);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
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

    private int getIndex(GroupMember member) {
        return groupMembers.indexOf(member);
    }

    private void discardStableMessages() {
//        for (Map.Entry<String, String> entry : messageBuffer.entrySet()) {
//            String message = entry.getValue();
//            String[] parts = message.split("\\|");
//            String clockString = parts[1];
//            int[] msgClock = parseClock(clockString);
//            String sender = parts[2];
//
//            boolean stable = true;
//            for (int[] clock : logicalClock.values()) {
//                if (msgClock[getIndex(sender)] > clock[getIndex(sender)]) {
//                    stable = false;
//                    break;
//                }
//            }
//
//            if (stable) {
//                messageBuffer.remove(entry.getKey());
//                System.out.println("MIDDLEWARE: Discarded stable message: " + entry.getKey());
//            }
//        }
    }
}
