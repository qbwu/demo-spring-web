/*
 * Copyright (c) 2020 qbwu, Inc All Rights Reserved
 *
 * Author: qb.wu@outlook.com
 * Date: 2020/7/30 16:51
 */

package com.xxxxx.xxxxxxxx.project.model.template;

import com.xxxxx.xxxxxxxx.project.mapper.TemplatableVisitTimeMapper;
import com.xxxxx.xxxxxxxx.project.model.enums.TemplateType;
import com.xxxxx.xxxxxxxx.project.model.data.DateRange;
import com.xxxxx.xxxxxxxx.project.model.data.TemplatableVisitor;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashSet;
import java.util.Set;

public class TemplatableUpdateTimeAttr extends TemplatableVisitTimeAttr {

    private static final long serialVersionUID = 1L;

    @Autowired
    private TemplatableVisitTimeMapper dao;

    public TemplatableUpdateTimeAttr(TemplateType objectType) {
        super(objectType, TemplatableVisitor.VisitType.UPDATE);
    }

    private TemplatableUpdateTimeAttr() {

    }

    @Override
    protected final Long getLatestTime(TemplatableVisitor visitor) {
        return dao.getMaxVisitTime(dropUserOfVisitor(visitor));
    }

    @Override
    protected final void updateLatestTime(TemplatableVisitor visitor, Long time) {
        dao.putVisitTime(visitor, time);
    }

    @Override
    protected final Set<String> findObjectsVisitedInTime(
            TemplatableVisitor visitor, DateRange period, Set<String> limit) {
        return new HashSet<>(dao.findObjectsByMaxVisitTime(
                dropUserOfVisitor(visitor), period, limit));
    }

    // The latest update time of any members
    private TemplatableVisitor dropUserOfVisitor(TemplatableVisitor visitor) {
        return TemplatableVisitor.builder().id(visitor.getId())
                .objectType(visitor.getObjectType())
                .visitType(visitor.getVisitType())
                .build();
    }
}
