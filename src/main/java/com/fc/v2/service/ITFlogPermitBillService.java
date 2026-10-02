package com.fc.v2.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fc.v2.model.auto.TFlogPermitBill;
import com.fc.v2.model.custom.PermitLedgerVo;

import java.util.List;

/**
 * 采伐许可证逐级核签单 Service接口（approval-chain 形状：多阶段签批，无增删改查入口）
 *
 * 旧一路签法（approve/reject/rollback 三方法）用法留着不动；
 * 现行采伐许可申领表四关签法以同名方法另叠一路，见各方法“四关”注释。
 *
 * @author fuce
 * @date 2026-09-14
 */
public interface ITFlogPermitBillService {

    /** 按主键回查单据 */
    TFlogPermitBill selectTFlogPermitBillById(Long id);

    /** 【旧签法】签批一票：返回更新后的单据；被拒返回 null */
    TFlogPermitBill approve(Long id, String approver, String comment);

    /** 【旧签法】否决：返回更新后的单据；被拒返回 null */
    TFlogPermitBill reject(Long id, String approver, String comment);

    /** 【旧签法】退回上一环节：返回更新后的单据；被拒返回 null */
    TFlogPermitBill rollback(Long id, String comment);

    /* ==================== 四关签法（收件→核地→并排签→用章） ==================== */

    /**
     * 四关·头一关收件（与旧 approve 同名另叠）：以现行采伐许可申领表起一件。
     * 申请人、小班号与权属、拟采地点与方式、文本打算怎么写，四样少一样门都进不来；
     * 超省级规定面积或蓄积量的须附伐区调查设计材料。收件落名后件摆到核地关。
     * 被拒返回 null。
     */
    TFlogPermitBill approve(TFlogPermitBill draft, String receiver, String comment);

    /**
     * 四关·逐级签批（与旧 approve 同名另叠）：只签件当下所在关，抢步不算过成。
     * 核地：只信底档字号不信人，且这片小班压着没勾销完的事务卡便卡住；
     * 并排签：signRole 1盯蓄积强度、2盯权属文本，两位各落一笔（掐同一分钟也是两笔），
     * 只到一位件仍停在本关，替签一律当没签；
     * 用章：同一申请人当年第二条出证当场止步；过则落印封死、一键转成许可证文本。
     * 被拒返回 null。
     */
    TFlogPermitBill approve(Long id, String approver, String comment, Integer signRole);

    /**
     * 四关·不予出证（与旧 reject 同名另叠）：在哪一关写下，件到这儿为止，
     * 过后谁也不往它身上追一枚。signRole 仅并排签用（1/2），其余关传 null。
     * 被拒返回 null。
     */
    TFlogPermitBill reject(Long id, String approver, String comment, Integer signRole);

    /**
     * 四关·市局打回（与旧 rollback 同名另叠）：只认已报市局存一份的件。
     * 先前各关记下的名目尽数沉底，从核地关重新往下累，作废的与后补的不并列。
     * 被拒返回 null。
     */
    TFlogPermitBill rollback(Long id, String officer, String comment);

    /**
     * 核签台账：件当下压在第几关、还缺几双手、每关落过的名目，
     * 一律照此回应摆到屏上（不先摆层数再凑名目）；逐级回捋与当日那一屏对得上号。
     */
    PermitLedgerVo ledger(Long id);

    /** 核签结论一键转对外许可证文本：各数皆取自同一件，两处端出的数一样粗。 */
    String permitText(Long id);

    /** 台账列表 */
    List<TFlogPermitBill> selectPermitBillList(Wrapper<TFlogPermitBill> queryWrapper);
}
