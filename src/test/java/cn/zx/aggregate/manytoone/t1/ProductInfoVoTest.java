package cn.zx.aggregate.manytoone.t1;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @author zx
 * @date 2021/2/23 18:01
 */
@Data
public class ProductInfoVoTest {

    List<SkuInfoVO> skuInfoVOS;
    /**
     * 主键ID
     */
    private String id;

    /**
     * spu 名称
     */
    private String spuName;

    /**
     * sku名称
     */
    private String skuName;

    /**
     * sku编码
     */
    private String skuCode;

    /**
     * sku图片
     */
    private String skuPic;

    /**
     * 单位
     */
    private String unit;

    /**
     * 商品标题
     */
    private String skuTitle;

    /**
     * 品牌名称
     */
    private String brandName;

    /**
     * 上下架状态
     */
    private String shelvesState;

    /**
     * 三级类目名字
     */
    private String threeCategoryName;


    /**
     * 供应商名称
     */
    private String skuSupplierName;

    /**
     * 采购价信息
     */
    private String purchasePriceJson;

    /**
     * 定价信息
     */
    private String sellingPriceJson;

    /**
     * 调价编码
     */
    private String modifyPriceCode;

    private int test;

    /**
     * 调价状态
     */
    private Integer auditStatus;

    /**
     * 驳回原因
     */
    private Integer rejectReason;

    /**
     * 申请来源
     */
    private Integer applicationSource;

    /**
     * 供应价
     */
    private BigDecimal purchasePrice;

    /**
     * 市场价
     */
    private BigDecimal referencePrice;

    /**
     * 会员价
     */
    private BigDecimal price;

    /**
     * vip价
     */
    private BigDecimal vipPrice;

    /**
     *调价后供应价
     */
    private BigDecimal modifySupplyPrice;

    /**
     * 调价后市场指导价
     */
    private BigDecimal modifyMarketGuidePrice;

    /**
     * 调整后会员价
     */
    private BigDecimal modifyMembershipPrice;

    /**
     * 调价后尊享价
     */
    private BigDecimal modifyVipPrice;

    /**
     * 供应数量
     */
    private Integer inventoryNum;

    /**
     * 创建时间
     */
    private LocalDateTime gmtCreateTime;

    /**
     * spu图片
     */
    private String spuPic;
}
