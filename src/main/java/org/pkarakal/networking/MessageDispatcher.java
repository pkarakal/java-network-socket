/*
 * MIT License
 *
 * Copyright (c) 2020 Pavlos Karakalidis <pavloc.kara@outlook.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package org.pkarakal.networking;

import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.logging.Logger;

public class MessageDispatcher implements Request{
    InetAddress serverIP;
    int serverPort;
    int clientPort;
    String code;
    String message;
    DatagramSocket[] datagramSockets;
    DatagramPacket[] datagramPackets;
    boolean waitingForResult = false;
    byte[] txBuffer;
    byte[] rxBuffer;
    Logger logger;
    File echo;
    boolean isThermo = false;
    
    public MessageDispatcher(String code, String message, DatagramSocket[] sockets,
                             InetAddress serverIP, int serverPort,
                             int clientPort, Logger logger, boolean isThermo) throws SocketException {
        this.code = code;
        this.message = message;
        this.datagramSockets = sockets;
        this.serverIP = serverIP;
        this.serverPort= serverPort;
        this.clientPort = clientPort;
        this.logger = logger;
        this.rxBuffer = new byte[2048];
        this.txBuffer = this.code.getBytes();
        this.isThermo = isThermo;
        try {
            this.initDatagrams();
        } catch (SocketException e){
            e.printStackTrace();
        }
    }
    
    private void initDatagrams() throws SocketException {
        this.datagramSockets[0] = new DatagramSocket(serverPort);
        this.datagramSockets[1] = new DatagramSocket(clientPort);
        this.datagramPackets= new DatagramPacket[2];
        this.datagramPackets[0] = new DatagramPacket(txBuffer, txBuffer.length, serverIP, serverPort);
        this.datagramPackets[1] = new DatagramPacket(rxBuffer, rxBuffer.length);
    }
    
    public MessageDispatcher() throws Exception{
        throw new Exception("Please specify a code to send");
    }
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public DatagramSocket[] getDatagramSocket() {
        return datagramSockets;
    }
    
    public void setDatagramSocket(DatagramSocket[] datagramSocket) {
        this.datagramSockets = datagramSocket;
    }
    
    public DatagramPacket[] getDatagramPacket() {
        return datagramPackets;
    }
    
    public void setDatagramPacket(DatagramPacket[] datagramPacket) {
        this.datagramPackets = datagramPacket;
    }
    
    public void sendRequest() throws SocketException {
        long totalMs = (long)(4 * 60000);
        long echoStartTime = System.currentTimeMillis();
        long totalElapsedMs = 0;
        int packetCount = 0;
        echo = new File(isThermo ? "thermo.csv": "echo.csv");
        FileWriter outputfile = null;
        try {
            outputfile = new FileWriter(echo);
        // create CSVWriter object filewriter object as parameter
        CSVWriter writer = new CSVWriter(outputfile);
        String[] headers = new String[4];
        headers[0] = "Packet"; headers[1]= "CurrentTime"; headers[2] = "Value"; headers[3]= "Duration";
        writer.writeNext(headers);
        while ((totalElapsedMs = System.currentTimeMillis() - echoStartTime) < totalMs){
            try {
                this.datagramSockets[0].send(this.datagramPackets[0]);
                long startTime = System.currentTimeMillis();
                this.waitingForResult = true;
                this.datagramSockets[1].setSoTimeout(9000000);
                this.datagramSockets[1].receive(this.datagramPackets[1]);
                long endTime = System.currentTimeMillis();
                long responseTime = endTime - startTime;
                String[] values = new String[4];
                values[0]= String.valueOf(packetCount);
                values[1]= String.valueOf(endTime);
                values[2]= new String(rxBuffer, 0, this.datagramPackets[1].getLength());
                values[3]= String.valueOf(responseTime);
                writer.writeNext(values);
                ++packetCount;
                logger.info(new String(rxBuffer, 0, this.datagramPackets[1].getLength()));
                this.waitingForResult = false;
            } catch (IOException e) {
                logger.severe(Arrays.toString(e.getStackTrace()));
            }
        }
        writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        int i =0;
        for(DatagramSocket socket = this.datagramSockets[i]; i< this.datagramPackets.length; ++i) {
            socket.close();
        }
    }
}
