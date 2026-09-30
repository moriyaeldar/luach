package io.github.moriyaeldar.luach.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PemEnvironmentPostProcessorTest {

    private static final String PEM = "-----BEGIN CERTIFICATE-----\nMIIBszCCAVmgAwIBAgIU\n-----END CERTIFICATE-----\n";

    @Test
    void keepsPlainPem() {
        assertThat(PemEnvironmentPostProcessor.toPem(PEM)).isEqualTo(PEM);
    }

    @Test
    void expandsEscapedLineBreaks() {
        assertThat(PemEnvironmentPostProcessor.toPem(PEM.replace("\n", "\\n"))).isEqualTo(PEM);
    }

    @Test
    void decodesBase64() {
        String encoded = Base64.getEncoder().encodeToString(PEM.getBytes(StandardCharsets.UTF_8));
        assertThat(PemEnvironmentPostProcessor.toPem(encoded)).isEqualTo(PEM);
    }

    @Test
    void rejectsSomethingElse() {
        assertThatThrownBy(() -> PemEnvironmentPostProcessor.toPem("not a certificate"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void overridesOnlyVariablesThatNeedIt() {
        var env = new MockEnvironment()
                .withProperty("KAFKA_CA_CERT", PEM)
                .withProperty("KAFKA_ACCESS_KEY", PEM.replace("\n", "\\n"));
        new PemEnvironmentPostProcessor().postProcessEnvironment(env, new SpringApplication());

        assertThat(env.getProperty("KAFKA_ACCESS_KEY")).isEqualTo(PEM);
        assertThat(env.getProperty("KAFKA_CA_CERT")).isEqualTo(PEM);
        assertThat(env.getProperty("KAFKA_ACCESS_CERT")).isNull();
    }
}
