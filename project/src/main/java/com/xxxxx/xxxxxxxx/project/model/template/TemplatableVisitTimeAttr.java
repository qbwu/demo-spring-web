/*
 * Copyright (c) 2020 qbwu, Inc All Rights Reserved
 *
 * Author: qb.wu@outlook.com
 * Date: 2020/7/24 18:45
 */

package com.xxxxx.xxxxxxxx.project.model.template;

import com.xxxxx.xxxxxxxx.project.model.Protocol;
import com.xxxxx.xxxxxxxx.project.model.enums.TemplateType;
import com.xxxxx.xxxxxxxx.project.model.data.DateRange;
import com.xxxxx.xxxxxxxx.project.model.data.TemplatableVisitor;
import com.xxxxx.xxxxxxxx.project.model.data.User;
import com.xxxxx.xxxxxxxx.project.model.exception.StaticException;
import com.xxxxx.xxxxxxxx.project.utility.ObjectMapUtils;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class TemplatableVisitTimeAttr extends ExtValueTemplateAttr {

    private static final long serialVersionUID = 1L;

    private final TemplatableVisitor visitor = new TemplatableVisitor();
    private Long timestamp = 0L;

    public TemplatableVisitTimeAttr(TemplateType objectType,
                                    TemplatableVisitor.VisitType visitType) {
        this.visitor.setObjectType(objectType);
        this.visitor.setVisitType(visitType);
    }

    protected TemplatableVisitTimeAttr() {

    }

    @PostConstruct
    private void init() {
        setDeletable(false);

        setExtValueOptional(false);
        setOptionalSettable(false);
    }

    protected abstract Long getLatestTime(TemplatableVisitor visitor);

    protected abstract void updateLatestTime(TemplatableVisitor visitor, Long time);

    protected abstract Set<String> findObjectsVisitedInTime(
            TemplatableVisitor visitor, DateRange period, Set<String> limit);

    @Override
    public ExtValueTemplateAttr copy() throws StaticException {
        TemplatableVisitTimeAttr twin = (TemplatableVisitTimeAttr) super.copy();
        twin.timestamp = 0L;
        return twin;
    }

    @Override
    public final Object getExtValue() {
        return timestamp;
    }

    @Override
    public final void loadExtValue() {
        buildVisitor();
        Long latest = getLatestTime(visitor);
        if (latest != null) {
            timestamp = latest;
        } else {
            timestamp = 0L;
        }
    }

    @Override
    public final void saveExtValue() {
        Long curr = System.currentTimeMillis();
        updateLatestTime(visitor, curr);
    }

    @Override
    public final void onTemplatableSetup() {
        buildVisitor();
    }

    @Override
    public final Set<String> filterExtValue(Set<String> ids, Object value) {
        try {
            Set<String> limit = ids.size() > 1000 ? null : ids;
            DateRange period = ObjectMapUtils.mapToObject(
                    (Map<String, Object>) value, DateRange.class);
            Set<String> res = findObjectsVisitedInTime(visitor, period, limit);
            if (limit == null) {
                res.retainAll(ids);
            }
            return res;
        } catch (ClassCastException | IllegalAccessException | InstantiationException e) {
            errorValueTypeMismatched("filter", DateRange.class, value.getClass(), e);
        }
        return new HashSet<>();
    }

    @Override
    public void initSetExtValue(Object extVal) {
        timestamp = System.currentTimeMillis();
    }

    @Override
    public void setExtValue(Object extValue) {
        timestamp = System.currentTimeMillis();
    }

    @Override
    public final void updateExtValue(Object value) {
        timestamp = System.currentTimeMillis();
    }

    @Override
    public Object getExtValueRange() {
        return null;
    }

    @Override
    public void setExtValueRange(Object extValue) {
        errorNotAllowedTo("set range");
    }

    private void buildVisitor() {
        if (visitor.getUser() != null) {
            return;
        }
        visitor.setId(getTemplatable().getId());
        visitor.setUser(getTemplatable().getRequestContext().getCurrentUser());
    }
}
