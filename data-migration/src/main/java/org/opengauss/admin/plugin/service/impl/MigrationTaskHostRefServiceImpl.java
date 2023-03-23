package org.opengauss.admin.plugin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gitee.starblues.bootstrap.annotation.AutowiredType;
import lombok.extern.slf4j.Slf4j;
import org.opengauss.admin.common.core.domain.entity.ops.OpsHostEntity;
import org.opengauss.admin.common.core.domain.entity.ops.OpsHostUserEntity;
import org.opengauss.admin.common.core.domain.model.ops.OpsClusterNodeVO;
import org.opengauss.admin.common.core.domain.model.ops.OpsClusterVO;
import org.opengauss.admin.common.core.domain.model.ops.jdbc.JdbcDbClusterInputDto;
import org.opengauss.admin.common.core.domain.model.ops.jdbc.JdbcDbClusterNodeInputDto;
import org.opengauss.admin.common.core.domain.model.ops.jdbc.JdbcDbClusterVO;
import org.opengauss.admin.common.utils.StringUtils;
import org.opengauss.admin.plugin.domain.MigrationHostPortalInstall;
import org.opengauss.admin.plugin.domain.MigrationTask;
import org.opengauss.admin.plugin.domain.MigrationTaskHostRef;
import org.opengauss.admin.plugin.dto.CustomDbResource;
import org.opengauss.admin.plugin.handler.PortalHandle;
import org.opengauss.admin.plugin.mapper.MigrationTaskHostRefMapper;
import org.opengauss.admin.plugin.service.MigrationHostPortalInstallHostService;
import org.opengauss.admin.plugin.service.MigrationTaskHostRefService;
import org.opengauss.admin.plugin.service.MigrationTaskService;
import org.opengauss.admin.system.plugin.facade.HostFacade;
import org.opengauss.admin.system.plugin.facade.HostUserFacade;
import org.opengauss.admin.system.plugin.facade.JdbcDbClusterFacade;
import org.opengauss.admin.system.plugin.facade.OpsFacade;
import org.opengauss.admin.system.service.ops.impl.EncryptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author xielibo
 * @date 2023/01/14 09:01
 **/
@Service
@Slf4j
public class MigrationTaskHostRefServiceImpl extends ServiceImpl<MigrationTaskHostRefMapper, MigrationTaskHostRef> implements MigrationTaskHostRefService {

    @Autowired
    @AutowiredType(AutowiredType.Type.PLUGIN_MAIN)
    private HostFacade hostFacade;

    @Autowired
    @AutowiredType(AutowiredType.Type.PLUGIN_MAIN)
    private HostUserFacade hostUserFacade;

    @Autowired
    @AutowiredType(AutowiredType.Type.PLUGIN_MAIN)
    private OpsFacade opsFacade;

    @Autowired
    @AutowiredType(AutowiredType.Type.PLUGIN_MAIN)
    private EncryptionUtils encryptionUtils;

    @Autowired
    @AutowiredType(AutowiredType.Type.PLUGIN_MAIN)
    private JdbcDbClusterFacade jdbcDbClusterFacade;

    @Autowired
    private MigrationTaskHostRefMapper migrationTaskHostRefMapper;
    @Autowired
    private MigrationTaskService migrationTaskService;
    @Value("${migration.portalPkgDownloadUrl}")
    private String portalPkgDownloadUrl;
    @Autowired
    private MigrationHostPortalInstallHostService migrationHostPortalInstallHostService;
    @Autowired
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;

    @Override
    public void deleteByMainTaskId(Integer mainTaskId) {
        LambdaQueryWrapper<MigrationTaskHostRef> query = new LambdaQueryWrapper<>();
        query.eq(MigrationTaskHostRef::getMainTaskId, mainTaskId);
        this.remove(query);
    }

    @Override
    public List<MigrationTaskHostRef> listByMainTaskId(Integer mainTaskId) {
        List<MigrationTaskHostRef> hosts = migrationTaskHostRefMapper.selectByMainTaskId(mainTaskId);
        hosts.stream().forEach(h -> {
            OpsHostEntity opsHost = hostFacade.getById(h.getRunHostId());
            h.setHostName(opsHost.getHostname());
            h.setHost(opsHost.getPublicIp());
            h.setPort(opsHost.getPort());
            List<OpsHostUserEntity> opsHostUserEntities = hostUserFacade.listHostUserByHostId(h.getRunHostId());
            if (opsHostUserEntities.size() > 0) {
                OpsHostUserEntity notRoot = opsHostUserEntities.stream().filter(x -> !x.getUsername().equals("root")).findFirst().orElse(null);
                if (notRoot != null) {
                    h.setUser(notRoot.getUsername());
                    h.setPassword(encryptionUtils.decrypt(notRoot.getPassword()));
                }
            }
        });
        return hosts;
    }


