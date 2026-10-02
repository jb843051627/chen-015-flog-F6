package com.fc.v2.service.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TFlogApplyCardMapper;
import com.fc.v2.mapper.auto.TFlogCloseCardMapper;
import com.fc.v2.mapper.auto.TFlogPermitBillMapper;
import com.fc.v2.mapper.auto.TFlogPermitTextMapper;
import com.fc.v2.mapper.auto.TFlogSignRowMapper;
import com.fc.v2.mapper.auto.TFlogStandDocMapper;
import com.fc.v2.model.auto.TFlogApplyCard;
import com.fc.v2.model.auto.TFlogCloseCard;
import com.fc.v2.model.auto.TFlogPermitBill;
import com.fc.v2.model.auto.TFlogPermitText;
import com.fc.v2.model.auto.TFlogSignRow;
import com.fc.v2.model.auto.TFlogStandDoc;
import com.fc.v2.service.ITFlogPermitBillService;

/**
 * 采伐许可证逐级核签单 Service业务层处理（approval-chain 形状：多阶段签批）
 *
 * <p>四关：0收件、1核地、2并排签、3用章报市局。每一关的审核都留得下名，
 * 上一关没通过，后一关连件都见不着；件的推进只认层级，抢步不算过成。</p>
 *
 * @author fuce
 * @date 2026-09-14
 */
@Service
public class TFlogPermitBillServiceImpl implements ITFlogPermitBillService {

    private static final int MAX_NODE = 2;
    private static final int MODE_OR = 0;
    private static final int MODE_AND = 1;
    private static final int STATUS_RUNNING = 0;
    private static final int STATUS_PASS = 1;
    private static final int STATUS_VETO = 2;

    /** 四关级次：0收件、1核地、2并排签、3用章报市局 */
    private static final int NODE_INTAKE = 0;
    private static final int NODE_LAND = 1;
    private static final int NODE_PARALLEL = 2;
    private static final int NODE_SEAL = 3;
    private static final String[] NODE_NAMES = {"收件", "核地", "并排签", "用章"};

    /** 流水落笔情形：0核签 1不予出证 */
    private static final int ACT_SIGN = 0;
    private static final int ACT_VETO = 1;

    /** 流水现行标记：1现行 0沉底 */
    private static final int CURRENT_YES = 1;
    private static final int CURRENT_NO = 0;

    /** 勾销事务卡落定：2已了断 */
    private static final int CLOSE_DONE = 2;

    /** 许可证文本情形：0已出证 1已作废 */
    private static final int TEXT_ISSUED = 0;

    @javax.annotation.Resource
    private TFlogPermitBillMapper flogPermitBillMapper;

    @javax.annotation.Resource
    private TFlogSignRowMapper flogSignRowMapper;

    @javax.annotation.Resource
    private TFlogPermitTextMapper flogPermitTextMapper;

    @javax.annotation.Resource
    private TFlogApplyCardMapper flogApplyCardMapper;

    @javax.annotation.Resource
    private TFlogStandDocMapper flogStandDocMapper;

    @javax.annotation.Resource
    private TFlogCloseCardMapper flogCloseCardMapper;

    @Override
    public TFlogPermitBill selectTFlogPermitBillById(Long id) {
        return this.flogPermitBillMapper.selectById(id);
    }

