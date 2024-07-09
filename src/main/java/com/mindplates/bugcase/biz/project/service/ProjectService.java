package com.mindplates.bugcase.biz.project.service;

import com.mindplates.bugcase.biz.project.dto.ProjectDTO;
import com.mindplates.bugcase.biz.project.dto.ProjectListDTO;
import com.mindplates.bugcase.biz.project.dto.ProjectMessageChannelDTO;
import com.mindplates.bugcase.biz.project.dto.ProjectReleaseDTO;
import com.mindplates.bugcase.biz.project.dto.ProjectUserDTO;
import com.mindplates.bugcase.biz.project.entity.Project;
import com.mindplates.bugcase.biz.project.entity.ProjectMessageChannel;
import com.mindplates.bugcase.biz.project.entity.ProjectToken;
import com.mindplates.bugcase.biz.project.repository.ProjectMessageChannelRepository;
import com.mindplates.bugcase.biz.project.repository.ProjectRepository;
import com.mindplates.bugcase.biz.project.repository.ProjectTokenRepository;
import com.mindplates.bugcase.biz.project.repository.ProjectUserRepository;
import com.mindplates.bugcase.biz.space.dto.SpaceDTO;
import com.mindplates.bugcase.biz.space.repository.SpaceRepository;
import com.mindplates.bugcase.biz.testcase.dto.TestcaseGroupDTO;
import com.mindplates.bugcase.biz.testcase.dto.TestcaseTemplateDTO;
import com.mindplates.bugcase.biz.testcase.dto.TestcaseTemplateItemDTO;
import com.mindplates.bugcase.biz.testcase.repository.TestcaseItemRepository;
import com.mindplates.bugcase.biz.testcase.service.TestcaseService;
import com.mindplates.bugcase.biz.testrun.dto.TestrunDTO;
import com.mindplates.bugcase.biz.testrun.repository.TestrunMessageChannelRepository;
import com.mindplates.bugcase.biz.testrun.repository.TestrunTestcaseGroupTestcaseItemRepository;
import com.mindplates.bugcase.biz.testrun.repository.TestrunTestcaseGroupTestcaseRepository;
import com.mindplates.bugcase.biz.testrun.service.TestrunService;
import com.mindplates.bugcase.biz.user.dto.UserDTO;
import com.mindplates.bugcase.common.code.UserRoleCode;
import com.mindplates.bugcase.common.exception.ServiceException;
import com.mindplates.bugcase.common.util.MappingUtil;
import com.mindplates.bugcase.framework.config.CacheConfig;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectService {

    private final SpaceRepository spaceRepository;

    private final ProjectRepository projectRepository;
    private final ProjectFileService projectFileService;
    private final TestrunService testrunService;
    private final TestcaseService testcaseService;
    private final TestcaseItemRepository testcaseItemRepository;
    private final TestrunTestcaseGroupTestcaseItemRepository testrunTestcaseGroupTestcaseItemRepository;
    private final ProjectUserRepository projectUserRepository;
    private final ProjectTokenRepository projectTokenRepository;
    private final ProjectReleaseService projectReleaseService;
    private final TestrunTestcaseGroupTestcaseRepository testrunTestcaseGroupTestcaseRepository;
    private final MappingUtil mappingUtil;
    private final ProjectMessageChannelRepository projectMessageChannelRepository;
    private final TestrunMessageChannelRepository testrunMessageChannelRepository;

    // @PersistenceContext
    // private EntityManager em;

    public ProjectService(SpaceRepository spaceRepository, ProjectRepository projectRepository, ProjectFileService projectFileService,
        @Lazy TestrunService testrunService, TestcaseItemRepository testcaseItemRepository,
        TestrunTestcaseGroupTestcaseItemRepository testrunTestcaseGroupTestcaseItemRepository, ProjectUserRepository projectUserRepository,
        ProjectTokenRepository projectTokenRepository, MappingUtil mappingUtil, TestcaseService testcaseService,
        ProjectReleaseService projectReleaseService, TestrunTestcaseGroupTestcaseRepository testrunTestcaseGroupTestcaseRepository, ProjectMessageChannelRepository projectMessageChannelRepository,
        TestrunMessageChannelRepository testrunMessageChannelRepository) {
        this.spaceRepository = spaceRepository;
        this.projectRepository = projectRepository;
        this.projectFileService = projectFileService;
        this.testrunService = testrunService;
        this.testcaseItemRepository = testcaseItemRepository;
        this.testrunTestcaseGroupTestcaseItemRepository = testrunTestcaseGroupTestcaseItemRepository;
        this.projectTokenRepository = projectTokenRepository;
        this.projectUserRepository = projectUserRepository;
        this.mappingUtil = mappingUtil;
        this.testcaseService = testcaseService;
        this.projectReleaseService = projectReleaseService;
        this.testrunTestcaseGroupTestcaseRepository = testrunTestcaseGroupTestcaseRepository;
        this.projectMessageChannelRepository = projectMessageChannelRepository;
        this.testrunMessageChannelRepository = testrunMessageChannelRepository;
    }

    @Transactional
    public ProjectDTO createProjectInfo(String spaceCode, ProjectDTO projectInfo, Long userId) {

        if (existByName(spaceCode, projectInfo.getName())) {
            throw new ServiceException("error.project.duplicated");
        }

        // projectInfo.getTestcaseTemplates()에서 default가 2개 이상이면 Exception
        long defaultTemplateCount = projectInfo.getTestcaseTemplates().stream().filter(TestcaseTemplateDTO::isDefaultTemplate).count();
        if (defaultTemplateCount < 1) {
            throw new ServiceException("error.default.template.not.exist");

        } else if (defaultTemplateCount > 1) {
            throw new ServiceException("error.default.template.count.over");
        }

        Long spaceId = spaceRepository.findIdByCode(spaceCode).orElseThrow(() -> new ServiceException(HttpStatus.NOT_FOUND));
        projectInfo.setSpace(SpaceDTO.builder().id(spaceId).build());
        projectInfo.setTestcaseSeq(0);
        projectInfo.setTestcaseGroupSeq(0);

        // 기본 어드민 유저로 사용자 추가
        ProjectUserDTO projectUser = ProjectUserDTO.builder().project(projectInfo).user(UserDTO.builder().id(userId).build()).role(UserRoleCode.ADMIN).build();
        projectInfo.setUsers(Collections.singletonList(projectUser));
        Project project = projectInfo.toEntity();
        return new ProjectDTO(projectRepository.save(project));
    }

    @Cacheable(key = "{#spaceCode,#projectId}", value = CacheConfig.PROJECT)
    public ProjectDTO selectProjectInfo(String spaceCode, Long projectId) {
        Long id = projectRepository.findIdBySpaceCodeAndId(spaceCode, projectId).orElseThrow(() -> new ServiceException(HttpStatus.NOT_FOUND));
        if (id == null) {
            throw new ServiceException(HttpStatus.NOT_FOUND);
        }

        Project project = projectRepository.findById(projectId).orElseThrow(() -> new ServiceException(HttpStatus.NOT_FOUND));
        return new ProjectDTO(project);
    }

    public List<ProjectListDTO> selectSpaceProjectList(Long spaceId) {
        List<Project> projectList = projectRepository.findAllBySpaceId(spaceId);
        return projectList.stream().map((ProjectListDTO::new)).collect(Collectors.toList());
    }

    public List<ProjectDTO> selectSpaceProjectDetailList(Long spaceId) {
        List<Project> projectList = projectRepository.findAllBySpaceId(spaceId);
        return projectList.stream().map((ProjectDTO::new)).collect(Collectors.toList());
    }

    public List<ProjectListDTO> selectSpaceMyProjectList(String spaceCode, Long userId) {
        /*
        String jpql = "SELECT new Project(p.id, p.name, p.description, p.activated, p.token, p.aiEnabled, p.testcaseGroupSeq, p.testcaseSeq, p.testcaseSeq, (SELECT COUNT(tr.id) FROM Testrun tr WHERE tr.project.id = p.id), (SELECT COUNT(tc.id) FROM Testcase tc WHERE tc.project.id = p.id)) FROM Project p  INNER JOIN ProjectUser pu ON p.id = pu.project.id WHERE p.space.code = :spaceCode AND pu.user.id = :userId";
        TypedQuery<Project> query = em.createQuery(jpql, Project.class);
        query.setParameter("spaceCode", spaceCode);
        query.setParameter("userId", userId);
        List<Project> projectList = query.getResultList();
        return projectList.stream().map((project -> new ProjectDTO(project, false))).collect(Collectors.toList());
        */
        List<Project> projectList = projectRepository.findAllBySpaceCodeAndUsersUserId(spaceCode, userId);
        return projectList.stream().map((ProjectListDTO::new)).collect(Collectors.toList());
    }



    public String selectProjectName(String spaceCode, Long projectId) {
        return projectRepository.findNameBySpaceCodeAndId(spaceCode, projectId).orElseThrow(() -> new ServiceException(HttpStatus.NOT_FOUND));
    }


    public Long selectProjectId(String token) {
        ProjectToken projectToken = projectTokenRepository.findByToken(token)
            .orElseThrow(() -> new ServiceException(HttpStatus.NOT_FOUND, "project.token.invalid"));
        return projectToken.getProject().getId();
    }


    public boolean existByName(String spaceCode, String name) {
        Long count = projectRepository.countBySpaceCodeAndName(spaceCode, name);
        return count > 0;
    }


    @Transactional
    @CacheEvict(key = "{#spaceCode,#updateProjectInfo.id}", value = CacheConfig.PROJECT)
    public ProjectDTO updateProjectInfo(String spaceCode, ProjectDTO updateProjectInfo, Long targetReleaseId) {
        ProjectDTO projectInfo = this.selectProjectInfo(spaceCode, updateProjectInfo.getId());
        projectInfo.setName(updateProjectInfo.getName());
        projectInfo.setDescription(updateProjectInfo.getDescription());
        projectInfo.setToken(updateProjectInfo.getToken());
        projectInfo.setActivated(updateProjectInfo.isActivated());
        projectInfo.setAiEnabled(updateProjectInfo.isAiEnabled());
        if (updateProjectInfo.getTestcaseGroupSeq() != null) {
            projectInfo.setTestcaseGroupSeq(updateProjectInfo.getTestcaseGroupSeq());
        }

        if (updateProjectInfo.getTestrunSeq() != null) {
            projectInfo.setTestrunSeq(updateProjectInfo.getTestrunSeq());
        }

        if (updateProjectInfo.getTestcaseSeq() != null) {
            projectInfo.setTestcaseSeq(updateProjectInfo.getTestcaseSeq());
        }

        List deleteTestcaseTemplateIds = new ArrayList();
        updateProjectInfo.getTestcaseTemplates()
            .stream()
            .filter((testcaseTemplate -> "D".equals(testcaseTemplate.getCrud())))
            .forEach(testcaseTemplateDTO -> {
                deleteTestcaseTemplateIds.add(testcaseTemplateDTO.getId());

                testrunTestcaseGroupTestcaseRepository.deleteByTestcaseTemplateId(testcaseTemplateDTO.getId());

            });

        projectInfo.setTestcaseTemplates(
            updateProjectInfo.getTestcaseTemplates()
                .stream()
                .filter((testcaseTemplate -> !"D".equals(testcaseTemplate.getCrud())))
                .map(testcaseTemplate -> {
                    testcaseTemplate.getTestcaseTemplateItems().stream().filter(TestcaseTemplateItemDTO::isDeleted).forEach(testcaseTemplateItem -> {
                        testcaseItemRepository.deleteByTestcaseId(testcaseTemplateItem.getId());
                        testrunTestcaseGroupTestcaseItemRepository.deleteByTestcaseTemplateItemId(testcaseTemplateItem.getId());
                    });

                    testcaseTemplate.setTestcaseTemplateItems(
                        testcaseTemplate.getTestcaseTemplateItems()
                            .stream()
                            .filter(testcaseTemplateItem -> !testcaseTemplateItem.isDeleted())
                            .collect(Collectors.toList())
                    );
                    return testcaseTemplate;
                }).collect(Collectors.toList()));

        AtomicBoolean foundDefaultTemplate = new AtomicBoolean(false);
        projectInfo.getTestcaseTemplates().forEach((testcaseTemplate -> {
            if (foundDefaultTemplate.get() && testcaseTemplate.isDefaultTemplate()) {
                testcaseTemplate.setDefaultTemplate(false);
            }
            if (!foundDefaultTemplate.get() && testcaseTemplate.isDefaultTemplate()) {
                foundDefaultTemplate.set(true);
            }
        }));

        if (!projectInfo.getTestcaseTemplates().isEmpty() && !foundDefaultTemplate.get()) {
            projectInfo.getTestcaseTemplates().get(0).setDefaultTemplate(true);
        }

        for (TestcaseGroupDTO testcaseGroup : projectInfo.getTestcaseGroups()) {
            testcaseGroup.getTestcases().removeIf(testcaseDTO -> deleteTestcaseTemplateIds.contains(testcaseDTO.getTestcaseTemplate().getId()));
        }

        projectInfo.setUsers(
            updateProjectInfo.getUsers().stream().filter(projectUser -> projectUser.getCrud() == null || !projectUser.getCrud().equals("D"))
                .collect(Collectors.toList()));

        if (!projectInfo.getProjectReleases().isEmpty()) {
            if (targetReleaseId == null) {
                projectInfo.getProjectReleases().forEach(projectReleaseDTO -> {
                    projectReleaseDTO.setIsTarget(false);
                });
            } else {
                projectInfo.getProjectReleases().forEach(projectReleaseDTO -> {
                    projectReleaseDTO.setIsTarget(projectReleaseDTO.getId().equals(targetReleaseId));
                });
            }

        }

        // projectInfo의 messageChannels에 아이템 중 updateProjectInfo의 messageChannels에 없는 ID 모음
        List<Long> deleteMessageChannelIds = projectInfo.getMessageChannels().stream()
            .filter((projectMessageChannel -> updateProjectInfo.getMessageChannels().stream()
                .noneMatch((updateMessageChannel -> updateMessageChannel.getId().equals(projectMessageChannel.getId())))))
            .map(ProjectMessageChannelDTO::getId)
            .collect(Collectors.toList());

        // projectInfo의 messageChannels에 아이템 중 deleteMessageChannelIds에 id가 없다면, 목록에서 제거
        projectInfo.getMessageChannels().removeIf((projectMessageChannel -> deleteMessageChannelIds.contains(projectMessageChannel.getId())));

        // deleteMessageChannelIds에 있는 ID를 가진 testrunMessageChannel 삭제
        deleteMessageChannelIds.forEach((testrunMessageChannelRepository::deleteByProjectMessageChannelId));

        // updateProjectInfo의 messageChannel을 projectInfo에 업데이트
        updateProjectInfo.getMessageChannels().forEach((updateMessageChannel -> {

            ProjectMessageChannelDTO projectMessageChannel = projectInfo.getMessageChannels().stream()
                .filter((messageChannel) -> messageChannel.getId() != null && messageChannel.getId().equals(updateMessageChannel.getId()))
                .findFirst()
                .orElse(null);

            if (projectMessageChannel != null) {
                projectMessageChannel.setMessageChannel(updateMessageChannel.getMessageChannel());
            } else {
                projectInfo.getMessageChannels().add(updateMessageChannel);
            }
        }));

        Project updateResult = mappingUtil.convert(projectInfo, Project.class);
        return new ProjectDTO(projectRepository.save(updateResult));

    }

    @Transactional
    @CacheEvict(key = "{#spaceCode,#project.id}", value = CacheConfig.PROJECT)
    public void deleteProjectInfo(String spaceCode, ProjectDTO project) {
        projectFileService.deleteProjectFile(project.getId());
        for (ProjectReleaseDTO projectRelease : project.getProjectReleases()) {
            projectReleaseService.deleteProjectRelease(spaceCode, project.getId(), projectRelease.getId());
        }
        List<TestrunDTO> testruns = testrunService.selectProjectAllTestrunList(spaceCode, project.getId());
        testruns.forEach((testrunDTO -> testrunService.deleteProjectTestrunInfo(spaceCode, project.getId(), testrunDTO.getId())));
        projectRepository.delete(mappingUtil.convert(project, Project.class));
    }


    public Long selectSpaceProjectCount(Long spaceId) {
        return projectRepository.countBySpaceId(spaceId);
    }

    @Transactional
    @CacheEvict(key = "{#spaceCode,#projectId}", value = CacheConfig.PROJECT)
    public void deleteProjectUser(String spaceCode, Long projectId, Long userId) {
        Project project = projectRepository.findById(projectId).orElseThrow(() -> new ServiceException(HttpStatus.NOT_FOUND));
        project.getUsers().removeIf((projectUser -> projectUser.getUser().getId().equals(userId)));
        long adminCount = project.getUsers().stream().filter((projectUser -> projectUser.getRole().equals(UserRoleCode.ADMIN))).count();
        if (adminCount < 1L) {
            throw new ServiceException("no.project.admin.exist");
        }

        projectRepository.save(project);
    }

    public boolean selectIsProjectMember(Long projectId, Long userId) {
        return projectUserRepository.existsByProjectIdAndUserId(projectId, userId);
    }

    public boolean selectIsProjectAdmin(Long projectId, Long userId) {
        return projectUserRepository.existsByProjectIdAndUserIdAndRole(projectId, userId, UserRoleCode.ADMIN);
    }

    public List<ProjectMessageChannelDTO> selectProjectMessageChannels(Long projectId) {
        List<ProjectMessageChannel> projectMessageChannels = projectMessageChannelRepository.findAllByProjectId(projectId);
        return projectMessageChannels.stream().map(ProjectMessageChannelDTO::new).collect(Collectors.toList());
    }

    @Transactional
    public void updateProjectAiEnabledFalse() {
        projectRepository.updateProjectAiEnable();
    }


}
