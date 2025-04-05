package com.whatstheplan.events.testconfig.wiremock;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

public class AuthWireMockExtension extends WireMockExtension {

    private static final int PORT = 8091;

    public AuthWireMockExtension() {
        super(newInstance().options(wireMockConfig().port(PORT)));
    }

    public void stubForToken() {

        stubFor(post(urlEqualTo("/oauth2/token"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .withBody("""
                                {
                                    "access_token": "token",
                                    "token_type": "bearer",
                                    "expires_in": "5000"
                                }
                                """)));
    }
}
