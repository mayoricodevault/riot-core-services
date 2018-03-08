package com.tierconnect.riot.ehcache;

import org.reflections.Reflections;

import javax.persistence.Cacheable;
import javax.persistence.Entity;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.TreeSet;

/**
 * Created by agutierrez on 12/9/15.
 */
public class EhcacheConfigGen {

    public static void main (String[] args) {
        Reflections reflections = new Reflections("com.tierconnect");
        Set<String> classes = new TreeSet<>();
        Set<Class<?>> annotated = reflections.getTypesAnnotatedWith(Entity.class);
        for (Class clazz : annotated) {
            Cacheable cacheable = (Cacheable) clazz.getAnnotation(Cacheable.class);
            if (cacheable == null || cacheable.value() == true) {
                classes.add(clazz.getCanonicalName());
            }
        }
//        for (String className: classes) {
//            System.out.println("Class name:"+className);
//        }
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        System.out.println("Introduce host y ports of the servers in the cluster, for example: 10.100.1.1:4001,10.100.1.2:4001");
        try {
            String hostports = in.readLine();
            if (hostports!=null) {
                final String[] hostPortsSplit = hostports.split(",");
                for (int i = 0; i < hostPortsSplit.length; i++) {
                    String[] auxi = hostPortsSplit[i].split(":");
                    String hostLocal = auxi[0];
                    String portLocal = auxi[1];
                    System.out.println("Cache configuration for Host:" + hostLocal + "\r\n\r\n");

                    String s1 = "    <cacheManagerPeerListenerFactory class=\"net.sf.ehcache.distribution.RMICacheManagerPeerListenerFactory\" " +
                            "properties=\"hostName=" + hostLocal + ", port=" + portLocal + ", socketTimeoutMillis=2000\"/>";
                    System.out.println(s1);

                    StringBuilder s2 = new StringBuilder();
                    s2.append("    <cacheManagerPeerProviderFactory class=\"net.sf.ehcache.distribution.RMICacheManagerPeerProviderFactory\" properties=\"peerDiscovery=manual,rmiUrls=\r\n        ");
                    for (int j = 0; j < hostPortsSplit.length; j++) {
                        if (i != j) {
                            String[] auxj = hostPortsSplit[j].split(":");
                            String hostRemote = auxj[0];
                            String portRemote = auxj[1];
                            StringBuilder s3 = new StringBuilder();
                            for (String className : classes) {
                                final int length = s3.length();
                                if (length != 0) {
                                    s3.append("\r\n        |");
                                }
                                s3.append("//" + hostRemote + ":" + portRemote + "/" + className);
                            }
                            s2.append(s3);
                        }
                    }
                    s2.append("\"/>");
                    System.out.println(s2);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
