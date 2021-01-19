package com.vd.canary.obmp.order.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import com.vd.canary.contract.api.feign.ContractServiceFeign;
import com.vd.canary.contract.api.request.SelfSignReq;
import com.vd.canary.contract.api.request.SingFileBO;
import com.vd.canary.contract.api.response.SignDataVO;
import com.vd.canary.core.bo.ResponseBO;
import com.vd.canary.core.bo.ResponsePageBO;
import com.vd.canary.core.exception.BusinessException;
import com.vd.canary.core.util.PageResponseUtil;
import com.vd.canary.core.util.ResponseUtil;
import com.vd.canary.file.api.feign.FileBillFeignClient;
import com.vd.canary.file.api.feign.FileCommonServiceFeign;
import com.vd.canary.file.api.request.CountBillNumReq;
import com.vd.canary.file.api.request.UploadFileReq;
import com.vd.canary.file.api.response.CountBillNumResp;
import com.vd.canary.file.api.response.FileInfoResp;
import com.vd.canary.file.api.response.vo.FileBillVO;
import com.vd.canary.obmp.customer.api.feign.customer.CustomerPurchaseManagerFeignClient;
import com.vd.canary.obmp.customer.api.request.customer.CustomerManagerInfoReq;
import com.vd.canary.obmp.customer.api.response.customer.vo.CustomerManagerVO;
import com.vd.canary.obmp.order.api.constants.*;
import com.vd.canary.obmp.order.api.request.*;
import com.vd.canary.obmp.order.api.request.mission.CreateMissionReq;
import com.vd.canary.obmp.order.api.request.mission.MissionRuleReq;
import com.vd.canary.obmp.order.api.request.mission.StaffReq;
import com.vd.canary.obmp.order.api.request.order.*;
import com.vd.canary.obmp.order.api.request.purchase.PomStaffReq;
import com.vd.canary.obmp.order.api.response.*;
import com.vd.canary.obmp.order.api.response.mission.ApplyMissionResp;
import com.vd.canary.obmp.order.api.response.mission.MissionConfigRuleResp;
import com.vd.canary.obmp.order.api.response.vo.PomPurchaseContractLineVO;
import com.vd.canary.obmp.order.api.response.vo.ShipmentPlanHeadVO;
import com.vd.canary.obmp.order.api.response.vo.ShipmentPlanLineVO;
import com.vd.canary.obmp.order.api.response.vo.SupplyInquiryLineAndQuotesVO;
import com.vd.canary.obmp.order.api.response.vo.order.*;
import com.vd.canary.obmp.order.api.status.SomSalesContractResponseStatus;
import com.vd.canary.obmp.order.api.status.SupplierInquiryResponseStatus;
import com.vd.canary.obmp.order.delay.DelayQueueService;
import com.vd.canary.obmp.order.operation.sales.entity.ShipmentPlanReq;
import com.vd.canary.obmp.order.operation.sales.entity.ShipmentPlanSaleVo;
import com.vd.canary.obmp.order.operation.sales.service.SomSalesContractService;
import com.vd.canary.obmp.order.repository.dto.purchase.PomAssembleHeadDTO;
import com.vd.canary.obmp.order.repository.dto.purchase.PomAssembleLineDTO;
import com.vd.canary.obmp.order.repository.dto.purchase.PomStaffDTO;
import com.vd.canary.obmp.order.repository.dto.purchase.PomSubmitDTO;
import com.vd.canary.obmp.order.repository.entity.*;
import com.vd.canary.obmp.order.repository.entity.order.PomPurchaseContractHeadHistoryEntity;
import com.vd.canary.obmp.order.repository.mapper.*;
import com.vd.canary.obmp.order.repository.mapper.order.PomAgreementMapper;
import com.vd.canary.obmp.order.repository.mapper.order.PomPurchaseContractHeadHistoryMapper;
import com.vd.canary.obmp.order.repository.mapper.order.ShipmentPlanBatchMapper;
import com.vd.canary.obmp.order.repository.mapper.qiao.PomPurchaseContractPaymentModel;
import com.vd.canary.obmp.order.repository.model.PomPurchaseContractHeadHistoryModel;
import com.vd.canary.obmp.order.repository.model.PomPurchaseContractHeadModel;
import com.vd.canary.obmp.order.repository.model.PomPurchaseContractLineModel;
import com.vd.canary.obmp.order.repository.to.PomPurchaseContractAuditTO;
import com.vd.canary.obmp.order.service.PomPurchaseContractHeadService;
import com.vd.canary.obmp.order.service.impl.order.purchase.OrderFactory;
import com.vd.canary.obmp.order.service.impl.order.purchase.PurchaseComponent;
import com.vd.canary.obmp.order.service.impl.qiao.view.PomPurchaseContractPdfView;
import com.vd.canary.obmp.order.service.mission.MissionService;
import com.vd.canary.obmp.order.service.order.PurchasePartitiveService;
import com.vd.canary.obmp.order.service.order.PurchaseService;
import com.vd.canary.obmp.order.util.*;
import com.vd.canary.obmp.product.api.constants.BackCategoryConstant;
import com.vd.canary.obmp.product.api.feign.BackgroundCategoryFeign;
import com.vd.canary.obmp.product.api.feign.ProductSkuFeign;
import com.vd.canary.obmp.product.api.response.category.CategoryBackgroundResp;
import com.vd.canary.obmp.product.api.response.sku.vo.ProductSkuInfoVo;
import com.vd.canary.obmp.staff.api.feign.staff.StaffInfoFeignClient;
import com.vd.canary.sequence.api.feign.SequenceService;
import com.vd.canary.sequence.api.response.SerialNumberVO;
import com.vd.canary.utils.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import javax.annotation.Resource;
import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author zx
 * @date 2020/4/3 13:42
 */
@Slf4j
@Service("purchaseService")
public class PurchaseServiceImpl implements PurchaseService {
    @Resource
    OrderFactory orderFactory;
    @Resource
    PurchaseComponent purchaseComponent;
    @Resource
    SupplyInquiryHeadServiceImpl supplyInquiryHeadService;
    @Resource
    SupplyInquiryLineServiceImpl supplyInquiryLineService;
    @Resource
    SupplyQuotesHeadServiceImpl supplyQuotesHeadService;
    @Resource
    SupplyQuotesLineServiceImpl supplyQuotesService;
    @Resource
    SupplyQuotesLineMapper supplyQuotesLineMapper;
    @Resource
    SequenceService sequenceService;
    @Resource
    FileCommonServiceFeign fileCommonServiceFeign;
    @Resource
    ContractServiceFeign contractServiceFeign;
    @Resource
    PomPurchaseContractHeadModel pomPurchaseContractHeadModel;
    @Resource
    PomPurchaseContractLineModel pomPurchaseContractLineModel;
    @Resource
    PomPurchaseContractPaymentModel pomPurchaseContractPaymentModel;
    @Resource
    PomPurchaseContractPaymentServiceImpl pomPurchaseContractPaymentService;
    @Resource
    PomPurchaseContractHeadHistoryModel pomPurchaseContractHeadHistoryModel;
    @Resource
    PomPurchaseContractPaymentMapper pomPurchaseContractPaymentMapper;
    @Resource
    PomPurchaseContractLineServiceImpl pomPurchaseContractLineService;
    @Resource
    PomPurchaseContractLineMapper pomPurchaseContractLineMapper;
    @Resource
    SomSalesContractMapper somSalesContractMapper;
    @Resource
    SomSalesContractHeadMapper somSalesContractHeadMapper;
    @Resource
    SomSalesContractLineMapper somSalesContractLineMapper;
    @Resource
    ShipmentPlanBatchMapper shipmentPlanBatchMapper;
    @Resource
    PomPurchaseContractHeadHistoryMapper pomPurchaseContractHeadHistoryMapper;
    @Resource
    PomPurchaseContractHeadMapper pomPurchaseContractHeadMapper;
    @Resource
    PomPurchaseContractHeadService pomPurchaseContractHeadService;
    @Resource
    PomPurchaseContractHeadVersionMapper pomPurchaseContractHeadVersionMapper;
    @Resource
    MissionService missionService;
    @Resource
    DelayQueueService delayQueueService;
    @Resource
    StaffInfoFeignClient staffInfoFeignClient;
    @Resource
    SomSalesContractService somSalesContractService;
    @Resource
    CustomerPurchaseManagerFeignClient customerPurchaseManagerFeignClient;
    @Resource
    FileBillFeignClient fileBillFeignClient;
    @Resource
    PomAgreementMapper pomAgreementMapper;
    @Resource
    BackgroundCategoryFeign backgroundCategoryFeign;
    @Resource
    ProductSkuFeign productSkuFeign;

    private final ThreadPoolExecutor threadPool = new ThreadPoolExecutor(2, 4, 6, TimeUnit.SECONDS, new ArrayBlockingQueue<>(10), new ThreadPoolExecutor.CallerRunsPolicy());

    @Override
    @Transactional
    public ResponseBO<PomPurchaseContractGenerateResp> generatePurchaseContract(PomPurchaseContractGenerateReq req) {
        req.valid();
        log.info(req.getOrderType().getValue() + "生成采购单...,销售订单合同Id:{}", req.getSomSalesContractId());

        PurchasePartitiveService partitiveService = orderFactory.getInstance(req.getOrderType());
        List<PomPurchaseCreateControlReq> creationPurchaseList = new ArrayList<>();
        for (Map.Entry<String, List<PomContractGenerateBO>> entry : req.getPomContractGenerateBOMap().entrySet()) {
            SomSalesContractHeadEntity somSalesContractHeadEntity = getSomSalesContractHeadEntity(entry.getKey(), "生成采购单");

            //一个销售单对应多个采购单
            for (PomContractGenerateBO pomContractGenerateBO : entry.getValue()) {
                checkPurchaseContract(somSalesContractHeadEntity.getSalesContractHeadId(), pomContractGenerateBO);

                List<SomSalesContractLineEntity> somSalesContractLineEntities = getSomSalesContractLineEntities(pomContractGenerateBO.getSalesContractLineId(), "生成采购单");

                //生成采购单主表信息
                PomPurchaseCreateControlReq pomPurchaseCreateControlReq = new PomPurchaseCreateControlReq();
                creationPurchaseList.add(pomPurchaseCreateControlReq);

                BeanUtils.copyProperties(partitiveService.assemblePomPurchaseHead(somSalesContractHeadEntity, somSalesContractLineEntities.get(0), req, pomContractGenerateBO), pomPurchaseCreateControlReq);
                pomPurchaseCreateControlReq.setPurchaseContractCode(getPurchaseContractCode());

                //生成采购单明细表信息
                List<PomPurchaseContractLineBO> lineBOList = new ArrayList<>();
                pomPurchaseCreateControlReq.setLineBOList(lineBOList);
                somSalesContractLineEntities.forEach(somSalesContractLineEntity -> {
                    checkPurchaseContractLine(somSalesContractHeadEntity, pomContractGenerateBO, somSalesContractLineEntity);

                    PomPurchaseContractLineBO pomLineBO = new PomPurchaseContractLineBO();
                    BeanUtils.copyProperties(partitiveService.assemblePomPurchaseLine(somSalesContractLineEntity, req, somSalesContractHeadEntity), pomLineBO);

                    pomLineBO.setPurchaseContractLineCode(getPurchaseContractLineCode(pomPurchaseCreateControlReq.getPurchaseContractCode()));
                    lineBOList.add(pomLineBO);
                });
            }
        }

        //落库
        var resp = creationPurchaseContract(creationPurchaseList);

        //完善订单-设置采购经理或采购内勤
        //涉及订单状态变化且失败时没有补偿机制所以属于主流程不能异步
        var purchaseContractHeads = perfectPomPurchase(req, creationPurchaseList);

        threadPool.execute(() -> afterPomPurchase(purchaseContractHeads));
        return resp;
    }

    private List<PomPurchaseContractHead> perfectPomPurchase(PomPurchaseContractGenerateReq req, List<PomPurchaseCreateControlReq> creationPurchaseList) {
        List<String> purchaseCodeList = creationPurchaseList.stream().map(PomPurchaseCreateControlReq::getPurchaseContractCode).collect(Collectors.toList());
        List<PomPurchaseContractHead> pomPurchaseContractHeads = pomPurchaseContractHeadMapper.selectList(new LambdaQueryWrapper<PomPurchaseContractHead>().in(PomPurchaseContractHead::getPurchaseContractCode, purchaseCodeList));

        PurchasePartitiveService partitiveService = orderFactory.getInstance(req.getOrderType());
        //设置采购经理
        List<PomStaffReq> pomStaffReqs = BeanUtil.convert(pomPurchaseContractHeads, PomStaffReq.class);
        List<PomStaffDTO> pomStaffDTOS = partitiveService.assignPomStaff(pomStaffReqs, req);
        ParameterUtil.conditionalCopy(pomStaffDTOS, pomPurchaseContractHeads, "purchaseContractHeadId");

        if (req.getOrderType().equals(PomConstants.FromType.STANDARD_STR)) {
            //生成付款条款-当前只有标品钢贸会自动生成,先不抽取
            createPomPaymentAndSetStatus(pomPurchaseContractHeads);
        }

        pomPurchaseContractHeadService.updateBatchById(pomPurchaseContractHeads);
        return pomPurchaseContractHeads;
    }

    private void afterPomPurchase(List<PomPurchaseContractHead> purchaseContractHeads) {
        purchaseContractHeads.forEach(pomPurchaseContractHead -> {
            //初始化快照
            pomPurchaseContractHeadModel.createVersion(pomPurchaseContractHead, JSON.toJSONString(pomPurchaseContractHeadModel.getPurchaseSnapshotVO(pomPurchaseContractHead)), PomConstants.INITIAL_VERSION - 1);
            log.info("初始化采购单快照成功,Id:{}", pomPurchaseContractHead.getPurchaseContractHeadId());
        });
    }

    private void createPomPaymentAndSetStatus(List<PomPurchaseContractHead> pomPurchaseContractHeads) {
        pomPurchaseContractHeads.forEach(pomPurchaseContractHead -> {
            //标品钢贸订单自动生成付款条款
            PomPurchaseContractPayment pomPayment = new PomPurchaseContractPayment();
            pomPayment.setAmount(pomPurchaseContractHead.getTotalAmount());
            pomPayment.setAmountRate(new BigDecimal(100));
            //付款依据:发货单
            pomPayment.setPaymentCondition("DELIVERY");
            //付款条款:到货款
            pomPayment.setPaymentTerm("PAY_FOR_RECEIVE");
            pomPayment.setPaymentRemark("货到票到7天");
            pomPayment.setPurchaseContractHeadId(pomPurchaseContractHead.getPurchaseContractHeadId());
            pomPurchaseContractPaymentMapper.insert(pomPayment);

            //设置供方接单定时(后续订单更新失败的情况先不考虑)
            purchaseOrderDelayClose(pomPurchaseContractHead);
            //状态改为待供方确认
            purchaseComponent.setPurchaseOrderStatus(pomPurchaseContractHead, PomConstants.OrderStatus.WAIT_SUPPLY_SUBMIT, "标品钢贸订单自动生成付款条款");
        });
    }

    private ResponseBO<CustomerManagerVO> getCustomerManager(PomPurchaseContractHead pomPurchaseContractHead) {
        CustomerManagerInfoReq cusReq = new CustomerManagerInfoReq();
        cusReq.setCustomerId(pomPurchaseContractHead.getSupplierId());
        //优化:批量接口
        ResponseBO<CustomerManagerVO> cusResp = customerPurchaseManagerFeignClient.getManagerInfo(cusReq);
        if (cusResp == null || cusResp.isFailed()) {
            log.error("调用企业服务获取采购经理失败:入参{},返回值:{}", JSON.toJSONString(cusReq), JSON.toJSONString(cusResp));
            throw new BusinessException(120_000, "调用企业服务获取采购经理失败");
        }
        return cusResp;
    }

    private void checkPurchaseContract(String salesContractHeadId, PomContractGenerateBO pomContractGenerateBO) {
        //校验同个采购单中商品明细数据是否重复
        if (new HashSet<>(pomContractGenerateBO.getSalesContractLineId()).size() != pomContractGenerateBO.getSalesContractLineId().size()) {
            log.error("生成采购单-参数中商品明细重复");
            throw new BusinessException(120_000, "销售单传参商品明细重复");
        }

        //校验采购单是否重复
        int purchaseCount = pomPurchaseContractHeadService.count(new LambdaQueryWrapper<PomPurchaseContractHead>()
                .eq(PomPurchaseContractHead::getFromId, salesContractHeadId)
                .eq(PomPurchaseContractHead::getSupplierId, pomContractGenerateBO.getSupplyId()));
        if (purchaseCount > 0) {
            log.error("生成采购单-已存在相同采购单,销售子表Id:{},供应商Id:{}", salesContractHeadId, pomContractGenerateBO.getSupplyId());
            throw new BusinessException(120_000, "已存在相同采购单");
        }
    }

    public void checkPurchaseContractLine(SomSalesContractHeadEntity somSalesContractHeadEntity, PomContractGenerateBO pomContractGenerateBO, SomSalesContractLineEntity somSalesContractLineEntity) {
        if (StringUtils.isBlank(somSalesContractLineEntity.getSupplierId()) || !somSalesContractLineEntity.getSupplierId().equals(pomContractGenerateBO.getSupplyId())) {
            log.error("生成采购单-销售明细表供应商Id与入参不一致,入参:{},销售明细:{}", JSON.toJSONString(pomContractGenerateBO), JSON.toJSONString(somSalesContractLineEntity));
            throw new BusinessException(120_000, "销售明细表供应商Id与入参不一致");
        }
        if (!somSalesContractLineEntity.getSalesContractHeadId().equals(somSalesContractHeadEntity.getSalesContractHeadId())) {
            log.error("生成采购单-销售明细详情关联的主表Id与当前不一致:{},销售主表Id:{}", JSON.toJSONString(pomContractGenerateBO), somSalesContractHeadEntity.getSalesContractHeadId());
            throw new BusinessException(120_000, "销售明细详情关联的主表Id与当前不一致");
        }
    }

    public SomSalesContractHeadEntity getSomSalesContractHeadEntity(String somSalesHeadId, String msg) {
        SomSalesContractHeadEntity somSalesContractHeadEntity = somSalesContractHeadMapper.selectById(somSalesHeadId);
        if (ObjectUtil.isEmpty(somSalesContractHeadEntity)) {
            log.error(msg + "异常-无法获取销售子表信息,销售子表Id:{}");
            throw new BusinessException(120_000, "无法获取销售子表信息");
        }
        return somSalesContractHeadEntity;
    }

    public List<SomSalesContractLineEntity> getSomSalesContractLineEntities(List<String> salesContractLineId, String msg) {
        List<SomSalesContractLineEntity> somSalesContractLineEntities = somSalesContractLineMapper.selectBatchIds(salesContractLineId);
        if (CollectionUtils.isEmpty(somSalesContractLineEntities)) {
            log.error(msg + "异常-无法获取销售子表对应的明细信息,lineId:{}", salesContractLineId.toArray());
            throw new BusinessException(120_000, "无法根据销售明细Id获取对应销售明细信息");
        }
        return somSalesContractLineEntities;
    }

    public List<SomSalesContractLineEntity> getSomSalesContractLineEntities(String salesContractHeadId, String msg) {
        List<SomSalesContractLineEntity> somSalesContractLineEntities = somSalesContractLineMapper.selectList(new LambdaQueryWrapper<SomSalesContractLineEntity>().eq(SomSalesContractLineEntity::getSalesContractHeadId, salesContractHeadId));
        if (CollectionUtils.isEmpty(somSalesContractLineEntities)) {
            log.error(msg + "异常-无法获取销售子表对应的明细信息,headId:{}", salesContractHeadId);
            throw new BusinessException(120_000, "无法根据销售子表获取对应销售明细信息");
        }
        return somSalesContractLineEntities;
    }

    public PomPurchaseContractHead getPomPurchaseContractHead(String purchaseContractHeadId, String msg) {
        PomPurchaseContractHead pomPurchaseContractHead = pomPurchaseContractHeadMapper.selectById(purchaseContractHeadId);
        if (ObjectUtil.isEmpty(pomPurchaseContractHead)) {
            log.error(msg + "异常-未找到当前采购单:{}", purchaseContractHeadId);
            throw new BusinessException(120_000, "未找到当前采购单");
        }
        return pomPurchaseContractHead;
    }

    public List<PomPurchaseContractHead> getPomPurchaseContractHeads(List<String> purchaseContractHeadIdList, String msg) {
        List<PomPurchaseContractHead> purchaseContractHeads = pomPurchaseContractHeadMapper.selectBatchIds(purchaseContractHeadIdList);
        if (CollectionUtils.isEmpty(purchaseContractHeads) || purchaseContractHeads.size() != purchaseContractHeadIdList.size()) {
            log.error(msg + "异常-未找到对应采购单:{}", JSON.toJSONString(purchaseContractHeadIdList));
            throw new BusinessException(120_000, "未找到对应采购单");
        }
        return purchaseContractHeads;
    }

    public List<PomPurchaseContractHead> getPomPurchaseContractHeads(String somSaleHeadId, String msg) {
        List<PomPurchaseContractHead> purchaseContractHeads = pomPurchaseContractHeadMapper.selectList(new LambdaQueryWrapper<PomPurchaseContractHead>().eq(PomPurchaseContractHead::getSalesContractHeadId, somSaleHeadId));
        if (CollectionUtils.isEmpty(purchaseContractHeads)) {
            log.error(msg + "异常-当前销售子单无法获取对应采购单:{}", JSON.toJSONString(somSaleHeadId));
            throw new BusinessException(120_000, "当前销售子单无法获取对应采购单");
        }
        return purchaseContractHeads;
    }

    public List<PomPurchaseContractLine> getPomPurchaseContractLines(String purchaseContractHeadId, String msg) {
        List<PomPurchaseContractLine> pomPurchaseContractLines = pomPurchaseContractLineMapper.selectList(new LambdaQueryWrapper<PomPurchaseContractLine>().eq(PomPurchaseContractLine::getPurchaseContractHeadId, purchaseContractHeadId));
        if (CollectionUtils.isEmpty(pomPurchaseContractLines)) {
            log.error(msg + "异常-未找到采购单对应明细信息:{}", purchaseContractHeadId);
            throw new BusinessException(120_000, "未找到采购单对应明细信息");
        }
        return pomPurchaseContractLines;
    }

