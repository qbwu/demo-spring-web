/*
 * Copyright (c) 2020 qbwu, Inc All Rights Reserved
 *
 * Author: qb.wu@outlook.com
 * Date: 2020/5/25 20:54
 */

package com.xxxxx.xxxxxxxx.project.library.hi;

public class AuthInfo {
    private String corpId;
    private String imid;

    public AuthInfo(String corpId, String imid) {
        this.corpId = corpId;
        this.imid = imid;
    }

    public String getCorpId() {
        return corpId;
    }
    public String getImid() {
        return imid;
    }
}

