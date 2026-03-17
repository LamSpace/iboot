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

import com.iboot.admin.application.service.DeptApplicationService;
import com.iboot.admin.common.annotation.Log;
import com.iboot.admin.common.enums.BusinessTypeEnum;
import com.iboot.admin.common.result.Result;
import com.iboot.admin.common.util.ExcelExportUtil;
import com.iboot.admin.domain.system.model.Dept;
import com.iboot.admin.interfaces.dto.export.DeptExportVO;
import com.iboot.admin.interfaces.dto.request.DeptRequest;
import com.iboot.admin.interfaces.dto.response.DeptResponse;
import com.iboot.admin.interfaces.dto.response.OrgChartResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 部门管理控制器
 *
 * @author iBoot
 */
@Tag(name = "部门管理", description = "部门管理相关接口")
@RestController
@RequestMapping("/api/dept")
public class DeptController {

    private final DeptApplicationService deptApplicationService;

    @SuppressWarnings("all")
    public DeptController(final DeptApplicationService deptApplicationService) {
        this.deptApplicationService = deptApplicationService;
    }

    @Operation(summary = "查询部门树形结构")
    @GetMapping("/tree")
    @PreAuthorize("hasAuthority('dept:list')")
    public Result<List<DeptResponse>> tree() {
        List<Dept> depts = deptApplicationService.getDeptTree();
        List<DeptResponse> responses = depts.stream().map(this::convertToTreeResponse).collect(Collectors.toList());
        return Result.success(responses);
    }

    @Operation(summary = "查询组织架构图")
    @GetMapping("/orgChart")
    @PreAuthorize("hasAuthority('dept:list')")
    public Result<List<OrgChartResponse>> orgChart() {
        List<Dept> depts = deptApplicationService.getOrgChart();
        List<OrgChartResponse> responses = depts.stream()
                .map(this::convertToOrgChartResponse)
                .collect(Collectors.toList());
        return Result.success(responses);
    }

    @Operation(summary = "查询部门列表")
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('dept:list')")
    public Result<List<DeptResponse>> list() {
        List<Dept> depts = deptApplicationService.getAllDepts();
        List<DeptResponse> responses = depts.stream().map(this::convertToResponse).collect(Collectors.toList());
        return Result.success(responses);
    }

    @Operation(summary = "查询部门详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('dept:query')")
    public Result<DeptResponse> getById(@PathVariable Long id) {
        Dept dept = deptApplicationService.getDeptById(id);
        return Result.success(convertToResponse(dept));
    }

    @Operation(summary = "新增部门")
    @PostMapping
    @PreAuthorize("hasAuthority('dept:add')")
    @Log(title = "部门管理", businessType = BusinessTypeEnum.INSERT)
    public Result<DeptResponse> add(@Valid @RequestBody DeptRequest request) {
        Dept dept = convertToEntity(request);
        Dept createdDept = deptApplicationService.createDept(dept);
        return Result.success(convertToResponse(createdDept));
    }

    @Operation(summary = "修改部门")
    @PutMapping
    @PreAuthorize("hasAuthority('dept:edit')")
    @Log(title = "部门管理", businessType = BusinessTypeEnum.UPDATE)
    public Result<Void> update(@Valid @RequestBody DeptRequest request) {
        Dept dept = convertToEntity(request);
        dept.setId(request.getId());
        deptApplicationService.updateDept(dept);
        return Result.success();
    }

    @Operation(summary = "删除部门")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('dept:remove')")
    @Log(title = "部门管理", businessType = BusinessTypeEnum.DELETE)
    public Result<Void> delete(@PathVariable Long id) {
        deptApplicationService.deleteDept(id);
        return Result.success();
    }

