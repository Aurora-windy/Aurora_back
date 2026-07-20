package com.aurora.ai.provider.service.impl;

import com.aurora.ai.provider.client.OpenAiClientFactory;
import com.aurora.ai.provider.entity.AiEmbeddingConfigDO;
import com.aurora.ai.provider.mapper.AiEmbeddingConfigMapper;
import com.aurora.ai.provider.model.req.EmbeddingConfigSaveReq;
import com.aurora.ai.provider.model.resp.EmbeddingConfigResp;
import com.aurora.ai.provider.support.AiSecretCipher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiEmbeddingConfigServiceImplTest {

    private final AiEmbeddingConfigMapper embeddingConfigMapper = mock(AiEmbeddingConfigMapper.class);
    private final OpenAiClientFactory openAiClientFactory = mock(OpenAiClientFactory.class);
    private final AiEmbeddingConfigServiceImpl embeddingConfigService = new AiEmbeddingConfigServiceImpl(embeddingConfigMapper, openAiClientFactory);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void get_shouldNotExposeGatewayBaseUrlOrApiKeyToBrowser() throws Exception {
        AiEmbeddingConfigDO config = new AiEmbeddingConfigDO();
        config.setId(1L);
        config.setBaseUrl("https://embedding.secret.example/v1");
        config.setApiKeyCipher("sk-embedding-secret");
        config.setModel("text-embedding");
        config.setDimension(1536);
        config.setEnabled(1);
        when(embeddingConfigMapper.selectOne(any())).thenReturn(config);

        EmbeddingConfigResp result = embeddingConfigService.get();

        String json = objectMapper.writeValueAsString(result);
        assertThat(json).doesNotContain("baseUrl");
        assertThat(json).doesNotContain("https://embedding.secret.example/v1");
        assertThat(json).doesNotContain("sk-embedding-secret");
        assertThat(json).contains("hasApiKey");
    }

    @Test
    void test_shouldNotExposeEmbeddingErrorDetailsToBrowser() {
        EmbeddingConfigSaveReq req = new EmbeddingConfigSaveReq();
        req.setBaseUrl("https://embedding.secret.example/v1");
        req.setApiKey("sk-embedding-secret");
        req.setModel("text-embedding");
        doThrow(new RuntimeException("Authorization: Bearer sk-embedding-secret https://embedding.secret.example/v1 failed"))
                .when(openAiClientFactory).testEmbedding(any(AiEmbeddingConfigDO.class));

        String message = embeddingConfigService.test(req).getMessage();

        assertThat(message).doesNotContain("sk-embedding-secret");
        assertThat(message).doesNotContain("https://embedding.secret.example/v1");
        assertThat(message).doesNotContain("Authorization");
        assertThat(message).isEqualTo("embedding connectivity test failed");
    }

    @Test
    void save_shouldStoreEncryptedApiKeyCipher() {
        EmbeddingConfigSaveReq req = new EmbeddingConfigSaveReq();
        req.setBaseUrl("https://embedding.secret.example/v1");
        req.setApiKey("sk-embedding-secret");
        req.setModel("text-embedding");

        embeddingConfigService.save(req);

        ArgumentCaptor<AiEmbeddingConfigDO> captor = ArgumentCaptor.forClass(AiEmbeddingConfigDO.class);
        verify(embeddingConfigMapper).insert(captor.capture());
        String stored = captor.getValue().getApiKeyCipher();
        assertThat(stored).startsWith("enc:v1:");
        assertThat(stored).doesNotContain("sk-embedding-secret");
        assertThat(AiSecretCipher.decrypt(stored)).isEqualTo("sk-embedding-secret");
    }
}