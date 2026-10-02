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
 * 采伐许可证文本对象 t_flog_permit_text（核签结论一键转出，各栏皆取自同一件）
 *
 * @author fuce
 * @date 2026-10-02
 */
@TableName("t_flog_permit_text")
@ApiModel(value = "TFlogPermitText", description = "采伐许可证文本")
public class TFlogPermitText implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 许可证文本号（与核签单同一件同一号） */
    @TableField("permit_no")
    @ApiModelProperty(value = "许可证文本号")
    private String permitNo;

    /** 所属核签单号 */
    @TableField("bill_no")
    @ApiModelProperty(value = "所属核签单号")
    private String billNo;

    /** 所绑申领建档卡 */
    @TableField("apply_id")
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "所绑申领建档卡")
    private Long applyId;

    /** 申请人底档字号（只信字号不信人） */
    @TableField("applicant_no")
    @ApiModelProperty(value = "申请人底档字号")
    private String applicantNo;

    /** 申请人全称 */
    @TableField("applicant_name")
    @ApiModelProperty(value = "申请人全称")
    private String applicantName;

    /** 小班号 */
    @TableField("subcomp_no")
    @ApiModelProperty(value = "小班号")
    private String subcompNo;

    /** 拟采地点(伐区名号) */
    @TableField("cut_block_no")
    @ApiModelProperty(value = "拟采地点(伐区名号)")
    private String cutBlockNo;

    /** 采伐方式 */
    @TableField("cut_mode")
    @ApiModelProperty(value = "采伐方式")
    private Integer cutMode;

    /** 皆伐株数 */
    @TableField("clear_trees")
    @ApiModelProperty(value = "皆伐株数")
    private Integer clearTrees;

    /** 拟采蓄积(立方米) */
    @TableField("qty")
    @ApiModelProperty(value = "拟采蓄积(立方米)")
    private BigDecimal qty;

    /** 拟采面积(公顷) */
    @TableField("fine_amt")
    @ApiModelProperty(value = "拟采面积(公顷)")
    private BigDecimal fineAmt;

    /** 拟采强度取用顺位 */
    @TableField("grade_level")
    @ApiModelProperty(value = "拟采强度取用顺位")
    private Integer gradeLevel;

    /** 权属底档字号 */
    @TableField("site_no")
    @ApiModelProperty(value = "权属底档字号")
    private String siteNo;

    /** 权属单位全称 */
    @TableField("site_name")
    @ApiModelProperty(value = "权属单位全称")
    private String siteName;

    /** 各级核签名目链（与当日屏上同一回计算） */
    @TableField("sign_trail")
    @ApiModelProperty(value = "各级核签名目链")
    private String signTrail;

    /** 出证年（同一位申请人当年第二条当场止步） */
    @TableField("issue_year")
    @ApiModelProperty(value = "出证年")
    private Integer issueYear;

    /** 文本核验串（与申领卡同串） */
    @TableField("content")
    @ApiModelProperty(value = "文本核验串")
    private String content;

    /** 文本情形 0已出证 1已作废 */
    @TableField("status")
    @ApiModelProperty(value = "文本情形 0已出证 1已作废")
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

    public String getPermitNo() {
        return permitNo;
    }

    public void setPermitNo(String permitNo) {
        this.permitNo = permitNo;
    }

    public String getBillNo() {
        return billNo;
    }

    public void setBillNo(String billNo) {
        this.billNo = billNo;
    }

    public Long getApplyId() {
        return applyId;
    }

    public void setApplyId(Long applyId) {
        this.applyId = applyId;
    }

    public String getApplicantNo() {
        return applicantNo;
    }

    public void setApplicantNo(String applicantNo) {
        this.applicantNo = applicantNo;
    }

    public String getApplicantName() {
        return applicantName;
    }

    public void setApplicantName(String applicantName) {
        this.applicantName = applicantName;
    }

    public String getSubcompNo() {
        return subcompNo;
    }

    public void setSubcompNo(String subcompNo) {
        this.subcompNo = subcompNo;
    }

    public String getCutBlockNo() {
        return cutBlockNo;
    }

    public void setCutBlockNo(String cutBlockNo) {
        this.cutBlockNo = cutBlockNo;
    }

    public Integer getCutMode() {
        return cutMode;
    }

    public void setCutMode(Integer cutMode) {
        this.cutMode = cutMode;
    }

    public Integer getClearTrees() {
        return clearTrees;
    }

    public void setClearTrees(Integer clearTrees) {
        this.clearTrees = clearTrees;
    }

    public BigDecimal getQty() {
        return qty;
    }

    public void setQty(BigDecimal qty) {
        this.qty = qty;
    }

    public BigDecimal getFineAmt() {
        return fineAmt;
    }

    public void setFineAmt(BigDecimal fineAmt) {
        this.fineAmt = fineAmt;
    }

    public Integer getGradeLevel() {
        return gradeLevel;
    }

    public void setGradeLevel(Integer gradeLevel) {
        this.gradeLevel = gradeLevel;
    }

    public String getSiteNo() {
        return siteNo;
    }

    public void setSiteNo(String siteNo) {
        this.siteNo = siteNo;
    }

    public String getSiteName() {
        return siteName;
    }

    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }

    public String getSignTrail() {
        return signTrail;
    }

    public void setSignTrail(String signTrail) {
        this.signTrail = signTrail;
    }

    public Integer getIssueYear() {
        return issueYear;
    }

    public void setIssueYear(Integer issueYear) {
        this.issueYear = issueYear;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
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
}
