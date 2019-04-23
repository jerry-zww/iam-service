package io.choerodon.iam.app.service.impl;

import static io.choerodon.iam.infra.common.utils.SagaTopic.Project.PROJECT_DISABLE;
import static io.choerodon.iam.infra.common.utils.SagaTopic.Project.PROJECT_UPDATE;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.Page;
import io.choerodon.iam.infra.dto.OrganizationDTO;
import io.choerodon.iam.infra.dto.ProjectDTO;
import io.choerodon.iam.infra.dto.UserDTO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.choerodon.asgard.saga.annotation.Saga;
import io.choerodon.asgard.saga.dto.StartInstanceDTO;
import io.choerodon.asgard.saga.feign.SagaClient;
import io.choerodon.core.exception.CommonException;
import io.choerodon.core.iam.ResourceLevel;
import io.choerodon.core.oauth.CustomUserDetails;
import io.choerodon.core.oauth.DetailsHelper;
import io.choerodon.iam.api.dto.payload.ProjectEventPayload;
import io.choerodon.iam.app.service.ProjectService;
import io.choerodon.iam.domain.repository.OrganizationRepository;
import io.choerodon.iam.domain.repository.ProjectRepository;
import io.choerodon.iam.domain.repository.UserRepository;

/**
 * @author flyleft
 */
@Service
@RefreshScope
public class ProjectServiceImpl implements ProjectService {

    private ProjectRepository projectRepository;

    private UserRepository userRepository;

    private OrganizationRepository organizationRepository;

    @Value("${choerodon.devops.message:false}")
    private boolean devopsMessage;

    @Value("${spring.application.name:default}")
    private String serviceName;

    private SagaClient sagaClient;

    private final ObjectMapper mapper = new ObjectMapper();

    public ProjectServiceImpl(ProjectRepository projectRepository,
                              UserRepository userRepository,
                              OrganizationRepository organizationRepository,
                              SagaClient sagaClient) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.sagaClient = sagaClient;
    }

    @Override
    public ProjectDTO queryProjectById(Long projectId) {
        return projectRepository.selectByPrimaryKey(projectId);
    }

    @Override
    public Page<UserDTO> pagingQueryTheUsersOfProject(Long id, Long userId, String email, int page, int size, String param) {
        return userRepository.pagingQueryUsersByProjectId(id, userId, email, page, size, param);
    }

    @Transactional(rollbackFor = CommonException.class)
    @Override
    @Saga(code = PROJECT_UPDATE, description = "iam更新项目", inputSchemaClass = ProjectEventPayload.class)
    public ProjectDTO update(ProjectDTO projectDTO) {
//        ProjectDO project = ConvertHelper.convert(projectDTO, ProjectDO.class);
        if (devopsMessage) {
            ProjectDTO dto = new ProjectDTO();
            CustomUserDetails details = DetailsHelper.getUserDetails();
            UserDTO user = userRepository.selectByLoginName(details.getUsername());
            ProjectDTO newProject = projectRepository.selectByPrimaryKey(projectDTO.getId());
            OrganizationDTO organizationDTO = organizationRepository.selectByPrimaryKey(newProject.getOrganizationId());
            ProjectEventPayload projectEventMsg = new ProjectEventPayload();
            projectEventMsg.setUserName(details.getUsername());
            projectEventMsg.setUserId(user.getId());
            if (organizationDTO != null) {
                projectEventMsg.setOrganizationCode(organizationDTO.getCode());
                projectEventMsg.setOrganizationName(organizationDTO.getName());
            }
            projectEventMsg.setProjectId(newProject.getId());
            projectEventMsg.setProjectCode(newProject.getCode());
            ProjectDTO newDTO = projectRepository.updateSelective(projectDTO);
            projectEventMsg.setProjectName(projectDTO.getName());
            projectEventMsg.setImageUrl(newDTO.getImageUrl());
            BeanUtils.copyProperties(newDTO, dto);
            try {
                String input = mapper.writeValueAsString(projectEventMsg);
                sagaClient.startSaga(PROJECT_UPDATE, new StartInstanceDTO(input, "project", "" + newProject.getId(), ResourceLevel.PROJECT.value(), projectDTO.getId()));
            } catch (Exception e) {
                throw new CommonException("error.projectService.update.event", e);
            }
            return dto;
        } else {
            return projectRepository.updateSelective(projectDTO);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Saga(code = PROJECT_DISABLE, description = "iam停用项目", inputSchemaClass = ProjectEventPayload.class)
    public ProjectDTO disableProject(Long id) {
        ProjectDTO project = projectRepository.selectByPrimaryKey(id);
        project.setEnabled(false);
        ProjectDTO projectDTO = disableAndSendEvent(project);
        return projectDTO;
    }

    private ProjectDTO disableAndSendEvent(ProjectDTO project) {
//        ProjectDTO dto;
        if (devopsMessage) {
//            projectE = new ProjectE();
            ProjectEventPayload payload = new ProjectEventPayload();
            payload.setProjectId(project.getId());
            project = projectRepository.updateSelective(project);
//            BeanUtils.copyProperties(projectRepository.updateSelective(project), projectE);
            try {
                String input = mapper.writeValueAsString(payload);
                sagaClient.startSaga(PROJECT_DISABLE, new StartInstanceDTO(input, "project", "" + payload.getProjectId(), ResourceLevel.PROJECT.value(), project.getId()));
            } catch (Exception e) {
                throw new CommonException("error.projectService.disableProject.event", e);
            }
        } else {
            project = projectRepository.updateSelective(project);
        }
        return project;
    }

    @Override
    public List<Long> listUserIds(Long projectId) {
        return projectRepository.listUserIds(projectId);
    }

    @Override
    public List<ProjectDTO> queryByIds(Set<Long> ids) {
        if (ids.isEmpty()) {
            return new ArrayList<>();
        } else {
            return projectRepository.queryByIds(ids);
        }
    }

    @Override
    public Boolean checkProjCode(String code) {
        ProjectDTO projectDTO = new ProjectDTO();
        projectDTO.setCode(code);
        return projectRepository.selectOne(projectDTO) == null;
    }
}
