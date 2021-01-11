package com.vd.canary.obmp.order.service.impl;


import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Maps;
import com.vd.canary.b2b.provider.service.impl.BaseServiceImpl;
import com.vd.canary.b2b.service.util.TokenUtil;
import com.vd.canary.core.bo.ResponseBO;
import com.vd.canary.core.bo.ResponsePageBO;
import com.vd.canary.core.exception.BusinessException;
import com.vd.canary.core.util.PageResponseUtil;
import com.vd.canary.core.util.ResponseUtil;
import com.vd.canary.file.api.feign.FileBillFeignClient;
import com.vd.canary.file.api.response.vo.FileBillVO;
import com.vd.canary.obmp.order.api.constants.*;
import com.vd.canary.obmp.order.api.operation.sale.request.SomSalesCreateInquiryLineReq;
import com.vd.canary.obmp.order.api.operation.sale.request.SomSalesCreateInquiryReq;
import com.vd.canary.obmp.order.api.request.*;
import com.vd.canary.obmp.order.api.request.mission.*;
import com.vd.canary.obmp.order.api.request.order.InquiryCountReq;
import com.vd.canary.obmp.order.api.request.order.SupplyInquiryQuotesInfoReq;
import com.vd.canary.obmp.order.api.request.order.SupplyInquiryShutReq;
import com.vd.canary.obmp.order.api.response.*;
import com.vd.canary.obmp.order.api.response.common.BooleanResp;
import com.vd.canary.obmp.order.api.response.mission.DispatchUnSomDetailResp;
import com.vd.canary.obmp.order.api.response.mission.DispatchUnSomResp;
import com.vd.canary.obmp.order.api.response.vo.*;
import com.vd.canary.obmp.order.api.response.vo.order.InquiryCountResp;
import com.vd.canary.obmp.order.api.response.vo.order.SupplyInquiryQuotesInfoResp;
import com.vd.canary.obmp.order.api.response.vo.order.SupplyInquiryQuotesInfoVO;
import com.vd.canary.obmp.order.api.response.vo.order.SupplyQuotesLineVO;
import com.vd.canary.obmp.order.api.status.InquiryResponseStatus;
import com.vd.canary.obmp.order.operation.sales.service.SomSalesCreateService;
import com.vd.canary.obmp.order.repository.dto.InquiryHeadDTO;
import com.vd.canary.obmp.order.repository.dto.InquiryLineDTO;
import com.vd.canary.obmp.order.repository.entity.InquiryLineGmEntity;
import com.vd.canary.obmp.order.repository.entity.OrderVersionEntity;
import com.vd.canary.obmp.order.repository.entity.QuotesHeadEntity;
import com.vd.canary.obmp.order.repository.entity.QuotesLineEntity;
import com.vd.canary.obmp.order.repository.entity.order.DemandHeadEntity;
import com.vd.canary.obmp.order.repository.entity.order.DemandLineEntity;
import com.vd.canary.obmp.order.repository.entity.order.InquiryHeadEntity;
import com.vd.canary.obmp.order.repository.entity.order.InquiryLineEntity;
import com.vd.canary.obmp.order.repository.mapper.*;
import com.vd.canary.obmp.order.repository.mapper.order.DemandHeadMapper;
import com.vd.canary.obmp.order.repository.mapper.order.FileManagementMapper;
import com.vd.canary.obmp.order.repository.model.FileManagementModel;
import com.vd.canary.obmp.order.repository.model.InquiryModel;
import com.vd.canary.obmp.order.repository.model.QuotesHeadModel;
import com.vd.canary.obmp.order.service.InquiryLineService;
import com.vd.canary.obmp.order.service.InquiryService;
import com.vd.canary.obmp.order.service.OrderVersionService;
import com.vd.canary.obmp.order.service.QuotesLineService;
import com.vd.canary.obmp.order.service.mission.MissionService;
import com.vd.canary.obmp.order.service.order.DemandHeadService;
import com.vd.canary.obmp.order.util.*;
import com.vd.canary.obmp.staff.api.feign.staff.StaffInfoFeignClient;
import com.vd.canary.obmp.staff.api.response.staff.StaffInfoVO;
import com.vd.canary.obmp.tender.client.CustomerFeignClient;
import com.vd.canary.sequence.api.feign.SequenceService;
import com.vd.canary.sequence.api.response.SerialNumberVO;
import com.vd.canary.utils.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestBody;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static com.vd.canary.obmp.order.api.constants.BizStatus.INQUIRY_TYPE.GENERAL;
import static com.vd.canary.obmp.order.api.constants.BizStatus.INQUIRY_TYPE.STEEL;
import static com.vd.canary.obmp.order.api.constants.BizStatus.QUOTES_FROM_SOURCE.B2B_INQUIRY;
import static com.vd.canary.obmp.order.api.status.InquiryResponseStatus.INTERFACT_QUERY_EXPORT_ERR;

/**
 * 询价单相关
 */
@Slf4j
@Service("iquiryService")
public class InquiryServiceImpl extends BaseServiceImpl<InquiryMapper, InquiryHeadEntity> implements InquiryService {

    @Autowired
    InquiryModel inquiryModel;

    @Autowired
    InquiryMapper somInquiryMapper;

    @Autowired
    private SequenceService sequenceService;
    @Autowired
    InquiryLineMapper somInquiryLineMapper;

    @Autowired
    PdfInquiryUtil pdfInquiryUtil;

    @Autowired
    ExcelInquiryUtil excelInquiryUtil;

    @Autowired
    InquiryLineGmMapper somInquiryLineGmMapper;

    @Autowired
    QuotesHeadModel somQuotesHeadModel;

    @Resource
    private QuotesHeadMapper somQuotesHeadMapper;

    @Autowired
    FileManagementModel fileManagementModel;

    @Autowired
    FileManagementMapper fileManagementMapper;
    @Autowired
    QuotesHeadMapper quotesHeadMapper;
    @Autowired
    QuotesLineGmMapper quotesLineGmMapper;
    @Autowired
    QuotesLineMapper quotesLineMapper;
    @Resource
    InquiryHeadMapper inquiryHeadMapper;
    @Autowired
    private DemandHeadMapper demandHeadMapper;
    @Resource
    private SupplyInquiryServiceImpl supplyInquiryService;
    @Resource
    private MissionService missionService;
    @Resource
    private DemandHeadService demandHeadService;
    @Resource
    private OrderVersionService orderVersionService;
    @Resource
    private SomSalesCreateService somSalesCreateService;
    @Autowired
    QuotesLineService quotesLineService;
    @Autowired
    SmsServiceImpl smsService;
    @Autowired
    StaffInfoFeignClient staffInfoFeignClient;
    @Resource
    OrderAuthUtil orderAuthUtil;
    @Autowired
    private FileBillFeignClient fileBillFeignClient;
    @Autowired
    InquiryLineService inquiryLineService;
    @Autowired
    private CustomerFeignClient customerFeignClient;
    /**
     * 新增/修改询价单
     *
     * @param req
     * @return
     */
    @Override
    @Transactional
    public ResponseBO<InquirySaveResp> saveInquiry(@RequestBody InquirySaveReq req) {
        InquirySaveResp response = new InquirySaveResp();
        // 转换模型
        InquiryHeadEntity inquiryHead = new InquiryHeadEntity();
        BeanUtil.copyProperties(req, inquiryHead);
        List<InquiryLineDTO> inquiryLineList = BeanUtil.convert(req.getList(), InquiryLineDTO.class);
        String inquiryHeadId = inquiryHead.getInquiryHeadId();
        try {
            if (StringUtils.isNotBlank(inquiryHeadId)) { // 修改报价单
                // 校验报价单是否为可修改状态
                // 删除的明细和条款
                String inquiryStatus = inquiryHead.getInquiryStatus();
                if (!inquiryStatus.equals("10")) {
                    throw new BusinessException(InquiryResponseStatus.INTERFACT_SAVE_INQUIRY_STATUS_ERR);
                }
                List<String> removeLineIdList = req.getRemoveList();
                inquiryModel.modifyInquiry(inquiryHead, inquiryLineList, removeLineIdList);
            } else {    // 新增报价单
                if (StringUtils.isBlank(inquiryHead.getInquiryType())) {
                    inquiryHead.setInquiryType(GENERAL);
                }
                InquiryHeadEntity newHead = inquiryModel.createInquiry(inquiryHead, inquiryLineList);
                //新增询价单的同时要新增报价单，如果是中台自制报价单则不新增
                if (!B2B_INQUIRY.equals(inquiryHead.getFromSource())) {
                    // 创建报价单
                    saveSomQuotesHead(newHead, 1);
                }
                inquiryHeadId = newHead.getInquiryHeadId();
            }
            // 保存附件
//            List<String> attachments = req.getAttachments();
//            if (CollectionUtil.isNotEmpty(attachments)) {
//                QueryWrapper<FileManagementEntity> wrapper = new QueryWrapper<>();
//                wrapper.eq("foreign_key", inquiryHeadId);
//                wrapper.eq("business_type", SomBizConstants.SOM_INQUIRY_FILE_ATTACHMENT);
//                fileManagementMapper.delete(wrapper);
//                for (String att : attachments) {
//                    FileManagementEntity fileManagementEntity = new FileManagementEntity();
//                    fileManagementEntity.setForeignKey(inquiryHeadId);
//                    fileManagementEntity.setFileUrl(att);
//                    fileManagementEntity.setBusinessType(SomBizConstants.SOM_INQUIRY_FILE_ATTACHMENT);
//                    fileManagementMapper.insert(fileManagementEntity);
//                }
//            }
        } catch (Exception e) {
            log.error("保存询价单出错", e);
            return ResponseUtil.failed(500, "保存询价单出错");
        }
        response.setInquiryHeadId(inquiryHeadId);
        response.setInquiryCode(req.getInquiryCode());
        // TODO 插入操作日志
//        operationLogUtil.addOperationLog(inquiryHeadId, "保存", "【保存询价单】",
//                "保存询价单", "SomSalesContractInquiryApi", "saveInquiry",
//                req.getCommandUserName(), req.getCommandUserId());
        return ResponseUtil.ok(response);
    }

