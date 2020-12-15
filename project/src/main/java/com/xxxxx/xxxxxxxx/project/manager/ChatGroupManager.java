/*
 * Copyright (c) 2020 qbwu, Inc All Rights Reserved
 *
 * Author: qb.wu@outlook.com
 * Date: 2020/8/7 14:43
 */

package com.xxxxx.xxxxxxxx.project.manager;

import com.xxxxx.eim.pb.open.OpenBasic;
import com.xxxxx.xxxxxxxx.project.model.data.ChatInfo;
import com.xxxxx.xxxxxxxx.project.model.data.User;
import com.xxxxx.xxxxxxxx.project.model.exception.BoundException;
import com.xxxxx.xxxxxxxx.serviceadapter.scagent.service.GroupSrvClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// No need to have the request-scoped cache, because there are not repeated group
// queries in one request.
@Service
public class ChatGroupManager {
    private static final Logger logger = LoggerFactory.getLogger(ChatGroupManager.class);

    @Autowired
    private GroupSrvClient groupSrvClient;
}
