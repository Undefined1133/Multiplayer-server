package com.example.game.server.side.udp;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(UdpServer.class);

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
            logger.info("Current time {} Last active {}", currentTime, lastActive);
            if (currentTime - lastActive > DISCONNECT_TIMEOUT) {
                lastHeartbeat.remove(clientKey);
                logger.info("Removing inactive player: {}", clientKey);
                return true;
            }
            return false;
        });
    }

    @PostConstruct
    public void startUdpServer() {
        new Thread(() -> {
            try {
                final var udpSocket = new DatagramSocket(UDP_PORT);
                logger.info("UDP Server is listening on port {}", UDP_PORT);

                while (true) {
                    final var receiveBuffer = new byte[1024];
                    final var packet = new DatagramPacket(receiveBuffer, receiveBuffer.length);

                    udpSocket.receive(packet);

                    final var packetData = packet.getData();
                    final var length = packet.getLength();

                    final var clientKey = packet.getAddress().getHostAddress() + ":" + packet.getPort();
                    connectedPlayers.putIfAbsent(clientKey, new InetSocketAddress(packet.getAddress(), packet.getPort()));
                    updateHeartbeat(clientKey);
                    logger.info("Connected players : {}", connectedPlayers);

                    logger.info("Received packet with length: {}", length);
                    logger.info("Received packetData {}", Arrays.toString(packetData));
                    handlePacket(packetData, length, packet.getAddress(), packet.getPort(), udpSocket);
                }
            } catch (Exception e) {
                logger.error("UDP server failed", e);
            }
        }).start();
    }

    private void handlePacket(byte[] data, int length, InetAddress senderAddress, int senderPort, DatagramSocket udpSocket) {
        final var buffer = ByteBuffer.wrap(data, 0, length).order(ByteOrder.LITTLE_ENDIAN);
        byte messageType = buffer.get(0);
        short packetLength = buffer.getShort(1);
        int playerId = buffer.getInt(3);
        final var extractedBytes = Arrays.copyOfRange(data, 7, 19);
        final var receivedPacket = new UDPPacket(messageType, playerId, extractedBytes, packetLength);
        logger.info("Message type {}", messageType);
        logger.info("Packet length {}", packetLength);
        logger.info("PlayerId {}", playerId);
        logger.info("Extracted bytes : {}", extractedBytes.length);

        switch (messageType) {
            case 0:
                final var clientKey = senderAddress.getHostAddress() + ":" + senderPort;
                connectedPlayers.remove(clientKey);
                logger.info("Player disconnected: {}", clientKey);
                break;
            case 1:
                handleMove(receivedPacket, senderAddress, senderPort, udpSocket);
                break;
            case 2:
                handleAction(receivedPacket);
                break;
            default:
                logger.info("Unknown message type");
        }
    }

    // Handle movement (example)
    private void handleMove(UDPPacket packet, InetAddress senderAddress, int senderPort, DatagramSocket udpSocket) {
        final var payloadBuffer = ByteBuffer.wrap(packet.getPayload());
        payloadBuffer.order(ByteOrder.LITTLE_ENDIAN);
        logger.info("Received Payload: {}", Arrays.toString(packet.getPayload()));

        // Read the first 12 bytes as a Vector3 (3 floats)
        if (payloadBuffer.remaining() >= 12) {
            final var x = payloadBuffer.getFloat();
            final var y = payloadBuffer.getFloat();
            final var z = payloadBuffer.getFloat();

            final var responseBytes = createResponsePacket(packet);
            logger.info("Response bytes sending back to client : {}", Arrays.toString(responseBytes));
            final var senderKey = senderAddress.getHostAddress() + ":" + senderPort;

            connectedPlayers.forEach((clientKey, clientAddress) -> {
                if (!clientKey.equals(senderKey)) { // Exclude sender
                    try {
                        DatagramPacket packet1 = new DatagramPacket(responseBytes, responseBytes.length, clientAddress.getAddress(), clientAddress.getPort());
                        udpSocket.send(packet1);
                    } catch (IOException e) {
                        logger.error("Failed to send packet to {} :{}", clientKey, e.getMessage());
                    }
                }
            });

            logger.info("Received movement data: x={} y={} z={}", x, y, z);
        } else {
            logger.info("Error: Payload size is incorrect. Expected 12 bytes for Vector3.");
        }
    }

    private void handleAction(UDPPacket packet) {
        logger.info("Processing action for player {}", packet.getPlayerId());
    }

    public byte[] createResponsePacket(UDPPacket packet) {
        final var messageType = packet.getMessageType();
        final var playerId = packet.getPlayerId();
        final var payload = packet.getPayload();
        logger.info("Creating buffer with payload length : {}", packet.getPacketLength());
        short packetLength = packet.getPacketLength();
        // TODO not a todo :) but this one is crucial in order to maintain same order of bytes as when received,
        //  be careful with modifying this
        final var buffer = ByteBuffer.allocate(packetLength);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(messageType);
        buffer.putShort(packetLength);
        buffer.putInt(playerId);
        buffer.put(payload);

        logger.info("Buffer array to be sent {}", buffer.array());

        return buffer.array();
    }
}
