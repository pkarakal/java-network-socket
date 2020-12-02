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
import java.io.InputStream;
import java.net.*;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.stream.Stream;

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
        options.addOption("p", "publicIP", true, "Input your public IP. This is necessary for tcp, ithaki and obd");
        options.addOption("m", "CAM", true, "Define one of two cameras: FIX or PTZ");
        options.addOption("d", "DIR", true, "Define the direction of the camera. Accepted values are U,D,L,R,C,M");
        options.addOption("f", "FLOW", true, "Define if flow is on or off");
        options.addOption("l", "UDP", true, "Define the length of the UDP packets. Accepted values are 128,256,512,1024. Default=1024");
        options.addOption("o", "obdOpCode", true, "Input the operation code for the OBD. Accepted vales are: \n" +
                                                          "1 -> Engine runtime\n 2 -> Intake air temperature\n 3 -> Throttle position\n" +
                                                          "4 -> Engine RPM\n 5 -> Vehicle speed\n 6 -> Coolant temperature");
        options.addOption("a", "audioDPCM", true, "Define if the audio uses Adaptive, Non-Adaptive or Adaptive Quantiser DPCM.\n " +
                                                          "Accepted values are AD -> Adaptive  and AQ -> Adaptive-Quantiser. Defaults to AD");
        options.addOption("t", "soundCode", true, "Define the audio code to send. This controls the source and number of packets of the sound");
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
                String[] publicIPStr = null;
                byte[] byteIP = new byte[4];
                byte[] publicIP = null;
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
                InetAddress receiveIP = null;
                boolean flow = false;
                int length = 128;
                if (job.equals("thermo")) {
                    code = code.concat("T00");
                }
                if (cmd.getOptionValue("j").equals("video") || cmd.getOptionValue("j").equals("image")) {
                    if (cmd.hasOption("m")) {
                        code = code.concat(" CAM=").concat(cmd.getOptionValue("m"));
                    }
                    if (cmd.hasOption("m") && cmd.getOptionValue("m").equals("PTZ")
                                && cmd.hasOption("d")) {
                        code = code.concat(" DIR=").concat(cmd.getOptionValue("d"));
                    }
                    if (cmd.hasOption("f") && cmd.getOptionValue("f").equals("ON")) {
                        code = code.concat(" FLOW=").concat(cmd.getOptionValue("f"));
                        flow = true;
                    }
                    if (cmd.hasOption("l")) {
                        int len = Integer.parseInt(cmd.getOptionValue("l"));
                        if (len != 128 && len != 256 && len != 512 && len != 1024) {
                            logger.warning("An invalid length was specified. Going with 128");
                            len = 128;
                        }
                        length = len;
                        code = code.concat(" UDP=").concat(String.valueOf(length));
                    }
                }
                if((cmd.getOptionValue("j").equals("tcp") || cmd.getOptionValue("j").equals("ithaki") || cmd.getOptionValue("j").equals("obd")) && cmd.hasOption("p")){
                    publicIPStr = cmd.getOptionValue("p").split("\\.");
                    i= 0;
                    publicIP = new byte[4];
                    for (String str: publicIPStr){
                        publicIP[i] = (byte) Integer.parseInt(str);
                        ++i;
                    }
                    receiveIP = InetAddress.getByAddress(publicIP);
                }
                String soundCode = null;
                if(cmd.hasOption('t')){
                    soundCode = cmd.getOptionValue('t');
                }
                code = code.concat("\r");
                DatagramSocket[] datagramSocket = new DatagramSocket[2];
//                Socket socket = null;
//                if(receiveIP != null){
//                    socket = new Socket(ip, port, receiveIP, receivePort);
//                } else {
//                    socket= new Socket(ip, port);
//                }
//                ImageVideoReceiver messageDispatcher = new ImageVideoReceiver(code, "", datagramSocket, ip, port, receivePort, logger, job.equals("image"), flow, length);
//                messageDispatcher.sendRequest();
//                int obdCode = 0;
//                if(cmd.hasOption('o')) {
//                    obdCode = Integer.parseInt(cmd.getOptionValue('o'));
//                }
                String audioCode = "AD";
                if(cmd.hasOption('a')){
                    audioCode = cmd.getOptionValue('a');
                }
                assert soundCode != null;
                AudioStreaming receiver = new AudioStreaming(code, audioCode, datagramSocket, ip, port, receivePort, logger,  length, soundCode);
                receiver.sendRequest();
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
