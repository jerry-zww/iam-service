package io.choerodon.iam.api.controller.v1;

import com.github.pagehelper.Page;
import io.choerodon.base.annotation.Permission;
import io.choerodon.base.constant.PageConstant;
import io.choerodon.base.enums.ResourceType;
import io.choerodon.iam.api.dto.ApplicationSearchDTO;
import io.choerodon.iam.app.service.ApplicationService;
import io.choerodon.iam.infra.dto.ApplicationDTO;
import io.choerodon.iam.infra.dto.ApplicationExplorationDTO;
import io.choerodon.swagger.annotation.CustomPageRequest;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * @author superlee
 * @since 0.15.0
 **/
@RestController
@RequestMapping(value = "/v1/organizations/{organization_id}/applications")
public class ApplicationController {

    private ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }


    @Permission(type = ResourceType.ORGANIZATION)
    @ApiOperation(value = "创建应用")
    @PostMapping
    public ResponseEntity<ApplicationDTO> create(@PathVariable("organization_id") Long organizationId,
                                                 @RequestBody @Valid ApplicationDTO applicationDTO) {
        applicationDTO.setOrganizationId(organizationId);
        return new ResponseEntity<>(applicationService.create(applicationDTO), HttpStatus.OK);
    }

    @Permission(type = ResourceType.ORGANIZATION)
    @ApiOperation(value = "更新应用")
    @PostMapping("/{id}")
    public ResponseEntity<ApplicationDTO> update(@PathVariable("organization_id") Long organizationId,
                                                 @PathVariable("id") Long id,
                                                 @RequestBody @Valid ApplicationDTO applicationDTO) {
        applicationDTO.setOrganizationId(organizationId);
        applicationDTO.setId(id);
        return new ResponseEntity<>(applicationService.update(applicationDTO), HttpStatus.OK);
    }


    @Permission(type = ResourceType.ORGANIZATION)
    @ApiOperation(value = "分页查询应用")
    @CustomPageRequest
    @GetMapping
    public ResponseEntity<Page<ApplicationDTO>> pagingQuery(@PathVariable("organization_id") Long organizationId,
                                                            @RequestParam(defaultValue = PageConstant.PAGE, required = false) final int page,
                                                            @RequestParam(defaultValue = PageConstant.SIZE, required = false) final int size,
                                                            ApplicationSearchDTO applicationSearchDTO) {
        applicationSearchDTO.setOrganizationId(organizationId);
        return new ResponseEntity<>(applicationService.pagingQuery(page,size,applicationSearchDTO), HttpStatus.OK);
    }

    @Permission(type = ResourceType.ORGANIZATION)
    @ApiOperation(value = "启用应用")
    @PutMapping(value = "/{id}/enable")
    public ResponseEntity<ApplicationDTO> enabled(@PathVariable Long id) {
        return new ResponseEntity<>(applicationService.enable(id), HttpStatus.OK);
    }

    @Permission(type = ResourceType.ORGANIZATION)
    @ApiOperation(value = "禁用应用")
    @PutMapping(value = "/{id}/disable")
    public ResponseEntity<ApplicationDTO> disable(@PathVariable Long id) {
        return new ResponseEntity<>(applicationService.disable(id), HttpStatus.OK);
    }

    @Permission(type = ResourceType.ORGANIZATION)
    @ApiOperation(value = "获取application的类型列表")
    @GetMapping(value = "/types")
    public ResponseEntity<List<String>> types() {
        return new ResponseEntity<>(applicationService.types(), HttpStatus.OK);

    }

    @Permission(type = ResourceType.ORGANIZATION)
    @ApiOperation(value = "重名校验")
    @PostMapping("/check")
    public ResponseEntity check(@PathVariable("organization_id") Long organizationId,
                                @RequestBody ApplicationDTO applicationDTO) {
        applicationDTO.setOrganizationId(organizationId);
        applicationService.check(applicationDTO);
        return new ResponseEntity(HttpStatus.OK);
    }

    @Permission(type = ResourceType.ORGANIZATION)
    @ApiOperation(value = "将应用/组合应用添加到组合应用中")
    @PostMapping("/{id}/add_to_combination")
    public ResponseEntity addToCombination(@PathVariable("organization_id") Long organizationId,
                                           @PathVariable("id") Long id,
                                           @RequestBody Long[] ids) {
        applicationService.addToCombination(organizationId, id, ids);
        return new ResponseEntity(HttpStatus.OK);
    }


    @Permission(type = ResourceType.ORGANIZATION)
    @ApiOperation(value = "查询组合应用的后代")
    @GetMapping("/{id}/descendant")
    public ResponseEntity<List<ApplicationExplorationDTO>> queryDescendant(@PathVariable("organization_id") Long organizationId,
                                                                           @PathVariable("id") Long id) {
        return new ResponseEntity<>(applicationService.queryDescendant(id), HttpStatus.OK);
    }

    @Permission(type = ResourceType.ORGANIZATION)
    @ApiOperation(value = "查询可以向指定组合应用添加的后代，判别标准是不构成环")
    @GetMapping("/{id}/enabled_app")
    public ResponseEntity<List<ApplicationDTO>> queryEnabledApplication(@PathVariable("organization_id") Long organizationId,
                                                                        @PathVariable("id") Long id) {
        return new ResponseEntity<>(applicationService.queryEnabledApplication(organizationId, id), HttpStatus.OK);
    }

    @Permission(type = ResourceType.ORGANIZATION)
    @ApiOperation(value = "查询组合应用下普通应用清单")
    @CustomPageRequest
    @GetMapping("/{id}/app_list")
    public ResponseEntity<Page<ApplicationDTO>> queryApplicationList(@RequestParam(defaultValue = PageConstant.PAGE, required = false) final int page,
                                                                     @RequestParam(defaultValue = PageConstant.SIZE, required = false) final int size,
                                                                     @PathVariable("organization_id") Long organizationId,
                                                                     @PathVariable("id") Long id,
                                                                     @RequestParam(required = false) String name,
                                                                     @RequestParam(required = false) String code) {
        return new ResponseEntity<>(applicationService.queryApplicationList(page,size, id, name, code), HttpStatus.OK);
    }

    @Permission(type = ResourceType.ORGANIZATION)
    @ApiOperation(value = "根据id查询应用详情")
    @GetMapping("/{id}")
    public ResponseEntity<ApplicationDTO> query(@PathVariable("organization_id") Long organizationId,
                                                @PathVariable("id") Long id) {
        return new ResponseEntity<>(applicationService.query(id), HttpStatus.OK);
    }


}
