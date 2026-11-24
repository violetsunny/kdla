package com.xxl.job.core.executor;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class XxlJobExecutorTest {

    @Test
    void startRefusesNullAccessToken() {
        XxlJobExecutor executor = new XxlJobExecutor();
        executor.setAccessToken(null);
        assertThatThrownBy(executor::start)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("accessToken is empty");
    }

    @Test
    void startRefusesEmptyAccessToken() {
        XxlJobExecutor executor = new XxlJobExecutor();
        executor.setAccessToken("");
        assertThatThrownBy(executor::start)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("accessToken is empty");
    }

    @Test
    void startRefusesBlankAccessToken() {
        XxlJobExecutor executor = new XxlJobExecutor();
        executor.setAccessToken("   ");
        assertThatThrownBy(executor::start)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("accessToken is empty");
    }
}
