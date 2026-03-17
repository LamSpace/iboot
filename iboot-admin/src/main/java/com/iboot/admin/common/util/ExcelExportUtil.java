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

package com.iboot.admin.common.util;

import com.iboot.admin.application.service.DictApplicationService;
import com.iboot.admin.common.annotation.ExcelColumn;
import jakarta.servlet.http.HttpServletResponse;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * Excel导出工具类
 *
 * @author iBoot
 */
public class ExcelExportUtil {

    private static final Logger log = LoggerFactory.getLogger(ExcelExportUtil.class);

    private static final int DEFAULT_COLUMN_WIDTH = 15;

    private static final int MAX_COLUMN_WIDTH = 50;

    private static final String HEADER_FILL_COLOR = "C0C0C0";

    private ExcelExportUtil() {
    }

    /**
     * 导出Excel文件
     *
     * @param response  HTTP响应
     * @param dataList  数据列表
     * @param clazz     数据类型
     * @param fileName  文件名（不含后缀）
     * @param sheetName Sheet名称
     * @param <T>       数据类型
     */
    public static <T> void exportExcel(HttpServletResponse response, List<T> dataList, Class<T> clazz, String fileName,
                                       String sheetName) throws IOException {
        // 获取并排序字段
        List<FieldInfo> fieldInfos = getExcelFields(clazz);
        if (fieldInfos.isEmpty()) {
            log.warn("No @ExcelColumn annotated fields found in {}", clazz.getName());
            return;
        }
        // 设置响应头（必须在获取OutputStream之前）
        setResponseHeaders(response, fileName);
        // 获取字典服务（延迟加载）
        DictApplicationService dictService = getDictService();
        Workbook workbook = new Workbook(response.getOutputStream(), "iBoot", "1.0");
        try {
            Worksheet ws = workbook.newWorksheet(sheetName);
            // 设置列宽
            setColumnWidths(ws, fieldInfos);
            // 创建表头
            createHeaderRow(ws, fieldInfos);
            // 填充数据
            int rowNum = 1;
            for (T data : dataList) {
                for (int colNum = 0; colNum < fieldInfos.size(); colNum++) {
                    FieldInfo fieldInfo = fieldInfos.get(colNum);
                    Object value = getFieldValue(data, fieldInfo.field);
                    setCellValue(ws, rowNum, colNum, value, fieldInfo, dictService);
                }
                rowNum++;
            }
            workbook.finish();
        } catch (IOException e) {
            try {
                workbook.finish();
            } catch (IOException ignored) {
            }
            // 忽略关闭时的异常
            throw e;
        }
    }

    /**
     * 获取带ExcelColumn注解的字段列表
     */
    private static List<FieldInfo> getExcelFields(Class<?> clazz) {
        List<FieldInfo> fieldInfos = new ArrayList<>();
        // 获取所有字段（包括父类）
        Class<?> currentClass = clazz;
        while (currentClass != null && currentClass != Object.class) {
            for (Field field : currentClass.getDeclaredFields()) {
                ExcelColumn annotation = field.getAnnotation(ExcelColumn.class);
                if (annotation != null) {
                    field.setAccessible(true);
                    fieldInfos.add(new FieldInfo(field, annotation));
                }
            }
            currentClass = currentClass.getSuperclass();
        }
        // 按order排序
        fieldInfos.sort(Comparator.comparingInt(f -> f.annotation.order()));
        return fieldInfos;
    }

    /**
     * 创建表头行
     */
    private static void createHeaderRow(Worksheet ws, List<FieldInfo> fieldInfos) throws IOException {
        for (int i = 0; i < fieldInfos.size(); i++) {
            ws.value(0, i, fieldInfos.get(i).annotation.name());
            ws.style(0, i)
                    .bold()
                    .fontSize(11)
                    .fillColor(HEADER_FILL_COLOR)
                    .horizontalAlignment("center")
                    .verticalAlignment("center")
                    .borderStyle("thin")
                    .set();
        }
    }

    /**
     * 获取字段值
     */
    private static Object getFieldValue(Object obj, Field field) {
        try {
            return field.get(obj);
        } catch (IllegalAccessException e) {
            log.error("Failed to get field value: {}", field.getName(), e);
            return null;
        }
    }

    /**
     * 设置单元格值
     */
    private static void setCellValue(Worksheet ws, int row, int col, Object value, FieldInfo fieldInfo,
                                     DictApplicationService dictService) throws IOException {
        if (value == null) {
            ws.value(row, col, "");
            applyDataStyle(ws, row, col);
            return;
        }
        ExcelColumn annotation = fieldInfo.annotation;
        // 字典翻译
        if (!annotation.dictType().isEmpty() && dictService != null) {
            String dictLabel = dictService.getDictLabel(annotation.dictType(), String.valueOf(value));
            ws.value(row, col, dictLabel.isEmpty() ? String.valueOf(value) : dictLabel);
            applyDataStyle(ws, row, col);
            return;
        }
        // 日期类型处理
        if (value instanceof LocalDateTime) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(annotation.dateFormat());
            ws.value(row, col, ((LocalDateTime) value).format(formatter));
            applyDataStyle(ws, row, col);
            return;
        }
        if (value instanceof LocalDate) {
            String format = annotation.dateFormat().contains(" ") ? annotation.dateFormat().split(" ")[0]
                    : annotation.dateFormat();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
            ws.value(row, col, ((LocalDate) value).format(formatter));
            applyDataStyle(ws, row, col);
            return;
        }
        if (value instanceof Date) {
            ws.value(row, col, value.toString());
            applyDataStyle(ws, row, col);
            return;
        }
        // 数字类型
        if (value instanceof Number) {
            ws.value(row, col, ((Number) value).doubleValue());
            applyDataStyle(ws, row, col);
            return;
        }
        // 布尔类型
        if (value instanceof Boolean) {
            ws.value(row, col, (Boolean) value ? "是" : "否");
            applyDataStyle(ws, row, col);
            return;
        }
        // 默认字符串处理
        ws.value(row, col, String.valueOf(value));
        applyDataStyle(ws, row, col);
    }

    /**
     * 应用数据单元格样式
     */
    private static void applyDataStyle(Worksheet ws, int row, int col) throws IOException {
        ws.style(row, col).horizontalAlignment("center").verticalAlignment("center").borderStyle("thin").set();
    }

    /**
     * 设置列宽
     */
    private static void setColumnWidths(Worksheet ws, List<FieldInfo> fieldInfos) {
        for (int i = 0; i < fieldInfos.size(); i++) {
            int width = fieldInfos.get(i).annotation.width();
            if (width <= 0) {
                width = DEFAULT_COLUMN_WIDTH;
            }
            width = Math.min(width, MAX_COLUMN_WIDTH);
            ws.width(i, width);
        }
    }

    /**
     * 设置响应头
     */
    private static void setResponseHeaders(HttpServletResponse response, String fileName) {
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + encodedFileName + ".xlsx\";"
                + "filename*=UTF-8''" + encodedFileName + ".xlsx");
    }

    /**
     * 获取字典服务
     */
    private static DictApplicationService getDictService() {
        try {
            return SpringUtils.getBean(DictApplicationService.class);
        } catch (Exception e) {
            log.warn("DictApplicationService not available for dictionary translation");
            return null;
        }
    }

    /**
     * 字段信息内部类
     */
    private record FieldInfo(Field field, ExcelColumn annotation) {

    }

}
