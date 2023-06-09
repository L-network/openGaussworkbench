package org.opengauss.admin.web.controller.ops;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.opengauss.admin.common.core.controller.BaseController;
import org.opengauss.admin.common.core.domain.AjaxResult;
import org.opengauss.admin.common.core.domain.entity.ops.OpsHostUserEntity;
import org.opengauss.admin.common.core.domain.model.ops.HostUserBody;
import org.opengauss.admin.common.core.page.TableDataInfo;
import org.opengauss.admin.system.service.ops.IHostUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author lhf
 * @date 2022/8/11 23:00
 **/
@RestController
@RequestMapping("/hostUser")
public class HostUserController extends BaseController {

    @Autowired
    private IHostUserService hostUserService;

    @GetMapping("/page/{hostId}")
    public TableDataInfo page(@PathVariable("hostId") String hostId) {
        IPage<OpsHostUserEntity> page = hostUserService.page(startPage(), Wrappers.lambdaQuery(OpsHostUserEntity.class).eq(OpsHostUserEntity::getHostId, hostId));
        return getDataTable(page);
    }

    @GetMapping("/listAll/{hostId}")
    public AjaxResult listAll(@PathVariable("hostId") String hostId) {
        List<OpsHostUserEntity> opsHostUserEntityList = hostUserService.list(Wrappers.lambdaQuery(OpsHostUserEntity.class).eq(OpsHostUserEntity::getHostId, hostId));
        return AjaxResult.success(opsHostUserEntityList);
    }

    @GetMapping("/listAllWithoutRoot/{hostId}")
    public AjaxResult listAllWithoutRoot(@PathVariable("hostId") String hostId) {
        LambdaQueryWrapper<OpsHostUserEntity> queryWrapper = Wrappers.lambdaQuery(OpsHostUserEntity.class)
                .eq(OpsHostUserEntity::getHostId, hostId)
                .ne(OpsHostUserEntity::getUsername, "root");
        List<OpsHostUserEntity> opsHostUserEntityList = hostUserService.list(queryWrapper);
        return AjaxResult.success(opsHostUserEntityList);
    }

    @PostMapping
    public AjaxResult add(@RequestBody @Validated HostUserBody hostUserBody) {
        hostUserService.add(hostUserBody);
        return AjaxResult.success();
    }

    @PutMapping("/{hostUserId}")
    public AjaxResult edit(@PathVariable String hostUserId,
                           @RequestBody HostUserBody hostUserBody) {
        hostUserService.edit(hostUserId, hostUserBody);
        return AjaxResult.success();
    }

    @DeleteMapping("/{hostUserId}")
    public AjaxResult del(@PathVariable String hostUserId) {
        hostUserService.del(hostUserId);
        return AjaxResult.success();
    }
}
