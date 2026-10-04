package com.bitejiuyeke.biteportalservice.flash.domain.dto.result;

import com.bitejiuyeke.biteportalservice.flash.domain.vo.GetSrcVO;
import lombok.Data;

/**
 *
 * @author Emrys
 * content:
 */
@Data
public class GetSrcDTO {
    public GetSrcVO convertToVO() {
        GetSrcVO getSrcVO = new GetSrcVO();
        // TODO: Implement the conversion logic
        return getSrcVO;
    }
}