    public PomPurchaseContractLine getPomPurchaseContractLineByFromId(String somSalesContractLineId, String msg) {
        PomPurchaseContractLine pomLine = pomPurchaseContractLineMapper.selectOne(new LambdaQueryWrapper<PomPurchaseContractLine>().eq(PomPurchaseContractLine::getFromLineId, somSalesContractLineId));
        if (null == pomLine) {
            log.error(msg + "异常-根据销售明细Id未找到采购单对应明细信息:{}", somSalesContractLineId);
            throw new BusinessException(120_000, "根据销售明细Id未找到采购单对应明细信息");
        }
        return pomLine;
    }

    public List<PomPurchaseContractPayment> getPomPurchasePayments(String purchaseContractHeadId) {
        return pomPurchaseContractPaymentMapper.selectList(new QueryWrapper<PomPurchaseContractPayment>().lambda().eq(PomPurchaseContractPayment::getPurchaseContractHeadId, purchaseContractHeadId));
    }

    /**
     * 获取采购单的订单类型对应枚举
     *
     * @param head
     * @return com.vd.canary.obmp.order.api.constants.PomConstants.FromType
     */
    public PomConstants.FromType getOrderTypeCompatible(PomPurchaseContractHead head) {
        PomConstants.FromType orderType;
        try {
            orderType = PomConstants.FromType.valueOf(head.getFromType());
        } catch (IllegalArgumentException e) {
            //兼容重构前的采购订单类型
            SomSalesContractHeadEntity somHead = getSomSalesContractHeadEntity(head.getSalesContractHeadId(), "获取采购单订单类型枚举");
            SomSalesContractEntity somSalesContractEntity = somSalesContractMapper.selectById(somHead.getSalesContractId());
            orderType = PomConstants.FromType.getPomTypeBySales(SomSalesContractConstant.OrderCategory.getOrderCategory(somSalesContractEntity.getOrderCategory()));
            if (orderType != null) {
                head.setFromType(orderType.getKey());
                pomPurchaseContractHeadMapper.updateById(head);
                log.info("更新重构前的采购订单类型,订单Id:{}", head.getPurchaseContractHeadId());
            } else {
                throw new BusinessException(120_000, "无法获取采购单的订单类型对应枚举");
            }
        }
        return orderType;
    }

    public String getPurchaseContractCode() {
        ResponseBO<SerialNumberVO> serialNumberVOResponseBO = sequenceService.nextByDefineId(PomConstants.SEQUENCE_SERIAL_DEF.PURCHASE_CONTRACT_CODE);
        if (serialNumberVOResponseBO.isFailed() || StringUtil.isBlank(serialNumberVOResponseBO.getData().getSequenceCode())) {
            log.error("生成采购单-调用序列号服务获取采购单号失败,入参:{},返回值:{}", PomConstants.SEQUENCE_SERIAL_DEF.PURCHASE_CONTRACT_CODE, JSON.toJSONString(serialNumberVOResponseBO));
            throw new BusinessException(120_000, "调用序列号服务获取采购单号失败");
        }

        String purchaseContractCode = serialNumberVOResponseBO.getData().getSequenceCode();
        log.info("生成的采购单code={}", purchaseContractCode);
        return purchaseContractCode;
    }

    public String getPurchaseContractLineCode(String purchaseContractCode) {
        ResponseBO<SerialNumberVO> serialNumberResp = sequenceService.nextByBizCode(PomConstants.SEQUENCE_SERIAL_DEF.PURCHASE_CONTRACT_LINE_CODE, purchaseContractCode);
        String sequenceCode = serialNumberResp.getData().getSequenceCode();
        if (serialNumberResp.isFailed() || StringUtil.isBlank(sequenceCode)) {
            log.error("生成采购单-调用序列号服务获取采购单明细号失败,入参:{},返回值:{}", PomConstants.SEQUENCE_SERIAL_DEF.PURCHASE_CONTRACT_LINE_CODE + "_" + purchaseContractCode, JSON.toJSONString(serialNumberResp));
            throw new BusinessException(120_000, "调用序列号服务获取采购单明细号失败");
        }

        log.info("生成的采购单明细code={}", sequenceCode);
        return sequenceCode;
    }

    @Override
    @Transactional
    public ResponseBO<Boolean> assignPurchaseContract(PomPurchaseContractAcceptReq req) {
        for (String purchaseContractHeadId : req.getPurchaseContractHeadIdList()) {
            PomPurchaseContractHead pomPurchaseContractHead = pomPurchaseContractHeadMapper.selectById(purchaseContractHeadId);
            if (ObjectUtil.isEmpty(pomPurchaseContractHead)) {
                log.error("指定采购经理异常,未找到当前采购单:{}", purchaseContractHeadId);
                throw new BusinessException(120_000, "未找到当前采购单");
            }

            purchaseComponent.checkStatus(StatusValidity.getAssignManager(), pomPurchaseContractHead, "指定采购经理");

            pomPurchaseContractHead.setDepartmentId(req.getDepartmentId());
            pomPurchaseContractHead.setDepartmentName(req.getDepartmentName());
            pomPurchaseContractHead.setDepartmentCode(req.getDepartmentCode());
            pomPurchaseContractHead.setStaffId(req.getStaffId());
            pomPurchaseContractHead.setStaffName(req.getStaffName());
            pomPurchaseContractHead.setStaffCode(req.getStaffCode());
            pomPurchaseContractHead.setStaffPhone(req.getStaffPhone());

            // 订单状态：待提交
            purchaseComponent.updatePurchaseOrderStatus(pomPurchaseContractHead, PomConstants.OrderStatus.SUBMIT_PENDING, "指定采购经理");

            //生成编辑采购订单的任务给到采购经理
            CreateMissionReq missionReq = new CreateMissionReq();
            missionReq.setSerialCode(pomPurchaseContractHead.getPurchaseContractCode());
            missionReq.setSerialType(MissionConstants.OrderType.POM_ORDER.getCode());
            missionReq.setSerialDealStatus(pomPurchaseContractHead.getOrderStatus());
            missionReq.setSerialName(pomPurchaseContractHead.getContractName());
            missionReq.setCustomerName(pomPurchaseContractHead.getSupplierName());
            missionReq.setCustomerType(1);
            missionReq.setCreateUserName(pomPurchaseContractHead.getCreatorName());
            missionReq.setSerialCreateTime(new Date());
            StaffReq staffReq = new StaffReq();
            BeanUtils.copyProperties(pomPurchaseContractHead, staffReq);
            missionReq.setDealStaff(staffReq);
            ResponseBO<ApplyMissionResp> mission = missionService.createMission(missionReq);
            if (mission.isFailed()) {
                log.error("指定采购经理异常,调用生成代办接口失败:入参:{},返回值:{}", JSON.toJSONString(missionReq), JSON.toJSONString(mission));
                throw new BusinessException(120_000, "指定采购经理异常,调用生成代办接口失败");
            }
        }

        return ResponseUtil.ok();
    }

    /**
     * 更新采购单
     *
     * @param req
     * @return
     */
    @Override
    @Transactional
    public ResponseBO<PomPurchaseContractGenerateResp> updatePurchaseContract(PomPurchaseContractUpdateReq req) {
        /** head表依据head,line,payment三者是否发生变化决定是否更新(要设置更新时间与快照版本)
         * line,payment不做对比直接更新*/
        PomPurchaseContractHead pomPurchaseContractHead = getPomPurchaseContractHead(req.getHeadUpdateBO().getPurchaseContractHeadId(), "更新采购单");

        purchaseComponent.checkStatus(StatusValidity.getPomUpdate(), pomPurchaseContractHead, "更新采购单");

        //更新商品行(商品备注)
        List<PomPurchaseContractLine> lineListUpdate = new ArrayList<>();
        for (PomPurchaseContractUpdateLineBO lineBO : req.getLineBOList()) {
            PomPurchaseContractLine line = new PomPurchaseContractLine();
            if (StringUtil.isBlank(lineBO.getPurchaseContractLineId())) {
                throw new BusinessException(120_000, "采购订单明细主键为空");
            }
            //先不对比直接更新
            line.setPurchaseContractLineId(lineBO.getPurchaseContractLineId());
            line.setContractLineRemarks(lineBO.getContractLineRemarks());
            line.setPurchaseContractHeadId(pomPurchaseContractHead.getPurchaseContractHeadId());
            lineListUpdate.add(line);
        }

        //更新商品行
        updatePomPurchaseContractLine(null, lineListUpdate, null, pomPurchaseContractHead);

        //更新付款条款
        BigDecimal sumAmountRate = BigDecimal.ZERO;
        //正确应该以订单实际金额与页面付款金额去比较
        for (PomPurchaseContractDetailPaymentBO paymentBO : req.getPaymentBOList()) {
            sumAmountRate = sumAmountRate.add(paymentBO.getAmountRate());
        }
        if (!(sumAmountRate.compareTo(new BigDecimal(100)) == 0)) {
            throw new BusinessException(120_000, "付款条款合计金额占比必须为100%");
        }

        List<PomPurchaseContractPayment> paymentListAdd = new ArrayList<>();
        List<PomPurchaseContractPayment> paymentListUpdate = new ArrayList<>();
        List<PomPurchaseContractPayment> paymentListDelete = new ArrayList<>();
        for (PomPurchaseContractDetailPaymentBO paymentBO : req.getPaymentBOList()) {
            PomPurchaseContractPayment payment = new PomPurchaseContractPayment();
            BeanUtils.copyProperties(paymentBO, payment);
            payment.setPurchaseContractHeadId(pomPurchaseContractHead.getPurchaseContractHeadId());
            //线下订单设置日利率默认值
            if (PomConstants.FromType.fuzzyEquals(getOrderTypeCompatible(pomPurchaseContractHead), PomConstants.FromType.OL_REGULAR)) {
                payment.setDayProfit(SomAgreementConstant.DAY_PROFIT);
            }
            if (!StringUtil.isBlank(paymentBO.getContractPaymentId())) {
                paymentListUpdate.add(payment);
            } else {
                paymentListAdd.add(payment);
            }
        }

        if (!ObjectUtils.isEmpty(req.getPaymentBOListRemoveList())) {
            req.getPaymentBOListRemoveList().stream().forEach(pomPurchaseContractDetailPaymentBO -> {
                PomPurchaseContractPayment payment = new PomPurchaseContractPayment();
                BeanUtils.copyProperties(pomPurchaseContractDetailPaymentBO, payment);
                payment.setPurchaseContractHeadId(pomPurchaseContractHead.getPurchaseContractHeadId());
                paymentListDelete.add(payment);
            });
        }

        updatePomPurchaseContractPayment(paymentListAdd, paymentListUpdate, paymentListDelete, pomPurchaseContractHead);

        updatePomPurchaseContractHead(pomPurchaseContractHead, req.getHeadUpdateBO());
        return ResponseUtil.ok();
    }

    public void updatePomPurchaseContractHead(PomPurchaseContractHead pomPurchaseContractHead, PomPurchaseHeadUpdateBO headUpdateBO) {
        if (headUpdateBO != null) {
            //以当前值覆盖旧值
            BeanUtils.copyProperties(headUpdateBO, pomPurchaseContractHead);
        }

        pomPurchaseContractHeadModel.checkIfUpdateAndCreateVersion(pomPurchaseContractHead);
    }

    public List<PomPurchaseContractLine> updatePomPurchaseContractLine(List<PomPurchaseContractLine> lineListAdd, List<PomPurchaseContractLine> lineListUpdate, List<PomPurchaseContractLine> lineListDelete, PomPurchaseContractHead pomPurchaseContractHead) {
        if (!CollectionUtils.isEmpty(lineListAdd)) {
            String pomPurchaseContractCode = pomPurchaseContractHead.getPurchaseContractCode();
            ResponseBO<List<String>> salesContractHeadCode = sequenceService.batchByBizCode("PomPurchaseLineCode1", pomPurchaseContractCode, lineListAdd.size());
            if (salesContractHeadCode.isFailed()) {
                throw new BusinessException(120_000, "采购单商品表调用公共服务:Code生成异常");
            }
            for (int i = 0; i < lineListAdd.size(); i++) {
                lineListAdd.get(i).setPurchaseContractLineCode(salesContractHeadCode.getData().get(i));
            }
            pomPurchaseContractLineService.saveBatch(lineListAdd);
        }

        if (!CollectionUtils.isEmpty(lineListUpdate)) {
            pomPurchaseContractLineService.updateBatchById(lineListUpdate);
        }

        if (!CollectionUtils.isEmpty(lineListDelete)) {
            pomPurchaseContractLineMapper.deleteBatchIds(lineListDelete.stream().map(PomPurchaseContractLine::getPurchaseContractLineId).collect(Collectors.toList()));
        }

        return pomPurchaseContractLineMapper.selectList(new QueryWrapper<PomPurchaseContractLine>().lambda().eq(PomPurchaseContractLine::getPurchaseContractHeadId, pomPurchaseContractHead.getPurchaseContractHeadId()));
    }

    public List<PomPurchaseContractPayment> updatePomPurchaseContractPayment(List<PomPurchaseContractPayment> paymentListAdd, List<PomPurchaseContractPayment> paymentListUpdate, List<PomPurchaseContractPayment> paymentListDelete, PomPurchaseContractHead pomPurchaseContractHead) {
        if (!CollectionUtils.isEmpty(paymentListAdd)) {
            for (int i = 0; i < paymentListAdd.size(); i++) {
                //todo 付款条款号
                //->
                paymentListAdd.get(i).setContractPaymentCode("111");
            }
            pomPurchaseContractPaymentService.saveBatch(paymentListAdd);
        }

        if (!CollectionUtils.isEmpty(paymentListUpdate)) {
            pomPurchaseContractPaymentService.updateBatchById(paymentListUpdate);
        }

        if (!CollectionUtils.isEmpty(paymentListDelete)) {
            pomPurchaseContractPaymentMapper.deleteBatchIds(paymentListDelete.stream().map(PomPurchaseContractPayment::getContractPaymentId).collect(Collectors.toList()));
        }

        List<PomPurchaseContractPayment> pomPurchaseContractPaymentList = getPomPurchasePayments(pomPurchaseContractHead.getPurchaseContractHeadId());
        return pomPurchaseContractPaymentList;
    }

    @Override
    @Transactional
    public ResponseBO<Boolean> submitPurchaseContract(PomPurchaseContractHeadComReq request) {
        List<PomPurchaseContractHead> purchaseContractHeads = getPomPurchaseContractHeads(request.getPurchaseContractHeadIdList(), "提交采购单");

        pomPurchaseContractHeadService.updateBatchById(purchaseContractHeads.stream().peek(head -> {
            PurchasePartitiveService partitiveService = orderFactory.getInstance(getOrderTypeCompatible(head));
            PomSubmitDTO pomSubmitDTO = partitiveService.pomPurchaseSubmit(head);
            purchaseComponent.setPurchaseOrderStatus(head, PomConstants.OrderStatus.getOrderStatus(pomSubmitDTO.getOrderStatus()), "指定采购经理");
            log.info("提交采购单成功,受影响单据Id:{}", head.getPurchaseContractHeadId());
        }).collect(Collectors.toList()));

        return ResponseUtil.ok();
    }

    @Override
    public ResponsePageBO<PurchaseOrderListResp> purchaseOrderList(PurchaseOrderListReq purchaseOrderListReq) {
        if (purchaseOrderListReq.getContractEndDate() != null) {
            purchaseOrderListReq.setContractEndDate(DateUtil.addDate(purchaseOrderListReq.getContractEndDate(), 1));
        }
        if (purchaseOrderListReq.getGmtCreateTimeEnd() != null) {
            purchaseOrderListReq.setGmtCreateTimeEnd(DateUtil.addDate(purchaseOrderListReq.getGmtCreateTimeEnd(), 1));
        }

        List<PurchaseOrderListResp> purchaseOrderListResps = new ArrayList<>();
        PageHelper.startPage(purchaseOrderListReq.getPageNum(), purchaseOrderListReq.getPageSize());
        List<PomPurchaseContractHeadVO> list = pomPurchaseContractHeadMapper.list(purchaseOrderListReq);
        PageInfo<PomPurchaseContractHeadVO> pageInfo = new PageInfo<>(list);
        if (CollectionUtils.isEmpty(list)) {
            return PageResponseUtil.ok(purchaseOrderListReq, pageInfo.getTotal(), purchaseOrderListResps);
        }

        //获取附件数量
        List<CountBillNumResp> attachmentWaitNum = null;
        List<CountBillNumResp> attachmentAlreadyNum = null;
        if (purchaseOrderListReq.getShowAttachmentNum()) {
            List<String> pomHeadIds = list.stream().map(PomPurchaseContractHeadVO::getPurchaseContractHeadId).collect(Collectors.toList());
            attachmentWaitNum = getAttachmentsNum(pomHeadIds, PomConstants.attachmentType.PURCHASE_HEAD_WAIT_APPROVE.getKey());
            attachmentAlreadyNum = getAttachmentsNum(pomHeadIds, PomConstants.attachmentType.PURCHASE_HEAD_ALREADY_APPROVE.getKey());
        }

        for (PomPurchaseContractHeadVO pomPurchaseContractHeadVO : list) {
            PurchaseOrderListResp purchaseOrderListRespTmp = new PurchaseOrderListResp();
            BeanUtils.copyProperties(pomPurchaseContractHeadVO, purchaseOrderListRespTmp);
            BigDecimal totalAmount = pomPurchaseContractHeadVO.getTotalAmount();
            if (totalAmount != null) {
                purchaseOrderListRespTmp.setTotalAmount(totalAmount.toString());
            }

            if (!CollectionUtils.isEmpty(attachmentWaitNum)) {
                attachmentWaitNum.stream().filter(resp -> resp.getBillId().equals(purchaseOrderListRespTmp.getPurchaseContractHeadId())).findFirst().ifPresent(v -> purchaseOrderListRespTmp.setWaitApproveAttachmentNum(v.getBillCount()));
                attachmentAlreadyNum.stream().filter(resp -> resp.getBillId().equals(purchaseOrderListRespTmp.getPurchaseContractHeadId())).findFirst().ifPresent(v -> purchaseOrderListRespTmp.setAlreadyApproveAttachmentNum(v.getBillCount()));
            }
            purchaseOrderListResps.add(purchaseOrderListRespTmp);
        }
        return PageResponseUtil.ok(purchaseOrderListReq, pageInfo.getTotal(), purchaseOrderListResps);
    }

    private List<CountBillNumResp> getAttachmentsNum(List<String> idList, String businessType) {
        CountBillNumReq req = new CountBillNumReq();
        req.setBillIdList(idList);
        req.setBusinessType(businessType);
        ResponseBO<List<CountBillNumResp>> listResponseBO = fileBillFeignClient.countGroupByBillIds(req);
        if (listResponseBO == null || listResponseBO.isFailed() || listResponseBO.getData() == null) {
            log.error("调用文件服务失败-无法获取附件数量,入参:{},返回值:{}", JSON.toJSONString(req), JSON.toJSONString(listResponseBO));
            throw new BusinessException(120_000, "调用文件服务失败-无法获取附件数量");
        }
        return listResponseBO.getData();
    }

    @Override
    public ResponseBO<PomPurchaseContractDetailResp> purchaseOrderDetail(PomPurchaseContractDetailReq req) {
        String purchaseContractHeadId = req.getPurchaseContractHeadId();
        PomPurchaseContractDetailResp resp = new PomPurchaseContractDetailResp();
        // 查询订单信息
        PomPurchaseContractHead pomPurchaseContractHead = getPomPurchaseContractHead(req.getPurchaseContractHeadId(), "采购单详情");
        PomPurchaseContractHeadVO pomPurchaseContractHeadVO = BeanUtil.copyProperties(pomPurchaseContractHead, PomPurchaseContractHeadVO.class);
        if (SomSalesContractConstant.SalesSignType.OFFLINE.equals(pomPurchaseContractHead.getSignType())) {
            ResponseBO<List<FileBillVO>> demandLineAnnex = fileBillFeignClient.listFilesById(pomPurchaseContractHead.getPurchaseContractHeadId(), PomBizConstants.FONT_POM_PURCHASE_ORDER);
            List<FileBillVO> files = demandLineAnnex.getData();
            List<String> signFiles = Optional.ofNullable(files).orElse(Lists.newArrayList()).stream().map(file -> String.join(",", file.getOriginName(), file.getFileUrl())).collect(Collectors.toList());
            pomPurchaseContractHeadVO.setSignFiles(signFiles);
        }

        // 查询订单明细
        List<PomPurchaseContractLine> pomPurchaseContractLines = pomPurchaseContractLineMapper.selectList(new QueryWrapper<PomPurchaseContractLine>().lambda().eq(PomPurchaseContractLine::getPurchaseContractHeadId, purchaseContractHeadId));
        List<PomPurchaseContractLineVO> pomPurchaseContractLineVOList = BeanUtil.convert(pomPurchaseContractLines, PomPurchaseContractLineVO.class);

        //查询付款条款信息
        List<PomPurchaseContractPaymentVO> pomPurchaseContractPaymentVOList = new ArrayList<>();
        List<PomPurchaseContractPayment> pomPurchaseContractPaymentList = getPomPurchasePayments(pomPurchaseContractHead.getPurchaseContractHeadId());
        if (!CollectionUtils.isEmpty(pomPurchaseContractPaymentList)) {
            pomPurchaseContractPaymentVOList = BeanUtil.convert(pomPurchaseContractPaymentList, PomPurchaseContractPaymentVO.class);
        }

        //结算方式
        if (!StringUtil.isBlank(pomPurchaseContractHead.getAgreementId())) {
            Optional.ofNullable(pomAgreementMapper.selectById(pomPurchaseContractHead.getAgreementId())).ifPresent(pomAgreementEntity -> resp.setCreditType(pomAgreementEntity.getCreditType()));
        }

        //毛利率与毛利额
        //同个采购单当前在相同二级类目下
        setGrossProfit(pomPurchaseContractHeadVO, pomPurchaseContractLines);
        setTextureOfMaterial(pomPurchaseContractLineVOList);

        resp.setPomPurchaseContractHeadVO(pomPurchaseContractHeadVO);
        resp.setPomPurchaseContractLineList(pomPurchaseContractLineVOList);
        resp.setPomPurchaseContractPaymentVOList(pomPurchaseContractPaymentVOList);
        return ResponseUtil.ok(resp);
    }

