package com.github.sdnwiselab.sdnwise.flow;

import com.github.sdnwiselab.sdnwise.util.NodeAddress;

import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Created by cemturker on 21.11.2018.
 */
public class FlowPathManager implements FlowPathService{

    protected static final Logger LOGGER = Logger.getLogger("FlowPath");


    private Map<SrcDstPair, LinkedList<NodeAddress>> pathMap;
    private static FlowPathService flowPathService;

    public static synchronized FlowPathService SingletonInstance() {
        if (flowPathService == null) {
            flowPathService = new FlowPathManager();
        }
        return flowPathService;
    }

    private FlowPathManager() {
        this.pathMap = new ConcurrentHashMap<>();
    }

    @Override
    public void addPath(SrcDstPair pair, LinkedList<NodeAddress> path) {
        pathMap.put(pair, path);
    }

    @Override
    public LinkedList<NodeAddress> getPath(SrcDstPair pair) {
        return pathMap.get(pair);
    }

    @Override
    public Set<SrcDstPair> getPairs() {
        return pathMap.keySet();
    }

}
