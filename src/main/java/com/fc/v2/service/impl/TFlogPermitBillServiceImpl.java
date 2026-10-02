package com.fc.v2.service.impl;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TFlogApplyCardMapper;
import com.fc.v2.mapper.auto.TFlogCloseCardMapper;
import com.fc.v2.mapper.auto.TFlogPermitBillMapper;
import com.fc.v2.mapper.auto.TFlogPermitSignMapper;
import com.fc.v2.mapper.auto.TFlogStandDocMapper;
import com.fc.v2.model.auto.TFlogApplyCard;
import com.fc.v2.model.auto.TFlogCloseCard;
import com.fc.v2.model.auto.TFlogPermitBill;
import com.fc.v2.model.auto.TFlogPermitSign;
import com.fc.v2.model.auto.TFlogStandDoc;
import com.fc.v2.model.custom.PermitLedgerVo;
import com.fc.v2.service.ITFlogPermitBillService;
import com.fc.v2.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 采伐许可证逐级核签单 Service业务层处理（approval-chain 形状：多阶段签批）
 *
 * 旧一路签法（approve/reject/rollback 三方法）用法原样留着；
 * 现行采伐许可申领表的四关签法以同名方法另叠一路，本件能否推进只由这里说了算。
 *
 * @author fuce
 * @date 2026-09-14

 */
@Service
public class TFlogPermitBillServiceImpl implements ITFlogPermitBillService {

    /* ============================== 旧签法常量（不动） ============================== */
    private static final int MAX_NODE = 2;
    private static final int MODE_OR = 0;
    private static final int MODE_AND = 1;
    private static final int STATUS_RUNNING = 0;
    private static final int STATUS_PASS = 1;
    private static final int STATUS_VETO = 2;

    /* ============================== 四关签法常量 ============================== */
    /** 关序：收件→核地→并排签→用章 */
    private static final int NODE_RECEIVE = 0;
    private static final int NODE_VERIFY = 1;
    private static final int NODE_PARALLEL = 2;
    private static final int NODE_SEAL = 3;

    private static final String[] NODE_NAMES = {"收件", "核地", "并排签", "用章"};

    /** 流水名目 */
    private static final int RESULT_PASS = 1;
    private static final int RESULT_REFUSE = 2;
    private static final int RESULT_SEND_BACK = 3;

    /** 并排签两位分管：1盯拟采蓄积/强度顺位，2盯权属/更新措施/拟发文本 */
    private static final int ROLE_QTY = 1;
    private static final int ROLE_OWNER_TEXT = 2;

    /** 报批情形：0在核 1已用印发证 2不予出证 3市局打回重报 */
    private static final int BILL_RUNNING = 0;
    private static final int BILL_SEALED = 1;
    private static final int BILL_REFUSED = 2;
    private static final int BILL_SEND_BACK = 3;

    /** 省级规定口径：面积或蓄积超此线，须附伐区调查设计材料（超，不含等于） */
    private static final BigDecimal PROV_SURVEY_AREA_LIMIT = new BigDecimal("10");
    private static final BigDecimal PROV_SURVEY_QTY_LIMIT = new BigDecimal("300");

    /** 底档/卡情形 */
    private static final int DOC_ACTIVE = 0;
    private static final int CARD_SETTLED = 2;
    private static final int APPLY_CARD_CHECKED = 1;

    @javax.annotation.Resource
    private TFlogPermitBillMapper flogPermitBillMapper;

    /* 四关签法用到的在册底档、申领建档卡、勾销事务卡、签批流水 */
    @javax.annotation.Resource
    private TFlogPermitSignMapper flogPermitSignMapper;
    @javax.annotation.Resource
    private TFlogStandDocMapper flogStandDocMapper;
    @javax.annotation.Resource
    private TFlogApplyCardMapper flogApplyCardMapper;
    @javax.annotation.Resource
    private TFlogCloseCardMapper flogCloseCardMapper;

    /* ============================== 旧签法（用法原样不动） ============================== */

    @Override
    public TFlogPermitBill selectTFlogPermitBillById(Long id) {
        return this.flogPermitBillMapper.selectById(id);
    }

