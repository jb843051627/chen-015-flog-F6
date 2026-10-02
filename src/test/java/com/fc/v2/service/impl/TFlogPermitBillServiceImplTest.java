package com.fc.v2.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

/**
 * 采伐许可证四关签法业务层测试：
 * 收件四样/超线附材料、核地只信字号/事务卡压件、并排签两笔与替签、
 * 手数由流水同算、当年第二证止步、落印封死、不予出证止步、
 * 市局打回沉底从核地重累、文本同源、旧一路签法用法不变。
 *
 * @author fuce
 * @date 2026-10-02
 */
@ExtendWith(MockitoExtension.class)
class TFlogPermitBillServiceImplTest {

    @Mock
    private TFlogPermitBillMapper billMapper;
    @Mock
    private TFlogPermitSignMapper signMapper;
    @Mock
    private TFlogStandDocMapper standDocMapper;
    @Mock
    private TFlogApplyCardMapper applyCardMapper;
    @Mock
    private TFlogCloseCardMapper closeCardMapper;

    @InjectMocks
    private TFlogPermitBillServiceImpl service;

    /** 内存流水表：insert 追加、selectList 按 wrapper 里的 eq 条件过滤 */
    private final List<TFlogPermitSign> signTable = new ArrayList<>();
    private long seqId = 1000L;
    private long billSeq = 100L;

    @BeforeEach
    void setUp() {
        signTable.clear();

        // 单据插入即补号（每件一号，互不撞），selectById 回内存里那件
        lenient().when(billMapper.insert(any(TFlogPermitBill.class))).thenAnswer(inv -> {
            TFlogPermitBill b = inv.getArgument(0);
            if (b.getId() == null) {
                b.setId(++billSeq);
            }
            return 1;
        });
        lenient().when(billMapper.updateById(any(TFlogPermitBill.class))).thenReturn(1);
        lenient().when(billMapper.update(isNull(), any(UpdateWrapper.class))).thenReturn(1);
        // 默认：单号不重、当年无已发证
        lenient().when(billMapper.selectCount(any())).thenReturn(0);

        lenient().when(signMapper.insert(any(TFlogPermitSign.class))).thenAnswer(inv -> {
            TFlogPermitSign s = inv.getArgument(0);
            s.setId(++seqId);
            signTable.add(s);
            return 1;
        });
        lenient().when(signMapper.selectList(any())).thenAnswer(inv -> {
            QueryWrapper<TFlogPermitSign> w = inv.getArgument(0);
            return filterSigns(w);
        });
        // 核地默认：字号在册、申领材料现行已核对、没有压着的事务卡
        lenient().when(standDocMapper.selectOne(any())).thenReturn(activeDoc());
        lenient().when(applyCardMapper.selectOne(any())).thenReturn(checkedCard());
        lenient().when(closeCardMapper.selectList(any())).thenReturn(new ArrayList<>());
    }

    /* ------------------------------ 头一关 收件 ------------------------------ */

    @Test
    void receive_fourItemsAllRequired() {
        TFlogPermitBill draft = validDraft();
        draft.setCutPlace(null);
        assertNull(service.approve(draft, "收件员甲", "缺地点"), "拟采地点缺一样，门都进不来");
        verify(billMapper, never()).insert(any());

        draft = validDraft();
        draft.setApplicant("  ");
        assertNull(service.approve(draft, "收件员甲", "缺申请人"));

        draft = validDraft();
        draft.setCutMode(9);
        assertNull(service.approve(draft, "收件员甲", "采伐方式不对"));

        draft = validDraft();
        TFlogPermitBill saved = service.approve(draft, "收件员甲", "收件齐");
        assertNotNull(saved);
        assertEquals(1, saved.getNodeNo(), "收件落名后件摆到核地关");
        assertEquals(0, saved.getStatus());
        assertEquals(1, saved.getRoundNo());
        assertEquals(1, signTable.size(), "收件关落一笔流水");
        assertEquals(0, signTable.get(0).getNodeNo());    }