    @Override
    public List<PomPurchaseBriefDetailAllResp> purchaseOrderBriefDetail(PomPurchaseContractBriefDetailAllReq req) {
        List<PomPurchaseContractHead> heads;
        if (!StringUtil.isBlank(req.getSalesContractHeadCode())) {
            heads = pomPurchaseContractHeadMapper.selectList(new LambdaQueryWrapper<PomPurchaseContractHead>().eq(PomPurchaseContractHead::getSalesContractCode, req.getSalesContractHeadCode()));
        } else {
            heads = pomPurchaseContractHeadMapper.selectBatchIds(req.getPurchaseIdList());
        }
        if (CollectionUtils.isEmpty(heads)) {
            return new ArrayList<>();
        }

        List<PomPurchaseBriefDetailAllResp> briefRespList = BeanUtil.convert(heads, PomPurchaseBriefDetailAllResp.class);
        List<String> pomHeadIds = briefRespList.stream().map(PomPurchaseBriefDetailAllResp::getPurchaseContractHeadId).collect(Collectors.toList());
        //批量查询所有采购单对应的payment并设置payment到对应的采购单里
        Optional.ofNullable(pomPurchaseContractPaymentMapper.selectList(new LambdaQueryWrapper<PomPurchaseContractPayment>()
                .in(PomPurchaseContractPayment::getPurchaseContractHeadId, pomHeadIds)))
                .ifPresent(entities -> entities.stream().forEach(entity -> briefRespList.stream()
                        .filter(head -> head.getPurchaseContractHeadId().equals(entity.getPurchaseContractHeadId()))
                        .findFirst().get().getPomPurchaseContractPaymentVOList().add(BeanUtil.convert(entity, PomPurchaseContractPaymentVO.class))));

        if (req.getShowAttachmentNum()) {
            List<CountBillNumResp> attachmentNum = getAttachmentsNum(pomHeadIds, PomConstants.attachmentType.PURCHASE_HEAD_WAIT_APPROVE.getKey());
            List<CountBillNumResp> attachmentAlreadyNum = getAttachmentsNum(pomHeadIds, PomConstants.attachmentType.PURCHASE_HEAD_ALREADY_APPROVE.getKey());
            briefRespList.stream().forEach(head -> {
                //毛利率与毛利额
                List<PomPurchaseContractLine> pomPurchaseContractLines = getPomPurchaseContractLines(head.getPurchaseContractHeadId(), "销售单关联采购单简略详情列表");
                PomPurchaseContractHeadVO pomPurchaseContractHeadVO = BeanUtil.convert(head, PomPurchaseContractHeadVO.class);
                setGrossProfit(pomPurchaseContractHeadVO, pomPurchaseContractLines);
                BeanUtils.copyProperties(pomPurchaseContractHeadVO, head);

                //附件数量
                attachmentNum.stream().filter(attachmentResp -> attachmentResp.getBillId().equals(head.getPurchaseContractHeadId())).findFirst().ifPresent(v -> head.setWaitApproveAttachmentNum(v.getBillCount()));
                attachmentAlreadyNum.stream().filter(attachmentResp -> attachmentResp.getBillId().equals(head.getPurchaseContractHeadId())).findFirst().ifPresent(v -> head.setAlreadyApproveAttachmentNum(v.getBillCount()));
            });
        }

        if (req.getShowSalesContractCodePrimary()) {
            briefRespList.forEach(resp -> {
                SomSalesContractHeadEntity somSalesContractHeadEntity = somSalesContractHeadMapper.selectById(
                        resp.getSalesContractHeadId());
                String salesContractCode = somSalesContractMapper
                        .selectById(somSalesContractHeadEntity.getSalesContractId())
                        .getSalesContractCode();
                resp.setSalesContractCodePrimary(salesContractCode);
            });
        }
        return briefRespList;
    }

    private void setGrossProfit(PomPurchaseContractHeadVO headVO, List<PomPurchaseContractLine> pomLines) {
        List<CategoryBackgroundResp> categoryResp = getCategoryBackground(Arrays.asList(pomLines.get(0).getCategoryCode()));
        CategoryBackgroundResp category = categoryResp.get(0);
        headVO.setOrderGross(category.getOrderGross());
        if (category.getOrderGross() == BackCategoryConstant.OrderGross.PROFIT.getKey()) {
            //毛利标准
            headVO.setCategoryGrossProfit(category.getOrderGrossMargin());
        } else if (category.getOrderGross() == BackCategoryConstant.OrderGross.MARGIN.getKey()) {
            //毛利额标准
            headVO.setOrderGrossProfit(category.getOrderGrossProfit());
            List<PriceUtilDomain> PriceUtilDomainList = org.apache.commons.compress.utils.Lists.newArrayList();
            pomLines.stream().forEach(
                    var -> {
                        PriceUtilDomain priceUtilDomain = new PriceUtilDomain();
                        priceUtilDomain.setPurchasePrice(var.getPurchasePrice());
                        priceUtilDomain.setSalePrice(var.getOriginalPrice());
                        priceUtilDomain.setSaleQuantity(var.getPurchaseQuantity());
                        priceUtilDomain.setTaxCodeType(var.getTaxCodeType());
                        priceUtilDomain.setTaxCode(var.getTaxCode());
                        PriceUtilDomainList.add(priceUtilDomain);
                    }
            );
            PriceUtil priceUtil = PriceUtil.calculateListPrice(PriceUtilDomainList);
            //整单毛利额
            headVO.setOrderGrossProfitAll(priceUtil.getOrderGrossProfit());
        }
    }

    /**
     * 为商品设置计量方式和材质
     *
     * @param good
     */
    private void setTextureOfMaterial(List<PomPurchaseContractLineVO> good) {
        List<String> goods = good.stream().map(PomPurchaseContractLineVO::getItemId).collect(Collectors.toList());
        ResponseBO<List<ProductSkuInfoVo>> resp = productSkuFeign.listBySkuIds(goods);
        if (ObjectUtil.isEmpty(resp) || ObjectUtil.isEmpty(resp.getData())) {
            return;
        }
        //key-skuId  value-商品对象
        Map<String, ProductSkuInfoVo> textureMap = resp.getData().stream().collect(Collectors.toMap(ProductSkuInfoVo::getId, var -> var));
        good.forEach(var -> {
            ProductSkuInfoVo productSkuInfoVo = textureMap.get(var.getItemId());
            if (ObjectUtil.isNotEmpty(productSkuInfoVo)) {
                //临时商品处理
                if (productSkuInfoVo.getSpuId().equals("0")) {
                    String skuName = productSkuInfoVo.getSkuName();
                    String[] split = skuName.split("-");
                    //对新逻辑的临时商品的处理
                    if (split.length == 3) {
                        if (!"null".equals(split[1])) {
                            //材质
                            var.setTextureOfMaterial(split[1]);
                        }
                        if (!"null".equals(split[2])) {
                            var.setWeightUnit(split[2]);
                        }
                    }
                    //对之前临时商品的兼容处理
                    else if (split.length == 1) {
                        //材质
                        var.setTextureOfMaterial(null);
                        //计量方式
                        var.setWeightUnit(null);
                    } else {
                        //优化临时商品格式问题 先不抛出异常
                        //材质
                        var.setTextureOfMaterial(null);
                        //计量方式
                        var.setWeightUnit(null);
                    }
                }
                //正常商品的处理
                else {
                    JSONObject attributeAggregation = productSkuInfoVo.getAttributeAggregation();
                    if (ObjectUtil.isNotEmpty(attributeAggregation)) {
                        Map map = JSONObject.parseObject(JSONObject.toJSONString(attributeAggregation), Map.class);
                        //材质
                        var.setTextureOfMaterial((String) map.get("材质"));
                        //计量方式
                        var.setWeightUnit((String) map.get("计量方式"));
                    }

                }

                // 填充  辅助单位 辅助单位数量
                var.setSkuAuxiliaryNum(productSkuInfoVo.getSkuAuxiliaryNum());
                var.setSkuAuxiliaryUnit(productSkuInfoVo.getSkuAuxiliaryUnit());

            }
        });
    }

    private List<CategoryBackgroundResp> getCategoryBackground(List<String> categoryCodes) {
        List<String> categoryCodeList = categoryCodes.stream().map(categoryCode -> categoryCode.substring(0, 6)).collect(Collectors.toList());
        ResponseBO<List<CategoryBackgroundResp>> listResponseBO = null;
        try {
            listResponseBO = backgroundCategoryFeign.listByCodeList(categoryCodeList);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("调用商品服务获取类目详情失败,入参:{},返回值:{}", JSON.toJSONString(categoryCodeList), JSON.toJSONString(listResponseBO));
        }

        if (listResponseBO == null || listResponseBO.isFailed()) {
            log.error("调用商品服务获取类目详情失败,入参:{},返回值:{}", JSON.toJSONString(categoryCodeList), JSON.toJSONString(listResponseBO));
            throw new BusinessException(120_000, "调用商品服务获取类目详情失败");
        }
        return listResponseBO.getData();
    }

    public ResponseBO<PomPurchaseContractGenerateResp> updatePurchaseBySalesContract(PomPurchaseUpdateBySalesContractReq req) {
        req.valid();
        log.info(req.getOrderType().getValue() + "销售单联动更新采购单...,销售订单合同req:{}", JSONUtil.toJSONString(req));

        PomPurchaseContractGenerateResp resp = new PomPurchaseContractGenerateResp();
        List<PomPurchaseContractGenVO> pomPurchaseContractGenVOList = new ArrayList<>();
        PurchasePartitiveService partitiveService = orderFactory.getInstance(req.getOrderType());
        for (Map.Entry<String, PomPurchaseUpdateBySalesContractBO> entry : req.getPomContractUpdateBOMap().entrySet()) {
            String somSalesHeadId = entry.getKey();
            PomPurchaseUpdateBySalesContractBO updateBO = entry.getValue();

            SomSalesContractHeadEntity somSalesContractHeadEntity = getSomSalesContractHeadEntity(somSalesHeadId, "销售单联动更新采购单");

            //todo 销售商品行不变但是修改头信息,此时mr那边不会调用 -> 修改销售头要修改所有的采购头
            List<PomContractGenerateBO> updateBOList = updateBO.getPomContractGenerateUpdateBOList();
            if (!CollectionUtils.isEmpty(updateBOList)) {
                //更新采购单
                for (PomContractGenerateBO updateGenerateBO : updateBOList) {
                    PomPurchaseContractHead pomHead = Optional.ofNullable(pomPurchaseContractHeadMapper.selectOne(new LambdaQueryWrapper<PomPurchaseContractHead>().eq(PomPurchaseContractHead::getFromId, somSalesHeadId)
                            .eq(PomPurchaseContractHead::getSupplierId, updateGenerateBO.getSupplyId()))).orElseThrow(() -> new BusinessException(120_000, "销售单联动更新采购单-无法获取对应采购单"));
                    List<String> salesContractLineIds = updateGenerateBO.getSalesContractLineId();

                    salesContractLineIds.forEach(somSalesContractLineId -> {
                        SomSalesContractLineEntity somSalesContractLineEntity = somSalesContractLineMapper.selectById(somSalesContractLineId);
                        if (somSalesContractLineEntity == null) {
                            //删除商品行
                            pomPurchaseContractLineMapper.delete(new LambdaQueryWrapper<PomPurchaseContractLine>().eq(PomPurchaseContractLine::getFromLineId, somSalesContractLineId));
                            log.info("销售单联动更新采购单-删除商品行,fromLineId:{}", somSalesContractLineId);
                        } else {
                            PomPurchaseContractLine pomLine = pomPurchaseContractLineMapper.selectOne(new LambdaQueryWrapper<PomPurchaseContractLine>().eq(PomPurchaseContractLine::getFromLineId, somSalesContractLineId));
                            PomAssembleLineDTO lineDTO = partitiveService.assemblePomPurchaseLine(somSalesContractLineEntity, req, somSalesContractHeadEntity);
                            if (pomLine == null) {
                                //新增商品行
                                PomPurchaseContractLine pomLineAdd = BeanUtil.convert(lineDTO, PomPurchaseContractLine.class);
                                pomLineAdd.setVersionNo(PomConstants.INITIAL_VERSION);
                                pomLineAdd.setPurchaseContractHeadId(pomHead.getPurchaseContractHeadId());
                                pomLineAdd.setPurchaseContractLineCode(getPurchaseContractLineCode(pomHead.getPurchaseContractCode()));
                                pomPurchaseContractLineMapper.insert(pomLineAdd);
                            } else {
                                //修改商品行
                                BeanUtils.copyProperties(lineDTO, pomLine);
                                pomPurchaseContractLineMapper.updateById(pomLine);
                            }
                        }
                    });

                    List<SomSalesContractLineEntity> somSalesContractLineEntities = somSalesContractLineMapper.selectList(new LambdaQueryWrapper<SomSalesContractLineEntity>()
                            .eq(SomSalesContractLineEntity::getSalesContractHeadId, somSalesHeadId)
                            .eq(SomSalesContractLineEntity::getSupplierId, updateGenerateBO.getSupplyId()));
                    if (CollectionUtils.isEmpty(somSalesContractLineEntities)) {
                        throw new BusinessException(120_000, "未找到当前供应商下的销售明细信息");
                    }
                    PomAssembleHeadDTO headDTO = partitiveService.assemblePomPurchaseHead(somSalesContractHeadEntity, somSalesContractLineEntities.get(0), req, updateGenerateBO);
                    String tmpOrderStatus = pomHead.getOrderStatus();
                    ParameterUtil.copyWithOutNull(headDTO, pomHead);
                    pomHead.setOrderStatus(tmpOrderStatus);

                    priceCalculation(getPomPurchaseContractLines(pomHead.getPurchaseContractHeadId(), "销售单联动更新采购单"), getPomPurchasePayments(pomHead.getPurchaseContractHeadId()), pomHead);
                    updatePomPurchaseContractHead(pomHead, null);
                }
            }

            List<PomContractGenerateBO> addBOList = updateBO.getPomContractGenerateAddBOList();
            if (!CollectionUtils.isEmpty(addBOList)) {
                //创建采购单
                PomPurchaseContractGenerateReq generateReq = new PomPurchaseContractGenerateReq(req.getOrderType());
                generateReq.setSomSalesContractId(req.getSomSalesContractId());
                Map<String, List<PomContractGenerateBO>> pomContractGenerateBOMap = new HashMap<>();
                List<PomContractGenerateBO> generateBOList = BeanUtil.convert(addBOList, PomContractGenerateBO.class);
                pomContractGenerateBOMap.put(somSalesHeadId, generateBOList);
                generateReq.setPomContractGenerateBOMap(pomContractGenerateBOMap);
                generatePurchaseContract(generateReq);
            }

            List<PomContractGenerateBO> deleteBOList = updateBO.getPomContractGenerateDeleteBOList();
            if (!CollectionUtils.isEmpty(deleteBOList)) {
                //删除对应采购单以及明细
                for (PomContractGenerateBO deleteGenerateBO : deleteBOList) {
                    PomPurchaseContractHead pomHead = Optional.ofNullable(pomPurchaseContractHeadMapper.selectOne(new LambdaQueryWrapper<PomPurchaseContractHead>().eq(PomPurchaseContractHead::getFromId, somSalesHeadId)
                            .eq(PomPurchaseContractHead::getSupplierId, deleteGenerateBO.getSupplyId()))).orElseThrow(() -> new BusinessException(120_000, "销售单联动更新采购单-删除供方时无法获取对应采购单"));
                    pomPurchaseContractHeadMapper.deleteById(pomHead);
                    pomPurchaseContractLineMapper.delete(new LambdaQueryWrapper<PomPurchaseContractLine>().eq(PomPurchaseContractLine::getPurchaseContractHeadId, pomHead.getPurchaseContractHeadId()));
                }
            }

            pomPurchaseContractGenVOList.addAll(BeanUtil.convert(getPomPurchaseContractHeads(somSalesHeadId, "销售单联动更新采购单"), PomPurchaseContractGenVO.class));
        }

        resp.setPomPurchaseContractGenVOList(pomPurchaseContractGenVOList);
        return ResponseUtil.ok(resp);
    }

    public ResponseBO<PomPurchaseContractGenerateResp> updatePurchaseBySalesContract(PomPurchaseContractUpdateBySalesReq req) {
        req.valid();
        log.info(req.getOrderType().getValue() + "销售单联动更新采购单,入参:{}", JSONUtil.toJSONString(req));

        List<PomPurchaseContractGenVO> pomPurchaseContractGenVOList = new ArrayList<>();

        for (Map.Entry<String, List<PomContractGenerateBO>> entry : req.getPomContractGenerateBOMap().entrySet()) {
            String somSalesHeadId = entry.getKey();
            SomSalesContractHeadEntity somSalesContractHeadEntity = getSomSalesContractHeadEntity(somSalesHeadId, "销售单联动更新采购单");
            Map<String, PomPurchaseContractHead> currentPomBySupplierIdMap = getPomPurchaseContractHeads(somSalesHeadId, "销售单联动更新采购单").stream().collect(Collectors.toMap(PomPurchaseContractHead::getSupplierId, pomPurchaseContractHead -> pomPurchaseContractHead));

            List<PomContractGenerateBO> generateBOList = new ArrayList<>();
            PurchasePartitiveService partitiveService = orderFactory.getInstance(req.getOrderType());
            for (PomContractGenerateBO generateBO : entry.getValue()) {
                if (!currentPomBySupplierIdMap.containsKey(generateBO.getSupplyId())) {
                    //创建新的采购单
                    generateBOList.add(generateBO);
                } else {
                    PomPurchaseContractHead purchaseContractHead = currentPomBySupplierIdMap.get(generateBO.getSupplyId());
                    List<String> salesContractLineIds = generateBO.getSalesContractLineId();
                    salesContractLineIds.forEach(salesContractLineId -> {
                        SomSalesContractLineEntity somSalesContractLineEntity = somSalesContractLineMapper.selectById(salesContractLineId);
                        PomPurchaseContractLine pomLine = pomPurchaseContractLineMapper.selectOne(new LambdaQueryWrapper<PomPurchaseContractLine>().eq(PomPurchaseContractLine::getFromLineId, salesContractLineId));
                        PomAssembleLineDTO lineDTO = partitiveService.assemblePomPurchaseLine(somSalesContractLineEntity, req, somSalesContractHeadEntity);
                        if (pomLine == null) {
                            //新增商品行
                            PomPurchaseContractLine pomLineAdd = BeanUtil.convert(lineDTO, PomPurchaseContractLine.class);
                            pomLineAdd.setVersionNo(PomConstants.INITIAL_VERSION);
                            pomLineAdd.setPurchaseContractHeadId(purchaseContractHead.getPurchaseContractHeadId());
                            pomLineAdd.setPurchaseContractLineCode(getPurchaseContractLineCode(purchaseContractHead.getPurchaseContractCode()));
                            pomPurchaseContractLineMapper.insert(pomLineAdd);
                            log.info("销售单联动更新采购单-新增商品行,pomHeadId:{},fromLineId:{}", purchaseContractHead.getPurchaseContractHeadId(), salesContractLineId);
                        } else {
                            //修改商品行
                            BeanUtils.copyProperties(lineDTO, pomLine);
                            pomPurchaseContractLineMapper.updateById(pomLine);
                            log.info("销售单联动更新采购单-修改商品行,pomHeadId:{},fromLineId:{}", purchaseContractHead.getPurchaseContractHeadId(), salesContractLineId);
                        }
                    });

                    List<String> currentPurchaseLines = getPomPurchaseContractLines(purchaseContractHead.getPurchaseContractHeadId(), "销售单联动更新采购单").stream().map(PomPurchaseContractLine::getFromLineId).collect(Collectors.toList());
                    currentPurchaseLines.forEach(purchaseFromLineId -> {
                        if (!salesContractLineIds.contains(purchaseFromLineId)) {
                            //删除商品行
                            pomPurchaseContractLineMapper.delete(new LambdaQueryWrapper<PomPurchaseContractLine>().eq(PomPurchaseContractLine::getFromLineId, purchaseFromLineId));
                            log.info("销售单联动更新采购单-删除商品行,purchaseFromLineId:{}", purchaseFromLineId);
                        }
                    });

                    List<SomSalesContractLineEntity> somSalesContractLineEntities = somSalesContractLineMapper.selectList(new LambdaQueryWrapper<SomSalesContractLineEntity>()
                            .eq(SomSalesContractLineEntity::getSalesContractHeadId, somSalesHeadId)
                            .eq(SomSalesContractLineEntity::getSupplierId, generateBO.getSupplyId()));
                    if (CollectionUtils.isEmpty(somSalesContractLineEntities)) {
                        throw new BusinessException(120_000, "未找到当前供应商下的销售明细信息");
                    }
                    PomAssembleHeadDTO headDTO = partitiveService.assemblePomPurchaseHead(somSalesContractHeadEntity, somSalesContractLineEntities.get(0), req, generateBO);
                    String tmpOrderStatus = purchaseContractHead.getOrderStatus();
                    ParameterUtil.copyWithOutNull(headDTO, purchaseContractHead);
                    purchaseContractHead.setOrderStatus(tmpOrderStatus);

                    priceCalculation(getPomPurchaseContractLines(purchaseContractHead.getPurchaseContractHeadId(), "销售单联动更新采购单"), getPomPurchasePayments(purchaseContractHead.getPurchaseContractHeadId()), purchaseContractHead);
                    updatePomPurchaseContractHead(purchaseContractHead, null);
                }
            }

            if (!CollectionUtils.isEmpty(generateBOList)) {
                //创建采购单
                PomPurchaseContractGenerateReq generateReq = new PomPurchaseContractGenerateReq(req.getOrderType());
                generateReq.setSomSalesContractId(req.getSomSalesContractId());
                Map<String, List<PomContractGenerateBO>> pomContractGenerateBOMap = new HashMap<>();
                pomContractGenerateBOMap.put(somSalesContractHeadEntity.getSalesContractHeadId(), generateBOList);
                generateReq.setPomContractGenerateBOMap(pomContractGenerateBOMap);
                generatePurchaseContract(generateReq);
            }

            List<PomPurchaseContractHead> purchaseContractHeads = getPomPurchaseContractHeads(somSalesHeadId, "销售单联动更新采购单");
            List<String> pomSupplierIdsBySalesHead = purchaseContractHeads.stream().map(PomPurchaseContractHead::getSupplierId).collect(Collectors.toList());
            if (pomSupplierIdsBySalesHead.size() > entry.getValue().size()) {
                List<String> somSupplierIds = entry.getValue().stream().map(PomContractGenerateBO::getSupplyId).collect(Collectors.toList());
                pomSupplierIdsBySalesHead.forEach(pomSupplierId -> {
                    if (!somSupplierIds.contains(pomSupplierId)) {
                        //删除对应采购单以及明细
                        PomPurchaseContractHead pomHead = Optional.ofNullable(pomPurchaseContractHeadMapper.selectOne(new LambdaQueryWrapper<PomPurchaseContractHead>()
                                .eq(PomPurchaseContractHead::getFromId, somSalesHeadId)
                                .eq(PomPurchaseContractHead::getSupplierId, pomSupplierId))).orElseThrow(() -> new BusinessException(120_000, "销售单联动更新采购单-删除供方时无法获取对应采购单"));
                        pomPurchaseContractHeadMapper.deleteById(pomHead);
                        pomPurchaseContractLineMapper.delete(new LambdaQueryWrapper<PomPurchaseContractLine>().eq(PomPurchaseContractLine::getPurchaseContractHeadId, pomHead.getPurchaseContractHeadId()));
                    }
                });
            }

            pomPurchaseContractGenVOList.addAll(BeanUtil.convert(purchaseContractHeads, PomPurchaseContractGenVO.class));
        }

        PomPurchaseContractGenerateResp resp = new PomPurchaseContractGenerateResp();
        resp.setPomPurchaseContractGenVOList(pomPurchaseContractGenVOList);
        return ResponseUtil.ok(resp);
    }

