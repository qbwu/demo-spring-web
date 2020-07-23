/*
 * Copyright (c) 2020 qbwu All Rights Reserved
 *
 * Author: qb.wu@outlook.com
 * Date: 2020/5/25 0:13
 */

package com.xxxxx.xxxxxxxx.project.models.data;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Tolerate;

@Data
@Builder
public class DateRange {
    private Number startTime;
    private Number endTime;

    @Tolerate
    public DateRange() {

    }

    public Boolean isEmpty() {
        return startTime == null && endTime == null;
    }

    public Boolean isIncomplete() {
        return (startTime == null) != (endTime == null);
    }

    // Must !isEmpty() && !isIncomplte()
    public Boolean isValidValue() {
        return startTime.longValue() > 0L && endTime.longValue() > 0L
                && startTime.longValue() < endTime.longValue();
    }

    public Long getLength() {
        return startTime == null || endTime == null ? null :
                endTime.longValue() - startTime.longValue();
    }
}
