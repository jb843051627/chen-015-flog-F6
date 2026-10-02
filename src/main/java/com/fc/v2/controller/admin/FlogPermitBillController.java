package com.fc.v2.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.domain.ResultTable;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.auto.TFlogPermitBill;
import com.fc.v2.model.custom.PermitLedgerVo;
import com.fc.v2.service.ITFlogPermitBillService;
import com.fc.v2.shiro.util.ShiroUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

/**
 * 采伐许可证逐级核签单 Controller（approval-chain 形状：只开四关流转入口）。
 * 模块自身不开自由建档/盖印/撤件的门：收件、签批、不予出证、市局打回、用印
 * 全部由业务服务层那套签法说了算，界面不留手数代填格，层数与缺手照服务回应摆。
 *
 * @author fuce
 * @date 2026-10-02
 */
@Api(value = "采伐许可证核签")
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
    @ApiOperation(value = "台账列表", notes = "台账列表")
    @GetMapping("/list")
    @RequiresPermissions("flogPermitBill:list")
    @ResponseBody
    public ResultTable list(TFlogPermitBill record) {
        QueryWrapper<TFlogPermitBill> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("del_flag", 0).orderByDesc("create_time");
        startPage();
        com.github.pagehelper.PageInfo<TFlogPermitBill> page =
                new com.github.pagehelper.PageInfo<>(flogPermitBillService.selectPermitBillList(queryWrapper));
        return pageTable(page.getList(), page.getTotal());
    }

    @Log(title = "采伐许可证核签·台账明细", action = "ledger")
    @ApiOperation(value = "一件的逐关名目台账", notes = "当前关/应到已到缺手/逐关名目/沉底旧轮，照服务回应摆")
    @GetMapping("/ledger")
    @RequiresPermissions("flogPermitBill:list")
    @ResponseBody
    public AjaxResult ledger(Long id) {
        PermitLedgerVo vo = flogPermitBillService.ledger(id);
        return vo == null ? AjaxResult.error("件不存在") : AjaxResult.successData(200, vo);
    }

    @Log(title = "采伐许可证核签·收件", action = "receive")
    @ApiOperation(value = "头一关收件", notes = "现行采伐许可申领表四样齐才进门，超线须附调查设计材料")
    @PostMapping("/receive")
    @RequiresPermissions("flogPermitBill:receive")
    @ResponseBody
    public AjaxResult receive(TFlogPermitBill draft, String comment) {
        TFlogPermitBill r = flogPermitBillService.approve(draft, ShiroUtils.getLoginName(), comment);
        return r == null ? AjaxResult.error("收件被拒：申领四样不齐、采伐方式不对或超线未附伐区调查设计材料")
                : AjaxResult.successData(200, r);
    }

    @Log(title = "采伐许可证核签·签批", action = "approve")
    @ApiOperation(value = "逐级签批", notes = "只签件当下所在关；并排签传 signRole 1蓄积强度 2权属文本")
    @PostMapping("/approve")
    @RequiresPermissions("flogPermitBill:approve")
    @ResponseBody
    public AjaxResult approve(Long id, Integer signRole, String comment) {
        TFlogPermitBill r = flogPermitBillService.approve(id, ShiroUtils.getLoginName(), comment, signRole);
        return r == null ? AjaxResult.error("签不进：件不在本关/上一关没过/替签不作数/当年第二证") : AjaxResult.successData(200, r);
    }

    @Log(title = "采伐许可证核签·不予出证", action = "refuse")
    @ApiOperation(value = "不予出证", notes = "件到这儿为止，过后谁也不追一枚")
    @PostMapping("/refuse")
    @RequiresPermissions("flogPermitBill:approve")
    @ResponseBody
    public AjaxResult refuse(Long id, Integer signRole, String comment) {
        TFlogPermitBill r = flogPermitBillService.reject(id, ShiroUtils.getLoginName(), comment, signRole);
        return r == null ? AjaxResult.error("不予出证这笔记不下") : AjaxResult.successData(200, r);
    }

    @Log(title = "采伐许可证核签·市局打回", action = "sendBack")
    @ApiOperation(value = "市局打回", notes = "先前各关名目沉底，从核地重新往下累")
    @PostMapping("/sendBack")
    @RequiresPermissions("flogPermitBill:sendBack")
    @ResponseBody
    public AjaxResult sendBack(Long id, String comment) {
        TFlogPermitBill r = flogPermitBillService.rollback(id, ShiroUtils.getLoginName(), comment);
        return r == null ? AjaxResult.error("只有已用印报市局的件打得回") : AjaxResult.successData(200, r);
    }

    @Log(title = "采伐许可证核签·许可证文本", action = "permitText")
    @ApiOperation(value = "核签结论一键转许可证文本", notes = "各数皆取自同一件，不必誊第二遍")
    @GetMapping("/permitText")
    @RequiresPermissions("flogPermitBill:list")
    @ResponseBody
    public AjaxResult permitText(Long id) {
        String text = flogPermitBillService.permitText(id);
        return text == null ? AjaxResult.error("件不存在") : AjaxResult.successData(200, text);
    }
}
