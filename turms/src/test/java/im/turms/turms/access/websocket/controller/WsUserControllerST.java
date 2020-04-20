/*
 * Copyright (C) 2019 The Turms Project
 * https://github.com/turms-im/turms
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.turms.turms.access.websocket.controller;

import im.turms.common.constant.ProfileAccessStrategy;
import im.turms.turms.pojo.domain.User;
import im.turms.turms.util.TurmsPasswordUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.util.Date;

import static im.turms.turms.constant.Common.DEFAULT_USER_PERMISSION_GROUP_ID;

public class WsUserControllerST extends BaseController {
    @LocalServerPort
    Integer port;

    @BeforeAll
    public static void initUser(@Autowired MongoTemplate mongoTemplate, @Autowired TurmsPasswordUtil passwordUtil) {
        Date now = new Date();
        User user = new User(1L, passwordUtil.encodeUserPassword("123"), "", "",
                ProfileAccessStrategy.ALL, DEFAULT_USER_PERMISSION_GROUP_ID, now, null, true, now);
        mongoTemplate.save(user);
    }

    @AfterAll
    public static void tearDown(@Autowired MongoTemplate mongoTemplate) {
        mongoTemplate.remove(new Query(), User.class);
    }
}
