package org.opengauss.admin.plugin.domain.model.ops;

import org.opengauss.admin.plugin.domain.entity.ops.OpsClusterNodeEntity;
import lombok.Data;

/**
 * @author lhf
 * @date 2022/9/26 17:39
 **/
@Data
public class OpsClusterNodeVO {
    private String nodeId;
    private String clusterRole;
    private String publicIp;
    private String privateIp;
    private String hostname;
    private String azName;
    private String azAddress;
    private String hostId;
    private Integer dbPort;
    private String dbName;
    private String dbUser;
    private String dbUserPassword;
    private Integer hostPort;
    //private String rootPassword;
    private Boolean isRemember;
    private String installUserName;

    public static OpsClusterNodeVO of(OpsClusterNodeEntity opsClusterNodeEntity) {
        OpsClusterNodeVO opsClusterNodeVO = new OpsClusterNodeVO();
        opsClusterNodeVO.setNodeId(opsClusterNodeEntity.getClusterNodeId());
        opsClusterNodeVO.setClusterRole(opsClusterNodeEntity.getClusterRole().name());
        return opsClusterNodeVO;
    }
}
