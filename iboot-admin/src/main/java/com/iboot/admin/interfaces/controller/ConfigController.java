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

package com.iboot.admin.interfaces.controller;

import com.iboot.admin.application.service.ConfigApplicationService;
import com.iboot.kernel.logging.Log;
import com.iboot.kernel.logging.BusinessTypeEnum;
import com.iboot.kernel.web.PageResult;
import com.iboot.kernel.web.Result;
import com.iboot.admin.common.util.ExcelExportUtil;
import com.iboot.admin.domain.system.model.Config;
import com.iboot.admin.interfaces.dto.export.ConfigExportVO;
import com.iboot.admin.interfaces.dto.request.ConfigRequest;
import com.iboot.admin.interfaces.dto.response.ConfigResponse;
import com.iboot.admin.interfaces.dto.response.PublicConfigResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 参数配置控制器。
 *
 * <p>提供系统参数的公开配置、分页查询、详情、增删改、缓存刷新和导出接口；
 * 管理接口对应 {@code config:*} 权限，公开配置接口允许匿名访问。
 *
 * @author iBoot Team
 * @since 1.0.0
 * @see ConfigApplicationService
 */
@Tag(name = "参数配置", description = "参数配置相关接口")
@RestController
@RequestMapping("/api/config")
public class ConfigController {

    private static final String CONFIG_KEY_SYSTEM_NAME = "sys.name";

    private static final String CONFIG_KEY_REGISTER_USER = "sys.account.registerUser";

    private static final String DEFAULT_SYSTEM_NAME = "iBoot 后台管理系统";

    private final ConfigApplicationService configApplicationService;

    @SuppressWarnings("all")
    public ConfigController(final ConfigApplicationService configApplicationService) {
        this.configApplicationService = configApplicationService;
    }

    /**
     * 获取公开系统配置。
     *
     * @return 系统名称和注册开关；未配置系统名称时使用默认值
     */
    @Operation(summary = "获取公开系统配置", description = "获取系统名称、注册开关等公开配置，无需登录")
    @GetMapping(version = "1", value = "/public")
    public Result<PublicConfigResponse> getPublicConfig() {
        String systemName = configApplicationService.getConfigValue(CONFIG_KEY_SYSTEM_NAME);
        String registerEnabled = configApplicationService.getConfigValue(CONFIG_KEY_REGISTER_USER);
        return Result.success(PublicConfigResponse.builder()
                .systemName(systemName != null ? systemName : DEFAULT_SYSTEM_NAME)
                .registerEnabled("true".equalsIgnoreCase(registerEnabled))
                .build());
    }

    /**
     * 分页查询参数配置。
     *
     * <p>任一带条件参数非空时走条件查询，否则查询全部配置。
     *
     * @param pageNum 页码，从 1 开始
     * @param pageSize 每页数量
     * @param configName 配置名称关键字，可为 {@code null}
     * @param configKey 配置键关键字，可为 {@code null}
     * @param configType 配置类型：0-默认，1-系统内置；可为 {@code null}
     * @return 配置分页结果
     */
    @Operation(summary = "查询参数配置列表")
    @GetMapping(version = "1", value = "/list")
    @PreAuthorize("hasAuthority('config:list')")
    public Result<PageResult<ConfigResponse>> list(@RequestParam(defaultValue = "1") Integer pageNum,
                                                   @RequestParam(defaultValue = "10") Integer pageSize,
                                                   @RequestParam(required = false) String configName,
                                                   @RequestParam(required = false) String configKey,
                                                   @RequestParam(required = false) Integer configType) {
        List<Config> configs;
        long total;
        if (configName != null || configKey != null || configType != null) {
            configs = configApplicationService.getConfigPageByCondition(configName, configKey, configType, pageNum,
                    pageSize);
            total = configApplicationService.countConfigsByCondition(configName, configKey, configType);
        } else {
            configs = configApplicationService.getConfigPage(pageNum, pageSize);
            total = configApplicationService.countConfigs();
        }
        List<ConfigResponse> responses = configs.stream().map(this::convertToResponse).collect(Collectors.toList());
        PageResult<ConfigResponse> pageResult = new PageResult<>(responses, total, pageNum, pageSize);
        return Result.success(pageResult);
    }

    /**
     * 查询所有参数配置。
     *
     * @return 配置列表；没有数据时返回空列表
     */
    @Operation(summary = "查询所有参数配置")
    @GetMapping(version = "1", value = "/all")
    @PreAuthorize("hasAuthority('config:list')")
    public Result<List<ConfigResponse>> all() {
        List<Config> configs = configApplicationService.getAllConfigs();
        List<ConfigResponse> responses = configs.stream().map(this::convertToResponse).collect(Collectors.toList());
        return Result.success(responses);
    }

    /**
     * 查询参数配置详情。
     *
     * @param id 配置 ID
     * @return 配置详情
     * @throws com.iboot.kernel.web.BusinessException 配置不存在时抛出
     */
    @Operation(summary = "查询参数配置详情")
    @GetMapping(version = "1", value = "/{id}")
    @PreAuthorize("hasAuthority('config:query')")
    public Result<ConfigResponse> getById(@PathVariable Long id) {
        Config config = configApplicationService.getConfigById(id);
        return Result.success(convertToResponse(config));
    }