    /**
     * 新增/修改询价单（商城）
     *
     * @param req
     * @return
     */
    @Override
    @Transactional
    public ResponseBO<InquirySaveResp> saveMallInquiry(InquiryMallSaveReq req) {
        InquirySaveResp response = new InquirySaveResp();
        // 转换模型
        InquiryHeadEntity inquiryHead = new InquiryHeadEntity();

        BeanUtil.copyProperties(req, inquiryHead);
        List<InquiryLineDTO> inquiryLineList = BeanUtil.convert(req.getList(), InquiryLineDTO.class);
        inquiryHead.setInquiryDate(new Date());
        String inquiryHeadId = inquiryHead.getInquiryHeadId();
        try {
            if (StringUtils.isNotBlank(inquiryHeadId)) { // 修改报价单
                // 校验报价单是否为可修改状态
                // 删除的明细和条款
                String inquiryStatus = inquiryHead.getInquiryStatus();
                if (!inquiryStatus.equals("10")) {
                    throw new BusinessException(InquiryResponseStatus.INTERFACT_SAVE_INQUIRY_STATUS_ERR);
                }
                List<String> removeLineIdList = req.getRemoveList();
                inquiryModel.modifyInquiry(inquiryHead, inquiryLineList, removeLineIdList);
            } else {    // 新增报价单
                if (StringUtils.isBlank(inquiryHead.getInquiryType())) {
                    inquiryHead.setInquiryType(GENERAL);
                }
                InquiryHeadEntity newHead = inquiryModel.createInquiry(inquiryHead, inquiryLineList);
                //新增询价单的同时要新增报价单
                saveSomQuotesHead(newHead, 1);
                inquiryHeadId = newHead.getInquiryHeadId();
                response.setInquiryCode(newHead.getInquiryCode());
            }

            //上传询价单附件
//            uploadFile(req, inquiryHeadId);

        } catch (Exception e) {
            log.error("保存询价单出错 {}", e.getMessage());
            return ResponseUtil.failed(500, "保存询价单出错" + e.getMessage());
        }
        response.setInquiryHeadId(inquiryHeadId);
        // 插入操作日志
//        operationLogUtil.addOperationLog(inquiryHeadId, "保存", "【保存询价单】",
//                "保存询价单", "SomSalesContractInquiryApi", "saveInquiry",
//                req.getCommandUserName(), req.getCommandUserId());


        return ResponseUtil.ok(response);
    }


    /**
     * 查询询价单列表（商城）
     */
    @Override
    public ResponsePageBO<InquiryHeadVO> queryMallList(InquiryListMallQueryReq req) {
        InquiryListQueryResp resp = new InquiryListQueryResp();
        PageHelper.startPage(req.getPageNum(), req.getPageSize());
        List<InquiryHeadDTO> list = somInquiryMapper.queryMallList(req);
        if (CollectionUtil.isNotEmpty(list)) {
            PageInfo<InquiryHeadDTO> pageInfo = new PageInfo<>(list);
            // 统计总条数
            resp.setTotal(pageInfo.getTotal());
            List voList = BeanUtil.convert(list, InquiryHeadVO.class);
            resp.setList(voList);
        }
        return PageResponseUtil.ok(req, resp.getTotal(), resp.getList());
    }

    /**
     * 查询询价单子表行列表
     */
    @Override
    public ResponsePageBO<InquiryLineVO> queryLineList(InquiryLineListQueryReq req) {
        InquiryLineListQueryResp resp = new InquiryLineListQueryResp();
        PageHelper.startPage(req.getPageNum(), req.getPageSize());
        InquiryHeadEntity param = new InquiryHeadEntity();
        if (ObjectUtil.isEmpty(req.getInquiryCode())) {
            throw new BusinessException(InquiryResponseStatus.INTERFACT_QUERY_INQUIRY_LINE_ERR);
        }
        param.setInquiryCode(req.getInquiryCode());
        InquiryHeadEntity head = somInquiryMapper.queryHead(param);
        if (ObjectUtil.isEmpty(head)) {
            throw new BusinessException(InquiryResponseStatus.INTERFACT_QUERY_INQUIRY_ERR);
        }
        InquiryLineEntity inquiryLine = new InquiryLineEntity();
        inquiryLine.setDeleted(0L);
        inquiryLine.setInquiryHeadId(head.getInquiryHeadId());
        List<InquiryLineEntity> inquiryLineList = somInquiryLineMapper.list(inquiryLine);
        List<InquiryLineVO> list = new ArrayList<>();
        if (CollectionUtil.isNotEmpty(inquiryLineList)) {
            PageInfo<InquiryLineEntity> pageInfo = new PageInfo<>(inquiryLineList);
            getInquiryLineDetail(inquiryLineList, head, list);
            resp.setTotal(pageInfo.getTotal());
            resp.setList(list);
        }
        return PageResponseUtil.ok(req, resp.getTotal(), list);
    }


    @Override
    public void createInquiry(DemandHeadEntity headEntity, List<DemandLineEntity> lineEntityList) {
        //根据三级类目拆单-- 生成多条询价单
        List<DispatchUnSomDetailReq> unSomDetail = new ArrayList<>();
        Map<String, List<DemandLineEntity>> categoryList = lineEntityList.stream().
                collect(Collectors.groupingBy(DemandLineEntity::getCategoryId));
        categoryList.forEach((key, value) -> {
            InquiryHeadEntity inquiryHeadEntity = new InquiryHeadEntity();
            //赋值操作
            BeanUtils.copyProperties(headEntity, inquiryHeadEntity);
            inquiryHeadEntity.setInquiryDate(DateTools.localDateTimeToDate(headEntity.getGmtCreateTime()));
            //设置询价单名称
            inquiryHeadEntity.setInquiryName(lineEntityList.get(0).getSpuName().concat("(共").concat(lineEntityList.size() + "").concat("种）"));
            inquiryHeadEntity.setInquiryStatus(InquiryConstants.InquiryStatus.TO_BE_ALLOCATED.getStatus());
            ResponseBO<SerialNumberVO> responseBo = sequenceService.nextByDefineId(OrderSequenceDefineEnum.RFQ_CODE.getId());
            inquiryHeadEntity.setInquiryCode(responseBo.getData().getSequenceCode());
            inquiryHeadEntity.setInquiryType("00".equals(headEntity.getDemandCategory()) ? "10" : "20");

            //  最晚交付日期查询批次表
            List<String> demandList = Arrays.asList(headEntity.getDemandHeadId());
            try {
                if (ObjectUtil.isNotEmpty(demandList)) {
                    List<ExpectedReceiptDateVO> dateByDemand = somInquiryMapper.selectDateByDemand(demandList);
                    inquiryHeadEntity.setExpectedDate(LocalDateUtil.convertToLocalDateTime(dateByDemand.get(0).getExpectedReceiptDate()));
                }
            } catch (Exception e) {
                e.printStackTrace();
                log.error("交货日期查询失败{}", e.getMessage());
            }
            somInquiryMapper.insert(inquiryHeadEntity);
            DispatchUnSomDetailReq unSomDetailReq = new DispatchUnSomDetailReq();
            unSomDetailReq.setCreateUserName(TokenUtil.getUserName());
            unSomDetailReq.setCustomerName(headEntity.getCustomerName());
            unSomDetailReq.setCustomerType(2);
            unSomDetailReq.setSerialCode(inquiryHeadEntity.getInquiryCode());
            unSomDetailReq.setSerialCreateTime(new Date());
            unSomDetailReq.setThirdCategoryId(key);
            unSomDetailReq.setSerialName(inquiryHeadEntity.getInquiryName());
            unSomDetail.add(unSomDetailReq);
            //赋值操作
            value.forEach(demandLine -> somInquiryLineMapper.insert(demandLine.createInquiry(inquiryHeadEntity.getInquiryHeadId())));
        });
        //自动设置采购专员
        DispatchUnSomReq dispatchUnSomReq = new DispatchUnSomReq();
        // 设置参数
        dispatchUnSomReq.setSerialHeadCode(headEntity.getDemandCode());
        dispatchUnSomReq.setPoms(unSomDetail);
        ResponseBO<DispatchUnSomResp> unSomRespResponseBO = missionService.dispatchUnSomEnquiry(dispatchUnSomReq);
        if (ObjectUtil.isNotEmpty(unSomRespResponseBO) && ObjectUtil.isNotEmpty(unSomRespResponseBO.getData())) {
            List<DispatchUnSomDetailResp> poms = unSomRespResponseBO.getData().getPoms();
            if (ObjectUtil.isNotEmpty(poms)) {
                log.info("自动设置采购专员返回值:{}", JSON.toJSONString(poms));
                poms.forEach(po -> {
                    if (ObjectUtil.isNotEmpty(po.getSerialCode())) {
                        InquiryHeadEntity inquiry = somInquiryMapper.selectOne(new LambdaQueryWrapper<InquiryHeadEntity>()
                                .eq(InquiryHeadEntity::getInquiryCode, po.getSerialCode())
                                .last("limit 1"));
                        if (ObjectUtil.isNotEmpty(po.getDealStaff())) {
                            inquiry.setOfficeStaffName(po.getDealStaff().getStaffName());
                            inquiry.setOfficeStaffId(po.getDealStaff().getStaffId());
                            somInquiryMapper.updateById(inquiry);
//                            somInquiryMapper.update(inquiry, new LambdaUpdateWrapper<InquiryHeadEntity>().eq(InquiryHeadEntity::getInquiryCode, po.getSerialCode()));
                        }
                    }
                });
            }
        }
    }

    private void getInquiryLineDetail(List<InquiryLineEntity> inquiryLineLists, InquiryHeadEntity somInquiryHead, List<InquiryLineVO> list) {
        for (InquiryLineEntity line : inquiryLineLists) {
            InquiryLineVO vo = new InquiryLineVO();
            BeanUtil.copyProperties(line, vo);
            if (ObjectUtil.isNotEmpty(somInquiryHead.getInquiryType())
                    && (somInquiryHead.getInquiryType()
                    .equals(STEEL))) {
                InquiryLineGmEntity paramGm = new InquiryLineGmEntity();
                paramGm.setInquiryLineId(line.getInquiryLineId());
                InquiryLineGmEntity gm = somInquiryLineGmMapper.query(paramGm);
                BeanUtil.copyProperties(gm, vo);
            }
            list.add(vo);
        }
    }

    /**
     * 批量提交询价单
     */
    @Override
    public ResponseBO<InquirySubmitResp> submit(InquirySubmitReq req) {
        InquirySubmitResp resp = new InquirySubmitResp();
        List<String> headIdList = req.getInquiryHeadIdList();
        if (ObjectUtil.isNotEmpty(headIdList) && headIdList.size() > 0) {
            List<InquiryHeadEntity> inquiryHeadList = somInquiryMapper.queryListStatusWaitSubmit(headIdList);
            if (headIdList.size() == inquiryHeadList.size()) {
                inquiryHeadList.forEach(data -> {
                    if (ObjectUtil.isNotEmpty(data.getInquiryHeadId())) {
                        InquiryHeadEntity somInquiryHead = new InquiryHeadEntity();
                        somInquiryHead.setInquiryHeadId(data.getInquiryHeadId());
                        somInquiryHead.setInquiryStatus(BizStatus.INQUIRY_STATUS.WAIT_QUOTE);
//                        DomainUtils.setUser(req,somInquiryHead);
//                        TlerpPropertiesUtil.setCreatePropertiesBeNull(somInquiryHead);
//                        inquiryModel.update(somInquiryHead);
                        somInquiryMapper.updateById(somInquiryHead);

                        if (B2B_INQUIRY.equals(data.getFromSource())) {
                            saveSomQuotesHead(data, 0);
                        }
                        // TODO 插入操作日志
//                        operationLogUtil.addOperationLog(data.getInquiryHeadId(), "更新", "【批量提交询价单】",
//                                "批量提交询价单", "SomSalesContractInquiryApiImpl", "submit",
//                                req.getCommandUserName(), req.getCommandUserId());
                    }
                });
            } else {
                throw new BusinessException(InquiryResponseStatus.INTERFACT_QUERY_NOT_INIT);
            }
        }
        return ResponseUtil.ok(resp);
    }

