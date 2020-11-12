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

import java.io.IOException;
import java.net.*;

public class MessageDispatcher {
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
    
    public MessageDispatcher(String code, String message,
                             DatagramSocket[] sockets,
                             InetAddress serverIP,
                             int serverPort,
                             int clientPort) throws SocketException {
        this.code = code;
        this.message = message;
        this.datagramSockets = sockets;
        this.serverIP = serverIP;
        this.serverPort= serverPort;
        this.clientPort = clientPort;
        this.rxBuffer = new byte[2048];
        this.txBuffer = this.code.getBytes();
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
        for (int i=0; i<10 && !this.waitingForResult; ++i) {
            try {
                this.datagramSockets[0].send(this.datagramPackets[0]);
                this.waitingForResult = true;
                this.datagramSockets[1].setSoTimeout(9000000);
                this.datagramSockets[1].receive(this.datagramPackets[1]);
                System.out.println(new String(rxBuffer, 0, this.datagramPackets[1].getLength()));
                this.waitingForResult = false;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        int i =0;
        for(DatagramSocket socket = this.datagramSockets[i]; i< this.datagramPackets.length; ++i) {
            socket.close();
        }
    }
}
