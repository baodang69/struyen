package com.struyen.struyen.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.DnsResolver;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Configuration
@Slf4j
public class RestTemplateConfig {

    private static final InetAddress[] MANGADEX_API_FALLBACK_IPS;
    private static final InetAddress[] MANGADEX_UPLOADS_FALLBACK_IPS;

    static {
        try {
            MANGADEX_API_FALLBACK_IPS = new InetAddress[]{
                    InetAddress.getByAddress("api.mangadex.org", new byte[]{(byte) 172, 67, 74, 88}),
                    InetAddress.getByAddress("api.mangadex.org", new byte[]{104, 26, 2, 74}),
                    InetAddress.getByAddress("api.mangadex.org", new byte[]{104, 26, 3, 74})
            };
            MANGADEX_UPLOADS_FALLBACK_IPS = new InetAddress[]{
                    InetAddress.getByAddress("uploads.mangadex.org", new byte[]{45, (byte) 129, (byte) 229, 1}),
                    InetAddress.getByAddress("uploads.mangadex.org", new byte[]{45, (byte) 129, (byte) 229, 2})
            };
        } catch (UnknownHostException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @Bean
    public RestTemplate restTemplate() {
        DnsResolver customDnsResolver = new DnsResolver() {
            @Override
            public InetAddress[] resolve(String host) throws UnknownHostException {
                try {
                    return InetAddress.getAllByName(host);
                } catch (UnknownHostException e) {
                    if ("api.mangadex.org".equalsIgnoreCase(host)) {
                        log.info("[DNS Fallback] Su dung fallback IP cho api.mangadex.org do ISP DNS chan.");
                        return MANGADEX_API_FALLBACK_IPS;
                    }
                    if ("uploads.mangadex.org".equalsIgnoreCase(host) || "dxlb.mangadex.org".equalsIgnoreCase(host)) {
                        log.info("[DNS Fallback] Su dung fallback IP cho uploads.mangadex.org do ISP DNS chan.");
                        return MANGADEX_UPLOADS_FALLBACK_IPS;
                    }
                    throw e;
                }
            }

            @Override
            public String resolveCanonicalHostname(String host) throws UnknownHostException {
                try {
                    return InetAddress.getByName(host).getCanonicalHostName();
                } catch (UnknownHostException e) {
                    return host;
                }
            }
        };

        PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setDnsResolver(customDnsResolver)
                .setMaxConnTotal(100)
                .setMaxConnPerRoute(25)
                .build();

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(6))
                .setResponseTimeout(Timeout.ofSeconds(20))
                .build();

        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .build();

        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
        return new RestTemplate(factory);
    }
}
