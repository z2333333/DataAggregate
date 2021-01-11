package com.vd.canary.obmp.order.service.impl;

import com.vd.canary.b2b.provider.service.impl.BaseServiceImpl;
import com.vd.canary.core.bo.ResponseBO;
import com.vd.canary.core.bo.ResponsePageBO;
import com.vd.canary.obmp.order.api.request.order.suppliersaas.*;
import com.vd.canary.obmp.order.api.response.common.BooleanResp;
import com.vd.canary.obmp.order.api.response.vo.suppliersaas.SupplyInquirySaasHeadDetailDelayVO;
import com.vd.canary.obmp.order.api.response.vo.suppliersaas.SupplyInquirySaasHeadDetailVO;
import com.vd.canary.obmp.order.api.response.vo.suppliersaas.SupplyInquirySaasHeadVO;
import com.vd.canary.obmp.order.api.response.vo.suppliersaas.SupplyInquirySaasVO;
import com.vd.canary.obmp.order.repository.entity.SupplyInquiryHeadEntity;
import com.vd.canary.obmp.order.repository.mapper.SupplyInquiryHeadMapper;
import com.vd.canary.obmp.order.service.SupplySaasInquiryHeadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 供方SAAS 询报价单表 服务实现类
 * </p>
 *
 * @author : xingdongyang   </p>
 * @date : 2020-04-5  </p>
 * Created with Canary Automatic Code Generator  </p>
 */
@Slf4j
@Service("supplyInquirySaasHeadService")
public class SupplySaasInquiryHeadServiceImpl extends BaseServiceImpl<SupplyInquiryHeadMapper, SupplyInquiryHeadEntity> implements SupplySaasInquiryHeadService {
    @Override
    public ResponsePageBO<SupplyInquirySaasVO> queryPageList(SupplierSaasInquiryListReq supplierInquiryListReq) {
        return null;
    }

    @Override
    public ResponseBO<List<SupplyInquirySaasHeadVO>> supplyInquiryGoodsDetail(SupplierSaasInquiryDetailReq detailId) {
        return null;
    }

    @Override
    public ResponseBO<SupplyInquirySaasHeadDetailVO> queryDetail(SupplierSaasInquiryDetailReq req) {
        return null;
    }

    @Override
    public ResponseBO<BooleanResp> defineSupplyQuotes(List<QuotesLineUpdate> quotesLineUpdateList) {
        return null;
    }

    @Override
    public ResponseBO<BooleanResp> operateSupplyQuotes(QuotesHeadUpdate quotesHeadUpdate) {
        return null;
    }

    @Override
    public ResponseBO<SupplyInquirySaasHeadDetailDelayVO> delay(SupplierSaasInquiryDelayReq detailId) {
        return null;
    }

