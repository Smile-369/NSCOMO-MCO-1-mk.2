import javax.swing.*;
import java.io.*;
import java.net.*;

public class TFTPClient {
    private static final int TFTP_PORT = 69;
    private static final int MAX_PACKET_SIZE = 516;
    private static final int DATA_SIZE = 512;
    private static final int OP_RRQ = 1;
    private static final int OP_WRQ = 2;
    private static final int OP_DATA = 3;
    private static final int OP_ACK = 4;
    private static final int OP_ERROR = 5;

    private InetAddress serverAddress;
    private DatagramSocket socket;

    public TFTPClient(String serverIP) throws SocketException, UnknownHostException {
        this.serverAddress = InetAddress.getByName(serverIP);
        this.socket = new DatagramSocket();
        this.socket.setSoTimeout(5000);
    }

    public void downloadFile(String remoteFilename, String localFilename) throws IOException {
        // Send RRQ packet
        ByteArrayOutputStream rrqByteStream = new ByteArrayOutputStream();
        DataOutputStream rrqDataStream = new DataOutputStream(rrqByteStream);
        rrqDataStream.writeShort(OP_RRQ);
        rrqDataStream.writeBytes(remoteFilename);
        rrqDataStream.writeByte(0);
        rrqDataStream.writeBytes("octet");
        rrqDataStream.writeByte(0);

        byte[] rrqPacket = rrqByteStream.toByteArray();
        DatagramPacket sendPacket = new DatagramPacket(rrqPacket, rrqPacket.length, serverAddress, TFTP_PORT);
        socket.send(sendPacket);

        // Receive DATA packets
        FileOutputStream fileOutputStream = new FileOutputStream(localFilename);
        byte[] dataPacket = new byte[MAX_PACKET_SIZE];
        DatagramPacket receivePacket = new DatagramPacket(dataPacket, dataPacket.length);
        short blockNumber = 1;
        boolean lastPacketReceived = false;

        while (!lastPacketReceived) {
            socket.receive(receivePacket);

            // Check if received packet is DATA
            if (dataPacket[1] == OP_DATA) {
                short receivedBlockNumber = (short) (((dataPacket[2] & 0xff) << 8) | (dataPacket[3] & 0xff));

                // Check if received block number is expected
                if (receivedBlockNumber == blockNumber) {
                    // Write data to file
                    fileOutputStream.write(dataPacket, 4, receivePacket.getLength() - 4);

                    // Send ACK packet
                    byte[] ackPacket = new byte[4];
                    ackPacket[1] = OP_ACK;
                    ackPacket[2] = dataPacket[2];
                    ackPacket[3] = dataPacket[3];
                    sendPacket.setData(ackPacket);
                    sendPacket.setLength(ackPacket.length);
                    sendPacket.setPort(receivePacket.getPort());
                    socket.send(sendPacket);

                    // Check if last packet received
                    if (receivePacket.getLength() < MAX_PACKET_SIZE) {
                        lastPacketReceived = true;
                    }

                    blockNumber++;
                }
            } else if (dataPacket[1] == OP_ERROR) {
                // Handle error
                short errorCode = (short) (((dataPacket[2] & 0xff) << 8) | (dataPacket[3] & 0xff));
                String errorMessage = new String(dataPacket, 4, receivePacket.getLength() - 4);
                System.err.println("Error: " + errorCode + " " + errorMessage);
                break;
            }
        }

        fileOutputStream.close();
    }

    public void uploadFile(String localFilename, String remoteFilename) throws IOException {
        // Check if file exists
        File localFile = new File(localFilename);
        if (!localFile.exists()) {
            throw new FileNotFoundException("Local file not found: " + localFilename);
        }

        // Send WRQ packet
        ByteArrayOutputStream wrqByteStream = new ByteArrayOutputStream();
        DataOutputStream wrqDataStream = new DataOutputStream(wrqByteStream);
        wrqDataStream.writeShort(OP_WRQ);
        wrqDataStream.writeBytes(remoteFilename);
        wrqDataStream.writeByte(0);
        wrqDataStream.writeBytes("octet");
        wrqDataStream.writeByte(0);

        byte[] wrqPacket = wrqByteStream.toByteArray();
        DatagramPacket sendPacket = new DatagramPacket(wrqPacket, wrqPacket.length, serverAddress, TFTP_PORT);
        socket.send(sendPacket);

        // Receive ACK packet
        byte[] ackPacket = new byte[4];
        DatagramPacket receivePacket = new DatagramPacket(ackPacket, ackPacket.length);
        socket.receive(receivePacket);

        // Update destination port
        sendPacket.setPort(receivePacket.getPort());

        // Check if received packet is ACK
        if (ackPacket[1] == OP_ACK) {
            short receivedBlockNumber = (short) (((ackPacket[2] & 0xff) << 8) | (ackPacket[3] & 0xff));

            // Check if received block number is 0
            if (receivedBlockNumber == 0) {
                // Send DATA packets
                FileInputStream fileInputStream = new FileInputStream(localFilename);
                byte[] dataBuffer = new byte[DATA_SIZE];
                int bytesRead;
                short blockNumber = 1;
                boolean lastPacketSent = false;

                while (!lastPacketSent) {
                    bytesRead = fileInputStream.read(dataBuffer);

                    // Check if bytesRead is not negative
                    if (bytesRead >= 0) {
                        // Create DATA packet
                        ByteArrayOutputStream dataByteStream = new ByteArrayOutputStream();
                        DataOutputStream dataDataStream = new DataOutputStream(dataByteStream);
                        dataDataStream.writeShort(OP_DATA);
                        dataDataStream.writeShort(blockNumber);
                        dataDataStream.write(dataBuffer, 0, bytesRead);

                        byte[] dataPacket = dataByteStream.toByteArray();
                        sendPacket.setData(dataPacket);
                        sendPacket.setLength(dataPacket.length);
                        socket.send(sendPacket);

                        // Receive ACK packet
                        receivePacket.setData(ackPacket);
                        receivePacket.setLength(ackPacket.length);
                        socket.receive(receivePacket);

                        // Check if received packet is ACK
                        if (ackPacket[1] == OP_ACK) {
                            receivedBlockNumber = (short) (((ackPacket[2] & 0xff) << 8) | (ackPacket[3] & 0xff));

                            // Check if received block number is expected
                            if (receivedBlockNumber == blockNumber) {
                                // Check if last packet sent
                                if (bytesRead < DATA_SIZE) {
                                    lastPacketSent = true;
                                }

                                blockNumber++;
                            }
                        } else if (ackPacket[1] == OP_ERROR) {
                            // Handle error
                            short errorCode = (short) (((ackPacket[2] & 0xff) << 8) | (ackPacket[3] & 0xff));
                            String errorMessage = new String(ackPacket, 4, receivePacket.getLength() - 4);
                            System.err.println("Error: " + errorCode + " " + errorMessage);
                            break;
                        }
                    }
                }

                fileInputStream.close();
            }
        } else if (ackPacket[1] == OP_ERROR) {
            // Handle error
            short errorCode = (short) (((ackPacket[2] & 0xff) << 8) | (ackPacket[3] & 0xff));
            String errorMessage = new String(ackPacket, 4, receivePacket.getLength() - 4);
            System.err.println("Error: " + errorCode + " " + errorMessage);
        }
    }





    public static void main(String[] args) {

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                TFTPClientGUI gui = new TFTPClientGUI();
            }
        });
    }

}
