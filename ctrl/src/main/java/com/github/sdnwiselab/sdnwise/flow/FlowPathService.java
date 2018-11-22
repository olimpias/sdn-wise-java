package com.github.sdnwiselab.sdnwise.flow;

import com.github.sdnwiselab.sdnwise.util.NodeAddress;

import java.util.LinkedList;
import java.util.Set;

/**
 * Created by cemturker on 21.11.2018.
 */
public interface FlowPathService {
    void addPath(SrcDstPair pair, LinkedList<NodeAddress> path);
    LinkedList<NodeAddress> getPath(SrcDstPair pair);
    Set<SrcDstPair> getPairs();
}
