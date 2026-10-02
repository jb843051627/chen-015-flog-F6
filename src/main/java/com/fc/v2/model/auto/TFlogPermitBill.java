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
import java.math.BigDecimal;
import java.util.Date;

/**
 * 采伐许可证逐级核签单对象 t_flog_permit_bill
 *
 * @author fuce
 * @date 2026-09-12
 */
@TableName("t_flog_permit_bill")
@ApiModel(value = "TFlogPermitBill", description = "采伐许可证逐级核签单")
public class TFlogPermitBill implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 采伐许可证核签单号 */
    @TableField("bill_no")
    @ApiModelProperty(value = "采伐许可证核签单号")
    private String billNo;

    /** 申请人（现行采伐许可申领表） */
    @TableField("applicant")
    @ApiModelProperty(value = "申请人")
    private String applicant;

    /** 申领年度（同申请人当年只准一证） */
    @TableField("apply_year")
    @ApiModelProperty(value = "申领年度")
    private Integer applyYear;

    /** 底档字号（核地只信字号不信姓名） */
    @TableField("site_no")
    @ApiModelProperty(value = "底档字号")
    private String siteNo;

    /** 小班号 */
    @TableField("subcomp_no")
    @ApiModelProperty(value = "小班号")
    private String subcompNo;

    /** 权属（自留山/承包集体等） */
    @TableField("owner_name")
    @ApiModelProperty(value = "权属")
    private String ownerName;

    /** 拟采地点 */
    @TableField("cut_place")
    @ApiModelProperty(value = "拟采地点")
    private String cutPlace;

    /** 采伐方式 1皆伐 2渐伐 3择伐 4抚育采伐 5更新采伐 6低质低效林改造 */
    @TableField("cut_mode")
    @ApiModelProperty(value = "采伐方式")
    private Integer cutMode;

    /** 林种树种 */
    @TableField("forest_kind")
    @ApiModelProperty(value = "林种树种")
    private String forestKind;

    /** 拟采面积(公顷) */
    @TableField("cut_area")
    @ApiModelProperty(value = "拟采面积(公顷)")
    private BigDecimal cutArea;

    /** 拟采蓄积(立方米) */
    @TableField("cut_qty")
    @ApiModelProperty(value = "拟采蓄积(立方米)")
    private BigDecimal cutQty;

    /** 更新措施 */
    @TableField("renew_measure")
    @ApiModelProperty(value = "更新措施")
    private String renewMeasure;

    /** 伐区调查设计材料 0未附 1已附（超省级规定面积或蓄积须附） */
    @TableField("survey_flag")
    @ApiModelProperty(value = "伐区调查设计材料 0未附 1已附")
    private Integer surveyFlag;

    /** 拟发/已发许可证文本（核签结论一键转成，数同此一件） */
    @TableField("permit_text")
    @ApiModelProperty(value = "许可证文本")
    private String permitText;

    /** 发出去的许可证号（用章落印时填，落印即封死） */
    @TableField("permit_no")
    @ApiModelProperty(value = "许可证号")
    private String permitNo;

    /** 推进轮次（市局打回后重新累一轮，旧名目沉底不并列） */
    @TableField("round_no")
    @ApiModelProperty(value = "推进轮次")
    private Integer roundNo;

    /** 本站用章落印时刻 */
    @TableField("seal_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "用章落印时刻")
    private Date sealTime;

    /** 当前所在关 0收件 1核地 2并排签 3用章 */
    @TableField("node_no")
    @ApiModelProperty(value = "当前所在关 0收件 1核地 2并排签 3用章")
    private Integer nodeNo;

    /** 同级核签办法 0任一人 1两名点齐 */
    @TableField("sign_mode")
    @ApiModelProperty(value = "同级核签办法 0任一人 1两名点齐")
    private Integer signMode;

    /** 本级应签人数 */
    @TableField("need_count")
    @ApiModelProperty(value = "本级应签人数")
    private Integer needCount;

    /** 本级已签人数（由流水实时同算，不留代填格） */
    @TableField("sign_count")
    @ApiModelProperty(value = "本级已签人数")
    private Integer signCount;

    /** 报批情形 0在核 1已用印发证 2不予出证 3市局打回重报 */
    @TableField("status")
    @ApiModelProperty(value = "报批情形 0在核 1已用印发证 2不予出证 3市局打回重报")
    private Integer status;

    /** 删除标记 0正常 1删除 */
    @TableField("del_flag")
    @ApiModelProperty(value = "删除标记 0正常 1删除")
    private Integer delFlag;

    /** 创建者 */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    @ApiModelProperty(value = "创建者")
    private String createBy;

    /** 创建时间 */
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

    public String getBillNo() {
        return billNo;
    }

    public void setBillNo(String billNo) {
        this.billNo = billNo;
    }

    public Integer getNodeNo() {
        return nodeNo;
    }

    public void setNodeNo(Integer nodeNo) {
        this.nodeNo = nodeNo;
    }

    public Integer getSignMode() {
        return signMode;
    }

    public void setSignMode(Integer signMode) {
        this.signMode = signMode;
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

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
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

    public String getApplicant() {
        return applicant;
    }

    public void setApplicant(String applicant) {
        this.applicant = applicant;
    }

    public Integer getApplyYear() {
        return applyYear;
    }

    public void setApplyYear(Integer applyYear) {
        this.applyYear = applyYear;
    }

    public String getSiteNo() {
        return siteNo;
    }

    public void setSiteNo(String siteNo) {
        this.siteNo = siteNo;
    }

    public String getSubcompNo() {
        return subcompNo;
    }

    public void setSubcompNo(String subcompNo) {
        this.subcompNo = subcompNo;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getCutPlace() {
        return cutPlace;
    }

    public void setCutPlace(String cutPlace) {
        this.cutPlace = cutPlace;
    }

    public Integer getCutMode() {
        return cutMode;
    }

    public void setCutMode(Integer cutMode) {
        this.cutMode = cutMode;
    }

    public String getForestKind() {
        return forestKind;
    }

    public void setForestKind(String forestKind) {
        this.forestKind = forestKind;
    }

    public BigDecimal getCutArea() {
        return cutArea;
    }

    public void setCutArea(BigDecimal cutArea) {
        this.cutArea = cutArea;
    }

    public BigDecimal getCutQty() {
        return cutQty;
    }

    public void setCutQty(BigDecimal cutQty) {
        this.cutQty = cutQty;
    }

    public String getRenewMeasure() {
        return renewMeasure;
    }

    public void setRenewMeasure(String renewMeasure) {
        this.renewMeasure = renewMeasure;
    }

    public Integer getSurveyFlag() {
        return surveyFlag;
    }

    public void setSurveyFlag(Integer surveyFlag) {
        this.surveyFlag = surveyFlag;
    }

    public String getPermitText() {
        return permitText;
    }

    public void setPermitText(String permitText) {
        this.permitText = permitText;
    }

    public String getPermitNo() {
        return permitNo;
    }

    public void setPermitNo(String permitNo) {
        this.permitNo = permitNo;
    }

    public Integer getRoundNo() {
        return roundNo;
    }

    public void setRoundNo(Integer roundNo) {
        this.roundNo = roundNo;
    }

    public Date getSealTime() {
        return sealTime;
    }

    public void setSealTime(Date sealTime) {
        this.sealTime = sealTime;
    }
}
