package com.example.game.server.side.udp;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

@Service
public class UdpServer {

    private final int UDP_PORT = 8081;

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

                    System.out.println("Received packet with length: " + length);
                    System.out.println("Received packetData " + Arrays.toString(packetData));

                    // Ensure the length is large enough to hold the header
                    if (length < 7) {
                        System.out.println("Packet is too small to contain a valid header.");
                        continue;
                    }

                    // Create a ByteBuffer to read and interpret the packet data
                    ByteBuffer buffer = ByteBuffer.wrap(packetData, 0, length);

                    // Check if there is enough data to read the header (1 byte for message type + 2 bytes for packet length + 4 bytes for player ID)
                    if (buffer.remaining() < 7) {
                        System.out.println("Not enough data to read header.");
                        continue;
                    }

                    final var messageType = buffer.get(0); // Message type sent by Client
                    final var packetLength = Short.reverseBytes(buffer.getShort(1)); // Length of packet sent by Client
                    final var playerId = buffer.get(3); // Player ID from sent by Client

                    System.out.println("Message type " + messageType);
                    System.out.println("Packet length " + packetLength);
                    System.out.println("PlayerId " + playerId);

                    // Extract the payload (remaining bytes in the packet)
                    final var extractedBytes = Arrays.copyOfRange(packetData, 7, 20);

                    System.out.println("Extracted bytes : " + extractedBytes.length);

                    // Create a Packet object
                    final var receivedPacket = new UDPPacket(messageType, playerId, extractedBytes);

                    // Log the parsed message
                    System.out.println("Received UDP packet:");
                    System.out.println("Message Type: " + receivedPacket.getMessageType());
                    System.out.println("Player ID: " + receivedPacket.getPlayerId());
                    System.out.println("Payload: " + new String(receivedPacket.getPayload()));

                    // Handle the packet based on message type
                    switch (receivedPacket.getMessageType()) {
                        case 1:
                            handleMove(receivedPacket, packet, udpSocket);
                            break;
                        case 2:
                            handleAction(receivedPacket);
                            break;
                        default:
                            System.out.println("Unknown message type");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();


    }

    // Handle movement (example)
    private void handleMove(UDPPacket packet, DatagramPacket datagramPacket, DatagramSocket udpSocket) {
        // Use ByteBuffer to read the payload as floats (first 12 bytes)
        ByteBuffer payloadBuffer = ByteBuffer.wrap(packet.getPayload());
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
            final var responsePacket = new DatagramPacket(responseBytes, 19, datagramPacket.getAddress(), datagramPacket.getPort());

            try {
                udpSocket.send(responsePacket);
                // Broadcast the movement to all clients using broadcast address
                InetAddress broadcastAddress = InetAddress.getByName("255.255.255.255"); // Broadcast address
                DatagramPacket broadcastPacket = new DatagramPacket(responseBytes, 19, broadcastAddress, UDP_PORT);
                udpSocket.send(broadcastPacket);
                System.out.println("Broadcasted movement: x=" + x + ", y=" + y + ", z=" + z);

            } catch (IOException e) {
                throw new RuntimeException("Sorry bro i failed to send udp back to client: " + e);
            }

            System.out.println("Received movement data: x=" + x + ", y=" + y + ", z=" + z);
        } else {
            System.out.println("Error: Payload size is incorrect. Expected 12 bytes for Vector3.");
        }

        // Additional logic to update player position, etc.
    }


    // Handle action (e.g., shoot, jump) (example)
    private void handleAction(UDPPacket packet) {
        System.out.println("Processing action for player " + packet.getPlayerId());
        // Additional logic for actions like shooting
    }

    public byte[] createResponsePacket(UDPPacket packet) {
        byte messageType = packet.getMessageType();
        int playerId = packet.getPlayerId();
        byte[] payload = packet.getPayload();
        System.out.println("Creating buffer with payload length : " + 7 + payload.length);
        short packetLength = (short) (7 + payload.length); // 7 bytes for header + payload length

        // Create a ByteBuffer with the required size
        ByteBuffer buffer = ByteBuffer.allocate(packetLength);

        // Write data to the buffer
        buffer.put(messageType);                          // 1 byte for message type
        buffer.putShort(packetLength);                    // 2 bytes for packet length
        buffer.putInt(playerId);                          // 4 bytes for player ID
        buffer.put(payload);                              // Remaining bytes for payload

        return buffer.array();                            // Convert to byte array
    }
}