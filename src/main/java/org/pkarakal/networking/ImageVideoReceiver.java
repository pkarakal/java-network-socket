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
    private void _createImageHashMap(){
        this.fileName = new HashMap<>(2);
        this.fileName.put(true, "image.jpg");
        this.fileName.put(false, "video.mjpeg");
    }
    
    @Override
    public void sendRequest() throws SocketException {
        try {
            File file = new File(this.fileName.get(this.isImage));
            OutputStream stream = new FileOutputStream(file);
            // If the necessary code is video send 30 requests to the server
            int limit= this.isImage ? 1 : 30;
            logger.info("Limit is ".concat(String.valueOf(limit)));
            this.datagramSockets[1].setSoTimeout(90000);
            // This loop runs once if it is an image code
            for(int i=0; i < limit; ++i) {
                this.datagramSockets[0].send(this.datagramPackets[0]);
                int length = 0;
                while (true) {
                    this.datagramSockets[1].receive(this.datagramPackets[1]);
                    if (length == 0)
                        length = this.datagramPackets[1].getLength();
                    if (length != this.datagramPackets[1].getLength())
                        break;
                    stream.write(this.datagramPackets[1].getData(), this.datagramPackets[1].getOffset(), this.datagramPackets[1].getLength());
                }
            }
            stream.close();
        } catch (IOException e) {
            logger.severe(Arrays.toString(e.getStackTrace()));
        }
        for(DatagramSocket socket: this.datagramSockets) {
            socket.close();
        }
    }
}