    @Operation(summary = "导出部门列表")
    @GetMapping("/export")
    @PreAuthorize("hasAuthority('dept:export')")
    @Log(title = "部门管理", businessType = BusinessTypeEnum.EXPORT)
    public void export(HttpServletResponse response, @RequestParam(required = false) String deptName,
                       @RequestParam(required = false) Integer status) throws IOException {
        List<Dept> depts = deptApplicationService.getDeptTree();
        List<DeptExportVO> exportList = new ArrayList<>();
        flattenDeptTree(depts, exportList, 1, deptName, status);
        ExcelExportUtil.exportExcel(response, exportList, DeptExportVO.class, "部门列表", "部门数据");
    }

    /**
     * 递归展平部门树并添加层级信息
     */
    private void flattenDeptTree(List<Dept> depts, List<DeptExportVO> exportList, int level, String deptName,
                                 Integer status) {
        for (Dept dept : depts) {
            boolean nameMatch = deptName == null || deptName.isEmpty()
                    || (dept.getDeptName() != null && dept.getDeptName().contains(deptName));
            boolean statusMatch = status == null || status.equals(dept.getStatus());
            if (nameMatch && statusMatch) {
                exportList.add(convertToExportVO(dept, level));
            }
            if (dept.getChildren() != null && !dept.getChildren().isEmpty()) {
                flattenDeptTree(dept.getChildren(), exportList, level + 1, deptName, status);
            }
        }
    }

    private DeptExportVO convertToExportVO(Dept dept, int level) {
        return DeptExportVO.builder()
                .id(dept.getId())
                .level(level)
                .deptCode(dept.getDeptCode())
                .deptName(dept.getDeptName())
                .leader(dept.getLeader())
                .phone(dept.getPhone())
                .email(dept.getEmail())
                .orderNum(dept.getOrderNum())
                .status(dept.getStatus())
                .createTime(dept.getCreateTime())
                .build();
    }

    private Dept convertToEntity(DeptRequest request) {
        return Dept.builder()
                .parentId(request.getParentId())
                .deptCode(request.getDeptCode())
                .deptName(request.getDeptName())
                .orderNum(request.getOrderNum())
                .leader(request.getLeader())
                .phone(request.getPhone())
                .email(request.getEmail())
                .status(request.getStatus())
                .build();
    }

    private DeptResponse convertToResponse(Dept dept) {
        return DeptResponse.builder()
                .id(dept.getId())
                .parentId(dept.getParentId())
                .deptCode(dept.getDeptCode())
                .deptName(dept.getDeptName())
                .orderNum(dept.getOrderNum())
                .leader(dept.getLeader())
                .phone(dept.getPhone())
                .email(dept.getEmail())
                .status(dept.getStatus())
                .createTime(dept.getCreateTime())
                .build();
    }

    private DeptResponse convertToTreeResponse(Dept dept) {
        DeptResponse response = convertToResponse(dept);
        if (dept.getChildren() != null && !dept.getChildren().isEmpty()) {
            response
                    .setChildren(dept.getChildren().stream().map(this::convertToTreeResponse).collect(Collectors.toList()));
        }
        return response;
    }

    private OrgChartResponse convertToOrgChartResponse(Dept dept) {
        // createBy 字段临时存储了成员数量
        int memberCount = 0;
        if (dept.getCreateBy() != null) {
            try {
                memberCount = Integer.parseInt(dept.getCreateBy());
            } catch (NumberFormatException ignored) {
            }
        }
        OrgChartResponse response = OrgChartResponse.builder()
                .id(dept.getId())
                .deptName(dept.getDeptName())
                .deptCode(dept.getDeptCode())
                .leader(dept.getLeader())
                .phone(dept.getPhone())
                .email(dept.getEmail())
                .status(dept.getStatus())
                .memberCount(memberCount)
                .build();
        if (dept.getChildren() != null && !dept.getChildren().isEmpty()) {
            response.setChildren(
                    dept.getChildren().stream().map(this::convertToOrgChartResponse).collect(Collectors.toList()));
        }
        return response;
    }

}