    @Override
    public void dealExpired(String quoteHeadId) {

    }

//    @Autowired
//    private SupplyInquiryHeadMapper supplyInquiryHeadMapper;
//    @Autowired
//    private SupplyQuotesHeadService supplyQuotesHeadService;
//    @Autowired
//    private SupplyQuotesLineService supplyQuotesLineService;
//    @Autowired
//    private SupplyInquiryLineService supplyInquiryLineService;
//    @Autowired
//    private FileManagementModel fileManagementModel;
//    @Autowired
//    private InquiryLineService inquiryLineService;
//    @Autowired
//    private InquiryHeadService inquiryHeadService;
//    @Autowired
//    private QuotesHeadMapper quotesHeadMapper;
//    @Autowired
//    private QuotesLineService quotesLineService;
//    @Autowired
//    private ShipmentPlanBatchService shipmentPlanBatchService;
//    @Autowired
//    private ShipmentPlanBatchLineService shipmentPlanBatchLineService;
//    @Autowired
//    private MissionService missionService;
//    @Autowired
//    private DelayQueueService delayQueueService;
//    @Autowired
//    SmsServiceImpl smsService;
//    @Autowired
//    StaffInfoFeignClient staffInfoFeignClient;
//    @Autowired
//    private FileBillFeignClient fileBillFeignClient;
//    @Autowired
//    private FileCommonServiceFeign fileCommonServiceFeign;
//    @Autowired
//    private FileServiceFeign fileServiceFeign;
//
//    @Override
//    public ResponsePageBO<SupplyInquirySaasVO> queryPageList(SupplierSaasInquiryListReq supplierInquiryListReq) {
//        if (supplierInquiryListReq.getInquiryDateEnd() != null) {
//            supplierInquiryListReq.setInquiryDateEnd(new Date(supplierInquiryListReq.getInquiryDateEnd().getTime() + DateUtil.MILLIS_A_DAY));
//        }
//        PageHelper.startPage(supplierInquiryListReq.getPageNum(), supplierInquiryListReq.getPageSize());
//        if(supplierInquiryListReq.getQuotesStatus()!=null){
//            List<String> filterStatus = CLOSED.getKey().equalsIgnoreCase(supplierInquiryListReq.getQuotesStatus()) ?
//                    Lists.newArrayList(INVALID.getKey(), CLOSED.getKey(), REJECT.getKey())
//                    : Lists.newArrayList(supplierInquiryListReq.getQuotesStatus());
//            supplierInquiryListReq.setFilterStatus(filterStatus);
//        }
//        List<SupplyInquirySaasVO> supplyInquiryHeadList = supplyInquiryHeadMapper.queryListBySaas(supplierInquiryListReq);
//        PageInfo<SupplyInquirySaasVO> pageInfo = new PageInfo<>(supplyInquiryHeadList);
//
//        List<SupplyInquirySaasVO> list = new ArrayList<>();
//        for (SupplyInquirySaasVO supplyInquiryEntity : supplyInquiryHeadList) {
//            //查询商品个数
//            List<SupplyInquiryLineEntity> supplyInquiryLineEntityList = supplyInquiryLineService.getBaseMapper().selectList(
//                    new LambdaQueryWrapper<SupplyInquiryLineEntity>().eq(SupplyInquiryLineEntity::getInquiryHeadId, supplyInquiryEntity.getInquiryId()));
//            supplyInquiryEntity.setQuantity(new BigDecimal(supplyInquiryLineEntityList.size()));
//            list.add(supplyInquiryEntity);
//        }
//        return PageResponseUtil.ok(supplierInquiryListReq, pageInfo.getTotal(), list);
//    }
//
//    @Override
//    public ResponseBO<List<SupplyInquirySaasHeadVO>> supplyInquiryGoodsDetail(SupplierSaasInquiryDetailReq detailId) {
//        return ResponseUtil.ok(supplyInquiryHeadMapper.supplyInquiryGoodsDetail(detailId.getInquiryHeadId(), CustomerUtil.getCustomerId()));
//    }
//
//    @Override
//    public ResponseBO<SupplyInquirySaasHeadDetailVO> queryDetail(SupplierSaasInquiryDetailReq req) {
//        SupplyInquirySaasHeadDetailVO supplyInquiryDetail = new SupplyInquirySaasHeadDetailVO();
//        SupplyInquiryHeadEntity entity = supplyInquiryHeadMapper.selectById(req.getInquiryHeadId());
//        if (Objects.isNull(entity)) {
//            throw new BusinessException(401, "查询的数据不存在，请重新选择");
//        }
//        BeanUtils.copyProperties(entity, supplyInquiryDetail);
//        supplyInquiryDetail.setQuoteIncludeFreight(entity.getIncludeFreight());
//        //todo 延时处理
//        supplyInquiryDetail.setDelay(getDelay(entity.getInquiryId()));
//        //增加询价商品信息
//        SupplyQuotesHeadEntity supplyQuotesHeadEntity = supplyQuotesHeadService.getBaseMapper().selectOne(new LambdaQueryWrapper<SupplyQuotesHeadEntity>().
//                eq(SupplyQuotesHeadEntity::getInquiryHeadId, req.getInquiryHeadId()).eq(SupplyQuotesHeadEntity::getSupplyId, req.getCustomerId()).last(" limit 1"));
//        if (Objects.nonNull(supplyQuotesHeadEntity)) {
//            supplyInquiryDetail.setQuotesStatus(supplyQuotesHeadEntity.getQuotesStatus());
//            supplyInquiryDetail.setQuoteRemark(supplyQuotesHeadEntity.getRemark());
//            // 特殊处理拒绝原因 状态为关闭时使用关闭原因替换拒绝原因
//            supplyInquiryDetail.setRefuseReason(SupplyInquiryConstants.QuotesStatus.CLOSED.getKey().equals(supplyQuotesHeadEntity.getQuotesStatus())
//                    ? supplyQuotesHeadEntity.getCloseReason() : supplyQuotesHeadEntity.getRefuseReason());
//            supplyInquiryDetail.setQuoteExpectedDeliveryDate(supplyQuotesHeadEntity.getExpectedDeliveryDate());
//            supplyInquiryDetail.setQuoteHeadId(supplyQuotesHeadEntity.getQuoteHeadId());
//            supplyInquiryDetail.setInquiryDate(Objects.isNull(supplyQuotesHeadEntity.getGmtModifyTime())
//                    ? supplyQuotesHeadEntity.getGmtCreateTime() : supplyQuotesHeadEntity.getGmtModifyTime());
//        }
//        List<SupplyInquirySaasHeadVO> goods = supplyInquiryHeadMapper.supplyInquiryGoodsDetail(req.getInquiryHeadId(), req.getCustomerId());
//        InquiryHeadEntity inquiryHead = getInquiryHead(entity.getFromCode());
//        if (inquiryHead != null) {
//            List<InquiryLineEntity> inquiryLines = getInquiryLine(inquiryHead.getInquiryHeadId());
//            List<ShipmentPlanBatchEntity> batchs = getBatch(inquiryHead.getDemandHeadId());
//            List<ShipmentPlanBatchLineEntity> batchLines = getBatchLine(batchs);
//            goods.stream().forEach(good->{
//                good.setBatch(generate(batchs, batchLines, good.getSkuId()));
//                good.setAttachment(getAttachment(inquiryLines, good.getSkuId()));
//            });
//        }
//        // 商品详情
//        supplyInquiryDetail.setQuoteGoods(goods);
//        return ResponseUtil.ok(supplyInquiryDetail);
//    }
//
//    public SupplyInquirySaasHeadDetailDelayVO getDelay(String supplyInquiryHeadId) {
//        SupplyInquirySaasHeadDetailDelayVO delayVO = new SupplyInquirySaasHeadDetailDelayVO();
//        Long surplusTime = delayQueueService.getSurplusTime(DelayQueueService.KEY_SUPPLY_INQUIRY.concat(supplyInquiryHeadId));
//        delayVO.setRemain(surplusTime);
//        MissionConfigRuleResp rule = getRule();
//        int delayCount = rule != null && rule.getDelayCount() > 0 && rule.getDelayMinute() > 0 ? rule.getDelayCount() : 0;
//        int delayMinute = rule != null && rule.getDelayMinute() > 0 ? rule.getDelayMinute() : 0;
//        boolean canDelay = delayQueueService.canOperateDelay(DelayQueueService.KEY_SUPPLY_INQUIRY.concat(supplyInquiryHeadId), delayCount);
//        delayVO.setDelay(canDelay);
//        delayVO.setDelayMinute(delayMinute);
//        return delayVO;
//    }
//
//    @Override
//    public ResponseBO<SupplyInquirySaasHeadDetailDelayVO> delay(SupplierSaasInquiryDelayReq detailId) {
//        MissionConfigRuleResp rule = getRule();
//        if (rule != null && rule.getDelayCount() > 0 && rule.getDelayMinute() > 0) {
//            delayQueueService.operateDelay(DelayQueueService.KEY_SUPPLY_INQUIRY.concat(detailId.getInquiryHeadId()), rule.getDelayMinute(), TimeUnit.MINUTES, rule.getDelayCount());
//        }
//        return ResponseUtil.ok(getDelay(detailId.getInquiryHeadId()));
//    }
//
//    /** 获取接单规则*/
//    private MissionConfigRuleResp getRule() {
//        MissionRuleReq ruleReq = new MissionRuleReq();
//        ruleReq.setSerialType(MissionConstants.OrderType.UN_SOM_ENQUIRY.getCode());
//        ruleReq.setSerialDealStatus(SupplyInquiryConstants.InquiryStatus.WAIT_FOR_QUOTE.getKey());
//        try {
//            ResponseBO<MissionConfigRuleResp> ruleConfig = missionService.getMissionConfig(ruleReq);
//            return ruleConfig.getData();
//        } catch (Exception e) {
//            log.error("查询配置失败{},入参:{}", e.getMessage(),JSON.toJSONString(ruleReq));
//        }
//        return null;
//    }
//
//    /**
//     * 组装批次信息
//     */
//    private List<SupplyGoodsBatchLineVO> generate(List<ShipmentPlanBatchEntity> batchs, List<ShipmentPlanBatchLineEntity> batchLines, String skuId) {
//        List<SupplyGoodsBatchLineVO> result = new ArrayList<>();
//        batchs.stream().forEach(batch ->
//                batchLines.stream()
//                        .filter(batchLine -> StringUtils.equalsIgnoreCase(batchLine.getSkuId(), skuId)&&batchLine.getShipmentPlanBatchId().equals(batch.getShipmentPlanBatchId()))
//                        .forEach(batchLine -> {
//                            SupplyGoodsBatchLineVO vo = new SupplyGoodsBatchLineVO();
//                            vo.setSkuId(skuId);
//                            vo.setSomShipmentBatchName(batch.getSomShipmentBatchName());
//                            vo.setExpectedReceiptDate(batch.getExpectedReceiptDate());
//                            vo.setShipmentQuantity(batchLine.getShipmentQuantity());
//                            vo.setUnitType(batchLine.getUnitType());
//                            result.add(vo);
//                        })
//        );
//        return result;
//    }
//
//    private List<ShipmentPlanBatchEntity> getBatch(String demandHeadId) {
//        return shipmentPlanBatchService.list(new LambdaQueryWrapper<ShipmentPlanBatchEntity>()
//                .eq(ShipmentPlanBatchEntity::getDemandHeadId, demandHeadId));
//    }
//
//    private List<ShipmentPlanBatchLineEntity> getBatchLine(List<ShipmentPlanBatchEntity> batchs) {
//        if (CollectionUtils.isEmpty(batchs)) {
//            return Lists.newArrayList();
//        }
//        List<String> shipmentPlanBatchId = batchs.stream().map(batch -> batch.getShipmentPlanBatchId()).collect(Collectors.toList());
//        return shipmentPlanBatchLineService.list(new LambdaQueryWrapper<ShipmentPlanBatchLineEntity>()
//                .in(ShipmentPlanBatchLineEntity::getShipmentPlanBatchId, shipmentPlanBatchId));
//    }
//
//    private List<InquiryLineEntity> getInquiryLine(String inquiryHeadId) {
//        List<InquiryLineEntity> inquiryLines = inquiryLineService.list(new LambdaQueryWrapper<InquiryLineEntity>()
//                .eq(InquiryLineEntity::getInquiryHeadId, inquiryHeadId));
//        return inquiryLines;
//    }
//
//    private InquiryHeadEntity getInquiryHead(String inquiryCode) {
//        return inquiryHeadService.getOne(new LambdaQueryWrapper<InquiryHeadEntity>()
//                .eq(InquiryHeadEntity::getInquiryCode, inquiryCode).last("limit 1"));
//    }
//
//    /** 查询商品附件*/
//    private List<FileManagementVO> getAttachment(List<InquiryLineEntity> inquiryLines,String skuId) {
//        List<InquiryLineEntity> lineEntity = inquiryLines.stream()
//                .filter(line -> StringUtils.equalsIgnoreCase(skuId, line.getSkuId())).collect(Collectors.toList());
//        if (CollectionUtils.isEmpty(lineEntity)) {
//            return Lists.newArrayList();
//        }
//        //List<FileManagementEntity> demandLineAnnex = fileManagementModel.queryAttachmentList(String.valueOf(lineEntity.get(0).getDemandLineId()), "DemandLineAnnex");
//        ResponseBO<List<FileBillVO>> demandLineAnnex = fileBillFeignClient.listFilesById(String.valueOf(lineEntity.get(0).getDemandLineId()), "DemandLineAnnex");
//        List<FileBillVO> fileData = demandLineAnnex.getData();
//        List<FileManagementVO> fileManagementVOS=new ArrayList<>();
//        for (FileBillVO fileBillVO:fileData){
//            FileManagementVO fileManagementVO = BeanUtil.convert(fileBillVO, FileManagementVO.class);
//            fileManagementVO.setFileName(fileBillVO.getOriginName());
//            fileManagementVO.setFileUrl(fileBillVO.getFileUrl());
//            fileManagementVO.setForeignKey(fileBillVO.getBillId());
//            fileManagementVO.setId(fileBillVO.getId());
//            fileManagementVO.setContentType(fileBillVO.getContentType());
//            fileManagementVOS.add(fileManagementVO);
//        }
//        return fileManagementVOS;
//    }
//
//    @Override
//    @Transactional(rollbackFor = Exception.class)
//    public ResponseBO<BooleanResp> defineSupplyQuotes(List<QuotesLineUpdate> quotesLineUpdateList) {
//        quotesLineUpdateList.forEach(lineUpdate -> lineUpdate.checkUpdateParam());
//        for (QuotesLineUpdate quotesLineUpdate : quotesLineUpdateList) {
//            SupplyQuotesLineEntity supplyQuotesLineEntity = supplyQuotesLineService.getById(quotesLineUpdate.getQuoteId());
//            if (Objects.isNull(supplyQuotesLineEntity)) {
//                throw new BusinessException(401, "更新的数据不存在，请重新选择");
//            }
//            supplyQuotesLineEntity.setTax(quotesLineUpdate.getTax());
//            // TAX含税价格  NOTAX 不含
//            supplyQuotesLineEntity.setTaxType("TAX");
//            supplyQuotesLineEntity.setPurPrice(quotesLineUpdate.getPurPrice());
//            supplyQuotesLineEntity.setLogisticsPrice(quotesLineUpdate.getLogisticsPrice());
//            supplyQuotesLineEntity.setValidate(new Date(quotesLineUpdate.getValidate().getTime() + 24 * 60 * 60 * 1000 - 1000));
//            if (Objects.nonNull(supplyQuotesLineEntity.getQuantity())) {
//                supplyQuotesLineEntity.setPurAmount(supplyQuotesLineEntity.getQuantity().multiply(quotesLineUpdate.getPurPrice()));
//            } else {
//                supplyQuotesLineEntity.setPurAmount(BigDecimal.ZERO);
//            }
//            supplyQuotesLineService.updateById(supplyQuotesLineEntity);
//        }
//        return ResponseUtil.ok();
//    }
//
//    @Override
//    @Transactional(rollbackFor = Exception.class)
//    public ResponseBO<BooleanResp> operateSupplyQuotes(QuotesHeadUpdate quotesHeadUpdate) {
//        SupplyQuotesHeadEntity existQuotesHead = supplyQuotesHeadService.getById(quotesHeadUpdate.getQuoteHeadId());
//        if (Objects.isNull(existQuotesHead)) {
//            throw new BusinessException(401, "更新的数据不存在，请重新选择");
//        }
//        // 提交报价  验证报价详情
//        if (SupplyInquiryConstants.QuotesStatus.QUOTED.getKey().equals(quotesHeadUpdate.getQuotesStatus())) {
//            List<SupplyQuotesLineEntity> quoteline = supplyQuotesLineService.list(new LambdaQueryWrapper<SupplyQuotesLineEntity>()
//                    .eq(SupplyQuotesLineEntity::getInquiryHeadId, existQuotesHead.getInquiryHeadId()));
//            Date min = null;
//            for (SupplyQuotesLineEntity line : quoteline) {
//                if (line.getLogisticsPrice() == null || line.getPurPrice() == null
//                        || line.getTax() == null || line.getValidate() == null) {
//                    throw new BusinessException(401, "报价信息未填写");
//                }
//                if (min == null) {
//                    min = line.getValidate();
//                }else {
//                    min = line.getValidate().before(min) ? line.getValidate() : min;
//                }
//            }
//            int delaySecond = (int) (min.getTime() - System.currentTimeMillis()) / 1000;
//            // 此处添加延迟失效队列，用于商品行超期处理
//            if(delaySecond>0) {
//                delayQueueService.initDelay(DelayQueueService.KEY_SUPPLY_QUOTE_LINE.concat(quotesHeadUpdate.getQuoteHeadId()), quotesHeadUpdate.getQuoteHeadId(),
//                        delaySecond, TimeUnit.SECONDS, "SupplyQuoteLineExpiredCallBack");
//            }
//        }
//        SupplyQuotesHeadEntity quotesHeadEntity = new SupplyQuotesHeadEntity();
//        quotesHeadEntity.setQuoteHeadId(quotesHeadUpdate.getQuoteHeadId());
//        quotesHeadEntity.setRemark(quotesHeadUpdate.getQuoteRemark());
//        quotesHeadEntity.setIncludeFreight(quotesHeadUpdate.getIncludeFreight());
//        quotesHeadEntity.setExpectedDeliveryDate(quotesHeadUpdate.getExpectedDeliveryDate());
//        quotesHeadEntity.setQuotesStatus(quotesHeadUpdate.getQuotesStatus());
//        if (REJECT.getKey().equals(quotesHeadUpdate.getQuotesStatus())) {
//            quotesHeadEntity.setRefuseReason(quotesHeadUpdate.getRefuseReason());
//        }
//        if (SupplyInquiryConstants.QuotesStatus.QUOTED.getKey().equals(quotesHeadUpdate.getQuotesStatus())) {
//            quotesHeadEntity.setQuoteDate(new Date());
//        }
//        supplyQuotesHeadService.getBaseMapper().updateById(quotesHeadEntity);
//        if (!SupplyInquiryConstants.QuotesStatus.WAIT_FOR_QUOTE.getKey().equals(quotesHeadUpdate.getQuotesStatus())) {
//            //修改供方询报价单表 的状态数据
//            SupplyInquiryHeadEntity updateHead = new SupplyInquiryHeadEntity();
//            List<SupplyQuotesHeadEntity> allQuotesHead = supplyQuotesHeadService.getBaseMapper().selectList(new LambdaQueryWrapper<SupplyQuotesHeadEntity>()
//                    .eq(SupplyQuotesHeadEntity::getInquiryHeadId, existQuotesHead.getInquiryHeadId()));
//
//            List<SupplyQuotesHeadEntity> rejectQuotes = supplyQuotesHeadService.getBaseMapper().selectList(new LambdaQueryWrapper<SupplyQuotesHeadEntity>()
//                    .eq(SupplyQuotesHeadEntity::getInquiryHeadId, existQuotesHead.getInquiryHeadId())
//                    .eq(SupplyQuotesHeadEntity::getQuotesStatus, REJECT.getKey()));
//
//            List<SupplyQuotesHeadEntity> quotes = supplyQuotesHeadService.getBaseMapper().selectList(new LambdaQueryWrapper<SupplyQuotesHeadEntity>()
//                    .eq(SupplyQuotesHeadEntity::getInquiryHeadId, existQuotesHead.getInquiryHeadId())
//                    .eq(SupplyQuotesHeadEntity::getQuotesStatus, SupplyInquiryConstants.QuotesStatus.QUOTED.getKey()));
//            if (allQuotesHead.size() == rejectQuotes.size()) {
//                // 所有供方拒绝询价, 产品要求中台已关闭
//                updateHead.setInquiryStatus(SupplyInquiryConstants.InquiryStatus.CLOSED.getKey());
//                log.info("供方拒绝报价，关闭需方询价单,询价单ID:{}", existQuotesHead.getInquiryHeadId());
//            } else if (allQuotesHead.size() == quotes.size()) {
//                // 所有供方提交询价, 产品要求中台展示 已报价
//                updateHead.setInquiryStatus(SupplyInquiryConstants.InquiryStatus.QUOTED.getKey());
//            } else {
//                // 展示部分报价
//                updateHead.setInquiryStatus(SupplyInquiryConstants.InquiryStatus.QUOTED.getKey());
//            }
//            updateHead.setInquiryId(existQuotesHead.getInquiryHeadId());
//            supplyInquiryHeadMapper.updateById(updateHead);
//            //todo 短信通知 已报价 已拒绝
//            sendSupplyInqueryMessage(updateHead);
//        }
//
//        return ResponseUtil.ok();
//    }
//
//    @Override
//    public void dealExpired(String quoteHeadId) {
//        SupplyQuotesHeadEntity supplyQuotesHeadEntity = supplyQuotesHeadService.getOne(new LambdaQueryWrapper<SupplyQuotesHeadEntity>()
//                .eq(SupplyQuotesHeadEntity::getQuoteHeadId, quoteHeadId)
//                .last("limit 1"));
//        if (supplyQuotesHeadEntity == null) {
//            return;
//        }
//        if (!StringUtils.equalsIgnoreCase(supplyQuotesHeadEntity.getQuotesStatus(), SupplyInquiryConstants.QuotesStatus.QUOTED.getKey())) {
//            return;
//        }
//        // 检查是否已经选择供方核价
//        SupplyInquiryHeadEntity supplyInquiryHeadEntity = supplyInquiryHeadMapper.selectOne(new LambdaQueryWrapper<SupplyInquiryHeadEntity>()
//                .eq(SupplyInquiryHeadEntity::getInquiryId, supplyQuotesHeadEntity.getInquiryHeadId())
//                .last("limit 1"));
//        if (supplyInquiryHeadEntity == null) {
//            log.info("无法获取供方询价单信息,supplyQuoteHeadId:{}", quoteHeadId);
//            return;
//        }
//        InquiryHeadEntity inquiryHeadEntity = inquiryHeadService.getOne(new LambdaQueryWrapper<InquiryHeadEntity>()
//                .eq(InquiryHeadEntity::getInquiryHeadId, supplyInquiryHeadEntity.getFromId())
//                .last("limit 1"));
//        if (inquiryHeadEntity == null) {
//            log.info("无法获取询价单信息,inquiryHeadId:{}", supplyInquiryHeadEntity.getFromId());
//            return;
//        }
//        QuotesHeadEntity quotesHeadEntity = quotesHeadMapper.selectOne(new LambdaQueryWrapper<QuotesHeadEntity>()
//                .eq(QuotesHeadEntity::getInquiryHeadId, inquiryHeadEntity.getInquiryHeadId())
//                .last("limit 1"));
//        if (quotesHeadEntity == null) {
//            log.info("无法获取报价单信息,inquiryHeadId:{}", inquiryHeadEntity.getInquiryHeadId());
//            return;
//        }
//        List<QuotesLineEntity> list = quotesLineService.list(new LambdaQueryWrapper<QuotesLineEntity>()
//                .eq(QuotesLineEntity::getQuotesHeadId, quotesHeadEntity.getQuotesHeadId())
//                .eq(QuotesLineEntity::getSupplierId, supplyQuotesHeadEntity.getSupplyId()));
//        if (CollectionUtils.isNotEmpty(list)) {
//            log.info("询价单已选择供方报价，不做失效处理:supplyQuoteHeadId:{},supplyId:{},quotesHeadId:{}",
//                    supplyQuotesHeadEntity.getQuoteHeadId(), supplyQuotesHeadEntity.getSupplyId(), inquiryHeadEntity.getInquiryHeadId());
//            return;
//        }
//        SupplyQuotesHeadEntity quoteUpdate = new SupplyQuotesHeadEntity();
//        quoteUpdate.setQuoteHeadId(quoteHeadId);
//        quoteUpdate.setQuotesStatus(INVALID.getKey());
//        quoteUpdate.setCloseReason("供方报价超过有效期");
//        supplyQuotesHeadService.updateById(quoteUpdate);
//        SupplyInquiryHeadEntity inquiryUpdate = new SupplyInquiryHeadEntity();
//        inquiryUpdate.setInquiryId(supplyQuotesHeadEntity.getInquiryHeadId());
//        log.info("供方商品超过最小报价日期，失效需方询价单,询价单ID:{}", supplyQuotesHeadEntity.getInquiryHeadId());
//        inquiryUpdate.setInquiryStatus(SupplyInquiryConstants.InquiryStatus.INVALID.getKey());
//        supplyInquiryHeadMapper.updateById(inquiryUpdate);
//    }
//
//    private void sendSupplyInqueryMessage(SupplyInquiryHeadEntity updateHead) {
//        try{
//            if (!SupplyInquiryConstants.QuotesStatus.QUOTED.getKey().equals(updateHead.getInquiryStatus())) {
//                //已报价
//                String staffId = updateHead.getStaffId();
//                ResponseBO<StaffInfoVO> staffInfoVOResponseBO = staffInfoFeignClient.get(staffId);
//                StaffInfoVO staffInfoVO = staffInfoVOResponseBO.getData();
//                HashMap<String, String> objectObjectHashMap = Maps.newHashMap();
//                objectObjectHashMap.put("OfferOrderCode", updateHead.getInquiryCode());
//                smsService.sendSms(staffInfoVO.getTelephone(),smsService.SMS_195863314,objectObjectHashMap);
//            }
//            if (!REJECT.getKey().equals(updateHead.getInquiryStatus())) {
//                //已拒绝
//                String staffId = updateHead.getStaffId();
//                ResponseBO<StaffInfoVO> staffInfoVOResponseBO = staffInfoFeignClient.get(staffId);
//                StaffInfoVO staffInfoVO = staffInfoVOResponseBO.getData();
//                HashMap<String, String> objectObjectHashMap = Maps.newHashMap();
//                objectObjectHashMap.put("OfferOrderCode", updateHead.getInquiryCode());
//                smsService.sendSms(staffInfoVO.getTelephone(),smsService.SMS_195873227,objectObjectHashMap);
//            }
//        }catch (Exception e){
//            log.error("供方拒绝报价后通知采购经理|供方提交询价信息后通知采购经理-发送短信失败，supplyInquiryHeadId：{},e",updateHead.getInquiryId(),e.getMessage());
//
//        }
//
//    }

}
