package com.linkwechat.web.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.collection.ListUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.linkwechat.common.annotation.SynchRecord;
import com.linkwechat.common.constant.SynchRecordConstants;
import com.linkwechat.common.constant.UserConstants;
import com.linkwechat.common.constant.WeConstans;
import com.linkwechat.common.context.SecurityContextHolder;
import com.linkwechat.common.core.domain.AjaxResult;
import com.linkwechat.common.core.domain.dto.SysUserDTO;
import com.linkwechat.common.core.domain.entity.SysDept;
import com.linkwechat.common.core.domain.entity.SysRole;
import com.linkwechat.common.core.domain.entity.SysUser;
import com.linkwechat.common.core.domain.entity.SysUserDept;
import com.linkwechat.common.core.domain.model.LoginUser;
import com.linkwechat.common.core.page.PageDomain;
import com.linkwechat.common.enums.CorpUserEnum;
import com.linkwechat.common.enums.RoleType;
import com.linkwechat.common.enums.UserTypes;
import com.linkwechat.common.exception.CustomException;
import com.linkwechat.common.utils.SecurityUtils;
import com.linkwechat.common.utils.SnowFlakeUtil;
import com.linkwechat.common.utils.StringUtils;
import com.linkwechat.config.rabbitmq.RabbitMQSettingConfig;
import com.linkwechat.domain.wecom.query.user.WeUserListQuery;
import com.linkwechat.domain.wecom.query.user.WeUserQuery;
import com.linkwechat.domain.wecom.vo.user.WeUserDetailVo;
import com.linkwechat.domain.wecom.vo.user.WeUserListVo;
import com.linkwechat.fegin.QwCorpClient;
import com.linkwechat.fegin.QwUserClient;
import com.linkwechat.service.IWeLeaveUserService;
import com.linkwechat.web.domain.SysPost;
import com.linkwechat.web.domain.SysUserPost;
import com.linkwechat.web.domain.SysUserRole;
import com.linkwechat.web.domain.vo.UserRoleVo;
import com.linkwechat.web.domain.vo.UserVo;
import com.linkwechat.web.mapper.*;
import com.linkwechat.web.service.*;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ?????? ???????????????
 *
 * @author ruoyi
 */
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements ISysUserService {
    private static final Logger log = LoggerFactory.getLogger(SysUserServiceImpl.class);

    @Resource
    private SysUserMapper userMapper;

    @Resource
    private SysRoleMapper roleMapper;

    @Resource
    private SysUserDeptMapper userDeptMapper;

    @Resource
    private SysPostMapper postMapper;

    @Resource
    private SysUserRoleMapper userRoleMapper;

    @Resource
    private SysUserPostMapper userPostMapper;

    @Resource
    private SysRoleDeptMapper roleDeptMapper;

    @Resource
    private ISysConfigService configService;

    @Resource
    private ISysUserDeptService sysUserDeptService;

    @Resource
    private QwUserClient userClient;

    @Resource
    private QwCorpClient corpClient;

    @Autowired
    private IWeLeaveUserService iWeLeaveUserService;

    @Autowired
    private RabbitMQSettingConfig rabbitMQSettingConfig;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ISysDeptService sysDeptService;

    @Autowired
    private ISysRoleService sysRoleService;


    /**
     * ????????????????????????????????????
     *
     * @param user ????????????
     * @return ????????????????????????
     */
    @Override
    public List<SysUser> selectUserList(SysUser user) {
        return userMapper.selectUserList(user);
    }

    @Override
    public List<UserVo> selectUserVoList(SysUser sysUser,PageDomain pageDomain) {
        List<UserVo> userDeptList = userMapper.selectUserDeptList(sysUser,pageDomain);

        if(CollectionUtil.isNotEmpty(userDeptList)){

            List<UserRoleVo> userRoleList = userMapper.selectUserRoleList(userDeptList.stream().map(UserVo::getUserId).collect(Collectors.toList()));

            Map<Long, List<UserRoleVo>> userRoleMap = userRoleList.stream().collect(Collectors.groupingBy(UserRoleVo::getUserId));
            Set<Long> roleIdSet = new HashSet<>();
            Map<Long, List<SysDept>> map = new HashMap<>();
            userDeptList.stream().filter(Objects::nonNull).forEach(u -> {
                if (CollectionUtils.isNotEmpty(userRoleMap.get(u.getUserId()))) {
                    List<Long> roleIdList = userRoleMap.get(u.getUserId()).stream().map(UserRoleVo::getRoleId).collect(Collectors.toList());
                    u.setRoles(userRoleMap.get(u.getUserId()));
                    roleIdSet.addAll(roleIdList);
                }
            });
            roleIdSet.stream().filter(Objects::nonNull).forEach(roleId -> {
                List<SysDept> depts = roleDeptMapper.selectRoleDeptList(roleId);
                map.put(roleId, depts);
            });
            return userDeptList.stream().peek(user -> {
                if (CollectionUtils.isNotEmpty(userRoleMap.get(user.getUserId()))) {
                    Set<SysDept> s = new HashSet<>();
                    userRoleMap.get(user.getUserId()).forEach(r -> s.addAll(new HashSet<>(map.get(r.getRoleId()))));
                    user.setRoleDepts(new ArrayList<>(s));
                }
            }).collect(Collectors.toList());
        }

        return new ArrayList<>();
    }

    @Override
    public int selectCountUserDeptList(SysUser sysUser) {
        return userMapper.selectCountUserDeptList(sysUser);
    }

    /**
     * ???????????????????????????
     *
     * @param userName ?????????
     * @return ??????????????????
     */
    @Override
    public SysUser selectUserByUserName(String userName) {
        return userMapper.selectUserByUserName(userName);
    }

    /**
     * ????????????ID????????????
     *
     * @param userId ??????ID
     * @return ??????????????????
     */
    @Override
    public SysUser selectUserById(Long userId) {
        return userMapper.selectUserById(userId);
    }

    /**
     * ???????????????????????????
     *
     * @param userName ?????????
     * @return ??????
     */
    @Override
    public String selectUserRoleGroup(String userName) {
        List<SysRole> list = roleMapper.selectRolesByUserName(userName);
        StringBuffer idsStr = new StringBuffer();
        for (SysRole role : list) {
            idsStr.append(role.getRoleName()).append(",");
        }
        if (StringUtils.isNotEmpty(idsStr.toString())) {
            return idsStr.substring(0, idsStr.length() - 1);
        }
        return idsStr.toString();
    }

    /**
     * ???????????????????????????
     *
     * @param userName ?????????
     * @return ??????
     */
    @Override
    public String selectUserPostGroup(String userName) {
        List<SysPost> list = postMapper.selectPostsByUserName(userName);
        StringBuffer idsStr = new StringBuffer();
        for (SysPost post : list) {
            idsStr.append(post.getPostName()).append(",");
        }
        if (StringUtils.isNotEmpty(idsStr.toString())) {
            return idsStr.substring(0, idsStr.length() - 1);
        }
        return idsStr.toString();
    }

    /**
     * ??????????????????????????????
     *
     * @param userName ????????????
     * @return ??????
     */
    @Override
    public String checkUserNameUnique(String userName) {
        int count = userMapper.checkUserNameUnique(userName);
        if (count > 0) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * ??????????????????????????????
     *
     * @param user ????????????
     * @return
     */
    @Override
    public String checkPhoneUnique(SysUser user) {
        Long userId = StringUtils.isNull(user.getUserId()) ? -1L : user.getUserId();
        SysUser info = userMapper.checkPhoneUnique(user.getPhoneNumber());
        if (StringUtils.isNotNull(info) && info.getUserId().longValue() != userId.longValue()) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * ??????email????????????
     *
     * @param user ????????????
     * @return
     */
    @Override
    public String checkEmailUnique(SysUser user) {
        Long userId = StringUtils.isNull(user.getUserId()) ? -1L : user.getUserId();
        SysUser info = userMapper.checkEmailUnique(user.getEmail());
        if (StringUtils.isNotNull(info) && info.getUserId().longValue() != userId.longValue()) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * ??????????????????????????????
     *
     * @param user ????????????
     */
    @Override
    public void checkUserAllowed(SysUser user) {
        if (StringUtils.isNotNull(user.getUserId()) && user.isAdmin()) {
            throw new CustomException("????????????????????????????????????");
        }
    }

    /**
     * ????????????????????????
     *
     * @param user ????????????
     * @return ??????
     */
    @Override
    @Transactional
    public int insertUser(SysUser user) {
        // ??????????????????
        int rows = userMapper.insertUser(user);
        // ????????????????????????
        insertUserPost(user);
        // ???????????????????????????
        insertUserRole(user);
        return rows;
    }

    /**
     * ????????????????????????
     *
     * @param sysUser ????????????
     * @return ??????
     */
    @Override
    @Transactional
    public void updateUser(SysUserDTO sysUser) {
        WeUserQuery query = new WeUserQuery();
        query.setUserid(sysUser.getWeUserId());
        query.setCorpid(sysUser.getCorpId());
        AjaxResult<WeUserDetailVo> result = userClient.getUserInfo(query);
        List<Long> delUserDeptId = new ArrayList<>();
        List<SysUserDept> userDeptList = new ArrayList<>();
        if (result.getData() != null) {
            WeUserDetailVo vo = result.getData();
            SysUser user = sysUserGenerator(vo);
            SysUser userExist = selectUserByWeUserId(user.getWeUserId());
            if (userExist != null) {
                user.setUserId(userExist.getUserId());
            }
            for (int i = 0; i < vo.getDepartment().size(); i++) {
                delUserDeptId.addAll(new LambdaQueryChainWrapper<>(userDeptMapper).eq(SysUserDept::getWeUserId, vo.getUserId()).eq(SysUserDept::getDeptId, vo.getDepartment().get(i)).list().stream().map(SysUserDept::getUserDeptId).collect(Collectors.toList()));
                userDeptList.add(userDeptGenerator(vo, i));
            }
            sysUserDeptService.removeByIds(delUserDeptId);
            sysUserDeptService.saveOrUpdateBatch(userDeptList);
            userMapper.updateUser(user);
        }

    }

    /**
     * ??????????????????
     *
     * @param user ????????????
     * @return ??????
     */
    @Override
    public int updateUserStatus(SysUser user) {
        return userMapper.updateUser(user);
    }

    /**
     * ????????????????????????
     *
     * @param user ????????????
     * @return ??????
     */
    @Override
    public int updateUserProfile(SysUser user) {
        return userMapper.updateUser(user);
    }

    /**
     * ??????????????????
     *
     * @param userName ?????????
     * @param avatar   ????????????
     * @return ??????
     */
    @Override
    public boolean updateUserAvatar(String userName, String avatar) {
        return userMapper.updateUserAvatar(userName, avatar) > 0;
    }

    /**
     * ??????????????????
     *
     * @param user ????????????
     * @return ??????
     */
    @Override
    public int resetPwd(SysUser user) {
        return userMapper.updateUser(user);
    }

    /**
     * ??????????????????
     *
     * @param userName ?????????
     * @param password ??????
     * @return ??????
     */
    @Override
    public int resetUserPwd(String userName, String password) {
        return userMapper.resetUserPwd(userName, password);
    }

    /**
     * ????????????????????????
     *
     * @param user ????????????
     */
    public void insertUserRole(SysUser user) {
        Long[] roles = user.getRoleIds();
        if (StringUtils.isNotNull(roles)) {
            // ???????????????????????????
            List<SysUserRole> list = new ArrayList<SysUserRole>();
            for (Long roleId : roles) {
                SysUserRole ur = new SysUserRole();
                ur.setUserId(user.getUserId());
                ur.setRoleId(roleId);
                list.add(ur);
            }
            if (list.size() > 0) {
                userRoleMapper.batchUserRole(list);
            }
        }
    }

    /**
     * ????????????????????????
     *
     * @param user ????????????
     */
    public void insertUserPost(SysUser user) {
        Long[] posts = user.getPostIds();
        if (StringUtils.isNotNull(posts)) {
            // ???????????????????????????
            List<SysUserPost> list = new ArrayList<SysUserPost>();
            for (Long postId : posts) {
                SysUserPost up = new SysUserPost();
                up.setUserId(user.getUserId());
                up.setPostId(postId);
                list.add(up);
            }
            if (list.size() > 0) {
                userPostMapper.batchUserPost(list);
            }
        }
    }

    /**
     * ????????????ID????????????
     *
     * @param userId ??????ID
     * @return ??????
     */
    @Override
    public int deleteUserById(Long userId) {
        // ???????????????????????????
        userRoleMapper.deleteUserRoleByUserId(userId);
        // ????????????????????????
        userPostMapper.deleteUserPostByUserId(userId);
        return userMapper.deleteUserById(userId);
    }

    /**
     * ????????????????????????
     *
     * @param userIds ?????????????????????ID
     * @return ??????
     */
    @Override
    public int deleteUserByIds(Long[] userIds) {
        for (Long userId : userIds) {
            checkUserAllowed(new SysUser(userId));
        }
        userMapper.deleteUserByIds(userIds);
        //???????????????????????????????????????


        return userMapper.deleteUserByIds(userIds);
    }

    /**
     * ??????????????????
     *
     * @param userList        ??????????????????
     * @param isUpdateSupport ????????????????????????????????????????????????????????????
     * @param operName        ????????????
     * @return ??????
     */
    @Override
    public String importUser(List<SysUser> userList, Boolean isUpdateSupport, String operName) {
        if (StringUtils.isNull(userList) || userList.size() == 0) {
            throw new CustomException("?????????????????????????????????");
        }
        int successNum = 0;
        int failureNum = 0;
        StringBuilder successMsg = new StringBuilder();
        StringBuilder failureMsg = new StringBuilder();
        String password = configService.selectConfigByKey("sys.user.initPassword");
        for (SysUser user : userList) {
            try {
                // ??????????????????????????????
                SysUser u = userMapper.selectUserByUserName(user.getUserName());
                if (StringUtils.isNull(u)) {
                    user.setPassword(SecurityUtils.encryptPassword(password));
                    user.setCreateBy(operName);
                    this.insertUser(user);
                    successNum++;
                    successMsg.append("<br/>" + successNum + "????????? " + user.getUserName() + " ????????????");
                } else if (isUpdateSupport) {
                    user.setUpdateBy(operName);
                    this.updateUser((SysUserDTO) user);
                    successNum++;
                    successMsg.append("<br/>" + successNum + "????????? " + user.getUserName() + " ????????????");
                } else {
                    failureNum++;
                    failureMsg.append("<br/>" + failureNum + "????????? " + user.getUserName() + " ?????????");
                }
            } catch (Exception e) {
                failureNum++;
                String msg = "<br/>" + failureNum + "????????? " + user.getUserName() + " ???????????????";
                failureMsg.append(msg + e.getMessage());
                log.error(msg, e);
            }
        }
        if (failureNum > 0) {
            failureMsg.insert(0, "?????????????????????????????? " + failureNum + " ??????????????????????????????????????????");
            throw new CustomException(failureMsg.toString());
        } else {
            successMsg.insert(0, "????????????????????????????????????????????? " + successNum + " ?????????????????????");
        }
        return successMsg.toString();
    }


    @Override
    public List<SysUser> syncWeUser(Long deptId, String corpId) {
        WeUserListQuery query = new WeUserListQuery();
        query.setDepartment_id(String.valueOf(deptId));
        query.setFetch_child(0);
        query.setCorpid(corpId);
        query.setFetch_child(1);
        AjaxResult<WeUserListVo> vo = userClient.getList(query);
        List<SysUserDept> userDeptList = new ArrayList<>();
        List<Long> delUserDeptId = new ArrayList<>();
        List<SysUser> userList = vo.getData().getUserList().stream().map(u -> {
            SysUser user = sysUserGenerator(u);
            for (int i = 0; i < u.getDepartment().size(); i++) {
                delUserDeptId.addAll(new LambdaQueryChainWrapper<>(userDeptMapper).eq(SysUserDept::getWeUserId, u.getUserId()).eq(SysUserDept::getDeptId, u.getDepartment().get(i)).list().stream().map(SysUserDept::getUserDeptId).collect(Collectors.toList()));
                userDeptList.add(userDeptGenerator(u, i));
            }
            return user;
        }).collect(Collectors.toList());

        if(CollectionUtil.isNotEmpty(userList)){
            this.baseMapper.batchAddOrUpdate(userList);

        }

        //??????????????????,?????????????????????????????????,?????????????????????????????????
        List<SysUserRole> allUserRole = userRoleMapper.findAllUserRole();
        if(CollectionUtil.isEmpty(allUserRole)){
            allUserRole=new ArrayList<>();
        }
        Collection<Long> subtractUserId = CollectionUtils.subtract(
                userList.stream().map(SysUser::getUserId).collect(Collectors.toList())
                , allUserRole.stream().map(SysUserRole::getUserId).collect(Collectors.toList()));
        if(CollectionUtil.isNotEmpty(subtractUserId)){
            List<SysUserRole> sysUserRoles=new ArrayList<>();
            //?????????????????????????????????
            //??????????????????,??????????????????
            Optional<RoleType> optionalRoleType = RoleType.of(5);
            if (optionalRoleType.isPresent()) {
                //????????????????????????????????????
                List<SysRole> defaultRoles = sysRoleService.selectRoleList(
                        new SysRole(optionalRoleType.get().getSysRoleKey()));
                if (CollectionUtil.isNotEmpty(defaultRoles)) {
                    subtractUserId.stream().forEach(userId->{
                        sysUserRoles.add(SysUserRole.builder()
                                        .roleId(defaultRoles.stream().findFirst().get().getRoleId())
                                        .userId(userId)
                                .build());
                    });
                    if(CollectionUtil.isNotEmpty(sysUserRoles)){
                        userRoleMapper.batchUserRole(sysUserRoles);
                    }
                }
            }
        }


        Map<String, Long> userIdMap = userList.stream().collect(Collectors.toMap(SysUser::getWeUserId, SysUser::getUserId));
        sysUserDeptService.removeByIds(delUserDeptId);
        sysUserDeptService.saveBatch(userDeptList.stream().peek(userDept -> {
            userDept.setUserId(userIdMap.get(userDept.getWeUserId()));
        }).collect(Collectors.toList()));
        return userList;
    }

    @Override
    public SysUser selectUserByWeUserId(String weUserId){
         return this.baseMapper.selectUserByWeUserId(weUserId);
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void leaveUser(String[] weUserIds) {
        List<SysUser> weUsers = new ArrayList<>();
        CollectionUtil.newArrayList(weUserIds).forEach(weUserId -> {
            SysUser sysUser = this.getOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getWeUserId, weUserId));
            if (null != sysUser) {
                sysUser.setIsAllocate(CorpUserEnum.NO_IS_ALLOCATE.getKey());
                sysUser.setDimissionTime(new Date());
                weUsers.add(sysUser);
            }
            if (this.updateBatchById(weUsers) && this.removeByIds(weUsers.stream().map(SysUser::getUserId).collect(Collectors.toList()))) {
                //???????????????????????????????????????
                iWeLeaveUserService.createWaitAllocateCustomerAndGroup(weUserId.split(","));
            }

        });

    }

    @Override
    @SynchRecord(synchType = SynchRecordConstants.SYNCH_MAIL_LIST)
    public void syncUserAndDept() {

        LoginUser loginUser = SecurityUtils.getLoginUser();
        rabbitTemplate.convertAndSend(rabbitMQSettingConfig.getWeSyncEx(), rabbitMQSettingConfig.getUserDepartRk(), JSONObject.toJSONString(loginUser));


    }

    @Override
    public void syncUserAndDeptHandler(String msg) {
        LoginUser loginUser = JSONObject.parseObject(msg, LoginUser.class);
        SecurityContextHolder.setCorpId(loginUser.getCorpId());
        SecurityContextHolder.setUserName(loginUser.getUserName());
        SecurityContextHolder.setUserId(String.valueOf(loginUser.getSysUser().getUserId()));
        SecurityContextHolder.setUserType(loginUser.getUserType());

        List<SysDept> deptList = sysDeptService.syncWeDepartment(loginUser.getCorpId());
        deptList.forEach(dept -> {
            this.syncWeUser(dept.getDeptId(), loginUser.getCorpId());
        });

    }

    @Override
    @Transactional
    public int addUser(SysUserDTO sysUser) {
        WeUserQuery query = new WeUserQuery();
        query.setUserid(sysUser.getWeUserId());
        query.setCorpid(sysUser.getCorpId());
        AjaxResult<WeUserDetailVo> result = userClient.getUserInfo(query);
        List<SysUserDept> userDeptList = new ArrayList<>();
        if (result.getData() != null) {
            WeUserDetailVo vo = result.getData();
            SysUser user = sysUserGenerator(vo);
            for (int i = 0; i < vo.getDepartment().size(); i++) {
                userDeptList.add(userDeptGenerator(vo, i));
            }
            boolean flag = save(user);
            SysRole role = new LambdaQueryChainWrapper<>(roleMapper).eq(SysRole::getRoleKey, RoleType.WECOME_USER_TYPE_CY.getSysRoleKey())
                    .eq(SysRole::getDelFlag, 0).one();
            if (role != null) {
                userRoleMapper.batchUserRole(ListUtil.toList(
                        SysUserRole.builder().roleId(role.getRoleId()).userId(user.getUserId()).build()));
            }
            sysUserDeptService.saveBatch(userDeptList.stream().peek(userDept -> {
                userDept.setUserId(user.getUserId());
            }).collect(Collectors.toList()));
            return flag ? 1 : 0;
        }
        return 0;
    }

    @Override
    @Transactional
    public void editUserRole(SysUserDTO user) {

        SysUser sysUser = this.getById(user.getUserId());
        if(null != sysUser){
            //??????????????????
            userRoleMapper.deleteUserRoleByUserId(sysUser.getUserId());
            List<Long> roleIds = ListUtil.toList(user.getRoleIds());
            if(CollectionUtil.isNotEmpty(roleIds)){

                List<SysRole> sysRoles = roleMapper.selectBatchIds(roleIds);

                if(CollectionUtil.isNotEmpty(sysRoles)){
                    //???????????????
                    List<SysUserRole> sysUserRoles=new ArrayList<>();
                    sysRoles.stream().forEach(sysUserRole->{
                        //???????????????
                        sysUserRoles.add(
                                SysUserRole.builder()
                                        .userId(sysUser.getUserId())
                                        .roleId(sysUserRole.getRoleId())
                                        .build()
                        );
                    });
                    userRoleMapper.batchUserRole(sysUserRoles);

                    Set<String> roleKeys
                            = sysRoles.stream().map(SysRole::getRoleKey).collect(Collectors.toSet());

                    if(roleKeys.contains(RoleType.WECOME_USER_TYPE_FJGLY.getSysRoleKey())){//???????????????
                        sysUser.setUserType(UserTypes.USER_TYPE_FJ_ADMIN.getSysRoleKey());
                    }else if(roleKeys.contains(RoleType.WECOME_USER_TYPE_CY.getSysRoleKey())){//????????????
                        sysUser.setUserType(UserTypes.USER_TYPE_COMMON_USER.getSysRoleKey());
                    }else{
                        sysUser.setUserType(UserTypes.USER_TYPE_SELFBUILD_USER.getSysRoleKey());//????????????
                    }
                    this.updateById(sysUser);
                }



            }
        }
    }

    @Override
    public void getUserSensitiveInfo(String userTicket) {
        getUserSensitiveInfo(SecurityUtils.getUserId(),userTicket);
    }

    @Async
    @Override
    public void getUserSensitiveInfo(Long userId, String userTicket) {
        SysUser sysUser = getById(userId);
        if (StringUtils.isNotEmpty(sysUser.getAvatar())) {
            return;
        }
        WeUserQuery query = new WeUserQuery();
        query.setUser_ticket(userTicket);
        WeUserDetailVo data = userClient.getUserSensitiveInfo(query).getData();

        sysUser.setAvatar(data.getAvatar());
        sysUser.setSex(String.valueOf(data.getGender()));
        sysUser.setPhoneNumber(data.getMobile());
        sysUser.setBizMail(data.getBizMail());
        sysUser.setEmail(data.getEmail());
        sysUser.setQrCode(data.getQrCode());
        sysUser.setAddress(data.getAddress());
        updateById(sysUser);
    }


    private SysUserDept userDeptGenerator(WeUserDetailVo u, int index) {
        SysUserDept userDept = new SysUserDept();
        userDept.setWeUserId(u.getUserId());
        userDept.setOpenUserid(u.getOpenUserId());
        userDept.setDeptId(Long.parseLong(String.valueOf(u.getDepartment().get(index))));
        if (u.getOrder() != null) {
            userDept.setOrderInDept(String.valueOf(u.getOrder().get(index)));
        } else {
            userDept.setOrderInDept("0");
        }
        if (u.getIsLeaderInDept() != null) {
            userDept.setLeaderInDept(u.getIsLeaderInDept().get(index));
        } else {
            userDept.setLeaderInDept(0);
        }
        return userDept;
    }

    private SysUser sysUserGenerator(WeUserDetailVo u) {
        log.debug("WeUserDetailVo: {}", u);
        SysUser user = null;

        if (user == null) {
            user = new SysUser();
        }
        user.setUserId(SnowFlakeUtil.nextId());
        user.setWeUserId(u.getUserId());
        if (u.getMainDepartment() != null) {
            user.setDeptId(Long.parseLong(String.valueOf(u.getMainDepartment())));
        } else {
            if (u.getDepartment().size() != 0) {
                user.setDeptId(Long.parseLong(String.valueOf(u.getDepartment().get(0))));
            } else {
                user.setDeptId(1L);
            }
        }
        user.setUserName(u.getName());
        user.setPosition(u.getPosition());
        user.setPhoneNumber(u.getMobile());
        user.setSex(String.valueOf(u.getGender()));
        user.setEmail(u.getEmail());
        user.setBizMail(u.getBizMail());
        if (u.getDirectLeader() != null) {
            user.setLeader(String.join(",", u.getDirectLeader()));
        }
        user.setAvatar(u.getAvatar());
        user.setThumbAvatar(u.getThumbAvatar());
        user.setTelephone(u.getTelephone());
        user.setNickName(u.getAlias());
        user.setExtAttr(u.getExtAttr());
        if (u.getStatus() != null) {
            user.setWeUserStatus(String.valueOf(u.getStatus()));
        } else {
            user.setWeUserStatus("1");
        }
        user.setQrCode(u.getQrCode());
        user.setExternalProfile(u.getExternalProfile());
        user.setExternalPosition(u.getExternalPosition());
        user.setAddress(u.getAddress());
        user.setCreateBy(SecurityUtils.getUserName());
        user.setCreateById(SecurityUtils.getUserId());
        user.setCreateTime(new Date());
        user.setUpdateBy(SecurityUtils.getUserName());
        user.setUpdateById(SecurityUtils.getUserId());
        user.setUpdateTime(new Date());
        user.setStatus("0");
        return user;
    }
}