    /**
     * 批量撤回询价单
     */
    @Override
    public ResponseBO<InquiryRevokeResp> revoke(InquiryRevokeReq req) {
        InquiryRevokeResp resp = new InquiryRevokeResp();
        List<String> headIdList = req.getInquiryHeadIdList();
        if (ObjectUtil.isNotEmpty(headIdList) && headIdList.size() > 0) {
            List<InquiryHeadEntity> inquiryHeadList = somInquiryMapper.queryListStatusWaitQuote(headIdList);
            if (headIdList.size() == inquiryHeadList.size()) {
                inquiryHeadList.forEach(data -> {
                    if (ObjectUtil.isNotEmpty(data.getInquiryHeadId())) {
                        InquiryHeadEntity somInquiryHead = new InquiryHeadEntity();
                        somInquiryHead.setInquiryHeadId(data.getInquiryHeadId());
                        somInquiryHead.setInquiryStatus(BizStatus.INQUIRY_STATUS.INIT);
//                        DomainUtils.setUser(req, somInquiryHead);
                        inquiryModel.update(somInquiryHead);
                        // TODO 插入操作日志
//                        operationLogUtil.addOperationLog(data.getInquiryHeadId(), "更新", "【批量撤回询价单】",
//                                "批量撤回询价单", "SomSalesContractInquiryApiImpl", "revoke",
//                                req.getCommandUserName(), req.getCommandUserId());
                    }
                });
            } else {
                throw new BusinessException(InquiryResponseStatus.INTERFACT_QUERY_NOT_WAIT_QUOTE);
            }
        }
        return ResponseUtil.ok(resp);
    }

    /**
     * 批量作废询价单
     */
    @Override
    public ResponseBO<InquiryInvalidResp> invalid(InquiryInvalidReq req) {
        InquiryInvalidResp resp = new InquiryInvalidResp();
        List<String> headIdList = req.getInquiryHeadIdList();
        if (ObjectUtil.isNotEmpty(headIdList) && headIdList.size() > 0) {
            List<InquiryHeadEntity> inquiryHeadList = somInquiryMapper.queryListStatusWaitSubmit(headIdList);
            if (headIdList.size() == inquiryHeadList.size()) {
                inquiryHeadList.forEach(data -> {
                    if (ObjectUtil.isNotEmpty(data.getInquiryHeadId())) {
                        InquiryHeadEntity somInquiryHead = new InquiryHeadEntity();
                        somInquiryHead.setInquiryHeadId(data.getInquiryHeadId());
                        somInquiryHead.setInquiryStatus(BizStatus.INQUIRY_STATUS.CANCEL);
                        inquiryModel.update(somInquiryHead);


                        // TODO 插入操作日志
//                        operationLogUtil.addOperationLog(data.getInquiryHeadId(), "更新", "【批量作废询价单】",
//                                "批量作废询价单", "SalesContractInquiryApiImpl", "invalid",
//                                req.getCommandUserName(), req.getCommandUserId());
                    }
                });
                // 作废报价单
                QuotesHeadEntity quotesHeadEntity = new QuotesHeadEntity();
                quotesHeadEntity.setQuotesStatus(BizStatus.QUOTES_STATUS.CANCELED);
                QueryWrapper<QuotesHeadEntity> wrapper = new QueryWrapper();
                wrapper.in("INQUIRY_HEAD_ID", headIdList);
                quotesHeadMapper.update(quotesHeadEntity, wrapper);
            } else {
                throw new BusinessException(InquiryResponseStatus.INTERFACT_QUERY_NOT_INIT);
            }
        }
        return ResponseUtil.ok(resp);
    }

    /**
     * 询价单导出（Excel格式）
     */
    @Override
    public ResponseBO<ExportFileVO> exportExcel(InquiryDetailQueryReq req) {
        ExportFileVO response = new ExportFileVO();
        InquiryHeadEntity param = new InquiryHeadEntity();
        BeanUtil.copyProperties(req, param);
        List<InquiryHeadEntity> somInquiryHeadList = somInquiryMapper.query(param);
        InquiryHeadEntity somInquiryHead = somInquiryHeadList.get(0);
        InquiryLineEntity somInquiryLine = new InquiryLineEntity();
        somInquiryLine.setInquiryHeadId(somInquiryHead.getInquiryHeadId());
        somInquiryLine.setDeleted(0L);
        List<InquiryLineEntity> inquiryLineList = somInquiryLineMapper.list(somInquiryLine);
        if (CollectionUtil.isNotEmpty(inquiryLineList) && ObjectUtil.isNotEmpty(somInquiryHead)) {
            Workbook workbook = excelInquiryUtil.export(inquiryLineList, somInquiryHead);
            byte[] bytes = POIUtil.createWorkbook(workbook);
            String date = DateUtil.format(new Date(), DatePattern.PURE_DATETIME_PATTERN);
            String fileName = "询价单-" + date;

            // TODO 文件上传
//            List<FileUploadBean> fileUploadBeanList = new ArrayList<>();
//            FileUploadBean fileUploadBean = new FileUploadBean();
//            fileUploadBean.setFileInputStream(bytes);
//            fileUploadBean.setFileName(fileName);
//            fileUploadBean.setFileType("xls");
//            fileUploadBeanList.add(fileUploadBean);
//            List<FileUploadResult> fileUploadResults = FileUpDownUtil.uploadAttachment(fileUploadBeanList);
            String filePath = ""; // fileUploadResults.get(0).getFilePath();

            response.setFilePath(filePath);
            response.setFileName(fileName + ".xls");
            response.setBytes(bytes);
            // TODO 插入操作日志
//            operationLogUtil.addOperationLog(req.getInquiryHeadId() + "", "导出", "【导出EXCEL】", "导出EXCEL",
//                    "SomSalesContractInquiryApi", "exportExcel", req.getCommandUserName(),
//                    req.getCommandUserId());
        } else {
            throw new BusinessException(INTERFACT_QUERY_EXPORT_ERR);
        }
        return ResponseUtil.ok(response);
    }

    /**
     * 询价单导出（PDF格式）
     */
    @Override
    public ResponseBO<ExportFileVO> exportPDF(InquiryDetailQueryReq req) {
        ExportFileVO response = new ExportFileVO();
        InquiryHeadEntity param = new InquiryHeadEntity();
        BeanUtil.copyProperties(req, param);
        List<InquiryHeadEntity> somInquiryHeadList = somInquiryMapper.query(param);
        InquiryHeadEntity somInquiryHead = somInquiryHeadList.get(0);
        InquiryLineEntity somInquiryLine = new InquiryLineEntity();
        somInquiryLine.setInquiryHeadId(somInquiryHead.getInquiryHeadId());
        somInquiryLine.setDeleted(0L);
        List<InquiryLineEntity> inquiryLineList = somInquiryLineMapper.list(somInquiryLine);
        if (CollectionUtil.isNotEmpty(inquiryLineList) && ObjectUtil.isNotEmpty(somInquiryHead)) {
            byte[] bytes = pdfInquiryUtil.exportPDF(inquiryLineList, somInquiryHead, TokenUtil.getUserName());
            String date = DateUtil.format(new Date(), DatePattern.PURE_DATETIME_PATTERN);
            String fileName = "询价单-" + date;

            // TODO 文件上传
//            List<FileUploadBean> fileUploadBeanList = new ArrayList<>();
//            FileUploadBean fileUploadBean = new FileUploadBean();
//            fileUploadBean.setFileInputStream(bytes);
//            fileUploadBean.setFileName(fileName);
//            fileUploadBean.setFileType("pdf");
//            fileUploadBeanList.add(fileUploadBean);
//            List<FileUploadResult> fileUploadResults = FileUpDownUtil.uploadAttachment(fileUploadBeanList);
//            String filePath = fileUploadResults.get(0).getFilePath();
            String filePath = "";
            response.setFilePath(filePath);
            response.setFileName(fileName + ".pdf");
            response.setBytes(bytes);
            //插入操作日志
//            operationLogUtil.addOperationLog(req.getInquiryHeadId() + "", "导出", "【导出PDF】", "导出PDF",
//                    "SomSalesContractInquiryApi", "exportPDF", req.getCommandUserName(),
//                    req.getCommandUserId());
        } else {
            throw new BusinessException(INTERFACT_QUERY_EXPORT_ERR);
        }
        return ResponseUtil.ok(response);
    }

    /**
     * 创建报价单
     *
     * @param inquiryHead
     */
    private void saveSomQuotesHead(InquiryHeadEntity inquiryHead, int syncFlag) {
        QuotesHeadEntity quotesHeadEntity = new QuotesHeadEntity();
        BeanUtil.copyProperties(inquiryHead, quotesHeadEntity);
        quotesHeadEntity.setRemark("");
        quotesHeadEntity.setInquiryHeadId(inquiryHead.getInquiryHeadId());
        quotesHeadEntity.setInquiryCreateUserId(inquiryHead.getCreatorId());
        quotesHeadEntity.setInquiryCreateUserName(inquiryHead.getCreatorName());
        quotesHeadEntity.setInquiryRemark(inquiryHead.getRemark());
        quotesHeadEntity.setQuotesType(inquiryHead.getInquiryType());
        quotesHeadEntity.setQuotesCode(inquiryHead.getInquiryCode());
        quotesHeadEntity.setSyncFlag(syncFlag);
        if (BizStatus.QUOTES_FROM_SOURCE.MALL_INQUIRY.equals(inquiryHead.getFromSource()) ||
                BizStatus.QUOTES_FROM_SOURCE.MALL_PRE_ORDER.equals(inquiryHead.getFromSource()) ||
                BizStatus.QUOTES_FROM_SOURCE.B2B_INQUIRY.equals(inquiryHead.getFromSource()) ||
                BizStatus.QUOTES_FROM_SOURCE.SUPPLY_LIST.equals(inquiryHead.getFromSource())
        ) {
            quotesHeadEntity.setQuotesStatus(BizStatus.QUOTES_STATUS.WAIT_QUOTE);
        } else {
            quotesHeadEntity.setQuotesStatus(BizStatus.QUOTES_STATUS.WAIT_SUBMIT);
        }
        // TODO 根据员工id查询员工值
//        HrPostStaffView somHrPostStaffView = queryPostByStaff(inquiryHead.getStaffId());
//        if (ObjectUtil.isNotEmpty(somHrPostStaffView)) {
//            quotesHeadEntity.setDepartmentId(somHrPostStaffView.getOrganizationId());
//            quotesHeadEntity.setDepartmentCode(somHrPostStaffView.getOrganizationCode());
//            quotesHeadEntity.setDepartmentName(somHrPostStaffView.getOrganizationName());
//        }
        log.info("quotesHeadEntity=" + JSON.toJSONString(quotesHeadEntity));
        somQuotesHeadModel.saveSomQuotesHead(quotesHeadEntity);
    }

