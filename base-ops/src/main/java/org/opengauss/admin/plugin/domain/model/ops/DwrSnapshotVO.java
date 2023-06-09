package org.opengauss.admin.plugin.domain.model.ops;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * @author lhf
 * @date 2022/10/24 13:55
 **/
@Data
public class DwrSnapshotVO {
    private String snapshot_id;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date start_ts;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date end_ts;
}
