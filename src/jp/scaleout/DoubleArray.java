// Copyright (C) 2015 Masahiko Higashiyama
//
// Permission is hereby granted, free of charge, to any person obtaining a
// copy of this software and associated documentation files (the "Software"),
// to deal in the Software without restriction, including without limitation
// the rights to use, copy, modify, merge, publish, distribute, sublicense,
// and/or sell copies of the Software, and to permit persons to whom the
// Software is furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.
package jp.scaleout;

import java.io.*;
import java.io.DataInputStream;import java.io.DataOutputStream;import java.io.File;import java.io.FileInputStream;import java.io.FileOutputStream;import java.io.IOException;import java.lang.*;import java.lang.Byte;import java.lang.Integer;import java.lang.Math;import java.lang.RuntimeException;import java.lang.String;import java.lang.System;import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DoubleArray {
    private ArrayList<Node> array_;
    private static final Charset INTERNAL_ENCODING = Charset.forName("UTF-8");

    private static class Node {
        int base;
        int check;
        public Node(int base, int check){
            this.base = base;
            this.check = check;
        }
    }

    private int emptyHead(){
        return array_.get(0).check;
    }

    private int entryNum(){
        return array_.get(0).base;
    }

    private void emptyHead(int head){
        array_.get(0).check = head;
    }

    private void entryNum(int num){
        array_.get(0).base = num;
    }

    private void setCheck(int pos, int base, boolean overwrite){
        if(overwrite && array_.get(pos).check > 0){
            array_.get(pos).check = base;
        }else if(pos == emptyHead()){
            emptyHead(-array_.get(pos).check);
            array_.get(pos).check = base;
        }else{
            int i = emptyHead();
            int n = array_.size();
            while(i < n){
                if(pos == -array_.get(i).check) break;
                i = -array_.get(i).check;
            }
            if(i >= n) throw new RuntimeException("failed set check");
            array_.get(i).check = array_.get(pos).check;
            array_.get(pos).check = base;
        }
    }

    private void deleteCheck(int pos){
        if(pos < emptyHead()){
            array_.get(pos).check = -emptyHead();
            emptyHead(pos);
        }else{
            int i = emptyHead();
            int n = array_.size();
            while(i < n){
                if(i < pos && pos < -array_.get(i).check) break;
                i = -array_.get(i).check;
            }
            if(i >= n) throw new RuntimeException("failed delete check");
            array_.get(pos).check = array_.get(i).check;
            array_.get(i).check = -pos;
        }
    }

    private void expand(int pos){
        if(pos < array_.size()) return;
        int n = array_.size();
        while(pos > n){
            n <<= 1;
        }
        array_.ensureCapacity(n);
        for(int i = array_.size(); i <= pos; i++){
            array_.add(new Node(0, -(i + 1)));
        }
    }

    private int fetch(StringIterator str) {
        int state = 1;
        while(true){
            int c = str.current();
            int t = array_.get(state).base + c;
            if(t < array_.size() && array_.get(t).check == state){
                if(c == 0) return state;
                state = t;
                str.next();
            }else{
                return -state;
            }
        }
    }

    private List<Integer> getLabels(int index, int base) {
        List<Integer> labels = new ArrayList<Integer>();
        int maxlen = Math.min(array_.size(), base + 256);
        for(int i = base; i < maxlen; i++){
            if(array_.get(i).check == index){
                labels.add(i - base);
            }
        }
        return labels;
    }


    private int findBase(List<Integer> codes, int c){
        int baseCand;
        int emptyIndex = emptyHead();
        while(true){
            expand(emptyIndex);
            baseCand = emptyIndex - c;
            if(baseCand <= 1){
                emptyIndex = -array_.get(emptyIndex).check;
                continue;
            }
            boolean found = true;
            for(int i = 0; i < codes.size(); i++){
                expand(baseCand + codes.get(i));
                if(array_.get(baseCand + codes.get(i)).check > 0){
                    found = false;
                    break;
                }
            }
            if(found) break;
            emptyIndex = - array_.get(emptyIndex).check;
        }
        return baseCand;
    }

    private void moveTo(int from, int fromBase, int to){
        array_.get(to).base = fromBase;
        if(fromBase > 0){
            List<Integer> trans = getLabels(from, fromBase);
            for(int j = 0; j < trans.size(); j++){
                //System.out.println("move from check [" + (fromBase + trans.get(j)) + "](" + (char) trans.get(j).intValue() + ") = " + to + "¥n");
                setCheck(fromBase + trans.get(j), to, true);
            }
        }
        //System.out.println("init from address " + (from) + " " + array_.get(from).base + " " + fromBase + "¥n");
        array_.get(from).base = 0;
        deleteCheck(from);
    }

    private void _insert(StringIterator str, int base, int id){
        int pos = array_.get(base).base + str.current();
        expand(Math.max(base, pos));
        if(array_.get(base).base == 0 || array_.get(pos).check >= 0){ //conflict
            int oldbase = array_.get(base).base;
            List<Integer> codes = Collections.emptyList();
            if(oldbase > 0) codes = getLabels(base, oldbase);
            int baseCand = findBase(codes, str.current());
            //System.out.println("set base base[" + base + "] = " + baseCand + "¥n");
            array_.get(base).base = baseCand;
            List<Integer> from = new ArrayList<Integer>();
            List<Integer> fromBase = new ArrayList<Integer>();

            for(int i = 0; i < codes.size(); i++){
                int oldT = oldbase + codes.get(i);
                from.add(oldT);
                fromBase.add(array_.get(oldT).base);
                //System.out.println("move check [" + (baseCand + codes.get(i)) + "](" + (char) codes.get(i).intValue() + ") = " + base + "¥n");
                setCheck(baseCand + codes.get(i), base, false);
            }
            for(int i = 0; i < from.size(); i++){
                moveTo(from.get(i), fromBase.get(i), baseCand + codes.get(i));
            }
            pos = baseCand + str.current();
        }
        //System.out.println("set check [" + (pos) + "](" + (char)str.current() + ") = " + base + "¥n");
        setCheck(pos, base, false);
        if(str.current() != 0){
            str.next();
            _insert(str, pos, id);
        }else{
            if(id < 1){
                array_.get(pos).base = -(entryNum() + 1);
            }else{
                array_.get(pos).base = -id;
            }
            entryNum(entryNum() + 1);
        }
    }

    private void _erase(StringIterator str, int index){
        int newbase = array_.get(index).check;
        deleteCheck(array_.get(index).base + str.current());
        List<Integer> labels = getLabels(index, array_.get(index).base);
        if(labels.size() == 0 && str.hasBefore()){
            str.before();
            _erase(str, newbase);
        }
    }

    private void _enumerate(int currentIdx, List<Byte> path, List<java.lang.String> resultStrs, List<Integer> resultIds) {
        List<Integer> labels = getLabels(currentIdx, array_.get(currentIdx).base);
        for(Integer label : labels){
            int newIdx = array_.get(currentIdx).base + label;
            if(label == 0){
                byte[] bytes = new byte[path.size()];
                for(int i = 0; i < path.size(); i++) bytes[i] = path.get(i);
                resultStrs.add(new String(bytes, INTERNAL_ENCODING));
                resultIds.add(-array_.get(newIdx).base);
            }else{
                path.add((byte)(label > 127 ? label - 256 : label));
                _enumerate(newIdx, path, resultStrs, resultIds);
                path.remove(path.size()-1);
            }
        }
    }

    // For Debug only
    void printArray(){
        System.out.println("[");
        for(int i = 0; i < array_.size(); i++){
            System.out.println(""+ i + ":" + array_.get(i).base + ":" + array_.get(i).check);
        }
        System.out.println("]");
    }


    /* public methods */

    public int exactMatch(String str) {
        StringIterator itr = new StringIterator(str);
        int state = fetch(itr);
        if(state > 0){
            int t = array_.get(state).base + itr.current();
            return -array_.get(t).base;
        }
        return -1;
    }

    public void commonPrefixSearch(StringIterator itr, List<Integer> resLens, List<Integer> resIds) {
        int state = 1;
        int offset = 0;
        while(true){
            int t = array_.get(state).base;
            if(state != 1 && t < array_.size() && array_.get(t).check == state){
                resLens.add(offset);
                resIds.add(-array_.get(t).base);
            }
            int c = itr.current();
            if(t + c < array_.size() && array_.get(t+c).check == state){
                if(c == 0) return;
                state = t + c;
                itr.next();
                offset += 1;
            }else{
                return;
            }
        }
    }

    public void commonPrefixSearch(String str, List<String> resStrs, List<Integer> resIds) {
        List<Integer> resLens = new ArrayList<Integer>();
        commonPrefixSearch(new StringIterator(str), resLens, resIds);
        byte[] bytes = str.getBytes(INTERNAL_ENCODING);
        for(int i = 0; i < resLens.size(); i++){
            resStrs.add(new String(bytes, 0, resLens.get(i), INTERNAL_ENCODING));
        }
    }

    public List<String> commonPrefixSearch(String str) {
        List<Integer> resLens = new ArrayList<Integer>();
        List<Integer> resIds = new ArrayList<Integer>();
        commonPrefixSearch(new StringIterator(str), resLens, resIds);
        List<String> resStrs = new ArrayList<String>();
        byte[] bytes = str.getBytes(INTERNAL_ENCODING);
        for(int i = 0; i < resLens.size(); i++){
            resStrs.add(new String(bytes, 0, resLens.get(i), INTERNAL_ENCODING));
        }
        return resStrs;
    }


    public boolean contains(String str) {
        List<Integer> resIds = new ArrayList<Integer>();
        List<Integer> resLens = new ArrayList<Integer>();
        int currentIdx = 0;
        while(currentIdx < str.length()){
            List<String> res = commonPrefixSearch(str.substring(currentIdx));
            if(res.size() > 0) return true;
            currentIdx++;
        }
        return false;
    }

    public void extractAllMatched(String str, List<Integer> resOffsets, List<String> resStrs, List<Integer> resIds) {
        List<String> bufResStrs = new ArrayList<String>();
        List<Integer> bufResIds = new ArrayList<Integer>();
        int currentIdx = 0;
        while(currentIdx < str.length()){
            bufResStrs.clear();
            bufResIds.clear();

            commonPrefixSearch(str.substring(currentIdx), bufResStrs, bufResIds);
            for(int i = 0; i < bufResStrs.size(); i++){
                resOffsets.add(currentIdx);
                resStrs.add(bufResStrs.get(i));
                resIds.add(bufResIds.get(i));
            }
            currentIdx++;
        }
    }

    public List<String> extractAllMatched(String str){
        List<Integer> resOffsets = new ArrayList<Integer>();
        List<String> resStrs = new ArrayList<String>();
        List<Integer> resIds = new ArrayList<Integer>();
        extractAllMatched(str, resOffsets, resStrs, resIds);
        return resStrs;
    }

    public boolean insert(String str, int id){
        StringIterator itr = new StringIterator(str);
        int state = fetch(itr);
        if(state > 0){
            return false;
        }
        //System.out.println("current:" + (char)itr.current());
        _insert(itr, -state, id);
        return true;
    }

    public boolean insert(String str){
        return insert(str, -1);
    }

    public boolean erase(String str){
        StringIterator itr = new StringIterator(str);
        int state = fetch(itr);
        if(state < 0){
            return false;
        }
        _erase(itr, state);
        return true;
    }

    public void enumerate(String str, List<String> resultStrs, List<Integer> resultIds){
        int index = 1;
        StringIterator itr = new StringIterator(str);
        if(itr.current() != 0){
            int state = fetch(itr);
            if(state > 0){
                index = state;
            }else{
                index = -state;
            }
        }
        List<Byte> path = new ArrayList<Byte>();
        byte[] bytes = str.getBytes(Charset.forName("UTF-8"));
        for(int i = 0; i < bytes.length; i++){
            path.add(bytes[i]);
        }
        _enumerate(index, path, resultStrs, resultIds);
    }

    public boolean build(List<String> words){
        Collections.sort(words);
        for(String word : words){
            if(!this.insert(word)){
                return false;
            }
        }
        return true;
    }

    public void save(String filename) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(2 * array_.size() * Integer.BYTES / Byte.SIZE);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        for(Node node : array_) {
            buf.putInt(node.base);
            buf.putInt(node.check);
        }
        DataOutputStream stream = null;
        try{
            stream = new DataOutputStream(new FileOutputStream(filename));
            stream.write(buf.array());
        }finally{
            if(stream != null) {
                stream.close();
            }
        }
    }

    public void load(String filename) throws IOException {
        File file = new File(filename);
        int bufSize = (int)file.length();
        byte[] bytes = new byte[bufSize];

        DataInputStream stream = null;
        try{
            stream = new  DataInputStream(new FileInputStream(file));
            stream.readFully(bytes);
        }finally{
            if(stream != null) {
                stream.close();
            }
        }
        array_.clear();
        ByteBuffer buf = ByteBuffer.wrap(bytes);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        int arraySize = bufSize / (2 * Integer.SIZE / Byte.SIZE);
        for(int i = 0; i < arraySize; i++){
            int base = buf.getInt();
            int check = buf.getInt();
            array_.add(new Node(base, check));
        }
    }

    public DoubleArray() {
        this.array_ = new ArrayList<Node>();
        this.array_.add(new Node(0, 0));
        entryNum(0);
        emptyHead(1);
        this.array_.add(new Node(0, -2));
        expand(8192);
    }
}
