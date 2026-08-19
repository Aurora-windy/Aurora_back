package com.aurora.common.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 业务错误码枚举
 *
 * <p>两套并存：</p>
 * <ul>
 *   <li>HTTP 风格：200/400/401/403/500 — 通用层，前端按 code === 200/401 等判断</li>
 *   <li>6 段业务码：1xxxx 系统 / 2xxxx HR / 3xxxx EDU / 4xxxx OJ / 5xxxx MALL / 6xxxx AI</li>
 * </ul>
 * <p>spec: docs/specs/2026-07-05-global-variables.md §4.3</p>
 */
@Getter
@AllArgsConstructor
public enum BizCode {

    SUCCESS         (200, "操作成功"),

    // === HTTP 风格通用层（前端按 code === 200/401 等判断） ===
    SYSTEM_ERROR    (500, "系统异常"),
    PARAM_ERROR     (400, "参数校验失败"),
    BIZ_FAIL        (500, "业务处理失败"),
    UNAUTHORIZED    (401, "未登录或 Token 已过期"),
    FORBIDDEN       (403, "权限不足"),
    TOKEN_INVALID   (401, "Token 无效"),
    TOKEN_EXPIRED   (401, "Token 已过期"),
    CAPTCHA_ERROR   (500, "验证码错误或已过期"),
    USER_NOT_FOUND  (500, "用户不存在"),
    USER_PASSWORD_ERROR (500, "用户名或密码错误"),
    USER_DISABLED   (500, "账号已禁用"),
    DATA_NOT_FOUND  (500, "数据不存在"),
    DATA_EXISTS     (500, "数据已存在"),
    OPERATION_FAIL  (500, "操作失败"),

    // === 1xxxx 系统段细分 ===
    // 10xxx 鉴权
    ACCOUNT_LOCKED          (10004, "账号已锁定，请15分钟后再试"),
    REFRESH_TOKEN_INVALID   (10013, "RefreshToken 无效"),
    // 11xxx 文件
    FILE_TOO_LARGE          (10101, "文件超过20MB限制"),
    FILE_TYPE_NOT_ALLOWED   (10102, "不支持的文件类型"),
    // 12xxx 用户/角色/菜单
    USERNAME_DUPLICATED     (12001, "用户名已存在"),
    LAST_ADMIN_PROTECT      (12002, "不允许删除最后一个管理员"),
    ROLE_CODE_DUPLICATED    (12003, "角色编码已存在"),
    ROLE_HAS_USERS          (12004, "该角色下仍有用户，无法删除"),

    // === 2xxxx HR ===
    DEPT_HAS_CHILDREN       (20001, "该部门下有子部门，无法删除"),
    DEPT_HAS_POSITIONS      (20002, "该部门下有岗位，无法删除"),
    DEPT_HAS_EMPLOYEES      (20003, "该部门下有员工，无法删除"),
    EMP_NO_DUPLICATED       (20004, "工号已存在"),
    NOT_CLOCK_TIME          (20005, "当前不在打卡时间范围内"),
    ALREADY_CLOCKED         (20006, "今日已打卡"),
    APPEAL_NOT_FOUND        (20007, "申诉记录不存在"),

    // === 3xxxx EDU ===
    STUDENT_NO_DUPLICATED   (30001, "学号已存在"),
    TEACHER_NO_DUPLICATED   (30002, "教师号已存在"),
    COURSE_FULL             (30003, "课程已满员"),
    DUPLICATE_SELECTION     (30004, "不可重复选课"),
    NOT_IN_SELECTION_PERIOD (30005, "非选课时间段"),
    SCORE_OUT_OF_RANGE      (30006, "成绩必须在0-100之间"),
    SCORE_NOT_YOURS         (30007, "只能录入本人课程的成绩"),
    STUDENT_PROFILE_NOT_BOUND (30008, "当前账号未绑定学生档案，请联系教务管理员"),
    STUDENT_USER_NOT_FOUND     (30009, "绑定的用户不存在或已删除"),
    STUDENT_USER_DISABLED      (30010, "绑定的用户账号已禁用"),
    STUDENT_USER_ROLE_INVALID  (30011, "只能绑定拥有 student 角色的用户"),
    STUDENT_USER_ALREADY_BOUND (30012, "该用户已绑定其他学生档案"),
    STUDENT_PROFILE_DISABLED   (30013, "当前学生档案不是在读状态"),

    // === 4xxxx OJ ===
    PROBLEM_NOT_FOUND       (40001, "题目不存在"),
    LANGUAGE_NOT_SUPPORTED  (40002, "不支持的编程语言"),
    CODE_TOO_LARGE          (40003, "代码长度超出限制"),
    SANDBOX_UNAVAILABLE     (40004, "判题沙箱不可用"),
    TESTCASE_NOT_FOUND      (40005, "测试用例不存在"),

    // === 5xxxx MALL ===
    STOCK_NOT_ENOUGH        (50001, "库存不足"),
    IDEMPOTENT_REJECT       (50002, "请勿重复提交"),
    ORDER_STATUS_ILLEGAL    (50003, "订单状态不允许此操作"),
    ORDER_PAID_TIMEOUT      (50004, "订单已超时，请重新下单"),
    PRODUCT_OFF_SHELF       (50005, "商品已下架"),
    REFUND_REJECTED         (50006, "退款审核未通过"),
    SECKILL_NOT_STARTED     (50007, "活动未开始"),
    SECKILL_FINISHED        (50008, "活动已结束"),
    SECKILL_LIMIT_EXCEEDED  (50009, "已超过限购数量"),
    ADDRESS_LIMIT_EXCEEDED  (50010, "收货地址数量超过上限"),

    // === 6xxxx AI ===
    DOC_PROCESSING_FAILED   (60001, "文档处理失败"),
    SENSITIVE_CONTENT       (60002, "问题包含敏感内容"),
    LLM_UNAVAILABLE         (60003, "AI助手暂不可用"),
    DOC_NOT_FOUND           (60004, "未在知识库中找到相关文档"),
    QUOTA_INSUFFICIENT      (60005, "AI 额度不足，请联系管理员充值或切换自有模型");

    private final Integer code;
    private final String msg;
}
