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

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Logger;

public class ImageVideoReceiver extends MessageDispatcher {
    HashMap<Boolean, String> fileName;
    boolean isImage;
    Thread send;
    Thread receive;
    
    public ImageVideoReceiver(String code, String message, DatagramSocket[] sockets,
                              InetAddress serverIP, int serverPort, int clientPort,
                              Logger logger, boolean isImage) throws SocketException {
        super(code, message, sockets, serverIP, serverPort, clientPort, logger);
        this.rxBuffer = new byte[128];
        this.datagramPackets[1] = new DatagramPacket(this.rxBuffer, this.rxBuffer.length);
        this.isImage = isImage;
        this._createImageHashMap();
    }
    
    /**
     * *createImageHashMap*
     * This creates a hashMap of type <Boolean, String>
     * to map the correct file name based on the isImage
     * property. If true set it to image.jpg else video.mjpeg
     */
    private void _createImageHashMap() {
        this.fileName = new HashMap<>(2);
        this.fileName.put(true, "image.jpg");
        this.fileName.put(false, "video.mjpeg");
    }
    
    @Override
    public void sendRequest() {
        try {
            // If the necessary code is video send 30 requests to the server
            int limit = this.isImage ? 1 : 100;
            this.datagramSockets[1].setSoTimeout(90000);
            this.request(limit, this.datagramSockets, datagramPackets, logger, this.fileName.get(this.isImage));
            logger.info("Exited");
        } catch (IOException | InterruptedException e) {
            logger.severe(Arrays.toString(e.getStackTrace()));
        }
        for (DatagramSocket socket : this.datagramSockets) {
            socket.close();
        }
    }
    
    private void request(int length, DatagramSocket[] socket, DatagramPacket[] packet, Logger logger, String name) throws InterruptedException {
        this.send = new SendThread(length, socket[0], packet[0], logger);
        this.receive = new ReceiveThread(length, socket[1], packet[1], logger, name);
        this.send.start();
        this.receive.start();
        this.send.join();
        this.receive.join();
        this.send.stop();
        this.receive.stop();
    }
}

class SendThread extends Thread {
    int length;
    DatagramSocket socket;
    DatagramPacket packet;
    Logger logger;
    
    SendThread(int length, DatagramSocket socket, DatagramPacket packet, Logger logger) {
        this.socket = socket;
        this.packet = packet;
        this.length = length;
        this.logger = logger;
    }
    
    public void run() {
        for (int i = 0; i < this.length; ++i) {
            try {
                this.socket.send(this.packet);
                // This is equal to 30fps
                Thread.sleep(33, 33);
            } catch (IOException | InterruptedException e) {
                logger.severe(Arrays.toString(e.getStackTrace()));
                logger.severe(e.getMessage());
                return;
            }
        }
        logger.info("Exited send thread");
    }
}

class ReceiveThread extends Thread {
    int length;
    DatagramSocket socket;
    DatagramPacket packet;
    Logger logger;
    String fileName;
    
    ReceiveThread(int length, DatagramSocket socket, DatagramPacket packet,
                  Logger logger, String fileName) {
        this.socket = socket;
        this.packet = packet;
        this.length = length;
        this.logger = logger;
        this.fileName = fileName;
    }
    
    public void run() {
        try {
            File file = new File(this.fileName);
            FileOutputStream stream = new FileOutputStream(file);
            for (int i = 0; i < this.length; ++i) {
                try {
                    int len = 0;
                    while (true) {
                        this.socket.receive(this.packet);
                        if (len == 0)
                            len = this.packet.getLength();
                        if (len != this.packet.getLength())
                            break;
                        stream.write(this.packet.getData(), this.packet.getOffset(), this.packet.getLength());
                    }
                } catch (IOException e) {
                    logger.severe(Arrays.toString(e.getStackTrace()));
                    logger.severe(e.getMessage());
                    return;
                }
            }
            stream.close();
            logger.info("Exited receiving thread");
        } catch (IOException e) {
            logger.severe(Arrays.toString(e.getStackTrace()));
            logger.severe(e.getMessage());
        }
    }
}

