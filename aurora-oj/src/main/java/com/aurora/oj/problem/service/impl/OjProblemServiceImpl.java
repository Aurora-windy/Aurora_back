package com.aurora.oj.problem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.aurora.common.exception.BizException;
import com.aurora.common.response.BizCode;
import com.aurora.common.response.PageResult;
import com.aurora.oj.enums.DifficultyEnum;
import com.aurora.oj.problem.entity.OjProblemDO;
import com.aurora.oj.problem.mapper.OjProblemMapper;
import com.aurora.oj.problem.model.req.ProblemPageReq;
import com.aurora.oj.problem.model.req.ProblemSaveReq;
import com.aurora.oj.problem.model.resp.ProblemResp;
import com.aurora.oj.problem.service.OjProblemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OjProblemServiceImpl implements OjProblemService {

    private static final int STATUS_ENABLED = 1;

    private final OjProblemMapper problemMapper;

    @Override
    public PageResult<ProblemResp> page(ProblemPageReq req) {
        return pageBy(req, false);
    }

    @Override
    public PageResult<ProblemResp> available(ProblemPageReq req) {
        return pageBy(req, true);
    }

    @Override
    public ProblemResp detail(Long id) {
        return toResp(requireProblem(id));
    }

    @Override
    public ProblemResp availableDetail(Long id) {
        OjProblemDO problem = requireProblem(id);
        if (problem.getStatus() == null || problem.getStatus() != STATUS_ENABLED) {
            throw new BizException(BizCode.DATA_NOT_FOUND);
        }
        return toResp(problem);
    }

    @Override
    public Long add(ProblemSaveReq req) {
        OjProblemDO problem = new OjProblemDO();
        fillProblem(problem, req);
        problemMapper.insert(problem);
        return problem.getId();
    }

    @Override
    public void update(Long id, ProblemSaveReq req) {
        requireProblem(id);
        OjProblemDO problem = new OjProblemDO();
        problem.setId(id);
        fillProblem(problem, req);
        problemMapper.updateById(problem);
    }

    @Override
    public void delete(Long id) {
        requireProblem(id);
        problemMapper.deleteById(id);
    }

    private PageResult<ProblemResp> pageBy(ProblemPageReq req, boolean availableOnly) {
        LambdaQueryWrapper<OjProblemDO> wrapper = Wrappers.<OjProblemDO>lambdaQuery()
                .like(StringUtils.hasText(req.getTitle()), OjProblemDO::getTitle, req.getTitle())
                .eq(req.getDifficulty() != null, OjProblemDO::getDifficulty, req.getDifficulty())
                .eq(req.getStatus() != null, OjProblemDO::getStatus, req.getStatus())
                .eq(availableOnly, OjProblemDO::getStatus, STATUS_ENABLED)
                .orderByDesc(OjProblemDO::getCreateTime);
        Page<OjProblemDO> page = problemMapper.selectPage(new Page<>(req.normalizedPageNum(), req.normalizedPageSize()), wrapper);
        List<ProblemResp> list = page.getRecords().stream().map(this::toResp).toList();
        return new PageResult<>(list, page.getTotal());
    }

    private void fillProblem(OjProblemDO problem, ProblemSaveReq req) {
        if (DifficultyEnum.fromCode(req.getDifficulty()) == null || (req.getStatus() != 0 && req.getStatus() != 1)) {
            throw new BizException(BizCode.PARAM_ERROR);
        }
        problem.setTitle(req.getTitle());
        problem.setDescription(req.getDescription());
        problem.setDifficulty(req.getDifficulty());
        problem.setTimeLimitMs(req.getTimeLimitMs());
        problem.setMemoryLimitMb(req.getMemoryLimitMb());
        problem.setSampleInput(req.getSampleInput());
        problem.setSampleOutput(req.getSampleOutput());
        problem.setTestInput(req.getTestInput());
        problem.setExpectedOutput(req.getExpectedOutput());
        problem.setStatus(req.getStatus());
    }

    private OjProblemDO requireProblem(Long id) {
        OjProblemDO problem = problemMapper.selectById(id);
        if (problem == null) {
            throw new BizException(BizCode.DATA_NOT_FOUND);
        }
        return problem;
    }

    private ProblemResp toResp(OjProblemDO problem) {
        return ProblemResp.builder()
                .id(problem.getId())
                .title(problem.getTitle())
                .description(problem.getDescription())
                .difficulty(problem.getDifficulty())
                .timeLimitMs(problem.getTimeLimitMs())
                .memoryLimitMb(problem.getMemoryLimitMb())
                .sampleInput(problem.getSampleInput())
                .sampleOutput(problem.getSampleOutput())
                .testInput(problem.getTestInput())
                .expectedOutput(problem.getExpectedOutput())
                .status(problem.getStatus())
                .createTime(problem.getCreateTime())
                .build();
    }
}
