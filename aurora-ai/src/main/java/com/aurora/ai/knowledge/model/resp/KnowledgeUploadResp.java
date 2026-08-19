package com.aurora.ai.knowledge.model.resp;

/**
 * 知识文档上传响应。
 *
 * @param docId     文档 ID
 * @param fileName  文件名
 * @param fileUrl   原文件访问地址（已保存到本地服务器，可下载/预览）
 * @param chars     提取的文本字符数
 * @param published 是否已自动发布（embedding 未配置时可能发布失败，转为草稿）
 * @param message   提示信息
 */
public record KnowledgeUploadResp(Long docId, String fileName, String fileUrl, int chars, boolean published, String message) {
}
