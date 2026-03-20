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
import com.iboot.admin.common.annotation.Log;
import com.iboot.admin.common.enums.BusinessTypeEnum;
import com.iboot.admin.common.result.PageResult;
import com.iboot.admin.common.result.Result;
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
 * 参数配置控制器
 *
 * @author iBoot
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

    @Operation(summary = "查询所有参数配置")
    @GetMapping(version = "1", value = "/all")
    @PreAuthorize("hasAuthority('config:list')")
    public Result<List<ConfigResponse>> all() {
        List<Config> configs = configApplicationService.getAllConfigs();
        List<ConfigResponse> responses = configs.stream().map(this::convertToResponse).collect(Collectors.toList());
        return Result.success(responses);
    }

    @Operation(summary = "查询参数配置详情")
    @GetMapping(version = "1", value = "/{id}")
    @PreAuthorize("hasAuthority('config:query')")
    public Result<ConfigResponse> getById(@PathVariable Long id) {
        Config config = configApplicationService.getConfigById(id);
        return Result.success(convertToResponse(config));
    }

    @Operation(summary = "根据配置键查询配置值")
    @GetMapping(version = "1", value = "/key/{configKey}")
    public Result<String> getByKey(@PathVariable String configKey) {
        String value = configApplicationService.getConfigValue(configKey);
        return Result.success(value);
    }

    @Operation(summary = "新增参数配置")
    @PostMapping
    @PreAuthorize("hasAuthority('config:add')")
    @Log(title = "参数配置", businessType = BusinessTypeEnum.INSERT)
    public Result<ConfigResponse> add(@Valid @RequestBody ConfigRequest request) {
        Config config = convertToEntity(request);
        Config createdConfig = configApplicationService.createConfig(config);
        return Result.success(convertToResponse(createdConfig));
    }

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

    @Operation(summary = "删除参数配置")
    @DeleteMapping(version = "1", value = "/{id}")
    @PreAuthorize("hasAuthority('config:remove')")
    @Log(title = "参数配置", businessType = BusinessTypeEnum.DELETE)
    public Result<Void> delete(@PathVariable Long id) {
        configApplicationService.deleteConfig(id);
        return Result.success();
    }

    @Operation(summary = "刷新配置缓存")
    @DeleteMapping(version = "1", value = "/refreshCache")
    @PreAuthorize("hasAuthority('config:remove')")
    @Log(title = "参数配置", businessType = BusinessTypeEnum.CLEAN)
    public Result<Void> refreshCache() {
        configApplicationService.refreshCache();
        return Result.success();
    }

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
