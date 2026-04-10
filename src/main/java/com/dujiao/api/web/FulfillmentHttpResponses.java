package com.dujiao.api.web;

import java.nio.charset.StandardCharsets;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/** 与 Go {@code respondFulfillmentDownload} 一致的 {@code text/plain} 附件响应。 */
public final class FulfillmentHttpResponses {

    private FulfillmentHttpResponses() {}

    public static ResponseEntity<byte[]> plaintextAttachment(String orderNo, byte[] body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "plain", StandardCharsets.UTF_8));
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename("fulfillment-" + orderNo + ".txt", StandardCharsets.UTF_8)
                        .build());
        return new ResponseEntity<>(body, headers, HttpStatus.OK);
    }
}