    @Test
    void receive_overProvincialLineMustAttachSurvey() {
        TFlogPermitBill draft = validDraft();
        draft.setCutQty(new BigDecimal("300.01")); // 超省级蓄积线
        draft.setSurveyFlag(null);
        assertNull(service.approve(draft, "收件员甲", "超线未附调查设计"), "超蓄积线未附伐区调查设计材料，进不了门");

        draft.setSurveyFlag(1);
        assertNotNull(service.approve(draft, "收件员甲", "附了材料"));

        TFlogPermitBill draft2 = validDraft();
        draft2.setCutArea(new BigDecimal("10.01")); // 超省级面积线
        draft2.setSurveyFlag(0);
        assertNull(service.approve(draft2, "收件员甲", "超面积线未附"));

        // 恰等于线不算超，不必附
        TFlogPermitBill draft3 = validDraft();
        draft3.setCutQty(new BigDecimal("300.00"));
        draft3.setSurveyFlag(null);
        assertNotNull(service.approve(draft3, "收件员甲", "压线不超"));
    }

    @Test
    void receive_duplicateBillNoRejected() {
        when(billMapper.selectCount(any())).thenReturn(1);
        assertNull(service.approve(validDraft(), "收件员甲", "重号"));
    }

    /* ------------------------------ 第二关 核地 ------------------------------ */

    @Test
    void verify_trustsSiteNoNotName() {
        TFlogPermitBill bill = runningAt(1);
        bill.setApplicant("王老五"); // 姓名对得上对不上都不作数
        TFlogPermitBill r = service.approve(bill.getId(), "核地员A", "字号对", null);
        assertNotNull(r);
        assertEquals(2, r.getNodeNo(), "字号配小班核得过，摆到并排签关");
        // 底档与材料都是按字号去对的，从不按申请人姓名
        verify(standDocMapper).selectOne(any());
        verify(applyCardMapper).selectOne(any());
    }

    @Test
    void verify_siteNoMismatchBlocks() {
        when(standDocMapper.selectOne(any())).thenReturn(null);
        assertNull(service.approve(runningAt(1).getId(), "核地员A", "字号查无", null));

        TFlogStandDoc revoked = activeDoc();
        revoked.setStatus(1); // 已收回
        when(standDocMapper.selectOne(any())).thenReturn(revoked);
        assertNull(service.approve(runningAt(1).getId(), "核地员A", "底档收回", null));
    }

    @Test
    void verify_missingCurrentCheckedMaterialBlocks() {
        when(applyCardMapper.selectOne(any())).thenReturn(null);
        assertNull(service.approve(runningAt(1).getId(), "核地员A", "无现行材料", null));

        TFlogApplyCard unchecked = checkedCard();
        unchecked.setStatus(0); // 待核对
        when(applyCardMapper.selectOne(any())).thenReturn(unchecked);
        assertNull(service.approve(runningAt(1).getId(), "核地员A", "材料没核对", null));
    }

    @Test
    void verify_pendingCloseCardOnSubcompBlocksForeverUntilCleared() {
        TFlogCloseCard pending = new TFlogCloseCard();
        pending.setBizNo("GS-7");
        pending.setSiteNo("DC00");
        pending.setSubcompNo("XB-1");
        pending.setStatus(1); // 在办，没勾销完
        when(closeCardMapper.selectList(any())).thenReturn(Arrays.asList(pending));
        assertNull(service.approve(runningAt(1).getId(), "核地员A", "材料再齐也不放行", null));

        // 事务卡了断后，同一片小班才放
        pending.setStatus(2);
        TFlogPermitBill r = service.approve(runningAt(1).getId(), "核地员A", "勾销已清", null);
        assertNotNull(r);
        assertEquals(2, r.getNodeNo());
    }

    @Test
    void verify_doesNotAcceptParallelSignRole() {
        // 核地关不带分管口
        assertNull(service.approve(runningAt(1).getId(), "核地员A", "乱带口", 1));
    }

    /* ------------------------------ 第三关 并排签 ------------------------------ */

    @Test
    void parallel_twoDifferentHandsBothRolesRequired() {
        TFlogPermitBill bill = runningAt(2);

        // 第一位（盯蓄积强度）落完，件仍停在本关，够不着用章
        TFlogPermitBill one = service.approve(bill.getId(), "分管钱某", "蓄积在本场本年面子里", 1);
        assertNotNull(one);
        assertEquals(2, one.getNodeNo());
        assertEquals(1, rows(bill.getId(), 2, 1, 1).size());

        // 同一分管同一人再签，刷不出第二笔
        TFlogPermitBill dup = service.approve(bill.getId(), "分管钱某", "又签一回", 1);
        assertNotNull(dup);
        assertEquals(2, dup.getNodeNo());
        assertEquals(1, rows(bill.getId(), 2, 1, 1).size());

        // 第二位（盯权属文本）落齐，两双手掐着走，件摆到用章关
        TFlogPermitBill two = service.approve(bill.getId(), "分管孙某", "权属文本对得拢", 2);
        assertNotNull(two);
        assertEquals(3, two.getNodeNo());
        assertEquals(2, rows(bill.getId(), 2, 1, 1).size());
        // 手数由流水累出：并排关两双手（用台账回看，单据已摆到用章关）
        when(billMapper.selectById(bill.getId())).thenReturn(two);
        assertEquals(2, service.ledger(bill.getId()).getNodes().get(2).getSignCount());
    }