    @Override
    public List<Map<String, Object>> getHosts() {
        List<OpsHostEntity> opsHostEntities = hostFacade.listAll();
        List<Map<String, Object>> hosts = opsHostEntities.stream().map(host -> {
            Map<String, Object> itemMap = BeanUtil.beanToMap(host);
            List<MigrationTask> tasks = migrationTaskService.listRunningTaskByHostId(host.getHostId());
            itemMap.put("tasks", tasks);
            OpsHostEntity opsHost = hostFacade.getById(host.getHostId());
            List<OpsHostUserEntity> opsHostUserEntities = hostUserFacade.listHostUserByHostId(opsHost.getHostId());
            if (opsHostUserEntities.size() > 0) {
                OpsHostUserEntity notRoot = opsHostUserEntities.stream().filter(x -> !x.getUsername().equals("root")).findFirst().orElse(null);
                if (notRoot != null) {
                    boolean isInstallPortal = PortalHandle.checkInstallPortal(opsHost.getPublicIp(), opsHost.getPort(), notRoot.getUsername(), encryptionUtils.decrypt(notRoot.getPassword()));
                    if (isInstallPortal) {
                        itemMap.put("installPortalStatus", 2);
                    } else {
                        MigrationHostPortalInstall portalInstall = migrationHostPortalInstallHostService.getOneByHostId(opsHost.getHostId());
                        if(portalInstall != null) {
                            itemMap.put("installPortalStatus", portalInstall.getInstallStatus());
                        } else {
                            itemMap.put("installPortalStatus", 0);
                        }
                    }
                }
            }
            return itemMap;
        }).collect(Collectors.toList());
        return hosts;
    }

    @Override
    public List<JdbcDbClusterVO> getSourceClusters(){
        List<JdbcDbClusterVO> jdbcDbClusterVOS = jdbcDbClusterFacade.listAll();
        return jdbcDbClusterVOS;
    }

    @Override
    public void saveDbResource(CustomDbResource dbResource){
        saveSource(dbResource.getClusterName(), dbResource.getDbUrl(), dbResource.getUserName(), dbResource.getPassword());
    }
    @Override
    public void saveSource(String clusterName, String dbUrl, String username, String password) {
        JdbcDbClusterInputDto clusterInput = new JdbcDbClusterInputDto();
        clusterInput.setClusterName(clusterName);
        JdbcDbClusterNodeInputDto node = new JdbcDbClusterNodeInputDto();
        node.setUrl(dbUrl);
        node.setUsername(username);
        node.setPassword(password);
        List<JdbcDbClusterNodeInputDto> nodes = new ArrayList<>();
        nodes.add(node);
        clusterInput.setNodes(nodes);
        jdbcDbClusterFacade.add(clusterInput);
    }

    @Override
    public List<OpsClusterVO> getTargetClusters(){
        List<OpsClusterVO> opsClusterVOS = opsFacade.listCluster();
        return opsClusterVOS;
    }

    @Override
    public List<String> getMysqlClusterDbNames(String url, String username, String password){
        String sql = "SELECT `SCHEMA_NAME` FROM `information_schema`.`SCHEMATA`;";
        List<String> dbList = new ArrayList<>();
        try {
            List<Map<String, Object>> resultSet = this.querySource(url, username, password, sql);
            resultSet.forEach(ret->{
                String schemaName = ret.get("SCHEMA_NAME").toString();
                dbList.add(schemaName);
            });
        } catch (SQLException | ClassNotFoundException e) {
        }
        return dbList;
    }

    @Override
    public List<Map<String, Object>> getOpsClusterDbNames(OpsClusterNodeVO clusterNode) {
        List<Map<String, Object>> dbList = new ArrayList<>();
        if (clusterNode.getHostPort() == 22) {
            //get database name list
            String sql = "select datname from pg_database;";
            try {
                List<Map<String, Object>> resultSet = this.queryTarget(clusterNode, "", sql);
                resultSet.forEach(ret -> {
                    Map<String, Object> itemMap = new HashMap<>();
                    String datname = ret.get("datname").toString();
                    itemMap.put("dbName", datname);
                    Integer count = migrationTaskService.countRunningByTargetDb(datname);
                    itemMap.put("isSelect", count == 0);
                    dbList.add(itemMap);
                });
            } catch (SQLException | ClassNotFoundException e) {
            }
        }
        return dbList;
    }

