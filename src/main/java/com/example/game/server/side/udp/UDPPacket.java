package com.example.game.server.side.udp;

import java.nio.ByteBuffer;

public class UDPPacket {
    private byte messageType;  // 1 byte for message type
    private short packetLength; // 2 bytes for packet length (optional)
    private int playerId;      // 4 bytes for player ID
    private byte[] payload;    // variable-length payload

    // Constructor
    public UDPPacket(byte messageType, int playerId, byte[] payload, short packetLength) {
        this.messageType = messageType;
        this.playerId = playerId;
        this.payload = payload;
        this.packetLength = packetLength;// 7 bytes for header
    }

    // Getters and setters
    public byte getMessageType() {
        return messageType;
    }

    public void setMessageType(byte messageType) {
        this.messageType = messageType;
    }

    public short getPacketLength() {
        return packetLength;
    }

    public void setPacketLength(short packetLength) {
        this.packetLength = packetLength;
    }

    public int getPlayerId() {
        return playerId;
    }

    public void setPlayerId(int playerId) {
        this.playerId = playerId;
    }

    public byte[] getPayload() {
        return payload;
    }

    public void setPayload(byte[] payload) {
        this.payload = payload;
    }

    // Method to convert the packet into a byte array to send over UDP
    public byte[] toByteArray() {
        ByteBuffer buffer = ByteBuffer.allocate(packetLength);
        buffer.put(messageType);     // 1 byte
        buffer.putShort(packetLength); // 2 bytes
        buffer.putInt(playerId);      // 4 bytes
        buffer.put(payload);          // payload

        return buffer.array();
    }

    // Static method to convert byte array back into a Packet object
    public static UDPPacket fromByteArray(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);

        byte messageType = buffer.get();
        short packetLength = buffer.getShort();
        int playerId = buffer.getInt();

        byte[] payload = new byte[packetLength - 7];
        buffer.get(payload);

        return new UDPPacket(messageType, playerId, payload, packetLength);
    }
}