    /**
     * @Author huangjinghua
     * @Description //需方saas询价列表
     * @Date 8:57 上午 2020/7/10
     * @Param [req]
     * @return com.vd.canary.core.bo.ResponsePageBO<com.vd.canary.obmp.order.api.response.vo.order.InquirySaasHeadVO>
     **/
//    @Override
//    public ResponsePageBO<DemandSaasHeadVO> querySaasList(DemandSaasInfoReq req) {
//        List<DemandSaasHeadVO> list = new ArrayList<>();
//        PageHelper.startPage(req.getPageNum(), req.getPageSize());
//        list = demandHeadMapper.selectSaasList(req);
//        PageInfo<DemandSaasHeadVO> page = new PageInfo<>(list);
//        if (list==null || list.size()==0) {
//            return PageResponseUtil.ok(req, page.getTotal(), page.getList(), DemandSaasHeadVO.class);
//        }
//        // 需求单状态 10-待提交 20-待受理 21-待审核 22-退回需方 30-待报价 40-已报价 50-退回 00-已关闭
//        // 需方saas显示询价单状态 10-待提交 20-待报价 30-已报价 40-已报价 50-已关闭
//        for (DemandSaasHeadVO vo : list) {
//            switch (vo.getDemandStatus()) {
//                case "10":
//                    vo.setDemandStatus("10");
//                    break;//待提交
//                case "20":
//                    vo.setDemandStatus("20");
//                    break;//待报价
//                case "21":
//                    vo.setDemandStatus("20");
//                    break;//待报价
//                case "30":
//                    vo.setDemandStatus("20");
//                    break;//待报价
//                case "50":
//                    vo.setDemandStatus("20");
//                    break;//待报价
//                case "40":
//                    vo.setDemandStatus("30");
//                    break;//已报价
//                case "22":
//                    vo.setDemandStatus("40");
//                    break;//已报价
//                case "00":
//                    vo.setDemandStatus("50");
//                    break;//已报价
//                default:
//                    vo.setDemandStatus("50");
//            }
//            List<ShipmentPlanBatchEntity> shipmentPlanBatchEntities = shipmentPlanBatchMapper.selectList(new LambdaQueryWrapper<ShipmentPlanBatchEntity>()
//                    .eq(ShipmentPlanBatchEntity::getDemandHeadId, vo.getDemandHeadId())
//                    .orderByDesc(ShipmentPlanBatchEntity::getExpectedReceiptDate));
//            if (shipmentPlanBatchEntities!=null && shipmentPlanBatchEntities.size()>0) {
//                vo.setExpectedReceiptDate(shipmentPlanBatchEntities.get(0).getExpectedReceiptDate());
//            }
//        }
//        return PageResponseUtil.ok(req, page.getTotal(), page.getList(), DemandSaasHeadVO.class);
//    }

    /**
     * @Description 需方saas获取待报价和已报价的数量
     * @Param
     * @Return
     * @Author huangjinghua
     * @Date
     */
    @Override
    public ResponseBO<InquiryCountResp> count(InquiryCountReq req) {
        InquiryCountResp inquiryCountResp = new InquiryCountResp();
        // 待报价数量
        List<String> quotedCountStatus = Arrays.asList(DemandConstants.DemandStatus.BE_ACCEPT.getCode(),
                DemandConstants.DemandStatus.BE_REVIEWED.getCode(),
                DemandConstants.DemandStatus.BE_QUOTED.getCode(),
                DemandConstants.DemandStatus.ROLLBACK.getCode(),
                DemandConstants.DemandStatus.DISPATCH_FAILED.getCode());
        Wrapper<DemandHeadEntity> eq = new QueryWrapper<DemandHeadEntity>().lambda()
                .eq(DemandHeadEntity::getCustomerId, req.getCompanyId())
                .in(DemandHeadEntity::getDemandStatus, quotedCountStatus);
        Integer quotedCount = demandHeadMapper.selectCount(eq);

        // 已报价数量
        List<String> quotedPriceCountStatus = Arrays.asList(
                DemandConstants.DemandStatus.QUOTED_PRICE.getCode());
        Wrapper<DemandHeadEntity> eq1 = new QueryWrapper<DemandHeadEntity>().lambda()
                .eq(DemandHeadEntity::getCustomerId, req.getCompanyId())
                .in(DemandHeadEntity::getDemandStatus,quotedPriceCountStatus);
        Integer quotedPriceCount = demandHeadMapper.selectCount(eq1);

        inquiryCountResp.setQuotedCount(quotedCount);
        inquiryCountResp.setQuotedPriceCount(quotedPriceCount);
        return ResponseUtil.ok(inquiryCountResp);
    }

    /**
     * 提交核价询价单 只更改状态（关闭关联的待报价供方询报价单、系统自动分配核价专员）
     *
     * @param req
     * @return
     */
    @Override
    @Transactional
    public ResponseBO submitNuclearPrice(InquirySubmitNuclearReq req) {

        //校验是否存在商品有未报价的 inquiry_line id
        //更改 询价单 报价单 状态变成待核价
        InquiryHeadEntity inquiryHeadEntity = somInquiryMapper.selectById(req.getInquiryHeadId());
        if (ObjectUtil.isEmpty(inquiryHeadEntity)) {
            throw new BusinessException(InquiryResponseStatus.INTERFACT_QUERY_INQUIRY_ERR);
        }
        //提交核价至少存在一个一个供方报价
        SupplyInquiryQuotesInfoReq supplyInquiryQuotesInfoReq = new SupplyInquiryQuotesInfoReq();
        supplyInquiryQuotesInfoReq.setInquiryHeadId(req.getInquiryHeadId());
        ResponseBO<SupplyInquiryQuotesInfoResp> supplyInquiryQuotesInfoRespResponseBO = supplyInquiryService.supplierQuoteInfo(supplyInquiryQuotesInfoReq);
        log.info("需方询报价核价校是否存在供方已报价...,返回参数supplyInquiryQuotesInfoRespResponseBO:{}", JSON.toJSONString(supplyInquiryQuotesInfoRespResponseBO));

        SupplyInquiryQuotesInfoResp infoRespResponseBOData = supplyInquiryQuotesInfoRespResponseBO.getData();
        if (infoRespResponseBOData == null || infoRespResponseBOData.getSupplyQuotesMap() == null) {
            throw new BusinessException(InquiryResponseStatus.INTERFACT_UPDATE_STATUS_ERR);
        }
        Map<String, List<SupplyInquiryQuotesInfoVO>> supplyQuotesMap = infoRespResponseBOData.getSupplyQuotesMap();
        List<InquiryLineEntity> inquiryLineEntities = somInquiryLineMapper.selectList(new QueryWrapper<InquiryLineEntity>().lambda().eq(InquiryLineEntity::getInquiryHeadId, req.getInquiryHeadId()));
        inquiryLineEntities.forEach(i -> {
            if (!supplyQuotesMap.containsKey(i.getInquiryLineId())) {
                throw new BusinessException(InquiryResponseStatus.INTERFACT_UPDATE_STATUS_ERR);
            }
            List<SupplyInquiryQuotesInfoVO> supplyInquiryQuotesInfoVOS = supplyQuotesMap.get(i.getInquiryLineId());
            if (CollectionUtil.isEmpty(supplyInquiryQuotesInfoVOS)) {
                throw new BusinessException(InquiryResponseStatus.INTERFACT_UPDATE_STATUS_ERR);
            }
            List<SupplyInquiryQuotesInfoVO> inquiryQuotesInfoVOS = supplyInquiryQuotesInfoVOS.stream().filter(s -> SupplyInquiryConstants.QuotesStatus.QUOTED.getKey().equals(s.getQuotesStatus())).collect(Collectors.toList());
            if (CollectionUtil.isEmpty(inquiryQuotesInfoVOS)) {
                throw new BusinessException(InquiryResponseStatus.INTERFACT_UPDATE_STATUS_ERR);
            }
        });
        // 待报价 核价退回 报价失效可以编辑
        List<String> statusList = Arrays.asList(
                InquiryConstants.InquiryStatus.PENDING_QUOTATION.getStatus(),
                InquiryConstants.InquiryStatus.REVIEW_RETURN.getStatus(),
                InquiryConstants.InquiryStatus.INVALID.getStatus()
        );
        if (!statusList.contains(inquiryHeadEntity.getInquiryStatus())) {
            throw new BusinessException(InquiryResponseStatus.INTERFACT_QUERY_STATUS_ERR);
        }
        inquiryHeadEntity.setInquiryStatus(InquiryConstants.InquiryStatus.PENDING_PRICE.getStatus());
        somInquiryMapper.updateById(inquiryHeadEntity);
        //同步更改报价单
        QuotesHeadEntity quotesHeadEntity = somQuotesHeadMapper.selectOne(new LambdaQueryWrapper<QuotesHeadEntity>()
                .eq(QuotesHeadEntity::getInquiryHeadId, req.getInquiryHeadId()));
        if (ObjectUtil.isEmpty(quotesHeadEntity)) {
            throw new BusinessException(InquiryResponseStatus.INTERFACT_QUERY_INQUIRY_ERR);
        }
        quotesHeadEntity.setQuotesStatus(InquiryConstants.InquiryStatus.PENDING_PRICE.getStatus());
        somQuotesHeadMapper.updateById(quotesHeadEntity);
        // 关闭其他供方待报价单子
        SupplyInquiryShutReq supplyInquiryShutReq = new SupplyInquiryShutReq();
        supplyInquiryShutReq.setInquiryHeadId(req.getInquiryHeadId());
        supplyInquiryShutReq.setReason("其他");
        supplyInquiryShutReq.setType(2);
        supplyInquiryService.shutSupplyInquiry(supplyInquiryShutReq);

        // 分配核价专员
        startIqAuditor(inquiryHeadEntity);

        return ResponseUtil.ok();
    }

    private void startIqAuditor(InquiryHeadEntity inquiryHeadEntity) {
        DispatchFinanceReq dispatchFinanceReq = new DispatchFinanceReq();
        dispatchFinanceReq.setCreateUserName(inquiryHeadEntity.getCreatorName());
        dispatchFinanceReq.setCustomerName(inquiryHeadEntity.getCustomerName());
        dispatchFinanceReq.setCustomerType(2);
        dispatchFinanceReq.setSerialCreateTime(DateTools.localDateTimeToDate(inquiryHeadEntity.getGmtCreateTime()));
        dispatchFinanceReq.setSerialCode(inquiryHeadEntity.getInquiryCode());
        dispatchFinanceReq.setSerialName(inquiryHeadEntity.getInquiryName());
        missionService.dispatchUnQuoteFinance(dispatchFinanceReq);
    }

