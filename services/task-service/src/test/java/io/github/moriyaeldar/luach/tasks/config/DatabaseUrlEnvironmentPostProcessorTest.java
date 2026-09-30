package io.github.moriyaeldar.luach.tasks.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DatabaseUrlEnvironmentPostProcessorTest {

    private final DatabaseUrlEnvironmentPostProcessor processor = new DatabaseUrlEnvironmentPostProcessor();

    @Test
    void convertsAHostedPostgresUrl() {
        var env = new MockEnvironment().withProperty("DATABASE_URL",
                "postgresql://luach:p%40ss@ep-cool-1.eu-central-1.aws.neon.tech/tasks_db?sslmode=require&channel_binding=require");
        processor.postProcessEnvironment(env, new SpringApplication());

        assertThat(env.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:postgresql://ep-cool-1.eu-central-1.aws.neon.tech/tasks_db?sslmode=require");
        assertThat(env.getProperty("spring.datasource.username")).isEqualTo("luach");
        assertThat(env.getProperty("spring.datasource.password")).isEqualTo("p@ss");
    }

    @Test
    void keepsThePort() {
        assertThat(DatabaseUrlEnvironmentPostProcessor.toDatasourceProperties("postgres://u:p@db:6543/x"))
                .containsEntry("spring.datasource.url", "jdbc:postgresql://db:6543/x");
    }

    @Test
    void explicitJdbcSettingsWin() {
        var env = new MockEnvironment()
                .withProperty("DATABASE_URL", "postgresql://u:p@host/db")
                .withProperty("DB_URL", "jdbc:postgresql://localhost/tasks_db");
        processor.postProcessEnvironment(env, new SpringApplication());
        assertThat(env.getPropertySources().contains(DatabaseUrlEnvironmentPostProcessor.SOURCE_NAME)).isFalse();
    }

    @Test
    void rejectsOtherDatabases() {
        assertThatThrownBy(() -> DatabaseUrlEnvironmentPostProcessor.toDatasourceProperties("mysql://u:p@h/db"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
