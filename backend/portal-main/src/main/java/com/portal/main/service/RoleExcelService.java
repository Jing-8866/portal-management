package com.portal.main.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.portal.common.model.SysRole;
import com.portal.main.dto.ImportResult;
import com.portal.main.mapper.SysRoleMapper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

@Service
public class RoleExcelService {

    @Autowired
    private SysRoleMapper roleMapper;

    public void exportRoles(OutputStream os, List<Long> idList) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("角色列表");

        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        String[] headers = {"角色名称", "角色编码", "描述", "状态"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(i, 6000);
        }

        List<SysRole> roles;
        if (idList != null && !idList.isEmpty()) {
            roles = roleMapper.selectList(new LambdaQueryWrapper<SysRole>().in(SysRole::getId, idList).orderByAsc(SysRole::getId));
        } else {
            roles = roleMapper.selectList(new LambdaQueryWrapper<SysRole>().orderByAsc(SysRole::getId));
        }
        int rowIdx = 1;
        for (SysRole role : roles) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(role.getRoleName() != null ? role.getRoleName() : "");
            row.createCell(1).setCellValue(role.getRoleCode() != null ? role.getRoleCode() : "");
            row.createCell(2).setCellValue(role.getDescription() != null ? role.getDescription() : "");
            row.createCell(3).setCellValue(role.getStatus() != null && role.getStatus() == 1 ? "启用" : "停用");
        }
        workbook.write(os);
        workbook.close();
    }

    public void generateTemplate(OutputStream os) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("角色导入模板");

        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        String[] headers = {"角色名称(必填)", "角色编码(必填)", "描述", "状态(启用/停用)"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(i, 7000);
        }

        Row sample = sheet.createRow(1);
        sample.createCell(0).setCellValue("业务管理员");
        sample.createCell(1).setCellValue("BIZ_ADMIN");
        sample.createCell(2).setCellValue("负责业务模块管理");
        sample.createCell(3).setCellValue("启用");

        workbook.write(os);
        workbook.close();
    }

    public ImportResult importRoles(InputStream is) throws IOException {
        ImportResult result = new ImportResult();
        Workbook workbook = new XSSFWorkbook(is);
        Sheet sheet = workbook.getSheetAt(0);
        int lastRow = sheet.getLastRowNum();
        result.setTotalRows(lastRow);

        for (int i = 1; i <= lastRow; i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            String roleName = getCellString(row, 0);
            String roleCode = getCellString(row, 1);
            if (!StringUtils.hasText(roleName) || !StringUtils.hasText(roleCode)) {
                result.setFailedCount(result.getFailedCount() + 1);
                result.getErrors().add("第" + (i + 1) + "行: 角色名称或角色编码为空");
                continue;
            }

            String description = getCellString(row, 2);
            String statusStr = getCellString(row, 3);
            Integer status = "停用".equals(statusStr) ? 0 : 1;

            try {
                SysRole existRole = roleMapper.selectOne(
                        new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, roleCode));
                if (existRole != null) {
                    LambdaUpdateWrapper<SysRole> w = new LambdaUpdateWrapper<>();
                    w.eq(SysRole::getId, existRole.getId());
                    w.set(SysRole::getRoleName, roleName);
                    if (StringUtils.hasText(description)) w.set(SysRole::getDescription, description);
                    w.set(SysRole::getStatus, status);
                    roleMapper.update(null, w);
                    result.setUpdatedCount(result.getUpdatedCount() + 1);
                } else {
                    SysRole newRole = new SysRole();
                    newRole.setRoleName(roleName);
                    newRole.setRoleCode(roleCode);
                    newRole.setDescription(description);
                    newRole.setStatus(status);
                    roleMapper.insert(newRole);
                    result.setCreatedCount(result.getCreatedCount() + 1);
                }
            } catch (Exception e) {
                result.setFailedCount(result.getFailedCount() + 1);
                result.getErrors().add("第" + (i + 1) + "行(" + roleCode + "): " + e.getMessage());
            }
        }
        workbook.close();
        return result;
    }

    private String getCellString(Row row, int idx) {
        Cell cell = row.getCell(idx);
        if (cell == null) return "";
        if (cell.getCellType() == CellType.NUMERIC) return String.valueOf((long) cell.getNumericCellValue());
        cell.setCellType(CellType.STRING);
        return cell.getStringCellValue().trim();
    }
}
