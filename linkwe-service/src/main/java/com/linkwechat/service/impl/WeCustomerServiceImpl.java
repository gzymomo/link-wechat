package com.linkwechat.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.linkwechat.common.annotation.SynchRecord;
import com.linkwechat.common.constant.Constants;
import com.linkwechat.common.constant.HttpStatus;
import com.linkwechat.common.constant.SynchRecordConstants;
import com.linkwechat.common.constant.WeConstans;
import com.linkwechat.common.context.SecurityContextHolder;
import com.linkwechat.common.core.domain.AjaxResult;
import com.linkwechat.common.core.domain.entity.SysUser;
import com.linkwechat.common.core.domain.model.LoginUser;
import com.linkwechat.common.core.page.PageDomain;
import com.linkwechat.common.core.page.TableDataInfo;
import com.linkwechat.common.enums.*;
import com.linkwechat.common.exception.wecom.WeComException;
import com.linkwechat.common.utils.DateUtils;
import com.linkwechat.common.utils.SnowFlakeUtil;
import com.linkwechat.common.utils.StringUtils;
import com.linkwechat.common.utils.bean.BeanUtils;
import com.linkwechat.config.rabbitmq.RabbitMQSettingConfig;
import com.linkwechat.domain.*;
import com.linkwechat.domain.customer.WeMakeCustomerTag;
import com.linkwechat.domain.customer.query.WeCustomersQuery;
import com.linkwechat.domain.customer.query.WeOnTheJobCustomerQuery;
import com.linkwechat.domain.customer.vo.*;
import com.linkwechat.domain.wecom.entity.customer.WeCustomerFollowInfoEntity;
import com.linkwechat.domain.wecom.entity.customer.WeCustomerFollowUserEntity;
import com.linkwechat.domain.wecom.query.WeBaseQuery;
import com.linkwechat.domain.wecom.query.customer.UnionidToExternalUserIdQuery;
import com.linkwechat.domain.wecom.query.customer.WeBatchCustomerQuery;
import com.linkwechat.domain.wecom.query.customer.WeCustomerQuery;
import com.linkwechat.domain.wecom.query.customer.tag.WeMarkTagQuery;
import com.linkwechat.domain.wecom.query.customer.transfer.WeTransferCustomerQuery;
import com.linkwechat.domain.wecom.vo.customer.UnionidToExternalUserIdVo;
import com.linkwechat.domain.wecom.vo.customer.WeBatchCustomerDetailVo;
import com.linkwechat.domain.wecom.vo.customer.WeCustomerDetailVo;
import com.linkwechat.domain.wecom.vo.customer.WeFollowUserListVo;
import com.linkwechat.domain.wecom.vo.customer.transfer.WeTransferCustomerVo;
import com.linkwechat.fegin.QwCustomerClient;
import com.linkwechat.mapper.WeCustomerMapper;
import com.linkwechat.service.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.linkwechat.common.utils.SecurityUtils;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class WeCustomerServiceImpl extends ServiceImpl<WeCustomerMapper, WeCustomer> implements IWeCustomerService {

    @Autowired
    private QwCustomerClient qwCustomerClient;


    @Autowired
    private IWeFlowerCustomerTagRelService iWeFlowerCustomerTagRelService;

    @Autowired
    private IWeTagService iWeTagService;

    @Autowired
    private IWeCustomerTrajectoryService iWeCustomerTrajectoryService;

    @Autowired
    private IWeAllocateCustomerService iWeAllocateCustomerService;


    @Autowired
    private IWeGroupService iWeGroupService;

    @Autowired
    private RabbitMQSettingConfig rabbitMQSettingConfig;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private IWeMessagePushService iWeMessagePushService;



    @Override
    public List<WeCustomersVo> findWeCustomerList(WeCustomersQuery weCustomersQuery, PageDomain pageDomain) {
        List<WeCustomersVo> weCustomersVos=new ArrayList<>();
        List<String> ids = this.baseMapper.findWeCustomerListIds(weCustomersQuery, pageDomain);

        if(CollectionUtil.isNotEmpty(ids)){
            weCustomersVos=this.baseMapper.findWeCustomerList(ids);
        }

        return weCustomersVos;
    }


    @Override
    public long countWeCustomerList(WeCustomersQuery weCustomersQuery) {
        return this.baseMapper.countWeCustomerList(weCustomersQuery);
    }

    @Override
    public  long countWeCustomerListByApp(WeCustomersQuery weCustomersQuery){
       return this.baseMapper.countWeCustomerListByApp(weCustomersQuery);
    }

    @Override
    public long noRepeatCountCustomer(WeCustomersQuery weCustomersQuery) {
        return this.baseMapper.noRepeatCountCustomer(weCustomersQuery);
    }

    @Override
    @SynchRecord(synchType = SynchRecordConstants.SYNCH_CUSTOMER)
    public void synchWeCustomer() {

        LoginUser loginUser = SecurityUtils.getLoginUser();
        rabbitTemplate.convertAndSend(rabbitMQSettingConfig.getWeSyncEx(), rabbitMQSettingConfig.getWeCustomerRk(), JSONObject.toJSONString(loginUser));



    }



    @Override
    @Async
    public void synchWeCustomerHandler(String msg) {

        LoginUser loginUser = JSONObject.parseObject(msg, LoginUser.class);
        SecurityContextHolder.setCorpId(loginUser.getCorpId());
        SecurityContextHolder.setUserName(loginUser.getUserName());
        SecurityContextHolder.setUserId(String.valueOf(loginUser.getSysUser().getUserId()));
        SecurityContextHolder.setUserType(loginUser.getUserType());

        WeFollowUserListVo followUserList = qwCustomerClient.getFollowUserList(new WeBaseQuery()).getData();
        if (null != followUserList && CollectionUtil.isNotEmpty(followUserList.getFollowUser())) {
            List<String> followUsers = followUserList.getFollowUser();

            List<List<String>> partition = Lists.partition(followUsers, 100);
            Map<String, SysUser> currentTenantSysUser = findCurrentTenantSysUser();

            for (List<String> followUser : partition) {

                List<WeCustomerDetailVo> weCustomerDetailVos=new ArrayList<>();
                this.getByUser(followUser,null,weCustomerDetailVos);

                if(CollectionUtil.isNotEmpty(weCustomerDetailVos)){
                    List<List<WeCustomerDetailVo>> userDetailPartition = Lists.partition(weCustomerDetailVos, 1000);
                    for (List<WeCustomerDetailVo> details : userDetailPartition) {
                        this.weFlowerCustomerHandle(details,currentTenantSysUser);
                    }
                }

            }
        }


    }

    private void getByUser(List<String> followUser, String nextCursor, List<WeCustomerDetailVo> list){
        WeBatchCustomerDetailVo weBatchCustomerDetails = qwCustomerClient
                .getBatchCustomerDetail(new WeBatchCustomerQuery(followUser, nextCursor, 100)).getData();

        if (WeErrorCodeEnum.ERROR_CODE_0.getErrorCode().equals(weBatchCustomerDetails.getErrCode())
                || WeConstans.NOT_EXIST_CONTACT.equals(weBatchCustomerDetails.getErrCode())
                && ArrayUtil.isNotEmpty(weBatchCustomerDetails.getExternalContactList())) {
            list.addAll(weBatchCustomerDetails.getExternalContactList());
            if (StringUtils.isNotEmpty(weBatchCustomerDetails.getNext_cursor())) {
                getByUser(followUser, weBatchCustomerDetails.getNext_cursor(), list);
            }
        }

    }


    //????????????????????????,??????
    private void weFlowerCustomerHandle(List<WeCustomerDetailVo> details,Map<String, SysUser> currentTenantSysUser) {

        List<WeCustomer> weCustomerList = new ArrayList<>();


        List<WeFlowerCustomerTagRel> weFlowerCustomerTagRels = new ArrayList<>();

        details.stream().forEach(k -> {

            WeCustomerDetailVo.ExternalContact externalContact = k.getExternalContact();

            WeCustomerFollowInfoEntity followInfo = k.getFollowInfo();

            if (null != followInfo && null != externalContact) {
                WeCustomer weCustomer = new WeCustomer();
                weCustomer.setId(SnowFlakeUtil.nextId());

                SysUser sysUser = currentTenantSysUser.get(followInfo.getUserId());

                if(null != sysUser){
                    weCustomer.setCreateBy(sysUser.getUserName());
                    weCustomer.setCreateById(sysUser.getUserId());
                    weCustomer.setUpdateBy(sysUser.getUserName());
                    weCustomer.setUpdateById(sysUser.getUserId());
                }

                weCustomer.setCreateTime(new Date());
                weCustomer.setUpdateTime(new Date());
                weCustomer.setExternalUserid(externalContact.getExternalUserId());
                weCustomer.setCustomerName(externalContact.getName());
                weCustomer.setCustomerType(externalContact.getType());
                weCustomer.setAvatar(externalContact.getAvatar());
                weCustomer.setGender(externalContact.getGender());
                weCustomer.setUnionid(externalContact.getUnionId());
                weCustomer.setCorpName(followInfo.getRemarkCompany());
                weCustomer.setAddUserId(followInfo.getUserId());
                weCustomer.setAddTime(new Date(followInfo.getCreateTime() * 1000L));
                weCustomer.setAddMethod(followInfo.getAddWay());
                weCustomer.setState(followInfo.getState());
                weCustomer.setDelFlag(0);
                weCustomer.setRemarkName(followInfo.getRemark());
                weCustomer.setOtherDescr(followInfo.getDescription());
                weCustomer.setPhone(String.join(",", Optional.ofNullable(followInfo.getRemarkMobiles()).orElseGet(ArrayList::new)));

                List<String> tagIds = followInfo.getTagId();

                if (CollectionUtil.isNotEmpty(tagIds)) {

                    weCustomer.setTagIds(
                            tagIds.stream().map(String::valueOf).collect(Collectors.joining(","))
                    );

                    tagIds.stream().forEach(tagId -> {
                        WeFlowerCustomerTagRel weFlowerCustomerTagRel = WeFlowerCustomerTagRel.builder()
                                .id(SnowFlakeUtil.nextId())
                                .externalUserid(externalContact.getExternalUserId())
                                .tagId(tagId)
                                .userId(followInfo.getUserId())
                                .isCompanyTag(true)
                                .delFlag(0)
                                .build();
                        weFlowerCustomerTagRel.setCreateTime(new Date());
                        weFlowerCustomerTagRel.setUpdateTime(new Date());
                        if(null != sysUser){
                            weFlowerCustomerTagRel.setCreateBy(sysUser.getUserName());
                            weFlowerCustomerTagRel.setCreateById(sysUser.getUserId());
                            weFlowerCustomerTagRel.setUpdateBy(sysUser.getUserName());
                            weFlowerCustomerTagRel.setUpdateById(sysUser.getUserId());
                        }
                        weFlowerCustomerTagRels.add(
                                weFlowerCustomerTagRel
                        );


                    });
                }


                weCustomerList.add(weCustomer);
            }

        });



        if (CollectionUtil.isNotEmpty(weFlowerCustomerTagRels)) {
            List<List<WeFlowerCustomerTagRel>> tagRels = Lists.partition(weFlowerCustomerTagRels, 500);
            for (List<WeFlowerCustomerTagRel> tagRelss : tagRels) {
                iWeFlowerCustomerTagRelService.batchAddOrUpdate(tagRelss);
            }
        }

        if (CollectionUtil.isNotEmpty(weCustomerList)) {

            List<List<WeCustomer>> partition = Lists.partition(weCustomerList, 500);
            for (List<WeCustomer> weCustomers : partition) {
                this.baseMapper.batchAddOrUpdate(weCustomers);
            }


        }


    }


    @Override
    @Transactional
    public void makeLabel(WeMakeCustomerTag weMakeCustomerTag) {
        List<WeTag> addTag = weMakeCustomerTag.getAddTag();

        //??????????????????????????????????????????
        iWeFlowerCustomerTagRelService.remove(new LambdaQueryWrapper<WeFlowerCustomerTagRel>()
                .eq(WeFlowerCustomerTagRel::getExternalUserid, weMakeCustomerTag.getExternalUserid())
                .eq(WeFlowerCustomerTagRel::getIsCompanyTag, weMakeCustomerTag.getIsCompanyTag())
                .eq(WeFlowerCustomerTagRel::getUserId, weMakeCustomerTag.getUserId()));

        if (CollectionUtil.isNotEmpty(addTag)) {
            List<WeFlowerCustomerTagRel> tagRels = new ArrayList<>();
            addTag.stream().forEach(k -> {
                WeFlowerCustomerTagRel weFlowerCustomerTagRel = WeFlowerCustomerTagRel.builder()
                        .id(SnowFlakeUtil.nextId())
                        .externalUserid(weMakeCustomerTag.getExternalUserid())
                        .userId(weMakeCustomerTag.getUserId())
                        .tagId(k.getTagId())
                        .isCompanyTag(weMakeCustomerTag.getIsCompanyTag())
                        .delFlag(Constants.COMMON_STATE)
                        .build();
                weFlowerCustomerTagRel.setUpdateTime(new Date());
                weFlowerCustomerTagRel.setUpdateBy(String.valueOf(SecurityUtils.getUserId()));
                tagRels.add(weFlowerCustomerTagRel);
            });
            iWeFlowerCustomerTagRelService.batchAddOrUpdate(tagRels);
        }

        if (weMakeCustomerTag.getIsCompanyTag()) {//????????????,???????????????????????????

            WeMarkTagQuery cutomerTagEdit = WeMarkTagQuery.builder()
                    .external_userid(weMakeCustomerTag.getExternalUserid())
                    .userid(weMakeCustomerTag.getUserId())
                    .add_tag(addTag.stream().map(WeTag::getTagId).collect(Collectors.toList()))
                    .build();

            //??????????????????????????????????????????????????????,????????????????????????,????????????
            List<WeFlowerCustomerTagRel> nowAddWeFlowerCustomerTagRel = iWeFlowerCustomerTagRelService
                    .findNowAddWeFlowerCustomerTagRel(weMakeCustomerTag.getExternalUserid(), weMakeCustomerTag.getUserId());

            if (CollectionUtil.isNotEmpty(nowAddWeFlowerCustomerTagRel)) {
                cutomerTagEdit.setAdd_tag(
                        nowAddWeFlowerCustomerTagRel.stream().map(WeFlowerCustomerTagRel::getTagId).collect(Collectors.toList())
                );



            }
            //?????????????????????+?????????????????????????????????,????????????
            List<WeFlowerCustomerTagRel> removeWeFlowerCustomerTagRel = iWeFlowerCustomerTagRelService
                    .findRemoveWeFlowerCustomerTagRel(weMakeCustomerTag.getExternalUserid(), weMakeCustomerTag.getUserId());

            if (CollectionUtil.isNotEmpty(removeWeFlowerCustomerTagRel)) {
                cutomerTagEdit.setRemove_tag(
                        removeWeFlowerCustomerTagRel.stream().map(WeFlowerCustomerTagRel::getTagId).collect(Collectors.toList())
                );
            }



                AjaxResult weResultVoAjaxResult = qwCustomerClient.makeCustomerLabel(
                        cutomerTagEdit
                );


                if (null != weResultVoAjaxResult && weResultVoAjaxResult.getCode()!=200){
                    log.error(weResultVoAjaxResult.getMsg());
                    throw new WeComException("?????????????????????????????????");
                }else{

                    if(CollectionUtil.isNotEmpty(nowAddWeFlowerCustomerTagRel)){
                        List<WeTag> weTags = iWeTagService.listByIds(
                                nowAddWeFlowerCustomerTagRel.stream().map(WeFlowerCustomerTagRel::getTagId).collect(Collectors.toList())
                        );
                        if(CollectionUtil.isNotEmpty(weTags)){
                            iWeCustomerTrajectoryService.createEditTrajectory(weMakeCustomerTag.getExternalUserid(),
                                    weMakeCustomerTag.getUserId(),
                                    weMakeCustomerTag.getIsCompanyTag() ?
                                            TrajectorySceneType.TRAJECTORY_TITLE_GXQYBQ.getType() :
                                            TrajectorySceneType.TRAJECTORY_TITLE_GXGRBQ.getType(),
                                    String.join(",", weTags.stream().map(WeTag::getName).collect(Collectors.toList()))
                            );
                        }
                    }


                }



        }


            WeCustomer weCustomer = this.getOne(new LambdaQueryWrapper<WeCustomer>()
                    .eq(WeCustomer::getAddUserId, weMakeCustomerTag.getUserId())
                    .eq(WeCustomer::getExternalUserid, weMakeCustomerTag.getExternalUserid()));
            if(null != weCustomer){

                /**
                 * ?????????????????????ids????????????
                 */
                List<WeFlowerCustomerTagRel> nowAddWeFlowerCustomerTagRel
                        = iWeFlowerCustomerTagRelService.findNowAddWeFlowerCustomerTagRel(weMakeCustomerTag.getExternalUserid(), weMakeCustomerTag.getUserId());
                if(CollectionUtil.isNotEmpty(nowAddWeFlowerCustomerTagRel)){
                    weCustomer.setTagIds(
                            nowAddWeFlowerCustomerTagRel.stream().map(WeFlowerCustomerTagRel::getTagId).collect(Collectors.toList())
                                    .stream().map(String::valueOf).collect(Collectors.joining(","))
                    );
                }else{
                    weCustomer.setTagIds(null);
                }
                this.updateById(weCustomer);
            }



    }

    @Override
    public void allocateOnTheJobCustomer(WeOnTheJobCustomerQuery weOnTheJobCustomerQuery) {
        if (this.getOne(
                new LambdaQueryWrapper<WeCustomer>()
                        .eq(WeCustomer::getExternalUserid, weOnTheJobCustomerQuery.getExternalUserid())
                        .eq(WeCustomer::getAddUserId, weOnTheJobCustomerQuery.getTakeoverUserId())
                        .eq(WeCustomer::getDelFlag, new Integer(0))) != null) {

            throw new WeComException("????????????????????????????????????,????????????");
        }

        List<WeAllocateCustomer> weAllocateCustomers = iWeAllocateCustomerService.list(new LambdaQueryWrapper<WeAllocateCustomer>()
                .eq(WeAllocateCustomer::getExternalUserid, weOnTheJobCustomerQuery.getExternalUserid())
                .between(WeAllocateCustomer::getAllocateTime, DateUtils.getBeforeByDayTime(-90)
                        , DateUtils.getBeforeByDayTime(0))
        );

        if (CollectionUtil.isNotEmpty(weAllocateCustomers)
                && weAllocateCustomers.size() >= 2) {
            throw new WeComException("???????????????90???????????????????????????2???,??????????????????");
        }


        //??????????????????
        WeCustomer weCustomer = this.getOne(
                new LambdaQueryWrapper<WeCustomer>()
                        .eq(WeCustomer::getExternalUserid, weOnTheJobCustomerQuery.getExternalUserid())
                        .eq(WeCustomer::getAddUserId, weOnTheJobCustomerQuery.getHandoverUserId())
                        .eq(WeCustomer::getDelFlag, new Integer(0))
        );

        if (null != weCustomer) {

            if (StringUtils.isEmpty(weCustomer.getTakeoverUserId())) { //???????????????

                this.extentCustomer(weCustomer, weOnTheJobCustomerQuery);

            } else { //???????????????(????????????)
                AjaxResult<WeTransferCustomerVo> weTransferCustomerVoAjaxResult = qwCustomerClient.transferResult(WeTransferCustomerQuery.builder()
                        .handover_userid(weCustomer.getAddUserId())
                        .takeover_userid(weCustomer.getTakeoverUserId())
                        .build());

                if (null != weTransferCustomerVoAjaxResult) {
                    WeTransferCustomerVo transferCustomerVo = weTransferCustomerVoAjaxResult.getData();
                    if (null != transferCustomerVo) {
                        List<WeTransferCustomerVo.TransferCustomerVo> extendsCustomers = transferCustomerVo.getCustomer();

                        if (CollectionUtil.isNotEmpty(extendsCustomers)) {
                            WeTransferCustomerVo.TransferCustomerVo extendsCustomer
                                    = extendsCustomers.stream().collect(Collectors.toMap(WeTransferCustomerVo.TransferCustomerVo::getExternalUserId
                                    , ExtendsCustomer -> ExtendsCustomer)).get(weOnTheJobCustomerQuery.getExternalUserid());
                            if (null != extendsCustomer) {

                                if (extendsCustomer.getStatus().equals(AllocateCustomerStatus.JOB_EXTENDS_DDJT.getCode())) {
                                    throw new WeComException(
                                            "????????????:" + weCustomer.getCustomerName() + ",??????:" + findUserNameByUserId(weCustomer.getTakeoverUserId()) + "??????,?????????????????????"
                                    );
                                } else { //???????????????????????????

                                    this.extentCustomer(weCustomer, weOnTheJobCustomerQuery);

                                }


                            }

                        }

                    }

                }


            }


        }

    }

    @Override
    public WeCustomerDetailInfoVo findWeCustomerDetail(String externalUserid, String userId, Integer delFlag) {
        WeCustomerDetailInfoVo weCustomerDetailInfoVo = new WeCustomerDetailInfoVo();

        List<WeCustomersVo> weCustomersVos = this.findWeCustomerList(
                WeCustomersQuery.builder()
                        .externalUserid(externalUserid)
                        .delFlag(delFlag)
                        .build(),
                null
        );


        if (CollectionUtil.isNotEmpty(weCustomersVos)) {
            BeanUtils.copyBeanProp(
                    weCustomerDetailInfoVo,
                    weCustomersVos.stream().findFirst().get()
            );
            List<WeCustomerDetailInfoVo.TrackUser> trackUsers = new ArrayList<>();

            weCustomersVos.stream().forEach(k -> {
                trackUsers.add(WeCustomerDetailInfoVo.TrackUser.builder()
                        .addMethod(k.getAddMethod())
                        .trackUserId(k.getFirstUserId())
                        .firstAddTime(k.getFirstAddTime())
                        .trackState(k.getTrackState())
                         .trackTime(k.getTrackTime())
                        .userName(k.getUserName())
                        .build());
            });
            weCustomerDetailInfoVo.setTrackUsers(
                    trackUsers
            );

            weCustomerDetailInfoVo.setGroups(
                    this.baseMapper.findWecustomerGroups(externalUserid)
            );


        }


        return weCustomerDetailInfoVo;

    }

    @Override
    public WeCustomerDetailInfoVo findWeCustomerInfoSummary(String externalUserid, String userId, Integer delFlag) {

        WeCustomerDetailInfoVo weCustomerDetail = new WeCustomerDetailInfoVo();

        List<WeCustomersVo> weCustomerList = this.findWeCustomerList(WeCustomersQuery.builder()
                .externalUserid(externalUserid)
                .userIds(userId)
                .delFlag(delFlag)
                .build(), null);

        if (CollectionUtil.isNotEmpty(weCustomerList)) {
            List<WeCustomerDetailInfoVo.CompanyOrPersonTag> companyTags = new ArrayList<>();

            List<WeCustomerDetailInfoVo.CompanyOrPersonTag> personTags = new ArrayList<>();

            weCustomerList.stream().forEach(weCustomersVo -> {

                if(StringUtils.isNotEmpty(weCustomersVo.getTagNames())){
                    companyTags.add(
                            WeCustomerDetailInfoVo.CompanyOrPersonTag.builder()
                                    .tagNames(weCustomersVo.getTagNames())
                                    .userName(weCustomersVo.getUserName())
                                    .build()

                    );
                }

                if(StringUtils.isNotEmpty(weCustomersVo.getPersonTagNames())){
                    personTags.add(
                            WeCustomerDetailInfoVo.CompanyOrPersonTag.builder()
                                    .tagNames(weCustomersVo.getPersonTagNames())
                                    .userName(weCustomersVo.getUserName())
                                    .build()

                    );
                }

            });

            weCustomerDetail.setCompanyTags(companyTags);
            weCustomerDetail.setPersonTags(personTags);
//            weCustomerDetail.setTagNames(
//                    Joiner.on(",").join(
//                            weCustomerList.stream().map(WeCustomersVo::getTagNames).collect(Collectors.toList()).stream().filter(
//                                    Objects::nonNull
//                            ).collect(Collectors.toList())
//                    )
//            );


//            weCustomerDetail.setPersonTags(
//                    Joiner.on(",").join(
//                            weCustomerList.stream().map(WeCustomersVo::getPersonTagNames).collect(Collectors.toList()).stream().filter(
//                                    Objects::nonNull
//                            ).collect(Collectors.toList())
//                    )
//            );

//





//
//            List<WeCustomerDetailInfoVo.TrackUser> trackUsers = new ArrayList<>();
//
//            List<WeCustomerDetailInfoVo.TrackStates> trackStates = new ArrayList<>();
//
//
//            weCustomerDetail.setCompanyTags(companyTags);
//            weCustomerDetail.setPersonTags(personTags);
//            weCustomerDetail.setTrackUsers(trackUsers);
//            weCustomerDetail.setTrackStates(trackStates);
        }


        return weCustomerDetail;


    }

    @Override
    public WeCustomerDetailInfoVo findWeCustomerInfoByUserId(String externalUserid, String userId, Integer delFlag) {

        WeCustomerDetailInfoVo weCustomerDetail
                = this.findWeCustomerInfoSummary(externalUserid, userId, delFlag);



        WeCustomer weCustomer = this.getOne(new LambdaQueryWrapper<WeCustomer>()
                .eq(WeCustomer::getExternalUserid, externalUserid)
                .eq(WeCustomer::getAddUserId, userId));
        if (null != weCustomer) {
            BeanUtils.copyBeanProp(weCustomerDetail, weCustomer);
        }

        List<WeCustomerAddGroupVo> groups
                = iWeGroupService.findWeGroupByCustomer(userId, externalUserid);
        if (CollectionUtil.isNotEmpty(groups)) {
            List<WeCustomerAddGroupVo> commonGroup
                    = groups.stream().filter(e -> e.getCommonGroup().equals(new Integer(0))).collect(Collectors.toList());
            weCustomerDetail.setCommonGroupChat(
                    CollectionUtil.isNotEmpty(commonGroup) ?
                            commonGroup.stream().map(WeCustomerAddGroupVo::getGroupName).collect(Collectors.joining(","))
                            : null
            );
        }


        return weCustomerDetail;

    }

    @Override
    public String findUserNameByUserId(String userId) {
        return this.baseMapper.findUserNameByUserId(userId);
    }

    @Override
    public WeCustomerPortraitVo findCustomerByOperUseridAndCustomerId(String externalUserid, String userid) throws Exception {
        WeCustomerPortraitVo weCustomerPortrait
                = this.baseMapper.findCustomerByOperUseridAndCustomerId(externalUserid, userid);

        if (null != weCustomerPortrait) {
            if (weCustomerPortrait.getBirthday() != null) {
                weCustomerPortrait.setAge(DateUtils.getAge(weCustomerPortrait.getBirthday()));
            }

            //??????????????????
            weCustomerPortrait.setSocialConn(
                    this.baseMapper.countSocialConn(externalUserid, userid)
            );

        } else {
            weCustomerPortrait = new WeCustomerPortraitVo();
        }


        return weCustomerPortrait;
    }

    @Override
    @Transactional
    public void updateWeCustomerPortrait(WeCustomerPortraitVo weCustomerPortrait) {
        WeCustomer weCustomer
                = WeCustomer.builder().build();
        BeanUtils.copyBeanProp(weCustomer, weCustomerPortrait);
        //?????????
        weCustomer.setPhone(weCustomerPortrait.getRemarkMobiles());
        weCustomer.setCorpName(weCustomerPortrait.getRemarkCorpName());
        weCustomer.setAddUserId(weCustomerPortrait.getUserId());
        weCustomer.setOtherDescr(weCustomerPortrait.getOtherDescr());


        if (this.update(weCustomer, new LambdaQueryWrapper<WeCustomer>()
                .eq(WeCustomer::getAddUserId, weCustomerPortrait.getUserId())
                .eq(WeCustomer::getExternalUserid, weCustomerPortrait.getExternalUserid()))) {


            iWeCustomerTrajectoryService.createEditTrajectory(
                    weCustomer.getExternalUserid(),weCustomer.getAddUserId(),TrajectorySceneType.TRAJECTORY_TITLE_BJBQ.getType(),null
            );
        }

    }

    @Override
    public List<WeCustomerAddUserVo> findWeUserByCustomerId(String externalUserid) {
        return this.baseMapper.findWeUserByCutomerId(externalUserid);
    }

    @Override
    @Transactional
    public void addOrEditWaitHandle(WeCustomerTrackRecord trajectory) {
        WeCustomer weCustomer = this.getOne(new LambdaQueryWrapper<WeCustomer>()
                .eq(WeCustomer::getExternalUserid, trajectory.getExternalUserid())
                .eq(WeCustomer::getAddUserId, trajectory.getWeUserId()));
        if (weCustomer != null) {
            weCustomer.setTrackState(trajectory.getTrackState());
            weCustomer.setTrackContent(trajectory.getTrackContent());
            weCustomer.setTrackTime(new Date());
            if (this.update(weCustomer, new LambdaQueryWrapper<WeCustomer>().eq(WeCustomer::getAddUserId,
                    weCustomer.getAddUserId())
                    .eq(WeCustomer::getExternalUserid, weCustomer.getExternalUserid()))) {
                iWeCustomerTrajectoryService.createTrackTrajectory(trajectory.getExternalUserid(),trajectory.getWeUserId(),
                        trajectory.getTrackState(),trajectory.getTrackContent());
            }
        }

    }

    //????????????
    private void extentCustomer(WeCustomer weCustomer, WeOnTheJobCustomerQuery weOnTheJobCustomerQuery) {
        weCustomer.setTakeoverUserId(weOnTheJobCustomerQuery.getTakeoverUserId());

        if (this.update(
                weCustomer, new LambdaQueryWrapper<WeCustomer>()
                        .eq(WeCustomer::getExternalUserid, weCustomer.getExternalUserid())
                        .eq(WeCustomer::getAddUserId, weCustomer.getAddUserId()))) {
            AjaxResult<WeTransferCustomerVo> jobExtendsCustomer = qwCustomerClient.transferCustomer(
                    WeTransferCustomerQuery.builder()
                            .external_userid(
                                    ListUtil.toList(weOnTheJobCustomerQuery.getExternalUserid().split(","))
                            )
                            .handover_userid(weOnTheJobCustomerQuery.getHandoverUserId())
                            .takeover_userid(weOnTheJobCustomerQuery.getTakeoverUserId())
                            .build()
            );

            if (null != jobExtendsCustomer) {
                WeTransferCustomerVo transferCustomerVo = jobExtendsCustomer.getData();

                if (null != transferCustomerVo) {

                    if (transferCustomerVo.getErrCode().equals(WeErrorCodeEnum.ERROR_CODE_0.getErrorCode())) {
                        iWeAllocateCustomerService.batchAddOrUpdate(
                                ListUtil.toList(
                                        WeAllocateCustomer.builder()
                                                .id(SnowFlakeUtil.nextId())
                                                .allocateTime(new Date())
                                                .extentType(new Integer(1))
                                                .externalUserid(weOnTheJobCustomerQuery.getExternalUserid())
                                                .handoverUserid(weOnTheJobCustomerQuery.getHandoverUserId())
                                                .takeoverUserid(weOnTheJobCustomerQuery.getTakeoverUserId())
                                                .failReason("????????????")
                                                .build()
                                )
                        );
                    }
                }

            }
        }

    }


    @Override
    public void addCustomer(String externalUserId, String userId, String state) {
        //??????????????????????????????????????????,???????????????????????????????????????????????????
        this.baseMapper.deleteWeCustomer(externalUserId,userId);
        //???????????????????????????
        WeCustomerQuery query = new WeCustomerQuery();
        query.setExternal_userid(externalUserId);
        WeCustomerDetailVo weCustomerDetail = qwCustomerClient.getCustomerDetail(query).getData();

        if (weCustomerDetail != null && weCustomerDetail.getExternalContact() != null) {
            WeCustomerDetailVo.ExternalContact externalContact = weCustomerDetail.getExternalContact();
            //????????????
            WeCustomer weCustomer = new WeCustomer();
            weCustomer.setId(SnowFlakeUtil.nextId());
            weCustomer.setExternalUserid(externalContact.getExternalUserId());
            weCustomer.setCustomerName(externalContact.getName());
            weCustomer.setCustomerType(externalContact.getType());
            weCustomer.setAvatar(externalContact.getAvatar());
            weCustomer.setGender(externalContact.getGender());
            weCustomer.setUnionid(externalContact.getUnionId());
            weCustomer.setDelFlag(0);
            weCustomer.setAddUserId(userId);



            List<WeCustomerFollowUserEntity> followUserList = weCustomerDetail.getFollowUser();
            if (CollectionUtil.isNotEmpty(followUserList)) {
                WeCustomerFollowUserEntity followUserEntity = followUserList.stream().filter(followUserInfo -> followUserInfo.getUserId().equals(userId)).findFirst().get();

                weCustomer.setState(state);
                weCustomer.setAddTime(new Date(followUserEntity.getCreateTime() * 1000L));
                weCustomer.setAddMethod(followUserEntity.getAddWay());
                //????????????????????????????????????,????????????????????????????????????????????????????????????????????????????????????????????????
                if(CustomerAddWay.ADD_WAY_GLYFP.getKey().equals(followUserEntity.getAddWay())){
                    this.remove(new LambdaQueryWrapper<WeCustomer>()
                            .eq(WeCustomer::getExternalUserid,externalUserId)
                            .eq(WeCustomer::getTakeoverUserId,userId));
                }

                weCustomer.setCorpName(followUserEntity.getRemarkCompany());
                weCustomer.setRemarkName(followUserEntity.getRemark());
                weCustomer.setOtherDescr(followUserEntity.getDescription());
                weCustomer.setPhone(String.join(",", Optional.ofNullable(followUserEntity.getRemarkMobiles()).orElseGet(ArrayList::new)));
                //????????????
                List<WeCustomerDetailVo.ExternalUserTag> tags = followUserEntity.getTags();
                if (CollectionUtil.isNotEmpty(tags)) {
                    List<WeFlowerCustomerTagRel> tagRels = tags.stream().map(tagInfo -> WeFlowerCustomerTagRel.builder()
                            .id(SnowFlakeUtil.nextId())
                            .externalUserid(externalContact.getExternalUserId())
                            .tagId(tagInfo.getTagId())
                            .userId(followUserEntity.getUserId())
                            .isCompanyTag(true)
                            .delFlag(0)
                            .build()).collect(Collectors.toList());
                    iWeFlowerCustomerTagRelService.batchAddOrUpdate(ListUtil.toList(tagRels));
                }
            }
            this.baseMapper.batchAddOrUpdate(ListUtil.toList(weCustomer));


//            if(StringUtils.isNotEmpty(state)){
                //????????????
                iWeCustomerTrajectoryService.createAddOrRemoveTrajectory(externalUserId,userId,true,true);
                //??????????????????????????????????????????
                iWeMessagePushService.pushMessageSelfH5(ListUtil.toList(userId), "??????????????????<br/><br/> ??????@"+weCustomer.getCustomerName()+"??????????????????", MessageNoticeType.ADDCUTOMER.getType(),false);
//            }

        }
    }

    @Override
    public void updateCustomer(String externalUserId, String userId) {
        //???????????????????????????
        WeCustomerQuery query = new WeCustomerQuery();
        query.setExternal_userid(externalUserId);
        WeCustomerDetailVo weCustomerDetail = qwCustomerClient.getCustomerDetail(query).getData();

        if (weCustomerDetail != null && weCustomerDetail.getExternalContact() != null) {

            WeCustomerDetailVo.ExternalContact externalContact = weCustomerDetail.getExternalContact();
            //????????????
            WeCustomer weCustomer = new WeCustomer();
            weCustomer.setId(SnowFlakeUtil.nextId());
            weCustomer.setExternalUserid(externalContact.getExternalUserId());
            weCustomer.setCustomerName(externalContact.getName());
            weCustomer.setCustomerType(externalContact.getType());
            weCustomer.setAvatar(externalContact.getAvatar());
            weCustomer.setGender(externalContact.getGender());
            weCustomer.setUnionid(externalContact.getUnionId());
            weCustomer.setDelFlag(0);
            weCustomer.setAddUserId(userId);

            List<WeCustomerFollowUserEntity> followUserList = weCustomerDetail.getFollowUser();
            if (CollectionUtil.isNotEmpty(followUserList)) {
                WeCustomerFollowUserEntity followUserEntity = followUserList.stream().filter(followUserInfo -> followUserInfo.getUserId().equals(userId)).findFirst().get();

                weCustomer.setState(followUserEntity.getState());
                weCustomer.setAddTime(new Date(followUserEntity.getCreateTime() * 1000L));
                weCustomer.setAddMethod(followUserEntity.getAddWay());
                weCustomer.setCorpName(followUserEntity.getRemarkCompany());
                weCustomer.setRemarkName(followUserEntity.getRemark());
                weCustomer.setOtherDescr(followUserEntity.getDescription());
                weCustomer.setPhone(String.join(",", Optional.ofNullable(followUserEntity.getRemarkMobiles()).orElseGet(ArrayList::new)));
                //????????????
                List<WeCustomerDetailVo.ExternalUserTag> tags = followUserEntity.getTags();
                if (CollectionUtil.isNotEmpty(tags)) {
                    List<WeFlowerCustomerTagRel> tagRels = tags.stream().map(tagInfo -> WeFlowerCustomerTagRel.builder()
                            .id(SnowFlakeUtil.nextId())
                            .externalUserid(externalContact.getExternalUserId())
                            .tagId(tagInfo.getTagId())
                            .userId(followUserEntity.getUserId())
                            .isCompanyTag(true)
                            .delFlag(0)
                            .build()).collect(Collectors.toList());
                    iWeFlowerCustomerTagRelService.batchAddOrUpdate(ListUtil.toList(tagRels));
                }
            }
            this.baseMapper.batchAddOrUpdate(ListUtil.toList(weCustomer));
            //????????????
            iWeCustomerTrajectoryService.createEditTrajectory(
                    weCustomer.getExternalUserid(),weCustomer.getAddUserId(),TrajectorySceneType.TRAJECTORY_TITLE_BJBQ.getType(),null
            );
        }
    }

    @Override
    public   Map<String, SysUser> findCurrentTenantSysUser() {
        Map<String, SysUser> sysUserMap=new HashMap<>();
        List<SysUser> sysUsers = this.baseMapper.findCurrentTenantSysUser();
        if(CollectionUtil.isNotEmpty(sysUsers)){
            sysUserMap=sysUsers.stream().collect(Collectors.toMap(SysUser::getWeUserId, Function.identity(), (key1, key2) -> key2));
        }
        return sysUserMap;
    }

    @Override
    public SysUser findSysUserInfoByWeUserId(String weUserId) {
        return this.baseMapper.findSysUserInfoByWeUserId(weUserId);
    }

    @Override
    public SysUser findCurrentSysUserInfo(Long userId) {
        return this.baseMapper.findCurrentSysUserInfo(userId);
    }

    @Async
    @Override
    public void updateCustomerUnionId(String unionId) {
        List<WeCustomer> list = list(new LambdaQueryWrapper<WeCustomer>().eq(WeCustomer::getUnionid, unionId).eq(WeCustomer::getDelFlag, 0));
        if(CollectionUtil.isEmpty(list)){
            UnionidToExternalUserIdQuery query = new UnionidToExternalUserIdQuery(SecurityUtils.getCorpId(),SecurityUtils.getWxLoginUser().getOpenId(),unionId);
            UnionidToExternalUserIdVo externalUserIdVo = qwCustomerClient.unionIdToExternalUserId3rd(query).getData();
            if(externalUserIdVo != null && CollectionUtil.isNotEmpty(externalUserIdVo.getExternalUseridInfo())){
                List<UnionidToExternalUserIdVo.UnionIdToExternalUserIdList> externalUseridInfo = externalUserIdVo.getExternalUseridInfo();
                String externaklUserId = externalUseridInfo.stream().filter(item -> ObjectUtil.equal(SecurityUtils.getCorpId(), item.getCorpId())).map(UnionidToExternalUserIdVo.UnionIdToExternalUserIdList::getExternalUserId).findFirst().orElseGet(null);
                WeCustomer weCustomer = new WeCustomer();
                weCustomer.setUnionid(unionId);
                update(weCustomer,new LambdaQueryWrapper<WeCustomer>().eq(WeCustomer::getExternalUserid,externaklUserId).eq(WeCustomer::getDelFlag,0));
            }
        }
    }

    @Override
    public List<WeCustomer> getCustomerListByCondition(WeCustomersQuery query) {
        return this.baseMapper.getCustomerListByCondition(query);
    }


    @Override
    public List<SysUser> findAllSysUser() {
        List<SysUser> allSysUser = this.baseMapper.findAllSysUser();
        if(CollectionUtil.isEmpty(allSysUser)){
            allSysUser=new ArrayList<>();
        }
        return allSysUser;
    }

    @Override
    public List<String> findWeUserIds() {
        return this.baseMapper.findWeUserIds();
    }

    @Override
    public List<WeCustomersVo> findWeCustomerList(List<String> customerIds) {
        return this.baseMapper.findWeCustomerList(customerIds);
    }


}