    @Override
    public ResponseBO<PomPurchaseContractHistoryResp> pomPurchaseContractHistoryList(PomPurchaseContractDetailReq req) {
        List<PomPurchaseContractHeadVersionEntity> pomPurchaseContractHeadHistoryEntityList = pomPurchaseContractHeadVersionMapper.selectList(new QueryWrapper<PomPurchaseContractHeadVersionEntity>().lambda().eq(PomPurchaseContractHeadVersionEntity::getPomContractHeadId, req.getPurchaseContractHeadId()));
        List<PomPurchaseContractHistoryVO> pomPurchaseContractHistoryVOList = new ArrayList<>();
        for (PomPurchaseContractHeadVersionEntity entity : pomPurchaseContractHeadHistoryEntityList) {
            PomPurchaseContractHistoryVO pomPurchaseContractHistoryVO = new PomPurchaseContractHistoryVO();
            BeanUtils.copyProperties(entity, pomPurchaseContractHistoryVO);
            PomPurchaseContractDetailResp pomPurchaseContractDetailResp = JSON.parseObject(entity.getHeadJson(), PomPurchaseContractDetailResp.class);
            if (ObjectUtil.isEmpty(pomPurchaseContractDetailResp) || ObjectUtil.isEmpty(pomPurchaseContractDetailResp.getPomPurchaseContractHeadVO())) {
                throw new BusinessException(120_000, "采购订单Json反序列化对象为空");
            }
            pomPurchaseContractHistoryVO.setPurchaseContractCode(pomPurchaseContractDetailResp.getPomPurchaseContractHeadVO().getPurchaseContractCode());
            BigDecimal totalAmount = pomPurchaseContractDetailResp.getPomPurchaseContractHeadVO().getTotalAmount();
            if (totalAmount != null) {
                //可能在json转化中损失精度,临时处理为小数点后保留2位
                String totalAmountString = totalAmount.toString();
                String[] strings = totalAmountString.split("\\.");
                if (strings.length >= 2) {
                    if (strings[1].length() == 1) {
                        totalAmountString = totalAmountString + "0";
                    }
                }
                pomPurchaseContractHistoryVO.setTotalAmount(totalAmountString);
            }
            pomPurchaseContractHistoryVOList.add(pomPurchaseContractHistoryVO);
        }
        PomPurchaseContractHistoryResp pomPurchaseContractHistoryResp = new PomPurchaseContractHistoryResp();
        pomPurchaseContractHistoryResp.setPomPurchaseContractHistoryVOList(pomPurchaseContractHistoryVOList);
        return ResponseUtil.ok(pomPurchaseContractHistoryResp);
    }

    @Override
    public ResponseBO<ShipmentPlanResp> pomPurchaseContractShipmentPlanList(PomPurchaseContractDetailReq req) {
        PomPurchaseContractHead pomPurchaseContractHead = pomPurchaseContractHeadMapper.selectById(req.getPurchaseContractHeadId());
        if (ObjectUtil.isEmpty(pomPurchaseContractHead)) {
            log.error("未找到当前采购单:{}", req.getPurchaseContractHeadId());
            throw new BusinessException(120_000, "未找到当前采购单");
        }

        List<PomPurchaseContractLine> pomPurchaseContractLines = pomPurchaseContractLineMapper.selectList(new QueryWrapper<PomPurchaseContractLine>().lambda().eq(PomPurchaseContractLine::getPurchaseContractHeadId, req.getPurchaseContractHeadId()));
        if (CollectionUtil.isEmpty(pomPurchaseContractLines)) {
            log.error("当前采购单的明细信息为空:{}", req.getPurchaseContractHeadId());
            throw new BusinessException(120_000, "当前采购单的明细信息为空");
        }

        //对应的销售明细Id集合
        List<String> somLineIdList = pomPurchaseContractLines.stream().map(PomPurchaseContractLine::getFromLineId).distinct().collect(Collectors.toList());
        List<SomSalesContractLineEntity> somSalesContractLineEntities = somSalesContractLineMapper.selectBatchIds(somLineIdList);
        if (CollectionUtil.isEmpty(somSalesContractLineEntities) || somSalesContractLineEntities.size() != somLineIdList.size()) {
            log.error("当前采购单明细数据无法与来源行一一对应:{}", req.getPurchaseContractHeadId());
            throw new BusinessException(120_000, "当前采购单明细数据无法与来源行一一对应");
        }
        //相同销售单下skuId不重复
        Map<String, SomSalesContractLineEntity> somSalesContractLineEntityMap = somSalesContractLineEntities.stream().collect(Collectors.toMap(SomSalesContractLineEntity::getSkuId, somSalesContractLineEntity -> somSalesContractLineEntity));

        //同个采购单肯定在相同销售单下
        SomSalesContractHeadEntity somSalesContractHeadEntity = somSalesContractHeadMapper.selectById(somSalesContractLineEntities.get(0).getSalesContractHeadId());
        if (ObjectUtil.isEmpty(somSalesContractHeadEntity) || StringUtils.isBlank(somSalesContractHeadEntity.getSalesContractId())) {
            log.error("来源单据的主表信息为空或对应合同Id为空:{}", JSON.toJSONString(somSalesContractHeadEntity));
            throw new BusinessException(120_000, "来源单据的主表信息为空或对应合同Id为空");
        }

        ShipmentPlanResp shipmentPlanResp = new ShipmentPlanResp();
        List<ShipmentPlanHeadVO> shipmentPlanHeadVOList = new ArrayList<>();
        shipmentPlanResp.setShipmentPlanHeadVOList(shipmentPlanHeadVOList);

        //校验返回数据行数
        List<ShipmentPlanSaleVo> shipmentPlanBySalesId = shipmentPlanBatchMapper.findShipmentPlanByOtherId(
                new ShipmentPlanReq(somSalesContractHeadEntity.getSalesContractId(), null, somSalesContractLineEntities.stream().map(SomSalesContractLineEntity::getSkuId).collect(Collectors.toList())));
        if (CollectionUtils.isEmpty(shipmentPlanBySalesId)) {
            if (pomPurchaseContractHead.getOrderStatus().equals(PomConstants.OrderStatus.CLOSE.getKey()) && pomPurchaseContractLines.size() == 1) {
                log.info("采购单剩余商品明细为空,无对应批次信息:{}", pomPurchaseContractHead.getPurchaseContractHeadId());
                return ResponseUtil.ok(shipmentPlanResp);
            }
            log.error("未找到当前单据的批次信息:{}", JSON.toJSONString(shipmentPlanBySalesId));
            throw new BusinessException(120_000, "未找到当前单据的批次信息");
        }

        //用于获取合计数量
        Map<String, BigDecimal> lineMapForTotalNum = pomPurchaseContractLines.stream().collect(Collectors.toMap(PomPurchaseContractLine::getItemId, pomPurchaseContractLine -> pomPurchaseContractLine.getPurchaseQuantity()));
        //按照批次信息组装返回值
        Set<String> skuIdSet = new HashSet<>();
        shipmentPlanBySalesId.forEach(shipmentPlanSaleVo -> {
            String batchNme = shipmentPlanSaleVo.getSomShipmentBatchName();
            if (StringUtil.isBlank(batchNme)) {
                LocalDateTime expectedReceiptDate = shipmentPlanSaleVo.getExpectedReceiptDate();
                if (ObjectUtil.isEmpty(expectedReceiptDate)) {
                    throw new BusinessException(120_000, "最晚交付时间为空");
                }
                DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                batchNme = df.format(expectedReceiptDate);
            }

            Integer index = containBatchName(shipmentPlanHeadVOList, batchNme);
            ShipmentPlanLineVO shipmentPlanLineVO = new ShipmentPlanLineVO();
            if (CollectionUtil.isEmpty(shipmentPlanHeadVOList) || index < 0) {
                //新增返回批次
                List<ShipmentPlanLineVO> shipmentPlanLineVOList = new ArrayList<>();

                shipmentPlanLineVOList.add(shipmentPlanLineVO);
                ShipmentPlanHeadVO shipmentPlanHeadVO = new ShipmentPlanHeadVO();
                shipmentPlanHeadVO.setSomShipmentBatchName(batchNme);
                shipmentPlanHeadVO.setExpectedReceiptDate(shipmentPlanSaleVo.getExpectedReceiptDate());
                shipmentPlanHeadVO.setShipmentPlanLineVOList(shipmentPlanLineVOList);
                shipmentPlanHeadVOList.add(shipmentPlanHeadVO);
                BeanUtils.copyProperties(shipmentPlanSaleVo, shipmentPlanLineVO);
                SomSalesContractLineEntity somSalesContractLineEntity = somSalesContractLineEntityMap.get(shipmentPlanSaleVo.getSkuId());
                if (ObjectUtil.isEmpty(somSalesContractLineEntity)) {
                    log.error("无法从来源单据获取当前商品Id对应的信息:商品Id{}", shipmentPlanSaleVo.getSkuId());
                    throw new BusinessException(120_000, "无法从来源单据获取当前商品Id对应的信息");
                }
                //复制商品信息
                BeanUtils.copyProperties(somSalesContractLineEntity, shipmentPlanLineVO);
            } else {
                BeanUtils.copyProperties(shipmentPlanSaleVo, shipmentPlanLineVO);
                SomSalesContractLineEntity somSalesContractLineEntity = somSalesContractLineEntityMap.get(shipmentPlanSaleVo.getSkuId());
                if (ObjectUtil.isEmpty(somSalesContractLineEntity)) {
                    log.error("无法从来源单据获取当前商品Id对应的信息:商品Id{}", shipmentPlanSaleVo.getSkuId());
                    throw new BusinessException(120_000, "无法从来源单据获取当前商品Id对应的信息");
                }
                //复制商品信息
                BeanUtils.copyProperties(somSalesContractLineEntity, shipmentPlanLineVO);
                ShipmentPlanHeadVO shipmentPlanHeadVO = shipmentPlanHeadVOList.get(index);
                shipmentPlanHeadVO.getShipmentPlanLineVOList().add(shipmentPlanLineVO);
            }

            //合计数量
            shipmentPlanLineVO.setTotalQuantity(lineMapForTotalNum.get(shipmentPlanLineVO.getSkuId()));
            skuIdSet.add(shipmentPlanLineVO.getSkuId());
        });

        if (!CollectionUtils.isEmpty(skuIdSet)) {
            ResponseBO<List<ProductSkuInfoVo>> listResponseBO = productSkuFeign.listBySkuIds(new ArrayList<>(skuIdSet));
            if (listResponseBO != null && ObjectUtil.isNotEmpty(listResponseBO.getData())) {
                Map<String, ProductSkuInfoVo> skuInfoVoMap = listResponseBO
                        .getData()
                        .stream()
                        .collect(Collectors.toMap(k -> k.getId(), v -> v));

                shipmentPlanResp.getShipmentPlanHeadVOList().stream().forEach(head -> head.getShipmentPlanLineVOList().forEach(line ->{
                    if (skuInfoVoMap.containsKey(line.getSkuId())) {
                        ProductSkuInfoVo skuInfoVo = skuInfoVoMap.get(line.getSkuId());
                        line.setSkuAuxiliaryNum(skuInfoVo.getSkuAuxiliaryNum());
                        line.setSkuAuxiliaryUnit(skuInfoVo.getSkuAuxiliaryUnit());
                    }
                }));
            }
        }

        return ResponseUtil.ok(shipmentPlanResp);
    }

    private Integer containBatchName(List<ShipmentPlanHeadVO> shipmentPlanHeadVOS, String batchNme) {
        if (CollectionUtils.isEmpty(shipmentPlanHeadVOS)) {
            return -1;
        }

        for (int i = 0; i < shipmentPlanHeadVOS.size(); i++) {
            if (shipmentPlanHeadVOS.get(i).getSomShipmentBatchName().equals(batchNme)) {
                return i;
            }
        }

        return -1;
    }

    @Override
    @Transactional
    public ResponseBO<Boolean> pomPurchaseContractRemoveLine(PomPurchaseContractRemoveLineReq req) {
        log.info("采购订单移除商品操作,入参:{}", JSON.toJSONString(req));
        if (ObjectUtil.isEmpty(req) || StringUtils.isBlank(req.getSomSalesContractHeadId()) || CollectionUtil.isEmpty(req.getPomRemoveLineBOList())) {
            log.error("采购订单移除商品-采购订单移除商品参数校验失败");
            throw new BusinessException(120_000, "参数校验失败");
        }
        for (PomPurchaseContractRemoveLineBO pomRemoveLineBO : req.getPomRemoveLineBOList()) {
            //供应商和来源单据确认时采购单唯一
            PomPurchaseContractHead purchaseContractHead = pomPurchaseContractHeadMapper.selectOne(new QueryWrapper<PomPurchaseContractHead>().lambda().eq(PomPurchaseContractHead::getSalesContractHeadId, req.getSomSalesContractHeadId())
                    .eq(PomPurchaseContractHead::getSupplierId, pomRemoveLineBO.getSupplierId()));
            log.info("采购订单移除商品操作,循环采购单Id:{}", purchaseContractHead.getPurchaseContractHeadId());
            if (ObjectUtil.isEmpty(purchaseContractHead)) {
                log.error("采购订单移除商品-未找到采购单，销售订单Id：{}，供应商Id：{}", req.getSomSalesContractHeadId(), pomRemoveLineBO.getSupplierId());
                throw new BusinessException(120_000, "未找到当前订单");
            }

            //校验订单状态
            purchaseComponent.checkStatus(StatusValidity.getPomRemoveLine(), purchaseContractHead, "采购订单移除商品");

            //获取采购明细
            List<PomPurchaseContractLine> pomPurchaseContractLineList = getPomPurchaseContractLines(purchaseContractHead.getPurchaseContractHeadId(), "采购订单移除商品");

            List<PomPurchaseContractLine> pomLineFilter = pomPurchaseContractLineList.stream().filter(pomPurchaseContractLine -> pomPurchaseContractLine.getItemId().equals(pomRemoveLineBO.getSkuId())).collect(Collectors.toList());
            if (CollectionUtil.isEmpty(pomLineFilter)) {
                log.error("采购订单移除商品-采购单明细中未找到待删除的对应商品，采购单Id:{},pomRemoveLineBO:{}", purchaseContractHead.getPurchaseContractHeadId(), JSON.toJSONString(pomRemoveLineBO));
                throw new BusinessException(120_000, "采购单明细中未找到待删除的对应商品");
            }

            if (pomPurchaseContractLineList.size() == pomLineFilter.size()) {
                //当删除所有商品行时仅关闭订单
                purchaseContractHead.setSupplyRefuseReason("需方取消交易");
                purchaseComponent.updatePurchaseOrderStatus(purchaseContractHead, PomConstants.OrderStatus.CLOSE, "采购订单移除商品");
                log.info("采购单移除商品操作-关闭采购单,采购单号:{}", purchaseContractHead.getPurchaseContractHeadId());
            } else {
                //删除对应商品明细
                List<String> list = pomLineFilter.stream().map(PomPurchaseContractLine::getPurchaseContractLineId).collect(Collectors.toList());
                log.info("采购单移除商品操作-删除采购单商品明细,采购单号:{}", JSON.toJSONString(list));
                pomPurchaseContractLineMapper.deleteBatchIds(list);

                //修改状态部分供货为整单供货
                List<PomPurchaseContractLine> pomPurchaseContractLineListNow = pomPurchaseContractLineMapper.selectList(new LambdaQueryWrapper<PomPurchaseContractLine>().eq(PomPurchaseContractLine::getPurchaseContractHeadId, purchaseContractHead.getPurchaseContractHeadId()));
                boolean isCompleteSupply = true;
                for (PomPurchaseContractLine pomPurchaseContractLine : pomPurchaseContractLineListNow) {
                    if (pomPurchaseContractLine.getCanSupply().equals(PomConstants.OrderLineStatus.NO_SUPPLY.key)) {
                        log.info("采购单移除商品操作-当前采购单商品明细中仍存在不可供货商品,采购单明细号:{}", pomPurchaseContractLine.getPurchaseContractLineId());
                        isCompleteSupply = false;
                    }
                }
                //剩余商品全可供货且主单状态为部分供货修改主单状态为全部供货
                if (isCompleteSupply && purchaseContractHead.getOrderStatus().equals(PomConstants.OrderStatus.SUPPLY_PART.getKey())) {
                    purchaseComponent.updatePurchaseOrderStatus(purchaseContractHead, PomConstants.OrderStatus.SUPPLY_ALL, "采购订单移除商品");
                    log.info("采购单移除商品操作-修改采购单状态为全部供货,采购单号:{}", purchaseContractHead.getPurchaseContractHeadId());
                }

                //重新计算价格
                List<PomPurchaseContractPayment> pomPurchaseContractPaymentList = getPomPurchasePayments(purchaseContractHead.getPurchaseContractHeadId());
                priceCalculation(pomPurchaseContractLineListNow, pomPurchaseContractPaymentList, purchaseContractHead);
                log.info("采购单移除商品操作-重新计算采购单价格,采购单号:{}", purchaseContractHead.getPurchaseContractHeadId());
            }

            pomPurchaseContractHeadModel.checkIfUpdateAndCreateVersion(purchaseContractHead);
        }

        return ResponseUtil.ok();
    }

    @Override
    @Transactional
    public ResponseBO<Boolean> shutPurchaseContract(PomPurchaseContractShutReq req) {
        if (ObjectUtil.isEmpty(req) || ObjectUtil.isEmpty(req.getType()) || StringUtil.isBlank(req.getReason())) {
            log.error("采购订单关闭参数校验失败,入参:{}", JSON.toJSONString(req));
            throw new BusinessException(120_000, "参数校验失败");
        }

        List<PomPurchaseContractHead> pomPurchaseContractHeadList = new ArrayList<>();
        if (req.getType().equals(0)) {
            pomPurchaseContractHeadList = getPomPurchaseContractHeads(req.getOrderId(), "采购订单关闭");
        } else if (req.getType().equals(1)) {
            if (StringUtil.isBlank(req.getOrderId())) {
                PomPurchaseContractHead pomPurchaseContractHead = pomPurchaseContractHeadMapper.selectOne(new LambdaQueryWrapper<PomPurchaseContractHead>().eq(PomPurchaseContractHead::getPurchaseContractCode, req.getOrderCode()));
                pomPurchaseContractHeadList.add(pomPurchaseContractHead);
            } else {
                PomPurchaseContractHead pomPurchaseContractHead = getPomPurchaseContractHead(req.getOrderId(), "采购订单关闭");
                pomPurchaseContractHeadList.add(pomPurchaseContractHead);
            }
        }

        if (CollectionUtil.isEmpty(pomPurchaseContractHeadList)) {
            log.error("采购订单关闭操作-未找采购单，入参:{}", JSON.toJSONString(req));
            throw new BusinessException(120_000, "未找采购单");
        }

        pomPurchaseContractHeadList.forEach(pomPurchaseContractHead -> {
            //校验订单状态
            //如果要取消生效状态的判断,需要分别确认标品(不影响其他单据)和定采(未确认)
            if (!StatusValidity.getPomClose().contains(pomPurchaseContractHead.getOrderStatus())) {
                log.error("采购订单关闭操作-采购单状态校验失败,采购单Id:{}", pomPurchaseContractHead.getPurchaseContractHeadId());
                throw new BusinessException(120_000, "平台已接单，请联系客服4009933077。");
            }
            purchaseComponent.setPurchaseOrderStatus(pomPurchaseContractHead, PomConstants.OrderStatus.CLOSE, "采购订单关闭");
            pomPurchaseContractHead.setSupplyRefuseReason(req.getReason());
        });

        pomPurchaseContractHeadService.updateBatchById(pomPurchaseContractHeadList);
        log.info("采购订单关闭操作,受影响采购订单Id：{}", JSON.toJSONString(pomPurchaseContractHeadList.stream().map(PomPurchaseContractHead::getPurchaseContractHeadId).collect(Collectors.toList()).toArray()));
        return ResponseUtil.ok();
    }

    private void purchaseOrderDelayClose(PomPurchaseContractHead pomPurchaseContractHead) {
        MissionConfigRuleResp ruleResp = getRule(pomPurchaseContractHead);
        if (ObjectUtil.isEmpty(ruleResp) || ObjectUtil.isEmpty(ruleResp.getMaxAcceptMinute())) {
            log.error("提交采购订单-获取接单时限配置失败,采购单号:{},返回值:{}", pomPurchaseContractHead.getPurchaseContractCode(), JSON.toJSONString(ruleResp));
            throw new BusinessException(120_000, "获取接单时限配置失败");
        }
        log.info("提交采购订单-设置接单定时...采购单号:{},接单时限:{}", pomPurchaseContractHead.getPurchaseContractCode(), ruleResp.getMaxAcceptMinute());
        if (ruleResp.getMaxAcceptMinute() != 0) {
            delayQueueService.initDelay(DelayQueueService.KEY_PURCHASE.concat(pomPurchaseContractHead.getPurchaseContractHeadId()), pomPurchaseContractHead.getPurchaseContractHeadId(), ruleResp.getMaxAcceptMinute(), TimeUnit.MINUTES, "PomPurchaseContracCallBack");
        }
    }

