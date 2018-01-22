package com.mc.dubbo.extensions.registry.consul;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.registry.NotifyListener;
import com.alibaba.dubbo.registry.support.FailbackRegistry;
import com.alibaba.dubbo.rpc.RpcException;
import com.mc.dubbo.extensions.registry.consul.client.DubboConsulClient;

import java.util.ArrayList;
import java.util.List;

public class ConsulRegistry extends FailbackRegistry {
    private final static Logger logger = LoggerFactory.getLogger(ConsulRegistry.class);
    private DubboConsulClient client;
    private ConsulHeartbeatManager heartbeatManager;


    public ConsulRegistry(URL url, DubboConsulClient client) {
        super(url);
        this.client = client;
        heartbeatManager = new ConsulHeartbeatManager(client);
        heartbeatManager.start();
    }

    @Override
    protected void doRegister(URL url) {
        ConsulService service = ConsulUtils.buildService(url);
        client.registerService(service);
        //注册服务后需要立刻check一次，以使服务状态为健康
        client.checkPass(service.getId());
        heartbeatManager.addHeartbeatServcieId(service.getId());
    }

    @Override
    protected void doUnregister(URL url) {
        ConsulService service = ConsulUtils.buildService(url);
        client.unregisterService(service.getId());
        heartbeatManager.removeHeartbeatServiceId(service.getId());
    }

    @Override
    protected void doSubscribe(URL url, NotifyListener listener) {
        listener.notify(lookup(url));
    }

    @Override
    protected void doUnsubscribe(URL url, NotifyListener listener) {
        close();
    }

    @Override
    public boolean isAvailable() {
        return client.isConnected();
    }

    @Override
    public List<URL> lookup(URL url) {
        if (url == null) {
            throw new IllegalArgumentException("lookup url == null");
        }
        try {
            ConsulService service = ConsulUtils.buildService(url);
            ConsulResponse<List<ConsulService>> response = client.lookupHealthService(service.getName(), 0L);
            List<ConsulService> services = response.getValue();
            List<URL> urls = new ArrayList<>(services.size());
            for (ConsulService service1 : services) {
                urls.add(ConsulUtils.buildUrl(service1));
            }
            return urls;
        } catch (Throwable e) {
            throw new RpcException("Failed to lookup " + url + " from consul " + getUrl() + ", cause: " + e.getMessage(), e);
        }
    }

    @Override
    public void destroy() {
        if (!canDestroy()) {
            return;
        }
        super.destroy();
        try {
            heartbeatManager.close();
        } catch (Throwable t) {
            logger.warn(t.getMessage(), t);
        }
    }

    public void close() {
        heartbeatManager.close();
    }
}
