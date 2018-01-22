package com.mc.dubbo.extensions.registry.consul.client;

import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.agent.model.NewService;
import com.ecwid.consul.v1.health.model.HealthService;
import com.ecwid.consul.v1.kv.model.GetValue;
import com.mc.dubbo.extensions.registry.consul.ConsulService;
import com.mc.dubbo.extensions.registry.consul.ConsulUtils;
import com.mc.dubbo.extensions.registry.consul.ConsulConstants;
import com.mc.dubbo.extensions.registry.consul.ConsulResponse;

import java.util.ArrayList;
import java.util.List;

public class ConsulEcwidClient extends DubboConsulClient {
    public  static ConsulClient client;
    static Logger logger= LoggerFactory.getLogger(ConsulEcwidClient.class);
    public ConsulEcwidClient(String host,int port){
        super(host,port);
        client=new ConsulClient(host,port);
    }
    @Override
    public void checkPass(String serviceId) {
        client.agentCheckPass("service:"+serviceId);
    }

    @Override
    public void checkFail(String serviceId) {
        client.agentCheckFail("service:"+serviceId);
    }

    @Override
    public void registerService(ConsulService service) {
        NewService newService=convertService(service);
        client.agentServiceRegister(newService);
    }

    @Override
    public void unregisterService(String serviceId) {
        client.agentServiceDeregister(serviceId);
    }

    @Override
    public ConsulResponse<List<ConsulService>> lookupHealthService(String serviceName, long lastConsulIndex) {
        QueryParams queryParams=new QueryParams(ConsulConstants.CONSUL_BLOCK_TIME_SECONDS,lastConsulIndex);
        Response<List<HealthService>> orgResponse=client.getHealthServices(serviceName,true,queryParams);
        ConsulResponse<List<ConsulService>> newResponse=new ConsulResponse<>();
        if(orgResponse!=null&&orgResponse.getValue()!=null&&!orgResponse.getValue().isEmpty()){
            List<HealthService> healthServices=orgResponse.getValue();
            List<ConsulService> consulServices=new ArrayList<>(healthServices.size());
            for(HealthService orgService:healthServices){
                try{
                    ConsulService newService=convertToConsulService(orgService);
                    consulServices.add(newService);
                }catch (Exception e){
                    String serviceId="null";
                    if(orgService.getService()!=null){
                        serviceId=orgService.getService().getId();
                    }
                    logger.error("convert consul service fail. org consulservice:"
                            + serviceId, e);
                }
            }
            if(!consulServices.isEmpty()){
                newResponse=new ConsulResponse<>();
                newResponse.setValue(consulServices);
                newResponse.setConsulIndex(orgResponse.getConsulIndex());
                newResponse.setConsulLastContact(orgResponse.getConsulLastContact());
                newResponse.setConsulKnownLeader(orgResponse.isConsulKnownLeader());
            }
        }
        return newResponse;
    }

    @Override
    public String lookupCommand(String group) {
        Response<GetValue> response=client.getKVValue(ConsulConstants.CONSUL_COMMAND + ConsulUtils.convertGroupToServiceName(group));
        GetValue value=response.getValue();
        String command="";
        if(value!=null)
        {
            command=value.getDecodedValue();
        }
        return command;
    }

    @Override
    public boolean isConnected() {
        return true;
    }

    private NewService convertService(ConsulService service)
    {
        NewService newService = new NewService();
        newService.setAddress(service.getAddress());
        newService.setId(service.getId());
        newService.setName(service.getName());
        newService.setPort(service.getPort());
        newService.setTags(service.getTags());
        NewService.Check check = new NewService.Check();
        check.setTtl(service.getTtl() + "s");
        newService.setCheck(check);
        return newService;
    }
    private ConsulService convertToConsulService(HealthService healthService) {
        ConsulService service = new ConsulService();
        HealthService.Service org = healthService.getService();
        service.setAddress(org.getAddress());
        service.setId(org.getId());
        service.setName(org.getService());
        service.setPort(org.getPort());
        service.setTags(org.getTags());
        return service;
    }
}
