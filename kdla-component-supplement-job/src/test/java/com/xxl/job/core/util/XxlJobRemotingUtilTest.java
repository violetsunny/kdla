package com.xxl.job.core.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class XxlJobRemotingUtilTest {

    @Test
    void nullConfiguredTokenRejected() {
        assertThat(XxlJobRemotingUtil.validAccessToken(null, "anything")).isFalse();
    }

    @Test
    void emptyConfiguredTokenRejected() {
        assertThat(XxlJobRemotingUtil.validAccessToken("", "anything")).isFalse();
    }

    @Test
    void blankConfiguredTokenRejected() {
        assertThat(XxlJobRemotingUtil.validAccessToken("   ", "anything")).isFalse();
    }

    @Test
    void nullRequestedTokenRejected() {
        assertThat(XxlJobRemotingUtil.validAccessToken("secret", null)).isFalse();
    }

    @Test
    void emptyRequestedTokenRejected() {
        assertThat(XxlJobRemotingUtil.validAccessToken("secret", "")).isFalse();
    }

    @Test
    void mismatchedTokenRejected() {
        assertThat(XxlJobRemotingUtil.validAccessToken("secret", "wrong")).isFalse();
    }

    @Test
    void matchingTokenAccepted() {
        assertThat(XxlJobRemotingUtil.validAccessToken("secret", "secret")).isTrue();
    }
}
