package com.example.game.server.side.udp;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

@Service
public class UdpServer {

    private final int UDP_PORT = 8081;

    @PostConstruct
    public void startUdpServer() {
        new Thread(() -> {
            try {
                final var udpSocket = new DatagramSocket(UDP_PORT);
                byte[] receiveBuffer = new byte[1024];
                System.out.println("UDP Server is listening on port " + UDP_PORT);

                while (true) {
                    // Receive the incoming UDP packet
                    final var packet = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                    udpSocket.receive(packet);
                    final var receivedMessage = new String(packet.getData(), 0, packet.getLength());
                    System.out.println("Received UDP message: " + receivedMessage);

                    // Optionally, send a response back to the client
                    final var response = "Acknowledged: " + receivedMessage;
                    byte[] responseBytes = response.getBytes();
                    final var responsePacket = new DatagramPacket(responseBytes, responseBytes.length, packet.getAddress(), packet.getPort());
                    udpSocket.send(responsePacket);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}