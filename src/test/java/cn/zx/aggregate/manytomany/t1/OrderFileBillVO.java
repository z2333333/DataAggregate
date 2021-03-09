package cn.zx.aggregate.manytomany.t1;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author zx
 * @date 2020/12/24 16:10
 */
@Data
public class OrderFileBillVO {

    /**
     * id 标识列
     */
    private String id;

    /**
     * 文件ID
     */
    private String fileId;

    /**
     * bill_id 单据id
     */
    private String billId;

    /**
     * bill_code 单据编号
     */
    private String billCode;

    /**
     * 单据类型
     */
    private String businessType;

    /**
     * 文件路径
     */
    private String fileUrl;

    /**
     * 文件原名
     */
    private String originName;

    private Integer sort;

    /**
     * 文件大小
     */
    private Long fileSize;

    /**
     * 文件后缀
     */
    private String contentType;

    /**
     * 创建时间
     */
    private Long createTime;

    /**
     * 创建时间
     */
    private LocalDateTime gmtCreateTime;

    /**
     * 创建人
     */
    private String creatorName;
}
