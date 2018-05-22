package io.choerodon.iam.api.controller.v1;

import javax.validation.Valid;

import io.choerodon.iam.app.service.UserService;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import io.choerodon.core.base.BaseController;
import io.choerodon.core.domain.Page;
import io.choerodon.core.iam.ResourceLevel;
import io.choerodon.iam.api.dto.UserDTO;
import io.choerodon.iam.api.dto.UserSearchDTO;
import io.choerodon.iam.app.service.OrganizationUserService;
import io.choerodon.mybatis.pagehelper.annotation.SortDefault;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;
import io.choerodon.mybatis.pagehelper.domain.Sort;
import io.choerodon.swagger.annotation.CustomPageRequest;
import io.choerodon.swagger.annotation.Permission;

/**
 * @author superlee
 */
@RestController
@RequestMapping(value = "/v1/organizations/{organization_id}/users")
public class OrganizationUserController extends BaseController {

    private OrganizationUserService organizationUserService;

    private UserService userService;

    public OrganizationUserController(OrganizationUserService organizationUserService,
                                      UserService userService) {
        this.organizationUserService = organizationUserService;
        this.userService = userService;
    }

    /**
     * 新增用户
     */
    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation(value = "新增用户")
    @PostMapping
    public ResponseEntity<UserDTO> create(@PathVariable(name = "organization_id") Long organizationId,
                                          @RequestBody @Valid UserDTO userDTO) {
        userDTO.setOrganizationId(organizationId);
        return new ResponseEntity<>(organizationUserService.create(userDTO, true), HttpStatus.OK);
    }

    /**
     * 删除组织下的用户
     */
    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation(value = "根据id删除用户")
    @DeleteMapping(value = "/{id}")
    public ResponseEntity delete(@PathVariable(name = "organization_id") Long organizationId,
                                 @PathVariable Long id) {
        organizationUserService.delete(organizationId, id);
        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

    /**
     * 更新用户
     *
     * @param organizationId
     * @param id
     * @param userDTO
     * @return
     */
    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation(value = "根据id修改用户")
    @PutMapping(value = "/{id}")
    public ResponseEntity<UserDTO> update(@PathVariable(name = "organization_id") Long organizationId,
                                          @PathVariable Long id,
                                          @RequestBody @Valid UserDTO userDTO) {
        userDTO.setOrganizationId(organizationId);
        userDTO.setId(id);
        return new ResponseEntity<>(organizationUserService.update(userDTO), HttpStatus.OK);
    }

    /**
     * 分页查询
     *
     * @param organizationId
     * @param pageRequest
     * @param user
     * @return
     */
    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation(value = "分页查询")
    @CustomPageRequest
    @PostMapping(value = "/search")
    public ResponseEntity<Page<UserDTO>> list(@PathVariable(name = "organization_id") Long organizationId,
                                              @ApiIgnore
                                              @SortDefault(value = "id", direction = Sort.Direction.DESC)
                                                      PageRequest pageRequest,
                                              @RequestBody UserSearchDTO user) {
        user.setOrganizationId(organizationId);
        return new ResponseEntity<>(organizationUserService.pagingQuery(pageRequest, user), HttpStatus.OK);
    }

    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation(value = "根据id查询组织下的用户")
    @GetMapping(value = "/{id}")
    public ResponseEntity<UserDTO> query(@PathVariable(name = "organization_id") Long organizationId,
                                         @PathVariable Long id) {
        return new ResponseEntity<>(organizationUserService.query(organizationId, id), HttpStatus.OK);
    }

    /**
     * 解锁用户
     */
    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation(value = "根据用户id解锁用户")
    @GetMapping(value = "/{id}/unlock")
    public ResponseEntity<UserDTO> unlock(@PathVariable(name = "organization_id") Long organizationId,
                                          @PathVariable Long id) {
        return new ResponseEntity<>(organizationUserService.unlock(organizationId, id), HttpStatus.OK);
    }

    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation(value = "用户启用")
    @PutMapping(value = "/{id}/enable")
    public ResponseEntity<UserDTO> enableUser(@PathVariable(name = "organization_id") Long organizationId,
                                              @PathVariable Long id) {
        return new ResponseEntity<>(organizationUserService.enableUser(organizationId, id), HttpStatus.OK);
    }

    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation(value = "用户禁用")
    @PutMapping(value = "/{id}/disable")
    public ResponseEntity<UserDTO> disableUser(@PathVariable(name = "organization_id") Long organizationId,
                                               @PathVariable Long id) {
        return new ResponseEntity<>(organizationUserService.disableUser(organizationId, id), HttpStatus.OK);
    }

    @Permission(level = ResourceLevel.ORGANIZATION)
    @ApiOperation(value = "用户信息重名校验接口(email/loginName)，新建校验json里面不传id,更新校验传id")
    @PostMapping(value = "/check")
    public ResponseEntity check(@PathVariable(name = "organization_id") Long organizationId,
                                @RequestBody UserDTO user) {
        userService.check(user);
        return new ResponseEntity(HttpStatus.OK);
    }

}
