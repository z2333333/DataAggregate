package com.vd.canary.obmp.order.api.operation.demand.response;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author xingdongyang
 * @date 2020/7/8
 */
@Data
public class DemandHeadVo implements Serializable {

    /**
     * 需求单主键
     */
    @ApiModelProperty(value = "需求单Id")
    private String demandHeadId;

    /**
     * 需求单号
     */
    @ApiModelProperty(value = "需求单号")
    private String demandCode;

    /**
     * 需求名称
     */
    @ApiModelProperty(value = "需求名称")
    private String demandName;

    /**
     * 询价日期
     */
    @ApiModelProperty(value = "询价日期")
    private LocalDateTime inquiryDate;

    /**
     * 交付截止日期
     */
    @ApiModelProperty(value = "交付截止日期")
    private LocalDateTime expectedDate;

    /**
     * 需方id
     */
    @ApiModelProperty(value = "需方id")
    private String customerId;

    /**
     * 需方名称
     */
    @ApiModelProperty(value = "需方名称")
    private String customerName;


    /**
     * 询价人 就是联系人
     */
    @ApiModelProperty("询价人就是联系人")
    private String inquiryUser;

    /**
     * 需方联系人号码
     */
    @ApiModelProperty("需方联系人号码")
    private String contactPhone;


    /**
     * 配送方式 10平台承运 20用户自提
     */
    @ApiModelProperty(value = "配送方式 10平台承运 20用户自提")
    private String deliveryType;

    /**
     * 创建人
     */
    @ApiModelProperty(value = "创建人")
    private String creatorName;

    /**
     * 创建时间
     */
    @ApiModelProperty(value = "创建时间")
    private LocalDateTime gmtCreateTime;

    /**
     * 提交人人名称
     */
    @ApiModelProperty(value = "提交人人名称")
    private String submitter;

    /**
     * 提交日期
     */
    @ApiModelProperty(value = "提交日期")
    private LocalDateTime submitDate;


    /**
     * 省份代码
     */
    @ApiModelProperty("省份代码")
    private String cusSiteRegionCode;


    /**
     * 省份名称
     */
    @ApiModelProperty("省份名称")
    private String cusSiteRegionName;


    /**
     * 城市代码
     */
    @ApiModelProperty("城市代码")
    private String cusSiteCityCode;


    /**
     * 城市名称
     */
    @ApiModelProperty("城市名称")
    private String cusSiteCityName;


    /**
     * 区县代码
     */
    @ApiModelProperty("区县代码")
    private String cusSiteCountyCode;


    /**
     * 区县名称
     */
    @ApiModelProperty("区县名称")
    private String cusSiteCountyName;


    /**
     * 状态
     */
    @ApiModelProperty("状态")
    private String demandStatus;


    /**
     * 其他要求
     */
    @ApiModelProperty("其他要求")
    private String remark;

    /**
     * 来源
     */
    @ApiModelProperty("来源")
    private String fromSource;
    /**
     * 版本信息
     */
    private Integer versionNo;

    /**
     * 跟进条数
     */
    @ApiModelProperty("跟进条数")
    private long trackCount;



}
