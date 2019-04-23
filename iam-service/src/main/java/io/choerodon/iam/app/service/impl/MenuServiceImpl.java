package io.choerodon.iam.app.service.impl;

import java.util.*;

import io.choerodon.base.enums.MenuType;
import io.choerodon.base.enums.ProjectCategory;
import io.choerodon.base.enums.ResourceType;
import io.choerodon.iam.infra.asserts.DetailsHelperAssert;
import io.choerodon.iam.infra.asserts.MenuAssertHelper;
import io.choerodon.iam.infra.dto.MenuDTO;
import io.choerodon.iam.infra.dto.ProjectDTO;
import io.choerodon.iam.infra.mapper.MenuMapper;
import io.choerodon.mybatis.entity.Criteria;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import io.choerodon.core.exception.CommonException;
import io.choerodon.core.oauth.CustomUserDetails;
import io.choerodon.iam.app.service.MenuService;
import io.choerodon.iam.domain.repository.ProjectRepository;

/**
 * @author wuguokai
 * @author superlee
 */
@Component
public class MenuServiceImpl implements MenuService {

    private ProjectRepository projectRepository;

    private MenuMapper menuMapper;
    private MenuAssertHelper menuAssertHelper;


    public MenuServiceImpl(ProjectRepository projectRepository,
                           MenuMapper menuMapper,
                           MenuAssertHelper menuAssertHelper) {
        this.projectRepository = projectRepository;
        this.menuMapper = menuMapper;
        this.menuAssertHelper = menuAssertHelper;
    }

    @Override
    public MenuDTO query(Long id) {
        return menuMapper.selectByPrimaryKey(id);
    }

    @Override
    public MenuDTO create(MenuDTO menuDTO) {
        preCreate(menuDTO);
        menuMapper.insertSelective(menuDTO);
        return menuDTO;
    }

    private void preCreate(MenuDTO menuDTO) {
        menuAssertHelper.codeExisted(menuDTO.getCode(), menuDTO.getResourceLevel(), menuDTO.getType());
        if (menuDTO.getSort() == null) {
            menuDTO.setSort(0);
        }
        if (menuDTO.getParentId() == null) {
            menuDTO.setParentId(0L);
        }
        String level = menuDTO.getResourceLevel();
        if (!ResourceType.contains(level)) {
            throw new CommonException("error.illegal.level");
        }
        String type = menuDTO.getType();
        if (!MenuType.contains(type)) {
            throw new CommonException("error.menu.illegal.type", type);
        }
    }

    @Override
    public void delete(Long id) {
        MenuDTO dto = menuAssertHelper.menuNotExisted(id);
        if (dto.getDefault()) {
            throw new CommonException("error.menu.default");
        }
        menuMapper.deleteByPrimaryKey(id);
    }

    @Override
    public MenuDTO update(Long id, MenuDTO menuDTO) {
        MenuDTO dto = menuAssertHelper.menuNotExisted(id);
        if (dto.getDefault()) {
            throw new CommonException("error.menu.default");
        }
        menuDTO.setId(id);
        Criteria criteria = new Criteria();
        criteria.update("name", "icon", "page_permission_code", "search_condition", "category");
        menuMapper.updateByPrimaryKeyOptions(menuDTO, criteria);
        return menuMapper.selectByPrimaryKey(id);
    }

    @Override
    public List<MenuDTO> menus(String level, Long sourceId) {
        if (!ResourceType.contains(level)) {
            throw new CommonException("error.illegal.menu.level", level);
        }
        CustomUserDetails userDetails = DetailsHelperAssert.userDetailNotExisted();
        Long userId = userDetails.getUserId();
        boolean isAdmin = userDetails.getAdmin();
        List<MenuDTO> topMenus = getTopMenus(level);
        Set<MenuDTO> menus;
        if (isAdmin) {
            MenuDTO dto = new MenuDTO();
            if (ResourceType.isProject(level)) {
                menus = new LinkedHashSet<>(
                        menuMapper.queryProjectMenusWithCategoryByRootUser(getProjectCategory(level, sourceId)));
            } else {
                dto.setResourceLevel(level);
                menus = new LinkedHashSet<>(menuMapper.select(dto));
            }
        } else {
            String category = getProjectCategory(level, sourceId);
            menus = new HashSet<>(
                    menuMapper.selectMenusAfterCheckPermission(userId, level, sourceId, category, "user"));
            //查类型为menu的菜单
            MenuDTO dto = new MenuDTO();
            dto.setType(MenuType.MENU.value());
            dto.setResourceLevel(level);
            menus.addAll(menuMapper.select(dto));
        }
        topMenus.forEach(top -> toTreeMenu(top, menus));
        topMenus.sort(Comparator.comparing(MenuDTO::getSort));
        return topMenus;
    }