    /**
     * 根据配置键查询配置值。
     *
     * @param configKey 配置键
     * @return 配置值；配置不存在时为 {@code null}
     */
    @Operation(summary = "根据配置键查询配置值")
    @GetMapping(version = "1", value = "/key/{configKey}")
    public Result<String> getByKey(@PathVariable String configKey) {
        String value = configApplicationService.getConfigValue(configKey);
        return Result.success(value);
    }

    /**
     * 新增参数配置。
     *
     * @param request 配置创建请求
     * @return 创建后的配置详情
     */
    @Operation(summary = "新增参数配置")
    @PostMapping
    @PreAuthorize("hasAuthority('config:add')")
    @Log(title = "参数配置", businessType = BusinessTypeEnum.INSERT)
    public Result<ConfigResponse> add(@Valid @RequestBody ConfigRequest request) {
        Config config = convertToEntity(request);
        Config createdConfig = configApplicationService.createConfig(config);
        return Result.success(convertToResponse(createdConfig));
    }

    /**
     * 修改参数配置。
     *
     * @param request 配置更新请求，{@code id} 不能为空
     * @return 无数据的成功响应
     */
    @Operation(summary = "修改参数配置")
    @PutMapping
    @PreAuthorize("hasAuthority('config:edit')")
    @Log(title = "参数配置", businessType = BusinessTypeEnum.UPDATE)
    public Result<Void> update(@Valid @RequestBody ConfigRequest request) {
        Config config = convertToEntity(request);
        config.setId(request.getId());
        configApplicationService.updateConfig(config);
        return Result.success();
    }

    /**
     * 删除参数配置。
     *
     * @param id 配置 ID
     * @return 无数据的成功响应
     */
    @Operation(summary = "删除参数配置")
    @DeleteMapping(version = "1", value = "/{id}")
    @PreAuthorize("hasAuthority('config:remove')")
    @Log(title = "参数配置", businessType = BusinessTypeEnum.DELETE)
    public Result<Void> delete(@PathVariable Long id) {
        configApplicationService.deleteConfig(id);
        return Result.success();
    }

    /**
     * 刷新配置缓存。
     *
     * <p>清除本地和 Redis 中的配置缓存，下次读取时重新加载。
     *
     * @return 无数据的成功响应
     */
    @Operation(summary = "刷新配置缓存")
    @DeleteMapping(version = "1", value = "/refreshCache")
    @PreAuthorize("hasAuthority('config:remove')")
    @Log(title = "参数配置", businessType = BusinessTypeEnum.CLEAN)
    public Result<Void> refreshCache() {
        configApplicationService.refreshCache();
        return Result.success();
    }

    /**
     * 导出配置列表。
     *
     * @param response HTTP 响应，用于写入 Excel 文件
     * @param configName 配置名称关键字，可为 {@code null}
     * @param configKey 配置键关键字，可为 {@code null}
     * @param configType 配置类型：0-默认，1-系统内置；可为 {@code null}
     * @throws IOException 写入响应流失败时抛出
     */
    @Operation(summary = "导出配置列表")
    @GetMapping(version = "1", value = "/export")
    @PreAuthorize("hasAuthority('config:export')")
    @Log(title = "参数配置", businessType = BusinessTypeEnum.EXPORT)
    public void export(HttpServletResponse response, @RequestParam(required = false) String configName,
                       @RequestParam(required = false) String configKey,
                       @RequestParam(required = false) Integer configType)
            throws IOException {
        List<Config> configs;
        if (configName != null || configKey != null || configType != null) {
            configs = configApplicationService.getAllConfigsByCondition(configName, configKey, configType);
        } else {
            configs = configApplicationService.getAllConfigs();
        }
        List<ConfigExportVO> exportList = configs.stream().map(this::convertToExportVO).collect(Collectors.toList());
        ExcelExportUtil.exportExcel(response, exportList, ConfigExportVO.class, "配置列表", "配置数据");
    }

    private ConfigExportVO convertToExportVO(Config config) {
        return ConfigExportVO.builder()
                .id(config.getId())
                .configName(config.getConfigName())
                .configKey(config.getConfigKey())
                .configValue(config.getConfigValue())
                .configType(config.getConfigType())
                .remark(config.getRemark())
                .createTime(config.getCreateTime())
                .build();
    }

    private Config convertToEntity(ConfigRequest request) {
        return Config.builder()
                .configName(request.getConfigName())
                .configKey(request.getConfigKey())
                .configValue(request.getConfigValue())
                .configType(request.getConfigType())
                .remark(request.getRemark())
                .build();
    }

    private ConfigResponse convertToResponse(Config config) {
        return ConfigResponse.builder()
                .id(config.getId())
                .configName(config.getConfigName())
                .configKey(config.getConfigKey())
                .configValue(config.getConfigValue())
                .configType(config.getConfigType())
                .remark(config.getRemark())
                .createTime(config.getCreateTime())
                .build();
    }

}
