package com.whatstheplan.events.testconfig.wiremock;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.whatstheplan.events.client.user.response.BasicUserResponse;

import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder.okForJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.whatstheplan.events.testconfig.utils.DataMockUtils.generateBasicUserResponse;

public class UserWireMockExtension extends WireMockExtension {

    private static final int PORT = 8090;

    public UserWireMockExtension() {
        super(newInstance().options(wireMockConfig().port(PORT)));
    }

    public void stubForUser(UUID userId, String username) {
        BasicUserResponse response = generateBasicUserResponse(username);
        stubFor(get(urlEqualTo("/users-info/" + userId))
                .willReturn(okForJson(response)));
    }
}