    /**
     * 提交核价询价单之后可以 -》退回 只更改状态
     *
     * @param req
     * @return
     */
    @Override
    @Transactional
    public ResponseBO refusedNuclearPrice(InquirySubmitNuclearReq req) {
        //更改 待核价->核价退回
        InquiryHeadEntity inquiryHeadEntity = somInquiryMapper.selectById(req.getInquiryHeadId());
        if (ObjectUtil.isEmpty(inquiryHeadEntity)) {
            throw new BusinessException(InquiryResponseStatus.INTERFACT_QUERY_INQUIRY_ERR);
        }
        // 待报价 核价退回 报价失效可以编辑
        if (!InquiryConstants.InquiryStatus.PENDING_PRICE.getStatus().equals(inquiryHeadEntity.getInquiryStatus())) {
            throw new BusinessException(InquiryResponseStatus.INTERFACT_QUERY_STATUS_ERR);
        }
        inquiryHeadEntity.setInquiryStatus(InquiryConstants.InquiryStatus.REVIEW_RETURN.getStatus());
        if (StringUtils.isNotBlank(req.getRejectReason())) {
            inquiryHeadEntity.setRejectReason(req.getRejectReason());
        }
        somInquiryMapper.updateById(inquiryHeadEntity);
        //同步更改报价单
        QuotesHeadEntity quotesHeadEntity = somQuotesHeadMapper.selectOne(new LambdaQueryWrapper<QuotesHeadEntity>()
                .eq(QuotesHeadEntity::getInquiryHeadId, req.getInquiryHeadId()));
        if (ObjectUtil.isEmpty(quotesHeadEntity)) {
            throw new BusinessException(InquiryResponseStatus.INTERFACT_QUERY_INQUIRY_ERR);
        }
        quotesHeadEntity.setQuotesStatus(InquiryConstants.InquiryStatus.REVIEW_RETURN.getStatus());
        somQuotesHeadMapper.updateById(quotesHeadEntity);

        return ResponseUtil.ok();

    }

    /**
     * 提交核价询价单之后可以 -》加价 （关联供方、销售价格、当需求单对应的所有需方询报价单都核价完成，系统自动创建销售订单）
     *
     * @param req
     * @return
     */
    @Override
    @Transactional
    public ResponseBO markupNuclearPrice(InquirySubmitNuclearReq req) {
        InquiryHeadEntity inquiryHeadEntity = somInquiryMapper.selectById(req.getInquiryHeadId());
        //数据校验
        if (InquiryConstants.InquiryUpdateType.SUBMIT.getStatus().equals(req.getUpdateType())) {
            if (CollectionUtil.isEmpty(req.getInquirySubmitNuclearDetailReqs())) {
                throw new BusinessException(InquiryResponseStatus.INTERFACT_UPDATE_STATUS_ERR);
            }
            req.getInquirySubmitNuclearDetailReqs().forEach(i -> {
                if (StringUtils.isBlank(i.getSupplyId()) || i.getSalePrice() == null) {
                    throw new BusinessException(InquiryResponseStatus.INTERFACT_UPDATE_STATUS_ERR);
                }
            });
        }
        // 更新商品行 关联供方和价格
        List<InquirySubmitNuclearDetailReq> inquirySubmitNuclearDetailReqs = req.getInquirySubmitNuclearDetailReqs();
        //todo 更改变量名称
        inquirySubmitNuclearDetailReqs.forEach(inquirySubmitNuclearDetailReq -> {
//            i.setTaxCode(i.getTax());
//            i.setTaxCodeName(i.getTaxName());
//            i.setTaxCodeType(i.getTaxCodeType());
//              物流费用  先写死
//            i.setLogisticsPrice(BigDecimal.TEN);

            inquirySubmitNuclearDetailReq.setSupplierCode(inquirySubmitNuclearDetailReq.getSupplyCode());
            inquirySubmitNuclearDetailReq.setSupplierId(inquirySubmitNuclearDetailReq.getSupplyId());
            inquirySubmitNuclearDetailReq.setSupplierName(inquirySubmitNuclearDetailReq.getSupplyName());
            inquirySubmitNuclearDetailReq.setExpectedReceiptDate(inquirySubmitNuclearDetailReq.getExpectedDate());
        });
        List<QuotesLineEntity> quotesLineEntityList = BeanUtil.convert(inquirySubmitNuclearDetailReqs, QuotesLineEntity.class);
        quotesLineService.updateBatchById(quotesLineEntityList, quotesLineEntityList.size());
        // 更改位置 在update之前
        if (InquiryConstants.InquiryUpdateType.SUBMIT.getStatus().equals(req.getUpdateType())) {
            //更改 待核价->核价退回
//            InquiryHeadEntity inquiryHeadEntity = somInquiryMapper.selectById(req.getInquiryHeadId());
            if (ObjectUtil.isEmpty(inquiryHeadEntity)) {
                throw new BusinessException(InquiryResponseStatus.INTERFACT_QUERY_INQUIRY_ERR);
            }
            // 待报价 核价退回 报价失效可以编辑
            if (!InquiryConstants.InquiryStatus.PENDING_PRICE.getStatus().equals(inquiryHeadEntity.getInquiryStatus())) {
                throw new BusinessException(InquiryResponseStatus.INTERFACT_QUERY_STATUS_ERR);
            }
            if (CollectionUtil.isEmpty(req.getInquirySubmitNuclearDetailReqs())) {
                throw new BusinessException(InquiryResponseStatus.INTERFACT_QUERY_INQUIRY_ERR);
            }

            inquiryHeadEntity.setInquiryStatus(InquiryConstants.InquiryStatus.REVIEW_PASSED.getStatus());
            somInquiryMapper.updateById(inquiryHeadEntity);
            //同步更改报价单
            QuotesHeadEntity quotesHeadEntity = somQuotesHeadMapper.selectOne(new LambdaQueryWrapper<QuotesHeadEntity>()
                    .eq(QuotesHeadEntity::getInquiryHeadId, req.getInquiryHeadId()));
            if (ObjectUtil.isEmpty(quotesHeadEntity)) {
                throw new BusinessException(InquiryResponseStatus.INTERFACT_QUERY_INQUIRY_ERR);
            }
            quotesHeadEntity.setQuotesStatus(InquiryConstants.InquiryStatus.REVIEW_PASSED.getStatus());
            somQuotesHeadMapper.updateById(quotesHeadEntity);
            //查询是否所以询报价都完成
                insertInquirySomSaleOrder(req.getInquiryHeadId(), inquiryHeadEntity.getDemandHeadId());
        }

        return ResponseUtil.ok();

    }

    private void insertInquirySomSaleOrder(String inquiryHeadId, String demandHeadId) {
        log.info("询报价生成销售订单开始-》insertInquirySomSaleOrder");
        ResponseBO<BooleanResp> booleanRespResponseBO = demandHeadService.updateDemandStatus(inquiryHeadId);
        if (booleanRespResponseBO.getSuccess() && booleanRespResponseBO.getData() != null && booleanRespResponseBO.getData().getFlag()) {
            log.info("询报价生成销售订单-》insertInquirySomSaleOrder，booleanRespResponseBO：{}",JSONUtil.toJSON(booleanRespResponseBO));
            // 完成之后生成销售订单
            DemandHeadEntity demandHeadEntity = demandHeadMapper.selectById(demandHeadId);
            SomSalesCreateInquiryReq createInquiryReq = new SomSalesCreateInquiryReq();
            BeanUtil.copyProperties(demandHeadEntity, createInquiryReq);
            createInquiryReq.setDemandId(demandHeadId);
            createInquiryReq.setDemandCode(demandHeadEntity.getDemandCode());
            createInquiryReq.setDemandCreateUserId(demandHeadEntity.getCreatorId());
            createInquiryReq.setContractSignType(demandHeadEntity.getDemandSignType());
            createInquiryReq.setStaffId(demandHeadEntity.getDemandStaffId());
            createInquiryReq.setStaffCode(demandHeadEntity.getDemandStaffCode());
            createInquiryReq.setStaffName(demandHeadEntity.getDemandStaffName());
            createInquiryReq.setContractName(demandHeadEntity.getDemandName());
            createInquiryReq.setContractType(SomSalesContractHeadConstant.ContractType.SUPPLY.key);
            createInquiryReq.setCustomerConsigneeId(demandHeadEntity.getCustomerConsigneetId());
            createInquiryReq.setCustomerConsigneeName(demandHeadEntity.getCustomerConsigneetName());
            createInquiryReq.setCustomerConsigneePhone(demandHeadEntity.getCustomerConsigneetPhone());
            createInquiryReq.setOrderCategory("00".equals(demandHeadEntity.getDemandCategory()) ? "2" : "3");
            createInquiryReq.setContractBeginDate(demandHeadEntity.getGmtCreateTime());
            // 暂定写死币种
            createInquiryReq.setCurrencyCode("CNY");
            List<InquiryHeadEntity> inquiryHeadEntities = somInquiryMapper.selectList(
                    new LambdaQueryWrapper<InquiryHeadEntity>()
                            .eq(InquiryHeadEntity::getDemandHeadId, demandHeadId)
                            .ne(InquiryHeadEntity::getInquiryStatus, InquiryConstants.InquiryStatus.CLOSE.getStatus()));
            log.info("询报价生成销售订单-》insertInquirySomSaleOrder，inquiryHeadEntities：{}",JSONUtil.toJSON(inquiryHeadEntities));

            List<String> inquiryHeadIdList = inquiryHeadEntities.stream().map(InquiryHeadEntity::getInquiryHeadId).collect(Collectors.toList());
            List<QuotesHeadEntity> quotesHeadEntities = quotesHeadMapper.selectList(
                    new LambdaQueryWrapper<QuotesHeadEntity>().in(QuotesHeadEntity::getInquiryHeadId, inquiryHeadIdList));
            log.info("询报价生成销售订单-》insertInquirySomSaleOrder，quotesHeadEntities：{}",JSONUtil.toJSON(quotesHeadEntities));

            List<String> quotesHeadIdList = quotesHeadEntities.stream().map(QuotesHeadEntity::getQuotesHeadId).collect(Collectors.toList());
            List<QuotesLineEntity> quotesLineEntities = quotesLineMapper.selectList(
                    new LambdaQueryWrapper<QuotesLineEntity>().in(QuotesLineEntity::getQuotesHeadId, quotesHeadIdList));
            log.info("询报价生成销售订单-》insertInquirySomSaleOrder，quotesLineEntities：{}",JSONUtil.toJSON(quotesLineEntities));

            List<SomSalesCreateInquiryLineReq> goodsList = new ArrayList<>();
            quotesLineEntities.forEach(quotes -> {
                SomSalesCreateInquiryLineReq goods = BeanUtil.convert(quotes, SomSalesCreateInquiryLineReq.class);
                //能否供货：00-待确认,10-可供货,20-不可供货  --默认都是10
                goods.setCanSupply("10");
                goods.setDeliveryCost(quotes.getLogisticsPrice());
                goods.setOriginalPrice(quotes.getOriginalSalesPrice());
                goods.setDeliveryCost(quotes.getLogisticsPrice());
                goods.setSaleQuantity((quotes.getQuantity()));
                goods.setPurchasePrice(quotes.getPurPrice());
                goods.setTaxCode(quotes.getTaxCode().toString());
                goods.setUnitType(quotes.getUnitName());
                goods.setSkuSpec(quotes.getItemSpecDesc());
                goodsList.add(goods);
            });
            log.info("询报价生成销售订单-》insertInquirySomSaleOrder，goodsList：{}",JSONUtil.toJSON(goodsList));
            createInquiryReq.setGoodsList(goodsList);
            somSalesCreateService.createSalesFromInquiry(createInquiryReq);
        }
    }