    private String getProjectCategory(String level, Long sourceId) {
        String category = null;
        if (ResourceType.isProject(level)) {
            ProjectDTO project = projectRepository.selectByPrimaryKey(sourceId);
            if (project != null && !ProjectCategory.isAgile(project.getCategory())) {
                category = ProjectCategory.PROGRAM.value();
            }
        }
        return category;
    }

    @Override
    public MenuDTO menuConfig(String code, String level, String type) {
        MenuDTO dto = new MenuDTO();
        dto.setCode(code);
        dto.setResourceLevel(level);
        dto.setType(type);
        MenuDTO menu = menuMapper.selectOne(dto);
        if (menu == null) {
            throw new CommonException("error.menu.top.not.existed");
        }
        Set<MenuDTO> menus = new HashSet<>(menuMapper.selectMenusWithPermission(level));
        toTreeMenu(menu, menus);
        return menu;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void saveMenuConfig(String level, List<MenuDTO> menus) {
        menus.forEach(m -> saveOrUpdate(m, level));
    }

    private void saveOrUpdate(MenuDTO menu, String level) {
        String code = menu.getCode();
        if (StringUtils.isEmpty(code)) {
            throw new CommonException("error.menu.code.empty");
        }
        MenuDTO example = new MenuDTO();
        example.setCode(code);
        MenuDTO dto = menuMapper.selectOne(example);
        if (dto == null) {
            //do insert
            validate(menu, level);
            menuMapper.insertSelective(menu);
        } else {
            //do update
            dto.setSort(menu.getSort());
            dto.setParentId(menu.getParentId());
            menuMapper.updateByPrimaryKey(dto);
        }
        List<MenuDTO> subMenus = menu.getMenus();
        if (subMenus != null && !subMenus.isEmpty()) {
            subMenus.forEach(m -> saveOrUpdate(m, level));
        }
    }

    private void validate(MenuDTO menu, String level) {
        menu.setResourceLevel(level);
        menu.setType(MenuType.MENU.value());
        String code = menu.getCode();
        if (StringUtils.isEmpty(menu.getName())) {
            throw new CommonException("error.menu.name.empty", code);
        }
        if (menu.getParentId() == null) {
            throw new CommonException("error.menu.patentId.null", code);
        }
        if (StringUtils.isEmpty(menu.getPagePermissionCode())) {
            throw new CommonException("error.menu.pagePermissionCode.empty", code);
        }
        if (menu.getSort() == null) {
            menu.setSort(0);
        }
        if (menu.getDefault() == null) {
            menu.setDefault(true);
        }
    }

    private List<MenuDTO> getTopMenus(String level) {
        MenuDTO example = new MenuDTO();
        example.setResourceLevel(level);
        example.setType(MenuType.TOP.value());
        List<MenuDTO> topMenus = menuMapper.select(example);
        if (topMenus.isEmpty()) {
            throw new CommonException("error.top.menu.not.existed", level);
        }
        return topMenus;
    }

    private void toTreeMenu(MenuDTO parentMenu, Set<MenuDTO> menus) {
        Long id = parentMenu.getId();
        List<MenuDTO> subMenus = new ArrayList<>();
        menus.forEach(menu -> {
            if (menu.getParentId().equals(id)) {
                subMenus.add(menu);
                if (MenuType.isMenu(menu.getType())) {
                    toTreeMenu(menu, menus);
                }
            }
        });
        //移除type=menu但是没有菜单项的菜单
        subMenus.forEach(menu -> {
            List<MenuDTO> menuList = menu.getMenus();
            boolean removed = (menuList == null || menuList.isEmpty());
            if (MenuType.isMenu(menu.getType()) && removed) {
                subMenus.remove(menu);
            }
        });
        subMenus.sort(Comparator.comparing(MenuDTO::getSort));
        parentMenu.setMenus(subMenus);
    }

    @Override
    public void check(MenuDTO menu) {
        if (StringUtils.isEmpty(menu.getCode())) {
            throw new CommonException("error.menu.code.empty");
        }
        if (StringUtils.isEmpty(menu.getResourceLevel())) {
            throw new CommonException("error.menu.level.empty");
        }
        if (StringUtils.isEmpty(menu.getType())) {
            throw new CommonException("error.menu.type.empty");
        }
        checkCode(menu);
    }


    private void checkCode(MenuDTO menu) {
        boolean createCheck = menu.getId() == null;
        MenuDTO dto = new MenuDTO();
        dto.setCode(menu.getCode());
        dto.setResourceLevel(menu.getResourceLevel());
        dto.setType(menu.getType());
        if (createCheck) {
            if (!menuMapper.select(dto).isEmpty()) {
                throw new CommonException("error.menu.code-level-type.exist");
            }
        } else {
            Long id = menu.getId();
            MenuDTO menuDTO = menuMapper.selectOne(dto);
            Boolean existed = menuDTO != null && !id.equals(menuDTO.getId());
            if (existed) {
                throw new CommonException("error.menu.code-level-type.exist");
            }
        }
    }
}
