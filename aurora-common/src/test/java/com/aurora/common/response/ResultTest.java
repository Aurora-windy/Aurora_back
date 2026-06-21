package com.aurora.common.response;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link Result} 单元测试 - Phase 0 出口验收要求
 */
class ResultTest {

    @Test
    void ok_shouldReturnCode200AndDefaultMsg() {
        Result<Void> result = Result.ok();
        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getMsg()).isEqualTo("操作成功");
        assertThat(result.getData()).isNull();
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void okWithData_shouldReturnCode200AndData() {
        Result<String> result = Result.ok("hello");
        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isEqualTo("hello");
    }

    @Test
    void fail_shouldReturnCode500AndCustomMsg() {
        Result<Void> result = Result.fail("出错了");
        assertThat(result.getCode()).isEqualTo(500);
        assertThat(result.getMsg()).isEqualTo("出错了");
        assertThat(result.getData()).isNull();
        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    void failWithBizCode_shouldReturnCorrespondingCode() {
        Result<Void> result = Result.fail(BizCode.UNAUTHORIZED);
        assertThat(result.getCode()).isEqualTo(401);
        assertThat(result.getMsg()).isEqualTo("未登录或 Token 已过期");
    }

    @Test
    void failWithCodeAndMsg_shouldReturnGivenValues() {
        Result<Void> result = Result.fail(403, "权限不足");
        assertThat(result.getCode()).isEqualTo(403);
        assertThat(result.getMsg()).isEqualTo("权限不足");
    }
}
