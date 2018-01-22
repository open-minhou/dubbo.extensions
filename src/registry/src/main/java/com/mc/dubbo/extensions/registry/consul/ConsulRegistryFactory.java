package com.mc.dubbo.extensions.registry.consul;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.registry.Registry;
import com.alibaba.dubbo.registry.support.AbstractRegistryFactory;
import com.mc.dubbo.extensions.registry.consul.client.ConsulEcwidClient;
import com.mc.dubbo.extensions.registry.consul.client.DubboConsulClient;
import org.apache.commons.lang3.StringUtils;

public class ConsulRegistryFactory extends AbstractRegistryFactory {

    @Override
    protected Registry createRegistry(URL url) {
        String host = ConsulConstants.DEFAULT_HOST;
        int port = ConsulConstants.DEFAULT_PORT;
        if (StringUtils.isNotBlank(url.getHost())) {
            host = url.getHost();
        }
        if (url.getPort() > 0) {
            port = url.getPort();
        }
        //可以使用不同的client实现
        DubboConsulClient client = new ConsulEcwidClient(host, port);
        return new ConsulRegistry(url, client);
    }
}