    @Test
    void parallel_sameHandCoveringBothRolesNotEnough() {
        TFlogPermitBill bill = runningAt(2);
        service.approve(bill.getId(), "分管钱某", "管口1", 1);
        TFlogPermitBill r = service.approve(bill.getId(), "分管钱某", "同一双手又管口2", 2);
        assertEquals(2, r.getNodeNo(), "只一双手，两口都占也不算齐");
        assertEquals(1, distinctHands(bill.getId(), 2), "替不了第二双，distinct 落名人只一双");
    }

    @Test
    void parallel_sameMinuteByTwoPeopleIsTwoRows() {
        TFlogPermitBill bill = runningAt(2);
        service.approve(bill.getId(), "分管钱某", "同分钟", 1);
        service.approve(bill.getId(), "分管孙某", "同分钟", 2);
        assertEquals(2, rows(bill.getId(), 2, 1, 1).size(), "掐同一分钟也是两笔");
        assertEquals(2, distinctHands(bill.getId(), 2));
    }

    @Test
    void parallel_mustNameRole() {
        TFlogPermitBill bill = runningAt(2);
        assertNull(service.approve(bill.getId(), "分管钱某", "不报分管口", null));
        assertNull(service.approve(bill.getId(), "分管钱某", "乱口", 3));
    }

    @Test
    void proxySignatureCountsAsNothing() {
        TFlogPermitBill bill = runningAt(2);
        // “替人签”：落名带代/替字样，一律当没签，这一笔不落
        assertNull(service.approve(bill.getId(), "赵某代钱某", "代签", 1), "替签驳回，不当落名");
        assertNull(service.approve(bill.getId(), "孙某替", "替签", 2));
        assertEquals(0, rows(bill.getId(), 2, null, 1).size(), "替签一笔都不进流水");
    }

    @Test
    void refusalAtAnyNodeStopsBillForever() {
        TFlogPermitBill bill = runningAt(2);
        TFlogPermitBill refused = service.reject(bill.getId(), "分管钱某", "强度顺位不合规程", 1);
        assertNotNull(refused);
        assertEquals(2, refused.getStatus(), "写下不予出证");

        assertNull(service.approve(bill.getId(), "分管孙某", "过后想追一枚", 2), "止步后谁也追不进");
        assertNull(service.reject(bill.getId(), "分管孙某", "再拒也不收", 2));
        assertEquals(1, rows(bill.getId(), 2, null, 1).size(), "只留下不予出证那一笔");
    }

    /* ------------------------------ 第四关 用章 ------------------------------ */

    @Test
    void seal_issuesPermitAndLocksBill() {
        TFlogPermitBill bill = runningAt(3);
        TFlogPermitBill sealed = service.approve(bill.getId(), "本站用章员", "用印报市局", null);
        assertNotNull(sealed);
        assertEquals(1, sealed.getStatus());
        assertNotNull(sealed.getPermitNo());
        assertNotNull(sealed.getSealTime());
        assertNotNull(sealed.getPermitText());
        assertTrue(sealed.getPermitText().contains("王老五"), "文本取自同一件：申请人");
        assertTrue(sealed.getPermitText().contains("120.00"), "文本取自同一件：蓄积");
        assertTrue(sealed.getPermitText().contains("皆伐"), "文本取自同一件：采伐方式");

        // 章一落到底，各栏封死，要动只能另起一件
        assertNull(service.approve(bill.getId(), "谁", "补签", null));
        assertNull(service.reject(bill.getId(), "谁", "翻案", null));
    }

    @Test
    void seal_secondPermitSameApplicantSameYearStopsOnSpot() {
        TFlogPermitBill bill = runningAt(3);
        when(billMapper.selectCount(any())).thenReturn(1); // 同申请人当年已有一证
        assertNull(service.approve(bill.getId(), "用章员", "第二条", null), "当年第二条当场止步");
        assertEquals(0, rows(bill.getId(), 3, 1, 1).size(), "止步件不留用章落名");
    }

