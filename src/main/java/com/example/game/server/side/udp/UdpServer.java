package com.example.game.server.side.udp;

import jakarta.annotation.PostConstruct;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UdpServer {

    private static final int UDP_PORT = 8081;
    private static final long DISCONNECT_TIMEOUT = 10000L;
    private final Map<String, InetSocketAddress> connectedPlayers = new ConcurrentHashMap<>();
    private final Map<String, Long> lastHeartbeat = new ConcurrentHashMap<>();

    private void updateHeartbeat(String clientKey) {
        lastHeartbeat.put(clientKey, System.currentTimeMillis());
    }

    @Scheduled(fixedDelay = 600000) // Run every 10 min
    public void cleanupInactivePlayers() {
        checkInactivePlayers();
    }

    private void checkInactivePlayers() {
        long currentTime = System.currentTimeMillis();
        connectedPlayers.entrySet().removeIf(entry -> {
            String clientKey = entry.getKey();
            long lastActive = lastHeartbeat.getOrDefault(clientKey, 0L);
            System.out.println("Current time " + currentTime + "Last active " + lastActive);
            if (currentTime - lastActive > DISCONNECT_TIMEOUT) { // e.g., 10 seconds
                lastHeartbeat.remove(clientKey);
                System.out.println("Removing inactive player: " + clientKey);
                return true; // Remove from connectedPlayers
            }
            return false;
        });
    }

    @PostConstruct
    public void startUdpServer() {
        new Thread(() -> {
            try {
                final var udpSocket = new DatagramSocket(UDP_PORT);
                System.out.println("UDP Server is listening on port " + UDP_PORT);

                while (true) {
                    // Receive the incoming UDP packet
                    final var receiveBuffer = new byte[1024];
                    final var packet = new DatagramPacket(receiveBuffer, receiveBuffer.length);

                    udpSocket.receive(packet);

                    final var packetData = packet.getData();
                    final var length = packet.getLength();

                    // Register the player if not already in the list
                    final var clientKey = packet.getAddress().getHostAddress() + ":" + packet.getPort();
                    connectedPlayers.putIfAbsent(clientKey, new InetSocketAddress(packet.getAddress(), packet.getPort()));
                    updateHeartbeat(clientKey);
                    System.out.println("Connected players : " + connectedPlayers);

                    System.out.println("Received packet with length: " + length);
                    System.out.println("Received packetData " + Arrays.toString(packetData));
                    handlePacket(packetData, length, packet.getAddress(), packet.getPort(), udpSocket);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();


    }

    private void handlePacket(byte[] data, int length, InetAddress senderAddress, int senderPort, DatagramSocket udpSocket) {
        final var buffer = ByteBuffer.wrap(data, 0, length);
        final var messageType = buffer.get(0);
        final var packetLength = Short.reverseBytes(buffer.getShort(1));
        final var playerId = buffer.get(3);
        final var extractedBytes = Arrays.copyOfRange(data, 7, 19);
        final var receivedPacket = new UDPPacket(messageType, playerId, extractedBytes, packetLength);
        System.out.println("Message type " + messageType);
        System.out.println("Packet length " + packetLength);
        System.out.println("PlayerId " + playerId);
        System.out.println("Extracted bytes : " + extractedBytes.length);

        switch (messageType) {
            case 0: // Disconnect message
                final var clientKey = senderAddress.getHostAddress() + ":" + senderPort;
                connectedPlayers.remove(clientKey);
                System.out.println("Player disconnected: " + clientKey);
                break;
            case 1: // Move message
                handleMove(receivedPacket, senderAddress, senderPort, udpSocket);
                break;
            case 2: // Action message
                handleAction(receivedPacket);
                break;
            default:
                System.out.println("Unknown message type");
        }
    }

    // Handle movement (example)
    private void handleMove(UDPPacket packet, InetAddress senderAddress, int senderPort, DatagramSocket udpSocket) {
        // Use ByteBuffer to read the payload as floats (first 12 bytes)
        final var payloadBuffer = ByteBuffer.wrap(packet.getPayload());
        payloadBuffer.order(ByteOrder.LITTLE_ENDIAN);  // Ensure correct byte order (Little Endian)
        // Debugging the raw payload
        System.out.println("Received Payload: " + Arrays.toString(packet.getPayload()));

        // Read the first 12 bytes as a Vector3 (3 floats)
        if (payloadBuffer.remaining() >= 12) {
            final var x = payloadBuffer.getFloat();  // First 4 bytes
            final var y = payloadBuffer.getFloat();  // Next 4 bytes
            final var z = payloadBuffer.getFloat();  // Last 4 bytes

            final var responseBytes = createResponsePacket(packet);
            System.out.println("Response bytes sending back to client :" + Arrays.toString(responseBytes));
            final var responsePacket = new DatagramPacket(responseBytes, packet.getPacketLength(), senderAddress, senderPort);
            final var senderKey = senderAddress.getHostAddress() + ":" + senderPort;

//            try {
                connectedPlayers.forEach((clientKey, clientAddress) -> {
                    if (!clientKey.equals(senderKey)) { // Exclude sender
                        try {

                            DatagramPacket packet1 = new DatagramPacket(responseBytes, responseBytes.length, clientAddress.getAddress(), clientAddress.getPort());
                            udpSocket.send(packet1);
                        } catch (IOException e) {
                            System.err.println("Failed to send packet to " + clientKey + ": " + e.getMessage());
                        }
                    }
                });
//                udpSocket.send(responsePacket);
//            } catch (IOException e) {
//                final var clientKey = senderAddress.getHostAddress() + ":" + senderPort;
//                System.err.println("Player disconnected or unreachable: " + clientKey);
//                connectedPlayers.remove(clientKey);
//            }

            System.out.println("Received movement data: x=" + x + ", y=" + y + ", z=" + z);
        } else {
            System.out.println("Error: Payload size is incorrect. Expected 12 bytes for Vector3.");
        }
    }


    private void handleAction(UDPPacket packet) {
        System.out.println("Processing action for player " + packet.getPlayerId());
    }

    public byte[] createResponsePacket(UDPPacket packet) {
        final var messageType = packet.getMessageType();
        final var playerId = packet.getPlayerId();
        final var payload = packet.getPayload();
        System.out.println("Creating buffer with payload length : " + packet.getPacketLength());
        short packetLength = packet.getPacketLength();

        final var buffer = ByteBuffer.allocate(packetLength);
        // TODO not a todo :) but this one is crucial in order to maintain same order of bytes as when received,
        //  be careful with modifying this
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        // Write data to the buffer
        buffer.put(messageType);
        buffer.putShort(packetLength);
        buffer.putInt(playerId);
        buffer.put(payload);

        System.out.println("Buffer array to be sent " + buffer.array());

        return buffer.array();
    }
}