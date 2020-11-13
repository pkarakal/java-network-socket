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
import java.nio.file.Path;
import java.util.Arrays;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * *Networking*
 * The networking application takes arguments from the CLI, parses them
 * and executes the application defined in applicationType
 * serverPort {int}: The port the server is listening for requests
 * clientPort {int}: The port the server is sending requests to the client
 * code {str}: The code to send to the server
 * applicationType {str}: The type of application to run: The accepted applications
 * are: audio, echo, image, ithaki, obd, thermo
 */

class Networking {
    public static void main(String[] args) throws Exception {
        Logger logger = Logger.getLogger("Networking");
        FileHandler fh;
        try {
            // This block configure the logger with handler and formatter
            fh = new FileHandler("./Networking.log", true);
            logger.addHandler(fh);
            SimpleFormatter formatter = new SimpleFormatter();
            fh.setFormatter(formatter);
            logger.setUseParentHandlers(false);
        } catch (SecurityException | IOException e) {
            e.printStackTrace();
        }
        logger.info("Application started");
        if (args != null && args.length == 4) {
            byte[] byteIP = {(byte) 155, (byte) 207, 18, (byte) 208};
            try {
                InetAddress ip = InetAddress.getByAddress(byteIP);
                int port = Integer.parseInt(args[0]);
                int receivePort = Integer.parseInt(args[1]);
                String code = args[2];
                if(args[3].equals("thermo")){
                    code = code.concat("T00");
                }
                code = code.concat("\r");
                DatagramSocket[] datagramSocket = new DatagramSocket[2];
                MessageDispatcher messageDispatcher = new MessageDispatcher(code, "", datagramSocket, ip, port, receivePort, logger);
                messageDispatcher.sendRequest();
            } catch (UnknownHostException | SocketException e) {
                logger.severe(e.toString());
                System.out.println(e.toString());
                logger.severe(Arrays.toString(e.getStackTrace()));
                System.exit(-1);
            }
        } else {
            logger.severe("Wrong number of application parameters. Exiting...");
            throw new Exception("Wrong number of application parameters");
        }
    }
}
