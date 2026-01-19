package com.resumeopt.service;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class LinkVerifierService {
    private final RestTemplate rest;

    public LinkVerifierService(){
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(3000);
        f.setReadTimeout(3000);
        this.rest = new RestTemplate(f);
    }

    public boolean verify(String url){
        if (url == null || url.isBlank()) {
            return false;
        }
        try {
            java.net.URI uri = java.net.URI.create(url);
            if (uri == null) {
                return false;
            }
            var req = org.springframework.http.RequestEntity.head(uri).build();
            var resp = rest.exchange(req, String.class);
            int code = resp.getStatusCode().value();
            if (code >= 200 && code < 400) return true;
            // Some servers may not support HEAD; fallback to GET with minimal load
            var r2 = rest.exchange(org.springframework.http.RequestEntity.get(uri).build(), String.class);
            return r2.getStatusCode().is2xxSuccessful();
        } catch (Exception e){
            return false;
        }
    }
}