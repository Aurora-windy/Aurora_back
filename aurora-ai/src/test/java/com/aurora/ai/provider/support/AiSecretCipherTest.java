package com.aurora.ai.provider.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiSecretCipherTest {

    @Test
    void encrypt_shouldStoreCipherTextAndDecryptToOriginalValue() {
        String encrypted = AiSecretCipher.encrypt("sk-secret");

        assertThat(encrypted).startsWith("enc:v1:");
        assertThat(encrypted).doesNotContain("sk-secret");
        assertThat(AiSecretCipher.decrypt(encrypted)).isEqualTo("sk-secret");
    }

    @Test
    void decrypt_shouldKeepLegacyPlainTextReadable() {
        assertThat(AiSecretCipher.decrypt("sk-legacy")).isEqualTo("sk-legacy");
    }
}