    @Override
    @Transactional
    public ResponseBO<Boolean> validPurchaseContract(PomPurchaseContractValidReq req) {
        log.info("采购订单生效操作,入参:{}", JSON.toJSONString(req));
        if (ObjectUtil.isEmpty(req) || CollectionUtils.isEmpty(req.getSomSalesContractHeadIdList())) {
            log.error("采购订单生效参数校验失败");
            throw new BusinessException(120_000, "参数校验失败");
        }

        for (String somSalesContractHeadId : req.getSomSalesContractHeadIdList()) {
            List<PomPurchaseContractHead> pomPurchaseContractHeadList = pomPurchaseContractHeadMapper.selectList(new QueryWrapper<PomPurchaseContractHead>().lambda()
                    .eq(PomPurchaseContractHead::getSalesContractHeadId, somSalesContractHeadId)
                    .ne(PomPurchaseContractHead::getOrderStatus, PomConstants.OrderStatus.CLOSE.getKey()));
            if (CollectionUtil.isEmpty(pomPurchaseContractHeadList)) {
                log.error("采购订单生效操作-未找到销售单对应的采购单，销售单Id:{}", somSalesContractHeadId);
                throw new BusinessException(120_000, "未找到销售单对应的采购单");
            }

            pomPurchaseContractHeadList.forEach(pomPurchaseContractHead -> {
                //校验订单状态
                purchaseComponent.checkStatus(StatusValidity.getPomValid(), pomPurchaseContractHead, "采购订单生效");

                purchaseComponent.setPurchaseOrderStatus(pomPurchaseContractHead, PomConstants.OrderStatus.VALID, "采购订单生效");
                pomPurchaseContractHead.setSupplyRefuseReason("来源单据(销售订单)发起生效操作");
                pomPurchaseContractHead.setSignDate(LocalDateTime.now());
            });

            pomPurchaseContractHeadService.updateBatchById(pomPurchaseContractHeadList);
            log.info("来源单据(销售订单)发起生效操作,受影响采购订单Id：{}", JSON.toJSONString(pomPurchaseContractHeadList.stream().map(PomPurchaseContractHead::getPurchaseContractHeadId).collect(Collectors.toList()).toArray()));
        }

        return ResponseUtil.ok();
    }

    @Override
    @Transactional
    public ResponseBO<Boolean> logisticalPurchaseContract(PomPurchaseContractShipReq req) {
        log.info("采购订单修改物流状态...,入参:{}", JSON.toJSONString(req));
        if (req == null || !req.validVal()) {
            throw new BusinessException(120_000, "参数校验失败");
        }

        String purchaseContractHeadId = req.getPurchaseContractHeadId();
        PomPurchaseContractHead pomPurchaseContractHead = pomPurchaseContractHeadMapper.selectById(purchaseContractHeadId);
        if (ObjectUtil.isEmpty(pomPurchaseContractHead)) {
            log.error("采购订单修改物流状态,未找到当前采购单:{}", purchaseContractHeadId);
            throw new BusinessException(120_000, "未找到当前采购单");
        }

        purchaseComponent.checkStatus(StatusValidity.getPomShip(), pomPurchaseContractHead, "采购订单修改物流状态");

        if (StringUtil.isBlank(pomPurchaseContractHead.getFromId())) {
            log.error("采购订单修改物流状态,当前采购单单未关联销售单:{}", purchaseContractHeadId);
            throw new BusinessException(120_000, "当前采购单单未关联销售单，修改订单物流状态失败");
        }

        purchaseComponent.updatePurchaseOrderStatus(pomPurchaseContractHead, PomConstants.OrderStatus.getOrderStatus(req.getStatus()), "采购订单修改物流状态");
        log.info("采购订单修改物流状态成功,受影响单据Id:{}", pomPurchaseContractHead.getPurchaseContractHeadId());

        //查找销售单关联的所有进入物流阶段的采购单
        List<PomPurchaseContractHead> pomPurchaseContractHeads = pomPurchaseContractHeadMapper.selectList(new LambdaQueryWrapper<PomPurchaseContractHead>().eq(PomPurchaseContractHead::getFromId, pomPurchaseContractHead.getFromId()).
                in(PomPurchaseContractHead::getOrderStatus, req.getLegalPomStatus()));
        if (!CollectionUtil.isEmpty(pomPurchaseContractHeads)) {
            boolean receivedAll = true;
            String somStatus = SomSalesContractHeadConstant.OrderStatus.WAIT_LOGISTICS_SIGN.getKey();
            for (PomPurchaseContractHead purchaseContractHead : pomPurchaseContractHeads) {
                String orderStatus = purchaseContractHead.getOrderStatus();
                if (orderStatus.equals(PomConstants.OrderStatus.SHIPPED.getKey())) {
                    //存在待签收的采购单->销售单也是待签收
                    receivedAll = false;
                    break;
                }
            }
            if (receivedAll) {
                somStatus = SomSalesContractHeadConstant.OrderStatus.LOGISTICS_SIGNED.getKey();
            }

            //修改销售单的状态
            somSalesContractService.updateOrderLogisticsStatus(pomPurchaseContractHead.getFromId(), somStatus);
            log.info("采购订单修改物流状态触发修改关联销售单状态成功,受影响单据Id:{}", pomPurchaseContractHead.getFromId());
        }
        return ResponseUtil.ok();
    }

    /**
     * 线下签署(当前线下订单用)
     *
     * @param req
     * @return PomPurchaseContractSignResp
     */
    @Override
    @Transactional
    public ResponseBO<Boolean> signPurchaseContract(PomPurchaseContractSignReq req) {
        String purchaseContractHeadId = req.getPurchaseContractHeadId();
        PomPurchaseContractHead pomPurchaseContractHead = getPomPurchaseContractHead(purchaseContractHeadId, "采购单线下签约");

        purchaseComponent.checkStatus(StatusValidity.getPomSign(), pomPurchaseContractHead, "采购单线下签约");

        BeanUtils.copyProperties(req, pomPurchaseContractHead);
        pomPurchaseContractHead.setSignDate(LocalDateTime.now());
        purchaseComponent.updatePurchaseOrderStatus(pomPurchaseContractHead, PomConstants.OrderStatus.VALID, "采购单线下签约");
        log.info("采购单线下签约成功,受影响订单Id:{}", purchaseContractHeadId);

        return ResponseUtil.ok();
    }

    /**
     * 采购单完结(当前线下订单用)
     *
     * @param req
     * @return com.vd.canary.core.bo.ResponseBO<java.lang.Boolean>
     */
    @Override
    public ResponseBO<Boolean> finishPurchaseContract(PomPurchaseContractHeadComReq req) {
        //线下完结不支持批量
        String purchaseContractHeadId = req.getPurchaseContractHeadIdList().get(0);
        PomPurchaseContractHead pomPurchaseContractHead = getPomPurchaseContractHead(purchaseContractHeadId, "采购单完结");

        purchaseComponent.checkStatus(StatusValidity.getPomFinish(), pomPurchaseContractHead, "采购单完结");

        purchaseComponent.updatePurchaseOrderStatus(pomPurchaseContractHead, PomConstants.OrderStatus.FINISH, "采购单完结");
        log.info("采购单完结成功,受影响订单Id:{}", purchaseContractHeadId);
        return ResponseUtil.ok();
    }

    /**
     * 采购单打开(当前线下订单用)
     *
     * @param req
     * @return com.vd.canary.core.bo.ResponseBO<java.lang.Boolean>
     */
    @Override
    public ResponseBO<Boolean> openPurchaseContract(PomPurchaseContractHeadComReq req) {
        String purchaseContractHeadId = req.getPurchaseContractHeadIdList().get(0);
        PomPurchaseContractHead pomPurchaseContractHead = getPomPurchaseContractHead(purchaseContractHeadId, "采购单完结");

        purchaseComponent.checkStatus(StatusValidity.getPomOpen(), pomPurchaseContractHead, "采购单打开");

        purchaseComponent.updatePurchaseOrderStatus(pomPurchaseContractHead, PomConstants.OrderStatus.VALID, "采购单打开");
        log.info("采购单打开成功,受影响订单Id:{}", purchaseContractHeadId);
        return ResponseUtil.ok();
    }

    /**
     * 采购单撤回(当前线下订单用)
     *
     * @param req
     * @return com.vd.canary.core.bo.ResponseBO<java.lang.Boolean>
     */
    @Override
    public ResponseBO<Boolean> revokePurchaseContract(PomPurchaseContractHeadComReq req) {
        String purchaseContractHeadId = req.getPurchaseContractHeadIdList().get(0);
        PomPurchaseContractHead pomPurchaseContractHead = getPomPurchaseContractHead(purchaseContractHeadId, "采购单撤回");

        purchaseComponent.checkStatus(StatusValidity.getPomRevoke(), pomPurchaseContractHead, "采购单撤回");

        SomSalesContractHeadEntity somHead = getSomSalesContractHeadEntity(pomPurchaseContractHead.getFromId(), "采购单撤回");
        if (!somHead.getOrderStatus().equals(SomSalesContractHeadConstant.OrderStatus.BE_SUBMITTED.getKey())) {
            //对应销售单状态=待提交才能撤回
            log.error("采购单撤回异常,对应销售单状态不正确:{}", pomPurchaseContractHead.getFromId());
            throw new BusinessException(120_000, "销售订单为待复核状态，请先撤回销售订单");
        }

        OrderAuthUtil.verifyOperator(List.of(pomPurchaseContractHead.getCreatorId(), pomPurchaseContractHead.getStaffId()), "非本人提交的单据无法撤回");

        purchaseComponent.updatePurchaseOrderStatus(pomPurchaseContractHead, PomConstants.OrderStatus.SUBMIT_PENDING, "采购单撤回");
        log.info("采购单撤回成功,受影响订单Id:{}", purchaseContractHeadId);
        return ResponseUtil.ok();
    }

    @Override
    @Transactional
    public ResponseBO<Boolean> changePurchaseContractStatus(PomContractChangeStatusReq req) {
        req.valid();

        List<PomPurchaseContractHead> pomPurchaseContractHeads;
        if (StringUtil.isNotBlank(req.getSalesContractHeadId())) {
            pomPurchaseContractHeads = getPomPurchaseContractHeads(req.getSalesContractHeadId(), "采购单对外修改状态");
        } else {
            pomPurchaseContractHeads = getPomPurchaseContractHeads(req.getPurchaseContractHeadIdList(), "采购单对外修改状态");
        }

        if (!CollectionUtils.isEmpty(pomPurchaseContractHeads)) {
            pomPurchaseContractHeadService.updateBatchById(
                    pomPurchaseContractHeads.stream()
                            .peek(pomHead -> purchaseComponent.setPurchaseOrderStatus(pomHead, req.getStatus(), req.getReason()))
                            .collect(Collectors.toList())
            );
        }

        return ResponseUtil.ok();
    }

    /**
     * 采购单数据落库
     *
     * @param req
     * @return
     */
    @Override
    @Transactional
    public ResponseBO<PomPurchaseContractGenerateResp> creationPurchaseContract(List<PomPurchaseCreateControlReq> req) {
        log.info("采购订单落库...,入参:{}", JSON.toJSONString(req));
        PomPurchaseContractGenerateResp resp = new PomPurchaseContractGenerateResp();
        List<PomPurchaseContractGenVO> pomPurchaseContractGenVOList = new ArrayList<>();

        for (PomPurchaseCreateControlReq command : req) {
            List<PomPurchaseContractLine> pomPurchaseContractLineList = new ArrayList<>();
            PomPurchaseContractGenVO pomPurchaseContractGenVO = new PomPurchaseContractGenVO();

            PomPurchaseContractHead pomPurchaseContractHead = new PomPurchaseContractHead();
            BeanUtils.copyProperties(command, pomPurchaseContractHead);
            pomPurchaseContractHead.setVersionNo(PomConstants.INITIAL_VERSION);

            log.info("采购订单落库-Head表:{}", JSON.toJSONString(pomPurchaseContractHead));
            int effects = pomPurchaseContractHeadModel.savePurchaseContractHead(pomPurchaseContractHead);
            if (effects != 1) {
                log.error("采购订单落库-保存采购主表信息失败:数据库异常");
                throw new BusinessException(120_000, "保存采购主表信息失败");
            }

            pomPurchaseContractGenVO.setPurchaseContractHeadId(pomPurchaseContractHead.getPurchaseContractHeadId());
            pomPurchaseContractGenVO.setPurchaseContractCode(pomPurchaseContractHead.getPurchaseContractCode());
            pomPurchaseContractGenVOList.add(pomPurchaseContractGenVO);

            if (CollectionUtils.isEmpty(command.getLineBOList())) {
                log.error("采购订单落库-商品明细信息为空");
                throw new BusinessException(120_000, "商品明细信息为空");
            }
            //生成采购合同明细信息
            for (int i = 0; i < command.getLineBOList().size(); i++) {
                PomPurchaseContractLineBO linBO = command.getLineBOList().get(i);
                PomPurchaseContractLine pomPurchaseContractLine = new PomPurchaseContractLine();
                BeanUtils.copyProperties(linBO, pomPurchaseContractLine);
                pomPurchaseContractLine.setVersionNo(PomConstants.INITIAL_VERSION);
                pomPurchaseContractLine.setPurchaseContractHeadId(pomPurchaseContractHead.getPurchaseContractHeadId());

                //采购订单行号
                if (StringUtils.isEmpty(pomPurchaseContractLine.getPurchaseContractLineCode())) {
                    pomPurchaseContractLine.setPurchaseContractLineCode(pomPurchaseContractHead.getPurchaseContractCode() + "00" + i);
                }

                log.info("采购订单落库-Line表:{}", JSON.toJSONString(pomPurchaseContractLine));
                effects = pomPurchaseContractLineModel.savePurchaseContractLine(pomPurchaseContractLine);
                if (effects != 1) {
                    log.error("采购订单落库-保存采购明细表信息失败:数据库异常");
                    throw new BusinessException(120_000, "保存采购明细表信息失败");
                }

                pomPurchaseContractLineList.add(pomPurchaseContractLine);
            }

            //添加采购合同明细-付款条款
            log.info("savePurchaseContract command.getPaymentBOList():{}", JSONUtil.toJSONString(command.getPaymentBOList()));
            List<PomPurchaseContractPayment> pomPurchaseContractPaymentList = new ArrayList<>();
            if (CollectionUtil.isNotEmpty(command.getPaymentBOList())) {
                List<PomPurchaseContractDetailPaymentBO> paymentBOList = command.getPaymentBOList();
                BigDecimal sumAmountRate = BigDecimal.ZERO;
                //正确应该以订单实际金额与页面付款金额去比较
                for (PomPurchaseContractDetailPaymentBO paymentBO : paymentBOList) {
                    sumAmountRate = sumAmountRate.add(paymentBO.getAmountRate());
                }
                if (!(sumAmountRate.compareTo(new BigDecimal(100)) == 0)) {
                    log.error("采购订单落库-校验付款条款失败:付款条款合计金额占比必须为100%");
                    throw new BusinessException(120_000, "付款条款合计金额占比必须为100%");
                }

                for (PomPurchaseContractDetailPaymentBO paymentBO : paymentBOList) {
                    PomPurchaseContractPayment payment = new PomPurchaseContractPayment();
                    BeanUtils.copyProperties(paymentBO, payment);

                    log.info("采购订单落库-付款条款表:{}", JSON.toJSONString(payment));
                    int effectsPayment = pomPurchaseContractPaymentModel.savePurchaseContractPayment(payment,
                            pomPurchaseContractHead);
                    if (effectsPayment != 1) {
                        log.error("采购订单落库-采购订单付款条款保存失败:数据库异常");
                        throw new BusinessException(120_000, "采购订单付款条款保存失败");
                    }
                    pomPurchaseContractPaymentList.add(payment);
                }
            }

            //计算并更新价格
            log.info("采购订单落库-计算并更新订单价格...");
            priceCalculation(pomPurchaseContractLineList, pomPurchaseContractPaymentList, pomPurchaseContractHead);
        }

        resp.setPomPurchaseContractGenVOList(pomPurchaseContractGenVOList);
        log.info("采购订单落库-成功,返回值:{}", JSON.toJSONString(resp));
        return ResponseUtil.ok(resp);
    }

    public void priceCalculation(List<PomPurchaseContractLine> pomPurchaseContractLineList, List<PomPurchaseContractPayment> pomPurchaseContractPaymentList, PomPurchaseContractHead pomPurchaseContractHead) {
        //合同总额
        final BigDecimal[] sumContractAmount = {BigDecimal.ZERO};
        //产品总价
        final BigDecimal[] sumTotalAmount = {BigDecimal.ZERO};
        //商品不含税金额
        final BigDecimal[] sumNoTaxAmount = {BigDecimal.ZERO};
        //产品税金
        final BigDecimal[] sumTaxAmount = {BigDecimal.ZERO};
        //费用总价
        final BigDecimal[] sumCostTotalAmount = {BigDecimal.ZERO};
        //费用税金
        final BigDecimal[] sumCostTaxAmount = {BigDecimal.ZERO};
        //销售不含税金额
        BigDecimal somSumNoTaxAmount = BigDecimal.ZERO;

        Boolean isNormal = pomPurchaseContractHead.getFromSource().equals(PomConstants.FromSource.NORMAL.getKey());
        BigDecimal hundred = new BigDecimal(100);
        for (PomPurchaseContractLine lineBO : pomPurchaseContractLineList) {
            /** 通过BigDecimal的divide方法进行除法时当不整除，出现无限循环小数时，就会抛异常的
             * 异   常 ：java.lang.ArithmeticException: Non-terminating decimal expansion; no exact
             * representable decimal result.
             */
            // 1, tax_type_code = 含税价格
            //税率
            BigDecimal taxCode = new BigDecimal(lineBO.getTaxCode());
            if (PomConstants.TAX_CODE_TYPE.TAX_PRICE.equals(lineBO.getTaxCodeType())) {
                // 税额 = （单价 * 数量） / (1 + 税率/100) * (税率 / 100)
                BigDecimal taxAmount = lineBO.getPurchasePrice().multiply(lineBO.getPurchaseQuantity())
                        .multiply(taxCode.divide(hundred))
                        .divide(BigDecimal.ONE.add(taxCode.divide(hundred)), PomConstants
                                .SCALE_AMOUNT, RoundingMode.HALF_UP);
                lineBO.setTaxAmount(taxAmount);
                // 不含税金额 = （单价 * 数量） - 税额
                BigDecimal noTaxAmount = lineBO.getPurchasePrice().multiply(lineBO.getPurchaseQuantity())
                        .subtract(taxAmount);
                lineBO.setNoTaxAmount(noTaxAmount.setScale(PomConstants.SCALE_AMOUNT, RoundingMode.HALF_UP));
                // 含税总金额 = 单价 * 数量
                BigDecimal totalAmount = lineBO.getPurchasePrice().multiply(lineBO.getPurchaseQuantity());
                lineBO.setTotalAmount(totalAmount.setScale(PomConstants.SCALE_AMOUNT, RoundingMode.HALF_UP));

                //库存商品
                sumTotalAmount[0] = sumTotalAmount[0].add(totalAmount);
                sumTaxAmount[0] = sumTaxAmount[0].add(taxAmount);
                sumNoTaxAmount[0] = sumNoTaxAmount[0].add(noTaxAmount);

                // 计算销售不含税金额
                if (isNormal) {
                    BigDecimal somTotalAmount = lineBO.getPurchaseQuantity().multiply(lineBO.getOriginalPrice());
                    BigDecimal somTaxAmount = somTotalAmount.multiply(taxCode.divide(hundred)).divide(BigDecimal.ONE.add(taxCode.divide(hundred)),
                            PomConstants.SCALE_AMOUNT, RoundingMode.HALF_UP);
                    BigDecimal somNoTaxAmount = somTotalAmount.subtract(somTaxAmount);
                    somSumNoTaxAmount = somSumNoTaxAmount.add(somNoTaxAmount);
                }
            }
            // 2, tax_type_code = 不含税价格
            if (PomConstants.TAX_CODE_TYPE.NO_TAX_PRICE.equals(lineBO.getTaxCodeType())) {
                // 不含税金额 = 单价 * 数量
                BigDecimal noTaxAmount = lineBO.getPurchasePrice().multiply(lineBO.getPurchaseQuantity());
                lineBO.setNoTaxAmount(noTaxAmount.setScale(PomConstants.SCALE_AMOUNT, RoundingMode.HALF_UP));
                // 税额 = 不含税金额 * （税率 / 100）
                BigDecimal taxAmount = noTaxAmount.multiply(taxCode.divide(hundred));
                lineBO.setTaxAmount(taxAmount.setScale(PomConstants.SCALE_AMOUNT, RoundingMode.HALF_UP));
                // 含税总金额 = 不含税金额 + 税额
                BigDecimal totalAmount = noTaxAmount.add(taxAmount);
                lineBO.setTotalAmount(totalAmount.setScale(PomConstants.SCALE_AMOUNT, RoundingMode.HALF_UP));

                //库存商品
                sumTotalAmount[0] = sumTotalAmount[0].add(totalAmount);
                sumTaxAmount[0] = sumTaxAmount[0].add(taxAmount);
                sumNoTaxAmount[0] = sumNoTaxAmount[0].add(noTaxAmount);

                if (isNormal) {
                    // 计算销售不含税金额 = 单价 * 数量
                    BigDecimal somNoTaxAmount = lineBO.getOriginalPrice().multiply(lineBO.getPurchaseQuantity());
                    somSumNoTaxAmount = somSumNoTaxAmount.add(somNoTaxAmount);
                }
            }

            //物流费用
            BigDecimal deliveryCost = lineBO.getDeliveryCost();
            BigDecimal purchaseQuantity = lineBO.getPurchaseQuantity();
            if (deliveryCost != null && purchaseQuantity != null) {
                BigDecimal purchaseQuantityTotal = deliveryCost.multiply(purchaseQuantity);
                sumTotalAmount[0] = sumTotalAmount[0].add(purchaseQuantityTotal);
            }
        }
        pomPurchaseContractLineService.updateBatchById(pomPurchaseContractLineList);

        sumContractAmount[0] = sumTotalAmount[0].add(sumCostTotalAmount[0]);
        // 4, 更新合同总金额
        pomPurchaseContractHead.setTaxAmount(sumTaxAmount[0].setScale(PomConstants.SCALE_AMOUNT,
                RoundingMode.HALF_UP));
        pomPurchaseContractHead.setNoTaxAmount(sumNoTaxAmount[0].setScale(PomConstants.SCALE_AMOUNT,
                RoundingMode.HALF_UP));
        pomPurchaseContractHead.setTotalAmount(sumContractAmount[0].setScale(PomConstants.SCALE_AMOUNT,
                RoundingMode.HALF_UP));

        //以销定采途径来的重新计算毛利率
        if (isNormal) {
            // 设置整单毛利率
            BigDecimal grossProfit = somSumNoTaxAmount.subtract(sumNoTaxAmount[0]).multiply(hundred)
                    .divide(somSumNoTaxAmount, PomConstants.SCALE_AMOUNT, RoundingMode.HALF_UP);
            pomPurchaseContractHead.setGrossProfit(grossProfit);
            log.info("以销定采途径采购单，计算整单毛利率：销售不含税金额:{}, 采购不含税金额:{}, 毛利：{}", somSumNoTaxAmount, sumNoTaxAmount, grossProfit);
        }

        pomPurchaseContractHeadMapper.updateById(pomPurchaseContractHead);

        //此处的付款条款都应已校验
        if (!CollectionUtil.isEmpty(pomPurchaseContractPaymentList)) {
            for (PomPurchaseContractPayment purchaseContractPayment : pomPurchaseContractPaymentList) {
                BigDecimal amountRate = purchaseContractPayment.getAmountRate().divide(hundred);
                BigDecimal paymentAmount = (sumContractAmount[0].multiply(amountRate)).setScale(PomConstants.SCALE_AMOUNT, RoundingMode.HALF_UP);
                purchaseContractPayment.setAmount(paymentAmount);
            }
            pomPurchaseContractPaymentService.updateBatchById(pomPurchaseContractPaymentList);
        }
    }