    @Override
    public TFlogPermitBill approve(Long id, String approver, String comment) {
        TFlogPermitBill r = this.flogPermitBillMapper.selectById(id);
        if (r == null || approver == null || approver.trim().isEmpty()) {
            return null;
        }
        r.setNodeNo(Integer.valueOf((r.getNodeNo() == null ? 0 : r.getNodeNo()) + 1));
        r.setStatus(Integer.valueOf(r.getNodeNo() >= MAX_NODE ? STATUS_PASS : STATUS_RUNNING));
        this.flogPermitBillMapper.updateById(r);
        return r;
    }

    @Override
    public TFlogPermitBill reject(Long id, String approver, String comment) {
        TFlogPermitBill r = this.flogPermitBillMapper.selectById(id);
        if (r == null) {
            return null;
        }
        this.flogPermitBillMapper.updateById(r);
        return r;
    }

    @Override
    public TFlogPermitBill rollback(Long id, String comment) {
        TFlogPermitBill r = this.flogPermitBillMapper.selectById(id);
        if (r == null) {
            return null;
        }
        int node = r.getNodeNo() == null ? 0 : r.getNodeNo();
        r.setNodeNo(Integer.valueOf(Math.max(0, node - 1)));
        this.flogPermitBillMapper.updateById(r);
        return r;
    }

    /* ============================== 四关签法 ============================== */

    @Override
    public List<TFlogPermitBill> selectPermitBillList(Wrapper<TFlogPermitBill> queryWrapper) {
        return this.flogPermitBillMapper.selectList(queryWrapper);
    }

    /**
     * 头一关收件：现行采伐许可申领表四样齐才进门——
     * ①申请人；②底档字号+小班号+权属；③拟采地点+采伐方式；
     * ④拟发文本的底料（林种树种、面积、蓄积、更新措施）。超省级面积/蓄积线须附调查设计材料。
     */
    @Override
    public TFlogPermitBill approve(TFlogPermitBill draft, String receiver, String comment) {
        if (draft == null || StringUtils.isEmpty(receiver)) {
            return null;
        }
        // 四样少一样，门都进不来
        if (StringUtils.isEmpty(draft.getApplicant())
                || StringUtils.isEmpty(draft.getSiteNo())
                || StringUtils.isEmpty(draft.getSubcompNo())
                || StringUtils.isEmpty(draft.getOwnerName())
                || StringUtils.isEmpty(draft.getCutPlace())
                || draft.getCutMode() == null
                || draft.getCutMode() < 1 || draft.getCutMode() > 6
                || StringUtils.isEmpty(draft.getForestKind())
                || draft.getCutArea() == null || draft.getCutQty() == null
                || StringUtils.isEmpty(draft.getRenewMeasure())) {
            return null;
        }
        // 超省级规定面积或蓄积量的，要附伐区调查设计材料
        boolean overLine = draft.getCutArea().compareTo(PROV_SURVEY_AREA_LIMIT) > 0
                || draft.getCutQty().compareTo(PROV_SURVEY_QTY_LIMIT) > 0;
        if (overLine && (draft.getSurveyFlag() == null || draft.getSurveyFlag() != 1)) {
            return null;
        }
        // 核签单号不许重
        if (StringUtils.isNotEmpty(draft.getBillNo())) {
            Integer dup = this.flogPermitBillMapper.selectCount(new QueryWrapper<TFlogPermitBill>()
                    .eq("bill_no", draft.getBillNo()).eq("del_flag", 0));
            if (dup != null && dup > 0) {
                return null;
            }
        }

        if (draft.getApplyYear() == null) {
            draft.setApplyYear(Calendar.getInstance().get(Calendar.YEAR));
        }
        draft.setId(null);
        draft.setRoundNo(1);
        draft.setNodeNo(NODE_RECEIVE);
        draft.setSignMode(MODE_AND);
        draft.setNeedCount(needHands(NODE_RECEIVE));
        draft.setSignCount(0);
        draft.setStatus(BILL_RUNNING);
        draft.setPermitNo(null);
        draft.setSealTime(null);
        draft.setPermitText(null);
        draft.setDelFlag(0);
        this.flogPermitBillMapper.insert(draft);

        // 头一关收件落名，随后件摆到核地关
        writeSign(draft, NODE_RECEIVE, null, receiver, RESULT_PASS, comment);
        advanceTo(draft, NODE_VERIFY);
        return draft;
    }

