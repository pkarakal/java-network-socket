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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.logging.Logger;

public class TCPReceiver implements  Request{
    InetAddress serverIP;
    int serverPort;
    int clientPort;
    String code;
    Socket sockets;
    boolean waitingForResult = false;
    InputStream inputStream;
    OutputStream outputStream;
    Logger logger;
    
    TCPReceiver() throws Exception {
        throw new Exception("Specify all the arguments.");
    }
    
    public TCPReceiver(InetAddress serverIP, int serverPort, int clientPort,
                       String code, Socket sockets, boolean waitingForResult,
                       Logger logger) {
        this.serverIP = serverIP;
        this.serverPort = serverPort;
        this.clientPort = clientPort;
        this.code = code;
        this.sockets = sockets;
        this.waitingForResult = waitingForResult;
        this.logger = logger;
    }
    
    public void sendRequest(){
        try{
            this.inputStream = sockets.getInputStream();
            this.outputStream = sockets.getOutputStream();
            this.outputStream.write(("GET /index.html HTTP/1.0\r\n\r\n").getBytes());
            System.out.println(new String(this.inputStream.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e){
            this.logger.severe(e.getMessage());
            this.logger.severe(Arrays.toString(e.getStackTrace()));
        }
    }
}