    /**
     * 询报价生成采购单时列表
     *
     * @param supplierDetailReq
     * @return
     */
    @Override
    public ResponseBO<SupplierDetailResp> querySupplierQuotesDetail(SupplierDetailReq supplierDetailReq) {
        //供方询报价head表
        SupplyInquiryHeadEntity supplyInquiryHeadEntity = Optional.ofNullable(supplyInquiryHeadService.getById(supplierDetailReq.getInquiryId())).orElseThrow(() -> new BusinessException(SupplierInquiryResponseStatus.SUPPLIER_INQUIRY_132010));
        //校验询价单状态
        if (!SupplyInquiryConstants.InquiryStatus.QUOTED.getKey().contains(supplyInquiryHeadEntity.getInquiryStatus())) {
            throw new BusinessException(120_000, "当前询价单状态未为部分报价或已报价,无法生成订单");
        }

        //查询商品信息
        List<SupplyInquiryLineEntity> supplyInquiryLineEntities = supplyInquiryLineService.list(new QueryWrapper<SupplyInquiryLineEntity>().eq("inquiry_head_id", supplyInquiryHeadEntity.getInquiryId()));
        if (CollectionUtils.isEmpty(supplyInquiryLineEntities)) {
            throw new BusinessException(SupplierInquiryResponseStatus.SUPPLIER_INQUIRY_132011);
        }

        //供方与供方报价信息(供方A-商品A，供方B-商品A，供方A-商品B)
        List<SupplyInquiryLineAndQuotesVO> supplyInquiryLineAndQuotesVOList = new ArrayList<>();

        //遍历商品
        for (SupplyInquiryLineEntity supplyInquiryLineEntity : supplyInquiryLineEntities) {
            //供方报价信息,一个商品存在多个供方报价
            //查找当前商品对应的报价信息(分别对应不同供应商)
            List<SupplyQuotesLineEntity> supplyQuotesEntities = supplyQuotesService.list(new LambdaQueryWrapper<SupplyQuotesLineEntity>().
                    eq(SupplyQuotesLineEntity::getInquiryHeadId, supplyInquiryHeadEntity.getInquiryId())
                    .eq(SupplyQuotesLineEntity::getInquiryLineId, supplyInquiryLineEntity.getInquiryLineId()));
            //新增询价时未选择供方的情况
            if (CollectionUtils.isEmpty(supplyQuotesEntities)) {
                continue;
            }

            //遍历对同个商品报价的不同供方
            for (SupplyQuotesLineEntity supplyQuotesEntity : supplyQuotesEntities) {
                //判断是否已报价
                SupplyQuotesHeadEntity supplyQuotesHeadEntity = supplyQuotesHeadService.getOne(new QueryWrapper<SupplyQuotesHeadEntity>().lambda()
                        .eq(SupplyQuotesHeadEntity::getInquiryHeadId, supplyInquiryHeadEntity.getInquiryId())
                        .eq(SupplyQuotesHeadEntity::getSupplyId, supplyQuotesEntity.getSupplyId()));
                if (!StatusValidity.getQuoteStatusInEffect().contains(supplyQuotesHeadEntity.getQuotesStatus())) {
                    continue;
                }
                SupplyInquiryLineAndQuotesVO supplyInquiryLineAndQuotesVO = new SupplyInquiryLineAndQuotesVO();
                //注入商品信息
                BeanUtils.copyProperties(supplyInquiryLineEntity, supplyInquiryLineAndQuotesVO);
                //注入供方报价信息
                BeanUtils.copyProperties(supplyQuotesEntity, supplyInquiryLineAndQuotesVO);
                supplyInquiryLineAndQuotesVOList.add(supplyInquiryLineAndQuotesVO);
            }
        }

        SupplierDetailResp supplierDetailResp = new SupplierDetailResp();
        BeanUtils.copyProperties(supplyInquiryHeadEntity, supplierDetailResp);
        supplierDetailResp.setLineAndQuotesVOList(supplyInquiryLineAndQuotesVOList);
        return ResponseUtil.ok(supplierDetailResp);
    }

    @Override
    public ResponseBO<Boolean> updateQuotesDetails(SupplierBatchUpdateReq supplierBatchUpdateReq) {
        for (SupplierBatchUpdateBO supplierBatchUpdateBO : supplierBatchUpdateReq.getSupplierBatchUpdateBOList()) {
            //todo 存在相同商品多个供方报价,不需要对同个商品多次更新,后期做去重处理
            //更新供方报价表
            SupplyQuotesLineEntity supplyQuotesEntity = new SupplyQuotesLineEntity();
            supplyQuotesEntity.setQuoteId(supplierBatchUpdateBO.getQuoteId());
            supplyQuotesEntity.setQuantity(supplierBatchUpdateBO.getQuantity());
            supplyQuotesEntity.setThreeCategoryId(supplierBatchUpdateBO.getThreeCategoryId());
            supplyQuotesEntity.setThreeCategoryCode(supplierBatchUpdateBO.getThreeCategoryCode());
            supplyQuotesEntity.setThreeCategoryName(supplierBatchUpdateBO.getThreeCategoryName());
            supplyQuotesService.updateById(supplyQuotesEntity);

            //更新商品表
            SupplyInquiryLineEntity supplyInquiryLineEntity = new SupplyInquiryLineEntity();
            BeanUtils.copyProperties(supplierBatchUpdateBO, supplyInquiryLineEntity);
            supplyInquiryLineEntity.setQuantity(null);
            supplyInquiryLineService.updateById(supplyInquiryLineEntity);
        }
        return ResponseUtil.ok();
    }

    @Override
    public ResponseBO<SupplierQuotesResp> quotesList(SupplierQuotesListReq supplierQuotesListReq) {
        List<SupplyQuotesLineEntity> supplyQuotesLineEntities = supplyQuotesLineMapper.selectBatchIds(supplierQuotesListReq.getQuotesList());
        if (CollectionUtils.isEmpty(supplyQuotesLineEntities)) {
            throw new BusinessException(120_000, "未找到供应商报价信息");
        }

        SupplyInquiryHeadEntity supplyInquiryHeadEntity = supplyInquiryHeadService.getById(supplyQuotesLineEntities.get(0).getInquiryHeadId());
        SupplierQuotesResp supplierQuotesListResp = new SupplierQuotesResp();
        BeanUtils.copyProperties(supplyInquiryHeadEntity, supplierQuotesListResp);
        List<SupplyGoodsAndSupInfoVO> supplyGoodsAndSupInfoVOList = new ArrayList<>();

        for (SupplyQuotesLineEntity supplyQuotesLineEntity : supplyQuotesLineEntities) {
            SupplyGoodsAndSupInfoVO supplyGoodsAndSupInfoVO = new SupplyGoodsAndSupInfoVO();

            SupplyQuotesLineVO supplyQuotesLineVO = new SupplyQuotesLineVO();
            BeanUtils.copyProperties(supplyQuotesLineEntity, supplyQuotesLineVO);
            supplyGoodsAndSupInfoVO.setSupplyQuotesLineVO(supplyQuotesLineVO);

            //获取商品与供应商信息
            SupplyInquiryLineEntity supplyInquiryLine = supplyInquiryLineService.getById(supplyQuotesLineEntity.getInquiryLineId());
            SupplyInquiryLineVO supplyInquiryLineVO = new SupplyInquiryLineVO();
            BeanUtils.copyProperties(supplyInquiryLine, supplyInquiryLineVO);
            supplyGoodsAndSupInfoVO.setSupplyInquiryLineVO(supplyInquiryLineVO);

            SupplyQuotesHeadEntity supplyQuotesHeadEntity = supplyQuotesHeadService.getOne(new QueryWrapper<SupplyQuotesHeadEntity>().lambda().eq(SupplyQuotesHeadEntity::getInquiryHeadId, supplyInquiryLine.getInquiryHeadId())
                    .eq(SupplyQuotesHeadEntity::getSupplyId, supplyQuotesLineEntity.getSupplyId()));
            SupplyQuotesHeadVO supplyQuotesHeadVO = new SupplyQuotesHeadVO();
            BeanUtils.copyProperties(supplyQuotesHeadEntity, supplyQuotesHeadVO);
            supplyGoodsAndSupInfoVO.setSupplyQuotesHeadVO(supplyQuotesHeadVO);

            supplyGoodsAndSupInfoVOList.add(supplyGoodsAndSupInfoVO);
        }
        supplierQuotesListResp.setSupplyGoodsAndSupInfoVOList(supplyGoodsAndSupInfoVOList);
        return ResponseUtil.ok(supplierQuotesListResp);
    }

    @Override
    public ResponseBO<Boolean> invalidPurchaseContract(PomPurchaseContractHeadOperateReq req) {
        List<String> purConHeadIds = req.getPurchaseContractHeadIdList();
        //允许作废的订单状态集合
        List<String> conStatusList = new ArrayList<>();
        //变更前允许作废的订单状态集合
        List<String> orderStatusList = new ArrayList<>(4);
        /*待接单*/
        conStatusList.add(PomBizConstants.PURCHASE_CONTRACT_STATUS.TAKE_ORDER_PENDING);
        orderStatusList.add(PomBizConstants.PURCHASE_CONTRACT_STATUS.TAKE_ORDER_PENDING);
        /*待提交*/
        conStatusList.add(PomBizConstants.PURCHASE_CONTRACT_STATUS.SUBMIT_PENDING);
        orderStatusList.add(PomBizConstants.PURCHASE_CONTRACT_STATUS.SUBMIT_PENDING);
        /*审批退回*/
        conStatusList.add(PomBizConstants.PURCHASE_CONTRACT_STATUS.APPROVE_BACK);
        orderStatusList.add(PomBizConstants.PURCHASE_CONTRACT_STATUS.APPROVE_BACK);
        /*供方退回*/
        conStatusList.add(PomBizConstants.PURCHASE_CONTRACT_STATUS.SUPPLIER_REJECT);
        orderStatusList.add(PomBizConstants.PURCHASE_CONTRACT_STATUS.SUPPLIER_REJECT);
        /*变更待提交*/
        conStatusList.add(PomBizConstants.PURCHASE_CONTRACT_STATUS.ALTER_SUBMIT_PENDING);
        /*变更审批退回*/
        conStatusList.add(PomBizConstants.PURCHASE_CONTRACT_STATUS.ALTER_APPROVE_REJECT);
        /*变更供方退回*/
        conStatusList.add(PomBizConstants.PURCHASE_CONTRACT_STATUS.ALTER_SUPPLIER_REJECT);
        /*根据合同id集合与状态 集合查询订单*/
        List<PomPurchaseContractHead> pomPurchaseContractHeads = pomPurchaseContractHeadModel
                .queryPurchaseContractHeadList(purConHeadIds, conStatusList);

        int pomHeadCount = (null == pomPurchaseContractHeads ? 0 : pomPurchaseContractHeads.size());
        if (pomHeadCount != (purConHeadIds.size())) {
            throw new BusinessException(120_000, "采购订单作废失败，采购合同状态不待提交或审批退回");
        }
        /*2. 如果是待签约前状态则状态改为作废， 如果是变更流程中作废，将订单回滚到历史销售合同表中上一个版本。
            3. 当前回滚版本关联的line表中的所有字段全部删除*/

        int count = 0;
        for (PomPurchaseContractHead purchaseContractHead : pomPurchaseContractHeads) {
            PomPurchaseContractHead pomPurchaseContractHead = new PomPurchaseContractHead();
            String orderStatus = purchaseContractHead.getOrderStatus();
            String purchaseContractHeadId = purchaseContractHead.getPurchaseContractHeadId();
            pomPurchaseContractHead.setPurchaseContractHeadId(purchaseContractHeadId);
            if (orderStatusList.contains(orderStatus)) {
                pomPurchaseContractHead.setOrderStatus(PomBizConstants.PURCHASE_CONTRACT_STATUS.INVALID);
                pomPurchaseContractHead.setContractStatus(PomBizConstants.PURCHASE_CONTRACT_STATUS.INVALID);
                count += pomPurchaseContractHeadMapper.updateById(pomPurchaseContractHead);
            } else {
                throw new BusinessException(120_000, "作废采购单-未测试分支");
                /*//当前回滚版本
                Integer currentVersion = purchaseContractHead.getVersionNo();
                *//*删除当前回滚版本对应的合同详情信息*//*
                PomPurchaseContractLine pomPurchaseContractLine = new PomPurchaseContractLine();
                pomPurchaseContractLine.setPurchaseContractHeadId(purchaseContractHeadId);
                pomPurchaseContractLine.setVersionNo(currentVersion);
                pomPurchaseContractLineModel.removePurContractByHeadIdAndVersion
                        (pomPurchaseContractLine);
                    *//*if (updPurContLineNum == 0) {
                        throw new PomOperatorBizException("采购订单作废失败，采购合同状态不待提交或审批退回");
                    }*//*
                 *//*回滚历史表最新版本到主表信息中*//*
                String purcContractHeadId = purchaseContractHead.getPurchaseContractHeadId();
                //查询历史表最新版本数据
                PomPurchaseContractHeadHistoryEntity pomPurchaseContractHeadHistory = pomPurchaseContractHeadHistoryMapper.selectOne(new QueryWrapper<PomPurchaseContractHeadHistoryEntity>()
                        .lambda().eq(PomPurchaseContractHeadHistoryEntity::getPurchaseContractHeadId, purcContractHeadId));
                if (TlerpObjectUtil.isEmpty(pomPurchaseContractHeadHistory)) {
                    throw new BusinessException(120_000, "采购订单作废失败，采购合同状态不待提交或审批退回");
                }
                PomPurchaseContractHead pomPurConHead = new PomPurchaseContractHead();
                TlerpBeanUtil.copyProperties(pomPurchaseContractHeadHistory, pomPurConHead);
                pomPurConHead.setVersionNo(pomPurchaseContractHeadHistory.getVersionNo());
                pomPurConHead.setContractStatus(pomPurchaseContractHeadHistory.getOrderStatus());
                pomPurConHead.setOrderStatus(pomPurchaseContractHeadHistory.getOrderStatus());
                int updPurConHeadCount = pomPurchaseContractHeadModel.updatePurchaseContractHead(pomPurConHead);
                if (updPurConHeadCount == 0) {
                    throw new BusinessException(120_000, "采购订单作废失败，采购合同状态不待提交或审批退回");
                }

                count++;*/
            }
        }
        if (count != purConHeadIds.size()) {
            throw new BusinessException(120_000, "作废失败");
        }
        return ResponseUtil.ok();
    }

    @Override
    public ResponseBO<List<PomPurchaseContractHeadVO>> selectBatchIds(PomPurchaseContractHeadOperateReq req) {
        List<PomPurchaseContractHead> pomPurchaseContractHeadList =
                pomPurchaseContractHeadMapper.selectBatchIds(req.getPurchaseContractHeadIdList());
        if (CollectionUtil.isEmpty(pomPurchaseContractHeadList)) {
            throw new BusinessException(120_000, JSONUtil.toJSONString(req.getPurchaseContractHeadIdList()));
        }
        List<PomPurchaseContractHeadVO> voList = new ArrayList<>();
        pomPurchaseContractHeadList.forEach(pomPurchaseContractHead -> {
            PomPurchaseContractHeadVO headVo = new PomPurchaseContractHeadVO();
            BeanUtils.copyProperties(pomPurchaseContractHead, headVo);
            voList.add(headVo);
        });
        return ResponseUtil.ok(voList);
    }

    @Override
    public ResponseBO<OrderStatisticsResp> orderStatistics() {
        OrderStatisticsResp orderStatisticsResp = new OrderStatisticsResp();
        QueryWrapper somTotalCustomNumQueryWrapper = new QueryWrapper<SomSalesContractHeadEntity>();
        somTotalCustomNumQueryWrapper.select("distinct CUSTOMER_ID");
        Integer somTotalCustomNum = somSalesContractHeadMapper.selectCount(somTotalCustomNumQueryWrapper);

        QueryWrapper somTotalCustomNumEffectWrapper = new QueryWrapper<SomSalesContractHeadEntity>();
        somTotalCustomNumEffectWrapper.select("distinct CUSTOMER_ID");
        //当前销售单没有完结状态
        somTotalCustomNumEffectWrapper.in("ORDER_STATUS", SomSalesContractHeadConstant.OrderStatus.INVALID.getKey());
        Integer somTotalCustomNumEffect = somSalesContractHeadMapper.selectCount(somTotalCustomNumEffectWrapper);

        QueryWrapper pomTotalCustomNumWrapper = new QueryWrapper<PomPurchaseContractHead>();
        pomTotalCustomNumWrapper.select("distinct SUPPLIER_ID");
        Integer pomTotalCustomNum = pomPurchaseContractHeadMapper.selectCount(pomTotalCustomNumWrapper);

        QueryWrapper pomTotalCustomNumEffectWrapper = new QueryWrapper<PomPurchaseContractHead>();
        pomTotalCustomNumEffectWrapper.select("distinct SUPPLIER_ID");
        pomTotalCustomNumEffectWrapper.in("ORDER_STATUS", PomConstants.OrderStatus.VALID.getKey());
        Integer pomTotalCustomNumEffect = pomPurchaseContractHeadMapper.selectCount(pomTotalCustomNumEffectWrapper);

        orderStatisticsResp.setSomTotalCustomNum(somTotalCustomNum);
        orderStatisticsResp.setSomTotalCustomNumEffect(somTotalCustomNumEffect);
        orderStatisticsResp.setPomTotalCustomNum(pomTotalCustomNum);
        orderStatisticsResp.setPomTotalCustomNumEffect(pomTotalCustomNumEffect);
        return ResponseUtil.ok(orderStatisticsResp);
    }

    @Override
    @Transactional
    public ResponseBO<Boolean> changePurchaseContract(PomPurchaseContractHeadOperateReq req) {
        for (String purchaseContractHeadId : req.getPurchaseContractHeadIdList()) {
            PomPurchaseContractHead purchaseContractHead = pomPurchaseContractHeadModel.queryPurchaseContract
                    (purchaseContractHeadId);
            if (TlerpObjectUtil.isEmpty(purchaseContractHead)) {
                log.error("采购订单为空 salesContractHead：{}", JSONUtil.toJSONString(purchaseContractHead));
                throw new BusinessException(120_000, "采购订单不存在！");
            }
            if (!PomBizConstants.PURCHASE_CONTRACT_STATUS.VALID.equals(purchaseContractHead.getOrderStatus())) {
                log.error("采购订单状态不符，不可变更 orderStatus：{}", purchaseContractHead.getOrderStatus());
                throw new BusinessException(120_000, "采购订单状态不符，只有生效状态下才可变更");
            }

            PomPurchaseContractHead contractHead = new PomPurchaseContractHead();
            contractHead.setPurchaseContractHeadId(purchaseContractHeadId);
            contractHead.setOrderStatus(PomBizConstants.PURCHASE_CONTRACT_STATUS.ALTER_SUBMIT_PENDING);
            if (purchaseContractHead.getVersionNo() == 1) {
                contractHead.setOldAmount(purchaseContractHead.getTotalAmount());
            }

            //版本升级
            Integer currentVersion = purchaseContractHead.getVersionNo();
            int nextVersionNo = currentVersion + 1;
            contractHead.setVersionNo(nextVersionNo);
            //contractHead.setSupplyConfirm(0);
            //当前head维护history,line仅做版本升级
            //todo 当前没有变更,需要调用统一创建版本方法
            int effects = pomPurchaseContractHeadMapper.updateById(contractHead);
            PomPurchaseContractHeadHistoryEntity contractHeadHistory = new PomPurchaseContractHeadHistoryEntity();
            BeanUtils.copyProperties(purchaseContractHead, contractHeadHistory);
            contractHeadHistory.setVersionNo(nextVersionNo);
            pomPurchaseContractHeadHistoryMapper.insert(contractHeadHistory);
            pomPurchaseContractLineService.updateBatchById(pomPurchaseContractLineMapper.selectList(new QueryWrapper<PomPurchaseContractLine>().lambda()
                    .eq(PomPurchaseContractLine::getPurchaseContractHeadId, purchaseContractHeadId)).stream()
                    .peek(pomPurchaseContractLine -> {
                        //pomPurchaseContractLine.setVersionNo(nextVersionNo);
                    }).collect(Collectors.toList()));

            if (effects <= 0) {
                log.error("变更采购订单失败！");
                throw new BusinessException(120_000, "变更采购订单失败！");
            }
        }
        return ResponseUtil.ok();
    }

    @Override
    @Deprecated
    public ResponseBO<Boolean> pomSAASRejectOrder(PomPurchaseContractHeadOperateReq req) {
        for (String purchaseContractHeadId : req.getPurchaseContractHeadIdList()) {
            PomPurchaseContractHead pomPurchaseContractHead = pomPurchaseContractHeadMapper.selectById(purchaseContractHeadId);
            if (!StatusValidity.getPomSAASAcceptOrReject().contains(pomPurchaseContractHead.getOrderStatus())) {
                throw new BusinessException(120_000, "采购订单状态不正确，拒绝订单失败");
            }

            if (pomPurchaseContractHead.getOrderStatus().equals(PomBizConstants.PURCHASE_CONTRACT_STATUS.NEED_SUPPLIER_SIGN)) {
                pomPurchaseContractHead.setOrderStatus(PomBizConstants.PURCHASE_CONTRACT_STATUS.INVALID);
                pomPurchaseContractHead.setContractStatus(PomBizConstants.PURCHASE_CONTRACT_STATUS.INVALID);
            } else if (pomPurchaseContractHead.getOrderStatus().equals(PomBizConstants.PURCHASE_CONTRACT_STATUS.ALTER_NEED_SUPPLIER_SIGN)) {
                pomPurchaseContractHead.setOrderStatus(PomBizConstants.PURCHASE_CONTRACT_STATUS.ALTER_SUPPLIER_REJECT);
                pomPurchaseContractHead.setContractStatus(PomBizConstants.PURCHASE_CONTRACT_STATUS.ALTER_SUPPLIER_REJECT);
            } else {
                pomPurchaseContractHead.setContractStatus(PomBizConstants.PURCHASE_CONTRACT_STATUS.SHUT);
                pomPurchaseContractHead.setOrderStatus(PomBizConstants.PURCHASE_CONTRACT_STATUS.SHUT);
            }
            pomPurchaseContractHeadMapper.updateById(pomPurchaseContractHead);
            //todo 操作日志中记录关闭原因
        }

        return ResponseUtil.ok();
    }

