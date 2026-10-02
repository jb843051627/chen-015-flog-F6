package com.fc.v2.model.custom;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fc.v2.model.auto.TFlogPermitBill;
import com.fc.v2.model.auto.TFlogPermitSign;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 核签台账回应：屏上摆的层数、还缺几双手、各级名目，全部由业务服务层按方法回应现算，
 * 不许先摆层数再回头凑名目。手数与流水出自同一回计算。
 *
 * @author fuce
 * @date 2026-10-02
 */
@ApiModel(value = "PermitLedgerVo", description = "采伐许可证核签台账回应")
public class PermitLedgerVo implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 核签单（申请要素与许可证文本同此一件） */
    @ApiModelProperty(value = "核签单")
    private TFlogPermitBill bill;

    /** 当前所在关 0收件 1核地 2并排签 3用章 */
    @ApiModelProperty(value = "当前所在关")
    private Integer currentNode;

    /** 当前关名 */
    @ApiModelProperty(value = "当前关名")
    private String currentNodeName;

    /** 本关应到手数 */
    @ApiModelProperty(value = "本关应到手数")
    private Integer needCount;

    /** 本关已到手数（由当轮流水 distinct 落名人实时 count，界面无代填格） */
    @ApiModelProperty(value = "本关已到手数")
    private Integer signCount;

    /** 本关还缺手数 */
    @ApiModelProperty(value = "本关还缺手数")
    private Integer lackCount;

    /** 报批情形 0在核 1已用印发证 2不予出证 3市局打回重报 */
    @ApiModelProperty(value = "报批情形")
    private Integer status;

    /** 情形名 */
    @ApiModelProperty(value = "情形名")
    private String statusName;

    /** 当轮逐关名目（一级一级往回捋，与当日那一屏对得上号） */
    @ApiModelProperty(value = "当轮逐关名目")
    private List<NodeLedger> nodes = new ArrayList<>();

    /** 已沉底的旧轮（市局打回前的名目，留痕但不与当轮并列） */
    @ApiModelProperty(value = "已沉底旧轮")
    private List<RoundLedger> sunkRounds = new ArrayList<>();

    /** 卡件缘由（进不了门/压在核地/手不够等，照服务回应摆） */
    @ApiModelProperty(value = "卡件缘由")
    private String blockReason;

    public TFlogPermitBill getBill() {
        return bill;
    }

    public void setBill(TFlogPermitBill bill) {
        this.bill = bill;
    }

    public Integer getCurrentNode() {
        return currentNode;
    }

    public void setCurrentNode(Integer currentNode) {
        this.currentNode = currentNode;
    }

    public String getCurrentNodeName() {
        return currentNodeName;
    }

    public void setCurrentNodeName(String currentNodeName) {
        this.currentNodeName = currentNodeName;
    }

    public Integer getNeedCount() {
        return needCount;
    }

    public void setNeedCount(Integer needCount) {
        this.needCount = needCount;
    }

    public Integer getSignCount() {
        return signCount;
    }

    public void setSignCount(Integer signCount) {
        this.signCount = signCount;
    }

    public Integer getLackCount() {
        return lackCount;
    }

    public void setLackCount(Integer lackCount) {
        this.lackCount = lackCount;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getStatusName() {
        return statusName;
    }

    public void setStatusName(String statusName) {
        this.statusName = statusName;
    }

    public List<NodeLedger> getNodes() {
        return nodes;
    }

    public void setNodes(List<NodeLedger> nodes) {
        this.nodes = nodes;
    }

    public List<RoundLedger> getSunkRounds() {
        return sunkRounds;
    }

    public void setSunkRounds(List<RoundLedger> sunkRounds) {
        this.sunkRounds = sunkRounds;
    }

    public String getBlockReason() {
        return blockReason;
    }

    public void setBlockReason(String blockReason) {
        this.blockReason = blockReason;
    }

    /** 一关的名目账 */
    @ApiModel(value = "NodeLedger", description = "一关的名目账")
    public static class NodeLedger implements Serializable {
        private static final long serialVersionUID = 1L;

        @ApiModelProperty(value = "关序 0收件 1核地 2并排签 3用章")
        private Integer nodeNo;

        @ApiModelProperty(value = "关名")
        private String nodeName;

        @ApiModelProperty(value = "应到手数")
        private Integer needCount;

        @ApiModelProperty(value = "已到手数")
        private Integer signCount;

        @ApiModelProperty(value = "本关逐笔流水")
        private List<SigItem> signs = new ArrayList<>();

        public Integer getNodeNo() {
            return nodeNo;
        }

        public void setNodeNo(Integer nodeNo) {
            this.nodeNo = nodeNo;
        }

        public String getNodeName() {
            return nodeName;
        }

        public void setNodeName(String nodeName) {
            this.nodeName = nodeName;
        }

        public Integer getNeedCount() {
            return needCount;
        }

        public void setNeedCount(Integer needCount) {
            this.needCount = needCount;
        }

        public Integer getSignCount() {
            return signCount;
        }

        public void setSignCount(Integer signCount) {
            this.signCount = signCount;
        }

        public List<SigItem> getSigns() {
            return signs;
        }

        public void setSigns(List<SigItem> signs) {
            this.signs = signs;
        }
    }

    /** 一笔落名 */
    @ApiModel(value = "SigItem", description = "一笔落名")
    public static class SigItem implements Serializable {
        private static final long serialVersionUID = 1L;

        @ApiModelProperty(value = "落名人")
        private String signer;

        @ApiModelProperty(value = "并排签分管口 1蓄积强度 2权属文本")
        private Integer signRole;

        @ApiModelProperty(value = "名目 1通过 2不予出证 3市局打回")
        private Integer signResult;

        @ApiModelProperty(value = "意见")
        private String opinion;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        @ApiModelProperty(value = "落名时刻")
        private Date signTime;

        public SigItem() {
        }

        public SigItem(TFlogPermitSign s) {
            this.signer = s.getSigner();
            this.signRole = s.getSignRole();
            this.signResult = s.getSignResult();
            this.opinion = s.getOpinion();
            this.signTime = s.getCreateTime();
        }

        public String getSigner() {
            return signer;
        }

        public void setSigner(String signer) {
            this.signer = signer;
        }

        public Integer getSignRole() {
            return signRole;
        }

        public void setSignRole(Integer signRole) {
            this.signRole = signRole;
        }

        public Integer getSignResult() {
            return signResult;
        }

        public void setSignResult(Integer signResult) {
            this.signResult = signResult;
        }

        public String getOpinion() {
            return opinion;
        }

        public void setOpinion(String opinion) {
            this.opinion = opinion;
        }

        public Date getSignTime() {
            return signTime;
        }

        public void setSignTime(Date signTime) {
            this.signTime = signTime;
        }
    }

    /** 一个沉底旧轮的留痕 */
    @ApiModel(value = "RoundLedger", description = "沉底旧轮留痕")
    public static class RoundLedger implements Serializable {
        private static final long serialVersionUID = 1L;

        @ApiModelProperty(value = "轮次")
        private Integer roundNo;

        @ApiModelProperty(value = "打回落名人")
        private String rejecter;

        @ApiModelProperty(value = "打回意见")
        private String opinion;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        @ApiModelProperty(value = "打回时刻")
        private Date rejectTime;

        @ApiModelProperty(value = "旧轮逐笔流水（沉底不并列）")
        private List<SigItem> signs = new ArrayList<>();

        public Integer getRoundNo() {
            return roundNo;
        }

        public void setRoundNo(Integer roundNo) {
            this.roundNo = roundNo;
        }

        public String getRejecter() {
            return rejecter;
        }

        public void setRejecter(String rejecter) {
            this.rejecter = rejecter;
        }

        public String getOpinion() {
            return opinion;
        }

        public void setOpinion(String opinion) {
            this.opinion = opinion;
        }

        public Date getRejectTime() {
            return rejectTime;
        }

        public void setRejectTime(Date rejectTime) {
            this.rejectTime = rejectTime;
        }

        public List<SigItem> getSigns() {
            return signs;
        }

        public void setSigns(List<SigItem> signs) {
            this.signs = signs;
        }
    }
}
