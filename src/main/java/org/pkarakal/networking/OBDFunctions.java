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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.script.ScriptEngineManager;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

public class OBDFunctions {
    public OBDFunctions(String OBDIIRequest, String description, int len, String unit, String formula) {
        this.OBDIIRequest = OBDIIRequest;
        this.description = description;
        this.bytes = new byte[len];
        this.unit = unit;
        this.formula = formula;
        String name = this.description.replaceAll("\\s+", "-").toLowerCase();
        this.output = new File(name.concat(".csv"));
    }
    
    public String getOBDIIRequest() {
        return OBDIIRequest;
    }
    
    public void setOBDIIRequest(String OBDIIRequest) {
        this.OBDIIRequest = OBDIIRequest;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public byte[] getBytes() {
        return bytes;
    }
    
    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }
    
    public String getUnit() {
        return unit;
    }
    
    public void setUnit(String unit) {
        this.unit = unit;
    }
    
    public String getFormula() {
        return formula;
    }
    
    public void setFormula(String formula) {
        this.formula = formula;
    }
    
    public String OBDIIRequest = "";
    public String description = "";
    public byte[] bytes;
    public String unit= "";
    public String formula = "";
    File output;
    
    public List<String> getResultFromCalculation(String[] numbers, ScriptEngine eng){
        String formula = this.getFormula();
        List<String> nums = new ArrayList<String>();
        Collections.addAll(nums, numbers);
        String result = "";
        if(formula.length() == 2){
            formula = formula.replace("XX", nums.get(nums.size() - this.getBytes().length));
            nums.add(formula);
            return nums;
        }
        formula = formula.replace("XX", nums.get(nums.size() - this.getBytes().length));
        if(this.bytes.length == 2)
            formula = formula.replace("YY", nums.get(nums.size() - 1));
        try {
            result = eng.eval(formula).toString();
            nums.add(result);
        } catch (ScriptException e) {
            e.printStackTrace();
        }
        return nums;
    }
}
