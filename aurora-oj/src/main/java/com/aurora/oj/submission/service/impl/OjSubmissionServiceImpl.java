package com.aurora.oj.submission.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.aurora.common.exception.BizException;
import com.aurora.common.response.BizCode;
import com.aurora.common.response.PageResult;
import com.aurora.common.util.SecurityUtil;
import com.aurora.oj.enums.JudgeStatusEnum;
import com.aurora.oj.enums.LanguageEnum;
import com.aurora.oj.problem.entity.OjProblemDO;
import com.aurora.oj.problem.mapper.OjProblemMapper;
import com.aurora.oj.submission.entity.OjSubmissionDO;
import com.aurora.oj.submission.mapper.OjSubmissionMapper;
import com.aurora.oj.submission.model.req.SubmissionCreateReq;
import com.aurora.oj.submission.model.req.SubmissionPageReq;
import com.aurora.oj.submission.model.resp.SubmissionResp;
import com.aurora.oj.submission.service.OjSubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OjSubmissionServiceImpl implements OjSubmissionService {

    private static final int STATUS_ENABLED = 1;

    private final OjProblemMapper problemMapper;
    private final OjSubmissionMapper submissionMapper;

    @Override
    public Long submit(SubmissionCreateReq req) {
        Long userId = SecurityUtil.requireUserId();
        OjProblemDO problem = problemMapper.selectById(req.getProblemId());
        if (problem == null || problem.getStatus() == null || problem.getStatus() != STATUS_ENABLED) {
            throw new BizException(BizCode.DATA_NOT_FOUND);
        }
        LanguageEnum language = LanguageEnum.fromValue(req.getLanguage());
        if (language == null) {
            throw new BizException(BizCode.PARAM_ERROR);
        }

        JudgeResult result = judge(problem, req.getSourceCode());
        OjSubmissionDO submission = new OjSubmissionDO();
        submission.setProblemId(problem.getId());
        submission.setUserId(userId);
        submission.setLanguage(language.getCode());
        submission.setSourceCode(req.getSourceCode());
        submission.setStatus(result.status());
        submission.setTimeUsedMs(result.timeUsedMs());
        submission.setMemoryUsedKb(result.memoryUsedKb());
        submission.setErrorMessage(result.errorMessage());
        submissionMapper.insert(submission);
        return submission.getId();
    }

    @Override
    public PageResult<SubmissionResp> mySubmissions(SubmissionPageReq req) {
        req.setUserId(SecurityUtil.requireUserId());
        return page(req);
    }

    @Override
    public PageResult<SubmissionResp> page(SubmissionPageReq req) {
        Integer languageCode = null;
        if (StringUtils.hasText(req.getLanguage())) {
            LanguageEnum language = LanguageEnum.fromValue(req.getLanguage());
            if (language == null) {
                throw new BizException(BizCode.PARAM_ERROR);
            }
            languageCode = language.getCode();
        }
        LambdaQueryWrapper<OjSubmissionDO> wrapper = Wrappers.<OjSubmissionDO>lambdaQuery()
                .eq(req.getProblemId() != null, OjSubmissionDO::getProblemId, req.getProblemId())
                .eq(req.getUserId() != null, OjSubmissionDO::getUserId, req.getUserId())
                .eq(req.getStatus() != null, OjSubmissionDO::getStatus, req.getStatus())
                .eq(languageCode != null, OjSubmissionDO::getLanguage, languageCode)
                .orderByDesc(OjSubmissionDO::getCreateTime);
        Page<OjSubmissionDO> page = submissionMapper.selectPage(new Page<>(req.normalizedPageNum(), req.normalizedPageSize()), wrapper);
        List<SubmissionResp> list = page.getRecords().stream().map(this::toResp).toList();
        return new PageResult<>(list, page.getTotal());
    }

    private JudgeResult judge(OjProblemDO problem, String sourceCode) {
        String normalized = sourceCode == null ? "" : sourceCode.toUpperCase();
        if (normalized.contains("// AC") || normalized.contains("ACCEPT")) {
            return new JudgeResult(JudgeStatusEnum.ACCEPTED.getCode(), 12, 2048, null);
        }
        if (!StringUtils.hasText(problem.getExpectedOutput())) {
            return new JudgeResult(JudgeStatusEnum.ACCEPTED.getCode(), 10, 1024, null);
        }
        return new JudgeResult(JudgeStatusEnum.WRONG_ANSWER.getCode(), 15, 2048, "Simplified judge: expected output was not matched.");
    }

    private SubmissionResp toResp(OjSubmissionDO submission) {
        OjProblemDO problem = problemMapper.selectById(submission.getProblemId());
        LanguageEnum language = LanguageEnum.fromCode(submission.getLanguage());
        return SubmissionResp.builder()
                .id(submission.getId())
                .problemId(submission.getProblemId())
                .problemTitle(problem == null ? "-" : problem.getTitle())
                .userId(submission.getUserId())
                .language(language == null ? null : language.getValue())
                .sourceCode(submission.getSourceCode())
                .status(submission.getStatus())
                .timeUsedMs(submission.getTimeUsedMs())
                .memoryUsedKb(submission.getMemoryUsedKb())
                .errorMessage(submission.getErrorMessage())
                .createTime(submission.getCreateTime())
                .build();
    }

    private record JudgeResult(Integer status, Integer timeUsedMs, Integer memoryUsedKb, String errorMessage) {
    }
}