    @Test
    void sealedBillCannotBeApprovedViaAnyNode() {
        TFlogPermitBill bill = runningAt(3);
        bill.setStatus(1);
        assertNull(service.approve(bill.getId(), "用章员", "封死后", null));
    }

    /* ------------------------------ 市局打回沉底 ------------------------------ */

    @Test
    void citySendBack_sinksAllSignsAndRestartsAtVerify() {
        TFlogPermitBill bill = sealedBill();
        UpdateWrapper<TFlogPermitBill> captor = doSendBackAndCapture(bill);

        assertEquals(2, bill.getRoundNo(), "进新一轮");
        assertEquals(1, bill.getNodeNo(), "从核地关重新往下累");
        assertEquals(3, bill.getStatus());
        String sqlSet = captor.getSqlSet();
        assertTrue(sqlSet.contains("permit_no"), "旧证号在库里显式作废");
        assertTrue(sqlSet.contains("seal_time"), "旧用章时间清掉");
        assertTrue(sqlSet.contains("permit_text"), "旧文本清掉，作废与后补不并列");
        assertTrue(captor.getParamNameValuePairs().containsValue(null), "作废字段确实置空");

        // 打回这一笔记在旧轮
        TFlogPermitSign sendBackRow = signTable.get(signTable.size() - 1);
        assertEquals(1, sendBackRow.getRoundNo());
        assertEquals(3, sendBackRow.getSignResult());

        // 新轮核地重新过：当轮流水从零累
        TFlogPermitBill again = service.approve(bill.getId(), "核地员B", "重核", null);
        assertNotNull(again);
        assertEquals(2, again.getNodeNo());
        assertEquals(2, signTable.get(signTable.size() - 1).getRoundNo(), "后补的记在新轮（第2轮）");

        PermitLedgerVo ledger = service.ledger(bill.getId());
        assertEquals(1, ledger.getSunkRounds().size(), "旧轮名目沉底留痕");
        assertEquals(2, ledger.getCurrentNode());
        // 当轮各关只摆新轮的名目（收件关当轮必空），旧轮名目只在沉底处可见
        assertEquals(0, ledger.getNodes().get(0).getSigns().size(), "新轮没收件名目，不与旧轮并列");
        assertEquals(1, ledger.getNodes().get(1).getSigns().size(), "新轮核地重新落的那一笔");
        long currentRoundRows = signTable.stream().filter(s -> s.getRoundNo() == 2).count();
        long oldRoundRows = signTable.stream().filter(s -> s.getRoundNo() == 1).count();
        assertTrue(currentRoundRows > 0 && oldRoundRows > 0, "新旧轮各自留账，不并在一处");
    }

    @Test
    void citySendBack_onlySealedBillAllowed() {
        assertNull(service.rollback(runningAt(2).getId(), "市局某", "在核件打不得回"), "在核件市局收都没收到");
        assertNull(service.rollback(sealedBill().getId(), "  ", "没落名也不行"));
    }

    /* ------------------------------ 台账：手数同算、逐级回捋 ------------------------------ */

    @Test
    void ledger_reflectsLayerAndLackFromFlowOnly() {
        TFlogPermitBill bill = runningAt(2);
        service.approve(bill.getId(), "分管钱某", "蓄积", 1);

        when(billMapper.selectById(bill.getId())).thenReturn(bill);
        PermitLedgerVo vo = service.ledger(bill.getId());
        assertEquals(2, vo.getCurrentNode());
        assertEquals("并排签", vo.getCurrentNodeName());
        assertEquals(2, vo.getNeedCount());
        assertEquals(1, vo.getSignCount(), "摆给人看的数出自流水同一回 count");
        assertEquals(1, vo.getLackCount(), "还缺一双手");
        assertTrue(vo.getBlockReason().contains("1"));
        // 库里单据的 sign_count 与屏上同一个算法
        assertEquals(vo.getSignCount(), bill.getSignCount());
    }

    @Test
    void ledger_listsEveryNodeForBackTracking() {
        TFlogPermitBill bill = runningAt(1);
        when(billMapper.selectById(bill.getId())).thenReturn(bill);
        PermitLedgerVo vo = service.ledger(bill.getId());
        assertEquals(4, vo.getNodes().size(), "四关一级一级往回捋");
        assertEquals("收件", vo.getNodes().get(0).getNodeName());
        assertEquals("核地", vo.getNodes().get(1).getNodeName());
        assertEquals("并排签", vo.getNodes().get(2).getNodeName());
        assertEquals("用章", vo.getNodes().get(3).getNodeName());
        // 头一关收件那笔在流水里，经得起复核
        assertEquals(1, vo.getNodes().get(0).getSignCount());
    }

