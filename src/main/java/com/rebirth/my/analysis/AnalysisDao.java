package com.rebirth.my.analysis;

import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface AnalysisDao {
    int insertAnalysis(AnalysisVo vo);

    List<AnalysisVo> selectAnalysisList();
}