    @Override
    public ResponseBO<Boolean> pomSAASAcceptOrder(PomPurchaseSaaSAcceptOrder req) {
        for (String purchaseContractHeadId : req.getPurchaseContractHeadIdList()) {
            PomPurchaseContractHead pomPurchaseContractHead = pomPurchaseContractHeadMapper.selectById(purchaseContractHeadId);
            if (ObjectUtil.isEmpty(pomPurchaseContractHead)) {
                log.error("供方SAAS接单-未找到当前采购订单,采购单ID:{}", purchaseContractHeadId);
                throw new BusinessException(120_000, "未找到当前采购订单");
            }

            if (!StatusValidity.getPomSAASAcceptOrReject().contains(pomPurchaseContractHead.getOrderStatus())) {
                log.error("供方SAAS接单-采购订单状态不正确,采购单ID:{}", purchaseContractHeadId);
                throw new BusinessException(120_000, "采购订单状态不正确，接受订单失败");
            }

            if (req.getType().equals(1)) {
                //todo 应该校验订单来源
                //接单
                log.info("供方SAAS接单-接单,采购单ID:{}", purchaseContractHeadId);
                pomPurchaseContractHead.setOrderStatus(PomConstants.OrderStatus.VALID.getKey());
                pomPurchaseContractHead.setSupplyConfirm(1);
                List<PomPurchaseContractLine> collect = pomPurchaseContractLineMapper.selectList(new LambdaQueryWrapper<PomPurchaseContractLine>().eq(PomPurchaseContractLine::getPurchaseContractHeadId, pomPurchaseContractHead.getPurchaseContractHeadId()))
                        .stream().peek(pomPurchaseContractLine -> {
                            pomPurchaseContractLine.setCanSupply(PomConstants.OrderLineStatus.CAN_SUPPLY.getKey());
                        }).collect(Collectors.toList());
                pomPurchaseContractLineService.updateBatchById(collect);
            } else if (req.getType().equals(0)) {
                //拒绝
                log.info("供方SAAS接单-拒绝,采购单ID:{}", purchaseContractHeadId);
                pomPurchaseContractHead.setOrderStatus(PomConstants.OrderStatus.CLOSE.getKey());
                if (StringUtil.isBlank(req.getReason())) {
                    log.error("供方SAAS拒绝接单-拒绝原因为空,采购单ID:{}", purchaseContractHeadId);
                    throw new BusinessException(120_000, "采购订单状态不正确，拒绝原因为空");
                }
                pomPurchaseContractHead.setSupplyRefuseReason(req.getReason());
            } else {
                log.error("供方SAAS接单-参数异常,type:{}", req.getType());
                throw new BusinessException(120_000, "Type参数异常");
            }

            pomPurchaseContractHeadMapper.updateById(pomPurchaseContractHead);
        }

        return ResponseUtil.ok();
    }

    /**
     * 甲方签约(线上)
     *
     * @param purchaseContractHeadId
     * @param pomPurchaseContractHead
     * @param orderStatus
     */
    private void createPdfAndSign(String purchaseContractHeadId, PomPurchaseContractHead pomPurchaseContractHead, String
            orderStatus) {
        //查询填充合同PDF模板的数据,并填充模板生成PDF
        //->
        PomPurchaseContractPdfView pomPurchaseContractPdfView = pomPurchaseContractHeadModel
                .queryPurchaseContractPdfData(purchaseContractHeadId);
        PurchaseContractPdfTemplate template = new PurchaseContractPdfTemplate();
        byte[] bytes = template.createContractPdfDocument(pomPurchaseContractPdfView);
        //上传到FastDFS
        String originFileName = StringUtils.defaultIfEmpty(pomPurchaseContractPdfView.getProjectName(), StringUtils.EMPTY) + "原件合同" + DateUtil.nowDate("yyyyMMddHHmmss").concat(".pdf");
        UploadFileReq uploadFileReq = new UploadFileReq();
        uploadFileReq.setContType("application/pdf");
        uploadFileReq.setInfo(bytes);
        uploadFileReq.setFileName(originFileName);
        ResponseBO<FileInfoResp> fileInfoRespResponseBO = fileCommonServiceFeign.uploadPublicByte(uploadFileReq);
        pomPurchaseContractPdfView.setData(bytes);

        //更新订单origin_sign_attachment_id字段
        PomPurchaseContractHead updateContractHead = new PomPurchaseContractHead();
        updateContractHead.setPurchaseContractHeadId(purchaseContractHeadId);
        updateContractHead.setOriginSignAttachmentId(fileInfoRespResponseBO.getData().getId());
        updateContractHead.setOriginSignAttachmentUrl(fileInfoRespResponseBO.getData().getUrl());
//        updateContractHead.setSignType(PomBizConstants.PURCHASE_SIGN_TYPE.ONLINE);
//        updateContractHead.setSignDate(LocalDateTime.now());
        //SomSalesContractHeadServiceImpl.callSign
        updateContractHead.setVersionNo(pomPurchaseContractHead.getVersionNo());
        log.info("电签前保存采购订单updateContractHead信息：{}", JSONUtil.toJSONString(updateContractHead));
        pomPurchaseContractHeadMapper.updateById(updateContractHead);
        callSign(pomPurchaseContractPdfView, orderStatus, purchaseContractHeadId, pomPurchaseContractHead);
    }

    /**
     * 调用电子签章SDK
     *
     * @param pomPurchaseContractPdfView
     */
    private void callSign(PomPurchaseContractPdfView pomPurchaseContractPdfView, String orderStatus, String purchaseContractHeadId, PomPurchaseContractHead pomPurchaseContractHead) {
        log.info("我方签约，开始调用电子签章接口........{}", pomPurchaseContractPdfView.getPurchaseContractCode());
        String mySignFileName = StringUtils.defaultIfEmpty(pomPurchaseContractPdfView.getProjectName(), StringUtils.EMPTY) + "我方已签合同" + DateUtil
                .nowDate("yyyyMMddHHmmss").concat(".pdf");
        // TODO 电子签章调用接口
        SelfSignReq selfSignReq = new SelfSignReq();
        selfSignReq.setFileName(mySignFileName);
        selfSignReq.setFileType("PDF");
        selfSignReq.setKey("乙方(签字/电子签章)");
        selfSignReq.setInfo(pomPurchaseContractPdfView.getData());
        ResponseBO<SignDataVO> signDataVOResponseBO = contractServiceFeign.electronicSelfSign(selfSignReq);

        if (!signDataVOResponseBO.getSuccess()) {
            log.error("我方签署失败：{}", signDataVOResponseBO.getMessage());
            throw new BusinessException(SomSalesContractResponseStatus.INTERFACT_ORDER_ERR35);
        }

        if (ObjectUtil.isNotEmpty(signDataVOResponseBO)) {
            byte[] mySignStreamBytes = signDataVOResponseBO.getData().getInfo();
            byte[] bytesWithSignDate = PurchaseContractPdfTemplate.appendSignDate(mySignStreamBytes);
            //上传到FastDFS
            // 上传我方签约PDF到OSS
            UploadFileReq uploadFileReq = new UploadFileReq();
            uploadFileReq.setContType("application/pdf");
            uploadFileReq.setInfo(bytesWithSignDate);
            uploadFileReq.setFileName(mySignFileName);
            ResponseBO<FileInfoResp> fileInfoRespResponseBO = fileCommonServiceFeign.uploadPublicByte(uploadFileReq);

            //更新订单my_sign_attachment_id字段
            if (PomBizConstants.PURCHASE_CONTRACT_STATUS.SUBMIT_PENDING.equals(orderStatus) ||
                    PomBizConstants.PURCHASE_CONTRACT_STATUS.SUPPLIER_REJECT.equals(orderStatus) ||
                    PomBizConstants.PURCHASE_CONTRACT_STATUS.APPROVING.equals(orderStatus)) {
                //20-审批中状态下 修改采购订单状态
                PomPurchaseContractHead mySignContractHead = new PomPurchaseContractHead();
                mySignContractHead.setPurchaseContractHeadId(purchaseContractHeadId);
                mySignContractHead.setMySignAttachmentId(fileInfoRespResponseBO.getData().getId());
                mySignContractHead.setMySignAttachmentUrl(fileInfoRespResponseBO.getData().getUrl());

                mySignContractHead.setVersionNo(pomPurchaseContractHead.getVersionNo());
                mySignContractHead.setContractStatus(PomBizConstants.PURCHASE_CONTRACT_STATUS.NEED_SUPPLIER_SIGN);
                mySignContractHead.setOrderStatus(PomBizConstants.PURCHASE_CONTRACT_STATUS.NEED_SUPPLIER_SIGN);
                mySignContractHead.setMySignAttachmentName(mySignFileName);
                log.info("电签后mySignContractHead:{}", JSONUtil.toJSONString(mySignContractHead));
                pomPurchaseContractHeadMapper.updateById(mySignContractHead);
                //20-审批中状态下 修改采购历史订单表
                PomPurchaseContractHeadHistoryEntity mySignContractHeadHistory = new PomPurchaseContractHeadHistoryEntity();
                mySignContractHeadHistory.setPurchaseContractHeadId(purchaseContractHeadId);
                mySignContractHeadHistory.setMySignAttachmentId(fileInfoRespResponseBO.getData().getId());
                mySignContractHeadHistory.setVersionNo(pomPurchaseContractHead.getVersionNo());
                mySignContractHeadHistory.setContractStatus(PomBizConstants.PURCHASE_CONTRACT_STATUS.NEED_SUPPLIER_SIGN);
                mySignContractHeadHistory.setOrderStatus(PomBizConstants.PURCHASE_CONTRACT_STATUS.NEED_SUPPLIER_SIGN);
                log.info("电签后mySignContractHeadHistory:{}", JSONUtil.toJSONString(mySignContractHeadHistory));
                //pomPurchaseContractHeadHistoryMapper.updateStatus(mySignContractHeadHistory);
                pomPurchaseContractHeadHistoryMapper.updateById(mySignContractHeadHistory);
            }
            if (PomBizConstants.PURCHASE_CONTRACT_STATUS.ALTER_SUBMIT_PENDING.equals(orderStatus)
                    || PomBizConstants.PURCHASE_CONTRACT_STATUS.ALTER_SUPPLIER_REJECT.equals(orderStatus)
                    || PomBizConstants.PURCHASE_CONTRACT_STATUS.ALTER_APPROVING.equals(orderStatus)) {
                //72-变更待提交状态下 修改采购订单状态
                PomPurchaseContractHead mySignAlterContractHead = new PomPurchaseContractHead();
                mySignAlterContractHead.setPurchaseContractHeadId(purchaseContractHeadId);
                mySignAlterContractHead.setMySignAttachmentId(fileInfoRespResponseBO.getData().getId());
                mySignAlterContractHead.setOrderStatus(PomBizConstants.PURCHASE_CONTRACT_STATUS.ALTER_NEED_SUPPLIER_SIGN);
                //重置供方接单标志
                mySignAlterContractHead.setSupplyConfirm(0);
                log.info("电签后保存变更mySignAlterContractHead:{}", JSONUtil.toJSONString(mySignAlterContractHead));
                pomPurchaseContractHeadMapper.updateById(mySignAlterContractHead);
            }
        }
    }

    /**
     * 供方电子签章在线签署(线上)
     *
     * @param req
     * @return
     */
    @Override
    @Transactional
    public ResponseBO<PomPurchaseContractHeadVO> supplierSignOrderOnline(SupplierSignOrderOnlineReq req) {
        // 查询订单详情
        PomPurchaseContractHead pomPurchaseContractHead = pomPurchaseContractHeadMapper.selectById(req.getPurchaseContractHeadId());
        if (cn.hutool.core.bean.BeanUtil.isEmpty(pomPurchaseContractHead) ||
                StringUtils.isEmpty(pomPurchaseContractHead.getMySignAttachmentId())) {
            throw new BusinessException(120_000, "待签约文件不存在");
        }
        String allSignFileName = StringUtils.defaultIfEmpty(pomPurchaseContractHead.getProjectName(), StringUtils.EMPTY) + "双方已签合同" + DateUtil
                .nowDate("yyyyMMddHHmmss").concat(".pdf");
        File file = new File("temp.pdf");
        HttpUtil.downloadFile(pomPurchaseContractHead.getMySignAttachmentUrl(), file);
        byte[] bytes = FileUtil.readBytes(file);
        SingFileBO singFileBO = new SingFileBO();
        if (org.apache.commons.lang3.StringUtils.isBlank(pomPurchaseContractHead.getSupplierId())) {
            throw new BusinessException(120_000, "采购单供方Id为空或不存在");
        }
        singFileBO.setCustomerId(pomPurchaseContractHead.getSupplierId());
        singFileBO.setCode(req.getCode());
        singFileBO.setFileName(allSignFileName);
        singFileBO.setFileType("PDF");
        singFileBO.setKey("甲方(签字/电子签章)");
        singFileBO.setMobile(req.getMobile());
        singFileBO.setInfo(bytes);
        ResponseBO<SignDataVO> signDataVOResponseBO = contractServiceFeign.electronicUse(singFileBO);
        if (signDataVOResponseBO.isFailed()) {
            throw new BusinessException(120_000, signDataVOResponseBO.getMessage());
        }

        byte[] finalPdf = signDataVOResponseBO.getData().getInfo();
        // 上传最终签约PDF到OSS
        UploadFileReq uploadFileReqFinal = new UploadFileReq();
        uploadFileReqFinal.setContType("application/pdf");
        uploadFileReqFinal.setInfo(finalPdf);
        uploadFileReqFinal.setFileName(allSignFileName);
        ResponseBO<FileInfoResp> fileInfoRespResponseBO = fileCommonServiceFeign.uploadPublicByte(uploadFileReqFinal);


        if (Integer.parseInt(pomPurchaseContractHead.getOrderStatus()) > 60) {
            pomPurchaseContractHead.setEffectStatus(PomBizConstants.PURCHASE_CONTRACT_STATUS.VALID_CHANGE);
            pomPurchaseContractHead.setEffectDate(LocalDateTime.now());
        }
        // 变更合同状态
        pomPurchaseContractHead.setSignDate(LocalDateTime.now());
        pomPurchaseContractHead.setSignType(PomBizConstants.PURCHASE_SIGN_TYPE.ONLINE);
        pomPurchaseContractHead.setSignPerson("电子签章");
        pomPurchaseContractHead.setCustomerSignPerson(req.getCustomerSignPerson());
        pomPurchaseContractHead.setContractStatus(PomBizConstants.PURCHASE_CONTRACT_STATUS.VALID);
        pomPurchaseContractHead.setOrderStatus(PomBizConstants.PURCHASE_CONTRACT_STATUS.VALID);
        pomPurchaseContractHead.setFinishSignAttachmentId(fileInfoRespResponseBO.getData().getId());
        pomPurchaseContractHead.setFinishSignAttachmentUrl(fileInfoRespResponseBO.getData().getUrl());
        pomPurchaseContractHead.setFinishSignAttachmentName(allSignFileName);
        doPomHistoryTask(pomPurchaseContractHead);
        pomPurchaseContractHeadMapper.updateById(pomPurchaseContractHead);
        PomPurchaseContractHeadVO vo = new PomPurchaseContractHeadVO();
        vo.setPurchaseContractHeadId(pomPurchaseContractHead.getPurchaseContractHeadId());
        vo.setFinishSignAttachmentName(pomPurchaseContractHead.getFinishSignAttachmentName());
        //vo.setFinishSignAttachmentUrl(pomPurchaseContractHead.getFinishSignAttachmentUrl());
        return ResponseUtil.ok(vo);
    }

    /**
     * 处理后续采购订单历史信息
     *
     * @param pomPurchaseContractHead
     */
    private void doPomHistoryTask(PomPurchaseContractHead pomPurchaseContractHead) {
        PomPurchaseContractHeadHistoryEntity pomPurchaseContractHeadHistory = new PomPurchaseContractHeadHistoryEntity();
        TlerpBeanUtil.copyProperties(pomPurchaseContractHead, pomPurchaseContractHeadHistory);
        pomPurchaseContractHeadHistory.setVersionNo(pomPurchaseContractHead.getVersionNo());
        pomPurchaseContractHeadHistoryModel.updatePurchaseContractHistoryStatus(pomPurchaseContractHeadHistory);
//        if (new Integer(1).equals(pomPurchaseContractHead.getVersionNo())) {
//            //修改历史采购订单状态 60-生效
//            pomPurchaseContractHeadHistory.setVersionNo(pomPurchaseContractHead.getVersionNo());
//            pomPurchaseContractHeadHistoryModel.updatePurchaseContractHistoryStatus(pomPurchaseContractHeadHistory);
//        } else {
//            pomPurchaseContractHeadHistory.setVersionNo(pomPurchaseContractHead.getVersionNo());
//            pomPurchaseContractHeadHistoryModel.savePomPurchaseContractHeadHistory(pomPurchaseContractHeadHistory);
//        }
    }

    /**
     * 查询此版本下的采购订单行是否为空，不为空则更新，为空则不做操作
     */
    private void updatePurchaseContractLine(PomPurchaseContractSignReq req, PomPurchaseContractHead pomPurchaseContractHead) {
        //查询此版本下的采购订单行是否为空，不为空则更新，为空则不做操作
        String purchaseContractHeadId = pomPurchaseContractHead.getPurchaseContractHeadId();
        PomPurchaseContractLine param = new PomPurchaseContractLine();
        param.setPurchaseContractHeadId(purchaseContractHeadId);
        param.setVersionNo(pomPurchaseContractHead.getVersionNo());
        //List<PomPurchaseContractLine> purchaseLineList = pomPurchaseContractLineModel.queryPurchaseContractLineList(param);
        /*List<PomPurchaseContractLine> pomPurchaseContractLineList = pomPurchaseContractLineMapper.selectList(new QueryWrapper<PomPurchaseContractLine>().lambda()
                .eq(PomPurchaseContractLine::getPurchaseContractHeadId, purchaseContractHeadId)
                .eq(PomPurchaseContractLine::getVersionNo, pomPurchaseContractHead.getVersionNo()));
        if (TlerpCollectionUtil.isNotEmpty(pomPurchaseContractLineList)) {
            //修改当前版本销售订单的PomPurchaseContractLine的MODIFY_STATUS为N
            PomPurchaseContractLine pomPurchaseContractLine = new PomPurchaseContractLine();
            pomPurchaseContractLine.setPurchaseContractHeadId(purchaseContractHeadId);
            pomPurchaseContractLine.setVersionNo(pomPurchaseContractHead.getVersionNo());
            pomPurchaseContractLineModel.updateSalesContractLineModifyStatus(pomPurchaseContractLine);
        }*/
        //如果当前订单版本为1.0，则对历史订单记录做更新操作，否则做插入操作
        //todo ztest 统一版本
        if (new Integer(1).equals(pomPurchaseContractHead.getVersionNo())) {
            //修改历史采购订单状态 60-生效
            PomPurchaseContractHeadHistoryEntity pomPurchaseContractHeadHistory = new
                    PomPurchaseContractHeadHistoryEntity();
            pomPurchaseContractHeadHistory.setPurchaseContractHeadId(purchaseContractHeadId);
            pomPurchaseContractHeadHistory.setVersionNo(pomPurchaseContractHead.getVersionNo());
            pomPurchaseContractHeadHistory.setContractStatus(PomBizConstants.PURCHASE_CONTRACT_STATUS.VALID);
            pomPurchaseContractHeadHistory.setOrderStatus(PomBizConstants.PURCHASE_CONTRACT_STATUS.VALID);
            pomPurchaseContractHeadHistoryModel.updatePurchaseContractHistoryStatus(pomPurchaseContractHeadHistory);
        } else {
            PomPurchaseContractHeadHistoryEntity insertContractHeadHistory = new PomPurchaseContractHeadHistoryEntity();
            TlerpBeanUtil.copyProperties(pomPurchaseContractHead, insertContractHeadHistory);
            insertContractHeadHistory.setPurchaseContractHeadId(purchaseContractHeadId);
            insertContractHeadHistory.setVersionNo(pomPurchaseContractHead.getVersionNo());
            pomPurchaseContractHeadHistoryModel.savePomPurchaseContractHeadHistory(insertContractHeadHistory);
        }
    }

