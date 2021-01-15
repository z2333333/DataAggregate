package com.vd.canary.obmp.aggregate.actuator;

import com.alibaba.fastjson.JSON;
import com.vd.canary.core.bo.ResponseBO;
import com.vd.canary.core.exception.BusinessException;
import com.vd.canary.file.api.feign.FileBillFeignClient;
import com.vd.canary.file.api.response.vo.FileBillVO;
import com.vd.canary.obmp.aggregate.AbstractOrderDataAggregate;
import com.vd.canary.obmp.order.api.response.vo.order.OrderFileBillVO;
import com.vd.canary.utils.BeanUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.annotation.Transient;

import javax.annotation.Resource;
import java.util.List;

/**
 * 附件信息获取执行器
 * 根据业务Id、业务key从文件服务获取所有附件
 * @author zx
 * @date 2021/1/15 13:16
 */
@Data
@Slf4j
public class OrderFileSeparateActuator extends AbstractOrderDataAggregate {

    @Resource
    FileBillFeignClient fileBillFeignClient;
    @Transient
    private String fillId;
    @Transient
    private String businessType;
    private List<OrderFileBillVO> orderFileBillVOs;

    @Override
    public void doDataAggregate(Object... args) {
        ResponseBO<List<FileBillVO>> listResponseBO = fileBillFeignClient.listFilesById(fillId, businessType);
        if (listResponseBO == null || listResponseBO.isFailed() || listResponseBO.getData() == null) {
            log.error("调用文件服务失败-无法获取附件,fillId={},businessType={},返回值:{}", fillId, businessType, JSON.toJSONString(listResponseBO));
            throw new BusinessException(120_000, "调用文件服务失败-无法获取附件");
        }

        orderFileBillVOs = BeanUtil.convert(listResponseBO.getData(), OrderFileBillVO.class);
    }
}
