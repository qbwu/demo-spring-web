/*
 * Copyright (c) 2020 qbwu, Inc All Rights Reserved
 *
 * Author: qb.wu@outlook.com
 * Date: 2020/5/23 14:48
 */

package com.xxxxx.xxxxxxxx.project.model.data;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Tolerate;

@Data
@Builder
public class User {
    private String userId;
    private String corpId;

    @Tolerate
    public User() {

    }

    public Boolean isEmpty() {
        return userId == null && corpId == null;
    }

    public Boolean isIncomplete() {
        return (userId == null) != (corpId == null);
    }
}