    @Test
    void permitText_singleSourceBeforeAndAfterSeal() {
        TFlogPermitBill bill = runningAt(2);
        String preview = service.permitText(bill.getId());
        assertTrue(preview.contains("王老五"));
        assertTrue(preview.contains("XB-1"));
        assertTrue(preview.contains("青屏县东沟"));
        assertTrue(preview.contains("更新措施"));

        TFlogPermitBill sealed = service.approve(runningAt(3).getId(), "用章员", "落印", null);
        when(billMapper.selectById(sealed.getId())).thenReturn(sealed);
        assertEquals(sealed.getPermitText(), service.permitText(sealed.getId()), "落印前后两处端出的数一样粗");
    }

    /* ------------------------------ 旧一路签法：用法原样不动 ------------------------------ */

    @Test
    void legacyApprove_keepsOldBehavior() {
        TFlogPermitBill bill = legacyBill(0, 0);
        when(billMapper.selectById(bill.getId())).thenReturn(bill);
        TFlogPermitBill r = service.approve(bill.getId(), "旧签人", "旧法");
        assertEquals(1, r.getNodeNo());
        assertEquals(0, r.getStatus(), "旧法：未到顶仍在核");

        bill = legacyBill(2, 0);
        when(billMapper.selectById(bill.getId())).thenReturn(bill);
        r = service.approve(bill.getId(), "旧签人", "旧法到顶");
        assertEquals(3, r.getNodeNo());
        assertEquals(1, r.getStatus(), "旧法：到顶置已核讫");

        // 旧法的空签名校验仍在
        assertNull(service.approve(legacyBill(0, 0).getId(), "", "x"));
    }

    @Test
    void legacyRejectAndRollback_keepOldBehavior() {
        TFlogPermitBill bill = legacyBill(1, 0);
        when(billMapper.selectById(bill.getId())).thenReturn(bill);
        assertNotNull(service.reject(bill.getId(), "旧签人", "旧法否决"));

        when(billMapper.selectById(bill.getId())).thenReturn(bill);
        TFlogPermitBill back = service.rollback(bill.getId(), "旧法退回");
        assertEquals(0, back.getNodeNo());

        TFlogPermitBill bottom = legacyBill(0, 0);
        when(billMapper.selectById(bottom.getId())).thenReturn(bottom);
        assertEquals(0, service.rollback(bottom.getId(), "到底不退穿").getNodeNo());
    }

    /* ------------------------------ 夹具 ------------------------------ */

    private TFlogPermitBill validDraft() {
        TFlogPermitBill b = new TFlogPermitBill();
        b.setBillNo("HJ-2026-001");
        b.setApplicant("王老五");
        b.setApplyYear(2026);
        b.setSiteNo("DC00");
        b.setSubcompNo("XB-1");
        b.setOwnerName("自留山");
        b.setCutPlace("青屏县东沟");
        b.setCutMode(1); // 皆伐
        b.setForestKind("人工杉木林");
        b.setCutArea(new BigDecimal("2.50"));
        b.setCutQty(new BigDecimal("120.00"));
        b.setRenewMeasure("皆伐后当年人工更新杉木");
        b.setSurveyFlag(0);
        return b;
    }

    private TFlogStandDoc activeDoc() {
        TFlogStandDoc d = new TFlogStandDoc();
        d.setId(0L);
        d.setSiteNo("DC00");
        d.setSiteName("青屏国有林场在册底档");
        d.setStatus(0);
        return d;
    }

    private TFlogApplyCard checkedCard() {
        TFlogApplyCard c = new TFlogApplyCard();
        c.setId(200L);
        c.setSiteNo("DC00");
        c.setSubcompNo("XB-1");
        c.setCurrentFlag(1);
        c.setStatus(1); // 已核对
        return c;
    }

