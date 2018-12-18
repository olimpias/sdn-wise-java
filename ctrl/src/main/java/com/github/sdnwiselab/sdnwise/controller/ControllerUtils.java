package com.github.sdnwiselab.sdnwise.controller;

import com.github.sdnwiselab.sdnwise.util.NodeAddress;

import java.util.Arrays;
import java.util.List;

public class ControllerUtils {

    private ControllerUtils(){}

    static boolean doNodeAddresslistsEqual(List<NodeAddress> firstList, List<NodeAddress> secondList) {
        if(firstList.size() != secondList.size()) {
            return false;
        }
        for (int i = 0;i< firstList.size();i ++ ) {
            if (!Arrays.equals(firstList.get(i).toByteArray(),secondList.get(i).toByteArray())){
                return false;
            }
        }
        return true;
    }
}