    /**
     * 逐级签批：只签件当下所在关，上一关没过，后一关连件都见不着，抢步不算过成。
     */
    @Override
    public TFlogPermitBill approve(Long id, String approver, String comment, Integer signRole) {
        TFlogPermitBill bill = loadActionable(id);
        if (bill == null || StringUtils.isEmpty(approver)) {
            return null;
        }
        int node = bill.getNodeNo();
        if (node == NODE_RECEIVE) {
            // 收件只由起件那一路走，这里不补签
            return null;
        }
        // 并排签才认分管口（1蓄积强度/2权属文本），其余关口不许带口
        if (node == NODE_PARALLEL) {
            if (signRole == null || (signRole != ROLE_QTY && signRole != ROLE_OWNER_TEXT)) {
                return null;
            }
        } else if (signRole != null) {
            return null;
        }
        // 替人签的一律当没签：这一笔不落，动作驳回
        if (isProxySignature(approver)) {
            return null;
        }

        if (node == NODE_VERIFY) {
            String block = verifyBlockReason(bill);
            if (block != null) {
                return null;
            }
        }

        // 并排签同一分管口同人不重复落名（手数按 distinct 落名人累，刷不出第二双）
        if (node == NODE_PARALLEL) {
            List<TFlogPermitSign> hands = roundSigns(bill, node, RESULT_PASS);
            for (TFlogPermitSign s : hands) {
                if (approver.equals(s.getSigner()) && signRole.equals(s.getSignRole())) {
                    return bill;
                }
            }
        }

        // 用章前先问当年第二证：同一申请人当年第二条出证的当场止步，一笔也不许先落
        if (node == NODE_SEAL) {
            Integer issued = this.flogPermitBillMapper.selectCount(new QueryWrapper<TFlogPermitBill>()
                    .eq("applicant", bill.getApplicant())
                    .eq("apply_year", bill.getApplyYear())
                    .eq("status", BILL_SEALED)
                    .ne("id", bill.getId())
                    .eq("del_flag", 0));
            if (issued != null && issued > 0) {
                return null;
            }
        }

        writeSign(bill, node, node == NODE_PARALLEL ? signRole : null, approver, RESULT_PASS, comment);

        if (node == NODE_PARALLEL) {
            // 两双各管一口都到齐才够得着用章；只到一位，件仍停在这一关
            if (!parallelFull(bill)) {
                refreshCurrentCount(bill);
                this.flogPermitBillMapper.updateById(bill);
                return bill;
            }
            advanceTo(bill, NODE_SEAL);
            return bill;
        }

        if (node == NODE_SEAL) {
            // 章一落到底：落印、发证号、核签结论一键转许可证文本、各栏随即封死
            bill.setPermitNo(buildPermitNo(bill));
            bill.setSealTime(new Date());
            bill.setPermitText(renderPermitText(bill));
            bill.setStatus(BILL_SEALED);
            bill.setSignCount(needHands(NODE_SEAL));
            this.flogPermitBillMapper.updateById(bill);
            return bill;
        }

        // 核地过了，往下到并排签
        advanceTo(bill, NODE_PARALLEL);
        return bill;
    }

    /**
     * 不予出证：在哪一关写下，件到这儿为止，过后谁也不往它身上追一枚。
     */
    @Override
    public TFlogPermitBill reject(Long id, String approver, String comment, Integer signRole) {
        TFlogPermitBill bill = loadActionable(id);
        if (bill == null || StringUtils.isEmpty(approver)) {
            return null;
        }
        int node = bill.getNodeNo();
        if (node == NODE_PARALLEL) {
            if (signRole == null || (signRole != ROLE_QTY && signRole != ROLE_OWNER_TEXT)) {
                return null;
            }
        } else if (signRole != null) {
            return null;
        }
        if (isProxySignature(approver)) {
            return null;
        }
        writeSign(bill, node, node == NODE_PARALLEL ? signRole : null, approver, RESULT_REFUSE, comment);
        bill.setStatus(BILL_REFUSED);
        refreshCurrentCount(bill);
        this.flogPermitBillMapper.updateById(bill);
        return bill;
    }

