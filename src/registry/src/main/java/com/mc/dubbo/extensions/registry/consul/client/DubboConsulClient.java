package com.mc.dubbo.extensions.registry.consul.client;

import com.mc.dubbo.extensions.registry.consul.ConsulService;
import com.mc.dubbo.extensions.registry.consul.ConsulResponse;

import java.util.List;

public abstract class DubboConsulClient {

    protected String host;
    protected int port;

    public DubboConsulClient(String host,int port){
        this.host=host;
        this.port=port;
    }

    public abstract void checkPass(String serviceId);

    public abstract void checkFail(String serviceId);

    public abstract void registerService(ConsulService service);

    public abstract void unregisterService(String serviceId);

    public abstract ConsulResponse<List<ConsulService>> lookupHealthService(
            String serviceName, long lastConsulIndex);

    public abstract String lookupCommand(String group);
    public abstract boolean isConnected();
}
