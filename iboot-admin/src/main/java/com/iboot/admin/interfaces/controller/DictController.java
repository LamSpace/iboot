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

import com.iboot.admin.application.service.DictApplicationService;
import com.iboot.admin.common.annotation.Log;
import com.iboot.admin.common.enums.BusinessTypeEnum;
import com.iboot.admin.common.result.PageResult;
import com.iboot.admin.common.result.Result;
import com.iboot.admin.common.util.ExcelExportUtil;
import com.iboot.admin.domain.system.model.DictData;
import com.iboot.admin.domain.system.model.DictType;
import com.iboot.admin.interfaces.dto.export.DictDataExportVO;
import com.iboot.admin.interfaces.dto.export.DictTypeExportVO;
import com.iboot.admin.interfaces.dto.request.DictDataRequest;
import com.iboot.admin.interfaces.dto.request.DictTypeRequest;
import com.iboot.admin.interfaces.dto.response.DictDataResponse;
import com.iboot.admin.interfaces.dto.response.DictTypeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 数据字典控制器
 * 
 * @author iBoot
 */
@Tag(name = "数据字典", description = "数据字典相关接口")
@RestController
@RequestMapping("/api/dict")
@RequiredArgsConstructor
public class DictController {
    
    private final DictApplicationService dictApplicationService;
    
    // ==================== 字典类型接口 ====================
    
    @Operation(summary = "查询字典类型列表")
    @GetMapping("/type/list")
    @PreAuthorize("hasAuthority('dict:list')")
    public Result<PageResult<DictTypeResponse>> typeList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        
        List<DictType> types = dictApplicationService.getDictTypePage(pageNum, pageSize);
        long total = dictApplicationService.countDictTypes();
        
        List<DictTypeResponse> responses = types.stream()
                .map(this::convertTypeToResponse)
                .collect(Collectors.toList());
        
