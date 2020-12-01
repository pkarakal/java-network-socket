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

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

public class OBDStatistics extends TCPReceiver {
    HashMap<Integer, OBDFunctions> opCodes;
    ArrayList<OBDFunctions> opCodesDetails;
    int obdCode;
    ScriptEngineManager script;
    ScriptEngine eng ;
    public OBDStatistics(InetAddress serverIP, int serverPort, int clientPort, String code, Socket sockets, boolean waitingForResult, Logger logger, int obdCode) {
        super(serverIP, serverPort, clientPort, code, sockets, waitingForResult, logger);
        this.obdCode= obdCode;
        this._createFunctions();
        this._createMaps();
        this.script = new ScriptEngineManager();
        this.eng = script.getEngineByName("JavaScript");
    }
    
    private void _createFunctions(){
        this.opCodesDetails = new ArrayList<>(6);
        this.opCodesDetails.add(new OBDFunctions("01 1F\r", "Engine runtime",  2, "sec", "256*XX+YY"));
        this.opCodesDetails.add(new OBDFunctions("01 0F\r", "Intake air temperature", 1, "C", "XX-40"));
        this.opCodesDetails.add(new OBDFunctions("01 11\r", "Throttle position", 1, "%", "XX*100/225"));
        this.opCodesDetails.add(new OBDFunctions("01 0C\r", "Engine RPM", 2 , "RPM", "((XX*256)+YY)/4"));
        this.opCodesDetails.add(new OBDFunctions("01 0D\r", "Vehicle speed", 1, "Km/h", "XX"));
        this.opCodesDetails.add(new OBDFunctions("01 05\r", "Coolant temperature", 1, "C", "XX-40"));
    }
    
    private void _createMaps() {
        this.opCodes = new HashMap<>(6);
        for(int i =0 ; i < this.opCodesDetails.size(); ++i){
            this.opCodes.put(i+1, this.opCodesDetails.get(i));
        }
    }
    
    @Override
    public void sendRequest() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(this.inputStream = this.sockets.getInputStream()));
            DataOutputStream outStream = new DataOutputStream(this.outputStream = this.sockets.getOutputStream());
            OBDFunctions function = this.opCodes.get(this.obdCode);
            FileWriter outputfile = new FileWriter(function.output);
            CSVWriter writer = new CSVWriter(outputfile);
            long BeginTime = System.currentTimeMillis();
            while (System.currentTimeMillis() - BeginTime < 240000) {
                outStream.writeBytes(function.getOBDIIRequest());
                String response = reader.readLine();
                String[] responseArray = response.split(" ");
                responseArray[0] = "0".concat(responseArray[0].substring(1));
                responseArray[2] = String.valueOf(Integer.parseInt(responseArray[2], 16));
                if(responseArray.length == 4 && function.bytes.length == 2) {
                    responseArray[3] = String.valueOf(Integer.parseInt(responseArray[3], 16));
                }
                List<String> list = function.getResultFromCalculation(responseArray, this.eng);
                String[] finalArr = new String[list.size()];
                finalArr = list.toArray(finalArr);
                writer.writeNext(finalArr);
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