    /**
     * 上传询价单附件
     *
     * @param req
     * @param inquiryHeadId
     */
//    private void uploadFile(InquiryMallSaveReq req, String inquiryHeadId) {
//        if (ObjectUtils.isNotEmpty(req.getFileManagementBo())) {
//            FileManagementBo fileManagementBo = req.getFileManagementBo();
//            FileManagementEntity fileManagementEntity = new FileManagementEntity();
//            fileManagementEntity.setFileName(fileManagementBo.getFileName());
//            fileManagementEntity.setForeignKey(req.getInquiryCode());
//            fileManagementEntity.setFileType(fileManagementBo.getFileType());
//            fileManagementEntity.setFileUrl(fileManagementBo.getFileUrl());
//            fileManagementEntity.setBusinessType(SomBizConstants.INQUIRY_FILE_ATTACHMENT);
//            fileManagementMapper.insert(fileManagementEntity);
//        }
//    }

    /**
     * 查询询价单列表
     */
    // TODO  WANGPAN 20200709
    @Override
    public ResponsePageBO<InquiryHeadVO> queryList(InquiryListQueryReq req) {
        PageHelper.startPage(req.getPageNum(), req.getPageSize());
        List<InquiryHeadVO> list = somInquiryMapper.queryList(req);
//        //  最晚交付日期查询批次表
//        List<String> demandList = list.stream().map(InquiryHeadVO::getDemandHeadId).collect(Collectors.toList());
//        try {
//            if (ObjectUtil.isNotEmpty(demandList)) {
//                List<ExpectedReceiptDateVO> dateByDemand = somInquiryMapper.selectDateByDemand(demandList);
//                Map<String, Date> collect = dateByDemand.stream().collect(Collectors.toMap(ExpectedReceiptDateVO::getDemandHeadId, ExpectedReceiptDateVO::getExpectedReceiptDate));
//                list.stream().forEach(vo -> vo.setExpectedDate(collect.get(vo.getDemandHeadId())));
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            log.error("交货日期查询失败{}", e.getMessage());
//        }
        PageInfo<InquiryHeadVO> pageInfo = new PageInfo<>(list);
        return PageResponseUtil.ok(req, pageInfo.getTotal(), pageInfo.getList());
    }

    /**
     * 查询询价单详情
     */
    @Override
    public ResponseBO<InquiryDetailQueryResp> queryDetail(InquiryDetailQueryReq req) {
        //询价单head表信息
        InquiryHeadEntity inquiryHead = somInquiryMapper.selectById(req.getInquiryHeadId());
        if (ObjectUtil.isEmpty(inquiryHead)) {
            throw new BusinessException(InquiryResponseStatus.INTERFACT_QUERY_INQUIRY_ERR);
        }
        InquiryDetailQueryResp resp = BeanUtil.copyProperties(inquiryHead, InquiryDetailQueryResp.class);

        resp.setDemandCode(demandHeadMapper.selectById(inquiryHead.getDemandHeadId()).getDemandCode());

//        if (InquiryConstants.InquiryNewStatus.PENDING_QUOTED_BY_SUPPLIER.getStatus().equals(inquiryHead.getInquiryStatus())) {
//            //待供方报价状态获取报价截止倒计时
//            try {
//                SupplyInquirySaasHeadDetailDelayVO delay = supplySaasInquiryHeadService.getDelay(req.getInquiryHeadId());
//            } catch (Exception e) {
//                log.error("获取报价截止倒计时异常,mag={},堆栈={}",e.getMessage(),JSON.toJSONString(e.getStackTrace()));
//                throw new BusinessException(120_000, "获取报价截止倒计时异常");
//            }
//        }

        //询价单line表信息
        List<InquiryLineEntity> inquiryLineLists = somInquiryLineMapper.selectList(
                new LambdaQueryWrapper<InquiryLineEntity>().eq(InquiryLineEntity::getInquiryHeadId, inquiryHead.getInquiryHeadId()));
        if (ObjectUtil.isNotEmpty(inquiryLineLists)) {
            List<InquiryLineVO> lineVOS = BeanUtil.convert(inquiryLineLists, InquiryLineVO.class);
            List<InquiryDetailQuotesLineVO> quotesLineVOS = new ArrayList<>();

            lineVOS.stream().forEach(list -> {
                //查询附件数量
                try {
                    ResponseBO<List<FileBillVO>>   listResponseBO =
                            fileBillFeignClient.listFilesById(list.getDemandLineId(), DemandConstants.DemandFileType.DEMAND_LINE_ANNEX.getCode());
                    if (ObjectUtil.isNotEmpty(listResponseBO.getData())) {
                        int size = listResponseBO.getData().size();
                        list.setNumberOfAttachments(String.valueOf(size));
                    }
                } catch (Exception e) {
                    log.info("查询附件数量-》fileBillFeignClient.listFilesById，list{}",JSON.toJSONString(list));
                }

                //拷贝报价商品基础信息
                InquiryDetailQuotesLineVO quotesLineVO = BeanUtil.convert(list,InquiryDetailQuotesLineVO.class);
                //查询报价商品信息
                QuotesLineEntity quotesLineEntity = quotesLineMapper.selectOne(new LambdaQueryWrapper<QuotesLineEntity>()
                        .eq(QuotesLineEntity::getDemandLineId,list.getDemandLineId())
                        //.eq(QuotesLineEntity::getCategoryCode, list.getCategoryCode())
                );
                if (quotesLineEntity != null) {
                    //拷贝报价商品供方报价信息(选定供应商报价后)
                    BeanUtils.copyProperties(quotesLineEntity,quotesLineVO);
                }
                quotesLineVOS.add(quotesLineVO);
            });
            resp.setInquiryLineVOS(lineVOS);
            resp.setQuotesLineVOS(quotesLineVOS);
        }

        return ResponseUtil.ok(resp);
    }

    private Map<String, List<SupplyInquiryQuotesInfoVO>> getSupplyQuotesMap(InquiryHeadEntity inquiryHeadEntity, List<InquiryLineEntity> inquiryLineEntities) {
        List<String> lineIds = inquiryLineEntities.stream().map(InquiryLineEntity::getInquiryLineId).collect(Collectors.toList());
        SupplyInquiryQuotesInfoReq infoReq = new SupplyInquiryQuotesInfoReq();
        infoReq.setInquiryHeadId(inquiryHeadEntity.getInquiryHeadId());
        infoReq.setInquiryLineIds(lineIds);
        ResponseBO<SupplyInquiryQuotesInfoResp> infoVO = supplyInquiryService.supplierQuoteInfo(infoReq);
        Map<String, List<SupplyInquiryQuotesInfoVO>> supplyQuotesMap =
                ObjectUtil.isEmpty(infoVO.getData()) ? null : infoVO.getData().getSupplyQuotesMap();
        return supplyQuotesMap;
    }

    @Override
    public ResponseBO<InquiryDetailSupplierQuotesInfoResp> supplierQuotesInfo(InquiryDetailSupplierQuotesInfoReq req) {
        //todo offer报价时获取供方商品图片
        InquiryDetailSupplierQuotesInfoResp resp = new InquiryDetailSupplierQuotesInfoResp();
        List<InquiryDetailSupplierQuotesInfoVO> infos = inquiryHeadMapper.querySupplierQuotesInfo(req.getInquiryHeadId());
        if (CollectionUtils.isEmpty(infos)) {

        }

        resp.setTotalQuoteNum(infos.size());
        for (InquiryDetailSupplierQuotesInfoVO info : infos) {
            if (SupplyInquiryConstants.QuotesFromSource.T3.getKey().equals(info.getFromSource())) {
                resp.setSelfQuoteNum(resp.getSelfQuoteNum() + 1);
            }

            SupplyInquiryConstants.QuotesStatus quotesStatus;
            try {
                quotesStatus = SupplyInquiryConstants.QuotesStatus.getValue(info.getQuotesStatus());
            } catch (Exception e) {
                log.error("供方报价单报价状态异常,报价单信息={}", JSON.toJSONString(info));
                throw new BusinessException(120_000,"供方报价单报价状态异常");
            }
            switch (quotesStatus) {
                case CLOSED:
                    resp.setClosedNum(resp.getClosedNum() + 1);
                    break;
                case WAIT_FOR_QUOTE:
                    resp.setQuoteIngNum(resp.getQuoteIngNum() + 1);
                    break;
                case QUOTED:
                    resp.setQuotedNum(resp.getQuotedNum() + 1);
                    break;
            }
        }


        //过滤已报价
        //按金额排序
        infos = infos.stream().filter(info -> SupplyInquiryConstants.QuotesStatus.QUOTED.getKey().equals(info.getQuotesStatus()))
                .peek(info ->
                        info.setTotalAmount(info.getSupplyQuotesLineVOS().stream().map(SupplyQuotesLineVO::getPurAmount).reduce(BigDecimal.ZERO, BigDecimal::add)))
                .collect(Collectors.toList())
        .stream()
                .sorted(Comparator.comparing(InquiryDetailSupplierQuotesInfoVO::getTotalAmount))
                .collect(Collectors.toList());

        //回写以待选字段
        resp.setSupplierQuotesInfoVOS(infos);
        return ResponseUtil.ok(resp);
    }

    @Override
    public ResponsePageBO<InquiryDetailQuotesRecordResp> quotesRecord(InquiryDetailQuotesRecordReq req) {
        //该商品行的所有供方报价结果
        PageHelper.startPage(req.getPageNum(), req.getPageSize());
        List<InquiryDetailQuotesRecordResp> recordDOs = inquiryHeadMapper.queryQuotesRecord("");
        if (CollectionUtils.isEmpty(recordDOs)) {
            return PageResponseUtil.ok(req, 0L, new ArrayList<>());
        }
        PageInfo<InquiryDetailQuotesRecordResp> pageInfo = new PageInfo<>(recordDOs);

        return PageResponseUtil.ok(req, pageInfo.getTotal(), recordDOs);
    }