    @Override
    public List<TFlogPermitBill> selectTFlogPermitBillList(QueryWrapper<TFlogPermitBill> queryWrapper) {
        List<TFlogPermitBill> list = this.flogPermitBillMapper.selectList(queryWrapper);
        // 摆给人看的数同库里存的数须出自同一回计算：逐件照流水现算，落库后再摆出
        for (TFlogPermitBill bill : list) {
            refreshCounts(bill);
            this.flogPermitBillMapper.updateById(bill);
        }
        return list;
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

    /**
     * 四关逐级核签（第二路签法）。哪一关写下名目，件才算过了哪一关；
     * 这一笔落不下（关把口不通过、替人签、件已止步或压死）一律返回 null。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TFlogPermitBill approve(Long id, String approver, String comment, Long applyCardId) {
        if (id == null || approver == null || approver.trim().isEmpty()) {
            return null;
        }
        TFlogPermitBill bill = this.flogPermitBillMapper.selectById(id);
        if (bill == null || isDeleted(bill.getDelFlag()) || isBlank(bill.getBillNo())) {
            return null;
        }
        // 章一落到底各栏随即封死；已压死的件，过后谁也不往它身上追一枚
        if (bill.getStatus() == null || bill.getStatus() != STATUS_RUNNING) {
            return null;
        }
        int node = bill.getNodeNo() == null ? NODE_INTAKE : bill.getNodeNo();
        if (node < NODE_INTAKE || node > NODE_SEAL) {
            return null;
        }
        String signer = approver.trim();

        // 同一件：收件一关把申领卡挂上，往后各关只认这一件
        TFlogApplyCard card = bindCard(bill, applyCardId);
        if (card == null) {
            return null;
        }

        // 各关把口：头一关收件四样缺一不可；核地只信字号不信人，压着没勾销完的事务卡不放行
        if (node == NODE_INTAKE && !intakePass(card)) {
            return null;
        }
        if (node == NODE_LAND && !landPass(card)) {
            return null;
        }

        // 替人签的一律当没签：同一关同一只手只落一笔
        if (countSigns(bill.getBillNo(), node, signer) > 0) {
            return null;
        }

        // 落一笔名目，两位各记各的，掐着同一分钟落完也是两笔
        TFlogSignRow row = new TFlogSignRow();
        row.setBillNo(bill.getBillNo());
        row.setNodeNo(Integer.valueOf(node));
        row.setSigner(signer);
        row.setActType(Integer.valueOf(ACT_SIGN));
        row.setContent(comment);
        row.setCurrentFlag(Integer.valueOf(CURRENT_YES));
        row.setDelFlag(Integer.valueOf(0));
        row.setCreateBy(signer);
        this.flogSignRowMapper.insert(row);

        // 这一关凑齐了几双手，由件自己在流水里一笔一笔累出来；凑齐才进下一关
        int signed = countSigns(bill.getBillNo(), node, null);
        if (signed >= needOf(node)) {
            node = node + 1;
            if (node > NODE_SEAL) {
                node = NODE_SEAL;
                // 章一落到底，各栏随即封死，要动只能另起一件
                bill.setStatus(Integer.valueOf(STATUS_PASS));
            }
        }
        bill.setNodeNo(Integer.valueOf(node));
        bill.setApplyId(card.getId());
        refreshCounts(bill);
        this.flogPermitBillMapper.updateById(bill);
        return this.flogPermitBillMapper.selectById(bill.getId());
    }

    /**
     * 不予出证：名目留在当下这一关，件到这儿为止（压死），
     * 过后谁也不往它身上追一枚。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TFlogPermitBill reject(Long id, String approver, String comment) {
        if (id == null || approver == null || approver.trim().isEmpty()) {
            return null;
        }
        TFlogPermitBill bill = this.flogPermitBillMapper.selectById(id);
        if (bill == null || isDeleted(bill.getDelFlag()) || isBlank(bill.getBillNo())) {
            return null;
        }
        if (bill.getStatus() == null || bill.getStatus() != STATUS_RUNNING) {
            return null;
        }
        int node = bill.getNodeNo() == null ? NODE_INTAKE : bill.getNodeNo();

        // 哪一关写下不予出证，名目留在哪一关
        TFlogSignRow row = new TFlogSignRow();
        row.setBillNo(bill.getBillNo());
        row.setNodeNo(Integer.valueOf(node));
        row.setSigner(approver.trim());
        row.setActType(Integer.valueOf(ACT_VETO));
        row.setContent(isBlank(comment) ? "不予出证" : "不予出证：" + comment.trim());
        row.setCurrentFlag(Integer.valueOf(CURRENT_YES));
        row.setDelFlag(Integer.valueOf(0));
        row.setCreateBy(approver.trim());
        this.flogSignRowMapper.insert(row);

        bill.setStatus(Integer.valueOf(STATUS_VETO));
        bill.setRemark("不予出证：" + (isBlank(comment) ? "" : comment.trim()));
        refreshCounts(bill);
        this.flogPermitBillMapper.updateById(bill);
        return this.flogPermitBillMapper.selectById(bill.getId());
    }

    /**
     * 市局打回：只在章落报市局之后受理。先前各关记下的名目尽数沉底，
     * 从核地那一关重新往下累，作废的与后补的不并列在一处。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TFlogPermitBill rollback(Long id, String comment) {
        if (id == null) {
            return null;
        }
        TFlogPermitBill bill = this.flogPermitBillMapper.selectById(id);
        if (bill == null || isDeleted(bill.getDelFlag()) || isBlank(bill.getBillNo())) {
            return null;
        }
        if (bill.getStatus() == null || bill.getStatus() != STATUS_PASS) {
            return null;
        }
        // 先前各关记下的名目尽数沉底
        List<TFlogSignRow> currents = selectCurrentSignRows(bill.getBillNo());
        for (TFlogSignRow row : currents) {
            row.setCurrentFlag(Integer.valueOf(CURRENT_NO));
            this.flogSignRowMapper.updateById(row);
        }
        // 从核地那一关重新往下累
        bill.setNodeNo(Integer.valueOf(NODE_LAND));
        bill.setStatus(Integer.valueOf(STATUS_RUNNING));
        bill.setRemark("市局打回：" + (isBlank(comment) ? "" : comment.trim()));
        refreshCounts(bill);
        this.flogPermitBillMapper.updateById(bill);
        return this.flogPermitBillMapper.selectById(bill.getId());
    }

    @Override
    public TFlogPermitBill progressOf(Long id) {
        if (id == null) {
            return null;
        }
        TFlogPermitBill bill = this.flogPermitBillMapper.selectById(id);
        if (bill == null || isDeleted(bill.getDelFlag())) {
            return null;
        }
        // 当下压在第几层、还缺几双手，一律照方法的回应摆到屏上
        refreshCounts(bill);
        this.flogPermitBillMapper.updateById(bill);
        return this.flogPermitBillMapper.selectById(bill.getId());
    }

    @Override
    public List<TFlogSignRow> listCurrentSigns(Long id) {
        if (id == null) {
            return new ArrayList<TFlogSignRow>();
        }
        TFlogPermitBill bill = this.flogPermitBillMapper.selectById(id);
        if (bill == null || isBlank(bill.getBillNo())) {
            return new ArrayList<TFlogSignRow>();
        }
        return selectCurrentSignRows(bill.getBillNo());
    }

    /**
     * 一键出证：四关都过完，核签结论转成对外那张许可证文本。
     * 申请人、小班号、地点、面积、蓄积、采伐方式与权属皆取自同一件，不必再请谁誊第二遍；
     * 同一位申请人当年第二条出证的，当场止步。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TFlogPermitText issuePermit(Long id) {
        if (id == null) {
            return null;
        }
        TFlogPermitBill bill = this.flogPermitBillMapper.selectById(id);
        if (bill == null || isDeleted(bill.getDelFlag()) || isBlank(bill.getBillNo())) {
            return null;
        }
        // 四关都过完（章已落到底）才出证
        if (bill.getStatus() == null || bill.getStatus() != STATUS_PASS) {
            return null;
        }
        TFlogApplyCard card = bindCard(bill, null);
        if (card == null) {
            return null;
        }
        // 一件一证：章已落、证已出的不再出
        Integer issued = this.flogPermitTextMapper.selectCount(new QueryWrapper<TFlogPermitText>()
                .eq("bill_no", bill.getBillNo()).eq("status", TEXT_ISSUED).eq("del_flag", 0));
        if (issued != null && issued.intValue() > 0) {
            return null;
        }
        // 同一位申请人当年第二条出证的，当场止步
        int year = Calendar.getInstance().get(Calendar.YEAR);
        Integer sameYear = this.flogPermitTextMapper.selectCount(new QueryWrapper<TFlogPermitText>()
                .eq("applicant_no", card.getSiteNo()).eq("issue_year", year)
                .eq("status", TEXT_ISSUED).eq("del_flag", 0));
        if (sameYear != null && sameYear.intValue() > 0) {
            return null;
        }
        TFlogStandDoc doc = card.getSiteId() == null ? null
                : this.flogStandDocMapper.selectById(card.getSiteId());

        TFlogPermitText text = new TFlogPermitText();
        text.setPermitNo(bill.getBillNo());
        text.setBillNo(bill.getBillNo());
        text.setApplyId(card.getId());
        text.setApplicantNo(card.getSiteNo());
        text.setApplicantName(doc == null ? null : doc.getSiteName());
        text.setSubcompNo(card.getSubcompNo());
        text.setCutBlockNo(card.getCutBlockNo());
        text.setCutMode(card.getCutMode());
        text.setClearTrees(card.getClearTrees());
        text.setQty(card.getQty());
        text.setFineAmt(card.getFineAmt());
        text.setGradeLevel(card.getGradeLevel());
        text.setSiteNo(card.getSiteNo());
        text.setSiteName(doc == null ? null : doc.getSiteName());
        // 各级落过的名目一级一级往回捋，与当日那一屏同一回计算
        text.setSignTrail(buildTrail(bill.getBillNo()));
        text.setIssueYear(Integer.valueOf(year));
        text.setContent(card.getContent());
        text.setStatus(Integer.valueOf(TEXT_ISSUED));
        text.setDelFlag(Integer.valueOf(0));
        text.setRemark("核签结论一键转出");
        this.flogPermitTextMapper.insert(text);
        return text;
    }

    @Override
    public TFlogPermitText selectPermitTextByBillNo(String billNo) {
        if (isBlank(billNo)) {
            return null;
        }
        List<TFlogPermitText> list = this.flogPermitTextMapper.selectList(new QueryWrapper<TFlogPermitText>()
                .eq("bill_no", billNo).eq("del_flag", 0).orderByDesc("id"));
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * 同一件：已挂卡的只认挂上的那一张（递来另一张即非同一件）；
     * 未挂卡的先按 applyCardId 取，取不到再按核签单号找回现行卡（只信字号）。
     */
    private TFlogApplyCard bindCard(TFlogPermitBill bill, Long applyCardId) {
        TFlogApplyCard card = null;
        if (bill.getApplyId() != null) {
            if (applyCardId != null && !applyCardId.equals(bill.getApplyId())) {
                return null;
            }
            card = this.flogApplyCardMapper.selectById(bill.getApplyId());
        } else {
            if (applyCardId != null) {
                card = this.flogApplyCardMapper.selectById(applyCardId);
            }
            if (card == null) {
                List<TFlogApplyCard> cards = this.flogApplyCardMapper.selectList(
                        new QueryWrapper<TFlogApplyCard>()
                                .eq("bill_no", bill.getBillNo()).eq("del_flag", 0)
                                .orderByDesc("current_flag").orderByDesc("node_no"));
                if (!cards.isEmpty()) {
                    card = cards.get(0);
                }
            }
        }
        if (card == null || isDeleted(card.getDelFlag())) {
            return null;
        }
        return card;
    }

    /**
     * 头一关收件：申请人是谁、小班号与权属、拟采地点与方式、文本打算怎么写，
     * 四样少一样，门都进不来。
     */
    private boolean intakePass(TFlogApplyCard card) {
        if (isBlank(card.getSiteNo())) {
            return false;
        }
        if (isBlank(card.getSubcompNo()) || card.getSiteId() == null) {
            return false;
        }
        if (isBlank(card.getCutBlockNo()) || card.getCutMode() == null) {
            return false;
        }
        if (isBlank(card.getContent())) {
            return false;
        }
        return true;
    }

    /**
     * 核地：把在册底档同申领材料并排看，只信字号不信人——申请人姓名对得一字不差也白搭；
     * 这一片小班若还压着没勾销完的事务卡，件便卡在这一关，往后凑得再齐也不放行。
     */
    private boolean landPass(TFlogApplyCard card) {
        if (card.getCurrentFlag() != null && card.getCurrentFlag() == 0) {
            return false;
        }
        if (card.getStatus() != null && card.getStatus() == 2) {
            return false;
        }
        if (card.getSiteId() == null) {
            return false;
        }
        TFlogStandDoc doc = this.flogStandDocMapper.selectById(card.getSiteId());
        if (doc == null || isDeleted(doc.getDelFlag())) {
            return false;
        }
        if (doc.getStatus() == null || doc.getStatus() != 0) {
            return false;
        }
        if (doc.getSiteNo() == null || !doc.getSiteNo().equals(card.getSiteNo())) {
            return false;
        }
        Integer open = this.flogCloseCardMapper.selectCount(new QueryWrapper<TFlogCloseCard>()
                .and(w -> w.eq("biz_no", card.getSubcompNo()).or().eq("biz_no", card.getCutBlockNo()))
                .and(w -> w.isNull("status").or().ne("status", CLOSE_DONE))
                .eq("del_flag", 0));
        return open == null || open.intValue() == 0;
    }

    /** 本级应签几双手：并排签一关两名点齐，其余各关一名 */
    private int needOf(int node) {
        return node == NODE_PARALLEL ? 2 : 1;
    }

    /**
     * 这一关在流水里凑齐了几双手：按现行核签名目逐笔记名去重累出来。
     * signer 不空时只问这只手在不在（替人签判定）；空则数这一关已签人数。
     */
    private int countSigns(String billNo, int node, String signer) {
        List<TFlogSignRow> rows = this.flogSignRowMapper.selectList(new QueryWrapper<TFlogSignRow>()
                .eq("bill_no", billNo).eq("node_no", node).eq("act_type", ACT_SIGN)
                .eq("current_flag", CURRENT_YES).eq("del_flag", 0));
        Set<String> names = new HashSet<String>();
        for (TFlogSignRow r : rows) {
            if (!isBlank(r.getSigner())) {
                names.add(r.getSigner().trim());
            }
        }
        if (signer != null) {
            return names.contains(signer.trim()) ? 1 : 0;
        }
        return names.size();
    }

    /** 所在级次、同级办法、应签/已签：一律照流水现算，落库与摆出同一回计算 */
    private void refreshCounts(TFlogPermitBill bill) {
        int node = bill.getNodeNo() == null ? NODE_INTAKE : bill.getNodeNo();
        bill.setSignMode(Integer.valueOf(node == NODE_PARALLEL ? MODE_AND : MODE_OR));
        bill.setNeedCount(Integer.valueOf(needOf(node)));
        bill.setSignCount(isBlank(bill.getBillNo()) ? Integer.valueOf(0)
                : Integer.valueOf(countSigns(bill.getBillNo(), node, null)));
    }

    /** 各级现行名目（一级一级往回捋；沉底的不与后补的并列） */
    private List<TFlogSignRow> selectCurrentSignRows(String billNo) {
        return this.flogSignRowMapper.selectList(new QueryWrapper<TFlogSignRow>()
                .eq("bill_no", billNo).eq("current_flag", CURRENT_YES).eq("del_flag", 0)
                .orderByAsc("node_no").orderByAsc("id"));
    }

    /** 名目链：收件：甲；核地：乙；并排签：丙、丁；用章：戊 */
    private String buildTrail(String billNo) {
        List<TFlogSignRow> rows = selectCurrentSignRows(billNo);
        StringBuilder sb = new StringBuilder();
        for (int n = NODE_INTAKE; n <= NODE_SEAL; n++) {
            StringBuilder names = new StringBuilder();
            for (TFlogSignRow r : rows) {
                if (r.getNodeNo() != null && r.getNodeNo() == n
                        && r.getActType() != null && r.getActType() == ACT_SIGN
                        && !isBlank(r.getSigner())) {
                    if (names.length() > 0) {
                        names.append("、");
                    }
                    names.append(r.getSigner().trim());
                }
            }
            if (names.length() > 0) {
                if (sb.length() > 0) {
                    sb.append("；");
                }
                sb.append(NODE_NAMES[n]).append("：").append(names);
            }
        }
        return sb.toString();
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static boolean isDeleted(Integer delFlag) {
        return delFlag != null && delFlag == 1;
    }
}
