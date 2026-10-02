package com.fc.v2.model.auto;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.Date;

/**
 * 采伐许可证核签签批流水对象 t_flog_permit_sign
 * 每一关每一名落一笔；掐同一分钟也是两笔。市局打回后旧轮次沉底，只摆当轮。
 * 手数由当轮本关 distinct 落名人 count 得出，替签（落名人串带“代/替”字样）当没签，
 * 库内不另存可代填的枚数格——摆给人看的数同库里的数出自同一回 count。
 *
 * @author fuce
 * @date 2026-10-02
 */
@TableName("t_flog_permit_sign")
@ApiModel(value = "TFlogPermitSign", description = "采伐许可证核签签批流水")
public class TFlogPermitSign implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 核签单主键 */
    @TableField("bill_id")
    @ApiModelProperty(value = "核签单主键")
    private Long billId;

    /** 所属推进轮次（打回后旧轮沉底） */
    @TableField("round_no")
    @ApiModelProperty(value = "所属推进轮次")
    private Integer roundNo;

    /** 落名关 0收件 1核地 2并排签 3用章 */
    @TableField("node_no")
    @ApiModelProperty(value = "落名关")
    private Integer nodeNo;

    /** 并排签分管口 1蓄积强度 2权属文本（其余关为 null） */
    @TableField("sign_role")
    @ApiModelProperty(value = "并排签分管口 1蓄积强度 2权属文本")
    private Integer signRole;

    /** 落名人（本人签名，替签一律当没签） */
    @TableField("signer")
    @ApiModelProperty(value = "落名人")
    private String signer;

    /** 名目 1通过 2不予出证 3市局打回 */
    @TableField("sign_result")
    @ApiModelProperty(value = "名目 1通过 2不予出证 3市局打回")
    private Integer signResult;

    /** 意见 */
    @TableField("opinion")
    @ApiModelProperty(value = "意见")
    private String opinion;

    /** 删除标记 0正常 1删除 */
    @TableField("del_flag")
    @ApiModelProperty(value = "删除标记 0正常 1删除")
    private Integer delFlag;

    /** 创建者 */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    @ApiModelProperty(value = "创建者")
    private String createBy;

    /** 创建时间（流水时刻，逐笔累） */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    /** 更新者 */
    @TableField(value = "update_by", fill = FieldFill.UPDATE)
    @ApiModelProperty(value = "更新者")
    private String updateBy;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "更新时间")
    private Date updateTime;

    /** 备注 */
    @TableField("remark")
    @ApiModelProperty(value = "备注")
    private String remark;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBillId() {
        return billId;
    }

    public void setBillId(Long billId) {
        this.billId = billId;
    }

    public Integer getRoundNo() {
        return roundNo;
    }

    public void setRoundNo(Integer roundNo) {
        this.roundNo = roundNo;
    }

    public Integer getNodeNo() {
        return nodeNo;
    }

    public void setNodeNo(Integer nodeNo) {
        this.nodeNo = nodeNo;
    }

    public Integer getSignRole() {
        return signRole;
    }

    public void setSignRole(Integer signRole) {
        this.signRole = signRole;
    }

    public String getSigner() {
        return signer;
    }

    public void setSigner(String signer) {
        this.signer = signer;
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

    public Integer getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(Integer delFlag) {
        this.delFlag = delFlag;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