    /**
     * 采购订单审批流流程变量组装
     *
     * @param businessKeyList 订单idList
     * @return
     */
    private Map<String, Map<String, Object>> findWorkFlowVariablesMap(List<String> businessKeyList) {
        Map<String, Map<String, Object>> variablesMap = new HashMap<>(businessKeyList.size());
        //查询采购订单列表，获取流程所需变量
        List<PomPurchaseContractAuditTO> purchaseContractAuditTOList = pomPurchaseContractHeadMapper
                .queryWorkFlowInfo(businessKeyList);
        Map<String, PomPurchaseContractAuditTO> purchaseContractAuditTOMap = new HashMap<>(purchaseContractAuditTOList.size());
        for (PomPurchaseContractAuditTO purchaseContractAuditTO : purchaseContractAuditTOList) {
            String purchaseContractHeadId = purchaseContractAuditTO.getPurchaseContractHeadId();
            purchaseContractAuditTOMap.put(purchaseContractHeadId, purchaseContractAuditTO);
        }
        //查询采购订单下商品清单列表
        for (Map.Entry<String, PomPurchaseContractAuditTO> auditTOEntry : purchaseContractAuditTOMap.entrySet()) {
            String purchaseContractHeadId = auditTOEntry.getKey();
            PomPurchaseContractAuditTO purchaseContractAuditTO = auditTOEntry.getValue();
            PomPurchaseContractLine entity = new PomPurchaseContractLine();
            entity.setPurchaseContractHeadId(purchaseContractHeadId);
            //查询商品清单列表
            List<PomPurchaseContractLine> lineEntityList = pomPurchaseContractLineMapper.query(entity);
            //计算此订单下的  优惠金额总和
            BigDecimal totalDiscountAmount = lineEntityList.stream()
                    .map(PomPurchaseContractLine::getDiscountAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            //放入流程变量对象中
            purchaseContractAuditTO.setDiscountAmount(totalDiscountAmount.setScale(2, RoundingMode.HALF_UP));
            //准备流程变量
            Map<String, Object> variables = new HashMap<>(2);
            Map<String, Object> toMap = BeanUtil.toMap(purchaseContractAuditTO);
            variables.putAll(toMap);
            variables.put("debugTexzt", JSONUtil.toJSONString(purchaseContractAuditTO));

            log.info("findWorkFlowVariablesMap {} variables {}", purchaseContractHeadId, JSONUtil.toJSONString(variables));
            variablesMap.put(purchaseContractHeadId, variables);
        }

        return variablesMap;
    }

    /**
     * 采购单审批完成后的业务操作
     *
     * @param pomPurchaseContractHeadList
     */
    private void doCompleteBusiness(List<PomPurchaseContractHead> pomPurchaseContractHeadList) {
        for (PomPurchaseContractHead pomPurchaseContractHead : pomPurchaseContractHeadList) {
            doCompleteBusiness(pomPurchaseContractHead);
        }
    }

    /**
     * 采购单审批完成后的业务操作
     *
     * @param pomPurchaseContractHead
     */
    private void doCompleteBusiness(PomPurchaseContractHead pomPurchaseContractHead) {
        String purchaseContractHeadId = pomPurchaseContractHead.getPurchaseContractHeadId();

        String orderStatus = pomPurchaseContractHead.getOrderStatus();
        //若采购订单合同为20-审批中，则修改状态为 50-审批通过，并修改历史订单表
        //未配置审批流时，10-待提交，11-审批退回状态的单据，也可完成审批
        List<String> statusList = Arrays.asList(
                PomBizConstants.PURCHASE_CONTRACT_STATUS.APPROVING,
                PomBizConstants.PURCHASE_CONTRACT_STATUS.SUBMIT_PENDING,
                PomBizConstants.PURCHASE_CONTRACT_STATUS.APPROVE_REJECT
        );
        if (statusList.contains(orderStatus)) {
            PomPurchaseContractHead purchaseContractHead = new PomPurchaseContractHead();
            purchaseContractHead.setPurchaseContractHeadId(purchaseContractHeadId);
            // 合同状态：50-审批通过/待供方确认
            purchaseContractHead.setContractStatus(PomBizConstants.PURCHASE_CONTRACT_STATUS.APPROVED);
            purchaseContractHead.setOrderStatus(PomBizConstants.PURCHASE_CONTRACT_STATUS.APPROVED);
            purchaseContractHead.setVersionNo(pomPurchaseContractHead.getVersionNo());
            log.info("PomPurchaseContractCompleteWfBiz doBusiness:{} ", JSONUtil.toJSONString
                    (pomPurchaseContractHead));
            int effect = pomPurchaseContractHeadModel.savePurchaseContractHead(purchaseContractHead);

            if (effect <= 0) {
                throw new BusinessException(120_000, "采购订单审批，状态更新失败");
            }
        }
        //未配置审批流时，70-变更待提交，74-变更审批退回状态的单据，也可完成审批
        List<String> alterStatusList = Arrays.asList(
                PomBizConstants.PURCHASE_CONTRACT_STATUS.ALTER_APPROVING,
                PomBizConstants.PURCHASE_CONTRACT_STATUS.ALTER_SUBMIT_PENDING,
                PomBizConstants.PURCHASE_CONTRACT_STATUS.ALTER_APPROVE_REJECT
        );
        //若采购订单为  72-变更审批中，则修改状态为  76-变更审批通过
        if (alterStatusList.contains(orderStatus)) {
            PomPurchaseContractHead alterContractHead = new PomPurchaseContractHead();
            alterContractHead.setPurchaseContractHeadId(purchaseContractHeadId);
            alterContractHead.setOrderStatus(PomBizConstants.PURCHASE_CONTRACT_STATUS.ALTER_APPROVED);
            int effects = pomPurchaseContractHeadMapper.updateById(alterContractHead);
            if (effects <= 0) {
                throw new BusinessException(120_000, "采购订单审批，状态更新失败");
            }
        }
        //创建PDF及电子签章
        //->电子签章延后调试
        createPdfAndSign(purchaseContractHeadId, pomPurchaseContractHead, orderStatus);
    }

    /**
     * submit 提交业务逻辑
     */
    private void doSubmitBusiness(List<PomPurchaseContractHead> pomPurchaseContractHeadList) {
        for (PomPurchaseContractHead purchaseContractHeadEntity : pomPurchaseContractHeadList) {
            String purchaseContractHeadId = purchaseContractHeadEntity.getPurchaseContractHeadId();

            log.info("PomPurchaseContractStartWfBiz doBusiness purchaseContractHeadId:[{}]", purchaseContractHeadId);
            //来源单据当前状态
            String orderStatus = purchaseContractHeadEntity.getOrderStatus();
            log.info("PomPurchaseContractStartWfBiz doBusiness orderStatus:[{}]", orderStatus);
            //如果采购订单状态为10-待提交，11-审核退回，51-供方退回，将订单状态修改为审核中，历史采购订单也进行修改
            List<String> enableStatusList = Arrays.asList(PomBizConstants.PURCHASE_CONTRACT_STATUS.SUBMIT_PENDING,
                    PomBizConstants.PURCHASE_CONTRACT_STATUS.APPROVE_REJECT,
                    PomBizConstants.PURCHASE_CONTRACT_STATUS.SUPPLIER_REJECT,
                    PomBizConstants.PURCHASE_CONTRACT_STATUS.ALTER_SUBMIT_PENDING,
                    PomBizConstants.PURCHASE_CONTRACT_STATUS.ALTER_APPROVE_REJECT,
                    PomBizConstants.PURCHASE_CONTRACT_STATUS.ALTER_SUPPLIER_REJECT
            );
            if (!enableStatusList.contains(orderStatus)) {
                throw new BusinessException(120_000, "期望提交的订单中订单状态不合法");
            }
            //如果采购订单状态为10-待提交，11-审核退回，51-供方退回，将订单状态修改为审核中，历史采购订单也进行修改
            List<String> statusList = Arrays.asList(PomBizConstants.PURCHASE_CONTRACT_STATUS.SUBMIT_PENDING,
                    PomBizConstants.PURCHASE_CONTRACT_STATUS.APPROVE_REJECT,
                    PomBizConstants.PURCHASE_CONTRACT_STATUS.SUPPLIER_REJECT
            );
            if (statusList.contains(orderStatus)) {
                //修改采购订单
                PomPurchaseContractHead contractHead = new PomPurchaseContractHead();
                contractHead.setPurchaseContractHeadId(purchaseContractHeadId);
                // 合同状态：20-审批中
                contractHead.setContractStatus(PomBizConstants.PURCHASE_CONTRACT_STATUS.APPROVING);
                contractHead.setOrderStatus(PomBizConstants.PURCHASE_CONTRACT_STATUS.APPROVING);
                log.info("PomPurchaseContractStartWfBiz doBusiness contractHead:{} ", JSONUtil.toJSONString
                        (contractHead));

                int effect = pomPurchaseContractHeadModel.savePurchaseContractHead(contractHead);
                //根据采购订单头id和版本号，修改历史采购订单
                PomPurchaseContractHeadHistoryEntity pomPurchaseContractHeadHistory = new PomPurchaseContractHeadHistoryEntity();
                pomPurchaseContractHeadHistory.setPurchaseContractHeadId(purchaseContractHeadId);
                pomPurchaseContractHeadHistory.setVersionNo(purchaseContractHeadEntity.getVersionNo());
                pomPurchaseContractHeadHistory.setContractStatus(PomBizConstants.PURCHASE_CONTRACT_STATUS.APPROVING);
                pomPurchaseContractHeadHistory.setOrderStatus(PomBizConstants.PURCHASE_CONTRACT_STATUS.APPROVING);
                log.info("PomPurchaseContractStartWfBiz doBusiness pomPurchaseContractHeadHistory:{} ", JSONUtil
                        .toJSONString(pomPurchaseContractHeadHistory));
                int effectRows = pomPurchaseContractHeadHistoryMapper.updateStatus(pomPurchaseContractHeadHistory);
                if (effect <= 0 || effectRows <= 0) {
                    log.error("采购订单提交，采购订单 effect:[{}],历史采购订单effectRows:[{}]", effect, effectRows);
                    throw new BusinessException(120_000, "采购订单提交，状态更新失败");
                }
            }


            //如果采购订单状态为70-变更待提交，74-变更审核退回，80-变更供方退回,将订单状态修改为72-变更审核中
            List<String> alterStatusList = Arrays.asList(
                    PomBizConstants.PURCHASE_CONTRACT_STATUS.ALTER_SUBMIT_PENDING,
                    PomBizConstants.PURCHASE_CONTRACT_STATUS.ALTER_APPROVE_REJECT,
                    PomBizConstants.PURCHASE_CONTRACT_STATUS.ALTER_SUPPLIER_REJECT
            );
            if (alterStatusList.contains(orderStatus)) {

                //修改采购订单
                PomPurchaseContractHead alterContractHead = new PomPurchaseContractHead();
                alterContractHead.setPurchaseContractHeadId(purchaseContractHeadId);
                // 合同状态：72-变更审批中
                alterContractHead.setOrderStatus(PomBizConstants.PURCHASE_CONTRACT_STATUS.ALTER_APPROVING);
                log.info("PomPurchaseContractStartWfBiz doBusiness alterContractHead:{} ", JSONUtil.toJSONString
                        (alterContractHead));
                int effects = pomPurchaseContractHeadModel.updatePurchaseContractHeadStatus(alterContractHead);
                if (effects <= 0) {
                    log.error("采购订单提交，采购订单 effect:[{}],历史采购订单effectRows:[{}]", effects);
                    throw new BusinessException(120_000, "采购订单提交，状态更新失败");
                }
            }
        }
    }

    /**
     * 审批流退回后业务
     *
     * @param pomPurchaseContractHead
     */
    private void doRejectBusiness(PomPurchaseContractHead pomPurchaseContractHead,
                                  WorkflowCompleteReq req) {

        String purchaseContractHeadId = pomPurchaseContractHead.getPurchaseContractHeadId();
        log.info("POM doRejectBusiness purchaseContractHeadId:[{}]", purchaseContractHeadId);

        String orderStatus = null;
        if (ObjectUtil.isNotEmpty(pomPurchaseContractHead)) {
            orderStatus = pomPurchaseContractHead.getOrderStatus();
        }
        //采购订单可退回的状态，20-审批中；72-变更审批中
        List<String> statusList = Arrays.asList(
                PomBizConstants.PURCHASE_CONTRACT_STATUS.APPROVING,
                PomBizConstants.PURCHASE_CONTRACT_STATUS.ALTER_APPROVING
        );
        if (!statusList.contains(orderStatus)) {
            throw new BusinessException(120_000, "期望退回的订单中订单状态不合法");
        }
        //如采购订单状态为20-审批中，则修改采购订单状态为11-审批退回，并同步修改历史采购订单状态
        if (PomBizConstants.PURCHASE_CONTRACT_STATUS.APPROVING.equals(orderStatus)) {
            PomPurchaseContractHead purchaseContractHead = new PomPurchaseContractHead();
            purchaseContractHead.setPurchaseContractHeadId(purchaseContractHeadId);
            // 合同状态：11-审批退回
            purchaseContractHead.setContractStatus(PomBizConstants.PURCHASE_CONTRACT_STATUS.APPROVE_BACK);
            purchaseContractHead.setOrderStatus(PomBizConstants.PURCHASE_CONTRACT_STATUS.APPROVE_BACK);
            log.info("POM doRejectBusiness purchaseContractHead:{} ", JSONUtil.toJSONString
                    (purchaseContractHead));
            int effect = pomPurchaseContractHeadModel.savePurchaseContractHead(purchaseContractHead);
            //修改历史采购订单状态,根据采购订单id与其当前版本号
            PomPurchaseContractHeadHistoryEntity purchaseContractHistory = new PomPurchaseContractHeadHistoryEntity();
            purchaseContractHistory.setPurchaseContractHeadId(purchaseContractHeadId);
            purchaseContractHistory.setVersionNo(pomPurchaseContractHead.getVersionNo());
            purchaseContractHistory.setContractStatus(PomBizConstants.PURCHASE_CONTRACT_STATUS.APPROVE_REJECT);
            purchaseContractHistory.setOrderStatus(PomBizConstants.PURCHASE_CONTRACT_STATUS.APPROVE_REJECT);
            log.info("POM doRejectBusiness purchaseContractHistory:{} ", JSONUtil.toJSONString
                    (purchaseContractHistory));
            int effectRows = pomPurchaseContractHeadHistoryMapper.updateStatus
                    (purchaseContractHistory);
            if (effect <= 0 || effectRows <= 0) {
                log.error("采购订单退回，采购订单 effect:[{}],历史采购订单effectRows:[{}]", effect, effectRows);
                throw new BusinessException(120_000, "采购订单退回，状态更新失败");
            }
        }
        //如采购订单状态为72-变更审批中，则修改采购订单状态为74-变更审批退回
        if (PomBizConstants.PURCHASE_CONTRACT_STATUS.ALTER_APPROVING.equals(orderStatus)) {
            PomPurchaseContractHead alterPurchaseContractHead = new PomPurchaseContractHead();
            alterPurchaseContractHead.setPurchaseContractHeadId(purchaseContractHeadId);
            // 合同状态：74-变更审批退回
            alterPurchaseContractHead.setOrderStatus(PomBizConstants.PURCHASE_CONTRACT_STATUS.ALTER_APPROVE_REJECT);
            log.info("POM doRejectBusiness alterPurchaseContractHead:{} ", JSONUtil
                    .toJSONString(alterPurchaseContractHead));
            int effects = pomPurchaseContractHeadModel.updatePurchaseContractHeadStatus(alterPurchaseContractHead);
            if (effects <= 0) {
                log.error("采购订单退回，采购订单 effects:[{}]", effects);
                throw new BusinessException(120_000, "采购订单退回，状态更新失败");
            }
        }
        // 发送短信

        /*try {
            RejectSmsReq rejectSmsReq = new RejectSmsReq();
            rejectSmsReq.setApproveTypeCode(APPROVE_TYPE_CODE);
            rejectSmsReq.setApproveTypeCode(APPROVE_TYPE_NAME);
            rejectSmsReq.setBusinessKey(pomPurchaseContractHead.getPurchaseContractHeadId());
            rejectSmsReq.setBusinessCode(pomPurchaseContractHead.getPurchaseContractCode());
            rejectSmsReq.setProcessInstanceId(request.getProcInstId());
            rejectSmsReq.setProcessNodeNotice(request.getProcessNodeNotice());
            workFlowProcessService.sendRejectSmsToProcessStarter(rejectSmsReq);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }*/
    }

    private MissionConfigRuleResp getRule(PomPurchaseContractHead pomPurchaseContractHead) {
        MissionRuleReq ruleReq = new MissionRuleReq();
        ruleReq.setSerialType(MissionConstants.OrderType.POM_ORDER.getCode());
        ruleReq.setSerialCode(pomPurchaseContractHead.getPurchaseContractCode());
        //配置的状态是待提交
        ruleReq.setSerialDealStatus(PomConstants.OrderStatus.WAIT_SUPPLY_SUBMIT.getKey());
        log.info("获取采购单配置...入参：{}", JSON.toJSONString(ruleReq));
        ResponseBO<MissionConfigRuleResp> ruleConfig = null;
        try {
            ruleConfig = missionService.getMissionConfig(ruleReq);
        } catch (Exception e) {
            log.error("查询配置失败{},入参:{}", e.getMessage(), JSON.toJSONString(ruleReq));
        }
        return Objects.isNull(ruleConfig) ? null : ruleConfig.getData();
    }

    /**
     * 采购单撤回后执行逻辑
     *
     * @param pomPurchaseContractHeadList
     */
    private void doRevokeBusiness(List<PomPurchaseContractHead> pomPurchaseContractHeadList) {
        for (PomPurchaseContractHead pomPurchaseContractHead : pomPurchaseContractHeadList) {
            String purchaseContractHeadId = pomPurchaseContractHead.getPurchaseContractHeadId();
            String orderStatus = pomPurchaseContractHead.getOrderStatus();
            if (ObjectUtil.isNotEmpty(pomPurchaseContractHead)
                    && !PomBizConstants.PURCHASE_CONTRACT_STATUS.APPROVING.equals(orderStatus) &&
                    !PomBizConstants.PURCHASE_CONTRACT_STATUS.ALTER_APPROVING.equals(orderStatus)) {
                throw new BusinessException(120_000, "期望撤回的订单中订单状态不合法");
            }
            PomPurchaseContractHead pomPurContHead = new PomPurchaseContractHead();

            String conStatus = pomPurchaseContractHead.getOrderStatus();
            pomPurContHead.setPurchaseContractHeadId(purchaseContractHeadId);
            if (PomBizConstants.PURCHASE_CONTRACT_STATUS.APPROVING.equals(conStatus)) {
                String submitPending = PomBizConstants.PURCHASE_CONTRACT_STATUS.SUBMIT_PENDING;
                pomPurContHead.setOrderStatus(submitPending);
                pomPurContHead.setContractStatus(submitPending);
                PomPurchaseContractHeadHistoryEntity purchaseContractHeadHistory = new
                        PomPurchaseContractHeadHistoryEntity();
                purchaseContractHeadHistory.setContractStatus(submitPending);
                purchaseContractHeadHistory.setOrderStatus(submitPending);
                purchaseContractHeadHistory.setPurchaseContractHeadId(purchaseContractHeadId);
                int updPurchaseVersionNum = pomPurchaseContractHeadHistoryMapper.updateStatus(purchaseContractHeadHistory);
                if (updPurchaseVersionNum == 0) {
                    throw new BusinessException(120_000, "采购订单撤回，合同状态更新失败");
                }
            } else if (PomBizConstants.PURCHASE_CONTRACT_STATUS.ALTER_APPROVING.equals(conStatus)) {
                String submitPending = PomBizConstants.PURCHASE_CONTRACT_STATUS.ALTER_SUBMIT_PENDING;

                pomPurContHead.setOrderStatus(submitPending);
            }

            log.info("PomPurchaseContractRevokeWfBiz doBusiness:{} ", JSONUtil.toJSONString(pomPurContHead));
            int updPurHeadCount = pomPurchaseContractHeadMapper.updateStatus(pomPurContHead);
            if (updPurHeadCount != 1) {
                throw new BusinessException(120_000, "采购订单撤回，合同状态更新失败");
            }
        }
    }

    /**
     * 领用，创建发货通知
     *
     * @param
     * @return
     *//*
    private String createDeliveryNotice(SomSalesContractHeadEntity somSalesContractHeadEntity, List<String> lineIdList){
        DeliveryNoticeHeadEntity deliveryNoticeHeadEntity = new DeliveryNoticeHeadEntity();
        BeanUtil.copyProperties(somSalesContractHeadEntity, deliveryNoticeHeadEntity);
        // 1. 保存发货通知主信息,设置供方-万郡
        // 生成发货通知号
        ResponseBO<SerialNumberVO> serialNumberVOResponseBO = sequenceService.nextByDefineId(BizStatus.SEQUENCE_SERIAL_DEF.DELIVERY_NOTICE_CODE);
        String deliveryNoticeCode = serialNumberVOResponseBO.getData().getBizCode();
        deliveryNoticeHeadEntity.setDeliveryNoticeCode(deliveryNoticeCode);
        deliveryNoticeHeadEntity.setSupplierName("");
        deliveryNoticeHeadEntity.setSupplierCode("");
        deliveryNoticeHeadEntity.setSupplierCompanyId("");

        // 初始化状态
        deliveryNoticeHeadEntity.setNoticeStatus(SomBizConstants.DELIVERY_NOTICE_STATUS.SUBMIT_PENDING);
        deliveryNoticeHeadEntity.setFromSource(BizStatus.FROM_SOURCE.RECEIVE);
        deliveryNoticeHeadEntity.setFromType("SOMPLAN");
        deliveryNoticeHeadEntity.setFromCode(somSalesContractHeadEntity.getSalesContractHeadCode());
        deliveryNoticeHeadEntity.setFromId(somSalesContractHeadEntity.getSalesContractHeadId());
        deliveryNoticeHeadMapper.insert(deliveryNoticeHeadEntity);

        // 2. 保存发货通知明细
        List<SomSalesContractLineEntity> somSalesContractLineEntities = somSalesContractLineMapper.selectBatchIds(lineIdList);
        Map<String, SomSalesContractLineEntity> salesContractLineMap = somSalesContractLineEntities.stream().collect(Collectors.toMap(SomSalesContractLineEntity::getSalesContractLineId, a -> a));
        List<DeliveryNoticeLineEntity> deliveryNoticeLineList = BeanUtil.convert(somSalesContractLineEntities, DeliveryNoticeLineEntity.class);
        deliveryNoticeLineList.stream().forEach(noticeLine -> {
            SomSalesContractLineEntity lineEntity = salesContractLineMap.get(noticeLine.getSalesContractLineId());
            // 生成发货通知line code
            ResponseBO<SerialNumberVO> serialNumberVOResponseLineBO = sequenceService.nextByBizCode(BizStatus.SEQUENCE_SERIAL_DEF.DELIVERY_NOTICE_LINE_CODE, deliveryNoticeCode);
            String deliveryNoticeLineCode = serialNumberVOResponseLineBO.getData().getBizCode();
            noticeLine.setDeliveryNoticeHeadId(deliveryNoticeHeadEntity.getDeliveryNoticeHeadId());
            noticeLine.setDeliveryNoticeLineCode(deliveryNoticeLineCode);
            noticeLine.setPrimaryUnitType(lineEntity.getUnitType());
            noticeLine.setNoticeQuantity(lineEntity.getReceiveQuantity());
            noticeLine.setNoticeUnitType(lineEntity.getUnitType());
            noticeLine.setFromType("SOMPLAN");
            noticeLine.setFromLineId(noticeLine.getSalesContractLineId());
            noticeLine.setFromLineCode(lineEntity.getSalesContractLineCode());
            deliveryNoticeLineMapper.insert(noticeLine);
        });

        // 3. TODO 减商品库存
//		skuWarehouseRelationsFeign.getStock();
        return deliveryNoticeCode;
    }*/
}
