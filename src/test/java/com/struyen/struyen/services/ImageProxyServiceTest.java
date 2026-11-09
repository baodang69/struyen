package com.struyen.struyen.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImageProxyServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private ImageProxyService imageProxyService;

    @BeforeEach
    void setUp() {
        imageProxyService = new ImageProxyService(restTemplate);
    }

    @Test
    void shouldReturnRealImageWhenCdnRespondsOk() {
        byte[] expectedBytes = new byte[]{1, 2, 3};
        ResponseEntity<byte[]> cdnResponse = ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(expectedBytes);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(byte[].class)))
                .thenReturn(cdnResponse);

        ResponseEntity<byte[]> response = imageProxyService.fetchImage("https://sv1.otruyencdn.com/test.jpg");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertArrayEquals(expectedBytes, response.getBody());
        assertEquals(MediaType.IMAGE_JPEG, response.getHeaders().getContentType());
    }

    @Test
    void shouldReturnFallbackSvgWhenCdnReturns404() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(byte[].class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        ResponseEntity<byte[]> response = imageProxyService.fetchImage("https://img.otruyenapi.com/uploads/comics/sample.jpg");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(MediaType.parseMediaType("image/svg+xml"), response.getHeaders().getContentType());
        assertTrue(new String(response.getBody()).contains("KOMIKA"));
    }

    @Test
    void shouldNormalizeRelativeFilenameAndFetch() {
        byte[] expectedBytes = new byte[]{4, 5, 6};
        ResponseEntity<byte[]> cdnResponse = ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(expectedBytes);

        when(restTemplate.exchange(eq("https://img.otruyenapi.com/uploads/comics/comic-thumb.jpg"), eq(HttpMethod.GET), any(), eq(byte[].class)))
                .thenReturn(cdnResponse);

        ResponseEntity<byte[]> response = imageProxyService.fetchImage("comic-thumb.jpg");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertArrayEquals(expectedBytes, response.getBody());
    }

    @Test
    void shouldBlockSsrfUrlAndReturnFallback() {
        ResponseEntity<byte[]> response = imageProxyService.fetchImage("http://169.254.169.254/latest/meta-data");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(MediaType.parseMediaType("image/svg+xml"), response.getHeaders().getContentType());
    }
}
