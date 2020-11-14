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

import org.apache.commons.cli.*;

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
 * serverIP {int[]}: The IP to request data from
 * serverPort {int}: The port the server is listening for requests
 * clientPort {int}: The port the server is sending requests to the client
 * code {str}: The code to send to the server
 * job {str}: The type of application to run: The accepted applications
 * are: audio, echo, image, ithaki, obd, thermo
 */

class Networking {
    private final static Options options = new Options();
    static {
        options.addRequiredOption("i", "serverIP", true, "Define the IP of the server.");
        options.addRequiredOption( "s", "serverPort", true, "Define the port the server is listening at.");
        options.addRequiredOption("c", "clientPort",  true, "Define the port the server replies to");
        options.addRequiredOption("r","request-code", true, "Define the request code");
        options.addRequiredOption("j","job", true, "Define the job to execute. The valid parameters are echo, thermo, image, video, audio, tcp, ithaki, obd");
        options.addOption("m", "CAM", true, "Define one of two cameras: FIX or PTZ");
        options.addOption("d", "DIR", true, "Define the direction of the camera. Accepted values are U,D,L,R,C,M");
        options.addOption("f", "FLOW", true, "Define if flow is on or off");
        options.addOption("l", "UDP", true, "Define the length of the UDP packets. Accepted values are 128,256,512,1024. Default=1024");
    }
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
        if (args.length > 1) {
            final CommandLineParser parser = new DefaultParser();
            try {
                final CommandLine cmd = parser.parse(options, args);
                String[] ipStr = (cmd.getOptionValue("i")).split("\\.");
                byte[] byteIP = new byte[4];
                int i = 0;
                for (String part : ipStr) {
                    byteIP[i] = (byte) Integer.parseInt(part);
                    ++i;
                }
                int port = Integer.parseInt(cmd.getOptionValue("s"));
                int receivePort = Integer.parseInt(cmd.getOptionValue("c"));
                String code = cmd.getOptionValue("r");
                String job = cmd.getOptionValue("j");
                InetAddress ip = InetAddress.getByAddress(byteIP);
                if (job.equals("thermo")) {
                    code = code.concat("T00");
                }
                code = code.concat("\r");
                DatagramSocket[] datagramSocket = new DatagramSocket[2];
                ImageVideoReceiver messageDispatcher = new ImageVideoReceiver(code, "", datagramSocket, ip, port, receivePort, logger, args[3].equals("image"));
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
