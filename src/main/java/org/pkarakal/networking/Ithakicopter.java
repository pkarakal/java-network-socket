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

import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.logging.Logger;

public class Ithakicopter extends TCPReceiver {
    InetAddress clientIP;
    File copter;
    
    public Ithakicopter(InetAddress serverIP, int serverPort, InetAddress clientIP, int clientPort, String code, Socket sockets, boolean waitingForResult, Logger logger) {
        super(serverIP, serverPort, clientPort, code, sockets, waitingForResult, logger);
        this.clientIP = clientIP;
    }
    
    @Override
    public void sendRequest() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(this.inputStream = this.sockets.getInputStream()));
            DataOutputStream outStream = new DataOutputStream(this.outputStream = this.sockets.getOutputStream());
            copter = new File("copter.csv");
            FileWriter outputfile = new FileWriter(copter);
            // create CSVWriter object filewriter object as parameter
            CSVWriter writer = new CSVWriter(outputfile);
            int level = 150;
            int left = 150, right = 150;
            for (int times = 0; times < 100; times++) {
                String response = reader.readLine();
                if (response.contains("ITHAKICOPTER")) {
                    writer.writeNext(response.split(" "));
                }
                outStream.writeBytes("AUTO FLIGHTLEVEL=" + level + " LMOTOR=" + left + " RMOTOR=" + right + " PILOT \r\n");
                if (times < 50) {
                    ++left;
                    ++right;
                } else {
                    --left;
                    --right;
                }
            }
            reader.close();
            outStream.close();
            this.sockets.close();
            writer.close();
            outputfile.close();
        } catch (IOException e) {
            this.logger.severe(e.getMessage());
            this.logger.severe(Arrays.toString(e.getStackTrace()));
        }
    }
}
