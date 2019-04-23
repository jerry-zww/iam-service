package io.choerodon.iam.infra.dto;

import io.choerodon.mybatis.annotation.MultiLanguage;
import io.choerodon.mybatis.annotation.MultiLanguageField;
import io.choerodon.mybatis.entity.BaseDTO;

import javax.persistence.*;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.util.List;
import java.util.Objects;

/**
 * @author superlee
 * @since 2019-04-15
 */
@MultiLanguage
@Table(name = "iam_menu_b")
public class MenuDTO extends BaseDTO {

    private static final String CODE_REGULAR_EXPRESSION = "^[a-z]([-.a-z0-9]*[a-z0-9])$";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotEmpty(message = "error.menu.code.empty")
    @Pattern(regexp = CODE_REGULAR_EXPRESSION, message = "error.menu.code.illegal")
    private String code;
    @MultiLanguageField
    @NotEmpty(message = "error.menu.name.empty")
    private String name;
    private String pagePermissionCode;
    @NotNull(message = "error.menu.parentId.null")
    private Long parentId;
    @NotEmpty(message = "error.menu.resourceLevel.empty")
    private String resourceLevel;
    @NotEmpty(message = "error.menu.type.empty")
    private String type;
    @NotEmpty
    private String serviceCode;
    private Integer sort;
    private Boolean isDefault;
    @NotEmpty(message = "error.menu.icon.empty")
    private String icon;
    private String category;
    private String searchCondition;
    @Transient
    private List<PermissionDTO> permissions;
    @Transient
    private List<MenuDTO> menus;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPagePermissionCode() {
        return pagePermissionCode;
    }

    public void setPagePermissionCode(String pagePermissionCode) {
        this.pagePermissionCode = pagePermissionCode;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getResourceLevel() {
        return resourceLevel;
    }

    public void setResourceLevel(String resourceLevel) {
        this.resourceLevel = resourceLevel;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }

    public Boolean getDefault() {
        return isDefault;
    }

    public void setDefault(Boolean aDefault) {
        isDefault = aDefault;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSearchCondition() {
        return searchCondition;
    }

    public void setSearchCondition(String searchCondition) {
        this.searchCondition = searchCondition;
    }

    public List<PermissionDTO> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<PermissionDTO> permissions) {
        this.permissions = permissions;
    }

    public List<MenuDTO> getMenus() {
        return menus;
    }

    public void setMenus(List<MenuDTO> menus) {
        this.menus = menus;
    }

    public String getServiceCode() {
        return serviceCode;
    }

    public void setServiceCode(String serviceCode) {
        this.serviceCode = serviceCode;
    }

    @Override
    public String toString() {
        return "MenuDTO{" +
                "id=" + id +
                ", code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", pagePermissionCode='" + pagePermissionCode + '\'' +
                ", parentId=" + parentId +
                ", resourceLevel='" + resourceLevel + '\'' +
                ", type='" + type + '\'' +
                ", serviceCode='" + serviceCode + '\'' +
                ", sort=" + sort +
                ", isDefault=" + isDefault +
                ", icon='" + icon + '\'' +
                ", category='" + category + '\'' +
                ", searchCondition='" + searchCondition + '\'' +
                ", permissions=" + permissions +
                ", menus=" + menus +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MenuDTO dto = (MenuDTO) o;
        return Objects.equals(id, dto.id) &&
                Objects.equals(code, dto.code) &&
                Objects.equals(name, dto.name) &&
                Objects.equals(pagePermissionCode, dto.pagePermissionCode) &&
                Objects.equals(parentId, dto.parentId) &&
                Objects.equals(resourceLevel, dto.resourceLevel) &&
                Objects.equals(type, dto.type) &&
                Objects.equals(serviceCode, dto.serviceCode) &&
                Objects.equals(sort, dto.sort) &&
                Objects.equals(isDefault, dto.isDefault) &&
                Objects.equals(icon, dto.icon) &&
                Objects.equals(category, dto.category) &&
                Objects.equals(searchCondition, dto.searchCondition) &&
                Objects.equals(permissions, dto.permissions) &&
                Objects.equals(menus, dto.menus);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, code, name, pagePermissionCode, parentId, resourceLevel, type, serviceCode, sort, isDefault, icon, category, searchCondition, permissions, menus);
    }
}
