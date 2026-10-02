package com.fc.v2.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.domain.ResultTable;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.auto.TFlogPermitBill;
import com.fc.v2.model.auto.TFlogPermitText;
import com.fc.v2.service.ITFlogPermitBillService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

/**
 * 采伐许可证逐级核签单 Controller（approval-chain 形状：签批入口）
 *
 * <p>件往下推得动推不动，只有业务服务层那套签法说了算；本模块自己不开
 * 收件、盖印、撤件任何一处的门，当下压在第几层、还缺几双手，一律照方法的回应摆到屏上。</p>
 *
 * @author fuce
 * @date 2026-10-02
 */
@Api(value = "采伐许可证逐级核签单")
@Controller
@RequestMapping("/flogPermitBill")
public class FlogPermitBillController extends BaseController {

    private final String prefix = "admin/flogPermitBill";

    @Autowired
    private ITFlogPermitBillService flogPermitBillService;

    @ApiOperation(value = "核签台账跳转", notes = "核签台账跳转")
    @GetMapping("/view")
    @RequiresPermissions("flogPermitBill:view")
    public String view(ModelMap model) {
        return prefix + "/list";
    }

    @Log(title = "采伐许可证核签台账", action = "list")
    @ApiOperation(value = "核签台账", notes = "核签台账")
    @GetMapping("/list")
    @RequiresPermissions("flogPermitBill:list")
    @ResponseBody
    public ResultTable list(TFlogPermitBill record) {
        QueryWrapper<TFlogPermitBill> queryWrapper = new QueryWrapper<TFlogPermitBill>();
        startPage();
        com.github.pagehelper.PageInfo<TFlogPermitBill> page =
                new com.github.pagehelper.PageInfo<TFlogPermitBill>(flogPermitBillService.selectTFlogPermitBillList(queryWrapper));
        return pageTable(page.getList(), page.getTotal());
    }

    @Log(title = "采伐许可证四关核签", action = "approve")
    @ApiOperation(value = "落一笔核签名目", notes = "四关逐级核签：0收件、1核地、2并排签、3用章；落得下落不下由服务层签法说了算")
    @PostMapping("/approve")
    @RequiresPermissions("flogPermitBill:approve")
    @ResponseBody
    public AjaxResult approve(Long id, String approver, String comment, Long applyCardId) {
        TFlogPermitBill r = flogPermitBillService.approve(id, approver, comment, applyCardId);
        return r != null ? AjaxResult.success(0, "已落一笔", r) : AjaxResult.error("这一笔落不下");
    }

    @Log(title = "采伐许可证不予出证", action = "reject")
    @ApiOperation(value = "不予出证", notes = "哪一关写下不予出证，件到这儿为止")
    @PostMapping("/reject")
    @RequiresPermissions("flogPermitBill:reject")
    @ResponseBody
    public AjaxResult reject(Long id, String approver, String comment) {
        TFlogPermitBill r = flogPermitBillService.reject(id, approver, comment);
        return r != null ? AjaxResult.success(0, "已不予出证，件到此为止", r) : AjaxResult.error("落不下");
    }

    @Log(title = "采伐许可证市局打回", action = "rollback")
    @ApiOperation(value = "市局打回", notes = "先前各关名目尽数沉底，从核地那一关重新往下累")
    @PostMapping("/rollback")
    @RequiresPermissions("flogPermitBill:rollback")
    @ResponseBody
    public AjaxResult rollback(Long id, String comment) {
        TFlogPermitBill r = flogPermitBillService.rollback(id, comment);
        return r != null ? AjaxResult.success(0, "已打回，自核地一关重累", r) : AjaxResult.error("打不回");
    }

    @Log(title = "采伐许可证一键出证", action = "issue")
    @ApiOperation(value = "一键出证", notes = "核签结论转对外许可证文本，各栏皆取自同一件")
    @PostMapping("/issue")
    @RequiresPermissions("flogPermitBill:issue")
    @ResponseBody
    public AjaxResult issue(Long id) {
        TFlogPermitText t = flogPermitBillService.issuePermit(id);
        return t != null ? AjaxResult.success(0, "已出证", t) : AjaxResult.error("出不了证");
    }

    @ApiOperation(value = "当前情形", notes = "当下压在第几层、还缺几双手，照方法的回应摆到屏上")
    @GetMapping("/progress")
    @RequiresPermissions("flogPermitBill:progress")
    @ResponseBody
    public AjaxResult progress(Long id) {
        TFlogPermitBill r = flogPermitBillService.progressOf(id);
        return r != null ? AjaxResult.success(0, "当前情形", r) : AjaxResult.error("查无此件");
    }

    @ApiOperation(value = "各级现行名目", notes = "一级一级往回捋；沉底的不与后补的并列")
    @GetMapping("/signs")
    @RequiresPermissions("flogPermitBill:signs")
    @ResponseBody
    public AjaxResult signs(Long id) {
        return AjaxResult.success(0, "各级现行名目", flogPermitBillService.listCurrentSigns(id));
    }
}
