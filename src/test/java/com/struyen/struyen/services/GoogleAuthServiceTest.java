package com.struyen.struyen.services;

import com.struyen.struyen.dtos.GoogleTokenInfoDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoogleAuthServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private GoogleAuthService googleAuthService;

    @Test
    @DisplayName("verifyToken should succeed and return token info for valid Google token")
    void verifyToken_Success() {
        GoogleTokenInfoDto mockInfo = GoogleTokenInfoDto.builder()
                .email("valid@gmail.com")
                .emailVerified("true")
                .sub("123456789")
                .name("Valid Google User")
                .build();

        when(restTemplate.getForObject(anyString(), eq(GoogleTokenInfoDto.class), eq("valid-token")))
                .thenReturn(mockInfo);

        GoogleTokenInfoDto result = googleAuthService.verifyToken("valid-token");

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("valid@gmail.com");
        assertThat(result.getSub()).isEqualTo("123456789");
    }

    @Test
    @DisplayName("verifyToken should throw BAD_REQUEST when token is blank")
    void verifyToken_BlankToken_ThrowsBadRequest() {
        assertThatThrownBy(() -> googleAuthService.verifyToken("   "))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("verifyToken should throw BAD_REQUEST when email is not verified by Google")
    void verifyToken_UnverifiedEmail_ThrowsBadRequest() {
        GoogleTokenInfoDto unverifiedInfo = GoogleTokenInfoDto.builder()
                .email("unverified@gmail.com")
                .emailVerified("false")
                .sub("123456789")
                .build();

        when(restTemplate.getForObject(anyString(), eq(GoogleTokenInfoDto.class), eq("token-unverified")))
                .thenReturn(unverifiedInfo);

        assertThatThrownBy(() -> googleAuthService.verifyToken("token-unverified"))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("verifyToken should throw UNAUTHORIZED when Google API returns HTTP error")
    void verifyToken_GoogleError_ThrowsUnauthorized() {
        when(restTemplate.getForObject(anyString(), eq(GoogleTokenInfoDto.class), eq("invalid-token")))
                .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Bad Request"));

        assertThatThrownBy(() -> googleAuthService.verifyToken("invalid-token"))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.UNAUTHORIZED);
    }
}

