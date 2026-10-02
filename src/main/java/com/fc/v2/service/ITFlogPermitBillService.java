package com.fc.v2.service;

import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.model.auto.TFlogPermitBill;
import com.fc.v2.model.auto.TFlogPermitText;
import com.fc.v2.model.auto.TFlogSignRow;

/**
 * 采伐许可证逐级核签单 Service接口（approval-chain 形状：多阶段签批，无增删改查入口）
 *
 * <p>四关：0收件、1核地、2并排签、3用章报市局。件往下推得动推不动，只由本层这套签法说了算；
 * 当下压在第几层、还缺几双手，一律照方法的回应摆到屏上。</p>
 *
 * @author fuce
 * @date 2026-09-14
 */
public interface ITFlogPermitBillService {

    /** 按主键回查单据 */
    TFlogPermitBill selectTFlogPermitBillById(Long id);

    /** 按条件查询核签台账（各件应签/已签照流水现算，落库与摆出同一回计算） */
    List<TFlogPermitBill> selectTFlogPermitBillList(QueryWrapper<TFlogPermitBill> queryWrapper);

    /** 签批一票：返回更新后的单据；被拒返回 null（原先那路签法，用法不动） */
    TFlogPermitBill approve(Long id, String approver, String comment);

    /**
     * 四关逐级核签（叠上来的第二路签法）：按件当前所在级落一笔名目，返回该件最新情形；被拒返回 null。
     * 件的推进只认层级，抢步不算过成；同一关同一只手只落一笔，替人签的一律当没签。
     * applyCardId 在收件一关把申领卡挂上同一件（空则按核签单号找回现行卡），往后各关只认这一件。
     */
    TFlogPermitBill approve(Long id, String approver, String comment, Long applyCardId);

    /** 不予出证：件到这儿为止（压死），过后谁也不往它身上追一枚；被拒返回 null */
    TFlogPermitBill reject(Long id, String approver, String comment);

    /**
     * 市局打回：先前各关记下的名目尽数沉底，从核地那一关重新往下累，
     * 作废的与后补的不并列在一处；只在章落报市局之后受理，其余情形返回 null
     */
    TFlogPermitBill rollback(Long id, String comment);

    /** 当前情形：所在级次、本级应签/已签（照流水现算），照此回应摆到屏上 */
    TFlogPermitBill progressOf(Long id);

    /** 各级现行名目（一级一级往回捋用；沉底的不与后补的并列） */
    List<TFlogSignRow> listCurrentSigns(Long id);

    /**
     * 一键出证：四关都过完，核签结论转成对外许可证文本，各栏皆取自同一件；
     * 同一位申请人当年第二条出证的，当场止步（返回 null）
     */
    TFlogPermitText issuePermit(Long id);

    /** 按核签单号回查已出文本 */
    TFlogPermitText selectPermitTextByBillNo(String billNo);
}
