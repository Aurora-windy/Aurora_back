package com.aurora.edu.selection.service;

import com.aurora.edu.selection.model.resp.SelectionResp;

import java.util.List;

public interface EduSelectionService {
    List<SelectionResp> mySelections();
    Long selectCourse(Long courseId);
    void dropCourse(Long courseId);
}
