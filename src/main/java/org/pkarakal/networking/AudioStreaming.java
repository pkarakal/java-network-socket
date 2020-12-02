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


import javax.sound.sampled.*;
import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class AudioStreaming extends MessageDispatcher {
    byte[] audioSample;
    int audioSampleSize;
    int audioPackets;
    String soundSource;
    AudioPlayer audioPlayer;
    AudioBuffer audioBuffer;
    
    public AudioStreaming(String code, String message, DatagramSocket[] sockets, InetAddress serverIP,
                          int serverPort, int clientPort, Logger logger, int rxBufferLength, String audioPackets) throws SocketException {
        super(code, message, sockets, serverIP, serverPort, clientPort, logger);
        this.rxBuffer = new byte[rxBufferLength];
        this.audioPackets = Integer.parseInt(audioPackets.substring(1));
        this.soundSource = audioPackets.substring(0,1);
        this.audioSampleSize = rxBufferLength * 8 * 1000;
        this.audioSample = new byte[rxBufferLength * 1000];
        this.datagramPackets[1] = new DatagramPacket(this.rxBuffer, this.rxBuffer.length);
    }
    
    @Override
    public void sendRequest() throws SocketException {
        try {
            if(message.equals("AD"))
                message = "A";
          this.request(this.code, this.soundSource, this.message, 1, this.audioPackets);
        }catch (LineUnavailableException| InterruptedException| IOException e){
            System.out.println(e.getMessage());
            System.out.println(Arrays.toString(e.getStackTrace()));
        }
    }
    public void request(String audioRequest, String audioSource, String reqType, int audioClips, int audioPackets) throws LineUnavailableException, InterruptedException, IOException {
        for (int i = 1; i <= audioClips; i++) {
            String tempRequest = null;
            int Q = 8;
            boolean AQCoding = false;
            
            // Create the request
            switch (reqType) {
                case "AQ":
                    Q = 16; // Since the request is AQ encoding, set the bits per sample to 16.
                    AQCoding = true;
                    tempRequest = audioRequest + "AQ" + audioSource + (audioPackets);
                    break;
                default:
                    tempRequest = audioRequest + audioSource + audioPackets;
            }
            
            // The LinkedBlockingQueue is a structure that works like a common Queue but for multi-threaded environments.
            LinkedBlockingQueue<Short> audioOutBuffer = new LinkedBlockingQueue<Short>();
            
            // Send the message.
            assert tempRequest != null;
            this.txBuffer = tempRequest.getBytes();
            this.datagramPackets[0] = new DatagramPacket(this.txBuffer, this.txBuffer.length, this.datagramPackets[0].getAddress(), this.datagramPackets[0].getPort());
            this.datagramSockets[0].send(this.datagramPackets[0]);
            
            System.out.println("Currently Playing: " + reqType);
            this.audioBuffer = new AudioBuffer(this.datagramSockets[1], this.datagramPackets[1], this.rxBuffer, audioPackets, reqType, AQCoding, audioOutBuffer);
            this.audioPlayer = new AudioPlayer(Q, audioOutBuffer);
            this.audioBuffer.join();
            this.audioPlayer.join();
            audioOutBuffer.clear();
        }
        System.out.println("~Done~ Audio request");
    }
}

class AudioBuffer extends Thread {
    
    DatagramSocket socket;
    DatagramPacket packet;
    byte[] rxBuffer;
    private int audioPackets;
    private String type;
    private boolean AQCoding;
    private LinkedBlockingQueue<Short> audioOutBuffer;
    
    public AudioBuffer(DatagramSocket socket, DatagramPacket packet, byte[] rxBuffer, int audioPackets, String type, boolean AQCoding, LinkedBlockingQueue<Short> audioOutBuffer) {
        this.socket =socket;
        this.packet = packet;
        this.rxBuffer = rxBuffer;
        this.audioPackets = audioPackets;
        this.type = type;
        this.AQCoding = AQCoding;
        this.audioOutBuffer = audioOutBuffer;
        this.run();
    }
    
