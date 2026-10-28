/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.iboot.ai.infrastructure.graph;

import com.iboot.ai.application.service.graph.GraphSyncException;
import com.iboot.ai.domain.graph.DeptNode;
import com.iboot.ai.domain.graph.GraphSnapshot;
import com.iboot.ai.domain.graph.MenuNode;
import com.iboot.ai.domain.graph.RoleNode;
import com.iboot.ai.domain.graph.UserNode;
import com.iboot.ai.infrastructure.config.GraphProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 以调用者身份从 admin 拉取组织快照。
 *
 * <p>复用 admin 既有的部门、菜单、角色、用户只读接口，不直连 admin 数据库；
 * 权限校验全部由 admin 侧完成，本类只负责解析与翻页。
 * 任一接口失败、响应非法或超过最大翻页数时抛出 {@link GraphSyncException}。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
@Component
@ConditionalOnProperty(prefix = "iboot.ai.graph", name = "enabled", havingValue = "true")
public class AdminOrgSnapshotClient {

    private static final String BEARER_PREFIX = "Bearer ";

    private static final String VERSION_HEADER = "Accept-Version";

    private static final String VERSION = "1";

    private final RestClient restClient;

    private final GraphProperties properties;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    /**
     * 创建客户端。
     *
     * @param restClient 指向 admin 的 REST 客户端
     * @param properties 知识图谱配置
     */
    public AdminOrgSnapshotClient(@Qualifier("adminRestClient") RestClient restClient,
                                  GraphProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    /**
     * 拉取一次完整组织快照。
     *
     * @param token 调用者登录令牌
     * @return 完整快照
     * @throws GraphSyncException 下游失败、响应非法或超过最大翻页数时抛出
     */
    public GraphSnapshot fetch(String token) {
        List<DeptNode> departments = parseDepartments(payload(get(token, "/api/dept/list")));
        List<MenuNode> menus = parseMenus(payload(get(token, "/api/menu/list")));
        List<RoleNode> roles = fetchRoles(token);
        List<UserNode> users = fetchUsers(token);
        return new GraphSnapshot(UUID.randomUUID().toString(), departments, users, roles, menus);
    }

    private List<RoleNode> fetchRoles(String token) {
        List<JsonNode> pages = fetchAllPages(token, "/api/role/list");
        List<RoleNode> roles = new ArrayList<>();
        for (JsonNode role : pages) {
            long id = requiredLong(role, "id");
            JsonNode detail = payload(get(token, "/api/role/" + id)).path("data");
            roles.add(new RoleNode(id, text(role, "roleCode"), text(role, "roleName"),
                    integer(role, "dataScope"), integer(role, "status"),
                    longList(detail, "menuIds"), longList(detail, "deptIds")));
        }
        return roles;
    }

    private List<UserNode> fetchUsers(String token) {
        List<UserNode> users = new ArrayList<>();
        for (JsonNode user : fetchAllPages(token, "/api/user/list")) {
            users.add(new UserNode(requiredLong(user, "id"), text(user, "username"), text(user, "nickname"),
                    longOrNull(user, "deptId"), integer(user, "status"), longList(user, "roleIds")));
        }
        return users;
    }

    private List<JsonNode> fetchAllPages(String token, String path) {
        List<JsonNode> items = new ArrayList<>();
        int pageNum = 1;
        int total = Integer.MAX_VALUE;
        while (items.size() < total) {
            if (pageNum > properties.getSync().getMaxPages()) {
                throw new GraphSyncException("组织数据超过最大翻页数，请调大 iboot.ai.graph.sync.max-pages");
            }
            JsonNode payload = payload(get(token, path, pageNum));
            JsonNode result = payload.path("data");
            JsonNode rows = result.isArray() ? result : result.path("data");
            if (!rows.isArray()) {
                throw new GraphSyncException("组织数据响应缺少列表：" + path);
            }
            rows.forEach(items::add);
            total = result.isArray() ? payload.path("total").asInt(0) : result.path("total").asInt(0);
            if (rows.isEmpty()) {
                break;
            }
            pageNum++;
        }
        return items;
    }

    private List<DeptNode> parseDepartments(JsonNode payload) {
        JsonNode data = requireArray(payload, "/api/dept/list");
        List<DeptNode> departments = new ArrayList<>();
        for (JsonNode dept : data) {
            departments.add(new DeptNode(requiredLong(dept, "id"), text(dept, "deptName"),
                    text(dept, "deptCode"), longOrNull(dept, "parentId"), integer(dept, "status")));
        }
        return departments;
    }

    private List<MenuNode> parseMenus(JsonNode payload) {
        JsonNode data = requireArray(payload, "/api/menu/list");
        List<MenuNode> menus = new ArrayList<>();
        for (JsonNode menu : data) {
            menus.add(new MenuNode(requiredLong(menu, "id"), text(menu, "menuName"), text(menu, "menuType"),
                    text(menu, "perms"), longOrNull(menu, "parentId"), integer(menu, "status")));
        }
        return menus;
    }

    private JsonNode requireArray(JsonNode payload, String path) {
        JsonNode data = payload.path("data");
        if (!data.isArray()) {
            throw new GraphSyncException("组织数据响应缺少列表：" + path);
        }
        return data;
    }

    private String get(String token, String path) {
        return get(token, path, null);
    }

    private String get(String token, String path, Integer pageNum) {
        try {
            return restClient.get()
                    .uri(builder -> {
                        builder.path(path);
                        if (pageNum != null) {
                            builder.queryParam("pageNum", pageNum)
                                    .queryParam("pageSize", properties.getSync().getPageSize());
                        }
                        return builder.build();
                    })
                    .header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + token)
                    .header(VERSION_HEADER, VERSION)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException e) {
            throw new GraphSyncException(describe(e), e);
        } catch (RuntimeException e) {
            throw new GraphSyncException("admin 组织数据不可用", e);
        }
    }

    private String describe(RestClientResponseException exception) {
        int status = exception.getStatusCode().value();
        if (status == 403) {
            return "无权读取组织数据，缺少 dept:list / user:list / role:list / menu:list 权限";
        }
        if (status == 401) {
            return "登录态已失效，无法同步组织数据";
        }
        return "拉取组织数据失败：HTTP " + status;
    }

    private JsonNode payload(String body) {
        if (body == null || body.isBlank()) {
            throw new GraphSyncException("组织数据响应为空");
        }
        try {
            JsonNode result = jsonMapper.readTree(body).path("data");
            if (!result.isObject()) {
                throw new GraphSyncException("组织数据响应缺少 data");
            }
            int code = result.path("code").asInt(-1);
            if (code != 200) {
                throw new GraphSyncException("组织数据响应返回错误码：" + code);
            }
            return result;
        } catch (GraphSyncException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new GraphSyncException("组织数据响应解析失败", e);
        }
    }

    private long requiredLong(JsonNode node, String field) {
        Long value = longOrNull(node, field);
        if (value == null) {
            throw new GraphSyncException("组织数据缺少字段：" + field);
        }
        return value;
    }

    private Long longOrNull(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asLong();
    }

    private Integer integer(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asInt();
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asString();
    }

    private List<Long> longList(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (!value.isArray()) {
            return List.of();
        }
        List<Long> result = new ArrayList<>();
        value.forEach(item -> result.add(item.asLong()));
        return result;
    }
}
