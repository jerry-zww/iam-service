package io.choerodon.iam.api.eventhandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.choerodon.asgard.saga.annotation.SagaTask;
import io.choerodon.core.exception.CommonException;
import io.choerodon.core.ldap.DirectoryType;
import io.choerodon.iam.api.dto.ProjectCategoryDTO;
import io.choerodon.iam.api.dto.payload.OrganizationCreateEventPayload;
import io.choerodon.iam.api.dto.payload.OrganizationRegisterEventPayload;
import io.choerodon.iam.app.service.*;
import io.choerodon.iam.infra.dto.*;
import io.choerodon.iam.infra.mapper.ProjectCategoryMapper;
import io.choerodon.iam.infra.mapper.ProjectMapCategoryMapper;
import io.choerodon.iam.infra.mapper.ProjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Random;

import static io.choerodon.iam.app.service.impl.OrganizationProjectServiceImpl.PROJECT_DEFAULT_CATEGORY;
import static io.choerodon.iam.infra.common.utils.SagaTopic.Organization.*;


/**
 * @author wuguokai
 */
@Component
public class OrganizationListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrganizationListener.class);
    private static final String ALPHANUMERIC = "abcdefghijklmnopqrstuvwxyz1234567890";

    private PasswordPolicyService passwordPolicyService;
    private OrganizationService organizationService;
    private LdapService ldapService;
    private ProjectMapper projectMapper;
    private UserService userService;
    private OrganizationProjectService organizationProjectService;

    private ProjectCategoryMapper projectCategoryMapper;

    private ProjectMapCategoryMapper projectMapCategoryMapper;

    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${choerodon.devops.message:false}")
    private boolean devopsMessage;
    @Value("${lock.expireTime:3600}")
    private Integer lockedExpireTime;
    @Value("${max.checkCaptcha:3}")
    private Integer maxCheckCaptcha;
    @Value("${max.errorTime:5}")
    private Integer maxErrorTime;
    @Value("${choerodon.category.enabled:false}")
    private Boolean categoryEnable;

    public OrganizationListener(LdapService ldapService, PasswordPolicyService passwordPolicyService,
                                OrganizationService organizationService,
                                UserService userService, OrganizationProjectService organizationProjectService,
                                ProjectMapper projectMapper,
                                ProjectCategoryMapper projectCategoryMapper,
                                ProjectMapCategoryMapper projectMapCategoryMapper) {
        this.passwordPolicyService = passwordPolicyService;
        this.organizationService = organizationService;
        this.ldapService = ldapService;
        this.userService = userService;
        this.organizationProjectService = organizationProjectService;
        this.projectMapper = projectMapper;
        this.projectCategoryMapper = projectCategoryMapper;
        this.projectMapCategoryMapper = projectMapCategoryMapper;
    }

    @SagaTask(code = TASK_ORG_CREATE, sagaCode = ORG_CREATE, seq = 1, description = "iam接收org服务创建组织事件")
    public OrganizationCreateEventPayload create(String message) throws IOException {
        OrganizationCreateEventPayload organizationEventPayload = mapper.readValue(message, OrganizationCreateEventPayload.class);
        LOGGER.info("iam create the organization trigger task,payload: {}", organizationEventPayload);
        Long orgId = organizationEventPayload.getId();
        OrganizationDTO organizationDTO = organizationService.queryOrganizationById(orgId);
        if (organizationDTO == null) {
            throw new CommonException("error.organization.not exist");
        }
        createLdap(orgId, organizationDTO.getName());
        createPasswordPolicy(orgId, organizationDTO.getCode(), organizationDTO.getName());
        return organizationEventPayload;
    }


    @SagaTask(code = TASK_ORG_REGISTER_INIT_ORG, sagaCode = ORG_REGISTER, seq = 20, description = "创建默认密码策略，创建默认ldap配置")
    public OrganizationRegisterEventPayload registerInitOrg(String message) throws IOException {
        OrganizationRegisterEventPayload organizationRegisterEventPayload =
                mapper.readValue(message, OrganizationRegisterEventPayload.class);
        LOGGER.info("Iam receives Saga event '{}' and triggers task: {},payload: {}",
                ORG_REGISTER, TASK_ORG_REGISTER_INIT_ORG, organizationRegisterEventPayload);
        Long orgId = organizationRegisterEventPayload.getOrganization().getId();
        OrganizationDTO organizationDTO = organizationService.queryOrganizationById(orgId);
        if (organizationDTO == null) {
            throw new CommonException("error.organization.not exist");
        }
        userService.updateUserDisabled(organizationRegisterEventPayload.getUser().getId());
        createLdap(orgId, organizationDTO.getName());
        createPasswordPolicy(orgId, organizationDTO.getCode(), organizationDTO.getName());
        return organizationRegisterEventPayload;
    }

    @SagaTask(code = TASK_ORG_REGISTER_INIT_PROJ, sagaCode = ORG_REGISTER, seq = 80, description = "创建项目")
    public OrganizationRegisterEventPayload registerInitProj(String message) throws IOException {
        OrganizationRegisterEventPayload organizationRegisterEventPayload =
                mapper.readValue(message, OrganizationRegisterEventPayload.class);
        LOGGER.info("Iam receives Saga event '{}' and triggers task: {},payload: {}",
                ORG_REGISTER, TASK_ORG_REGISTER_INIT_PROJ, organizationRegisterEventPayload);
        ProjectDTO dto = new ProjectDTO();
        dto.setName("公司内销平台");
        dto.setType("type/develop-platform");
        dto.setOrganizationId(organizationRegisterEventPayload.getOrganization().getId());
        dto.setCode(randomProjCode());
        dto.setEnabled(true);
        dto = organizationProjectService.create(dto);
        if (categoryEnable) {
            assignDefaultCategoriesToProjects(dto.getId());
        }
        organizationRegisterEventPayload.setProject(
                new OrganizationRegisterEventPayload.Project(dto.getId(), dto.getCode(), dto.getName()));
        return organizationRegisterEventPayload;
    }

    private ProjectMapCategoryDTO assignDefaultCategoriesToProjects(Long projectId) {
        ProjectMapCategoryDTO projectMapCategoryDTO = new ProjectMapCategoryDTO();

        ProjectCategoryDTO projectCategoryDTO = new ProjectCategoryDTO();
        projectCategoryDTO.setCode(PROJECT_DEFAULT_CATEGORY);
        projectCategoryDTO = projectCategoryMapper.selectOne(projectCategoryDTO);
        if (projectCategoryDTO != null) {
            projectMapCategoryDTO.setCategoryId(projectCategoryDTO.getId());
        }
        projectMapCategoryDTO.setProjectId(projectId);

        if (projectMapCategoryMapper.insert(projectMapCategoryDTO) != 1) {
            throw new CommonException("error.project.map.category.insert");
        }
        return projectMapCategoryDTO;
    }

    private void createPasswordPolicy(Long orgId, String code, String name) {
        try {
            LOGGER.info("### begin create password policy of organization {} ", orgId);
            PasswordPolicyDTO passwordPolicyDTO = new PasswordPolicyDTO();
            passwordPolicyDTO.setOrganizationId(orgId);
            passwordPolicyDTO.setCode(code);
            passwordPolicyDTO.setName(name);
            passwordPolicyDTO.setMaxCheckCaptcha(maxCheckCaptcha);
            passwordPolicyDTO.setMaxErrorTime(maxErrorTime);
            passwordPolicyDTO.setLockedExpireTime(lockedExpireTime);
            //默认开启登陆安全策略，设置为
            passwordPolicyDTO.setEnableSecurity(true);
            passwordPolicyDTO.setEnableCaptcha(true);
            passwordPolicyDTO.setMaxCheckCaptcha(3);
            passwordPolicyDTO.setEnableLock(true);
            passwordPolicyDTO.setMaxErrorTime(5);
            passwordPolicyDTO.setLockedExpireTime(600);
            passwordPolicyService.create(orgId, passwordPolicyDTO);
        } catch (Exception e) {
            LOGGER.error("create password policy error of organizationId: {}, exception: {}", orgId, e);
        }
    }

    private void createLdap(Long orgId, String name) {
        try {
            LOGGER.info("### begin create ldap of organization {} ", orgId);
            LdapDTO ldapDTO = new LdapDTO();
            ldapDTO.setOrganizationId(orgId);
            ldapDTO.setName(name);
            ldapDTO.setServerAddress("");
            ldapDTO.setPort("389");
            ldapDTO.setDirectoryType(DirectoryType.OPEN_LDAP.value());
            ldapDTO.setEnabled(true);
            ldapDTO.setUseSSL(false);
            ldapDTO.setObjectClass("person");
            ldapDTO.setSagaBatchSize(500);
            ldapDTO.setConnectionTimeout(10);
            ldapDTO.setAccount("test");
            ldapDTO.setPassword("test");
            ldapDTO.setUuidField("entryUUID");
            ldapService.create(orgId, ldapDTO);
        } catch (Exception e) {
            LOGGER.error("create ldap error of organization, organizationId: {}, exception: {}", orgId, e);
        }
    }

    /**
     * 生成随机项目编码
     * 1.数据库中未存在
     * 2.格式："proj-"+8位随机小写字母或数字
     *
     * @return 符合条件的项目编码
     */
    private String randomProjCode() {
        String projectCode = "";
        boolean flag = false;
        while (!flag) {
            projectCode = "proj-" + generateString(false, 8);
            ProjectDTO projectDTO = new ProjectDTO();
            projectDTO.setCode(projectCode);
            ProjectDTO projByCode = projectMapper.selectOne(projectDTO);
            if (projByCode == null) {
                flag = true;
            }
        }
        return projectCode;
    }

    private String generateString(Boolean isChinese, int length) {
        char[] text = new char[length];
        for (int i = 0; i < length; i++) {
            if (isChinese) {
                text[i] = (char) (0x4e00 + (int) (Math.random() * (0x5ea5 - 0x4ea5 + 1)));
            } else {
                text[i] = ALPHANUMERIC.charAt(new Random().nextInt(ALPHANUMERIC.length()));
            }
        }
        return new String(text);
    }
}