    public void run() {
        try {
            BufferedOutputStream outSubs = new BufferedOutputStream(new FileOutputStream("subs"+ this.type));
            BufferedOutputStream outputSamples = new BufferedOutputStream(new FileOutputStream("samples"+ this.type));
            BufferedOutputStream expectedValues = new BufferedOutputStream(new FileOutputStream("expected"+ this.type));
            BufferedOutputStream bValues = new BufferedOutputStream(new FileOutputStream("bValue"+ this.type));
            
            for (int j = 0; j < audioPackets; j++) {
                short prevSample = 0;
                byte[] packetBytes;
                short expValue = 0;
                short b = 1;
                
                if (AQCoding) {
                    this.rxBuffer = new byte[132];
                    this.packet = new DatagramPacket(this.rxBuffer, rxBuffer.length);
                    this.socket.receive(this.packet);
                    byte[] tempPacket = this.packet.getData();
                    expValue = (short)(tempPacket[1]<<8 | tempPacket[0]);
                    b = (short)(tempPacket[3]<<8 | tempPacket[2]);
                    packetBytes = Arrays.copyOfRange(tempPacket, 4, 132);
                    expectedValues.write(expValue);
                    bValues.write(b);
                } else {
                    this.rxBuffer = new byte[128];
                    this.packet = new DatagramPacket(this.rxBuffer, rxBuffer.length);
                    this.socket.receive(this.packet);
                    packetBytes = this.packet.getData();
                }
                
                for (byte diffByte: packetBytes) {
                    short[] diffSample = new short[2];
                    diffSample[0] = (short)((diffByte>>4) & 0x0F);
                    diffSample[1] = (short)(diffByte & 0x0F);
                    if (AQCoding) {
                        diffSample[0] = (short)((diffSample[0] - 8) * b + expValue);
                        diffSample[1] = (short)((diffSample[1] - 8) * b + expValue);
                    } else {
                        diffSample[0] -= 8;
                        diffSample[1] -= 8;
                    }for (short diff: diffSample) {
                        prevSample += diff;
                        audioOutBuffer.offer(prevSample);
                        outputSamples.write(prevSample);
                        outSubs.write(diff);
                    }
                }
            }
            outSubs.close();
            outputSamples.close();
            expectedValues.close();
            bValues.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

class AudioPlayer extends Thread {
    
    private LinkedBlockingQueue<Short> audioOutBuffer;
    private final int Q;
    private final AudioFormat linearPCM;
    private final SourceDataLine lineOut;
    
    public AudioPlayer(int Q, LinkedBlockingQueue<Short> audioOutBuffer) throws LineUnavailableException {
        this.audioOutBuffer = audioOutBuffer;
        this.Q = Q;
        // Setup the audio format on mono-channel with 8k samples/second and Q bits/sample, the lineOut for playback and then start the player thread.
        linearPCM = new AudioFormat(8000, Q, 1, true, false);
        lineOut = AudioSystem.getSourceDataLine(linearPCM);
        this.run();
    }
    
    public void run() {
        try {
            // Opens and starts the channel for playback with internal buffer of size 32000 bytes.
            lineOut.open(linearPCM,32000);
            lineOut.start();
            
            byte[] outBuffer = new byte[8000];
            Short temp;
            int i = 0;
            while((temp = audioOutBuffer.poll(3000, TimeUnit.MILLISECONDS)) != null) {
                if (Q == 8) {
                    outBuffer[i] = temp.byteValue();
                    i++;
                } else if (Q == 16) {
                    outBuffer[i] = (byte)(temp & 0x00FF);
                    outBuffer[i+1] = (byte)((temp >>8) & 0x00FF);
                    i += 2;
                }
                if (i == 8000) {
                    lineOut.write(outBuffer,0,8000);
                    i = 0;
                }
            }
            lineOut.flush();
            lineOut.stop();
            lineOut.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}

