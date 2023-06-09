package org.opengauss.admin.system.plugin.facade;

import org.opengauss.admin.common.core.domain.entity.ops.OpsHostUserEntity;
import org.opengauss.admin.common.core.domain.model.ops.HostUserBody;
import org.opengauss.admin.system.service.ops.IHostService;
import org.opengauss.admin.system.service.ops.IHostUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * @author lhf
 * @date 2022/11/20 21:09
 **/
@Slf4j
@Service
public class HostUserFacade {

    @Autowired
    private IHostService hostService;
    @Autowired
    private IHostUserService hostUserService;

    public List<OpsHostUserEntity> listHostUserByHostId(String hostId) {
        return hostUserService.listHostUserByHostId(hostId);
    }

    public void removeByHostId(String hostId) {
        hostUserService.removeByHostId(hostId);
    }

    public List<OpsHostUserEntity> listHostUserByHostIdList(List<String> hostIdList) {
        return hostUserService.listHostUserByHostIdList(hostIdList);
    }

    public void add(HostUserBody hostUserBody) {
        hostUserService.add(hostUserBody);
    }

    public void edit(String hostUserId, HostUserBody hostUserBody) {
        hostUserService.edit(hostUserId, hostUserBody);
    }

    public void del(String hostUserId) {
        hostUserService.del(hostUserId);
    }

    public OpsHostUserEntity getOmmUserByHostId(String hostId) {
        return hostUserService.getOmmUserByHostId(hostId);
    }

    public OpsHostUserEntity getRootUserByHostId(String hostId) {
        return hostUserService.getRootUserByHostId(hostId);
    }

    public void save(OpsHostUserEntity hostUserEntity) {
        hostUserService.save(hostUserEntity);
    }

    public OpsHostUserEntity getById(String id) {
        return hostUserService.getById(id);
    }

    public List<OpsHostUserEntity> listByIds(Collection ids) {
        return hostUserService.listByIds(ids);
    }

    public void removeByIds(Collection<String> ids){
        hostUserService.removeByIds(ids);
    }
}
