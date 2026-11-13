package com.struyen.struyen.services;

import com.struyen.struyen.dtos.GoogleTokenInfoDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleAuthService {

    private final RestTemplate restTemplate;

    @Value("${google.client-id:}")
    private String expectedClientId;

    private static final String GOOGLE_TOKENINFO_URL = "https://oauth2.googleapis.com/tokeninfo?id_token={idToken}";

    /**
     * Xac thuc Google ID Token thong qua Google Tokeninfo API
     */
    public GoogleTokenInfoDto verifyToken(String idToken) {
        if (idToken == null || idToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Google credential khong duoc de trong.");
        }

        try {
            GoogleTokenInfoDto tokenInfo = restTemplate.getForObject(
                    GOOGLE_TOKENINFO_URL,
                    GoogleTokenInfoDto.class,
                    idToken.trim()
            );

            if (tokenInfo == null || tokenInfo.getEmail() == null || tokenInfo.getSub() == null) {
                log.warn("Google tokeninfo tra ve ket qua rong hoac thieu thong tin quan trong.");
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Google token khong hop le.");
            }

            if (!"true".equalsIgnoreCase(tokenInfo.getEmailVerified())) {
                log.warn("Email Google {} chua duoc xac minh.", tokenInfo.getEmail());
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email Google chua duoc xac minh boi Google.");
            }

            if (expectedClientId != null && !expectedClientId.isBlank()) {
                if (!expectedClientId.equals(tokenInfo.getAud())) {
                    log.warn("Audience cua Google token {} khong khop voi google.client-id cau hinh {}",
                            tokenInfo.getAud(), expectedClientId);
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Google Client ID khong hop le.");
                }
            }

            return tokenInfo;
        } catch (HttpStatusCodeException ex) {
            log.warn("Loi xac thuc token tu Google API: HTTP {} - {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Xac thuc tai khoan Google that bai hoac token da het han.");
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Loi ket noi den Google Tokeninfo API: {}", ex.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Khong the ket noi den may chu Google de xac thuc token.");
        }
    }
}