    private List<Map<String, Object>> convertList(ResultSet rs) {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        try {
            ResultSetMetaData md = rs.getMetaData();
            int columnCount = md.getColumnCount();
            while (rs.next()) {
                Map<String, Object> rowData = new HashMap<String, Object>();
                for (int i = 1; i <= columnCount; i++) {
                    rowData.put(md.getColumnName(i), rs.getObject(i));
                }
                list.add(rowData);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return list;
    }

    private List<Map<String, Object>> querySource(String url, String username, String password, String sql) throws ClassNotFoundException, SQLException {
        ResultSet resultSet;
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection conn = DriverManager.getConnection(url, username, password);

        if (sql == null || conn == null) {
            throw new RuntimeException("sql is empty");
        }
        PreparedStatement preparedStatement = conn.prepareStatement(sql);
        resultSet = preparedStatement.executeQuery();
        List<Map<String, Object>> result = convertList(resultSet);
        resultSet.close();
        preparedStatement.close();
        conn.close();
        return result;
    }


    private List<Map<String, Object>> queryTarget(OpsClusterNodeVO clusterNode, String schema, String sql) throws ClassNotFoundException, SQLException {
        ResultSet resultSet;

        Class.forName("org.opengauss.Driver");
        String openGaussUrl = "jdbc:opengauss://"+clusterNode.getPublicIp()+":"+clusterNode.getDbPort()+"/"+clusterNode.getDbName()+"?currentSchema="+schema;
        Connection conn = DriverManager.getConnection(openGaussUrl, clusterNode.getDbUser(), clusterNode.getDbUserPassword());

        if (sql == null || conn == null) {
            throw new RuntimeException("sql is empty");
        }
        PreparedStatement preparedStatement = conn.prepareStatement(sql);
        resultSet = preparedStatement.executeQuery();
        List<Map<String, Object>> result = convertList(resultSet);
        resultSet.close();
        preparedStatement.close();
        conn.close();
        return result;
    }

    @Override
    public void installPortal(String hostId, boolean isNewFileInstall) {
        threadPoolTaskExecutor.submit(() -> {
            try {
                installPortalHandler(hostId, isNewFileInstall);
            } catch (Exception e) {
                e.printStackTrace();
                log.error("sync install portal error", e);
            }
        });
        migrationHostPortalInstallHostService.saveRecord(hostId, 1);
    }

    private void installPortalHandler(String hostId, boolean isNewFileInstall) {
        OpsHostEntity opsHost = hostFacade.getById(hostId);
        List<OpsHostUserEntity> opsHostUserEntities = hostUserFacade.listHostUserByHostId(hostId);
        if (opsHostUserEntities.size() > 0) {
            OpsHostUserEntity notRoot = opsHostUserEntities.stream().filter(x -> !x.getUsername().equals("root")).findFirst().orElse(null);
            if (notRoot != null) {
                boolean flag = PortalHandle.installPortal(opsHost.getPublicIp(), opsHost.getPort(), notRoot.getUsername(), encryptionUtils.decrypt(notRoot.getPassword()), portalPkgDownloadUrl, isNewFileInstall);
                migrationHostPortalInstallHostService.saveRecord(hostId, flag ? 2 : 10);
            }
        }
    }

    @Override
    public String getPortalInstallLog(String hostId) {
        OpsHostEntity opsHost = hostFacade.getById(hostId);
        String logContent = " ";
        List<OpsHostUserEntity> opsHostUserEntities = hostUserFacade.listHostUserByHostId(hostId);
        if (opsHostUserEntities.size() > 0) {
            OpsHostUserEntity notRoot = opsHostUserEntities.stream().filter(x -> !x.getUsername().equals("root")).findFirst().orElse(null);
            if (notRoot != null) {
                String logPath = "~/portal/logs/portal_.log";
                String content = PortalHandle.getTaskLogs(opsHost.getPublicIp(), opsHost.getPort(), notRoot.getUsername(), encryptionUtils.decrypt(notRoot.getPassword()), logPath);
                if (StringUtils.isNotBlank(content)) {
                    logContent = content;
                }
            }
        }
        return logContent;
    }

}
