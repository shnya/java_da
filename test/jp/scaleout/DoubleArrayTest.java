package jp.scaleout;

import java.util.ArrayList;
import java.util.List;

public class DoubleArrayTest {

    public static void main(String[] args){
        DoubleArray da = new DoubleArray();
        // da.printArray();
        da.insert("aaa");
        // da.printArray();
        da.insert("abc");
        // da.printArray();
        System.out.println(da.exactMatch("aaa"));
        System.out.println(da.exactMatch("abc"));
        List<String> resultStrs = new ArrayList<String>();
        List<Integer> resultIds = new ArrayList<Integer>();
        da.enumerate("", resultStrs, resultIds);
        for(int i = 0; i < resultStrs.size(); i++){
            System.out.println(resultStrs.get(i) + ":" + resultIds.get(i));
        }
        List<String> resStrs = da.extractAllMatched("aaabbbdccabcddd");
        for(int i = 0; i < resStrs.size(); i++){
            System.out.println(resStrs.get(i));
        }
    }
}