    /**
     * 分配采购经理
     */
    @Override
    public ResponseBO<Boolean> assignPurchaseManager(InquiryAssignReq req) {

        List<String> inquiryHeadIds = req.getInquiryHeadId();
        for (String inquiryHeadId : inquiryHeadIds) {
            //状态为待分配状态可分配采购经理，状态变更为待受理状态
            InquiryHeadEntity inquiryHeadEntity = somInquiryMapper.selectById(inquiryHeadId);
            if (ObjectUtil.isEmpty(inquiryHeadEntity)) {
                throw new BusinessException(InquiryResponseStatus.INTERFACT_QUERY_INQUIRY_ERR);
            }
            //校验当前操作人是否是采购内勤人
            orderAuthUtil.verifyOperator(inquiryHeadEntity.getOfficeStaffId());
            if (!InquiryConstants.InquiryStatus.TO_BE_ALLOCATED.getStatus().equals(inquiryHeadEntity.getInquiryStatus())) {
                throw new BusinessException(InquiryResponseStatus.INTERFACT_QUERY_STATUS_ERR);
            }
            BeanUtil.copyProperties(req, inquiryHeadEntity);
            inquiryHeadEntity.setInquiryStatus(InquiryConstants.InquiryStatus.PENDING.getStatus());
            try {
                somInquiryMapper.updateById(inquiryHeadEntity);
            } catch (Exception e) {
                log.error("分配采购经理失败失败{}", e.getMessage());
                throw new BusinessException(InquiryResponseStatus.FAILED_TO_ASSIGN_PURCHASING_MANAGER);
            }
            //  通知待办
            try {
                CreateMissionReq createMissionReq = new CreateMissionReq();
                StaffReq staffReq = new StaffReq();
                staffReq.setStaffId(req.getStaffId());
                staffReq.setStaffName(req.getStaffName());
                createMissionReq.setSerialType(MissionConstants.OrderType.UN_SOM_ENQUIRY.getCode());
                createMissionReq.setSerialDealStatus(InquiryConstants.InquiryStatus.PENDING.getStatus());
                createMissionReq.setSerialCode(req.getInquiryCode());
                createMissionReq.setDealStaff(staffReq);
                missionService.createMission(createMissionReq);
            } catch (Exception e) {
                log.error("通知待办失败{}", e.getMessage());
            }



        }
        return ResponseUtil.ok();
    }

    private void sendSupplyInqueryMessage(StaffReq staffReq, InquiryHeadEntity inquiryHeadEntity) {
        try {
            ResponseBO<StaffInfoVO> staffInfoVOResponseBO = staffInfoFeignClient.get(staffReq.getStaffId());
            StaffInfoVO staffInfoVO = staffInfoVOResponseBO.getData();
            HashMap<String, String> objectObjectHashMap = Maps.newHashMap();
            objectObjectHashMap.put("InquiryOrderCode", inquiryHeadEntity.getInquiryCode());
            smsService.sendSms(staffInfoVO.getTelephone(), smsService.SMS_196147301, objectObjectHashMap);
        } catch (Exception e) {
            log.error("采购小组管理员指派给对应的采购经理后通知采购小组管理员-发送短信失败，InquiryCode：{},e", inquiryHeadEntity.getInquiryCode(), e.getMessage());
        }


    }

    /**
     * 采购经理接单
     */
    @Override
    @Transactional
    public ResponseBO<Boolean> accept(InquirySubmitReq req) {
        //状态为待受理时可接单，接单完成后需同步生成报价单，状态变更为待报价状态
        if (ObjectUtil.isEmpty(req)) {
            throw new BusinessException(InquiryResponseStatus.INTERFACT_QUERY_INQUIRY_ERR);
        }
        List<InquiryHeadEntity> inquiryHeadEntities = somInquiryMapper.selectBatchIds(req.getInquiryHeadIdList());
        if (ObjectUtil.isEmpty(inquiryHeadEntities) || req.getInquiryHeadIdList().size() != inquiryHeadEntities.size()) {
            throw new BusinessException(InquiryResponseStatus.INTERFACT_QUERY_INQUIRY_ERR);
        }
        inquiryHeadEntities.stream().forEach(list -> {
            orderAuthUtil.verifyOperator(list.getStaffId());

            if (!InquiryConstants.InquiryStatus.PENDING.getStatus().equals(list.getInquiryStatus())) {
                throw new BusinessException(InquiryResponseStatus.INTERFACT_QUERY_STATUS_ERR);
            }
            list.setInquiryStatus(InquiryConstants.InquiryStatus.PENDING_QUOTATION.getStatus());
            somInquiryMapper.updateById(list);
            //同步生成报价单
            QuotesHeadEntity headEntity = quotesHeadMapper.selectOne(
                    new LambdaQueryWrapper<QuotesHeadEntity>()
                            .eq(QuotesHeadEntity::getQuotesCode, list.getInquiryCode())
                            .or()
                            .eq(QuotesHeadEntity::getInquiryHeadId, list.getInquiryHeadId()));
            if (ObjectUtil.isNotEmpty(headEntity)) {
                throw new BusinessException(InquiryResponseStatus.QUOTATION_ALREADY_EXISTS);
            }
            QuotesHeadEntity quotesHeadEntity = new QuotesHeadEntity();
            quotesHeadEntity.setInquiryHeadId(list.getInquiryHeadId());
            quotesHeadEntity.setQuotesCode(list.getInquiryCode());
            quotesHeadEntity.setQuotesStatus(InquiryConstants.InquiryStatus.PENDING_QUOTATION.getStatus());
            try {
                quotesHeadMapper.insert(quotesHeadEntity);
            } catch (Exception e) {
                log.error("接单同步报价单主表失败{}", e.getMessage());
                throw new BusinessException(InquiryResponseStatus.ORDER_FAILED);
            }
            List<InquiryLineEntity> inquiryLineEntities = somInquiryLineMapper.selectList(
                    new LambdaQueryWrapper<InquiryLineEntity>().eq(InquiryLineEntity::getInquiryHeadId, list.getInquiryHeadId()));
            if (ObjectUtil.isEmpty(inquiryLineEntities)) {
                throw new BusinessException(InquiryResponseStatus.ORDER_FAILED);
            }
            inquiryLineEntities.stream().forEach(lineList -> {
                try {
                    QuotesLineEntity quotesLineEntity = new QuotesLineEntity();
                    BeanUtil.copyProperties(lineList, quotesLineEntity);
                    quotesLineEntity.setQuotesHeadId(quotesHeadEntity.getQuotesHeadId());
                    quotesLineMapper.insert(quotesLineEntity);
                } catch (Exception e) {
                    log.error("接单同步报价单子表失败{}", e.getMessage());
                    throw new BusinessException(InquiryResponseStatus.ORDER_FAILED);
                }
            });
        });
        return ResponseUtil.ok();
    }

    @Override
    @Transactional
    public ResponseBO<Boolean> returnDemand(InquirySubmitNuclearReq req) {
        // 状态为待报价、报价失效、核价退回状态且无待报价的供方存在时可以退回至需求单，退回后需同步需求单状态为退回状态
        List<String> statusList = Arrays.asList(
                InquiryConstants.InquiryStatus.PENDING_QUOTATION.getStatus(),
                InquiryConstants.InquiryStatus.INVALID.getStatus(),
                InquiryConstants.InquiryStatus.REVIEW_RETURN.getStatus()
        );
        InquiryHeadEntity inquiryHeadEntity = somInquiryMapper.selectById(req.getInquiryHeadId());
        if (ObjectUtil.isEmpty(inquiryHeadEntity)) {
            throw new BusinessException(InquiryResponseStatus.INTERFACT_QUERY_INQUIRY_ERR);
        }
        //判断是否可退回状态
        if (!statusList.contains(inquiryHeadEntity.getInquiryStatus())) {
            throw new BusinessException(InquiryResponseStatus.INTERFACT_QUERY_STATUS_ERR);
        }
        //判断询价商品是否有在待报价状态
        List<InquiryLineEntity> inquiryLineEntities = somInquiryLineMapper.selectList(
                new LambdaQueryWrapper<InquiryLineEntity>().eq(InquiryLineEntity::getInquiryHeadId, inquiryHeadEntity.getInquiryHeadId()));
        if (ObjectUtil.isEmpty(inquiryLineEntities)) {
            // 子单无商品主单状态应为关闭状态
            throw new BusinessException(InquiryResponseStatus.INTERFACT_QUERY_STATUS_ERR);
        }
        Map<String, List<SupplyInquiryQuotesInfoVO>> supplyQuotesMap = getSupplyQuotesMap(inquiryHeadEntity, inquiryLineEntities);
        if (ObjectUtil.isNotEmpty(supplyQuotesMap)) {
            inquiryLineEntities.stream().forEach(list -> {
                List<SupplyInquiryQuotesInfoVO> supplyInquiryQuotesInfoVOS = supplyQuotesMap.get(list.getInquiryLineId());
                if (ObjectUtil.isNotEmpty(supplyInquiryQuotesInfoVOS)) {
                    supplyInquiryQuotesInfoVOS.stream().forEach(supplys -> {
                        if (ObjectUtil.isNotEmpty(supplys)) {
                            if (SupplyInquiryConstants.QuotesStatus.WAIT_FOR_QUOTE.key.equals(supplys.getQuotesStatus())) {
                                throw new BusinessException(InquiryResponseStatus.CANNOT_BE_RETURNED);
                            }
                        }
                    });
                }
            });
        }
        //询报价单状态退回
        // TODO  退回原因待完善
        if (ObjectUtil.isNotEmpty(req.getRejectReason())) {
            inquiryHeadEntity.setReturnReason(req.getRejectReason() + ";" + (ObjectUtil.isEmpty(inquiryHeadEntity.getReturnReason()) ? "" : inquiryHeadEntity.getReturnReason()));
        }
        inquiryHeadEntity.setInquiryHeadId(req.getInquiryHeadId());
        inquiryHeadEntity.setInquiryStatus(InquiryConstants.InquiryStatus.RETURN.getStatus());
        somInquiryMapper.updateById(inquiryHeadEntity);
        QuotesHeadEntity quotesHeadEntity = quotesHeadMapper.selectOne(new LambdaQueryWrapper<QuotesHeadEntity>()
                .eq(QuotesHeadEntity::getInquiryHeadId, inquiryHeadEntity.getInquiryHeadId()));
        if (ObjectUtil.isNotEmpty(quotesHeadEntity)) {
            quotesHeadEntity.setQuotesStatus(InquiryConstants.InquiryStatus.RETURN.getStatus());
            somQuotesHeadMapper.updateById(quotesHeadEntity);
        }
        //同步需求单状态
        // 退回原因
        DemandHeadEntity demandHeadEntity = demandHeadMapper.selectById(inquiryHeadEntity.getDemandHeadId());
        if (ObjectUtil.isNotEmpty(req.getRejectReason())) {
            demandHeadEntity.setRejectReason(req.getRejectReason() + ";" + (ObjectUtil.isEmpty(demandHeadEntity.getRejectReason()) ? "" : demandHeadEntity.getRejectReason()));
        }
        demandHeadEntity.setDemandStatus(DemandConstants.DemandStatus.ROLLBACK.getCode());
        demandHeadEntity.setGmtModifyTime(null);
        demandHeadMapper.updateById(demandHeadEntity);
        return ResponseUtil.ok();
    }