    /** 起一件、过完收件，摆到指定关；selectById 始终回这一件 */
    private TFlogPermitBill runningAt(int node) {
        TFlogPermitBill b = validDraft();
        service.approve(b, "收件员甲", "收件齐"); // 收件落名，到核地
        lenient().when(billMapper.selectById(b.getId())).thenReturn(b);
        while (b.getNodeNo() < node) {
            int n = b.getNodeNo();
            if (n == 1) {
                service.approve(b.getId(), "核地员A", "核地过", null);
            } else if (n == 2) {
                service.approve(b.getId(), "分管钱某", "蓄积", 1);
                service.approve(b.getId(), "分管孙某", "权属", 2);
            }
        }
        b.setStatus(0);
        // 到了目标关，把目标关及更后关在“推进路上”写下的流水抹掉：
        // 本关手数由各用例自己当场落，收件/核地的前序流水留着供逐级回捋。
        signTable.removeIf(s -> s.getBillId().equals(b.getId()) && s.getNodeNo() >= node);
        b.setSignCount(0);
        return b;
    }

    private TFlogPermitBill sealedBill() {
        TFlogPermitBill b = runningAt(3);
        TFlogPermitBill sealed = service.approve(b.getId(), "用章员", "落印报市局", null);
        assertNotNull(sealed);
        return sealed;
    }

    private List<TFlogPermitSign> rows(Long billId, Integer node, Integer result, Integer round) {
        List<TFlogPermitSign> out = new ArrayList<>();
        for (TFlogPermitSign s : signTable) {
            if (!s.getBillId().equals(billId)) {
                continue;
            }
            if (node != null && !node.equals(s.getNodeNo())) {
                continue;
            }
            if (result != null && !result.equals(s.getSignResult())) {
                continue;
            }
            if (round != null && !round.equals(s.getRoundNo())) {
                continue;
            }
            out.add(s);
        }
        return out;
    }

    private long distinctHands(Long billId, int node) {
        return rows(billId, node, 1, 1).stream().map(TFlogPermitSign::getSigner).distinct().count();
    }

    private TFlogPermitBill legacyBill(int node, int status) {
        TFlogPermitBill b = new TFlogPermitBill();
        b.setId(++seqId);
        b.setBillNo("OLD-" + seqId);
        b.setNodeNo(node);
        b.setStatus(status);
        b.setDelFlag(0);
        lenient().when(billMapper.selectById(b.getId())).thenReturn(b);
        return b;
    }

    @SuppressWarnings("unchecked")
    private UpdateWrapper<TFlogPermitBill> doSendBackAndCapture(TFlogPermitBill bill) {
        TFlogPermitBill r = service.rollback(bill.getId(), "市局复核员", "表述不合规矩");
        assertNotNull(r);
        ArgumentCaptor<UpdateWrapper<TFlogPermitBill>> cap = ArgumentCaptor.forClass(UpdateWrapper.class);
        verify(billMapper).update(isNull(), cap.capture());
        return cap.getValue();
    }

    private long distinctHands() {
        return signTable.stream().map(TFlogPermitSign::getSigner).distinct().count();
    }

    /** 按 QueryWrapper 中 eq 列条件过滤内存流水（列名→字段名） */
    private List<TFlogPermitSign> filterSigns(QueryWrapper<TFlogPermitSign> w) {
        Map<String, Object> cond = eqConditions(w);
        List<TFlogPermitSign> out = new ArrayList<>();
        for (TFlogPermitSign s : signTable) {
            if (matches(s, cond)) {
                out.add(s);
            }
        }
        return out;
    }

    private Map<String, Object> eqConditions(QueryWrapper<?> w) {
        String segment = w.getSqlSegment();
        Map<String, Object> params = w.getParamNameValuePairs();
        Map<String, Object> cond = new LinkedHashMap<>();
        Matcher m = Pattern.compile("([a-z_]+)\\s*=\\s*#\\{ew\\.paramNameValuePairs\\.(MPGENVAL\\d+)}")
                .matcher(segment);
        while (m.find()) {
            cond.put(m.group(1), params.get(m.group(2)));
        }
        return cond;
    }

    private boolean matches(TFlogPermitSign s, Map<String, Object> cond) {
        for (Map.Entry<String, Object> e : cond.entrySet()) {
            Object v;
            try {
                Field f = TFlogPermitSign.class.getDeclaredField(columnToField(e.getKey()));
                f.setAccessible(true);
                v = f.get(s);
            } catch (NoSuchFieldException | IllegalAccessException ex) {
                return false;
            }
            if (v == null || !v.equals(e.getValue())) {
                return false;
            }
        }
        return true;
    }

    private String columnToField(String column) {
        StringBuilder sb = new StringBuilder();
        for (String part : column.split("_")) {
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        sb.setCharAt(0, Character.toLowerCase(sb.charAt(0)));
        return sb.toString();
    }
}
