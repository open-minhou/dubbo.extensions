package com.mc.dubbo.extensions.registry.consul;

import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.ConcurrentHashSet;
import com.mc.dubbo.extensions.registry.consul.client.DubboConsulClient;

import java.util.concurrent.*;

public class ConsulHeartbeatManager {

    private DubboConsulClient client;
    private ConcurrentHashSet<String> serviceIds=new ConcurrentHashSet<>();
    private ThreadPoolExecutor jobExecutor;
    private ScheduledExecutorService heartbeatExecutor;
    private volatile boolean currentHeartBeatSwitcherStatus=false;
    static Logger logger= LoggerFactory.getLogger(ConsulHeartbeatManager.class);

    public ConsulHeartbeatManager(DubboConsulClient client){
        this.client=client;
        heartbeatExecutor= Executors.newSingleThreadScheduledExecutor();
        ArrayBlockingQueue<Runnable> workQueue=new ArrayBlockingQueue<>(10000);
        jobExecutor=new ThreadPoolExecutor(5,30,30*1000,
                TimeUnit.MILLISECONDS,workQueue);

    }
    public void start() {
        heartbeatExecutor.scheduleAtFixedRate(
                () -> {
                    processHeartbeat(true);
                }, 0,
                25*1000, TimeUnit.MILLISECONDS);
    }



    protected void processHeartbeat(boolean isPass) {
        for (String serviceId : serviceIds) {
            try {
                jobExecutor.execute(new HeartbeatJob(serviceId, isPass));
            } catch (RejectedExecutionException ree) {
                logger.error("execute heartbeat job fail! serviceId:"
                        + serviceId + " is rejected");
            }
        }
    }

    public void close() {
        for (String serviceId : serviceIds) {
            try {
                client.unregisterService(serviceId);
            } catch (RejectedExecutionException ree) {
                logger.error("execute heartbeat job fail! serviceId:"
                        + serviceId + " is rejected");
            }
        }
        heartbeatExecutor.shutdown();
        jobExecutor.shutdown();
        logger.info("Consul heartbeatManager closed.");
    }

    /**
     * 添加consul serviceId，添加后的serviceId会通过定时设置passing状态保持心跳。
     *
     * @param serviceId
     */
    public void addHeartbeatServcieId(String serviceId) {
        serviceIds.add(serviceId);
    }

    /**
     * 移除serviceId，对应的serviceId不会在进行心跳。
     *
     * @param serviceId
     */
    public void removeHeartbeatServiceId(String serviceId) {
        serviceIds.remove(serviceId);
    }

    // 检查心跳开关是否打开
    private boolean isHeartbeatOpen() {
        return currentHeartBeatSwitcherStatus;
    }

    public void setHeartbeatOpen(boolean open) {
        currentHeartBeatSwitcherStatus = open;
    }

    class HeartbeatJob implements Runnable {
        private String serviceId;
        private boolean isPass;

        public HeartbeatJob(String serviceId, boolean isPass) {
            super();
            this.serviceId = serviceId;
            this.isPass = isPass;
        }

        @Override
        public void run() {
            try {
                if (isPass) {
                    client.checkPass(serviceId);
                } else {
                    client.checkFail(serviceId);
                }
            } catch (Exception e) {
                logger.error(
                        "consul heartbeat-set check pass error!serviceId:"
                                + serviceId, e);
            }

        }

    }

    public void setClient(DubboConsulClient client) {
        this.client = client;
    }


}
