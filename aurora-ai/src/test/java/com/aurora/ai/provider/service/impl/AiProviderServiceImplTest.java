package com.aurora.ai.provider.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.aurora.ai.provider.client.OpenAiClientFactory;
import com.aurora.ai.provider.entity.AiModelProviderDO;
import com.aurora.ai.provider.mapper.AiModelProviderMapper;
import com.aurora.ai.provider.model.req.ProviderPageReq;
import com.aurora.ai.provider.model.req.ProviderSaveReq;
import com.aurora.ai.provider.model.resp.ProviderOptionResp;
import com.aurora.ai.provider.model.resp.ProviderResp;
import com.aurora.ai.provider.support.AiSecretCipher;
import com.aurora.common.response.PageResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.mockito.ArgumentCaptor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiProviderServiceImplTest {

    private final AiModelProviderMapper providerMapper = mock(AiModelProviderMapper.class);
    private final OpenAiClientFactory openAiClientFactory = mock(OpenAiClientFactory.class);
    private final AiProviderServiceImpl providerService = new AiProviderServiceImpl(providerMapper, openAiClientFactory);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void listEnabled_shouldNotExposeGatewayBaseUrlToBrowser() throws Exception {
        when(providerMapper.selectList(any())).thenReturn(List.of(provider()));

        List<ProviderOptionResp> options = providerService.listEnabled();

        String json = objectMapper.writeValueAsString(options);
        assertThat(json).doesNotContain("baseUrl");
        assertThat(json).doesNotContain("https://api.secret.example/v1");
        assertThat(json).doesNotContain("sk-secret");
    }

    @Test
    void page_shouldNotExposeGatewayBaseUrlOrApiKeyToBrowser() throws Exception {
        Page<AiModelProviderDO> page = new Page<>(1, 10);
        page.setRecords(List.of(provider()));
        page.setTotal(1);
        when(providerMapper.selectPage(any(), any())).thenReturn(page);

        PageResult<ProviderResp> result = providerService.page(new ProviderPageReq());

        String json = objectMapper.writeValueAsString(result);
        assertThat(json).doesNotContain("baseUrl");
        assertThat(json).doesNotContain("https://api.secret.example/v1");
        assertThat(json).doesNotContain("sk-secret");
        assertThat(json).contains("hasApiKey");
    }

    @Test
    void test_shouldNotExposeProviderErrorDetailsToBrowser() {
        AiModelProviderDO provider = provider();
        when(providerMapper.selectById(1L)).thenReturn(provider);
        doThrow(new RuntimeException("Authorization: Bearer sk-secret https://api.secret.example/v1 failed"))
                .when(openAiClientFactory).testChatCompletion(provider);

        String message = providerService.test(1L).getMessage();

        assertThat(message).doesNotContain("sk-secret");
        assertThat(message).doesNotContain("https://api.secret.example/v1");
        assertThat(message).doesNotContain("Authorization");
        assertThat(message).isEqualTo("provider connectivity test failed");
    }


    @Test
    void create_shouldStoreEncryptedApiKeyCipher() {
        when(providerMapper.selectCount(any())).thenReturn(0L);
        ProviderSaveReq req = new ProviderSaveReq();
        req.setCode("deepseek");
        req.setName("DeepSeek");
        req.setBaseUrl("https://api.secret.example/v1");
        req.setApiKey("sk-secret");
        req.setModel("deepseek-chat");

        providerService.create(req);

        ArgumentCaptor<AiModelProviderDO> captor = ArgumentCaptor.forClass(AiModelProviderDO.class);
        verify(providerMapper).insert(captor.capture());
        String stored = captor.getValue().getApiKeyCipher();
        assertThat(stored).startsWith("enc:v1:");
        assertThat(stored).doesNotContain("sk-secret");
        assertThat(AiSecretCipher.decrypt(stored)).isEqualTo("sk-secret");
    }
    private AiModelProviderDO provider() {
        AiModelProviderDO provider = new AiModelProviderDO();
        provider.setId(1L);
        provider.setCode("deepseek");
        provider.setName("DeepSeek");
        provider.setBaseUrl("https://api.secret.example/v1");
        provider.setApiKeyCipher("sk-secret");
        provider.setModel("deepseek-chat");
        provider.setEnabled(1);
        provider.setSortOrder(1);
        return provider;
    }
}