    /**
     * 市局打回：只认已用印报市局存一份的件。先前各关名目尽数沉底，
     * 从核地关重新往下累；作废的与后补的不并列在一处（按轮次隔开）。
     */
    @Override
    public TFlogPermitBill rollback(Long id, String officer, String comment) {
        TFlogPermitBill bill = this.flogPermitBillMapper.selectById(id);
        if (bill == null || bill.getDelFlag() != null && bill.getDelFlag() == 1) {
            return null;
        }
        if (bill.getStatus() == null || bill.getStatus() != BILL_SEALED || StringUtils.isEmpty(officer)) {
            return null;
        }
        // 打回这一笔记在旧轮，旧轮随即沉底
        writeSign(bill, NODE_SEAL, null, officer, RESULT_SEND_BACK, comment);

        int nextRound = (bill.getRoundNo() == null ? 1 : bill.getRoundNo()) + 1;
        bill.setRoundNo(nextRound);
        bill.setNodeNo(NODE_VERIFY);
        bill.setSignMode(MODE_AND);
        bill.setNeedCount(needHands(NODE_VERIFY));
        bill.setSignCount(0);
        bill.setStatus(BILL_SEND_BACK);
        // 旧证随旧轮作废，新轮从核地重累，落印时再一键转新文本。
        // 注：updateById 默认不落 null，旧证号/用章时刻/旧文本须显式 set 清空，作废与后补不并列。
        this.flogPermitBillMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<TFlogPermitBill>()
                        .eq("id", bill.getId())
                        .set("round_no", nextRound)
                        .set("node_no", NODE_VERIFY)
                        .set("sign_mode", MODE_AND)
                        .set("need_count", needHands(NODE_VERIFY))
                        .set("sign_count", 0)
                        .set("status", BILL_SEND_BACK)
                        .set("permit_no", null)
                        .set("seal_time", null)
                        .set("permit_text", null));
        return bill;
    }

    @Override
    public PermitLedgerVo ledger(Long id) {
        TFlogPermitBill bill = this.flogPermitBillMapper.selectById(id);
        if (bill == null || bill.getDelFlag() != null && bill.getDelFlag() == 1) {
            return null;
        }
        int round = bill.getRoundNo() == null ? 1 : bill.getRoundNo();
        List<TFlogPermitSign> all = this.flogPermitSignMapper.selectList(new QueryWrapper<TFlogPermitSign>()
                .eq("bill_id", bill.getId()).eq("del_flag", 0).orderByAsc("create_time", "id"));

        PermitLedgerVo vo = new PermitLedgerVo();
        vo.setBill(bill);
        vo.setCurrentNode(bill.getNodeNo());
        vo.setCurrentNodeName(nodeName(bill.getNodeNo()));
        vo.setStatus(bill.getStatus());
        vo.setStatusName(statusName(bill.getStatus()));

        int node = bill.getNodeNo() == null ? NODE_RECEIVE : bill.getNodeNo();
        int need = needHands(node);
        int signed = countHands(all, round, node);
        vo.setNeedCount(need);
        vo.setSignCount(signed);
        vo.setLackCount(Math.max(0, need - signed));

        // 当轮逐关名目：一级一级往回捋，与当日那一屏对得上号
        for (int n = NODE_RECEIVE; n <= NODE_SEAL; n++) {
            PermitLedgerVo.NodeLedger nl = new PermitLedgerVo.NodeLedger();
            nl.setNodeNo(n);
            nl.setNodeName(NODE_NAMES[n]);
            nl.setNeedCount(needHands(n));
            List<TFlogPermitSign> nodeSigns = pick(all, round, n);
            nl.setSignCount(countHands(nodeSigns));
            for (TFlogPermitSign s : nodeSigns) {
                nl.getSigns().add(new PermitLedgerVo.SigItem(s));
            }
            vo.getNodes().add(nl);
        }

        // 沉底旧轮：打回前的名目留痕，但不与当轮并列
        Map<Integer, List<TFlogPermitSign>> sunk = new LinkedHashMap<>();
        for (TFlogPermitSign s : all) {
            int r = s.getRoundNo() == null ? 1 : s.getRoundNo();
            if (r < round) {
                sunk.computeIfAbsent(r, k -> new ArrayList<>()).add(s);
            }
        }
        for (Map.Entry<Integer, List<TFlogPermitSign>> e : sunk.entrySet()) {
            PermitLedgerVo.RoundLedger rl = new PermitLedgerVo.RoundLedger();
            rl.setRoundNo(e.getKey());
            for (TFlogPermitSign s : e.getValue()) {
                rl.getSigns().add(new PermitLedgerVo.SigItem(s));
                if (s.getSignResult() != null && s.getSignResult() == RESULT_SEND_BACK) {
                    rl.setRejecter(s.getSigner());
                    rl.setOpinion(s.getOpinion());
                    rl.setRejectTime(s.getCreateTime());
                }
            }
            vo.getSunkRounds().add(rl);
        }

        vo.setBlockReason(ledgerBlockReason(bill, all, round));
        return vo;
    }

    @Override
    public String permitText(Long id) {
        TFlogPermitBill bill = this.flogPermitBillMapper.selectById(id);
        if (bill == null) {
            return null;
        }
        // 已落印的端库里存的（落印时即由此式转成），未落印的按同一件现算——两处一个算法
        return bill.getPermitNo() != null && StringUtils.isNotEmpty(bill.getPermitText())
                ? bill.getPermitText() : renderPermitText(bill);
    }

    /* ============================== 四关内里 ============================== */

    /** 只在核（含打回重报）的件签得动；已用印封死、已不予出证的，谁也追不进一枚 */
    private TFlogPermitBill loadActionable(Long id) {
        if (id == null) {
            return null;
        }
        TFlogPermitBill bill = this.flogPermitBillMapper.selectById(id);
        if (bill == null || bill.getDelFlag() != null && bill.getDelFlag() == 1) {
            return null;
        }
        Integer st = bill.getStatus();
        if (st == null || (st != BILL_RUNNING && st != BILL_SEND_BACK)) {
            return null;
        }
        return bill;
    }

    /**
     * 核地：在册底档同申领材料并排看，只信字号不信人（申请人姓名不作数）；
     * 这片小班压着没勾销完的事务卡，件便卡在这一关。
     */
    private String verifyBlockReason(TFlogPermitBill bill) {
        TFlogStandDoc doc = this.flogStandDocMapper.selectOne(new QueryWrapper<TFlogStandDoc>()
                .eq("site_no", bill.getSiteNo()).eq("del_flag", 0));
        if (doc == null) {
            return "底档字号「" + bill.getSiteNo() + "」对不上在册底档";
        }
        if (doc.getStatus() != null && doc.getStatus() != DOC_ACTIVE) {
            return "底档字号「" + bill.getSiteNo() + "」已收回，不在册";
        }
        // 申领材料只按字号配小班号认现行、已核对的建档卡；姓名对得一字不差也白搭
        TFlogApplyCard card = this.flogApplyCardMapper.selectOne(new QueryWrapper<TFlogApplyCard>()
                .eq("site_no", bill.getSiteNo())
                .eq("subcomp_no", bill.getSubcompNo())
                .eq("current_flag", 1)
                .eq("del_flag", 0));
        if (card == null || card.getStatus() == null || card.getStatus() != APPLY_CARD_CHECKED) {
            return "字号「" + bill.getSiteNo() + "」配小班号「" + bill.getSubcompNo() + "」没有现行已核对的申领材料";
        }
        // 没勾销完 = 未起或在办；已了断的不压件
        List<TFlogCloseCard> pending = this.flogCloseCardMapper.selectList(new QueryWrapper<TFlogCloseCard>()
                .eq("site_no", bill.getSiteNo())
                .eq("subcomp_no", bill.getSubcompNo())
                .eq("del_flag", 0));
        for (TFlogCloseCard c : pending) {
            if (c.getStatus() == null || c.getStatus() < CARD_SETTLED) {
                return "小班「" + bill.getSubcompNo() + "」还压着没勾销完的事务卡「" + c.getBizNo() + "」";
            }
        }
        return null;
    }

    /** 并排签凑齐：蓄积强度、权属文本两口各一笔，且是两双不同的手 */
    private boolean parallelFull(TFlogPermitBill bill) {
        List<TFlogPermitSign> hands = roundSigns(bill, NODE_PARALLEL, RESULT_PASS);
        boolean qty = false;
        boolean ownerText = false;
        for (TFlogPermitSign s : hands) {
            if (s.getSignRole() != null && s.getSignRole() == ROLE_QTY) {
                qty = true;
            }
            if (s.getSignRole() != null && s.getSignRole() == ROLE_OWNER_TEXT) {
                ownerText = true;
            }
        }
        return qty && ownerText && countHands(hands) >= needHands(NODE_PARALLEL);
    }

    private void advanceTo(TFlogPermitBill bill, int node) {
        bill.setNodeNo(node);
        bill.setNeedCount(needHands(node));
        refreshCurrentCount(bill);
        if (bill.getStatus() != null && bill.getStatus() == BILL_SEND_BACK) {
            bill.setStatus(BILL_RUNNING);
        }
        this.flogPermitBillMapper.updateById(bill);
    }

    /** 本关已签手数：库数与屏数同此一回计算——当轮本关通过流水 distinct 落名人 count */
    private void refreshCurrentCount(TFlogPermitBill bill) {
        int round = bill.getRoundNo() == null ? 1 : bill.getRoundNo();
        int node = bill.getNodeNo() == null ? NODE_RECEIVE : bill.getNodeNo();
        bill.setSignCount(countHands(roundSigns(bill, round, node, RESULT_PASS)));
    }

    private List<TFlogPermitSign> roundSigns(TFlogPermitBill bill, int node, int result) {
        return roundSigns(bill, bill.getRoundNo() == null ? 1 : bill.getRoundNo(), node, result);
    }

    private List<TFlogPermitSign> roundSigns(TFlogPermitBill bill, int round, int node, int result) {
        return this.flogPermitSignMapper.selectList(new QueryWrapper<TFlogPermitSign>()
                .eq("bill_id", bill.getId())
                .eq("round_no", round)
                .eq("node_no", node)
                .eq("sign_result", result)
                .eq("del_flag", 0)
                .orderByAsc("create_time", "id"));
    }

    private List<TFlogPermitSign> pick(List<TFlogPermitSign> all, int round, int node) {
        List<TFlogPermitSign> out = new ArrayList<>();
        for (TFlogPermitSign s : all) {
            int r = s.getRoundNo() == null ? 1 : s.getRoundNo();
            if (r == round && s.getNodeNo() != null && s.getNodeNo() == node) {
                out.add(s);
            }
        }
        return out;
    }

    /** 手数一律按 distinct 落名人 count；同一分钟两笔仍是两笔，同一个人签两回只算一双 */
    private int countHands(List<TFlogPermitSign> signs) {
        Map<String, Boolean> who = new LinkedHashMap<>();
        for (TFlogPermitSign s : signs) {
            if (StringUtils.isNotEmpty(s.getSigner())) {
                who.put(s.getSigner(), Boolean.TRUE);
            }
        }
        return who.size();
    }

    private int countHands(List<TFlogPermitSign> all, int round, int node) {
        List<TFlogPermitSign> pass = new ArrayList<>();
        for (TFlogPermitSign s : pick(all, round, node)) {
            if (s.getSignResult() != null && s.getSignResult() == RESULT_PASS) {
                pass.add(s);
            }
        }
        return countHands(pass);
    }

    private void writeSign(TFlogPermitBill bill, int node, Integer signRole, String signer, int result, String opinion) {
        TFlogPermitSign s = new TFlogPermitSign();
        s.setBillId(bill.getId());
        s.setRoundNo(bill.getRoundNo() == null ? 1 : bill.getRoundNo());
        s.setNodeNo(node);
        s.setSignRole(signRole);
        s.setSigner(signer);
        s.setSignResult(result);
        s.setOpinion(opinion);
        s.setDelFlag(0);
        Date now = new Date();
        s.setCreateBy(signer);
        s.setCreateTime(now);
        s.setUpdateTime(now);
        this.flogPermitSignMapper.insert(s);
    }

    /** 替签认字样：落名人里带“代/替”的，当没签 */
    private boolean isProxySignature(String signer) {
        return signer != null && (signer.contains("代") || signer.contains("替"));
    }

    private int needHands(int node) {
        if (node == NODE_PARALLEL) {
            return 2;
        }
        return 1;
    }

    private String nodeName(Integer node) {
        if (node == null || node < 0 || node >= NODE_NAMES.length) {
            return "";
        }
        return NODE_NAMES[node];
    }

    private String statusName(Integer status) {
        if (status == null) {
            return "";
        }
        switch (status) {
            case BILL_RUNNING: return "在核";
            case BILL_SEALED: return "已用印发证";
            case BILL_REFUSED: return "不予出证";
            case BILL_SEND_BACK: return "市局打回重报";
            default: return "";
        }
    }

    private String ledgerBlockReason(TFlogPermitBill bill, List<TFlogPermitSign> all, int round) {
        Integer st = bill.getStatus();
        if (st != null && st == BILL_REFUSED) {
            for (TFlogPermitSign s : pick(all, round, bill.getNodeNo())) {
                if (s.getSignResult() != null && s.getSignResult() == RESULT_REFUSE) {
                    return "不予出证（" + nodeName(bill.getNodeNo()) + "关，" + s.getSigner() + "）";
                }
            }
            return "不予出证";
        }
        if (st != null && st == BILL_SEALED) {
            return "已用印发证，各栏封死，要动只能另起一件";
        }
        int node = bill.getNodeNo() == null ? NODE_RECEIVE : bill.getNodeNo();
        if (node == NODE_VERIFY) {
            return verifyBlockReason(bill);
        }
        if (node == NODE_PARALLEL) {
            int lack = needHands(NODE_PARALLEL) - countHands(all, round, NODE_PARALLEL);
            if (lack > 0) {
                return "并排签还差" + lack + "双手，够不着用章";
            }
        }
        return null;
    }

    private String buildPermitNo(TFlogPermitBill bill) {
        String tail = StringUtils.isNotEmpty(bill.getBillNo())
                ? bill.getBillNo()
                : String.valueOf(bill.getId());
        return "采许字〔" + bill.getApplyYear() + "〕" + tail + "号";
    }

    /**
     * 核签结论一键转对外许可证文本：申请人、小班号、地点、林种树种、面积、蓄积、
     * 采伐方式、更新措施、权属皆取自同一件，不必再誊第二遍。
     */
    private String renderPermitText(TFlogPermitBill bill) {
        StringBuilder sb = new StringBuilder();
        sb.append("林木采伐许可证");
        if (StringUtils.isNotEmpty(bill.getPermitNo())) {
            sb.append("（").append(bill.getPermitNo()).append("）");
        }
        sb.append("\n申请人：").append(nullToEmpty(bill.getApplicant()));
        sb.append("\n权属：").append(nullToEmpty(bill.getOwnerName()));
        sb.append("\n底档字号：").append(nullToEmpty(bill.getSiteNo()));
        sb.append("　小班号：").append(nullToEmpty(bill.getSubcompNo()));
        sb.append("\n拟采地点：").append(nullToEmpty(bill.getCutPlace()));
        sb.append("\n林种树种：").append(nullToEmpty(bill.getForestKind()));
        sb.append("\n采伐方式：").append(cutModeName(bill.getCutMode()));
        sb.append("\n拟采面积：").append(bill.getCutArea() == null ? "" : bill.getCutArea().toPlainString()).append("公顷");
        sb.append("　拟采蓄积：").append(bill.getCutQty() == null ? "" : bill.getCutQty().toPlainString()).append("立方米");
        sb.append("\n更新措施：").append(nullToEmpty(bill.getRenewMeasure()));
        return sb.toString();
    }

    private String cutModeName(Integer cutMode) {
        if (cutMode == null) {
            return "";
        }
        switch (cutMode) {
            case 1: return "皆伐";
            case 2: return "渐伐";
            case 3: return "择伐";
            case 4: return "抚育采伐";
            case 5: return "更新采伐";
            case 6: return "低质低效林改造";
            default: return "";
        }
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
