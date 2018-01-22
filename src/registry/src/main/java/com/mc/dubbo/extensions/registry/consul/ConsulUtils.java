package com.mc.dubbo.extensions.registry.consul;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import sun.misc.BASE64Encoder;

import java.io.UnsupportedEncodingException;
import java.lang.management.ManagementFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConsulUtils {

    /**
     * 判断两个list中的url是否一致。 如果任意一个list为空，则返回false； 此方法并未做严格互相判等
     *
     * @param urls1
     * @param urls2
     * @return
     */
    public static boolean isSame(List<URL> urls1, List<URL> urls2) {
        if (urls1 == null || urls2 == null) {
            return false;
        }
        if (urls1.size() != urls2.size()) {
            return false;
        }
        return urls1.containsAll(urls2);
    }

    /**
     * 根据服务的url生成consul对应的service
     *
     * @param url
     * @return
     */
    public static ConsulService buildService(URL url) {
        ConsulService service = new ConsulService();
        service.setAddress(url.getHost());
        service.setName(url.getServiceInterface());
        service.setId(ConsulUtils.convertServiceId(url));//+"_"+URL.encode(url.toFullString())
        service.setPort(url.getPort());
        service.setTtl(ConsulConstants.TTL);

        List<String> tags = new ArrayList<String>();
        tags.add(ConsulConstants.CONSUL_TAG_PROTOCOL + url.getProtocol());
        tags.add(ConsulConstants.CONSUL_TAG_URL + URL.encode(url.toFullString()));
        tags.add(ConsulConstants.NODE_TYPE+"_"+url.getParameter(Constants.SIDE_KEY));
        service.setTags(tags);

        return service;
    }

    /**
     * 根据service生成URL
     *
     * @param service
     * @return
     */
    public static URL buildUrl(ConsulService service) {
        URL url = null;
        for (String tag : service.getTags()) {
            if (tag.startsWith(ConsulConstants.CONSUL_TAG_URL)) {
                String encodeUrl = tag.substring(tag.indexOf("_") + 1);
                url = URL.valueOf(URL.decode(encodeUrl));
            }
        }
        if (url == null) {
            Map<String, String> params = new HashMap<String, String>();
            String group = service.getName().substring(ConsulConstants.CONSUL_SERVICE_PRE.length());
            params.put(Constants.GROUP_KEY, group);
            params.put(ConsulConstants.NODE_TYPE, service.getTags().get(2));
            String protocol = ConsulUtils.getProtocolFromTag(service.getTags().get(0));
            url = new URL(protocol, service.getAddress(), service.getPort(),
                    ConsulUtils.getPathFromServiceId(service.getId()), params);
        }
        return url;
    }

    /**
     * 根据url获取cluster信息，cluster 信息包括协议和path（rpc服务中的接口类）。
     *
     * @param url
     * @return
     */
    public static String getUrlClusterInfo(URL url) {
        return url.getProtocol() + "-" + url.getPath();
    }

    /**
     * 有motan的group生成consul的serivce name
     *
     * @param group
     * @return
     */
    public static String convertGroupToServiceName(String group) {
        return ConsulConstants.CONSUL_SERVICE_PRE + group;
    }

    /**
     * 从consul的service name中获取motan的group
     *
     * @param group
     * @return
     */
    public static String getGroupFromServiceName(String group) {
        return group.substring(ConsulConstants.CONSUL_SERVICE_PRE.length());
    }

//    /**
//     * 根据motan的url生成consul的serivce id。 serviceid 包括ip＋port＋rpc服务的接口类名
//     *
//     * @param url
//     * @return
//     */
//    public static String convertConsulSerivceId(URL url) {
//        if (url == null) {
//            return null;
//        }
//        return convertServiceId(url.getHost(), url.getPort(), url.getPath());
//    }

    /**
     * 从consul 的serviceid中获取rpc服务的接口类名（url的path）
     *
     * @param serviceId
     * @return
     */
    public static String getPathFromServiceId(String serviceId) {
        return serviceId.substring(serviceId.indexOf("-") + 1);
    }

    /**
     * 从consul的tag获取motan的protocol
     *
     * @param tag
     * @return
     */
    public static String getProtocolFromTag(String tag) {
        return tag.substring(ConsulConstants.CONSUL_TAG_PROTOCOL.length());
    }


    public static String convertServiceId(URL url) {
        try
        {
            return url.getParameter(Constants.SIDE_KEY)+"_"+url.getProtocol()+"_"+url.getHost()
                    +"_"+url.getPort()+"_"+ConsulUtils.currentPid();
        }catch (Exception e){
            return "";
        }

    }
    public static String encoderByMd5(String str) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest md5=MessageDigest.getInstance("MD5");
        BASE64Encoder base64en = new BASE64Encoder();
        //加密后的字符串
        String newstr=base64en.encode(md5.digest(str.getBytes("utf-8")));
        return newstr;
    }

    public static String currentPid(){
        String name = ManagementFactory.getRuntimeMXBean().getName();
        System.out.println(name);
// get pid
        String pid = name.split("@")[0];
        return pid;
    }

}
