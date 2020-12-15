/*
 * Copyright (c) 2020 qbwu, Inc All Rights Reserved
 *
 * Author: qb.wu@outlook.com
 * Date: 2020/7/24 17:53
 */

package com.xxxxx.xxxxxxxx.project.model.data;

import com.xxxxx.xxxxxxxx.project.model.enums.TemplateType;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Tolerate;

@Data
@Builder
public class TemplatableVisitor {
    public enum VisitType {
        UPDATE, VIEW
    }

    @Tolerate
    public TemplatableVisitor() {

    }

    private String id;
    private User user;
    private TemplateType objectType;
    private VisitType visitType;
}
