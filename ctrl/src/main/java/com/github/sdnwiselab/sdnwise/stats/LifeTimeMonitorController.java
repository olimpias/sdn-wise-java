package com.github.sdnwiselab.sdnwise.stats;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Created by cemturker on 29.11.2018.
 */
public class LifeTimeMonitorController implements LifeTimeMonitorService {
    private static final Logger logger = Logger.getLogger(LifeTimeMonitorController.class.getName());

    private static LifeTimeMonitorService service;

    private MonitorType type = MonitorType.DEFAULT;
    private Date startTime;
    private Date endTime;
    private boolean isEnded;

    public synchronized static LifeTimeMonitorService Instance(){
        if(service == null) {
            service = new LifeTimeMonitorController();
        }
        return service;
    }

    @Override
    public void setMonitorType(MonitorType type) {
        this.type = type;
    }

    @Override
    public void start() {
        this.startTime = new Date();
        isEnded = false;
        logger.info("Monitor timer has started");
    }

    @Override
    public void end() {
        if(isEnded) {
            logger.info("Monitor timer has already ended");
            return;
        }
        logger.info("Monitor timer has ended");
        isEnded = true;
        this.endTime = new Date();
        dataExporter();
    }

    private void dataExporter(){
        SimpleDateFormat dt = new SimpleDateFormat("yyyyy-mm-dd hh:mm:ss");
        UUID uuid = UUID.randomUUID();
        try {
            PrintWriter printWriter = new PrintWriter(uuid.toString()+".data", "UTF-8");
            StringBuffer stringBuffer = new StringBuffer();
            stringBuffer.append("Type: "+type+"\n");
            stringBuffer.append("Test start time: "+ dt.format(startTime)+"\n");
            stringBuffer.append("Test end time: "+ dt.format(endTime)+"\n");
            stringBuffer.append("Spend time: "+(endTime.getTime() - startTime.getTime())/1000+"s\n");
            printWriter.write(stringBuffer.toString());
            printWriter.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }


}
