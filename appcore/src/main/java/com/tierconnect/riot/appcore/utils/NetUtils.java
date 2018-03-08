package com.tierconnect.riot.appcore.utils;

import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created by agutierrez on 6/4/15.
 */
public class NetUtils {
    static Logger logger = Logger.getLogger(NetUtils.class);

    public static String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("REMOTE_ADDR");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.substring(0, ip.indexOf(",")).trim();
        }
        return ip;
    }

    public static String getServerIpAddress(HttpServletRequest request) {
        return request.getLocalAddr();
    }

    public static int getIp4Int(String address) {
        try {
            InetAddress a = (InetAddress) InetAddress.getByName(address);
            byte[] b = a.getAddress();
            int i = ((b[b.length - 4] & 0xFF) << 24) |
                    ((b[b.length - 3] & 0xFF) << 16) |
                    ((b[b.length - 2] & 0xFF) << 8) |
                    ((b[b.length - 1] & 0xFF) << 0);
            return i;
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return 0;
    }

    //http://stackoverflow.com/questions/4209760/validate-an-ip-address-with-mask
    public static boolean isInSubNet(int ip, int subnet, int bits) {
        // Create bitmask to clear out irrelevant bits. For 10.1.1.0/24 this is
        // 0xFFFFFF00 -- the first 24 bits are 1's, the last 8 are 0's.
        //
        //     -1        == 0xFFFFFFFF
        //     32 - bits == 8
        //     -1 << 8   == 0xFFFFFF00
        int mask = -1 << (32 - bits);

        if ((subnet & mask) == (ip & mask)) {
            // IP address is in the subnet.
            return true;
        }
        return false;
    }

    public static boolean isInSubNet(String ip_, String subnet_) {
        String ip = ip_ != null ? ip_.trim() : "";
        String subnet = subnet_ != null ? subnet_.trim() : "";
        if (!subnet.contains("/")) {
            subnet = subnet+"/32";
        }
        String[] parts = subnet.split("/");
        return isInSubNet(getIp4Int(ip), getIp4Int(parts[0]), Integer.parseInt(parts[1]));
    }

    public static boolean isAddressInSubnet(String address, String subnetList) {
        boolean matches = false;
        address = address != null ? address.trim() : "";
        String[] subNets = subnetList.split(",");
        if ("localhost".equalsIgnoreCase(address) || "127.0.0.1".equals(address) || "::1".equals(address) || "0:0:0:0:0:0:0:1".equals(address) || subnetList.equalsIgnoreCase("ANY")) {
            matches = true;
        } else {
            for (String subnet_ : subNets) {
                String subnet = subnet_ != null ? subnet_.trim() : "";
                if (address.equalsIgnoreCase(subnet)) {
                    matches = true;
                    break;
                }
                try {
                    if (NetUtils.isInSubNet(address, subnet)) {
                        matches = true;
                        break;
                    }
                } catch (Exception ex) {
                    logger.error(ex.getMessage(), ex);
                }
            }
        }
        return matches;
    }

    public static void main(String[] args) {
        System.out.println(isInSubNet("10.1.1.99","10.1.1.99"));
        System.out.println(isInSubNet("10.1.1.99","10.1.1.99/32"));
        System.out.println(isInSubNet("  10.1.1.99  "," 10.1.1.99   "));
        System.out.println(isInSubNet("  10.1.1.99  "," 10.1.1.99/32 "));
        System.out.println(isInSubNet("::FFFF:10.1.1.99", "10.1.1.0/24"));
        System.out.println(isInSubNet("::FFFF:10.1.1.99", "::FFFF:10.1.1.0/24"));
        System.out.println(isInSubNet("::10.1.1.99", "10.1.1.0/24"));
        System.out.println(isInSubNet("::10.1.1.99", "::10.1.1.0/24"));
        System.out.println(isInSubNet("192.168.5.63","192.168.5.0/26"));
        System.out.println(!isInSubNet("192.168.5.127","192.168.5.0/26"));

    }
}