        PageResult<DictTypeResponse> pageResult = new PageResult<>(responses, total, pageNum, pageSize);
        return Result.success(pageResult);
    }
    
    @Operation(summary = "查询所有字典类型")
    @GetMapping("/type/all")
    @PreAuthorize("hasAuthority('dict:list')")
    public Result<List<DictTypeResponse>> typeAll() {
        List<DictType> types = dictApplicationService.getAllDictTypes();
        List<DictTypeResponse> responses = types.stream()
                .map(this::convertTypeToResponse)
                .collect(Collectors.toList());
        return Result.success(responses);
    }
    
    @Operation(summary = "查询字典类型详情")
    @GetMapping("/type/{id}")
    @PreAuthorize("hasAuthority('dict:query')")
    public Result<DictTypeResponse> typeGetById(@PathVariable Long id) {
        DictType dictType = dictApplicationService.getDictTypeById(id);
        return Result.success(convertTypeToResponse(dictType));
    }
    
    @Operation(summary = "新增字典类型")
    @PostMapping("/type")
    @PreAuthorize("hasAuthority('dict:add')")
    @Log(title = "数据字典", businessType = BusinessTypeEnum.INSERT)
    public Result<DictTypeResponse> typeAdd(@Valid @RequestBody DictTypeRequest request) {
        DictType dictType = convertTypeToEntity(request);
        DictType createdType = dictApplicationService.createDictType(dictType);
        return Result.success(convertTypeToResponse(createdType));
    }
    
    @Operation(summary = "修改字典类型")
    @PutMapping("/type")
    @PreAuthorize("hasAuthority('dict:edit')")
    @Log(title = "数据字典", businessType = BusinessTypeEnum.UPDATE)
    public Result<Void> typeUpdate(@Valid @RequestBody DictTypeRequest request) {
        DictType dictType = convertTypeToEntity(request);
        dictType.setId(request.getId());
        dictApplicationService.updateDictType(dictType);
        return Result.success();
    }
    
    @Operation(summary = "删除字典类型")
    @DeleteMapping("/type/{id}")
    @PreAuthorize("hasAuthority('dict:remove')")
    @Log(title = "数据字典", businessType = BusinessTypeEnum.DELETE)
    public Result<Void> typeDelete(@PathVariable Long id) {
        dictApplicationService.deleteDictType(id);
        return Result.success();
    }
    
    @Operation(summary = "导出字典类型列表")
    @GetMapping("/type/export")
    @PreAuthorize("hasAuthority('dict:export')")
    @Log(title = "数据字典", businessType = BusinessTypeEnum.EXPORT)
    public void exportType(HttpServletResponse response) throws IOException {
        List<DictType> types = dictApplicationService.getAllDictTypes();
        List<DictTypeExportVO> exportList = types.stream()
                .map(this::convertTypeToExportVO)
                .collect(Collectors.toList());
        ExcelExportUtil.exportExcel(response, exportList, DictTypeExportVO.class, "字典类型", "字典类型数据");
    }
    
    // ==================== 字典数据接口 ====================
    
    @Operation(summary = "根据字典类型查询字典数据")
    @GetMapping("/data/type/{dictType}")
    public Result<List<DictDataResponse>> dataByType(@PathVariable String dictType) {
        List<DictData> dataList = dictApplicationService.getDictDataByType(dictType);
        List<DictDataResponse> responses = dataList.stream()
                .map(this::convertDataToResponse)
                .collect(Collectors.toList());
        return Result.success(responses);
    }
    
    @Operation(summary = "查询字典数据详情")
    @GetMapping("/data/{id}")
    @PreAuthorize("hasAuthority('dict:query')")
    public Result<DictDataResponse> dataGetById(@PathVariable Long id) {
        DictData dictData = dictApplicationService.getDictDataById(id);
        return Result.success(convertDataToResponse(dictData));
    }
    
    @Operation(summary = "新增字典数据")
    @PostMapping("/data")
    @PreAuthorize("hasAuthority('dict:add')")
    @Log(title = "数据字典", businessType = BusinessTypeEnum.INSERT)
    public Result<DictDataResponse> dataAdd(@Valid @RequestBody DictDataRequest request) {
        DictData dictData = convertDataToEntity(request);
        DictData createdData = dictApplicationService.createDictData(dictData);
        return Result.success(convertDataToResponse(createdData));
    }
    
    @Operation(summary = "修改字典数据")
    @PutMapping("/data")
    @PreAuthorize("hasAuthority('dict:edit')")
    @Log(title = "数据字典", businessType = BusinessTypeEnum.UPDATE)
    public Result<Void> dataUpdate(@Valid @RequestBody DictDataRequest request) {
        DictData dictData = convertDataToEntity(request);
        dictData.setId(request.getId());
        dictApplicationService.updateDictData(dictData);
        return Result.success();
    }
    
    @Operation(summary = "删除字典数据")
    @DeleteMapping("/data/{id}")
    @PreAuthorize("hasAuthority('dict:remove')")
    @Log(title = "数据字典", businessType = BusinessTypeEnum.DELETE)
    public Result<Void> dataDelete(@PathVariable Long id) {
        dictApplicationService.deleteDictData(id);
        return Result.success();
    }
    
    @Operation(summary = "导出字典数据列表")
    @GetMapping("/data/export")
    @PreAuthorize("hasAuthority('dict:export')")
    @Log(title = "数据字典", businessType = BusinessTypeEnum.EXPORT)
    public void exportData(HttpServletResponse response,
                           @RequestParam(required = false) String dictType) throws IOException {
        List<DictData> dataList;
        if (dictType != null && !dictType.isEmpty()) {
            dataList = dictApplicationService.getDictDataByType(dictType);
        } else {
            dataList = dictApplicationService.getAllDictData();
        }
        List<DictDataExportVO> exportList = dataList.stream()
                .map(this::convertDataToExportVO)
                .collect(Collectors.toList());
        ExcelExportUtil.exportExcel(response, exportList, DictDataExportVO.class, "字典数据", "字典数据");
    }
    
    @Operation(summary = "获取字典标签")
    @GetMapping("/label")
    public Result<String> getDictLabel(@RequestParam String dictType, @RequestParam String dictValue) {
        String label = dictApplicationService.getDictLabel(dictType, dictValue);
        return Result.success(label);
    }
    
    // ==================== 转换方法 ====================
    
    private DictType convertTypeToEntity(DictTypeRequest request) {
        return DictType.builder()
                .dictType(request.getDictType())
                .dictName(request.getDictName())
                .status(request.getStatus())
                .remark(request.getRemark())
                .build();
    }
    
    private DictTypeResponse convertTypeToResponse(DictType dictType) {
        return DictTypeResponse.builder()
                .id(dictType.getId())
                .dictType(dictType.getDictType())
                .dictName(dictType.getDictName())
                .status(dictType.getStatus())
                .remark(dictType.getRemark())
                .createTime(dictType.getCreateTime())
                .build();
    }
    
    private DictData convertDataToEntity(DictDataRequest request) {
        return DictData.builder()
                .dictType(request.getDictType())
                .dictLabel(request.getDictLabel())
                .dictValue(request.getDictValue())
                .dictSort(request.getOrderNum())
                .cssClass(request.getCssClass())
                .listClass(request.getListClass())
                .isDefault(request.getIsDefault())
                .status(request.getStatus())
                .remark(request.getRemark())
                .build();
    }
    
    private DictDataResponse convertDataToResponse(DictData dictData) {
        return DictDataResponse.builder()
                .id(dictData.getId())
                .dictType(dictData.getDictType())
                .dictLabel(dictData.getDictLabel())
                .dictValue(dictData.getDictValue())
                .orderNum(dictData.getDictSort())
                .cssClass(dictData.getCssClass())
                .listClass(dictData.getListClass())
                .isDefault(dictData.getIsDefault())
                .status(dictData.getStatus())
                .remark(dictData.getRemark())
                .createTime(dictData.getCreateTime())
                .build();
    }
    
    private DictTypeExportVO convertTypeToExportVO(DictType dictType) {
        return DictTypeExportVO.builder()
                .id(dictType.getId())
                .dictName(dictType.getDictName())
                .dictType(dictType.getDictType())
                .status(dictType.getStatus())
                .remark(dictType.getRemark())
                .createTime(dictType.getCreateTime())
                .build();
    }
    
    private DictDataExportVO convertDataToExportVO(DictData dictData) {
        return DictDataExportVO.builder()
                .id(dictData.getId())
                .dictType(dictData.getDictType())
                .dictLabel(dictData.getDictLabel())
                .dictValue(dictData.getDictValue())
                .orderNum(dictData.getDictSort())
                .cssClass(dictData.getCssClass())
                .listClass(dictData.getListClass())
                .isDefault(dictData.getIsDefault())
                .status(dictData.getStatus())
                .remark(dictData.getRemark())
                .createTime(dictData.getCreateTime())
                .build();
    }
}