    @Override
    public ResponseBO<Boolean> deleteInquiry(InquiryDeleteReq req) {
        //需方SaaS删除商品，所有供方都拒绝报价的商品都删除后询价单状态置为待报价或关闭
        if (ObjectUtil.isEmpty(req)) {
            throw new BusinessException(InquiryResponseStatus.INTERFACT_QUERY_INQUIRY_ERR);
        }
        // 判断需求单对应询价单是否存在
        List<InquiryHeadEntity> inquiryEntities = somInquiryMapper.selectList(
                new LambdaQueryWrapper<InquiryHeadEntity>().eq(InquiryHeadEntity::getDemandHeadId, req.getDemandHeadId()));
        if (ObjectUtil.isEmpty(inquiryEntities)) {
            throw new BusinessException(InquiryResponseStatus.INTERFACT_QUERY_INQUIRY_ERR);
        }
        //  查询所有要删除的商品所在的询价单line表，以主表id分组
        List<InquiryLineEntity> inquiryLineEntities = somInquiryLineMapper.selectList(
                new LambdaQueryWrapper<InquiryLineEntity>().in(InquiryLineEntity::getDemandLineId,
                        req.getDemandLineId()).groupBy(InquiryLineEntity::getInquiryHeadId));
        List<String> headIdList = inquiryLineEntities.stream().map(InquiryLineEntity::getInquiryHeadId).collect(Collectors.toList());
        // 所有要删除商品对应的所有询价单
        List<InquiryHeadEntity> inquiryHeadEntities = somInquiryMapper.selectBatchIds(headIdList);
        // 所有删除商品询价单对应的所有子单
        List<InquiryLineEntity> inquiryLineEntityList = somInquiryLineMapper.selectList(
                new LambdaQueryWrapper<InquiryLineEntity>().in(InquiryLineEntity::getDemandLineId, req.getDemandLineId()));
        if (ObjectUtil.isEmpty(inquiryLineEntityList)) {
            throw new BusinessException(InquiryResponseStatus.FAILED_TO_DELETE);
        }
        // 以询价单主表id为key ,对应的所有需要删除商品的询价单子为value
        Map<String, List<InquiryLineEntity>> stringMap = inquiryLineEntityList.stream().collect(Collectors.groupingBy(i -> i.getInquiryHeadId()));
        inquiryHeadEntities.stream().forEach(list -> {
            if (!InquiryConstants.InquiryStatus.RETURN.getStatus().equals(list.getInquiryStatus())) {
                throw new BusinessException(InquiryResponseStatus.INTERFACT_INOPERABLE);
            }
            List<String> inquiryLineIds = stringMap.get(list.getInquiryHeadId()).stream().map(InquiryLineEntity::getInquiryLineId).collect(Collectors.toList());
            int size = inquiryLineIds.size();
            // 该询价单下所有子单
            List<InquiryLineEntity> lineEntities = somInquiryLineMapper.selectList(
                    new LambdaQueryWrapper<InquiryLineEntity>().eq(InquiryLineEntity::getInquiryHeadId, list.getInquiryHeadId()));
            int count = lineEntities.size();
            // 是否全量删除？关闭：删除
            if (size == count) {
                list.setInquiryStatus(InquiryConstants.InquiryStatus.CLOSE.getStatus());
                list.setCloseRemark("相关询价商品已全部删除");
                somInquiryMapper.updateById(list);

            } else {
                // 删除逻辑
                somInquiryLineMapper.delete(new LambdaQueryWrapper<InquiryLineEntity>().in(InquiryLineEntity::getInquiryLineId, inquiryLineIds));
                quotesLineMapper.delete(new LambdaQueryWrapper<QuotesLineEntity>().in(QuotesLineEntity::getInquiryLineId, inquiryLineIds));
                // todo
                boolean falg = true;
                List<InquiryLineEntity> inquiryLineS = somInquiryLineMapper.selectList(
                        new LambdaQueryWrapper<InquiryLineEntity>().eq(InquiryLineEntity::getInquiryHeadId, list.getInquiryHeadId()));
                Map<String, List<SupplyInquiryQuotesInfoVO>> supplyQuotesMap = getSupplyQuotesMap(list, inquiryLineS);
                log.info("deleteInquiry->supplyQuotesMap:{}", JSONUtil.toJSON(supplyQuotesMap));
                if (ObjectUtil.isNotEmpty(supplyQuotesMap)) {
                    for (InquiryLineEntity inquiryLineEntity : inquiryLineS) {
                        List<SupplyInquiryQuotesInfoVO> infoVOList = supplyQuotesMap.get(inquiryLineEntity.getInquiryLineId());
                        log.info("deleteInquiry->infoVOList:{}", JSONUtil.toJSON(infoVOList));
                        //一个商品对应的供方
                        if (ObjectUtil.isNotEmpty(infoVOList)) {
                            List<SupplyInquiryQuotesInfoVO> validInfo = infoVOList.stream().filter(i -> SupplyInquiryConstants.QuotesStatus.WAIT_FOR_QUOTE.getKey().equals(i.getQuotesStatus())
                                    || SupplyInquiryConstants.QuotesStatus.QUOTED.getKey().equals(i.getQuotesStatus())).collect(Collectors.toList());
                            //没有存在待报价的
                            log.info("deleteInquiry->validInfo:{}", JSONUtil.toJSON(validInfo));
                            if (ObjectUtil.isEmpty(validInfo)) {
                                falg = false;
                            }
                        }
                    }
                }
                if (falg) {
                    list.setInquiryStatus(InquiryConstants.InquiryStatus.PENDING_QUOTATION.getStatus());
                    somInquiryMapper.updateById(list);
                }
            }
        });
        List<InquiryHeadEntity> inquiryEntityList = somInquiryMapper.selectList(
                new LambdaQueryWrapper<InquiryHeadEntity>()
                        .eq(InquiryHeadEntity::getDemandHeadId, req.getDemandHeadId())
                        .ne(InquiryHeadEntity::getInquiryStatus, InquiryConstants.InquiryStatus.CLOSE.getStatus()));
        Set<String> statusList = inquiryEntityList.stream().map(InquiryHeadEntity::getInquiryStatus).collect(Collectors.toSet());
        DemandHeadEntity demandHeadEntity = new DemandHeadEntity();
        demandHeadEntity.setDemandHeadId(req.getDemandHeadId());
        if (!statusList.contains(InquiryConstants.InquiryStatus.RETURN.getStatus())) {
            demandHeadEntity.setDemandStatus(DemandConstants.DemandStatus.BE_QUOTED.getCode());
            demandHeadMapper.updateById(demandHeadEntity);
        }
        if (statusList.size() == 1 && statusList.contains(InquiryConstants.InquiryStatus.REVIEW_PASSED.getStatus())) {
            // 生成销售订单
                insertInquirySomSaleOrder(inquiryHeadEntities.get(0).getInquiryHeadId(), req.getDemandHeadId());
        }
        return ResponseUtil.ok();
    }

    @Override
    @Transactional
    public ResponseBO<Boolean> closeInquiry(InquiryDeleteReq req) {
        //需方SaaS关闭询价->关联关闭需求单->需方询价单

        if (ObjectUtil.isEmpty(req)) {
            throw new BusinessException(InquiryResponseStatus.CLOSE_FAILED);
        }
        List<InquiryHeadEntity> inquiryHeadEntities = somInquiryMapper.selectList(
                new LambdaQueryWrapper<InquiryHeadEntity>().eq(InquiryHeadEntity::getDemandHeadId, req.getDemandHeadId()));
        if (ObjectUtil.isNotEmpty(inquiryHeadEntities)) {
            inquiryHeadEntities.stream().forEach(inquiryHeadEntity -> {
                inquiryHeadEntity.setInquiryStatus(InquiryConstants.InquiryStatus.CLOSE.getStatus());
                inquiryHeadEntity.setCloseRemark("关联需求单已关闭");
                somInquiryMapper.updateById(inquiryHeadEntity);
                SupplyInquiryShutReq supplyInquiryShutReq = new SupplyInquiryShutReq();
                supplyInquiryShutReq.setType(1);
                supplyInquiryShutReq.setInquiryHeadId(inquiryHeadEntity.getInquiryHeadId());
                supplyInquiryShutReq.setReason("关联询报价单已关闭");
                try {
                    supplyInquiryService.shutSupplyInquiry(supplyInquiryShutReq);
                } catch (Exception e) {
                    log.error("关联关闭供方询报价失败{}", e.getMessage());
                }
            });
        }
        return ResponseUtil.ok();
    }

    public InquiryHeadEntity inquiryValidStatus(String inquiryHeadId) {
        List<String> inquiryValidStatus = Arrays.asList(
                InquiryConstants.InquiryStatus.PENDING_QUOTATION.getStatus(),
                InquiryConstants.InquiryStatus.REVIEW_RETURN.getStatus(),
                InquiryConstants.InquiryStatus.INVALID.getStatus()
        );
        InquiryHeadEntity inquiryHeadEntity = somInquiryMapper.selectOne(
                new LambdaQueryWrapper<InquiryHeadEntity>()
                        .eq(InquiryHeadEntity::getInquiryHeadId, inquiryHeadId)
                        .in(InquiryHeadEntity::getInquiryStatus, inquiryValidStatus));
        return inquiryHeadEntity;
    }

    @Override
    public void insertVersionByDemand(String demandId) {
        //增加询价单版本号
        Wrapper<InquiryHeadEntity> eq = new QueryWrapper<InquiryHeadEntity>().lambda().eq(InquiryHeadEntity::getDemandHeadId, demandId);
        List<InquiryHeadEntity> inquiryHeadEntities = somInquiryMapper.selectList(eq);
        inquiryHeadEntities.forEach(i -> {
            InquiryDetailQueryReq inquiryDetailQueryReq = new InquiryDetailQueryReq();
            inquiryDetailQueryReq.setInquiryHeadId(i.getInquiryHeadId());
            ResponseBO<InquiryDetailQueryResp> responseBo = queryDetail(inquiryDetailQueryReq);
            if (Objects.nonNull(responseBo) && Objects.nonNull(responseBo.getData())) {
                orderVersionService.createSaleVersion(i.getInquiryHeadId(),
                        OrderVersionConstant.BusinessType.OP_INQUIRY.key, i.getVersionNo(),
                        (JSONObject) JSONObject.toJSON(responseBo.getData()));
                i.setVersionNo(i.getVersionNo() + 1);
                somInquiryMapper.updateById(i);
            }
        });

    }

    @Override
    public ResponseBO<List<InquiryVersionVo>> inquiryVersionList(InquiryDetailQueryReq inquiryDetailQueryReq) {
        OrderVersionReq orderVersionReq = new OrderVersionReq();
        orderVersionReq.setBusinessId(inquiryDetailQueryReq.getInquiryHeadId());
        orderVersionReq.setBusinessType(OrderVersionConstant.BusinessType.OP_INQUIRY.key);
        List<OrderVersionEntity> versionList = orderVersionService.listByBusinessId(orderVersionReq);
        List<InquiryVersionVo> demandList = new ArrayList<>();
        versionList.forEach(orderVersion -> {
            InquiryVersionVo vo = new InquiryVersionVo();
            BeanUtils.copyProperties(orderVersion, vo);
            InquiryDetailQueryResp detailVo = JSONUtil.parseObject(orderVersion.getBusinessJson(), InquiryDetailQueryResp.class);
            vo.setInquiryCode(Objects.isNull(detailVo.getInquiryCode()) ? null : detailVo.getInquiryCode());
            vo.setInquiryHeadId(Objects.isNull(detailVo.getInquiryHeadId()) ? null : detailVo.getInquiryHeadId());

            demandList.add(vo);
        });
        return ResponseUtil.ok(demandList);
    }

    @Override
    public ResponseBO inquiryVersionDetail(OrderVersionReq orderVersionReq) {
        return ResponseUtil.ok(orderVersionService.detail(orderVersionReq));
    }
}
